package com.github.jing332.tts_server_android.service.systts.help

import com.github.jing332.tts_server_android.conf.SystemTtsConfig
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal object InnerThoughtAiClassifier {

    private const val CONNECT_TIMEOUT_MS = 2000
    private const val READ_TIMEOUT_MS = 4000
    private const val MAX_CACHE_SIZE = 256
    private const val FAILURE_TRIP_COUNT = 3
    private const val CIRCUIT_OPEN_MS = 60_000L

    private const val SYSTEM_PROMPT =
        "你是小说朗读标注助手。判断用户给出的文本片段是否为小说中人物的内心独白" +
            "（心声、心理活动、心中默念），而不是说出口的对话或旁白叙述。" +
            "只回答 yes 或 no。"

    private val cache = object : LinkedHashMap<String, Boolean>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>): Boolean {
            return size > MAX_CACHE_SIZE
        }
    }

    private var consecutiveFailures = 0
    private var circuitOpenUntil = 0L

    /**
     * AI 兜底判定是否心声。
     * @return null 表示未启用/未配置/请求失败/熔断中（调用方应回退正则结果）
     */
    fun classify(text: String): Boolean? {
        if (!SystemTtsConfig.isInnerThoughtAiEnabled.value) return null
        val baseUrl = SystemTtsConfig.innerThoughtAiUrl.value.trim().trimEnd('/')
        val apiKey = SystemTtsConfig.innerThoughtAiKey.value.trim()
        val model = SystemTtsConfig.innerThoughtAiModel.value.trim()
        if (baseUrl.isEmpty() || model.isEmpty()) return null

        synchronized(cache) { cache[text]?.let { return it } }
        val now = System.currentTimeMillis()
        if (now < circuitOpenUntil) return null

        val answer = request(baseUrl, apiKey, model, text) ?: run {
            if (++consecutiveFailures >= FAILURE_TRIP_COUNT) {
                circuitOpenUntil = System.currentTimeMillis() + CIRCUIT_OPEN_MS
                consecutiveFailures = 0
            }
            return null
        }

        consecutiveFailures = 0
        val result = answer.lowercase().contains("yes")
        synchronized(cache) { cache[text] = result }
        return result
    }

    private fun request(
        baseUrl: String,
        apiKey: String,
        model: String,
        text: String,
    ): String? {
        var conn: HttpURLConnection? = null
        return try {
            val body = JSONObject().apply {
                put("model", model)
                put("temperature", 0)
                put("max_tokens", 16)
                put("stream", false)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", SYSTEM_PROMPT)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", text)
                    })
                })
            }
            conn = URL("$baseUrl/chat/completions").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            if (apiKey.isNotEmpty())
                conn.setRequestProperty("Authorization", "Bearer $apiKey")

            conn.outputStream.use {
                it.write(body.toString().toByteArray(Charsets.UTF_8))
            }
            if (conn.responseCode !in 200..299) return null
            val resp = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            JSONObject(resp)
                .getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").optString("content", "")
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
