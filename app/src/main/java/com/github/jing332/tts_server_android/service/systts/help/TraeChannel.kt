package com.github.jing332.tts_server_android.service.systts.help

import org.json.JSONArray
import org.json.JSONObject

/**
 * trae（字节）渠道（10-09 全渠道批，协议=规格书 §5）。
 *
 * 登录 = 本地回调 token 直传（回调无 code，直接 ?refreshToken=...&userInfo=...）；
 * app 侧第二期做 WebView+回调口，本期落「凭据续期/对话/签到」主体。
 *
 * 对话 = 自定义 SOLO SSE @ https://trae-api-cn.mchost.guru/api/agent/v3/llm_utils_chat。
 * ⚠️ 鉴权前缀 `Cloud-IDE-JWT`（不是 Bearer）；三个头同值（Authorization/X-Cloudide-Token/X-Ide-Token）。
 * ⚠️ 思考字段是 delta.reasoning（非 reasoning_content）。
 * 续期 = ExchangeToken（refresh_token 每次轮换必须回写）。
 */
object TraeChannel : ChatChannel {
    override val id = "trae"
    override val displayName = "TRAE 字节"
    override val chatBaseUrl = "https://trae-api-cn.mchost.guru/api/agent/v3"

    private const val OAUTH_HOST = "https://api.trae.com.cn"
    private const val UG_HOST = "https://api.trae.cn"
    private const val CLIENT_ID = "en1oxy7wnw8j9n"
    private const val APP_ID = "6eefa01c-1036-4c7e-9ca5-d891f63bfcd8"
    private const val IDE_VERSION = "0.1.52"
    private const val IDE_VERSION_CODE = "20260811"

    // ==================== 续期（ExchangeToken） ====================

    override fun refresh(acc: AccountPool.Account): Triple<String, String, Long>? {
        val body = JSONObject().apply {
            put("ClientID", CLIENT_ID)
            put("RefreshToken", acc.refreshToken)
            put("ClientSecret", "-")
            put("UserID", "")
        }
        val r = AccountPool.channelPost(
            "$OAUTH_HOST/cloudide/api/v3/trae/oauth/ExchangeToken",
            mapOf("User-Agent" to "Trae/$IDE_VERSION"),
            body.toString(),
        )
        if (!r.ok) return null
        val o = try { JSONObject(r.body) } catch (_: Exception) { return null }
        val result = o.optJSONObject("Result") ?: return null
        val token = result.optString("Token")
        if (token.isEmpty()) return null
        // refresh_token 每次轮换，必须回写
        val newRefresh = result.optString("RefreshToken").ifEmpty { acc.refreshToken }
        val expiresAt = parseTraeExpiry(result)
        return Triple(token, newRefresh, expiresAt)
    }

    /** 过期三回退：TokenExpireAt（秒/毫秒兼容）> TokenExpireDuration（相对秒）> +24h */
    private fun parseTraeExpiry(result: JSONObject): Long {
        val at = result.optString("TokenExpireAt").trim()
        at.toLongOrNull()?.let { n -> return if (n < 10_000_000_000L) n * 1000L else n }
        val dur = result.optLong("TokenExpireDuration", 0L)
        if (dur > 0) return System.currentTimeMillis() + dur * 1000L
        return System.currentTimeMillis() + 24 * 3600_000L
    }

    // ==================== 对话（SOLO 形状头族） ====================

    override fun chatHeaders(accessToken: String): Map<String, String> {
        // machine_id/device_id 从 extra 取（登录期持久化；本期凭据直填路径可为空串）
        return mapOf(
            "X-Cloudide-Token" to accessToken,
            "X-Ide-Token" to accessToken,
            "X-App-Id" to APP_ID,
            "X-App-Version" to "default",
            "X-Ide-Version" to IDE_VERSION,
            "X-Ide-Version-Code" to IDE_VERSION_CODE,
            "X-App-Version-Code" to IDE_VERSION_CODE,
            "X-Ide-Version-Type" to "stable",
            "X-Device-Type" to "macos",
            "X-OS-Version" to "macOS 15.7.4",
            "X-Device-Brand" to "Apple",
            "Request-Traffic-Type" to "prod",
            "User-Agent" to "Trae/$IDE_VERSION",
            "Accept" to "text/event-stream",
        )
    }

    /**
     * OpenAI → SOLO 请求体转换（7 条规则，照插件）：
     * stream:true / function:{channel} / model→config_name+model 双字段（剥 __dev/__max）
     * / content 字符串→[{type:text}] / tool_calls.function→function_call / tools[].parameters→JSON串。
     */
    override fun patchBody(bodyJson: String, model: String): String = try {
        val o = JSONObject(bodyJson)
        val cleanModel = model.removeSuffix("__dev").removeSuffix("__max")
        val out = JSONObject()
        out.put("stream", true)
        out.put("function", JSONObject().put("channel", channelFor(cleanModel)))
        out.put("config_name", cleanModel)
        out.put("model", cleanModel)
        val msgs = JSONArray()
        val inMsgs = o.optJSONArray("messages") ?: JSONArray()
        for (i in 0 until inMsgs.length()) {
            val m = inMsgs.optJSONObject(i) ?: continue
            val nm = JSONObject()
            nm.put("role", m.optString("role"))
            val content = m.opt("content")
            if (content is String) {
                nm.put("content", JSONArray().put(JSONObject().put("type", "text").put("text", content)))
            } else {
                nm.put("content", content)
            }
            // assistant 的 tool_calls[].function → function_call（SOLO 字段名）
            val tc = m.optJSONArray("tool_calls")
            if (tc != null && tc.length() > 0) {
                val fn = tc.optJSONObject(0)?.optJSONObject("function")
                if (fn != null) {
                    nm.put("function_call", JSONObject()
                        .put("name", fn.optString("name"))
                        .put("arguments", fn.optString("arguments")))
                }
            }
            msgs.put(nm)
        }
        out.put("messages", msgs)
        val tools = o.optJSONArray("tools")
        if (tools != null) {
            val soloTools = JSONArray()
            for (i in 0 until tools.length()) {
                val t = tools.optJSONObject(i) ?: continue
                val fn = t.optJSONObject("function") ?: t
                val st = JSONObject()
                st.put("name", fn.optString("name"))
                st.put("description", fn.optString("description"))
                // ⚠️ parameters 对象 → JSON 字符串（SOLO 方言）
                val params = fn.opt("parameters")
                st.put("parameters", if (params is JSONObject) params.toString() else params ?: "")
                soloTools.put(st)
            }
            out.put("tools", soloTools)
        }
        o.optInt("max_tokens", -1).takeIf { it > 0 }?.let { out.put("max_tokens", it) }
        o.optDouble("temperature", -1.0).takeIf { it >= 0 }?.let { out.put("temperature", it) }
        out.toString()
    } catch (_: Exception) {
        bodyJson
    }

    /** 模型通道（规格书白名单取更靠前者；缺省 solo_work_lite） */
    private fun channelFor(model: String): String = "solo_work_lite"

    /** SOLO SSE：delta.reasoning（非 reasoning_content）—— parseResponse 层处理流内 error 帧 */
    override fun classifyError(httpStatus: Int, body: String): ChatChannel.ErrClass = when {
        // ⚠️ 4008（quota 日额）必须排在 4011（soft 60s）前——同一响应体可能双码
        body.contains("4008") -> ChatChannel.ErrClass.RATE_LIMIT
        body.contains("4011") || httpStatus == 429 -> ChatChannel.ErrClass.RATE_LIMIT
        body.contains("1005") -> ChatChannel.ErrClass.RATE_LIMIT // hard-plan 12h
        httpStatus == 401 -> ChatChannel.ErrClass.AUTH
        else -> ChatChannel.ErrClass.OTHER
    }

    // ==================== 签到 / 余额 / 模型 ====================

    override fun checkIn(acc: AccountPool.Account): Pair<Boolean, String> {
        try {
            val token = acc.accessToken
            // status：{checked_in, credits, enable}
            val st = AccountPool.channelPost("$UG_HOST/trae/api/v2/ug/checkin_credits/status", checkinHeaders(acc), "{}")
            if (!st.ok) return false to "HTTP ${st.code}"
            val so = JSONObject(st.body)
            val sd = so.optJSONObject("data") ?: so
            if (sd.optBoolean("checked_in", false)) return true to "今日已签到（credits=${sd.optLong("credits", 0)}）"
            // claim 响应不含积分数——成功后补查 status
            val claim = AccountPool.channelPost("$UG_HOST/trae/api/v2/ug/checkin_credits/claim", checkinHeaders(acc), "{}")
            if (!claim.ok) return false to "HTTP ${claim.code}：${claim.body.take(120)}"
            val co = JSONObject(claim.body)
            return if (co.optInt("code", -1) == 0) true to "签到成功"
            else false to "业务码 ${co.optInt("code")}：${co.optString("message")}"
        } catch (e: Exception) {
            return false to (e.message ?: "签到失败")
        }
    }

    /** 签到客户端头族（≈20 头；设备身份按 user_id 确定性派生，每账号稳定） */
    private fun checkinHeaders(acc: AccountPool.Account): Map<String, String> {
        val seed = acc.accessToken.ifEmpty { acc.id }
        val devid = derivedDigits("devid:$seed", 15)
        val market = derivedDigits("market:$seed", 15)
        return mapOf(
            "Authorization" to "Cloud-IDE-JWT ${acc.accessToken}",
            "X-Market-Client-Id" to "VSCode 1.107.1",
            "X-Market-User-Id" to market,
            "X-User-Region" to "CN",
            "X-Device-Id" to devid,
            "X-Lgw-Req-Sdk-Type" to "3",
            "Package-Type" to "stable_cn",
            "X-Lscbd-Aid" to "787976",
            "X-Lscbd-Platform" to "windows",
            "App-Version" to IDE_VERSION,
            "X-Tt-Trace-Id" to "00-${DeviceCodeLogin.randomHex(32)}-01",
            "Vscode-Sessionid" to DeviceCodeLogin.randomHex(32) + DeviceCodeLogin.randomHex(32),
            "X-Request-Id" to DeviceCodeLogin.randomUuid(),
            "User-Agent" to "VSCode 1.107.1 (TRAE SOLO CN)",
        )
    }

    /** SHA-256 确定性数字串派生（照插件伪随机流思路：hash→取数字段） */
    private fun derivedDigits(saltSeed: String, len: Int): String {
        val h = java.security.MessageDigest.getInstance("SHA-256").digest(saltSeed.toByteArray(Charsets.UTF_8))
        val digits = StringBuilder()
        for (b in h) { digits.append((b.toInt() and 0xFF) % 10); if (digits.length >= len) break }
        return digits.toString().take(len)
    }

    override fun queryCredits(acc: AccountPool.Account): Double {
        return try {
            val r = AccountPool.channelPost(
                "$UG_HOST/trae/api/v2/pay/ide_user_ent_usage",
                checkinHeaders(acc) + mapOf("Content-Type" to "application/json"),
                JSONObject().put("require_usage", true).put("req_source", 2).toString(),
            )
            if (!r.ok) Double.NaN
            else {
                val d = JSONObject(r.body).optJSONObject("data") ?: return Double.NaN
                val packs = d.optJSONArray("user_entitlement_pack_list") ?: return Double.NaN
                var remain = 0.0
                for (i in 0 until packs.length()) {
                    val p = packs.optJSONObject(i) ?: continue
                    remain += p.optDouble("credits_limit", 0.0) - p.optDouble("credits_amount", 0.0)
                }
                remain
            }
        } catch (_: Exception) { Double.NaN }
    }

    override fun fetchModels(accessToken: String): List<String> = listOf(
        "DeepSeek-V4-Flash-Official", "Doubao-Seed-2.1-Pro", "seed-code-pro-0430",
        "Doubao-Seed-2.1-Turbo", "Doubao-Seed-2.0-Code", "glm-5.2", "DeepSeek-V4-Pro",
        "DeepSeek-V4-Flash", "kimi-k3", "kimi-k2.7-code", "kimi-k2.6", "minimax-m3", "qwen-3.7-plus",
    )
}
