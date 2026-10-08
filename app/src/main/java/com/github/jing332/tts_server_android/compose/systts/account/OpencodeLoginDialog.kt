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
import com.github.jing332.tts_server_android.service.systts.help.KeyListFile
import com.github.jing332.tts_server_android.service.systts.help.OpencodeChannel

/**
 * opencode 登录弹窗（10-09 用户反馈「凭据是啥都不知道」——它无 OAuth 流，插件同款只有两种拿法）：
 * ①一键匿名通道：Bearer public，免注册直接用（仅免费模型）——主推；
 * ②付费 Key 引导：打开 Zen 控制台网页注册拿 sk- key，粘回即可（官方形态就这样，电脑插件同）。
 *
 * ⚠️ 10-09 真机实锤修复：匿名凭据**不进账号池**——账号池凭据是全站通用的（轮换时会被拿到
 * 别的站点用，字面量 public 打腾讯接口必拒，「*尾blic」事故）。匿名/Key 直接落成 opencode
 * 站点的密钥条目（KeyListFile），与账号池隔离。
 */
@Composable
fun OpencodeLoginDialog(
    onDismiss: () -> Unit,
    onDone: (String) -> Unit,
) {
    val context = LocalContext.current
    var key by remember { mutableStateOf("") }

    /**
     * 落成 opencode 站点密钥条目（不进账号池）：接口分组「OpenCode」同站同名复用，
     * 匿名条目模型=免费清单、sk- 条目模型=全清单；密钥按（站点+钥+模型）去重。
     */
    fun saveToKeys(apiKey: String, nickname: String): Boolean {
        if (apiKey.isBlank()) return false
        val baseUrl = OpencodeChannel.chatBaseUrl
        val models = OpencodeChannel.fetchModels(apiKey)
        val tagRuleId = KeyListFile.DEFAULT_TAG_RULE_ID
        val ifaces = KeyListFile.readInterfaces(tagRuleId)
        val keys = KeyListFile.readKeys(tagRuleId)
        val updatedIfaces = if (ifaces.any { it.name == "OpenCode" }) {
            ifaces.map {
                if (it.name == "OpenCode") {
                    val newModels = models.filter { m -> m !in it.models }
                    if (newModels.isEmpty()) it else it.copy(models = it.models + newModels)
                } else it
            }
        } else {
            ifaces + KeyListFile.ApiInterface(
                name = "OpenCode",
                baseUrl = baseUrl,
                apiKey = apiKey,
                models = models,
            )
        }
        val modelName = models.firstOrNull() ?: return false
        // 同站+同钥+同模型去重（照 addAsKey 口径）
        val dup = keys.any { e ->
            val p = KeyListFile.parseKeyValue(e.value)
            p != null && !p.isDirect && p.key == apiKey && p.model == modelName
        }
        if (!dup) {
            KeyListFile.saveKeys(
                tagRuleId,
                keys + KeyListFile.KeyEntry(
                    name = KeyListFile.dedupName("$nickname · $modelName", keys.map { it.name }.toSet()),
                    keyCode = KeyListFile.nextKeyCode(keys),
                    value = "$baseUrl@@$modelName@@$apiKey",
                ),
            )
        }
        KeyListFile.saveInterfaces(tagRuleId, updatedIfaces)
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
                // 方式一：一键匿名（主推）——直接落成 OpenCode 组密钥（免费模型）
                TextButton(onClick = {
                    if (saveToKeys(OpencodeChannel.ANON_KEY, "匿名通道")) {
                        onDismiss(); onDone("匿名通道（OpenCode 组已生成免费模型）")
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
                    if (saveToKeys(key, "OpenCode")) {
                        onDismiss(); onDone("OpenCode（组已生成模型清单）")
                    }
                },
            ) { Text("保存 Key") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
