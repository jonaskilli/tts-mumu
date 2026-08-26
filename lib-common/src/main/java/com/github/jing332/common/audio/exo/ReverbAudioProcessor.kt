package com.github.jing332.common.audio.exo

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.tanh

@OptIn(UnstableApi::class)
class ReverbAudioProcessor : BaseAudioProcessor() {

    private var channelStates: Array<ReverbChannelState> = emptyArray()
    private var wetRampTotal = 1
    private var wetRampRemaining = 1

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        val sampleRate = inputAudioFormat.sampleRate
        channelStates = Array(inputAudioFormat.channelCount) { ch ->
            ReverbChannelState(
                sampleRate,
                ch,
                inputAudioFormat.channelCount,
            )
        }
        wetRampTotal = (WET_RAMP_SECONDS * sampleRate).toInt().coerceAtLeast(1)
        wetRampRemaining = wetRampTotal
        return inputAudioFormat
    }

    override fun isActive(): Boolean = true

    override fun queueInput(inputBuffer: ByteBuffer) {
        val channels = channelStates.size.coerceAtLeast(1)
        val bytesPerFrame = 2 * channels
        val frameCount = inputBuffer.remaining() / bytesPerFrame
        val output = replaceOutputBuffer(inputBuffer.remaining())
        repeat(frameCount) {
            val mix = nextWetMix()
            repeat(channels) { ch ->
                val lo = inputBuffer.get().toInt() and 0xFF
                val hi = inputBuffer.get().toInt()
                val dry = ((hi shl 8) or lo).toShort().toInt() / 32768f
                val wet = channelStates[ch].process(dry)
                val shaped = softLimit(dry + wet * mix)
                val out = (shaped * 32767f).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                output.put((out and 0xFF).toByte())
                output.put(((out shr 8) and 0xFF).toByte())
            }
        }
        while (inputBuffer.hasRemaining()) {
            output.put(inputBuffer.get())
        }
        output.flip()
    }

    override fun onFlush() {
        wetRampRemaining = wetRampTotal
        channelStates.forEach { it.reset() }
    }

    override fun onReset() {
        channelStates = emptyArray()
        wetRampTotal = 1
        wetRampRemaining = 1
    }

    private fun nextWetMix(): Float {
        val remaining = wetRampRemaining
        if (remaining <= 0) return WET_MIX
        wetRampRemaining = remaining - 1
        return WET_MIX * (1f - remaining.toFloat() / wetRampTotal)
    }

    private fun softLimit(value: Float): Float {
        val absValue = abs(value)
        if (absValue <= LIMITER_KNEE) return value
        val sign = if (value < 0f) -1f else 1f
        val excess = absValue - LIMITER_KNEE
        val range = 1f - LIMITER_KNEE
        val limited =
            LIMITER_KNEE + range * tanh((excess / range).toDouble()).toFloat()
        return sign * limited.coerceAtMost(1f)
    }

    private class ReverbChannelState(
        sampleRate: Int,
        channelIndex: Int,
        channelCount: Int,
    ) {
        private val spreadMs = if (channelCount > 1) CHANNEL_DELAY_SPREAD_MS * channelIndex else 0.0
        private val preDelay = DelayLine(msToSamples(PRE_DELAY_MS + spreadMs, sampleRate))
        private val combs =
            COMB_DELAY_MS.map { CombFilter(sampleRate, msToSamples(it + spreadMs, sampleRate)) }
        private val allPasses =
            ALL_PASS_DELAY_MS.map { AllPassFilter(sampleRate, msToSamples(it + spreadMs, sampleRate)) }
        private val wetLowPass = OnePoleLowPass(sampleRate, WET_LOW_PASS_HZ)

        fun process(input: Float): Float {
            val delayed = preDelay.process(input)
            var combSum = 0f
            for (c in combs) combSum += c.process(delayed)
            combSum /= combs.size
            var apOut = combSum
            for (ap in allPasses) apOut = ap.process(apOut)
            return wetLowPass.process(apOut)
        }

        fun reset() {
            preDelay.reset()
            combs.forEach { it.reset() }
            allPasses.forEach { it.reset() }
            wetLowPass.reset()
        }
    }

    private class DelayLine(sizeSamples: Int) {
        private val buffer = FloatArray(sizeSamples)
        private var index = 0

        fun process(input: Float): Float {
            val out = buffer[index]
            buffer[index] = input
            index++
            if (index >= buffer.size) index = 0
            return out
        }

        fun reset() {
            java.util.Arrays.fill(buffer, 0f)
            index = 0
        }
    }

    private class CombFilter(
        sampleRate: Int,
        sizeSamples: Int,
    ) {
        private val buffer = FloatArray(sizeSamples)
        private var index = 0
        private var filterStore = 0f

        private val cycleSeconds = sizeSamples.toDouble() / sampleRate
        private val feedback = exp(LN_NEG60DB * cycleSeconds / DECAY_SECONDS).toFloat()
        private val damp1 = COMB_DAMPING.toFloat()
        private val damp2 = (1.0 - COMB_DAMPING).toFloat()

        fun process(input: Float): Float {
            val output = buffer[index]
            filterStore = output * damp2 + filterStore * damp1
            buffer[index] = input + filterStore * feedback
            index++
            if (index >= buffer.size) index = 0
            return output
        }

        fun reset() {
            java.util.Arrays.fill(buffer, 0f)
            filterStore = 0f
            index = 0
        }
    }

    private class AllPassFilter(
        sampleRate: Int,
        sizeSamples: Int,
    ) {
        private val buffer = FloatArray(sizeSamples)
        private var index = 0
        private val fb = ALL_PASS_FEEDBACK.toFloat()

        fun process(input: Float): Float {
            val bufOut = buffer[index]
            val output = -input + bufOut
            buffer[index] = input + bufOut * fb
            index++
            if (index >= buffer.size) index = 0
            return output
        }

        fun reset() {
            java.util.Arrays.fill(buffer, 0f)
            index = 0
        }
    }

    private class OnePoleLowPass(
        sampleRate: Int,
        cutoffHz: Double,
    ) {
        private val coefficient = exp(-2.0 * PI * cutoffHz / sampleRate).toFloat()
        private var state = 0f

        fun process(input: Float): Float {
            state = input + coefficient * (state - input)
            return state
        }

        fun reset() {
            state = 0f
        }
    }

    companion object {
        private const val WET_MIX = 0.14f
        private const val WET_RAMP_SECONDS = 0.02
        private const val PRE_DELAY_MS = 28.0
        private const val DECAY_SECONDS = 0.42
        private val COMB_DELAY_MS = doubleArrayOf(29.7, 34.9, 39.1, 43.7)
        private val ALL_PASS_DELAY_MS = doubleArrayOf(5.0, 1.7)
        private const val COMB_DAMPING = 0.48
        private const val ALL_PASS_FEEDBACK = 0.50
        private const val WET_LOW_PASS_HZ = 3600.0
        private const val LIMITER_KNEE = 0.95f
        private const val CHANNEL_DELAY_SPREAD_MS = 13.0
        private const val LN_NEG60DB = -6.907755278982137

        private fun msToSamples(ms: Double, sampleRate: Int): Int =
            (ms * sampleRate / 1000.0).toInt().coerceAtLeast(1)
    }
}
