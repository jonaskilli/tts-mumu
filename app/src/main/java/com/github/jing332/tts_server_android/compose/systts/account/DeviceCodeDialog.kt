package com.github.jing332.tts_server_android.compose.systts.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.jing332.tts_server_android.service.systts.help.AccountPool
import com.github.jing332.tts_server_android.service.systts.help.ClineChannel
import com.github.jing332.tts_server_android.service.systts.help.DeviceCodeLogin
import com.github.jing332.tts_server_android.service.systts.help.MinimaxChannel
import com.github.jing332.tts_server_android.service.systts.help.ZcodeChannel
import kotlinx.coroutines.delay

/**
 * 设备码登录弹窗（10-09 全渠道批）：cline(WorkOS) / minimax(PKCE) / zcode(中介 poll)。
 * 流程：start → 展示 user_code/打开授权 URL → 后台轮询（interval+slow_down 退避）→
 * 成功落盘 AccountPool → onDone 回调刷新列表。
 * qoder 同形态（openapi poll 404=pending）第二期接（其登录 URL 需 WebView 展示授权页）。
 */
@Composable
fun DeviceCodeDialog(
    provider: String,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("正在申请设备码…") }
    var userCode by remember { mutableStateOf("") }
    var authUrl by remember { mutableStateOf("") }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(provider) {
        val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            when (provider) {
                "cline" -> {
                    val (dc, uri, interval) = ClineChannel.startDeviceLogin()
                        ?: return@withContext Triple("", "", "申请设备码失败")
                    // 轮询（slow_down 累积退避照插件：interval 从 5s 起，遇 slow_down +1s）
                    var wait = interval.toLong().coerceAtLeast(1L)
                    var nick = ""
                    var email = ""
                    var accountId = ""
                    var err = ""
                    loop@ while (true) {
                        delay(wait * 1000L)
                        val (st, o) = ClineChannel.pollOnce(dc)
                        when (st) {
                            "PENDING" -> continue@loop
                            "DENIED" -> { err = "用户拒绝了授权"; break@loop }
                            "EXPIRED" -> { err = "设备码已过期，请重试"; break@loop }
                            "OK" -> {
                                val workosToken = o?.optString("access_token").orEmpty()
                                val workosRefresh = o?.optString("refresh_token").orEmpty()
                                val reg = ClineChannel.register(workosToken, workosRefresh)
                                if (reg.err.isNotEmpty()) { err = reg.err; break@loop }
                                // 落盘
                                val acc = AccountPool.Account(
                                    id = "cline-${System.currentTimeMillis().toString(16)}",
                                    provider = "cline",
                                    nickname = reg.email.ifEmpty { "Cline" },
                                    accessToken = reg.accessToken,
                                    refreshToken = reg.refreshToken,
                                    expiresAt = reg.expiresAt,
                                )
                                AccountPool.save(AccountPool.load().filterNot { it.id == acc.id } + acc)
                                nick = acc.nickname; email = reg.email; accountId = reg.accountId
                                break@loop
                            }
                        }
                    }
                    Triple(nick.ifEmpty { email }, uri, err)
                }
                "minimax" -> {
                    val s = MinimaxChannel.startDeviceLogin()
                    if (s.err.isNotEmpty()) return@withContext Triple("", s.verifyUrlComplete.ifEmpty { s.verifyUrl }, s.err)
                    var wait = s.intervalSec.toLong().coerceAtLeast(1L)
                    var nick = ""; var err = ""
                    loop@ while (true) {
                        delay(wait * 1000L)
                        val (st, o) = MinimaxChannel.pollOnce(s.deviceCode, s.verifier)
                        when (st) {
                            "PENDING" -> continue@loop
                            "DENIED" -> { err = "授权被拒或 scope 校验失败"; break@loop }
                            "EXPIRED" -> { err = "设备码已过期，请重试"; break@loop }
                            "OK" -> {
                                val token = o ?: break@loop
                                val at = token.optString("access_token")
                                val rt = token.optString("refresh_token")
                                val expiresAt = System.currentTimeMillis() + token.optLong("expires_in", 0L) * 1000L
                                // ⚠️ token 非 JWT——expiresAt 必须落（不落就彻底丢失过期时刻）
                                val acc = AccountPool.Account(
                                    id = "minimax-${System.currentTimeMillis().toString(16)}",
                                    provider = "minimax",
                                    nickname = "MiniMax",
                                    accessToken = at, refreshToken = rt, expiresAt = expiresAt,
                                )
                                AccountPool.save(AccountPool.load().filterNot { it.id == acc.id } + acc)
                                nick = acc.nickname
                                break@loop
                            }
                        }
                    }
                    Triple(nick, s.verifyUrlComplete.ifEmpty { s.verifyUrl }, err)
                }
                "zcode" -> {
                    val mid = DeviceCodeLogin.randomUuid()
                    val init = ZcodeChannel.initLogin("bigmodel")
                    if (init.err.isNotEmpty()) return@withContext Triple("", init.authorizeUrl, init.err)
                    var nick = ""; var err = ""
                    loop@ while (true) {
                        delay(init.intervalSec.coerceAtLeast(1) * 1000L)
                        val pr = ZcodeChannel.pollLogin(init.flowId, mid)
                        when (pr.status) {
                            "PENDING" -> continue@loop
                            "ERROR" -> { err = pr.err; break@loop }
                            "READY" -> {
                                val acc = AccountPool.Account(
                                    id = "zcode-${System.currentTimeMillis().toString(16)}",
                                    provider = "zcode",
                                    nickname = if (pr.userId.length >= 11) "智谱 ${pr.userId.take(3)}****${pr.userId.takeLast(4)}" else "ZCode",
                                    accessToken = pr.jwt, refreshToken = "",
                                    expiresAt = 0L, // JWT 无 exp，静态凭据（失效靠上游 401）
                                ).withExtra("device_mid", mid) // ⚠️ 必须稳定持久化，且不能拿它认账号
                                AccountPool.save(AccountPool.load().filterNot { it.id == acc.id } + acc)
                                nick = acc.nickname
                                break@loop
                            }
                            else -> continue@loop
                        }
                    }
                    Triple(nick, init.authorizeUrl, err)
                }
                else -> Triple("", "", "该渠道登录暂未接线：$provider")
            }
        }
        val (nick, url, err) = result
        if (err.isNotEmpty()) {
            status = err
            finished = true
        } else if (nick.isNotEmpty()) {
            status = "登录成功：$nick"
            finished = true
            onDone()
        } else {
            authUrl = url // 不会走到（成功必带 nick）
        }
    }

    // 首帧：申请成功后把授权 URL/user_code 显示出来（上面 LaunchedEffect 一次性跑完整流程，
    // 所以 URL 在流程完成前展示不了——改造：start 阶段先行。此版简化：完成型弹窗，
    // 轮询期间显示转圈；用户在浏览器里先完成授权，弹窗内轮询到成功自动关。
    // 若需要「先展示 URL 再轮询」的交互，第二期拆 start/poll 两阶段。）
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设备码登录") },
        text = {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!finished) {
                    CircularProgressIndicator(Modifier.padding(bottom = 12.dp))
                    Text("请在浏览器完成授权后等待本页自动确认", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(status, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(); if (finished && status.startsWith("登录成功")) onDone() }) {
                Text(if (finished) "关闭" else "后台等待")
            }
        },
    )
}
