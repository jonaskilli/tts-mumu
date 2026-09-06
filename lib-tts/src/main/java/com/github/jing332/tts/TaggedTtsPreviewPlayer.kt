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
            val audioPlayer = AudioPlayer(context.applicationContext)
            player?.release()
            player = audioPlayer
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
                        audioPlayer.play(bytes, local.speed, localVolume, local.pitch)
                    } else {
                        val sampleRate = if (declaredPcm) {
                            bridgeFormat!!.sampleRate
                        } else {
                            AudioDecoder.getSampleRateAndMime(bytes).first
                                .takeIf { it > 0 }
                                ?: resolved.configuration.audioFormat.sampleRate
                        }
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
                }
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            job?.cancel()
            job = null
            player?.stop()
            player?.release()
            player = null
        }
    }
}
