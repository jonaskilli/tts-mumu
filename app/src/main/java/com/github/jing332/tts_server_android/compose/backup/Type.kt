package com.github.jing332.tts_server_android.compose.backup

import com.github.jing332.tts_server_android.R


sealed class Type(val nameStrId: Int) {
    companion object {
        val typeList by lazy {
            listOf(
                Preference,
                List,
                SpeechRule,
                ReplaceRule,
                Plugin,
                PluginVars,
                Keys,
                Loudness,
                WebDav
            )
        }
    }

    data object Preference : Type(R.string.preference_settings)
    data object List : Type(R.string.config_list)
    data object SpeechRule : Type(R.string.speech_rule)
    data object ReplaceRule : Type(R.string.replace_rule)

    abstract class IPlugin(val id: Int, val includeVars: Boolean) : Type(id)
    object Plugin : IPlugin(R.string.plugin, false)
    object PluginVars : IPlugin(R.string.plugin_vars, true)

    /** 密钥：控制配置列表导出时是否保留 keyListJson，不勾则脱敏 */
    data object Keys : Type(R.string.backup_keys)

    /** 响度学习数据：备份/恢复 loudness_stats.json */
    data object Loudness : Type(R.string.backup_loudness)

    /** WebDAV 设置：不勾则从 app.xml 中移除 webDav 相关字段 */
    data object WebDav : Type(R.string.backup_webdav)
}