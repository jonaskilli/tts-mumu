package com.github.jing332.tts_server_android.compose.backup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import kotlinx.coroutines.launch

@Composable
internal fun RestoreDialog(
    onDismissRequest: () -> Unit,
    bytes: ByteArray,
    vm: BackupRestoreViewModel = viewModel()
) {
    var isLoading by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<RestoreResult?>(null) }
    // null=尚未检测；非空=合并恢复遇到插件 ID 冲突，等待用户选择覆盖/共存
    var conflicts by remember { mutableStateOf<List<PluginConflict>?>(null) }
    var resolution by remember { mutableStateOf(PluginConflictResolution.OVERWRITE) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val found = vm.pluginConflicts(bytes)
        if (found.isEmpty()) {
            result = vm.restore(bytes)
        } else {
            conflicts = found
        }
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
                    val asking = conflicts
                    when {
                        asking != null && resolved == null -> {
                            Text(
                                stringResource(R.string.backup_plugin_conflict_title),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                asking.joinToString("\n") { conflict ->
                                    val name = conflict.name.ifBlank { conflict.pluginId }
                                    "• $name（${conflict.pluginId}）"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            ConflictOptionRow(
                                label = stringResource(R.string.backup_plugin_conflict_overwrite),
                                selected = resolution == PluginConflictResolution.OVERWRITE,
                                onSelect = { resolution = PluginConflictResolution.OVERWRITE },
                            )
                            ConflictOptionRow(
                                label = stringResource(R.string.backup_plugin_conflict_coexist),
                                selected = resolution == PluginConflictResolution.COEXIST,
                                onSelect = { resolution = PluginConflictResolution.COEXIST },
                            )
                        }

                        resolved is RestoreResult.Success -> {
                            if (resolved.restartRequired) {
                                Text(stringResource(id = R.string.restarting))
                            } else {
                                Text(stringResource(id = R.string.restore_finished))
                            }
                            if (resolved.warnings.isNotEmpty()) {
                                Text(
                                    resolved.warnings.joinToString("\n") { "⚠️ $it" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }

                        resolved is RestoreResult.Failure -> Text(
                            resolved.message,
                            color = MaterialTheme.colorScheme.error,
                        )

                        else -> Unit
                    }
                }
            }
        },
        buttons = {
            when {
                askingConflict(conflicts, resolved) -> {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(onClick = {
                        val chosen = resolution
                        conflicts = null
                        isLoading = true
                        scope.launch {
                            result = vm.restore(bytes, chosen)
                            isLoading = false
                        }
                    }) {
                        Text(stringResource(R.string.confirm))
                    }
                }

                mustRestart -> {
                    // 自动重启进行中；保留按钮作为手动立即重启的兜底
                    TextButton(onClick = { app.restart() }) {
                        Text(stringResource(id = R.string.restart))
                    }
                }

                !isLoading -> {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(id = R.string.confirm))
                    }
                }
            }
        }
    )
}

private fun askingConflict(conflicts: List<PluginConflict>?, resolved: RestoreResult?) =
    conflicts != null && resolved == null

@Composable
private fun ConflictOptionRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
