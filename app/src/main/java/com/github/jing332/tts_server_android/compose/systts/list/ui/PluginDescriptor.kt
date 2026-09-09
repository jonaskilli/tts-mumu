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

    // 卡片行2：voice id（限一行，超20字符截断防换行，用户定稿）；行3=参数行（bodyMedium 同字号）。
    // 参数行规律（用户 09-10 拍板，退回 09-07 之前的格式）：三维始终全显，数字 <b> 加粗与
    // 标签同字号，管道 | 分隔，1 位小数；无「无设置」占位行；层标后缀彻底消失。
    // 终值=配置×插件×全局（pluginHandles 路由只在弹窗 applyDim 生效，卡片只展示已知层）。
    override val desc: String
        get() {
            val p = cfg.audioParams
            val pluginId = (cfg.source as? PluginTtsSource)?.pluginId
            val pluginParams = pluginId?.let {
                synchronized(pluginParamsCache) {
                    // 轻量元数据查询：列表滚动首次命中逐插件触发，SELECT * 会把 5MB+ JS 读进 CursorWindow
                    pluginParamsCache.getOrPut(it) {
                        dbm.pluginDao.getMetaByPluginId(it)?.audioParams ?: AudioParams()
                    }
                }
            }
            val pluginSpeed = pluginParams?.speed ?: 1f
            val pluginVolume = pluginParams?.volume ?: 1f
            val pluginPitch = pluginParams?.pitch ?: 1f
            val globalSpeed = com.github.jing332.tts_server_android.conf.SysTtsConfig.audioParamsSpeed
            val globalVolume = com.github.jing332.tts_server_android.conf.SysTtsConfig.audioParamsVolume
            val globalPitch = com.github.jing332.tts_server_android.conf.SysTtsConfig.audioParamsPitch

            // 三维恒显+加粗+同字号+管道分隔+1 位小数（退回 09-07 之前的卡片格式）；
            // 后缀格式 `语速/音量/音高: 值` 由 HtmlCompat + <b> 渲染（SpanStyle.Bold），
            // 字号与标签一致（无 <small>）。无前缀——卡片无分层滑杆对照，
            // 与弹窗/面板/编辑页「最终：」逗号+2位+无后缀口径刻意不同（用户 09-10 二稿"反过来统一"）
            val paramsLine = "语速:<b>%.1f</b> | 音量:<b>%.1f</b> | 音高:<b>%.1f</b>".format(
                p.speed * pluginSpeed * globalSpeed,
                p.volume * pluginVolume * globalVolume,
                p.pitch * pluginPitch * globalPitch,
            )

            return source.voice.limitLength(20, "…") + "<br>$paramsLine"
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
