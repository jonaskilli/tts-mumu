package com.github.jing332.tts_server_android.service.systts.help

import org.json.JSONObject

/**
 * raccoon（商汤小浣熊）渠道（10-09 全渠道批，协议=规格书 §8）。
 *
 * 登录 = 自造 qrcode_code 扫码轮询（实测任意 32hex 都被接受；微信内扫码完成授权）
 * 或短信（手机号 AES-128-CFB + 阿里云滑块——滑块需 WebView，第二期）。
 * 扫码 app 侧完全可行：本地生成 code → 画二维码 → 轮询 login_with_qrcode_code。
 *
 * 对话 = 标准 OpenAI @ xiaohuanxiong.com/api/web/llm/v2/chat/completions。
 * ⚠️ refresh 可能不回新 refresh_token——必须保留旧值（否则续期一次毁号）。
 * ⚠️ 轮询异常一律降级 pending（误判 success 会拿空 token 卡死）。
 * token 寿命约 3h，官方提前 300s 刷新。
 */
object RaccoonChannel : ChatChannel {
    override val id = "raccoon"
    override val displayName = "Raccoon 小浣熊"
    override val chatBaseUrl = "https://xiaohuanxiong.com/api/web/llm/v2"

    private const val BASE = "https://xiaohuanxiong.com"
    private const val AUTH = "$BASE/api/web/auth/v1"
    private const val POINTS = "$BASE/api/web/points/v1"

    private fun baseHeaders(token: String? = null): Map<String, String> {
        val m = mutableMapOf(
            "User-Agent" to "Raccoon Work/1.0.35 (Windows)",
            "X-Client-Platform" to "desktop-windows",
            "X-Client-Version" to "v1.0.35",
            "X-Client-Device-ID" to DeviceCodeLogin.randomHex(16), // device_id 由 extra 持久化时覆盖
        )
        if (token != null) m["Authorization"] = "Bearer $token"
        return m
    }

    // ==================== 扫码登录 ====================

    /** 自造 32hex code → 二维码内容（公开页，微信内完成授权） */
    fun qrcodeContent(): Pair<String, String> {
        val code = DeviceCodeLogin.randomHex(16)
        return code to "$BASE/login/mp?code=$code&appname=${java.net.URLEncoder.encode("商汤小浣熊官网", "UTF-8")}"
    }

    /** 轮询一次：PENDING / LOGGING / CANCELED / (OK with tokens)。异常一律 pending */
    fun pollQrcode(code: String): Triple<String, String, String> {
        return try {
            val r = AccountPool.channelPost(
                "$AUTH/login_with_qrcode_code",
                baseHeaders() + mapOf("Content-Type" to "application/json"),
                JSONObject().put("qrcode_code", code).toString(),
            )
            if (!r.ok) return Triple("PENDING", "", "")
            val o = JSONObject(r.body)
            val status = o.optString("status", "pending")
            when (status) {
                "pending" -> Triple("PENDING", "", "")
                "logging" -> Triple("LOGGING", "", "")
                "canceled" -> Triple("CANCELED", "", "")
                "success" -> {
                    val d = o.optJSONObject("data") ?: return Triple("PENDING", "", "") // 异常降级
                    val at = d.optString("access_token")
                    val rt = d.optString("refresh_token")
                    if (at.isEmpty()) Triple("PENDING", "", "") else Triple("OK", at, rt)
                }
                else -> Triple("PENDING", "", "")
            }
        } catch (_: Exception) {
            Triple("PENDING", "", "") // ⚠️ 任何轮询异常降级 pending（误判 success 卡死）
        }
    }

    // ==================== 续期（⚠️ 保留旧 refresh_token） ====================

    override fun refresh(acc: AccountPool.Account): Triple<String, String, Long>? = try {
        val r = AccountPool.channelPost(
            "$AUTH/refresh",
            baseHeaders() + mapOf("Content-Type" to "application/json"),
            JSONObject().put("refresh_token", acc.refreshToken).toString(),
        )
        if (r.code == 401 || r.body.contains("200003")) null // 终态：重登
        else {
            val o = JSONObject(r.body)
            if (o.optInt("code", -1) != 0) null
            else {
                val d = o.optJSONObject("data") ?: return null
                val at = d.optString("access_token")
                if (at.isEmpty()) null
                else Triple(
                    at,
                    d.optString("refresh_token").ifEmpty { acc.refreshToken }, // ⚠️ 必须保留旧值
                    jwtExp(at) ?: (System.currentTimeMillis() + 3 * 3600_000L),
                )
            }
        }
    } catch (_: Exception) { null }

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

    override fun chatHeaders(accessToken: String): Map<String, String> = baseHeaders(accessToken) + mapOf(
        "X-Org-Code" to "", // office_identity（个人=空串）
        "X-Raccoon-Language" to "zh",
        "Accept" to "text/event-stream",
    )

    override fun classifyError(httpStatus: Int, body: String): ChatChannel.ErrClass = when {
        body.contains("200003") || httpStatus == 401 -> ChatChannel.ErrClass.AUTH
        httpStatus == 429 -> ChatChannel.ErrClass.RATE_LIMIT
        body.contains("100006") || body.contains("100003") -> ChatChannel.ErrClass.OTHER
        else -> ChatChannel.ErrClass.OTHER
    }

    // ==================== 余额 / 模型 ====================

    override fun queryCredits(acc: AccountPool.Account): Double = try {
        val r = AccountPool.channelGet("$POINTS/balance", baseHeaders(acc.accessToken))
        if (!r.ok) Double.NaN
        else {
            val d = JSONObject(r.body).optJSONObject("data") ?: return Double.NaN
            if (!d.has("available_points")) Double.NaN // 形状不对不编造
            else d.optDouble("available_points", Double.NaN)
        }
    } catch (_: Exception) { Double.NaN }

    override fun fetchModels(accessToken: String): List<String> = listOf(
        "sn-sensenova-6-8-flash", "sn-sensenova-6-8-flash-lite", "sn-glm-5-3",
        "sn-deepseek-v4-flash", "sn-kimi-k3", "sn-qwen-3.8-max",
    )
}
