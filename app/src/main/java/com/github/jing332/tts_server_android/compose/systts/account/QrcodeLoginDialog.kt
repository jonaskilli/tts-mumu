package com.github.jing332.tts_server_android.compose.systts.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.jing332.common.utils.toast
import com.github.jing332.tts_server_android.service.systts.help.AccountPool
import com.github.jing332.tts_server_android.service.systts.help.RaccoonChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * raccoon（商汤小浣熊）扫码登录弹窗（10-09 全渠道批，协议=规格书 §8.1）。
 *
 * 流程：本地自造 32hex code → 二维码内容 = 公开页链接（微信内完成授权）→
 * 2s 轮询 pollQrcode（5 分钟超时）→ success 拿 access/refresh token 落盘。
 * 状态机：PENDING 等待 / LOGGING 已扫码确认中 / CANCELED 已取消可重试（重新生成 code）/
 * 超时可重试 / OK 落盘（expiresAt = JWT exp，解析不出 +3h——实测 exp−nbf≈10805s≈3h）。
 *
 * ⚠️ 轮询异常引擎侧已降级 pending（RaccoonChannel.pollQrcode），这里只按终态分流。
 * 二维码渲染第二期接（项目无 zxing/qrcode 依赖，不为此引新库）——当前显示链接全文+复制。
 */
// 10-10 M3 Expressive：LoadingIndicator 仍在实验 API，需显式 OptIn
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QrcodeLoginDialog(
    onDismiss: () -> Unit,
    onDone: (String) -> Unit,
) {
    val context = LocalContext.current

    // 弹窗阶段：LOADING(生成 code 中) / PENDING(等扫码) / LOGGING(已扫码) /
    // CANCELED(已取消) / TIMEOUT(超时) / SUCCESS / ERROR
    var phase by remember { mutableStateOf("LOADING") }
    var qrUrl by remember { mutableStateOf("") }
    var errText by remember { mutableStateOf("") }
    // 重换代号：CANCELED/TIMEOUT 点「重试」自增，LaunchedEffect 重启=重新生成 code 再轮询
    var generation by remember { mutableIntStateOf(0) }

    LaunchedEffect(generation) {
        errText = ""
        val (code, url) = withContext(Dispatchers.IO) { RaccoonChannel.qrcodeContent() }
        qrUrl = url
        phase = "PENDING"

        // 规格书 8.1：2s 间隔，5 分钟总超时
        val deadline = System.currentTimeMillis() + 5 * 60_000L
        while (System.currentTimeMillis() < deadline) {
            delay(2000L)
            val (st, at, rt) = withContext(Dispatchers.IO) { RaccoonChannel.pollQrcode(code) }
            when (st) {
                "PENDING" -> Unit
                "LOGGING" -> phase = "LOGGING"
                "CANCELED" -> { phase = "CANCELED"; return@LaunchedEffect }
                "OK" -> {
                    // JWT exp 优先（RaccoonChannel 私有逻辑不可见，这里简单解析）；兜底 +3h
                    val expiresAt = jwtExpMs(at)
                        ?: (System.currentTimeMillis() + 3L * 3600_000L)
                    // 10-10 登录去重：identity = access_token 的 JWT sub；解不出传 null 诚实降级
                    val acc = AccountPool.upsert("raccoon", AccountPool.jwtSub(at)) { existing ->
                        AccountPool.Account(
                            id = existing?.id ?: "raccoon-${System.currentTimeMillis().toString(16)}",
                            provider = "raccoon",
                            nickname = existing?.nickname ?: "小浣熊",
                            accessToken = at,
                            refreshToken = rt,
                            expiresAt = expiresAt,
                        ).withExtra("_uid", AccountPool.jwtSub(at) ?: "")
                    }
                    phase = "SUCCESS"
                    onDone(if (acc.isUpdate) "${acc.nickname}(已更新)" else acc.nickname)
                    return@LaunchedEffect
                }
                else -> Unit // 未知状态一律当 pending（引擎已兜底，双保险）
            }
        }
        phase = "TIMEOUT"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Raccoon 扫码登录") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (phase) {
                    "LOADING" -> {
                        // 10-10 M3 Expressive 改造：生成登录码等待换 LoadingIndicator
                        LoadingIndicator(Modifier.padding(bottom = 12.dp))
                        Text("正在生成登录码…", style = MaterialTheme.typography.bodySmall)
                    }
                    "PENDING" -> {
                        Text(
                            "用微信「扫一扫」，打开此链接后完成授权",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        // 二维码渲染第二期接（无现成依赖不引新库）：先给链接全文，可选可复制
                        SelectionContainer {
                            Text(
                                qrUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = {
                            val cb = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                as android.content.ClipboardManager
                            cb.setPrimaryClip(android.content.ClipData.newPlainText("url", qrUrl))
                            context.toast("链接已复制，可在微信中打开完成授权")
                        }) {
                            Icon(Icons.Default.ContentCopy, "复制链接", Modifier.padding(end = 4.dp))
                            Text("复制链接")
                        }
                    }
                    "LOGGING" -> Text("已扫码，确认中…", style = MaterialTheme.typography.bodyMedium)
                    "CANCELED" -> Text("已取消", color = MaterialTheme.colorScheme.error)
                    "TIMEOUT" -> Text("二维码已超时，请重试", color = MaterialTheme.colorScheme.error)
                    "SUCCESS" -> Text("登录成功", style = MaterialTheme.typography.bodyMedium)
                    else -> Text(
                        errText.ifEmpty { "登录失败" },
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = {
            when {
                phase == "CANCELED" || phase == "TIMEOUT" ->
                    TextButton(onClick = { generation++ }) { Text("重试") }
                else ->
                    TextButton(onClick = onDismiss) { Text(if (phase == "SUCCESS") "完成" else "后台等待") }
            }
        },
        dismissButton = {
            if (phase != "SUCCESS")
                TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

/** JWT payload exp（base64url 解析不验签，秒→毫秒）；解析不出返回 null（调用方兜底 +3h） */
private fun jwtExpMs(token: String): Long? = try {
    val parts = token.split(".")
    if (parts.size < 2) null
    else {
        val payload = String(
            android.util.Base64.decode(
                parts[1],
                android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE
            ),
            Charsets.UTF_8
        )
        val exp = org.json.JSONObject(payload).optLong("exp", 0L)
        if (exp > 0) exp * 1000L else null
    }
} catch (_: Exception) {
    null
}
