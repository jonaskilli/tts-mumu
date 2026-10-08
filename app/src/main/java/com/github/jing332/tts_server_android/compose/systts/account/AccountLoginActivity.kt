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
        // 10-09 全渠道批：provider 决定轮询函数（缺省 codebuddy；workbuddy 走 WorkbuddyChannel）
        const val EXTRA_PROVIDER = "account_login_provider"
    }

    private var loginUrl: String = ""
    private var loginState: String = ""
    private var provider: String = "codebuddy"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loginUrl = intent.getStringExtra(EXTRA_LOGIN_URL) ?: ""
        loginState = intent.getStringExtra(EXTRA_LOGIN_STATE) ?: ""
        provider = intent.getStringExtra(EXTRA_PROVIDER) ?: "codebuddy"
        if (loginUrl.isEmpty()) {
            finish()
            return
        }
        // 清 Cookie（10-10 实锤：WebView 共享全局 CookieManager，登录第二个号时页面上
        // 还带着第一个号的会话，轮询 2s 内拿到的是旧号 token → 同身份被判「已更新」，
        // 永远加不进第二个号）。在 WebView 创建前清一次，不影响已登录的其他 app。
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        android.webkit.CookieManager.getInstance().flush()

        // 轮询凭据：浏览器登录成功前 token 接口返回未授权/空，成功即返回 access_token。
        // 5 分钟上限，到点静默结束（用户可自行关闭页面）
        // 10-08 排障：起点/终点进 App 日志页（手机直接看，不用连电脑）
        com.github.jing332.tts_server_android.SysttsLogger.log(
            com.github.jing332.common.LogEntry(
                level = android.util.Log.INFO,
                time = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                message = "[Jet] 登录轮询启动（上限 5 分钟）——完成登录后本页会自动关闭"
            )
        )
        var polling = true
        lifecycleScope.launch {
            repeat(150) {
                if (!polling) return@launch
                // provider 分流（10-09 全渠道批）：codebuddy 走原轮询+落盘；
                // workbuddy 走 WorkbuddyChannel.pollToken（响应相对秒换算），成功经 upsert 落盘
                // 10-10 登录去重：identity = access_token 的 JWT sub（pollToken 不回 user_id，
                // 规格书 §2.2 的 GET /v2/plugin/login/account 才有——本期 pragmatic 取 sub；
                // 解不出传 null = 诚实降级不去重）。身份值同时写进 extra._uid
                val nickname: String? = withContext(Dispatchers.IO) {
                    when (provider) {
                        "workbuddy" -> {
                            val q = com.github.jing332.tts_server_android.service.systts.help.WorkbuddyChannel.pollToken(loginState)
                            if (q.status == "OK") {
                                val acc = com.github.jing332.tts_server_android.service.systts.help.AccountPool.upsert(
                                    "workbuddy",
                                    com.github.jing332.tts_server_android.service.systts.help.AccountPool.jwtSub(q.accessToken),
                                ) { existing ->
                                    com.github.jing332.tts_server_android.service.systts.help.AccountPool.Account(
                                        id = existing?.id ?: "workbuddy-${System.currentTimeMillis().toString(16)}",
                                        provider = "workbuddy",
                                        nickname = existing?.nickname ?: "WorkBuddy",
                                        accessToken = q.accessToken,
                                        refreshToken = q.refreshToken,
                                        expiresAt = q.expiresAt,
                                    ).withExtra(
                                        "_uid",
                                        com.github.jing332.tts_server_android.service.systts.help.AccountPool.jwtSub(q.accessToken) ?: ""
                                    )
                                }
                                if (acc.isUpdate) "WorkBuddy(已更新)" else acc.nickname
                            } else null
                        }
                        else -> com.github.jing332.tts_server_android.service.systts.help.AccountPool
                            .pollToken(loginState, null).let { (acc, _) ->
                                // 10-10 登录去重：走更新路径时昵称旁标「(已更新)」
                                acc?.let { if (it.isUpdate) "${it.nickname}(已更新)" else it.nickname }
                            }
                    }
                }
                if (nickname != null) {
                    polling = false
                    setResult(RESULT_OK, Intent().putExtra("nickname", nickname))
                    finish()
                    return@launch
                }
                delay(2000)
            }
            if (polling) {
                com.github.jing332.tts_server_android.SysttsLogger.log(
                    com.github.jing332.common.LogEntry(
                        level = android.util.Log.WARN,
                        time = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        message = "[Jet] 登录轮询 5 分钟超时退出（未拿到凭据）——各次轮询失败原因见上方[Jet]日志"
                    )
                )
                finish()
            }
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
