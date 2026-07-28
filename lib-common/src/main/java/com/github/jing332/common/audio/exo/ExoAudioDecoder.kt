package com.github.jing332.common.audio.exo

import android.annotation.SuppressLint
import android.content.Context
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.source.MediaSource
import com.drake.net.utils.withMain
import com.github.jing332.common.audio.ExoPlayerHelper
import com.github.jing332.common.utils.runOnUI
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@SuppressLint("UnsafeOptInUsageError")
class ExoAudioDecoder(val context: Context) {
    private var mContinuation: Continuation<Unit>? = null
    var callback: Callback? = null

    private val exoPlayer by lazy {
        val rendererFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink? {
                return DecoderAudioSink(
                    onPcmBuffer = {
                        callback?.onReadPcmAudio(it)
                    },
                    onEndOfStream = {

                    }
                )
            }
        }

        ExoPlayer.Builder(context, rendererFactory).build().apply {
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

            playWhenReady = true
        }
    }

    @Throws(ExoPlaybackException::class)
    suspend fun doDecode(bytes: ByteArray) {
        if (bytes.isNotEmpty())
            decodeInternal(ExoPlayerHelper.createMediaSourceFromByteArray(context, bytes))
    }

    @Throws(ExoPlaybackException::class)
    suspend fun doDecode(inputStream: InputStream) {
        decodeInternal(ExoPlayerHelper.createMediaSourceFromInputStream(context, inputStream))
    }

    private suspend fun decodeInternal(mediaSource: MediaSource) {
        withMain {
            if (exoPlayer.isReleased)
                throw IllegalStateException("ExoPlayer is released")

            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
        }

        try {
            // throw ExoPlayerException
            suspendCancellableCoroutine<Unit> { continuation ->
                mContinuation = continuation
                continuation.invokeOnCancellation {
                    runOnUI {
                        exoPlayer.stop()
                    }
                }
            }
        } finally {
            mContinuation = null
        }
    }


    fun interface Callback {
        fun onReadPcmAudio(byteBuffer: ByteBuffer)
    }

    suspend fun destroy() {
        withMain {
            if (!exoPlayer.isReleased) exoPlayer.release()
        }
        mContinuation?.context?.apply { if (isActive) cancel() }
        mContinuation = null
        callback = null
    }

}