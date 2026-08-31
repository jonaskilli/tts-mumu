package com.github.jing332.database.entities.systts

import android.media.AudioFormat
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
/**
 * [sampleRate] 是原始 PCM 的格式声明，并参与系统 TTS 统一 PCM 输出格式的选择。
 * 对 MP3/WAV/Opus 等可识别格式，播放时会从音频头读取实际输入格式，不应将其视为
 * 每次返回音频的真实采样率。
 */
data class BasicAudioFormat(
    var sampleRate: Int = 16000,
    var bitRate: Int = AudioFormat.ENCODING_PCM_16BIT,
    var isNeedDecode: Boolean = true,
) : Parcelable