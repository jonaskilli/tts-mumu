package com.github.jing332.tts.synthesizer

import androidx.annotation.MainThread
import android.os.SystemClock
import com.drake.net.utils.withMain
import com.github.jing332.common.utils.StringUtils
import com.github.jing332.common.utils.toByteArray
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts.CachedEngineManager
import com.github.jing332.tts.SynthesizerContext
import com.github.jing332.tts.speech.EngineState
import com.github.jing332.tts.error.RequesterError
import com.github.jing332.tts.error.SynthesisError
import com.github.jing332.tts.error.TextProcessorError
import com.github.jing332.tts.speech.EmptyInputStream
import com.github.jing332.tts.synthesizer.event.ErrorEvent
import com.github.jing332.tts.synthesizer.event.Event
import com.github.jing332.tts.synthesizer.event.NormalEvent
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import io.github.oshai.kotlinlogging.KLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.coroutineScope

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.InputStream

abstract class AbstractMixSynthesizer() : Synthesizer {
    companion object {
        // 32→128：缓冲按块计，128块×2048B≈256KB，24kHz下约5.5秒音频，
        // 高倍速听书（1-3x）时盖插件网络等待，消除段间接缝；停止时通道直接丢弃，不影响停止响应
        const val PROCUDE_CAPACITY: Int = 128

        /**
         * 生成静音 PCM 音频数据（不含 WAV 头）
         *
         * 注意：TTS 回调已通过 onSynthesizeStart(sampleRate) 声明了音频格式，
         * 因此 onSynthesizeAvailable 应发送裸 PCM 数据，不能带 WAV 头，
         * 否则 44 字节的 RIFF 头会被当成音频数据播放产生噪音。
         *
         * @param sampleRate 采样率，必须与 onSynthesizeStart 声明的一致
         * @param durationMs 持续时间，默认 100ms
         * @return PCM 格式的字节数组（16bit 单声道）
         */
        fun createSilentPcmAudio(sampleRate: Int = 16000, durationMs: Int = 100): ByteArray {
            val numChannels = 1
            val bitsPerSample = 16
            val numSamples = (sampleRate * durationMs) / 1000
            val dataSize = numSamples * numChannels * (bitsPerSample / 8)
            return ByteArray(dataSize) // 全 0 即静音
        }
    }

    private val logger: KLogger
        get() = context.logger

    // 预取结果：流 + 请求段耗时(用于获取成功日志的耗时合计)
    private data class PrefetchResult(val stream: InputStream?, val costMs: Long)


    abstract val context: SynthesizerContext

    abstract val textProcessor: ITextProcessor
    abstract val ttsRequester: ITtsRequester
    abstract val streamProcessor: IResultProcessor
    abstract val repo: ITtsRepository
    abstract val bgmPlayer: IBgmPlayer

    var isInitialized: Boolean = false
        private set

    private var maxSampleRate: Int = 16000

    // All enabled configs
    private var mConfigs: Map<Long, TtsConfiguration> = mapOf()
        set(value) {
            field = value
            maxSampleRate =
                mConfigs.values.maxByOrNull { it.audioFormat.sampleRate }?.audioFormat?.sampleRate
                    ?: 16000
        }

    private fun event(event: Event) {
        context.event?.dispatch(event)
    }

    /**
     * @return null means presetConfigId is not found from database
     */
    private suspend fun textProcess(
        params: SystemParams,
        presetConfigId: Long?,
    ): Result<List<TextSegment>, SynthesisError> {
        val presetConfig: TtsConfiguration? = presetConfigId?.run { repo.getTts(this) }
        if (presetConfigId != null && presetConfig == null) {
            return Err(SynthesisError.PresetMissing(presetConfigId))
        }
        textProcessor
            .process(params.text, presetConfig)
            .onSuccess { list -> return Ok(list.filterNot { StringUtils.isSilent(it.text) }) }
            .onFailure { err: TextProcessorError ->
                event(ErrorEvent.TextProcessor(err))
                return Err(SynthesisError.TextHandle(err))
            }

        return Ok(emptyList())
    }

    /**
     * @return null means request failed, [EmptyInputStream] means direct play
     *
     */
    private suspend fun requestInternal(
        request: RequestPayload,
        playCallback: suspend (ITtsRequester.ISyncPlayCallback) -> Unit,
    ): InputStream? {
        val result = try {
            withTimeout(context.cfg.requestTimeout()) {
                ttsRequester.request(request.params, request.config)
            }
        } catch (e: TimeoutCancellationException) {
            event(ErrorEvent.RequestTimeout(request))
            return null
        } catch (e: CancellationException) {
            throw e
        }

        result.onSuccess { resp ->
            resp.onCallback {
                playCallback(it)
                return EmptyInputStream
            }.onStream { ins ->
                return ins
            }
        }.onFailure {
            when (it) {
                is RequesterError.RequestError -> {
                    event(ErrorEvent.Request(request, it.error))
                }

                is RequesterError.StateError -> {
                    event(ErrorEvent.Request(request, IllegalStateException(it.message)))
                }
            }

            return null
        }

        return null
    }


    private suspend fun requestAndProcess(
        channel: SendChannel<ChannelPayload>,
        params: SystemParams,
        config: TtsConfiguration,
        retries: Int = 0,
        maxRetries: Int = context.cfg.maxRetryTimes(),
        prefetchedStream: InputStream? = null,
        prefetchedCostMs: Long = 0,
    ) {
        val request = RequestPayload(params, config)
        suspend fun retry() {
            CachedEngineManager.removeEngine(config.source)
            delay(context.cfg.retryDelay())
            val toggleTryValue = context.cfg.toggleTry()
            logger.debug {
                "retry: retries=$retries, toggleTry=$toggleTryValue, maxRetries=$maxRetries, hasStandby=${config.standbyConfig != null}"
            }
            return if (config.standbyConfig != null && toggleTryValue <= retries + 1) {
                val fromTag = config.speechInfo.tag
                val toTag = config.standbyConfig?.speechInfo?.tag ?: ""
                logger.info { "standby triggered: toggleTry=$toggleTryValue, retries=$retries, fromTag=$fromTag, toTag=$toTag" }
                event(NormalEvent.StandbyTts(
                    request.copy(config = config.standbyConfig),
                    fromTag = fromTag,
                    toTag = toTag,
                    reason = "retry",
                ))
                requestAndProcess(channel, params, config.standbyConfig, 0, maxRetries)
            } else {
                val next = retries + 1
                // 重试时在原文末尾追加可配置的字符（次数 = 重试次数），
                // 用于绕过部分插件对相同文本的缓存/去重逻辑，提高重试成功率。
                // 仅对插件合成生效，避免影响其它引擎的输出内容。
                val append = context.cfg.retryAppendText()
                val retryParams = if (config.source is PluginTtsSource && append.isNotEmpty()) {
                    params.copy(text = params.text + append.repeat(next))
                } else {
                    params
                }
                requestAndProcess(channel, retryParams, config, next, maxRetries)
            }
        }

        if (retries > maxRetries) {
            event(NormalEvent.RequestCountEnded)
            // 兜底发音人(duihua/duihuaA/duihuaB)或音效(localSound)失败时记录错误日志
            // 括号1-4、narration 不在此列；记录日志后仍按用户配置的 restartOnMaxRetryMode 处理
            val curTag = config.speechInfo.tag
            val isFallbackOrSound = curTag in listOf("duihua", "duihuaA", "duihuaB")
                || curTag.startsWith("localSound")
            if (isFallbackOrSound) {
                logger.error { "兜底/音效标签失败: tag=$curTag, text=${params.text.take(20)}" }
                event(ErrorEvent.Request(request, IllegalStateException("功能标签($curTag)兜底发音人重试失败")))
            }
            when (context.cfg.restartOnMaxRetryMode()) {
                1 -> { // 不生成空音频直接重启
                    logger.warn { "max retries exceeded, restarting app directly..." }
                    Runtime.getRuntime().exit(0)
                }
                2 -> { // 生成空音频后重启
                    val silentAudio = createSilentPcmAudio(maxSampleRate, durationMs = 100)
                    channel.trySendBlocking(ChannelPayload.Bytes(silentAudio))
                    logger.warn { "max retries exceeded, restarting app after empty audio..." }
                    Runtime.getRuntime().exit(0)
                }
                else -> { // 0 = 关闭，生成空音频但不重启
                    val silentAudio = createSilentPcmAudio(maxSampleRate, durationMs = 100)
                    channel.trySendBlocking(ChannelPayload.Bytes(silentAudio))
                }
            }
            return
        }

        logger.debug { "start request: $retries, ${params}, ${config}" }
        event(NormalEvent.Request(request, retries))

        // 优先使用预取的流，避免重复请求；重试时 prefetchedStream=null 走正常请求路径
        // 请求段耗时单独计量：ByteArray型插件(豆包/千问)的下载发生在 requestInternal 内部，
        // 流式插件的下载发生在后续读流阶段，两段相加才是完整的真实耗时(获取成功日志)
        val reqStart = SystemClock.elapsedRealtime()
        val stream = prefetchedStream ?: requestInternal(request, playCallback = {
                logger.debug { "send direct play callback..." }
                channel.send(ChannelPayload.DirectPlayCallback(request, it))
            })
        val requestCostMs = if (prefetchedStream != null)
            prefetchedCostMs
        else
            SystemClock.elapsedRealtime() - reqStart

        if (stream == null) return retry() // request failed
        else if (stream is EmptyInputStream) return // direct play

        // 流处理（解码/读取）不在 requestInternal 的 withTimeout 范围内，
        // 网络流的 read() 可能无限阻塞导致既不成功也不失败、卡住无法重试。
        // 这里用超时保护，超时后关闭流打断阻塞 IO 并触发重试。
        val streamTimeout = maxOf(context.cfg.requestTimeout() * 3, 30_000L)
        val processResult = try {
            withTimeout(streamTimeout) {
                streamProcessor.processStream(
                    ins = stream,
                    request = request,
                    targetSampleRate = maxSampleRate,
                    callback = { pcm -> channel.trySendBlocking(ChannelPayload.Bytes(pcm.toByteArray())) },
                    requestCostMs = requestCostMs,
                )
            }
        } catch (e: TimeoutCancellationException) {
            runCatching { stream.close() }
            event(ErrorEvent.RequestTimeout(request))
            return retry()
        } catch (e: CancellationException) {
            throw e
        }

        processResult.onFailure { e ->
            event(ErrorEvent.ResultProcessor(request, e))
            return retry()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun executeSynthesis(
        params: SystemParams, callback: SynthesisCallback, presetConfigId: Long?,
    ): Result<Unit, SynthesisError> = coroutineScope {
        if (mConfigs.isEmpty()) return@coroutineScope Err(SynthesisError.ConfigEmpty)

        logger.debug { "onSynthesizeStart: sampleRate=${maxSampleRate}" }

        val channel =
            produce<ChannelPayload>(CoroutineName("Synthesis producer"), PROCUDE_CAPACITY) {
                textProcess(params, presetConfigId)
                    .onSuccess { list ->
                        // 预取：段N处理流时并发请求段N+1，消除段间网络等待
                        var prefetchJob: Deferred<PrefetchResult>? = null

                        for ((index, segment) in list.withIndex()) {
                            val segParams = params.copy(text = segment.text)

                            // 等待预取结果（首段为null，走正常请求路径）
                            val prefetched = prefetchJob?.await()
                            prefetchJob = null

                            if (prefetched?.stream != null) {
                                // 预取成功，直接用预取的流处理
                                requestAndProcess(
                                    channel, segParams, segment.tts,
                                    prefetchedStream = prefetched.stream,
                                    prefetchedCostMs = prefetched.costMs
                                )
                            } else {
                                // 无预取（首段/预取失败），正常请求
                                requestAndProcess(channel, segParams, segment.tts)
                            }

                            // 启动下一段的预取请求（与当前段的处理并行执行）
                            if (index + 1 < list.size) {
                                val nextSeg = list[index + 1]
                                val nextRequest = RequestPayload(params.copy(text = nextSeg.text), nextSeg.tts)
                                prefetchJob = async(Dispatchers.IO) {
                                    val start = SystemClock.elapsedRealtime()
                                    val s = runCatching {
                                        requestInternal(nextRequest, playCallback = {
                                            channel.send(ChannelPayload.DirectPlayCallback(nextRequest, it))
                                        })
                                    }.getOrNull()
                                    PrefetchResult(s, SystemClock.elapsedRealtime() - start)
                                }
                            }

                            // 段间停顿：每段音频之后插入固定时长静音PCM(含批次最后一段)
                            // 直写通道，不经过Sonic变速/静音跳过管线，停顿=设置的固定墙钟时长
                            // 末段也插：阅读App常按句投喂、纯旁白段整段一个角色，批内往往只有
                            // 1段，仅在批内多段间插入会永远不触发；段尾停顿充当批间气口，
                            // 下一批到达时本批被强制收尾(onStop破窗)，停顿自然截断不叠加
                            val pauseMs = context.cfg.segmentPauseMs()
                            if (pauseMs > 0) {
                                logger.info { "segment pause: insert ${pauseMs}ms after segment ${index + 1}/${list.size}" }
                                channel.trySendBlocking(
                                    ChannelPayload.Bytes(
                                        createSilentPcmAudio(maxSampleRate, durationMs = pauseMs)
                                    )
                                )
                            }
                        }
                    }
                    .onFailure {
                        channel.send(ChannelPayload.Error(it))
                    }
            }

        try {
            var isFirst = true
            for (payload in channel) {
                if (isFirst && (payload is ChannelPayload.Bytes || payload is ChannelPayload.DirectPlayCallback)) {
                    callback.onSynthesizeStart(maxSampleRate)
                    isFirst = false
                }

                when (payload) {
                    is ChannelPayload.Bytes -> callback.onSynthesizeAvailable(payload.data)

                    is ChannelPayload.DirectPlayCallback -> try {
                        event(NormalEvent.DirectPlay(payload.request))
                        payload.callback.play()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        event(ErrorEvent.DirectPlay(payload.request, e))
                    } finally {
                        logger.debug { "direct play done" }
                    }

                    is ChannelPayload.Error -> {
                        return@coroutineScope Err(payload.err)
                    }

                    else -> logger.error { "unknown data: $payload" }
                }
            }
        } finally {
            logger.debug { "channel closed" }
        }

        Ok(Unit)
    }

    private val mutex = Mutex()

    override val isSynthesizing: Boolean
        get() = mutex.isLocked


    private var initError: SynthesisError? = null

    override suspend fun synthesize(
        params: SystemParams, forceConfigId: Long?, callback: SynthesisCallback,
    ): Result<Unit, SynthesisError> = mutex.withLock {
        logger.atTrace {
            message = "synthesize"
            payload = mapOf(
                "forceConfigId" to forceConfigId,
                "params" to params,
            )
        }

        initError?.let {
            return@withLock Err(it)
        }

        withMain { bgmPlayer.play() }
        try {
            executeSynthesis(params, callback, forceConfigId)
        } finally {
            logger.debug { "synthesize done" }
            withContext(NonCancellable) {
                withMain { bgmPlayer.stop() }
            }
        }
    }

    override suspend fun init() = mutex.withLock {
        try {
            repo.init()
        } catch (e: Exception) {
            event(ErrorEvent.Repository(e))
            return@withLock
        }

        mConfigs = repo.getAllTts()
        if (mConfigs.isEmpty()) {
            event(ErrorEvent.ConfigEmpty)
            return@withLock
        }

        // 引擎预热：提前初始化所有引擎，避免首请求时同步初始化延迟
        mConfigs.values.map { it.source }.distinctBy { it.getKey() }.forEach { source ->
            try {
                CachedEngineManager.getEngine(context.androidContext, source)?.let { engine ->
                    if (engine.state != EngineState.Initialized) {
                        engine.onInit()
                    }
                }
            } catch (e: Exception) {
                logger.warn { "引擎预热失败: ${source.getKey()}, ${e.message}" }
            }
        }

        textProcessor.init(context.androidContext, mConfigs).onFailure {
            event(ErrorEvent.TextProcessor(it))
            return@withLock
        }

        streamProcessor.init(context.androidContext)

        val bgmList = mutableListOf<BgmSource>()
        try {
            repo.getAllBgm().forEach { bgm ->
                bgm.musicList.forEach {
                    bgmList.add(BgmSource(path = it, volume = bgm.volume))
                }
            }

            withMain {
                bgmPlayer.init()
                bgmPlayer.setPlayList(list = bgmList)
            }
        } catch (e: Exception) {
            event(ErrorEvent.BgmLoading(e))
            return@withLock
        }

        isInitialized = true
    }


    @MainThread
    override suspend fun destroy() = mutex.withLock {
        isInitialized = false
        repo.destroy()
        ttsRequester.destroy()
        streamProcessor.destroy()
        bgmPlayer.destroy()
    }

    sealed interface ChannelPayload {
        data class Bytes(val data: ByteArray) : ChannelPayload
        data class DirectPlayCallback(
            val request: RequestPayload,
            val callback: ITtsRequester.ISyncPlayCallback,
        ) : ChannelPayload

        data class Error(val err: SynthesisError) : ChannelPayload
    }
}