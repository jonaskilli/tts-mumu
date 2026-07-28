package com.github.jing332.deepseekproxy

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import com.github.jing332.deepseekproxy.proxy.LogStore
import com.github.jing332.deepseekproxy.proxy.LongCatClient
import com.github.jing332.deepseekproxy.proxy.ProxyServer

/** 一条聊天消息：可含文本和/或图片 URL 列表。 */
data class ChatMessageItem(
    val role: String,
    val text: String = "",
    val images: List<String> = emptyList()
)

/** 生图比例选项：展示名 → 豆包 ratio 串。
 *  竖版（含默认 9:16）排在前，横版在后。高清/低清尺寸对豆包无效，仅比例生效。 */
val IMAGE_SIZE_OPTIONS = listOf(
    "9:16 (竖屏)" to "9:16",
    "3:4 (竖版)" to "3:4",
    "1:1 (方形)" to "1:1",
    "16:9 (横屏)" to "16:9",
    "4:3 (横版)" to "4:3",
    "2:5 (长竖屏)" to "2:5",
    "5:2 (长横屏)" to "5:2"
)

class ProxyViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("ds_proxy", Context.MODE_PRIVATE)
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /** 当前选中的服务商：doubao（豆包）/ kimi（Kimi）/ qwen（千问）。 */
    val provider = MutableStateFlow(prefs.getString("provider", "doubao") ?: "doubao")

    /** 豆包 Cookie 列表（索引 0 为「默认Cookie」），支持多个并自动轮换。 */
    private val doubaoCookies = MutableStateFlow(loadList("doubao_cookies", "doubao_cookie"))
    /** Kimi Cookie 列表（索引 0 为「默认Cookie」，内含登录 JWT），支持多个并自动轮换。 */
    private val kimiCookies = MutableStateFlow(loadList("kimi_cookies", "kimi_tokens"))

    /** 千问（Qwen）Cookie 列表（索引 0 为「默认Cookie」，含 tongyi_sso_ticket），支持多个并自动轮换。 */
    private val qwenCookies = MutableStateFlow(loadList("qwen_cookies", "qwen_cookie"))

    /** DeepSeek Bearer Token 列表（索引 0 为「默认Token」），支持多个并自动轮换。 */
    private val deepseekTokens = MutableStateFlow(loadList("deepseek_tokens", "deepseek_token"))

    /** 当前选中的 DeepSeek Token 下标（浏览器提取会写入此下标）。 */
    val deepseekSelectedIndex = MutableStateFlow(loadDeepSeekSelectedIndex())

    /** 暴露给 UI 的 DeepSeek Token 列表（供切换/新建弹窗）。 */
    val deepseekTokensFlow: StateFlow<List<String>> = deepseekTokens

    /** 当前 DeepSeek Token（供 UI 输入框展示/编辑，取选中下标那一条）。 */
    val deepseekToken: StateFlow<String> = combine(deepseekTokens, deepseekSelectedIndex) { list, idx ->
        list.getOrNull(idx) ?: ""
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    /** DeepSeek 网页中转模式：default（快速模式）/ expert（专家模式）。 */
    val deepseekMode = MutableStateFlow(prefs.getString("deepseek_mode", "default") ?: "default")

    /** LongCat（美团龙猫）配置 JSON 列表（索引 0 为「默认配置」），支持多个并自动轮换。 */
    private val longCatConfigs = MutableStateFlow(loadList("longcat_configs", "longcat_config"))

    /** 当前选中的 LongCat 配置下标（浏览器提取会写入此下标）。 */
    val longCatSelectedIndex = MutableStateFlow(loadLongCatSelectedIndex())

    /** 暴露给 UI 的 LongCat 配置列表（供切换/新建弹窗）。 */
    val longCatConfigsFlow: StateFlow<List<String>> = longCatConfigs

    /** 当前选中的 LongCat 配置（供 UI 输入框展示/编辑，取选中下标那一条）。 */
    val longCatConfig: StateFlow<String> = combine(longCatConfigs, longCatSelectedIndex) { list, idx ->
        list.getOrNull(idx) ?: ""
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    init {
        // 启动时把当前选中的 LongCat 配置应用到客户端，并把列表交给服务端用于轮换
        applyLongCatConfig(longCatConfigs.value.getOrNull(longCatSelectedIndex.value) ?: "")
        ProxyServer.setLongCatConfigList(longCatConfigs.value)
    }

    /** 当前服务商对应的凭证列表（供 UI 展示/编辑）。 */
    val cookies: StateFlow<List<String>> = combine(provider, doubaoCookies, kimiCookies, qwenCookies) { p, d, k, q ->
        when (p) {
            "kimi" -> k
            "qwen" -> q
            else -> d
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, doubaoCookies.value)

    /** 当前选中的凭证下标（浏览器提取会写入此下标）。 */
    val selectedIndex = MutableStateFlow(loadSelectedIndex())

    private fun loadList(key: String, legacyKey: String): List<String> {
        val legacy = prefs.getString(legacyKey, null)
        val raw = prefs.getString(key, null)
        return if (raw != null) {
            try {
                val arr = org.json.JSONArray(raw)
                val list = (0 until arr.length()).map { arr.optString(it, "") }
                if (list.isEmpty()) listOf("") else list
            } catch (_: Exception) { listOf(legacy ?: "") }
        } else if (!legacy.isNullOrEmpty()) {
            listOf(legacy)
        } else {
            listOf("")
        }
    }

    private fun loadSelectedIndex(): Int {
        val p = provider.value
        val key = when (p) {
            "kimi" -> "kimi_cookie_selected"
            "qwen" -> "qwen_cookie_selected"
            else -> "doubao_cookie_selected"
        }
        val list = when (p) {
            "kimi" -> kimiCookies
            "qwen" -> qwenCookies
            else -> doubaoCookies
        }.value
        return prefs.getInt(key, 0).coerceIn(0, list.lastIndex)
    }

    private fun activeList(): MutableStateFlow<List<String>> =
        when (provider.value) {
            "kimi" -> kimiCookies
            "qwen" -> qwenCookies
            else -> doubaoCookies
        }

    private fun persistActive() {
        val arr = org.json.JSONArray()
        activeList().value.forEach { arr.put(it) }
        val key = when (provider.value) {
            "kimi" -> "kimi_cookies"
            "qwen" -> "qwen_cookies"
            else -> "doubao_cookies"
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    private fun persistSelectedIndex() {
        val key = when (provider.value) {
            "kimi" -> "kimi_cookie_selected"
            "qwen" -> "qwen_cookie_selected"
            else -> "doubao_cookie_selected"
        }
        prefs.edit().putInt(key, selectedIndex.value).apply()
    }

    /** 凭证展示名：豆包与 Kimi 统一用 Cookie。 */
    fun cookieLabel(i: Int): String {
        val isKimi = provider.value == "kimi"
        return if (i == 0) "默认Cookie" else "Cookie ${i + 1}"
    }

    /** 当前选中的凭证文本。 */
    fun currentCookie(): String = cookies.value.getOrNull(selectedIndex.value) ?: ""

    /** 选中某个凭证（点击弹窗中的某一项）。 */
    fun setSelectedIndex(i: Int) {
        if (i in activeList().value.indices) {
            selectedIndex.value = i
            persistSelectedIndex()
        }
    }

    /** 切换服务商（豆包 / Kimi）。 */
    fun setProvider(p: String) {
        if (p == provider.value) return
        provider.value = p
        prefs.edit().putString("provider", p).apply()
        val key = when (p) {
            "kimi" -> "kimi_cookie_selected"
            "qwen" -> "qwen_cookie_selected"
            else -> "doubao_cookie_selected"
        }
        val list = activeList().value
        selectedIndex.value = prefs.getInt(key, 0).coerceIn(0, list.lastIndex.coerceAtLeast(0))
        syncServerCookies()
    }

    /** 更新当前选中的凭证文本（输入框编辑 / 浏览器提取）。 */
    fun updateSelectedCookie(value: String) {
        val i = selectedIndex.value
        val list = activeList().value.toMutableList()
        if (i in list.indices) list[i] = value else list.add(value)
        activeList().value = list
        persistActive()
        syncServerCookies()
    }

    /** 新增一个空的凭证（默认排在最后，并自动选中）。 */
    fun addCookie() {
        val list = activeList().value.toMutableList()
        list.add("")
        activeList().value = list
        persistActive()
        setSelectedIndex(list.lastIndex)
        syncServerCookies()
    }

    /** 删除指定凭证（至少保留默认一项）。 */
    fun removeCookie(i: Int) {
        if (activeList().value.size <= 1) return
        val list = activeList().value.toMutableList()
        list.removeAt(i)
        activeList().value = list
        persistActive()
        if (selectedIndex.value >= list.size) selectedIndex.value = list.lastIndex
        persistSelectedIndex()
        syncServerCookies()
    }

    /** 将两个服务商的凭证同步给本地中转服务。 */
    private fun syncServerCookies() {
        ProxyServer.setDoubaoCookieList(doubaoCookies.value)
        ProxyServer.setKimiCookieList(kimiCookies.value)
        ProxyServer.setQwenCookieList(qwenCookies.value)
        ProxyServer.setDeepSeekTokenList(deepseekTokens.value)
        ProxyServer.setLongCatConfigList(longCatConfigs.value)
    }

    /** 当前 DeepSeek Token（供 UI 展示/编辑，取选中下标那一条）。 */
    fun currentDeepSeekToken(): String = deepseekTokens.value.getOrNull(deepseekSelectedIndex.value) ?: ""

    /** 更新当前选中的 DeepSeek Token（输入框编辑 / 浏览器提取）。 */
    fun setDeepSeekToken(value: String) {
        val i = deepseekSelectedIndex.value
        val list = deepseekTokens.value.toMutableList()
        if (i in list.indices) list[i] = value else list.add(value)
        deepseekTokens.value = list
        persistDeepSeek()
        syncServerCookies()
    }

    private fun persistDeepSeek() {
        val arr = org.json.JSONArray()
        deepseekTokens.value.forEach { arr.put(it) }
        prefs.edit().putString("deepseek_tokens", arr.toString()).apply()
    }

    private fun loadDeepSeekSelectedIndex(): Int {
        val list = deepseekTokens.value
        return prefs.getInt("deepseek_token_selected", 0).coerceIn(0, list.lastIndex.coerceAtLeast(0))
    }

    /** 选中某个 DeepSeek Token（点击弹窗中的某一项）。 */
    fun setDeepSeekSelectedIndex(i: Int) {
        if (i in deepseekTokens.value.indices) {
            deepseekSelectedIndex.value = i
            prefs.edit().putInt("deepseek_token_selected", i).apply()
        }
    }

    /** 设置 DeepSeek 网页中转模式（default 快速 / expert 专家），同步给本地中转服务。 */
    fun setDeepSeekMode(m: String) {
        deepseekMode.value = m
        prefs.edit().putString("deepseek_mode", m).apply()
        ProxyServer.deepseekMode = m
    }

    /** 设置 LongCat 配置（JSON），写入当前选中的账号，解析并同步给 LongCatClient，持久化保存。 */
    fun setLongCatConfig(value: String) {
        val i = longCatSelectedIndex.value
        val list = longCatConfigs.value.toMutableList()
        if (i in list.indices) list[i] = value else list.add(value)
        longCatConfigs.value = list
        persistLongCat()
        applyLongCatConfig(value)
        ProxyServer.setLongCatConfigList(longCatConfigs.value)
    }

    private fun persistLongCat() {
        val arr = org.json.JSONArray()
        longCatConfigs.value.forEach { arr.put(it) }
        prefs.edit().putString("longcat_configs", arr.toString()).apply()
    }

    private fun loadLongCatSelectedIndex(): Int {
        val list = longCatConfigs.value
        return prefs.getInt("longcat_config_selected", 0).coerceIn(0, list.lastIndex.coerceAtLeast(0))
    }

    /** 选中某个 LongCat 配置（点击弹窗中的某一项），并应用到 LongCatClient。 */
    fun setLongCatSelectedIndex(i: Int) {
        if (i in longCatConfigs.value.indices) {
            longCatSelectedIndex.value = i
            prefs.edit().putInt("longcat_config_selected", i).apply()
            applyLongCatConfig(longCatConfigs.value[i])
        }
    }

    /** 新增一个空的 LongCat 配置（默认排在最后，并自动选中）。 */
    fun addLongCatConfig() {
        val list = longCatConfigs.value.toMutableList()
        list.add("")
        longCatConfigs.value = list
        persistLongCat()
        setLongCatSelectedIndex(list.lastIndex)
        ProxyServer.setLongCatConfigList(longCatConfigs.value)
    }

    /** 删除指定 LongCat 配置（至少保留默认一项）。 */
    fun removeLongCatConfig(i: Int) {
        if (longCatConfigs.value.size <= 1) return
        val list = longCatConfigs.value.toMutableList()
        list.removeAt(i)
        longCatConfigs.value = list
        persistLongCat()
        if (longCatSelectedIndex.value >= list.size) longCatSelectedIndex.value = list.lastIndex
        prefs.edit().putInt("longcat_config_selected", longCatSelectedIndex.value).apply()
        applyLongCatConfig(longCatConfigs.value.getOrNull(longCatSelectedIndex.value) ?: "")
        ProxyServer.setLongCatConfigList(longCatConfigs.value)
    }

    /** 浏览器拦截到 LongCat 网站请求后，把请求头里的 mtgsig / m_traceid / Cookie / base_url 直接写入配置（像 DeepSeek 抓 Authorization 一样）。
     *  注意：抓到的请求 URL 可能是 task-check / chat-completion-V2 等任意接口，必须统一重建为聊天接口地址，
     *  仅借用其 query 参数（yodaReady / csecplatform / csecversion）。 */
    fun setLongCatFromRequest(mtgsig: String, mTraceId: String, cookie: String, baseUrl: String) {
        val o = try {
            org.json.JSONObject(longCatConfig.value.ifBlank { "{}" })
        } catch (_: Exception) {
            org.json.JSONObject()
        }
        o.put("mtgsig", mtgsig)
        o.put("m_traceid", mTraceId)
        o.put("cookies", cookie)
        val chatUrl = runCatching {
            val u = android.net.Uri.parse(baseUrl)
            val host = u.host ?: "longcat.chat"
            val query = u.encodedQuery?.let { "?$it" } ?: ""
            "https://$host/api/v1/chat-completion-V2$query"
        }.getOrElse { baseUrl }
        o.put("base_url", chatUrl)
        if (!o.has("agent_id")) o.put("agent_id", "1")
        setLongCatConfig(o.toString())
    }

    /** 解析 LongCat 配置 JSON，写入 LongCatClient 的静态字段；空串则复位为默认。 */
    private fun applyLongCatConfig(json: String) {
        LongCatClient.applyConfig(json)
    }

    /** 更新当前选中的 DeepSeek Token 文本（与 setDeepSeekToken 同义，供 UI 语义化调用）。 */
    fun updateSelectedDeepSeekToken(value: String) = setDeepSeekToken(value)

    /** 新增一个空的 DeepSeek Token（默认排在最后，并自动选中）。 */
    fun addDeepSeekToken() {
        val list = deepseekTokens.value.toMutableList()
        list.add("")
        deepseekTokens.value = list
        persistDeepSeek()
        setDeepSeekSelectedIndex(list.lastIndex)
        syncServerCookies()
    }

    /** 删除指定 DeepSeek Token（至少保留默认一项）。 */
    fun removeDeepSeekToken(i: Int) {
        if (deepseekTokens.value.size <= 1) return
        val list = deepseekTokens.value.toMutableList()
        list.removeAt(i)
        deepseekTokens.value = list
        persistDeepSeek()
        if (deepseekSelectedIndex.value >= list.size) deepseekSelectedIndex.value = list.lastIndex
        prefs.edit().putInt("deepseek_token_selected", deepseekSelectedIndex.value).apply()
        syncServerCookies()
    }
    val chatModel = MutableStateFlow(prefs.getString("chat_model", "hy3-preview") ?: "hy3-preview")
    /**
     * 混元太极独立端口，与「转发器」(SystemTtsForwarderConfig.port) 互不干扰，
     * 各自可单独设置。默认 8801。
     */
    val port: MutableStateFlow<Int> = MutableStateFlow(prefs.getInt("proxy_port", 8801))
    val streamMode = MutableStateFlow(prefs.getBoolean("stream_mode", true))
    /** 完成回复后是否删除会话（Kimi / DeepSeek 通用）。默认开启，与之前 Kimi 的默认行为一致。 */
    val deleteSessionAfterReply = MutableStateFlow(prefs.getBoolean("delete_session_after_reply", true))
    val serverRunning = MutableStateFlow(prefs.getBoolean("running", false))
    val logs = LogStore.lines
    val chatMessages = MutableStateFlow<List<ChatMessageItem>>(emptyList())
    val imageSize = MutableStateFlow(prefs.getString("image_size", "9:16") ?: "9:16")
    val chatInput = MutableStateFlow("")

    fun setChatModel(m: String) {
        chatModel.value = m
        prefs.edit().putString("chat_model", m).apply()
    }

    fun setPort(p: Int) {
        port.value = p
        prefs.edit().putInt("proxy_port", p).apply()
    }

    fun setStreamMode(v: Boolean) {
        streamMode.value = v
        ProxyServer.streamMode = v
        prefs.edit().putBoolean("stream_mode", v).apply()
    }

    /** 更新「完成回复后删除会话」开关，并实时同步给中转服务。 */
    fun setDeleteSessionAfterReply(v: Boolean) {
        deleteSessionAfterReply.value = v
        ProxyServer.deleteSessionAfterReply = v
        prefs.edit().putBoolean("delete_session_after_reply", v).apply()
    }

    fun setImageSize(s: String) {
        imageSize.value = s
        ProxyServer.defaultImageSize = s
        prefs.edit().putString("image_size", s).apply()
    }

    fun startServer() {
        val app = getApplication<Application>()
        val intent = Intent(app, ProxyService::class.java).apply {
            putExtra(ProxyService.EXTRA_PORT, port.value)
            putExtra(ProxyService.EXTRA_DOUBAO_COOKIES, ArrayList(doubaoCookies.value))
            putExtra(ProxyService.EXTRA_KIMI_COOKIES, ArrayList(kimiCookies.value))
            putExtra(ProxyService.EXTRA_QWEN_COOKIES, ArrayList(qwenCookies.value))
            putExtra(ProxyService.EXTRA_STREAM, streamMode.value)
        }
        ContextCompat.startForegroundService(app, intent)
        ProxyServer.streamMode = streamMode.value
        ProxyServer.defaultImageSize = imageSize.value
        ProxyServer.deepseekMode = deepseekMode.value
        ProxyServer.deleteSessionAfterReply = deleteSessionAfterReply.value
        syncServerCookies()
        applyLongCatConfig(longCatConfig.value)
        serverRunning.value = true
        prefs.edit().putBoolean("running", true).apply()
        LogStore.i("Proxy", "▶ 启动前台服务: http://0.0.0.0:${port.value}/v1/chat/completions (混元3 / 豆包 / Kimi, 流式=${streamMode.value})")
    }

    fun stopServer() {
        val app = getApplication<Application>()
        app.stopService(Intent(app, ProxyService::class.java))
        ProxyServer.stop()
        serverRunning.value = false
        prefs.edit().putBoolean("running", false).apply()
        LogStore.i("Proxy", "■ 服务已停止")
    }

    fun sendChat() {
        val prompt = chatInput.value
        if (prompt.isBlank()) return
        chatInput.value = ""
        chatMessages.value = chatMessages.value + ChatMessageItem("user", prompt)
        LogStore.i("App", "发送消息: ${prompt.take(80)} (model=${chatModel.value})")

        val endpoint = "http://127.0.0.1:${port.value}/v1/chat/completions"
        val body = JSONObject().apply {
            put("model", chatModel.value)
            put("stream", false)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "user").put("content", prompt))
            })
        }.toString()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val req = Request.Builder().url(endpoint)
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .addHeader("Content-Type", "application/json")
                    .build()
                http.newCall(req).execute().use { resp ->
                    val respBody = resp.body!!.string()
                    if (!resp.isSuccessful) {
                        val err = try {
                            JSONObject(respBody).optString("error", respBody.take(300))
                        } catch (_: Exception) {
                            respBody.take(300)
                        }
                        LogStore.e("App", "请求失败 HTTP ${resp.code}: $err")
                        chatMessages.value = chatMessages.value + ChatMessageItem("assistant", "⚠️ 错误: $err")
                        return@use
                    }
                    val json = JSONObject(respBody)
                    val content = json.getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").getString("content")
                    chatMessages.value = chatMessages.value + ChatMessageItem("assistant", content)
                }
            } catch (e: Exception) {
                LogStore.e("App", "sendChat 异常: ${e.stackTraceToString()}")
                chatMessages.value = chatMessages.value + ChatMessageItem("assistant", "⚠️ 错误: ${e.localizedMessage}")
            }
        }
    }

    /** 生图测试：调用本地 /v1/images/generations（豆包生图接口）。 */
    fun generateImage() {
        val prompt = chatInput.value
        if (prompt.isBlank()) return
        chatInput.value = ""
        chatMessages.value = chatMessages.value + ChatMessageItem("user", "【生图】$prompt")
        LogStore.i("App", "生图请求: ${prompt.take(80)} (size=${imageSize.value})")

        val endpoint = "http://127.0.0.1:${port.value}/v1/images/generations"
        val body = JSONObject().apply {
            put("prompt", prompt)
            put("size", imageSize.value)
            put("n", 1)
            // 携带当前模型，便于后端区分生图后端（如选中「智谱」→ 智谱生图）
            put("model", chatModel.value)
        }.toString()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val req = Request.Builder().url(endpoint)
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .addHeader("Content-Type", "application/json")
                    .build()
                http.newCall(req).execute().use { resp ->
                    val respBody = resp.body!!.string()
                    if (!resp.isSuccessful) {
                        val err = try {
                            JSONObject(respBody).optString("error", respBody.take(300))
                        } catch (_: Exception) {
                            respBody.take(300)
                        }
                        LogStore.e("App", "生图失败 HTTP ${resp.code}: $err")
                        chatMessages.value = chatMessages.value + ChatMessageItem("assistant", "⚠️ 生图错误: $err")
                        return@use
                    }
                    val data = JSONObject(respBody).optJSONArray("data") ?: JSONArray()
                    val urls = (0 until data.length()).mapNotNull { i ->
                        data.optJSONObject(i)?.optString("url", "")
                    }.filter { it.isNotBlank() }
                    if (urls.isEmpty()) {
                        chatMessages.value = chatMessages.value + ChatMessageItem("assistant", "⚠️ 生图未返回图片")
                    } else {
                        chatMessages.value = chatMessages.value + ChatMessageItem("assistant", images = urls)
                    }
                }
            } catch (e: Exception) {
                LogStore.e("App", "generateImage 异常: ${e.stackTraceToString()}")
                chatMessages.value = chatMessages.value + ChatMessageItem("assistant", "⚠️ 生图错误: ${e.localizedMessage}")
            }
        }
    }
}
