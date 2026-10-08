package com.github.jing332.tts_server_android.compose.systts.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.jing332.tts_server_android.service.systts.help.ChannelBootstrap
import com.github.jing332.tts_server_android.service.systts.help.ChatChannel
import com.github.jing332.tts_server_android.service.systts.help.ChatChannels

/**
 * 渠道选择弹窗（10-09 全渠道批）：池页顶栏「+」先选渠道再进对应登录流。
 * 登录形态分组（照规格书 0.1）：
 *  - WEBVIEW：codebuddy（既有 AccountLoginActivity 轮询链）
 *  - DEVICE_CODE：cline / minimax / zcode / qoder（弹窗内轮询，DeviceCodeDialog）
 *  - CREDENTIAL：opencode（API key/匿名）/ loomy（session）/ raccoon / trae / lobsterai /
 *    codearts / gemini（凭据/token 直填或第二期 WebView 回调；本期给「粘贴凭据」路径）
 */
object LoginFlowKind {
    const val WEBVIEW = "webview"
    const val DEVICE_CODE = "device"
    const val CREDENTIAL = "credential"
    const val QRCODE = "qrcode"
    const val SMS = "sms"
    const val CALLBACK = "callback"
    const val OPENCODE = "opencode"
}

/** 渠道 → 登录形态（静态表；与各渠道实现同步维护） */
fun loginKindOf(provider: String): String = when (provider) {
    "codebuddy", "workbuddy" -> LoginFlowKind.WEBVIEW // workbuddy 同为 auth/state 轮询链（10-09 接入）
    "cline", "minimax", "zcode", "qoder" -> LoginFlowKind.DEVICE_CODE
    "raccoon" -> LoginFlowKind.QRCODE
    "loomy" -> LoginFlowKind.SMS
    "trae", "gemini", "lobsterai", "codearts" -> LoginFlowKind.CALLBACK
    "opencode" -> LoginFlowKind.OPENCODE // 一键匿名+控制台引导（无 OAuth 流，官方形态）
    else -> LoginFlowKind.CREDENTIAL
}

@Composable
fun ChannelPickerDialog(
    onDismiss: () -> Unit,
    onPick: (ChatChannel) -> Unit,
) {
    ChannelBootstrap.install()
    val channels = ChatChannels.all()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择登录渠道") },
        text = {
            // 13 渠道超高：AlertDialog text 槽不自带滚动（真机实锤截断不可滑）——
            // 手动 verticalScroll + 高度上限（屏幕 0.7f），超出即弹内滚动
            Column(
                Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                channels.forEach { ch ->
                    val kind = loginKindOf(ch.id)
                    val note = when {
                        !ch.available -> "对话待接入（可登录管凭据）"
                        kind == LoginFlowKind.DEVICE_CODE -> "设备码授权"
                        kind == LoginFlowKind.WEBVIEW -> "浏览器登录"
                        kind == LoginFlowKind.QRCODE -> "微信扫码登录"
                        kind == LoginFlowKind.SMS -> "手机验证码登录"
                        kind == LoginFlowKind.CALLBACK -> "浏览器授权（本地回调）"
                        kind == LoginFlowKind.OPENCODE -> "一键匿名 / 控制台 Key"
                        else -> "粘贴凭据"
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(ch) }
                            .padding(vertical = 10.dp)
                    ) {
                        Text(ch.displayName, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            note,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
