package com.github.jing332.deepseekproxy.proxy

import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 美团 LongCat（长猫助手）接口客户端，移植自 longcat_proxy.py。
 * 认证为抓包得到的静态值：mtgsig（JSON 串）、m-traceid、Cookie(passport_token_key)，
 * 直接作为请求头发送，无需动态签名算法（与 MiniMax 不同，这里没有 HMAC/md5 计算）。
 *
 *  - 建会话：POST https://longcat.chat/api/v1/session-create
 *  - 聊天：  POST https://longcat.chat/api/v1/chat-completion-V2（SSE 流式）
 */
object LongCatClient {
    private const val TAG = "LongCat"
    /** mtgsig 签名串（JSON 字符串，抓包获得）。 */
    @Volatile var mtgsig: String = ""
    /** 请求追踪 ID（数字字符串，抓包获得）。 */
    @Volatile var mTraceId: String = ""
    /** Cookie 字符串，含 passport_token_key（抓包获得）。 */
    @Volatile var cookies: String = ""
    /** 智能体 ID，默认 "1"。 */
    @Volatile var agentId: String = "1"
    @Volatile var baseUrl: String =
        "https://longcat.chat/api/v1/chat-completion-V2?yodaReady=h5&csecplatform=4&csecversion=4.2.4"

    /** 解析配置 JSON 并应用到本对象的静态字段；空串则复位为默认（未配置）状态。 */
    fun applyConfig(json: String) {
        if (json.isBlank()) {
            mtgsig = ""
            mTraceId = ""
            cookies = ""
            agentId = "1"
            baseUrl =
                "https://longcat.chat/api/v1/chat-completion-V2?yodaReady=h5&csecplatform=4&csecversion=4.2.4"
            return
        }
        try {
            val o = org.json.JSONObject(json)
            mtgsig = o.optString("mtgsig", "")
            mTraceId = o.optString("m_traceid", "")
            cookies = o.optString("cookies", "")
            agentId = o.optString("agent_id", "1").ifBlank { "1" }
            val bu = o.optString("base_url", "")
            if (bu.isNotBlank()) {
                // 防御：抓到的请求可能是 task-check 等轮询接口，强制改写为聊天接口地址
                baseUrl = if (bu.contains("/task-check")) {
                    bu.replace("/task-check", "/chat-completion-V2")
                } else bu
            }
        } catch (_: Exception) {
        }
    }

    private val SESSION_URL =
        "https://longcat.chat/api/v1/session-create?yodaReady=h5&csecplatform=4&csecversion=4.2.4"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /** 将全部 messages 拼成单段文本（对齐 longcat_proxy.py 的 _build_content）。 */
    private fun buildContent(messages: List<ChatMessage>): String {
        return messages.joinToString("\n\n") { m ->
            val c = m.content
            when (m.role.lowercase()) {
                "system" -> "system: $c"
                "assistant" -> "assistant: $c"
                else -> "user: $c"
            }
        }
    }

    private fun makeHeaders(): Headers = Headers.Builder().apply {
        add("Host", "longcat.chat")
        add("m-traceid", mTraceId)
        add("mtgsig", mtgsig)
        add("User-Agent", "Mozilla/5.0 (Linux; Android 15; V2352A Build/AP3A.240905.015.A2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.6478.71 Mobile Safari/537.36")
        add("Content-Type", "application/json")
        add("Accept", "text/event-stream,application/json")
        add("X-Requested-With", "XMLHttpRequest")
        add("m-appkey", "fe_com.sankuai.friday.fe.longcat")
        add("x-client-language", "zh")
        add("Origin", "https://longcat.chat")
        add("Referer", "https://longcat.chat/t")
        add("Cookie", cookies)
    }.build()

    /** 创建会话，返回 conversationId；失败抛异常。 */
    private fun createSession(): String {
        val req = Request.Builder().url(SESSION_URL)
            .post(
                JSONObject().apply { put("model", ""); put("agentId", agentId) }
                    .toString().toRequestBody("application/json".toMediaType())
            )
            .headers(makeHeaders())
            .build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw RuntimeException("LongCat 创建会话失败 HTTP ${resp.code}: ${text.take(300)}")
            }
            val o = JSONObject(text)
            if (o.optInt("code", -1) == 0) {
                val cid = o.optJSONObject("data")?.optString("conversationId")?.takeIf { it.isNotBlank() }
                if (cid != null) return cid
            }
            throw RuntimeException("LongCat 创建会话失败: ${o.optString("message", text.take(200))}")
        }
    }

    /** 调用聊天补全，返回原始 SSE 响应（调用方负责读取并关闭）。 */
    fun chatCompletion(messages: List<ChatMessage>, model: String): Response {
        val convId = createSession()
        val searchEnabled = if (model == "longcat-search") 1 else 0
        val payload = JSONObject().apply {
            put("conversationId", convId)
            put("content", buildContent(messages))
            put("agentId", agentId)
            put("files", JSONArray())
            put("creationParam", JSONObject())
            put("reasonEnabled", 0)
            put("searchEnabled", searchEnabled)
            put("parentMessageId", 0)
            put("location", JSONArray())
        }
        val req = Request.Builder().url(baseUrl)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .headers(makeHeaders())
            .build()
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) {
            val err = resp.body?.string() ?: ""
            resp.close()
            throw RuntimeException("LongCat 聊天失败 HTTP ${resp.code}: ${err.take(300)}")
        }
        return resp
    }
}
