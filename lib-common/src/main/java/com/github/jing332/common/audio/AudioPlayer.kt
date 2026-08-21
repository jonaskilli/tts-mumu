package com.github.jing332.common.audio

import android.content.Context
import java.io.InputStream

class AudioPlayer(context: Context) {
    private val exoAudioPlayer = ExoAudioPlayer(context)
    private val pcmAudioPlayer = PcmAudioPlayer()

    suspend fun play(inputStream: InputStream, sampleRate: Int) {
        pcmAudioPlayer.play(inputStream, sampleRate)
    }

    // speed/volume/pitch：本地应用的音频参数(试听对齐朗读效果)，默认1f行为不变
    fun play(bytes: ByteArray, sampleRate: Int, speed: Float = 1f, volume: Float = 1f, pitch: Float = 1f) {
        pcmAudioPlayer.play(bytes, sampleRate, speed, volume, pitch)
    }

    suspend fun play(inputStream: InputStream) {
        exoAudioPlayer.play(inputStream)
    }

    suspend fun play(bytes: ByteArray, speed: Float = 1f, volume: Float = 1f, pitch: Float = 1f) {
        exoAudioPlayer.play(bytes, speed, volume, pitch)
    }

    fun stop() {
        exoAudioPlayer.stop()
        pcmAudioPlayer.stop()
    }

    fun release() {
        exoAudioPlayer.release()
        pcmAudioPlayer.release()
    }
}