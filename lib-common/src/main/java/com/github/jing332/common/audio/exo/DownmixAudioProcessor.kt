package com.github.jing332.common.audio.exo

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

@OptIn(UnstableApi::class)
/**
 * 多声道 16bit PCM 降混为单声道（各声道取平均）。
 * 朗读链向系统声明的是单声道（channelCount=1），解码出的立体声音源若直接按单声道
 * 消费，交错样本数翻倍会导致时长翻倍（表现为 0.5 倍速），必须先降混再进后续管线。
 */
class DownmixAudioProcessor : BaseAudioProcessor() {
    private var channelCount = 1

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        channelCount = inputAudioFormat.channelCount.coerceAtLeast(1)
        return AudioProcessor.AudioFormat(
            inputAudioFormat.sampleRate,
            1,
            inputAudioFormat.encoding
        )
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val frames = inputBuffer.remaining() / (2 * channelCount)
        if (frames == 0) {
            inputBuffer.position(inputBuffer.limit())
            return
        }

        val buffer = replaceOutputBuffer(frames * 2)
        repeat(frames) {
            var sum = 0L
            repeat(channelCount) {
                val lo = inputBuffer.get().toInt() and 0xFF
                val hi = inputBuffer.get().toInt()
                sum += ((hi shl 8) or lo).toShort().toInt()
            }
            val avg = (sum / channelCount).toInt()
            buffer.put((avg and 0xFF).toByte())
            buffer.put((avg shr 8).toByte())
        }
        // 解码器输出按帧对齐，正常无残余；不足一帧的零头字节直接放弃
        inputBuffer.position(inputBuffer.limit())
        buffer.flip()
    }
}
