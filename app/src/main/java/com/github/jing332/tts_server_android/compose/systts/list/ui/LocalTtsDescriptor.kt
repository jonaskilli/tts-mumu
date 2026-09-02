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
            // 卡片三行制(同PluginDescriptor)：删voice id行,格式并入参数行;toScale(2)去噪
            val config = systemTts.config as TtsConfigurationDTO
            val params = config.audioParams

            val rateStr =
                if (params.speed == 0f) strFollow else params.speed.toScale(2)
            val pitchStr =
                if (params.pitch == 0f) strFollow else params.pitch.toScale(2)
            val volumeStr =
                if (params.volume == 0f) strFollow else params.volume.toScale(2)

            return context.getString(
                R.string.systts_play_params_description,
                "<b>${rateStr}</b>",
                "<b>${volumeStr}</b>",
                "<b>${pitchStr}</b>"
            ) + " · " + bottom
        }


    // 格式信息已并入 desc；置空避免卡片重复渲染
    override val bottom: String = ""

}