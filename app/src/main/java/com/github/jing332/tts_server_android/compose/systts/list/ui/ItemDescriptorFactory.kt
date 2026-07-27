package com.github.jing332.tts_server_android.compose.systts.list.ui

import android.content.Context
import com.github.jing332.database.entities.systts.BgmConfiguration
import com.github.jing332.database.entities.systts.EmptyConfiguration
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.database.entities.systts.source.PluginTtsSource

object ItemDescriptorFactory {
    fun from(context: Context, systemTts: SystemTtsV2): ItemDescriptor {
        if (systemTts.config is BgmConfiguration)
            return BgmDescriptor(context, systemTts)
        else if (systemTts.config is EmptyConfiguration)
            return EmptyDescriptor()

        return when ((systemTts.config as TtsConfigurationDTO).source) {
            is LocalTtsSource -> LocalTtsDescriptor(context, systemTts)
            is PluginTtsSource -> PluginDescriptor(context, systemTts)

            else -> throw IllegalArgumentException("Unknown source: ${(systemTts.config as TtsConfigurationDTO).source}")
        }
    }
}