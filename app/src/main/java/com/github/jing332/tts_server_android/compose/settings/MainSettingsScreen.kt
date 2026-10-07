package com.github.jing332.tts_server_android.compose.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ManageSearch

import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.MoreHoriz
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.github.jing332.tts_server_android.conf.AppConfig
import com.github.jing332.tts_server_android.conf.SystemTtsForwarderConfig
import com.github.jing332.tts_server_android.conf.SystemTtsConfig
import androidx.core.content.ContextCompat.startActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    var query by remember { mutableStateOf("") }
    // 子页（10-05 用户令：稳定性/服务与网络/后台与保活/其他 不再是"页面内折叠"，改为设置页上的
    // 入口行 —— 点开是一个独立子页，顶栏带返回键 + 区名，卡内不再出区标题）
    var openSection by rememberSaveable { mutableStateOf<String?>(null) }
    // 主页与子页各用一份滚动状态。曾共用一份：子页内容短，挂载时把 value 夹回 0，
    // 返回设置页就跳到顶部（用户 10-05 实机反馈「点进去返回跑到顶部」）。
    // 分离后主页状态在子页期间不被挂载，位置保持；子页每次进入从头开始。
    val mainScrollState = rememberScrollState()
    val subScrollState = remember(openSection) { ScrollState(0) }
    // 子页内不做搜索过滤（子页顶栏没有搜索框）：查询强制为空 ⇒ search 恒不激活，条目全部渲染
    val search = rememberSettingsSearch(if (openSection != null) "" else query)
    // 系统返回键：子页时先退回主页
    BackHandler(enabled = openSection != null) { openSection = null }
    // 子页标题（供顶栏用；委托属性不能 smart cast，先落到局部值）
    val openSectionTitle = sectionTitle(openSection ?: "")

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
                if (openSection != null) {
                    // 子页顶栏：返回键 + 区名（无搜索框——子页内不做过滤）
                    NavTopAppBar(
                        title = { Text(openSectionTitle) },
                        navigationIcon = {
                            IconButton(onClick = { openSection = null }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    stringResource(R.string.navigate_back)
                                )
                            }
                        },
                        scrollBehavior = scrollBehaviour,
                    )
                } else {
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
                        .verticalScroll(if (openSection == null) mainScrollState else subScrollState)
                ) {
                    // ===== 我的（10-07 用户令：常用+资源管理合并，一区六项一屏看全）=====
                    // ===== 主页内容（子页打开时整块不渲染）=====
                    if (openSection == null) {
                    SettingsGroup(title = { Text("我的") }, show = !search.active()) {

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

                // 朗读与播放 / 稳定性 两区（SysttsSettingsScreen 渲染；10-05 用户令：
                // 交换键与心声 AI 都并进「朗读与播放」，故「显示与交互」「心声」两个区已撤）
                // 朗读与播放（留在主页；稳定性改独立子页，故只渲染 Loudness 部分）
                SysttsSettingsScreen(search, SysttsSettingsPart.Loudness)
                    } // 主页内容收尾

                // ===== 子页：稳定性（10-05 用户令：由"页面内折叠"改为入口行 → 独立子页）=====
                if (openSection == "stability") {
                    SysttsSettingsScreen(search, SysttsSettingsPart.Stability)
                }

                // ===== 子页：转发器（10-05 用户令：原名「服务与网络」，因区内就是转发器/端口/一键导入，改名）=====
                if (openSection == "service") {
                SettingsGroup(
                    title = {},
                    show = !search.active(),
                    showHeader = false,
                ) {

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
                } // 服务与网络子页收尾
                }

                // ===== 子页：后台与保活（10-05 用户令：原「服务与网络」混装两类，拆出后台存活类；
                // 同日再令：改为入口行 → 独立子页）=====
                // 拆分依据：本区几项都是「让进程活着」（保活/前台服务/唤醒锁/最近任务排除）；
                // 上一区是「对外服务与网络」（转发器/端口/一键导入）。
                if (openSection == "keepalive") {
                SettingsGroup(
                    title = {},
                    show = !search.active(),
                    showHeader = false,
                ) {

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

                // 最近任务排除（10-05 用户令：自「其他」迁来——它改的是系统"最近任务"里是否显示本 App，
                // 属系统集成，与本区"让进程活着"是一路）
                SettingItem(search, "最近任务", "排除", "recent", "后台") {
                var excludeFromRecent by remember { AppConfig.isExcludeFromRecent }
                SwitchPreference(
                    title = { Text(stringResource(id = R.string.exclude_from_recent)) },
                    subTitle = { Text(stringResource(id = R.string.exclude_from_recent_summary)) },
                    checked = excludeFromRecent,
                    onCheckedChange = { excludeFromRecent = it },
                    icon = { Icon(Icons.Default.HideSource, contentDescription = null) }
                )
                }
                } // 后台与保活子页收尾
                }

                // ===== 子页：其他（OtherSettingsScreen 渲染；10-05 用户令：由页面内折叠改为入口行 → 子页）=====
                if (openSection == "other") {
                    OtherSettingsScreen(search, showGroupHeader = false)
                }

                // ===== 主页尾部：4 个入口行（10-05 用户令：这些不再是"折叠"，而是点开一个独立子页）=====
                // 每项**各占一张独立卡片**（10-05 用户实机纠正：照用户所发 QQ 设置页截图，
                // 那里是"一个功能一张卡"而不是"多个共卡+分隔线"；四行同卡会看着连成一块）
                if (openSection == null) {
                    EntryRowCard(Icons.Default.HealthAndSafety, "稳定性") { openSection = "stability" }
                    EntryRowCard(Icons.Default.Lan, "转发器") { openSection = "service" }
                    EntryRowCard(Icons.Default.PowerSettingsNew, "后台与保活") { openSection = "keepalive" }
                    EntryRowCard(Icons.Default.MoreHoriz, "其他") { openSection = "other" }
                }

                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

/**
 * 子页标题（与设置页上的入口行同文案）。
 * 这些区名本来就是硬编码中文（分区标题亦然），故此处不引入 strings 键。
 */
private fun sectionTitle(key: String): String = when (key) {
    "stability" -> "稳定性"
    "service" -> "转发器"
    "keepalive" -> "后台与保活"
    "other" -> "其他"
    else -> "设置"
}

/**
 * 设置页尾部的「入口卡片」：**一项一张独立卡片**，卡内一行（图标 + 名称 + 右侧 ›）。
 * 10-05 用户实机纠正：照用户所发 QQ 设置页截图，那里是"一个功能一张卡"——
 * 最初四项共卡 + 行间分隔线的做法被否（"明明 QQ 图是两个卡片"）。
 * 卡边/底色/圆角与其它分区卡一致（SettingsGroup 的 card 分支同款）；
 * 卡与卡之间留 [verticalPadding] 的缝，与分区卡之间的节奏一致。
 */
@Composable
private fun EntryRowCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    SettingsGroup(title = {}, show = true, showHeader = false) {
        BasePreferenceWidget(
            onClick = onClick,
            title = { Text(title) },
            icon = { Icon(icon, contentDescription = null) },
        )
    }
}
