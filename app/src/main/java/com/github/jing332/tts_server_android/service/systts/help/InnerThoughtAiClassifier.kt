package com.github.jing332.tts_server_android.service.systts.help

import com.github.jing332.tts_server_android.conf.SystemTtsConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
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

    private const val CREDENTIAL_CACHE_MS = 60_000L
    private var cachedCredentials: Triple<String, String, String>? = null
    private var credentialsReadAt = 0L

    /**
     * AI 兜底判定是否心声。
     * @return null 表示未启用/未配置/请求失败/熔断中（调用方应回退正则结果）
     */
    fun classify(text: String): Boolean? {
        if (!SystemTtsConfig.isInnerThoughtAiEnabled.value) return null

        synchronized(cache) { cache[text]?.let { return it } }
        val now = System.currentTimeMillis()
        if (now < circuitOpenUntil) return null

        val credentials = resolveCredentials() ?: return null
        val (baseUrl, apiKey, model) = credentials

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

    /**
     * 凭证解析：只读角色管理插件密钥文件(60s 缓存)，无手动配置。
     * @return Triple(接口地址, Key, 模型名)，无可用凭证返回 null
     */
    private fun resolveCredentials(): Triple<String, String, String>? {
        val now = System.currentTimeMillis()
        if (now - credentialsReadAt < CREDENTIAL_CACHE_MS) return cachedCredentials
        synchronized(this) {
            if (System.currentTimeMillis() - credentialsReadAt < CREDENTIAL_CACHE_MS)
                return cachedCredentials
            cachedCredentials = readFileCredentials()
            credentialsReadAt = System.currentTimeMillis()
            return cachedCredentials
        }
    }

    /** 密钥来源探针(绕过缓存直读文件)，供设置页展示链路状态 */
    fun describeCredentialSource(): String {
        val c = readFileCredentials() ?: return "未找到可用密钥：需先在角色管理插件中添加「接口地址@@模型名@@API Key」格式密钥"
        val masked = if (c.second.length > 8) c.second.take(4) + "****" + c.second.takeLast(4) else "****"
        return "已连接：${c.first}｜模型 ${c.third}｜Key $masked"
    }

    /** 扫描 Download/chajian/*/key_list.json，取第一条 OpenAI 格式(URL@@模型名@@APIKey)密钥 */
    private fun readFileCredentials(): Triple<String, String, String>? {
        return runCatching {
            val root = File("/storage/emulated/0/Download/chajian")
            val dirs = root.listFiles(File::isDirectory) ?: return@runCatching null
            for (dir in dirs) {
                val f = File(dir, "key_list.json")
                if (!f.exists()) continue
                val arr = JSONArray(f.readText())
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONArray(i) ?: continue
                    val value = item.optJSONObject(1)?.optString("value")?.trim() ?: continue
                    val parts = value.split("@@")
                    if (parts.size >= 3 && parts[0].trim().startsWith("http")) {
                        return@runCatching Triple(
                            parts[0].trim().trimEnd('/'),
                            parts.drop(2).joinToString("@@").trim(),
                            parts[1].trim(),
                        )
                    }
                }
            }
            null
        }.getOrNull()
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
