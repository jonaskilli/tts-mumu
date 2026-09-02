package com.github.jing332.tts_server_android.compose.systts.list.ui

import android.content.Context
import com.github.jing332.common.utils.toScale
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.R

class PluginDescriptor(
    val context: Context,
    val systemTts: SystemTtsV2,
    // 响应式插件名映射（来自 ViewModel 的插件表 Flow）；null 时回退同步查库（小场景可接受）
    // 列表渲染必须传非空映射：数据库允许主线程查询，逐卡片查库是大库滚动卡顿的实锤来源
    private val pluginNames: Map<String, String>? = null,
) : ItemDescriptor() {
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
            val strFollow by lazy { context.getString(R.string.follow) }

            // 卡片三行制：行1名称+标签、行2参数·格式(desc)、行3插件名(type)。
            // 旧版行2是 voice id 技术串(信息量低)，按用户要求删除；格式(bottom)并入参数行。
            // toScale(2) 去噪：历史数据里存在 1.1499999f 这类浮点噪声，直接插值会原样上屏
            val p = cfg.audioParams
            val rateStr = if (p.speed == 0f) strFollow else p.speed.toScale(2)
            val pitchStr = if (p.pitch == 0f) strFollow else p.pitch.toScale(2)
            val volumeStr = if (p.volume == 0f) strFollow else p.volume.toScale(2)

            return context.getString(
                R.string.systts_play_params_description,
                "<b>${rateStr}</b>",
                "<b>${volumeStr}</b>",
                "<b>${pitchStr}</b>"
            ) + " · " + bottom
        }

    // 格式信息已并入 desc 参数行；bottom 槽位由卡片渲染为独立 HtmlText，置空避免重复
    override val bottom: String = ""
    override val type: String by lazy {
        if (pluginNames != null) {
            // 传了映射就完全不走 IO：映射来自插件表 Flow（含未启用插件），查不到即插件已删除
            pluginNames[source.pluginId]
                ?: context.getString(R.string.not_found_plugin, source.pluginId)
        } else {
            synchronized(nameCache) {
                nameCache[source.pluginId] ?: dbm.pluginDao.getEnabledName(source.pluginId)?.also {
                    nameCache[source.pluginId] = it
                } ?: context.getString(R.string.not_found_plugin, source.pluginId)
            }
        }
    }
    override val tagName: String = cfg.speechRule.tagName
    override val standby: Boolean = cfg.speechRule.isStandby
}