package com.github.jing332.tts_server_android.service.systts.help

import org.json.JSONObject

/**
 * lobsterai（有道龙虾）渠道（10-09 全渠道批，协议=规格书 §3）。
 *
 * 登录 = 本地回调 + authCode（WebView 打开 portal 登录页，回调 code 由本地页承载——
 * app 侧用自定义 scheme 或手动粘贴 callback URL，第二期定；本渠道先落「凭据直填」路径）。
 *
 * 对话 = OpenAI 兼容仅 SSE @ .../api/proxy/v1/chat/completions + 双 X-LobsterAI-Client-* 头。
 * ⚠️ reasoning_effort 的 wire 值 openclawLevel：low/high/xhigh（max→xhigh）。
 * ⚠️ 额度耗尽常是 HTTP 200 + SSE 流内错误帧 + 400/中文「积分不足」——分类必须读体。
 * 续期不带 Authorization（服务端只认体里的 refreshToken）；keyfrom 一律发存档原值。
 */
object LobsteraiChannel : ChatChannel {
    override val id = "lobsterai"
    override val displayName = "LobsterAI 有道"
    override val chatBaseUrl = "https://lobsterai-server.youdao.com/api/proxy/v1"

    private const val SERVER = "https://lobsterai-server.youdao.com"

    /** 客户端能力头（模型列表准入 + reasoning_effort=off 前提，缺任一功能降级） */
    private fun capabilityHeaders(): Map<String, String> = mapOf(
        "User-Agent" to "LobsterAI/0.1.0",
        "X-LobsterAI-Client-Capabilities" to "kimi-k3-agentic-v1,thinking-level-control-v1",
        "X-LobsterAI-Client-Version" to clientVersion(),
    )

    /** 客户端版本（动态拉取失败兜底 2026.9.4；照插件正则校验） */
    private fun clientVersion(): String = try {
        val r = AccountPool.channelGet("https://api-overmind.youdao.com/openapi/get/luna/hardware/lobsterai/prod/update", emptyMap())
        if (!r.ok) "2026.9.4"
        else {
            val v = JSONObject(r.body).optJSONObject("data")?.optString("value")?.let {
                try { JSONObject(it).optString("version") } catch (_: Exception) { "" }
            }.orEmpty()
            if (Regex("^\\d+(?:\\.\\d+)*(-[0-9A-Za-z.-]+)?$").matches(v)) v else "2026.9.4"
        }
    } catch (_: Exception) { "2026.9.4" }

    // ==================== 登录 URL（规格书 3.1） ====================

    /**
     * 登录 URL：portal 登录页 hash 路由（#/login 段不能用 searchParams 构造），
     * redirect_uri 必须百分号编码后与本地回调逐字一致。纯函数。
     */
    fun buildLoginUrl(port: Int, state: String): String =
        "https://lobsterai.youdao.com/portal#/login?source=electron" +
            "&redirect_uri=${java.net.URLEncoder.encode("http://127.0.0.1:$port/auth/callback", "UTF-8")}" +
            "&state=$state"

    // ==================== 换 token / 续期 ====================

    /** authCode 换凭据（登录第二跳）：body 带 uuid/firstKeyfrom（随凭据永久持久化） */
    data class TokenResult(val accessToken: String, val refreshToken: String, val expiresAt: Long, val uid: String, val nickname: String, val err: String) {
        companion object { fun err(e: String) = TokenResult("", "", 0, "", "", e) }
    }

    fun exchange(authCode: String, uuid: String, firstKeyfrom: String): TokenResult {
        val body = JSONObject().apply {
            put("authCode", authCode)
            put("firstKeyfrom", firstKeyfrom)
            put("latestKeyfrom", System.currentTimeMillis().toString())
            put("uuid", uuid)
            put("version", clientVersion())
        }
        return parseTokenResponse(postAuth("/api/auth/exchange", body), uuid, firstKeyfrom, fallbackRefresh = "")
    }

    override fun refresh(acc: AccountPool.Account): Triple<String, String, Long>? {
        val uuid = acc.extraStr("uuid")
        val first = acc.extraStr("firstKeyfrom")
        val body = JSONObject().apply {
            put("firstKeyfrom", first) // ⚠️ 一律发存档原值（从不更新）
            put("latestKeyfrom", acc.extraStr("latestKeyfrom", first)) // 同上
            put("version", clientVersion())
            if (uuid.isNotEmpty()) put("uuid", uuid)
            put("refreshToken", acc.refreshToken)
        }
        val tr = parseTokenResponse(postAuth("/api/auth/refresh", body), uuid, first, fallbackRefresh = acc.refreshToken)
        if (tr.err.isNotEmpty()) return null
        return Triple(tr.accessToken, tr.refreshToken, tr.expiresAt)
    }

    private fun postAuth(path: String, body: JSONObject): ChannelHttpResp =
        AccountPool.channelPost(SERVER + path, mapOf("Accept" to "application/json"), body.toString())

    /** 统一解析 exchange/refresh 响应：{code:0, data:{accessToken, refreshToken?, expiresIn?}} */
    private fun parseTokenResponse(r: ChannelHttpResp, uuid: String, first: String, fallbackRefresh: String): TokenResult {
        if (!r.ok) return TokenResult.err("HTTP ${r.code}：${r.body.take(120)}")
        val o = try { JSONObject(r.body) } catch (_: Exception) { return TokenResult.err("响应不是 JSON") }
        val code = o.optInt("code", -1)
        // ⚠️ code:0 但 data:null = 凭据已失效（插件坑）
        if (code == 40100 || code == 40101) return TokenResult.err("会话已失效（$code），请重新登录")
        if (code != 0) return TokenResult.err("业务码 $code：${o.optString("msg")}")
        val d = o.optJSONObject("data") ?: return TokenResult.err("code=0 但 data 为空（凭据已失效）")
        val at = d.optString("accessToken")
        if (at.isEmpty()) return TokenResult.err("响应缺 accessToken")
        val expiresInSec = d.optLong("expiresIn", 0L)
        val expiresAt = if (expiresInSec > 0) System.currentTimeMillis() + expiresInSec * 1000L
        else jwtExp(at) ?: (System.currentTimeMillis() + 30L * 24 * 3600_000L)
        val uid = listOfNotNull(
            d.optJSONObject("user")?.optString("id")?.takeIf { it.isNotEmpty() },
            d.optJSONObject("user")?.optString("userId")?.takeIf { it.isNotEmpty() },
            d.optJSONObject("user")?.optString("yid")?.takeIf { it.isNotEmpty() },
            sha256_16(at),
        ).first()
        val nickname = d.optJSONObject("user")?.optString("nickname").orEmpty()
        return TokenResult(at, d.optString("refreshToken").ifEmpty { fallbackRefresh }, expiresAt, uid, nickname, "")
    }

    private fun sha256_16(s: String): String =
        java.security.MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }.take(16)

    private fun jwtExp(token: String): Long? = try {
        val parts = token.split(".")
        if (parts.size < 2) null
        else {
            val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE), Charsets.UTF_8)
            val exp = JSONObject(payload).optLong("exp", 0L)
            if (exp > 0) exp * 1000L else null
        }
    } catch (_: Exception) { null }

    // ==================== 对话 ====================

    override fun chatHeaders(accessToken: String): Map<String, String> = capabilityHeaders() + mapOf(
        "Accept" to "text/event-stream, application/json",
    )

    /**
     * 错误分类（照插件优先级：402 → hard 关键词 → session-dead → 429 → 404 → 5xx → 4xx。
     * hard 关键词必须排在 429/404 前——上游用 400+中文「积分不足」表达余额耗尽）。
     */
    override fun classifyError(httpStatus: Int, body: String): ChatChannel.ErrClass {
        val hard = body.contains("insufficient credit", true) || body.contains("free quota", true) ||
            body.contains("upgrade your plan", true) || body.contains("积分不足") ||
            body.contains("额度已用完") || body.contains("升级套餐")
        if (httpStatus == 402 || hard) return ChatChannel.ErrClass.RATE_LIMIT // hard-credit=额度耗尽换号
        if (body.contains("40100") || body.contains("40101")) return ChatChannel.ErrClass.AUTH
        if (httpStatus == 429) return ChatChannel.ErrClass.RATE_LIMIT
        if (httpStatus == 401 || httpStatus == 403) return ChatChannel.ErrClass.AUTH
        return ChatChannel.ErrClass.OTHER
    }

    // ==================== 签到 / 模型 / 余额 ====================

    override fun queryCredits(acc: AccountPool.Account): Double {
        return try {
            // ⚠️ 用 profile-summary（quota 端点不含活动积分）
            val r = AccountPool.channelGet(
                "$SERVER/api/user/profile-summary",
                mapOf("Authorization" to "Bearer ${acc.accessToken}") + capabilityHeaders(),
            )
            if (!r.ok) Double.NaN
            else {
                val d = JSONObject(r.body).optJSONObject("data") ?: return Double.NaN
                d.optDouble("totalCreditsRemaining", Double.NaN).let { if (it < 0) 0.0 else it }
            }
        } catch (_: Exception) { Double.NaN }
    }

    override fun fetchModels(accessToken: String): List<String> = listOf(
        "deepseek-v4-flash", "deepseek-v4-pro", "MiniMax-M3", "MiniMax-M2.7",
        "qwen3.7-max", "qwen3.7-plus", "qwen3.6-plus", "kimi-k2.7-code", "kimi-k2.7-code-highspeed",
        "kimi-k2.6", "kimi-k2.5", "doubao-seed-2-1-pro-260628", "doubao-seed-2-1-turbo-260628",
        "doubao-seed-2-0-code-preview-260215", "glm-5.2", "glm-5.1", "glm-5v-turbo", "glm-5", "kimi-k3",
    )
}
