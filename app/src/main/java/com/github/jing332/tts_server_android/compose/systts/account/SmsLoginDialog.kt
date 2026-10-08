package com.github.jing332.tts_server_android.compose.systts.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.github.jing332.common.utils.toast
import com.github.jing332.tts_server_android.service.systts.help.AccountPool
import com.github.jing332.tts_server_android.service.systts.help.AutoclawChannel
import com.github.jing332.tts_server_android.service.systts.help.LoomyChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 短信登录弹窗（loomy=讯飞办公助手 10-09 全渠道批；autoclaw=智谱 10-08 增）。
 *
 * 按 provider 分流：
 *  - loomy：发送 → data.msgid → checkCode 拿 (session, userid)。session 14 天（登录参数
 *    expire=1209600 声明），无 refresh，expiresAt 本地推算；昵称「讯飞 + userid 尾 4 位」。
 *  - autoclaw：device_id 先随机生成并贯穿全程，send-code（data.result===true 才算发出）→
 *    agent-login（code 转 Number）拿 access/refresh_token。access_token 是 JWT，
 *    expiresAt 按 exp 解、解不出 +7 天兜底；device_id 存 Account.extra（续期/请求必须带），
 *    昵称「AutoClaw + 手机尾 4 位」。
 * 网络全程 Dispatchers.IO；签名/鉴权头各渠道引擎内部完成（LoomyChannel.authedHeaders /
 * AutoclawChannel.autoclawHeaders）。
 */
@Composable
fun SmsLoginDialog(
    provider: String = "loomy", // 默认 loomy：既有调用零改动（10-08 前只有 loomy 一家）
    onDismiss: () -> Unit,
    onDone: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var phone by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    // 发送验证码倒计时（秒）；>0 期间按钮禁用
    var countdown by remember { mutableIntStateOf(0) }
    // loomy：发码响应的 msgid（提交必带）；autoclaw：无 msgid 概念，发出即置非空解锁验证码框
    var msgId by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var errText by remember { mutableStateOf("") }
    var done by remember { mutableStateOf(false) }
    // autoclaw：device_id 进弹窗即定（登录/落盘/续期全程同一个）
    val deviceId = remember(provider) {
        if (provider == "autoclaw") java.util.UUID.randomUUID().toString() else ""
    }

    // 60s 倒计时（发送成功后启动）
    LaunchedEffect(countdown) {
        while (countdown > 0) {
            delay(1000L)
            countdown--
        }
    }

    val phoneValid = Regex("^1[3-9]\\d{9}$").matches(phone.trim())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (provider == "autoclaw") "AutoClaw 短信登录" else "Loomy 短信登录") },
        text = {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                if (errText.isNotEmpty()) {
                    Text(
                        errText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { c -> c.isDigit() }.take(11) },
                    label = { Text("手机号") },
                    isError = phone.isNotEmpty() && !phoneValid,
                    supportingText = {
                        if (phone.isNotEmpty() && !phoneValid)
                            Text("11 位手机号，1[3-9] 开头")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = smsCode,
                    onValueChange = { smsCode = it.filter { c -> c.isDigit() }.take(if (provider == "autoclaw") 8 else 6) },
                    label = { Text("验证码") },
                    enabled = msgId.isNotEmpty(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        countdown > 0 -> "验证码已发送，${countdown}s 后可重发"
                        msgId.isNotEmpty() -> "验证码已发送，未收到可重发"
                        else -> "发送验证码后输入短信里的验证码"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            if (submitting) {
                CircularProgressIndicator(Modifier.padding(horizontal = 12.dp))
            } else {
                TextButton(
                    enabled = phoneValid && msgId.isNotEmpty() && smsCode.length >= 4,
                    onClick = {
                        errText = ""
                        submitting = true
                        val p = phone.trim()
                        val mid = msgId
                        val code = smsCode.trim()
                        scope.launch {
                            if (provider == "autoclaw") {
                                val r = withContext(Dispatchers.IO) {
                                    AutoclawChannel.login(p, code, deviceId)
                                }
                                submitting = false
                                if (r.err.isNotEmpty() || r.accessToken.isEmpty()) {
                                    errText = r.err.ifEmpty { "提交失败（access_token 为空）" }
                                } else {
                                    val now = System.currentTimeMillis()
                                    val acc = AccountPool.Account(
                                        id = "autoclaw-${now.toString(16)}",
                                        provider = "autoclaw",
                                        nickname = r.nickname,
                                        accessToken = r.accessToken,
                                        refreshToken = r.refreshToken,
                                        // access_token 是 JWT：expiresAt 按 exp 解，解不出 +7 天兜底
                                        expiresAt = AutoclawChannel.jwtExpiryMs(r.accessToken) ?: (now + 7L * 24 * 3600_000L),
                                    ).withExtra("device_id", deviceId) // device_id 必须随凭据持久化
                                    AccountPool.save(AccountPool.load().filterNot { it.id == acc.id } + acc)
                                    done = true
                                    onDone(acc.nickname)
                                }
                            } else {
                                val (session, userid, err) = withContext(Dispatchers.IO) {
                                    LoomyChannel.checkCode(p, code, mid)
                                }
                                submitting = false
                                if (err.isNotEmpty() || session.isEmpty()) {
                                    errText = err.ifEmpty { "提交失败（session 为空）" }
                                } else {
                                    val now = System.currentTimeMillis()
                                    val acc = AccountPool.Account(
                                        id = "loomy-${now.toString(16)}",
                                        provider = "loomy",
                                        nickname = "讯飞 " + userid.takeLast(4),
                                        accessToken = session,
                                        refreshToken = "",
                                        // session 14 天（登录声明 expire=1209600s），响应不带到期，本地推算
                                        expiresAt = now + 14L * 24 * 3600_000L,
                                    )
                                    AccountPool.save(AccountPool.load().filterNot { it.id == acc.id } + acc)
                                    done = true
                                    onDone(acc.nickname)
                                }
                            }
                        }
                    },
                ) { Text("提交") }
            }
        },
        dismissButton = {
            // 取消与发送验证码同占 dismiss 槽（Row 平铺），与确认键一行右对齐；
            // 此前取消独占 Column 第二行被 M3 按钮流居中换行（10-10 用户实机截图）
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text(if (done) "关闭" else "取消") }
                // 发送验证码（60s 倒计时防连发）
                TextButton(
                    enabled = phoneValid && !sending && countdown <= 0,
                    onClick = {
                        errText = ""
                        sending = true
                        val p = phone.trim()
                        scope.launch {
                            if (provider == "autoclaw") {
                                val r = withContext(Dispatchers.IO) { AutoclawChannel.sendSmsCode(p, deviceId) }
                                sending = false
                                if (!r.ok) {
                                    errText = "发送失败：" + r.err.ifEmpty { "上游未确认发送" }
                                } else {
                                    msgId = deviceId // 无 msgid 概念，置非空解锁验证码框
                                    countdown = 60
                                    context.toast("验证码已发送")
                                }
                            } else {
                                val r = withContext(Dispatchers.IO) { LoomyChannel.sendSmsCode(p) }
                                sending = false
                                if (r.err.isNotEmpty() || r.msgId.isEmpty()) {
                                    errText = "发送失败：" + r.err.ifEmpty { "响应缺 msgid" }
                                } else {
                                    msgId = r.msgId
                                    countdown = 60
                                    context.toast("验证码已发送")
                                }
                            }
                        }
                    },
                ) {
                    if (sending) CircularProgressIndicator(Modifier.padding(end = 6.dp))
                    Text(if (countdown > 0) "重发(${countdown}s)" else "发送验证码")
                }
            }
        },
    )
}
