package com.github.jing332.tts_server_android.compose.systts.plugin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.github.jing332.tts_server_android.compose.systts.ConfigImportBottomSheet
import com.github.jing332.tts_server_android.compose.systts.ConfigModel
import com.github.jing332.tts_server_android.compose.systts.SelectImportConfigDialog
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.common.utils.toJsonListString
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.plugin.Plugin
import com.drake.net.utils.withIO
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun PluginImportBottomSheet(onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<Plugin>?>(null) }
    if (list != null) {
        SelectImportConfigDialog(
            onDismissRequest = { list = null },
            models = list!!.map {
                ConfigModel(
                    isSelected = true,
                    title = it.name,
                    subtitle = it.author,
                    it
                )
            },
            onSelectedList = {
                val plugins = it.map { plugin -> plugin as Plugin }
                scope.launch {
                    withIO { dbm.pluginDao.insert(*plugins.toTypedArray()) }
                }

                it.size
            }
        )
    }

    ConfigImportBottomSheet(onDismissRequest = onDismissRequest, onImport = {
        list = parsePluginsJson(it)
    })
}

/**
 * 解析插件JSON，兼容两种格式：
 * 1. 原生格式：直接是 Plugin 对象或 JSON 数组 [{...}, {...}]
 * 2. JRead插件包格式：{"format":"jread_voice_plugin_bundle","version":1,"plugins":[...]}
 *
 * 注意：ConfigImportBottomSheet 会调用 toJsonListString() 给字符串加 []，
 * 所以 JRead 格式可能被包裹成 [{"format":...,"plugins":[...]}]，需要兼容处理。
 *
 * JRead格式与原生格式的差异及转换：
 * - 嵌套在 plugins 数组中 → 提取出来
 * - version 是字符串(如"20260625.v2") → Plugin.version 是 Int，无法转换则设为0
 * - enabled 字段 → 映射到 isEnabled
 * - defVars 子字段 name → 映射到 label（原生用 label）
 * - 多余字段(pluginGroupId等) → ignoreUnknownKeys=true 自动忽略
 */
private fun parsePluginsJson(jsonStr: String): List<Plugin> {
    val json = AppConst.jsonBuilder
    val trimmed = jsonStr.trim()
    val element = json.parseToJsonElement(trimmed)

    // 辅助函数：从 JsonObject 提取 JRead 插件列表
    fun extractJReadPlugins(obj: JsonObject): List<Plugin>? {
        val isJRead = obj["format"]?.jsonPrimitive?.contentOrNull?.contains("jread") == true ||
            obj["plugins"] != null
        if (!isJRead) return null

        val pluginsArr = obj["plugins"]?.jsonArray ?: return emptyList()
        return pluginsArr.mapNotNull { elem ->
            try {
                val pobj = elem.jsonObject
                Plugin(
                    name = pobj["name"]?.jsonPrimitive?.contentOrNull ?: "",
                    pluginId = pobj["pluginId"]?.jsonPrimitive?.contentOrNull
                        ?: pobj["id"]?.jsonPrimitive?.contentOrNull ?: "",
                    author = pobj["author"]?.jsonPrimitive?.contentOrNull ?: "",
                    iconUrl = pobj["iconUrl"]?.jsonPrimitive?.contentOrNull ?: "",
                    code = pobj["code"]?.jsonPrimitive?.contentOrNull ?: "",
                    version = 0, // JRead的version是字符串，无法转Int，统一设0
                    isEnabled = pobj["isEnabled"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
                        ?: pobj["enabled"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
                        ?: false,
                    order = pobj["order"]?.jsonPrimitive?.intOrNull ?: 0,
                    defVars = convertDefVars(pobj["defVars"]?.jsonObject),
                    userVars = emptyMap(),
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    // 情况1：直接是 JsonObject（JRead包格式，未被包裹）
    if (element is JsonObject) {
        extractJReadPlugins(element)?.let { return it }
    }

    // 情况2：是 JsonArray，可能被 toJsonListString 包裹过
    if (element is JsonArray) {
        // 检查数组第一个元素是否是 JRead 包格式
        if (element.isNotEmpty() && element[0] is JsonObject) {
            val jreadList = extractJReadPlugins(element[0] as JsonObject)
            if (jreadList != null) return jreadList
        }
        // 普通数组格式，按原生解析
        return json.decodeFromString<List<Plugin>>(JsonArray(element).toString())
    }

    // 情况3：其他情况按原生格式解析
    return json.decodeFromString<List<Plugin>>(trimmed.toJsonListString())
}

/**
 * 转换 defVars：JRead格式中用 name 作为标签，原生用 label
 * 统一转换成 Map<String, Map<String, String>>
 */
private fun convertDefVars(defVarsObj: JsonObject?): Map<String, Map<String, String>> {
    if (defVarsObj == null) return emptyMap()
    val result = mutableMapOf<String, Map<String, String>>()
    for ((key, value) in defVarsObj) {
        try {
            val inner = value.jsonObject
            val converted = mutableMapOf<String, String>()
            for ((k, v) in inner) {
                val content = v.jsonPrimitive.contentOrNull ?: ""
                // name → label（原生格式用 label 作为显示名）
                if (k == "name") converted["label"] = content
                else converted[k] = content
            }
            result[key] = converted
        } catch (_: Exception) {
            // 跳过格式不对的项
        }
    }
    return result
}