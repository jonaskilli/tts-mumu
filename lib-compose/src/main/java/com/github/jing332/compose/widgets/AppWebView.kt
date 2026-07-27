package com.github.jing332.compose.widgets

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.JsResult
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.os.bundleOf
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.github.jing332.common.utils.longToast
import com.google.accompanist.web.AccompanistWebChromeClient
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.WebViewNavigator
import com.google.accompanist.web.WebViewState
import com.google.accompanist.web.rememberWebViewNavigator
import com.google.accompanist.web.rememberWebViewState
import kotlin.math.roundToInt

private fun isMobile(userAgent: String): Boolean {
    val regex = Regex("android|mobile", RegexOption.IGNORE_CASE)
    return userAgent.isBlank() || regex.containsMatchIn(userAgent)
}

@Suppress("DEPRECATION")
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppWebView(
    modifier: Modifier = Modifier,
    captureBackPresses: Boolean = true,
    state: WebViewState,
    userAgent: String = "",
    navigator: WebViewNavigator,
    onPageFinished: (WebView, String) -> Unit = { _, _ -> },
    onCreated: (WebView) -> Unit = {},
    onDispose: (WebView) -> Unit = {},
) {
    var showAlertDialog by remember { mutableStateOf<Triple<String, String, JsResult>?>(null) }
    if (showAlertDialog != null) {
        val webUrl = showAlertDialog!!.first
        val msg = showAlertDialog!!.second
        val result = showAlertDialog!!.third
        AlertDialog(onDismissRequest = {
            result.cancel()
            showAlertDialog = null
        },
            title = { Text(webUrl) },
            text = { Text(msg) },
            confirmButton = {
                TextButton(
                    onClick = {
                        result.confirm()
                        showAlertDialog = null
                    }) {
                    Text(stringResource(id = android.R.string.ok))
                }
            }, dismissButton = {
                result.cancel()
                showAlertDialog = null
            })
    }

    val context = LocalContext.current
    val chromeClient = remember {
        object : AccompanistWebChromeClient() {
            override fun onJsConfirm(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?,
            ): Boolean {
                if (result == null) return false
                showAlertDialog = Triple(url ?: "", message ?: "", result)
                return true
            }

            override fun onJsAlert(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?,
            ): Boolean {
                if (result == null) return false
                showAlertDialog = Triple(url ?: "", message ?: "", result)

                return true
            }
        }
    }

    val client = remember {
        object : AccompanistWebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean {
                kotlin.runCatching {
                    if (request?.url?.scheme?.startsWith("http") == false) {
                        val intent = Intent(Intent.ACTION_VIEW, request.url)
                        context.startActivity(Intent.createChooser(intent, request.url.toString()))
                        return true
                    }
                }.onFailure {
                    context.longToast("Go to App failed: ${request?.url}")
                }

                return super.shouldOverrideUrlLoading(view, request)
            }

            override fun onPageFinished(view: WebView, url: String?) {
                if (!isMobile(view.settings.userAgentString))
                    view.evaluateJavascript(
                        """document.querySelector('meta[name="viewport"]').setAttribute('content', 'width=1024px, height=auto, initial-scale=' + (document.documentElement.clientWidth / 1024));""",
                        null
                    );

                super.onPageFinished(view, url)
                onPageFinished.invoke(view, url ?: "")
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                // 优化：仅在主页面报错时重试，避免死循环
                if (request?.isForMainFrame == true && request.url?.toString()?.startsWith(state.lastLoadedUrl ?: "null") == true){
                    view.reload()
                }
            }
        }
    }
    val refreshState = rememberPullToRefreshState()
    val isDarkTheme = isSystemInDarkTheme()

    val bundle: Bundle = rememberSaveable { bundleOf() }

    val webview = remember {
        object : WebView(context) {
            val verticalScrollRange: Int get() = computeVerticalScrollRange() - height
        }
    }

    val scrollableState = rememberScrollableState { delta ->
        val scrollY = webview.scrollY
        val consume = (scrollY - delta).coerceIn(0f, webview.verticalScrollRange.toFloat())
        webview.scrollTo(0, consume.roundToInt())
        (scrollY - webview.scrollY).toFloat()
    }

    PullToRefreshBox(
        modifier = modifier,
        isRefreshing = state.isLoading,
        state = refreshState,
        onRefresh = { navigator.reload() }) {
        WebView(
            captureBackPresses = captureBackPresses,
            modifier = Modifier
                .fillMaxSize()
                .scrollable(scrollableState, Orientation.Vertical),

            state = state,
            navigator = navigator,
            onCreated = {
                if (!bundle.isEmpty) {
                    it.restoreState(bundle)
                }
                it.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    
                    // 🛠️ 关键修复：优化加载和缓存策略
                    cacheMode = WebSettings.LOAD_DEFAULT 
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW // 允许 HTTP/HTTPS 混合
                    allowFileAccess = true 
                    allowContentAccess = true

                    userAgentString = userAgent

                    if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                        WebSettingsCompat.setForceDark(
                            it.settings,
                            if (isDarkTheme)
                                WebSettingsCompat.FORCE_DARK_ON
                            else WebSettingsCompat.FORCE_DARK_OFF
                        )
                    }

                    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                        WebSettingsCompat.setAlgorithmicDarkeningAllowed(
                            it.settings,
                            true
                        )
                    }

                    if (!isMobile(userAgent)) {
                        loadWithOverviewMode = true;
                        layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL;
                        useWideViewPort = true;
                        setSupportZoom(true)
                        builtInZoomControls = true
                    }
                }

                onCreated.invoke(it)
            },
            onDispose = {
                it.saveState(bundle)
                onDispose(it)
            },
            client = client,
            chromeClient = chromeClient,
            factory = {
                webview.apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            }
        )
    }
}
