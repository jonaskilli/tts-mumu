package com.github.jing332.tts_server_android.compose.systts.plugin

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.github.jing332.common.utils.toJsonListString
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.ConfigImportBottomSheet
import com.github.jing332.tts_server_android.compose.systts.list.AutoImportResult
import com.github.jing332.tts_server_android.compose.systts.list.doAutoImport
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import com.drake.net.utils.withIO
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun PluginImportBottomSheet(onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 结果以模态对话框展示，导入过程的连续状态机由 ConfigImportBottomSheet 承载，
    // 用户确认后再 onDismissRequest，避免 AlertDialog 叠在 ModalBottomSheet 上被遮挡。
    var successMsg = remember { mutableStateOf<String?>(null) }
    // 先取局部 val 再判空：局部 val 支持 smart cast，MutableState.value 属性不支持
    val msgText = successMsg.value
    if (msgText != null) {
        AlertDialog(
            onDismissRequest = { successMsg.value = null; onDismissRequest() },
            confirmButton = {
                TextButton(onClick = { successMsg.value = null; onDismissRequest() }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            text = { Text(msgText) }
        )
        return
    }

    ConfigImportBottomSheet(onDismissRequest = onDismissRequest,
        autoImport = true,
        onResult = { msg -> if (msg != null) successMsg.value = msg },
        onImport = { json ->
            // 自动识别 JSON 类型并直接导入，无需手动选择/确认
            scope.launch {
                val result = withIO { doAutoImport(json) }
                when (result) {
                    AutoImportResult.EmptyOrUnrecognized -> {
                        successMsg.value = context.getString(R.string.import_no_valid_config)
                    }
                    is AutoImportResult.Truncated -> {
                        context.displayErrorDialog(
                            Exception(result.detail),
                            title = context.getString(R.string.import_failed)
                        )
                    }
                    is AutoImportResult.Success -> {
                        successMsg.value = "已导入 ${result.count} 项${result.typeName}"
                    }
                }
            }
        }
    )
}

/**
 * 解析插件JSON，兼容两种格式：
 * 1. 原生格式：直接是 Plugin 对象或 JSON 数组 [{...}, {...}]
 * 2. JRead插件包格式：{"format":"jread_voice_plugin_bundle","version":1,"plugins":[...]}
 *
 * 注意：ConfigImportBottomSheet 会调用 toJsonListString() 给字符串加 []，
 * 所以 JRead 格式可能被包裹成 [{"format":...,"plugins":[...]}]，需要兼容处理。
 *
 * 第14项性能优化(避免大文件OOM闪退):
 * - 原生格式: 先用廉价字符串检测排除JRead, 再直接 decodeFromString, 不构建中间 JsonElement DOM,
 *   避免「输入串 + DOM + toString副本 + List<Plugin>」同时驻留内存导致峰值过高(3.78MB即闪退)。
 * - JRead格式: 仅在确认是JRead时才 parseToJsonElement, 且用 decodeFromJsonElement 而非 toString()+decodeFromString。
 */
internal fun parsePluginsJson(jsonStr: String): List<Plugin> {
    val json = AppConst.jsonBuilder
    val trimmed = jsonStr.trim()

    // 廉价检测 JRead 格式：只看前200字符是否含 jread/plugins 标记，避免对大文件全量构建 DOM
    val head = trimmed.take(200)
    val isLikelyJRead = head.contains("\"format\"") &&
        (head.contains("jread") || head.contains("\"plugins\""))

    // 原生格式：直接解码，不构建中间 JsonElement DOM，大幅降低峰值内存
    if (!isLikelyJRead) {
        return runCatching { json.decodeFromString<List<Plugin>>(trimmed) }
            .recoverCatching { json.decodeFromString<Plugin>(trimmed).let { listOf(it) } }
            .getOrDefault(emptyList())
    }

    // JRead 格式：需要解析 DOM 提取 plugins 数组
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
        if (element.isNotEmpty() && element[0] is JsonObject) {
            val jreadList = extractJReadPlugins(element[0] as JsonObject)
            if (jreadList != null) return jreadList
        }
        // 普通数组格式：直接从已解析的 DOM 解码，避免 toString() 再造一份大字符串
        return runCatching { json.decodeFromJsonElement<List<Plugin>>(element) }.getOrDefault(emptyList())
    }

    // 情况3：其他情况按原生格式解析
    return runCatching { json.decodeFromString<List<Plugin>>(trimmed.toJsonListString()) }
        .recoverCatching { json.decodeFromString<Plugin>(trimmed).let { listOf(it) } }
        .getOrDefault(emptyList())
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