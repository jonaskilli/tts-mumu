package com.github.jing332.common.audio

import android.content.Context
import java.io.InputStream

class AudioPlayer(context: Context) {
    private val exoAudioPlayer = ExoAudioPlayer(context)
    private val pcmAudioPlayer = PcmAudioPlayer()

    suspend fun play(inputStream: InputStream, sampleRate: Int) {
        pcmAudioPlayer.play(inputStream, sampleRate)
    }

    /**
     * 播放裸 PCM(单声道16bit)。带音频参数时包 WAV 头经 Exo 播放
     * (Exo 原生支持 speed/volume/pitch，与朗读管线一致)；
     * 参数全为默认值时直通 AudioTrack，行为与原实现相同。
     */
    suspend fun play(
        bytes: ByteArray,
        sampleRate: Int,
        speed: Float = 1f,
        volume: Float = 1f,
        pitch: Float = 1f,
    ) {
        if (speed == 1f && volume == 1f && pitch == 1f) {
            pcmAudioPlayer.play(bytes, sampleRate)
            return
        }
        val wav = AudioUtils.createWavHeader(sampleRate, 1, 16, bytes.size) + bytes
        exoAudioPlayer.play(wav, speed, volume, pitch)
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