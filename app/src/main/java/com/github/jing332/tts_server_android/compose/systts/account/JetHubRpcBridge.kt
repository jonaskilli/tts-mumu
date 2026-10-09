package com.github.jing332.tts_server_android.compose.systts.account

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.github.jing332.tts_server_android.service.systts.help.AccountPool
import com.github.jing332.tts_server_android.service.systts.help.ChannelBootstrap
import com.github.jing332.tts_server_android.service.systts.help.ChatChannels
import org.json.JSONArray
import org.json.JSONObject

/**
 * Jet Hub 插件 UI 的原生 RPC 桥（10-10）。
 *
 * 路线：插件前端（React 原版，构建产物 assets/jethub/jethub-ui.js）+ 官方手机适配层
 * （dsh-phone.css/js）跑在 WebView 里；前端通过 `rpcCall(endpoint, payload)` 发请求，
 * 本类把端点映射到 app 现有的账号池（AccountPool / ChatChannels）。
 *
 * 契约来源 = 插件服务端 `src/jet-hub-rpc.ts`（逐端点核对过响应结构）；
 * 信封：JS 侧 `AndroidBridge.rpc(callId, endpoint, payloadJson)`，
 * Kotlin 回 `window.__jhRpc(callId, json)`，json = `{"ok":true,"value":…}` 或 `{"ok":false,"error":"…"}`。
 *
 * ⚠️ provider 命名映射：插件对 CodeBuddy 用 `buddy`，app 用 `codebuddy`——进出都要转，
 * 否则 CodeBuddy 分组永远空。
 *
 * ⚠️ 本桥只覆盖账号面板真正用到的端点；app 没有对应能力的（模型黑名单、备份导出、
 * 聚合目录、cline 额度、成长任务）返回 `ok:false` + 说明，让插件 UI 显示错误而不是崩。
 */
class JetHubRpcBridge(private val context: android.content.Context) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null

    /**
     * 「新建账号」去原生登录页（10-10）：13 渠道登录形态各异（WebView/设备码/凭据直填/
     * 扫码/短信），这些流程原生页已全部接好——桥不重写，只负责把请求转过去。
     * 由宿主 Activity 设置（它才能 registerForActivityResult）。
     */
    var onLaunchLogin: ((provider: String) -> Unit)? = null

    fun attach(wv: WebView) {
        webView = wv
    }

    /** JS 桥入口（@JavascriptInterface 在 WebView 的 JS 线程执行，可阻塞做网络/IO） */
    inner class Impl {
        @JavascriptInterface
        fun rpc(callId: String, endpoint: String, payloadJson: String) {
            val envelope = try {
                val payload = runCatching { JSONObject(payloadJson) }.getOrElse { JSONObject() }
                val value = handle(endpoint, payload)
                JSONObject().put("ok", true).put("value", value)
            } catch (e: Exception) {
                JSONObject().put("ok", false).put("error", e.message ?: "rpc failed")
            }
            val script = "window.__jhRpc('$callId', ${JSONObject.quote(envelope.toString())})"
            mainHandler.post {
                runCatching { webView?.evaluateJavascript(script, null) }
            }
        }

        @JavascriptInterface
        fun close() {
            (context as? android.app.Activity)?.finish()
        }
    }

    // ==================== 端点分派 ====================

    private fun handle(endpoint: String, p: JSONObject): Any = when (endpoint) {
        "provider.status" -> providerStatus(p)
        "account.list" -> accountList(p)
        "account.create" -> accountCreate(p)
        "account.update" -> accountUpdate(p)
        "account.delete" -> accountDelete(p)
        "account.reorder" -> accountReorder(p)
        "account.reset" -> accountReset(p)
        "account.resetAll" -> accountResetAll(p)
        "account.retest" -> accountRetest(p)
        "account.retestAll" -> accountRetestAll(p)
        "account.test" -> accountTest(p)
        "credits.balances" -> creditsBalances(p)
        // 签到三件套（10-10 晚批）：一键领取（面板级）/ 单卡签到 / 单卡续期。
        // app 侧能力全在（AccountPool.checkInAny/refreshAny 按渠道路由 13 渠道），
        // 桥只做形状翻译——照插件服务端 jet-hub-rpc.ts 的响应契约。
        "credits.claimAll" -> creditsClaimAll(p)
        "account.checkin" -> accountCheckin(p)
        "account.refresh" -> accountRefresh(p)
        "backup.status" -> JSONObject().put("accounts", AccountPool.load().size).put("withoutExpiry", 0)
        // 登录：app 走自己的原生登录页（桥的 create 已把用户送过去），
        // 这里以「该渠道账号数变化」为完成判据——用户回来时列表已刷新。
        "login.poll" -> loginPoll(p)
        // app 尚无对应能力的端点：明确报错（插件 UI 会显示原因，不会崩）
        "model.list" -> throw IllegalStateException("模型显示列表尚未接入（app 侧模型可见性开关未实现）")
        "aggregate.catalog" -> throw IllegalStateException("聚合模型尚未接入")
        "cline.quota" -> throw IllegalStateException("订阅额度尚未接入")
        "backup.export" -> throw IllegalStateException("备份导出尚未接入（用设置页的备份与恢复）")
        "backup.import" -> throw IllegalStateException("备份导入尚未接入（用设置页的备份与恢复）")
        "onboarding.claim" -> throw IllegalStateException("成长任务尚未接入")
        else -> throw IllegalStateException("未实现的端点：$endpoint")
    }

    /**
     * 新建账号：把用户送到原生登录页（13 渠道登录流全在那边），立刻返回一个
     * `loginUrl` 占位让插件前端进入「轮询等待」分支。
     *
     * ⚠️ 插件前端的 `loginUrl` 语义是「window.open 打开的网页」——手机 WebView 里
     * 开新窗口不可用，故这里返回**空 loginUrl + reused=true**（插件认出 reused 就会
     * 跳过 window.open、直接 loadAccounts），同时我们另起原生页让用户完成登录。
     */
    private fun accountCreate(p: JSONObject): JSONObject {
        val pluginProvider = p.optString("provider")
        if (pluginProvider.isEmpty()) throw IllegalStateException("缺 provider")
        val appProvider = toAppProvider(pluginProvider)
        val launch = onLaunchLogin
            ?: throw IllegalStateException("登录入口未就绪（请重启本页）")
        mainHandler.post { launch(appProvider) }
        // reused=true：插件据此走「已复用、直接重载列表」分支，不会去 window.open 空地址
        return JSONObject().put("reused", true).put("loginUrl", "").put("accountId", "")
    }

    /** 登录轮询：插件 UI 每 1s 调一次；原生页关闭后插件会自行 loadAccounts，故恒回 done=false */
    private fun loginPoll(p: JSONObject): JSONObject = JSONObject().put("done", false)

    // ==================== 端点实现 ====================

    /** 供应商状态：账号数 + 模型计数（app 无模型黑名单 → disabled 恒 0、closed 恒 false） */
    private fun providerStatus(p: JSONObject): JSONObject {
        val wanted = p.optJSONArray("providers") ?: JSONArray()
        val all = AccountPool.load()
        val statuses = JSONObject()
        for (i in 0 until wanted.length()) {
            val pluginId = wanted.optString(i)
            if (pluginId.isEmpty()) continue
            val appId = toAppProvider(pluginId)
            val list = all.filter { it.provider == appId }
            statuses.put(pluginId, JSONObject()
                .put("models", JSONObject().put("total", 0).put("disabled", 0))
                .put("accounts", JSONObject()
                    .put("total", list.size)
                    .put("enabled", list.count { it.enabled }))
                .put("closed", false))
        }
        return JSONObject().put("statuses", statuses)
    }

    /** 账号列表：app Account → 插件期望形状 */
    private fun accountList(p: JSONObject): JSONObject {
        val appId = toAppProvider(p.optString("provider"))
        val arr = JSONArray()
        AccountPool.load().filter { it.provider == appId }.forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("provider", toPluginProvider(a.provider))
                put("nickname", a.nickname)
                put("enabled", a.enabled)
                put("credentialRef", synthCredentialRef(a))
                put("createdAt", a.createdAt)
                // expiresAt=0（未知）时省略，插件侧按可选处理
                if (a.expiresAt > 0) put("expiresAt", a.expiresAt)
                put("refreshable", a.refreshToken.isNotEmpty())
                if (a.modelRateLimits.isNotEmpty()) put("modelRateLimits", JSONObject(a.modelRateLimits))
            })
        }
        return JSONObject().put("accounts", arr)
    }

    /** 改名 / 停用启用（插件 patch 只带 nickname/enabled） */
    private fun accountUpdate(p: JSONObject): Any {
        val id = p.optString("accountId")
        val patch = p.optJSONObject("patch") ?: JSONObject()
        val acc = AccountPool.load().firstOrNull { it.id == id }
            ?: throw IllegalStateException("账号不存在：$id")
        var nick = acc.nickname
        var enabled = acc.enabled
        if (patch.has("nickname")) nick = patch.optString("nickname").ifEmpty { acc.nickname }
        if (patch.has("enabled")) enabled = patch.optBoolean("enabled", acc.enabled)
        AccountPool.save(AccountPool.load().map {
            if (it.id == id) it.copy(nickname = nick, enabled = enabled) else it
        })
        return JSONObject.NULL
    }

    private fun accountDelete(p: JSONObject): Any {
        val id = p.optString("accountId")
        if (!AccountPool.remove(id)) throw IllegalStateException("账号不存在：$id")
        return JSONObject.NULL
    }

    /** 拖拽排序：按 orderedIds 重排该 provider 的账号（顺序=自动选号优先级） */
    private fun accountReorder(p: JSONObject): Any {
        val appId = toAppProvider(p.optString("provider"))
        val ordered = p.optJSONArray("orderedIds") ?: throw IllegalStateException("缺 orderedIds")
        val wanted = (0 until ordered.length()).map { ordered.optString(it) }
        val all = AccountPool.load()
        val mine = all.filter { it.provider == appId }
        if (mine.size != wanted.size || mine.map { it.id }.toSet() != wanted.toSet()) {
            throw IllegalStateException("账号列表已变化，请刷新后重试")
        }
        val byId = mine.associateBy { it.id }
        var i = 0
        // 原位替换（保持其它 provider 的绝对位置不变，照插件「只动本 provider」约定）
        val next = all.map { if (it.provider == appId) byId[wanted[i++]] ?: it else it }
        AccountPool.save(next)
        return JSONObject.NULL
    }

    /** 重置 = 清该账号全部限流标记（不发请求） */
    private fun accountReset(p: JSONObject): JSONObject {
        val id = p.optString("accountId")
        // 先记条数再清（clearRateLimits 清空后 modelRateLimits 已空，取不到原值）
        val before = AccountPool.load().firstOrNull { it.id == id }?.modelRateLimits?.size ?: 0
        AccountPool.clearRateLimits(id)
        return JSONObject()
            .put("clearedCount", before)
            .put("accountCount", if (AccountPool.load().any { it.id == id }) 1 else 0)
    }

    private fun accountResetAll(p: JSONObject): JSONObject {
        val appId = toAppProvider(p.optString("provider"))
        val before = AccountPool.load().filter { it.provider == appId }
            .sumOf { it.modelRateLimits.size }
        val accounts = AccountPool.resetAllAccounts(appId)
        return JSONObject().put("clearedCount", before).put("accountCount", accounts)
    }

    /** 重测：真发最小消息验证（会消耗额度），通了的清标记 */
    private fun accountRetest(p: JSONObject): JSONObject {
        val id = p.optString("accountId")
        val r = AccountPool.retestAccount(id)
        if (r.error.isNotEmpty()) throw IllegalStateException(r.error)
        val acc = AccountPool.load().firstOrNull { it.id == id }
        val item = JSONObject()
            .put("accountId", id)
            .put("nickname", acc?.nickname ?: "")
            .put("tested", r.tested)
            .put("cleared", strArray(r.cleared))
            .put("stillLimited", JSONArray().apply {
                r.stillLimited.forEach { m ->
                    put(JSONObject().put("modelId", m).put("ok", false).put("message", "仍受限"))
                }
            })
        return JSONObject()
            .put("accounts", JSONArray().put(item))
            .put("clearedCount", r.cleared.size)
    }

    private fun accountRetestAll(p: JSONObject): JSONObject {
        val appId = toAppProvider(p.optString("provider"))
        val list = AccountPool.load().filter { it.provider == appId }
        val accounts = JSONArray()
        var clearedTotal = 0
        list.forEach { a ->
            val r = AccountPool.retestAccount(a.id)
            clearedTotal += r.cleared.size
            accounts.put(JSONObject()
                .put("accountId", a.id)
                .put("nickname", a.nickname)
                .put("tested", r.tested)
                .put("cleared", strArray(r.cleared))
                .put("stillLimited", JSONArray().apply {
                    r.stillLimited.forEach { m ->
                        put(JSONObject().put("modelId", m).put("ok", false).put("message", "仍受限"))
                    }
                }))
        }
        return JSONObject().put("accounts", accounts).put("clearedCount", clearedTotal)
    }

    /** 单账号测试：无条件真发一次（不依赖有无标记，也不写存储） */
    private fun accountTest(p: JSONObject): JSONObject {
        val id = p.optString("accountId")
        val acc = AccountPool.load().firstOrNull { it.id == id }
            ?: throw IllegalStateException("账号不存在：$id")
        val model = p.optString("modelId").ifEmpty { acc.modelRateLimits.keys.firstOrNull() ?: "" }
        if (model.isEmpty()) throw IllegalStateException("该账号无可测模型（请先拉取模型清单）")
        ChannelBootstrap.install()
        val ch = ChatChannels.byProvider(acc.provider)
        // 对话基址：与重测链同口径（非 codebuddy 用渠道声明 chatBaseUrl；codebuddy 走 /v2）
        val base = ch?.chatBaseUrl ?: "https://${AccountPool.CHAT_HOST}/v2"
        val raw = JSONObject()
            .put("model", model)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", "只回复 pong")))
            .put("max_tokens", 16).put("temperature", 0.0).toString()
        val body = ch?.patchBody(raw, model) ?: raw
        val (ok, msg) = com.github.jing332.tts_server_android.service.systts.help.SseAggregator.chatCompletion(
            base, acc.accessToken, body,
            channel = ch, model = model,
            extraHeaders = ch?.perRequestHeaders(model) ?: emptyMap(),
        )
        return JSONObject()
            .put("accountId", id)
            .put("nickname", acc.nickname)
            .put("modelId", model)
            .put("ok", ok)
            .put("message", if (ok) "正常" else msg.take(160))
    }

    /**
     * 余额（插件做「长期/临时」分池显示的来源）：返回插件期望的 `packages[]`，
     * 前端渲染两套分桶（`credit-expiry.js`），按渠道走哪套由**包名**决定：
     *  - 按到期时间分（buddy/workbuddy/trae/lobsterai）：`splitCreditsByExpiry` 读
     *    `deductionEndTime`，把 <windowDays 的算临时 → 显示「长期 X · 临时 Y」；
     *  - 按池名分（loomy/raccoon）：`findDailyPool` 认包名「每日赠送」/「每日积分」→
     *    显示「永久 X · 每日 Y」。
     * 故支持的渠道合成两个包，**包名按渠道给**（loomy 的池就叫「永久积分」+「每日赠送」，
     * 插件同口径），送达与展示就都对得上。
     */
    private fun creditsBalances(p: JSONObject): JSONObject {
        val appId = toAppProvider(p.optString("provider"))
        val out = JSONArray()
        var windowDays: Int? = null
        val now = System.currentTimeMillis()
        // 池名（按渠道）：loomy/raccoon 走池名分桶，其余走到期时间分桶
        val longName: String
        val ephName: String
        when (appId) {
            "loomy" -> { longName = "永久积分"; ephName = "每日赠送" }
            "raccoon" -> { longName = "奖励积分"; ephName = "每日积分" }
            else -> { longName = "长期"; ephName = "临时" }
        }
        val byExpiry = appId !in setOf("loomy", "raccoon")
        AccountPool.load().filter { it.provider == appId }.forEach { a ->
            // 拉一次余额（网络），顺带刷新分池字段
            val (v, err) = AccountPool.queryCreditsAny(a)
            val fresh = AccountPool.load().firstOrNull { it.id == a.id } ?: a
            val item = JSONObject().put("accountId", a.id).put("nickname", a.nickname)
            if (v < 0) {
                item.put("balance", JSONObject.NULL)
                if (err.isNotEmpty()) item.put("error", err)
            } else {
                val pkgs = JSONArray()
                fun pkg(name: String, remaining: Double, daysFromNow: Long): JSONObject =
                    JSONObject()
                        .put("name", name)
                        .put("unit", "credits")
                        .put("remaining", remaining)
                        .put("total", remaining)
                        .put("used", 0)
                        .put("active", true)
                        .put("cycleStartTime", "").put("cycleEndTime", "").put("expiredTime", "")
                        .put("deductionEndTime", now + daysFromNow * 24 * 3600_000L)
                if (fresh.permanentCredits > 0) pkgs.put(pkg(longName, fresh.permanentCredits, 3650L))
                if (fresh.ephemeralCredits > 0) pkgs.put(pkg(ephName, fresh.ephemeralCredits, 3L))
                item.put("balance", JSONObject()
                    .put("total", v)
                    .put("packages", pkgs)
                    .put("expiredTotal", 0))
                // windowDays 只有按期分桶的四家需要（插件同口径；池名分桶的两家不回）
                if (byExpiry && (fresh.permanentCredits >= 0 || fresh.ephemeralCredits >= 0)) windowDays = 15
            }
            out.put(item)
        }
        val res = JSONObject().put("accounts", out)
        windowDays?.let { res.put("windowDays", it) }
        return res
    }

    // ==================== 签到/续期（10-10 晚批） ====================

    /**
     * 签到结果的插件形状（照 ClaimOutcome 联合类型 + computeClaimSummary）。
     * kind: claimed(带 credit/unit) / already-claimed / inactive / failed(带 code)。
     * app 侧 checkInAny 只回 (Boolean, String)，靠文案归类：
     *  - 成功 → claimed（额度增量 app 侧拿不到，回 0——汇总的「共 +N」由明细行支撑，
     *    每行 message 带上游原话；这比编一个数字诚实）。
     *  - 文案含「已签/已领」→ already-claimed；含「无签到接口/暂不支持」→ inactive；
     *  - 其余 → failed。
     */
    private fun claimOutcome(success: Boolean, msg: String): JSONObject {
        if (success) {
            return JSONObject()
                .put("kind", "claimed").put("credit", 0).put("streakDays", 0).put("isStreakDay", false)
                .put("message", msg)
        }
        val kind = when {
            msg.contains("已签") || msg.contains("已领") -> "already-claimed"
            msg.contains("无签到接口") || msg.contains("暂不支持") -> "inactive"
            else -> "failed"
        }
        val o = JSONObject().put("kind", kind).put("message", msg)
        if (kind == "failed") o.put("code", -1)
        return o
    }

    /** 单渠道一键领取（面板级按钮）：串行签到该渠道全部账号（含停用，照插件口径） */
    private fun creditsClaimAll(p: JSONObject): JSONObject {
        val appId = toAppProvider(p.optString("provider"))
        val list = AccountPool.load().filter { it.provider == appId }
        val results = JSONArray()
        var claimed = 0; var already = 0; var inactive = 0; var failed = 0
        list.forEach { a ->
            // 渠道无签到接口时不再逐账号白打：整体记 inactive（一条汇总，等同插件 inactive 分支）
            val (success, msg) = runCatching { AccountPool.checkInAny(a) }
                .getOrElse { false to (it.message ?: "签到失败") }
            if (msg.contains("无签到接口") || msg.contains("暂不支持")) {
                inactive++
                results.put(JSONObject().put("accountId", a.id).put("nickname", a.nickname)
                    .put("outcome", JSONObject().put("kind", "inactive").put("message", msg)))
            } else {
                val outcome = claimOutcome(success, msg)
                when (outcome.optString("kind")) {
                    "claimed" -> { claimed++; AccountPool.markCheckedIn(a.id) }
                    "already-claimed" -> already++
                    "failed" -> failed++
                }
                results.put(JSONObject().put("accountId", a.id).put("nickname", a.nickname)
                    .put("outcome", outcome))
            }
        }
        val summary = JSONObject()
            .put("claimed", claimed).put("totalCredit", 0.0)
            .put("alreadyClaimed", already).put("inactive", inactive).put("failed", failed)
            .put("coversToday", claimed + already)
            .put("totalByUnit", JSONObject().put("token", 0).put("credit", 0))
        return JSONObject().put("results", results).put("summary", summary)
    }

    /** 单卡签到：签到 + 记账（markCheckedIn 收口，照 AccountCheckinReceiver 口径） */
    private fun accountCheckin(p: JSONObject): JSONObject {
        val id = p.optString("accountId")
        val acc = AccountPool.load().firstOrNull { it.id == id }
            ?: throw IllegalStateException("账号不存在：$id")
        val (success, msg) = runCatching { AccountPool.checkInAny(acc) }
            .getOrElse { false to (it.message ?: "签到失败") }
        if (success) AccountPool.markCheckedIn(id)
        return JSONObject()
            .put("accountId", id).put("nickname", acc.nickname)
            .put("success", success).put("message", msg)
            .put("outcome", claimOutcome(success, msg))
    }

    /** 单卡续期：凭据刷新（过期前手动续；渠道不支持时如实报错） */
    private fun accountRefresh(p: JSONObject): JSONObject {
        val id = p.optString("accountId")
        val acc = AccountPool.load().firstOrNull { it.id == id }
            ?: throw IllegalStateException("账号不存在：$id")
        val (updated, err) = runCatching { AccountPool.refreshAny(acc) }
            .getOrElse { null to (it.message ?: "续期失败") }
        if (updated == null) throw IllegalStateException(err.ifEmpty { "续期失败" })
        return JSONObject()
            .put("accountId", id).put("nickname", acc.nickname)
            .put("expiresAt", if (updated.expiresAt > 0) updated.expiresAt else JSONObject.NULL)
            .put("message", "续期成功")
    }

    // ==================== 工具 ====================

    /** app provider → 插件 provider（CodeBuddy：codebuddy → buddy） */
    private fun toPluginProvider(app: String): String = if (app == "codebuddy") "buddy" else app

    /** 插件 provider → app provider（buddy → codebuddy） */
    private fun toAppProvider(plugin: String): String = if (plugin == "buddy") "codebuddy" else plugin

    /** 合成凭据标识（app 把 token 存在账号里，没有插件那种 credentialRef 命名字符串） */
    private fun synthCredentialRef(a: AccountPool.Account): String {
        val tail = a.id.substringAfterLast('-').takeLast(8).uppercase()
        return "${a.provider.uppercase()}_ACCOUNT_$tail"
    }

    /** List<String> → JSONArray（显式构造，不依赖 org.json 的 Collection 重载） */
    private fun strArray(items: List<String>): JSONArray {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        return arr
    }

    companion object {
        @SuppressLint("SetJavaScriptEnabled")
        fun applySettings(wv: WebView) {
            wv.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                useWideViewPort = true
                loadWithOverviewMode = true
            }
        }

        const val PAGE_URL = "file:///android_asset/jethub/index.html"
    }
}
