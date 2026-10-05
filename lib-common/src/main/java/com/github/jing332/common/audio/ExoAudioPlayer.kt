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

    // @Volatile：协程线程写、主线程(播放器回调)读；先于 prepare 赋值是硬性时序要求，见 playInternal
    @Volatile
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
                            mContinuation = null
                        }
                    }

                    super.onPlaybackStateChanged(playbackState)
                }

                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    mContinuation?.resumeWithException(error)
                    mContinuation = null
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
    ) = suspendCancellableCoroutine<Unit> { continuation ->
        // 必须先挂 continuation 再 prepare（用户 09-09：试听结束后按钮不复位的根因）——
        // 原实现先 prepare 后赋值，短音频可能在赋值前就到 STATE_ENDED，resume 落空，
        // 调用方协程永久挂起（朗读/试听等所有等待播完的链路都会卡死）
        mContinuation = continuation
        continuation.invokeOnCancellation {
            mContinuation = null
            runMain { exoPlayer.stop() }
        }
        runMain {
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.playbackParameters =
                PlaybackParameters(speed, pitch)
            exoPlayer.volume = volume
            exoPlayer.prepare()
        }
    }

    fun stop() {
        // 取消 continuation 即停声+给等待方解挂；无等待方时天然什么都不发生（不触碰 lazy exoPlayer）
        mContinuation?.context?.cancel()
    }

    fun release() {
        // 取消 continuation 廉价且空安全，无条件执行（保证等待方解挂）
        mContinuation?.context?.cancel()
        // 未创建过就跳过（10-05 性能修：`exoPlayer by lazy` 会在 release 时先创建再销毁——
        // 角色面板试听走纯 PCM 路径从不碰 Exo，每会话却都白烧一次 ExoPlayer 创建/销毁）
        if (::exoPlayer.isInitialized) exoPlayer.release()
    }

}