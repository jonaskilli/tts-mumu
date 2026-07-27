package com.github.jing332.tts.speech.plugin.engine

import androidx.annotation.Keep
import com.github.jing332.script.exception.ScriptException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.mozilla.javascript.Context
import org.mozilla.javascript.typedarrays.NativeArrayBuffer
import org.mozilla.javascript.typedarrays.NativeUint8Array
import java.io.IOException
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

class JsBridgeInputStream : InputStream() {
    companion object {
        private const val TAG = "JsBridgeInputStream"
        private val logger = KotlinLogging.logger(TAG)
    }

    private val pis: PipedInputStream = PipedInputStream()
    private val pos: PipedOutputStream = PipedOutputStream(pis)
    private var isClosed = false
    private var errorCause: Exception? = null
    private val hasError: Boolean
        get() = errorCause != null

    private fun checkError() {
        errorCause?.let {
            throw it
        }
    }

    override fun read(): Int {
        checkError()
        if (isClosed && pis.available() == 0) {
            return -1 // Signal end of stream
        }

        try {
            val byte = pis.read() // Reads a single byte
            checkError()
            return byte
        } catch (e: IOException) {
            errorCause = e
            throw e
        }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        checkError()
        if (isClosed && pis.available() == 0) {  // Check for EOF *before* blocking read.  Crucial!
            return -1
        }
        if (off < 0 || len < 0 || len > b.size - off) {
            throw IndexOutOfBoundsException()
        } else if (len == 0) {
            return 0
        }

        try {
            val byte = pis.read(b, off, len) // Reads up to 'len' bytes into the buffer
            checkError()
            return byte
        } catch (e: IOException) {
            errorCause = e
            throw e
        }
    }

    override fun available(): Int {
        checkError()
        return pis.available().apply {
            checkError()
        }
    }

    @Synchronized
    override fun close() {
        if (!isClosed) {
            isClosed = true
            try {
                pos.close() // Close output end first!  Very important.
            } finally {
                pis.close() // Then close the input end.
            }
        }
    }

    /**
     *  Interface for JavaScript to interact with the OutputStream.  The names
     *  and signatures MUST match your Kotlin definitions.
     */
    @Keep
    interface Callback {
        fun write(data: Any?)
        fun close()
        fun error(data: Any?)
    }

    suspend fun getCallback(): Callback {
        return object : Callback {
            private var length = 0
            private fun writeBytes(data: ByteArray) {
                length += data.size
                // 屏蔽音频数据写入的调试日志，避免日志过多
                // logger.debug { "write(${data.size}) byteWritten: $length" }

                if (isClosed || hasError) return

                try {
                    pos.write(data)
                    pos.flush()
                } catch (e: IOException) {
                    errorCause = e
                    try {
                        close()
                    } catch (ignored: IOException) {
                    }
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
        }
    }
}
