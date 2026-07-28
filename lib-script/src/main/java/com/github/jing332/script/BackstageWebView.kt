package com.github.jing332.script;

import android.annotation.SuppressLint
import android.net.http.SslError
import android.util.AndroidRuntimeException
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.github.jing332.common.utils.runOnUI
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.commons.text.StringEscapeUtils
import splitties.init.appCtx
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.jvm.Throws

/**
 * 后台webView
 */
class BackstageWebView(
    private val payload: Payload,
    private val js: String = JS,
    private val headerMap: Map<String, String> = emptyMap(),
) {
    private var mWebView: WebView? = null
    private var mContinuation: Continuation<Result<String, Error>>? = null

    sealed interface Error {
        data class E(val code: Int, val description: String) : Error
    }

    private val mutex = Mutex()
    suspend fun getHtmlResponse(): Result<String, Error> = coroutineScope {
        mutex.withLock {
            withContext(Dispatchers.Main) { load() }
            suspendCancellableCoroutine {
                mContinuation = it
                it.invokeOnCancellation {
                    runOnUI {
                        mWebView?.destroy()
                    }
                }
            }
        }
    }

    @Throws(AndroidRuntimeException::class)
    private fun load() {
        val webView = createWebView()
        mWebView = webView

        when (payload) {
            is Payload.Url -> if (headerMap.isEmpty())
                webView.loadUrl(payload.url)
            else
                webView.loadUrl(payload.url, headerMap)

            is Payload.Data -> webView.loadData(payload.data, payload.mimeType, payload.encoding)
            is Payload.DataWithBaseUrl -> webView.loadDataWithBaseURL(
                payload.baseUrl,
                payload.data,
                payload.mimeType,
                payload.encoding,
                payload.historyUrl
            )
        }

    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun createWebView(): WebView {
        val webView = WebView(appCtx)
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.blockNetworkImage = true
        settings.userAgentString = headerMap["User-Agent"] ?: headerMap["user-agent"]
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        webView.webViewClient = HtmlWebViewClient()

        return webView
    }

    private fun destroy() {
        mWebView?.destroy()
        mWebView = null
    }


    private inner class HtmlWebViewClient : WebViewClient() {
         override fun onPageFinished(view: WebView, url: String) {
            view.evaluateJavascript(js, object : ValueCallback<String> {
                override fun onReceiveValue(value: String) {
                    val str = StringEscapeUtils.unescapeJson(value)
                    mContinuation?.resume(Ok(str.replace(quoteRegex, "")))
                    mContinuation = null
                    destroy()
                }
            })
        }

        @SuppressLint("WebViewClientOnReceivedSslError")
        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: SslError?,
        ) {
            handler?.proceed()
        }
    }

    sealed interface Payload {
        data class Url(val url: String) : Payload
        data class Data(
            val data: String,
            val mimeType: String = "text/html",
            val encoding: String = "utf-8",
        ) : Payload

        data class DataWithBaseUrl(
            val baseUrl: String,
            val data: String,
            val mimeType: String,
            val encoding: String,
            val historyUrl: String,
        ) : Payload
    }

    companion object {
        const val JS = "document.documentElement.outerHTML"
        private val quoteRegex = "^\"|\"$".toRegex()
    }
}