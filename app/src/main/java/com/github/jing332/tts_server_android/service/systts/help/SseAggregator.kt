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
            val content = StringBuilder()
            var usageJson: String? = null
            var model = ""
            var finishReason: String? = null
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                while (true) {
                    if (cancelled.isCancelled()) return false to "已取消"
                    val line = reader.readLine() ?: break
                    if (!line.startsWith("data:")) continue
                    val payload = line.removePrefix("data:").trim()
                    if (payload.isEmpty()) continue
                    if (payload == "[DONE]") break
                    try {
                        val o = org.json.JSONObject(payload)
                        if (o.optString("model").isNotEmpty()) model = o.optString("model")
                        val choices = o.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val c = choices.optJSONObject(0) ?: continue
                            // delta（流式）优先，message 兜底（上游某些网关会把聚合帧伪装成 message）
                            val delta = c.optJSONObject("delta") ?: c.optJSONObject("message")
                            delta?.optString("content")?.let { if (it.isNotEmpty()) content.append(it) }
                            c.optString("finish_reason").takeIf { it.isNotEmpty() }?.let { finishReason = it }
                        }
                        o.optJSONObject("usage")?.let { usageJson = it.toString() }
                    } catch (e: Exception) {
                        // 单帧坏数据跳过，不废整流
                    }
                }
            }
            if (content.isEmpty()) return false to "流式响应无内容（finish=$finishReason）"

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

        val tried = mutableSetOf<String>()
        // 显式非空类型：refresh()/pickAccount() 都返回可空对，解构赋值会把 var 推成可空
        var current: AccountPool.Account = first
        var currentKey: String = apiKey
        var sawAuthFail = false
        var lastErr = ""
        while (true) {
            val (ok, body) = chatCompletion(baseUrl, currentKey, effectiveBody, cancelled, channel, model)

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
                        val (ok2, body2) = chatCompletion(baseUrl, newAccess, effectiveBody, cancelled, channel, model)
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

            // 换号：启用中、未试过、该模型不限流、未过期
            val next = AccountPool.pickAccount(model, tried)
            if (next == null) {
                val head = if (rateLimited) "该模型所有账号均受限" else if (sawAuthFail) "所有账号均被拒绝" else "所有账号均失败"
                return false to "$head（已试 ${tried.size} 个账号）：${lastErr.take(180)}"
            }
            AccountPool.logLine("对话失败（${lastErr.take(60)}），换账号「${next.nickname}」重试")
            current = next
            currentKey = next.accessToken
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
