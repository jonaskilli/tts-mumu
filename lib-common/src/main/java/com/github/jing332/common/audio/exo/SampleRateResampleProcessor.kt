package com.github.jing332.common.audio.exo

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.floor

@OptIn(UnstableApi::class)
/**
 * 线性插值重采样（16bit PCM）：inputSampleRate → outputSampleRate，时长与音调不变。
 * 用于把探测/配置的真实输入采样率统一到目标采样率——
 * Sonic 的 rate 是"变速变调"参数（时长与音调一起变），不能当重采样器用。
 * 仅单声道（调用方管线为单声道）。
 */
class SampleRateResampleProcessor(
    private val inputSampleRate: Int,
    private val outputSampleRate: Int,
) : BaseAudioProcessor() {

    init {
        require(inputSampleRate > 0 && outputSampleRate > 0 && inputSampleRate != outputSampleRate)
    }

    // 跨 buffer 的插值连续性：上一 buffer 的最后一个样本值 + 下一个输出样本的相位（相对本 buffer 首样本）
    private var carry = 0.0
    private var hasCarry = false
    private var pos = 0.0

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        return AudioProcessor.AudioFormat(
            outputSampleRate,
            inputAudioFormat.channelCount,
            inputAudioFormat.encoding
        )
    }

    // 小端 PCM16 读样本
    private fun readSample(buf: ByteBuffer, sampleIndex: Int): Double {
        val lo = buf.get(sampleIndex * 2).toInt() and 0xFF
        val hi = buf.get(sampleIndex * 2 + 1).toInt()
        return ((hi shl 8) or lo).toShort().toDouble()
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val inSamples = inputBuffer.remaining() / 2
        if (inSamples == 0) {
            inputBuffer.position(inputBuffer.limit())
            return
        }

        // 输出样本 k 对应输入位置 p = pos + k*ratio；
        // p 落在 [-1, inSamples-1) 内即可在本 buffer 内插值（-1 用 carry），越界部分留给下一 buffer
        val ratio = inputSampleRate.toDouble() / outputSampleRate
        val maxOut = (inSamples / ratio).toInt() + 2
        val out = ShortArray(maxOut)
        var outN = 0
        var p = pos
        while (p < inSamples - 1) {
            val i0 = floor(p).toInt()
            val frac = p - i0
            val s0 = if (i0 < 0) {
                if (hasCarry) carry else readSample(inputBuffer, 0)
            } else readSample(inputBuffer, i0)
            val s1 = readSample(inputBuffer, i0 + 1)
            out[outN++] = (s0 + (s1 - s0) * frac).toInt().toShort()
            p += ratio
        }

        // carry = 本 buffer 最后一个样本（下一 buffer 的 -1 位置）
        carry = readSample(inputBuffer, inSamples - 1)
        hasCarry = true
        // pos 相对下一 buffer 首样本：p 已 >= inSamples-1，减去 inSamples 后落在 [-1, ...)
        pos = p - inSamples

        val buffer = replaceOutputBuffer(outN * 2)
        for (i in 0 until outN) {
            val v = out[i]
            buffer.put((v.toInt() and 0xFF).toByte())
            buffer.put((v.toInt() shr 8).toByte())
        }
        buffer.flip()
        inputBuffer.position(inputBuffer.limit())
    }
}
