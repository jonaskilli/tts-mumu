package com.github.jing332.tts_server_android.compose.systts.list.ui

import android.content.Context
import com.github.jing332.database.entities.systts.BasicAudioFormat
import com.github.jing332.tts_server_android.R

/**
 * 卡片格式串：带音频头（MP3/WAV/Opus 等）播放时由音频头自动识别 →「跟随音源格式」；
 * 裸 PCM 且声明了采样率 →「PCM N Hz」；异常值兜底显示「跟随音源格式」。
 */
internal fun formatString(context: Context, format: BasicAudioFormat): String {
    return if (!format.isNeedDecode && format.sampleRate > 0)
        context.getString(R.string.systts_pcm_format, format.sampleRate)
    else
        context.getString(R.string.systts_auto_detect_audio_format)
}

abstract class ItemDescriptor {
    open val name: String = "name"
    open val desc: String = "desc"
    open val bottom: String = "bottom"
    open val type: String = "type"
    open val tagName: String = "tag"
    open val standby: Boolean = false
}
