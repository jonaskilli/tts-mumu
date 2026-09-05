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
import com.github.jing332.common.audio.exo.DownmixAudioProcessor
import com.github.jing332.common.audio.exo.ExoAudioDecoder
import com.github.jing332.common.audio.exo.LoudnessAudioProcessor
import com.github.jing332.common.audio.exo.ReverbAudioProcessor
import com.github.jing332.common.audio.exo.SampleRateResampleProcessor
import com.github.jing332.common.utils.rootCause
import com.github.jing332.tts.error.StreamProcessorError
import com.github.jing332.tts.error.StreamProcessorError.AudioDecoding
import com.github.jing332.tts.speech.plugin.engine.JsBridgeInputStream
import com.github.jing332.tts.speech.plugin.parameterRoute
import com.github.jing332.database.entities.systts.source.PluginTtsSource
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
        bridgePcmFormat: JsBridgeInputStream.StreamFormat?,
        onFormatDetected: (sampleRate: Int, channelCount: Int) -> Unit = { _, _ -> },
        onRead: (ByteBuffer) -> Unit,
    ) {

        // 插件经 getAudioV2 桥 callback.streamStart 声明的裸 PCM 流(MiMo/硅基 CosyVoice2 等):
        // 字节无容器头,与配置的 isNeedDecode 声明可能矛盾(此类插件常误标 true,送去
        // extractor 嗅探会报 UnrecognizedInputFormatException)。有 PCM 声明时强制直通,
        // 并用声明格式回调纠正采样率/声道(jread 导入的占位值一并纠正)。
        val declaredPcm =
            bridgePcmFormat != null &&
                    bridgePcmFormat.encoding?.startsWith("pcm", ignoreCase = true) == true
        if (declaredPcm) {
            onFormatDetected(bridgePcmFormat!!.sampleRate, bridgePcmFormat.channels)
        }

        if (tts.shouldDecode() && !declaredPcm) {
            mDecoder.onAudioFormatDetected = onFormatDetected
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
        requestCostMs: Long,
    ): Result<Unit, StreamProcessorError> {
        val config = request.config
        logger.debug {
            "req=${request.text}, sampleRate=${config.audioFormat.sampleRate}, targetSampleRate=${targetSampleRate}"
        }

        try {
            // 桥接流的 PCM 格式声明必须在 getAudioStream 包装/读尽前取出(非流式模式会重建流)
            val bridgePcmFormat = (ins as? JsBridgeInputStream)?.streamFormat
            val stream = getAudioStream(ins, request, requestCostMs).onFailure { return Err(it) }.value

            // 响度均衡：始终开启，计算当前发音人的增益
            val loudnessInfo = SpeakerLoudnessManager.infoFor(config)

            val pluginSource = config.source as? PluginTtsSource
            val route = parameterRoute(
                pluginId = pluginSource?.pluginId.orEmpty(),
                legacySpeed = config.pluginHandlesSpeed,
                legacyVolume = config.pluginHandlesVolume,
                legacyPitch = config.pluginHandlesPitch,
            )
            // 已知插件按内置能力路由；旧版未收录插件沿用已保存标志；未知默认本机。
            val effectiveSpeed = if (route.pluginSpeed || config.audioParams.speed <= 0f) 1f else config.audioParams.speed
            val effectiveVolume = if (route.pluginVolume || config.audioParams.volume <= 0f) 1f else config.audioParams.volume
            val effectivePitch = if (route.pluginPitch || config.audioParams.pitch <= 0f) 1f else config.audioParams.pitch
            val needsLoudness = loudnessInfo.gain != 1f

            // 真实输入采样率：优先用解码器从音频头探测的值（mp3/wav 等自描述格式）。
            // 配置采样率对多采样率音源（如本地音效，各音频文件速率不同）和占位导入值（jread 写 16000）天然不准，
            // 统一以音频头为准；裸 PCM 无头可读，回退配置值（此时配置值是唯一事实）
            var inputSampleRate = config.audioFormat.sampleRate
            // 真实声道数：音频头探测（立体声音源按单声道消费时长会翻倍，表现为慢速）
            var inputChannelCount = 1

            // 管线推迟到拿到真实输入采样率后再装配（解码路径在首段 PCM 输出前就会回调真实格式）
            var processor: AudioProcessingPipeline? = null
            fun ensurePipeline() {
                if (processor != null) return
                val needsSonic = effectiveSpeed != 1f || effectiveVolume != 1f || effectivePitch != 1f
                // 注意：Sonic 的 rate 是"变速变调"参数（时长与音调一起变），不能拿来当重采样器——
                // 采样率转换交给末尾的线性插值重采样器（时长音调不变），否则语速/音调错误
                // 多声道音源先降混为单声道（向系统声明的是单声道），其余处理器只处理单声道
                val pipelines = listOf(
                    if (inputChannelCount > 1) DownmixAudioProcessor() else null,
                    if (context.cfg.silenceSkipEnabled()) skipAudioProcessor else null,
                    if (needsSonic) sonicAudioProcessor else null,
                    if (needsLoudness) loudnessAudioProcessor else null,
                    if (config.audioParams.reverbEnabled) ReverbAudioProcessor() else null,
                    if (inputSampleRate != targetSampleRate)
                        SampleRateResampleProcessor(inputSampleRate, targetSampleRate) else null,
                ).filterNotNull()
                if (pipelines.isEmpty()) return

                val p = AudioProcessingPipeline(ImmutableList.copyOf(pipelines))
                p.configure(
                    AudioProcessor.AudioFormat(
                        inputSampleRate,
                        inputChannelCount,
                        C.ENCODING_PCM_16BIT
                    )
                )

                if (needsSonic) {
                    sonicAudioProcessor.apply {
                        speed = effectiveSpeed
                        volume = effectiveVolume
                        pitch = effectivePitch
                    }
                }

                if (needsLoudness) {
                    loudnessAudioProcessor.setGain(loudnessInfo.gain)
                }

                p.flush()
                processor = p
            }


            // 响度学习：收集最终输出 PCM 用于分析
            val needsLoudnessAnalysis = SpeakerLoudnessManager.needsAnalysis(config)
            val loudnessPcmCollector = if (needsLoudnessAnalysis) {
                java.io.ByteArrayOutputStream()
            } else null

            fun handle(pcm: ByteBuffer?) {
                ensurePipeline()
                val p = processor
                if (p == null) {
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
                    p.queueEndOfStream()
                    val out = p.output
                    if (loudnessPcmCollector != null && out.hasRemaining()) {
                        val dup = out.duplicate()
                        while (dup.hasRemaining()) loudnessPcmCollector.write(dup.get().toInt())
                    }
                    callback.receive(out)
                } else {
                    while (pcm.hasRemaining()) {
                        p.queueInput(pcm)
                        val out = p.output
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
                    bridgePcmFormat = bridgePcmFormat,
                    onFormatDetected = { detected, channels ->
                        // 解码器从音频头解析出的真实格式（首段 PCM 前回调）：多采样率音源（本地音效等）
                        // 各音频文件速率/声道不同，固定配置值天然不准，统一以音频头为准
                        logger.debug {
                            "audio format detected: rate=$detected ch=$channels (config rate=${config.audioFormat.sampleRate})"
                        }
                        inputSampleRate = detected
                        inputChannelCount = channels
                    },
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
        requestCostMs: Long = 0,
    ): Result<InputStream, StreamProcessorError> = if (context.cfg.streamPlayEnabled()) {
        context.event?.dispatch(NormalEvent.HandleStream(request))
        Ok(ins)
    } else {
        try {
            ins.use {
                val bytes: ByteArray
                val cost = measureTimeMillis { bytes = it.readBytes() }
                // 请求段(插件内部下载) + 读流段(body下载) 相加：
                // ByteArray型插件前段为主、流式插件后段为主，两种形态都得到完整真实耗时
                context.event?.dispatch(
                    NormalEvent.ReadAllFromStream(request, bytes.size, requestCostMs + cost)
                )

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
