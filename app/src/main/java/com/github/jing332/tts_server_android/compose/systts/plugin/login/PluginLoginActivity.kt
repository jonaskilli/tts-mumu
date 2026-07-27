package com.github.jing332.tts_server_android.compose.systts.plugin.login

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.drake.net.utils.withIO
import com.drake.net.utils.withMain
import com.github.jing332.common.utils.fromCookie
import com.github.jing332.common.utils.longToast
import com.github.jing332.compose.widgets.AppWebView
import com.github.jing332.compose.widgets.AsyncCircleImage
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.ComposeActivity
import com.github.jing332.tts_server_android.compose.theme.AppTheme
import com.google.accompanist.web.LoadingState
import com.google.accompanist.web.WebViewState
import com.google.accompanist.web.rememberWebViewNavigator
import com.google.accompanist.web.rememberWebViewState
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.apache.commons.text.StringEscapeUtils

class PluginLoginActivity : ComposeActivity() {
    companion object {
        private const val TAG = "PluginLoginActivity"
        private val logger = KotlinLogging.logger(TAG)

        const val ARG_LOGIN_URL = "PluginLoginActivity.login_url"
        const val ARG_BINDING = "PluginLoginActivity.binding"
        const val ARG_DESC = "PluginLoginActivity.description"
        const val ARG_UA = "PluginLoginActivity.ua"

        const val RESULT = "PluginLoginActivity.result"
        const val OK: Int = 1

        private const val UA_PC =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36 Edg/133.0.0.0"
    }

    private var binding = ""
    private var loginUrl = ""

    private suspend fun WebView.evaluateJavascript(script: String): String = coroutineScope {
        val mutex = Mutex(true)
        var result = ""
        withMain {
            evaluateJavascript(script, object : ValueCallback<String> {
                override fun onReceiveValue(value: String?) {
                    result = value ?: ""
                    mutex.unlock()
                }

            })
        }

        logger.info { "js result: $result" }
        mutex.lock()
        StringEscapeUtils.unescapeJson(result).trimStart('"').trimEnd('"')
    }

    private suspend fun parseBinding(webView: WebView, binding: String): String = withIO {
        val split = binding.trim().split('.')

        val start = split[0]
        val end: String = split.getOrElse(1) { "" }
        val all = end.isBlank()

        when (start) {
            "cookies" -> {
                val cookies = CookieManager.getInstance().getCookie(loginUrl)
//                val cookies = webView.evaluateJavascript("document.cookie") // Deprecated; incomplete cookie.”
                if (all) cookies else cookies.fromCookie()[end] ?: ""
            }

            "locals" -> {
                webView.evaluateJavascript("localStorage.getItem('${end}')")
            }

            "sessions" -> {
                webView.evaluateJavascript("sessionStorage.getItem('${end}')")
            }

            else -> {
                ""
            }
        }

    }

    private suspend fun finished(webView: WebView, binding: String) {
        val binding = parseBinding(webView, binding).ifBlank { finish();return }
        logger.info { "binding: $binding" }
        withMain {
            setResult(OK, Intent().apply {
                putExtra(RESULT, binding)
            })
            finish()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loginUrl = intent.getStringExtra(ARG_LOGIN_URL) ?: ""
        binding = intent.getStringExtra(ARG_BINDING) ?: ""
        val description = intent.getStringExtra(ARG_DESC) ?: ""
        val ua = intent.getStringExtra(ARG_UA) ?: ""

        val userAgent = when (ua.lowercase()) {
            "pc", "" -> UA_PC
            "mobile" -> ""
            "android" -> ""
            else -> ua
        }

        if (loginUrl.isBlank() || binding.isBlank()) {
            longToast("loginUrl or binding is null")
            finish()
            return
        }
        logger.debug { "loginUrl: $loginUrl, binding: $binding, ua: $ua, userAgent: ${userAgent} desc: $description" }

        var webview: WebView? = null

        setContent {
            val state = rememberWebViewState(
                loginUrl,
                if (userAgent.isBlank()) emptyMap() else mapOf("User-Agent" to userAgent)
            )
            AppTheme {
                var title by remember { mutableStateOf(getString(R.string.login)) }
                var icon by remember { mutableStateOf<Bitmap?>(null) }

                Scaffold(topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                icon?.let {
                                    AsyncCircleImage(
                                        it,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(title, maxLines = 1)

                                    Text(
                                        description,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 3
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(enabled = state.loadingState == LoadingState.Finished, onClick = {
                                webview?.reload()
                            }) {
                                Icon(Icons.Default.Refresh, stringResource(R.string.reload))
                            }

                            IconButton(onClick = {
                                lifecycleScope.launch {
                                    finished(
                                        webview ?: return@launch, binding
                                    )
                                }
                            }) {
                                Icon(Icons.Default.Save, stringResource(R.string.save))
                            }
                        },
                    )
                }) { padding ->
                    LoginScreen(
                        modifier = Modifier.padding(padding),
                        state = state,
                         userAgent = userAgent,
                        onTitleUpdate = { title = it },
                        onIconUpdate = { icon = it },
                        onCreated = { webview = it }
                    )
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    fun LoginScreen(
        modifier: Modifier = Modifier,
        state: WebViewState,
        userAgent: String = "",
        onTitleUpdate: (String) -> Unit,
        onIconUpdate: (Bitmap) -> Unit,
        onCreated: (WebView) -> Unit,
    ) {
        val navigator = rememberWebViewNavigator()

        BackHandler(navigator.canGoBack || state.isLoading) {
            if (navigator.canGoBack) {
                navigator.navigateBack()
            } else {
                navigator.stopLoading()
            }
        }

        Column(modifier) {
            val process =
                if (state.loadingState is LoadingState.Loading) (state.loadingState as LoadingState.Loading).progress else 0f

            AnimatedVisibility(process > 0) {
                LinearProgressIndicator(
                    progress = { process },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            LaunchedEffect(state.pageIcon) {
                onIconUpdate(state.pageIcon ?: return@LaunchedEffect)
            }

            LaunchedEffect(state.pageTitle) {
                onTitleUpdate(state.pageTitle ?: return@LaunchedEffect)
            }

            AppWebView(
                modifier = Modifier.fillMaxSize(),
                captureBackPresses = false,
                state = state, navigator = navigator,
                userAgent = userAgent,
                onCreated = onCreated
            )

        }

    }
}