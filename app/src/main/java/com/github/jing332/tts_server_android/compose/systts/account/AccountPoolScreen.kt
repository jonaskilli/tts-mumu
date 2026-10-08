package com.github.jing332.tts_server_android.compose.systts.account

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.jing332.common.utils.toast
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.nav.NavTopAppBar
import com.github.jing332.tts_server_android.compose.systts.OrderBadge
import com.github.jing332.tts_server_android.service.systts.help.AccountPool
import com.github.jing332.tts_server_android.service.systts.help.ChannelBootstrap
import com.github.jing332.tts_server_android.service.systts.help.ChatChannels
import com.github.jing332.tts_server_android.service.systts.help.KeyListFile
import kotlinx.coroutines.Dispatchers
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

    // 登录页结果回传：成功 = 轮询已拿到凭据并落盘，回来重读列表即可（无需手动刷新）
    val loginLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            context.toast("登录成功：${result.data?.getStringExtra("nickname") ?: ""}")
            reload()
        }
    }

    Scaffold(
        topBar = {
            NavTopAppBar(
                title = { Text(stringResource(R.string.account_pool_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.nav_back))
                    }
                },
                actions = {
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
            itemsIndexed(accounts, key = { _, acc -> acc.id }) { idx, acc ->
                AccountRow(
                    acc = acc,
                    index = idx,
                    busy = busyId == acc.id,
                    timeFmt = timeFmt,
                    onRefresh = {
                        scope.launch {
                            busyId = acc.id
                            val (updated, err) = withContext(Dispatchers.IO) { AccountPool.refreshAny(acc) }
                            busyId = null
                            if (updated != null) {
                                context.toast(R.string.account_pool_refresh_ok)
                                reload()
                            } else context.toast("续期失败：$err")
                        }
                    },
                    onCheckIn = {
                        scope.launch {
                            busyId = acc.id
                            val (ok, msg) = withContext(Dispatchers.IO) { AccountPool.checkInAny(acc) }
                            busyId = null
                            context.toast(msg)
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
                                context.toast("积分：${if (c % 1.0 == 0.0) c.toLong().toString() else c.toString()}")
                                reload()
                            } else context.toast("查询失败：$err")
                        }
                    },
                    onCopyToken = {
                        // 令牌是长串，走系统 ClipboardManager（LocalClipboardManager 对超长串无优势且此处非 Compose 作用域惯用）
                        val cb = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                as android.content.ClipboardManager
                        cb.setPrimaryClip(
                            android.content.ClipData.newPlainText("token", acc.accessToken)
                        )
                        context.toast("令牌已复制，去密钥管理添加密钥时粘贴到 Key 段")
                    },
                    onToggleEnabled = {
                        // 停用/启用（10-08 移植）：停用只退出自动选号，签到/续期照跑（插件同语义）
                        scope.launch {
                            withContext(Dispatchers.IO) { AccountPool.setEnabled(acc.id, !acc.enabled) }
                            context.toast(if (acc.enabled) "已停用「${acc.nickname}」" else "已启用「${acc.nickname}」")
                            reload()
                        }
                    },
                    onClearLimits = {
                        scope.launch {
                            val n = withContext(Dispatchers.IO) { AccountPool.clearRateLimits(acc.id) }
                            context.toast(if (n) "已清除限流标记" else "无限流标记")
                            if (n) reload()
                        }
                    },
                    onDelete = { confirmDelete = acc },
                )
                // 行间分隔线（同启用池 0.6dp 半透明；末行不画）
                if (idx < accounts.lastIndex) {
                    HorizontalDivider(
                        thickness = 0.6.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    // 删除确认弹窗（删除不可逆；连带说明：密钥条目不随删，由用户在密钥页自行管理）
    // 渠道选择弹窗（10-09 全渠道批）：选完按登录形态分流
    // 首次进页图例（乙方案）：账号行工具条的含义，只弹一次
    if (showLegend) {
        AlertDialog(
            onDismissRequest = { showLegend = false; prefs.edit().putBoolean("legend_shown", true).apply() },
            title = { Text("账号行工具条说明") },
            text = {
                Column {
                    Text("☑ 签到（每日领积分）", style = MaterialTheme.typography.bodyMedium)
                    Text("⟳ 续期（手动刷新令牌）", style = MaterialTheme.typography.bodyMedium)
                    Text("🏦 查积分（查余额）", style = MaterialTheme.typography.bodyMedium)
                    Text("⧉ 复制令牌（access_token）", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "账号落池即自动进密钥管理，无需手动添加。长按账号行：停用 / 清限流 / 删除。",
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
                            context.toast("获取登录地址失败：$err")
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
                context.toast("已添加：$nick")
                reload()
            },
        )
    }
    if (qrcodeLoginOpen) {
        QrcodeLoginDialog(
            onDismiss = { qrcodeLoginOpen = false },
            onDone = { nick ->
                qrcodeLoginOpen = false
                context.toast("已添加：$nick")
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
                context.toast("已添加：$nick")
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
                context.toast("已添加")
                reload()
            },
        )
    }
    if (opencodeLoginOpen) {
        OpencodeLoginDialog(
            onDismiss = { opencodeLoginOpen = false },
            onDone = { nick ->
                opencodeLoginOpen = false
                context.toast("已添加：$nick")
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
                        context.toast("已删除「${victim.nickname}」")
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AccountRow(
    acc: AccountPool.Account,
    index: Int,
    busy: Boolean,
    timeFmt: SimpleDateFormat,
    onRefresh: () -> Unit,
    onCheckIn: () -> Unit,
    onQueryCredits: () -> Unit,
    onCopyToken: () -> Unit,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit,
    onClearLimits: () -> Unit,
) {
    // 长按菜单（10-08 移植插件账号卡片能力）：停用/启用、清限流（有标记才显示）、删除
    var menuOpen by remember { mutableStateOf(false) }
    Column(
        // 丙案（10-10 用户拍板）：第一行 序号+昵称+状态胶囊（零图标）；第二行 信息副行；
        // 第三段 工具条（签到/续期/查积分/复制令牌 四键带文字标签整行宽）。
        // 行间分隔线由列表层画（同启用池 0.6dp 半透明）。
        // 长按 = 管理菜单（删除/停用/清限流）
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .combinedClickable(onClick = {}, onLongClick = { menuOpen = true })
    ) {
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            // 10-10 用户令（弹窗位置）：默认锚在整行 top-start = 永远弹屏幕左上角。
            // 右移下移对准动作图标区（右上角），菜单出现在长按行旁而不是屏幕角落
            offset = androidx.compose.ui.unit.DpOffset(x = (-40).dp, y = 40.dp)
        ) {
            // 乙方案（10-09 用户拍板）：长按菜单兼作图例——先列五个图标动作的文字说明
            //（点了不执行，纯查阅；图标行含义在此可见），再列管理动作（点即执行）
            DropdownMenuItem(
                text = { Text("☑ 签到（每日领积分）", style = MaterialTheme.typography.bodySmall) },
                onClick = { menuOpen = false; onCheckIn() }
            )
            DropdownMenuItem(
                text = { Text("⟳ 续期（手动刷新令牌）", style = MaterialTheme.typography.bodySmall) },
                onClick = { menuOpen = false; onRefresh() }
            )
            DropdownMenuItem(
                text = { Text("🏦 查积分（查余额）", style = MaterialTheme.typography.bodySmall) },
                onClick = { menuOpen = false; onQueryCredits() }
            )
            DropdownMenuItem(
                text = { Text("⧉ 复制令牌（access_token）", style = MaterialTheme.typography.bodySmall) },
                onClick = { menuOpen = false; onCopyToken() }
            )
            // 「添加为密钥」菜单项已撤（10-10 用户令）：登录落盘即自动进密钥池，
            // 进页还有幂等补齐，无需手动动作
            DropdownMenuItem(
                text = { Text("⸺", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outlineVariant) },
                onClick = {}
            )
            DropdownMenuItem(
                text = { Text(if (acc.enabled) "停用（不参与自动选号）" else "启用") },
                onClick = { menuOpen = false; onToggleEnabled() }
            )
            if (acc.modelRateLimits.isNotEmpty())
                DropdownMenuItem(
                    text = { Text("清除限流标记（${acc.modelRateLimits.size} 项）") },
                    onClick = { menuOpen = false; onClearLimits() }
                )
            DropdownMenuItem(
                text = { Text("删除账号", color = MaterialTheme.colorScheme.error) },
                onClick = { menuOpen = false; onDelete() }
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // 丙案（10-10 用户拍板「两行全宽图标排」）：第一行 = 序号+名字+状态胶囊，
            // **零图标**——五个动作整体下移副行下方的工具条（整行宽、带文字标签）
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 30.dp)
        )
        // 丙案工具条（10-10 用户拍板）：整行宽四键均排，图标 18dp 下带 10sp 文字标签，
        // 全部动作一键直达、谁也不进长按菜单。
        // 10-10 二令：字符字形（☑⟳⧉）有豆腐块风险且与 🏦 彩色 emoji 风格打架——
        // 换 Material 矢量图标（原行内键同款四枚），单色同字体渲染永不缺字形
        // 「添加为密钥」已退役（登录落盘即自动进密钥池，无需手动）
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolAction(Icons.Default.EventAvailable, "签到", busy) { onCheckIn() }
            ToolAction(Icons.Default.Refresh, "续期", busy) { onRefresh() }
            ToolAction(Icons.Default.Savings, "查积分", busy) { onQueryCredits() }
            ToolAction(Icons.Default.ContentCopy, "复制令牌", busy) { onCopyToken() }
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
