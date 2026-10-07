package com.github.jing332.tts_server_android.service.systts.help

import org.json.JSONObject

/**
 * loomy（讯飞办公助手）渠道（10-09 全渠道批，协议=规格书 §7）。
 *
 * 登录 = 短信验证码三步（HMAC-SHA1 九段拼串签名，body 序列化一次共用）——
 * app 侧第二期做短信表单 UI；本渠道先落「凭据直填（session 32hex）」+ 对话 + 签到主体。
 *
 * 对话 = 标准 OpenAI @ loomyad.xunfei.cn/api/v1/chat/completions。
 * ⚠️ 双认证头都发（Authorization: Bearer + token:）；chat 只认 Bearer、业务端点只认 token。
 * ⚠️ 失败恒 HTTP 200，成败只能读 body code（成功码 000000）。
 * ⚠️ 100002=登录失效不得重试；无 refresh（session 14 天，过期重登）。
 */
object LoomyChannel : ChatChannel {
    override val id = "loomy"
    override val displayName = "Loomy 讯飞"
    override val chatBaseUrl = "https://loomyad.xunfei.cn/api/v1"

    private const val ACCOUNT_BASE = "https://account.xfinfr.com"
    private const val API_BASE = "https://loomyad.xunfei.cn/api/v1"
    private const val ACCESS_KEY_ID = "2thryby66wxi53sk"
    private const val ACCESS_KEY_SECRET = "zsak6eadrbawz683wf5r3m2snrwj868r"
    private const val APP_ID = "GM3LOOMY"

    // ==================== HMAC-SHA1 签名（逐字节复刻 sign.js） ====================

    /**
     * 待签串 9 段 \n 连接（后两段恒空串→结尾 \n\n 不去掉）；
     * ESCAPED_PATH 分段 RFC3986 转义；query 按传入顺序不排序。
     */
    fun sign(method: String, path: String, queryParams: List<Pair<String, String>>, body: String, dateUtc: String, nonce: String): String {
        val escapedPath = "/" + path.trim('/').split("/").joinToString("/") { rfc3986(it) }
        val escapedQuery = queryParams.joinToString("&") { (k, v) -> "$k=${rfc3986(v)}" }
        val contentMd5 = if (body.isEmpty()) "" else android.util.Base64.encodeToString(
            java.security.MessageDigest.getInstance("MD5").digest(body.toByteArray(Charsets.UTF_8)),
            android.util.Base64.NO_WRAP)
        val contentType = if (body.isEmpty()) "" else "application/json"
        val toSign = listOf(method, escapedPath, escapedQuery, contentMd5, contentType, dateUtc, nonce, "", "")
            .joinToString("\n")
        val mac = javax.crypto.Mac.getInstance("HmacSHA1")
        mac.init(javax.crypto.spec.SecretKeySpec(ACCESS_KEY_SECRET.toByteArray(Charsets.UTF_8), "HmacSHA1"))
        val sig = android.util.Base64.encodeToString(mac.doFinal(toSign.toByteArray(Charsets.UTF_8)), android.util.Base64.NO_WRAP)
        return "account $ACCESS_KEY_ID:$sig"
    }

    /** RFC3986 严格转义（JSONObject 没有现成的；照 sign.js encodeURIComponent + !'()* 补转） */
    private fun rfc3986(s: String): String {
        val enc = java.net.URLEncoder.encode(s, "UTF-8")
        return enc.replace("+", "%20").replace("%21", "!").replace("%27", "'")
            .replace("%28", "(").replace("%29", ")").replace("%2A", "*").replace("~", "%7E")
    }

    /** 带签名的账号端点请求头（body 序列化一次、签名与发送共用同一字符串——调用方约定） */
    fun authedHeaders(method: String, path: String, query: List<Pair<String, String>>, body: String): Map<String, String> {
        val date = java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
            .withZone(java.time.ZoneId.of("GMT")).format(java.time.Instant.now())
        val nonce = DeviceCodeLogin.randomUuid()
        return mapOf(
            "Authorization" to sign(method, path, query, body, date, nonce),
            "Date" to date,
            "Nonce" to nonce,
            "Content-Type" to "application/json",
        )
    }

    // ==================== 短信登录（第二期接 UI；引擎先落） ====================

    data class SendResult(val msgId: String, val err: String)

    /** 发验证码 → data.msgid */
    fun sendSmsCode(phone: String): SendResult {
        val base = JSONObject().apply {
            put("appid", APP_ID); put("modelid", "Web"); put("version", "1.0.0")
            put("devid", "web"); put("ua", "Loomy|Desktop|Electron|macOS")
            put("traceid", DeviceCodeLogin.randomUuid().replace("-", ""))
        }
        val param = JSONObject().put("ccode", "86").put("phone", phone).put("expire", 300)
        val body = JSONObject().put("base", base).put("param", param).toString()
        val r = AccountPool.channelPost("$ACCOUNT_BASE/login/phone/sendMsgCode", authedHeaders("POST", "/login/phone/sendMsgCode", emptyList(), body), body)
        if (!r.ok) return SendResult("", "HTTP ${r.code}")
        val d = try { JSONObject(r.body).optJSONObject("data") } catch (_: Exception) { null } ?: return SendResult("", "响应缺 data")
        return SendResult(d.optString("msgid"), if (d.optString("msgid").isEmpty()) "响应缺 msgid" else "")
    }

    /** 提交验证码 → (session, userid, err)；expires_at = 本地登录时刻+14 天 */
    fun checkCode(phone: String, code: String, msgId: String): Triple<String, String, String> {
        val base = JSONObject().apply {
            put("appid", APP_ID); put("modelid", "Web"); put("version", "1.0.0")
            put("devid", "web"); put("ua", "Loomy|Desktop|Electron|macOS")
            put("traceid", DeviceCodeLogin.randomUuid().replace("-", ""))
        }
        val param = JSONObject().put("ccode", "86").put("phone", phone)
            .put("mcode", code).put("msgid", msgId).put("expire", 1209600)
        val body = JSONObject().put("base", base).put("param", param).toString()
        val r = AccountPool.channelPost("$ACCOUNT_BASE/login/phone/checkCode", authedHeaders("POST", "/login/phone/checkCode", emptyList(), body), body)
        if (!r.ok) return Triple("", "", "HTTP ${r.code}")
        val o = try { JSONObject(r.body) } catch (_: Exception) { return Triple("", "", "响应不是 JSON") }
        val d = o.optJSONObject("data") ?: return Triple("", "", "响应缺 data：${o.toString().take(120)}")
        val session = d.optString("session")
        if (session.isEmpty()) return Triple("", "", "响应缺 session")
        return Triple(session, d.optString("userid"), "")
    }

    // ==================== 续期：无（诚实标记，照插件 isLoomyRefreshable 恒 false） ====================

    override fun refresh(acc: AccountPool.Account): Triple<String, String, Long>? = null

    // ==================== 对话 ====================

    override fun chatHeaders(accessToken: String): Map<String, String> = mapOf(
        "Authorization" to "Bearer $accessToken", // chat 只认 Bearer
        "token" to accessToken,                   // 业务端点只认 token（双发）
        "Accept" to "text/event-stream",
    )

    /** 失败恒 HTTP 200 + code 字段——状态码分类不够，读体 */
    override fun classifyError(httpStatus: Int, body: String): ChatChannel.ErrClass = when {
        body.contains("\"100002\"") || body.contains("020002") -> ChatChannel.ErrClass.AUTH // 登录失效不得重试
        body.contains("\"100001\"") -> ChatChannel.ErrClass.OTHER // 参数错误
        Regex("insufficient.{0,40}(point|credit|balance|quota|token)").containsMatchIn(body.lowercase()) -> ChatChannel.ErrClass.RATE_LIMIT
        httpStatus == 401 || httpStatus == 403 -> ChatChannel.ErrClass.AUTH
        else -> ChatChannel.ErrClass.OTHER
    }

    // ==================== 签到/余额/模型 ====================

    /** 「签到」= points/first-login（写、幂等）；查询余额用 points/records（读写严格分离） */
    override fun checkIn(acc: AccountPool.Account): Pair<Boolean, String> {
        return try {
            val r = AccountPool.channelPost(
                "$API_BASE/points/first-login",
                mapOf("token" to acc.accessToken, "Content-Type" to "application/json"), // 业务端点只认 token 头
                "{}",
            )
            if (!r.ok) return false to "HTTP ${r.code}"
            val o = JSONObject(r.body)
            val code = o.optString("code")
            if (code == "000000") {
                val d = o.optJSONObject("data") ?: JSONObject()
                if (d.optBoolean("alreadyProcessed", false)) true to "今日已领取"
                else true to "领取成功（daily=${d.optLong("dailyBalance", 0)}）"
            } else return false to "业务码 $code：${o.optString("desc")}"
        } catch (e: Exception) {
            return false to (e.message ?: "签到失败")
        }
    }

    override fun queryCredits(acc: AccountPool.Account): Double {
        return try {
            val r = AccountPool.channelGet(
                "$API_BASE/points/records?pageNo=1&pageSize=1&recordType=all",
                mapOf("token" to acc.accessToken),
            )
            if (!r.ok) Double.NaN
            else {
                val d = JSONObject(r.body).optJSONObject("data") ?: return Double.NaN
                d.optDouble("availableBalance", Double.NaN) // 永久+每日合计
            }
        } catch (_: Exception) { Double.NaN }
    }

    override fun fetchModels(accessToken: String): List<String> {
        return try {
            val r = AccountPool.channelGet("$API_BASE/models", mapOf("token" to accessToken))
            if (!r.ok) fallback()
            else {
                val arr = JSONObject(r.body).optJSONArray("data") ?: return fallback()
                (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
                    .filter { it.optString("type") == "chat" } // ⚠️ 不用 output_modalities 判断
                    .mapNotNull { it.optString("id").ifEmpty { null } }
                    .ifEmpty { fallback() }
            }
        } catch (_: Exception) { return fallback() }
    }

    private fun fallback(): List<String> = listOf(
        "deepseek-v4-flash-0731", "MiniMax-M3", "Kimi-k2.6", "qwen-3.8-max",
        "GLM-5.3-Flash", "qwen3.8-flash", "spark-x", "mimo-v2.5",
    )
}
