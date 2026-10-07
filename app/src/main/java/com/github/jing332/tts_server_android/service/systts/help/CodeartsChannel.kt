package com.github.jing332.tts_server_android.service.systts.help

import org.json.JSONObject
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * codearts（华为云 CodeArts）渠道（10-09 全渠道批，协议=规格书 §1）。
 *
 * 对话 = OpenAI 形 @ snap-access /api/v2/chat/completions，鉴权 = **SDK-HMAC-SHA256 签名**
 * （非 Bearer）。⚠️ Agent-Type/X-Language 头**签名后追加**（混进签名必 401 APIG.0301）。
 * ⚠️ 额度用尽 = HTTP 200 + SSE 流内 InferHub.4291.200（按 UTC+8 日切标记）；
 * ⚠️ 判 429 必须数字边界锚定（4291 误命中=30 分钟零输出，插件真实事故）。
 *
 * 登录 = Portal 授权 + PKCE + DPoP(ES256) + 本地回调（端口≥10000）——第二期 WebView 宿主；
 * 本期落凭据直填（用户从插件导出的 AK/SK/ST）+ 续期 + 对话 + 签到主体。
 */
object CodeartsChannel : ChatChannel {
    override val id = "codearts"
    override val displayName = "CodeArts 华为云"
    override val chatBaseUrl = "https://snap-access.cn-north-4.myhuaweicloud.com/api/v2"
    override val rateLimitFallbackMs: Long
        get() = ZcodeChannel.utc8DayEnd() - System.currentTimeMillis() // 额度 UTC+8 日切

    private const val SNAP_ACCESS = "https://snap-access.cn-north-4.myhuaweicloud.com"
    private const val STS_HOST = "sts.cn-north-4.myhuaweicloud.com"

    // ==================== SDK-HMAC-SHA256 签名（照插件 1.4） ====================

    /**
     * 构造签名头族。extraSigned=参与签名的业务头（x-security-token/maas_type 等）。
     * 返回完整头 map（含 Authorization；Agent-Type/X-Language 由调用方事后加，不进签名）。
     */
    fun signedHeaders(
        accessKey: String, secretKey: String, securityToken: String,
        method: String, urlPath: String, query: String, body: String,
        extraSigned: Map<String, String> = emptyMap(),
    ): Map<String, String> {
        val sdkDate = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(java.time.ZoneId.of("UTC")).format(java.time.Instant.now())
        val payloadHash = sha256Hex(body)
        val signed = sortedMapOf(
            "host" to "snap-access.cn-north-4.myhuaweicloud.com",
            "x-sdk-content-sha256" to payloadHash,
            "x-sdk-date" to sdkDate,
        )
        if (securityToken.isNotEmpty()) signed["x-security-token"] = securityToken
        if (method != "GET") signed["content-type"] = "application/json"
        signed.putAll(extraSigned)
        // canonical request：METHOD\nURI(补/)\nquery\n头行\n\nSignedHeaders\npayloadHash
        val uri = if (urlPath.endsWith("/")) urlPath else "$urlPath/"
        val headerLines = signed.entries.joinToString("\n") { "${it.key}:${it.value}" }
        val signedHeadersStr = signed.keys.joinToString(";")
        val canonical = "$method\n$uri\n$query\n$headerLines\n\n$signedHeadersStr\n$payloadHash"
        // 串行签名：SDK-HMAC-SHA256\n{dateStamp}\n{sha256(canonical)}
        val dateStamp = sdkDate.substring(0, 8)
        val stringToSign = "SDK-HMAC-SHA256\n$dateStamp\n${sha256Hex(canonical)}"
        val sig = hmacSha256Hex(secretKey.toByteArray(Charsets.UTF_8), stringToSign)
        val out = signed.toMutableMap()
        out["Authorization"] = "SDK-HMAC-SHA256 Access=$accessKey, SignedHeaders=$signedHeadersStr, Signature=$sig"
        return out
    }

    private fun sha256Hex(s: String): String =
        java.security.MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun hmacSha256Hex(key: ByteArray, data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    // ==================== 凭据（extra 字段：ak/sk/st） ====================

    private fun ak(acc: AccountPool.Account) = acc.extraStr("access_key_id")
    private fun sk(acc: AccountPool.Account) = acc.extraStr("secret_access_key")
    private fun st(acc: AccountPool.Account) = acc.extraStr("security_token")

    // ==================== 续期（STS token 端点 + DPoP） ====================

    override fun refresh(acc: AccountPool.Account): Triple<String, String, Long>? {
        // STS 刷新要 DPoP(ES256) 头 + refresh_token form 体；DPoP 密钥对在 extra 持久化
        // （第二期 WebView 登录一并落 ES256 生成；本期凭据直填路径 ST 由用户侧给全）
        val refreshToken = acc.refreshToken
        val verifier = acc.extraStr("code_verifier")
        if (refreshToken.isEmpty() || verifier.isEmpty()) return null
        val form = "client_id=codearts-agent&code_verifier=${java.net.URLEncoder.encode(verifier, "UTF-8")}" +
            "&grant_type=refresh_token&refresh_token=${java.net.URLEncoder.encode(refreshToken, "UTF-8")}"
        val r = AccountPool.channelPost("https://$STS_HOST/v1/oauth2/tokens", mapOf("Content-Type" to "application/x-www-form-urlencoded"), form)
        if (!r.ok) return null
        val o = try { JSONObject(r.body) } catch (_: Exception) { return null }
        // 终态：invalid_grant / ExpiredRefreshToken（⚠️ InvalidDPoPHeader 不是终态）
        val errCode = o.optString("error_code") + o.optString("error")
        if (errCode.contains("invalid_grant") || errCode.contains("ExpiredRefreshToken")) return null
        val c = o.optJSONObject("credentials") ?: return null
        val newAk = c.optString("access_key_id")
        if (newAk.isEmpty()) return null
        // 落 extra（调用方 AccountRefreshScheduler 只更新 token 三元组——AK/SK 变化需走 saveExtras）
        pendingExtraUpdate = JSONObject()
            .put("access_key_id", newAk)
            .put("secret_access_key", c.optString("secret_access_key"))
            .put("security_token", c.optString("security_token"))
        val expiresAt = parseIsoOrPlus(c.optString("expiration"), 24 * 3600_000L)
        return Triple(newAk, acc.refreshToken, expiresAt)
    }

    /** refresh() 给 AccountPool 的 AK/SK/ST 落盘载荷（线程安全：调度器单线程串行） */
    @Volatile internal var pendingExtraUpdate: JSONObject? = null
        private set

    internal fun clearPendingExtraUpdate() { pendingExtraUpdate = null }

    private fun parseIsoOrPlus(iso: String, plusMs: Long): Long =
        runCatching { java.time.Instant.parse(iso).toEpochMilli() }.getOrElse { System.currentTimeMillis() + plusMs }

    // ==================== 对话 ====================

    override fun chatHeaders(accessToken: String): Map<String, String> = mapOf(
        // 占位：真实签名头由 SseAggregator 层调 signedHeaders 生成（需要 body + 路径）
        "Content-Type" to "application/json",
        "lang" to "en",
        "Chat-Id" to DeviceCodeLogin.randomUuid().replace("-", ""),
        "Session-Id" to DeviceCodeLogin.randomUuid().replace("-", ""),
    )

    /**
     * 签名后追加头：Agent-Type: PromptCenter + X-Language: zh-cn（不参与签名）。
     * SseAggregator 对本渠道走 chatHeadersForSigned(body)。
     */
    fun chatHeadersForSigned(acc: AccountPool.Account, body: String, model: String, benefit: Boolean): Map<String, String> {
        val extra = if (benefit) mapOf("maas_type" to "benefit") else emptyMap()
        val signed = signedHeaders(ak(acc), sk(acc), st(acc), "POST", "/api/v2/chat/completions", "", body, extra)
        return signed + mapOf(
            "Agent-Type" to "PromptCenter",
            "X-Language" to "zh-cn",
            "Accept" to "text/event-stream",
        ) + chatHeaders(accessToken = acc.accessToken).filterKeys { it == "Chat-Id" || it == "Session-Id" || it == "lang" }
    }

    /**
     * 错误分类（⚠️ 429 数字边界锚定——额度码 4291 误命中=插件真实事故；
     * InferHub.4291.200 额度用尽=不可重试，UTC+8 日切标记换号）。
     */
    override fun classifyError(httpStatus: Int, body: String): ChatChannel.ErrClass {
        val rate429 = Regex("(^|[^0-9])429([^0-9]|$)").containsMatchIn(body) || httpStatus == 429
        if (body.contains("InferHub.4291")) return ChatChannel.ErrClass.RATE_LIMIT // insufficient quota
        if (body.contains("APIG.0602") || body.contains("APIG.0301")) return ChatChannel.ErrClass.AUTH
        if (rate429 || body.contains("81111") || body.contains("TM.00001041")) return ChatChannel.ErrClass.RATE_LIMIT
        if (body.contains("InferHub.4004")) return ChatChannel.ErrClass.OTHER // 无 benefit 包：去 maas_type 重试
        if (httpStatus == 401 || httpStatus == 403) return ChatChannel.ErrClass.AUTH
        return ChatChannel.ErrClass.OTHER
    }

    // ==================== 签到（三步+确认，均签名） ====================

    override fun checkIn(acc: AccountPool.Account): Pair<Boolean, String> {
        return try {
            // 活动列表 → USER_LOGIN → claim → confirm
            val lr = AccountPool.channelGet(SNAP_ACCESS + "/snap-manager/v1/ops/delivery?channel=IDE", signedHeaders(ak(acc), sk(acc), st(acc), "GET", "/snap-manager/v1/ops/delivery", "channel=IDE", ""))
            if (!lr.ok) return false to "HTTP ${lr.code}"
            val items = JSONObject(lr.body).optJSONObject("data")?.optJSONArray("items") ?: return false to "响应无 items"
            var campaignId = ""
            for (i in 0 until items.length()) {
                val it = items.optJSONObject(i) ?: continue
                if (it.optString("type") == "USER_LOGIN") {
                    val status = it.optString("status")
                    if (status !in setOf("CLAIMED", "CONFIRMED", "CONSUMED")) campaignId = it.opt("campaignId").toString()
                    break
                }
            }
            if (campaignId.isEmpty()) return true to "今日已签到"
            val claimBody = JSONObject().put("campaignId", campaignId).put("channel", "IDE")
            val cr = AccountPool.channelPost(
                "$SNAP_ACCESS/snap-manager/v1/ops/claim",
                signedHeaders(ak(acc), sk(acc), st(acc), "POST", "/snap-manager/v1/ops/claim", "", claimBody.toString()),
                claimBody.toString(),
            )
            if (!cr.ok) return false to "claim HTTP ${cr.code}"
            val claimResp = JSONObject(cr.body)
            // ⚠️ id 非 null 时必须 confirm（漏了积分停在待确认不入账；confirm 失败不算整体失败）
            val confirmId = claimResp.optString("id", "null")
            if (confirmId.isNotEmpty() && confirmId != "null") {
                val cfBody = JSONObject().put("campaignId", campaignId)
                AccountPool.channelPost(
                    "$SNAP_ACCESS/snap-manager/v1/ops/confirm",
                    signedHeaders(ak(acc), sk(acc), st(acc), "POST", "/snap-manager/v1/ops/confirm", "", cfBody.toString()),
                    cfBody.toString(),
                )
            }
            true to "签到成功"
        } catch (e: Exception) {
            false to (e.message ?: "签到失败")
        }
    }

    // ==================== 模型 / 余额 ====================

    override fun fetchModels(accessToken: String): List<String> = listOf(
        "GLM-5.2", "GLM-5.1", "GLM-5", "glm-5.3-flash", "openpangu-2.0-flash", "openpangu-2.0-pro",
        "deepseek-v4-flash", "deepseek-v4-pro", "deepseek-v4.1-flash",
    ) // -VL- 模型在目录层过滤

    override fun queryCredits(acc: AccountPool.Account): Double {
        return try {
            val r = AccountPool.channelGet(
                "$SNAP_ACCESS/snap-manager/v1/statistics/plugin",
                signedHeaders(ak(acc), sk(acc), st(acc), "GET", "/snap-manager/v1/statistics/plugin", "", ""),
            )
            if (!r.ok) return Double.NaN
            val o = JSONObject(r.body) // ⚠️ 裸对象无信封
            val pkg = o.optJSONObject("package")
            if (pkg == null || !pkg.optBoolean("is_credit_package", false)) return Double.NaN
            val metrics = o.optJSONArray("metrics") ?: return Double.NaN
            for (i in 0 until metrics.length()) {
                val m = metrics.optJSONObject(i) ?: continue
                if (m.optString("name") == "usageTotalPackageCredit")
                    return m.optDouble("package_credit_remain", Double.NaN)
            }
            Double.NaN
        } catch (_: Exception) { Double.NaN }
    }
}
