package com.github.jing332.tts_server_android.service.systts.help

import android.util.Log
import com.github.jing332.common.LogEntry
import com.github.jing332.common.LogLevel
import com.github.jing332.tts_server_android.SysttsLogger
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

    /** 排障日志进 App 日志页（10-08 用户令「手机直接看，不用连电脑」）：账号池登录/续期/签到关键分支走这里 */
    private fun appLog(level: Int, msg: String) {
        Log.i(TAG, msg) // logcat 同步留一份，双通道
        runCatching {
            SysttsLogger.log(
                LogEntry(
                    level = level,
                    time = java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    message = "[Jet] $msg",
                )
            )
        }
    }

    /** 供 SseAggregator 轮换层写日志（包内可见）：统一挂 [账号池] 前缀 */
    internal fun logLine(msg: String) = appLog(LogLevel.INFO, msg)

    /**
     * 渠道层续期成功后的落盘（10-09 全渠道批）：更新 token 三元组，保留其余字段。
     * codebuddy 的续期在 refresh() 内部落盘不走这里；其它渠道走这里统一落。
     */
    fun saveRefreshed(accountId: String, accessToken: String, refreshToken: String, expiresAt: Long): Boolean {
        val acc = load().firstOrNull { it.id == accountId } ?: return false
        save(load().map {
            if (it.id == accountId) acc.copy(accessToken = accessToken, refreshToken = refreshToken, expiresAt = expiresAt)
            else it
        })
        appLog(LogLevel.SUCCESS, "「${acc.nickname}」（${acc.provider}）凭据已续期")
        return true
    }

    // ==================== 多渠道路由（10-09 全渠道批） ====================

    /**
     * 续期按渠道路由：codebuddy 走内置实现（已实测稳定）；其它渠道走 ChatChannel。
     * 返回 (更新后账号, 错误)；渠道无续期能力返回 (null, "该渠道不支持自动续期")。
     */
    fun refreshAny(acc: Account): Pair<Account?, String> {
        if (acc.provider == "codebuddy") return refresh(acc)
        ChannelBootstrap.install()
        val ch = ChatChannels.byProvider(acc.provider)
            ?: return null to "未知渠道：${acc.provider}"
        val r = ch.refresh(acc)
            ?: return null to if (acc.provider == "zcode" || acc.provider == "opencode" || acc.provider == "loomy")
                "该渠道无续期接口（凭据静态/会话到期重登）" else "续期失败"
        val updated = acc.copy(accessToken = r.first, refreshToken = r.second, expiresAt = r.third)
        // codearts 的 AK/SK/ST 变化落 extra
        val withExtra = if (acc.provider == "codearts") {
            CodeartsChannel.pendingExtraUpdate?.let { pj ->
                var u = updated
                for (k in pj.keys()) u = u.withExtra(k, pj.optString(k))
                CodeartsChannel.clearPendingExtraUpdate()
                u
            } ?: updated
        } else updated
        save(load().map { if (it.id == acc.id) withExtra else it })
        appLog(LogLevel.SUCCESS, "「${acc.nickname}」（${ch.displayName}）凭据已续期")
        return withExtra to ""
    }

    /** 签到按渠道路由（渠道无签到接口返回其文案） */
    fun checkInAny(acc: Account, retried: Boolean = false): Pair<Boolean, String> {
        if (acc.provider == "codebuddy") return checkIn(acc, retried)
        ChannelBootstrap.install()
        val ch = ChatChannels.byProvider(acc.provider) ?: return false to "未知渠道：${acc.provider}"
        return ch.checkIn(acc)
    }

    /**
     * 签到成功后的记账（10-10 对账 TOP5 第 5 条）：写 lastCheckinAt（epoch ms，展示沿用）
     * + lastCheckinDate（UTC+8 日期串，「今天是否已签」的判据，见 CheckinPolicy）。
     * codebuddy 的 checkIn 内部已写，勿重复调；其它渠道由 AccountCheckinReceiver 统一调。
     */
    fun markCheckedIn(accountId: String) {
        val acc = load().firstOrNull { it.id == accountId } ?: return
        val now = System.currentTimeMillis()
        save(load().map {
            if (it.id == accountId) it.copy(lastCheckinAt = now, lastCheckinDate = CheckinPolicy.utc8Today(now))
            else it
        })
    }

    /** 余额按渠道路由（NaN=不支持，调用方显示「未知」）。⚠️ 本类 queryCredits 返回 Pair（codebuddy 旧签名），ChatChannel.queryCredits 返回 Double（渠道新签名） */
    fun queryCreditsAny(acc: Account): Pair<Double, String> {
        if (acc.provider == "codebuddy") return queryCredits(acc)
        ChannelBootstrap.install()
        val ch = ChatChannels.byProvider(acc.provider) ?: return Pair(-1.0, "未知渠道：${acc.provider}")
        // 明细优先（10-10 分池）：实现了 queryCreditDetail 的渠道一次请求拿到合计+两桶；
        // 未实现的走 queryCredits 单值，分池字段清成 -1（UI 不显示分池行）。
        val detail = runCatching { ch.queryCreditDetail(acc) }.getOrNull()
        val v: Double = detail?.total ?: ch.queryCredits(acc)
        if (v.isNaN()) return Pair(-1.0, "该渠道无余额接口")
        val rounded = Math.round(v * 100.0) / 100.0
        val permR = detail?.let { Math.round(it.permanent * 100.0) / 100.0 } ?: -1.0
        val ephR = detail?.let { Math.round(it.ephemeral * 100.0) / 100.0 } ?: -1.0
        save(load().map {
            if (it.id == acc.id) it.copy(credits = rounded, permanentCredits = permR, ephemeralCredits = ephR) else it
        })
        return Pair(rounded, "")
    }

    // 轮询「等待登录完成」去重：上游 body 不变就不重复打（2s 一次会刷屏）
    private var lastWaitBrief: String = ""

    // CodeBuddy 上游根（对话/鉴权同源）
    const val UPSTREAM_BASE = "https://copilot.tencent.com"

    // ==================== 协议路径（10-07 实测定版，热更时与 codebuddy.js 的 CB 常量同步） ====================
    private const val PATH_AUTH_STATE = "/v2/plugin/auth/state?platform=ide"        // POST → data.state + data.authUrl
    private const val PATH_AUTH_TOKEN = "/v2/plugin/auth/token?state="              // GET ?state=xxx 轮询
    private const val PATH_AUTH_REFRESH = "/v2/plugin/auth/token/refresh"           // POST + X-Refresh-Token
    private const val PATH_CHECKIN = "/v2/billing/meter/daily-checkin"              // POST {}，重复领取=HTTP400+业务码
    private const val PATH_CHECKIN_STATUS = "/v2/billing/meter/checkin-activity-status" // POST {}，今日已签/连续天数
    private const val PATH_CREDITS = "/v2/billing/meter/get-user-resource"          // POST {}，Accounts[] 求和

    /**
     * 「临时积分」窗口（10-10 分池）：距扣费截止不足此值的包算临时（再不用就作废）。
     * 与插件同值 15 天（buddy-balance-rank.ts BUDDY_EXPIRING_WINDOW_DAYS，用户 2026-09-29 定）。
     */
    const val CREDIT_EXPIRING_WINDOW_MS = 15L * 24 * 3600_000L

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
        // 积分分池（10-10 移植插件 splitBuddyCreditsByExpiry 语义）：把余额按「会不会很快作废」
        // 拆两桶显示——距扣费截止 <15 天的算临时（ephemeralCredits；再不用就没了、优先消耗），
        // 其余算长期（permanentCredits）。窗口与插件同值（15 天，buddy-balance-rank.ts）。
        // 两者均为 -1.0 = 该渠道没有「会不会作废」这个维度（不显示分池行）；
        // 只有支持的渠道（codebuddy/workbuddy/trae/lobsterai/loomy/raccoon）填真值。
        val permanentCredits: Double = -1.0,
        val ephemeralCredits: Double = -1.0,
        // 最近一次成功签到时刻（epoch ms；0=从未）。展示层沿用（池页「签到 HH:mm」）
        val lastCheckinAt: Long = 0L,
        // 最近一次成功签到的 UTC+8 日期串（YYYY-MM-DD；""=从未）。⚠️ 10-10 对账 TOP5 第 5 条：
        // 「今天是否已签」的记账判据用它（=== CheckinPolicy.utc8Today()）——按 UTC+8 算术
        // 平移取日，不用本机时区，也不直信上游「今日已签」文案（qoder 活动每日 10:00 UTC+8
        // 刷新，上午查到的 already-claimed 是昨天的；插件 ClaimOutcome.coversToday 同款语义）。
        val lastCheckinDate: String = "",
        val enabled: Boolean = true,
        val createdAt: Long = System.currentTimeMillis(),
        // 模型级限流标记（10-08 移植插件 modelRateLimits）：模型名 → 重置时刻 epoch ms。
        // 对话链 429/11140 时写入；选号跳过未解禁的（同模型）；解禁后标记自然失效。
        val modelRateLimits: Map<String, Long> = emptyMap(),
        // 渠道扩展字段（10-09 全渠道批）：各渠道私有持久化（zcode 的 device_mid、
        // lobsterai 的 uuid/firstKeyfrom、trae 的 machine_id/device_id 等），JSON 串。
        // 渠道实现自己读写，AccountPool 不解释内容。
        val extra: String = "{}",
        // 本次落盘是否走了「更新已有账号」路径（10-10 登录去重）。仅 upsert 返回值上有意义
        // （供调用方 toast 标「(已更新)」）；save/load 不落盘不读回，恒为 false。
        val isUpdate: Boolean = false,
    ) {
        /** access_token 是否已过期（留 10 分钟余量；未知不算过期） */
        fun isExpired(now: Long = System.currentTimeMillis()): Boolean =
            expiresAt in 1..(now + 10 * 60_000L)

        /** 该模型是否处于限流期（空模型名=未知目标，不过滤——照插件同判据） */
        fun isRateLimitedFor(model: String, now: Long = System.currentTimeMillis()): Boolean {
            if (model.isEmpty()) return false
            val resetAt = modelRateLimits[model] ?: return false
            return resetAt > 0 && now < resetAt
        }

        /** extra JSON 里取字段（空/坏 JSON 返回 def） */
        fun extraStr(key: String, def: String = ""): String = try {
            JSONObject(extra).optString(key).takeIf { it.isNotEmpty() } ?: def
        } catch (_: Exception) { def }

        /** 返回一份 extra 更新后的新 Account（不改存储，调用方 save） */
        fun withExtra(key: String, value: String): Account {
            val o = try { JSONObject(extra) } catch (_: Exception) { JSONObject() }
            o.put(key, value)
            return copy(extra = o.toString())
        }
    }

    /** 签到状态（checkin-activity-status 响应） */
    data class CheckinStatus(
        val active: Boolean,
        val todayCheckedIn: Boolean,
        val streakDays: Int,
        val dailyCredit: Long,
    )

    // ==================== 登录去重统一落盘（10-10 对账 TOP5 第 3 条） ====================

    // extra 里的内部身份键（下划线前缀=内部字段）：upsert 按它匹配同渠道已有账号
    private const val EXTRA_UID = "_uid"

    /**
     * 登录落盘统一入口（照插件 findAccountIdByIdentityField 语义）：
     * identity 非空且池里有同 provider + 同 extra._uid 的账号 → 复用其 id/enabled/createdAt/
     * 列表位置，用 make(existing) 产出的新条目原位替换（token 等字段刷新）；
     * 否则走 make(null) 新增（append 到列表尾）。identity 传 null/空 = 不去重直接新增
     * （诚实降级：缺身份判据就明说，不做猜测试的匹配）。
     *
     * 与插件「不看 enabled」一致：停用的账号同样占位，重复添加它仍是更新而非新增。
     * 返回的 Account.isUpdate=true 表示走了更新路径（调用方 toast 标「(已更新)」）。
     * 身份值同时写进 extra._uid（渠道调用方负责，或由 make 产出条目携带）。
     */
    fun upsert(provider: String, identity: String?, make: (existing: Account?) -> Account): Account {
        val list = load()
        var isUpdate = false
        val acc: Account = if (!identity.isNullOrEmpty()) {
            val existing = list.firstOrNull {
                it.provider == provider && it.extraStr(EXTRA_UID) == identity
            }
            if (existing != null) {
                isUpdate = true
                make(existing).copy(
                    id = existing.id,
                    enabled = existing.enabled,
                    createdAt = existing.createdAt,
                    // 更新≠重建：余额/签到记账/限流标记是账号的历史状态，重登不该清零
                    //（raccoon 重登若丢 lastCheckinAt/Date 会当天重复签到）。token 等以 make 产出为准
                    credits = existing.credits,
                    lastCheckinAt = existing.lastCheckinAt,
                    lastCheckinDate = existing.lastCheckinDate,
                    modelRateLimits = existing.modelRateLimits,
                    isUpdate = true,
                )
            } else make(null)
        } else make(null)
        val next = if (isUpdate) list.map { if (it.id == acc.id) acc else it }
        else list.filterNot { it.id == acc.id } + acc
        save(next)
        appLog(
            if (isUpdate) LogLevel.INFO else LogLevel.SUCCESS,
            if (isUpdate) "登录成功，已有账号「${acc.nickname}」（$provider）凭据已更新"
            else "登录成功，新账号「${acc.nickname}」（$provider）已落盘"
        )
        return acc
    }

    /**
     * JWT payload sub（base64url 解析不验签，照 WorkbuddyChannel/SseAggregator 的 jwtExp 模式）。
     * 解析不出返回 null（调用方传 null 给 upsert = 诚实降级不去重）。
     * internal 而非 private：workbuddy/raccoon 的落盘点在 compose 层，跨包要取身份值
     * （照 logLine 的先例——单份实现放本类，别处各抄一份迟早抄岔）。
     */
    internal fun jwtSub(token: String): String? = try {
        val parts = token.split(".")
        if (parts.size < 2) null
        else {
            val payload = String(
                android.util.Base64.decode(
                    parts[1], android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE
                ), Charsets.UTF_8
            )
            JSONObject(payload).optString("sub").takeIf { it.isNotEmpty() }
        }
    } catch (_: Exception) {
        null
    }

    fun load(): List<Account> = try {
        if (!stateFile.exists()) emptyList()
        else {
            val arr = JSONObject(stateFile.readText()).optJSONArray("accounts") ?: return emptyList()
                (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    val limits = mutableMapOf<String, Long>()
                    o.optJSONObject("modelRateLimits")?.let { m ->
                        for (k in m.keys()) m.optLong(k, 0L).takeIf { it > 0L }?.let { limits[k] = it }
                    }
                    Account(
                        id = o.optString("id"),
                        provider = o.optString("provider", "codebuddy"),
                        nickname = o.optString("nickname"),
                        accessToken = o.optString("accessToken"),
                        refreshToken = o.optString("refreshToken"),
                        expiresAt = o.optLong("expiresAt"),
                        credits = o.optDouble("credits", 0.0),
                        // 分池缺键 = 该渠道无此维度（或旧数据），-1 让 UI 不显示分池行
                        permanentCredits = o.optDouble("permanentCredits", -1.0),
                        ephemeralCredits = o.optDouble("ephemeralCredits", -1.0),
                        lastCheckinAt = o.optLong("lastCheckinAt"),
                        lastCheckinDate = o.optString("lastCheckinDate"),
                        enabled = o.optBoolean("enabled", true),
                        createdAt = o.optLong("createdAt"),
                        modelRateLimits = limits,
                        extra = o.optString("extra").ifEmpty { "{}" },
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
                // 分池只在取到真值时落盘（-1 = 该渠道无此维度，不写键；读回给默认值）
                if (a.permanentCredits >= 0.0) put("permanentCredits", a.permanentCredits)
                if (a.ephemeralCredits >= 0.0) put("ephemeralCredits", a.ephemeralCredits)
                put("lastCheckinAt", a.lastCheckinAt)
                if (a.lastCheckinDate.isNotEmpty()) put("lastCheckinDate", a.lastCheckinDate)
                put("enabled", a.enabled)
                put("createdAt", a.createdAt)
                if (a.modelRateLimits.isNotEmpty()) put("modelRateLimits", JSONObject(a.modelRateLimits))
                put("extra", a.extra)
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

    // ==================== 渠道层共用 HTTP / JSON（10-09 全渠道批） ====================

    /** 渠道实现共用 POST（AccountPool.httpJson 是 private，这里开公开壳） */
    fun channelPost(url: String, headers: Map<String, String>, body: String): ChannelHttpResp {
        var conn: java.net.HttpURLConnection? = null
        return try {
            conn = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 30_000
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
                if (!headers.containsKey("Content-Type"))
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                if (body.isNotEmpty()) {
                    doOutput = true
                    outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
            }
            val code = conn.responseCode
            val stream = if (code >= 400) conn.errorStream else conn.inputStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            ChannelHttpResp(code in 200..299, code, text)
        } catch (e: Exception) {
            ChannelHttpResp(false, -1, e.message ?: e.toString())
        } finally {
            conn?.disconnect()
        }
    }

    /** 渠道实现共用 GET */
    fun channelGet(url: String, headers: Map<String, String>): ChannelHttpResp {
        var conn: java.net.HttpURLConnection? = null
        return try {
            conn = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 30_000
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }
            val code = conn.responseCode
            val stream = if (code >= 400) conn.errorStream else conn.inputStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            ChannelHttpResp(code in 200..299, code, text)
        } catch (e: Exception) {
            ChannelHttpResp(false, -1, e.message ?: e.toString())
        } finally {
            conn?.disconnect()
        }
    }

    /** 数字或数字字符串取值（billing Accounts[] 条目通用；本类 firstNumber 的公开版） */
    fun firstNumberOf(o: JSONObject, vararg keys: String): Double? = firstNumber(o, *keys)

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
        val (list, _, err) = fetchModelsWithRates(token)
        return list to err
    }

    /**
     * 拉模型 + 顺手抓计费倍率（10-10）。倍率来源 = /v3/config 每条模型元数据里的计费
     * 字段（CodeBuddy 系促销补 `discountedCreditsRate`）+ 模型名内嵌 `·xN.N`。
     * 字段名/层级未真机核实 → 宽松扫描（含 rate/multiplier/factor 的数值字段），
     * 取不到就不写（UI 不显示，宁缺勿错）。返回 (清单, 倍率表, err)。
     */
    fun fetchModelsWithRates(token: String): Triple<List<String>, Map<String, String>, String> {
        val r = httpJson("$UPSTREAM_BASE/v3/config", "GET", billingHeaders(token), null)
        if (!r.ok) return Triple(builtinModels(), emptyMap(), "HTTP ${r.code}，用内置清单（${briefBody(r.body)}）")
        return try {
            val d = JSONObject(r.body).optJSONObject("data")
                ?: return Triple(builtinModels(), emptyMap(), "响应无 data，用内置清单")
            // data.models 元数据表（过滤判据 + 倍率来源）
            val metaById = HashMap<String, JSONObject>()
            d.optJSONArray("models")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val m = arr.optJSONObject(i) ?: continue
                    val id = m.optString("id")
                    if (id.isNotEmpty()) metaById[id] = m
                }
            }
            val rates = mutableMapOf<String, String>()
            metaById.forEach { (id, m) -> rateOfConfigEntry(m)?.let { rates[id] = it } }
            // agent 引用 id 集合（保序：先 preferred agent 后其余）
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
            val list = if (out.isEmpty()) builtinModels() else out.toList()
            Triple(list, rates, if (out.isEmpty()) "清单为空，用内置清单" else "")
        } catch (e: Exception) {
            Triple(builtinModels(), emptyMap(), "解析失败用内置清单：${e.message}")
        }
    }

    /** /v3/config 单条目倍率：扫计费数值字段；免费/折扣文案照上游；无则名字内嵌 xN.N */
    private fun rateOfConfigEntry(m: JSONObject): String? {
        val effKeys = arrayOf(
            "discountedCreditsRate", "discounted_credits_rate",
            "billing_effective_multiplier", "effectiveMultiplier", "effective_multiplier",
            "price_factor", "priceFactor",
        )
        val origKeys = arrayOf("creditsRate", "credits_rate", "billing_multiplier", "originalMultiplier")
        fun num(keys: Array<String>): Double? {
            for (k in keys) if (m.has(k) && !m.isNull(k)) {
                val dd = m.optDouble(k, Double.NaN)
                if (!dd.isNaN()) return dd
            }
            return null
        }
        val eff = num(effKeys)
        if (eff != null) {
            if (eff == 0.0) return "免费"
            val e = if (eff % 1.0 == 0.0) eff.toLong().toString() else eff.toString()
            val orig = num(origKeys)
            val o = orig?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }
            val note = m.optString("billing_status_note")
            return when {
                o != null && o != e -> "x$o→x$e"
                note.isNotEmpty() -> "x$e·$note"
                else -> "x$e"
            }
        }
        val text = m.optString("name") + " " + m.optString("description")
        return Regex("[x×]\\s*(\\d+(?:\\.\\d+)?)").findAll(text).lastOrNull()
            ?.groupValues?.get(1)?.let { "x$it" }
    }

    /** 内置兜底清单（10-08 全模型实测 14/14 后按真清单收录；/v3/config 拉不到时不至于无模型可选） */
    private fun builtinModels(): List<String> = listOf(
        "deepseek-v4.1-flash",
        "deepseek-v4-pro",
        "deepseek-v4-flash",
        "glm-5.3",
        "glm-5.3-flash",
        "glm-5.2",
        "kimi-k3-1",
        "minimax-m3",
        "hy3",
    )

    // ==================== 一键添加为密钥（10-08：账号即凭据，照原插件免手填） ====================

    /**
     * 把账号直接落成密钥管理的一条密钥：网址=该账号渠道的上游、Key=该账号 access_token、
     * 模型=清单第一个（拉不到用内置默认）。接口分组同名复用、密钥条目按（站点+钥+模型）
     * 去重——重复点不会堆重复条目。免复制粘贴、免见令牌本体（原插件「登录即用」的形态）。
     * @param tagRuleId 密钥归属规则（现两入口都传 mingwuyan）
     * @return (是否新增, 提示)；已存在= false + 说明文案
     */
    fun addAsKey(tagRuleId: String, acc: Account): Pair<Boolean, String> {
        // 按渠道取上游与展示名（10-10 实锤修复：原先所有渠道硬编码 CodeBuddy 上游，
        // workbuddy 号被落到 copilot.tencent.com/v2 → 网关 401，还被 heal 自愈成
        // 「copilot」组并把 CodeBuddy 组级 Key 覆盖掉，模型串组）。
        // 渠道实现在 ChatChannels 注册表里，codebuddy 不迁（历史原因见 ChatChannel.kt 头注）。
        ChannelBootstrap.install() // 幂等
        val ch = ChatChannels.byProvider(acc.provider)
        val baseUrl = ch?.chatBaseUrl ?: "https://$CHAT_HOST/v2"
        val displayName = ch?.displayName ?: "CodeBuddy"
        val ifaces = KeyListFile.readInterfaces(tagRuleId)
        val keys = KeyListFile.readKeys(tagRuleId)
        // 组归并（10-10 用户定稿：一个平台一个分组，几个账号都进同一组）：
        // 判据只看站点（sameApiSite），同站即归组——组名被用户改过也能归上。
        // 组级 Key 只在**建组时**写（首个账号的）；后来者的凭据全靠密钥条目自身 key 段。
        val targetIfc = ifaces.firstOrNull { KeyListFile.sameApiSite(it.baseUrl, baseUrl) }
        // 模型清单不自动塞（10-10 用户令）：组里有什么模型完全由用户「拉取模型」决定，
        // 账号登录只落分组+密钥条目，杜绝「乱七八糟不匹配的模型」。
        // 条目模型段取该组第一个模型（对话链按组模型发；组暂无模型=空串占位）
        val model = targetIfc?.models?.firstOrNull().orEmpty()
        // 条目去重判据：同站点+同钥（模型段不再参与——同账号只落一条）
        val existing = keys.firstOrNull {
            val p = KeyListFile.parseKeyValue(it.value)
            p != null && !p.isDirect && KeyListFile.sameApiSite(p.url, baseUrl) &&
                p.key == acc.accessToken
        }
        if (existing != null) {
            // 组缺失补建（10-10 真机死角）：条目在但组被删（旧包删组漏删残留/手动删过条目外的组）时，
            // 去重分支此前直接秒回——组永远缺席，密钥页看不到分组（条目全落未分组）。
            // 现在：targetIfc==null → 补建组（与新建分支同款形状），条目不动。
            if (targetIfc == null) {
                KeyListFile.saveInterfaces(tagRuleId, ifaces + KeyListFile.ApiInterface(
                    name = displayName,
                    baseUrl = baseUrl,
                    apiKey = acc.accessToken,
                    models = emptyList(),
                ))
                appLog(LogLevel.SUCCESS, "Jet：$displayName 分组已重建（条目已在）")
            }
            // 模型补齐（10-10 真机报障修复）：组里没拉到模型时落的空串占位条目，
            // 密钥页一测试就报「模型名不能为空」。此前注释承诺「重进池页幂等补齐」
            // 但补齐逻辑从没实现——去重分支直接秒回，占位永远是空的。现在真补：
            // 组里已有模型 且 占位条目模型段还空着 → 回填第一个模型（条目名同步改，
            // 与后落条目的命名口径一致）。幂等：模型段非空的条目原样秒回。
            val parsed = KeyListFile.parseKeyValue(existing.value)
            if (parsed != null && !parsed.isDirect && parsed.model.isEmpty() && model.isNotEmpty()) {
                KeyListFile.saveKeys(tagRuleId, keys.map {
                    if (it.keyCode == existing.keyCode) it.copy(
                        value = "$baseUrl@@$model@@${acc.accessToken}",
                        name = KeyListFile.dedupName(model, keys.map { k -> k.name }.toSet()),
                    ) else it
                })
                appLog(LogLevel.SUCCESS, "Jet：$displayName 占位密钥已补齐模型 $model")
                return false to "已补齐模型：$displayName → $model"
            }
            return false to if (targetIfc == null) "已重建分组：$displayName（条目已在）"
            else "已在密钥管理（$displayName），无需重复添加"
        }
        val value = "$baseUrl@@$model@@${acc.accessToken}"

        val updatedIfaces = if (targetIfc != null) ifaces
        else ifaces + KeyListFile.ApiInterface(
            name = displayName,
            baseUrl = baseUrl,
            apiKey = acc.accessToken,
            models = emptyList(), // 模型清单留空，等用户拉取
        )
        val newKeys = keys + KeyListFile.KeyEntry(
            name = KeyListFile.dedupName(
                model.ifEmpty { "${displayName}Key" },
                keys.map { it.name }.toSet()
            ),
            keyCode = KeyListFile.nextKeyCode(keys),
            value = value,
        )
        KeyListFile.saveInterfaces(tagRuleId, updatedIfaces)
        KeyListFile.saveKeys(tagRuleId, newKeys)
        return true to "已添加：$displayName（模型请在该分组拉取）"
    }

    /**
     * 存量迁移（10-08 真机实锤）：addAsKey 早期落的是裸 https://copilot.tencent.com，
     * 对话拼 /chat/completions 被网关 302 跳 www.codebuddy.cn → 空流。
     * 把密钥 value 网址段与接口 baseUrl 里的裸域改写成 …/v2；密钥/接口都改才不留半吊子
     * （sameApiSite 逐字相等，只改一边会把归属拆散）。幂等：已是 /v2 不动。
     * @return 修过的条目数（0=无需迁移）
     */
    fun migrateLegacyKeyUrls(tagRuleId: String): Int {
        val bare = "https://$CHAT_HOST"
        val fixed = "$bare/v2"
        val ifaces = KeyListFile.readInterfaces(tagRuleId)
        val keys = KeyListFile.readKeys(tagRuleId)
        var n = 0
        val newIfaces = ifaces.map {
            if (it.baseUrl.trimEnd('/') == bare) { n++; it.copy(baseUrl = fixed) } else it
        }
        val newKeys = keys.map { e ->
            val p = KeyListFile.parseKeyValue(e.value)
            if (p != null && !p.isDirect && p.url.trimEnd('/') == bare) {
                n++
                e.copy(value = "$fixed@@${p.model}@@${p.key}")
            } else e
        }
        if (n > 0) {
            KeyListFile.saveInterfaces(tagRuleId, newIfaces)
            KeyListFile.saveKeys(tagRuleId, newKeys)
            appLog(LogLevel.SUCCESS, "Jet：已把 $n 处旧网址补上 /v2（302 空流修复）")
        }
        return n
    }

    /**
     * 渠道错位迁移（10-10 真机实锤）：旧版 addAsKey 把所有渠道硬编码 CodeBuddy 上游，
     * workbuddy 等渠道的号被落到 copilot.tencent.com/v2 → 网关 401，还被 heal 自愈出
     * 「copilot」组、顶掉 CodeBuddy 组级 Key。按账号渠道把密钥条目网址段改回各自上游、
     * 把落错的接口组归位（组级 Key 先还原给本站账号，无人认领才整组改挂该渠道上游），
     * 改挂后同站+同钥的重复组合并（模型取并集）。幂等：站点已一致的不动。
     * @return 修过的条目/组数（0=无需迁移）
     */
    fun migrateWrongChannelKeyUrls(tagRuleId: String): Int {
        ChannelBootstrap.install() // 幂等
        val accounts = load()
        val channelByToken = accounts.mapNotNull { acc ->
            ChatChannels.byProvider(acc.provider)?.let { ch -> acc.accessToken to ch }
        }.toMap()
        if (channelByToken.isEmpty()) return 0
        var n = 0
        var keys = KeyListFile.readKeys(tagRuleId)
        val ifaces = KeyListFile.readInterfaces(tagRuleId)

        // ① 密钥条目：key 段命中账号、站点与该渠道上游不符 → 网址段改回渠道上游
        keys = keys.map { e ->
            val p = KeyListFile.parseKeyValue(e.value) ?: return@map e
            if (p.isDirect) return@map e
            val ch = channelByToken[p.key] ?: return@map e
            if (KeyListFile.sameApiSite(p.url, ch.chatBaseUrl)) e
            else { n++; e.copy(value = "${ch.chatBaseUrl}@@${p.model}@@${p.key}") }
        }

        // ② 接口组：组级 Key 命中账号但站点与该渠道上游不符 →
        //    本站有该渠道（与组同站）的密钥条目 = 组级 Key 被别人顶了，还原给本站条目；
        //    没有 = 整组落错站，改挂该渠道上游并按渠道改名
        val allNames = ifaces.map { it.name }.toMutableSet()
        val rebased = ifaces.map { ifc ->
            val acc = accounts.firstOrNull { it.accessToken == ifc.apiKey.trim() } ?: return@map ifc
            val ch = ChatChannels.byProvider(acc.provider) ?: return@map ifc
            if (KeyListFile.sameApiSite(ifc.baseUrl, ch.chatBaseUrl)) return@map ifc
            n++
            val nativeKey = keys.firstNotNullOfOrNull { e ->
                val p = KeyListFile.parseKeyValue(e.value)
                if (p == null || p.isDirect || !KeyListFile.sameApiSite(p.url, ifc.baseUrl)) null
                else channelByToken[p.key]
                    ?.takeIf { KeyListFile.sameApiSite(it.chatBaseUrl, ifc.baseUrl) }
                    ?.let { p.key }
            }
            if (nativeKey != null) ifc.copy(apiKey = nativeKey)
            else {
                val nm = KeyListFile.uniqueIfcName(ch.displayName, allNames)
                allNames.add(nm)
                ifc.copy(baseUrl = ch.chatBaseUrl, name = nm)
            }
        }

        // ③ 改挂后同站+同钥的组去重合并（保先出现的名字，模型取并集）
        val merged = LinkedHashMap<String, KeyListFile.ApiInterface>()
        rebased.forEach { ifc ->
            val k = ifc.baseUrl.trimEnd('/') + "@" + ifc.apiKey.trim()
            val ex = merged[k]
            merged[k] = if (ex == null) ifc else ex.copy(models = (ex.models + ifc.models).distinct())
        }
        val newIfaces = merged.values.toList()

        val ifcChanged = newIfaces != ifaces
        if (n > 0 || ifcChanged) {
            if (ifcChanged) KeyListFile.saveInterfaces(tagRuleId, newIfaces)
            KeyListFile.saveKeys(tagRuleId, keys)
            if (n > 0) appLog(LogLevel.SUCCESS, "Jet：已把 $n 处错挂到 CodeBuddy 上游的条目/分组改回各自渠道（401 修复）")
        }
        return n
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
        // 10-08 真机排障：轮询全程静默→失败无从查起，关键分支落日志页（tag=AccountPool）
        if (!r.ok) {
            val msg = "HTTP ${r.code}（未登录完或已过期）：${briefBody(r.body)}"
            appLog(LogLevel.WARN, "轮询凭据失败：$msg")
            return null to msg
        }
        return try {
            val o = JSONObject(r.body).let { it.optJSONObject("data") ?: it }
            // 10-08 真机实锤：上游键名是驼峰 accessToken/refreshToken（code:0 响应见日志），
            // 试水期记的下划线式留作兼容
            val access = o?.optString("accessToken").orEmpty()
                .ifEmpty { o?.optString("access_token").orEmpty() }
            val refresh = o?.optString("refreshToken").orEmpty()
                .ifEmpty { o?.optString("refresh_token").orEmpty() }
                .ifEmpty { existing?.refreshToken ?: "" }
            if (access.isEmpty()) {
                // HTTP 200 + 业务码未完成（如 11217 login ing）= 正常等待。同因只记首条
                //（轮询 2s 一次，逐条打会刷屏），调用方有 lastWaitBrief 传短状态时跳过重复
                val brief = briefBody(r.body)
                if (!brief.startsWith(lastWaitBrief))
                    appLog(LogLevel.INFO, "等待登录完成（200 无凭据）：$brief")
                lastWaitBrief = brief
                return null to "响应无 accessToken（可能还没登录完）：$brief"
            }
            // expires 完整形态未见过（日志截断）：expiresIn/expires_in 相对秒、
            // expiresAt/expires_at 绝对毫秒，四式兜底
            val relSec = maxOf(
                o?.optLong("expiresIn", 0L) ?: 0L,
                o?.optLong("expires_in", 0L) ?: 0L
            )
            val absMs = maxOf(
                o?.optLong("expiresAt", 0L) ?: 0L,
                o?.optLong("expires_at", 0L) ?: 0L
            )
            val expiresAt = when {
                absMs > 0L -> absMs
                relSec > 0L -> System.currentTimeMillis() + relSec * 1000L
                else -> 0L
            }
            val nick = o?.optString("nickname").orEmpty()
                .ifEmpty { o?.optString("username").orEmpty() }
            // 10-10 登录去重（对账 TOP5 第 3 条）：identity = access_token 的 JWT sub，
            // 同号重登复用原条目（id/顺序/启用状态/记账不变）；解不出 sub 传 null 诚实降级。
            // 身份值同时写进 extra._uid
            val sub = jwtSub(access)
            val acc = upsert("codebuddy", sub) { found ->
                Account(
                    id = found?.id ?: "codebuddy-${System.currentTimeMillis().toString(16)}",
                    // 多号区分（10-10 用户定案）：真名拉得到用真名，拉不到回退渠道名+「*尾4」
                    // （同密钥 *尾4BeP 口径，keyTail>4 显尾4）。更新路径沿用旧名不重算，
                    // 防上游改名把用户已识别的标签搅动
                    nickname = nick.ifEmpty {
                        found?.nickname ?: existing?.nickname
                        ?: "CodeBuddy *" + KeyListFile.keyTail(access)
                    },
                    accessToken = access,
                    refreshToken = refresh,
                    expiresAt = expiresAt,
                ).withExtra("_uid", sub ?: "")
            }
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
            // 10-08 真机实锤：上游键名驼峰（同 pollToken），下划线式留作兼容
            val access = o?.optString("accessToken").orEmpty()
                .ifEmpty { o?.optString("access_token").orEmpty() }
                .ifEmpty { acc.accessToken }
            val refresh = o?.optString("refreshToken").orEmpty()
                .ifEmpty { o?.optString("refresh_token").orEmpty() }
                .ifEmpty { acc.refreshToken }
            val relSec = maxOf(
                o?.optLong("expiresIn", 0L) ?: 0L,
                o?.optLong("expires_in", 0L) ?: 0L
            )
            val absMs = maxOf(
                o?.optLong("expiresAt", 0L) ?: 0L,
                o?.optLong("expires_at", 0L) ?: 0L
            )
            val expiresAt = when {
                absMs > 0L -> absMs
                relSec > 0L -> System.currentTimeMillis() + relSec * 1000L
                else -> acc.expiresAt
            }
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
     * ⚠️ coversToday 判据（10-10 对账 TOP5 第 5 条）：这里直信 today_checked_in 仅限
     * codebuddy——该字段是服务端当日实时状态（非「昨日痕迹」），且成功路径的记账已改为
     * 写 lastCheckinDate（UTC+8，见下），重复触发的最终拦截在 checkinAll 按
     * lastCheckinDate === utc8Today() 判，上游文案不再参与记账。
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
        val now = System.currentTimeMillis()
        val updated = acc.copy(lastCheckinAt = now, lastCheckinDate = CheckinPolicy.utc8Today(now))
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
        // 分池（10-10）：同一份响应顺手算两桶——包带 DeductionEndTime，距今不足
        // 窗口（15 天）的算临时，其余算长期。窗口与插件同值（buddy-balance-rank.ts）。
        var permanent = 0.0
        var ephemeral = 0.0
        var splitFound = false
        val now = System.currentTimeMillis()
        if (accounts != null) {
            for (i in 0 until accounts.length()) {
                val p = accounts.optJSONObject(i) ?: continue
                if (p.optInt("Status", 0) == 3) continue  // 服务端标记的失效包
                val v = firstNumber(p, "CycleCapacityRemainPrecise", "CycleCapacityRemain") ?: continue
                total += v
                found = true
                // 到期时刻（epoch ms）：拿不到 = 归长期（插件：unknown 归 permanent 桶）
                if (v > 0) {
                    val end = firstNumber(p, "DeductionEndTime")
                    if (end != null && end > 0 && end - now < CREDIT_EXPIRING_WINDOW_MS) ephemeral += v
                    else permanent += v
                    splitFound = true
                }
            }
        }
        if (!found) return -1.0 to "响应无资源包字段：${briefBody(r.body)}"
        val credits = Math.round(total * 100.0) / 100.0
        // 拆分值仅在有包时落盘（splitFound）；无包渠道/无数据保持 -1（UI 不显示分池行）
        val permR = if (splitFound) Math.round(permanent * 100.0) / 100.0 else -1.0
        val ephR = if (splitFound) Math.round(ephemeral * 100.0) / 100.0 else -1.0
        save(load().map {
            if (it.id == acc.id) it.copy(credits = credits, permanentCredits = permR, ephemeralCredits = ephR) else it
        })
        return credits to ""
    }

    // ==================== 选号 / 限流标记 / 账号管理（10-08 移植插件账号池语义） ====================

    /**
     * 选号（照插件 getAvailableAccount 语义）：启用中的账号按**落盘顺序**（=列表展示序，
     * 插件「拖拽顺序即优先级」在 app 里对应账号池列表顺序）取第一个满足：
     * ① enabled ② **provider 与目标渠道一致**（10-09 真机实锤补：匿名 public 被拿到腾讯站
     * 打=「*尾blic」事故——账号池凭据曾设计为全站通用，但各站凭据互不通用，必须隔离）
     * ③ 该模型不在限流期（空模型不过滤）④ 非 expired（过期的跳过——
     * 插件里凭据过期由续期调度兜着，选号侧不选它；app 侧请求链的 401 会现场续期一次）。
     * excludeIds：换号循环排除已试过的。无候选返回 null。
     */
    fun pickAccount(model: String, provider: String, excludeIds: Set<String> = emptySet()): Account? {
        val now = System.currentTimeMillis()
        return load().filter { it.enabled && it.id !in excludeIds }
            .filter { it.provider == provider }
            .filter { !it.isExpired(now) && !it.isRateLimitedFor(model, now) }
            .firstOrNull()
    }

    /**
     * 记限流标记（照插件 updateModelRateLimit）：账号×模型 → 重置时刻。429 与 11140 安全
     * 拦截共用本标记（插件同款复用；11140 无时间字段，用 30 分钟冷却）。
     * 解析出服务端重置时刻用真实值；解析不出用兜底（插件 RATE_LIMIT_FALLBACK_MS=1h）。
     */
    fun markRateLimited(accountId: String, model: String, resetAtMs: Long): Boolean {
        val acc = load().firstOrNull { it.id == accountId } ?: return false
        val limits = acc.modelRateLimits + (model to resetAtMs)
        save(load().map { if (it.id == accountId) acc.copy(modelRateLimits = limits) else it })
        appLog(LogLevel.INFO, "账号「${acc.nickname}」模型 $model 限流，${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(resetAtMs))} 解禁")
        return true
    }

    /** 清某账号的全部限流标记（插件「重测/重置」的人工解禁路径） */
    fun clearRateLimits(accountId: String): Boolean {
        val acc = load().firstOrNull { it.id == accountId } ?: return false
        if (acc.modelRateLimits.isEmpty()) return false
        save(load().map { if (it.id == accountId) acc.copy(modelRateLimits = emptyMap()) else it })
        return true
    }

    /** 删除账号（连带无其它账号引用的「账号池」密钥不动——密钥归 KeyListFile 管，用户手动删） */
    fun remove(accountId: String): Boolean {
        val list = load()
        val next = list.filterNot { it.id == accountId }
        if (next.size == list.size) return false
        save(next)
        return true
    }

    /** 停用/启用（停用只影响自动选号，续期/签到照跑——照插件同语义） */
    fun setEnabled(accountId: String, value: Boolean): Boolean {
        val acc = load().firstOrNull { it.id == accountId } ?: return false
        save(load().map { if (it.id == accountId) acc.copy(enabled = value) else it })
        return true
    }

    // ==================== 重测 / 重置（10-10 移植插件 account-probe 语义） ====================

    /**
     * 逐模型重置结果：仍受限的模型回写**新**解禁时刻（上游时刻是滚动的，不写回
     * 存储会一直停在第一次撞限流的旧时刻——旧时刻过期后 UI 不再显示限流、选号也会
     * 误判为可用，插件同款补丁 [patch-codearts-probe-ratelimit]）。
     */
    data class RetestResult(
        /** 该账号拥有限流标记的模型数（0 = 无标记，未发请求） */
        val tested: Int,
        /** 实测通过、已清除标记的模型 */
        val cleared: List<String>,
        /** 仍受限的模型（仍受限/无法确认） */
        val stillLimited: List<String>,
        /** 整体性错误（账号不存在/取号失败等）；非空时 tested/cleared 可能为 0 */
        val error: String = "",
    )

    /**
     * 重测单账号（插件 retestAccount 语义）：对**每个带限流标记的模型**各发一条真实
     * 最小消息，正常返回的才清标记；仍受限的把上游给的新时刻写回。
     * 无标记 → 一个请求都不发（返回 tested=0，与插件一致），这是与「测试」的区别。
     *
     * ⚠️ 会真实消耗模型额度。**顺序执行、不并发**（插件同律：并发容易触发真正想验证的限流，
     * 反得假阳性）。
     */
    fun retestAccount(accountId: String): RetestResult {
        val acc = load().firstOrNull { it.id == accountId }
            ?: return RetestResult(0, emptyList(), emptyList(), "账号不存在")
        val modelIds = acc.modelRateLimits.keys.toList()
        if (modelIds.isEmpty()) return RetestResult(0, emptyList(), emptyList())
        ChannelBootstrap.install()
        val ch = ChatChannels.byProvider(acc.provider)
        // 对话基址：非 codebuddy 渠道用渠道声明的 chatBaseUrl；codebuddy 走 /v2
        //（与 addAsKey 落的密钥网址段同源，规格书 §1.4 POST {endpoint}/v2/chat/completions）
        val base = ch?.chatBaseUrl ?: "https://$CHAT_HOST/v2"
        val cleared = mutableListOf<String>()
        val still = mutableListOf<String>()
        for (model in modelIds) {
            // 渠道请求体变换必须先走（chatCompletion 不代调 patchBody）：trae 的 SOLO 字段名、
            // gemini 的嵌套键序、各家的 stream 处理都在这层，漏了会得到与真实对话不同的请求。
            val raw = chatPayload(model, "只回复 pong", 16, 0.0)
            val bodyJson = ch?.patchBody(raw, model) ?: raw
            val (ok, body) = SseAggregator.chatCompletion(
                base, acc.accessToken,
                bodyJson,
                channel = ch, model = model,
                extraHeaders = ch?.perRequestHeaders(model) ?: emptyMap(),
            )
            if (ok) {
                cleared.add(model)
            } else {
                still.add(model)
                // 仍受限：上游给的新解禁时刻写回（滚动时刻，不写回会停在旧时刻）
                val m = Regex("HTTP (\\d{3})").find(body)
                val status = m?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val cls = ch?.classifyError(status, body)
                val rateLimited = cls == ChatChannel.ErrClass.RATE_LIMIT ||
                    (cls == null && (status == 429 || body.contains("频率限制") || body.contains("reset at", true) || body.contains("11140")))
                if (rateLimited) {
                    val resetAt = parseResetTime(body)
                        ?: ch?.let { System.currentTimeMillis() + it.rateLimitFallbackMs }
                        ?: System.currentTimeMillis() + if (body.contains("11140")) 30 * 60_000L else 3_600_000L
                    markRateLimited(accountId, model, resetAt)
                }
            }
        }
        if (cleared.isNotEmpty()) clearModelRateLimits(accountId, cleared)
        appLog(LogLevel.INFO, "「${acc.nickname}」重测：${cleared.size} 通 / ${still.size} 仍受限（测 ${modelIds.size} 个模型）")
        return RetestResult(modelIds.size, cleared, still)
    }

    /** 重测某渠道**全部**账号（含已停用——插件同语义：停用号的消息照样发） */
    fun retestAllAccounts(provider: String): Pair<Int, Int> {
        val list = load().filter { it.provider == provider }
        var cleared = 0
        var tested = 0
        for (a in list) {
            val r = retestAccount(a.id)
            tested += r.tested
            cleared += r.cleared.size
        }
        return cleared to tested
    }

    /** 清某账号**指定**模型的限流标记（重测通过者用；返回是否确有变更） */
    fun clearModelRateLimits(accountId: String, models: List<String>): Boolean {
        val acc = load().firstOrNull { it.id == accountId } ?: return false
        if (models.isEmpty()) return false
        val next = acc.modelRateLimits.filterKeys { it !in models }
        if (next.size == acc.modelRateLimits.size) return false
        save(load().map { if (it.id == accountId) acc.copy(modelRateLimits = next) else it })
        return true
    }

    /** 重置某渠道**全部**账号的限流标记（不发送任何请求；返回被清账号数） */
    fun resetAllAccounts(provider: String): Int {
        val list = load()
        var changed = 0
        val next = list.map { a ->
            if (a.provider == provider && a.modelRateLimits.isNotEmpty()) {
                changed++
                a.copy(modelRateLimits = emptyMap())
            } else a
        }
        if (changed > 0) save(next)
        appLog(LogLevel.INFO, "重置全部限流标记：${provider} 共 ${changed} 个账号")
        return changed
    }

    /**
     * 最小对话请求体（重测用）。`max_tokens` 取 16：够回一个 pong，又不至于被
     * 「max_tokens 超上限才拒」的渠道按尺寸拦（校验的是上限不是下限）。
     * `stream` 不在此写死——无渠道实现时 SseAggregator 会自动补 `stream=true`
     * （codebuddy 上游只收流式），有渠道实现时由 patchBody 自行处理。
     */
    private fun chatPayload(model: String, text: String, maxTokens: Int, temperature: Double): String =
        org.json.JSONObject()
            .put("model", model)
            .put("messages", org.json.JSONArray().put(
                org.json.JSONObject().put("role", "user").put("content", text)
            ))
            .put("max_tokens", maxTokens)
            .put("temperature", temperature)
            .toString()

    /** 从错误体解析服务端给的解禁时刻（与 SseAggregator.parseResetTime 同口径） */
    private fun parseResetTime(body: String): Long? {
        val epoch = Regex("\"reset_at\"\\s*:\\s*(\\d{10,13})").find(body)?.groupValues?.get(1)?.toLongOrNull()
        if (epoch != null) return if (epoch < 10_000_000_000L) epoch * 1000 else epoch
        val m = Regex("(\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2})").find(body) ?: return null
        return runCatching {
            java.time.LocalDateTime.parse(m.groupValues[1].replace(' ', 'T'))
                .atZone(java.time.ZoneId.of("UTC+8")).toInstant().toEpochMilli()
        }.getOrNull()
    }
}
