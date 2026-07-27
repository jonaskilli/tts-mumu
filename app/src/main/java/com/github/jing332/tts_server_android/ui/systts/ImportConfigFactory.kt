package com.github.jing332.tts_server_android.ui.systts

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.list.ListImportBottomSheet
import com.github.jing332.tts_server_android.compose.systts.plugin.PluginImportBottomSheet
import com.github.jing332.tts_server_android.compose.systts.plugin.PluginManagerActivity
import com.github.jing332.tts_server_android.compose.systts.replace.ReplaceRuleImportBottomSheet
import com.github.jing332.tts_server_android.compose.systts.speechrule.SpeechRuleImportBottomSheet
import com.github.jing332.tts_server_android.compose.systts.speechrule.SpeechRuleManagerActivity
import com.github.jing332.tts_server_android.constant.AppConst
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

enum class ImportType(val id: String, @StringRes val strResId: Int) {
    LIST("list", R.string.config_list),
    PLUGIN("plugin", R.string.plugin),
    REPLACE_RULE("replaceRule", R.string.replace_rule),
    SPEECH_RULE("speechRule", R.string.speech_rule)
}

object ImportConfigFactory {
    fun getBottomSheet(type: String, onBadFormat: () -> Unit): @Composable (() -> Unit) -> Unit {
        return when (ImportType.values().find { it.id == type }) {
            ImportType.LIST -> {
                { ListImportBottomSheet(it) }
            }

            ImportType.PLUGIN -> {
                { PluginImportBottomSheet(it) }
            }

            ImportType.REPLACE_RULE -> {
                { ReplaceRuleImportBottomSheet(it) }
            }

            ImportType.SPEECH_RULE -> {
                { SpeechRuleImportBottomSheet(it) }
            }

            else -> {
                onBadFormat()

                return { println("bad format") }
            }
        }
    }

    /**
     * 根据 JSON 内容自动识别导入类型，识别不出返回 null（调用方回退到手动选择）。
     */
    fun detectType(json: String): ImportType? {
        val trimmed = json.trim()
        if (trimmed.isBlank()) return null
        val element = runCatching { AppConst.jsonBuilder.parseToJsonElement(trimmed) }.getOrNull()
            ?: return null

        fun JsonObject.detect(): ImportType? {
            // JRead 插件包
            val fmt = this["format"]
            if (fmt is JsonPrimitive &&
                fmt.contentOrNull?.contains("jread", ignoreCase = true) == true
            ) return ImportType.PLUGIN
            if (this["plugins"] != null) return ImportType.PLUGIN
            // 插件：含 pluginId 或裸 code
            if (this["pluginId"] != null) return ImportType.PLUGIN
            // 朗读规则：含 ruleId / tags
            if (this["ruleId"] != null || this["tags"] != null) return ImportType.SPEECH_RULE
            // 替换规则：含 pattern / replacement
            if (this["pattern"] != null || this["replacement"] != null) return ImportType.REPLACE_RULE
            // 配置列表：含 config（SystemTtsV2）
            if (this["config"] != null) return ImportType.LIST
            // 含 group：GroupWithSystemTts 或 GroupWithReplaceRule
            if (this["group"] != null) {
                val list = this["list"]
                if (list is JsonArray && list.isNotEmpty() && list[0] is JsonObject) {
                    val first = list[0].jsonObject
                    if (first["config"] != null || first["displayName"] != null) return ImportType.LIST
                    if (first["pattern"] != null || first["replacement"] != null) return ImportType.REPLACE_RULE
                }
                return ImportType.LIST
            }
            // 兜底：仅含 code（插件脚本）
            if (this["code"] != null) return ImportType.PLUGIN
            return null
        }

        return when (element) {
            is JsonObject -> element.detect()
            is JsonArray -> if (element.isEmpty()) null else (element[0] as? JsonObject)?.detect()
            else -> null
        }
    }

    /**
     * @return 是否识别成功
     */
    fun Context.gotoEditorFromJS(js: String): Boolean {
        if (js.contains("PluginJS")) {
            startActivity(Intent(this, PluginManagerActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("js", js)
            })

        } else if (js.contains("SpeechRuleJS")) {
            startActivity(Intent(this, SpeechRuleManagerActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("js", js)
            })
        } else
            return false

        return true
    }
}