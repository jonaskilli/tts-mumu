package com.github.jing332.tts_server_android.service.systts.help

import android.util.Log
import com.github.jing332.tts_server_android.constant.AppConst
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 账号池（10-06 立项，任务书：临时文件/任务书-账号池-20261006.md）。
 *
 * 管理 CodeBuddy 账号：登录（POST auth/state?platform=ide 取 authUrl+state → WebView 浏览器
 * 登录 → GET auth/token?state= 轮询换凭据）、续期（POST auth/token/refresh + X-Refresh-Token，
 * refresh_token 可能被服务端轮换，返回什么就必须存回什么）、每日签到领积分（/v2/billing/meter 系端点）。
 * 凭据存 app 私有外部存储，不进备份白名单（凭据跟着账号走，恢复后重登）。
 *
 * 协议底数 = 2026-10-07 真实上游实测（权威 = 临时文件/codebuddy.js 试水版，五项全通；
 * 源头 = DSH 插件 buddy-adapter/credits 源码逐字段照抄）。⚠️ 10-06 反推版两条旧底数已推翻：
 *  - auth/token 是 **GET ?state=** 轮询（旧版当成 POST，且没接住 state）；
 *  - state 响应的登录地址在 **data.authUrl**（旧版找顶层 url，必报「响应无登录地址」）。
 * 另：billing 头族（X-Domain/X-Product/X-Product-Code/UA）不能省——实测只有 Bearer 时
 * 对话 HTTP 200 但零内容；积分余量是资源包合计（Accounts[] 求和，含小数，用 Double 存）。
 * 续期接口未实测（refresh token 可能旋转，打废要重登），成功存回即兜底。
 *
 * 存储惯例跟 KeyListFile：org.json + File 直读写。文件 = AppConst.externalFilesDir/
 * account_pool/state.json（敏感凭据不落 Download/chajian 公共目录）。
 */
object AccountPool {
    private const val TAG = "AccountPool"

    // CodeBuddy 上游根（对话/鉴权同源）
    const val UPSTREAM_BASE = "https://copilot.tencent.com"

    // ==================== 协议路径（10-07 实测定版，热更时与 codebuddy.js 的 CB 常量同步） ====================
    private const val PATH_AUTH_STATE = "/v2/plugin/auth/state?platform=ide"        // POST → data.state + data.authUrl
    private const val PATH_AUTH_TOKEN = "/v2/plugin/auth/token?state="              // GET ?state=xxx 轮询
    private const val PATH_AUTH_REFRESH = "/v2/plugin/auth/token/refresh"           // POST + X-Refresh-Token
    private const val PATH_CHECKIN = "/v2/billing/meter/daily-checkin"              // POST {}，重复领取=HTTP400+业务码
    private const val PATH_CHECKIN_STATUS = "/v2/billing/meter/checkin-activity-status" // POST {}，今日已签/连续天数
    private const val PATH_CREDITS = "/v2/billing/meter/get-user-resource"          // POST {}，Accounts[] 求和

    // billing 头族（credits.js checkinHeaders 权威形状；Accept/Content-Type 由 httpJson 统一带）
    const val API_DOMAIN = "copilot.tencent.com"
    const val CLIENT_VERSION = "1.106.1"

    // 登录链路头族（buddy-oauth.js fetchAuthState/loopGetToken 逐字段照抄）：
    // X-No-Authorization 声明免登录请求；X-No-User-Id/Enterprise-Id/Department-Info
    // 声明不带组织信息；网关认 UA 里的产品身份（10-08 补——此前裸发，真机登录轮询未过）
    private val authStateHeaders: Map<String, String> = mapOf(
        "X-Domain" to API_DOMAIN,
        "X-No-Authorization" to "true",
        "X-No-User-Id" to "true",
        "X-No-Enterprise-Id" to "true",
        "X-No-Department-Info" to "true",
        "User-Agent" to "CodeBuddyIDE/$CLIENT_VERSION",
    )

    private val dir: File
        get() = File(AppConst.externalFilesDir, "account_pool")
    private val stateFile: File
        get() = File(dir, "state.json")

    // ==================== 数据 ====================

    data class Account(
        val id: String,
        val provider: String = "codebuddy",
        val nickname: String,
        val accessToken: String,
        val refreshToken: String,
        // access_token 过期时刻（epoch ms；0=未知）
        val expiresAt: Long = 0L,
        // 最近一次积分查询结果（0=未知）。余额=资源包合计，实测含小数（如 3930.73），Double 保真
        val credits: Double = 0.0,
        // 最近一次成功签到时刻（epoch ms；0=从未）
        val lastCheckinAt: Long = 0L,
        val enabled: Boolean = true,
        val createdAt: Long = System.currentTimeMillis(),
    ) {
        /** access_token 是否已过期（留 10 分钟余量；未知不算过期） */
        fun isExpired(now: Long = System.currentTimeMillis()): Boolean =
            expiresAt in 1..(now + 10 * 60_000L)
    }

    /** 签到状态（checkin-activity-status 响应） */
    data class CheckinStatus(
        val active: Boolean,
        val todayCheckedIn: Boolean,
        val streakDays: Int,
        val dailyCredit: Long,
    )

    fun load(): List<Account> = try {
        if (!stateFile.exists()) emptyList()
        else {
            val arr = JSONObject(stateFile.readText()).optJSONArray("accounts") ?: return emptyList()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                Account(
                    id = o.optString("id"),
                    provider = o.optString("provider", "codebuddy"),
                    nickname = o.optString("nickname"),
                    accessToken = o.optString("accessToken"),
                    refreshToken = o.optString("refreshToken"),
                    expiresAt = o.optLong("expiresAt"),
                    credits = o.optDouble("credits", 0.0),
                    lastCheckinAt = o.optLong("lastCheckinAt"),
                    enabled = o.optBoolean("enabled", true),
                    createdAt = o.optLong("createdAt"),
                )
            }.filter { it.id.isNotEmpty() && it.accessToken.isNotEmpty() }
        }
    } catch (e: Exception) {
        Log.w(TAG, "load failed: ${e.message}")
        emptyList()
    }

    fun save(accounts: List<Account>): Boolean = try {
        if (!dir.exists()) dir.mkdirs()
        val root = JSONObject()
        val arr = JSONArray()
        accounts.forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("provider", a.provider)
                put("nickname", a.nickname)
                put("accessToken", a.accessToken)
                put("refreshToken", a.refreshToken)
                put("expiresAt", a.expiresAt)
                put("credits", a.credits)
                put("lastCheckinAt", a.lastCheckinAt)
                put("enabled", a.enabled)
                put("createdAt", a.createdAt)
            })
        }
        root.put("accounts", arr)
        stateFile.writeText(root.toString(2))
        true
    } catch (e: Exception) {
        Log.w(TAG, "save failed: ${e.message}")
        false
    }

    // ==================== HTTP ====================

    private data class HttpResp(val ok: Boolean, val code: Int, val body: String)

    private fun httpJson(url: String, method: String, headers: Map<String, String>, body: String?): HttpResp {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/json")
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
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

    /** billing 头族（签到/状态/余额；实测不能只有 Bearer）。Accept/Content-Type 由 httpJson 统一带。 */
    private fun billingHeaders(token: String): Map<String, String> = mapOf(
        "Authorization" to "Bearer $token",
        "X-Domain" to API_DOMAIN,
        "X-Product" to "SaaS",
        "X-Product-Code" to "codebuddy",
        "User-Agent" to "CodeBuddyIDE/$CLIENT_VERSION",
    )

    /** CodeBuddy 上游域名——密钥/测试/朗读链按它识别「这是 CodeBuddy 站点」自动带头族 */
    const val CHAT_HOST = "copilot.tencent.com"

    /**
     * 对话头族（codebuddy.js cb_chatHeaders 权威形状，10-07 PC 实测五项全通的那份）：
     * 裸 Bearer 对话 HTTP 200 但 SSE 零内容，必须带全。X-Product 实源发 "SaaS" 归属。
     * Authorization/Accept/Content-Type 由调用方自定，这里只给身份族。
     */
    fun chatHeaders(): Map<String, String> = mapOf(
        "X-Domain" to API_DOMAIN,
        "X-Product" to "SaaS",
        "X-Product-Code" to "codebuddy",
        "X-Agent-Purpose" to "conversation",
        "X-IDE-Name" to "CodeBuddy",
        "X-IDE-Type" to "CodeBuddy",
        "X-IDE-Version" to CLIENT_VERSION,
        "User-Agent" to "CodeBuddyIDE/$CLIENT_VERSION",
    )

    /** url 是否 CodeBuddy 上游（密钥测试/朗读链自动识别用） */
    fun isChatHost(url: String): Boolean = try {
        java.net.URI(url).host == CHAT_HOST
    } catch (_: Exception) {
        url.contains(CHAT_HOST)
    }

    private fun parseJson(text: String): JSONObject? = try { JSONObject(text) } catch (e: Exception) { null }

    /** 数字或数字字符串取值（CycleCapacityRemainPrecise 优先，照 codebuddy.js cb_firstNumber） */
    private fun firstNumber(o: JSONObject, vararg keys: String): Double? {
        for (k in keys) when (val v = o.opt(k)) {
            is Number -> return v.toDouble()
            is String -> if (v.isNotEmpty()) v.toDoubleOrNull()?.let { return it }
        }
        return null
    }

    private fun briefBody(body: String): String {
        val compact = body.replace(Regex("\\s+"), " ").trim()
        return if (compact.length > 180) compact.take(180) + "..." else compact.ifEmpty { "无响应内容" }
    }

    // ==================== 模型清单（10-08 接线：密钥管理「拉模型」） ====================

    // 模型解析常量（buddy.js parseModelsFromConfig 忠实精简）：
    // 过滤——非对话模型前缀 / 自动别名 / 画图标签；agent 引用优先级 cli > craft
    private val NON_CHAT_PREFIXES = arrayOf("nes-", "completion-", "codewise-")
    private val PREFERRED_AGENTS = arrayOf("cli", "craft")

    /**
     * 拉 CodeBuddy 模型清单：GET /v3/config（带对话头族+Bearer）。
     * 响应 data.{agents:[{name,models[]}], models:[{id,tags[],maxOutputTokens}]}；
     * 解析顺序照 parseModelsFromConfig：①cli/craft agent 引用的模型优先 ②data.models 其余
     * ③跳过 auto/default、nes-/completion-/codewise- 前缀、text-to-image 标签、
     * maxOutputTokens≤256（补全类）。返回 (模型 id 列表, err)；解析失败给内置兜底清单。
     */
    fun fetchModels(token: String): Pair<List<String>, String> {
        val r = httpJson("$UPSTREAM_BASE/v3/config", "GET", billingHeaders(token), null)
        if (!r.ok) return builtinModels() to "HTTP ${r.code}，用内置清单（${briefBody(r.body)}）"
        return try {
            val d = JSONObject(r.body).optJSONObject("data") ?: return builtinModels() to "响应无 data，用内置清单"
            // data.models 元数据表（过滤判据用）
            val metaById = HashMap<String, JSONObject>()
            d.optJSONArray("models")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val m = arr.optJSONObject(i) ?: continue
                    val id = m.optString("id")
                    if (id.isNotEmpty()) metaById[id] = m
                }
            }
            // agent 引用 id 集合（保序：先 preferred agent 后其余——实测 cli/craft 已覆盖，其余兜底）
            val agentIds = LinkedHashSet<String>()
            val agents = d.optJSONArray("agents")
            val preferredFirst = ArrayList<JSONArray?>()
            if (agents != null) {
                for (p in PREFERRED_AGENTS) {
                    for (i in 0 until agents.length()) {
                        val a = agents.optJSONObject(i) ?: continue
                        if (a.optString("name") == p) { preferredFirst.add(a.optJSONArray("models")); break }
                    }
                }
                for (i in 0 until agents.length()) {
                    val a = agents.optJSONObject(i) ?: continue
                    if (PREFERRED_AGENTS.any { a.optString("name") == it }) continue
                    preferredFirst.add(a.optJSONArray("models"))
                }
            }
            preferredFirst.forEach { arr ->
                if (arr != null) for (i in 0 until arr.length()) {
                    val id = arr.optString(i)
                    if (id.isNotEmpty()) agentIds.add(id)
                }
            }
            val out = LinkedHashSet<String>()
            fun push(id: String) {
                if (id == "auto" || id == "default") return
                if (NON_CHAT_PREFIXES.any { id.startsWith(it) }) return
                val meta = metaById[id]
                if (meta != null) {
                    if (meta.optBoolean("supportsExtra", false)) return
                    val maxOut = meta.optLong("maxOutputTokens", 0L)
                    if (maxOut in 1..256) return
                    val tags = meta.optJSONArray("tags")
                    if (tags != null) for (i in 0 until tags.length())
                        if (tags.optString(i) == "text-to-image") return
                }
                out.add(id)
            }
            agentIds.forEach { push(it) }
            metaById.keys.forEach { push(it) }
            if (out.isEmpty()) builtinModels() to "清单为空，用内置清单"
            else out.toList() to ""
        } catch (e: Exception) {
            builtinModels() to "解析失败用内置清单：${e.message}"
        }
    }

    /** 内置兜底清单（codebuddy.js CB.DEFAULT_MODEL 同源；清单接口未稳定，拉不到不至于无模型可选） */
    private fun builtinModels(): List<String> = listOf(
        "deepseek-v4.1-flash",
        "deepseek-v3.2",
        "glm-5.3",
        "kimi-k2.6",
    )

    // ==================== 一键添加为密钥（10-08：账号即凭据，照原插件免手填） ====================

    /**
     * 把账号直接落成密钥管理的一条密钥：网址=CodeBuddy 上游、Key=该账号 access_token、
     * 模型=清单第一个（拉不到用内置默认）。接口分组同名复用、密钥条目按（站点+钥+模型）
     * 去重——重复点不会堆重复条目。免复制粘贴、免见令牌本体（原插件「登录即用」的形态）。
     * @param tagRuleId 密钥归属规则（现两入口都传 mingwuyan）
     * @return (是否新增, 提示)；已存在= false + 说明文案
     */
    fun addAsKey(tagRuleId: String, acc: Account): Pair<Boolean, String> {
        val baseUrl = "https://$CHAT_HOST"
        val model = runCatching { fetchModels(acc.accessToken).first.first() }
            .getOrElse { builtinModels().first() }
        val ifaces = KeyListFile.readInterfaces(tagRuleId)
        val keys = KeyListFile.readKeys(tagRuleId)
        val value = "$baseUrl@@$model@@${acc.accessToken}"
        // 去重判据与分组同口径：同站点+同钥+同模型
        val dup = keys.any {
            val p = KeyListFile.parseKeyValue(it.value)
            p != null && !p.isDirect && KeyListFile.sameApiSite(p.url, baseUrl) &&
                p.key == acc.accessToken && p.model == model
        }
        if (dup) return false to "已在密钥管理（CodeBuddy / $model），无需重复添加"

        // 接口分组：同站点同名复用并把模型补进清单；没有则新建「CodeBuddy」组（带组级 Key）
        val updatedIfaces = if (ifaces.any { KeyListFile.sameApiSite(it.baseUrl, baseUrl) && it.name == "CodeBuddy" }) {
            ifaces.map {
                if (KeyListFile.sameApiSite(it.baseUrl, baseUrl) && it.name == "CodeBuddy" && model !in it.models)
                    it.copy(models = it.models + model, apiKey = acc.accessToken)
                else it
            }
        } else {
            ifaces + KeyListFile.ApiInterface(
                name = "CodeBuddy",
                baseUrl = baseUrl,
                apiKey = acc.accessToken,
                models = listOf(model),
            )
        }
        val newKeys = keys + KeyListFile.KeyEntry(
            name = KeyListFile.dedupName(model, keys.map { it.name }.toSet()),
            keyCode = KeyListFile.nextKeyCode(keys),
            value = value,
        )
        KeyListFile.saveInterfaces(tagRuleId, updatedIfaces)
        KeyListFile.saveKeys(tagRuleId, newKeys)
        return true to "已添加：CodeBuddy / $model"
    }

    // ==================== 登录 ====================

    /**
     * 登录第一步：POST auth/state?platform=ide 取浏览器登录地址。
     * 返回 (state, authUrl, err)；authUrl==null = 失败。state 必须带到 pollToken。
     */
    fun fetchLoginUrl(): Triple<String?, String?, String> {
        val r = httpJson("$UPSTREAM_BASE$PATH_AUTH_STATE", "POST", authStateHeaders, "{}")
        if (!r.ok) return Triple(null, null, "HTTP ${r.code}：${briefBody(r.body)}")
        return try {
            // 实测（10-07）：登录地址在 data.authUrl（不是顶层 url），轮询凭据用 data.state
            val d = JSONObject(r.body).optJSONObject("data")
            val state = d?.optString("state").orEmpty()
            val url = d?.optString("authUrl").orEmpty()
            if (state.isEmpty() || url.isEmpty())
                Triple(null, null, "响应缺 state/authUrl：${briefBody(r.body)}")
            else Triple(state, url, "")
        } catch (e: Exception) {
            Triple(null, null, "解析失败：${briefBody(r.body)}")
        }
    }

    /**
     * 登录第二步：浏览器登录完成后凭 state 轮询换凭据（GET auth/token?state=，10-07 实测定版）。
     * 未登录完上游返回失败，调用方隔 2s 重试即可（旧版「POST + X-Refresh-Token 当轮询」的
     * 协议已废；已登录账号的凭据恢复走 refresh）。成功即落盘。
     */
    fun pollToken(state: String?, existing: Account?): Pair<Account?, String> {
        if (state.isNullOrEmpty()) return null to "无 state，无法查询登录结果"
        val r = httpJson(
            "$UPSTREAM_BASE$PATH_AUTH_TOKEN" + URLEncoder.encode(state, "UTF-8"),
            "GET", authStateHeaders, null
        )
        if (!r.ok) return null to "HTTP ${r.code}（未登录完或已过期）：${briefBody(r.body)}"
        return try {
            val o = JSONObject(r.body).let { it.optJSONObject("data") ?: it }
            val access = o?.optString("access_token").orEmpty()
            val refresh = o?.optString("refresh_token").orEmpty().ifEmpty { existing?.refreshToken ?: "" }
            if (access.isEmpty()) return null to "响应无 access_token（可能还没登录完）：${briefBody(r.body)}"
            val expiresAt = System.currentTimeMillis() + (o?.optLong("expires_in", 0L) ?: 0L) * 1000L
            val nick = o?.optString("nickname").orEmpty()
                .ifEmpty { o?.optString("username").orEmpty() }
                .ifEmpty { existing?.nickname ?: "CodeBuddy" }
            val acc = Account(
                id = existing?.id ?: "codebuddy-${System.currentTimeMillis().toString(16)}",
                nickname = nick,
                accessToken = access,
                refreshToken = refresh,
                expiresAt = expiresAt,
                credits = existing?.credits ?: 0.0,
                lastCheckinAt = existing?.lastCheckinAt ?: 0L,
                enabled = existing?.enabled ?: true,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            )
            val list = load().filterNot { it.id == acc.id } + acc
            save(list)
            acc to ""
        } catch (e: Exception) {
            null to "解析失败：${briefBody(r.body)}"
        }
    }

    // ==================== 续期 / 签到 / 积分 ====================

    /**
     * 续期：POST auth/token/refresh + X-Refresh-Token。成功返回更新后账号（已落盘）。
     * ⚠️ 未实测：refresh token 可能旋转——响应返回什么 refresh_token 就存回什么，别沿用旧的。
     */
    fun refresh(acc: Account): Pair<Account?, String> {
        if (acc.refreshToken.isEmpty()) return null to "无 refresh_token，请重新登录"
        val r = httpJson(
            "$UPSTREAM_BASE$PATH_AUTH_REFRESH", "POST",
            mapOf("X-Refresh-Token" to acc.refreshToken), "{}"
        )
        if (!r.ok) return null to "HTTP ${r.code}：${briefBody(r.body)}"
        return try {
            val o = JSONObject(r.body).let { it.optJSONObject("data") ?: it }
            val access = o?.optString("access_token").orEmpty().ifEmpty { acc.accessToken }
            val refresh = o?.optString("refresh_token").orEmpty().ifEmpty { acc.refreshToken }
            val expiresAt = System.currentTimeMillis() + (o?.optLong("expires_in", 0L) ?: 0L) * 1000L
            val updated = acc.copy(accessToken = access, refreshToken = refresh, expiresAt = expiresAt)
            save(load().map { if (it.id == acc.id) updated else it })
            updated to ""
        } catch (e: Exception) {
            null to "解析失败：${briefBody(r.body)}"
        }
    }

    /**
     * 签到状态（active/今日已签/连续天数/每日积分）。裸调用不做自动续期——
     * 401 由调用方（checkIn 主链路）统一处理，避免两处各续一次把轮换的 refresh token 打废。
     */
    fun checkinStatus(acc: Account): Pair<CheckinStatus?, String> {
        val r = httpJson("$UPSTREAM_BASE$PATH_CHECKIN_STATUS", "POST", billingHeaders(acc.accessToken), "{}")
        if (r.code == 401) return null to "HTTP 401（登录已过期）"
        if (!r.ok) return null to "HTTP ${r.code}：${briefBody(r.body)}"
        val o = parseJson(r.body)
            ?: return null to "响应不是 JSON：${briefBody(r.body)}"
        if (o.optInt("code", -1) != 0)
            return null to "业务码 ${o.optInt("code")}：${o.optString("msg")}"
        val d = o.optJSONObject("data") ?: JSONObject()
        return CheckinStatus(
            active = d.optBoolean("active", false),
            todayCheckedIn = d.optBoolean("today_checked_in", false),
            streakDays = d.optInt("streak_days", 0),
            dailyCredit = d.optLong("daily_credit", 0L),
        ) to ""
    }

    /**
     * 每日签到（/v2/billing/meter/daily-checkin，10-07 实测定版；旧 credits/claim 端点已废）。
     * 先查状态：今日已签直接报成功，不再打必 400 的签到接口（重复领取=HTTP400+业务码，
     * 真实原因在 body 的 msg，不能只看状态码）。401 只在本链路续期重试一次。
     * 返回 (成功?, 提示)。
     */
    fun checkIn(acc: Account, retried: Boolean = false): Pair<Boolean, String> {
        val (st, _) = checkinStatus(acc)
        if (st != null && st.todayCheckedIn) return true to "今日已签到（连签 ${st.streakDays} 天）"
        val r = httpJson("$UPSTREAM_BASE$PATH_CHECKIN", "POST", billingHeaders(acc.accessToken), "{}")
        if (r.code == 401) {
            if (retried) return false to "续期后仍 401：${briefBody(r.body)}"
            val (ref, err) = refresh(acc)
            if (ref != null) return checkIn(ref, retried = true)
            return false to "登录已过期且续期失败：$err"
        }
        if (!r.ok) {
            val o = parseJson(r.body)
            val code = o?.optInt("code", -1) ?: -1
            val msg = o?.optString("msg").orEmpty()
            return false to buildString {
                append("HTTP ${r.code}")
                if (code >= 0) append(" code=$code")
                append("：").append(msg.ifEmpty { briefBody(r.body) })
            }
        }
        val o = parseJson(r.body)
        if (o != null && o.optInt("code", -1) != 0)
            return false to "业务码 ${o.optInt("code")}：${o.optString("msg").ifEmpty { "领取失败" }}"
        val updated = acc.copy(lastCheckinAt = System.currentTimeMillis())
        save(load().map { if (it.id == acc.id) updated else it })
        return true to "签到成功"
    }

    /**
     * 积分查询（get-user-resource 资源包合计；-1=失败）。成功即落盘。
     * 实测结构：data.Response.Data.Accounts[] 扁平条目，余量=各条
     * CycleCapacityRemainPrecise（缺则 CycleCapacityRemain）求和，失效包（Status=3）剔除；
     * 合计与顶层 TotalDosage 对账一致（10-07 实测 3930.73）。旧 /v2/plugin/credits 端点已废。
     */
    fun queryCredits(acc: Account, retried: Boolean = false): Pair<Double, String> {
        val r = httpJson("$UPSTREAM_BASE$PATH_CREDITS", "POST", billingHeaders(acc.accessToken), "{}")
        if (r.code == 401) {
            if (retried) return -1.0 to "续期后仍 401：${briefBody(r.body)}"
            val (ref, err) = refresh(acc)
            if (ref != null) return queryCredits(ref, retried = true)
            return -1.0 to "登录已过期且续期失败：$err"
        }
        if (!r.ok) return -1.0 to "HTTP ${r.code}：${briefBody(r.body)}"
        val o = parseJson(r.body) ?: return -1.0 to "响应不是 JSON：${briefBody(r.body)}"
        if (o.optInt("code", -1) != 0)
            return -1.0 to "业务码 ${o.optInt("code")}：${o.optString("msg")}"
        val accounts = runCatching {
            o.optJSONObject("data")
                ?.optJSONObject("Response")
                ?.optJSONObject("Data")
                ?.optJSONArray("Accounts")
        }.getOrNull()
        var total = 0.0
        var found = false
        if (accounts != null) {
            for (i in 0 until accounts.length()) {
                val p = accounts.optJSONObject(i) ?: continue
                if (p.optInt("Status", 0) == 3) continue  // 服务端标记的失效包
                val v = firstNumber(p, "CycleCapacityRemainPrecise", "CycleCapacityRemain") ?: continue
                total += v
                found = true
            }
        }
        if (!found) return -1.0 to "响应无资源包字段：${briefBody(r.body)}"
        val credits = Math.round(total * 100.0) / 100.0
        save(load().map { if (it.id == acc.id) acc.copy(credits = credits) else it })
        return credits to ""
    }
}
