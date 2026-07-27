package com.github.jing332.tts_server_android.compose.systts.list.ui

import com.github.jing332.database.entities.systts.BgmConfiguration
import com.github.jing332.database.entities.systts.EmptyConfiguration
import com.github.jing332.database.entities.systts.IConfiguration
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.database.entities.systts.source.PluginTtsSource

object ConfigUiFactory {
    fun from(config: IConfiguration): IConfigUI? {
        if (config is BgmConfiguration) {
            return BgmConfigUI()
        } else if (config is EmptyConfiguration)
            return null

        return when ((config as TtsConfigurationDTO).source) {
            is LocalTtsSource -> LocalTtsUI()
            is PluginTtsSource -> PluginTtsUI()
            else -> null
        }
    }
}