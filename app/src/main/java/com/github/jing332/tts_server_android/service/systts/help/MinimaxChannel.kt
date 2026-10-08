package com.github.jing332.tts_server_android.service.systts.help

import org.json.JSONObject
import java.security.SecureRandom

/**
 * minimax 渠道（10-09 全渠道批，协议=插件 minimax-oauth.ts/minimax-adapter.ts）。
 *
 * 登录 = 设备码+PKCE。⚠️ pending 是 HTTP 200 + status=pending（非 OAuth 标准 400）。
 * 对话 = Anthropic Messages @ https://agent.minimax.cn/mavis/api/v1/llm/v1/messages
 * （全插件唯一 Anthropic 族的另一家；SSE 消费按 Anthropic 事件流）。
 *
 * ⚠️⚠️ token 非 JWT（mmoat_ 60 字符）→ 过期只能靠 expires_in 自算，登录/续期必须写 expiresAt。
 * 硬校验：scope 必须含 agent.default，token_type 小写 bearer。
 */
object MinimaxChannel : ChatChannel {
    override val id = "minimax"
    override val displayName = "MiniMax"
    override val chatBaseUrl = "https://agent.minimax.cn/mavis/api/v1/llm/v1"

    private const val ACCOUNT_HOST = "https://account.minimax.cn"
    private const val CLIENT_ID = "mcode-public"
    private const val SCOPE = "agent.default"
    private const val AUDIENCE = "agent-backend"

    // ==================== 登录 ====================

    data class DeviceStart(val deviceCode: String, val userCode: String, val verifyUrl: String, val verifyUrlComplete: String, val intervalSec: Int, val expiresInSec: Int, val verifier: String, val err: String)

    fun startDeviceLogin(): DeviceStart {
        val (verifier, challenge) = DeviceCodeLogin.pkce()
        val r = DeviceCodeLogin.postForm(
            "$ACCOUNT_HOST/oauth2/device/code",
            mapOf(
                "client_id" to CLIENT_ID,
                "scope" to SCOPE,
                "audience" to AUDIENCE,
                "code_challenge" to challenge,
                "code_challenge_method" to "S256",
            ),
        )
        if (!r.ok) return DeviceStart("", "", "", "", 0, 0, "", "HTTP ${r.code}：${r.body.take(120)}")
        val o = try { JSONObject(r.body) } catch (_: Exception) { return DeviceStart("", "", "", "", 0, 0, "", "响应不是 JSON") }
        val dc = o.optString("device_code")
        if (dc.isEmpty()) return DeviceStart("", "", "", "", 0, 0, "", "响应缺 device_code")
        return DeviceStart(
            dc, o.optString("user_code"),
            o.optString("verification_uri"), o.optString("verification_uri_complete"),
            o.optInt("interval", 5), o.optInt("expires_in", 300), verifier, "",
        )
    }

    /**
     * 轮询一次。返回 PENDING / DENIED / EXPIRED / OK(json)。
     * ⚠️ pending 是 HTTP 200 + status=pending；也兼容标准 400 error 形态。
     */
    fun pollOnce(deviceCode: String, verifier: String): Pair<String, JSONObject?> {
        val r = DeviceCodeLogin.postForm(
            "$ACCOUNT_HOST/oauth2/token",
            mapOf(
                "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
                "device_code" to deviceCode,
                "client_id" to CLIENT_ID,
                "code_verifier" to verifier,
            ),
        )
        val o = try { JSONObject(r.body) } catch (_: Exception) { JSONObject() }
        // 形态一：200 + status=pending
        if (r.ok && o.optString("status") == "pending") return "PENDING" to null
        // 形态二：标准 400 + error
        val err = o.optString("error")
        if (err == "authorization_pending" || o.optString("status") == "pending") return "PENDING" to null
        if (err == "slow_down") return "PENDING" to null
        if (err == "access_denied") return "DENIED" to null
        if (err == "expired_token") return "EXPIRED" to null
        val at = o.optString("access_token")
        if (at.isNotEmpty()) {
            // 硬校验（照官方）：scope 必须含 agent.default
            if (!o.optString("scope").contains(SCOPE)) return "DENIED" to null
            return "OK" to o
        }
        return "PENDING" to null
    }

    // ==================== 续期 ====================

    override fun refresh(acc: AccountPool.Account): Triple<String, String, Long>? {
        val r = DeviceCodeLogin.postForm(
            "$ACCOUNT_HOST/oauth2/token",
            mapOf(
                "grant_type" to "refresh_token",
                "refresh_token" to acc.refreshToken,
                "client_id" to CLIENT_ID,
                "scope" to SCOPE,
                "audience" to AUDIENCE,
            ),
        )
        if (!r.ok) return null
        val o = try { JSONObject(r.body) } catch (_: Exception) { return null }
        val at = o.optString("access_token")
        if (at.isEmpty()) return null
        // 响应缺 refresh_token 时回退旧值（插件同语义）
        val rt = o.optString("refresh_token").ifEmpty { acc.refreshToken }
        val expiresAt = System.currentTimeMillis() + o.optLong("expires_in", 0L) * 1000L
        return Triple(at, rt, expiresAt)
    }

    // ==================== 对话（Anthropic Messages） ====================

    override fun chatHeaders(accessToken: String): Map<String, String> = mapOf(
        "Content-Type" to "application/json; charset=utf-8",
        "Accept" to "text/event-stream",
    )

    override fun classifyError(httpStatus: Int, body: String): ChatChannel.ErrClass = when {
        httpStatus == 401 || httpStatus == 403 -> ChatChannel.ErrClass.AUTH
        httpStatus == 429 -> ChatChannel.ErrClass.RATE_LIMIT
        else -> ChatChannel.ErrClass.OTHER
    }

    // ==================== 签到 / 模型 ====================

    override fun checkIn(acc: AccountPool.Account): Pair<Boolean, String> {
        return try {
            // ⚠️ timezone_id 是 query 且必填（放头无效）；业务码在 base_resp.status_code
            val tz = java.net.URLEncoder.encode("Asia/Shanghai", "UTF-8")
            val st = DeviceCodeLogin.get(
                "https://agent.minimax.cn/minimax-cloud/api/v1/signin/status?timezone_id=$tz",
                mapOf("Authorization" to "Bearer ${acc.accessToken}"),
            )
            if (st.ok) {
                val so = JSONObject(st.body)
                val d = so.optJSONObject("data")
                if (d != null && d.optBoolean("today_signed", d.optBoolean("checked_in", false)))
                    return true to "今日已签到"
                val claim = DeviceCodeLogin.postJson(
                    "https://agent.minimax.cn/minimax-cloud/api/v1/signin/claim?timezone_id=$tz",
                    "{}", mapOf("Authorization" to "Bearer ${acc.accessToken}"),
                )
                if (!claim.ok) return false to "HTTP ${claim.code}"
                val co = JSONObject(claim.body)
                val br = co.optJSONObject("base_resp")
                val sc = br?.optInt("status_code", -1) ?: -1
                if (sc == 0) true to "签到成功"
                else false to "业务码 $sc：${br?.optString("message") ?: ""}"
            } else return false to "HTTP ${st.code}：${st.body.take(120)}"
        } catch (e: Exception) {
            return false to (e.message ?: "签到失败")
        }
    }

    override fun fetchModels(accessToken: String): List<String> {
        return try {
            val r = DeviceCodeLogin.get("https://agent.minimax.cn/mavis/api/v1/models?region=cn&buildEnv=prod")
            if (!r.ok) fallback()
            else {
                val o = JSONObject(r.body)
                val arr = o.optJSONArray("data") ?: o.optJSONArray("models") ?: return fallback()
                (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("id") }
                    .filter { it.isNotEmpty() }
                    .ifEmpty { fallback() }
            }
        } catch (_: Exception) { return fallback() }
    }

    private fun fallback(): List<String> = listOf(
        "MiniMax-M3.1-Flash-Preview", "MiniMax-M3", "MiniMax-M2.7-highspeed", "MiniMax-M2.7",
    )
}
