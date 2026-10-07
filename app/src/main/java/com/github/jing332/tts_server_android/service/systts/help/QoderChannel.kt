package com.github.jing332.tts_server_android.service.systts.help

import org.json.JSONObject

/**
 * qoder/qodercn（阿里）渠道（10-09 全渠道批，协议=规格书 §4）。
 *
 * ⚠️⚠️ **本渠道对话被 WASM 加密链锁死**：请求体加密、`Bearer COSY.<载荷>.<签名>` 鉴权、
 * 模型目录解密全走官方内嵌 298KB WASM（qoder-auth-wasm），普通 Bearer 一律
 * `403 Signature invalid`——全清单唯一没有纯手写复刻路径的环节。
 *
 * 本期落**可复刻部分**：设备码登录引擎（PKCE 轮询）+ 续期 + 签到/余额（/sash/ 端点
 * 无需 WASM 签名）。对话置 available=false（UI 显示「待接入」，等 WASM 移植评估：
 * Android 侧可试 WebView 跑 WASM 或嵌入 qjs/wasm 运行时——单独二期任务）。
 */
object QoderChannel : ChatChannel {
    override val id = "qoder"
    override val displayName = "Qoder 阿里"
    override val chatBaseUrl = "https://api2.qoder.sh/algo/api/v2"
    override val available = false // 对话待 WASM 链

    private const val AUTH_BASE = "https://qoder.com"
    private const val OPEN_API_BASE = "https://openapi.qoder.sh"
    private const val CLIENT_ID = "e883ade2-e6e3-4d6d-adf7-f92ceff5fdcb" // ⚠️ 必须 prod clientId

    // ==================== 设备码登录（PKCE 轮询，404=未授权继续） ====================

    data class QoderStart(val authorizeUrl: String, val verifier: String, val nonce: String, val machineId: String, val err: String)

    fun startLogin(): QoderStart {
        val (verifier, challenge) = DeviceCodeLogin.pkce()
        val nonce = DeviceCodeLogin.randomUuid()
        val machineId = DeviceCodeLogin.randomUuid()
        val url = "$AUTH_BASE/device/selectAccounts" +
            "?challenge=${java.net.URLEncoder.encode(challenge, "UTF-8")}" +
            "&challenge_method=S256&nonce=$nonce&machine_id=$machineId&client_id=$CLIENT_ID"
        return QoderStart(url, verifier, nonce, machineId, "")
    }

    /** 轮询一次：404=未授权继续；2xx 解 data（token/user_id/user_name 必读） */
    fun pollOnce(nonce: String, verifier: String): Pair<String, JSONObject?> {
        val r = AccountPool.channelGet(
            "$OPEN_API_BASE/api/v1/deviceToken/poll?nonce=$nonce&verifier=${java.net.URLEncoder.encode(verifier, "UTF-8")}&challenge_method=S256",
            mapOf("User-Agent" to "qoder/1.0.0", "Accept" to "application/json"),
        )
        if (r.code == 404) return "PENDING" to null // 尚未授权
        if (!r.ok) return "PENDING" to null // 网络抖动继续（连续失败计数由调用方做）
        val d = try { JSONObject(r.body).optJSONObject("data") } catch (_: Exception) { null }
            ?: return "PENDING" to null
        val token = listOf("token", "device_token", "access_token").firstNotNullOfOrNull { d.optString(it).takeIf { s -> s.isNotEmpty() } }
        if (token.isNullOrEmpty()) return "PENDING" to null
        return "OK" to d
    }

    // ==================== 续期 ====================

    override fun refresh(acc: AccountPool.Account): Triple<String, String, Long>? {
        return try {
            val machineId = acc.extraStr("machine_id")
            val r = AccountPool.channelPost(
                "$OPEN_API_BASE/api/v1/deviceToken/refresh",
                mapOf("User-Agent" to "qoder/1.0.0", "Accept" to "application/json"),
                JSONObject().put("refresh_token", acc.refreshToken).put("machine_id", machineId).toString(),
            )
            if (!r.ok) null // 401/403=终态；网络失败不能判终态（本层区分不了，调度器按失败重试）
            else {
                val d = JSONObject(r.body).optJSONObject("data") ?: return null
                val token = listOf("device_token", "token", "access_token").firstNotNullOfOrNull { d.optString(it).takeIf { s -> s.isNotEmpty() } }
                if (token.isNullOrEmpty()) null
                else {
                    val expire = parseMillis(d.opt("expire_time"))
                        ?: (System.currentTimeMillis() + 24 * 3600_000L)
                    Triple(token, d.optString("refresh_token").ifEmpty { acc.refreshToken }, expire)
                }
            }
        } catch (_: Exception) { null }
    }

    /** ISO/秒/毫秒三形态兼容；解析失败不填 0（照插件） */
    private fun parseMillis(v: Any?): Long? = when (v) {
        is Number -> if (v.toLong() < 10_000_000_000L) v.toLong() * 1000 else v.toLong()
        is String -> v.trim().removeSurrounding("\"").toLongOrNull()?.let { if (it < 10_000_000_000L) it * 1000 else it }
            ?: runCatching { java.time.Instant.parse(v).toEpochMilli() }.getOrNull()
        else -> null
    }

    // ==================== 对话（WASM 锁死，占位） ====================

    override fun chatHeaders(accessToken: String): Map<String, String> = emptyMap() // available=false 不走

    override fun classifyError(httpStatus: Int, body: String): ChatChannel.ErrClass = when {
        body.contains("105") && body.contains("auth") -> ChatChannel.ErrClass.AUTH
        httpStatus == 401 -> ChatChannel.ErrClass.AUTH
        body.contains("10605") -> ChatChannel.ErrClass.RATE_LIMIT // model_queued
        body.contains("110") -> ChatChannel.ErrClass.RATE_LIMIT // billing 日额
        else -> ChatChannel.ErrClass.OTHER
    }

    // ==================== 签到 / 余额（/sash/ 无需 WASM） ====================

    override fun queryCredits(acc: AccountPool.Account): Double {
        return try {
            // ⚠️ 余额不只在 userQuota：资源包在 addOnQuota，只读 userQuota 显示 0
            val r = AccountPool.channelGet(
                "$OPEN_API_BASE/sash/api/v2/me/usage",
                mapOf("Authorization" to "Bearer ${acc.accessToken}", "Cosy-ClientType" to "5", "User-Agent" to "Qoder"),
            )
            if (!r.ok) Double.NaN
            else {
                val d = JSONObject(r.body).optJSONObject("data") ?: return Double.NaN
                val usage = d.optJSONObject("qoderUsage") ?: return Double.NaN
                var remain = 0.0
                for (key in listOf("userQuota", "addOnQuota")) {
                    val q = usage.optJSONObject(key) ?: continue
                    remain += q.optDouble("remaining", 0.0)
                }
                remain
            }
        } catch (_: Exception) { Double.NaN }
    }

    override fun fetchModels(accessToken: String): List<String> = listOf(
        "auto", "ultimate", "performance", "efficient", "smodel", "cmodel",
        "qmodel_38max", "qfmodel", "qmodel_latest", "qmodel", "kmodel_latest", "kmodel",
        "gmodel", "gfmodel", "dmodel", "dfmodel", "mmodel",
    )
}
