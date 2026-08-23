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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jing332.common.LogLevel
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.ui.LogLevelColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogFilterDialog(
    selectedLevels: List<Int>,
    onLevelToggle: (Int) -> Unit,
    showPluginLogs: Boolean,
    onPluginLogsToggle: () -> Unit,
    showSpeechRuleLogs: Boolean,
    onSpeechRuleLogsToggle: () -> Unit,
    autoScrollToBottom: Boolean,
    onAutoScrollToggle: () -> Unit,
    onDismiss: () -> Unit
) {
    val levelOptions = listOf(
        LogLevel.ERROR to "ERROR",
        LogLevel.WARN to "WARN",
        LogLevel.INFO to "INFO",
        LogLevel.DEBUG to "DEBUG",
        LogLevel.TRACE to "VERBOSE"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filter_log_level)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.select_log_level_to_filter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    levelOptions.forEach { (level, name) ->
                        val isSelected = level in selectedLevels
                        FilterChip(
                            selected = isSelected,
                            onClick = { onLevelToggle(level) },
                            label = { Text(name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = getLevelContainerColor(level),
                                selectedLabelColor = LogLevelColors.palette(level).onContainer
                            )
                        )
                    }
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

@Composable
private fun getLevelContainerColor(level: Int): Color {
    // 统一走 LogLevelColors 令牌，保证明暗主题下容器底色都合适
    return LogLevelColors.palette(level).container
}
