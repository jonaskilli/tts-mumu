package com.github.jing332.tts_server_android.compose.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.ArrowCircleUp

import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.DropdownMenuItem
import android.content.IntentFilter
import androidx.compose.foundation.clickable
import androidx.compose.material3.Switch
import androidx.compose.ui.Alignment
import com.github.jing332.compose.widgets.LocalBroadcastReceiver
import com.github.jing332.compose.widgets.TextFieldDialog
import com.github.jing332.common.utils.toast
import com.github.jing332.tts_server_android.service.forwarder.system.SysTtsForwarderService
import com.github.jing332.tts_server_android.service.forwarder.ForwarderServiceManager.switchSysTtsForwarder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.github.jing332.tts_server_android.AppLocale
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.app
import com.github.jing332.tts_server_android.compose.backup.BackupRestoreActivity
import com.github.jing332.tts_server_android.compose.forwarder.systts.ForwarderWebDialog
import com.github.jing332.tts_server_android.compose.nav.NavTopAppBar
import com.github.jing332.tts_server_android.compose.systts.directlink.LinkUploadRuleActivity
import com.github.jing332.tts_server_android.compose.theme.getAppTheme
import com.github.jing332.tts_server_android.compose.theme.setAppTheme
import com.github.jing332.tts_server_android.conf.AppConfig
import com.github.jing332.tts_server_android.conf.SystemTtsForwarderConfig
import androidx.core.content.ContextCompat.startActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    var query by remember { mutableStateOf("") }
    val search = rememberSettingsSearch(query)

    var showThemeDialog by remember { mutableStateOf(false) }
    if (showThemeDialog)
        ThemeSelectionDialog(
            onDismissRequest = { showThemeDialog = false },
            currentTheme = getAppTheme(),
            onChangeTheme = {
                setAppTheme(it)
            }
        )

    val scrollBehaviour = TopAppBarDefaults.pinnedScrollBehavior()

        // 转发器运行状态（供设置页开关实时显示）
        var forwarderRunning by remember { mutableStateOf(SysTtsForwarderService.isRunning) }
        LocalBroadcastReceiver(
            intentFilter = IntentFilter().apply {
                addAction(SysTtsForwarderService.ACTION_ON_STARTED)
                addAction(SysTtsForwarderService.ACTION_ON_CLOSED)
            }
        ) { intent ->
            when (intent?.action) {
                SysTtsForwarderService.ACTION_ON_STARTED -> forwarderRunning = true
                SysTtsForwarderService.ACTION_ON_CLOSED -> forwarderRunning = false
            }
        }

        // 第3项: 转发器端口快捷编辑(无需进入转发器页即可修改端口)
        var forwarderPort by remember { SystemTtsForwarderConfig.port }
        var showPortDialog by remember { mutableStateOf(false) }
        if (showPortDialog) {
            var portText by remember { mutableStateOf(forwarderPort.toString()) }
            TextFieldDialog(
                title = stringResource(id = R.string.listen_port),
                text = portText,
                onTextChange = { portText = it.filter { c -> c.isDigit() } },
                onDismissRequest = { showPortDialog = false },
                onConfirm = {
                    portText.toIntOrNull()?.let { p ->
                        if (p in 1..65535) forwarderPort = p
                    }
                    showPortDialog = false
                }
            )
        }

        // 第2项: 转发器网页弹窗(点击转发器项非开关时触发, 自动启动+内嵌WebView)
        var showForwarderWebDialog by remember { mutableStateOf(false) }
        if (showForwarderWebDialog) {
            ForwarderWebDialog(
                port = forwarderPort,
                onDismissRequest = { showForwarderWebDialog = false }
            )
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            modifier = Modifier.nestedScroll(scrollBehaviour.nestedScrollConnection),
            topBar = {
                NavTopAppBar(
                    title = {
                        // 标题右侧嵌入紧凑搜索框，占满顶栏剩余宽度
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.settings))
                            Spacer(Modifier.width(12.dp))
                            SettingsSearchField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier.weight(1f),
                                compact = true
                            )
                        }
                    },
                    scrollBehavior = scrollBehaviour,
                )
            }
        ) { paddingValues ->
            val context = LocalContext.current
            Column(
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (!search.active())
                        DividerPreference { Text(stringResource(id = R.string.app_name)) }

            // 后台保活设置入口（使用 Activity 启动，与备份恢复保持一致）
            SettingItem(search, "保活", "keepalive", "后台", "alive", "自启动") {
                BasePreferenceWidget(
                    onClick = {
                        context.startActivity(
                            Intent(context, KeepAliveSettingsActivity::class.java)
                        )
                    },
                    title = { Text(stringResource(id = R.string.keep_alive_settings)) },
                    subTitle = { Text(stringResource(R.string.keep_alive_settings_summary)) },
                    icon = { Icon(Icons.Default.PowerSettingsNew, null) }
                )
            }

                SettingItem(search, "备份", "恢复", "backup", "restore") {
                BasePreferenceWidget(
                    icon = {
                        Icon(Icons.Default.SettingsBackupRestore, null)
                    },
                    onClick = {
                        context.startActivity(
                            Intent(
                                context,
                                BackupRestoreActivity::class.java
                            ).apply { action = Intent.ACTION_VIEW })
                    },
                    title = { Text(stringResource(id = R.string.backup_restore)) },
                )
                }

                SettingItem(search, "直链", "directlink", "链接", "direct") {
                BasePreferenceWidget(
                    icon = {
                        Icon(Icons.Default.Link, null)
                    },
                    onClick = {
                        context.startActivity(
                            Intent(
                                context, LinkUploadRuleActivity::class.java
                            ).apply { action = Intent.ACTION_VIEW })
                    },
                    title = { Text(stringResource(id = R.string.direct_link_settings)) },
                )
                }

                // 转发器（从设置进入，底栏不再单独占用一栏）
                SettingItem(search, "转发器", "forwarder", "服务器") {
                BasePreferenceWidget(
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_app_notification),
                            contentDescription = null
                        )
                    },
                    onClick = {
                        // 第2项: 点击转发器项(非开关)自动启动转发器并弹出网页弹窗
                        // (功能与原网页Tab一致, 日志详情已删除)
                        showForwarderWebDialog = true
                    },
                    title = { Text(stringResource(id = R.string.forwarder_systts)) },
                    subTitle = {
                        Text(
                            if (forwarderRunning) stringResource(id = R.string.forwarder_running)
                            else stringResource(id = R.string.forwarder_stopped)
                        )
                    },
                    content = {
                        Switch(
                            checked = forwarderRunning,
                            onCheckedChange = null,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .clickable {
                                    // 第3项: 与转发器详情页一致, 先写“记忆启动”状态再切换服务,
                                    // 保证 App/开机重启时按记忆恢复, 而非“一直启动”
                                    SystemTtsForwarderConfig.isAutoStart.value =
                                        !SysTtsForwarderService.isRunning
                                    context.switchSysTtsForwarder()
                                }
                        )
                    }
                )
                }

                // 第3项: 转发器端口快捷入口(点击弹窗改端口, 无需进入转发器页面)
                SettingItem(search, "端口", "port", "监听") {
                BasePreferenceWidget(
                    onClick = { showPortDialog = true },
                    icon = { Icon(Icons.Default.Lan, null) },
                    title = { Text(stringResource(id = R.string.listen_port)) },
                    subTitle = { Text(forwarderPort.toString()) }
                )
                }

                SettingItem(search, "导入", "阅读", "legado", "一键", "引擎") {
                BasePreferenceWidget(
                    onClick = {
                        // 第10项: 导入到阅读前必须强制开启转发器, 否则阅读无法访问接口
                        // 之前修改有误(仅跳转深链但未启动服务), 此处先确保转发器运行再导入
                        if (!SysTtsForwarderService.isRunning) {
                            SystemTtsForwarderConfig.isAutoStart.value = true
                            context.switchSysTtsForwarder()
                        }
                        val pkg = context.packageName
                        val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
                        val name = "$appName ($pkg)"
                        val api = "http://localhost:$forwarderPort/api/tts"
                        val apiLegado = "http://localhost:$forwarderPort/api/legado" +
                                "?api=" + java.net.URLEncoder.encode(api, "UTF-8") +
                                "&name=" + java.net.URLEncoder.encode(name, "UTF-8") +
                                "&engine=" + java.net.URLEncoder.encode(pkg, "UTF-8") +
                                "&pitch=100"
                        val deepLink = "legado://import/httpTTS?src=" + java.net.URLEncoder.encode(apiLegado, "UTF-8")
                        kotlin.runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)))
                        }.onFailure {
                            context.toast(R.string.toast_legado_import_failed)
                        }
                    },
                    icon = { Icon(Icons.Default.Input, null) },
                    title = { Text("一键导入") },
                    subTitle = { Text("将TTS转发器引擎导入至阅读") }
                )
                }

                SettingItem(search, "主题", "theme", "深色", "浅色", "外观") {
                BasePreferenceWidget(
                    icon = { Icon(Icons.Default.ColorLens, null) },
                    onClick = { showThemeDialog = true },
                    title = { Text(stringResource(id = R.string.theme)) },
                    subTitle = { Text(stringResource(id = getAppTheme().stringResId)) },
                )
                }

                val languageKeys = remember {
                    mutableListOf("").apply { addAll(AppLocale.localeMap.keys.toList()) }
                }

                val languageNames = remember {
                    AppLocale.localeMap.map { "${it.value.displayName} - ${it.value.getDisplayName(it.value)}" }
                        .toMutableList()
                        .apply { add(0, context.getString(R.string.follow_system)) }
                }

                var langMenu by remember { mutableStateOf(false) }
                SettingItem(search, "语言", "language", "locale", "地区") {
                DropdownPreference(
                    Modifier.minimumInteractiveComponentSize(),
                    expanded = langMenu,
                    onExpandedChange = { langMenu = it },
                    icon = {
                        Icon(Icons.Default.Language, null)
                    },
                    title = { Text(stringResource(id = R.string.language)) },
                    subTitle = {
                        Text(
                            if (AppLocale.getLocaleCodeFromFile(context).isEmpty()) {
                                stringResource(id = R.string.follow_system)
                            } else {
                                AppLocale.getLocaleFromFile(context).displayName
                            }
                        )
                    }) {
                    languageNames.forEachIndexed { index, name ->
                        DropdownMenuItem(
                            text = {
                                Text(name)
                            }, onClick = {
                                langMenu = false

                                AppLocale.saveLocaleCodeToFile(context, languageKeys[index])
                                AppLocale.setLocale(app as Context)
                            }
                        )
                    }
                }
                }

                SettingItem(search, "更新", "update", "检查", "自动") {
                var autoCheck by remember { AppConfig.isAutoCheckUpdateEnabled }
                SwitchPreference(
                    title = { Text(stringResource(id = R.string.auto_check_update)) },
                    subTitle = { Text(stringResource(id = R.string.check_update_summary)) },
                    checked = autoCheck,
                    onCheckedChange = { autoCheck = it },
                    icon = {
                        Icon(Icons.Default.ArrowCircleUp, contentDescription = null)
                    }
                )
                }

                SettingItem(search, "最近任务", "排除", "recent", "后台") {
                var excludeFromRecent by remember { AppConfig.isExcludeFromRecent }
                SwitchPreference(
                    title = { Text(stringResource(id = R.string.exclude_from_recent)) },
                    subTitle = { Text(stringResource(id = R.string.exclude_from_recent_summary)) },
                    checked = excludeFromRecent,
                    onCheckedChange = { excludeFromRecent = it },
                    icon = {
                        Icon(Icons.Default.HideSource, contentDescription = null)
                    }
                )
                }

                SettingItem(search, "下拉", "spinner", "数量", "菜单") {
                var maxDropdownCount by remember { AppConfig.spinnerMaxDropDownCount }
                SliderPreference(
                    title = { Text(stringResource(id = R.string.spinner_drop_down_max_count)) },
                    subTitle = { Text(stringResource(id = R.string.spinner_drop_down_max_count_summary)) },
                    value = maxDropdownCount.toFloat(),
                    onValueChange = { maxDropdownCount = it.toInt() },
                    label = if (maxDropdownCount == 0) stringResource(id = R.string.unlimited) else maxDropdownCount.toString(),
                    valueRange = 0f..50f,
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuOpen, null) }
                )
                }

                SysttsSettingsScreen(search)
                OtherSettingsScreen(search)

                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}
