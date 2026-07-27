package com.github.jing332.common.audio

import android.annotation.SuppressLint
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import com.github.jing332.common.audio.exo.InputStreamDataSource
import java.io.InputStream


object ExoPlayerHelper {
    @SuppressLint("UnsafeOptInUsageError")
    fun createMediaSourceFromInputStream(context: Context, inputStream: InputStream): MediaSource {
        val factory = DataSource.Factory {
            InputStreamDataSource(inputStream)
        }
        return DefaultMediaSourceFactory(context).setDataSourceFactory(factory)
            .createMediaSource(MediaItem.fromUri(""))
    }

    // 创建音频媒体源
    @SuppressLint("UnsafeOptInUsageError")
    fun createMediaSourceFromByteArray(context: Context, data: ByteArray): MediaSource {
        val factory = DataSource.Factory { ByteArrayDataSource(data) }
        return DefaultMediaSourceFactory(context).setDataSourceFactory(factory)
            .createMediaSource(MediaItem.fromUri(""))
    }
}