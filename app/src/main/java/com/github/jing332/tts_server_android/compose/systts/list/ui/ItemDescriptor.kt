package com.github.jing332.tts_server_android.compose.systts.list.ui

import android.content.Context
import com.github.jing332.database.entities.systts.BasicAudioFormat
import com.github.jing332.tts_server_android.R

/**
 * 卡片格式串（两分支）：
 * 裸 PCM（无需解码）且采样率>0 →「PCM N Hz」（无音频头，声明值即事实，必须显示）；
 * 带音频头（MP3/WAV/Opus 等）→「跟随音源格式」（播放时由音频头自动识别实际格式，
 * 配置里的声明采样率不参与播放、参考价值弱且与插件名挤同一行，不再显示）。
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
