package com.github.jing332.tts_server_android.compose.systts.list.ui

import android.content.Context
import com.github.jing332.database.entities.systts.BasicAudioFormat
import com.github.jing332.tts_server_android.R

/**
 * 卡片格式串：
 * 裸 PCM（无需解码）且采样率>0 →「PCM N Hz」（无音频头，声明值即事实）；
 * 带音频头（MP3/WAV/Opus 等，播放时自动识别实际格式）→「跟随音源格式」，
 * 括注配置里的声明采样率供参考（jread 导入占位值 16000 不显示）。
 */
internal fun formatString(context: Context, format: BasicAudioFormat): String {
    return if (!format.isNeedDecode && format.sampleRate > 0)
        context.getString(R.string.systts_pcm_format, format.sampleRate)
    else if (format.sampleRate > 0 && format.sampleRate != 16000)
        context.getString(R.string.systts_auto_detect_audio_format) + "(${format.sampleRate}Hz)"
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
