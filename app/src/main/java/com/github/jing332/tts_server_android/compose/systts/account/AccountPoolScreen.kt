package com.github.jing332.tts_server_android.compose.systts.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.CircularProgressIndicator
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
import com.github.jing332.common.utils.toast
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.nav.NavTopAppBar
import com.github.jing332.tts_server_android.compose.systts.OrderBadge
import com.github.jing332.tts_server_android.service.systts.help.AccountPool
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
    val timeFmt = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    LaunchedEffect(version) {
        accounts = withContext(Dispatchers.IO) { AccountPool.load() }
    }

    fun reload() { version++ }

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
                    IconButton(onClick = {
                        scope.launch {
                            val (url, err) = withContext(Dispatchers.IO) { AccountPool.fetchLoginUrl() }
                            if (url == null) {
                                context.toast("获取登录地址失败：$err")
                            } else {
                                context.startActivity(
                                    android.content.Intent(context, AccountLoginActivity::class.java)
                                        .putExtra(AccountLoginActivity.EXTRA_LOGIN_URL, url)
                                )
                            }
                        }
                    }) {
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
                            val (updated, err) = withContext(Dispatchers.IO) { AccountPool.refresh(acc) }
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
                            val (ok, msg) = withContext(Dispatchers.IO) { AccountPool.checkIn(acc) }
                            busyId = null
                            context.toast(msg)
                            if (ok) reload()
                        }
                    },
                    onQueryCredits = {
                        scope.launch {
                            busyId = acc.id
                            val (c, err) = withContext(Dispatchers.IO) { AccountPool.queryCredits(acc) }
                            busyId = null
                            if (c >= 0) {
                                context.toast("积分：$c")
                                reload()
                            } else context.toast("查询失败：$err")
                        }
                    },
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
) {
    Column(
        // 10-07 装机反馈：照启用池 PoolRow 同款两行式——第一行 序号徽章+昵称+状态+图标动作区，
        // 第二行 信息副行；整行不再可点（原「点行=查积分」易误触，动作全走图标键）。
        // 行间分隔线由列表层画（同启用池 0.6dp 半透明）
        Modifier.fillMaxWidth().padding(vertical = 10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // 序号徽章：与启用池同一个 OrderBadge（10-05 形「丙」胶囊；本页浅绿配色随全局）
            OrderBadge(number = index + 1)
            Spacer(Modifier.width(10.dp))
            Text(
                acc.nickname,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            // 状态胶囊（有效/已过期）：沿用原两色语义
            if (acc.isExpired())
                StatusChip("已过期", MaterialTheme.colorScheme.errorContainer)
            else
                StatusChip("有效", MaterialTheme.colorScheme.primaryContainer)
            Spacer(Modifier.width(4.dp))
            // 图标动作区（36dp 热区 + 18dp 图标，与启用池 FlatIconAction 同规格）：
            // 签到（绿，主操作）/ 续期 / 查积分；操作中该键原位转小圈
            if (busy) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            } else {
                // 签到：事件可用图标（带 ✓ 语义）；主操作用主色
                FlatIconAction(
                    Icons.Default.EventAvailable,
                    "签到",
                    tint = MaterialTheme.colorScheme.primary
                ) { onCheckIn() }
                FlatIconAction(Icons.Default.Refresh, "续期") { onRefresh() }
                FlatIconAction(Icons.Default.Savings, "查积分") { onQueryCredits() }
            }
        }
        // 副行：过期/积分/签到时间（缩进对齐名字列 = 徽章 20 + 间距 10 = 30dp，同启用池）
        Spacer(Modifier.height(2.dp))
        Text(
            buildString {
                append("过期：")
                append(if (acc.expiresAt > 0) timeFmt.format(Date(acc.expiresAt)) else "未知")
                if (acc.credits != 0L) append(" · 积分 ${acc.credits}")
                if (acc.lastCheckinAt > 0) append(" · 签到 ${timeFmt.format(Date(acc.lastCheckinAt))}")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 30.dp)
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

/**
 * 图标动作键（10-07 装机反馈：账号池行 UI 对齐启用池）——与密钥页 FlatIconAction 同规格：
 * 36dp 圆形热区 + 18dp 图标（本地复刻，跨包 internal 不通）。原 FlatTextAction 文字键
 * 随本改造退役（动作全归图标，行间分隔线与两行式排版见 AccountRow）。
 */
@Composable
private fun FlatIconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val effectiveTint = if (enabled) tint else tint.copy(alpha = 0.3f)
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = effectiveTint, modifier = Modifier.size(18.dp))
    }
}
