package com.github.jing332.common.audio.exo

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.github.jing332.common.audio.Sonic
import java.nio.ByteBuffer


@OptIn(UnstableApi::class)
class SonicAudioProcessor() : BaseAudioProcessor() {
    private lateinit var sonic: Sonic

    var speed: Float
        get() = sonic.speed
        set(value) {
            sonic.setSpeed(value)
        }

    var volume: Float
        get() = sonic.volume
        set(value) {
            sonic.setVolume(value)
        }

    var pitch: Float
        get() = sonic.pitch
        set(value) {
            sonic.setPitch(value)
        }

    var rate: Float
        get() = sonic.rate
        set(value) {
            sonic.setRate(value)
        }


    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        sonic = Sonic(inputAudioFormat.sampleRate, inputAudioFormat.channelCount)
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val position = inputBuffer.position()
        val limit = inputBuffer.limit()
        val size = limit - position

        // Allocate a new output buffer with the same size as the input buffer.
        val buffer = ByteArray(size)
        // Copy the input buffer to the output buffer.
        for (i in 0 until size) {
            buffer[i] = inputBuffer[position + i] // Corrected index
        }

        sonic.writeBytesToStream(buffer, size)

        val outSize = sonic.samplesAvailable()
        val outputBuffer = replaceOutputBuffer(outSize)
        sonic.readBytesFromStream(outputBuffer, outSize)

        // Update the positions and limits of the input and output buffers.
        inputBuffer.position(inputBuffer.limit())
        outputBuffer.flip()
    }

    override fun onFlush() {
        super.onFlush()
        sonic.flushStream()
    }

    override fun onQueueEndOfStream() {
        super.onQueueEndOfStream()

        sonic.flushStream()
    }
}