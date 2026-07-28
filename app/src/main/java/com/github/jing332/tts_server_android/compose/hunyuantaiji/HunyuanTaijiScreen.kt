package com.github.jing332.tts_server_android.compose.hunyuantaiji

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.jing332.deepseekproxy.App
import com.github.jing332.deepseekproxy.ProxyViewModel

/**
 * 混元太极（本地 LLM 中转）入口页面。
 * 直接复用原版 app-proxy 的 [App] 界面（含中转 / 日志 / 浏览器三个子页），
 * 端口与「转发器」共享同一份配置（SystemTtsForwarderConfig.port），改一处两处同步。
 */
@Composable
fun HunyuanTaijiScreen() {
    val proxyVm: ProxyViewModel = viewModel()
    App(proxyVm)
}
