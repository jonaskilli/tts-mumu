package com.github.jing332.tts_server_android.compose.systts.list.ui

import android.content.Context
import com.github.jing332.database.entities.systts.BgmConfiguration
import com.github.jing332.database.entities.systts.EmptyConfiguration
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.database.entities.systts.source.PluginTtsSource

object ItemDescriptorFactory {
    /**
     * [pluginNames]：响应式插件名映射（ViewModel 从插件表 Flow 构建，含未启用插件）。
     * 传入后插件卡片不再在渲染路径同步查库（大库滚动/展开卡顿源之一）。
     */
    fun from(
        context: Context,
        systemTts: SystemTtsV2,
        pluginNames: Map<String, String>? = null,
    ): ItemDescriptor {
        if (systemTts.config is BgmConfiguration)
            return BgmDescriptor(context, systemTts)
        else if (systemTts.config is EmptyConfiguration)
            return EmptyDescriptor()

        return when ((systemTts.config as TtsConfigurationDTO).source) {
            is LocalTtsSource -> LocalTtsDescriptor(context, systemTts)
            is PluginTtsSource -> PluginDescriptor(context, systemTts, pluginNames)

            else -> throw IllegalArgumentException("Unknown source: ${(systemTts.config as TtsConfigurationDTO).source}")
        }
    }
}