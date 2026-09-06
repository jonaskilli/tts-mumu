package com.github.jing332.tts_server_android.compose.backup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.LoadingContent
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.app
import kotlinx.coroutines.delay

@Composable
internal fun RestoreDialog(
    onDismissRequest: () -> Unit,
    bytes: ByteArray,
    vm: BackupRestoreViewModel = viewModel()
) {
    var isLoading by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<RestoreResult?>(null) }

    LaunchedEffect(Unit) {
        result = vm.restore(bytes)
        isLoading = false
    }

    // 恢复偏好设置后进程内缓存(DataSaver等)与磁盘不一致，必须重启彻底重载；
    // 恢复成功即自动重启，无需用户确认
    val resolved = result
    val mustRestart = resolved is RestoreResult.Success && resolved.restartRequired
    LaunchedEffect(mustRestart) {
        if (mustRestart) {
            delay(1200)
            app.restart()
        }
    }

    AppDialog(
        onDismissRequest = { if (mustRestart) app.restart() else onDismissRequest() },
        properties = DialogProperties(
            dismissOnBackPress = !mustRestart,
            dismissOnClickOutside = !mustRestart,
        ),
        title = { Text(stringResource(id = R.string.restore)) },
        content = {
            LoadingContent(
                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                isLoading = isLoading
            ) {
                // LoadingContent 内容区是 Box，多段文字必须手动纵向排列，否则全部叠在同一位置
                Column {
                    when (val value = resolved) {
                        is RestoreResult.Success -> {
                            if (value.restartRequired) {
                                Text(stringResource(id = R.string.restarting))
                            } else {
                                Text(stringResource(id = R.string.restore_finished))
                            }
                            if (value.warnings.isNotEmpty()) {
                                Text(
                                    value.warnings.joinToString("\n") { "⚠️ $it" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }

                        is RestoreResult.Failure -> Text(
                            value.message,
                            color = MaterialTheme.colorScheme.error,
                        )

                        null -> Unit
                    }
                }
            }
        },
        buttons = {
            if (mustRestart) {
                // 自动重启进行中；保留按钮作为手动立即重启的兜底
                TextButton(onClick = { app.restart() }) {
                    Text(stringResource(id = R.string.restart))
                }
            } else {
                if (!isLoading) {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(id = R.string.confirm))
                    }
                }
            }
        }
    )
}
