package com.github.jing332.tts_server_android.compose.systts.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.ComposeActivity
import com.github.jing332.tts_server_android.service.systts.help.AccountPool
import com.github.jing332.tts_server_android.service.systts.help.KeyListFile
import com.github.jing332.tts_server_android.compose.nav.NavTopAppBar
import com.github.jing332.tts_server_android.compose.theme.AppTheme

/**
 * Jet Hub 插件 UI 宿主（10-10）。
 *
 * 路线（用户令「照抄版式」）：不再用原生 Compose 一笔一笔描插件的样子——把插件那份
 * React 前端原样构建成单文件产物（assets/jethub/jethub-ui.js，React 打入），配官方
 * 手机适配层（dsh-phone.css + phone-adapt.js，来自 dsh-phone 仓），在 WebView 里跑；
 * 前端的 `rpcCall(endpoint, payload)` 经 JetHubRpcBridge 接到 app 现有账号池。
 *
 * 这样版式与插件逐像素一致（就是同一份代码），后续插件更新只需重新构建产物。
 *
 * 「+ 新建账号」：桥把请求转给本 Activity 的 launcher，拉起 JetHubLoginActivity
 * （原生 13 渠道登录流），用户登录完回来 → 重载 WebView 让插件 UI 重新读账号列表。
 */
class JetHubActivity : ComposeActivity() {
    private var webView: WebView? = null

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
                // 返回链（10-10 排查）：插件弹窗（显示列表/供应商开关/网关/Token 用量等）
                // 在桌面端靠 ESC 关闭，手机没有 ESC——系统返回要先给 WebView 机会关弹窗，
                // 没弹窗才退本页（回密钥页）。
                val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                val backCallback = remember {
                    object : androidx.activity.OnBackPressedCallback(true) {
                        override fun handleOnBackPressed() {
                            val wv = webView
                            if (wv != null) {
                                wv.evaluateJavascript("window.__jhBack && window.__jhBack()") { result ->
                                    if (result != "true") finishAfterTransition()
                                }
                            } else finishAfterTransition()
                        }
                    }
                }
                androidx.compose.runtime.DisposableEffect(backDispatcher) {
                    backDispatcher?.addCallback(backCallback)
                    onDispose { backCallback.remove() }
                }
                // 旧 BackHandler { finishAfterTransition() } 已删（10-10 返回链排查）：
                // 它与新 callback 同时 enabled 会按注册序竞争，系统返回可能绕过弹窗拦截。

                // 「新建账号」launcher：去原生登录页；回来后①补跑 addAsKey（自动建密钥分组+拉
                // 模型——此前只挂在原生池页进页时机，Jet 页直连登录成功后没人触发，TRAE 等
                // 新渠道账号落了池却没分组，10-11 实锤）②重载 WebView（插件 UI 重读账号）
                val loginLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) {
                    // 网络+文件 IO：独立线程跑，完不成也不阻塞 reload（密钥分组晚几秒出现可接受）
                    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching {
                            AccountPool.load().forEach { acc ->
                                runCatching { AccountPool.addAsKey(KeyListFile.DEFAULT_TAG_RULE_ID, acc) }
                            }
                        }
                    }
                    runCatching { webView?.reload() }
                }
                val bridge = remember {
                    JetHubRpcBridge(this).apply {
                        // provider 透传（10-10 用户定稿）：登录宿主直进该渠道登录流程，不弹选择框
                        onLaunchLogin = { provider ->
                            loginLauncher.launch(
                                Intent(this@JetHubActivity, JetHubLoginActivity::class.java)
                                    .putExtra(JetHubLoginActivity.EXTRA_PROVIDER, provider)
                            )
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        NavTopAppBar(
                            title = { Text("Jet") },
                            navigationIcon = {
                                IconButton(onClick = { finishAfterTransition() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.nav_back))
                                }
                            },
                            scrollBehavior = scrollBehavior,
                        )
                    }
                ) { padding ->
                    AndroidView(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                                JetHubRpcBridge.applySettings(this)
                                webViewClient = WebViewClient()
                                addJavascriptInterface(bridge.Impl(), "AndroidBridge")
                                bridge.attach(this)
                                loadUrl(JetHubRpcBridge.PAGE_URL)
                                webView = this
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        runCatching { webView?.destroy() }
        webView = null
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, JetHubActivity::class.java))
        }
    }
}
