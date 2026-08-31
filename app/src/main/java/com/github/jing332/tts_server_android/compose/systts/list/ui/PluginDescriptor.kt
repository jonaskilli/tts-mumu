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

    companion object {
        // 插件名缓存：数千配置项的列表滚动/展开时每张卡片组合期同步查库会拖慢主线程。
        // 只缓存命中名（未启用的插件不缓存，恢复启用后可及时显示）；插件改名后重启刷新，列表卡片场景可接受
        private val nameCache = HashMap<String, String>()
    }

    override val name: String = systemTts.displayName
    override val desc: String
        get() {
            // 列表配置项显示在「仅界面模式」开启前后保持一致；
            // 仅界面模式的差异只在点击进入编辑页面时体现(见 PluginTtsUI.EditContentScreen)。
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

    override val bottom: String = cfg.audioFormat.run {
        if (source.shouldDecode(this)) {
            context.getString(R.string.systts_auto_detect_audio_format)
        } else {
            context.getString(R.string.systts_pcm_format, sampleRate)
        }
    }
    override val type: String by lazy {
        synchronized(nameCache) {
            nameCache[source.pluginId] ?: dbm.pluginDao.getEnabledName(source.pluginId)?.also {
                nameCache[source.pluginId] = it
            } ?: context.getString(R.string.not_found_plugin, source.pluginId)
        }
    }
    override val tagName: String = cfg.speechRule.tagName
    override val standby: Boolean = cfg.speechRule.isStandby
}