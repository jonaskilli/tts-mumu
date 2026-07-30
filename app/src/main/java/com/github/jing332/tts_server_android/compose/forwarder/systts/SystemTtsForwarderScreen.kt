package com.github.jing332.tts_server_android.compose.forwarder.systts

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.jing332.common.utils.toast
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.forwarder.BasicConfigScreen
import com.github.jing332.tts_server_android.compose.forwarder.BasicForwarderScreen
import com.github.jing332.tts_server_android.compose.forwarder.ConfigViewModel
import com.github.jing332.tts_server_android.compose.forwarder.ForwarderTopAppBar
import com.github.jing332.tts_server_android.conf.SystemTtsForwarderConfig
import com.github.jing332.tts_server_android.service.forwarder.ForwarderServiceManager.switchSysTtsForwarder
import com.github.jing332.tts_server_android.service.forwarder.system.SysTtsForwarderService
import com.github.jing332.tts_server_android.ui.forwarder.SystemForwarderSwitchActivity
import com.github.jing332.tts_server_android.utils.MyTools
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemTtsForwarderScreen(cfgVM: ConfigViewModel = viewModel()) {
    val context = LocalContext.current
    var port by remember { SystemTtsForwarderConfig.port }

    BasicForwarderScreen(
        topBar = {
            var wakeLockEnabled by remember { SystemTtsForwarderConfig.isWakeLockEnabled }
            ForwarderTopAppBar(
                title = { Text(text = stringResource(id = R.string.forwarder_systts)) },
                wakeLockEnabled = wakeLockEnabled,
                onWakeLockEnabledChange = { wakeLockEnabled = it },
                onOpenWeb = { "http://localhost:${port}" }
            ) {
                MyTools.addShortcut(
                    ctx = context,
                    name = context.getString(R.string.forwarder_systts),
                    id = "switch_systts_forwarder",
                    iconResId = R.mipmap.ic_app_launcher_round,
                    launcherIntent = Intent(context, SystemForwarderSwitchActivity::class.java)
                )
            }
        },
        configScreen = {
            var isRunning by remember { mutableStateOf(SysTtsForwarderService.isRunning) }
            BasicConfigScreen(
                modifier = Modifier.fillMaxSize(),
                vm = cfgVM,
                intentFilter = android.content.IntentFilter().apply {
                    addAction(SysTtsForwarderService.ACTION_ON_LOG)
                    addAction(SysTtsForwarderService.ACTION_ON_CLOSED)
                    addAction(SysTtsForwarderService.ACTION_ON_STARTED)
                },
                actionOnLog = SysTtsForwarderService.ACTION_ON_LOG,
                actionOnClosed = SysTtsForwarderService.ACTION_ON_CLOSED,
                actionOnStarted = SysTtsForwarderService.ACTION_ON_STARTED,
                isRunning = isRunning,
                onRunningChange = { isRunning = it },
                switch = {
                    SystemTtsForwarderConfig.isAutoStart.value = !SysTtsForwarderService.isRunning
                    context.switchSysTtsForwarder()
                },
                port = port,
                onPortChange = { port = it },
                onImportToLegado = {
                    if (!SysTtsForwarderService.isRunning) {
                        context.toast(R.string.toast_forwarder_not_running)
                        return@BasicConfigScreen
                    }
                    importToLegado(context, port)
                }
            )
        }) {
        "http://localhost:${port}"
    }
}

/**
 * 生成阅读的 httpTTS 导入链接并跳转阅读APP。
 * name=应用名(包名), engine=APP包名, 端口跟随当前软件。
 * 接口格式：http://localhost:端口/api/tts?engine=包名&text={{java.encodeURI(speakText)}}&rate={{speakSpeed * 2}}&pitch=100&voice=
 */
private fun importToLegado(context: Context, port: Int) {
    val pkg = context.packageName
    val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
    val name = "$appName ($pkg)"

    val api = "http://localhost:$port/api/tts"
    val apiLegado = "http://localhost:$port/api/legado" +
            "?api=" + URLEncoder.encode(api, "UTF-8") +
            "&name=" + URLEncoder.encode(name, "UTF-8") +
            "&engine=" + URLEncoder.encode(pkg, "UTF-8") +
            "&pitch=100"

    val deepLink = "legado://import/httpTTS?src=" + URLEncoder.encode(apiLegado, "UTF-8")

    kotlin.runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)))
    }.onFailure {
        context.toast(R.string.toast_legado_import_failed)
    }
}
