package com.github.jing332.tts

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessingPipeline
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import com.github.jing332.common.audio.AudioDecoder.Companion.readPcmChunk
import com.github.jing332.common.audio.exo.ExoAudioDecoder
import com.github.jing332.common.audio.exo.LoudnessAudioProcessor
import com.github.jing332.common.utils.rootCause
import com.github.jing332.tts.error.StreamProcessorError
import com.github.jing332.tts.error.StreamProcessorError.AudioDecoding
import com.github.jing332.tts.error.StreamProcessorError.HandleError
import com.github.jing332.tts.loudness.SpeakerLoudnessManager
import com.github.jing332.tts.synthesizer.IResultProcessor
import com.github.jing332.tts.synthesizer.PcmAudioDataListener
import com.github.jing332.tts.synthesizer.RequestPayload
import com.github.jing332.tts.synthesizer.TtsConfiguration
import com.github.jing332.tts.synthesizer.event.NormalEvent
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onFailure
import com.google.common.collect.ImmutableList
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.jvm.Throws
import kotlin.system.measureTimeMillis

internal class DefaultResultProcessor(
    private val context: SynthesizerContext,
) : IResultProcessor {
    companion object {
        val logger = KotlinLogging.logger("DefaultResultProcessor")
    }

    private var _decoder: ExoAudioDecoder? = null
    private val mDecoder: ExoAudioDecoder
        get() = _decoder ?: throw IllegalStateException("decoder not init")

    override suspend fun init(context: Context) {
        _decoder = ExoAudioDecoder(context)
    }

    @Throws(ExoPlaybackException::class)
    private suspend fun decode(
        ins: InputStream,
        tts: TtsConfiguration,
        onRead: (ByteBuffer) -> Unit,
    ) {

        if (tts.shouldDecode()) {
            mDecoder.callback = ExoAudioDecoder.Callback { byteBuffer ->
                onRead(byteBuffer)
            }

            if (context.cfg.streamPlayEnabled())
                mDecoder.doDecode(ins)
            else
                ins.use {
                    mDecoder.doDecode(ins.readBytes())
                }


        } else {
            ins.readPcmChunk { onRead(ByteBuffer.wrap(it)) }
        }

    }

    @OptIn(UnstableApi::class)
    private fun silenceSkippingAudioProcessor(
    ): SilenceSkippingAudioProcessor {
        val p = SilenceSkippingAudioProcessor(
            SilenceSkippingAudioProcessor.DEFAULT_MINIMUM_SILENCE_DURATION_US,
            SilenceSkippingAudioProcessor.DEFAULT_SILENCE_RETENTION_RATIO,
            SilenceSkippingAudioProcessor.DEFAULT_MAX_SILENCE_TO_KEEP_DURATION_US,
            SilenceSkippingAudioProcessor.DEFAULT_MIN_VOLUME_TO_KEEP_PERCENTAGE,
            SilenceSkippingAudioProcessor.DEFAULT_SILENCE_THRESHOLD_LEVEL
        )
        p.setEnabled(true)
        return p
    }

    private val sonicAudioProcessor by lazy { com.github.jing332.common.audio.exo.SonicAudioProcessor() }
    private val skipAudioProcessor by lazy { silenceSkippingAudioProcessor() }
    private val loudnessAudioProcessor by lazy { LoudnessAudioProcessor() }

    /**
     * @throw [CancellationException]
     */
    @OptIn(UnstableApi::class)
    override suspend fun processStream(
        ins: InputStream,
        request: RequestPayload,
        targetSampleRate: Int,
        callback: PcmAudioDataListener,
    ): Result<Unit, StreamProcessorError> {
        val config = request.config
        logger.debug {
            "req=${request.text}, sampleRate=${config.audioFormat.sampleRate}, targetSampleRate=${targetSampleRate}"
        }

        try {
            val stream = getAudioStream(ins, request).onFailure { return Err(it) }.value

            // 响度均衡：始终开启，计算当前发音人的增益
            val loudnessInfo = SpeakerLoudnessManager.infoFor(config)

            // 计算实际生效的音频参数，判断是否需要 Sonic 和 Loudness 处理
            val effectiveSpeed = if (config.audioParams.speed <= 0f) 1f else config.audioParams.speed
            val effectiveVolume = if (config.audioParams.volume <= 0f) 1f else config.audioParams.volume
            val effectivePitch = if (config.audioParams.pitch <= 0f) 1f else config.audioParams.pitch
            val needsResample = config.audioFormat.sampleRate != targetSampleRate
            val needsSonic = effectiveSpeed != 1f || effectiveVolume != 1f || effectivePitch != 1f || needsResample
            val needsLoudness = loudnessInfo.gain != 1f

            val pipelines = listOf(
                if (context.cfg.silenceSkipEnabled()) skipAudioProcessor else null,
                if (needsSonic) sonicAudioProcessor else null,
                if (needsLoudness) loudnessAudioProcessor else null,
            ).filterNotNull()
            val processor = AudioProcessingPipeline(ImmutableList.copyOf(pipelines))

            if (pipelines.isNotEmpty()) {
                processor.configure(
                    AudioProcessor.AudioFormat(
                        config.audioFormat.sampleRate,
                        1,
                        C.ENCODING_PCM_16BIT
                    )
                )

                if (needsSonic) {
                    sonicAudioProcessor.apply {
                        speed = effectiveSpeed
                        volume = effectiveVolume
                        pitch = effectivePitch
                        rate = config.audioFormat.sampleRate.toFloat() / targetSampleRate.toFloat()
                    }
                }

                if (needsLoudness) {
                    loudnessAudioProcessor.setGain(loudnessInfo.gain)
                }

                processor.flush()
            }


            // 响度学习：收集最终输出 PCM 用于分析
            val needsLoudnessAnalysis = SpeakerLoudnessManager.needsAnalysis(config)
            val loudnessPcmCollector = if (needsLoudnessAnalysis) {
                java.io.ByteArrayOutputStream()
            } else null

            fun handle(pcm: ByteBuffer?) {
                if (pipelines.isEmpty()) {
                    if (pcm != null) {
                        if (loudnessPcmCollector != null) {
                            val dup = pcm.duplicate()
                            while (dup.hasRemaining()) loudnessPcmCollector.write(dup.get().toInt())
                        }
                        callback.receive(pcm)
                    }
                    return
                }

                if (pcm == null) {
                    processor.queueEndOfStream()
                    val out = processor.output
                    if (loudnessPcmCollector != null && out.hasRemaining()) {
                        val dup = out.duplicate()
                        while (dup.hasRemaining()) loudnessPcmCollector.write(dup.get().toInt())
                    }
                    callback.receive(out)
                } else {
                    while (pcm.hasRemaining()) {
                        processor.queueInput(pcm)
                        val out = processor.output
                        if (loudnessPcmCollector != null && out.hasRemaining()) {
                            val dup = out.duplicate()
                            while (dup.hasRemaining()) loudnessPcmCollector.write(dup.get().toInt())
                        }
                        callback.receive(out)
                    }
                }
            }

            try {
                decode(
                    ins = stream,
                    tts = config,
                    onRead = { pcm -> handle(pcm = pcm) })
                handle(null)
            } catch (e: ExoPlaybackException) {
                logger.error(e) { "streaming error" }
                return if (e.type == ExoPlaybackException.TYPE_SOURCE)
                    Err(StreamProcessorError.AudioSource(e.rootCause))
                else
                    Err(StreamProcessorError.AudioDecoding(e))
            }

            // 异步分析最终输出 PCM 的响度
            if (loudnessPcmCollector != null && loudnessPcmCollector.size() > 0) {
                val pcmBytes = loudnessPcmCollector.toByteArray()
                SpeakerLoudnessManager.analyzePcmAndRecord(pcmBytes, config)
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Err(StreamProcessorError.HandleError(e))
        }

        return Ok(Unit)
    }

    private suspend fun getAudioStream(
        ins: InputStream,
        request: RequestPayload,
    ): Result<InputStream, StreamProcessorError> = if (context.cfg.streamPlayEnabled()) {
        context.event?.dispatch(NormalEvent.HandleStream(request))
        Ok(ins)
    } else {
        try {
            ins.use {
                val bytes: ByteArray
                val cost = measureTimeMillis { bytes = it.readBytes() }
                context.event?.dispatch(NormalEvent.ReadAllFromStream(request, bytes.size, cost))

                Ok(ByteArrayInputStream(bytes))
            }
        } catch (e: Exception) {
            logger.error(e) { "readBytes error" }
            Err(StreamProcessorError.AudioSource(e.cause ?: e))
        }
    }

    override suspend fun destroy() {
        _decoder?.destroy()
        _decoder = null
    }
}
