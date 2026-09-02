package com.github.jing332.tts.speech.plugin.engine

import androidx.annotation.Keep
import com.github.jing332.script.exception.ScriptException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.mozilla.javascript.Context
import org.mozilla.javascript.typedarrays.NativeArrayBuffer
import org.mozilla.javascript.typedarrays.NativeUint8Array
import java.io.IOException
import java.io.InputStream

/**
 * JS getAudioV2 回调写入 → 播放链读取 的桥接流。
 *
 * 旧实现用 PipedInputStream/PipedOutputStream（固定 1024 字节缓冲）：
 * JS 在 invokeMethod 内同步 write，而消费端要等 invokeMethod 返回后才开始读，
 * 音频一超过缓冲区写端就永久阻塞 → 30s 超时被当协程取消 → 试听一直转圈无声
 * （所有 callback.write 型插件的通病，如本悦读/nami/ytbarrage）。
 *
 * 现改为可增长的分块阻塞队列：write 永不阻塞（拷贝后入队），read 空时等待
 * 直到新数据/close/错误；close 后已入队数据仍可读完再返回 -1（EOF），
 * 与原管道语义一致。
 */
class JsBridgeInputStream : InputStream() {
    companion object {
        private const val TAG = "JsBridgeInputStream"
        private val logger = KotlinLogging.logger(TAG)
    }

    private val lock = Object()
    private val chunks = ArrayDeque<ByteArray>()
    private var current: ByteArray? = null
    private var currentPos = 0
    private var closed = false
    private var errorCause: Exception? = null

    private val hasError: Boolean
        get() = errorCause != null

    private fun checkError() {
        errorCause?.let { throw it }
    }

    override fun read(): Int {
        val b = ByteArray(1)
        val n = read(b, 0, 1)
        return if (n == -1) -1 else b[0].toInt() and 0xFF
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        checkError()
        if (off < 0 || len < 0 || len > b.size - off) {
            throw IndexOutOfBoundsException()
        } else if (len == 0) {
            return 0
        }

        synchronized(lock) {
            while (current == null && chunks.isEmpty() && !closed) {
                checkError()
                // 定期唤醒检查 closed/error，避免消费端取消时永久挂起
                lock.wait(1000)
            }
            checkError()
            if (current == null && chunks.isEmpty() && closed) return -1 // EOF

            if (current == null) {
                current = chunks.removeFirst()
                currentPos = 0
            }
            val cur = current!!
            val n = minOf(len, cur.size - currentPos)
            System.arraycopy(cur, currentPos, b, off, n)
            currentPos += n
            if (currentPos >= cur.size) {
                current = null
                currentPos = 0
            }
            return n
        }
    }

    override fun available(): Int {
        synchronized(lock) {
            return (current?.let { it.size - currentPos } ?: 0) + chunks.sumOf { it.size }
        }
    }

    @Synchronized
    override fun close() {
        synchronized(lock) {
            closed = true
            lock.notifyAll()
        }
    }

    /**
     * 流式 PCM 格式声明（小米 MiMo/硅基 CosyVoice2 等插件合成首块前调用，与 streaming 声明对应）
     */
    var streamFormat: StreamFormat? = null

    data class StreamFormat(val encoding: String?, val sampleRate: Int, val channels: Int)

    /**
     *  Interface for JavaScript to interact with the OutputStream.  The names
     *  and signatures MUST match your Kotlin definitions.
     */
    @Keep
    interface Callback {
        fun write(data: Any?)
        fun close()
        fun error(data: Any?)
        fun streamStart(encoding: String?, sampleRate: Int, channels: Int)
        fun streamWrite(data: Any?)
        fun streamComplete()
    }

    suspend fun getCallback(): Callback {
        return object : Callback {
            private var length = 0
            private fun writeBytes(data: ByteArray) {
                length += data.size
                // 屏蔽音频数据写入的调试日志，避免日志过多
                // logger.debug { "write(${data.size}) byteWritten: $length" }

                if (closed || hasError) return

                // 拷贝入队：JS 侧可能复用底层缓冲，且写永不阻塞（根治定长管道卡死）
                synchronized(lock) {
                    chunks.addLast(data.copyOf())
                    lock.notifyAll()
                }
            }

            override fun write(data: Any?) {
                when (data) {
                    is ByteArray -> writeBytes(data)
                    is String -> writeBytes(data.toByteArray())
                    is NativeUint8Array -> write(data.buffer.buffer)
                    is NativeArrayBuffer -> write(data.buffer)
                }

            }

            override fun close() {
                logger.debug { "close" }

                try {
                    if (length <= 0 && errorCause == null) errorCause = IOException("No data written")

                    this@JsBridgeInputStream.close()
                } catch (e: IOException) {
                    if (errorCause == null) errorCause = e
                }
            }

            override fun error(data: Any?) {
                logger.debug { "error(${data})" }

                errorCause = Context.reportRuntimeError(data.toString()).run {
                    ScriptException(
                        sourceName = sourceName(),
                        lineNumber = lineNumber(),
                        columnNumber = columnNumber(),
                        message = message,
                        cause = this
                    )
                }
                try {
                    close()
                } catch (ignored: IOException) {
                }
            }

            // 流式协议桥（小米 MiMo 三款/硅基 CosyVoice2）：streamStart 声明格式，
            // streamWrite 等价 write 分块入队，streamComplete 收尾同 close
            override fun streamStart(encoding: String?, sampleRate: Int, channels: Int) {
                this@JsBridgeInputStream.streamFormat = StreamFormat(encoding, sampleRate, channels)
            }

            override fun streamWrite(data: Any?) {
                write(data)
            }

            override fun streamComplete() {
                close()
            }
        }
    }
}
