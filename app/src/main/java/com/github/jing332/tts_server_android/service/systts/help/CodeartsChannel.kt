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
 * 本期落凭据直填（用户从插件导出的 AK/SK/ST）+ 续期（10-10 接线 DPoP：ES256 私钥 JWK
 * 持久化到 extra.dpop_private_key_jwk，token 请求带 DPoP 头）+ 对话 + 签到主体。
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

    // STS token 端点（照插件 oauth.ts STS_TOKEN_ENDPOINT）。DPoP htu 用完整 URL。
    private const val STS_TOKEN_URL = "https://$STS_HOST/v1/oauth2/tokens"

    /** extra 里存 DPoP ES256 私钥 JWK 的键（含 x/y 公钥分量，公钥从同一 JWK 重建） */
    private const val EXTRA_DPOP_JWK = "dpop_private_key_jwk"

    /**
     * 取（或首次生成）DPoP ES256 密钥对：私钥 JWK 持久化到 Account.extra 的
     * dpop_private_key_jwk（路径新建），已有该字段直接复用不重新生成。
     * 返回 (私钥 JWK 对象, 公钥 JWK 对象)；生成成功但调用方未落盘时下次会重生成——
     * 所以调用方 refresh() 必须把私钥 JWK 写回 extra。
     */
    private fun ensureDpopJwk(acc: AccountPool.Account): Pair<JSONObject, JSONObject>? {
        val stored = acc.extraStr(EXTRA_DPOP_JWK)
        if (stored.isNotEmpty()) {
            val o = try { JSONObject(stored) } catch (_: Exception) { null }
            // 已存且四分量齐（kty/crv/x/y/d）→ 复用
            if (o != null && o.optString("d").isNotEmpty() && o.optString("x").isNotEmpty() &&
                o.optString("y").isNotEmpty() && o.optString("kty") == "EC") return o to o
        }
        // 生成 EC P-256 密钥对（java.security），JWK 手拼：x/y/d = 各大数定长 32 字节 base64url 无填充
        return try {
            val kpg = java.security.KeyPairGenerator.getInstance("EC")
            val ecSpec = java.security.spec.ECGenParameterSpec("secp256r1")
            kpg.initialize(ecSpec)
            val kp = kpg.generateKeyPair()
            val pub = kp.public as java.security.interfaces.ECPublicKey
            val priv = kp.private as java.security.interfaces.ECPrivateKey
            val jwk = JSONObject()
                .put("kty", "EC")
                .put("crv", "P-256")
                .put("x", b64u32(pub.w.x))
                .put("y", b64u32(pub.w.y))
                .put("d", b64u32(priv.s))
            jwk.toString() to jwk
        } catch (_: Exception) {
            null
        }
    }

    /** 大数 → 定长 32 字节（P-256 坐标/标量宽度）base64url 无填充（前零填充，不截断更长值） */
    private fun b64u32(v: java.math.BigInteger): String {
        val raw = v.toByteArray() // 补符号位形式：正数首字节可能多一个 0x00
        val fixed = ByteArray(32)
        // 取末 32 字节（不足则前零填充在 fixed 里天然完成）
        val src = if (raw.size > 32) raw.copyOfRange(raw.size - 32, raw.size) else raw
        // 右对齐拷贝：raw 短 → 前面留零；raw 恰 33（带 0x00）→ src 已截到 32
        System.arraycopy(src, 0, fixed, 32 - src.size, src.size)
        return android.util.Base64.encodeToString(
            fixed, android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING or android.util.Base64.URL_SAFE
        )
    }

    /**
     * 签发 DPoP JWS（dpop+jwt）：header {alg:ES256, typ:dpop+jwt, jwk:公钥}，
     * payload {htm, htu, iat:秒级, jti:UUID}（照插件 oauth.ts signDpopJws 形状）。
     * ⚠️ SHA256withECDSA 输出 DER 编码，JWS 要求 raw R||S 各 32 字节共 64 字节——
     * 必须解 DER 转 raw（最易错点，node 侧已对拍验证）。
     */
    private fun signDpopJws(privJwk: JSONObject, pubJwk: JSONObject, htm: String, htu: String): String {
        fun b64u(bytes: ByteArray) = android.util.Base64.encodeToString(
            bytes, android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING or android.util.Base64.URL_SAFE
        )
        val header = JSONObject()
            .put("alg", "ES256")
            .put("typ", "dpop+jwt")
            .put("jwk", JSONObject()
                .put("kty", pubJwk.optString("kty"))
                .put("crv", pubJwk.optString("crv"))
                .put("x", pubJwk.optString("x"))
                .put("y", pubJwk.optString("y"))) // 只给公钥分量，不带 d
        val payload = JSONObject()
            .put("htm", htm)
            .put("htu", htu)
            .put("iat", System.currentTimeMillis() / 1000)
            .put("jti", DeviceCodeLogin.randomUuid())
        val signingInput = b64u(header.toString().toByteArray(Charsets.UTF_8)) + "." +
            b64u(payload.toString().toByteArray(Charsets.UTF_8))
        // P-256 曲线参数从命名组推导（不手写曲线常量）
        val ecParams = java.security.AlgorithmParameters.getInstance("EC")
            .run { init(java.security.spec.ECGenParameterSpec("secp256r1")); getParameterSpec(java.security.spec.ECParameterSpec::class.java) }
        val privKey = java.security.KeyFactory.getInstance("EC").generatePrivate(
            java.security.spec.ECPrivateKeySpec(
                java.math.BigInteger(1, b64uDecode(privJwk.optString("d"))), ecParams)
        )
        val sig = java.security.Signature.getInstance("SHA256withECDSA")
        sig.initSign(privKey)
        sig.update(signingInput.toByteArray(Charsets.US_ASCII))
        return signingInput + "." + b64u(derToRaw(sig.sign()))
    }

    private fun b64uDecode(s: String): ByteArray = android.util.Base64.decode(
        s, android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING or android.util.Base64.URL_SAFE
    )

    /** ECDSA 签名 DER(0x30 len 0x02 rlen r 0x02 slen s) → JWS raw R||S 各 32 字节 */
    private fun derToRaw(der: ByteArray): ByteArray {
        var i = 2 // 跳过 0x30 + 总长
        val rLen = der[i + 1].toInt() and 0xff; val r = der.copyOfRange(i + 2, i + 2 + rLen); i += 2 + rLen
        val sLen = der[i + 1].toInt() and 0xff; val s = der.copyOfRange(i + 2, i + 2 + sLen)
        val out = ByteArray(64)
        // DER 整数高零字节（含 0x00 符号位）剥掉后右对齐拷 32 字节
        val rTrim = r.dropWhile { it == 0.toByte() }.toByteArray()
        val sTrim = s.dropWhile { it == 0.toByte() }.toByteArray()
        System.arraycopy(rTrim, 0, out, 32 - rTrim.size, rTrim.size)
        System.arraycopy(sTrim, 0, out, 64 - sTrim.size, sTrim.size)
        return out
    }

    override fun refresh(acc: AccountPool.Account): Triple<String, String, Long>? {
        // STS 刷新要 DPoP(ES256) 头 + refresh_token form 体；DPoP 私钥 JWK 在 extra 持久化
        val refreshToken = acc.refreshToken
        val verifier = acc.extraStr("code_verifier")
        if (refreshToken.isEmpty() || verifier.isEmpty()) return null
        // 凭据直填（无 refresh_token）账号不走续期：上面 refreshToken 空已挡，这里不报错直接 null
        val (privJwkObj, pubJwk) = ensureDpopJwk(acc) ?: return null
        val dpop = try {
            signDpopJws(privJwkObj, pubJwk, "POST", STS_TOKEN_URL)
        } catch (_: Exception) { return null }
        val form = "client_id=codearts-agent&code_verifier=${java.net.URLEncoder.encode(verifier, "UTF-8")}" +
            "&grant_type=refresh_token&refresh_token=${java.net.URLEncoder.encode(refreshToken, "UTF-8")}"
        val r = AccountPool.channelPost(STS_TOKEN_URL, mapOf(
            "DPoP" to dpop,
            "Content-Type" to "application/x-www-form-urlencoded",
        ), form)
        if (!r.ok) return null
        val o = try { JSONObject(r.body) } catch (_: Exception) { return null }
        // 终态判定（照插件 oauth.ts requestToken）：**只认 refresh_token 自己失效的两种信号**
        // —— invalid_grant / error_code 含 ExpiredRefreshToken。
        // ⚠️ InvalidDPoPHeader（及 401 invalid_dpop 类）**不是终态**：时钟偏差让 iat 落窗口外、
        // proof 被判重放、网关抖动都是**一次请求层面**的拒绝，与 refresh_token 寿命无关；
        // 当终态会把材料完好的账号一步标死（插件真实事故，10-09 对账表三 #4 必抄项）。
        // 本渠道 classifyError/调度器对这类失败只记日志，下轮照试——保持普通失败语义即可。
        val errCode = o.optString("error_code") + o.optString("error")
        if (errCode.contains("invalid_grant") || errCode.contains("ExpiredRefreshToken")) return null
        val c = o.optJSONObject("credentials") ?: return null
        val newAk = c.optString("access_key_id")
        if (newAk.isEmpty()) return null
        // 落 extra（调用方 AccountRefreshScheduler 只更新 token 三元组——AK/SK 变化需走 saveExtras）；
        // DPoP 私钥 JWK 首次生成时一并写回（ensureDpopJwk 语义：不落盘下次会重生成）
        pendingExtraUpdate = JSONObject()
            .put("access_key_id", newAk)
            .put("secret_access_key", c.optString("secret_access_key"))
            .put("security_token", c.optString("security_token"))
            .put(EXTRA_DPOP_JWK, privJwkObj.toString())
        val expiresAt = parseIsoOrPlus(c.optString("expiration"), 24 * 3600_000L)
        // 响应给新 refresh_token 直接覆盖（插件 credentialFromTokenResponse 同款；不轮换语义未明）
        val newRefresh = o.optString("refresh_token").ifEmpty { refreshToken }
        return Triple(newAk, newRefresh, expiresAt)
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
            return false to (e.message ?: "签到失败")
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
            return Double.NaN
        } catch (_: Exception) { return Double.NaN }
    }
}
