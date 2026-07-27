package com.github.jing332.deepseekproxy.proxy

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64
import java.util.concurrent.TimeUnit
import android.util.Log

/**
 * DeepSeek（chat.deepseek.com）网页端接口客户端。
 *
 * 与 Doubao/Kimi 客户端风格一致：纯 OkHttp，无 wasm/JS 引擎。
 * 聊天需先完成 PoW（见 [DeepSeekPow]），再把答案塞进 `x-ds-pow-response` 头。
 *
 * 鉴权：Authorization: Bearer <token>（网页登录后的 token，非 Cookie）。
 */
object DeepSeekClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    private const val BASE = "https://chat.deepseek.com"
    const val UA = "Mozilla/5.0 (Linux; Android 15; V2352A Build/AP3A.240905.015.A2) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.6478.71 Mobile Safari/537.36"

    /** Bearer token，由 ProxyServer 在每次请求前注入（来自 UI 设置的凭证）。 */
    var token: String = ""

    private fun newRequestBuilder(path: String): Request.Builder {
        return Request.Builder().url(BASE + path)
            .header("authorization", token)
            .header("content-type", "application/json")
            .header("user-agent", UA)
            .header("origin", BASE)
            .header("referer", BASE + "/")
            .header("x-client-version", "2.2.0")
            .header("x-client-platform", "web")
            .header("x-client-locale", "zh_CN")
            .header("x-client-bundle-id", "com.deepseek.chat")
            .header("accept", "*/*")
    }

    private fun postJson(path: String, body: JSONObject): JSONObject {
        val req = newRequestBuilder(path)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                throw RuntimeException("DeepSeek $path HTTP ${resp.code}: ${text.take(300)}")
            }
            return JSONObject(text)
        }
    }

    fun createSession(): String {
        val d = postJson("/api/v0/chat_session/create", JSONObject())
        return d.getJSONObject("data").getJSONObject("biz_data")
            .getJSONObject("chat_session").getString("id")
    }

    /** 删除已完成的 DeepSeek 会话（静默执行，不阻塞）。回复完毕后调用，避免会话堆积。 */
    fun deleteSession(sessionId: String) {
        if (sessionId.isBlank()) return
        try {
            postJson(
                "/api/v0/chat_session/delete",
                JSONObject().put("chat_session_id", sessionId)
            )
        } catch (_: Exception) {
            // 删除失败不影响主流程，静默忽略
        }
    }

    data class PowChallenge(
        val algorithm: String,
        val challenge: String,
        val salt: String,
        val signature: String,
        val difficulty: Int,
        val expireAt: Long,
        val targetPath: String
    )

    fun createPowChallenge(targetPath: String): PowChallenge {
        val d = postJson(
            "/api/v0/chat/create_pow_challenge",
            JSONObject().put("target_path", targetPath)
        )
        val c = d.getJSONObject("data").getJSONObject("biz_data").getJSONObject("challenge")
        return PowChallenge(
            c.getString("algorithm"),
            c.getString("challenge"),
            c.getString("salt"),
            c.getString("signature"),
            c.getInt("difficulty"),
            c.getLong("expire_at"),
            c.getString("target_path")
        )
    }

    fun buildPowHeader(ch: PowChallenge, answer: Long, targetPath: String): String {
        val payload = JSONObject().apply {
            put("algorithm", ch.algorithm)
            put("challenge", ch.challenge)
            put("salt", ch.salt)
            put("answer", answer)
            put("signature", ch.signature)
            put("target_path", targetPath)
        }
        return Base64.getEncoder().encodeToString(payload.toString().toByteArray(Charsets.UTF_8))
    }

    /**
     * 调用 /api/v0/chat/completion（流式 SSE）。返回的 Response 由调用方读取并关闭。
     * 若非 SSE（如鉴权失败），会读取并抛出异常。
     */
    fun chatCompletion(
        sessionId: String, prompt: String, powHeader: String, modelType: String = "default"
    ): Response {
        val body = JSONObject().apply {
            put("chat_session_id", sessionId)
            put("parent_message_id", JSONObject.NULL)
            put("model_type", modelType)
            put("prompt", prompt)
            put("ref_file_ids", JSONArray())
            put("thinking_enabled", false)
            put("search_enabled", false)
            put("action", JSONObject.NULL)
            put("preempt", false)
        }
        val req = newRequestBuilder("/api/v0/chat/completion")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .header("x-ds-pow-response", powHeader)
            .header("accept", "text/event-stream")
            .build()
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) {
            val err = resp.body?.string() ?: ""
            resp.close()
            throw RuntimeException("DeepSeek /chat/completion HTTP ${resp.code}: ${err.take(300)}")
        }
        val ct = resp.header("Content-Type") ?: ""
        if (!ct.contains("text/event-stream", ignoreCase = true)) {
            val bodyText = resp.body?.string() ?: ""
            resp.close()
            throw RuntimeException("DeepSeek /chat/completion 响应异常(content-type=$ct): ${bodyText.take(300)}")
        }
        return resp
    }

    /**
     * 解析 DeepSeek SSE，只回调「模型真正的回复文本」。
     *
     * 真实线格式（同一端点 /api/v0/chat/completion，与 deepseek-chat-toolkit 的 py 参考一致）：
     *   每一行 `data:` 是一个独立 JSON 对象：
     *     - 元数据事件 {"response":{... fragments:[{content:"开头几个字"}] ...}}：
     *         开头那几个字可能在此（兼容多种字段位置），拼接为前缀，在首个正文前补上；
     *     - 增量文本事件 {"v":"..."}：v 字段即本段回复文本 → 立即回调（流式）；
     *     - 遥测事件 {"p":"...","v":"FINISHED"}：带 p 字段，是遥测参数，其 v 不是正文，跳过；
     *     - 遥测数组 [{"p":...}] 是 JSONArray，JSONObject 解析抛异常 → 忽略；
     *     - "FINISHED" 不是合法 JSON → 忽略。
     *   若整段是 blob（含 '!' 分隔符、无 v 流），则在循环结束后走 blob 回退提取。
     *
     * 调试：每行原始 SSE 写入 /sdcard/Download/deepseek_sse.txt 并打印 Log.d("DeepSeekSSE", ...)
     */
    suspend fun parseSse(src: okio.BufferedSource, onDelta: suspend (String) -> Unit) {
        try {
            java.io.File("/sdcard/Download/deepseek_sse.txt").writeText(
                "=== parseSse @ ${System.currentTimeMillis()} ===\n"
            )
        } catch (_: Exception) {
        }
        var prefix = ""
        var prefixEmitted = false
        var gotV = false
        val rawForBlob = mutableListOf<String>()
        suspend fun emitPrefix() {
            if (!prefixEmitted && prefix.isNotEmpty()) {
                onDelta(prefix)
                prefixEmitted = true
            }
        }

        while (true) {
            val line = src.readUtf8Line() ?: break
            logRaw(line)
            val t = line.trimStart()
            if (!t.startsWith("data:")) continue
            val data = t.substring(5).trim()
            if (data.isEmpty() || data.startsWith("[") || data == "FINISHED") continue
            try {
                val obj = JSONObject(data)
                val vVal = obj.opt("v")
                when {
                    // 带 p 的补丁/遥测优先：fragments content 的 APPEND 并入前缀，其余（含 FINISHED）跳过
                    obj.has("p") -> {
                        val p = obj.optString("p", "")
                        if (p.endsWith("/content") && obj.optString("o", "") == "APPEND") {
                            val pv = obj.optString("v", "")
                            if (pv.isNotEmpty()) prefix += pv
                        }
                    }
                    // 顶层 {"response":{...}}（无 v）：取 fragments 前缀
                    obj.has("response") -> {
                        prefix += extractOpening(obj)
                    }
                    // v 是字符串：真正的增量正文
                    vVal is String -> {
                        gotV = true
                        val v = vVal
                        // 去重：首个正文已包含前缀则不重复补（避免双字），否则先补前缀
                        if (!prefixEmitted && prefix.isNotEmpty() && v.startsWith(prefix)) {
                            prefixEmitted = true
                        } else {
                            emitPrefix()
                        }
                        onDelta(v)
                    }
                    // v 是对象（如 {"response":{...}} 开头 metadata）：只取 fragments 前缀，不要整段 JSON
                    vVal is JSONObject && vVal.has("response") -> {
                        prefix += extractOpening(vVal)
                    }
                }
            } catch (_: Exception) {
                // 非 JSON（如 blob 格式含 '!' 分隔）→ 仅在未遇到 v 流时收集用于回退
                if (!gotV) rawForBlob.add(data)
            }
        }
        // 未遇到任何 v 流：尝试 blob（整块）格式回退
        if (!gotV) {
            val blob = extractBlobText(rawForBlob.joinToString("\n"))
            if (blob.isNotEmpty()) onDelta(blob)
        }
    }

    /** 诊断用：原始行写文件 + Logcat（失败静默）。 */
    private fun logRaw(line: String) {
        try {
            java.io.File("/sdcard/Download/deepseek_sse.txt").appendText(line + "\n")
        } catch (_: Exception) {
        }
        try {
            Log.d("DeepSeekSSE", line)
        } catch (_: Exception) {
        }
    }

    /**
     * 从 {"response":{...}} 的 fragments 提取开头片段文本。
     * 只取 fragments[].content / fragments[].text（开头的字/几个字），
     * 不取 response.content 等整段字段，避免把前面一整段都显示出来。
     * 没有就返回空，按 v 流正常处理（无前缀）。
     */
    private fun extractOpening(respObj: JSONObject): String {
        return try {
            val resp = respObj.optJSONObject("response") ?: return ""
            val frags = resp.optJSONArray("fragments") ?: return ""
            val sb = StringBuilder()
            for (i in 0 until frags.length()) {
                val f = frags.optJSONObject(i) ?: continue
                val c = f.optString("content", "")
                sb.append(if (c.isNotEmpty()) c else f.optString("text", ""))
            }
            sb.toString()
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * blob（整块）格式回退：用户要求「只取 content 字段」。
     * 从 JSON metadata 提取主 content（排除 fragments 内部那几个开头字），
     * 不再返回 '!' 之后整段 body，避免把前面所有内容都返回。
     */
    private fun extractBlobText(raw: String): String {
        return extractPrimaryContent(raw).trimEnd()
    }

    /**
     * 提取主 content 字段（response.content / 顶层 content），排除 fragments 块内的 content。
     */
    private fun extractPrimaryContent(raw: String): String {
        return try {
            // 先把 fragments 数组清空，避免只匹配到 fragments 里那几个开头字
            val noFrag = Regex("\"fragments\"\\s*:\\s*\\[.*?\\]", RegexOption.DOT_MATCHES_ALL)
                .replace(raw, "\"fragments\":[]")
            val m = Regex("\"content\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").find(noFrag)
            if (m != null) return unescapeJson(m.groupValues[1])
            ""
        } catch (_: Exception) {
            ""
        }
    }

    /** 还原 JSON 字符串转义。 */
    private fun unescapeJson(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (s[i + 1]) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    'r' -> sb.append('\r')
                    else -> sb.append(s[i + 1])
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }
}
