package com.github.jing332.tts_server_android.compose.systts.account

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.github.jing332.tts_server_android.service.systts.help.AccountPool
import com.github.jing332.tts_server_android.service.systts.help.ClineChannel
import com.github.jing332.tts_server_android.service.systts.help.DeviceCodeLogin
import com.github.jing332.tts_server_android.service.systts.help.MinimaxChannel
import com.github.jing332.tts_server_android.service.systts.help.QoderChannel
import com.github.jing332.tts_server_android.service.systts.help.ZcodeChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 设备码登录弹窗（10-09 全渠道批；10-10 改造为「两阶段」交互）。
 *
 * 两阶段设计（旧版是申请+轮询一体的「盲等」型，用户看不到 user_code/授权地址）：
 *   阶段一（申请）：调 start/init，成功后展示——
 *     user_code（大号等宽 + 复制按钮）、verification_uri_complete / authorize_url
 *     完整地址 + 「打开授权页」（ACTION_VIEW 拉浏览器）、剩余有效期与轮询间隔。
 *   阶段二（轮询）：LaunchedEffect 按 interval 循环 pollOnce/pollLogin，
 *     每轮更新「等待授权中…第 N 次查询」；cline 遇 slow_down 间隔秒数 +1 累积（§6.1）；
 *     成功 → 落盘 AccountPool（cline 走 register、minimax 自算 expiresAt、
 *     zcode device_mid 存 extra）→ 显示成功 → onDone。
 *   超时：响应带 expires_in 用之，否则总上限 5 分钟 → 「已超时，请重试」。
 *
 * 协议出处（E:\TTS\临时文件\渠道协议规格书-20261009.md）：
 *   cline   = §6.1（WorkOS 设备码 RFC 轮询；slow_down 必须真退避，固定间隔会被持续限流；
 *             access_token 的 workos: 前缀不可剥）
 *   minimax = §9.1/§9.2（设备码+PKCE；pending 是 HTTP 200 + status=pending；
 *             token 非 JWT → expiresAt 必须自算落盘）
 *   zcode   = §10.1/§10.2（服务端中介 poll 流；无 user_code 只有 authorize_url；
 *             device_mid 必须稳定持久化且不能拿它认账号）
 */
// 10-10 M3 Expressive：LoadingIndicator 仍在实验 API，需显式 OptIn
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeviceCodeDialog(
    provider: String,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current

    // ---------- 阶段一状态 ----------
    var startReady by remember { mutableStateOf(false) }
    var startErr by remember { mutableStateOf("") }
    var userCode by remember { mutableStateOf("") }
    var authUrl by remember { mutableStateOf("") }
    var intervalSec by remember { mutableIntStateOf(5) }
    // expires_in 换算的到期时刻（epoch ms；0=响应没给，用 5 分钟兜底）
    var deadlineMs by remember { mutableLongStateOf(0L) }
    // 各渠道 start 产物（阶段二轮询要用）
    var clineStart by remember { mutableStateOf<ClineChannel.DeviceStart?>(null) }
    var minimaxStart by remember { mutableStateOf<MinimaxChannel.DeviceStart?>(null) }
    var zcodeStart by remember { mutableStateOf<ZcodeChannel.LoginInit?>(null) }
    var zcodeDeviceMid by remember { mutableStateOf("") }
    var qoderStart by remember { mutableStateOf<QoderChannel.QoderStart?>(null) }

    // ---------- 阶段二状态 ----------
    var pollCount by remember { mutableIntStateOf(0) }
    var pollErr by remember { mutableStateOf("") }
    var doneNick by remember { mutableStateOf("") }
    // success / timeout 双终态都置 finished
    var finished by remember { mutableStateOf(false) }

    // ==================== 阶段一：申请 ====================
    LaunchedEffect(provider) {
        val r = withContext(Dispatchers.IO) {
            when (provider) {
                "cline" -> ClineChannel.startDeviceLogin()
                "minimax" -> MinimaxChannel.startDeviceLogin()
                "zcode" -> ZcodeChannel.initLogin("bigmodel") // 规格书 §10.1
                "qoder" -> QoderChannel.startLogin()
                else -> null
            }
        }
        when (r) {
            is ClineChannel.DeviceStart -> {
                clineStart = r
                if (r.err.isNotEmpty()) startErr = r.err
                else {
                    userCode = r.userCode
                    authUrl = r.verifyUrlComplete.ifEmpty { r.verifyUrl }
                    intervalSec = r.intervalSec.coerceAtLeast(1)
                    deadlineMs =
                        if (r.expiresInSec > 0) System.currentTimeMillis() + r.expiresInSec * 1000L else 0L
                    startReady = true
                }
            }
            is MinimaxChannel.DeviceStart -> {
                minimaxStart = r
                if (r.err.isNotEmpty()) startErr = r.err
                else {
                    userCode = r.userCode
                    authUrl = r.verifyUrlComplete.ifEmpty { r.verifyUrl }
                    intervalSec = r.intervalSec.coerceAtLeast(1)
                    // §9.1：interval 单位秒、缺省 5；expires_in 同样自算到期时刻
                    deadlineMs =
                        if (r.expiresInSec > 0) System.currentTimeMillis() + r.expiresInSec * 1000L else 0L
                    startReady = true
                }
            }
            is ZcodeChannel.LoginInit -> {
                zcodeStart = r
                if (r.err.isNotEmpty()) startErr = r.err
                else {
                    // zcode 无 user_code（§10.1），只展示 authorize_url
                    authUrl = r.authorizeUrl
                    intervalSec = r.intervalSec.coerceAtLeast(1)
                    zcodeDeviceMid = DeviceCodeLogin.randomUuid()
                    startReady = true
                }
            }
            is QoderChannel.QoderStart -> {
                qoderStart = r
                if (r.err.isNotEmpty()) startErr = r.err
                else {
                    // qoder 无 user_code，只展示 authorize_url（PKCE+nonce 在 URL 里）
                    authUrl = r.authorizeUrl
                    intervalSec = 2
                    startReady = true
                }
            }
            else -> startErr = "该渠道登录暂未接线：$provider"
        }
    }

    // ==================== 阶段二：轮询 ====================
    if (startReady) {
        LaunchedEffect(provider) {
            val deadline =
                if (deadlineMs > 0L) deadlineMs else System.currentTimeMillis() + 5 * 60_000L
            withContext(Dispatchers.IO) {
                when (provider) {
                    "cline" -> {
                        val s = clineStart ?: return@withContext
                        var wait = intervalSec.toLong()
                        loop@ while (pollErr.isEmpty() && doneNick.isEmpty() && System.currentTimeMillis() < deadline) {
                            delay(wait * 1000L)
                            pollCount += 1
                            val (st, o) = ClineChannel.pollOnceEx(s.deviceCode)
                            when (st) {
                                "PENDING" -> Unit
                                // §6.1：slow_down 间隔秒数 +1 累积（必须真退避，固定间隔会被持续限流）
                                "SLOW_DOWN" -> wait += 1
                                "DENIED" -> pollErr = "用户拒绝了授权"
                                "EXPIRED" -> pollErr = "设备码已过期，请重试"
                                "OK" -> {
                                    val workosToken = o?.optString("access_token").orEmpty()
                                    val workosRefresh = o?.optString("refresh_token").orEmpty()
                                    val reg = ClineChannel.register(workosToken, workosRefresh)
                                    if (reg.err.isNotEmpty()) {
                                        pollErr = reg.err
                                    } else {
                                        // 10-10 登录去重：identity = register 返回的 accountId（clineUserId）
                                        val acc = AccountPool.upsert("cline", reg.accountId.ifEmpty { null }) { existing ->
                                            AccountPool.Account(
                                                id = existing?.id ?: "cline-${System.currentTimeMillis().toString(16)}",
                                                provider = "cline",
                                                nickname = reg.email.ifEmpty { "Cline" },
                                                accessToken = reg.accessToken,
                                                refreshToken = reg.refreshToken,
                                                expiresAt = reg.expiresAt,
                                            ).withExtra("_uid", reg.accountId)
                                        }
                                        doneNick = if (acc.isUpdate) "${acc.nickname}(已更新)" else acc.nickname
                                    }
                                }
                                else -> Unit
                            }
                        }
                    }
                    "minimax" -> {
                        val s = minimaxStart ?: return@withContext
                        loop@ while (pollErr.isEmpty() && doneNick.isEmpty() && System.currentTimeMillis() < deadline) {
                            delay(intervalSec * 1000L)
                            pollCount += 1
                            val (st, o) = MinimaxChannel.pollOnce(s.deviceCode, s.verifier)
                            when (st) {
                                "PENDING" -> Unit
                                "DENIED" -> pollErr = "授权被拒或 scope 校验失败"
                                "EXPIRED" -> pollErr = "设备码已过期，请重试"
                                "OK" -> {
                                    val token = o ?: break@loop
                                    val at = token.optString("access_token")
                                    val rt = token.optString("refresh_token")
                                    // ⚠️ §9.2：token 非 JWT——expiresAt 必须自算落盘
                                    val expiresAt = System.currentTimeMillis() + token.optLong("expires_in", 0L) * 1000L
                                    // 10-10 登录去重：identity = token 响应的 account_id（有则用，无则不去重）
                                    val accountId = token.optString("account_id")
                                    val acc = AccountPool.upsert("minimax", accountId.ifEmpty { null }) { existing ->
                                        AccountPool.Account(
                                            id = existing?.id ?: "minimax-${System.currentTimeMillis().toString(16)}",
                                            provider = "minimax",
                                            nickname = existing?.nickname ?: "MiniMax",
                                            accessToken = at, refreshToken = rt, expiresAt = expiresAt,
                                        ).withExtra("_uid", accountId)
                                    }
                                    doneNick = if (acc.isUpdate) "${acc.nickname}(已更新)" else acc.nickname
                                }
                                else -> Unit
                            }
                        }
                    }
                    "zcode" -> {
                        val init = zcodeStart ?: return@withContext
                        loop@ while (pollErr.isEmpty() && doneNick.isEmpty() && System.currentTimeMillis() < deadline) {
                            delay(intervalSec * 1000L)
                            pollCount += 1
                            val pr = ZcodeChannel.pollLogin(init.flowId, zcodeDeviceMid)
                            when (pr.status) {
                                "PENDING" -> Unit
                                "ERROR" -> pollErr = pr.err
                                "READY" -> {
                                    // 10-10 登录去重：identity = pollLogin 的 userId（已有）。
                                    // device_mid §10.2 要求稳定持久化：更新已有账号时沿用旧值，不换新
                                    val acc = AccountPool.upsert("zcode", pr.userId.ifEmpty { null }) { existing ->
                                        AccountPool.Account(
                                            id = existing?.id ?: "zcode-${System.currentTimeMillis().toString(16)}",
                                            provider = "zcode",
                                            nickname = if (pr.userId.length >= 11) "智谱 ${pr.userId.take(3)}****${pr.userId.takeLast(4)}" else "ZCode",
                                            accessToken = pr.jwt, refreshToken = "",
                                            expiresAt = 0L, // JWT 无 exp，静态凭据（失效靠上游 401）
                                        )
                                            // ⚠️ §10.2 必须稳定持久化，且不能拿它认账号
                                            .withExtra("device_mid", existing?.extraStr("device_mid")?.ifEmpty { null } ?: zcodeDeviceMid)
                                            .withExtra("_uid", pr.userId)
                                    }
                                    doneNick = if (acc.isUpdate) "${acc.nickname}(已更新)" else acc.nickname
                                }
                                else -> Unit
                            }
                        }
                    }
                    "qoder" -> {
                        val s = qoderStart ?: return@withContext
                        loop@ while (pollErr.isEmpty() && doneNick.isEmpty() && System.currentTimeMillis() < deadline) {
                            delay(intervalSec * 1000L)
                            pollCount += 1
                            val (st, d) = QoderChannel.pollOnce(s.nonce, s.verifier)
                            when (st) {
                                "PENDING" -> Unit // 404=未授权继续（连续失败计数由 pollOnce 语义吸收）
                                "OK" -> {
                                    val token = d ?: break@loop
                                    // token/user_id/user_name 必读（uid 是 WASM 加密链身份来源，缺它对话挂）
                                    val at = listOf("token", "device_token", "access_token")
                                        .firstNotNullOfOrNull { token.optString(it).takeIf { x -> x.isNotEmpty() } }
                                        ?: break@loop
                                    val uid = token.optString("user_id")
                                    val nickname = token.optString("user_name").ifEmpty { "Qoder" }
                                    val rt = token.optString("refresh_token")
                                    val acc = AccountPool.upsert("qoder", uid.ifEmpty { null }) { existing ->
                                        AccountPool.Account(
                                            id = existing?.id ?: "qoder-${System.currentTimeMillis().toString(16)}",
                                            provider = "qoder",
                                            nickname = existing?.nickname ?: nickname,
                                            accessToken = at, refreshToken = rt,
                                            expiresAt = 0L, // expire 三形态由 refresh 自算，登录期不猜
                                        )
                                            // machine_id：登录时生成稳定持久化（WASM 上下文绑定，换号纪律）
                                            .withExtra("machine_id", existing?.extraStr("machine_id")?.ifEmpty { null } ?: s.machineId)
                                            .withExtra("_uid", uid)
                                    }
                                    doneNick = if (acc.isUpdate) "${acc.nickname}(已更新)" else acc.nickname
                                }
                                else -> Unit
                            }
                        }
                    }
                }
                if (pollErr.isEmpty() && doneNick.isEmpty()) pollErr = "已超时，请重试"
            }
            finished = true
            if (doneNick.isNotEmpty()) onDone()
        }
    }

    // ==================== UI ====================
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设备码登录") },
        text = {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when {
                    startErr.isNotEmpty() -> Text(startErr, color = MaterialTheme.colorScheme.error)

                    finished && doneNick.isNotEmpty() -> Text(
                        "登录成功：$doneNick",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    finished && pollErr.isNotEmpty() -> Text(
                        pollErr,
                        color = MaterialTheme.colorScheme.error,
                    )

                    !startReady -> {
                        // 10-10 M3 Expressive 改造：申请设备码等待换 LoadingIndicator
                        LoadingIndicator(Modifier.padding(bottom = 12.dp))
                        Text("正在申请设备码…", style = MaterialTheme.typography.bodySmall)
                    }

                    else -> {
                        // ---------- 阶段一：user_code / 授权地址 ----------
                        if (userCode.isNotEmpty()) {
                            Text("授权码", style = MaterialTheme.typography.bodySmall)
                            Text(
                                userCode,
                                style = MaterialTheme.typography.headlineMedium
                                    .copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.padding(vertical = 6.dp),
                            )
                            TextButton(onClick = {
                                runCatching {
                                    val cb =
                                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cb.setPrimaryClip(ClipData.newPlainText("user_code", userCode))
                                    Toast.makeText(context, "已复制授权码", Toast.LENGTH_SHORT).show()
                                }
                            }) { Text("复制授权码") }
                        }
                        if (authUrl.isNotEmpty()) {
                            if (userCode.isEmpty()) {
                                Text("请在浏览器打开以下地址完成授权：", style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text("或手动打开授权地址：", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                authUrl,
                                style = MaterialTheme.typography.bodySmall
                                    .copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                            )
                            OutlinedButton(onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)))
                                }.onFailure {
                                    Toast.makeText(
                                        context, "打开浏览器失败：${it.message}", Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }, modifier = Modifier.padding(top = 4.dp)) { Text("打开授权页") }
                        }
                        // 剩余有效期与轮询间隔
                        val remainSec = ((deadlineMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
                        val remainText =
                            if (deadlineMs > 0L) "有效期剩 ${remainSec / 60} 分 ${remainSec % 60} 秒"
                            else "有效期 5 分钟"
                        Text(
                            "$remainText，每 $intervalSec 秒自动查询授权结果",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        // ---------- 阶段二：轮询进度 ----------
                        if (!finished) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp),
                            ) {
                                // 10-10 M3 Expressive 改造：轮询等待换 LoadingIndicator
                                LoadingIndicator(Modifier.padding(end = 8.dp).size(20.dp))
                                Text("等待授权中…第 $pollCount 次查询", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (finished || startErr.isNotEmpty()) "关闭" else "取消")
            }
        },
    )
}
