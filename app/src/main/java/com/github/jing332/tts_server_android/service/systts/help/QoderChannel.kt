package com.github.jing332.tts_server_android.service.systts.help

import org.json.JSONObject

/**
 * qoder/qodercn（阿里）渠道（10-09 全渠道批，协议=规格书 §4）。
 *
 * ⚠️⚠️ **本渠道对话被 WASM 加密链锁死**：请求体加密、`Bearer COSY.<载荷>.<签名>` 鉴权、
 * 模型目录解密全走官方内嵌 298KB WASM（qoder-auth-wasm），普通 Bearer 一律
 * `403 Signature invalid`——全清单唯一没有纯手写复刻路径的环节。
 *
 * 本期落**可复刻部分**：设备码登录引擎（PKCE 轮询）+ 续期 + 余额（/sash/ 端点
 * 无需 WASM 签名）。对话置 available=false（UI 显示「待接入」，等 WASM 移植评估：
 * Android 侧可试 WebView 跑 WASM 或嵌入 qjs/wasm 运行时——单独二期任务）。
 *
 * ⚠️ **签到 = /sash/ 活动领取（10-10 已接，协议=规格书 §4.8 + 插件 qoder-credits.ts）**：
 * 拉活动列表（带完整头族）→ 筛 CLAIM_BENEFIT/CLAIMABLE → 逐个 claim（body 空串）。
 * 可领项下发依赖机器头族（Cosy-ClientType:'10' + 成对 Cosy-MachineToken/MachineType），
 * token/type 从账号 extra 手填字段读取（用户从桌面端 machine_token.json 抄），
     * 没填则跳过领取并日志说明，不影响签到余额主链。coversToday 口径（10:00 UTC+8
     * 刷新）与 CheckinPolicy 对齐，见「签到/余额」节注释。
     */
object QoderChannel : ChatChannel {
    override val id = "qoder"
    override val displayName = "Qoder 阿里"
    override val chatBaseUrl = "https://api2.qoder.sh/algo/api/v2"
    override val available = true // 10-10 WASM 对话接线（WebView 跑官方 wasm，见 chatViaChannel 节）

    // ⚠️ 签到待接（10-10 对账 TOP5 第 5 条）：qoder 的「签到」= /sash/ 活动领取，claim
    // 依赖 Cosy-MachineToken 头族（规格书 §4.8，WASM 机器签名，与对话同链锁死）且
    // claim body 必须是空串——本期不实现，checkIn 走 ChatChannel 默认（「无签到接口」，
    // AccountCheckinReceiver 计为跳过不记账）。将来接入时注意 coversToday 口径：
    // qoder 活动每日 10:00（UTC+8）才刷新，10 点前上游报的 already-claimed 是昨天的
    // （插件 ClaimOutcome.coversToday 定位的真实缺陷），记账只认本地 lastCheckinDate
    // （UTC+8），不直信上游文案。

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

    /**
     * 403 三义（规格书 §4.5，插件 model-queue.ts 的事故修正——旧实现把所有 401/403
     * 当认证失败导致排队永远等不到）：对话锁死期间本分类是占位，解锁时按此接线。
     *  - `10605`（model_queued）→ RATE_LIMIT：按服务端 retryAfterSeconds 等待重试，不是认证问题；
     *  - `duplicate_request` → OTHER：不刷新直接重发（重发语义在轮换层，这里不标 AUTH）；
     *  - `105`（auth_error）或 401 → AUTH：**唯一**该走续期的情形。
     */
    override fun classifyError(httpStatus: Int, body: String): ChatChannel.ErrClass = when {
        httpStatus == 401 -> ChatChannel.ErrClass.AUTH
        httpStatus == 403 && body.contains("105") && body.contains("auth") -> ChatChannel.ErrClass.AUTH
        httpStatus == 403 && body.contains("duplicate_request") -> ChatChannel.ErrClass.OTHER // 重发，勿续期
        body.contains("10605") -> ChatChannel.ErrClass.RATE_LIMIT // model_queued（403/流内帧都可能有）
        body.contains("110") -> ChatChannel.ErrClass.RATE_LIMIT // billing 日额
        else -> ChatChannel.ErrClass.OTHER
    }

    // ==================== 签到 / 余额（/sash/ 无需 WASM） ====================

    /*
     * 签到 = /sash/ 活动领取（10-10 已接，对账 TOP5 第 5 条；协议=规格书 §4.8，
     * 权威实现=插件 qoder-credits.ts / qoder-machine.ts，本节注释只留与实现决策相关的坑）：
     *
     * ① 头族消融实测（插件 2026-09-21 抓包）：`Cosy-ClientType: '10'`（桌面 app 身份，
     *    注意余额查询用的 '5' 是 CLI 身份、两者不要合并）是前提，但**必要不充分**——
     *    必须再带成对的 `Cosy-MachineToken` + `Cosy-MachineType`，缺任一服务端只回
     *    1 条 VIEW_DETAILS（无 CLAIM_BENEFIT），症状=「没有可领取的活动」且无法与
     *    「已领完」区分。Cosy-MachineId/Version/MachineOS/Hostname/MachineCode 实测非必需。
     * ② machine token 官方来源 = 桌面端 `%APPDATA%\Qoder\SharedClientCache\cache\machine_token.json`
     *    的 `{token, type}`（由 runtime-info.exe 生成；旧文件 token 依然有效，不做时效校验）。
     *    Android 拿不到该文件 → 用户手填进账号 extra（字段名 cosy_machine_token，JSON 形状
     *    照 machine_token.json 原文，见本节常量注释）。没填 = 保守降级：跳过领取并日志说明，
     *    余额主链（queryCredits）不受影响。插件教训：**发空串头会坏事，漏发才是安全降级**。
     * ③ claim：POST /sash/api/v1/me/campaigns/{id}/claim，**body 必须空串**（抓包
     *    content-length:0；发 {} 未经验证不做）。幂等判据是响应体 `replayed:true`
     *    （重复领取也 200，但无 benefit、claimedAt 是旧时间）——只看状态码会把
     *    「今天已领」误报成「领取成功 +100」。
     * ④ coversToday：活动每日 10:00（UTC+8）刷新，刷新前查到的 CLAIMED 属于昨天——
     *    本文件在「无可领项但有 CLAIMED」分支按该口径回话（刷新前=未刷新提示，刷新后=
     *    已领取）；「成功是否记到今天」的落盘判据仍收口在 CheckinPolicy/
     *    AccountCheckinReceiver（不直信上游文案），两处语义对齐不重复落表。
     * ⑤ 本实现不落盘记账：成功与否由 AccountCheckinReceiver 统一写 lastCheckinDate
     *    （与 zcode 同款纪律）。
     */

    /** extra 字段名：用户手填的 machine 身份（值 = machine_token.json 原文或其 {token,type} 部分）。 */
    private const val EXTRA_MACHINE = "cosy_machine_token"

    // /sash/ 端点（挂 OPEN_API_BASE；活动刷新时刻 10:00 UTC+8 只在 CheckinPolicy 注释里管）
    private const val CAMPAIGNS_PATH = "/sash/api/v1/me/campaigns"
    private const val CLAIM_SUFFIX = "/claim"

    /**
     * 从账号 extra 读机器身份（手填字段）。
     *
     * 容忍两种写法（都来自官方文件原文，用户直接整贴最不容易错）：
     *  - 完整 machine_token.json 原文：`{"token":"…","type":"…","updateAt":…}`
     *  - 只留成对两键的精简 JSON：`{"token":"…","type":"…"}`
     *
     * ⚠️ token 与 type **必须成对且都非空**（缺一服务端即退化下发，见①消融），
     * 形状不符一律视为没填（不凑半对头发）。
     */
    private fun machineIdentity(acc: AccountPool.Account): Pair<String, String>? {
        val raw = acc.extraStr(EXTRA_MACHINE)
        if (raw.isEmpty()) return null
        return try {
            val o = JSONObject(raw)
            val token = o.optString("token")
            val type = o.optString("type")
            if (token.isNotEmpty() && type.isNotEmpty()) token to type else null
        } catch (_: Exception) {
            null // 整段填坏了当没填：主链照常，领取侧由 checkIn 日志说明
        }
    }

    /** /sash/ 公共请求头（活动端点用 '10' 桌面身份；有机器身份则成对并入，无则漏发降级）。 */
    private fun sashHeaders(acc: AccountPool.Account, withMachine: Boolean): Map<String, String> {
        val h = linkedMapOf(
            "Accept" to "application/json",
            "Authorization" to "Bearer ${acc.accessToken}",
            "Cosy-ClientType" to "10", // 桌面 app 身份（余额查询的 '5' 是 CLI 身份，两身份不合并）
            "User-Agent" to "Qoder",
        )
        if (withMachine) {
            val id = machineIdentity(acc)
            if (id != null) {
                h["Cosy-MachineToken"] = id.first
                h["Cosy-MachineType"] = id.second // 必须成对（缺任一服务端退化，见①）
            }
        }
        return h
    }

    /**
     * claim 专用 POST：body 必须是**空串**（content-length:0）。
     *
     * ⚠️ 不走 AccountPool.channelPost——它对空 body 不开 doOutput，多数网关会当作
     * 无 Content-Length 的普通 POST，虽大致等价，但这里照抓包原文收口（显式
     * doOutput + content-length:0），把「body 形状」这个已实测的坑钉死在实现里。
     */
    private fun claimPost(url: String, headers: Map<String, String>): ChannelHttpResp {
        var conn: java.net.HttpURLConnection? = null
        return try {
            conn = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 30_000
                doOutput = true
                setFixedLengthStreamingMode(0) // 抓包 content-length:0
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
                if (!headers.containsKey("Content-Type"))
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            val code = conn.responseCode
            val stream = if (code >= 400) conn.errorStream else conn.inputStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            ChannelHttpResp(code in 200..299, code, text)
        } catch (e: Exception) {
            ChannelHttpResp(false, -1, e.message ?: e.toString())
        } finally {
            conn?.disconnect()
        }
    }

    /** 活动列表响应里的一条活动（只保留判据所需字段）。 */
    private data class Campaign(val campaignId: String, val actionType: String, val claimStatus: String, val amount: Double)

    /** 解析 campaigns[]：形状非法返回空表（不判「无活动」，与失败区分靠调用方看 HTTP）。 */
    private fun parseCampaigns(body: String): List<Campaign> {
        val arr = JSONObject(body).optJSONArray("campaigns") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val c = arr.optJSONObject(i) ?: return@mapNotNull null
            val id = c.optString("campaignId")
            if (id.isEmpty()) return@mapNotNull null
            Campaign(
                campaignId = id,
                actionType = c.optString("actionType"),
                claimStatus = c.optString("claimStatus"),
                amount = c.optJSONObject("benefit")?.optDouble("amount", 0.0) ?: 0.0,
            )
        }
    }

    /**
     * 签到 = 领取该账号当前所有可领活动（CLAIM_BENEFIT + CLAIMABLE；一个账号可能同时
     * 挂每日 100 与运营活动，逐个领而非只领第一个——照插件）。
     *
     * 返回 (true, "…") 计签到成功（含 replayed 的幂等回放——重复领取无害，记账层
     * coversToday 判据不受它影响）；(false, "…") 只在不影响记账语义的分支用：
     *  - 没填 machine token → 明说「已跳过」（AccountCheckinReceiver 按文案计跳过不记账，
     *    与「暂不支持」同路），不标死账号；
     *  - 401/403(105) → 提示凭据失效，走主链续期分类，不在这里标死账号。
     */
    override fun checkIn(acc: AccountPool.Account): Pair<Boolean, String> {
        // 机器身份必须先在：没填就不打活动接口（头不全服务端只回 VIEW_DETAILS，
        // 「无可领项」与「已领完」无法区分——插件真实缺陷的方向性教训：宁可跳过，
        // 不能把空态误报成「已领」）。⚠️ 文案含「暂不支持」：AccountCheckinReceiver
        // 按「无签到接口/暂不支持」计跳过不记账（本任务不越界改它），且不标死账号。
        if (machineIdentity(acc) == null)
            return false to "Qoder 活动自动领取暂不支持：账号 extra 未填机器身份（cosy_machine_token，取桌面端 machine_token.json 的 token/type 成对填入），签到/余额主链不受影响"

        return try {
            // ① 拉活动列表（完整头族）
            val r = AccountPool.channelGet("$OPEN_API_BASE$CAMPAIGNS_PATH", sashHeaders(acc, withMachine = true))
            if (!r.ok)
                return false to "Qoder 活动列表查询失败：HTTP ${r.code}（凭据失效请在账号页续期后重试）"
            val campaigns = parseCampaigns(r.body)
            // 权威判据：CLAIM_BENEFIT 且当前 CLAIMABLE（VIEW_DETAILS 是跳转项，不领）
            val targets = campaigns.filter { it.actionType == "CLAIM_BENEFIT" && it.claimStatus == "CLAIMABLE" }
            if (targets.isEmpty()) {
                val claimedBefore = campaigns.any { it.actionType == "CLAIM_BENEFIT" && it.claimStatus == "CLAIMED" }
                if (claimedBefore) {
                    // coversToday 口径：活动每日 10:00（UTC+8）刷新，刷新前看到的那条 CLAIMED
                    // 属于**昨天**——报「今日已领」是谎报且方向不可逆（用户会以为今天不必再领，
                    // 官方 IDE 里却还没刷新）。刷新前一律报「未刷新」不记账；刷新后才算今天已领
                    // （真·已领取返回 true，让记账层把当天记上，重复触发就此打住）。
                    val refreshed = (System.currentTimeMillis() + CheckinPolicy.UTC8_OFFSET_MS) / 3_600_000L % 24 >= 10
                    return if (refreshed) true to "Qoder 今日活动已领取（无可领项）"
                    else false to "Qoder 每日活动尚未刷新（每日 10:00 UTC+8），当前是昨天那一轮，10 点后再试（自动领取暂不支持 10 点前的轮次，不算失败）"
                }
                // 三态不可区分：未刷新中的空档 / 账号本就无此类活动 / 机器身份没配对生效——
                // 保守报「无可领」不记账，不谎报已领（插件「无可领≠已领」的方向性教训）。
                // ⚠️ 文案带「暂不支持」= 记账层按跳过算（非可操作空态，不算失败制造假警报）
                return false to "Qoder 当前没有可领取的活动（若官方客户端里可领，请检查 extra 机器身份 token/type 是否成对填对；账号无活动时暂不支持自动领取）"
            }
            // ② 逐个 claim（幂等：replayed:true=服务端回放上次结果，仍算成功）
            var total = 0.0
            var claimedCount = 0
            var firstErr = ""
            for (t in targets) {
                val cr = claimPost("$OPEN_API_BASE$CAMPAIGNS_PATH/${java.net.URLEncoder.encode(t.campaignId, "UTF-8")}$CLAIM_SUFFIX", sashHeaders(acc, withMachine = true))
                if (!cr.ok) {
                    // 不标死账号：非 2xx 只记原因继续下一个（claim 失败常见=排名竞争/临时限流）
                    if (firstErr.isEmpty()) firstErr = "HTTP ${cr.code}：${cr.body.take(120)}"
                    continue
                }
                val o = JSONObject(cr.body)
                if (o.optBoolean("replayed", false)) {
                    claimedCount++ // 重复领取（回放旧结果）：无害，按已处理计
                } else if (o.optString("status").let { it.isNotEmpty() && it != "CLAIMED" }) {
                    if (firstErr.isEmpty()) firstErr = "领取未成功（status=${o.optString("status")}）"
                } else {
                    claimedCount++
                    total += o.optJSONObject("benefit")?.optDouble("amount", 0.0) ?: t.amount
                }
            }
            if (claimedCount > 0) true to "Qoder 活动领取成功 ${claimedCount}/${targets.size} 项，+${total.toLong()} credits"
            else false to "Qoder 活动领取失败：$firstErr"
        } catch (e: Exception) {
            false to "Qoder 活动领取异常：${e.message}"
        }
    }

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

    // ==================== WASM 加密对话（10-10 接线，任务书 4b） ====================

    // 链路：QoderWasmBridge（无头 WebView 跑官方 WASM）生成加密请求三件套
    // {url, headers, body} → HttpURLConnection 发送（headers 原样透传，Authorization
    // 是 WASM 生成的 Bearer COSY.<载荷>.<签名>，覆盖即 403 Signature invalid）→
    // 响应 SSE 每帧剥信封（外层 {headers, body, statusCode...}，内层 body 才是
    // 标准 OpenAI chunk JSON 字符串——请求加密、响应不加密）。
    // 403 三义（10605 排队/105 auth/duplicate_request 重发）见 classifyError。

    /** 加密端点 host（bridge.js QODER_ENCRYPTED_INFER_BASE 同值）。 */
    private const val INFER_HOST = "https://api2.qoder.sh"

    override fun chatViaChannel(
        acc: AccountPool.Account,
        bodyJson: String,
        model: String,
        cancelled: Cancelled,
    ): Pair<Boolean, String> {
        return try {
            // uid：加密链必备（runtime auth fields 的身份来源）；缺了提示重登，不崩
            val uid = acc.extraStr("_uid").ifEmpty { acc.extraStr("uid") }
            if (uid.isEmpty()) return false to "Qoder 账号缺少 uid（请删除后重新登录该账号）"
            // machine_id：per 账号持久化（WASM 上下文绑定）；登录时已存，无则现生成补落盘
            // （落盘失败无害：服务端不绑定 machine 身份到凭据，下次对话重生成）
            var machineId = acc.extraStr("machine_id")
            if (machineId.isEmpty()) {
                machineId = DeviceCodeLogin.randomUuid()
                val updated = acc.withExtra("machine_id", machineId)
                AccountPool.save(AccountPool.load().map { if (it.id == acc.id) updated else it })
            }
            // 明文请求基本字段（OpenAI 形）：抽 messages/userText，剥离 stream/stream_options
            // 等加密端点不认识的字段（服务端拒收未知顶层字段风险，宁剥勿传）
            val src = JSONObject(bodyJson)
            val messages = src.optJSONArray("messages")
            val userText = lastUserText(messages) ?: run {
                return false to "Qoder 请求无 user 消息（朗读链应恒有）"
            }
            // 建上下文 + 构造加密请求（JS 侧负责明文 payload 完整形状与 WASM 加密）
            val ctxId = QoderWasmBridge.createContext(uid, acc.accessToken, machineId)
                ?: return false to "Qoder WASM 上下文创建失败（WebView 桥不可用？看日志）"
            val historyArr = org.json.JSONArray()
            if (messages != null) {
                for (i in 0 until messages.length()) {
                    val m = messages.optJSONObject(i) ?: continue
                    historyArr.put(JSONObject().put("role", m.optString("role")).put("content", m.optString("content")))
                }
            }
            val infer = QoderWasmBridge.prepareInfer(ctxId, model, userText, false, historyArr.toString(), "system")
                ?: return false to "Qoder WASM 加密请求构造失败"
            QoderWasmBridge.dropContext(ctxId)

            // 发送（headers 原样透传——含 COSY 签名 Authorization）
            val headers = mutableMapOf(
                "Accept" to "text/event-stream",
                "Content-Type" to "application/json; charset=utf-8",
            )
            val hs = infer.optJSONObject("headers")
            if (hs != null) for (k in hs.keys()) headers[k] = hs.optString(k)
            val conn = java.net.URL(infer.optString("url")).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = 120_000 // 流式首包可能慢（排队/思考）
            for ((k, v) in headers) conn.setRequestProperty(k, v)
            conn.doOutput = true
            conn.outputStream.use { it.write(infer.optString("body").toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            if (code < 200 || code >= 300) {
                val err = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                return false to "HTTP $code：${err.take(180).ifEmpty { "无响应内容" }}"
            }

            // 消费 SSE：每帧剥信封取内层 OpenAI chunk（unwrapQoderEnvelope 语义）
            val content = StringBuilder()
            var streamEnded = false
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                while (true) {
                    if (cancelled.isCancelled()) return false to "已取消"
                    val line = reader.readLine() ?: break
                    if (!line.startsWith("data:")) continue
                    val payload = line.removePrefix("data:").trim()
                    if (payload.isEmpty()) continue
                    if (payload == "[DONE]") { streamEnded = true; break }
                    // 信封剥壳：{headers, body:"<openai chunk json 字符串>", statusCode...}
                    val inner = envelopeInner(payload) ?: continue
                    if (inner == "[DONE]") { streamEnded = true; break }
                    try {
                        val o = JSONObject(inner)
                        // 流内错误帧（排队 10605 等）：抛给轮换层按 classifyError 语义处理
                        val bizCode = o.optString("code")
                        if (bizCode.isNotEmpty() && bizCode != "0") {
                            conn.disconnect()
                            return false to "HTTP 403：$inner"
                        }
                        val choices = o.optJSONArray("choices") ?: continue
                        val c = choices.optJSONObject(0) ?: continue
                        val delta = c.optJSONObject("delta") ?: c.optJSONObject("message")
                        delta?.optString("content")?.let { if (it.isNotEmpty()) content.append(it) }
                        if (c.optString("finish_reason").isNotEmpty()) streamEnded = true
                    } catch (_: Exception) {
                        // 内层不是 JSON（如 [FAIL]node 文本）：按错误帧转发
                        if (inner.contains("[FAIL]")) {
                            conn.disconnect()
                            return false to "HTTP 403：$inner"
                        }
                    }
                }
            }
            val text = content.toString()
            when {
                text.isNotEmpty() -> {
                    // 半截内容纪律：已吐内容但流未正常收尾——按截断结果返回 ok（不换号重放）
                    true to text
                }
                streamEnded -> false to "Qoder 对话完成但无内容"
                else -> false to "Qoder 对话流中断（无内容）"
            }
        } catch (e: Exception) {
            false to "Qoder 对话异常：${e.message}"
        }
    }

    /** 信封剥壳：取外层 JSON 的 body 字段（字符串原样返回；缺 body=已是标准帧，原样透传）。 */
    private fun envelopeInner(payload: String): String? {
        val o = try { JSONObject(payload) } catch (_: Exception) { return null }
        if (!o.has("body")) return payload
        val b = o.opt("body")
        return when (b) {
            is String -> b
            null -> null
            else -> b.toString()
        }
    }

    /** 取最后一条 user 消息文本（加密端点 chat_context.text 语义）。 */
    private fun lastUserText(messages: org.json.JSONArray?): String? {
        if (messages == null) return null
        for (i in messages.length() - 1 downTo 0) {
            val m = messages.optJSONObject(i) ?: continue
            if (m.optString("role") == "user") {
                val c = m.opt("content")
                return when (c) {
                    is String -> c.ifEmpty { null }
                    is org.json.JSONArray -> {
                        // 多模态块取 text 块拼接（朗读场景一般纯文本）
                        val sb = StringBuilder()
                        for (j in 0 until c.length()) {
                            val blk = c.optJSONObject(j) ?: continue
                            if (blk.optString("type") == "text") sb.append(blk.optString("text"))
                        }
                        sb.toString().ifEmpty { null }
                    }
                    else -> null
                }
            }
        }
        return null
    }

    override fun fetchModels(accessToken: String): List<String> = listOf(
        "auto", "ultimate", "performance", "efficient", "smodel", "cmodel",
        "qmodel_38max", "qfmodel", "qmodel_latest", "qmodel", "kmodel_latest", "kmodel",
        "gmodel", "gfmodel", "dmodel", "dfmodel", "mmodel",
    )
}
