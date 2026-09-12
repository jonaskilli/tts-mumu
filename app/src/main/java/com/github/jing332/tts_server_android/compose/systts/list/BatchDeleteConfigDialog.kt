package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.AppSpinner
import com.github.jing332.tts_server_android.R

/**
 * 批量删除配置项（用户 09-12 拍板新增）：按来源插件筛出一批配置项，确认后整体删除。
 *
 * [pluginOptions] **只含具体插件**（调用方需过滤掉「全部（不按插件筛选）」一项）——
 * 用户 09-12 拍板：删除下拉不提供「全部」，避免一手滑把整个池子删空。
 * [pluginItemCounts] pluginId → 作用域内配置项数，供选插件后实时显示将删除多少项。
 * [scopeDesc] 作用域描述（「当前池全部配置项」/「搜索结果」），与「批量修改配置」同口径。
 */
@Composable
fun BatchDeleteConfigDialog(
    scopeDesc: String,
    pluginOptions: List<Pair<String, String>>,
    pluginItemCounts: Map<String, Int>,
    onDismissRequest: () -> Unit,
    onDelete: (pluginId: String) -> Unit,
) {
    // 默认选中第一个插件（无插件时保持空 key，下方显示 0 项且删除键禁用）
    var selectedPluginKey by remember {
        mutableStateOf<Any>(pluginOptions.firstOrNull()?.first ?: "")
    }
    val targetCount = pluginItemCounts[selectedPluginKey] ?: 0

    AppDialog(
        title = { Text("批量删除配置项") },
        content = {
            Column {
                AppSpinner(
                    modifier = Modifier.fillMaxWidth(),
                    labelText = "插件",
                    value = selectedPluginKey,
                    values = pluginOptions.map { it.first },
                    entries = pluginOptions.map { it.second },
                    onSelectedChange = { key, _ -> selectedPluginKey = key }
                )
                Text(
                    "匹配 $targetCount 项",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "作用域：$scopeDesc",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "删除后不可恢复",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        buttons = {
            Row {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = { (selectedPluginKey as? String)?.takeIf { it.isNotEmpty() }?.let(onDelete) },
                    enabled = targetCount > 0
                ) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        onDismissRequest = onDismissRequest
    )
}
