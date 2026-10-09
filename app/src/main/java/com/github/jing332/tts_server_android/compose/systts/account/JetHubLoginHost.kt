package com.github.jing332.tts_server_android.compose.systts.account

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.github.jing332.tts_server_android.service.systts.help.AccountPool
import com.github.jing332.tts_server_android.service.systts.help.ChannelBootstrap
import com.github.jing332.tts_server_android.service.systts.help.ChatChannel
import com.github.jing332.tts_server_android.service.systts.help.ChatChannels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Jet 页「+ 新建账号」的轻量登录宿主（10-10 用户定稿流程）：
 * 选供应商 → 新建 → 渠道选择弹窗 → 对应登录流程 → 登录成功 finish 回 Jet 页。
 *
 * 旧实现拉起整个 AccountPoolScreen（分组头/签到/续期那套全在）——与 Jet 页（WebView
 * 插件版）两层 UI 打架，用户截图吐槽「我也是醉了」。本宿主只渲染登录链：
 * ChannelPickerDialog + 各渠道登录弹窗（照 AccountPoolScreen 的装配搬过来，
 * onDone 统一 finish；Jet 页的账号列表刷新由 loginLauncher 回调 webView.reload() 承担）。
 *
 * ⚠️ 弹窗全关（用户取消或流程结束）即 onFinished——本 Activity 只服务登录一件事，
 * 绝不停留。WEBVIEW 类需要 ActivityResult launcher，context 由宿主 Activity 传入。
 */
@Composable
fun JetHubLoginHost(context: Context, initialProvider: String, onFinished: () -> Unit) {
    // 直连模式（10-10 用户定稿）：带 initialProvider 时不弹渠道选择，直接进该渠道的
    // 登录流程；选好/失败后照样回 Jet 页。缺省（无 provider）退回渠道选择弹窗。
    var showChannelPicker by remember { mutableStateOf(initialProvider.isEmpty()) }
    // 直连的渠道立即分发：挑出 loginKind，一次 LaunchedEffect 完成分流
    LaunchedEffect(initialProvider) {
        if (initialProvider.isNotEmpty()) ChannelBootstrap.install()
    }
    var directChannel: ChatChannel? = if (initialProvider.isEmpty()) null
    else ChatChannels.byProvider(initialProvider)
    var directDispatched by remember { mutableStateOf(false) }
    var deviceLoginChannel by remember { mutableStateOf<String?>(null) }
    var credentialChannel by remember { mutableStateOf<String?>(null) }
    var qrcodeLoginOpen by remember { mutableStateOf(false) }
    var smsLoginOpen by remember { mutableStateOf<String?>(null) }
    var callbackChannel by remember { mutableStateOf<String?>(null) }
    var opencodeLoginOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // WEBVIEW 类登录需要 ActivityResult launcher（同 AccountPoolScreen 契约：
    // AccountLoginActivity 回 setResult(OK, nickname)）。直连与选择框两条路径共用。
    // ⚠️ 必须声明在使用点（直连分发 LaunchedEffect）之前——Kotlin 局部变量先声明后用
    // （CI 37964783699 实锤：Unresolved reference 'webviewLaunch'）。
    val loginLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> onFinished() } // 登录成功与取消都回 Jet 页；成功与否由 Jet 页 reload 后列表体现
    val webviewLaunch: (Intent) -> Unit = { loginLauncher.launch(it) }

    // 直连分发：把 initialProvider 当成 ChannelPickerDialog.onPick 的等价物执行一次
    LaunchedEffect(directChannel, directDispatched) {
        val ch = directChannel
        if (ch != null && !directDispatched) {
            directDispatched = true
            when (loginKindOf(ch.id)) {
                LoginFlowKind.WEBVIEW -> {
                    val triple: Triple<String?, String?, String> = withContext(Dispatchers.IO) {
                        if (ch.id == "workbuddy") {
                            val s = com.github.jing332.tts_server_android.service.systts.help.WorkbuddyChannel.fetchLoginUrl()
                            Triple(s.state, s.url, s.err)
                        } else {
                            val (state, url, err) = AccountPool.fetchLoginUrl()
                            Triple(state, url, err)
                        }
                    }
                    val (state, url, _) = triple
                    if (url == null || url.isEmpty()) {
                        onFinished()
                    } else {
                        webviewLaunch?.invoke(
                            Intent(context, AccountLoginActivity::class.java)
                                .putExtra(AccountLoginActivity.EXTRA_LOGIN_URL, url)
                                .putExtra(AccountLoginActivity.EXTRA_LOGIN_STATE, state)
                                .putExtra(AccountLoginActivity.EXTRA_PROVIDER, ch.id)
                        )
                    }
                }
                LoginFlowKind.DEVICE_CODE -> deviceLoginChannel = ch.id
                LoginFlowKind.QRCODE -> qrcodeLoginOpen = true
                LoginFlowKind.SMS -> smsLoginOpen = ch.id
                LoginFlowKind.CALLBACK -> callbackChannel = ch.id
                LoginFlowKind.OPENCODE -> opencodeLoginOpen = true
                else -> credentialChannel = ch.id
            }
        }
    }

    // 全部弹窗关闭（取消/完成）→ 回 Jet 页。本宿主不留任何停留态。
    // ⚠️ 直连模式（directChannel 在途）不算空闲：分发前的空档期（fetchLoginUrl 网络往返）
    // 任何一个弹窗 state 都还是关闭值——不加这半会进页瞬间就 finish（自查抓到）。
    LaunchedEffect(
        showChannelPicker, deviceLoginChannel, credentialChannel,
        qrcodeLoginOpen, smsLoginOpen, callbackChannel, opencodeLoginOpen, directChannel
    ) {
        val busy = showChannelPicker || deviceLoginChannel != null || credentialChannel != null ||
            qrcodeLoginOpen || smsLoginOpen != null || callbackChannel != null || opencodeLoginOpen ||
            directChannel != null
        if (!busy) onFinished()
    }

    // 登录链全走弹窗；底下垫一层主题色 Surface 防瞬时空白
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {}

    if (showChannelPicker) {
        ChannelPickerDialog(
            onDismiss = { showChannelPicker = false }, // 关闭即触发上面的 LaunchedEffect 回 Jet 页
            onPick = { ch ->
                showChannelPicker = false
                when (loginKindOf(ch.id)) {
                    LoginFlowKind.WEBVIEW -> scope.launch {
                        // WebView 登录链（10-07/10-08 定稿交互）：codebuddy 走原轮询；
                        // workbuddy 同形态不同产品（EXTRA_PROVIDER 分流，WorkbuddyChannel.pollToken）
                        val triple: Triple<String?, String?, String> = withContext(Dispatchers.IO) {
                            if (ch.id == "workbuddy") {
                                val s = com.github.jing332.tts_server_android.service.systts.help.WorkbuddyChannel.fetchLoginUrl()
                                Triple(s.state, s.url, s.err)
                            } else {
                                val (state, url, err) = AccountPool.fetchLoginUrl()
                                Triple(state, url, err)
                            }
                        }
                        val (state, url, _) = triple
                        if (url == null || url.isEmpty()) {
                            onFinished() // 拿不到登录地址无处可去，回 Jet 页
                        } else {
                            loginLauncher.launch(
                                Intent(context, AccountLoginActivity::class.java)
                                    .putExtra(AccountLoginActivity.EXTRA_LOGIN_URL, url)
                                    .putExtra(AccountLoginActivity.EXTRA_LOGIN_STATE, state)
                                    .putExtra(AccountLoginActivity.EXTRA_PROVIDER, ch.id)
                            )
                        }
                    }
                    LoginFlowKind.DEVICE_CODE -> deviceLoginChannel = ch.id
                    LoginFlowKind.QRCODE -> qrcodeLoginOpen = true
                    LoginFlowKind.SMS -> smsLoginOpen = ch.id
                    LoginFlowKind.CALLBACK -> callbackChannel = ch.id
                    LoginFlowKind.OPENCODE -> opencodeLoginOpen = true
                    else -> credentialChannel = ch.id
                }
            },
        )
    }
    deviceLoginChannel?.let { chId ->
        DeviceCodeDialog(
            provider = chId,
            onDismiss = { deviceLoginChannel = null },
            onDone = { onFinished() },
        )
    }
    credentialChannel?.let { chId ->
        CredentialDialog(
            provider = chId,
            onDismiss = { credentialChannel = null },
            onDone = { _ -> onFinished() },
        )
    }
    if (qrcodeLoginOpen) {
        QrcodeLoginDialog(
            onDismiss = { qrcodeLoginOpen = false },
            onDone = { _ -> onFinished() },
        )
    }
    smsLoginOpen?.let { smsProvider ->
        SmsLoginDialog(
            provider = smsProvider,
            onDismiss = { smsLoginOpen = null },
            onDone = { _ -> onFinished() },
        )
    }
    callbackChannel?.let { chId ->
        CallbackLoginDialog(
            provider = chId,
            onDismiss = { callbackChannel = null },
            onDone = { onFinished() },
        )
    }
    if (opencodeLoginOpen) {
        OpencodeLoginDialog(
            onDismiss = { opencodeLoginOpen = false },
            onDone = { _ -> onFinished() },
        )
    }
}
