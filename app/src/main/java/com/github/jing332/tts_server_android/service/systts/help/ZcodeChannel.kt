package com.github.jing332.tts_server_android.service.systts.help

import org.json.JSONObject

/**
 * zcode（智谱）渠道（10-09 全渠道批，协议=插件 zcode-oauth.ts/zcode-upstream.ts/zcode-adapter.ts）。
 *
 * 登录 = 服务端中介：POST /oauth/cli/init（Bearer=自生成 32hex 会话密钥）→ 浏览器授权
 * → GET /oauth/cli/poll/{flow_id}（pending/ready，token 由轮询直接返回——绕开故障的
 * POST /oauth/token，该端点 2026-09-28 起稳定 500，勿改回去）。
 *
 * 对话 = Anthropic Messages @ https://zcode.z.ai/api/v1/zcode-plan/anthropic/v1/messages
 * （无论账号是 bigmodel 还是 zai 都走 zcode.z.ai）。
 *
 * 无 refresh（JWT 无 exp，静态凭据）；401/1002 = JWT 失效提示重登。
 * device_mid 随机 UUID，服务端不校验但 billing 缺它 400 —— 必须稳定持久化，且不能拿它认账号。
 */
object ZcodeChannel : ChatChannel {
    override val id = "zcode"
    override val displayName = "ZCode 智谱"
    override val chatBaseUrl = "https://zcode.z.ai/api/v1/zcode-plan/anthropic/v1"
    override val rateLimitFallbackMs = utc8DayEnd() - System.currentTimeMillis() // 额度按 UTC+8 日切

    private const val APP_VERSION = "3.14.3"

    fun utc8DayEnd(now: Long = System.currentTimeMillis()): Long {
        val zone = java.time.ZoneId.of("GMT+8")
        val tomorrow = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate().plusDays(1)
        return tomorrow.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    /** 渠道头族（登录 init/poll 与对话共用基座） */
    private fun baseHeaders(): Map<String, String> = mapOf(
        "User-Agent" to "ZCode/$APP_VERSION",
        "HTTP-Referer" to "https://zcode.z.ai",
        "X-ZCode-App-Version" to APP_VERSION,
        "X-Release-Channel" to "stable",
        "X-Client-Language" to "zh-CN",
        "X-Client-Timezone" to "Asia/Shanghai",
        "X-Platform" to "win32",
        "X-Os-Category" to "windows",
    )

    // ==================== 登录（服务端中介） ====================

    data class LoginInit(val flowId: String, val authorizeUrl: String, val intervalSec: Int, val err: String)

    /** 第一步：init（provider=bigmodel/zai）。Bearer 是自生成会话密钥，不是用户凭据 */
    fun initLogin(provider: String = "bigmodel"): LoginInit {
        val sessionKey = DeviceCodeLogin.randomHex(32)
        val r = DeviceCodeLogin.postJson(
            "https://zcode.z.ai/api/v1/oauth/cli/init",
            JSONObject().put("provider", provider).toString(),
            baseHeaders() + mapOf(
                "Authorization" to "Bearer $sessionKey",
                "Content-Type" to "application/json; charset=utf-8",
            ),
        )
        if (!r.ok) return LoginInit("", "", 0, "HTTP ${r.code}：${r.body.take(120)}")
        val d = try { JSONObject(r.body).optJSONObject("data") } catch (_: Exception) { null }
            ?: return LoginInit("", "", 0, "响应缺 data：${r.body.take(120)}")
        val flowId = d.optString("flow_id")
        val url = d.optString("authorize_url")
        if (flowId.isEmpty() || url.isEmpty()) return LoginInit("", "", 0, "响应缺 flow_id/authorize_url")
        return LoginInit(flowId, url, d.optInt("poll_interval_sec", 2), "")
    }

    data class PollResult(val status: String, val jwt: String, val userId: String, val err: String)

    /** 第二步：轮询一次。status=PENDING 继续；READY 拿 zcode_jwt */
    fun pollLogin(flowId: String, deviceMid: String): PollResult {
        val r = DeviceCodeLogin.get(
            "https://zcode.z.ai/api/v1/oauth/cli/poll/$flowId",
            baseHeaders() + mapOf("X-Device-Mid" to deviceMid),
        )
        if (!r.ok) return PollResult("ERROR", "", "", "HTTP ${r.code}：${r.body.take(120)}")
        val o = try { JSONObject(r.body) } catch (_: Exception) { return PollResult("ERROR", "", "", "响应不是 JSON") }
        val status = o.optString("status", "pending")
        if (status == "pending") return PollResult("PENDING", "", "", "")
        if (status != "ready") return PollResult("ERROR", "", "", "status=$status")
        val token = o.optString("token")
        if (token.isEmpty()) return PollResult("ERROR", "", "", "ready 但缺 token")
        val userId = o.optJSONObject("user")?.optString("user_id").orEmpty()
        return PollResult("READY", token, userId, "")
    }

    // ==================== 对话（Anthropic Messages） ====================

    override fun chatHeaders(accessToken: String): Map<String, String> = baseHeaders() + mapOf(
        "anthropic-version" to "2023-06-01",
        "Content-Type" to "application/json; charset=utf-8",
    )

    /** Anthropic error 帧（{type:"error", error:{type,message}}）与 401/1002 归类 */
    override fun classifyError(httpStatus: Int, body: String): ChatChannel.ErrClass = when {
        httpStatus == 401 || body.contains("\"1002\"") || body.contains("1002") && body.contains("token", true) -> ChatChannel.ErrClass.AUTH
        httpStatus == 429 || body.contains("rate", true) -> ChatChannel.ErrClass.RATE_LIMIT
        else -> ChatChannel.ErrClass.OTHER
    }

    override fun refresh(acc: AccountPool.Account): Triple<String, String, Long>? = null // 无续期

    // ==================== 模型 / 余额 ====================

    override fun fetchModels(accessToken: String): List<String> = listOf("GLM-5.3-Flash", "GLM-5.3")

    /** 余额（balance 端点：Bearer + X-Device-Mid 双必须；缺 device_mid=400 code 3001） */
    override fun queryCredits(acc: AccountPool.Account): Double = try {
        val mid = acc.extraStr("device_mid")
        if (mid.isEmpty() || acc.accessToken.isEmpty()) Double.NaN
        else {
            val r = DeviceCodeLogin.get(
                "https://zcode.z.ai/api/v1/zcode-plan/billing/balance",
                baseHeaders() + mapOf(
                    "Authorization" to "Bearer ${acc.accessToken}",
                    "X-Device-Mid" to mid,
                ),
            )
            if (!r.ok) Double.NaN
            else {
                val d = JSONObject(r.body).optJSONObject("data") ?: return Double.NaN
                // 各桶 credits 求和；expiresAt 单位是秒（照插件）
                val plans = d.optJSONArray("plans") ?: return Double.NaN
                var total = 0.0
                for (i in 0 until plans.length()) {
                    val p = plans.optJSONObject(i) ?: continue
                    total += p.optDouble("credits", 0.0)
                }
                total
            }
        }
    } catch (_: Exception) { Double.NaN }
}
