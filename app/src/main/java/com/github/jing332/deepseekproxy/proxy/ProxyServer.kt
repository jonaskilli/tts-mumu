package com.github.jing332.deepseekproxy.proxy

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.http.content.OutgoingContent
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import kotlin.concurrent.thread

@Serializable
data class ChatMessage(val role: String, val content: String)

@Serializable
data class ChatCompletionRequest(
    val model: String = "hy3-preview",
    val messages: List<ChatMessage> = emptyList(),
    val stream: Boolean = false,
    val temperature: Double? = null,
    val max_tokens: Int? = null,
    val conversation_id: String? = null
)

@Serializable
data class ChoiceDelta(val role: String? = null, val content: String? = null)

@Serializable
data class ChunkChoice(
    val index: Int = 0,
    val delta: ChoiceDelta,
    val finish_reason: String? = null
)

@Serializable
data class ChatCompletionChunk(
    val id: String,
    val `object`: String = "chat.completion.chunk",
    val created: Int,
    val model: String,
    val choices: List<ChunkChoice>
)

@Serializable
data class Msg(val role: String, val content: String)

@Serializable
data class Choice(
    val index: Int = 0,
    val message: Msg,
    val finish_reason: String? = null
)

@Serializable
data class Usage(
    val prompt_tokens: Int = 0,
    val completion_tokens: Int = 0,
    val total_tokens: Int = 0
)

@Serializable
data class ChatCompletion(
    val id: String,
    val `object`: String = "chat.completion",
    val created: Int,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage,
    val conversation_id: String
)

@Serializable
data class ImageDataItem(val url: String, val b64_json: String? = null)

@Serializable
data class ImageGenResponse(val created: Int, val data: List<ImageDataItem>)

/**
 * 本地 OpenAI 兼容中转服务（端口默认 8800）。
 * 上游按模型名路由：
 *   - 豆包（Doubao）模型  → 豆包接口（聊天 / 生图）
 *   - 其他（含混元 hy3）  → CNB 混元3
 */
object ProxyServer {
    private val json = Json { ignoreUnknownKeys = true }
    private var engine: EmbeddedServer<*, *>? = null

    /** 是否向上游（CNB）使用流式请求。由 UI 开关实时控制。 */
    @Volatile var streamMode: Boolean = false

    /** 完成回复后是否删除会话（Kimi / DeepSeek 通用）。由 UI 开关实时控制，默认开启。 */
    @Volatile var deleteSessionAfterReply: Boolean = true

    /** 生图默认比例（豆包 ratio 串），由设置界面实时控制，默认 9:16 竖屏。 */
    @Volatile var defaultImageSize: String = "9:16"

    // ── 豆包 Cookie 轮换（豆包接口使用 Cookie 鉴权）──
    private val doubaoLock = Any()
    @Volatile private var doubaoCookieList: List<String> = emptyList()
    private var doubaoCookieNames: List<String> = emptyList()
    private val doubaoRotationIdx = java.util.concurrent.atomic.AtomicInteger(0)

    /** 设置参与轮换的豆包 Cookie 列表（自动过滤空值，并保留对应名称）。 */
    fun setDoubaoCookieList(list: List<String>) {
        synchronized(doubaoLock) {
            val pairs = list.mapIndexed { i, c ->
                val name = if (i == 0) "默认Cookie" else "Cookie ${i + 1}"
                name to c
            }.filter { it.second.isNotBlank() }
            doubaoCookieList = pairs.map { it.second }
            doubaoCookieNames = pairs.map { it.first }
            if (doubaoRotationIdx.get() >= doubaoCookieList.size) doubaoRotationIdx.set(0)
        }
    }

    /** 取下一个用于豆包请求的 Cookie（在已保存的非空 Cookie 间轮询）。 */
    fun nextDoubaoCookie(): String {
        val list = synchronized(doubaoLock) { doubaoCookieList }
        if (list.isEmpty()) {
            LogStore.i("Doubao", "轮换：暂无可用 Cookie（列表为空）")
            return ""
        }
        val idx = doubaoRotationIdx.getAndUpdate { (it + 1) % list.size }
        val c = list[idx]
        val name = synchronized(doubaoLock) { doubaoCookieNames.getOrNull(idx) ?: "Cookie ${idx + 1}" }
        val preview = if (c.length <= 16) c else c.take(16) + "...(len=${c.length})"
        LogStore.i(
            "Doubao",
            "轮换：使用 [$name]（第 $idx 个，共 ${list.size} 个），预览=${preview}"
        )
        return c
    }

    // ── Kimi Cookie 轮换（Kimi Cookie 含登录 JWT，调用时由 KimiClient 提取 Bearer）──
    private val kimiLock = Any()
    @Volatile private var kimiCookieList: List<String> = emptyList()
    private var kimiCookieNames: List<String> = emptyList()
    private val kimiRotationIdx = java.util.concurrent.atomic.AtomicInteger(0)

    /** 设置参与轮换的 Kimi Cookie 列表（自动过滤空值，并保留对应名称）。 */
    fun setKimiCookieList(list: List<String>) {
        synchronized(kimiLock) {
            val pairs = list.mapIndexed { i, c ->
                val name = if (i == 0) "默认Cookie" else "Cookie ${i + 1}"
                name to c
            }.filter { it.second.isNotBlank() }
            kimiCookieList = pairs.map { it.second }
            kimiCookieNames = pairs.map { it.first }
            if (kimiRotationIdx.get() >= kimiCookieList.size) kimiRotationIdx.set(0)
        }
    }

    /** 取下一个用于 Kimi 请求的 Cookie（在已保存的非空 Cookie 间轮询）。 */
    fun nextKimiCookie(): String {
        val list = synchronized(kimiLock) { kimiCookieList }
        if (list.isEmpty()) {
            LogStore.i("Kimi", "轮换：暂无可用 Cookie（列表为空），将使用兜底 JWT")
            return ""
        }
        val idx = kimiRotationIdx.getAndUpdate { (it + 1) % list.size }
        val c = list[idx]
        val name = synchronized(kimiLock) { kimiCookieNames.getOrNull(idx) ?: "Cookie ${idx + 1}" }
        val preview = if (c.length <= 16) c else c.take(16) + "...(len=${c.length})"
        LogStore.i(
            "Kimi",
            "轮换：使用 [$name]（第 $idx 个，共 ${list.size} 个），预览=${preview}"
        )
        return c
    }

    // ── 千问（Qwen）Cookie 轮换（千问使用 Cookie 鉴权，含 tongyi_sso_ticket）──
    private val qwenLock = Any()
    @Volatile private var qwenCookieList: List<String> = emptyList()
    private var qwenCookieNames: List<String> = emptyList()
    private val qwenRotationIdx = java.util.concurrent.atomic.AtomicInteger(0)

    /** 设置参与轮换的千问 Cookie 列表（自动过滤空值，并保留对应名称）。 */
    fun setQwenCookieList(list: List<String>) {
        synchronized(qwenLock) {
            val pairs = list.mapIndexed { i, c ->
                val name = if (i == 0) "默认Cookie" else "Cookie ${i + 1}"
                name to c
            }.filter { it.second.isNotBlank() }
            qwenCookieList = pairs.map { it.second }
            qwenCookieNames = pairs.map { it.first }
            if (qwenRotationIdx.get() >= qwenCookieList.size) qwenRotationIdx.set(0)
        }
    }

    /** 取下一个用于千问请求的 Cookie（在已保存的非空 Cookie 间轮询）。 */
    fun nextQwenCookie(): String {
        val list = synchronized(qwenLock) { qwenCookieList }
        if (list.isEmpty()) {
            LogStore.i("Qwen", "轮换：暂无可用 Cookie（列表为空）")
            return ""
        }
        val idx = qwenRotationIdx.getAndUpdate { (it + 1) % list.size }
        val c = list[idx]
        val name = synchronized(qwenLock) { qwenCookieNames.getOrNull(idx) ?: "Cookie ${idx + 1}" }
        val preview = if (c.length <= 16) c else c.take(16) + "...(len=${c.length})"
        LogStore.i(
            "Qwen",
            "轮换：使用 [$name]（第 $idx 个，共 ${list.size} 个），预览=${preview}"
        )
        return c
    }

    // ── DeepSeek Token 轮换（DeepSeek 使用 Bearer token 鉴权）──
    private val deepseekLock = Any()
    @Volatile private var deepseekTokenList: List<String> = emptyList()
    private var deepseekTokenNames: List<String> = emptyList()
    /** DeepSeek 网页中转模式：default（快速模式）/ expert（专家模式）。 */
    @Volatile var deepseekMode: String = "default"
    private val deepseekRotationIdx = java.util.concurrent.atomic.AtomicInteger(0)

    fun setDeepSeekTokenList(list: List<String>) {
        synchronized(deepseekLock) {
            val pairs = list.mapIndexed { i, c ->
                val name = if (i == 0) "默认Token" else "Token ${i + 1}"
                name to c
            }.filter { it.second.isNotBlank() }
            deepseekTokenList = pairs.map { it.second }
            deepseekTokenNames = pairs.map { it.first }
            if (deepseekRotationIdx.get() >= deepseekTokenList.size) deepseekRotationIdx.set(0)
        }
    }

    fun nextDeepSeekToken(): String {
        val list = synchronized(deepseekLock) { deepseekTokenList }
        if (list.isEmpty()) {
            LogStore.i("DeepSeek", "轮换：暂无可用 Token（列表为空）")
            return ""
        }
        val idx = deepseekRotationIdx.getAndUpdate { (it + 1) % list.size }
        val c = list[idx]
        val name = synchronized(deepseekLock) { deepseekTokenNames.getOrNull(idx) ?: "Token ${idx + 1}" }
        LogStore.i("DeepSeek", "轮换：使用 [$name]（第 $idx 个，共 ${list.size} 个）")
        return c
    }

    /** 识别为豆包模型的名称（前缀匹配，大小写不敏感）。 */
    private val DOUBAO_MODELS = listOf("doubao", "豆包")

    /** 识别为 Kimi 模型的名称（前缀匹配，大小写不敏感）。 */
    private val KIMI_MODELS = listOf("kimi")

    /** 识别为千问（Qwen）模型的名称（前缀匹配，大小写不敏感）。 */
    private val QWEN_MODELS = listOf("qwen")

    /** 识别为 DeepSeek 模型的名称（前缀匹配，大小写不敏感）。 */
    private val DEEPSEEK_MODELS = listOf("deepseek")

    /**
     * 识别为智谱（GLM）文本模型的名称（免登录）。
     * 本地接口用 gml/glm/zhipu 调用；UI 上的「智谱」(zhipu) 即智谱文本入口。
     */
    private val ZHIPU_MODELS = listOf("gml", "zhipu")

    /** 识别为智谱生图模型的名称（免登录）。UI 上的 zhipu 点「生图」同样走智谱生图。 */
    private val ZHIPU_IMAGE_MODELS = listOf("智谱生图", "zhipu-image", "glm-image", "gml-image", "cogview")

    /** 识别为 LongCat（美团龙猫）模型的名称（前缀匹配，大小写不敏感）。 */
    private val LONGCAT_MODELS = listOf("longcat")

    // ── LongCat 配置轮换（龙猫配置为整段 JSON：mtgsig / m-traceid / Cookie）──
    private val longcatLock = Any()
    @Volatile private var longcatConfigList: List<String> = emptyList()
    private val longcatRotationIdx = java.util.concurrent.atomic.AtomicInteger(0)

    /** 设置参与轮换的 LongCat 配置列表（自动过滤空值）。 */
    fun setLongCatConfigList(list: List<String>) {
        synchronized(longcatLock) {
            longcatConfigList = list.filter { it.isNotBlank() }
            if (longcatRotationIdx.get() >= longcatConfigList.size) longcatRotationIdx.set(0)
        }
    }

    /** 取下一个 LongCat 配置并应用到 LongCatClient（在已保存的非空配置间轮询）。 */
    fun applyNextLongCatConfig() {
        val list = synchronized(longcatLock) { longcatConfigList }
        if (list.isEmpty()) return
        val idx = longcatRotationIdx.getAndUpdate { (it + 1) % list.size }
        LongCatClient.applyConfig(list[idx])
    }

    fun isRunning() = engine != null

    fun start(
        port: Int, csrfkeyProvider: () -> String,
        doubaoCookieProvider: () -> List<String>,
        kimiCookieProvider: () -> List<String>,
        qwenCookieProvider: () -> List<String>,
        storageDir: File
    ) {
        stop()
        CnbClient.csrfkeySeed = csrfkeyProvider()
        CnbClient.storageDir = storageDir
        setDoubaoCookieList(doubaoCookieProvider())
        setKimiCookieList(kimiCookieProvider())
        setQwenCookieList(qwenCookieProvider())
        // 启动服务时强制刷新一次混元凭证（APP 重新打开 / 服务（重）启动场景）
        thread(name = "cnb-auth-init") {
            runCatching { CnbClient.ensureAuth(force = true) }
        }
        engine = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            install(ContentNegotiation) { json(json) }
            install(CORS) {
                anyHost()
                allowMethod(HttpMethod.Options)
                allowMethod(HttpMethod.Post)
                allowMethod(HttpMethod.Get)
                allowHeader(HttpHeaders.Authorization)
                allowHeader(HttpHeaders.ContentType)
            }
            routing {
                get("/") {
                    call.respond(
                        mapOf(
                            "service" to "Hunyuan/Doubao Proxy (OpenAI-compatible)",
                            "endpoint" to "POST /v1/chat/completions",
                            "image_endpoint" to "POST /v1/images/generations"
                        )
                    )
                }
                get("/v1/models") {
                    // 兜底列表：任何异常时仍返回合法 200 JSON，避免客户端报 500。
                    val fallbackJson = """{"object":"list","data":[
                        {"id":"hy3-preview","object":"model","owned_by":"cnb"},
                        {"id":"longcat","object":"model","owned_by":"longcat"},
                        {"id":"longcat-search","object":"model","owned_by":"longcat"},
                        {"id":"doubao","object":"model","owned_by":"doubao"},
                        {"id":"doubao-think","object":"model","owned_by":"doubao"},
                        {"id":"doubao-image","object":"model","owned_by":"doubao"},
                        {"id":"Kimi","object":"model","owned_by":"kimi"},
                        {"id":"deepseek","object":"model","owned_by":"deepseek"},
                        {"id":"deepseek-pro","object":"model","owned_by":"deepseek"},
                        {"id":"gml","object":"model","owned_by":"zhipu"},
                        {"id":"gml-plus","object":"model","owned_by":"zhipu"},
                        {"id":"glm-image","object":"model","owned_by":"zhipu"},
                        {"id":"glm-image-hd","object":"model","owned_by":"zhipu"},
                        {"id":"qwen","object":"model","owned_by":"qwen"},
                        {"id":"qwen-think","object":"model","owned_by":"qwen"}
                    ]}"""
                    try {
                        val data = org.json.JSONArray()
                        val entries = listOf(
                            CnbClient.DEFAULT_MODEL to "cnb",          // hy3-preview
                            "longcat" to "longcat",
                            "longcat-search" to "longcat",
                            "doubao" to "doubao",
                            "doubao-think" to "doubao",
                            "doubao-image" to "doubao",
                            "Kimi" to "kimi",
                            "deepseek" to "deepseek",
                            "deepseek-pro" to "deepseek",
                            "gml" to "zhipu",
                            "gml-plus" to "zhipu",
                            "glm-image" to "zhipu",
                            "glm-image-hd" to "zhipu",
                            "qwen" to "qwen",
                            "qwen-think" to "qwen"
                        )
                        for ((id, owner) in entries) {
                            data.put(
                                org.json.JSONObject()
                                    .put("id", id)
                                    .put("object", "model")
                                    .put("owned_by", owner)
                            )
                        }
                        val json = org.json.JSONObject()
                            .put("object", "list")
                            .put("data", data)
                            .toString()
                        call.respondText(json, ContentType.Application.Json)
                    } catch (e: Exception) {
                        LogStore.e("Proxy", "构建 /v1/models 失败，返回兜底列表: ${e.stackTraceToString()}")
                        call.respondText(fallbackJson, ContentType.Application.Json)
                    }
                }
                post("/v1/chat/completions") { handleChatCompletions() }
                post("/v1/images/generations") { handleImageGen() }
            }
        }.start(wait = false)
    }

    fun stop() {
        engine?.stop(1000, 2000)
        engine = null
    }

    private fun mapModel(m: String): String {
        return when (m) {
            "hy3-preview", "glm-5.2", "deepseek-v4-pro", "deepseek-v4-flash" -> m
            else -> CnbClient.DEFAULT_MODEL
        }
    }

    private suspend fun RoutingContext.handleChatCompletions() {
        try {
            val req = call.receive<ChatCompletionRequest>()
            LogStore.i(
                "Proxy",
                "收到请求 | clientStream=${req.stream} upstreamStream=$streamMode model=${req.model} msgs=${req.messages.size}"
            )

            // Kimi 模型 → 走 Kimi 接口（聊天）
            if (isKimiModel(req.model)) {
                val completionId = "chatcmpl-${UUID.randomUUID().toString().take(12)}"
                val created = (System.currentTimeMillis() / 1000).toInt()
                handleKimiChat(req, completionId, created)
                return
            }

            // 智谱（GLM）文本模型 → 走智谱游客接口（免登录）
            if (isZhipuModel(req.model)) {
                val completionId = "chatcmpl-${UUID.randomUUID().toString().take(12)}"
                val created = (System.currentTimeMillis() / 1000).toInt()
                handleZhipuChat(req, completionId, created, isZhipuPlusModel(req.model))
                return
            }

            // LongCat（美团龙猫）模型 → 走 LongCat 接口
            if (isLongCatModel(req.model)) {
                val completionId = "chatcmpl-${UUID.randomUUID().toString().take(12)}"
                val created = (System.currentTimeMillis() / 1000).toInt()
                handleLongCatChat(req, completionId, created)
                return
            }

            // 千问模型 → 走千问接口（普通 / 思考，由模型名决定）
            if (isQwenModel(req.model)) {
                val completionId = "chatcmpl-${UUID.randomUUID().toString().take(12)}"
                val created = (System.currentTimeMillis() / 1000).toInt()
                handleQwenChat(req, completionId, created)
                return
            }

            // DeepSeek 模型 → 走 DeepSeek 接口（含 PoW）
            if (isDeepSeekModel(req.model)) {
                val completionId = "chatcmpl-${UUID.randomUUID().toString().take(12)}"
                val created = (System.currentTimeMillis() / 1000).toInt()
                handleDeepSeekChat(req, completionId, created)
                return
            }

            // 豆包模型 → 走豆包接口（聊天）
            if (isDoubaoModel(req.model)) {
                val completionId = "chatcmpl-${UUID.randomUUID().toString().take(12)}"
                val created = (System.currentTimeMillis() / 1000).toInt()
                handleDoubaoChat(req, completionId, created)
                return
            }

            val model = mapModel(req.model)
            val bodyJson = org.json.JSONObject().apply {
                put("model", model)
                put("stream", streamMode)
                put(
                    "messages",
                    org.json.JSONArray(
                        req.messages.map {
                            org.json.JSONObject().put("role", it.role).put("content", it.content)
                        }
                    )
                )
                req.temperature?.let { put("temperature", it) }
                req.max_tokens?.let { put("max_tokens", it) }
            }

            val completionId = "chatcmpl-${UUID.randomUUID().toString().take(12)}"
            val created = (System.currentTimeMillis() / 1000).toInt()

            if (streamMode) {
                handleStreamUpstream(req, model, bodyJson.toString(), completionId, created)
            } else {
                handleNonStreamUpstream(req, model, bodyJson.toString(), completionId, created)
            }
        } catch (e: Exception) {
            LogStore.e("Proxy", "请求处理失败: ${e.stackTraceToString()}")
            if (!call.response.isSent) {
                try {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to (e.localizedMessage ?: e.toString()))
                    )
                } catch (_: Exception) {
                }
            }
        }
    }

    /** 上游非流式：拿到完整内容后，按客户端要求返回 JSON 或单块 SSE。 */
    private suspend fun RoutingContext.handleNonStreamUpstream(
        req: ChatCompletionRequest, model: String, body: String,
        completionId: String, created: Int
    ) {
        val (content, errMsg) = withContext(Dispatchers.IO) {
            try {
                withTimeout(300_000) {
                    CnbClient.chatCompletion(body).use { resp ->
                        val text = resp.body!!.string()
                        val api = org.json.JSONObject(text)
                        val c = api.getJSONArray("choices").getJSONObject(0)
                            .getJSONObject("message").getString("content")
                        c to null
                    }
                }
            } catch (e: Exception) {
                LogStore.e("Proxy", "CNB 请求异常: ${e.stackTraceToString()}")
                null to (e.localizedMessage ?: e.toString())
            }
        }

        if (errMsg != null) {
            LogStore.e("Proxy", "返回错误给客户端: $errMsg")
            call.respond(HttpStatusCode.BadGateway, mapOf("error" to errMsg))
            return
        }

        val contentText = content ?: ""
        if (req.stream) {
            call.respond(object : OutgoingContent.WriteChannelContent() {
                override val contentType = ContentType.Text.EventStream
                override val headers = Headers.build {
                    append("Cache-Control", "no-cache")
                    append("Connection", "keep-alive")
                    append("X-Accel-Buffering", "no")
                }

                override suspend fun writeTo(channel: ByteWriteChannel) {
                    val sb = StringBuilder()
                    try {
                        val chunk = ChatCompletionChunk(
                            completionId, "chat.completion.chunk", created, model,
                            listOf(ChunkChoice(delta = ChoiceDelta(content = contentText)))
                        )
                        sb.append("data: ")
                            .append(json.encodeToString(ChatCompletionChunk.serializer(), chunk))
                            .append("\n\n")
                        val stop = ChatCompletionChunk(
                            completionId, "chat.completion.chunk", created, model,
                            listOf(ChunkChoice(delta = ChoiceDelta(), finish_reason = "stop"))
                        )
                        sb.append("data: ")
                            .append(json.encodeToString(ChatCompletionChunk.serializer(), stop))
                            .append("\n\n")
                        sb.append("data: [DONE]\n\n")
                        channel.writeFully(sb.toString().toByteArray())
                        channel.flush()
                    } catch (e: Exception) {
                        LogStore.e("Proxy", "流式写出异常: ${e.stackTraceToString()}")
                    }
                }
            })
        } else {
            LogStore.i("Proxy", "返回完成结果，长度=${contentText.length}")
            call.respond(
                ChatCompletion(
                    completionId, "chat.completion", created, model,
                    listOf(Choice(message = Msg("assistant", contentText), finish_reason = "stop")),
                    Usage(), ""
                )
            )
        }
    }

    /** 上游流式：直接透传 CNB 的 SSE；若客户端要非流式则累加后返回 JSON。 */
    private suspend fun RoutingContext.handleStreamUpstream(
        req: ChatCompletionRequest, model: String, body: String,
        completionId: String, created: Int
    ) {
        if (!req.stream) {
            val acc = StringBuilder()
            val errMsg = withContext(Dispatchers.IO) {
                try {
                    withTimeout(300_000) {
                        CnbClient.chatCompletion(body).use { resp ->
                            val src = resp.body!!.source()
                            parseCnbSse(src) { acc.append(it) }
                            null
                        }
                    }
                } catch (e: Exception) {
                    LogStore.e("Proxy", "CNB 流式请求异常: ${e.stackTraceToString()}")
                    e.localizedMessage ?: e.toString()
                }
            }
            if (errMsg != null) {
                call.respond(HttpStatusCode.BadGateway, mapOf("error" to errMsg))
                return
            }
            call.respond(
                ChatCompletion(
                    completionId, "chat.completion", created, model,
                    listOf(Choice(message = Msg("assistant", acc.toString()), finish_reason = "stop")),
                    Usage(), ""
                )
            )
            return
        }

        call.respond(object : OutgoingContent.WriteChannelContent() {
            override val contentType = ContentType.Text.EventStream
            override val headers = Headers.build {
                append("Cache-Control", "no-cache")
                append("Connection", "keep-alive")
                append("X-Accel-Buffering", "no")
            }

            override suspend fun writeTo(channel: ByteWriteChannel) {
                withContext(Dispatchers.IO) {
                    try {
                        withTimeout(300_000) {
                            CnbClient.chatCompletion(body).use { resp ->
                                val src = resp.body!!.source()
                                while (true) {
                                    val line = src.readUtf8Line() ?: break
                                    channel.writeFully((line + "\n").toByteArray())
                                    if (line.isEmpty()) channel.flush()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        LogStore.e("Proxy", "流式转发异常: ${e.stackTraceToString()}")
                    }
                }
            }
        })
    }

    /** 解析 CNB 的 SSE，逐块回调 delta.content（多行 data 字段会拼接）。 */
    private fun parseCnbSse(src: okio.BufferedSource, onDelta: (String) -> Unit) {
        val data = StringBuilder()
        while (true) {
            val line = src.readUtf8Line() ?: break
            if (line.isEmpty()) {
                val payload = data.toString().trim()
                data.setLength(0)
                if (payload.isNotEmpty() && payload != "[DONE]") {
                    try {
                        val o = org.json.JSONObject(payload)
                        val choices = o.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val delta = choices.getJSONObject(0).optJSONObject("delta")
                            delta?.optString("content")?.let { if (it.isNotEmpty()) onDelta(it) }
                        }
                    } catch (_: Exception) {
                    }
                }
            } else if (line.startsWith("data:")) {
                data.append(line.substring(5).trimStart()).append("\n")
            }
        }
    }

    // ── 豆包（Doubao）相关 ───────────────────────────────────

    private fun isDoubaoModel(m: String): Boolean {
        val lower = m.lowercase()
        return DOUBAO_MODELS.any { lower.startsWith(it) || lower.contains(it) }
    }

    private fun isKimiModel(m: String): Boolean {
        val lower = m.lowercase()
        return KIMI_MODELS.any { lower.startsWith(it) || lower.contains(it) }
    }

    private fun isQwenModel(m: String): Boolean {
        val lower = m.lowercase()
        return QWEN_MODELS.any { lower.startsWith(it) || lower.contains(it) }
    }

    private fun isDeepSeekModel(m: String): Boolean {
        val lower = m.lowercase()
        return DEEPSEEK_MODELS.any { lower.startsWith(it) || lower.contains(it) }
    }

    /** 是否 LongCat（美团龙猫）模型。 */
    private fun isLongCatModel(m: String): Boolean {
        val lower = m.lowercase()
        return LONGCAT_MODELS.any { lower.startsWith(it) || lower.contains(it) }
    }

    /**
     * 是否智谱文本模型（免登录）。
     *  - 本地接口：model 名以 gml/zhipu 开头或包含，或恰为 "glm"（不匹配 "glm-5.2" 等透传给 CNB 的名字）
     *  - UI 入口：恰为 "zhipu"（即界面「智谱」）
     */
    private fun isZhipuModel(m: String): Boolean {
        val lower = m.lowercase()
        if (lower == "zhipu") return true
        if (lower == "glm") return true
        // 「智谱-电影级生图」(glm-image-hd) 选项：发文本也归智谱文本模型
        if (isZhipuCinematicImageModel(lower)) return true
        return ZHIPU_MODELS.any { lower.startsWith(it) || lower.contains(it) }
    }

    /** 是否智谱生图模型（免登录）。UI「智谱」(zhipu) 点生图同样命中。 */
    private fun isZhipuImageModel(m: String): Boolean {
        val lower = m.lowercase()
        if (lower == "zhipu") return true
        return ZHIPU_IMAGE_MODELS.any { lower == it || lower.startsWith(it) || lower.contains(it) }
    }

    /** 是否智谱电影级生图模型（glm-image-hd）：走 GLM Image 电影级模式。 */
    private fun isZhipuCinematicImageModel(m: String): Boolean {
        val lower = m.lowercase()
        return lower.contains("glm-image-hd") || lower.contains("gml-image-hd")
    }

    /** 是否智谱增强（plus/思考）模型：model 名含 plus/thinking/reason 即走增强模型。
     *  另：「智谱-电影级生图」(glm-image-hd) 选项发文本时也走思考模式。 */
    private fun isZhipuPlusModel(m: String): Boolean {
        val lower = m.lowercase()
        if (isZhipuCinematicImageModel(lower)) return true
        return lower.contains("plus") || lower.contains("thinking") || lower.contains("reason")
    }

    /**
     * 将全部聊天消息拼成单段文本（对齐 doubao2api._extract_prompt）：
     * 每条消息格式为 "[role]: content"，用换行连接，保留 system 提示与多轮上下文。
     * 仅单条消息时直接使用其内容，避免多余的 "[role]:" 前缀。
     */
    private fun extractDoubaoPrompt(messages: List<ChatMessage>): String {
        if (messages.size == 1) return messages[0].content
        return messages.joinToString("\n") { "[${it.role}]: ${it.content}" }
    }

    /** OpenAI size（如 1024x1024）映射到豆包 ratio。 */
    private fun sizeToRatio(size: String): String? {
        return when (size) {
            "768x768", "1024x1024", "512x512", "256x256" -> "1:1"
            "768x1152", "1024x1792", "1024x1536" -> "9:16"
            "1152x768", "1792x1024", "1536x1024" -> "16:9"
            "1056x768", "1024x768" -> "4:3"
            "768x1024" -> "3:4"
            "768x1920" -> "2:5"
            "1920x768" -> "5:2"
            else -> null
        }
    }

    /** 解析生图比例：原始 ratio（如 9:16）直接返回，否则按 OpenAI size 映射，兜底 9:16。 */
    private fun resolveRatio(size: String): String {
        if (size.isBlank()) return "9:16"
        if (size.contains(":")) return size
        return sizeToRatio(size) ?: "9:16"
    }

    /**
     * 豆包聊天：上游恒为 SSE 流式。
     *  - 客户端要求流式 → 边解析边转发 OpenAI 格式 chunk
     *  - 客户端要求非流式 → 累加完整文本后返回单个 JSON
     */
    /** 豆包模型名 → need_deep_think（对齐 doubao2api CHAT_MODELS：0 极速/1 思考/2 自动/3 专家）。 */
    private fun doubaoDeepThink(model: String): Int {
        return when (model.lowercase()) {
            "doubao-think" -> 1
            "doubao-auto" -> 2
            "doubao-expert", "doubao-pro" -> 3
            else -> 0
        }
    }

    private suspend fun RoutingContext.handleDoubaoChat(
        req: ChatCompletionRequest, completionId: String, created: Int
    ) {
        val cookies = DoubaoClient.parseCookie(nextDoubaoCookie())
        val text = extractDoubaoPrompt(req.messages)
        val needDeep = doubaoDeepThink(req.model)
        LogStore.i("Doubao", "调用 /chat/completion，输入长度=${text.length} needDeepThink=$needDeep")

        if (req.stream) {
            call.respond(object : OutgoingContent.WriteChannelContent() {
                override val contentType = ContentType.Text.EventStream
                override val headers = Headers.build {
                    append("Cache-Control", "no-cache")
                    append("Connection", "keep-alive")
                    append("X-Accel-Buffering", "no")
                }

                override suspend fun writeTo(channel: ByteWriteChannel) {
                    withContext(Dispatchers.IO) {
                        try {
                            withTimeout(300_000) {
                                val resp = DoubaoClient.chatCompletion(text, cookies, needDeep)
                                try {
                                    parseDoubaoSse(resp.body!!.source()) { delta ->
                                        val chunk = ChatCompletionChunk(
                                            completionId, "chat.completion.chunk", created, req.model,
                                            listOf(ChunkChoice(delta = ChoiceDelta(content = delta)))
                                        )
                                        val sb = StringBuilder()
                                        sb.append("data: ")
                                            .append(json.encodeToString(ChatCompletionChunk.serializer(), chunk))
                                            .append("\n\n")
                                        channel.writeFully(sb.toString().toByteArray())
                                        channel.flush()
                                    }
                                } finally {
                                    resp.close()
                                }
                                channel.writeFully("data: [DONE]\n\n".toByteArray())
                                channel.flush()
                            }
                        } catch (e: Exception) {
                            LogStore.e("Doubao", "流式转发异常: ${e.stackTraceToString()}")
                        }
                    }
                }
            })
            return
        }

        val (content, errMsg) = withContext(Dispatchers.IO) {
            try {
                withTimeout(300_000) {
                    val resp = DoubaoClient.chatCompletion(text, cookies, needDeep)
                    try {
                        val sb = StringBuilder()
                        parseDoubaoSse(resp.body!!.source()) { sb.append(it) }
                        sb.toString() to null
                    } finally {
                        resp.close()
                    }
                }
            } catch (e: Exception) {
                LogStore.e("Doubao", "请求异常: ${e.stackTraceToString()}")
                null to (e.localizedMessage ?: e.toString())
            }
        }

        if (errMsg != null) {
            LogStore.e("Doubao", "返回错误给客户端: $errMsg")
            call.respond(HttpStatusCode.BadGateway, mapOf("error" to errMsg))
            return
        }

        val contentText = content ?: ""
        LogStore.i("Doubao", "返回完成结果，长度=${contentText.length}")
        call.respond(
            ChatCompletion(
                completionId, "chat.completion", created, req.model,
                listOf(Choice(message = Msg("assistant", contentText), finish_reason = "stop")),
                Usage(), ""
            )
        )
    }

    /**
     * 智谱（GLM）聊天：游客模式（免登录）。上游恒为 SSE 流式，但智谱每次推送的是
     * 「累积完整文本」而非增量 delta，故本地计算增量后转发为 OpenAI 格式 chunk。
     */
    private suspend fun RoutingContext.handleZhipuChat(
        req: ChatCompletionRequest, completionId: String, created: Int, plus: Boolean = false
    ) {
        // 与 DeepSeek 一致：把 system 人设 + 全部历史对话拼成一个完整 prompt 一并发送
        val text = extractDeepSeekPrompt(req.messages)
        LogStore.i("Zhipu", "调用 /assistant/stream，输入长度=${text.length} 消息数=${req.messages.size} plus=$plus")

        if (req.stream) {
            call.respond(object : OutgoingContent.WriteChannelContent() {
                override val contentType = ContentType.Text.EventStream
                override val headers = Headers.build {
                    append("Cache-Control", "no-cache")
                    append("Connection", "keep-alive")
                    append("X-Accel-Buffering", "no")
                }

                override suspend fun writeTo(channel: ByteWriteChannel) {
                    withContext(Dispatchers.IO) {
                        try {
                            withTimeout(300_000) {
                                val resp = ZhipuClient.chatCompletion(text, plusModel = plus)
                                try {
                                    parseZhipuSse(resp.body!!.source()) { delta ->
                                        val chunk = ChatCompletionChunk(
                                            completionId, "chat.completion.chunk", created, req.model,
                                            listOf(ChunkChoice(delta = ChoiceDelta(content = delta)))
                                        )
                                        val sb = StringBuilder()
                                        sb.append("data: ")
                                            .append(json.encodeToString(ChatCompletionChunk.serializer(), chunk))
                                            .append("\n\n")
                                        channel.writeFully(sb.toString().toByteArray())
                                        channel.flush()
                                    }
                                } finally {
                                    resp.close()
                                }
                                channel.writeFully("data: [DONE]\n\n".toByteArray())
                                channel.flush()
                            }
                        } catch (e: Exception) {
                            LogStore.e("Zhipu", "流式转发异常: ${e.stackTraceToString()}")
                        }
                    }
                }
            })
            return
        }

        val (content, errMsg) = withContext(Dispatchers.IO) {
            try {
                withTimeout(300_000) {
                    val resp = ZhipuClient.chatCompletion(text, plusModel = plus)
                    try {
                        val sb = StringBuilder()
                        parseZhipuSse(resp.body!!.source()) { sb.append(it) }
                        sb.toString() to null
                    } finally {
                        resp.close()
                    }
                }
            } catch (e: Exception) {
                LogStore.e("Zhipu", "请求异常: ${e.stackTraceToString()}")
                null to (e.localizedMessage ?: e.toString())
            }
        }

        if (errMsg != null) {
            LogStore.e("Zhipu", "返回错误给客户端: $errMsg")
            call.respond(HttpStatusCode.BadGateway, mapOf("error" to errMsg))
            return
        }

        val contentText = content ?: ""
        LogStore.i("Zhipu", "返回完成结果，长度=${contentText.length}")
        call.respond(
            ChatCompletion(
                completionId, "chat.completion", created, req.model,
                listOf(Choice(message = Msg("assistant", contentText), finish_reason = "stop")),
                Usage(), ""
            )
        )
    }

    /**
     * LongCat（美团龙猫）聊天：上游恒为 SSE 流式。
     *  - 客户端要求流式 → 边解析边转发 OpenAI 格式 chunk
     *  - 客户端要求非流式 → 聚合完整文本后返回单个 JSON
     */
    private suspend fun RoutingContext.handleLongCatChat(
        req: ChatCompletionRequest, completionId: String, created: Int
    ) {
        // 每次请求轮换到下一个龙猫配置（多账号），并应用到 LongCatClient
        applyNextLongCatConfig()
        if (LongCatClient.mtgsig.isBlank() || LongCatClient.cookies.isBlank()) {
            call.respond(
                HttpStatusCode.BadGateway,
                mapOf("error" to "LongCat 未配置：请在「中转」页选择龙猫并填写抓包得到的配置(JSON)")
            )
            return
        }
        val model = req.model
        LogStore.i("LongCat", "调用 chat-completion-V2，消息数=${req.messages.size} model=$model")

        if (req.stream) {
            call.respond(object : OutgoingContent.WriteChannelContent() {
                override val contentType = ContentType.Text.EventStream
                override val headers = Headers.build {
                    append("Cache-Control", "no-cache")
                    append("Connection", "keep-alive")
                    append("X-Accel-Buffering", "no")
                }

                override suspend fun writeTo(channel: ByteWriteChannel) {
                    withContext(Dispatchers.IO) {
                        try {
                            withTimeout(300_000) {
                                LongCatClient.chatCompletion(req.messages, model).use { resp ->
                                    parseLongCatSse(resp.body!!.source()) { delta ->
                                        val chunk = ChatCompletionChunk(
                                            completionId, "chat.completion.chunk", created, model,
                                            listOf(ChunkChoice(delta = ChoiceDelta(content = delta)))
                                        )
                                        channel.writeFully(
                                            ("data: " + json.encodeToString(ChatCompletionChunk.serializer(), chunk) + "\n\n").toByteArray()
                                        )
                                        channel.flush()
                                    }
                                }
                                channel.writeFully(
                                    ("data: " + json.encodeToString(
                                        ChatCompletionChunk.serializer(),
                                        ChatCompletionChunk(
                                            completionId, "chat.completion.chunk", created, model,
                                            listOf(ChunkChoice(delta = ChoiceDelta(), finish_reason = "stop"))
                                        )
                                    ) + "\n\n").toByteArray()
                                )
                                channel.writeFully("data: [DONE]\n\n".toByteArray())
                                channel.flush()
                            }
                        } catch (e: Exception) {
                            LogStore.e("LongCat", "流式转发异常: ${e.stackTraceToString()}")
                        }
                    }
                }
            })
            return
        }

        val (content, errMsg) = withContext(Dispatchers.IO) {
            try {
                withTimeout(300_000) {
                    LongCatClient.chatCompletion(req.messages, model).use { resp ->
                        val sb = StringBuilder()
                        parseLongCatSse(resp.body!!.source()) { sb.append(it) }
                        sb.toString() to null
                    }
                }
            } catch (e: Exception) {
                LogStore.e("LongCat", "请求异常: ${e.stackTraceToString()}")
                null to (e.localizedMessage ?: e.toString())
            }
        }

        if (errMsg != null) {
            call.respond(HttpStatusCode.BadGateway, mapOf("error" to errMsg))
            return
        }

        val contentText = content ?: ""
        LogStore.i("LongCat", "返回完成结果，长度=${contentText.length}")
        call.respond(
            ChatCompletion(
                completionId, "chat.completion", created, model,
                listOf(Choice(message = Msg("assistant", contentText), finish_reason = "stop")),
                Usage(), ""
            )
        )
    }

    /** 解析 LongCat SSE：event.type=content（status!=FINISHED）累加为增量；finish 事件的 finalContentX 作为兜底完整文本。 */
    private suspend fun parseLongCatSse(src: okio.BufferedSource, onDelta: suspend (String) -> Unit) {
        val data = StringBuilder()
        var emitted = false
        while (true) {
            val line = src.readUtf8Line() ?: break
            if (line.isEmpty()) {
                val payload = data.toString().trim()
                data.setLength(0)
                if (payload.isEmpty() || payload == "[DONE]") continue
                try {
                    val o = org.json.JSONObject(payload)
                    val event = o.optJSONObject("event")
                    val etype = event?.optString("type", "") ?: ""
                    if (etype == "content") {
                        val c = event?.optString("content", "") ?: ""
                        val status = event?.optString("status", "") ?: ""
                        if (c.isNotEmpty() && status != "FINISHED") {
                            emitted = true
                            onDelta(c)
                        }
                    } else if (etype == "finish") {
                        val fcx = event?.optString("finalContentX", "") ?: ""
                        if (fcx.isNotEmpty() && !emitted) onDelta(fcx)
                    }
                } catch (_: Exception) {
                }
            } else if (line.startsWith("data:")) {
                data.append(line.substring(5).trimStart()).append("\n")
            }
        }
    }

    /** 解析智谱 SSE：上游推送累积完整文本，本地计算增量（delta = newFull 去掉 prevFull 前缀）后回调。 */
    private suspend fun parseZhipuSse(src: okio.BufferedSource, onDelta: suspend (String) -> Unit) {
        var prevFull = ""
        val data = StringBuilder()
        while (true) {
            val line = src.readUtf8Line() ?: break
            if (line.isEmpty()) {
                val payload = data.toString().trim()
                data.setLength(0)
                if (payload.isEmpty() || payload == "[DONE]") continue
                try {
                    val o = org.json.JSONObject(payload)
                    val parts = o.optJSONArray("parts") ?: continue
                    var full = prevFull
                    for (i in 0 until parts.length()) {
                        val part = parts.optJSONObject(i) ?: continue
                        val content = part.optJSONArray("content") ?: continue
                        for (j in 0 until content.length()) {
                            val c = content.optJSONObject(j) ?: continue
                            if (c.optString("type") == "text") {
                                val t = c.optString("text", "")
                                if (t.isNotEmpty()) full = t
                            }
                        }
                    }
                    if (full.length > prevFull.length) {
                        onDelta(full.substring(prevFull.length))
                        prevFull = full
                    }
                } catch (_: Exception) {
                }
            } else if (line.startsWith("data:")) {
                data.append(line.substring(5).trimStart()).append("\n")
            }
        }
    }

    /** 生图接口：POST /v1/images/generations → 豆包生图 / 智谱生图（按 model 区分）。 */
    private suspend fun RoutingContext.handleImageGen() {
        try {
            val raw = call.receiveText()
            val o = org.json.JSONObject(raw)
            val prompt = o.optString("prompt", "")
            val size = o.optString("size", "")
            val model = o.optString("model", "")
            // 外部指定且可识别 → 用之；否则用设置界面尺寸；再不行兜底 9:16 竖版
            val ratio = resolveRatio(if (size.isNotBlank()) size else defaultImageSize)

            // 智谱生图（免登录）：model 命中智谱生图 / UI「智谱」(zhipu) → 走智谱，只返回第一张
            if (isZhipuImageModel(model)) {
                val cinematic = isZhipuCinematicImageModel(model)
                LogStore.i("Zhipu", "调用生图，prompt=${prompt.take(60)} ratio=$ratio 电影级=$cinematic")
                val (urls, errMsg) = withContext(Dispatchers.IO) {
                    try {
                        ZhipuClient.generateImage(prompt, ratio, cinematic) to null
                    } catch (e: Exception) {
                        LogStore.e("Zhipu", "生图异常: ${e.stackTraceToString()}")
                        emptyList<String>() to (e.localizedMessage ?: e.toString())
                    }
                }
                if (errMsg != null) {
                    call.respond(HttpStatusCode.BadGateway, mapOf("error" to errMsg))
                    return
                }
                if (urls.isEmpty()) {
                    call.respond(HttpStatusCode.BadGateway, mapOf("error" to "智谱生图未返回图片"))
                    return
                }
                // 仅返回第一张
                call.respond(
                    ImageGenResponse(
                        created = (System.currentTimeMillis() / 1000).toInt(),
                        data = listOf(ImageDataItem(url = urls.first()))
                    )
                )
                return
            }

            val cookies = DoubaoClient.parseCookie(nextDoubaoCookie())
            LogStore.i("Doubao", "调用生图，prompt=${prompt.take(60)} ratio=$ratio")

            val (result, errMsg) = withContext(Dispatchers.IO) {
                try {
                    DoubaoClient.generateImage(prompt, cookies, ratio) to null
                } catch (e: Exception) {
                    LogStore.e("Doubao", "生图异常: ${e.stackTraceToString()}")
                    null to (e.localizedMessage ?: e.toString())
                }
            }
            if (errMsg != null) {
                call.respond(HttpStatusCode.BadGateway, mapOf("error" to errMsg))
                return
            }
            val data = (result?.images ?: emptyList()).map {
                ImageDataItem(url = if (it.rawUrl.isNotBlank()) it.rawUrl else it.oriUrl)
            }
            if (data.isEmpty()) {
                call.respond(HttpStatusCode.BadGateway, mapOf("error" to "豆包生图未返回图片"))
                return
            }
            call.respond(
                ImageGenResponse(
                    created = (System.currentTimeMillis() / 1000).toInt(),
                    data = data
                )
            )
        } catch (e: Exception) {
            LogStore.e("Doubao", "生图处理失败: ${e.stackTraceToString()}")
            if (!call.response.isSent) {
                try {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to (e.localizedMessage ?: e.toString()))
                    )
                } catch (_: Exception) {
                }
            }
        }
    }

    /**
     * Kimi 聊天：上游恒为 Connect 二进制帧流（见 KimiClient）。
     *  - 客户端要求流式 → 边解析帧边转发 OpenAI 格式 chunk
     *  - 客户端要求非流式 → 聚合完整文本后返回单个 JSON
     */
    private suspend fun RoutingContext.handleKimiChat(
        req: ChatCompletionRequest, completionId: String, created: Int
    ) {
        val rawToken = KimiClient.extractToken(nextKimiCookie())
        LogStore.i("Kimi", "调用 /Chat，消息数=${req.messages.size} token空白=${rawToken.isBlank()}")

        if (req.stream) {
            call.respond(object : OutgoingContent.WriteChannelContent() {
                override val contentType = ContentType.Text.EventStream
                override val headers = Headers.build {
                    append("Cache-Control", "no-cache")
                    append("Connection", "keep-alive")
                    append("X-Accel-Buffering", "no")
                }

                override suspend fun writeTo(channel: ByteWriteChannel) {
                    withContext(Dispatchers.IO) {
                        try {
                            withTimeout(300_000) {
                                val resp = KimiClient.chatCompletion(req.messages, rawToken)
                                try {
                                    val roleChunk = ChatCompletionChunk(
                                        completionId, "chat.completion.chunk", created, req.model,
                                        listOf(ChunkChoice(delta = ChoiceDelta(role = "assistant")))
                                    )
                                    channel.writeFully(
                                        ("data: " + json.encodeToString(ChatCompletionChunk.serializer(), roleChunk) + "\n\n").toByteArray()
                                    )
                                    channel.flush()

                                    var chatId: String? = null
                                    KimiClient.decodeFrames(
                                        resp.body!!.source(),
                                        onText = { t ->
                                            val chunk = ChatCompletionChunk(
                                                completionId, "chat.completion.chunk", created, req.model,
                                                listOf(ChunkChoice(delta = ChoiceDelta(content = t)))
                                            )
                                            channel.writeFully(
                                                ("data: " + json.encodeToString(ChatCompletionChunk.serializer(), chunk) + "\n\n").toByteArray()
                                            )
                                            channel.flush()
                                        },
                                        onChatId = { chatId = it }
                                    )
                                    channel.writeFully(
                                        ("data: " + json.encodeToString(
                                            ChatCompletionChunk.serializer(),
                                            ChatCompletionChunk(
                                                completionId, "chat.completion.chunk", created, req.model,
                                                listOf(ChunkChoice(delta = ChoiceDelta(), finish_reason = "stop"))
                                            )
                                        ) + "\n\n").toByteArray()
                                    )
                                    channel.writeFully("data: [DONE]\n\n".toByteArray())
                                    channel.flush()
                                    if (deleteSessionAfterReply) chatId?.let { KimiClient.deleteChat(it, rawToken) }
                                } finally {
                                    resp.close()
                                }
                            }
                        } catch (e: Exception) {
                            LogStore.e("Kimi", "流式转发异常: ${e.stackTraceToString()}")
                        }
                    }
                }
            })
            return
        }

        val (content, errMsg) = withContext(Dispatchers.IO) {
            try {
                withTimeout(300_000) {
                    val resp = KimiClient.chatCompletion(req.messages, rawToken)
                    try {
                        val sb = StringBuilder()
                        var chatId: String? = null
                        KimiClient.decodeFrames(
                            resp.body!!.source(),
                            onText = { sb.append(it) },
                            onChatId = { chatId = it }
                        )
                        if (deleteSessionAfterReply) chatId?.let { KimiClient.deleteChat(it, rawToken) }
                        sb.toString() to null
                    } finally {
                        resp.close()
                    }
                }
            } catch (e: Exception) {
                LogStore.e("Kimi", "请求异常: ${e.stackTraceToString()}")
                null to (e.localizedMessage ?: e.toString())
            }
        }

        if (errMsg != null) {
            LogStore.e("Kimi", "返回错误给客户端: $errMsg")
            call.respond(HttpStatusCode.BadGateway, mapOf("error" to errMsg))
            return
        }

        val contentText = content ?: ""
        if (contentText.isBlank()) {
            call.respond(HttpStatusCode.BadGateway, mapOf("error" to "Kimi 未返回内容"))
            return
        }
        LogStore.i("Kimi", "返回完成结果，长度=${contentText.length}")
        call.respond(
            ChatCompletion(
                completionId, "chat.completion", created, req.model,
                listOf(Choice(message = Msg("assistant", contentText), finish_reason = "stop")),
                Usage(), ""
            )
        )
    }

    /**
     * 将 OpenAI 格式 messages 拼成单个 prompt（含 system 人设 + 全部历史对话）。
     * 与豆包策略一致：多轮用 `[role]: content` 拼接；单条直接返回；system 作为前置人设。
     */
    private fun extractDeepSeekPrompt(messages: List<ChatMessage>): String {
        if (messages.isEmpty()) return ""
        val system = messages.firstOrNull { it.role == "system" }?.content?.takeIf { it.isNotBlank() }
        val rest = messages.filter { it.role != "system" }
        val sb = StringBuilder()
        if (system != null) sb.append(system).append("\n\n")
        when {
            rest.isEmpty() -> return system ?: ""
            rest.size == 1 -> sb.append(rest[0].content)
            else -> sb.append(rest.joinToString("\n\n") { "[${it.role}]: ${it.content}" })
        }
        return sb.toString().trim().ifBlank { messages.lastOrNull { it.role == "user" }?.content ?: "" }
    }

    /**
     * DeepSeek 聊天：每次请求需完成一次 PoW（见 DeepSeekPow），再把答案写入
     * `x-ds-pow-response` 头。上游恒为 SSE 流式。
     *  - 客户端要求流式 → 边解析边转发 OpenAI 格式 chunk
     *  - 客户端要求非流式 → 聚合完整文本后返回单个 JSON
     */
    private suspend fun RoutingContext.handleDeepSeekChat(
        req: ChatCompletionRequest, completionId: String, created: Int
    ) {
        val prompt = extractDeepSeekPrompt(req.messages)
        if (prompt.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "empty prompt"))
            return
        }

        // 根据客户端请求里的 model 名字决定 DeepSeek 网页端 model_type：
        //   deepseek-pro / deepseek-reasoner / deepseek-expert / deepseek-think / deepseek-r1 → 专家模式(expert)
        //   其余（deepseek / deepseek-chat，或不带以上关键字）→ 沿用 UI 设定的 deepseekMode（默认 default）
        val lowerModel = req.model.lowercase()
        val modelType = if (lowerModel.contains("pro") || lowerModel.contains("reasoner")
            || lowerModel.contains("expert") || lowerModel.contains("think") || lowerModel.contains("r1")
        ) "expert" else deepseekMode
        LogStore.i("DeepSeek", "model=${req.model} → model_type=$modelType")

        LogStore.i("DeepSeek", "调用 /chat/completion，输入长度=${prompt.length}")

        if (req.stream) {
            call.respond(object : OutgoingContent.WriteChannelContent() {
                override val contentType = ContentType.Text.EventStream
                override val headers = Headers.build {
                    append("Cache-Control", "no-cache")
                    append("Connection", "keep-alive")
                    append("X-Accel-Buffering", "no")
                }

                override suspend fun writeTo(channel: ByteWriteChannel) {
                    withContext(Dispatchers.IO) {
                        try {
                            withTimeout(300_000) {
                                DeepSeekClient.token = nextDeepSeekToken()
                                val sessionId = DeepSeekClient.createSession()
                                val ch = DeepSeekClient.createPowChallenge("/api/v0/chat/completion")
                                val answer = DeepSeekPow.solve(ch.challenge, ch.salt, ch.expireAt, ch.difficulty)
                                val powHeader = DeepSeekClient.buildPowHeader(ch, answer, "/api/v0/chat/completion")
                                DeepSeekClient.chatCompletion(sessionId, prompt, powHeader, modelType).use { resp ->
                                    val roleChunk = ChatCompletionChunk(
                                        completionId, "chat.completion.chunk", created, req.model,
                                        listOf(ChunkChoice(delta = ChoiceDelta(role = "assistant")))
                                    )
                                    channel.writeFully(
                                        ("data: " + json.encodeToString(ChatCompletionChunk.serializer(), roleChunk) + "\n\n").toByteArray()
                                    )
                                    channel.flush()

                                    // 流式逐块转发：parseSse 每解析到一个 v 字段就立即回传给本地接口。
                                    // 现在解析正确（仅取 v 字段），无需等完整响应，流式更实时。
                                    DeepSeekClient.parseSse(resp.body!!.source()) { t ->
                                        if (t.isNotEmpty()) {
                                            val chunk = ChatCompletionChunk(
                                                completionId, "chat.completion.chunk", created, req.model,
                                                listOf(ChunkChoice(delta = ChoiceDelta(content = t)))
                                            )
                                            channel.writeFully(
                                                ("data: " + json.encodeToString(ChatCompletionChunk.serializer(), chunk) + "\n\n").toByteArray()
                                            )
                                            channel.flush()
                                        }
                                    }
                                    channel.writeFully(
                                        ("data: " + json.encodeToString(
                                            ChatCompletionChunk.serializer(),
                                            ChatCompletionChunk(
                                                completionId, "chat.completion.chunk", created, req.model,
                                                listOf(ChunkChoice(delta = ChoiceDelta(), finish_reason = "stop"))
                                            )
                                        ) + "\n\n").toByteArray()
                                    )
                                    channel.writeFully("data: [DONE]\n\n".toByteArray())
                                    channel.flush()
                                }
                                if (deleteSessionAfterReply) DeepSeekClient.deleteSession(sessionId)
                            }
                        } catch (e: Exception) {
                            LogStore.e("DeepSeek", "流式转发异常: ${e.stackTraceToString()}")
                        }
                    }
                }
            })
            return
        }

        val (content, errMsg) = withContext(Dispatchers.IO) {
            try {
                withTimeout(300_000) {
                    DeepSeekClient.token = nextDeepSeekToken()
                    val sessionId = DeepSeekClient.createSession()
                    val ch = DeepSeekClient.createPowChallenge("/api/v0/chat/completion")
                    val answer = DeepSeekPow.solve(ch.challenge, ch.salt, ch.expireAt, ch.difficulty)
                    val powHeader = DeepSeekClient.buildPowHeader(ch, answer, "/api/v0/chat/completion")
                    val dsPair = DeepSeekClient.chatCompletion(sessionId, prompt, powHeader, modelType).use { resp ->
                        val sb = StringBuilder()
                        DeepSeekClient.parseSse(resp.body!!.source()) { sb.append(it) }
                        sb.toString() to null
                    }
                    if (deleteSessionAfterReply) DeepSeekClient.deleteSession(sessionId)
                    dsPair
                }
            } catch (e: Exception) {
                LogStore.e("DeepSeek", "请求异常: ${e.stackTraceToString()}")
                null to (e.localizedMessage ?: e.toString())
            }
        }

        if (errMsg != null) {
            LogStore.e("DeepSeek", "返回错误给客户端: $errMsg")
            call.respond(HttpStatusCode.BadGateway, mapOf("error" to errMsg))
            return
        }

        val contentText = content ?: ""
        LogStore.i("DeepSeek", "返回完成结果，长度=${contentText.length}")
        call.respond(
            ChatCompletion(
                completionId, "chat.completion", created, req.model,
                listOf(Choice(message = Msg("assistant", contentText), finish_reason = "stop")),
                Usage(), ""
            )
        )
    }

    /**
     * 解析豆包 SSE，逐块回调文本增量（思考/正文均作为 content 输出）。
     * 对齐 doubao2api 的 iter_sse_events：同时解析 event: 与 data: 行，
     * 处理 gateway-error / STREAM_ERROR 事件，并提取 block_type 10040（深度思考）。
     */
    private suspend fun parseDoubaoSse(src: okio.BufferedSource, onDelta: suspend (String) -> Unit) {
        val data = StringBuilder()
        var eventName = "message"
        while (true) {
            val line = src.readUtf8Line() ?: break
            if (line.isEmpty()) {
                val payload = data.toString().trim()
                val ev = eventName
                data.setLength(0)
                eventName = "message"
                if (payload.isEmpty()) continue
                try {
                    val obj = org.json.JSONObject(payload)
                    // 网关级错误（event: gateway-error，登录失效/用户失效）
                    if (ev == "gateway-error") {
                        val code = obj.optString("code", "")
                        val msg = obj.optString("message", payload.take(200))
                        throw RuntimeException("豆包网关错误: code=$code message=$msg")
                    }
                    // STREAM_ERROR 事件或 data 内 error_code 限流/业务错误
                    if (ev == "STREAM_ERROR" || obj.has("error_code")) {
                        val code = obj.optInt("error_code", 0)
                        if (code != 0) {
                            val msg = obj.optString("error_msg", "")
                            if (code == 710022002 || code == 710022004) {
                                throw RuntimeException("豆包限流($code): $msg")
                            }
                            LogStore.e("Doubao", "error_code=$code $msg")
                            return
                        }
                    }
                    // 直接文本字段
                    if (obj.has("text") && obj.optString("text", "").isNotEmpty()) {
                        onDelta(obj.getString("text"))
                    }
                    // content_block 解析
                    val blocks = mutableListOf<org.json.JSONObject>()
                    val patchOps = obj.optJSONArray("patch_op")
                    if (patchOps != null) {
                        for (i in 0 until patchOps.length()) {
                            val pv = patchOps.optJSONObject(i)?.optJSONObject("patch_value") ?: continue
                            val cb = pv.optJSONArray("content_block") ?: continue
                            for (j in 0 until cb.length()) blocks.add(cb.getJSONObject(j))
                        }
                    }
                    val contentObj = obj.optJSONObject("content")
                    if (contentObj != null) {
                        val cb = contentObj.optJSONArray("content_block")
                        if (cb != null) for (j in 0 until cb.length()) blocks.add(cb.getJSONObject(j))
                    }
                    for (cb in blocks) {
                        val bt = cb.optInt("block_type", 0)
                        val c = cb.optJSONObject("content") ?: org.json.JSONObject()
                        when (bt) {
                            10000 -> {
                                val t = c.optJSONObject("text_block")?.optString("text", "")
                                if (!t.isNullOrEmpty()) onDelta(t)
                            }
                            10040 -> { // 深度思考（思考过程）同样作为内容输出
                                val t = c.optJSONObject("text_block")?.optString("text", "")
                                if (!t.isNullOrEmpty()) onDelta(t)
                            }
                            10101 -> {
                                val t = c.optJSONObject("loading_block")?.optJSONObject("text_loading")?.optString("text", "")
                                if (!t.isNullOrEmpty()) onDelta(t)
                            }
                            10024 -> {
                                val t = c.optJSONObject("generic_tool_block")?.optString("title", "")
                                if (!t.isNullOrEmpty()) onDelta("[tool] $t")
                            }
                        }
                    }
                    // patch_value.content 字符串内嵌 JSON
                    if (patchOps != null) {
                        for (i in 0 until patchOps.length()) {
                            val pv = patchOps.optJSONObject(i)?.optJSONObject("patch_value") ?: continue
                            val pvContent = pv.optString("content", "")
                            if (pvContent.isNotEmpty()) {
                                try {
                                    val co = org.json.JSONObject(pvContent)
                                    val t = co.optString("text", "")
                                    if (t.isNotEmpty()) onDelta(t)
                                } catch (_: Exception) {
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (e.message?.startsWith("豆包") == true) throw e
                }
            } else if (line.startsWith("event:")) {
                eventName = line.substring(6).trim()
            } else if (line.startsWith("data:")) {
                data.append(line.substring(5).trimStart()).append("\n")
            }
        }
    }

    /**
     * 千问（Qwen）聊天：上游为 SSE 流式。
     *  - 客户端要求流式 → 边解析帧边转发 OpenAI 格式 chunk
     *  - 客户端要求非流式 → 聚合完整文本后返回单个 JSON
     * 普通 / 思考由 model 名是否含 think/reason 决定（deep_search）。
     * 回复结束后按 deleteSessionAfterReply 开关删除该会话。
     */
    private suspend fun RoutingContext.handleQwenChat(
        req: ChatCompletionRequest, completionId: String, created: Int
    ) {
        val cookie = nextQwenCookie()
        val text = extractDoubaoPrompt(req.messages)
        val deepSearch = if (req.model.lowercase().let { it.contains("think") || it.contains("reason") }) "1" else "0"
        val qwenModel = if (req.model.lowercase() in setOf("qwen", "qwen-think")) QwenClient.DEFAULT_MODEL else req.model
        LogStore.i("Qwen", "调用 /chat，输入长度=${text.length} deepSearch=$deepSearch model=$qwenModel cookie空白=${cookie.isBlank()}")

        if (req.stream) {
            call.respond(object : OutgoingContent.WriteChannelContent() {
                override val contentType = ContentType.Text.EventStream
                override val headers = Headers.build {
                    append("Cache-Control", "no-cache")
                    append("Connection", "keep-alive")
                    append("X-Accel-Buffering", "no")
                }

                override suspend fun writeTo(channel: ByteWriteChannel) {
                    withContext(Dispatchers.IO) {
                        try {
                            withTimeout(300_000) {
                                val resp = QwenClient.chatCompletion(text, cookie, deepSearch, qwenModel)
                                try {
                                    val roleChunk = ChatCompletionChunk(
                                        completionId, "chat.completion.chunk", created, req.model,
                                        listOf(ChunkChoice(delta = ChoiceDelta(role = "assistant")))
                                    )
                                    channel.writeFully(
                                        ("data: " + json.encodeToString(ChatCompletionChunk.serializer(), roleChunk) + "\n\n").toByteArray()
                                    )
                                    channel.flush()

                                    var sessionId: String? = null
                                    QwenClient.parseQwenSse(
                                        resp.body!!.source(),
                                        onText = { t ->
                                            val chunk = ChatCompletionChunk(
                                                completionId, "chat.completion.chunk", created, req.model,
                                                listOf(ChunkChoice(delta = ChoiceDelta(content = t)))
                                            )
                                            channel.writeFully(
                                                ("data: " + json.encodeToString(ChatCompletionChunk.serializer(), chunk) + "\n\n").toByteArray()
                                            )
                                            channel.flush()
                                        },
                                        onSessionId = { sessionId = it }
                                    )
                                    channel.writeFully(
                                        ("data: " + json.encodeToString(
                                            ChatCompletionChunk.serializer(),
                                            ChatCompletionChunk(
                                                completionId, "chat.completion.chunk", created, req.model,
                                                listOf(ChunkChoice(delta = ChoiceDelta(), finish_reason = "stop"))
                                            )
                                        ) + "\n\n").toByteArray()
                                    )
                                    channel.writeFully("data: [DONE]\n\n".toByteArray())
                                    channel.flush()
                                    if (deleteSessionAfterReply) sessionId?.let { QwenClient.deleteSession(it, cookie) }
                                } finally {
                                    resp.close()
                                }
                            }
                        } catch (e: Exception) {
                            LogStore.e("Qwen", "流式转发异常: ${e.stackTraceToString()}")
                        }
                    }
                }
            })
            return
        }

        val (content, errMsg) = withContext(Dispatchers.IO) {
            try {
                withTimeout(300_000) {
                    val resp = QwenClient.chatCompletion(text, cookie, deepSearch, qwenModel)
                    try {
                        val sb = StringBuilder()
                        var sessionId: String? = null
                        QwenClient.parseQwenSse(
                            resp.body!!.source(),
                            onText = { sb.append(it) },
                            onSessionId = { sessionId = it }
                        )
                        if (deleteSessionAfterReply) sessionId?.let { QwenClient.deleteSession(it, cookie) }
                        sb.toString() to null
                    } finally {
                        resp.close()
                    }
                }
            } catch (e: Exception) {
                LogStore.e("Qwen", "请求异常: ${e.stackTraceToString()}")
                null to (e.localizedMessage ?: e.toString())
            }
        }

        if (errMsg != null) {
            LogStore.e("Qwen", "返回错误给客户端: $errMsg")
            call.respond(HttpStatusCode.BadGateway, mapOf("error" to errMsg))
            return
        }

        val contentText = content ?: ""
        if (contentText.isBlank()) {
            call.respond(HttpStatusCode.BadGateway, mapOf("error" to "千问未返回内容"))
            return
        }
        LogStore.i("Qwen", "返回完成结果，长度=${contentText.length}")
        call.respond(
            ChatCompletion(
                completionId, "chat.completion", created, req.model,
                listOf(Choice(message = Msg("assistant", contentText), finish_reason = "stop")),
                Usage(), ""
            )
        )
    }
}
