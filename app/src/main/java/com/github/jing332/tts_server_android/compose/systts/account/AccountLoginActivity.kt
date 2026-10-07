package com.github.jing332.tts_server_android.compose.systts.account

import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.github.jing332.compose.widgets.AppWebView
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.ComposeActivity
import com.github.jing332.tts_server_android.compose.nav.NavTopAppBar
import com.github.jing332.tts_server_android.compose.theme.AppTheme
import com.github.jing332.tts_server_android.service.systts.help.AccountPool
import com.google.accompanist.web.LoadingState
import com.google.accompanist.web.rememberWebViewNavigator
import com.google.accompanist.web.rememberWebViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 账号池登录宿主（10-06）：WebView 打开 auth/state 返回的登录地址，
 * 用户在页内完成登录；同时后台轮询 auth/token（2s 间隔、5 分钟上限），
 * 拿到 access_token 即回传结果给 AccountPoolScreen（setResult），无需用户手动确认。
 */
class AccountLoginActivity : ComposeActivity() {
    companion object {
        const val EXTRA_LOGIN_URL = "account_login_url"
        // 10-07 协议修正：auth/token 改 GET ?state= 轮询，state 由 auth/state 下发、全程携带
        const val EXTRA_LOGIN_STATE = "account_login_state"
    }

    private var loginUrl: String = ""
    private var loginState: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loginUrl = intent.getStringExtra(EXTRA_LOGIN_URL) ?: ""
        loginState = intent.getStringExtra(EXTRA_LOGIN_STATE) ?: ""
        if (loginUrl.isEmpty()) {
            finish()
            return
        }

        // 轮询凭据：浏览器登录成功前 token 接口返回未授权/空，成功即返回 access_token。
        // 5 分钟上限，到点静默结束（用户可自行关闭页面）
        var polling = true
        lifecycleScope.launch {
            repeat(150) {
                if (!polling) return@launch
                val (acc, err) = withContext(Dispatchers.IO) { AccountPool.pollToken(loginState, null) }
                if (acc != null) {
                    polling = false
                    setResult(RESULT_OK, Intent().putExtra("nickname", acc.nickname))
                    finish()
                    return@launch
                }
                delay(2000)
            }
            if (polling) finish()
        }

        setContent {
            AppTheme {
                Content(onBack = {
                    polling = false
                    finish()
                })
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Content(onBack: () -> Unit) {
        val state = rememberWebViewState(url = loginUrl)
        // 刷新走 navigator（10-07 编译修复）：原 `state.view?.reload()` —— accompanist 的
        // WebViewState 没有公开 view，参照 PluginLoginActivity 用 rememberWebViewNavigator()
        val navigator = rememberWebViewNavigator()
        BackHandler { onBack() }
        Scaffold(
            topBar = {
                NavTopAppBar(
                    title = { Text(stringResource(R.string.account_pool_login)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.nav_back))
                        }
                    },
                    actions = {
                        IconButton(onClick = { navigator.reload() }) {
                            Icon(Icons.Default.Refresh, stringResource(R.string.reload))
                        }
                    },
                )
            }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val loading = state.loadingState
                if (loading is LoadingState.Loading)
                    LinearProgressIndicator(
                        progress = { loading.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                AppWebView(
                    state = state,
                    // navigator 必传（10-07 编译修复）：AppWebView 签名里它是无默认值的必填参数；
                    // captureBackPresses=false 与参照页一致——返回键由本页 BackHandler 统一接管
                    navigator = navigator,
                    captureBackPresses = false,
                    onDispose = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
