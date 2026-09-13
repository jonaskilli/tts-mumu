package com.github.jing332.tts_server_android.service.systts.help

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 密钥管理本地文件通道（与角色管理 v10 同目录同格式，完全互通）：
 *  - key_list.json：有序数组 [["名字", {"keyCode":"key01","value":"网址@@模型@@Key"}], ...]
 *    （消费端唯一数据源；value 也可以是纯 Key=直连）
 *  - key_list.backup.json：写主文件前的损坏自愈备份
 *  - miyue.txt / gengxin.txt / miyue_backup.txt：当前生效密钥的原始值（朗读规则消费）
 *  - api_center.json：接口中心（管理视图）{"interfaces":[{name,baseUrl,apiKey,models:[...]}]}
 *
 * URL 归一/测试/拉模型逻辑照插件 getOpenAiBaseUrl/testModelKey 实现。
 */
object KeyListFile {
    private const val TAG = "KeyListFile"
    private const val BASE_DIR = "/storage/emulated/0/Download/chajian"

    /** 密钥条目（保序） */
    data class KeyEntry(val name: String, val keyCode: String, val value: String) {
        /** 展示用摘要：@@ 串显示 model（或网址），纯 key 显示尾 6 位 */
        fun brief(): String {
            val p = parseKeyValue(value)
            return when {
                p == null -> ""
                p.isDirect -> "••••" + p.key.takeLast(6)
                else -> p.model.ifEmpty { p.url }
            }
        }
    }

    /** 接口（接口中心条目） */
    data class ApiInterface(
        val name: String,
        val baseUrl: String,
        val apiKey: String,
        val models: List<String>,
    )

    data class ParsedKey(val isDirect: Boolean, val url: String, val model: String, val key: String)

    private fun dir(tagRuleId: String) = File(BASE_DIR, tagRuleId)
    private fun keyFile(tagRuleId: String) = File(dir(tagRuleId), "key_list.json")
    private fun keyBackupFile(tagRuleId: String) = File(dir(tagRuleId), "key_list.backup.json")
    private fun centerFile(tagRuleId: String) = File(dir(tagRuleId), "api_center.json")
    private fun currentFile(tagRuleId: String) = File(dir(tagRuleId), "miyue.txt")

    // ==================== key_list.json ====================

    /** 读全部密钥条目（保序）；主文件损坏自动回退备份 */
    fun readKeys(tagRuleId: String): List<KeyEntry> {
        for (f in arrayOf(keyFile(tagRuleId), keyBackupFile(tagRuleId))) {
            if (!f.exists()) continue
            try {
                val arr = JSONArray(f.readText())
                val out = mutableListOf<KeyEntry>()
                for (i in 0 until arr.length()) {
                    val pair = arr.optJSONArray(i) ?: continue
                    val name = pair.optString(0).trim()
                    val obj = pair.optJSONObject(1) ?: continue
                    val value = obj.optString("value")
                    if (name.isEmpty()) continue
                    out.add(KeyEntry(name, obj.optString("keyCode"), value))
                }
                if (f == keyBackupFile(tagRuleId)) Log.w(TAG, "key_list.json 损坏，已回退备份")
                return out
            } catch (e: Exception) {
                Log.w(TAG, "readKeys(${f.name}) failed: ${e.message}")
            }
        }
        return emptyList()
    }

    /** 全量保存：先留备份再写主文件（与插件 saveKeyMapToData 同顺序） */
    fun saveKeys(tagRuleId: String, keys: List<KeyEntry>): Boolean = try {
        val d = dir(tagRuleId)
        if (!d.exists()) d.mkdirs()
        val main = keyFile(tagRuleId)
        if (main.exists()) runCatching { keyBackupFile(tagRuleId).writeText(main.readText()) }
        val arr = JSONArray()
        keys.forEach { k ->
            val obj = JSONObject()
            obj.put("keyCode", k.keyCode)
            obj.put("value", k.value)
            val pair = JSONArray()
            pair.put(k.name)
            pair.put(obj)
            arr.put(pair)
        }
        main.writeText(arr.toString(2))
        true
    } catch (e: Exception) {
        Log.w(TAG, "saveKeys failed: ${e.message}")
        false
    }

    fun nextKeyCode(keys: List<KeyEntry>): String {
        val max = keys.mapNotNull { it.keyCode.removePrefix("key").toIntOrNull() }.maxOrNull() ?: 0
        val n = max + 1
        return "key" + if (n < 10) "0$n" else n.toString()
    }

    /** 重名时生成「模型@接口N」/「模型@接口N2」…（与插件 uniqueKeyName 同规则） */
    fun uniqueKeyName(base: String, ifcName: String, existing: Set<String>): String {
        if (base !in existing) return base
        val alt = "$base@$ifcName"
        if (alt !in existing) return alt
        var n = 2
        while ("$alt$n" in existing) n++
        return "$alt$n"
    }

    // ==================== 当前密钥（miyue/gengxin/backup 三写）====================

    fun readCurrentRaw(tagRuleId: String): String = try {
        currentFile(tagRuleId).readText().trim()
    } catch (e: Exception) {
        ""
    }

    /** 设为当前：miyue.txt + gengxin.txt + miyue_backup.txt 三写（与插件 saveKeyToLocal 同口径） */
    fun saveCurrentRaw(tagRuleId: String, value: String): Boolean = try {
        val d = dir(tagRuleId)
        if (!d.exists()) d.mkdirs()
        val v = value.trim()
        File(d, "miyue.txt").writeText(v)
        File(d, "gengxin.txt").writeText(v)
        File(d, "miyue_backup.txt").writeText(v)
        true
    } catch (e: Exception) {
        Log.w(TAG, "saveCurrentRaw failed: ${e.message}")
        false
    }

    // ==================== 接口中心（api_center.json）====================

    fun readInterfaces(tagRuleId: String): List<ApiInterface> = try {
        val f = centerFile(tagRuleId)
        if (!f.exists()) emptyList()
        else {
            val obj = JSONObject(f.readText())
            val arr = obj.optJSONArray("interfaces") ?: return emptyList()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val models = mutableListOf<String>()
                o.optJSONArray("models")?.let { ma ->
                    for (j in 0 until ma.length()) ma.optString(j).takeIf { it.isNotEmpty() }?.let { models.add(it) }
                }
                ApiInterface(
                    name = o.optString("name"),
                    baseUrl = o.optString("baseUrl"),
                    apiKey = o.optString("apiKey"),
                    models = models,
                )
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "readInterfaces failed: ${e.message}")
        emptyList()
    }

    fun saveInterfaces(tagRuleId: String, ifaces: List<ApiInterface>): Boolean = try {
        val d = dir(tagRuleId)
        if (!d.exists()) d.mkdirs()
        val root = JSONObject()
        val arr = JSONArray()
        ifaces.forEach { ifc ->
            val o = JSONObject()
            o.put("name", ifc.name)
            o.put("baseUrl", ifc.baseUrl)
            o.put("apiKey", ifc.apiKey)
            val ma = JSONArray()
            ifc.models.forEach { ma.put(it) }
            o.put("models", ma)
            arr.put(o)
        }
        root.put("interfaces", arr)
        centerFile(tagRuleId).writeText(root.toString(2))
        true
    } catch (e: Exception) {
        Log.w(TAG, "saveInterfaces failed: ${e.message}")
        false
    }

    /** 解析密钥值：@@串 → {url, model, key}；纯 key → 直连 */
    fun parseKeyValue(v: String): ParsedKey? {
        val text = v.trim()
        if (text.isEmpty()) return null
        val parts = text.split("@@")
        if (parts.size >= 3) {
            return ParsedKey(false, parts[0].trim(), parts[1].trim(), parts.drop(2).joinToString("@@").trim())
        }
        return ParsedKey(true, "", "", text)
    }

    // ==================== URL 归一（照插件 getOpenAiBaseUrl 系列）====================

    fun normalizeBaseUrl(url: String): String {
        var u = url.trim()
        if (u.startsWith("http:/") && !u.startsWith("http://")) u = "http://" + u.substring(6)
        if (u.startsWith("https:/") && !u.startsWith("https://")) u = "https://" + u.substring(7)
        while (u.length > 1 && u.endsWith("/")) u = u.dropLast(1)
        return u
    }

    /** OpenAI 兼容 base：剥掉末尾端点段；无版本段（/v1、/v4…）自动补 /v1 */
    fun openAiBaseUrl(url: String): String {
        var u = normalizeBaseUrl(url)
        for (suffix in arrayOf("/chat/completions", "/completions", "/models")) {
            if (u.endsWith(suffix)) {
                u = u.dropLast(suffix.length)
                break
            }
        }
        if (!Regex("/v\\d+[a-z]*").containsMatchIn(u)) u += "/v1"
        return u
    }

    private fun chatUrl(url: String): String {
        val u = normalizeBaseUrl(url)
        return if (u.endsWith("/chat/completions")) u else openAiBaseUrl(u) + "/chat/completions"
    }

    // ==================== HTTP（HttpURLConnection，与插件 httpJsonRequest 同实现）====================

    private data class HttpResp(val ok: Boolean, val code: Int, val body: String)

    private fun httpJson(url: String, method: String, apiKey: String?, body: String?): HttpResp {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/json")
                if (!apiKey.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $apiKey")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
            }
            val code = conn.responseCode
            val stream = if (code >= 400) conn.errorStream else conn.inputStream
            val content = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            HttpResp(code in 200..299, code, content)
        } catch (e: Exception) {
            HttpResp(false, -1, e.message ?: e.toString())
        } finally {
            conn?.disconnect()
        }
    }

    private fun briefBody(body: String): String {
        val compact = body.replace(Regex("\\s+"), " ").trim()
        return if (compact.length > 180) compact.take(180) + "..." else compact.ifEmpty { "无响应内容" }
    }

    /** 拉取模型清单：GET {base}/models → data[].id；失败返回 null+错误描述 */
    fun fetchModels(baseUrl: String, apiKey: String): Pair<List<String>?, String> {
        val resp = httpJson(openAiBaseUrl(baseUrl) + "/models", "GET", apiKey, null)
        if (!resp.ok) return null to "HTTP ${resp.code}，${briefBody(resp.body)}"
        return try {
            val arr = JSONObject(resp.body).optJSONArray("data")
            val models = mutableListOf<String>()
            arr?.let { for (i in 0 until it.length()) it.optString(i).takeIf { m -> m.isNotEmpty() }?.let { m -> models.add(m) } }
            models.sorted() to ""
        } catch (e: Exception) {
            null to "响应解析失败：${e.message}"
        }
    }

    /**
     * 密钥通断测试（与插件 testModelKey 同策略）：先 GET /models（不依赖模型名），
     * 失败再用 chat/completions 发一条 1 token 请求兜底；返回 成功与否+简述。
     */
    fun testKey(baseUrl: String, apiKey: String, model: String): Pair<Boolean, String> {
        val modelsResp = httpJson(openAiBaseUrl(baseUrl) + "/models", "GET", apiKey, null)
        if (modelsResp.ok) return true to "HTTP ${modelsResp.code}，模型清单可读"
        if (model.isBlank()) return false to "HTTP ${modelsResp.code}，${briefBody(modelsResp.body)}"
        val payload = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", "hi")))
            put("max_tokens", 1)
        }.toString()
        val chat = httpJson(chatUrl(baseUrl), "POST", apiKey, payload)
        return if (chat.ok) true to "HTTP ${chat.code}，对话可用"
        else false to "HTTP ${chat.code}，${briefBody(chat.body)}"
    }
}
