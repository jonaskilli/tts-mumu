package com.github.jing332.common.audio.exo

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.tanh

/**
 * 响度均衡 PCM 增益处理器
 *
 * 在 SonicAudioProcessor 之后对 PCM 数据施加自动增益补偿，
 * 用于平衡不同发音人之间的响度差异。
 *
 * 设计参考 legado 的 ReadAloudLoudnessAudioProcessor。
 *
 * 特性：
 * - 增益范围 0.35x ~ 2.4x
 * - 1600 样本线性插值平滑过渡，避免切换发音人时爆音
 * - tanh 软限幅，增益放大后防止硬削波失真
 */
@OptIn(UnstableApi::class)
class LoudnessAudioProcessor : BaseAudioProcessor() {

    @Volatile
    private var targetGain = 1f
    private var currentGain = 1f
    private var rampSamplesRemaining = 0

    /**
     * 设置目标增益，会在 1600 个采样内平滑过渡。
     * @param gain 目标增益值，会被限制在 [MIN_GAIN, MAX_GAIN] 范围内
     */
    fun setGain(gain: Float) {
        val next = gain.coerceIn(MIN_GAIN, MAX_GAIN)
        if (abs(next - targetGain) < 0.01f) return
        targetGain = next
        rampSamplesRemaining = RAMP_SAMPLES
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun isActive(): Boolean = true

    override fun queueInput(inputBuffer: ByteBuffer) {
        val output = replaceOutputBuffer(inputBuffer.remaining())
        while (inputBuffer.remaining() >= 2) {
            val lo = inputBuffer.get().toInt() and 0xFF
            val hi = inputBuffer.get().toInt()
            val sample = ((hi shl 8) or lo).toShort().toInt()
            val gain = nextGain()
            val normalized = (sample / 32768f) * gain
            val shaped = softLimit(normalized)
            val out = (shaped * 32767f).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            output.put((out and 0xFF).toByte())
            output.put(((out shr 8) and 0xFF).toByte())
        }
        // 奇数剩余字节直接透传
        while (inputBuffer.hasRemaining()) {
            output.put(inputBuffer.get())
        }
        output.flip()
    }

    override fun onFlush() {
        currentGain = targetGain
        rampSamplesRemaining = 0
    }

    override fun onReset() {
        targetGain = 1f
        currentGain = 1f
        rampSamplesRemaining = 0
    }

    /**
     * 线性插值过渡：每个采样将 currentGain 向 targetGain 推进
     */
    private fun nextGain(): Float {
        val remaining = rampSamplesRemaining
        if (remaining <= 0) {
            currentGain = targetGain
            return currentGain
        }
        currentGain += (targetGain - currentGain) / remaining
        rampSamplesRemaining = remaining - 1
        return currentGain
    }

    /**
     * tanh 软限幅：0.92 以下完全透明，0.92~1.0 用 tanh 曲线平滑压缩
     */
    private fun softLimit(value: Float): Float {
        val absValue = abs(value)
        if (absValue <= SOFT_LIMIT_THRESHOLD) return value
        val sign = if (value < 0f) -1f else 1f
        val excess = absValue - SOFT_LIMIT_THRESHOLD
        val limited = SOFT_LIMIT_THRESHOLD +
            (SOFT_LIMIT_RANGE * tanh((excess / SOFT_LIMIT_RANGE).toDouble()).toFloat())
        return sign * limited.coerceAtMost(1f)
    }

    companion object {
        private const val MIN_GAIN = 0.35f
        private const val MAX_GAIN = 2.4f
        private const val RAMP_SAMPLES = 1_600
        private const val SOFT_LIMIT_THRESHOLD = 0.92f
        private const val SOFT_LIMIT_RANGE = 0.08f
    }
}
