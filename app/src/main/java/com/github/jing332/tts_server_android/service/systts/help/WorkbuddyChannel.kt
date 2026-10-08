package com.github.jing332.tts_server_android.service.systts.help

import org.json.JSONObject

/**
 * workbuddy（腾讯国际版）渠道（10-09 全渠道批，协议=规格书 §2，与 buddy 同协议不同产品）。
 *
 * 与 CodeBuddy（AccountPool 内已实现）的差异全在常量：
 * endpoint=www.workbuddy.ai、platform=workbuddy-ai、UA/X-IDE-*=WorkBuddy 系、无签到。
 * 登录/续期/对话协议形状逐字同 buddy（auth/state 轮询、X-Refresh-Token 续期、/v2 对话）。
 *
 * ⚠️ UA 按模型族分档（后台按 UA 归因使用端）：gpt-/gemini-/claude- 前缀 → 国际版 UA；
 * 国产模型前缀 → WorkBuddy/5.5.2 WorkBuddy/5.5.2 CLI/5.5.2。
 * ⚠️ chat 的 X-Product 发归属名 WorkBuddy（发 SaaS 后台归因不到）。
 */
object WorkbuddyChannel : ChatChannel {
    override val id = "workbuddy"
    override val displayName = "WorkBuddy 国际版"
    override val chatBaseUrl = "https://www.workbuddy.ai/v2"

    private const val ENDPOINT = "https://www.workbuddy.ai"
    private const val DOMAIN = "www.workbuddy.ai"
    private const val PLATFORM = "workbuddy-ai"
    private const val VERSION = "5.5.2"

    /** 默认 UA + 模型族覆盖（规格书 2.1） */
    private fun uaFor(model: String): String {
        val international = model.startsWith("gpt-") || model.startsWith("gemini-") || model.startsWith("claude-")
        return if (international) "WorkBuddy/$VERSION WorkBuddy AI/$VERSION CLI/$VERSION"
        else "WorkBuddy/$VERSION WorkBuddy/$VERSION CLI/$VERSION"
    }

    private fun noAuthHeaders(ua: String): Map<String, String> = mapOf(
        "X-Domain" to DOMAIN,
        "X-No-Authorization" to "true",
        "X-No-User-Id" to "true",
        "X-No-Enterprise-Id" to "true",
        "X-No-Department-Info" to "true",
        "User-Agent" to ua,
    )

    // ==================== 登录（同 buddy 两步，第三步拉账号信息） ====================

    data class LoginStart(val state: String, val url: String, val err: String)

    fun fetchLoginUrl(): LoginStart {
        val r = AccountPool.channelPost(
            "$ENDPOINT/v2/plugin/auth/state?platform=$PLATFORM",
            noAuthHeaders(uaFor("")), "{}",
        )
        if (!r.ok) return LoginStart("", "", "HTTP ${r.code}：${r.body.take(120)}")
        val d = try { JSONObject(r.body).optJSONObject("data") } catch (_: Exception) { null }
            ?: return LoginStart("", "", "响应缺 data")
        val state = d.optString("state")
        var url = d.optString("authUrl")
        if (state.isEmpty() || url.isEmpty()) return LoginStart("", "", "响应缺 state/authUrl")
        // workbuddy 附加参数（只追加不重建）
        url += if (url.contains('?')) "&" else "?"
        url += "version=$VERSION&loginSessionId=${DeviceCodeLogin.randomUuid()}"
        return LoginStart(state, url, "")
    }

    /** 轮询凭据（业务码 11217=未就绪）。成功返回 (access, refresh, expiresAtMs, err) */
    fun pollToken(state: String): Quad {
        val r = AccountPool.channelGet(
            "$ENDPOINT/v2/plugin/auth/token?state=" + java.net.URLEncoder.encode(state, "UTF-8"),
            mapOf("X-No-Authorization" to "true", "User-Agent" to uaFor("")),
        )
        if (!r.ok) return Quad.err("HTTP ${r.code}（未登录完或已过期）：${r.body.take(120)}")
        val o = try { JSONObject(r.body) } catch (_: Exception) { return Quad.err("响应不是 JSON") }
        if (o.optInt("code", 0) == 11217) return Quad.pending()
        val d = o.optJSONObject("data") ?: o.optJSONObject("data") ?: return Quad.pending()
        val access = d.optString("accessToken").ifEmpty { d.optString("access_token") }
        if (access.isEmpty()) return Quad.pending()
        // ⚠️ 只有相对秒 expiresIn/refreshExpiresIn，无绝对时刻——基准=当前时刻（JWT iat 同值近似）
        val expiresInSec = maxOf(d.optLong("expiresIn", 0L), d.optLong("expires_in", 0L))
        val refreshInSec = maxOf(d.optLong("refreshExpiresIn", 0L), d.optLong("refresh_expires_in", 0L))
        val expiresAt = if (expiresInSec > 0) System.currentTimeMillis() + expiresInSec * 1000L else jwtExp(access) ?: 0L
        val refresh = d.optString("refreshToken").ifEmpty { d.optString("refresh_token") }
        return Quad("OK", access, refresh, expiresAt, refreshInSec, "")
    }

    /** JWT exp 解析（base64url 解 payload，不验签；照插件 0.7 共性） */
    private fun jwtExp(token: String): Long? = try {
        val parts = token.split(".")
        if (parts.size < 2) null
        else {
            val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE), Charsets.UTF_8)
            val exp = JSONObject(payload).optLong("exp", 0L)
            if (exp > 0) exp * 1000L else null
        }
    } catch (_: Exception) { null }

    data class Quad(val status: String, val accessToken: String, val refreshToken: String, val expiresAt: Long, val refreshExpiresInSec: Long, val err: String) {
        companion object {
            fun pending() = Quad("PENDING", "", "", 0, 0, "")
            fun err(e: String) = Quad("ERROR", "", "", 0, 0, e)
        }
    }

    // ==================== 续期（同 buddy：X-Refresh-Token 头） ====================

    override fun refresh(acc: AccountPool.Account): Triple<String, String, Long>? {
        val r = AccountPool.channelPost(
            "$ENDPOINT/v2/plugin/auth/token/refresh",
            mapOf(
                "X-Domain" to DOMAIN,
                "User-Agent" to uaFor(""),
                "Authorization" to "Bearer ${acc.accessToken}",
                "X-Refresh-Token" to acc.refreshToken,
                "X-Auth-Refresh-Source" to "ide-main",
            ), "",
        )
        if (!r.ok) return null
        val o = try { JSONObject(r.body) } catch (_: Exception) { return null }
        val code = o.optInt("code", 0)
        val msg = o.optString("msg") + o.optString("message")
        // 终态：HTTP 401/403（上面 r.ok 已滤）、业务 401/403、message 含 expired/invalid
        if (code == 401 || code == 403 || msg.contains("expired", true) || msg.contains("invalid", true)) return null
        val d = o.optJSONObject("data") ?: o
        val access = d.optString("accessToken").ifEmpty { d.optString("access_token") }
        if (access.isEmpty()) return null
        val refresh = d.optString("refreshToken").ifEmpty { d.optString("refresh_token") }.ifEmpty { acc.refreshToken }
        val expiresInSec = maxOf(d.optLong("expiresIn", 0L), d.optLong("expires_in", 0L))
        val expiresAt = if (expiresInSec > 0) System.currentTimeMillis() + expiresInSec * 1000L
        else jwtExp(access) ?: (System.currentTimeMillis() + 20 * 3600_000L)
        return Triple(access, refresh, expiresAt)
    }

    // ==================== 对话 ====================

    override fun chatHeaders(accessToken: String): Map<String, String> = mapOf(
        "X-Domain" to DOMAIN,
        "X-Product" to "WorkBuddy", // 归属名（非 SaaS）
        "X-Product-Code" to "workbuddy",
        "X-Agent-Purpose" to "conversation",
        "X-IDE-Name" to "WorkBuddy",
        "X-IDE-Type" to "WorkBuddy",
        "X-IDE-Version" to VERSION,
    )

    /** UA 按模型族覆盖（patchBody 阶段不知道模型……改为在 SseAggregator 层注入时带 model） */
    fun chatHeadersFor(accessToken: String, model: String): Map<String, String> =
        chatHeaders(accessToken) + mapOf(
            "User-Agent" to uaFor(model),
            "Accept" to "text/event-stream",
        )

    override fun classifyError(httpStatus: Int, body: String): ChatChannel.ErrClass = when {
        httpStatus == 429 || httpStatus == 402 || body.contains("频率限制") -> ChatChannel.ErrClass.RATE_LIMIT
        httpStatus == 401 || httpStatus == 403 -> ChatChannel.ErrClass.AUTH
        body.contains("11102") -> ChatChannel.ErrClass.OTHER // service info not found=模型不可用，换号无用
        else -> ChatChannel.ErrClass.OTHER
    }

    // ==================== 模型 / 签到（无签到） ====================

    override fun checkIn(acc: AccountPool.Account) = false to "WorkBuddy 无签到接口"

    override fun fetchModels(accessToken: String): List<String> = listOf(
        "default-model", "fast-model", "balanced-model", "primary-model", "deep-model",
        "hy4-preview", "hy3", "deepseek-v4.1-flash", "gpt-5.6-sol", "gpt-5.5",
        "gemini-3.5-flash", "glm-5.3", "glm-5.2", "kimi-k3",
    )

    override fun queryCredits(acc: AccountPool.Account): Double {
        return try {
            // 余额 get-user-resource 两产品通用（creditItems[]）
            val r = AccountPool.channelPost(
                "$ENDPOINT/v2/billing/meter/get-user-resource",
                mapOf(
                    "Authorization" to "Bearer ${acc.accessToken}",
                    "X-Domain" to DOMAIN,
                    "X-Product" to "SaaS",
                    "X-Product-Code" to "workbuddy",
                    "User-Agent" to uaFor(""),
                ), "{}",
            )
            if (!r.ok) Double.NaN
            else {
                val d = JSONObject(r.body).optJSONObject("data")
                    ?.optJSONObject("Response")?.optJSONObject("Data") ?: return Double.NaN
                val accs = d.optJSONArray("Accounts") ?: return Double.NaN
                var total = 0.0
                for (i in 0 until accs.length()) {
                    val p = accs.optJSONObject(i) ?: continue
                    if (p.optInt("Status", 0) == 3) continue
                    val v = AccountPool.firstNumberOf(p, "CycleCapacityRemainPrecise", "CycleCapacityRemain") ?: continue
                    total += v
                }
                Math.round(total * 100.0) / 100.0
            }
        } catch (_: Exception) { Double.NaN }
    }

    /**
     * 余额明细（10-10 分池，同 codebuddy 口径）：与 queryCredits 同一端点/同一份响应，
     * 额外按包的 DeductionEndTime 分「长期 / 临时」两桶（窗口 15 天，插件同值）。
     */
    override fun queryCreditDetail(acc: AccountPool.Account): CreditDetail? {
        return try {
            val r = AccountPool.channelPost(
                "$ENDPOINT/v2/billing/meter/get-user-resource",
                mapOf(
                    "Authorization" to "Bearer ${acc.accessToken}",
                    "X-Domain" to DOMAIN,
                    "X-Product" to "SaaS",
                    "X-Product-Code" to "workbuddy",
                    "User-Agent" to uaFor(""),
                ), "{}",
            )
            if (!r.ok) null
            else {
                val d = JSONObject(r.body).optJSONObject("data")
                    ?.optJSONObject("Response")?.optJSONObject("Data") ?: return null
                val accs = d.optJSONArray("Accounts") ?: return null
                var total = 0.0
                var permanent = 0.0
                var ephemeral = 0.0
                var found = false
                val now = System.currentTimeMillis()
                for (i in 0 until accs.length()) {
                    val p = accs.optJSONObject(i) ?: continue
                    if (p.optInt("Status", 0) == 3) continue
                    val v = AccountPool.firstNumberOf(p, "CycleCapacityRemainPrecise", "CycleCapacityRemain") ?: continue
                    total += v
                    found = true
                    // 拿不到到期时刻 = 归长期（插件 splitBuddyCreditsByExpiry 同口径）
                    if (v > 0) {
                        val end = AccountPool.firstNumberOf(p, "DeductionEndTime")
                        if (end != null && end > 0 && end - now < AccountPool.CREDIT_EXPIRING_WINDOW_MS) ephemeral += v
                        else permanent += v
                    }
                }
                if (!found) null
                else CreditDetail(
                    Math.round(total * 100.0) / 100.0,
                    Math.round(permanent * 100.0) / 100.0,
                    Math.round(ephemeral * 100.0) / 100.0,
                )
            }
        } catch (_: Exception) { null }
    }
}
