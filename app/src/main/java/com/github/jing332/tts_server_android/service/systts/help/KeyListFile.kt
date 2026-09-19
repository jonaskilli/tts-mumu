package com.github.jing332.tts_server_android.service.systts.help

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 密钥管理本地文件通道（与角色管理 v10 同目录同格式，互通）：
 *  - key_list.json：有序数组 [["名字",{keyCode,value}]]，value = 网址@@模型@@Key（纯 Key = 直连）
 *  - key_list.backup.json / miyue.txt / gengxin.txt / miyue_backup.txt / api_center.json
 * 数据格式、URL 归一、测试、拉模型逻辑照插件 getOpenAiBaseUrl / testModelKey 实现。
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
    /** current = 导出时「当前生效」那条的值；插件时代的老文件没有此字段，读出来是空串 */
    data class ExportData(
        val keys: List<KeyEntry>,
        val interfaces: List<ApiInterface>,
        val current: String = "",
    )

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

/** 条目名去重：只按名字占位加序号（名字须唯一，key_list.json 是「名字→值」映射） */
    fun dedupName(base: String, existing: Set<String>): String {
        if (base !in existing) return base
        var n = 2
        while ("$base$n" in existing) n++
        return "$base$n"
    }

/** 同组判定（值口径）：归一化网址 + 密钥都相等（判据同 keyBelongsTo）；直连条目同属「直连」桶 */
    fun sameGroupValue(a: String, b: String): Boolean {
        val pa = parseKeyValue(a) ?: return false
        val pb = parseKeyValue(b) ?: return false
        if (pa.isDirect || pb.isDirect) return pa.isDirect && pb.isDirect
        return sameApiSite(pa.url, pb.url) && pa.key == pb.key
    }

    // ==================== 显示名 / 组内去重 / 分组自愈 ====================

/** 显示名：@@ 条目取值里的真实模型名（值才是消费端真源，条目名只是标签）；直连回落条目名 */
    fun displayName(entry: KeyEntry): String {
        val p = parseKeyValue(entry.value) ?: return entry.name
        return if (!p.isDirect && p.model.isNotBlank()) p.model else entry.name
    }

/** 该接口下是否已有同（站点 && 密钥 && 模型）的条目——组内去重 */
    fun hasModel(keys: List<KeyEntry>, ifc: ApiInterface, model: String): Boolean =
        keys.any { e ->
            val p = parseKeyValue(e.value) ?: return@any false
            !p.isDirect && p.model == model && p.key == ifc.apiKey.trim() && sameApiSite(p.url, ifc.baseUrl)
        }

/** 接口名去重：重名追加 2、3…（不带分隔符，与条目名去重区分） */
    fun uniqueIfcName(base: String, existing: Set<String>): String {
        if (base !in existing) return base
        var n = 2
        while ("$base$n" in existing) n++
        return "$base$n"
    }

/** 主机名通用前缀（放哪都没信息量）：命中就顺延到下一段 */
    private val HOST_NOISE = setOf(
        "www", "api", "open", "aip", "chat", "ai", "llm", "studio", "endpoints", "inference",
        "server", "service", "services", "gateway", "proxy", "relay", "panel", "admin",
        "dashboard", "portal", "console", "beta", "dev", "test", "one", "new", "my",
    )

/** api 家族（api / apis / apix / api2…） */
    private val API_LIKE = Regex("^api[a-z]?\\d*$")

/** 这段有没有信息量（判断用）：通用词本体 / 通用词+数字 / 通用词打头的复合段 */
    private fun isNoiseLabel(label: String): Boolean {
        val l = label.lowercase()
        val bare = l.trimEnd { it.isDigit() }
        if (l in HOST_NOISE || API_LIKE.matches(l)) return true
        if (bare != l && (bare in HOST_NOISE || API_LIKE.matches(bare))) return true
        val head = l.substringBefore('-').substringBefore('_')
        if (head == l) return false
        return head in HOST_NOISE || API_LIKE.matches(head) || API_LIKE.matches(head.trimEnd { it.isDigit() })
    }

/**
 * 组名短名：接口地址掐头去尾、只留关键段。
 *  - 掐头：去协议 + 顺延跳过通用前缀（www. / api. / open.…），最多到「只剩一段 + 后缀」
 *  - 去尾：丢域名后缀与路径（/api/v1）；段内两端挂通用碎块也掐（spark-api-open → spark）
 *  - 纯 IP / localhost 整份保留（带端口）；剔除 @；解析不出返回空串
 *  - 例：cavoti.com → cavoti；openrouter.ai/api/v1 → openrouter；xiaoqun.lyzm.xyz/v1 → xiaoqun
 * ⚠️ 启发式：组名只是标签，取偏了界面上改名即可，不影响朗读链。
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
        if (isIp || labels.size == 1) return (host + if (port > 0) ":$port" else "").replace("@", "").trim()
        // 掐头：通用词前缀顺延，但至少给「域名 + 后缀」留两段
        var i = 0
        while (i < labels.size - 2 && isNoiseLabel(labels[i])) i++
        var one = labels[i]
        // 去尾（碎块裁剪）：两端挂着通用碎块就掐掉，全掐没了就保留原样
        val parts = one.split('-', '_')
        if (parts.size > 1) {
            var lo = 0
            var hi = parts.lastIndex
            while (lo < hi && isNoiseLabel(parts[lo])) lo++
            while (hi > lo && isNoiseLabel(parts[hi])) hi--
            if ((lo > 0 || hi < parts.lastIndex) && parts.subList(lo, hi + 1).any { it.isNotBlank() }) {
                one = parts.subList(lo, hi + 1).joinToString("-")
            }
        }
        return one.replace("@", "").trim()
    }

/** 自动分组名：网址短名（见 shortName）；解析不出才回落「接口N」（取第一个空位） */
    private fun autoIfcName(url: String, existing: Set<String>): String {
        val s = shortName(url)
        if (s.isNotBlank()) return uniqueIfcName(s, existing)
        var n = existing.size + 1
        while ("接口$n" in existing) n++
        return "接口$n"
    }

/**
 * 分组自愈：匹配不上任何接口的 @@ 条目 → 按（归一化网址 + 密钥）建接口（短名，重复加序号），
 * 模型名登记进 models；已匹配但 models 缺该模型也补上，纯 Key 直连不参与。有变化才落盘。
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

/**
 * 手动往分组加一个模型：直接落库（建密钥条目 + 登记 iface.models）。
 * ⚠️ 与拉取弹窗里那个「手动添加模型」不同——那个只进候选列表，还要再点「添加选中」。
 * 返回 (是否成功, 原因)：exist = 组内已有同（站点 + 密钥 + 模型）。
 */
    fun addManualModel(tagRuleId: String, ifc: ApiInterface, model: String): Pair<Boolean, String> {
        val m = model.trim()
        if (m.isEmpty()) return false to "empty"
        val keys = readKeys(tagRuleId)
        if (hasModel(keys, ifc, m)) return false to "exist"
        val used = keys.map { it.name }.toMutableSet()
        val entry = KeyEntry(
            name = dedupName(m, used),
            keyCode = nextKeyCode(keys),
            value = "${ifc.baseUrl}@@$m@@${ifc.apiKey}",
        )
        if (!saveKeys(tagRuleId, keys + entry)) return false to "save"
        addModelsToInterface(tagRuleId, ifc.name, listOf(m))
        return true to ""
    }

/**
 * 确保分组存在：按（归一化网址 + 密钥）找组，找到返回、没找到用短名新建并落盘。
 * 判据与 heal 同一套 ⇒ 不会建出两个同网址同密钥的分组；网址/密钥为空或落盘失败返回 null。
 */
    fun ensureGroup(tagRuleId: String, url: String, apiKey: String, preferredName: String = ""): ApiInterface? {
        val u = url.trim()
        val k = apiKey.trim()
        if (u.isEmpty() || k.isEmpty()) return null
        val ifaces = readInterfaces(tagRuleId)
        ifaces.firstOrNull { sameApiSite(it.baseUrl, u) && it.apiKey.trim() == k }?.let { return it }
        // 手填分组名优先（撞名 UI 已拦，这里再兜一次 uniqueIfcName）；留空 = 网址短名
        val nm = preferredName.trim()
        val ifc = ApiInterface(
            name = if (nm.isEmpty()) autoIfcName(u, ifaces.map { it.name }.toSet())
            else uniqueIfcName(nm, ifaces.map { it.name }.toSet()),
            baseUrl = openAiBaseUrl(u),
            apiKey = k,
            models = emptyList(),
        )
        return if (saveInterfaces(tagRuleId, ifaces + ifc)) ifc else null
    }

    // ==================== 页面 UI 状态（折叠记忆）====================

    private fun uiStateFile(tagRuleId: String) = File(dir(tagRuleId), "key_ui_state.json")

    /** 密钥管理页折叠的分组名集合（进页面读一次；文件缺失/损坏返回空集） */
    fun readCollapsedGroups(tagRuleId: String): Set<String> = try {
        val f = uiStateFile(tagRuleId)
        if (!f.exists()) emptySet()
        else {
            val obj = JSONObject(f.readText())
            val arr = obj.optJSONArray("collapsedGroups") ?: return emptySet()
            (0 until arr.length()).mapNotNull { i -> arr.optString(i).takeIf { it.isNotEmpty() } }.toSet()
        }
    } catch (e: Exception) {
        Log.w(TAG, "readCollapsedGroups failed: ${e.message}")
        emptySet()
    }

    /** 保存折叠的分组名。组名即唯一键：组改名/删除后留下的过期项，下次覆盖保存时自然清掉 */
    fun saveCollapsedGroups(tagRuleId: String, titles: Set<String>): Boolean = try {
        val d = dir(tagRuleId)
        if (!d.exists()) d.mkdirs()
        val obj = JSONObject()
        val arr = JSONArray()
        titles.sorted().forEach { arr.put(it) }
        obj.put("collapsedGroups", arr)
        uiStateFile(tagRuleId).writeText(obj.toString(2))
        true
    } catch (e: Exception) {
        Log.w(TAG, "saveCollapsedGroups failed: ${e.message}")
        false
    }

    // ==================== 启用池 / 当前密钥（miyue/gengxin/backup 三写）====================

/** 直连裸 Key 入池时补的默认模型（与朗读规则 DualKeyManager 的 defaultConfig.model 同源） */
    const val DIRECT_MODEL = "glm-4-flash"

/**
 * 启用池条目归一化：@@串拆段 trim 后回拼；裸 Key 补成「@@模型@@Key」。
 * 规则按「每 3 段一组 = 地址@@模型@@Key」解析 miyue.txt，裸 Key 不归一就混进 @@串会错位成
 * 「地址」被整把丢掉；补成空地址段后两端都回落默认端点（智谱），直连与接口串可混选。
 */
    fun normalizePoolValue(value: String): String {
        val p = parseKeyValue(value) ?: return ""
        return if (p.isDirect) "@@$DIRECT_MODEL@@${p.key}" else "${p.url}@@${p.model}@@${p.key}"
    }

/**
 * miyue 原文 → 启用池（有序值列表）：每 3 段一组还原成 地址@@模型@@Key，Key 空的组跳过
 * （语义照规则 parseSingleGroup）；「##」旧双池格式**两段并入同池**——0919 规则就是这么
 * 合并消费的，只取前段会在下次保存时把别名段悄悄丢掉。结果按序去重（重复段各保留一份）。
 */
    fun parsePoolValues(raw: String): List<String> {
        val text = raw.trim()
        if (text.isEmpty()) return emptyList()
        val segments = if (text.contains("##")) text.split("##").map { it.trim() } else listOf(text)
        val out = mutableListOf<String>()
        for (scene in segments) {
            if (scene.isEmpty()) continue
            if (!scene.contains("@@")) {
                normalizePoolValue(scene).takeIf { it.isNotEmpty() }?.let { out.add(it) }
                continue
            }
            val parts = scene.split("@@")
            var i = 0
            while (i < parts.size) {
                val url = parts.getOrNull(i)?.trim().orEmpty()
                val model = parts.getOrNull(i + 1)?.trim().orEmpty()
                val key = parts.getOrNull(i + 2)?.trim().orEmpty()
                if (key.isNotEmpty()) out.add("$url@@$model@@$key")
                i += 3
            }
        }
        return out.distinct()
    }

    /** 读启用池（miyue → gengxin → backup 链） */
    fun readPool(tagRuleId: String): List<String> = parsePoolValues(readCurrentRaw(tagRuleId))

    /** 启用池落盘：值按序 @@ 连接（规则即按序轮换该池），miyue/gengxin/miyue_backup 三写不变 */
    fun savePool(tagRuleId: String, values: List<String>): Boolean =
        saveCurrentRaw(tagRuleId, values.map { normalizePoolValue(it) }.filter { it.isNotEmpty() }.joinToString("@@"))

/**
 * 当前生效密钥原值：miyue.txt → gengxin.txt → miyue_backup.txt 取第一个非空（照插件 showKeyManageDialog）。
 * ⚠️ 旧版只读 miyue.txt ⇒ 它空而 backup 有值时被误判「没有当前密钥」，被兜底覆写成第一条。
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

/** 设为当前：miyue / gengxin / miyue_backup 三写（与插件 saveKeyToLocal 同口径） */
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

/**
 * 补协议头：已带 :// 或空串原样返回；手滑的 http:/x 先修正再补（否则补成 https://http:/x）。
 * ⚠️ 只补协议头，版本段归 openAiBaseUrl。
 */
    fun withScheme(url: String): String {
        val u = url.trim()
        if (u.isEmpty() || u.contains("://")) return u
        if (u.startsWith("http:/")) return "http://" + u.substring(6)
        if (u.startsWith("https:/")) return "https://" + u.substring(7)
        return "https://$u"
    }

    fun normalizeBaseUrl(url: String): String {
        var u = withScheme(url)
        while (u.length > 1 && u.endsWith("/")) u = u.dropLast(1)
        return u
    }

/**
 * OpenAI 兼容 base（照插件 getOpenAiBaseUrl）：命中末尾端点段就剥掉直接返回，否则无版本段时补 /v1。
 * 端点清单 = 插件三个（/chat/completions、/completions、/models）+ /responses（部分中转站文档直给）。
 * ⚠️ 旧版把「剥后缀」和「补 /v1」串成一条流水线 ⇒ http://x/chat/completions 变 http://x/v1，
 *    sameApiSite 判同站被放宽，与插件的分组 / 级联删除结果对不上。
 */
    fun openAiBaseUrl(url: String): String {
        val u = normalizeBaseUrl(url)
        for (suffix in arrayOf("/chat/completions", "/completions", "/models", "/responses")) {
            if (u.endsWith(suffix)) return u.dropLast(suffix.length)
        }
        return if (!Regex("/v\\d+[a-z]*").containsMatchIn(u)) "$u/v1" else u
    }

/** 对话端点（照插件 getOpenAiChatUrl）：已是 /chat/completions 直接返回，其余在 base 后拼 */
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
 * 拉取模型清单：GET {base}/models → data[].id（兼容裸字符串 / models 字段 / 顶层数组），保持返回顺序。
 * ⚠️ 旧版用 optString(i) 取元素 ⇒ 标准 {data:[{id}]} 会把整个对象当模型名写进密钥。
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
 * 密钥通断测试（照插件 testModelKey）：@@串先打 /chat/completions 并校验响应体，失败降级重试一次；
 * /models 只作参考（多数中转站不校验密钥，任何 key 都返回 200）。纯 Key 走智谱 /models。
 * ⚠️ 旧版策略相反：先 GET /models、2xx 就判可用 ⇒ 错的密钥也显示「可用」。
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
 * 密钥是否属于某接口（照插件 keyBelongsTo）：value 网址段同站 且 key 相同；纯 Key 不归属。
 * ⚠️ 旧版丢了「key 相同」这半：同站点两把 key 时，删接口 A 会连带删掉 B 的密钥。
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
     * 导出全部密钥 + 分组 + 当前生效那条到 密钥备份_yyMMdd-HHmm.json。
     * v2 格式：{version,exportedAt,current,interfaces,keys}。
     * ⚠️ 文件名不能用 密钥导出_ 前缀：插件导入对话框扫该前缀且把顶层当数组读，会崩。
     */
    fun exportKeys(tagRuleId: String, keys: List<KeyEntry>): String? {
        val now = java.util.Date()
        // 精确到时分（09-15）：只到天时同一天导多次会互相覆盖，留不下当天多份
        val date = java.text.SimpleDateFormat("yyMMdd-HHmm", java.util.Locale.US).format(now)
        val fileName = "密钥备份_$date.json"
        return try {
            val root = JSONObject()
            root.put("version", 2)
            root.put(
                "exportedAt",
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.US).format(now)
            )
            // 当前生效那条也带上：否则导入后页面兜底（当前无匹配 ⇒ 启用第一条）会把它切到第一条
            root.put("current", readCurrentRaw(tagRuleId))
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

    /**
     * 找现存导出/备份文件（密钥导出_* 插件时代 + 密钥备份_* 本版），按名内日期倒序 = 最新在前。
     * ⚠️ 不能直接按名字倒序：中文「导」>「备」⇒ 老 密钥导出_* 永远压在 密钥备份_* 上面，
     * 用户点第一条以为是最新备份、实际是最老的。改为抽名字里的 日期[时分] 当排序键。
     */
    fun listExportFiles(tagRuleId: String): List<String> = try {
        dir(tagRuleId).listFiles()
            ?.filter {
                it.name.endsWith(".json") &&
                    (it.name.startsWith("密钥导出_") || it.name.startsWith("密钥备份_"))
            }
            ?.sortedByDescending { exportSortKey(it.name) }
            ?.map { it.name } ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    /** 排序键 = 名内 6 位日期 + 4 位时分（缺时分补 0000）；抽不出的返回空串 ⇒ 排最后 */
    private fun exportSortKey(name: String): String {
        val m = Regex("(\\d{6})(?:-(\\d{4}))?").find(name) ?: return ""
        return m.groupValues[1] + m.groupValues[2].ifEmpty { "0000" }
    }

/** 读导出/备份文件：顶层是数组 = 插件时代扁平格式，是对象 = 本版 v2；损坏返回 null */
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
                ExportData(
                    root.optJSONArray("keys")?.let { parseKeyPairs(it) } ?: emptyList(),
                    ifcs,
                    root.optString("current"),
                )
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

/** 导入合并（照插件 doImport）：重名跳过；返回 (新增数, 跳过数)。不写备份（增量合并） */
    fun importKeys(tagRuleId: String, incoming: List<KeyEntry>): Pair<Int, Int> {
        val existing = readKeys(tagRuleId)
        val nameSet = existing.map { it.name }.toMutableSet()
        var added = 0
        var skipped = 0
        val merged = existing.toMutableList()
        incoming.forEach { item ->
            // 照插件 doImport：空名 / 空 value 的条目直接忽略，不计入统计
            if (item.name.isBlank() || item.value.isBlank()) {
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
 * 导入（v2 含分组）：密钥走 importKeys 的合并口径（重名跳过）；接口按（站点 + 密钥）去重合并、
 * models 取并集；名字冲突自动加序号；「当前生效」按备份恢复（见 restoreCurrent）。
 * 返回 (新增密钥, 跳过密钥, 新增接口)。
 */
    fun importAll(tagRuleId: String, data: ExportData): Triple<Int, Int, Int> {
        val (added, skipped) = importKeys(tagRuleId, data.keys)
        restoreCurrent(tagRuleId, data.current)
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
 * 恢复备份里的「当前生效」：现口径 miyue 存的是启用池（可多把），只在目标当前**全空**时
 * 才整串顶上（不抢占现有启用集），且备份池里至少一把能在导入后的列表里对上号。
 * ⚠️ 页面加载有兜底「miyue 全空 ⇒ 启用第一条」，不在这里先恢复的话备份的启用集会被它顶掉。
 */
    private fun restoreCurrent(tagRuleId: String, fromBackup: String) {
        val incoming = parsePoolValues(fromBackup)
        if (incoming.isEmpty()) return
        if (readCurrentRaw(tagRuleId).isNotBlank()) return
        val norms = readKeys(tagRuleId).map { normalizePoolValue(it.value) }.toSet()
        if (incoming.none { it in norms }) return
        saveCurrentRaw(tagRuleId, incoming.joinToString("@@"))
    }

/**
 * 模型分类（照插件 classifyModel 五类词表）：embed/rerank → 向量、image/diffusion 系 → 图像、
 * video/sora 系 → 视频、audio/tts 系 → 音频，其余文本。
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
