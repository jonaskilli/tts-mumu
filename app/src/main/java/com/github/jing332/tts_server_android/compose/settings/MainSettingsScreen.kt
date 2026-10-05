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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ManageSearch

import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.SettingsBackupRestore
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.backup.BackupRestoreActivity
import com.github.jing332.tts_server_android.compose.forwarder.systts.ForwarderWebDialog
import com.github.jing332.tts_server_android.compose.nav.NavTopAppBar
import com.github.jing332.tts_server_android.compose.systts.plugin.PluginManagerActivity
import com.github.jing332.tts_server_android.compose.systts.replace.ReplaceManagerActivity
import com.github.jing332.tts_server_android.compose.systts.role.KeyManagerActivity
import com.github.jing332.tts_server_android.compose.systts.speechrule.SpeechRuleManagerActivity
import com.github.jing332.tts_server_android.compose.theme.getAppTheme
import com.github.jing332.tts_server_android.compose.theme.setAppTheme
import com.github.jing332.tts_server_android.conf.SystemTtsForwarderConfig
import com.github.jing332.tts_server_android.conf.SystemTtsConfig
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
                            Spacer(Modifier.width(20.dp))
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
                    // ===== 常用（10-05 用户令：常用/重要置顶，一进页就够得着）=====
                    SettingsGroup(title = { Text("常用") }, show = !search.active()) {

                SettingItem(search, "主题", "theme", "深色", "浅色", "外观") {
                BasePreferenceWidget(
                    icon = { Icon(Icons.Default.ColorLens, null) },
                    onClick = { showThemeDialog = true },
                    title = { Text(stringResource(id = R.string.theme)) },
                    subTitle = { Text(stringResource(id = getAppTheme().stringResId)) },
                )
                }

                // 「语言」整项已删（10-05 用户令：只留中文，外语翻译五个目录一并退役）

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
                }

                // ===== 资源管理（10-05 用户令：常用，就第 2 区；四项各占一行，保留 ⋮ 同名入口）=====
                // 原名「规则与插件」，加入「密钥管理」后分区改名为「资源管理」（都是独立管理页）。
                SettingsGroup(title = { Text("资源管理") }, show = !search.active()) {
                    SettingItem(search, "朗读规则", "规则", "speech", "rule") {
                        BasePreferenceWidget(
                            // 显式 Intent：本文件 import 了 ContextCompat.startActivity（要 Intent 的静态重载），
                            // 它遮蔽 Context.startActivity(Class) —— 直接传 Class 会编译报「期望 Intent」
                            onClick = { context.startActivity(Intent(context, SpeechRuleManagerActivity::class.java)) },
                            title = { Text(stringResource(id = R.string.speech_rule_manager)) },
                            icon = { Icon(Icons.AutoMirrored.Default.MenuBook, null) }
                        )
                    }
                    SettingItem(search, "插件", "plugin", "插件管理") {
                        BasePreferenceWidget(
                            onClick = { context.startActivity(Intent(context, PluginManagerActivity::class.java)) },
                            title = { Text(stringResource(id = R.string.plugin_manager)) },
                            icon = { Icon(painterResource(id = R.drawable.ic_shortcut_plugin), null) }
                        )
                    }
                    SettingItem(search, "替换规则", "replace", "净化") {
                        BasePreferenceWidget(
                            onClick = { context.startActivity(Intent(context, ReplaceManagerActivity::class.java)) },
                            title = { Text(stringResource(id = R.string.replace_rule_manager)) },
                            icon = { Icon(Icons.AutoMirrored.Default.ManageSearch, null) }
                        )
                    }
                    // 密钥管理（10-05 用户令：补一行；角色管理页顶栏「密钥」入口保留）。
                    // tagRuleId 用 "mingwuyan"，与 RoleManagementScreen.ROLE_RULE_ID 同值——
                    // 该常量是 RoleManagementScreen 的 private，外部取不到，此处以字面量对齐
                    // （两处同值：密钥/角色数据都挂在 mingwuyan 规则目录下）。
                    SettingItem(search, "密钥", "key", "密钥管理", "接口") {
                        BasePreferenceWidget(
                            onClick = { KeyManagerActivity.start(context, "mingwuyan") },
                            title = { Text(stringResource(id = R.string.role_key_title)) },
                            icon = { Icon(Icons.Default.Key, null) }
                        )
                    }
                }

                // 朗读与播放 / 稳定性 / 心声与标签 三区（SysttsSettingsScreen 渲染）
                SysttsSettingsScreen(search)

                // ===== 服务与网络（10-05 用户令：不常用，移至倒数第二区）=====
                SettingsGroup(title = { Text("服务与网络") }, show = !search.active()) {

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
                    subTitle = { Text("将TTS转发器引擎导入至阅读") },
                    // 纯动作（跳深链），不是「进下一页」：不给右侧 ›（10-05 用户令重排时定）
                    showChevron = false,
                )
                }

                // 「直链设置」已迁往「其他」区（10-05 用户令：它本质是"导出到网盘拿直链"的 JS 规则，
                // 放"服务与网络/资源管理"都不合适；顺带解掉了"直链 vs 唤醒锁"的跨区顺序纠结）
                } // 服务与网络区收尾

                // ===== 后台与保活（10-05 用户令：原「服务与网络」混装两类，拆出后台存活类）=====
                // 拆分依据：本区三项都是「让进程活着」（保活/前台服务/唤醒锁）；
                // 上一区是「对外服务与网络」（转发器/端口/一键导入/直链）。
                SettingsGroup(title = { Text("后台与保活") }, show = !search.active()) {

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

                SettingItem(search, "前台服务", "通知", "foreground", "notification") {
                var foregroundService by remember { SystemTtsConfig.isForegroundServiceEnabled }
                SwitchPreference(
                    title = { Text(stringResource(id = R.string.foreground_service_and_notification)) },
                    subTitle = { Text(stringResource(id = R.string.foreground_service_and_notification_summary)) },
                    checked = foregroundService,
                    onCheckedChange = { foregroundService = it },
                    icon = { Icon(Icons.Default.NotificationsNone, null) }
                )
                }

                SettingItem(search, "唤醒锁", "wakelock", "锁屏") {
                var wakeLock by remember { SystemTtsConfig.isWakeLockEnabled }
                SwitchPreference(
                    title = { Text(stringResource(id = R.string.wake_lock)) },
                    subTitle = { Text(stringResource(id = R.string.wake_lock_summary)) },
                    checked = wakeLock,
                    onCheckedChange = { wakeLock = it },
                    icon = { Icon(Icons.Default.Lock, null) }
                )
                }
                } // 后台与保活区收尾

                // 「其他」区（OtherSettingsScreen 渲染，10-05 用户令默认折叠；原名「数据与关于」，
                // 同日因区内含语言而改名，随后语言项本身也退役）：
                // 最近任务排除 / 关于 / 清除网页数据 / 清空数据
                // （帮助文档、检查更新、自动检查更新、下拉数量、语言 均已退役）
                OtherSettingsScreen(search)

                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}
