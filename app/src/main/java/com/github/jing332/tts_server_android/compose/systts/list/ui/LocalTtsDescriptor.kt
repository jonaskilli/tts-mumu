package com.github.jing332.tts_server_android.compose.systts.list.ui

import android.content.Context
import com.github.jing332.common.utils.toScale
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.tts_server_android.R

class LocalTtsDescriptor(val context: Context, val systemTts: SystemTtsV2) :
    TtsItemDescriptor<LocalTtsSource>(systemTts.config) {

    override val type: String
        get() = context.getString(R.string.local)

    override val desc: String
        get() {
            val strFollow by lazy { context.getString(R.string.follow) }
            val config = systemTts.config as TtsConfigurationDTO
            val params = config.audioParams

            // toScale(2) 去噪：历史数据里存在 1.1499999f 这类浮点噪声，直接插值会原样上屏
            val rateStr =
                if (params.speed == 0f) strFollow else params.speed.toScale(2)
            val pitchStr =
                if (params.pitch == 0f) strFollow else params.pitch.toScale(2)
            val volumeStr =
                if (params.volume == 0f) strFollow else params.volume.toScale(2)

            return source.voice + "<br>" + context.getString(
                R.string.systts_play_params_description,
                "<b>${rateStr}</b>",
                "<b>${volumeStr}</b>",
                "<b>${pitchStr}</b>"
            )
        }


    override val bottom: String
        get() = config.audioFormat.run {
            if (source.shouldDecode(this)) {
                context.getString(R.string.systts_auto_detect_audio_format)
            } else {
                context.getString(R.string.systts_pcm_format, sampleRate)
            }
        }

}