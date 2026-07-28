package com.github.jing332.deepseekproxy.proxy

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 用 App 内已启用的 WebView（Chromium/V8 内核，原生支持 WebAssembly）直接加载
 * DeepSeek 官方 wasm（sha3_wasm_bg.wasm），复用与网页端一致的 `wasm_solve` 逻辑。
 *
 * 这样无需在 Android 上重写 keccak，也无需任何额外依赖——正是 app-proxy 已具备的 JS 能力。
 *
 * 用法：
 *   DeepSeekPowWebView.init(context, wasmBytes)   // 在 Service/Application 启动早期调用一次
 *   val answer = DeepSeekPowWebView.solve(challenge, salt, expireAt, difficulty)
 */
object DeepSeekPowWebView {
    private const val TAG = "DeepSeekPowWV"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val solveMutex = Mutex()

    @Volatile
    private var webView: WebView? = null
    @Volatile
    private var initError: String? = null
    // 可重置的「初始化完成」闸门：destroy 后重建时换新实例。
    private var initLatch: CountDownLatch = CountDownLatch(1)
    private var wasmBase64: String = ""
    private var appContext: Context? = null

    // 当前正在等待结果的挂起协程（因为有 Mutex 串行化，同一时刻最多一个）。
    private val pendingCont = AtomicReference<Continuation<String>?>(null)

    /** 在应用/服务启动早期调用一次：传入 wasm 字节与 Context。 */
    fun init(context: Context, wasmBytes: ByteArray) {
        appContext = context.applicationContext
        wasmBase64 = Base64.getEncoder().encodeToString(wasmBytes)
    }

    private class PowJsBridge {
        @JavascriptInterface
        fun onReady() {
            initLatch.countDown()
        }

        @JavascriptInterface
        fun onError(msg: String) {
            initError = msg
            initLatch.countDown()
        }

        @JavascriptInterface
        fun onResult(json: String) {
            val cont = pendingCont.getAndSet(null)
            cont?.resume(json)
        }
    }

    private suspend fun ensureWebView() {
        if (webView == null) {
            withContext(Dispatchers.Main) {
                if (webView == null) {
                    val ctx = appContext
                        ?: throw RuntimeException("DeepSeekPowWebView 未初始化（请先调用 init）")
                    val wv = WebView(ctx)
                    wv.settings.javaScriptEnabled = true
                    wv.settings.domStorageEnabled = true
                    wv.addJavascriptInterface(PowJsBridge(), "AndroidPow")
                    wv.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            // 页面加载完成后注入 wasm 并实例化（异步）。
                            view?.evaluateJavascript(
                                "(function(){ window.__initPow('$wasmBase64'); })();",
                                null
                            )
                        }
                    }
                    webView = wv
                    wv.loadDataWithBaseURL(
                        "https://ds.local/", POW_HTML, "text/html", "utf-8", null
                    )
                }
            }
            initLatch.await()
            initError?.let { throw RuntimeException("PoW WebView 初始化失败: $it") }
        }
    }

    /**
     * 求解 PoW，返回 answer（与网页端一致的整数）。
     * 内部串行化，避免并发请求在同一 WebView 上交错。
     */
    suspend fun solve(
        challenge: String, salt: String, expireAt: Long, difficulty: Int
    ): Long = solveMutex.withLock {
        ensureWebView()
        val json = suspendCancellableCoroutine<String> { cont ->
            pendingCont.set(cont)
            cont.invokeOnCancellation { pendingCont.set(null) }
            mainHandler.post {
                val wv = webView
                if (wv == null) {
                    pendingCont.set(null)
                    cont.resumeWithException(RuntimeException("WebView 不可用"))
                    return@post
                }
                // 用 JSONObject.quote 生成 JS 安全的字符串字面量，避免转义问题。
                val js = "window.__solve(" +
                        JSONObject.quote(challenge) + "," +
                        JSONObject.quote(salt) + "," +
                        "$expireAt,$difficulty, AndroidPow)"
                wv.evaluateJavascript(js, null)
            }
        }
        val obj = JSONObject(json)
        if (obj.optBoolean("ok", false)) {
            obj.getDouble("answer").toLong()
        } else {
            throw RuntimeException("PoW 求解失败: " + obj.optString("error"))
        }
    }

    /** 释放 WebView（应用退出时调用），并重置初始化状态以便下次重建。 */
    fun destroy() {
        webView?.post { webView?.destroy() }
        webView = null
        initLatch = CountDownLatch(1)
        initError = null
    }

    private const val POW_HTML = """
<!doctype html>
<html>
<head><meta charset="utf-8"></head>
<body>
<script>
window.__inst = null;
window.__ready = false;

// 由 Kotlin 注入 wasm 的 base64 并实例化（与 deepseek_pow.js 的实例化逻辑一致）。
window.__initPow = async function (b64) {
  try {
    const bin = atob(b64);
    const bytes = new Uint8Array(bin.length);
    for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
    const { instance } = await WebAssembly.instantiate(bytes, {});
    window.__inst = instance;
    if (window.AndroidPow) AndroidPow.onReady();
  } catch (e) {
    if (window.AndroidPow) AndroidPow.onError(String(e));
  }
};

// 与 deepseek_pow.js 的 solvePow 一一对应。
async function __solvePow(challenge, salt, expireAt, difficulty) {
  const inst = window.__inst;
  if (!inst) throw new Error('wasm 未就绪');
  const enc = new TextEncoder();
  const chBytes = enc.encode(challenge);
  const prefix = salt + '_' + expireAt + '_';
  const pfBytes = enc.encode(prefix);

  const chPtr = inst.exports.__wbindgen_export_0(chBytes.length, 1) >>> 0;
  const pfPtr = inst.exports.__wbindgen_export_0(pfBytes.length, 1) >>> 0;

  const u8 = new Uint8Array(inst.exports.memory.buffer);
  u8.set(chBytes, chPtr);
  u8.set(pfBytes, pfPtr);

  const sp = inst.exports.__wbindgen_add_to_stack_pointer(-16);
  inst.exports.wasm_solve(sp, chPtr, chBytes.length, pfPtr, pfBytes.length, difficulty);
  const dv = new DataView(inst.exports.memory.buffer);
  const flag = dv.getInt32(sp, true);
  const answer = dv.getFloat64(sp + 8, true);
  inst.exports.__wbindgen_add_to_stack_pointer(16);

  if (flag === 0) throw new Error('PoW: no solution found');
  return answer;
}

// cb 为注入的 AndroidPow 接口，其 onResult(json) 会把结果回传给 Kotlin。
window.__solve = function (challenge, salt, expireAt, difficulty, cb) {
  (async () => {
    try {
      const a = await __solvePow(challenge, salt, expireAt, difficulty);
      cb.onResult(JSON.stringify({ ok: true, answer: a }));
    } catch (e) {
      cb.onResult(JSON.stringify({ ok: false, error: String(e) }));
    }
  })();
};
</script>
</body>
</html>
"""
}
