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
    data class KeyEntry(val name: String, val keyCode: String, val value: String)

    /** 接口（接口中心条目） */
    data class ApiInterface(
        val name: String,
        val baseUrl: String,
        val apiKey: String,
        val models: List<String>,
    )

    data class ParsedKey(val isDirect: Boolean, val url: String, val model: String, val key: String)

    /** 导出/备份数据的解析结果：keys 两版通用；interfaces 只有 v2 有 */
    data class ExportData(val keys: List<KeyEntry>, val interfaces: List<ApiInterface>)

    private fun dir(tagRuleId: String) = File(BASE_DIR, tagRuleId)
    private fun keyFile(tagRuleId: String) = File(dir(tagRuleId), "key_list.json")
    private fun keyBackupFile(tagRuleId: String) = File(dir(tagRuleId), "key_list.backup.json")
    private fun centerFile(tagRuleId: String) = File(dir(tagRuleId), "api_center.json")

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

    // ==================== 显示名 / 组内去重 / 分组自愈 ====================

    /**
     * 显示名（目目 09-15「同模型跨组共存」）：@@ 条目取**值里的真实模型名**——
     * 条目名可能被 uniqueKeyName 加过 `@组名` 去重后缀，但值里的模型名永远是原始的，
     * 所以显示层不必去猜后缀，直接读值即可（插件让名字当标签，值才是消费端真源）。
     * 直连条目（纯 Key）没有模型名，回落到条目名。
     */
    fun displayName(entry: KeyEntry): String {
        val p = parseKeyValue(entry.value) ?: return entry.name
        return if (!p.isDirect && p.model.isNotBlank()) p.model else entry.name
    }

    /** 该接口下是否已有同（站点 && 密钥 && 模型）的条目——组内去重用（拉两次同一模型不再多出一条） */
    fun hasModel(keys: List<KeyEntry>, ifc: ApiInterface, model: String): Boolean =
        keys.any { e ->
            val p = parseKeyValue(e.value) ?: return@any false
            !p.isDirect && p.model == model && p.key == ifc.apiKey.trim() && sameApiSite(p.url, ifc.baseUrl)
        }

    /** 接口名去重：重名追加 2、3…（不带 @，免得和条目名 uniqueKeyName 的形态混淆） */
    fun uniqueIfcName(base: String, existing: Set<String>): String {
        if (base !in existing) return base
        var n = 2
        while ("$base$n" in existing) n++
        return "$base$n"
    }

    /**
     * 组名短名（目目 09-15 定）：接口地址**掐头去尾、只留关键段**。
     *  - 掐头：去掉协议，以及 `www.` / `api.`（可带数字，如 `api2.`）这类没信息量的前缀；
     *  - 去尾：只取主机名的**第一段** ⇒ 域名后缀（.com/.xyz）与路径（/api/v1、/v1）全丢；
     *  - 例：`https://cavoti.com` → `cavoti`；`https://openrouter.ai/api/v1` → `openrouter`；
     *        `https://xiaoqun.lyzm.xyz/v1` → `xiaoqun`
     *        （同站点下按子域区分，比 `lyzm` 好认；也避免同一家不同账号撞名）；
     *  - 纯 IP / 无点主机（localhost）没有「第一段」可言 ⇒ 整份保留并带端口（端口就是身份）；
     *  - 剔除 `@`：`@` 是条目名「模型@组名」的分隔符，不许混进组名；解析不出来返回空串。
     */
    fun shortName(url: String): String {
        var body = normalizeBaseUrl(url)
        if (body.isBlank()) return ""
        // 手粘的 @@ 串常常不带协议头，补一个才能解析出 host
        if (!body.contains("://")) body = "https://$body"
        val host: String
        val port: Int
        try {
            val u = java.net.URI(body)
            host = u.host.orEmpty()
            port = u.port
        } catch (e: Exception) {
            return ""
        }
        if (host.isBlank()) return ""
        val labels = host.split('.').filter { it.isNotEmpty() }
        if (labels.isEmpty()) return ""
        val isIp = labels.size == 4 && labels.all { v -> val n = v.toIntOrNull(); n != null && n in 0..255 }
        val one = when {
            isIp || labels.size == 1 -> host + if (port > 0) ":$port" else ""
            labels.size >= 3 && Regex("^(www|api)\\d*$", RegexOption.IGNORE_CASE).matches(labels[0]) -> labels[1]
            else -> labels[0]
        }
        return one.replace("@", "").trim()
    }

    /** 自动分组名：网址短名（口径见 [shortName]）；网址解析不出来才回落「接口N」（N 取第一个空位，不撞号） */
    private fun autoIfcName(url: String, existing: Set<String>): String {
        val s = shortName(url)
        if (s.isNotBlank()) return uniqueIfcName(s, existing)
        var n = existing.size + 1
        while ("接口$n" in existing) n++
        return "接口$n"
    }

    /**
     * 分组自愈（目目 09-15 ④「未分组自动收编」；照插件 ensureApiCenter 的聚合口径升级）：
     *  - 匹配不上任何接口的 @@ 条目（首次迁移 / 刚导入 / 刚手加一个 @@ 密钥）→ 按
     *    （归一化网址 + 密钥）自动建一个新接口，名字取网址**短名**（口径见 [shortName]，重复加序号），模型名登记进 models；
     *  - 已匹配到接口、但 models 里缺这个模型名 → 补上（models 是管理视图，供分组展示/导出复原）。
     * **纯 Key 直连条目不参与聚合**（照插件：直连留独立一组）。有变化才落盘。
     */
    fun heal(tagRuleId: String): Boolean {
        val keys = readKeys(tagRuleId)
        if (keys.isEmpty()) return false
        var ifaces = readInterfaces(tagRuleId)
        var changed = false
        val names = ifaces.map { it.name }.toMutableSet()
        keys.forEach { e ->
            val p = parseKeyValue(e.value) ?: return@forEach
            if (p.isDirect) return@forEach
            val url = p.url.trim()
            val key = p.key.trim()
            if (url.isEmpty() || key.isEmpty()) return@forEach
            val hit = ifaces.firstOrNull { sameApiSite(it.baseUrl, url) && it.apiKey.trim() == key }
            if (hit == null) {
                val nm = autoIfcName(url, names)
                names.add(nm)
                ifaces = ifaces + ApiInterface(
                    name = nm,
                    baseUrl = openAiBaseUrl(url),
                    apiKey = key,
                    models = if (p.model.isBlank()) emptyList() else listOf(p.model),
                )
                changed = true
            } else if (p.model.isNotBlank() && p.model !in hit.models) {
                ifaces = ifaces.map { if (it.name == hit.name) it.copy(models = it.models + p.model) else it }
                changed = true
            }
        }
        if (changed) saveInterfaces(tagRuleId, ifaces)
        return changed
    }

    /** 拉取模型后把模型名登记进接口的 models（去重）；返回是否发生写入 */
    fun addModelsToInterface(tagRuleId: String, ifcName: String, models: List<String>): Boolean {
        if (ifcName.isBlank() || models.isEmpty()) return false
        val ifaces = readInterfaces(tagRuleId)
        val hit = ifaces.firstOrNull { it.name == ifcName } ?: return false
        val merged = hit.models.toMutableList()
        var changed = false
        models.forEach { if (it !in merged) { merged.add(it); changed = true } }
        if (!changed) return false
        return saveInterfaces(tagRuleId, ifaces.map { if (it.name == ifcName) it.copy(models = merged) else it })
    }

    // ==================== 当前密钥（miyue/gengxin/backup 三写）====================

    /**
     * 当前生效密钥的原始值（照插件 showKeyManageDialog：miyue.txt → gengxin.txt → miyue_backup.txt
     * 依次找**第一个非空**）。旧版只读 miyue.txt：它空而 backup 有值时会被误判"没有当前密钥"，
     * 进而被下面的兜底自动覆写成列表第一条（用户看到"当前密钥自己跳了"）。
     */
    fun readCurrentRaw(tagRuleId: String): String {
        val d = File(BASE_DIR, tagRuleId)
        for (n in arrayOf("miyue.txt", "gengxin.txt", "miyue_backup.txt")) {
            val v = try {
                File(d, n).readText().trim()
            } catch (e: Exception) {
                ""
            }
            if (v.isNotEmpty()) return v
        }
        return ""
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

    /**
     * OpenAI 兼容 base（照插件 getOpenAiBaseUrl 1:1）：命中末尾端点段就**剥掉后直接返回**、
     * 不再补 /v1；只有地址本身不带端点段时，才走「无版本段（/v1、/v4…）自动补 /v1」。
     *
     * ⚠️ 旧版把两个分支串成一条流水线：先剥后缀、再统一补 /v1 ⇒ `http://x/chat/completions`
     * 得到 `http://x/v1`（原版是 `http://x`），`sameApiSite` 的判同站口径因此被放宽，
     * 与插件侧的密钥分组 / 级联删除结果对不上。
     */
    fun openAiBaseUrl(url: String): String {
        val u = normalizeBaseUrl(url)
        for (suffix in arrayOf("/chat/completions", "/completions", "/models")) {
            if (u.endsWith(suffix)) return u.dropLast(suffix.length)
        }
        return if (!Regex("/v\\d+[a-z]*").containsMatchIn(u)) "$u/v1" else u
    }

    /**
     * 对话端点（照插件 getOpenAiChatUrl）：已是 /chat/completions 直接返回，
     * 其余在补过版本段的 base 后拼 /chat/completions。
     * （插件对不带版本段的裸地址走的是 `u + "/chat/completions"`、不补 /v1；这里统一走 base，
     *   比原版自洽——原版那种写法在 "http://x/models" 上会拼成 "/models/chat/completions"。）
     */
    private fun openAiChatUrl(url: String): String {
        val u = normalizeBaseUrl(url)
        if (u.endsWith("/chat/completions")) return u
        return openAiBaseUrl(u) + "/chat/completions"
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

    /**
     * 拉取模型清单（照插件）：GET {base}/models → 取 `data[].id`（兼容 data 里的裸字符串 /
     * `models` 字段 / 顶层数组）。保持接口返回顺序（插件不排序）。
     *
     * ⚠️ 旧版用 `JSONArray.optString(i)` 取元素：标准 `{"data":[{"id":"gpt-4o"}]}` 下 optString
     * 会把**整个对象 toString** 当模型名 ⇒ 写进密钥后那段 value 直接作废。
     */
    fun fetchModels(baseUrl: String, apiKey: String): Pair<List<String>?, String> {
        val resp = httpJson(openAiBaseUrl(baseUrl) + "/models", "GET", apiKey, null)
        if (!resp.ok) return null to "HTTP ${resp.code}，${briefBody(resp.body)}"
        return parseModelList(resp.body) to ""
    }

    /** 从 /models 响应体取模型名（data[].id / 裸字符串 / models 字段 / 顶层数组） */
    private fun parseModelList(body: String): List<String> {
        val out = mutableListOf<String>()
        fun collect(arr: JSONArray?) {
            if (arr == null) return
            for (i in 0 until arr.length()) {
                when (val v = arr.opt(i)) {
                    is JSONObject -> v.optString("id").trim().takeIf { it.isNotEmpty() }?.let { out.add(it) }
                    is String -> v.trim().takeIf { it.isNotEmpty() }?.let { out.add(it) }
                }
            }
        }
        try {
            val o = JSONObject(body)
            collect(o.optJSONArray("data") ?: o.optJSONArray("models"))
        } catch (e: Exception) {
            runCatching { collect(JSONArray(body)) }
        }
        return out
    }

    /** 测试目标（照插件 parseKeyForTest 的返回形状） */
    private class TestTarget(
        val isDirect: Boolean,           // true = 纯 Key（走智谱 /models）
        val baseUrl: String,
        val chatUrl: String,
        val model: String,
        val apiKey: String,
    )

    /** 测试前解析密钥值（照插件 parseKeyForTest）：@@串→openai；纯 key→智谱分支；非法给出原因 */
    private fun parseForTest(raw: String): Pair<TestTarget?, String> {
        val text = raw.trim()
        if (text.isEmpty()) return null to "密钥内容为空"
        val parts = text.split("@@")
        if (parts.size >= 3) {
            val apiKey = parts.drop(2).joinToString("@@").trim()
            val rawUrl = normalizeBaseUrl(parts[0].trim())
            val base = openAiBaseUrl(rawUrl)
            val chat = openAiChatUrl(rawUrl)
            val model = parts[1].trim()
            if (base.isEmpty()) return null to "URL 不能为空"
            if (!base.startsWith("http://") && !base.startsWith("https://")) {
                return null to "URL 必须以 http:// 或 https:// 开头"
            }
            if (model.isEmpty()) return null to "模型名不能为空"
            if (apiKey.isEmpty()) return null to "API Key 不能为空"
            return TestTarget(false, base, chat, model, apiKey) to ""
        }
        if (parts.size > 1) return null to "格式不完整，应为 URL@@模型名@@API Key"
        return TestTarget(true, "", "", "", text) to ""
    }

    /** 构造对话请求体（照插件：只回复 pong + max_tokens 16 + temperature 0 + stream false） */
    private fun chatPayload(model: String, content: String, maxTokens: Int?, temperature: Int?): String =
        JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", content)))
            if (maxTokens != null) put("max_tokens", maxTokens)
            if (temperature != null) put("temperature", temperature)
            put("stream", false)
        }.toString()

    /** 对话响应业务校验（照插件 chatReplyOk）：HTTP 2xx ≠ 可用——带 error 一律否 */
    private fun chatReplyOk(body: String): Boolean = try {
        val j = JSONObject(body)
        when {
            j.has("error") && !j.isNull("error") -> false
            (j.optJSONArray("choices")?.length() ?: 0) > 0 -> true
            else -> j.has("content") || j.has("output") || j.has("data")
        }
    } catch (e: Exception) {
        false
    }

    /** 模型清单业务校验（照插件 modelListOk）：data/models/顶层是数组才算通 */
    private fun modelListOk(body: String): Boolean = try {
        val j = JSONObject(body)
        if (j.has("error") && !j.isNull("error")) false
        else j.optJSONArray("data") != null || j.optJSONArray("models") != null
    } catch (e: Exception) {
        runCatching { JSONArray(body); true }.getOrDefault(false)
    }

    /**
     * 密钥通断测试（照插件 testModelKey）：
     * - @@串（openai）：**先打真实对话端点** `/chat/completions` 并校验响应体；失败再降级重试一次
     *   （部分推理模型不接受 max_tokens/temperature）；`/models` 只在失败后附作**参考**——
     *   插件注释写得很明白：多数中转站 /models 不校验密钥、任何 key 都返回 200，不能说明可用。
     * - 纯 Key：走智谱 `https://open.bigmodel.cn/api/paas/v4/models`（旧版直接拒测 ⇒ 直连条目没有检验入口）。
     *
     * ⚠️ 旧版策略是反的：先 GET /models、`resp.ok` 就判成功 ⇒ 错的密钥也显示"可用"。
     * 返回 (是否可用, 简述)。
     */
    fun testKey(rawValue: String): Pair<Boolean, String> {
        val (t, err) = parseForTest(rawValue)
        if (t == null) return false to err
        if (t.isDirect) {
            val t0 = System.currentTimeMillis()
            val r = httpJson("https://open.bigmodel.cn/api/paas/v4/models", "GET", t.apiKey, null)
            if (r.ok && modelListOk(r.body)) {
                return true to "智谱 /models 验证成功，${System.currentTimeMillis() - t0}ms"
            }
            if (r.code == 401 || r.code == 403) return false to "密钥无效或无权限（HTTP ${r.code}）"
            return false to "智谱 /models 验证失败：HTTP ${r.code}，${briefBody(r.body)}"
        }
        val t0 = System.currentTimeMillis()
        var resp = httpJson(t.chatUrl, "POST", t.apiKey, chatPayload(t.model, "只回复 pong", 16, 0))
        if (!(resp.ok && chatReplyOk(resp.body))) {
            val retry = httpJson(t.chatUrl, "POST", t.apiKey, chatPayload(t.model, "ping", null, null))
            if (retry.ok && chatReplyOk(retry.body)) resp = retry
        }
        if (resp.ok && chatReplyOk(resp.body)) {
            return true to "测试成功，${System.currentTimeMillis() - t0}ms"
        }
        val msg = when {
            resp.ok -> "接口返回异常：HTTP 状态正常但内容不是有效的对话响应。HTTP ${resp.code}，${briefBody(resp.body)}"
            resp.code == 401 || resp.code == 403 -> "密钥无效或无权限（HTTP ${resp.code}）：${briefBody(resp.body)}"
            resp.code == 404 -> "对话端点不存在（HTTP 404），请检查接口地址结尾/模型名：${briefBody(resp.body)}"
            resp.code == 400 -> "请求被拒绝（HTTP 400，常见原因：模型名不存在或参数不支持）：${briefBody(resp.body)}"
            else -> "chat/completions 验证失败：HTTP ${resp.code}，${briefBody(resp.body)}"
        }
        val models = httpJson(t.baseUrl + "/models", "GET", t.apiKey, null)
        return false to (
            if (models.ok) "$msg\n(参考：/models 可访问——该端点多数站点不校验密钥，不能说明密钥可用)"
            else msg
            )
    }

    // ==================== 1:1 复刻补充（对照 角色管理v10 插件函数）====================

    /** 同站判断（照插件 sameApiSite：两边 base（剥端点、补版本段）一致算同站；异常回退原文比较） */
    fun sameApiSite(a: String, b: String): Boolean = try {
        openAiBaseUrl(a) == openAiBaseUrl(b)
    } catch (e: Exception) {
        normalizeBaseUrl(a) == normalizeBaseUrl(b)
    }

    /**
     * 密钥是否属于某接口（照插件 keyBelongsTo：value 的网址段**同站** **且 key 相同**；
     * 纯 Key=直连不归属）。
     *
     * ⚠️ 旧版把 `&& key 相同` 这一半丢了：同一站点下挂两把不同 key 时，
     * 删除接口 A 会级联把属于 B 的密钥一起删掉，分组也会把 B 归进 A。
     */
    fun keyBelongsTo(entry: KeyEntry, ifc: ApiInterface): Boolean {
        val p = parseKeyValue(entry.value) ?: return false
        if (p.isDirect || p.url.isEmpty()) return false
        return sameApiSite(p.url, ifc.baseUrl) && p.key == ifc.apiKey.trim()
    }

    /** 删除接口连同其下所有密钥条目（照插件接口表单🗑）；返回 (新密钥表, 删除的密钥数) */
    fun deleteInterfaceCascade(tagRuleId: String, ifc: ApiInterface, keys: List<KeyEntry>): Pair<List<KeyEntry>, Int> {
        val kept = keys.filter { !keyBelongsTo(it, ifc) }
        val removed = keys.size - kept.size
        val ifaces = readInterfaces(tagRuleId).filter { it.name != ifc.name }
        saveInterfaces(tagRuleId, ifaces)
        return kept to removed
    }

    /**
     * 导出全部密钥 + 分组到 密钥备份_yyMMdd.json（目目 09-15 ③「导出一个文件，导入后能复原」）。
     * v2 格式：{version, exportedAt, interfaces:[{name,baseUrl,apiKey,models}], keys:[[名,{keyCode,value}]]}
     * ⚠️ 文件名**不能**用 `密钥导出_`：插件自己的导入对话框会扫该前缀、并把顶层当**数组**读，
     * 撞上我们的对象结构会崩。故分两个前缀；导入侧两个都认（插件时代的老文件照样能导进来）。
     */
    fun exportKeys(tagRuleId: String, keys: List<KeyEntry>): String? {
        val now = java.util.Date()
        val date = java.text.SimpleDateFormat("yyMMdd", java.util.Locale.US).format(now)
        val fileName = "密钥备份_$date.json"
        return try {
            val root = JSONObject()
            root.put("version", 2)
            root.put(
                "exportedAt",
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.US).format(now)
            )
            val ifcArr = JSONArray()
            readInterfaces(tagRuleId).forEach { ifc ->
                val o = JSONObject()
                o.put("name", ifc.name)
                o.put("baseUrl", ifc.baseUrl)
                o.put("apiKey", ifc.apiKey)
                val ma = JSONArray()
                ifc.models.forEach { ma.put(it) }
                o.put("models", ma)
                ifcArr.put(o)
            }
            root.put("interfaces", ifcArr)
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
            root.put("keys", arr)
            val d = dir(tagRuleId)
            if (!d.exists()) d.mkdirs()
            File(d, fileName).writeText(root.toString(2))
            fileName
        } catch (e: Exception) {
            Log.w(TAG, "exportKeys failed: ${e.message}")
            null
        }
    }

    /** 找现存导出/备份文件（密钥导出_* 插件时代 + 密钥备份_* 本版，按名倒序=新在前） */
    fun listExportFiles(tagRuleId: String): List<String> = try {
        dir(tagRuleId).listFiles()
            ?.filter {
                it.name.endsWith(".json") &&
                    (it.name.startsWith("密钥导出_") || it.name.startsWith("密钥备份_"))
            }
            ?.map { it.name }?.sortedDescending() ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    /**
     * 读导出/备份文件；顶层是**数组** = 插件时代扁平格式，是**对象** = 本版 v2 含分组。
     * 损坏返回 null。
     */
    fun readExportFile(tagRuleId: String, fileName: String): ExportData? = try {
        val f = File(dir(tagRuleId), fileName)
        if (!f.exists()) null
        else {
            val text = f.readText().trim()
            if (text.startsWith("[")) {
                ExportData(parseKeyPairs(JSONArray(text)), emptyList())
            } else {
                val root = JSONObject(text)
                val ifcs = mutableListOf<ApiInterface>()
                root.optJSONArray("interfaces")?.let { ia ->
                    for (i in 0 until ia.length()) {
                        val o = ia.optJSONObject(i) ?: continue
                        val models = mutableListOf<String>()
                        o.optJSONArray("models")?.let { ma ->
                            for (j in 0 until ma.length()) {
                                ma.optString(j).takeIf { t -> t.isNotEmpty() }?.let { models.add(it) }
                            }
                        }
                        ifcs.add(
                            ApiInterface(
                                name = o.optString("name"),
                                baseUrl = o.optString("baseUrl"),
                                apiKey = o.optString("apiKey"),
                                models = models,
                            )
                        )
                    }
                }
                ExportData(root.optJSONArray("keys")?.let { parseKeyPairs(it) } ?: emptyList(), ifcs)
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "readExportFile failed: ${e.message}")
        null
    }

    /** [[名字,{keyCode,value}],...] → 条目列表（空名跳过；空 value 交给导入侧过滤） */
    private fun parseKeyPairs(arr: JSONArray): List<KeyEntry> {
        val out = mutableListOf<KeyEntry>()
        for (i in 0 until arr.length()) {
            val pair = arr.optJSONArray(i) ?: continue
            val name = pair.optString(0).trim()
            val obj = pair.optJSONObject(1) ?: continue
            if (name.isNotEmpty()) out.add(KeyEntry(name, obj.optString("keyCode"), obj.optString("value")))
        }
        return out
    }

    /**
     * 导入合并（照插件 doImport）：重名跳过；返回 (新增数, 跳过数)。
     * 注意：与 saveKeys 不同，此操作**不写备份**（导入是增量合并）。
     */
    fun importKeys(tagRuleId: String, incoming: List<KeyEntry>): Pair<Int, Int> {
        val existing = readKeys(tagRuleId)
        val nameSet = existing.map { it.name }.toMutableSet()
        var added = 0
        var skipped = 0
        val merged = existing.toMutableList()
        incoming.forEach { item ->
            // 照插件 doImport：`name && item.value` 为真才处理——空名/空 value 的条目直接忽略
            // （旧版空 value 也入库，导入出一堆点不动的空密钥）
            if (item.name.isBlank() || item.value.isBlank()) {
                // 忽略，不计入新增也不计入跳过（与插件一致）
            } else if (item.name in nameSet) {
                skipped++
            } else {
                nameSet.add(item.name)
                merged.add(item.copy(keyCode = item.keyCode.ifEmpty { nextKeyCode(merged) }))
                added++
            }
        }
        return if (saveKeys(tagRuleId, merged)) added to skipped else 0 to incoming.size
    }

    /**
     * 导入（v2 含分组）：密钥走 importKeys 的合并口径（重名跳过）；
     * 接口按（站点 + 密钥）去重合并、models 取并集；名字冲突自动加序号。
     * 返回 (新增密钥, 跳过密钥, 新增接口)。
     */
    fun importAll(tagRuleId: String, data: ExportData): Triple<Int, Int, Int> {
        val (added, skipped) = importKeys(tagRuleId, data.keys)
        if (data.interfaces.isEmpty()) return Triple(added, skipped, 0)
        val existing = readInterfaces(tagRuleId)
        val names = existing.map { it.name }.toMutableSet()
        val merged = existing.toMutableList()
        var addedIfc = 0
        var changed = false
        data.interfaces.forEach { inc ->
            val hit = merged.firstOrNull {
                sameApiSite(it.baseUrl, inc.baseUrl) && it.apiKey.trim() == inc.apiKey.trim()
            }
            if (hit == null) {
                val nm = uniqueIfcName(inc.name.ifBlank { autoIfcName(inc.baseUrl, names) }, names)
                names.add(nm)
                merged.add(inc.copy(name = nm))
                addedIfc++
                changed = true
            } else if (inc.models.isNotEmpty()) {
                val mm = hit.models.toMutableList()
                var grew = false
                inc.models.forEach { if (it !in mm) { mm.add(it); grew = true } }
                if (grew) {
                    val idx = merged.indexOfFirst { it.name == hit.name }
                    if (idx >= 0) {
                        merged[idx] = hit.copy(models = mm)
                        changed = true
                    }
                }
            }
        }
        if (changed) saveInterfaces(tagRuleId, merged)
        return Triple(added, skipped, addedIfc)
    }

    /**
     * 模型分类（照插件 classifyModel 的五类词表，逐项对齐——旧版自造的
     * `bge/draw/paint/stable/sd/runway` 让 rerank、midjourney 等落进"文本"，
     * 而插件词表里的 `babbage/rerank/diffusion/cogview/kolors/janus/vidu/cogvideo/transcri/cosyvoice` 全缺）。
     */
    fun classifyModel(modelName: String): String {
        val n = modelName.lowercase()
        fun has(vararg ks: String) = ks.any { it in n }
        return when {
            has("embed", "babbage", "rerank") -> "向量"
            has("dall-e", "image", "diffusion", "midjourney", "flux", "sdxl", "sd3", "cogview", "kolors", "janus") -> "图像"
            has("video", "sora", "kling", "vidu", "cogvideo") -> "视频"
            has("whisper", "tts", "audio", "speech", "voice", "asr", "transcri", "cosyvoice") -> "音频"
            else -> "文本"
        }
    }
}
