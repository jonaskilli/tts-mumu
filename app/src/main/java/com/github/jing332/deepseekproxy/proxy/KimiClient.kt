package com.github.jing332.deepseekproxy.proxy

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Kimi（kimi.com）接口客户端。
 * 协议参考上传的 kimi2api.js：
 *   - 聊天：POST https://www.kimi.com/apiv2/kimi.gateway.chat.v1.ChatService/Chat
 *   - 采用 Connect 二进制帧协议： [1字节 flag] + [4字节大端长度] + [UTF-8 JSON payload]
 *   - 鉴权使用 Authorization: Bearer <token>（即网页登录后的 JWT，非 Cookie）
 *   - 响应同样是 Connect 二进制帧流，逐帧解析 block.text.content 得到回复文本
 *
 * 注意：kimi2api.js 内硬编码了一个 JWT（KIMI_AUTH），该 token 会过期。
 * 此处保留为兜底默认值（仅当未提供 Kimi Token 时使用），建议从浏览器提取后替换。
 */
object KimiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    private const val HOST = "www.kimi.com"
    private const val CHAT_PATH = "/apiv2/kimi.gateway.chat.v1.ChatService/Chat"
    private const val DELETE_PATH = "/apiv2/kimi.gateway.chat.v1.ChatService/DeleteChat"
    private const val UA =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    /** kimi2api.js 内硬编码的兜底 JWT（会过期，仅作缺省值）。 */
    private const val FALLBACK_AUTH =
        "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ1c2VyLWNlbnRlciIsImV4cCI6MTc4NjQ0NjgxNCwiaWF0IjoxNzgzODU0ODE0LCJqdGkiOiJkOTluZG5rZGVpanFmNDhrMTcyMCIsInR5cCI6ImFjY2VzcyIsImFwcF9pZCI6ImtpbWkiLCJzdWIiOiJkM3B0cmo0YmNkcnNzcnNtMHIxMCIsInNwYWNlX2lkIjoiZDNwdHJqNGJjZHJzc3JzbTByMGciLCJhYnN0cmFjdF91c2VyX2lkIjoiZDNwdHJqNGJjZHJzc3JzbTByMDAiLCJzc2lkIjoiMTczMTQyNzM0ODYzMzAzNzQ2OCIsImRldmljZV9pZCI6Ijc2MDM4OTk0NjYyMjI5Mzc2MDkiLCJyZWdpb24iOiJjbiIsIm1lbWJlcnNoaXAiOnsibGV2ZWwiOjEwfX0.mOa7UKFRaO_VXY4ff2wq92LJD9gtEaU42uacNXLuDQxBlDi7c979AOLNHDY6ZVLBiOSobCP_C0-kzjco5pbeCQ"

    /**
     * 从存储的 Kimi 凭证字符串中提取 Bearer token。
     * 支持：直接是 JWT（eyJ 开头）、或从 Cookie 串中识别 token（eyJ 值或常见 token 名）。
     * 若都无法识别，返回原始串（由调用方决定是否可用）。
     */
    fun extractToken(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith("eyJ")) return trimmed
        // 在 cookie 串中查找 JWT 值
        for (p in trimmed.split(';')) {
            val v = p.trim().substringAfter('=', "").trim()
            if (v.startsWith("eyJ")) return v
        }
        // 常见 token 名
        val names = setOf(
            "kimi_token", "token", "access_token", "user_token",
            "auth", "sessionid", "sid", "ssid"
        )
        for (p in trimmed.split(';')) {
            val idx = p.indexOf('=')
            if (idx < 0) continue
            val k = p.substring(0, idx).trim().lowercase()
            val v = p.substring(idx + 1).trim()
            if (k in names && v.isNotEmpty()) return v
        }
        return trimmed
    }

    /** 将 OpenAI 格式 messages 转换为 Kimi 请求体（对齐 kimi2api.js buildKimiRequest）。 */
    private fun buildRequestBody(messages: List<ChatMessage>): String {
        val systemMsg = messages.firstOrNull { it.role == "system" }
        val systemPrompt = systemMsg?.content?.takeIf { it.isNotBlank() }

        val parts = mutableListOf<String>()
        for (m in messages) {
            val content = m.content
            if (content.isBlank()) continue
            val label = when (m.role) {
                "system" -> "System"
                "user" -> "User"
                "assistant" -> "Assistant"
                else -> m.role
            }
            parts.add("$label: $content")
        }
        val text = parts.joinToString("\n\n")

        val block = JSONObject().apply {
            put("id", "")
            put("parentId", JSONObject.NULL)
            put("messageId", JSONObject.NULL)
            put("text", JSONObject().apply {
                put("content", text)
                put("tips", JSONObject.NULL)
            })
            put("search", JSONObject.NULL)
            put("file", JSONObject.NULL)
            put("think", JSONObject.NULL)
            put("exception", JSONObject.NULL)
            put("memory", JSONObject.NULL)
            put("contractReview", JSONObject.NULL)
            put("tool", JSONObject.NULL)
            put("artifact", JSONObject.NULL)
            put("slidesView", JSONObject.NULL)
            put("multiStage", JSONObject.NULL)
            put("elemeMenuCard", JSONObject.NULL)
            put("elemeOrderCard", JSONObject.NULL)
            put("videoCards", JSONObject.NULL)
            put("explorerResearch", JSONObject.NULL)
            put("explorerResearchReanswer", JSONObject.NULL)
            put("aippt", JSONObject.NULL)
            put("contentViewZhidemaiCard", JSONObject.NULL)
            put("error", JSONObject.NULL)
            put("createTime", JSONObject.NULL)
        }

        val msg = JSONObject().apply {
            put("id", "")
            put("parentId", "")
            put("role", "user")
            put("status", JSONObject.NULL)
            put("refs", JSONObject.NULL)
            put("scenario", "SCENARIO_K2D5")
            put("kimiPlus", JSONObject().apply {
                put("id", ""); put("avatarUrl", ""); put("name", ""); put("specialId", "")
            })
            put("vote", JSONObject.NULL)
            put("error", JSONObject.NULL)
            put("createTime", JSONObject.NULL)
            put("childrenMessageIds", JSONArray())
            put("blocks", JSONArray().apply { put(block) })
            put("labels", JSONArray())
            put("labelsValue", JSONArray())
        }

        val options = JSONObject().apply {
            put("lbs", JSONObject.NULL)
            put("voiceId", "S_LvP7QSYN")
            put("speed", 1.25)
            put("thinking", false)
            put("systemPrompt", systemPrompt ?: JSONObject.NULL)
            put("slides", JSONObject.NULL)
        }

        val tools = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "TOOL_TYPE_DEVICE_LBS")
                put("search", JSONObject.NULL); put("genImage", JSONObject.NULL); put("name", "")
            })
            put(JSONObject().apply {
                put("type", "TOOL_TYPE_SEARCH")
                put("search", JSONObject.NULL); put("genImage", JSONObject.NULL); put("name", "")
            })
        }

        return JSONObject().apply {
            put("chatId", "")
            put("kimiplusId", "")
            put("scenario", "SCENARIO_K2D5")
            put("message", msg)
            put("options", options)
            put("projectId", JSONObject.NULL)
            put("tools", tools)
        }.toString()
    }

    /**
     * 调用 Kimi /Chat，返回原始 Response（Connect 二进制帧流）。
     * 由调用方读取并解析帧。token 为空时回退到兜底 JWT。
     */
    fun chatCompletion(messages: List<ChatMessage>, token: String): Response {
        val auth = if (token.isBlank()) FALLBACK_AUTH else token
        val json = buildRequestBody(messages)
        val jBytes = json.toByteArray(Charsets.UTF_8)
        val buf = ByteArray(5 + jBytes.size)
        buf[0] = 0x00
        buf[1] = ((jBytes.size shr 24) and 0xFF).toByte()
        buf[2] = ((jBytes.size shr 16) and 0xFF).toByte()
        buf[3] = ((jBytes.size shr 8) and 0xFF).toByte()
        buf[4] = (jBytes.size and 0xFF).toByte()
        System.arraycopy(jBytes, 0, buf, 5, jBytes.size)

        val req = Request.Builder().url("https://$HOST$CHAT_PATH")
            .post(buf.toRequestBody("application/connect+json".toMediaType()))
            .header("Authorization", "Bearer $auth")
            .header("Content-Type", "application/connect+json")
            .header("Accept", "application/json")
            .header("User-Agent", UA)
            .build()

        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) {
            val err = resp.body?.string() ?: ""
            resp.close()
            throw RuntimeException("Kimi /Chat HTTP ${resp.code}: ${err.take(300)}")
        }
        return resp
    }

    /**
     * 解析 Connect 二进制帧流：逐帧回调文本块与 chatId。
     * 帧格式：[1字节 flag] + [4字节大端长度] + [UTF-8 JSON]。
     */
    suspend fun decodeFrames(
        src: okio.BufferedSource,
        onText: suspend (String) -> Unit,
        onChatId: suspend (String) -> Unit
    ) {
        while (true) {
            if (!src.request(5)) break
            src.readByte() // flag
            val len = src.readInt() // 大端 4 字节
            if (len <= 0 || len > 50_000_000) break
            if (!src.request(len.toLong())) break
            val body = src.readUtf8(len.toLong())
            try {
                val obj = JSONObject(body)
                // chatId
                val chat = obj.optJSONObject("chat")
                if (chat != null) {
                    val id = chat.optString("id", "")
                    if (id.isNotEmpty()) onChatId(id)
                }
                obj.optString("chatId", "").takeIf { it.isNotEmpty() }?.let { onChatId(it) }
                // 文本块（AI 实际回复）
                val block = obj.optJSONObject("block")
                val content = block?.optJSONObject("text")?.optString("content", "")
                if (!content.isNullOrEmpty()) onText(content)
                // 兼容旧格式 chat.reply.blocks
                val reply = chat?.optJSONObject("reply")
                val blocks = reply?.optJSONArray("blocks")
                if (blocks != null) {
                    for (i in 0 until blocks.length()) {
                        val t = blocks.optJSONObject(i)?.optJSONObject("text")?.optString("content", "")
                        if (!t.isNullOrEmpty()) onText(t)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    /** 删除已完成的 Kimi 会话（静默执行，不阻塞）。 */
    fun deleteChat(chatId: String, token: String) {
        if (chatId.isBlank()) return
        val auth = if (token.isBlank()) FALLBACK_AUTH else token
        try {
            val body = JSONObject().put("chat_id", chatId).toString()
            val req = Request.Builder().url("https://$HOST$DELETE_PATH")
                .post(body.toRequestBody("application/json".toMediaType()))
                .header("Authorization", "Bearer $auth")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", UA)
                .build()
            client.newCall(req).execute().use { }
        } catch (_: Exception) {
        }
    }
}
