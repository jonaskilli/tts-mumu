package com.github.jing332.tts_server_android.compose.systts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jing332.tts_server_android.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogFilterDialog(
    errorOnly: Boolean,
    onShowAll: () -> Unit,
    onErrorsOnly: () -> Unit,
    showPluginLogs: Boolean,
    onPluginLogsToggle: () -> Unit,
    showSpeechRuleLogs: Boolean,
    onSpeechRuleLogsToggle: () -> Unit,
    autoScrollToBottom: Boolean,
    onAutoScrollToggle: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filter_log_level)) },
        text = {
            Column {
                Text(
                    text = "显示范围",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 显示范围两态（用户 09-08 简化：五级别键退役——只留"全部/只看错误"）
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !errorOnly,
                        onClick = onShowAll,
                        label = { Text("显示全部") }
                    )
                    FilterChip(
                        selected = errorOnly,
                        onClick = onErrorsOnly,
                        label = { Text("只看错误") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    )
                }
                
                // 调试选项分割线
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // 调试选项标题
                Text(
                    text = "调试选项",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // 调试选项（同一行）
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 插件日志开关
                    FilterChip(
                        selected = showPluginLogs,
                        onClick = { onPluginLogsToggle() },
                        label = { Text("插件日志") },
                        leadingIcon = {
                            if (showPluginLogs) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    )
                    
                    // 朗读规则日志开关
                    FilterChip(
                        selected = showSpeechRuleLogs,
                        onClick = { onSpeechRuleLogsToggle() },
                        label = { Text("朗读规则日志") },
                        leadingIcon = {
                            if (showSpeechRuleLogs) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    )

                    // 实时滚动开关
                    FilterChip(
                        selected = autoScrollToBottom,
                        onClick = { onAutoScrollToggle() },
                        label = { Text("实时显示最新日志") },
                        leadingIcon = {
                            if (autoScrollToBottom) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
