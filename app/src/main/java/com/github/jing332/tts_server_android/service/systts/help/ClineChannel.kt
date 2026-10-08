package com.github.jing332.tts_server_android.service.systts.help

import org.json.JSONObject

/**
 * cline 渠道（10-09 全渠道批，协议=插件 cline-oauth.ts/cline.ts/cline-rate-limit.ts）。
 *
 * 登录 = WorkOS 设备码（RFC 标准，slow_down 累积退避）→ 注册换 cline 凭据。
 * 对话 = OpenAI 兼容 @ https://api.cline.bot/api/v1/chat/completions（仅 SSE）。
 * ⚠️ access_token 的 `workos:` 前缀不可剥（剥掉即 401）。
 * ⚠️ 续期 body 是驼峰 refreshToken/grantType（OAuth 标准下划线写错=泛化失败）。
 */
object ClineChannel : ChatChannel {
    override val id = "cline"
    override val displayName = "Cline"
    override val chatBaseUrl = "https://api.cline.bot/api/v1"

    private const val WORKOS_BASE = "https://api.workos.com"
    private const val WORKOS_CLIENT_ID = "client_01K3A541FN8TA3EPPHTD2325AR"

    /** 客户端头族（注册/续期/对话三处共用） */
    private fun clientHeaders(): Map<String, String> = mapOf(
        "HTTP-Referer" to "https://cline.bot",
        "X-Title" to "Cline",
        "X-IS-MULTIROOT" to "false",
        "X-CLIENT-TYPE" to "cline-sdk",
        "Accept" to "application/json",
    )

    // ==================== 设备码登录 ====================

    /** 申请结果：两阶段弹窗需要 user_code/expires_in 透出（规格书 §6.1） */
    data class DeviceStart(
        val deviceCode: String, val userCode: String,
        val verifyUrl: String, val verifyUrlComplete: String,
        val intervalSec: Int, val expiresInSec: Int, val err: String,
    ) {
        companion object {
            fun err(e: String) = DeviceStart("", "", "", "", 0, 0, e)
        }
    }

    /** 申请设备码；失败返回 err 非空 */
    fun startDeviceLogin(): DeviceStart {
        val r = DeviceCodeLogin.postForm(
            "$WORKOS_BASE/user_management/authorize/device",
            mapOf("client_id" to WORKOS_CLIENT_ID),
        )
        if (!r.ok) return DeviceStart.err("HTTP ${r.code}：${r.body.take(120)}")
        val o = try { JSONObject(r.body) } catch (_: Exception) { return DeviceStart.err("响应不是 JSON") }
        val dc = o.optString("device_code")
        if (dc.isEmpty()) return DeviceStart.err("响应缺 device_code")
        return DeviceStart(
            dc, o.optString("user_code"),
            o.optString("verification_uri"), o.optString("verification_uri_complete"),
            o.optInt("interval", 5), o.optInt("expires_in", 300), "",
        )
    }

    /**
     * 轮询一次（调用方按 interval 循环）：返回
     * PENDING / DENIED / EXPIRED / ("OK" to json)
     */
    fun pollOnce(deviceCode: String): Pair<String, JSONObject?> {
        val r = DeviceCodeLogin.postForm(
            "$WORKOS_BASE/user_management/authenticate",
            mapOf(
                "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
                "device_code" to deviceCode,
                "client_id" to WORKOS_CLIENT_ID,
            ),
        )
        val o = try { JSONObject(r.body) } catch (_: Exception) { JSONObject() }
        val err = o.optString("error")
        if (!r.ok && err == "authorization_pending") return "PENDING" to null
        if (err == "slow_down") return "PENDING" to null // 调用方负责累积退避
        if (err == "access_denied") return "DENIED" to null
        if (err == "expired_token" || err == "invalid_grant") return "EXPIRED" to null
        val token = o.optString("access_token")
        if (token.isNotEmpty()) return "OK" to o
        return "PENDING" to null
    }

    /**
     * 轮询一次（区分 slow_down 的扩展版，10-10 两阶段弹窗专用；pollOnce 保持原样未动）。
     * 返回 PENDING / SLOW_DOWN / DENIED / EXPIRED / ("OK" to json)。
     * 之所以单独加这个函数：规格书 §6.1 要求 slow_down 必须真退避（interval +1 秒累积），
     * 而 pollOnce 把 slow_down 归并成了 PENDING，调用方无从感知。
     */
    fun pollOnceEx(deviceCode: String): Pair<String, JSONObject?> {
        val r = DeviceCodeLogin.postForm(
            "$WORKOS_BASE/user_management/authenticate",
            mapOf(
                "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
                "device_code" to deviceCode,
                "client_id" to WORKOS_CLIENT_ID,
            ),
        )
        val o = try { JSONObject(r.body) } catch (_: Exception) { JSONObject() }
        val err = o.optString("error")
        if (!r.ok && err == "authorization_pending") return "PENDING" to null
        if (err == "slow_down") return "SLOW_DOWN" to null
        if (err == "access_denied") return "DENIED" to null
        if (err == "expired_token" || err == "invalid_grant") return "EXPIRED" to null
        val token = o.optString("access_token")
        if (token.isNotEmpty()) return "OK" to o
        return "PENDING" to null
    }

    /** 用 WorkOS token 注册换 cline 凭据：→ (accessToken 带 workos: 前缀, refreshToken, expiresAtMs, accountId, email, err) */
    fun register(workosToken: String, workosRefresh: String): Sextuple {
        val body = JSONObject().apply {
            put("accessToken", workosToken)
            put("refreshToken", workosRefresh)
        }
        val r = DeviceCodeLogin.postJson("$chatBaseUrl/auth/register", body.toString(), clientHeaders())
        if (!r.ok) return Sextuple.err("HTTP ${r.code}：${r.body.take(120)}")
        val o = try { JSONObject(r.body) } catch (_: Exception) { return Sextuple.err("响应不是 JSON") }
        // ⚠️ success && data.accessToken 才算成功（失败信封裸看像成功）
        if (!o.optBoolean("success", false)) return Sextuple.err("注册失败：${o.optString("error", o.toString().take(120))}")
        val d = o.optJSONObject("data") ?: return Sextuple.err("响应缺 data")
        val at = d.optString("accessToken")
        if (at.isEmpty()) return Sextuple.err("响应缺 accessToken")
        return Sextuple(
            ensureWorkosPrefix(at),
            d.optString("refreshToken"),
            parseExpiry(d.optString("expiresAt")),
            d.optJSONObject("userInfo")?.optString("clineUserId").orEmpty(),
            d.optJSONObject("userInfo")?.optString("email").orEmpty(),
            "",
        )
    }

    data class Sextuple(
        val accessToken: String, val refreshToken: String, val expiresAt: Long,
        val accountId: String, val email: String, val err: String,
    ) { companion object { fun err(e: String) = Sextuple("", "", 0, "", "", e) } }

    /** 幂等补 workos: 前缀（不可剥，剥掉即 401） */
    private fun ensureWorkosPrefix(t: String) = if (t.startsWith("workos:")) t else "workos:$t"

    /** ISO8601 / 秒 / 毫秒 三形态兼容（照插件） */
    internal fun parseExpiry(raw: String): Long {
        val t = raw.trim()
        if (t.isEmpty()) return 0L
        t.toLongOrNull()?.let { n -> return if (n < 10_000_000_000L) n * 1000L else n }
        return runCatching { java.time.Instant.parse(t).toEpochMilli() }.getOrDefault(0L)
    }

    // ==================== 续期 ====================

    override fun refresh(acc: AccountPool.Account): Triple<String, String, Long>? {
        val body = JSONObject().apply {
            put("refreshToken", acc.refreshToken) // ⚠️ 驼峰
            put("grantType", "refresh_token")
        }
        val r = DeviceCodeLogin.postJson("$chatBaseUrl/auth/refresh", body.toString(), clientHeaders())
        if (!r.ok) return null
        val o = try { JSONObject(r.body) } catch (_: Exception) { return null }
        if (!o.optBoolean("success", false)) return null
        val d = o.optJSONObject("data") ?: return null
        val at = d.optString("accessToken")
        if (at.isEmpty()) return null
        return Triple(
            ensureWorkosPrefix(at),
            d.optString("refreshToken").ifEmpty { acc.refreshToken },
            parseExpiry(d.optString("expiresAt")).takeIf { it > 0 } ?: (System.currentTimeMillis() + 20 * 3600_000L),
        )
    }

    // ==================== 对话 ====================

    override fun chatHeaders(accessToken: String): Map<String, String> = clientHeaders() +
        mapOf("Accept" to "text/event-stream")

    override fun classifyError(httpStatus: Int, body: String): ChatChannel.ErrClass = when {
        httpStatus == 429 || httpStatus == 402 -> ChatChannel.ErrClass.RATE_LIMIT // 402=额度耗尽换号
        httpStatus == 401 || httpStatus == 403 -> ChatChannel.ErrClass.AUTH
        else -> ChatChannel.ErrClass.OTHER // 400/5xx 换号无用
    }

    // ==================== 模型 / 余额 ====================

    override fun fetchModels(accessToken: String): List<String> = try {
        val r = DeviceCodeLogin.get("$chatBaseUrl/models", clientHeaders())
        if (!r.ok) fallbackModels()
        else parseModelList(r.body) { it.isNotEmpty() }.ifEmpty { fallbackModels() }
    } catch (_: Exception) { fallbackModels() }

    private fun fallbackModels(): List<String> = listOf(
        "claude-sonnet-4.5", "claude-opus-4.6", "gemini-3-pro", "gpt-5.3-codex",
        "deepseek-v4.1", "kimi-k3", "qwen3.8-max", "x-ai/grok-code-fast-1",
    )

    private fun parseModelList(body: String, filter: (String) -> Boolean): List<String> {
        return try {
        val o = JSONObject(body)
        val arr = o.optJSONArray("models") ?: o.optJSONArray("data") ?: return emptyList()
        (0 until arr.length()).mapNotNull {
            when (val v = arr.opt(it)) {
                is String -> v
                is JSONObject -> v.optString("id")
                else -> null
            }
        }.filter(filter)
        } catch (_: Exception) { emptyList() }
    }

    /** 余额（credits）：GET /users/me + quota 端点。返回 NaN=查不到 */
    override fun queryCredits(acc: AccountPool.Account): Double = try {
        val r = DeviceCodeLogin.get(
            "$chatBaseUrl/users/me",
            clientHeaders() + mapOf("Authorization" to "Bearer ${acc.accessToken}"),
        )
        if (!r.ok) Double.NaN
        else {
            val o = JSONObject(r.body)
            val d = o.optJSONObject("data") ?: o
            d.optDouble("credits", Double.NaN)
        }
    } catch (_: Exception) { Double.NaN }
}
