package com.github.jing332.tts_server_android.service.systts.help

import org.json.JSONArray
import org.json.JSONObject

/**
 * opencode（OpenCode Zen）渠道（10-09 全渠道批，协议=插件 opencode.ts/opencode-auth.ts）。
 *
 * 登录 = 无 OAuth：API key（用户手填 sk-…）或匿名槽（Bearer public，仅免费模型）。
 * 对话 = OpenAI 兼容 @ https://opencode.ai/zen/v1/chat/completions + 五项指纹头。
 * 续期 = 无（API key 静态）。
 *
 * ⚠️ session id 形状门禁：`ses_` + 12hex（毫秒时间戳）+ **14** base62（整段 38 字符；
 * 写 26 位随机段 → 匿名通道一律 403 FreeTierError）。会话内缓存复用（请求亲和）。
 */
object OpencodeChannel : ChatChannel {
    override val id = "opencode"
    override val displayName = "OpenCode"
    override val chatBaseUrl = "https://opencode.ai/zen/v1"

    /** 匿名槽固定凭据（免费模型专用；账号槽=用户手填的 sk- key） */
    const val ANON_KEY = "public"

    // ---- 指纹（照插件：projectId = sha1("git-remote:opencode/" + sha256("{key} {generation}"))，40hex）----
    private fun projectId(apiKey: String, generation: Long): String {
        val inner = java.security.MessageDigest.getInstance("SHA-256")
            .digest("$apiKey $generation".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return java.security.MessageDigest.getInstance("SHA-1")
            .digest("git-remote:opencode/$inner".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    /** ses_ + 12hex(毫秒) + 14base62 —— 形状门禁，段长错=匿名全 403 */
    private fun sessionId(): String {
        val hex12 = "%012x".format(System.currentTimeMillis()).takeLast(12)
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val rnd = StringBuilder()
        val rng = java.security.SecureRandom()
        repeat(14) { rnd.append(alphabet[rng.nextInt(alphabet.length)]) }
        return "ses_$hex12$rnd"
    }

    override fun chatHeaders(accessToken: String): Map<String, String> {
        val pid = projectId(accessToken, 0L)
        return mapOf(
            "x-opencode-project" to pid,
            "x-opencode-session" to sessionId(),
            "x-opencode-request" to DeviceCodeLogin.randomHex(16),
            "x-opencode-client" to "cli",
            "User-Agent" to "opencode/1.18.22",
            "Accept" to "text/event-stream",
        )
    }

    /**
     * 错误分类（照插件 opencode 错误优先级：先读体再判状态码——
     * 限流语义在响应体里，可能带任意状态码）。
     */
    override fun classifyError(httpStatus: Int, body: String): ChatChannel.ErrClass {
        if (body.contains("FreeUsageLimitError") || body.contains("GoUsageLimitError")) return ChatChannel.ErrClass.RATE_LIMIT
        if (body.contains("FreeTierError")) return ChatChannel.ErrClass.OTHER // 形状门禁，换号无用
        if (httpStatus == 429 || body.contains("too many requests", true)) return ChatChannel.ErrClass.RATE_LIMIT
        if (httpStatus == 401 || httpStatus == 403) return ChatChannel.ErrClass.AUTH
        return ChatChannel.ErrClass.OTHER
    }

    override fun refresh(acc: AccountPool.Account): Triple<String, String, Long>? = null // 无续期

    override fun queryCredits(acc: AccountPool.Account): Double = Double.NaN

    /** 匿名可用 + 付费兜底清单（照插件 opencode-product 静态表，已移除 ling-3.0-flash-fin-free） */
    override fun fetchModels(accessToken: String): List<String> {
        val anon = listOf(
            "big-pickle", "space-bunny-free", "longcat-2.5-preview-free", "mimo-v2.6-flash-free",
            "mimo-v2.5-free", "nemotron-3-ultra-free", "nemotron-3.5-lightning-free",
        )
        val paid = listOf(
            "deepseek-v4-flash", "deepseek-v4.1-flash", "glm-5.2", "kimi-k2.5",
            "minimax-m2.7", "minimax-m3", "qwen3.8-max",
        )
        return if (accessToken == ANON_KEY) anon else anon + paid
    }

    /** 从 opencode.ai/zen/v1/models 拉动态清单（失败静默回静态表） */
    fun fetchModelsRemote(apiKey: String): List<String> {
        return try {
            val r = DeviceCodeLogin.get("$chatBaseUrl/models", mapOf("Authorization" to "Bearer $apiKey"))
            if (!r.ok) emptyList()
            else {
                val arr = JSONObject(r.body).optJSONArray("data") ?: return emptyList()
                (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("id") }.filter { it.isNotEmpty() }
            }
        } catch (_: Exception) {
            return emptyList()
        }
    }
}
