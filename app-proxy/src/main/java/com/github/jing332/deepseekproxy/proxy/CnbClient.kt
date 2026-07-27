package com.github.jing332.deepseekproxy.proxy

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.jvm.Synchronized

/**
 * CNB (cnb.cool) 混元3 接口客户端。OpenAI 兼容格式。
 * 认证方式（对齐朗读规则脚本）：
 *   1) GET /explore，从响应头 Set-Cookie 取 csrfkey（32 位 hex），从页面
 *      __NEXT_DATA__ 的 props.pageProps.csrftoken 取 csrftoken；
 *   2) 调用 /ai/chat/completions 时携带 Cookie: csrfkey=xxx 与 Csrftoken: xxx；
 *   3) 刷新策略（避免过期 token 被一直复用）：
 *      - 每次重新打开 APP 时强制重新获取（见 MainActivity / ProxyServer.start 的 ensureAuth(force=true)）；
 *      - 正常请求路径按时间限频：距上次获取超过 [AUTH_MAX_AGE_MS]（8 小时）才重新获取；
 *      - 401/403 或「200 但响应体是错误文本/HTML」时清空 csrftoken 并刷新重试一次。
 */
object CnbClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // 流式读取不设超时，由上层 withTimeout 兜底
        .build()
    private const val BASE = "https://cnb.cool"
    private const val EXPLORE = "$BASE/explore"
    private const val COMPLETIONS = "$BASE/ai/chat/completions"
    private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    const val DEFAULT_MODEL = "hy3-preview"

    /** 凭证最长有效期：超过该时长（默认 8 小时）才允许在请求路径上重新获取，避免频繁请求。 */
    private const val AUTH_MAX_AGE_MS = 8L * 60 * 60 * 1000

    /** 可选：用户手动提供的 csrfkey（留空则首次访问 explore 时由服务端分配）。 */
    @Volatile var csrfkeySeed: String = ""
    /** 持久化目录（用于缓存 csrfkey / csrftoken，避免每次都请求 explore）。 */
    var storageDir: File? = null

    @Volatile private var csrfkey: String = ""
    @Volatile private var csrftoken: String = ""
    @Volatile private var restored = false
    /** 上次成功获取凭证的时间戳（用于 8 小时限频）。 */
    @Volatile private var lastAuthTime = 0L

    private val csrfKeyRe = Regex("csrfkey=([a-f0-9]{32})", RegexOption.IGNORE_CASE)
    private val nextDataRe =
        Regex("""<script[^>]*id=["']__NEXT_DATA__["'][^>]*>([\s\S]*?)</script>""")

    private fun tokenFile() = File(storageDir, "cnb_token.json")
    private fun cookieFile() = File(storageDir, "cnb_cookie.json")

    /** 从持久化文件恢复 csrfkey / csrftoken（storageDir 设置后调用，仅执行一次）。 */
    fun loadPersisted() {
        if (restored) return
        restored = true
        try {
            tokenFile().takeIf { it.exists() }?.readText()?.let {
                csrftoken = JSONObject(it).optString("csrftoken", "")
            }
        } catch (_: Exception) {
        }
        try {
            cookieFile().takeIf { it.exists() }?.readText()?.let {
                val o = JSONObject(it)
                val c = o.optString("cookie", o.optString("csrfkey", ""))
                val v = if (c.startsWith("csrfkey=", true)) c.substring("csrfkey=".length) else c
                if (v.isNotBlank()) csrfkey = v
            }
        } catch (_: Exception) {
        }
        if (csrfkey.isBlank()) csrfkey = csrfkeySeed
    }

    /**
     * 确保已持有有效的 csrfkey + csrftoken。
     * @param force true 时忽略时间与缓存，强制重新获取（用于 APP 重新打开等场景）；
     *              否则仅当凭证缺失或距上次获取已超过 [AUTH_MAX_AGE_MS] 时才重新获取，避免频繁请求。
     */
    @Synchronized
    fun ensureAuth(force: Boolean = false) {
        loadPersisted()
        val now = System.currentTimeMillis()
        if (!force && csrftoken.isNotBlank() && csrfkey.isNotBlank()
            && (now - lastAuthTime) < AUTH_MAX_AGE_MS
        ) {
            return
        }
        fetchAuth()
        lastAuthTime = System.currentTimeMillis()
    }

    private fun fetchAuth() {
        LogStore.d("CNB", "获取认证: GET $EXPLORE")
        val reqBuilder = Request.Builder().url(EXPLORE)
            .header("User-Agent", UA)
            .header("Accept", "text/html,*/*")
        if (csrfkey.isNotBlank()) reqBuilder.header("Cookie", "csrfkey=$csrfkey")
        client.newCall(reqBuilder.build()).execute().use { resp ->
            // csrfkey 来自 Set-Cookie 响应头（页面不回显）
            val setCookie = resp.headers("set-cookie").joinToString("; ")
            csrfKeyRe.find(setCookie)?.groupValues?.get(1)?.let { k ->
                if (k != csrfkey) {
                    csrfkey = k
                    try {
                        cookieFile().writeText(JSONObject().put("cookie", "csrfkey=$k").toString())
                    } catch (_: Exception) {
                    }
                    LogStore.d("CNB", "已从 Set-Cookie 更新 csrfkey")
                }
            }
            // csrftoken 来自页面 __NEXT_DATA__
            val html = resp.body?.string() ?: ""
            nextDataRe.find(html)?.groupValues?.get(1)?.let { script ->
                try {
                    val token = JSONObject(script).optJSONObject("props")
                        ?.optJSONObject("pageProps")?.optString("csrftoken")
                    if (!token.isNullOrBlank()) {
                        csrftoken = token
                        try {
                            tokenFile().writeText(JSONObject().put("csrftoken", token).toString())
                        } catch (_: Exception) {
                        }
                        LogStore.d("CNB", "已获取 csrftoken")
                        return
                    }
                } catch (_: Exception) {
                }
            }
            throw RuntimeException("未能从 explore 页面提取 csrftoken")
        }
    }

    /**
     * 调用 /ai/chat/completions。失败时（401/403，或 200 但响应体是错误文本/HTML）清空
     * csrftoken 并刷新凭证重试一次。返回的 Response 由调用方负责读取与关闭。
     */
    fun chatCompletion(rawBody: String, model: String = DEFAULT_MODEL): Response {
        return doCompletion(rawBody, model, retry = true)
    }

    private fun doCompletion(rawBody: String, model: String, retry: Boolean): Response {
        ensureAuth()
        val bodyJson = JSONObject(rawBody)
        bodyJson.put("model", model)
        val upstreamStream = bodyJson.optBoolean("stream", false)
        val req = Request.Builder().url(COMPLETIONS)
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .header("User-Agent", UA)
            .header("Origin", BASE)
            .header("Referer", "$BASE/explore")
            .header("Cookie", "csrfkey=$csrfkey")
            .header("Csrftoken", csrftoken)
            .build()
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) {
            val code = resp.code
            val err = resp.body?.string() ?: ""
            resp.close()
            if ((code == 401 || code == 403)) {
                return refreshAndRetry(rawBody, model, retry, "鉴权失败($code)")
            }
            throw RuntimeException("CNB 返回 HTTP $code: ${err.take(300)}")
        }
        // 非流式响应：读取完整体，识别「200 但内容是错误文本/HTML」的情况（CNB 过期时
        // 常返回登录页 HTML 或带 error 字段的 JSON，而非真正的错误状态码）。
        if (!upstreamStream) {
            val text = resp.body?.string() ?: ""
            resp.close()
            if (looksLikeAuthError(text)) {
                return refreshAndRetry(rawBody, model, retry, "响应体疑似鉴权失效")
            }
            // 正常：用已读取的文本构造等价 Response 返回（避免二次消费 body）
            return Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(text.toResponseBody("application/json".toMediaType()))
                .build()
        }
        return resp
    }

    /** 清空凭证并强制刷新后重试一次；已重试过则直接抛错。 */
    private fun refreshAndRetry(rawBody: String, model: String, retry: Boolean, reason: String): Response {
        if (!retry) throw RuntimeException("CNB $reason，刷新凭证重试后仍失败")
        LogStore.w("CNB", "$reason，刷新凭证后重试")
        csrftoken = ""
        try {
            tokenFile().writeText("")
        } catch (_: Exception) {
        }
        ensureAuth(force = true)
        return doCompletion(rawBody, model, retry = false)
    }

    /**
     * 判断非流式响应体是否像「鉴权失效」而非正常结果：
     *  - 成功结果应为含 choices 字段的 JSON；
     *  - 含 error/error_code/error_message/detail 字段、或非 JSON（HTML 登录页等）视为错误。
     */
    private fun looksLikeAuthError(text: String): Boolean {
        val t = text.trim()
        if (t.isEmpty()) return true
        return try {
            val o = JSONObject(t)
            if (o.has("choices")) false
            else o.has("error") || o.has("error_code") || o.has("error_message")
                    || o.optString("detail", "").isNotBlank()
        } catch (_: Exception) {
            true
        }
    }
}
