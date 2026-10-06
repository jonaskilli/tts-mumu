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
    ): Pair<Boolean, String> {
        val url = if (baseUrl.endsWith("/chat/completions")) baseUrl
        else baseUrl.trimEnd('/') + "/chat/completions"
        // 强制 stream=true：上游仅收流式；stream_options 带回 usage 供测试口径
        val body = try {
            val o = org.json.JSONObject(bodyJson)
            o.put("stream", true)
            o.put("stream_options", org.json.JSONObject().put("include_usage", true))
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
}
