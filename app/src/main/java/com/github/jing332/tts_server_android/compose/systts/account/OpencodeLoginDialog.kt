package com.github.jing332.tts_server_android.compose.systts.account

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.jing332.tts_server_android.service.systts.help.AccountPool
import com.github.jing332.tts_server_android.service.systts.help.OpencodeChannel

/**
 * opencode 登录弹窗（10-09 用户反馈「凭据是啥都不知道」——它无 OAuth 流，插件同款只有两种拿法）：
 * ①一键匿名通道：Bearer public，免注册直接用（仅免费模型）——主推；
 * ②付费 Key 引导：打开 Zen 控制台网页注册拿 sk- key，粘回即可（官方形态就这样，电脑插件同）。
 */
@Composable
fun OpencodeLoginDialog(
    onDismiss: () -> Unit,
    onDone: (String) -> Unit,
) {
    val context = LocalContext.current
    var key by remember { mutableStateOf("") }

    fun save(apiKey: String, nickname: String): Boolean {
        if (apiKey.isBlank()) return false
        val acc = AccountPool.Account(
            id = "opencode-${System.currentTimeMillis().toString(16)}",
            provider = "opencode",
            nickname = nickname,
            accessToken = apiKey.trim(),
            refreshToken = "",
        )
        AccountPool.save(AccountPool.load().filterNot { it.id == acc.id } + acc)
        return true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("登录 OpenCode") },
        text = {
            Column {
                Text(
                    "两种方式，任选其一：",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                // 方式一：一键匿名（主推）
                TextButton(onClick = {
                    if (save(OpencodeChannel.ANON_KEY, "匿名通道")) {
                        onDismiss(); onDone("匿名通道")
                    }
                }) {
                    Text("一键使用匿名通道（免注册，仅免费模型）")
                }
                Spacer(Modifier.height(4.dp))
                // 方式二：控制台拿 Key
                Text(
                    "要用付费模型：点下方打开 OpenCode 控制台，注册后在 API Keys 页创建一个 sk- 开头的 Key，复制粘贴到这里。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://opencode.ai/zen")))
                }) {
                    Text("打开 OpenCode 控制台 ↗")
                }
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("粘贴 sk- 开头的 Key") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = key.trim().startsWith("sk-"),
                onClick = {
                    if (save(key, "OpenCode")) {
                        onDismiss(); onDone("OpenCode")
                    }
                },
            ) { Text("保存 Key") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
