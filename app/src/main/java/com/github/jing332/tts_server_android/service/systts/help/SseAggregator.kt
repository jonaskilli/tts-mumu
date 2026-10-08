package com.github.jing332.tts_server_android.service.systts.help

import java.io.BufferedReader
import java.net.HttpURLConnection

/**
 * SSE 聚合桥（10-06 账号池配套）：CodeBuddy 上游只收流式（非流式 code 11101 被拒），
 * 而密钥测试/心声分类/朗读链三处都按"非流式 OpenAI 响应"解析——本桥发 stream=true 的
 * 请求后把 SSE data: 帧里的 delta.content 增量拼成完整文本，再包回标准
 * {choices:[{message:{content}}]} JSON 字符串，让三处原解析逻辑零改动。
 *
 * （与规则 JS 侧 firstSentence 同思路的 app 侧实现，协议底数见 AccountPool 头注）
 */
object SseAggregator {

    /** 立即取消判定：调用方返回 true 时中止读取（朗读链停止/超时） */
    fun interface Cancelled {
        fun isCancelled(): Boolean
    }

    /**
     * 发起流式对话请求并聚合成非流式 JSON。
     * @param baseUrl 上游根（如 https://copilot.tencent.com/v2）
     * @param apiKey Bearer 令牌
     * @param bodyJson 请求体 JSON（原样发送；本桥强制覆写 stream=true）
     * @param cancelled 取消钩子（可传 { false }）
     * @return (成功?, 非流式格式 JSON 或错误信息)；HTTP 失败/解析失败都走第二参
     *
     * 半截内容纪律（移植自 dsh-phone buddy-adapter「已有流输出后不换号不重放」）：
     * 流异常中断（未收到 finish_reason/[DONE]）但已聚合出非空 content 时，返回成功
     * JSON 且 finish_reason="length"（OpenAI 标准截断记号）——绝不冒充 "stop"，也
     * 绝不报失败（失败会让 chatCompletionWithPool 换号重放整轮请求）。
     */
    fun chatCompletion(
        baseUrl: String,
        apiKey: String,
        bodyJson: String,
        cancelled: Cancelled = Cancelled { false },
        channel: ChatChannel? = null,
        model: String = "",
        extraHeaders: Map<String, String> = emptyMap(),
    ): Pair<Boolean, String> {
        val url = if (baseUrl.endsWith("/chat/completions")) baseUrl
        else baseUrl.trimEnd('/') + "/chat/completions"
        // 强制 stream=true：上游仅收流式；stream_options 带回 usage 供测试口径
        // （Anthropic 族渠道在 patchBody 里自行处理 stream；这里只对无渠道实现者覆写）
        val body = try {
            val o = org.json.JSONObject(bodyJson)
            if (channel == null) {
                o.put("stream", true)
                o.put("stream_options", org.json.JSONObject().put("include_usage", true))
            }
            o.toString()
        } catch (e: Exception) {
            return false to "请求体不是合法 JSON：${e.message}"
        }

        var conn: HttpURLConnection? = null
        // 提升到 try 外：catch 的半截内容兜底要读它们（流中断时已有聚合内容的判据）
        val content = StringBuilder()
        var usageJson: String? = null
        var model = ""
        return try {
            conn = (java.net.URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 120_000 // 流式首包可能慢（排队/思考），正文按帧到达不会真等满
                setRequestProperty("Accept", "text/event-stream")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Authorization", "Bearer $apiKey")
                // 渠道头族优先（10-09 全渠道）：各自的身份头族/双认证头/指纹头；
                // 无渠道实现 = CodeBuddy 对话头族（10-08 接线，权威形状单点维护）
                if (channel != null) {
                    channel.chatHeaders(apiKey).forEach { (k, v) -> setRequestProperty(k, v) }
                    extraHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
                } else {
                    AccountPool.chatHeaders().forEach { (k, v) -> setRequestProperty(k, v) }
                }
                doOutput = true
                outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            if (code < 200 || code >= 300) {
                val err = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                return false to "HTTP $code：${err.take(180).ifEmpty { "无响应内容" }}"
            }
            var finishReason: String? = null
            // 半截内容判据：流是否正常收尾（[DONE] 或读到 finish_reason）。
            // 未收尾 + content 非空 = 半截内容，见上方纪律注释。
            var streamEnded = false
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                while (true) {
                    if (cancelled.isCancelled()) return false to "已取消"
                    val line = reader.readLine() ?: break
                    if (!line.startsWith("data:")) continue
                    val payload = line.removePrefix("data:").trim()
                    if (payload.isEmpty()) continue
                    if (payload == "[DONE]") { streamEnded = true; break }
                    try {
                        val o = org.json.JSONObject(payload)
                        if (o.optString("model").isNotEmpty()) model = o.optString("model")
                        val choices = o.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val c = choices.optJSONObject(0) ?: continue
                            // delta（流式）优先，message 兜底（上游某些网关会把聚合帧伪装成 message）
                            val delta = c.optJSONObject("delta") ?: c.optJSONObject("message")
                            delta?.optString("content")?.let { if (it.isNotEmpty()) content.append(it) }
                            c.optString("finish_reason").takeIf { it.isNotEmpty() }?.let { finishReason = it; streamEnded = true }
                        }
                        o.optJSONObject("usage")?.let { usageJson = it.toString() }
                    } catch (e: Exception) {
                        // 单帧坏数据跳过，不废整流
                    }
                }
            }
            if (content.isEmpty()) return false to "流式响应无内容（finish=$finishReason）"

            // 半截内容：已向调用方聚合出非空 content，但流没正常收尾（既无 [DONE] 也无
            // finish_reason）——dsh-phone buddy-adapter 纪律：绝不报失败让轮换循环换号重放
            // （重放会产出两段拼接的回答），把已有内容按截断结果返回。finish_reason 用
            // OpenAI 标准截断记号 "length"，不冒充 "stop"，调用方可据此区分完整/截断。
            if (!streamEnded) finishReason = "length"

            // 包回非流式标准形状，调用方原解析零改动
            val out = org.json.JSONObject()
            out.put("model", model)
            out.put("choices", org.json.JSONArray().put(org.json.JSONObject().apply {
                put("message", org.json.JSONObject().put("role", "assistant").put("content", content.toString()))
                put("finish_reason", finishReason ?: "stop")
            }))
            usageJson?.let { out.put("usage", org.json.JSONObject(it)) }
            true to out.toString()
        } catch (e: Exception) {
            // 半截内容纪律同款：流中途异常（网络断流等）但已聚合出非空 content 时，
            // 绝不报失败——失败会触发账号池换号重放（前半 A 账号 + 后半 B 账号的拼接
            // 回答）。把已聚合内容包成截断结果（finish_reason="length"）返回。这是
            // 网络断流不是账号的错，走成功路径也天然不给该账号记限流标记。
            if (content.isNotEmpty()) {
                val out = org.json.JSONObject()
                out.put("model", model)
                out.put("choices", org.json.JSONArray().put(org.json.JSONObject().apply {
                    put("message", org.json.JSONObject().put("role", "assistant").put("content", content.toString()))
                    put("finish_reason", "length")
                }))
                usageJson?.let { out.put("usage", org.json.JSONObject(it)) }
                return true to out.toString()
            }
            false to (e.message ?: e.toString())
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * 带账号池轮换的对话（10-08 移植插件账号池语义，照 buddy-adapter 换号循环）：
     *  - key 命中账号池 → 该账号首发；429/限流体=记限流标记+换下一启用账号（同模型过滤）；
     *    401/403=现场续期一次→重试→仍失败换号；全部试完才报错。
     *  - 11140 安全策略拦截（HTTP 403 + code 11140，账号级）：30 分钟冷却后换号。
     *  - key 不在账号池（普通第三方密钥）→ 单发，行为与 chatCompletion 完全一致。
     * 限流解析照插件 llm-adapter.parseRateLimitError：msg 里「将在 … 重置」取真实时刻，
     * 解析不出兜底 1h（RATE_LIMIT_FALLBACK_MS）。
     */
    fun chatCompletionWithPool(
        baseUrl: String,
        apiKey: String,
        bodyJson: String,
        model: String,
        cancelled: Cancelled = Cancelled { false },
    ): Pair<Boolean, String> {
        val pool = AccountPool.load()
        val first = pool.firstOrNull { it.accessToken == apiKey }
        if (first == null) return chatCompletion(baseUrl, apiKey, bodyJson, cancelled)
        // 渠道路由（10-09 全渠道批）：非 codebuddy 渠道走 ChatChannel 头族/请求体变换；
        // 状态机（限流标记/续期/换号）渠道无关共用
        ChannelBootstrap.install() // 幂等；进程内首调装配注册表
        val channel = ChatChannels.byProvider(first.provider)
        val effectiveBody = channel?.patchBody(bodyJson, model) ?: bodyJson
        // 渠道自管对话分叉（10-10 qoder WASM）：加密端点 URL/鉴权形状与通用流完全不同，
        // chatViaChannel 非 null 即整条对话由渠道自己完成；失败复用同一套
        // classifyError 状态机（限流标记/续期/换号），见 chatViaPoolWithRotation。
        if (channel != null && channel.chatViaChannel(first, effectiveBody, model, cancelled) != null) {
            return chatViaPoolWithRotation(channel, first, effectiveBody, model, cancelled)
        }

        val tried = mutableSetOf<String>()
        // 显式非空类型：refresh()/pickAccount() 都返回可空对，解构赋值会把 var 推成可空
        var current: AccountPool.Account = first
        var currentKey: String = apiKey
        var sawAuthFail = false
        var lastErr = ""
        while (true) {
            // ⚠️ 半截内容纪律（移植自 dsh-phone buddy-adapter.js:410「已吐了一半再重放」/
            // :1662「本轮不重发」）：轮换只允许发生在**零内容失败**上。chatCompletion 保证
            // 已聚合出非空 content 后的中断/异常一律按截断结果返回 ok=true，走不到这里；
            // 因此本轮请求若已向调用方吐出过可见内容，绝不会换号重发——换号重放会产生
            // 两段拼接（前半 A 账号 + 后半 B 账号）甚至重复播报的回答，正确行为是保留
            // 已有内容/原错误返回给调用方。
            // 按-请求注头（10-08 钩子落地）：渠道需要 model 的头族（如 AutoClaw
            // X-Request-Model）在这里生成，随 extraHeaders 合并进每次请求
            val (ok, body) = chatCompletion(baseUrl, currentKey, effectiveBody, cancelled, channel, model,
                extraHeaders = channel?.perRequestHeaders(model) ?: emptyMap())

            if (ok) return true to body

            // 错误分类：渠道实现优先（各自限流语义：流内错误帧/业务码/402 额度…），
            // 无渠道实现回退 codebuddy 内联分类（11140/频率限制/HTTP 状态码）
            val m = Regex("HTTP (\\d{3})").find(body)
            val status = m?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val errClass = channel?.classifyError(status, body)
            val isPolicyBlock = body.contains("11140")
            val rateLimited = errClass == ChatChannel.ErrClass.RATE_LIMIT ||
                (errClass == null && (status == 429 || body.contains("频率限制")
                    || body.contains("reset at", true) || isPolicyBlock))
            val authFailed = errClass == ChatChannel.ErrClass.AUTH ||
                (errClass == null && (status == 401 || status == 403))

            if (rateLimited) {
                // 限流/拦截标记（插件同款：11140 与认证处理**都走**——先记冷却再换号，
                // 否则下轮它还是第一候选每轮重撞）。解禁时刻：服务端给了用真实值；
                // 渠道有专属兜底（zcode/codearts UTC+8 日切）用渠道值；否则 1h；11140 用 30min。
                val resetAt = parseResetTime(body)
                    ?: channel?.let { System.currentTimeMillis() + it.rateLimitFallbackMs }
                    ?: System.currentTimeMillis() + if (isPolicyBlock) 30 * 60_000L else 3_600_000L
                AccountPool.markRateLimited(current.id, model, resetAt)
            }
            if (authFailed) {
                sawAuthFail = true
                // 401/403：现场续期一次再试同账号（插件：刷新→重试→换号）。
                // 渠道实现优先（cline 驼峰 body/minimax form/zcode 无续期…），codebuddy 走 AccountPool
                if (current.refreshToken.isNotEmpty()) {
                    val refreshed = channel?.refresh(current)?.let { Triple(it.first, it.second, it.third) }
                        ?: AccountPool.refresh(current).let { (a, _) -> a?.let { Triple(it.accessToken, it.refreshToken, it.expiresAt) } }
                    if (refreshed != null) {
                        val (newAccess, newRefresh, newExpires) = refreshed
                        // 渠道凭据续期成功后落盘（AccountPool.refresh 内部落；渠道层在这里落）
                        if (channel != null) {
                            AccountPool.saveRefreshed(current.id, newAccess, newRefresh, newExpires)
                        }
                        val (ok2, body2) = chatCompletion(baseUrl, newAccess, effectiveBody, cancelled, channel, model,
                            extraHeaders = channel?.perRequestHeaders(model) ?: emptyMap())
                        if (ok2) return true to body2
                        val status2 = Regex("HTTP (\\d{3})").find(body2)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                        lastErr = body2
                        if (status2 != 401 && status2 != 403 && errClass != ChatChannel.ErrClass.AUTH) {
                            // 续期后变成别的错误（限流/参数）→ 换成续期后的凭据，回循环顶统一分类
                            tried.add(current.id) // 防死循环：ref 与原账号同 id
                            current = current.copy(accessToken = newAccess, refreshToken = newRefresh, expiresAt = newExpires)
                            currentKey = newAccess
                            continue
                        }
                    }
                }
            }
            tried.add(current.id)
            lastErr = if (lastErr.isEmpty()) body else lastErr

            // 换号：同 provider（10-09 实锤补站隔离）、启用中、未试过、该模型不限流、未过期
            val next = AccountPool.pickAccount(model, current.provider, tried)
            if (next == null) {
                val head = if (rateLimited) "该模型所有账号均受限" else if (sawAuthFail) "所有账号均被拒绝" else "所有账号均失败"
                return false to "$head（已试 ${tried.size} 个账号）：${lastErr.take(180)}"
            }
            AccountPool.logLine("对话失败（${lastErr.take(60)}），换账号「${next.nickname}」重试")
            current = next
            currentKey = next.accessToken
        }
    }
    /**
     * 渠道自管对话的轮换状态机（10-10 qoder WASM）：请求体由渠道 chatViaChannel 生成
     * 与发送，本函数只负责失败分类后的状态机动作（限流标记/现场续期/换号重试），
     * 与 chatCompletionWithPool 的通用循环同语义（半截内容纪律由渠道层保证：
     * chatViaChannel 有内容即 ok=true，永不换号重放）。
     */
    private fun chatViaPoolWithRotation(
        channel: ChatChannel,
        first: AccountPool.Account,
        bodyJson: String,
        model: String,
        cancelled: Cancelled,
    ): Pair<Boolean, String> {
        val tried = mutableSetOf<String>()
        var current: AccountPool.Account = first
        var sawAuthFail = false
        var lastErr = ""
        while (true) {
            // 显式非空 Pair：chatViaChannel 声明返回可空，解构须先断言（编译器要求）
            val callResult: Pair<Boolean, String> = channel.chatViaChannel(current, bodyJson, model, cancelled)
                ?: return false to "渠道对话路径意外返回空（chatViaChannel 契约破坏）"
            val (ok, body) = callResult
            if (ok) return true to body

            // 错误分类（渠道实现；与通用循环同一套判据）
            val status = Regex("HTTP (\\d{3})").find(body)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val errClass = channel.classifyError(status, body)
            val rateLimited = errClass == ChatChannel.ErrClass.RATE_LIMIT
            val authFailed = errClass == ChatChannel.ErrClass.AUTH

            if (rateLimited) {
                val resetAt = parseResetTime(body)
                    ?: (System.currentTimeMillis() + channel.rateLimitFallbackMs)
                AccountPool.markRateLimited(current.id, model, resetAt)
            }
            if (authFailed) {
                sawAuthFail = true
                // 401/403(105 auth_error)：现场续期一次再试同账号（唯一该走续期的情形）
                if (current.refreshToken.isNotEmpty()) {
                    channel.refresh(current)?.let { r ->
                        AccountPool.saveRefreshed(current.id, r.first, r.second, r.third)
                        val refreshed = current.copy(accessToken = r.first, refreshToken = r.second, expiresAt = r.third)
                        // 显式非空 Pair（同上，解构前断言）
                        val retry: Pair<Boolean, String> = channel.chatViaChannel(refreshed, bodyJson, model, cancelled)
                            ?: return false to "渠道对话路径意外返回空（chatViaChannel 契约破坏）"
                        val (ok2, body2) = retry
                        if (ok2) return true to body2
                        val status2 = Regex("HTTP (\\d{3})").find(body2)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                        lastErr = body2
                        if (status2 != 401 && status2 != 403 && errClass != ChatChannel.ErrClass.AUTH) {
                            tried.add(current.id)
                            current = refreshed
                            continue
                        }
                    }
                }
            }
            tried.add(current.id)
            if (lastErr.isEmpty()) lastErr = body

            // 换号：同 provider、启用中、未试过、该模型不限流、未过期
            val next = AccountPool.pickAccount(model, current.provider, tried)
            if (next == null) {
                val head = if (rateLimited) "该模型所有账号均受限" else if (sawAuthFail) "所有账号均被拒绝" else "所有账号均失败"
                return false to "$head（已试 ${tried.size} 个账号）：${lastErr.take(180)}"
            }
            AccountPool.logLine("对话失败（${lastErr.take(60)}），换账号「${next.nickname}」重试")
            current = next
        }
    }

    /** 从限流文案抠重置时刻（照插件 RESET_TIME_PATTERN：`将在 2026-09-11 18:08:17 UTC+8 重置`） */
    private fun parseResetTime(body: String): Long? {
        val m = Regex("(?:将在|reset at)\\s+([\\d-]+)\\s+([\\d:]+)\\s+(UTC[+-]\\d{1,2}(?::\\d{1,2})?)", RegexOption.IGNORE_CASE)
            .find(body) ?: return null
        return runCatching {
            val zone = java.time.ZoneId.of(m.groupValues[3].replace("UTC", "GMT"))
            val date = m.groupValues[1].split("-").map { it.toInt() }
            val time = m.groupValues[2].split(":").map { it.toInt() }
            java.time.LocalDateTime.of(date[0], date[1], date[2], time[0], time[1], time.getOrElse(2) { 0 })
                .atZone(zone).toInstant().toEpochMilli()
        }.getOrNull()
    }
}
