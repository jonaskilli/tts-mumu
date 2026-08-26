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

    // 与"多角色朗读2.87"defaultConfig 对齐：裸Key时兜底智谱端点(基址不带/chat/completions)
    private const val MIYUE_ENDPOINT_BASE = "https://open.bigmodel.cn/api/paas/v4"
    private const val MIYUE_DEFAULT_MODEL = "glm-4-flash"

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
        val c = readFileCredentials()
            ?: return "未找到可用密钥：miyue.txt 未读取到。请在角色管理插件选择「当前密钥」或直接配置 Download/chajian/<插件目录>/miyue.txt"
        val masked = if (c.second.length > 8) c.second.take(4) + "****" + c.second.takeLast(4) else "****"
        return "已连接：${c.first}｜模型 ${c.third}｜Key $masked"
    }

    /** 密钥读取：只读 miyue.txt（角色管理「当前密钥」由插件写入此文件，多角色朗读同源） */
    private fun readFileCredentials(): Triple<String, String, String>? = readMiyueTxt()

    /**
     * 主力且唯一密钥源：读多角色朗读(2.87)/角色管理共用的 miyue.txt。
     * 角色管理选「当前密钥」即写入此文件；格式与该插件 loadKeyFile 对齐：
     * ## 分场景取前段(姓名分析)；无@@整段为裸Key；有@@按每3个一组流式解析，
     * Key 空的组跳过；端点归一化后回填基址（请求时再拼 /chat/completions）；
     * 裸Key沿用其 defaultConfig（智谱+glm-4-flash）兜底。
     */
    private fun readMiyueTxt(): Triple<String, String, String>? {
        return runCatching {
            val root = File("/storage/emulated/0/Download/chajian")
            val dirs = root.listFiles(File::isDirectory) ?: return@runCatching null
            for (dir in dirs) {
                val f = File(dir, "miyue.txt")
                if (!f.exists()) continue
                val content = f.readText().trim()
                if (content.isEmpty()) continue
                val scene = if (content.contains("##")) content.split("##")[0].trim() else content
                val parsed = parseMiyueGroup(scene)
                if (parsed != null) return@runCatching parsed
            }
            null
        }.getOrNull()
    }

    /** 按 2.87 parseSingleGroup 语义解析单场景内容 */
    private fun parseMiyueGroup(content: String): Triple<String, String, String>? {
        val c = content.trim()
        if (c.isEmpty()) return null
        if (!c.contains("@@")) {
            return Triple(MIYUE_ENDPOINT_BASE, MIYUE_DEFAULT_MODEL, c)
        }
        val arr = c.split("@@")
        var i = 0
        while (i < arr.size) {
            var endpoint = arr[i].trim()
            val model = arr.getOrNull(i + 1)?.trim().orEmpty()
            val key = arr.getOrNull(i + 2)?.trim().orEmpty()
            if (key.isNotEmpty()) {
                if (endpoint.endsWith("/chat/completions"))
                    endpoint = endpoint.removeSuffix("/chat/completions")
                endpoint = endpoint.trimEnd('/')
                return Triple(endpoint.ifEmpty { MIYUE_ENDPOINT_BASE }, model.ifEmpty { MIYUE_DEFAULT_MODEL }, key)
            }
            i += 3
        }
        return null
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
