package com.github.jing332.deepseekproxy.proxy

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * 豆包（Doubao）接口客户端。认证完全依赖 Cookie（sessionid 等），
 * 无需 HMAC 签名。端口自 doubao2api.js（桌面客户端协议 / Samantha 生图协议）。
 *
 *  - 聊天：POST https://www.doubao.com/chat/completion （桌面客户端协议，SSE 流式）
 *  - 生图：POST https://www.doubao.com/samantha/chat/completion （Samantha 协议，content_type=2009）
 */
object DoubaoClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "https://www.doubao.com"
    private const val DEFAULT_BOT_ID = "7234781073513644036"
    private const val EXTENSION_BOT_ID = "7338286299411103781"
    private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36"

    // 设备指纹：每次请求随机生成一套（不再使用固定值，降低被风控/关联的概率）
    private data class DeviceParams(val deviceId: String, val webId: String, val fp: String)

    /** 生成指定位数的随机数字串（首位非零）。 */
    private fun randomNumeric(length: Int): String {
        val sb = StringBuilder()
        sb.append((1..9).random()) // 首位非零
        repeat(length - 1) { sb.append((0..9).random()) }
        return sb.toString()
    }

    /** 生成指定位数的随机字母数字串（base64 字符集）。 */
    private fun randomB64(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length).map { chars.random() }.joinToString("")
    }

    /** 生成一套随机设备指纹（device_id / web_id / fp 每次请求都不同）。 */
    private fun genDeviceParams(): DeviceParams {
        return DeviceParams(
            deviceId = randomNumeric(19),
            webId = randomNumeric(19),
            fp = "verify_${randomB64(8)}_${randomB64(13)}_${randomB64(5)}_${randomB64(5)}_${randomB64(13)}"
        )
    }

    data class DoubaoImage(val key: String, val oriUrl: String, val rawUrl: String, val thumbUrl: String, val width: Int, val height: Int, val format: String)
    data class DoubaoImageResult(val images: List<DoubaoImage>, val prompt: String)

    // ── Cookie 解析 ──────────────────────────────────────────
    fun parseCookie(raw: String): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        raw.split(';').forEach { pair ->
            val idx = pair.indexOf('=')
            if (idx < 0) return@forEach
            val k = pair.substring(0, idx).trim()
            val v = pair.substring(idx + 1).trim()
            if (k.isNotEmpty()) map[k] = v
        }
        return map
    }

    // ── Security Params ─────────────────────────────────────
    private fun securityParams(device: DeviceParams): Map<String, String> {
        val params = linkedMapOf(
            "aid" to "582478",
            "real_aid" to "582478",
            "device_id" to device.deviceId,
            "tea_uuid" to device.deviceId,
            "web_id" to device.webId,
            "device_platform" to "web",
            "language" to "zh",
            "region" to "CN",
            "sys_region" to "CN",
            "pkg_type" to "release_version",
            "version_code" to "20800",
            "pc_version" to "2.1.7",
            "chromium_version" to "135.0.7049.72",
            "client_platform" to "pc_client",
            "runtime" to "web",
            "runtime_version" to "3.5.4",
            "samantha_web" to "1",
            "use-olympus-account" to "1",
            "fp" to device.fp,
            "web_tab_id" to UUID.randomUUID().toString()
        )
        return params
    }

    private fun buildQueryString(params: Map<String, String>): String {
        return params.entries.sortedBy { it.key }.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }
    }

    private fun uuid() = UUID.randomUUID().toString()
    private fun uuid16() = uuid().replace("-", "").take(16)

    // ── 聊天 payload（桌面客户端协议）─────────────────────────
    private fun buildCompletionPayload(text: String, needDeepThink: Int, device: DeviceParams): String {
        val blocks = JSONArray()

        // 文本块
        blocks.put(JSONObject().apply {
            put("block_type", 10000)
            put("content", JSONObject().apply {
                put("text_block", JSONObject().apply {
                    put("text", text)
                    put("icon_url", "")
                    put("icon_url_dark", "")
                    put("summary", "")
                })
                put("pc_event_block", "")
            })
            put("block_id", uuid())
            put("parent_id", "")
            put("meta_info", JSONArray())
            put("append_fields", JSONArray())
            put("is_finish", true)
            put("patch_type", 2)
        })

        val payload = JSONObject().apply {
            put("client_meta", JSONObject().apply {
                put("local_conversation_id", "local_${uuid16()}")
                put("conversation_id", "")
                put("bot_id", EXTENSION_BOT_ID)
                put("last_section_id", "")
                put("last_message_index", 0)
            })
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("local_message_id", uuid())
                    put("content_block", blocks)
                    put("message_status", 0)
                })
            })
            put("option", JSONObject().apply {
                put("send_message_scene", "")
                put("create_time_ms", 0)
                put("collect_id", "")
                put("is_audio", false)
                put("answer_with_suggest", true)
                put("tts_switch", false)
                put("need_deep_think", needDeepThink)
                put("click_clear_context", false)
                put("from_suggest", false)
                put("is_regen", false)
                put("is_replace", false)
                put("disable_sse_cache", false)
                put("select_text_action", "")
                put("resend_for_regen", false)
                put("scene_type", 0)
                put("unique_key", uuid())
                put("start_seq", 0)
                put("need_create_conversation", true)
                put("conversation_init_option", JSONObject().apply {
                    put("need_ack_conversation", true)
                })
                put("regen_query_id", JSONArray())
                put("edit_query_id", JSONArray())
                put("regen_instruction", "")
                put("no_replace_for_regen", false)
                put("message_from", 0)
                put("shared_app_name", "")
                put("action_bar_skill_id", 0)
                put("sse_recv_event_options", JSONObject().apply { put("support_chunk_delta", true) })
                put("is_ai_playground", false)
            })
            put("chat_ability", JSONObject())
            put("ext", JSONObject().apply {
                put("use_deep_think", needDeepThink.toString())
                put("fp", device.fp)
                put("use_submit_pipeline", "1")
                put("commerce_credit_config_enable", "0")
                put("sub_conv_firstmet_type", "1")
            })
        }
        return payload.toString()
    }

    /**
     * 调用 /chat/completion（流式）。返回的 Response 由调用方读取并关闭。
     * 若返回非 SSE（如 JSON 鉴权错误），会在内部读取并抛出异常。
     */
    fun chatCompletion(text: String, cookies: Map<String, String>, needDeepThink: Int = 0): Response {
        val device = genDeviceParams()
        val params = securityParams(device)
        val url = "$BASE_URL/chat/completion?${buildQueryString(params)}"
        val csrfToken = cookies["passport_csrf_token"] ?: cookies["passport_csrf_token_default"] ?: ""

        val cookieHeader = cookies.entries.joinToString("; ") { (k, v) -> "$k=$v" }

        val req = Request.Builder().url(url)
            .post(buildCompletionPayload(text, needDeepThink, device).toRequestBody("application/json".toMediaType()))
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .header("x-tt-passport-csrf-token", csrfToken)
            .header("User-Agent", UA)
            .header("Origin", BASE_URL)
            .header("Referer", "$BASE_URL/chat")
            .header("Cookie", cookieHeader)
            .build()

        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) {
            val err = resp.body?.string() ?: ""
            resp.close()
            throw RuntimeException("豆包 /chat/completion HTTP ${resp.code}: ${err.take(300)}")
        }
        val ct = resp.header("Content-Type") ?: ""
        if (!ct.contains("text/event-stream", ignoreCase = true)) {
            val bodyText = resp.body?.string() ?: ""
            resp.close()
            try {
                val o = JSONObject(bodyText)
                if (o.has("code")) {
                    throw RuntimeException("豆包鉴权错误: code=${o.opt("code")} msg=${o.opt("msg") ?: o.opt("message") ?: ""}")
                }
            } catch (_: Exception) {
            }
            throw RuntimeException("豆包 /chat/completion 响应异常(content-type=$ct): ${bodyText.take(300)}")
        }
        return resp
    }

    // ── 生图 payload（Samantha 协议）──────────────────────────
    private fun buildImageGenPayload(prompt: String, ratio: String?): String {
        val contentObj = JSONObject().apply {
            put("text", prompt)
            if (!ratio.isNullOrBlank()) put("ratio", ratio)
        }
        val msg = JSONObject().apply {
            put("content", contentObj.toString())
            put("content_type", 2009)
            put("attachments", JSONArray())
            put("references", JSONArray())
            put("skill", JSONObject().apply {
                put("skill_type", 3)
                put("skill_type_no_default", 3)
                put("skill_id", "3")
                put("skill_id_no_default", "3")
            })
        }
        return JSONObject().apply {
            put("messages", JSONArray().apply { put(msg) })
            put("completion_option", JSONObject().apply {
                put("is_regen", false)
                put("with_suggest", true)
                put("need_create_conversation", true)
                put("launch_stage", 1)
                put("is_replace", false)
                put("is_delete", false)
                put("is_ai_playground", false)
                put("memory_type", 2)
                put("message_from", 0)
                put("use_deep_think", false)
                put("use_auto_cot", false)
                put("resend_for_regen", false)
                put("enable_commerce_credit", false)
                put("action_bar_skill_id", 3)
            })
            put("evaluate_option", JSONObject().apply { put("web_ab_params", "") })
            put("local_conversation_id", uuid())
            put("local_message_id", uuid())
        }.toString()
    }

    /**
     * 生成图片（同步，读取完整响应后解析）。
     */
    fun generateImage(prompt: String, cookies: Map<String, String>, ratio: String? = null): DoubaoImageResult {
        val device = genDeviceParams()
        val params = securityParams(device)
        val url = "$BASE_URL/samantha/chat/completion?${buildQueryString(params)}"
        val cookieHeader = cookies.entries.joinToString("; ") { (k, v) -> "$k=$v" }

        val req = Request.Builder().url(url)
            .post(buildImageGenPayload(prompt, ratio).toRequestBody("application/json".toMediaType()))
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .header("Agw-Js-Conv", "str")
            .header("User-Agent", UA)
            .header("Origin", BASE_URL)
            .header("Referer", "$BASE_URL/chat")
            .header("Cookie", cookieHeader)
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                val err = resp.body?.string() ?: ""
                throw RuntimeException("豆包生图 HTTP ${resp.code}: ${err.take(300)}")
            }
            val bodyText = resp.body!!.string()
            val images = mutableListOf<DoubaoImage>()
            for (block in bodyText.split("\n\n")) {
                if (block.isBlank()) continue
                var dataStr = ""
                for (line in block.trim().lineSequence()) {
                    if (line.startsWith("data:")) dataStr = line.substring(5).trim()
                }
                if (dataStr.isBlank()) continue
                try {
                    val data = JSONObject(dataStr)
                    if (data.optInt("event_type") == 2005) {
                        throw RuntimeException("豆包生图错误: ${data.opt("event_data")}")
                    }
                    if (data.optInt("event_type") != 2001) continue
                    val ed = if (data.opt("event_data") is String) JSONObject(data.getString("event_data")) else data.optJSONObject("event_data") ?: JSONObject()
                    val msg = ed.optJSONObject("message") ?: JSONObject()
                    if (msg.optInt("content_type") != 2010) continue
                    val content = if (msg.opt("content") is String) JSONObject(msg.getString("content")) else msg.optJSONObject("content") ?: JSONObject()
                    for (i in 0 until (content.optJSONArray("data")?.length() ?: 0)) {
                        val item = content.optJSONArray("data")!!.optJSONObject(i) ?: continue
                        val ori = item.optJSONObject("image_ori") ?: JSONObject()
                        val raw = item.optJSONObject("image_raw") ?: JSONObject()
                        val thumb = item.optJSONObject("image_thumb") ?: JSONObject()
                        images.add(DoubaoImage(
                            key = item.optString("key", ""),
                            oriUrl = ori.optString("url", ""),
                            rawUrl = raw.optString("url", ""),
                            thumbUrl = thumb.optString("url", ""),
                            width = ori.optInt("width", thumb.optInt("width", 0)),
                            height = ori.optInt("height", thumb.optInt("height", 0)),
                            format = ori.optString("format", thumb.optString("format", ""))
                        ))
                    }
                } catch (e: Exception) {
                    if (e.message?.startsWith("豆包生图错误") == true) throw e
                }
            }
            return DoubaoImageResult(images, prompt)
        }
    }
}
