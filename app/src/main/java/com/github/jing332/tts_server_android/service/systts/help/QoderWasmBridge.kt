package com.github.jing332.tts_server_android.service.systts.help

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Qoder WASM WebView 桥（10-10 接线，任务书 4b）。
 *
 * 路线：无头 WebView（Chromium 内核原生 WebAssembly）加载 assets/qoder/index.html，
 * JS 侧 window.QoderWasm.call(method, args, callId) 异步执行，结果经
 * AndroidBridge.onResult(callId, json) 回传；Kotlin 侧 CountDownLatch 同步等待。
 *
 * 接口契约（与 bridge.js 对齐，任务书 §6）：
 *  - ready() → {version}
 *  - generateAuthFields({uid, securityOauthToken}) → {encrypt_user_info, key}
 *  - createContext({uid, securityOauthToken, machineId}) → {ctxId}
 *  - prepareInfer({ctxId, modelKey, userText, isReasoning, history, source}) → {url, headers, body}
 *  - decryptModelCatalog({encrypted, machineId}) → 目录 JSON（二期）
 *
 * 线程模型：WebView 创建/destroy/evaluateJavascript 必须主线程（内部统一 post）；
 * 业务调用线程用 CountDownLatch 阻塞等结果（朗读链本就在后台线程）。
 * 懒加载单例进程级复用；单次调用超时后销毁重建一次再试（WebView 状态可能坏）。
 */
object QoderWasmBridge {
    private const val TAG = "QoderWasm"
    private const val PAGE_URL = "file:///android_asset/qoder/index.html"
    private const val LOAD_TIMEOUT_SEC = 20L
    private const val CALL_TIMEOUT_SEC = 20L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val callSeq = AtomicLong(0)
    private val pending = ConcurrentHashMap<String, PendingCall>()

    private class PendingCall(val id: String) {
        val latch = CountDownLatch(1)
        @Volatile var resultJson: String? = null
    }

    private var webView: WebView? = null
    private var readyOk = false

    /** JS 回调入口（bridge.js：AndroidBridge.onResult(callId, json)） */
    private class AndroidBridgeImpl {
        @JavascriptInterface
        fun onResult(callId: String, json: String) {
            pending[callId]?.let { p ->
                p.resultJson = json
                p.latch.countDown()
            }
        }
    }

    /** 无头 WebView 初始化 + JS ready 自检。失败抛 IllegalStateException（调用方降级）。 */
    @Synchronized
    private fun ensureReady() {
        if (readyOk && webView != null) return
        destroyLocked() // 半死状态先清干净
        // ① 主线程建 WebView 并加载页面；onPageFinished 后 latch 开
        val pageLatch = CountDownLatch(1)
        var pageError: String? = null
        mainHandler.post {
            try {
                val wv = createWebView(object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        pageLatch.countDown()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                        pageError = "load error: $description"
                        pageLatch.countDown()
                    }
                })
                webView = wv
            } catch (e: Exception) {
                pageError = "create failed: ${e.message}"
                pageLatch.countDown()
            }
        }
        if (!pageLatch.awaitSec(LOAD_TIMEOUT_SEC) || pageError != null) {
            destroyLocked()
            throw IllegalStateException("QoderWasmBridge page: ${pageError ?: "timeout"}")
        }
        // ② ready 调用验证 wasm 实例化链（js 异常由 try/catch 包裹回调报错）
        val r = callInternal("ready", JSONObject())
        if (!r.optBoolean("ok")) {
            destroyLocked()
            throw IllegalStateException("QoderWasmBridge ready: ${r.optString("error")}")
        }
        readyOk = true
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(client: WebViewClient): WebView =
        WebView(appContext()).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = false
                allowFileAccess = true
                allowContentAccess = false
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
            }
            webViewClient = client
            addJavascriptInterface(AndroidBridgeImpl(), "AndroidBridge")
            loadUrl(PAGE_URL)
        }

    /** 调 JS 桥一个方法并同步等结果。未初始化先初始化；失败 ok=false 不抛（调用方降级）。 */
    private fun call(method: String, args: JSONObject): JSONObject {
        if (!readyOk) {
            try { ensureReady() } catch (e: IllegalStateException) {
                return JSONObject().put("ok", false).put("error", e.message)
            }
        }
        return callInternal(method, args)
    }

    private fun callInternal(method: String, args: JSONObject): JSONObject {
        val wv = webView ?: return JSONObject().put("ok", false).put("error", "bridge not initialized")
        val p = PendingCall(callSeq.incrementAndGet().toString())
        pending[p.id] = p
        // JS 侧 try/catch 包裹：同步异常（未知方法/参数错）也走 onResult 回调
        val script = "javascript:try{window.QoderWasm.call('$method', ${args}, '${p.id}')}" +
            "catch(e){window.AndroidBridge.onResult('${p.id}', JSON.stringify({ok:false,error:'js: '+e.message}))}"
        val evalFailed = CountDownLatch(1)
        mainHandler.post {
            try {
                wv.evaluateJavascript(script, null)
            } catch (e: Exception) {
                p.resultJson = JSONObject().put("ok", false).put("error", "eval failed: ${e.message}").toString()
                p.latch.countDown()
            } finally {
                evalFailed.countDown()
            }
        }
        if (!p.latch.awaitSec(CALL_TIMEOUT_SEC)) {
            pending.remove(p.id)
            return JSONObject().put("ok", false).put("error", "bridge call timeout: $method")
        }
        pending.remove(p.id)
        val raw = p.resultJson ?: return JSONObject().put("ok", false).put("error", "bridge no result: $method")
        return try { JSONObject(raw) } catch (e: Exception) {
            JSONObject().put("ok", false).put("error", "bridge bad result: ${raw.take(120)}")
        }
    }

    private fun CountDownLatch.awaitSec(sec: Long): Boolean = await(sec, TimeUnit.SECONDS)

    // —— 业务便捷封装（返回 null=桥或业务失败，调用方降级）——

    private fun callData(method: String, args: JSONObject): JSONObject? {
        val r = call(method, args)
        return if (r.optBoolean("ok")) r.optJSONObject("data") else null
    }

    /** 生成运行时鉴权字段 {encrypt_user_info, key}。 */
    fun generateAuthFields(uid: String, securityOauthToken: String): JSONObject? =
        callData("generateAuthFields", JSONObject().put("uid", uid).put("securityOauthToken", securityOauthToken))

    /** 创建加密推理上下文，返回 ctxId。 */
    fun createContext(uid: String, securityOauthToken: String, machineId: String): String? =
        callData("createContext", JSONObject()
            .put("uid", uid).put("securityOauthToken", securityOauthToken).put("machineId", machineId))
            ?.optString("ctxId")?.takeIf { it.isNotEmpty() }

    /** 构造加密推理请求 {url, headers, body}（headers 必须原样透传，见 QoderChannel）。 */
    fun prepareInfer(
        ctxId: String, modelKey: String, userText: String, isReasoning: Boolean,
        historyJson: String, source: String,
    ): JSONObject? {
        val args = JSONObject()
            .put("ctxId", ctxId).put("modelKey", modelKey).put("userText", userText)
            .put("isReasoning", isReasoning).put("source", source)
        args.put("history", org.json.JSONArray(historyJson))
        return callData("prepareInfer", args)
    }

    /** 释放上下文句柄（bridge.js 内 contexts 表；可选项）。 */
    fun dropContext(ctxId: String) {
        runCatching { call("dropContext", JSONObject().put("ctxId", ctxId)) }
    }

    /** 销毁桥（重置/半死恢复用；下次 call 自动重建）。 */
    @Synchronized
    fun destroy() = destroyLocked()

    private fun destroyLocked() {
        readyOk = false
        val wv = webView
        webView = null
        pending.clear()
        if (wv != null) mainHandler.post {
            try { wv.destroy() } catch (_: Exception) {}
        }
    }

    private fun appContext(): android.content.Context =
        com.github.jing332.tts_server_android.app
}
