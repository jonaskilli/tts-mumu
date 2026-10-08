package com.github.jing332.tts_server_android.compose.systts.account

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.nav.NavTopAppBar
import com.github.jing332.tts_server_android.compose.systts.OrderBadge
import com.github.jing332.tts_server_android.compose.systts.role.FlatIconAction
import com.github.jing332.tts_server_android.service.systts.help.AccountPool
import com.github.jing332.tts_server_android.service.systts.help.ChannelBootstrap
import com.github.jing332.tts_server_android.service.systts.help.ChatChannels
import com.github.jing332.tts_server_android.service.systts.help.KeyListFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 账号池二级页（10-06，方案B）：KeyManagerScreen 顶栏入口进、页内全屏覆盖、返回键退回。
 * 账号列表（昵称/状态/过期/积分/最近签到）+ 登录新账号 + 逐账号 续期/签到/查积分。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPoolScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var accounts by remember { mutableStateOf<List<AccountPool.Account>>(emptyList()) }
    var version by remember { mutableIntStateOf(0) }
    // 操作中账号（转圈定位到行）
    var busyId by remember { mutableStateOf<String?>(null) }
    // 批量限流操作进行中（组头两键禁点；重测会逐个真发请求，耗时长）
    var busyAll by remember { mutableStateOf(false) }
    // 待确认删除的账号（10-08 移植插件「删除」动作；删除不可逆，弹窗确认）
    var confirmDelete by remember { mutableStateOf<AccountPool.Account?>(null) }
    // 渠道选择弹窗（10-09 全渠道批）：选完按登录形态分流
    var showChannelPicker by remember { mutableStateOf(false) }
    // 首次进页图例弹窗（乙方案）：SharedPreferences 记「已看过」，只弹一次
    val prefs = remember {
        context.getSharedPreferences("account_pool_ui", android.content.Context.MODE_PRIVATE)
    }
    var showLegend by remember { mutableStateOf(!prefs.getBoolean("legend_shown", false)) }
    var deviceLoginChannel by remember { mutableStateOf<String?>(null) }
    var credentialChannel by remember { mutableStateOf<String?>(null) }
    var qrcodeLoginOpen by remember { mutableStateOf(false) }
    // 短信登录（10-08 autoclaw 增）：记 provider 字符串（loomy/autoclaw），null=不弹
    var smsLoginOpen by remember { mutableStateOf<String?>(null) }
    var callbackChannel by remember { mutableStateOf<String?>(null) }
    var opencodeLoginOpen by remember { mutableStateOf(false) }
    // 系统返回拦截（10-10 实锤修复）：本页是 KeyManagerScreen 内的覆盖层（非导航路由），
    // 原先不拦返回键 → 系统返回直接 finish 整个密钥 Activity，跳过密钥页回角色管理
    androidx.activity.compose.BackHandler(onBack = onBack)
    val timeFmt = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    // 页内提示（10-10 用户令「提示要显示全部不要省略」）：原先走系统 Toast，
    // 而 Android 12+ 对纯文本 Toast **强制截断为两行**——渠道侧的长文案
    //（如 Qoder「每日 10:00 UTC+8 刷新…」上百字）只显示前半截、看完不知所以。
    // 改页内 Snackbar：不限行数自动换行，位置同在屏幕底部，读数体验不变。
    // 用 notifyJob 顶掉上一条（showSnackbar 默认是排队，连点账号会一条条积压；
    // Toast 是即时替换语义，这里保持一致）。
    val snackbarHostState = remember { SnackbarHostState() }
    var notifyJob by remember { mutableStateOf<Job?>(null) }
    fun notify(msg: String) {
        if (msg.isBlank()) return
        notifyJob?.cancel()
        notifyJob = scope.launch {
            snackbarHostState.showSnackbar(
                message = msg,
                // 长文多给读数时间（Snackbar 上限 10s）；短提示 4s 不拖沓
                duration = if (msg.length > 30) SnackbarDuration.Long else SnackbarDuration.Short,
            )
        }
    }

    LaunchedEffect(version) {
        accounts = withContext(Dispatchers.IO) {
            // 存量迁移（10-08）：旧版 addAsKey 落的裸域名会 302 空流，进页顺手修（幂等）
            runCatching { AccountPool.migrateLegacyKeyUrls(KeyListFile.DEFAULT_TAG_RULE_ID) }
            // 渠道错位迁移（10-10）：旧版 addAsKey 把 workbuddy 等渠道硬挂 CodeBuddy 上游
            // → 网关 401 + 「copilot」串组，进页顺手改回各自渠道（幂等）
            runCatching { AccountPool.migrateWrongChannelKeyUrls(KeyListFile.DEFAULT_TAG_RULE_ID) }
            // 自动落键补齐（10-10 用户令「添加为密钥」退役）：登录落盘的账号本就该在
            // 密钥管理有条目——对池内每个账号补跑 addAsKey（幂等去重，已有条目秒回）
            val loaded = AccountPool.load()
            loaded.forEach { acc ->
                runCatching { AccountPool.addAsKey(KeyListFile.DEFAULT_TAG_RULE_ID, acc) }
            }
            AccountPool.load()
        }
    }

    fun reload() { version++ }

    /** 重测某渠道全部账号（含已停用）：逐个真发最小消息，顺序执行不并发（插件同律，防假阳性） */
    fun retestAll(provider: String) {
        scope.launch {
            busyAll = true
            val (cleared, tested) = withContext(Dispatchers.IO) { AccountPool.retestAllAccounts(provider) }
            busyAll = false
            notify(if (tested == 0) "该渠道没有限流标记，无需重测" else "重测完成：${cleared} 通 / ${tested - cleared} 仍受限")
            reload()
        }
    }

    /** 重置某渠道全部账号的限流标记（不发任何请求） */
    fun resetAll(provider: String) {
        scope.launch {
            busyAll = true
            val n = withContext(Dispatchers.IO) { AccountPool.resetAllAccounts(provider) }
            busyAll = false
            notify(if (n > 0) "已重置 $n 个账号的限流标记" else "该渠道没有限流标记")
            reload()
        }
    }

    // 登录页结果回传：成功 = 轮询已拿到凭据并落盘，回来重读列表即可（无需手动刷新）
    val loginLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            notify("登录成功：${result.data?.getStringExtra("nickname") ?: ""}")
            reload()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            NavTopAppBar(
                title = { Text(stringResource(R.string.account_pool_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.nav_back))
                    }
                },
                actions = {
                    // 全部签到（10-10 用户令）：逐启用账号跑（过期的先续期），
                    // 判据/记账与每日闹钟 checkinAll 同口径（coversToday 跳过已签）
                    IconButton(
                        onClick = {
                            scope.launch {
                                busyId = "__all__"
                                val summary = withContext(Dispatchers.IO) {
                                    val list = AccountPool.load().filter { it.enabled }
                                    var ok = 0
                                    var skipped = 0
                                    list.forEach { acc ->
                                        if (com.github.jing332.tts_server_android.service.systts.help.CheckinPolicy.coversToday(acc.lastCheckinAt)) {
                                            skipped++; return@forEach
                                        }
                                        val target = if (acc.isExpired()) AccountPool.refreshAny(acc).first ?: acc else acc
                                        val (success, msg) = AccountPool.checkInAny(target)
                                        if (success) {
                                            ok++
                                            AccountPool.markCheckedIn(target.id)
                                        } else if (msg.contains("无签到接口") || msg.contains("暂不支持")) {
                                            skipped++
                                        }
                                    }
                                    if (skipped > 0) "签到 $ok/${list.size}（$skipped 个已签/跳过）"
                                    else "签到 $ok/${list.size}"
                                }
                                busyId = null
                                notify(summary)
                                reload()
                            }
                        },
                        enabled = accounts.any { it.enabled } && busyId == null
                    ) {
                        Icon(Icons.Default.EventAvailable, "全部签到")
                    }
                    IconButton(onClick = { reload() }) {
                        Icon(Icons.Default.Refresh, stringResource(R.string.reload))
                    }
                    IconButton(onClick = { showChannelPicker = true }) {
                        Icon(Icons.Default.Add, stringResource(R.string.account_pool_login))
                    }
                },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            )
        }
    ) { padding ->
        // 按渠道分组（10-10 用户令）：组头=渠道名+账号数+积分合计；组内保持落盘顺序，
        // 序号徽章跨组连续（轮换优先级口径不变）
        val grouped = remember(accounts) {
            accounts.groupBy { acc ->
                if (acc.provider == "codebuddy") "codebuddy"
                else {
                    ChannelBootstrap.install()
                    acc.provider
                }
            }
        }
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (accounts.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.account_pool_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            grouped.forEach { (provider, groupAccounts) ->
                item(key = "hdr:$provider") {
                    // 组头：渠道名 + (N) + 积分合计（只计已取到的，NaN/0 不计）
                    val total = groupAccounts.sumOf { if (it.credits > 0) it.credits else 0.0 }
                    val totalText = if (total > 0.0) {
                        " · 积分 " + (if (total % 1.0 == 0.0) total.toLong().toString() else total.toString())
                    } else ""
                    val chName = if (provider == "codebuddy") "CodeBuddy"
                    else ChatChannels.byProvider(provider)?.displayName ?: provider
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "$chName ${groupAccounts.size}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.weight(1f))
                        if (totalText.isNotEmpty()) Text(
                            totalText.removePrefix(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // 批量键（10-10 移植插件「重测所有 / 重置所有」）：对本渠道**全部**
                        // 账号（含已停用——插件同语义：停用号的消息照样发）执行。仅当该组至少
                        // 有一个账号带限流标记时才显示（没标记的批量重测一个请求都不发）。
                        if (groupAccounts.any { it.modelRateLimits.isNotEmpty() }) {
                            Spacer(Modifier.width(4.dp))
                            FlatIconAction(
                                icon = Icons.Default.Bolt,
                                contentDescription = "重测所有（真发消息验证，消耗额度）",
                                enabled = !busyAll,
                            ) { retestAll(provider) }
                            FlatIconAction(
                                icon = Icons.Default.CleaningServices,
                                contentDescription = "重置所有（直接清标记，不发请求）",
                                enabled = !busyAll,
                            ) { resetAll(provider) }
                        }
                    }
                }
                itemsIndexed(groupAccounts, key = { _, acc -> acc.id }) { idx, acc ->
                    AccountRow(
                        acc = acc,
                        index = accounts.indexOf(acc),
                        busy = busyId == acc.id || busyId == "__all__",
                    timeFmt = timeFmt,
                    onRefresh = {
                        scope.launch {
                            busyId = acc.id
                            val (updated, err) = withContext(Dispatchers.IO) { AccountPool.refreshAny(acc) }
                            busyId = null
                            if (updated != null) {
                                notify(context.getString(R.string.account_pool_refresh_ok))
                                reload()
                            } else notify("续期失败：$err")
                        }
                    },
                    onCheckIn = {
                        scope.launch {
                            busyId = acc.id
                            val (ok, msg) = withContext(Dispatchers.IO) { AccountPool.checkInAny(acc) }
                            busyId = null
                            notify(msg)
                            if (ok) reload()
                        }
                    },
                    onQueryCredits = {
                        scope.launch {
                            busyId = acc.id
                            val (c, err) = withContext(Dispatchers.IO) { AccountPool.queryCreditsAny(acc) }
                            busyId = null
                            if (c >= 0) {
                                // 余额=资源包合计，实测含小数（如 3930.73），整数位不打 .0
                                notify("积分：${if (c % 1.0 == 0.0) c.toLong().toString() else c.toString()}")
                                reload()
                            } else notify("查询失败：$err")
                        }
                    },
                    onToggleEnabled = {
                        // 停用/启用（10-08 移植）：停用只退出自动选号，签到/续期照跑（插件同语义）
                        scope.launch {
                            withContext(Dispatchers.IO) { AccountPool.setEnabled(acc.id, !acc.enabled) }
                            notify(if (acc.enabled) "已停用「${acc.nickname}」" else "已启用「${acc.nickname}」")
                            reload()
                        }
                    },
                    onRetest = {
                        // 重测（10-10 移植插件 account-probe）：对每个带限流标记的模型**真发一条
                        // 最小消息**，通了的才清标记；仍受限的把上游新解禁时刻写回。会消耗额度。
                        scope.launch {
                            busyId = acc.id
                            val r = withContext(Dispatchers.IO) { AccountPool.retestAccount(acc.id) }
                            busyId = null
                            when {
                                r.error.isNotEmpty() -> notify("重测失败：${r.error}")
                                r.tested == 0 -> notify("该账号没有限流标记，无需重测")
                                else -> {
                                    notify("重测「${acc.nickname}」：${r.cleared.size} 通 / ${r.stillLimited.size} 仍受限")
                                    reload()
                                }
                            }
                        }
                    },
                    onResetLimits = {
                        // 重置（不发送任何请求）：直接清掉该账号全部限流标记
                        scope.launch {
                            val n = withContext(Dispatchers.IO) { AccountPool.clearRateLimits(acc.id) }
                            notify(if (n) "已重置限流标记" else "无限流标记")
                            if (n) reload()
                        }
                    },
                    onDelete = { confirmDelete = acc },
                )
                // 行间分隔线（同启用池 0.6dp 半透明；组末行不画——组头自带上边距分区）
                if (idx < groupAccounts.lastIndex) {
                    HorizontalDivider(
                        thickness = 0.6.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                }
                }
            }
        }
    }

    // 首次进页图例（乙方案）：账号行各键含义，只弹一次
    if (showLegend) {
        AlertDialog(
            onDismissRequest = { showLegend = false; prefs.edit().putBoolean("legend_shown", true).apply() },
            title = { Text("账号行说明") },
            text = {
                Column {
                    Text("账号名右侧：⏻ 停用/启用（不参与自动选号）· 🗑 删除", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("下方工具条：签到 · 续期 · 查积分（有限流标记时多「重测」「重置」）", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("重测 = 真发一条消息验证能否恢复，通了的才清标记（消耗额度）；重置 = 直接清掉限流标记。组头同款两键作用于该平台全部账号。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "账号落池即自动进密钥管理，无需手动添加。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLegend = false; prefs.edit().putBoolean("legend_shown", true).apply() }) {
                    Text("知道了")
                }
            },
        )
    }

    if (showChannelPicker) {
        ChannelPickerDialog(
            onDismiss = { showChannelPicker = false },
            onPick = { ch ->
                showChannelPicker = false
                when (loginKindOf(ch.id)) {
                    LoginFlowKind.WEBVIEW -> scope.launch {
                        // WebView 登录链（10-07/10-08 定稿交互）：codebuddy 走原轮询；
                        // workbuddy 同形态不同产品（EXTRA_PROVIDER 分流，WorkbuddyChannel.pollToken）
                        val (state, url, err) = withContext(Dispatchers.IO) {
                            if (ch.id == "workbuddy") {
                                val s = com.github.jing332.tts_server_android.service.systts.help.WorkbuddyChannel.fetchLoginUrl()
                                Triple(s.state, s.url, s.err)
                            } else {
                                val (state, url, err) = AccountPool.fetchLoginUrl()
                                Triple(state, url, err)
                            }
                        }
                        if (url == null || url.isEmpty()) {
                            notify("获取登录地址失败：$err")
                        } else {
                            loginLauncher.launch(
                                android.content.Intent(context, AccountLoginActivity::class.java)
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
            onDone = { reload() },
        )
    }
    credentialChannel?.let { chId ->
        CredentialDialog(
            provider = chId,
            onDismiss = { credentialChannel = null },
            onDone = { nick ->
                credentialChannel = null
                notify("已添加：$nick")
                reload()
            },
        )
    }
    if (qrcodeLoginOpen) {
        QrcodeLoginDialog(
            onDismiss = { qrcodeLoginOpen = false },
            onDone = { nick ->
                qrcodeLoginOpen = false
                notify("已添加：$nick")
                reload()
            },
        )
    }
    smsLoginOpen?.let { smsProvider ->
        SmsLoginDialog(
            provider = smsProvider,
            onDismiss = { smsLoginOpen = null },
            onDone = { nick ->
                smsLoginOpen = null
                notify("已添加：$nick")
                reload()
            },
        )
    }
    callbackChannel?.let { chId ->
        CallbackLoginDialog(
            provider = chId,
            onDismiss = { callbackChannel = null },
            onDone = {
                callbackChannel = null
                notify("已添加")
                reload()
            },
        )
    }
    if (opencodeLoginOpen) {
        OpencodeLoginDialog(
            onDismiss = { opencodeLoginOpen = false },
            onDone = { nick ->
                opencodeLoginOpen = false
                notify("已添加：$nick")
                reload()
            },
        )
    }

    confirmDelete?.let { victim ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("删除账号") },
            text = { Text("确定删除「${victim.nickname}」？删除后自动选号不再使用该账号；已添加的密钥条目不受影响。") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { AccountPool.remove(victim.id) }
                        confirmDelete = null
                        notify("已删除「${victim.nickname}」")
                        reload()
                    }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun AccountRow(
    acc: AccountPool.Account,
    index: Int,
    busy: Boolean,
    timeFmt: SimpleDateFormat,
    onRefresh: () -> Unit,
    onCheckIn: () -> Unit,
    onQueryCredits: () -> Unit,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit,
    onRetest: () -> Unit,
    onResetLimits: () -> Unit,
) {
    Column(
        // 丙案（10-10 用户拍板）：第一行 序号+昵称+状态胶囊（零图标）；第二行 信息副行；
        // 第三段 工具条（签到/续期/查积分/复制令牌 四键带文字标签整行宽）。
        // 行间分隔线由列表层画（同启用池 0.6dp 半透明）。
        // 10-10 二令（用户）：长按菜单整体取消——启停/删除不再藏菜单，提到卡面第一行右端
        //（两个行内图标键）；菜单里有价值的细项（复制令牌 / 清除限流）并入下方工具条。
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // 第一行 = 序号 + 名字 + 状态胶囊 + 两个管理图标（⏻ 启停 / 🗑 删除）。
            // 频率高的签到/续期/查积分仍在下方工具条（整行宽、带文字标签）。
            OrderBadge(number = index + 1)
            Spacer(Modifier.width(10.dp))
            Text(
                acc.nickname,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            // 状态胶囊（有效/已过期/已停用）：停用优先显示（停用号不参与自动选号）
            when {
                !acc.enabled -> StatusChip("已停用", MaterialTheme.colorScheme.surfaceVariant)
                acc.isExpired() -> StatusChip("已过期", MaterialTheme.colorScheme.errorContainer)
                else -> StatusChip("有效", MaterialTheme.colorScheme.primaryContainer)
            }
            // 管理键（10-10 用户令：原长按菜单的启停/删除提到卡面，一眼可见可点）
            // 复用密钥页 FlatIconAction 口径：18dp 图标 / 36dp 热区
            Spacer(Modifier.width(2.dp))
            FlatIconAction(
                icon = Icons.Default.PowerSettingsNew,
                contentDescription = if (acc.enabled) "停用（不参与自动选号）" else "启用",
                enabled = !busy,
            ) { onToggleEnabled() }
            FlatIconAction(
                icon = Icons.Default.Delete,
                contentDescription = "删除账号",
                tint = MaterialTheme.colorScheme.error,
                enabled = !busy,
            ) { onDelete() }
        }
        // 副行：过期/积分/签到时间/限流（缩进对齐名字列 = 徽章 20 + 间距 10 = 30dp，同启用池）
        Spacer(Modifier.height(2.dp))
        Text(
            buildString {
                // 渠道名（10-09 全渠道批）：非 codebuddy 显示 displayName（渠道与插件 id 同名）
                if (acc.provider != "codebuddy") {
                    ChannelBootstrap.install()
                    append("[").append(ChatChannels.byProvider(acc.provider)?.displayName ?: acc.provider).append("] ")
                }
                append("过期：")
                append(if (acc.expiresAt > 0) timeFmt.format(Date(acc.expiresAt)) else "未知")
                // 积分 Double 保小数（余额=资源包合计，实测 3930.73 这类）；0 显示「未知」
                if (acc.credits != 0.0) {
                    append(" · 积分 ")
                    append(if (acc.credits % 1.0 == 0.0) acc.credits.toLong().toString() else acc.credits.toString())
                    // 分池（10-10 移植插件，用户令「一个平台下方展示两个池」）：支持的渠道
                    // （codebuddy/workbuddy/trae/lobsterai/loomy/raccoon）在合计后跟
                    // 「长期 X · 临时 Y」——临时 = 距到期 <15 天、再不用就作废的部分，优先消耗。
                    // 两值均 -1 = 该渠道没有这个维度，整段不显示（不凭空造数）。
                    // 池名标签：loomy 服务端那个池就叫「永久积分」，其余渠道用「长期」（插件同口径）。
                    if (acc.permanentCredits >= 0.0 && acc.ephemeralCredits >= 0.0) {
                        val longLabel = if (acc.provider == "loomy") "永久" else "长期"
                        append("　").append(longLabel).append(" ")
                        append(if (acc.permanentCredits % 1.0 == 0.0) acc.permanentCredits.toLong().toString() else acc.permanentCredits.toString())
                        append(" · 临时 ")
                        append(if (acc.ephemeralCredits % 1.0 == 0.0) acc.ephemeralCredits.toLong().toString() else acc.ephemeralCredits.toString())
                    }
                }
                if (acc.lastCheckinAt > 0) append(" · 签到 ${timeFmt.format(Date(acc.lastCheckinAt))}")
                // 限流中（10-08 移植）：模型名+解禁时刻；已解禁的不显示（标记自然失效）
                val now = System.currentTimeMillis()
                val active = acc.modelRateLimits.filterValues { it > now }
                if (active.isNotEmpty()) {
                    val fmt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    append(" · 限流 " + active.entries.joinToString("、") { (m, t) -> "$m→${fmt.format(Date(t))}" })
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // 10-10：加了「长期/临时」分池后内容变长（渠道+过期+积分+分池+签到+限流），
            // 单行必然截断——放开到两行，信息完整可读（用户令「要显示全部不要省略」同口径）
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 30.dp)
        )
        // 丙案工具条（10-10 用户拍板）：整行宽均排，图标 18dp 下带 10sp 文字标签，
        // 全部动作一键直达、谁也不进长按菜单。
        // 10-10 二令：字符字形（☑⟳⧉）有豆腐块风险且与 🏦 彩色 emoji 风格打架——
        // 换 Material 矢量图标（原行内键同款四枚），单色同字体渲染永不缺字形
        // 10-10 四令（移植插件 account-probe）：两个限流键**仅在有标记时出现**（没标记
        // 重测一个请求都不发、秒回，显示只会让人以为按钮失灵）——
        //   重测 = 真发一条最小消息验真，通了的清标记（会消耗额度）；
        //   重置 = 不发送任何请求，直接清掉全部标记。
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolAction(Icons.Default.EventAvailable, "签到", busy) { onCheckIn() }
            ToolAction(Icons.Default.Refresh, "续期", busy) { onRefresh() }
            ToolAction(Icons.Default.Savings, "查积分", busy) { onQueryCredits() }
            if (acc.modelRateLimits.isNotEmpty()) {
                ToolAction(Icons.Default.Bolt, "重测", busy) { onRetest() }
                ToolAction(Icons.Default.CleaningServices, "重置", busy) { onResetLimits() }
            }
        }
    }
}

/** 丙案工具条键：Material 矢量图标 18dp + 10sp 标签纵排。busy 时整体禁点防连击 */
@Composable
private fun ToolAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    busy: Boolean,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = !busy, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun StatusChip(text: String, bg: androidx.compose.ui.graphics.Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
// FlatIconAction（10-07~10-10 行内图标键）随丙案工具条退役：动作键改 ToolAction
// （字符图标+10sp 文字标签纵排），见 AccountRow。
