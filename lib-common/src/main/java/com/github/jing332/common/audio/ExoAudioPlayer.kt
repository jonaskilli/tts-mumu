package com.github.jing332.common.audio

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.FloatRange
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import com.drake.net.utils.runMain
import com.drake.net.utils.withMain
import com.github.jing332.common.audio.ExoPlayerHelper.createMediaSourceFromByteArray
import com.github.jing332.common.audio.ExoPlayerHelper.createMediaSourceFromInputStream
import kotlinx.coroutines.*
import java.io.InputStream
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class ExoAudioPlayer(val context: Context) {
    companion object {
        const val TAG = "AudioPlayer"

        const val MSG_STATE_ENDED = "MSG_STATE_ENDED"
        const val MSG_PLAYER_ERROR = "MSG_PLAYER_ERROR"
    }

    private var mContinuation: Continuation<Unit>? = null

    private val exoPlayer by lazy {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            addListener(object : Player.Listener {
                @SuppressLint("SwitchIntDef")
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        ExoPlayer.STATE_ENDED -> {
                            mContinuation?.resume(Unit)
                        }
                    }

                    super.onPlaybackStateChanged(playbackState)
                }

                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    mContinuation?.resumeWithException(error)
                }
            })
        }
    }

    suspend fun play(audio: InputStream, speed: Float = 1f, volume: Float = 1f, pitch: Float = 1f) {
        playInternal(createMediaSourceFromInputStream(context, audio), speed, volume, pitch)
    }

    suspend fun play(audio: ByteArray, speed: Float = 1f, volume: Float = 1f, pitch: Float = 1f) {
        if (audio.isNotEmpty())
            playInternal(createMediaSourceFromByteArray(context, audio), speed, volume, pitch)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private suspend fun playInternal(
        mediaSource: MediaSource,
        speed: Float = 1f,
        @FloatRange(from = 0.0, to = 1.0) volume: Float = 1f,
        pitch: Float = 1f,
    ) = coroutineScope {
        withMain {
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.playbackParameters =
                PlaybackParameters(speed, pitch)
            exoPlayer.volume = volume
            exoPlayer.prepare()
        }

        suspendCancellableCoroutine<Unit> { continuation ->
            mContinuation = continuation
            continuation.invokeOnCancellation {
                runMain { exoPlayer.stop() }
            }
        }
    }

    fun stop() {
        mContinuation?.context?.cancel()
    }

    fun release() {
        stop()
        exoPlayer.release()
    }

}