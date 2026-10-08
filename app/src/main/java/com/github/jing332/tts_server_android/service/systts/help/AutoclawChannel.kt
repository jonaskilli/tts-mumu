package com.github.jing332.tts_server_android.service.systts.help

import org.json.JSONArray
import org.json.JSONObject

/**
 * autoclaw（智谱 AutoClaw）渠道（10-08 移植，协议权威源=临时文件/dsh-phone-src/vendor/
 * dsh-codearts-auth/src/autoclaw-api.ts 逐字照抄，勿意译）。
 *
 * 登录 = 短信验证码：POST /userapi/v1/agent-send-code（data.result===true 才算发出）→
 * POST /userapi/v1/agent-login（code 要 Number）→ data 含 access_token/refresh_token/
 * user_id/user_name。续期 = POST /userapi/v1/refresh，失败信息含 (400002) 时改打
 * /userapi/v1/agent-refresh（源码 AutoclawApi.refresh 原语义）。
 * device_id 随机 UUID 生成后必须随凭据持久化（存 Account.extra），登录/续期/请求都带。
 *
 * 信封 {code, msg, data}：code!==0 即失败；msg 截 160 字符并隐藏 Bearer/JWT 片段
 * （照源码 replace(/Bearer\s+\S+|eyJ[A-Za-z0-9_.-]+/g, '[已隐藏]')）。
 *
 * 对话 = /autoclaw-proxy/proxy/autoclaw/ 下的 OpenAI 或 Anthropic 形，由模型目录里
 * models[].api（openai-completions | anthropic-messages）决定；当前 SseAggregator 只支持
 * OpenAI 形——头族按 OpenAI 发，Anthropic 模型处理与 minimax 同策略（留待后续批次）。
 */
object AutoclawChannel : ChatChannel {
    override val id = "autoclaw"
    override val displayName = "AutoClaw 智谱"
    override val chatBaseUrl = "https://autoglm-acceleration-api.zhipuai.cn/autoclaw-proxy/proxy/autoclaw"

    // 端点 origin 与公共应用标识（源码 AUTOCLAW.origin / APP_ID / APP_KEY 原文照抄）
    private const val ORIGIN = "https://autoglm-acceleration-api.zhipuai.cn"
    private const val APP_ID = "100003"
    private const val APP_KEY = "38d2391985e2369a5fb8227d8e6cd5e5"

    /**
     * 对话请求体 system 段协议前缀（源码 AUTOCLAW_SYSTEM_PREFIX 原文逐字照抄，
     * 权威源=临时文件/dsh-phone-src/vendor/dsh-codearts-auth/src/autoclaw.ts）。
     * 对话接口要求客户端身份和 Tooling 段，缺失时实机返回 HTTP 406/网关 502。
     * 注意：前缀末尾自带一个 \n；与已有 system 提示词拼接时中间再加一个 \n
     * （源码 autoclawSystem：PREFIX + `\n${system}`，即两段之间隔一个空行）。
     */
    private const val AUTOCLAW_SYSTEM_PREFIX =
        "You are a personal assistant running inside OpenClaw.\n\n## Tooling\nAvailable tools are policy-filtered. Names are case-sensitive; call exactly as listed.\n"

    /**
     * 源码 autoclawSystem 同语义：已有前缀则原样返回（幂等）；
     * 否则前缀开头，非空 system 用 \n 隔开接在后（前缀自身末尾已带 \n）。
     */
    private fun autoclawSystem(system: String?): String {
        return if (system?.startsWith(AUTOCLAW_SYSTEM_PREFIX) == true) system
        else AUTOCLAW_SYSTEM_PREFIX + (if (system.isNullOrEmpty()) "" else "\n$system")
    }

    // ==================== 签名头族（autoclawHeaders 逐字段照抄） ====================

    /**
     * X-Auth-Sign = md5("{APP_ID}&{秒级时间戳}&{APP_KEY}")；X-Tm 固定 win（App 端无
     * darwin/win32 之分，按官方 Windows 客户端发）；登录后加 Authorization: Bearer
     * （剥掉已有 Bearer 前缀再包，照源码 token.replace(/^Bearer\s+/i, '')）。
     */
    fun autoclawHeaders(token: String = ""): Map<String, String> {
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val sign = java.security.MessageDigest.getInstance("MD5")
            .digest("$APP_ID&$timestamp&$APP_KEY".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return mapOf(
            "Content-Type" to "application/json",
            "X-Version" to "2.0.4",
            "X-Tm" to "win",
            "X-Product" to "autoclaw",
            "X-Auth-Appid" to APP_ID,
            "X-Auth-TimeStamp" to timestamp,
            "X-Auth-Sign" to sign,
            "X-Lang" to "zh-CN",
            "X-Channel" to "official",
        ) + if (token.isNotEmpty()) mapOf("Authorization" to "Bearer ${token.replace(Regex("^Bearer\\s+", RegexOption.IGNORE_CASE), "")}") else emptyMap()
    }

    /** 公共请求壳：POST（body!=null）/GET，15s 超时；信封 code!==0 抛错（msg 脱敏截断） */
    private fun request(path: String, body: String?, token: String = ""): JSONObject {
        val r = if (body != null) AccountPool.channelPost("$ORIGIN$path", autoclawHeaders(token), body)
        else AccountPool.channelGet("$ORIGIN$path", autoclawHeaders(token))
        if (!r.ok) throw Exception("AutoClaw 请求失败（HTTP ${r.code}）")
        val text = if (r.body.length > 512 * 1024) throw Exception("AutoClaw 响应过大") else r.body
        val value = try { JSONObject(text) } catch (_: Exception) { throw Exception("AutoClaw 返回格式无效") }
        if (value.has("code") && !value.isNull("code") && value.optInt("code", 0) != 0) {
            // 不把完整响应或登录令牌写入日志（源码同语义：截 160 + 脱敏）
            val message = value.optString("msg").take(160)
                .replace(Regex("Bearer\\s+\\S+|eyJ[A-Za-z0-9_.-]+"), "[已隐藏]")
                .ifEmpty { "服务拒绝请求" }
            throw Exception("AutoClaw：$message（${value.optInt("code")}）")
        }
        return value
    }

    // ==================== 短信登录引擎（SmsLoginDialog provider=autoclaw 调） ====================

    data class SendResult(val ok: Boolean, val err: String)

    /** POST /userapi/v1/agent-send-code；data.result===true 才算发出（源码硬校验） */
    fun sendSmsCode(phone: String, deviceId: String): SendResult {
        return try {
            val body = JSONObject()
                .put("phone", phone)
                .put("source_id", "autoclaw")
                .put("device_id", deviceId)
            val r = request("/userapi/v1/agent-send-code", body.toString())
            if (r.optJSONObject("data")?.optBoolean("result") == true) SendResult(true, "")
            else SendResult(false, "AutoClaw 未确认发送验证码，请稍后重试")
        } catch (e: Exception) {
            SendResult(false, e.message ?: "发送失败")
        }
    }

    data class LoginResult(
        val accessToken: String,
        val refreshToken: String,
        val userId: String,
        val nickname: String,
        val err: String,
    )

    /** POST /userapi/v1/agent-login（code 转 Number，源码同款）；凭据字段不全判失败 */
    fun login(phone: String, code: String, deviceId: String): LoginResult {
        return try {
            val body = JSONObject()
                .put("phone", phone)
                .put("code", code.toLongOrNull() ?: JSONObject.NULL)
                .put("source_id", "autoclaw")
                .put("device_id", deviceId)
            val r = request("/userapi/v1/agent-login", body.toString())
            val d = r.optJSONObject("data")
            val access = d?.optString("access_token").orEmpty()
            val refresh = d?.optString("refresh_token").orEmpty()
            val userId = d?.optString("user_id").orEmpty()
            if (access.isEmpty() || refresh.isEmpty() || userId.isEmpty())
                return LoginResult("", "", "", "", "AutoClaw 登录未返回完整凭据")
            val nickname = d.optString("user_name").ifEmpty { "AutoClaw (${phone.takeLast(4)})" }
            LoginResult(access, refresh, userId, nickname, "")
        } catch (e: Exception) {
            LoginResult("", "", "", "", e.message ?: "登录失败")
        }
    }

    // ==================== 续期（/refresh，400002 回退 /agent-refresh） ====================

    /**
     * 源码 AutoclawApi.refresh：先打 /userapi/v1/refresh，错误信息含 (400002) 时
     * 改打 /userapi/v1/agent-refresh；响应缺凭据字段判失败。access_token 是 JWT，
     * expiresAt 由调用方按 JWT exp 解（jwtExpiryMs）。
     */
    override fun refresh(acc: AccountPool.Account): Triple<String, String, Long>? {
        return try {
            val deviceId = acc.extraStr("device_id")
            if (acc.refreshToken.isEmpty() || deviceId.isEmpty()) return null
            val body = JSONObject()
                .put("refresh_token", acc.refreshToken)
                .put("source_id", "autoclaw")
                .put("device_id", deviceId)
            val r = try {
                request("/userapi/v1/refresh", body.toString())
            } catch (e: Exception) {
                if (e.message?.contains("(400002)") != true) throw e
                request("/userapi/v1/agent-refresh", body.toString())
            }
            val d = r.optJSONObject("data") ?: return null
            val access = d.optString("access_token").ifEmpty { return null }
            val refresh = d.optString("refresh_token").ifEmpty { acc.refreshToken }
            Triple(access, refresh, jwtExpiryMs(access) ?: 0L)
        } catch (_: Exception) {
            null
        }
    }

    /** JWT exp 解析（autoclawExpiry 同语义）：payload 段 base64url → exp 秒 → ms；解不出返回 null */
    fun jwtExpiryMs(accessToken: String): Long? {
        return try {
            val payload = accessToken.replace(Regex("^Bearer\\s+", RegexOption.IGNORE_CASE), "").split(".")[1]
            val json = JSONObject(String(
                java.util.Base64.getUrlDecoder().decode(padBase64url(payload)),
                Charsets.UTF_8,
            ))
            val exp = json.optLong("exp", 0L)
            if (exp > 0) exp * 1000L else null
        } catch (_: Exception) {
            null
        }
    }

    private fun padBase64url(s: String): String {
        val rem = s.length % 4
        return if (rem == 0) s else s + "=".repeat(4 - rem)
    }

    // ==================== 对话 ====================

    /**
     * 头族按 OpenAI 形发（照源码 stream() 的 OpenAI 分支：autoclawHeaders() 基座 +
     * X-Authorization/X-Client-Type/X-Trace-Id/X-Request-Id/x_trace_id +
     * Accept: text/event-stream；Authorization: Bearer 由 SseAggregator 统一加）。
     * X-Request-Model: {model} 由 perRequestHeaders 按-请求注（见下方）。
     * TODO ①Anthropic 形模型（models[].api=anthropic-messages）走 /v1/messages +
     *     anthropic-version 头 + Anthropic SSE 事件流，当前 SseAggregator 只支持
     *     OpenAI 形——与 minimax 同策略，留待后续批次接。
     */
    override fun chatHeaders(accessToken: String): Map<String, String> = autoclawHeaders("") + mapOf(
        "X-Authorization" to "Bearer ${accessToken.replace(Regex("^Bearer\\s+", RegexOption.IGNORE_CASE), "")}",
        "X-Client-Type" to "pc",
        "X-Trace-Id" to DeviceCodeLogin.randomUuid(),
        "X-Request-Id" to DeviceCodeLogin.randomUuid(),
        "x_trace_id" to "autoclaw-desktop",
        "Accept" to "text/event-stream",
        "Content-Type" to "application/json; charset=utf-8",
    )

    /**
     * 按-请求注头（ChatChannel.perRequestHeaders 落地）：源码 stream() 还发
     * X-Request-Model: {model}（autoclaw.ts Object.assign 段逐字段照抄），此前
     * chatHeaders 拿不到 model 只能缺——现由 SseAggregator 在 extraHeaders 处合并。
     */
    override fun perRequestHeaders(model: String): Map<String, String> =
        if (model.isNotEmpty()) mapOf("X-Request-Model" to model) else emptyMap()

    /**
     * 请求体变换：解析 messages，给 system 角色消息（OpenAI 形 role:"system"）的
     * content 前拼 AUTOCLAW_SYSTEM_PREFIX（已带前缀则不重复，幂等，源码
     * autoclawSystem 同语义）；没有 system 消息则新建一条插在最前。其余字段原样保留。
     */
    override fun patchBody(bodyJson: String, model: String): String {
        return try {
            val o = JSONObject(bodyJson)
            val messages = o.optJSONArray("messages") ?: JSONArray()
            var inserted = false
            for (i in 0 until messages.length()) {
                val m = messages.optJSONObject(i) ?: continue
                if (m.optString("role") == "system") {
                    m.put("content", autoclawSystem(m.optString("content").ifEmpty { null }))
                    inserted = true
                    break // 源码 autoclawSystem 只处理首条 system 段；多 system 非本客户端形态
                }
            }
            if (!inserted) {
                // 没有 system 消息：新建一条插在最前（源码语义：前缀必须开头）
                val sys = JSONObject().put("role", "system").put("content", autoclawSystem(null))
                val arr = JSONArray()
                arr.put(sys)
                for (i in 0 until messages.length()) arr.put(messages.get(i))
                o.put("messages", arr)
            }
            o.toString()
        } catch (_: Exception) {
            bodyJson // 解析失败透传原样，由上游按原路径报错
        }
    }

    override fun classifyError(httpStatus: Int, body: String): ChatChannel.ErrClass = when {
        httpStatus == 401 || httpStatus == 403 -> ChatChannel.ErrClass.AUTH
        httpStatus == 429 -> ChatChannel.ErrClass.RATE_LIMIT
        // 信封式业务失败也按状态走（源码信封 code!=0 常见 400002=凭据失效）
        body.contains("(400002)") -> ChatChannel.ErrClass.AUTH
        else -> ChatChannel.ErrClass.OTHER
    }

    // ==================== 余额 / 模型（无签到，接口默认实现） ====================

    /** GET /agent-assetmgr/api/v2/wallets?biz_app_id=autoclaw → data.total_balance */
    override fun queryCredits(acc: AccountPool.Account): Double {
        return try {
            val r = request("/agent-assetmgr/api/v2/wallets?biz_app_id=autoclaw", null, acc.accessToken)
            val total = r.optJSONObject("data")?.optDouble("total_balance", Double.NaN) ?: Double.NaN
            // 源码硬校验：total 非有限数字即「未返回积分余额」
            if (total.isNaN() || total.isInfinite()) throw Exception("AutoClaw 未返回积分余额")
            total
        } catch (_: Exception) {
            Double.NaN
        }
    }

    /**
     * GET /autoclaw-proxy/proxy/autoclaw-model-config（要登录态）。
     * 解析校验照源码 parseAutoclawModels：models 数组缺失或 >128 判格式无效；
     * id 非串/空/超 256 判无效模型；api 字段只认 openai-completions|anthropic-messages
     * （缺省 openai-completions）；重复 id 去重（保留首个）。返回 id 清单
     * （Anthropic 形模型当前发不出去——SseAggregator 只支持 OpenAI 形，见 chatHeaders TODO）。
     */
    override fun fetchModels(accessToken: String): List<String> {
        return try {
            val r = request("/autoclaw-proxy/proxy/autoclaw-model-config", null, accessToken)
            val models = r.optJSONArray("models") ?: throw Exception("AutoClaw 模型目录格式无效")
            if (models.length() > 128) throw Exception("AutoClaw 模型目录格式无效")
            val seen = HashSet<String>()
            val out = ArrayList<String>()
            for (i in 0 until models.length()) {
                // 源码也收字符串形态（typeof raw === 'string' → {id: raw}）
                if (models.isNull(i)) continue
                val m = models.optJSONObject(i)
                if (m == null) {
                    val raw = models.optString(i)
                    if (raw.isNotEmpty() && raw.length <= 256 && seen.add(raw)) out.add(raw)
                    continue
                }
                val mid = m.optString("id")
                if (mid.isEmpty() || mid.length > 256) throw Exception("AutoClaw 返回了无效模型")
                if (m.has("api") && !m.isNull("api")) {
                    val api = m.optString("api")
                    if (api != "openai-completions" && api != "anthropic-messages")
                        throw Exception("AutoClaw 模型协议暂不支持")
                }
                if (seen.add(mid)) out.add(mid)
            }
            out
        } catch (_: Exception) {
            emptyList() // 失败返回空表由调用方兜底（源码：不编造模型目录）
        }
    }
}
