package com.github.jing332.deepseekproxy.proxy

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * 千问（Qwen / qianwen.com）接口客户端。
 * 协议参考 qwen_api_test.py 与「千问API无思考模式测试+会话删除-使用文档.md」（逆向 qwen.ts）：
 *   - 聊天：POST https://chat2.qianwen.com/api/v2/chat （SSE 流式）
 *   - 鉴权使用 Cookie: tongyi_sso_ticket=... （Cookie 鉴权，与豆包一致）
 *   - 普通 / 思考模式由请求体 deep_search 控制："0" 关闭深度思考，"1" 启用
 *   - 响应 SSE 中 communication.sessionid 为服务端分配的会话ID
 *   - 删除会话：POST https://chat2-api.qianwen.com/api/v1/session/delete/batch
 */
object QwenClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    private const val CHAT_HOST = "chat2.qianwen.com"
    private const val CHAT_PATH = "/api/v2/chat"
    private const val DELETE_HOST = "chat2-api.qianwen.com"
    private const val DELETE_PATH = "/api/v1/session/delete/batch"
    private const val ORIGIN = "https://www.qianwen.com"
    private const val REFERER = "https://www.qianwen.com/"
    private const val UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36"

    /** 默认模型名（下拉「普通千问 / 思考模式」使用；本地接口可传入任意模型名覆盖）。 */
    const val DEFAULT_MODEL = "Qwen3.6"

    private fun uuid() = UUID.randomUUID().toString().replace("-", "")
    private fun nonce() = uuid().take(12)
    private fun timestamp() = System.currentTimeMillis().toString()

    /** 固定 biz 查询参数 + 每次重建的防重放参数（ut/nonce/timestamp）。 */
    private fun buildQuery(): String {
        val params = linkedMapOf(
            "biz_id" to "ai_qwen",
            "chat_client" to "h5",
            "device" to "pc",
            "fr" to "pc",
            "pr" to "qwen",
            "ut" to uuid(),
            "nonce" to nonce(),
            "timestamp" to timestamp()
        )
        return params.entries.joinToString("&") { (k, v) -> "$k=$v" }
    }

    /**
     * 调用千问 /chat（SSE 流式）。返回的 Response 由调用方读取并关闭。
     * cookieHeader 为完整 Cookie 字符串（含 tongyi_sso_ticket）。
     * deepSearch: "0" 普通, "1" 思考。model 为具体模型名。
     */
    fun chatCompletion(
        text: String, cookieHeader: String, deepSearch: String, model: String
    ): Response {
        val url = "https://$CHAT_HOST$CHAT_PATH?${buildQuery()}"
        val sessionId = uuid()
        val body = JSONObject().apply {
            put("deep_search", deepSearch)
            put("req_id", uuid())
            put("model", model)
            put("scene", "chat")
            put("session_id", sessionId)
            put("sub_scene", "chat")
            put("temporary", false)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("content", text)
                    put("mime_type", "text/plain")
                    put("meta_data", JSONObject().apply { put("ori_query", text) })
                })
            })
            put("from", "default")
            put("parent_req_id", "0")
            put("enable_search", false)
            put("biz_data", "{\"entryPoint\":\"tongyigw\"}")
            put("scene_param", "first_turn")
            put("chat_client", "h5")
            put("client_tm", timestamp())
            put("protocol_version", "v2")
            put("biz_id", "ai_qwen")
        }

        val req = Request.Builder().url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream, text/plain, */*")
            .header("Origin", ORIGIN)
            .header("Referer", REFERER)
            .header("Cookie", cookieHeader)
            .header("User-Agent", UA)
            .build()

        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) {
            val err = resp.body?.string() ?: ""
            resp.close()
            throw RuntimeException("千问 /chat HTTP ${resp.code}: ${err.take(300)}")
        }
        // 千问返回 SSE；若返回 JSON 鉴权错误也兼容处理（交由调用方解析）
        val ct = resp.header("Content-Type") ?: ""
        if (!ct.contains("text/event-stream", ignoreCase = true) &&
            !ct.contains("application/json", ignoreCase = true)
        ) {
            val bodyText = resp.body?.string() ?: ""
            resp.close()
            throw RuntimeException("千问 /chat 响应异常(content-type=$ct): ${bodyText.take(300)}")
        }
        return resp
    }

    /**
     * 解析千问 SSE 流：逐行读取 data: JSON。
     *  - onSessionId: 回调服务端 communication.sessionid（用于后续删除会话）
     *  - onText: 回调 AI 回复的增量文本（默认只回答案，过滤思考链/进度条）
     * 千问 SSE 每次推送的 content 多为「累积全量」，故用快照算增量；
     * 若是增量分片也能兼容（见 delta 计算逻辑）。
     */
    suspend fun parseQwenSse(
        src: okio.BufferedSource,
        onText: suspend (String) -> Unit,
        onSessionId: suspend (String) -> Unit
    ) {
        var lastAnswer = ""
        while (true) {
            val line = src.readUtf8Line() ?: break
            if (line.isBlank()) continue
            if (!line.startsWith("data:")) continue
            val jsonStr = line.substring(5).trim()
            if (jsonStr.isEmpty()) continue
            val obj = try {
                JSONObject(jsonStr)
            } catch (_: Exception) {
                continue
            }
            // 会话ID（服务端分配，用于删除）
            val comm = obj.optJSONObject("communication")
            val sid = comm?.optString("sessionid", "") ?: ""
            if (sid.isNotEmpty()) onSessionId(sid)

            // AI 回复
            val dataObj = obj.optJSONObject("data") ?: continue
            val messages = dataObj.optJSONArray("messages") ?: continue
            for (i in 0 until messages.length()) {
                val msg = messages.optJSONObject(i) ?: continue
                val mime = msg.optString("mime_type", "")
                // 进度条（深度思考进度）不计入答案
                if (mime == "bar/progress") continue
                // 思考链消息（multi_load/iframe + meta_data.multi_load[].type == deep_think）跳过
                if (mime == "multi_load/iframe") {
                    val ml = msg.optJSONObject("meta_data")?.optJSONArray("multi_load")
                    var isThink = false
                    if (ml != null) {
                        for (j in 0 until ml.length()) {
                            if (ml.optJSONObject(j)?.optString("type", "") == "deep_think") {
                                isThink = true
                                break
                            }
                        }
                    }
                    if (isThink) continue
                }
                val content = msg.optString("content", "")
                if (content.isBlank()) continue
                // 标记式思考链（[(deep_think)] 等）视为思考过程，默认不计入答案
                val stripped = content
                    .replace("\\[\\(deep_think\\)]".toRegex(), "")
                    .replace("\\[\\(multimodal_chat_think_\\d+\\)]".toRegex(), "")
                    .trim()
                if (stripped.isBlank()) continue
                // 快照算增量：当前内容是上次的超集 → 只发新增后缀；否则整体发送（兼容增量分片）
                val delta = if (stripped.length > lastAnswer.length && stripped.startsWith(lastAnswer)) {
                    stripped.substring(lastAnswer.length)
                } else if (stripped != lastAnswer) {
                    stripped
                } else {
                    ""
                }
                if (delta.isNotBlank()) {
                    lastAnswer = stripped
                    onText(delta)
                }
            }
        }
    }

    /** 删除指定千问会话（静默执行，不阻塞，由开关控制是否调用）。 */
    fun deleteSession(sessionId: String, cookieHeader: String) {
        if (sessionId.isBlank()) return
        try {
            val url = "https://$DELETE_HOST$DELETE_PATH?${buildQuery()}"
            val body = JSONObject().put("session_ids", JSONArray().apply { put(sessionId) }).toString()
            val req = Request.Builder().url(url)
                .post(body.toRequestBody("application/json".toMediaType()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/plain, */*")
                .header("Origin", ORIGIN)
                .header("Referer", REFERER)
                .header("Cookie", cookieHeader)
                .header("User-Agent", UA)
                .build()
            client.newCall(req).execute().use { resp ->
                val text = try {
                    resp.body?.string() ?: ""
                } catch (_: Exception) {
                    ""
                }
                val ok = try {
                    JSONObject(text).optInt("error_code", -1) == 0
                } catch (_: Exception) {
                    resp.isSuccessful
                }
                LogStore.i("Qwen", "删除会话 $sessionId: HTTP ${resp.code}, ok=$ok")
            }
        } catch (e: Exception) {
            LogStore.e("Qwen", "删除会话异常: ${e.localizedMessage}")
        }
    }
}
