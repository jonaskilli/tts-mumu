package com.github.jing332.deepseekproxy.proxy

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * 智谱清言（GLM）网页端 游客模式 客户端。
 *  - 认证：POST /user-api/guest/access + 签名 → 自动获取 guest token，**无需登录、无需 Cookie**
 *  - 聊天：POST /backend-api/assistant/stream（文本助手 assistant_id）
 *  - 生图：POST /backend-api/assistant/stream（图片助手 assistant_id，CogView）
 *
 * 签名算法与抓包逆向一致（已验证）：
 *   timestamp = 13位毫秒时间戳，倒数第二位替换为 (各位和 - 倒数第二位) % 10
 *   sign = md5("$timestamp-$nonce-$SIGN_SECRET")
 */
object ZhipuClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    private const val BASE = "https://chatglm.cn/chatglm"
    private const val SIGN_SECRET = "8a1317a7468aa3ad86e997d08f3f31cb"
    private const val TEXT_ASSISTANT_ID = "65940acff94777010aa6b796"
    private const val IMAGE_ASSISTANT_ID = "65a232c082ff90a2ad2f15e2"
    private const val UA = "Mozilla/5.0 (Linux; Android 15; V2352A Build/AP3A.240905.015.A2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.6478.71 Mobile Safari/537.36"
    private const val X_EXP_GROUPS = "na_android_config:exp:NA,na_4o_config:exp:4o_A,tts_config:exp:tts_config_a,na_glm4plus_config:exp:open,mainchat_server_app:exp:A,mobile_history_daycheck:exp:a,desktop_toolbar:exp:A,chat_drawing_server:exp:A,drawing_server_cogview:exp:cogview4,app_welcome_v2:exp:B,chat_drawing_streamv2:exp:A,mainchat_rm_fc:exp:add,mainchat_dr:exp:open,chat_auto_entrance:exp:A,drawing_server_hi_dream:control:A,homepage_square:exp:close,assistant_recommend_prompt:exp:3,app_home_regular_user:exp:A,mainchat_moe:exp:300,assistant_greet_user:exp:greet_user,app_welcome_personalize:exp:A,assistant_model_exp_group:exp:glm4.5,ai_wallet:exp:ai_wallet_enable"

    // ── 工具 ───────────────────────────────────────────────
    private fun md5(s: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(s.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
    private fun uuid16(): String = UUID.randomUUID().toString().replace("-", "").take(16)

    private data class Sign(val timestamp: String, val nonce: String, val sign: String)

    /** 生成签名三件套。 */
    private fun buildSign(): Sign {
        val now = System.currentTimeMillis().toString()
        val digits = now.map { it.digitToInt() }
        val sum = digits.sum()
        val secondLast = digits[digits.lastIndex - 1]
        val checksum = (sum - secondLast) % 10
        val timestamp = now.substring(0, now.length - 2) + checksum.toString() + now.last()
        val nonce = hex(ByteArray(16).also { SecureRandom().nextBytes(it) })
        val sign = md5("$timestamp-$nonce-$SIGN_SECRET")
        return Sign(timestamp, nonce, sign)
    }

    // ── guest token（每次合成都重新获取，deviceId 随之刷新）──────────
    // Token 仅在单次请求内把 access 与本次生成的 deviceId 绑定传递，不再跨请求缓存。
    private data class Token(val access: String, val refresh: String, val expiresAt: Long, val deviceId: String)

    private fun fetchGuestToken(): Token {
        val sign = buildSign()
        // 每次拉取 guest token 都生成全新的设备号，和 token 绑定使用
        val deviceId = UUID.randomUUID().toString().replace("-", "")
        val headers = linkedMapOf(
            "Accept" to "application/json, text/plain, */*",
            "Accept-Encoding" to "gzip, deflate",
            "Accept-Language" to "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6",
            "App-Name" to "chatglm",
            "Cache-Control" to "no-cache",
            "Content-Type" to "application/json",
            "Origin" to "https://chatglm.cn",
            "Pragma" to "no-cache",
            "Referer" to "https://chatglm.cn/",
            "User-Agent" to UA,
            "X-App-Fr" to "default",
            "X-App-Platform" to "pc",
            "X-App-Version" to "0.0.1",
            "X-Device-Brand" to "",
            "X-Device-Model" to "",
            "X-Device-Id" to deviceId,
            "X-Lang" to "zh",
            "X-Request-Id" to uuid16(),
            "X-Nonce" to sign.nonce,
            "X-Sign" to sign.sign,
            "X-Timestamp" to sign.timestamp
        )
        val req = Request.Builder().url("$BASE/user-api/guest/access")
            .post("".toRequestBody("application/json".toMediaType()))
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            .build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw RuntimeException("智谱获取 guest token HTTP ${resp.code}: ${text.take(300)}")
            val o = JSONObject(text)
            val result = o.optJSONObject("result") ?: JSONObject()
            val access = result.optString("access_token", "")
            if (access.isEmpty()) throw RuntimeException("智谱获取 guest token 失败: ${text.take(300)}")
            val expire = result.optLong("expire_time", 3600L)
            return Token(access, result.optString("refresh_token", ""), System.currentTimeMillis() + expire * 1000 - 30_000, deviceId)
        }
    }

    private fun streamHeaders(token: Token, sign: Sign): Map<String, String> {
        return linkedMapOf(
            "Host" to "chatglm.cn",
            "Authorization" to "Bearer ${token.access}",
            "Content-Type" to "application/json",
            "Accept" to "text/event-stream",
            "Origin" to "https://chatglm.cn",
            "Referer" to "https://chatglm.cn/",
            "User-Agent" to UA,
            "X-App-Platform" to "h5",
            "X-App-Version" to "0.0.1",
            "X-App-fr" to "default",
            "X-Lang" to "zh",
            "X-Device-Id" to token.deviceId,
            "X-Device-Model" to "",
            "X-Device-Brand" to "",
            "X-Request-Id" to uuid16(),
            "X-Exp-Groups" to X_EXP_GROUPS,
            "X-Timestamp" to sign.timestamp,
            "X-Nonce" to sign.nonce,
            "X-Sign" to sign.sign,
            "X-Requested-With" to "mark.via",
            "Sec-Fetch-Site" to "same-origin",
            "Sec-Fetch-Mode" to "cors",
            "Sec-Fetch-Dest" to "empty",
            "Accept-Language" to "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7"
        )
    }

    // ── 聊天（SSE 流式）─────────────────────────────────
    /**
     * 执行一次智谱请求。buildReq 内部会重新拉取 guest token（并生成全新 deviceId），
     * 所以「每次合成就换一次 deviceId」在此天然成立。遇到 401/403（鉴权失效 / 设备被风控）时，
     * 直接再 build 一次（再次换新 token + 新 deviceId）重试。
     */
    private fun executeWithAuthRetry(buildReq: () -> Request): Response {
        val resp = client.newCall(buildReq()).execute()
        if (resp.isSuccessful) return resp
        val code = resp.code
        val err = resp.body?.string() ?: ""
        resp.close()
        if (code == 401 || code == 403) {
            LogStore.i("Zhipu", "鉴权失效 HTTP $code，换新 token+deviceId 重试一次")
            val r2 = client.newCall(buildReq()).execute()
            if (!r2.isSuccessful) {
                val e2 = r2.body?.string() ?: ""
                r2.close()
                throw RuntimeException("智谱请求 HTTP ${r2.code}: ${e2.take(300)}")
            }
            return r2
        }
        throw RuntimeException("智谱请求 HTTP $code: ${err.take(300)}")
    }

    // ── 聊天（SSE 流式）─────────────────────────────────
    fun chatCompletion(text: String, assistantId: String = TEXT_ASSISTANT_ID, plusModel: Boolean = false): Response {
        val body = JSONObject().apply {
            put("assistant_id", assistantId)
            put("conversation_id", "")
            put("meta_data", JSONObject().apply {
                put("mention_assistant_id", "")
                put("mention_assistant_name", "")
                put("mention_assistant_avatar", "")
                put("mention_conversation_id", "")
                put("is_test", false)
                put("input_question_type", "xxxx")
                put("channel", "")
                put("agent_id", "")
                put("is_greeting", false)
                put("chat_mode", "")
                put("is_networking", false)
                put("platform", "h5")
                put("tm", "h5")
                // 增强/思考模式开关：true 走 plus 模型（更强、更慢）
                put("if_plus_model", plusModel)
                put("cogview", JSONObject().apply { put("rm_label_watermark", false) })
            })
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply { put("type", "text"); put("text", text) })
                    })
                })
            })
            put("is_cache", true)
        }.toString()

        return executeWithAuthRetry {
            val token = fetchGuestToken() // 每次合成重新获取 token，并生成全新 deviceId
            val sign = buildSign()
            Request.Builder().url("$BASE/backend-api/assistant/stream")
                .post(body.toRequestBody("application/json".toMediaType()))
                .apply { streamHeaders(token, sign).forEach { (k, v) -> header(k, v) } }
                .build()
        }
    }

    // ── 生图（同步读取完整响应后解析图片 URL）────────────────
    // cinematic=true 时走电影级模式（GLM Image）：chat_model=glm_image + resolution=hd + style=none
    fun generateImage(prompt: String, aspectRatio: String = "1:1", cinematic: Boolean = false): List<String> {
        val body = JSONObject().apply {
            put("assistant_id", IMAGE_ASSISTANT_ID)
            put("conversation_id", "")
            put("project_id", "")
            put("chat_type", "user_chat")
            put("meta_data", JSONObject().apply {
                put("cogview", JSONObject().apply {
                    put("aspect_ratio", aspectRatio)
                    put("style", if (cinematic) "none" else "")
                    put("scene", "")
                    if (cinematic) put("resolution", "hd")
                    put("chat_model", if (cinematic) "glm_image" else "")
                    put("rm_label_watermark", false)
                })
                put("is_test", false)
                put("input_question_type", "xxxx")
                put("channel", "")
                put("draft_id", "")
                put("chat_mode", "")
                put("is_networking", false)
                put("quote_log_id", "")
                put("platform", "pc")
            })
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply { put("type", "text"); put("text", prompt) })
                    })
                })
            })
        }.toString()

        val resp = executeWithAuthRetry {
            val token = fetchGuestToken() // 每次合成重新获取 token，并生成全新 deviceId
            val sign = buildSign()
            Request.Builder().url("$BASE/backend-api/assistant/stream")
                .post(body.toRequestBody("application/json".toMediaType()))
                .apply { streamHeaders(token, sign).forEach { (k, v) -> header(k, v) } }
                .build()
        }
        resp.use {
            val src = resp.body!!.source()
            val urls = mutableListOf<String>()
            val data = StringBuilder()
            while (true) {
                val line = src.readUtf8Line() ?: break
                if (line.isEmpty()) {
                    val payload = data.toString().trim()
                    data.setLength(0)
                    if (payload.isBlank() || payload == "[DONE]") continue
                    collectImageUrls(payload, urls)
                } else if (line.startsWith("data:")) {
                    data.append(line.substring(5).trimStart()).append("\n")
                }
            }
            return urls
        }
    }

    private fun collectImageUrls(payload: String, urls: MutableList<String>) {
        try {
            val o = JSONObject(payload)
            val parts = o.optJSONArray("parts") ?: return
            for (i in 0 until parts.length()) {
                val content = parts.optJSONObject(i)?.optJSONArray("content") ?: continue
                for (j in 0 until content.length()) {
                    val c = content.optJSONObject(j) ?: continue
                    if (c.optString("type") == "image") {
                        val imgs = c.optJSONArray("image") ?: continue
                        for (k in 0 until imgs.length()) {
                            val u = imgs.optJSONObject(k)?.optString("image_url", "") ?: ""
                            if (u.startsWith("http") && !urls.contains(u)) urls.add(u)
                        }
                    }
                    if (c.optString("type") == "text") {
                        val t = c.optString("text", "")
                        val re = Regex("\\((https?://\\S+)\\)")
                        re.findAll(t).forEach {
                            val u = it.groupValues[1]
                            if (!urls.contains(u)) urls.add(u)
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }
}
