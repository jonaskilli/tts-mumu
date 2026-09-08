package com.github.jing332.tts

import android.content.Context
import com.github.jing332.common.audio.AudioDecoder
import com.github.jing332.common.audio.AudioPlayer
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.tts.loudness.SpeakerLoudnessManager
import com.github.jing332.tts.speech.EngineState
import com.github.jing332.tts.speech.plugin.engine.JsBridgeInputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

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

    // 试听申请瞬时音频焦点（用户 09-08）：朗读客户端收到焦点丢失自动暂停，
    // 试听结束释放焦点、朗读续播——解决"试听与朗读混音听不清"
    private val focusListener = android.media.AudioManager.OnAudioFocusChangeListener { }
    private var focusSession = 0
    private var appContext: Context? = null

    @Suppress("DEPRECATION")
    private fun requestAudioFocus(context: Context) {
        appContext = context.applicationContext
        val am = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        am.requestAudioFocus(
            focusListener,
            android.media.AudioManager.STREAM_MUSIC,
            android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
        )
    }

    @Suppress("DEPRECATION")
    private fun abandonAudioFocus() {
        val ctx = appContext ?: return
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        am.abandonFocus(focusListener)
    }

    // 本次会话是否已真正出声(合成完毕进入播放)；JS 用它把按钮从…切到■,对齐v9时机
    @Volatile
    private var audible: Boolean = false

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
            val audioPlayer = AudioPlayer(context.applicationContext)
            player?.release()
            player = audioPlayer
            // 申请瞬时焦点：书声让位暂停（用户 09-08），试听结束在 finally 释放、朗读续播
            focusSession++
            val session = focusSession
            requestAudioFocus(context)
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

                    if (resolved.configuration.shouldDecode() && !declaredPcm) {
                        audible = true
                        audioPlayer.play(bytes, local.speed, localVolume, local.pitch)
                    } else {
                        val sampleRate = if (declaredPcm) {
                            bridgeFormat!!.sampleRate
                        } else {
                            AudioDecoder.getSampleRateAndMime(bytes).first
                                .takeIf { it > 0 }
                                ?: resolved.configuration.audioFormat.sampleRate
                        }
                        audible = true
                        audioPlayer.play(bytes, sampleRate, local.speed, localVolume, local.pitch)
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
                    // 播完/被替换/失败都释放焦点；被新试听顶替时 session 不匹配，由新会话接管
                    if (focusSession == session) abandonAudioFocus()
                }
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            job?.cancel()
            job = null
            audible = false
            player?.stop()
            player?.release()
            player = null
            focusSession++
            abandonAudioFocus()
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
