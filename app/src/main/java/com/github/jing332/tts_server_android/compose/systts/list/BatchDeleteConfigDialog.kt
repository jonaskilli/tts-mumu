package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.AppSpinner
import com.github.jing332.tts_server_android.R

/**
 * 批量删除清单条目：来源插件 + 所属**实际分组**（大分组，或「大分组 › 子分组」）+ 项名 + 音色。
 * 本地TTS项不参与本弹窗（无来源插件），由调用方过滤掉。
 */
data class BatchDeleteEntry(
    val pluginId: String,
    val groupLabel: String,
    val name: String,
    val voice: String,
)

/**
 * 批量删除插件配置项（用户 09-12 拍板新增）：
 * 按来源插件筛出待删配置项，并在弹窗里**按实际分组折叠列出具体项**——用户 09-12 要求
 * 「应该能列出来全部的配置项」，所以默认折叠、点分组才展开显示项名与音色。
 *
 * [entries] 调用方一次性备好（含 pluginId/分组标签/项名/音色），弹窗内按当前所选插件过滤。
 * [pluginOptions] **只含具体插件**（调用方已过滤掉「全部（不按插件筛选）」）——
 * 用户 09-12 拍板：删除下拉不提供「全部」，避免一手滑把整个池子删空。
 */
@Composable
fun BatchDeleteConfigDialog(
    scopeDesc: String,
    pluginOptions: List<Pair<String, String>>,
    entries: List<BatchDeleteEntry>,
    onDismissRequest: () -> Unit,
    onDelete: (pluginId: String) -> Unit,
) {
    // 默认选中第一个插件（无插件时保持空 key，下方 0 项且删除键禁用）
    var selectedPluginKey by remember {
        mutableStateOf<Any>(pluginOptions.firstOrNull()?.first ?: "")
    }
    // 分组展开状态：默认全收起，点分组行才展开
    var expandedGroups by remember { mutableStateOf<Set<String>>(emptySet()) }
    val selectedPluginId = selectedPluginKey as? String ?: ""
    val buckets = entries.filter { it.pluginId == selectedPluginId }
        .groupBy { it.groupLabel }
        .toList()
    val targetCount = buckets.sumOf { it.second.size }

    AppDialog(
        title = { Text("批量删除插件配置项") },
        content = {
            Column {
                AppSpinner(
                    modifier = Modifier.fillMaxWidth(),
                    labelText = "插件",
                    value = selectedPluginKey,
                    values = pluginOptions.map { it.first },
                    entries = pluginOptions.map { it.second },
                    onSelectedChange = { key, _ ->
                        selectedPluginKey = key
                        // 换插件后清单整批变样，展开状态一并重置
                        expandedGroups = emptySet()
                    }
                )
                Text(
                    "匹配 $targetCount 项　·　作用域：$scopeDesc",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                // 清单：按实际分组折叠，展开后列出该分组下待删的具体配置项
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    items(buckets, key = { it.first }) { (groupLabel, groupItems) ->
                        val expanded = groupLabel in expandedGroups
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedGroups = if (expanded)
                                            expandedGroups - groupLabel
                                        else
                                            expandedGroups + groupLabel
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = if (expanded) "收起" else "展开",
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .rotate(if (expanded) 0f else -90f),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = groupLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "(${groupItems.size})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (expanded) {
                                groupItems.forEach { entry ->
                                    Text(
                                        text = if (entry.voice.isBlank())
                                            entry.name
                                        else
                                            "${entry.name}　·　${entry.voice}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 28.dp, top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        buttons = {
            Row {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = { selectedPluginId.takeIf { it.isNotEmpty() }?.let(onDelete) },
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
