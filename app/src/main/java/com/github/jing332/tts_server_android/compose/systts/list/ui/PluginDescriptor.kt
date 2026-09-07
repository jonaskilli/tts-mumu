package com.github.jing332.tts_server_android.compose.systts.list.ui

import android.content.Context
import com.github.jing332.common.utils.StringUtils.limitLength
import com.github.jing332.common.utils.toScale
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.AudioParams
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

        // 插件层音频参数缓存（同 nameCache 策略）：卡片显示"插件语速2.5"等层值用；
        // 插件参数被编辑后由调用方 invalidate 清空重查
        private val pluginParamsCache = HashMap<String, AudioParams>()

        /** 插件音频参数变化后调用（插件参数应用/插件更新），下次卡片重组重新查库 */
        fun invalidatePluginParamsCache(pluginId: String? = null) {
            synchronized(pluginParamsCache) {
                if (pluginId == null) pluginParamsCache.clear()
                else pluginParamsCache.remove(pluginId)
            }
        }
    }

    override val name: String = systemTts.displayName
    override val desc: String
        get() {
            val strFollow by lazy { context.getString(R.string.follow) }

            // 卡片三行制：行2=voice id(限一行,超20字符截断防换行,用户定稿)，行3=参数，行4=格式(bottom)。
            // 参数行按维度合并显示(用户定稿)：每个维度内按固定顺序(配→插→全)显示全部值，
            // 用×连接、层标(配)/(插)/(全)后缀；全1.0时也显示一行作占位(防卡片排布跳变)；
            // 音高维度加回(换行防放不下)
            val p = cfg.audioParams
            val rateStr = if (p.speed == 0f) strFollow else p.speed.toScale(2)
            val volumeStr = if (p.volume == 0f) strFollow else p.volume.toScale(2)
            val pitchStr = if (p.pitch == 0f) strFollow else p.pitch.toScale(2)

            val pluginId = (cfg.source as? PluginTtsSource)?.pluginId
            val pluginParams = pluginId?.let {
                synchronized(pluginParamsCache) {
                    pluginParamsCache.getOrPut(it) {
                        dbm.pluginDao.getByPluginId(it)?.audioParams ?: AudioParams()
                    }
                }
            }
            val pluginSpeed = pluginParams?.speed ?: 1f
            val pluginVolume = pluginParams?.volume ?: 1f
            val pluginPitch = pluginParams?.pitch ?: 1f
            val globalSpeed = com.github.jing332.tts_server_android.conf.SysTtsConfig.audioParamsSpeed
            val globalVolume = com.github.jing332.tts_server_android.conf.SysTtsConfig.audioParamsVolume

            fun dimensionText(configVal: Float, pluginVal: Float, globalVal: Float): String? {
                // 全部=1.0时该维度不显示(由上层统一显示"无设置"占位)
                if (kotlin.math.abs(configVal - 1f) <= 0.005f &&
                    kotlin.math.abs(pluginVal - 1f) <= 0.005f &&
                    kotlin.math.abs(globalVal - 1f) <= 0.005f
                ) return null
                // 固定顺序(配→插→全)；值=1.0时省略不写，≠1.0时写"数值(层标)"后缀提示来源
                val layerConfig = context.getString(R.string.audio_params_tag_config)
                val layerPlugin = context.getString(R.string.audio_params_tag_plugin)
                val layerGlobal = context.getString(R.string.audio_params_tag_global)
                val parts = buildList {
                    if (kotlin.math.abs(configVal - 1f) > 0.005f)
                        add("${configVal.toScale(2)}($layerConfig)")
                    if (pluginParams != null && kotlin.math.abs(pluginVal - 1f) > 0.005f)
                        add("${pluginVal.toScale(2)}($layerPlugin)")
                    if (kotlin.math.abs(globalVal - 1f) > 0.005f)
                        add("${globalVal.toScale(2)}($layerGlobal)")
                }
                return parts.joinToString("×")
            }

            val speedText = dimensionText(p.speed, pluginSpeed, globalSpeed)
            val volumeText = dimensionText(p.volume, pluginVolume, globalVolume)
            val pitchText = dimensionText(p.pitch, pluginPitch, 1f)

            // 全默认时显示一行浅灰占位(用户定稿：防卡片排布跳变)；有设置时显示三维数值
            val paramsLine = if (speedText == null && volumeText == null && pitchText == null) {
                context.getString(R.string.audio_params_none)
            } else {
                listOfNotNull(
                    speedText?.let { "语速: $it" },
                    volumeText?.let { "音量: $it" },
                    pitchText?.let { "音高: $it" },
                ).joinToString(" | ")
            }

            // 样式恢复旧版（用户 09-07 反馈：span 颜色走 HTML 渲染链路异常发蓝）——
            // 与旧行一致用 <b> 加粗、默认字色字号，仅内容格式为新的三维合并行
            return source.voice.limitLength(20, "…") + "<br><b>$paramsLine</b>"
        }

    override val bottom: String
        get() = formatString(context, cfg.audioFormat)
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