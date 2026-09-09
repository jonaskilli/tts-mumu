package com.github.jing332.tts

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import com.github.jing332.common.audio.AudioDecoder
import com.github.jing332.common.audio.AudioPlayer
import com.github.jing332.common.audio.exo.ReverbAudioProcessor
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.tts.loudness.SpeakerLoudnessManager
import com.github.jing332.tts.speech.EngineState
import com.github.jing332.tts.speech.plugin.engine.JsBridgeInputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/** 试听会话状态：合成中(…) / 已出声播放中(■) / 空闲(▶)；与角色管理v9/v10按钮时机对齐 */
enum class PreviewState { IDLE, SYNTHESIZING, PLAYING }

/**
 * Silent preview session for plugin-owned UI (for example role management).
 *
 * It deliberately does not use MixSynthesizer: previews must not trigger BGM, speech-rule
 * processing, service logs, or normal reading queues. It does share the exact final resolver,
 * provider routing, decoding, loudness gain, and local parameter application.
 */
object TaggedTtsPreviewPlayer {
    private const val PREVIEW_TIMEOUT_MS = 30_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private var job: Job? = null
    private var player: AudioPlayer? = null

    // 会话计数守卫：被新试听顶替时旧 job 的 finally 不得清掉新会话的 state
    private var focusSession = 0

    // 本次会话是否已真正出声(合成完毕进入播放)；JS 用它把按钮从…切到■,对齐v9时机
    @Volatile
    private var audible: Boolean = false

    // Compose 侧可观察状态：日志面板等 UI 直接收集渲染 ▶/…/■，与 JS 轮询 isPlaying/isAudible 同源
    private val _state = MutableStateFlow(PreviewState.IDLE)
    val state: StateFlow<PreviewState> = _state

    private fun toast(context: Context, msg: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(
                context.applicationContext, msg, android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun play(context: Context, entity: SystemTtsV2, text: String) {
        synchronized(lock) {
            job?.cancel()
            player?.stop()
            audible = false
            _state.value = PreviewState.SYNTHESIZING
            val audioPlayer = AudioPlayer(context.applicationContext)
            player?.release()
            player = audioPlayer
            focusSession++
            val session = focusSession
            job = scope.launch {
                try {
                    val resolved = resolveTtsPlayback(entity, TtsPreviewConfig.globalAudioParamsProvider())
                    if (resolved == null) {
                        toast(context, "试听失败：配置项解析失败")
                        return@launch
                    }
                    val provider = CachedEngineManager.getEngine(context.applicationContext, resolved.configuration.source)
                    if (provider == null) {
                        toast(context, "试听失败：目标插件未启用或不存在")
                        return@launch
                    }
                    if (provider.state != EngineState.Initialized) provider.onInit()

                    // Local direct-play engines already apply their final parameters themselves.
                    if (provider.isSyncPlay(resolved.configuration.source)) {
                        audible = true
                        _state.value = PreviewState.PLAYING
                        provider.syncPlay(
                            resolved.providerParams(text, PREVIEW_TIMEOUT_MS),
                            resolved.configuration.source,
                        )
                        return@launch
                    }

                    val stream = withTimeout(PREVIEW_TIMEOUT_MS) {
                        provider.getStream(
                            resolved.providerParams(text, PREVIEW_TIMEOUT_MS),
                            resolved.configuration.source,
                        )
                    }
                    val bridgeFormat = (stream as? JsBridgeInputStream)?.streamFormat
                    val bytes = stream.readBytes()
                    if (bytes.isEmpty()) {
                        toast(context, "试听失败：合成返回空音频")
                        return@launch
                    }

                    val declaredPcm = bridgeFormat?.encoding?.startsWith("pcm", ignoreCase = true) == true
                    val local = resolved.localPlaybackParams
                    val loudnessGain = SpeakerLoudnessManager.infoFor(resolved.configuration).gain
                    val localVolume = (local.volume * loudnessGain).coerceIn(0f, 1f)

                    val reverbOn = resolved.configuration.audioParams.reverbEnabled
                    if (resolved.configuration.shouldDecode() && !declaredPcm) {
                        audible = true
                        _state.value = PreviewState.PLAYING
                        if (reverbOn) {
                            // 试听混响（09-10）：编码音频先解码为 PCM16，再过与正式朗读链同一个
                            // ReverbAudioProcessor，保证试听与实际播放音色一致
                            val decodeRate = AudioDecoder.getSampleRateAndMime(bytes).first
                                .takeIf { it > 0 }
                                ?: resolved.configuration.audioFormat.sampleRate
                            val pcm = decodeToPcm(bytes, decodeRate)
                            audioPlayer.play(
                                applyReverbToPcm(pcm, decodeRate),
                                decodeRate, local.speed, localVolume, local.pitch
                            )
                        } else {
                            audioPlayer.play(bytes, local.speed, localVolume, local.pitch)
                        }
                    } else {
                        val sampleRate = if (declaredPcm) {
                            bridgeFormat!!.sampleRate
                        } else {
                            AudioDecoder.getSampleRateAndMime(bytes).first
                                .takeIf { it > 0 }
                                ?: resolved.configuration.audioFormat.sampleRate
                        }
                        audible = true
                        _state.value = PreviewState.PLAYING
                        val out =
                            if (reverbOn && declaredPcm) applyReverbToPcm(bytes, sampleRate) else bytes
                        audioPlayer.play(out, sampleRate, local.speed, localVolume, local.pitch)
                    }
                } catch (_: CancellationException) {
                    // Replacing/stopping a preview is normal.
                } catch (e: Exception) {
                    // A newer preview may have released this session's player mid-write
                    // (AudioTrack.write is blocking and cannot honour cancellation): the
                    // replacement then owns `player` and owns playback from here on — stay
                    // silent. Report only when this session's player is still the active one,
                    // so real synthesis errors are not swallowed.
                    val stillOwnsPlayer = synchronized(lock) { player === audioPlayer }
                    if (stillOwnsPlayer) {
                        toast(context, "试听失败：${e.message ?: e.javaClass.simpleName}")
                    }
                } finally {
                    // 被新试听顶替时 session 不匹配，由新会话接管，不得清掉新会话的 state
                    if (focusSession == session) {
                        _state.value = PreviewState.IDLE
                    }
                }
            }
        }
    }

    /** 任意格式音频解码为 PCM16 字节（混响前处理用）；解不出则抛异常由外层 catch 提示 */
    @OptIn(UnstableApi::class)
    private suspend fun decodeToPcm(data: ByteArray, sampleRate: Int): ByteArray {
        val out = ByteArrayOutputStream(data.size)
        AudioDecoder().doDecode(data, sampleRate) { pcm -> out.write(pcm) }
        return out.toByteArray()
    }

    /**
     * 试听混响（09-10）：复用正式朗读链的 ReverbAudioProcessor（Exo AudioProcessor），
     * 手动驱动其 queueInput 对整段 PCM16 处理，保证试听与实际播放音色一致。
     * 按单声道处理（与试听链既有 WAV 封装/AudioTrack 假设一致）；混响尾音在输入末尾截断，预览可接受。
     */
    @OptIn(UnstableApi::class)
    private fun applyReverbToPcm(pcm: ByteArray, sampleRate: Int): ByteArray {
        return try {
            val processor = ReverbAudioProcessor()
            processor.configure(AudioProcessor.AudioFormat(sampleRate, 1, C.ENCODING_PCM_16BIT))
            val out = ByteArrayOutputStream(pcm.size)
            val input = ByteBuffer.wrap(pcm)
            val chunkSize = 4096
            while (input.hasRemaining()) {
                val size = minOf(chunkSize, input.remaining())
                val chunk = ByteArray(size)
                input.get(chunk)
                processor.queueInput(ByteBuffer.wrap(chunk))
                drainProcessorOutput(processor, out)
            }
            processor.queueEndOfStream()
            drainProcessorOutput(processor, out)
            out.toByteArray()
        } catch (_: AudioProcessor.UnhandledAudioFormatException) {
            pcm
        }
    }

    private fun drainProcessorOutput(processor: AudioProcessor, out: ByteArrayOutputStream) {
        val buffer = processor.output
        if (buffer.hasRemaining()) {
            val arr = ByteArray(buffer.remaining())
            buffer.get(arr)
            out.write(arr)
        }
    }

    fun stop() {
        synchronized(lock) {
            job?.cancel()
            job = null
            audible = false
            _state.value = PreviewState.IDLE
            player?.stop()
            player?.release()
            player = null
            focusSession++
        }
    }

    /** 播放会话是否仍存活(合成中或播放中)；供 JS 侧轮询以在播完后复位按钮。 */
    fun isPlaying(): Boolean = synchronized(lock) { job?.isActive == true }

    /** 本次会话是否已真正出声(合成完毕进入播放)；供 JS 把按钮从…切到■,对齐v9时机。 */
    fun isAudible(): Boolean = synchronized(lock) { audible }

    /**
     * 阻塞等待到真正出声(供JS后台线程同步调用,v9式单线程模型)。
     * 会话在出声前死亡(合成失败/被停止)→返回false;超时→返回当前状态。
     */
    fun awaitAudible(timeoutMs: Long): Boolean {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (!isPlaying()) return isAudible()
            if (isAudible()) return true
            Thread.sleep(100)
        }
        return isAudible()
    }

    /** 阻塞等待会话结束(播完/失败/被停止)；超时返回false。 */
    fun awaitDone(timeoutMs: Long): Boolean {
        val start = System.currentTimeMillis()
        while (isPlaying()) {
            if (System.currentTimeMillis() - start >= timeoutMs) return false
            Thread.sleep(100)
        }
        return true
    }
}
