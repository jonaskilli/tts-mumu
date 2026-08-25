package com.github.jing332.tts_server_android.compose.systts.plugin

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.github.jing332.common.utils.toJsonListString
import com.github.jing332.compose.widgets.LoadingDialog
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.ConfigImportBottomSheet
import com.github.jing332.tts_server_android.compose.systts.list.AutoImportResult
import com.github.jing332.tts_server_android.compose.systts.list.doAutoImport
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import com.drake.net.utils.withIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun PluginImportBottomSheet(onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    // 导入协程作用域：独立于 BottomSheet 生命周期，关闭面板后导入仍继续
    val importScope = rememberCoroutineScope()

    // 面板可见性：导入开始后置 false 仅收起 ModalBottomSheet，本 composable 保持挂载，
    // 使 importScope / 全屏遮罩 / 结果弹窗存活，避免协程被取消导致导入静默失败。
    var sheetVisible by remember { mutableStateOf(true) }
    // 导入进行中遮罩（全屏，面板收起后由本 composable 承载，自然置于最上层）
    var isImporting by remember { mutableStateOf(false) }
    // 导入结果文案（成功/失败原因），非 null 时弹出模态对话框
    var successMsg = remember { mutableStateOf<String?>(null) }

    // 先取局部 val 再判空：局部 val 支持 smart cast，MutableState.value 属性不支持
    val msgText = successMsg.value
    if (msgText != null) {
        AlertDialog(
            onDismissRequest = {
                successMsg.value = null
                sheetVisible = false
                onDismissRequest()
            },
            confirmButton = {
                TextButton(onClick = {
                    successMsg.value = null
                    sheetVisible = false
                    onDismissRequest()
                }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            text = { Text(msgText) }
        )
        return
    }

    if (isImporting) {
        LoadingDialog(onDismissRequest = {}, text = context.getString(R.string.importing))
    }

    if (sheetVisible) {
        ConfigImportBottomSheet(onDismissRequest = { sheetVisible = false },
            autoImport = true,
            importScope = importScope,
            sheetVisible = sheetVisible,
            // 开始导入：仅收起面板 + 显示全屏遮罩（不卸载本 composable），
            // 否则 importScope 随组合销毁被取消，导入静默失败。
            onImportStart = { isImporting = true; sheetVisible = false },
            onResult = {
                isImporting = false
                if (it != null) {
                    successMsg.value = it
                } else {
                    // 无结果文案 = 读取/识别/解析失败（错误对话框已另行弹出）：
                    // 重新弹出面板供换源重试，避免「面板不可见但组合仍挂载」导致导入入口卡死
                    sheetVisible = true
                }
            },
            onImport = { json ->
                // 自动识别 JSON 类型并直接导入，无需手动选择/确认
                // （suspend lambda：在 importScope 内执行，勿再自起协程）
                val result = withIO { doAutoImport(json, context = context) }
                when (result) {
                    is AutoImportResult.EmptyOrUnrecognized -> {
                        context.displayErrorDialog(
                            Exception(result.reason),
                            title = context.getString(R.string.import_no_valid_config)
                        )
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
        )
    }
}

/**
 * 解析插件JSON，兼容两种格式：
 * 1. 原生格式：直接是 Plugin 对象或 JSON 数组 [{...}, {...}]
 * 2. JRead插件包格式：{"format":"jread_voice_plugin_bundle","version":1,"plugins":[...]}
 *
 * JRead插件可能包含以下形态：
 * - JS代码插件：有 code 字段，含 PluginJS.getAudio/getAudioV2（与原生格式兼容）
 * - URL模板插件：有 urlTemplate/method/headersText/bodyTemplate/responseAudioPath，无 code
 *   → 自动生成合成 PluginJS 包装器，使原生引擎可直接执行
 * - @js: 前缀：urlTemplate 以 "@js:" 开头时，后续内容作为 JS code 使用
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
                val name = pobj["name"]?.jsonPrimitive?.contentOrNull ?: ""
                val pluginId = pobj["pluginId"]?.jsonPrimitive?.contentOrNull
                    ?: pobj["id"]?.jsonPrimitive?.contentOrNull ?: ""
                val author = pobj["author"]?.jsonPrimitive?.contentOrNull ?: ""
                val iconUrl = pobj["iconUrl"]?.jsonPrimitive?.contentOrNull
                    ?: pobj["icon"]?.jsonPrimitive?.contentOrNull ?: ""
                val version = pobj["version"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                    ?: pobj["version"]?.jsonPrimitive?.intOrNull ?: 0
                val defVars = convertDefVars(
                    pobj["defVars"]?.jsonObject ?: pobj["vars"]?.jsonObject
                )
                var code = pobj["code"]?.jsonPrimitive?.contentOrNull ?: ""
                val urlTemplate = pobj["urlTemplate"]?.jsonPrimitive?.contentOrNull ?: ""
                val method = pobj["method"]?.jsonPrimitive?.contentOrNull ?: "GET"
                val headersText = pobj["headersText"]?.jsonPrimitive?.contentOrNull ?: ""
                val bodyTemplate = pobj["bodyTemplate"]?.jsonPrimitive?.contentOrNull ?: ""
                val responseAudioPath = pobj["responseAudioPath"]?.jsonPrimitive?.contentOrNull ?: ""

                // @js: 前缀：urlTemplate 以 "@js:" 开头时，后续内容作为 JS code
                if (code.isBlank() && urlTemplate.startsWith("@js:")) {
                    code = urlTemplate.substringAfter("@js:").trimStart()
                }

                // URL模板插件：无 code 但有 urlTemplate → 生成合成 PluginJS 包装器
                if (code.isBlank() && urlTemplate.isNotBlank()) {
                    code = generateUrlTemplatePluginCode(
                        name = name,
                        pluginId = pluginId,
                        author = author,
                        iconUrl = iconUrl,
                        version = version,
                        urlTemplate = urlTemplate,
                        method = method,
                        headersText = headersText,
                        bodyTemplate = bodyTemplate,
                        responseAudioPath = responseAudioPath,
                        defVars = defVars,
                    )
                } else if (code.isNotBlank()) {
                    // JS 代码插件：前置 JRead 运行时兼容垫层（http/Buffer/console/atob/fs 等），
                    // 全部带 typeof 守卫，不影响插件自身同名定义
                    code = JREAD_COMPAT_SHIM + "\n" + code
                }

                Plugin(
                    name = name,
                    pluginId = pluginId,
                    author = author,
                    iconUrl = iconUrl,
                    code = code,
                    version = version,
                    // enabled/isEnabled 可能是 JSON 布尔（JRead 用 optBoolean），也可能是字符串
                    isEnabled = pobj["enabled"]?.jsonPrimitive?.booleanOrNull
                        ?: pobj["isEnabled"]?.jsonPrimitive?.booleanOrNull
                        ?: pobj["enabled"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
                        ?: pobj["isEnabled"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
                        ?: false,
                    order = pobj["order"]?.jsonPrimitive?.intOrNull ?: 0,
                    defVars = defVars,
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
 * 转换 defVars，兼容两种结构：
 * 1. 原生嵌套格式：{key: {label: "显示名", hint: "提示", ...}}
 * 2. JRead扁平格式：{key: 默认值字符串}（JRead jsonObjectToMap 返回 Map<String,String>，
 *    运行时直接作为变量表使用，无 label/hint 元数据）
 *
 * 扁平格式统一转换为原生嵌套结构：{key: {label: key, default: 值}}，
 * 保证 PluginVarsBottomSheet 能正常展示编辑。
 */
private fun convertDefVars(defVarsObj: JsonObject?): Map<String, Map<String, String>> {
    if (defVarsObj == null) return emptyMap()
    val result = mutableMapOf<String, Map<String, String>>()
    for ((key, value) in defVarsObj) {
        try {
            when {
                // 嵌套对象：原生/JRead富格式
                value is JsonObject -> {
                    val converted = mutableMapOf<String, String>()
                    for ((k, v) in value) {
                        val content = v.jsonPrimitive.contentOrNull ?: ""
                        // name → label（JRead富格式用 name 作为显示名，原生用 label）
                        if (k == "name") converted["label"] = content
                        else converted[k] = content
                    }
                    result[key] = converted
                }
                // 扁平标量：JRead 简单格式，值为默认值
                else -> {
                    val defaultValue = value.jsonPrimitive.contentOrNull ?: ""
                    result[key] = mapOf("label" to key, "default" to defaultValue)
                }
            }
        } catch (_: Exception) {
            // 跳过格式不对的项
        }
    }
    return result
}

/**
 * JRead 插件运行时兼容垫层，导入 JS 代码插件时前置到插件代码。
 *
 * 背景：JRead（基于阅读 legado 的语音分支）给插件暴露的运行时面与 mumu 高度同源
 * （同为 PluginJS.getAudio 约定、ttsrv 对象、Rhino 引擎），但存在以下差异：
 * - JRead 的 http 门面响应是 SimpleResponse（code()/body().string()/text()/header()），
 *   而 mumu 全局 http 返回 NativeResponse（status 属性/text()/json()）
 * - JRead 有 Buffer/atob/btoa/globalThis/window/console 守卫等全局，mumu 缺失部分
 * - JRead 把 java.lang.Thread 重写为调度器调用（preparePluginCode），避免阻塞宿主线程
 *
 * 垫层全部带 typeof 守卫且不覆盖插件自身定义；响应适配器缓存首次消费结果，
 * 解决 okhttp ResponseBody 单次消费限制。
 */
private const val JREAD_COMPAT_SHIM = """
if (typeof globalThis === 'undefined') { var globalThis = this; }
if (typeof window === 'undefined') { var window = globalThis; }
if (typeof self === 'undefined') { var self = globalThis; }
if (typeof global === 'undefined') { var global = globalThis; }
if (typeof console === 'undefined') {
    var console = {
        log: function() {}, info: function() {}, warn: function() {},
        error: function() {}, debug: function() {}, trace: function() {}
    };
}
if (typeof logger === 'undefined') {
    var logger = {
        log: function() {}, i: function() {}, d: function() {}, w: function() {},
        e: function() {}, info: function() {}, debug: function() {}, warn: function() {},
        error: function() {}
    };
}
if (typeof atob === 'undefined') {
    var atob = function(v) {
        return new java.lang.String(ttsrv.base64DecodeToBytes(String(v)), 'UTF-8');
    };
}
if (typeof btoa === 'undefined') {
    var btoa = function(v) {
        return String(ttsrv.base64Encode(String(v)));
    };
}
if (typeof Buffer === 'undefined') {
    var Buffer = {
        from: function(value, encoding) {
            var enc = String(encoding || 'utf8').toLowerCase();
            var raw = value;
            try { if (raw && typeof raw.unwrap === 'function') raw = raw.unwrap(); } catch (e) {}
            if (enc === 'base64') return ttsrv.base64DecodeToBytes(String(raw));
            if (enc === 'hex') {
                var hx = String(raw);
                var s = '';
                for (var hi = 0; hi + 1 < hx.length; hi += 2)
                    s += String.fromCharCode(parseInt(hx.substring(hi, hi + 2), 16));
                return new java.lang.String(s).getBytes('ISO-8859-1');
            }
            if (enc === 'ascii') return new java.lang.String(String(raw)).getBytes('US-ASCII');
            if (enc === 'utf8' || enc === 'utf-8')
                return new java.lang.String(String(raw)).getBytes('UTF-8');
            return new java.lang.String(String(raw)).getBytes(enc.toUpperCase());
        }
    };
}
if (typeof startMaoxiangTimeoutCheck === 'undefined') {
    var startMaoxiangTimeoutCheck = function() {};
}
if (typeof __jreadThread === 'undefined') {
    var __jreadThread = function(runnable) {
        return { start: function() { try { runnable.run(); } catch (e) {} } };
    };
}
if (typeof __jreadThreadSleep === 'undefined') {
    var __jreadThreadSleep = function(ms) { java.lang.Thread.sleep(ms); };
}
if (typeof Websocket === 'undefined' && typeof WebSocket !== 'undefined') {
    var Websocket = WebSocket;
}
if (typeof Object.keys !== 'function') {
    Object.keys = function(obj) {
        var out = [];
        for (var key in obj)
            if (Object.prototype.hasOwnProperty.call(obj, key)) out.push(key);
        return out;
    };
}
if (typeof Object.values !== 'function') {
    Object.values = function(obj) {
        var ks = Object.keys(obj), out = [];
        for (var i = 0; i < ks.length; i++) out.push(obj[ks[i]]);
        return out;
    };
}
if (typeof Object.entries !== 'function') {
    Object.entries = function(obj) {
        var ks = Object.keys(obj), out = [];
        for (var i = 0; i < ks.length; i++) out.push([ks[i], obj[ks[i]]]);
        return out;
    };
}
function __jreadAbToJBytes(ab) {
    if (ab == null) return null;
    try {
        var src = ab;
        if (src.buffer && src.byteLength !== undefined) src = src.buffer;
        var u8 = null;
        try { u8 = new Uint8Array(src); } catch (e) {}
        if (u8 != null && u8.length !== undefined) {
            var s = '', CH = 8192;
            for (var i = 0; i < u8.length; i += CH) {
                var end = (i + CH > u8.length) ? u8.length : i + CH;
                s += String.fromCharCode.apply(null, Array.prototype.slice.call(u8, i, end));
            }
            return new java.lang.String(s).getBytes('ISO-8859-1');
        }
    } catch (e) {}
    return ab;
}
function __jreadAdaptResponse(resp) {
    if (resp == null || resp.__jreadAdapted) return resp;
    if (typeof resp.code === 'function' && typeof resp.body === 'function') return resp;
    var cache = null;
    function asString() {
        if (cache == null) {
            try { cache = ['s', String(resp.text())]; }
            catch (e) { cache = ['b', __jreadAbToJBytes(resp.bytes())]; }
        }
        return cache[0] === 's'
            ? cache[1]
            : new java.lang.String(cache[1], 'ISO-8859-1');
    }
    function asBytes() {
        if (cache == null) {
            try { cache = ['b', __jreadAbToJBytes(resp.bytes())]; }
            catch (e) { cache = ['s', String(resp.text())]; }
        }
        return cache[0] === 'b'
            ? cache[1]
            : new java.lang.String(String(cache[1])).getBytes('ISO-8859-1');
    }
    var hdrs = {};
    try {
        var h0 = resp.headers;
        if (typeof h0 === 'function') h0 = h0();
        if (h0 && typeof h0 === 'object') {
            for (var hk in h0) hdrs[hk] = h0[hk];
        }
    } catch (e) {}
    var stCode = 0;
    try {
        var sv = resp.status;
        stCode = Number(typeof sv === 'function' ? sv() : sv) || 0;
    } catch (e) {}
    var stOk = false;
    try {
        var ov = resp.ok;
        stOk = typeof ov === 'function' ? !!ov() : !!ov;
    } catch (e) { stOk = stCode >= 200 && stCode < 300; }
    var stMsg = '';
    try {
        var mv = resp.statusText;
        stMsg = String(typeof mv === 'function' ? mv() : (mv || ''));
    } catch (e) {}
    var adapted = {
        __jreadAdapted: true,
        code: function() { return stCode; },
        status: function() { return stCode; },
        message: function() { return stMsg; },
        statusText: stMsg,
        ok: stOk,
        isSuccessful: function() { return stOk; },
        headers: function() { return hdrs; },
        header: function(name, def) {
            var want = String(name).toLowerCase();
            for (var k in hdrs)
                if (String(k).toLowerCase() === want) return String(hdrs[k]);
            return def;
        },
        text: function() { return String(asString()); },
        string: function() { return this.text(); },
        json: function() { return JSON.parse(this.text()); },
        bytes: function() { return asBytes(); },
        contentType: function() {
            var ct = this.header('Content-Type', '');
            return ct ? ct.split(';')[0].replace(/^\s+|\s+${'$'}/g, '') : '';
        },
        body: function() {
            var slf = adapted;
            return {
                string: function() { return slf.text(); },
                text: function() { return slf.text(); },
                bytes: function() { return slf.bytes(); },
                byteStream: function() {
                    return new java.io.ByteArrayInputStream(slf.bytes());
                },
                contentLength: function() { return slf.bytes().length; },
                contentType: function() { return slf.contentType(); }
            };
        }
    };
    return adapted;
}
function __jreadMaybeJson(body) {
    try {
        if (body != null && typeof body === 'object' &&
            body.getClass === undefined && !(body instanceof Date)) {
            return JSON.stringify(body);
        }
    } catch (e) {}
    return body;
}
try {
    // 宿主 ttsrv 是 Java 对象，部分 Rhino 求值上下文对其做属性探测
    // （如 "typeof ttsrv !== 'undefined' && ttsrv.httpPost"）会抛
    // "TtsEngineContext 类型的 JavaScript 值无效"；用纯 JS 门面遮蔽，
    // 常用 API 转发到宿主对象，插件侧只接触普通对象。
    var __origTtsrv = typeof ttsrv !== 'undefined' ? ttsrv : null;
    if (__origTtsrv) {
        var __jreadTtsrvFacade = { __jreadWrapped: true };
        var __fwd = function(name) {
            return function() {
                var fn = __origTtsrv[name];
                if (fn == null) throw new Error('ttsrv.' + name + ' not available');
                try {
                    if (typeof fn.call === 'function') return fn.call(__origTtsrv);
                    return fn.apply(__origTtsrv, arguments);
                } catch (err) {
                    if (typeof fn === 'function') return fn.apply(null, arguments);
                    throw err;
                }
            };
        };
        for (var __tn in {
            base64Encode: 1, base64Decode: 1, base64DecodeToBytes: 1,
            httpGet: 1, httpPost: 1, userVars: 1, tts: 1, defVars: 1,
            getAudioByTag: 1, getVoiceByTag: 1, getVoiceNamesByTags: 1,
            getSpeechRuleList: 1, runSpeechRule: 1, deleteConfigByTag: 1,
            updateConfigDisplayName: 1, jsEncrypt: 1, jsCrypto: 1, fs: 1
        }) {
            try { if (__origTtsrv[__tn] !== undefined) __jreadTtsrvFacade[__tn] = __fwd(__tn); } catch (e) {}
        }
        var ttsrv = __jreadTtsrvFacade;
    }
} catch (e) {}
try {
    // 注意：全局 http 是 READONLY 属性，不能用赋值覆盖；
    // 用 var 在当前求值作用域声明同名变量遮蔽原型链上的全局定义
    var __origHttp = http;
    if (__origHttp && !__origHttp.__jreadWrapped) {
        var http = {
            __jreadWrapped: true,
            get: function(url, headers, timeoutMs) {
                // 第 3 参超时仅为兼容 JRead 签名；全局 http.get 只接受 1-2 参，
                // 超时实际由引擎层 runWithTimeout 强制执行，此处直接截断转发
                return __jreadAdaptResponse(__origHttp.get(url, headers));
            },
            post: function(url, body, headers, timeoutMs) {
                return __jreadAdaptResponse(
                    __origHttp.post(url, __jreadMaybeJson(body), headers)
                );
            }
        };
    }
} catch (e) {}
"""

/**
 * 为 JRead URL模板插件生成合成 PluginJS 包装器。
 *
 * JRead 的 URL模板插件不包含 JS code，而是通过 urlTemplate/method/headersText/bodyTemplate
 * 配置 HTTP 请求。本函数将这些配置编译成等效的 PluginJS.getAudio 实现，使原生
 * TtsPluginEngineV2 可直接执行。
 *
 * 【关键刻度转换】JRead 模板变量是倍率刻度（config.speed: Float = 1f），
 * 而 mumu 引擎传给 getAudio 的是 0-100 整数（50=1.0x，见 TtsPluginEngineV2.getAudio）。
 * 因此所有 final/base/scale 变量统一用 (入参/50) 还原为倍率字符串。
 *
 * 【与 JRead render() 对齐的行为】
 * - {text}/{voice}/{voiceTag} 等在 url、headers、body 三处均做 URL 编码（URLEncoder 风格，
 *   空格转 +），{raw*} 为原文；别名 {speed}/{volume}/{pitch} 映射到 final 值
 * - headersText 为逐行 "Key: Value" 格式（parseHeaders），支持模板变量
 * - POST 未指定 Content-Type 时默认 application/json; charset=utf-8
 * - responseAudioPath 支持 a.b.c 与数字数组下标路径；提取值为 URL 时二段下载，
 *   为 base64 时自动剥离 "data:*;base64," 前缀后解码
 * - 替换用 split/join 实现，规避 String.replace(regex) 的 $&/$1 注入问题
 *
 * 【增强】JRead 原版不支持自定义变量替换；此处额外支持 {自定义变量}：
 * 取值优先级为 ttsrv.userVars（用户在变量编辑界面填写）> defVars 默认值。
 */
@Suppress("ktlint:standard:function-naming")
private fun generateUrlTemplatePluginCode(
    name: String,
    pluginId: String,
    author: String,
    iconUrl: String,
    version: Int,
    urlTemplate: String,
    method: String,
    headersText: String,
    bodyTemplate: String,
    responseAudioPath: String,
    defVars: Map<String, Map<String, String>>,
): String {
    // 将字符串安全嵌入 JS 单引号字面量：反斜杠/单引号/各类换行符
    fun esc(s: String): String = s
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\r\n", "\\n")
        .replace("\r", "\\n")
        .replace("\n", "\\n")
        .replace("\u2028", "\\u2028")
        .replace("\u2029", "\\u2029")

    // defVars 转成 JS 字面量：引擎 eval() 会从 PluginJS.vars 回读并覆盖实体字段，
    // 缺失会导致导入时已转换的 defVars 被清空
    val varsLiteral = if (defVars.isEmpty()) "{}" else buildString {
        append("{")
        defVars.entries.forEachIndexed { i, (k, attrs) ->
            if (i > 0) append(",")
            append("'").append(esc(k)).append("':{")
            attrs.entries.forEachIndexed { j, (ak, av) ->
                if (j > 0) append(",")
                append("'").append(esc(ak)).append("':'").append(esc(av)).append("'")
            }
            append("}")
        }
        append("}")
    }

    // 自定义变量默认值字面量（扁平 key→value），供模板 {变量名} 替换兜底
    val varDefaultsLiteral = buildString {
        append("{")
        defVars.entries.forEachIndexed { i, (k, attrs) ->
            if (i > 0) append(",")
            append("'").append(esc(k)).append("':'").append(esc(attrs["default"] ?: "")).append("'")
        }
        append("}")
    }

    val m = method.uppercase().ifBlank { "GET" }

    val responseLogic = if (responseAudioPath.isNotBlank()) {
        // 从 JSON 响应中按路径提取音频数据（对象键或数组下标）
        """
        var node = JSON.parse(resp.body().string());
        var parts = '${esc(responseAudioPath.trim())}'.split('.');
        for (var i = 0; i < parts.length; i++) {
            if (node === null || node === undefined)
                throw 'responseAudioPath 未找到: ' + parts.slice(0, i + 1).join('.');
            node = node[parts[i]];
        }
        if (node === null || node === undefined) throw 'responseAudioPath 未找到';
        var audioData = String(node);
        // 提取到 URL 时二段下载；否则当作 base64（剥离 data:*;base64, 前缀）
        if (audioData.indexOf('http://') === 0 || audioData.indexOf('https://') === 0) {
            var ar = ttsrv.httpGet(audioData);
            if (ar.code() !== 200) throw '音频下载失败: HTTP-' + ar.code();
            return ar.body().byteStream();
        }
        var bi = audioData.indexOf('base64,');
        if (bi >= 0) audioData = audioData.substring(bi + 7);
        return ttsrv.base64DecodeToBytes(audioData);
        """.trimIndent()
    } else {
        "        return resp.body().byteStream();"
    }

    return """
var PluginJS = {
    name: '${esc(name)}',
    id: '${esc(pluginId)}',
    author: '${esc(author)}',
    iconUrl: '${esc(iconUrl)}',
    version: $version,
    vars: $varsLiteral,
    getAudio: function(text, locale, voice, rate, volume, pitch) {
        function enc(s) {
            s = String(s);
            return encodeURIComponent(s)
                .replace(/[!'()*]/g, function(c) {
                    return '%' + c.charCodeAt(0).toString(16).toUpperCase();
                })
                .replace(/%20/g, '+');
        }
        // mumu 0-100 刻度 → JRead 倍率字符串。
        // scale()/final() 输出两位小数（JRead formatEmotionBridgeNumber "%.2f"，如 "1.20"），
        // base() 输出普通 toString（JRead config.speed.toString()，如 "1.5"）
        function fmt(v) {
            var n = Math.round(v / 50 * 100) / 100;
            return (Math.round(n * 100) / 100).toFixed(2);
        }
        function plain(v) {
            return String(Math.round((v / 50) * 100) / 100);
        }
        function uuid() {
            return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
                var r = Math.random() * 16 | 0;
                return (c === 'x' ? r : (r & 3 | 8)).toString(16);
            });
        }
        // 模板渲染：split/join 替换，避免正则替换的 $ 特殊序列注入
        function render(s) {
            if (!s || s.indexOf('{') < 0) return s;
            var reps = [
                ['{text}', enc(text)], ['{rawText}', String(text)],
                ['{voice}', enc(voice)], ['{rawVoice}', String(voice)],
                ['{voiceTag}', enc(voice)], ['{rawVoiceTag}', String(voice)],
                ['{roleName}', enc(voice)], ['{rawRoleName}', String(voice)],
                ['{emotion}', ''], ['{rawEmotion}', ''],
                ['{emotionPrompt}', ''], ['{rawEmotionPrompt}', ''],
                ['{effectText}', ''], ['{rawEffectText}', ''],
                ['{vScale}', fmt(volume)], ['{rawVScale}', fmt(volume)],
                ['{volumeScale}', fmt(volume)], ['{rawVolumeScale}', fmt(volume)],
                ['{speedScale}', fmt(rate)], ['{rawSpeedScale}', fmt(rate)],
                ['{pitchScale}', fmt(pitch)], ['{rawPitchScale}', fmt(pitch)],
                ['{finalVolume}', fmt(volume)], ['{rawFinalVolume}', fmt(volume)],
                ['{finalSpeed}', fmt(rate)], ['{rawFinalSpeed}', fmt(rate)],
                ['{finalPitch}', fmt(pitch)], ['{rawFinalPitch}', fmt(pitch)],
                ['{baseVolume}', plain(volume)], ['{rawBaseVolume}', plain(volume)],
                ['{baseSpeed}', plain(rate)], ['{rawBaseSpeed}', plain(rate)],
                ['{basePitch}', plain(pitch)], ['{rawBasePitch}', plain(pitch)],
                ['{volume}', fmt(volume)], ['{speed}', fmt(rate)], ['{pitch}', fmt(pitch)],
                ['{requestId}', uuid()]
            ];
            for (var i = 0; i < reps.length; i++) {
                s = s.split(reps[i][0]).join(reps[i][1]);
            }
            // 自定义变量：优先用户填写值（ttsrv.userVars），兜底 defVars 默认值
            var defaults = $varDefaultsLiteral;
            var userVars = null;
            try { userVars = ttsrv.userVars || null; } catch (e) {}
            // 兼容两种容器：ttsrv.userVars 是 Java Map（用 get），defaults 是原生 JS 对象
            function lookup(map, k) {
                if (!map) return undefined;
                try {
                    if (typeof map.get === 'function') {
                        var v = map.get(k);
                        if (v !== undefined && v !== null) return String(v);
                    }
                } catch (e) {}
                try {
                    if (Object.prototype.hasOwnProperty.call(map, k)) return String(map[k]);
                } catch (e) {}
                return undefined;
            }
            var remain = s.match(/\{[^{}]+\}/g);
            for (var j = 0; j < (remain ? remain.length : 0); j++) {
                var token = remain[j];
                var name = token.substring(1, token.length - 1);
                var known = false;
                for (var r = 0; r < reps.length; r++) {
                    if (reps[r][0] === token) { known = true; break; }
                }
                if (known) continue;
                var val_ = lookup(userVars, name);
                if (val_ === undefined) val_ = defaults[name];
                // 自定义变量原样插入：多为 API Key 等配置值，编码会破坏其语义
                if (val_ !== undefined && val_ !== '')
                    s = s.split(token).join(String(val_));
            }
            return s;
        }
        var method = '$m';
        var url = render('${esc(urlTemplate)}');
        // JRead headersText 为逐行 Key: Value 格式，值支持模板变量（先渲染再解析）
        var headers = {};
        render('${esc(headersText)}').split('\n').forEach(function(line) {
            var i = line.indexOf(':');
            if (i > 0) {
                var k = line.substring(0, i).trim();
                var v = line.substring(i + 1).trim();
                if (k && v) headers[k] = v;
            }
        });
        var hasCT = false;
        for (var hk in headers) {
            if (hk.toLowerCase() === 'content-type') { hasCT = true; break; }
        }
        if (method === 'POST' && !hasCT)
            headers['Content-Type'] = 'application/json; charset=utf-8';
        var body = render('${esc(bodyTemplate)}');
        var resp = method === 'POST'
            ? ttsrv.httpPost(url, body, headers)
            : ttsrv.httpGet(url, headers);
        if (resp.code() !== 200) throw 'HTTP Error: ' + resp.code();
$responseLogic
    }
};
""".trimIndent()
}

/**
 * 将 mumu Plugin 列表导出为 JRead 插件包格式 JSON。
 *
 * 字段结构与 JRead 官方 pluginToJson() 对齐：
 * - defVars 必须是扁平 {key: 默认值}（JRead jsonObjectToMap 用 optString 读取，
 *   嵌套对象会被读成整串 JSON 文本，破坏变量替换）
 * - version 为字符串；enabled 为布尔；code 直接放 code 字段（JRead 兼容）
 */
internal fun toJReadBundleJson(plugins: List<Plugin>, includeUserVars: Boolean): String {
    val json = AppConst.jsonBuilder
    val pluginsArray = kotlinx.serialization.json.buildJsonArray {
        for (p in plugins) {
            add(kotlinx.serialization.json.buildJsonObject {
                put("id", JsonPrimitive(p.pluginId.ifBlank { p.id.toString() }))
                put("name", JsonPrimitive(p.name))
                put("pluginId", JsonPrimitive(p.pluginId))
                put("pluginGroupId", JsonPrimitive(""))
                put("pluginGroupName", JsonPrimitive(""))
                put("author", JsonPrimitive(p.author))
                put("version", JsonPrimitive(p.version.toString()))
                put("streaming", kotlinx.serialization.json.buildJsonObject { })
                put("iconUrl", JsonPrimitive(p.iconUrl))
                put("code", JsonPrimitive(p.code))
                // 扁平结构：{key: 默认值}，默认值取自嵌套属性的 default 键
                put("defVars", kotlinx.serialization.json.buildJsonObject {
                    for ((key, vars) in p.defVars) {
                        put(key, JsonPrimitive(vars["default"] ?: ""))
                    }
                })
                put("userVars", if (includeUserVars) {
                    kotlinx.serialization.json.buildJsonObject {
                        for ((k, v) in p.userVars) put(k, JsonPrimitive(v))
                    }
                } else {
                    kotlinx.serialization.json.buildJsonObject { }
                })
                put("method", JsonPrimitive("GET"))
                put("urlTemplate", JsonPrimitive(""))
                put("headersText", JsonPrimitive(""))
                put("bodyTemplate", JsonPrimitive(""))
                put("responseAudioPath", JsonPrimitive(""))
                put("enabled", JsonPrimitive(p.isEnabled))
            })
        }
    }
    val bundle = kotlinx.serialization.json.buildJsonObject {
        put("format", JsonPrimitive("jread_voice_plugin_bundle"))
        put("version", JsonPrimitive(1))
        put("plugins", pluginsArray)
    }
    return json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), bundle)
}