package com.github.jing332.tts_server_android.compose.forwarder.systts

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.forwarder.WebScreen
import com.github.jing332.tts_server_android.service.forwarder.ForwarderServiceManager.startSysTtsForwarder
import com.github.jing332.tts_server_android.service.forwarder.system.SysTtsForwarderService
import com.google.accompanist.web.WebContent
import com.google.accompanist.web.rememberSaveableWebViewState
import com.google.accompanist.web.rememberWebViewNavigator

/**
 * 第2项: 转发器网页弹窗(全屏Dialog)。
 *
 * 点击设置页"转发器"项(非开关)时触发: 若转发器未运行则自动启动,
 * 弹窗内嵌 WebView 加载 http://localhost:端口, 功能与原网页Tab一致
 * (选引擎/语音/试听/导入阅读)。日志详情已删除, 不再单独占用入口。
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ForwarderWebDialog(
    port: Int,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val url = "http://localhost:$port"
    val webState = rememberSaveableWebViewState().apply {
        content = WebContent.Url(url)
    }
    val navigator = rememberWebViewNavigator()

    // 弹窗打开时若转发器未运行则自动启动, 服务启动后网页会自动加载
    LaunchedEffect(Unit) {
        if (!SysTtsForwarderService.isRunning) {
            context.startSysTtsForwarder()
        }
        navigator.loadUrl(url)
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.forwarder_systts)) },
                    navigationIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                )
            }
        ) { padding ->
            WebScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                state = webState,
                navigator = navigator
            )
        }
    }
}
