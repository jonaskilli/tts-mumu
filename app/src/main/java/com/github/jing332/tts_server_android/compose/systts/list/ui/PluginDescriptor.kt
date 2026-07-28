package com.github.jing332.tts_server_android.compose.systts.list.ui

import android.content.Context
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.R

class PluginDescriptor(val context: Context, val systemTts: SystemTtsV2) : ItemDescriptor() {
    private val cfg = (systemTts.config as TtsConfigurationDTO)
    private val source: PluginTtsSource = cfg.source as PluginTtsSource

    override val name: String = systemTts.displayName
    override val desc: String
        get() {
            val strFollow by lazy { context.getString(R.string.follow) }

            val rateStr =
                if (source.speed == PluginTtsSource.SPEED_FOLLOW) strFollow else source.speed
            val pitchStr =
                if (source.pitch == PluginTtsSource.PITCH_FOLLOW) strFollow else source.pitch
            val volumeStr =
                if (source.volume == PluginTtsSource.VOLUME_FOLLOW) strFollow else source.volume

            return source.voice + "<br>" + context.getString(
                R.string.systts_play_params_description,
                "<b>${rateStr}</b>",
                "<b>${volumeStr}</b>",
                "<b>${pitchStr}</b>"
            )

        }

    override val bottom: String = (systemTts.config as TtsConfigurationDTO).audioFormat.run {
        "${sampleRate}hz" + if (isNeedDecode) " | " + context.getString(R.string.decode) else ""
    }
    override val type: String =
        dbm.pluginDao.getEnabled(source.pluginId)?.name
            ?: context.getString(R.string.not_found_plugin, source.pluginId)
    override val tagName: String = cfg.speechRule.tagName
    override val standby: Boolean = cfg.speechRule.isStandby
}