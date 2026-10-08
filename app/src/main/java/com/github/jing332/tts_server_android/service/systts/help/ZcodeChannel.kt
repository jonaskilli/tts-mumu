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

    // ==================== 签到（补活跃信号 + 查可领；claim 需 captcha 不自动做） ====================

    // 客户端活跃上报端点（免 Authorization）；可领活动预览端点（免 Authorization，需 X-Device-Mid）
    private const val EVENT_REPORT_URL = "https://zcode.z.ai/api/v1/event/report"
    private const val BILLING_PREVIEW_URL = "https://zcode.z.ai/api/v1/zcode-plan/billing/preview"

    /**
     * zcode 签到（10-10 对账 TOP5 第 5 条，协议 = 插件 zcode-upstream.ts 照抄）：
     *
     * ① **补活跃上报不能省**：服务端不主动推送活动，preview 内容依赖客户端活跃信号——
     *    不补 `POST /api/v1/event/report {app_launch, app_daily_active}` 两条事件，
     *    preview 恒为 `plans:[]`，「今天可领」永远是「没有可领」（插件注释原文结论）。
     *    幂等（服务端按 device_mid+日期去重）。鉴权要求实测：event/report 与 preview
     *    都**不需要** Authorization，但**必须**带 X-Device-Mid（缺则 400 code 3001）。
     * ② 查 preview（GET ?app_version=&platform=win32）确认是否有可领活动。
     * ③ **claim 不自动做**：`POST /zcode-plan/billing/claim` 需要 `Authorization` +
     *    阿里云 captcha 双头（x-aliyun-captcha-verify-param/-region，每个 plan 现产
     *    新 param），app 无法自动过验证——照插件排除表（isAutoCheckinExcluded）语义，
     *    明确返回 false 不白耗配额。用户可手动在官方客户端领取。
     *
     * ⚠️ 本实现不落盘记账：成功与否由 AccountCheckinReceiver 统一写 lastCheckinDate。
     */
    override fun checkIn(acc: AccountPool.Account): Pair<Boolean, String> {
        val mid = acc.extraStr("device_mid")
        if (mid.isEmpty() || acc.accessToken.isEmpty())
            return false to "ZCode 缺 device_mid/JWT（重新登录账号池账号后重试）"
        val headers = baseHeaders() + mapOf("X-Device-Mid" to mid)
        // ① 补活跃信号：两个事件各发一条，body 形状照 zcode-upstream.ts reportZcodeActivation 原文
        for (event in listOf("app_launch", "app_daily_active")) {
            val body = JSONObject()
                .put("event", event)
                .put("device_mid", mid)
                .put("platform", "win32")
                .put("app_version", APP_VERSION)
                .toString()
            AccountPool.channelPost(EVENT_REPORT_URL, headers, body) // 上报失败不阻塞（幂等，下次再补）
        }
        // ② 查可领活动（只确认状态，不领取）
        return try {
            val r = DeviceCodeLogin.get(
                "$BILLING_PREVIEW_URL?app_version=$APP_VERSION&platform=win32",
                headers,
            )
            if (!r.ok) false to "ZCode 活动查询失败：HTTP ${r.code}"
            else {
                val plans = JSONObject(r.body).optJSONObject("data")?.optJSONArray("plans")
                if (plans != null && plans.length() > 0)
                    false to "ZCode 签到需要人机验证，暂不支持自动签到（有 ${plans.length()} 个可领活动）"
                else
                    false to "ZCode 签到需要人机验证，暂不支持自动签到"
            }
        } catch (e: Exception) {
            false to "ZCode 活动查询异常：${e.message}"
        }
    }

    // ==================== 模型 / 余额 ====================

    override fun fetchModels(accessToken: String): List<String> = listOf("GLM-5.3-Flash", "GLM-5.3")

    /** 余额（balance 端点：Bearer + X-Device-Mid 双必须；缺 device_mid=400 code 3001） */
    override fun queryCredits(acc: AccountPool.Account): Double {
        return try {
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
}
