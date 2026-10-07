package com.github.jing332.tts_server_android.compose.systts.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.jing332.tts_server_android.service.systts.help.AccountPool
import com.github.jing332.tts_server_android.service.systts.help.DeviceCodeLogin

/**
 * 凭据直填弹窗（10-09 全渠道批）：登录为「凭据形态简单」的渠道——
 * opencode（API key 或匿名单词 public）/ loomy（32hex session）/ raccoon / trae /
 * lobsterai / codearts（AK/SK/ST）/ gemini（refresh token）。第二期再接各家完整登录流。
 * 字段按渠道显示（1~3 个输入框），落盘 = Account + extra 渠道私有字段。
 */
@Composable
fun CredentialDialog(
    provider: String,
    onDismiss: () -> Unit,
    onDone: (String) -> Unit,
) {
    var f1 by remember { mutableStateOf("") }
    var f2 by remember { mutableStateOf("") }
    var f3 by remember { mutableStateOf("") }

    val (label1, label2, label3, hint) = when (provider) {
        "opencode" -> Quad("API Key（匿名通道填 public）", "", "", "sk-… 或 public；免费模型仅匿名可用")
        "loomy" -> Quad("session（32 位 hex）", "userid（18 位数字）", "", "短信登录后取自 checkCode 响应")
        "raccoon" -> Quad("access_token", "refresh_token", "", "JWT，约 3 小时寿命自动续")
        "trae" -> Quad("refresh_token", "", "", "ExchangeToken 自动换 access")
        "lobsterai" -> Quad("refresh_token", "uuid", "", "keyfrom 可留空（将自动生成）")
        "codearts" -> Quad("access_key_id", "secret_access_key", "security_token", "华为云 STS 临时凭据")
        "gemini" -> Quad("refresh_token", "", "", "Google OAuth refresh token（串行续期）")
        else -> Quad("凭据", "", "", "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加凭据") },
        text = {
            Column {
                if (hint.isNotEmpty()) {
                    Text(hint, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp))
                }
                OutlinedTextField(value = f1, onValueChange = { f1 = it }, label = { Text(label1) }, modifier = Modifier.fillMaxWidth())
                if (label2.isNotEmpty())
                    OutlinedTextField(value = f2, onValueChange = { f2 = it }, label = { Text(label2) }, modifier = Modifier.fillMaxWidth())
                if (label3.isNotEmpty())
                    OutlinedTextField(value = f3, onValueChange = { f3 = it }, label = { Text(label3) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = f1.isNotBlank(),
                onClick = {
                    val now = System.currentTimeMillis()
                    var acc = when (provider) {
                        "opencode" -> AccountPool.Account(
                            id = "opencode-${now.toString(16)}", provider = "opencode",
                            nickname = if (f1.trim() == "public") "匿名通道" else "OpenCode",
                            accessToken = f1.trim(), refreshToken = "",
                        )
                        "loomy" -> AccountPool.Account(
                            id = "loomy-${now.toString(16)}", provider = "loomy",
                            nickname = if (f2.isNotBlank()) "讯飞 ${f2.trim().takeLast(4)}" else "Loomy",
                            accessToken = f1.trim(), refreshToken = "",
                            // session 14 天（登录声明期）；本地推算
                            expiresAt = now + 14L * 24 * 3600_000L,
                        )
                        "raccoon" -> AccountPool.Account(
                            id = "raccoon-${now.toString(16)}", provider = "raccoon",
                            nickname = "小浣熊", accessToken = f1.trim(), refreshToken = f2.trim(),
                        )
                        "trae" -> AccountPool.Account(
                            id = "trae-${now.toString(16)}", provider = "trae",
                            nickname = "TRAE", accessToken = "", refreshToken = f1.trim(),
                        )
                        "lobsterai" -> AccountPool.Account(
                            id = "lobsterai-${now.toString(16)}", provider = "lobsterai",
                            nickname = "LobsterAI", accessToken = "", refreshToken = f1.trim(),
                        ).withExtra("uuid", DeviceCodeLogin.randomUuid())
                            .withExtra("firstKeyfrom", now.toString())
                        "codearts" -> AccountPool.Account(
                            id = "codearts-${now.toString(16)}", provider = "codearts",
                            nickname = "CodeArts", accessToken = f1.trim(), refreshToken = "",
                        ).withExtra("access_key_id", f1.trim())
                            .withExtra("secret_access_key", f2.trim())
                            .withExtra("security_token", f3.trim())
                        "gemini" -> AccountPool.Account(
                            id = "gemini-${now.toString(16)}", provider = "gemini",
                            nickname = "Gemini", accessToken = "", refreshToken = f1.trim(),
                        )
                        else -> null
                    }
                    if (acc != null) {
                        // trae/lobsterai/gemini 先续期换 access（refresh 为唯一凭据）
                        if (provider in setOf("trae", "lobsterai", "gemini")) {
                            val ch = com.github.jing332.tts_server_android.service.systts.help.ChatChannels.byProvider(provider)
                            val r = ch?.refresh(acc)
                            if (r != null) {
                                acc = acc.copy(accessToken = r.first, refreshToken = r.second, expiresAt = r.third)
                                if (provider == "trae") {
                                    acc = acc.copy(
                                        extra = acc.extra.let {
                                            val o = org.json.JSONObject(it); o.put("machine_id", DeviceCodeLogin.randomUuid()); o.put("device_id", DeviceCodeLogin.randomHex(8).replace(Regex("[a-f]"), "") + "0000000000000000".substring(0, 8)); o.toString()
                                        })
                                }
                            }
                        }
                        AccountPool.save(AccountPool.load().filterNot { it.id == acc.id } + acc)
                        onDone(acc.nickname)
                    }
                }
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private data class Quad(val l1: String, val l2: String, val l3: String, val hint: String)
