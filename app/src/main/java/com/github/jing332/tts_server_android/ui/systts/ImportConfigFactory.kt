package com.github.jing332.tts_server_android.ui.systts

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.list.ListImportBottomSheet
import com.github.jing332.tts_server_android.compose.systts.plugin.PluginImportBottomSheet
import com.github.jing332.tts_server_android.compose.systts.replace.ReplaceRuleImportBottomSheet
import com.github.jing332.tts_server_android.compose.systts.speechrule.SpeechRuleImportBottomSheet
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
    SPEECH_RULE("speechRule", R.string.speech_rule),
    // 密钥导出文件（密钥列表.json / 老密钥导出_*.json，10-14 起统一自识别）：
    // 不在 getBottomSheet 出面板（导入即写库+toast），strResId 复用密钥页标题
    KEY("key", R.string.role_key_title)
}

object ImportConfigFactory {
    /**
     * @param onExternal true = 外部入口（ImportConfigActivity：文件管理器「打开方式」/ ttsrv 链接），
     *   导入成功后一律跳识别出的类型对应管理页（用户此刻停在导入中转页，不跳=踢回文件管理器）；
     *   false = app 内部 ⋮ 菜单导入——10-15 B 方案：识别出的类型 = 发起页类型时不跳（人本来
     *   就在那），导出**别的**类型才跳（判据由各面板 ownType 实现，见各 ImportBottomSheet）。
     *   内核 doAutoImport 四面板同一套，跳转只关乎「人在哪」，与文件内容无关。
     */
    fun getBottomSheet(
        type: String,
        onBadFormat: () -> Unit,
        onExternal: Boolean = true,
    ): @Composable (() -> Unit) -> Unit {
        // B 方案：内部菜单调用时传发起页自身类型；null=外部入口全跳
        val ownType = if (onExternal) null else ImportType.values().find { it.id == type }
        return when (ImportType.values().find { it.id == type }) {
            ImportType.LIST -> {
                { ListImportBottomSheet(it, ownType = ownType) }
            }

            ImportType.PLUGIN -> {
                { PluginImportBottomSheet(it, ownType = ownType) }
            }

            ImportType.REPLACE_RULE -> {
                { ReplaceRuleImportBottomSheet(it, ownType = ownType) }
            }

            ImportType.SPEECH_RULE -> {
                { SpeechRuleImportBottomSheet(it, ownType = ownType) }
            }

            // KEY 不出面板：doAutoImport 导入即完成（写库+toast），无手动导入入口
            ImportType.KEY -> {
                onBadFormat()
                return { println("bad format") }
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
            // JRead 包：声音配置包归 LIST，其余（插件包）归 PLUGIN
            val fmt = this["format"]
            if (fmt is JsonPrimitive) {
                if (fmt.contentOrNull?.contains("jread_voice_config_bundle") == true)
                    return ImportType.LIST
                if (fmt.contentOrNull?.contains("jread", ignoreCase = true) == true)
                    return ImportType.PLUGIN
            }
            if (this["plugins"] != null) return ImportType.PLUGIN
            // JRead 配置：bundle 的 configs 数组 / 单条配置对象
            if (this["configs"] != null || this["voiceTag"] != null) return ImportType.LIST
            // 插件：含 pluginId 或裸 code
            if (this["pluginId"] != null) return ImportType.PLUGIN
            // 朗读规则：含 ruleId / tags
            if (this["ruleId"] != null || this["tags"] != null) return ImportType.SPEECH_RULE
            // 替换规则：含 pattern / replacement
            if (this["pattern"] != null || this["replacement"] != null) return ImportType.REPLACE_RULE
            // 密钥导出文件：v2 对象带 keys。
            // 放 replace-rule 之后：key 老导出对内层 {keyCode,value} 无 pattern，此处不会误接
            if (this["keys"] is JsonArray) return ImportType.KEY
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
            is JsonArray -> {
                if (element.isEmpty()) return null
                // 插件时代老密钥导出：[["名字",{keyCode,value}],...]——首元素是二元内嵌数组特判
                val pair = element[0] as? JsonArray
                if (pair != null && pair.size == 2 && pair[1] is JsonObject &&
                    (pair[1] as JsonObject)["value"] != null
                ) return ImportType.KEY
                val first = element[0] as? JsonObject
                // 分组数组（group+list）：扫到第一个**非空** list 再判型——
                // 首组常是空「默认分组」，只看它会误判 LIST，替换规则条目随后被
                // 配置列表 v1 解析器要求 tts 字段而炸（10-15 实报 replace_rules.json）。
                // 全组皆空时无从判型，维持旧口径 LIST（空组列表更可能是配置导出）。
                if (first != null && first["group"] != null) {
                    for (g in element) {
                        val obj = g as? JsonObject ?: continue
                        val list = obj["list"] as? JsonArray ?: continue
                        if (list.isEmpty()) continue
                        val item = list[0] as? JsonObject
                            ?: return ImportType.LIST
                        return if (item["pattern"] != null || item["replacement"] != null)
                            ImportType.REPLACE_RULE
                        else ImportType.LIST
                    }
                    return ImportType.LIST
                }
                first?.detect()
            }
            else -> null
        }
    }

    /**
     * @return 是否识别成功
     */
}