package com.github.jing332.tts_server_android.compose.systts.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.github.jing332.common.utils.toast
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.nav.NavTopAppBar
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
            items(accounts, key = { it.id }) { acc ->
                AccountRow(
                    acc = acc,
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
            }
        }
    }
}

@Composable
private fun AccountRow(
    acc: AccountPool.Account,
    busy: Boolean,
    timeFmt: SimpleDateFormat,
    onRefresh: () -> Unit,
    onCheckIn: () -> Unit,
    onQueryCredits: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = !busy) { onQueryCredits() }
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    acc.nickname,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (acc.isExpired())
                    StatusChip("已过期", MaterialTheme.colorScheme.errorContainer)
                else
                    StatusChip("有效", MaterialTheme.colorScheme.primaryContainer)
            }
            Spacer(Modifier.size(4.dp))
            Text(
                buildString {
                    append("过期：")
                    append(if (acc.expiresAt > 0) timeFmt.format(Date(acc.expiresAt)) else "未知")
                    if (acc.credits != 0L) append(" · 积分 ${acc.credits}")
                    if (acc.lastCheckinAt > 0) append(" · 签到 ${timeFmt.format(Date(acc.lastCheckinAt))}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlatTextAction("签到", MaterialTheme.colorScheme.primary, enabled = !busy, onClick = onCheckIn)
                FlatTextAction("续期", MaterialTheme.colorScheme.onSurfaceVariant, enabled = !busy, onClick = onRefresh)
                FlatTextAction("查积分", MaterialTheme.colorScheme.onSurfaceVariant, enabled = !busy, onClick = onQueryCredits)
            }
        }
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

// 与密钥页 FlatTextAction 同款次要文字键（本地复刻避免跨包可见性纠缠）
@Composable
private fun FlatTextAction(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = if (enabled) color else color.copy(alpha = 0.4f),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
