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
    ): Pair<Boolean, String> {
        val url = if (baseUrl.endsWith("/chat/completions")) baseUrl
        else baseUrl.trimEnd('/') + "/chat/completions"
        // 强制 stream=true：上游仅收流式；stream_options 带回 usage 供测试口径
        // （Anthropic 族渠道在 patchBody 里自行处理 stream；这里只对无渠道实现者覆写）
        val body = try {
            val o = org.json.JSONObject(bodyJson)
            // 强制 stream=true：上游仅收流式；stream_options 带回 usage 供测试口径
            o.put("stream", true)
            o.put("stream_options", org.json.JSONObject().put("include_usage", true))
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
}
