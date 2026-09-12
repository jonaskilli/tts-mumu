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
 * 清单条目：来源插件 + 所属**实际分组**（大分组，或「大分组 › 子分组」）+ 项名 + 配置项 id。
 * 本地TTS项不参与（无来源插件），由调用方过滤掉。
 */
data class BatchConfigEntry(
    val pluginId: String,
    val groupLabel: String,
    val name: String,
    val configId: Long,
)

/**
 * 批量配置操作（用户 09-12 晚拍板：原「批量修改配置」与「批量删除插件配置项」两弹窗合并为一个）：
 * 插件筛选 + 采样率 + 来源插件切换 + 清单（按实际分组折叠，可整组删除）。
 * 合并依据：两弹窗外壳相同；且修改侧原本看不到匹配到的具体项（盲操作），借清单一并补上。
 * 原「启用状态」（批量启用/批量停用/不改）一行已按用户 09-12 拍板删除。
 * [sampleRateOptions] 由调用方提供（「采样率自动识别」=-1 语义由调用方解释）。
 * [pluginOptions] 插件筛选候选：pluginId（""=全部，不按插件筛选）→ 显示名，仅含作用域内实际出现的插件；
 * [pluginItemCounts] pluginId → 作用域内配置项数（""=总数），供选择后实时显示影响范围。
 * [targetPluginOptions] 来源插件切换候选：全部已安装插件 pluginId → 显示名。
 * [entries] 清单数据源（调用方一次性备好），弹窗内按所选插件过滤后折叠展示**项名**（不显示音色id，用户 09-12 晚定）。
 * [onDelete] 删除请求：groupLabel=null 表示删所选插件的**全部**匹配项，非空表示只删该分组。
 *   弹窗内不落库——调用方弹二次确认后才删（破坏性操作必须有确认，用户 09-12 晚定）。
 * 采样率/来源插件两个维度均可跳过（不选=不改）；采样率仅插件型配置生效。
 */
@Composable
fun BatchSourceFieldsDialog(
    scopeDesc: String,
    pluginOptions: List<Pair<String, String>>,
    pluginItemCounts: Map<String, Int>,
    sampleRateOptions: List<Int>,
    targetPluginOptions: List<Pair<String, String>>,
    entries: List<BatchConfigEntry>,
    onDismissRequest: () -> Unit,
    onApply: (
        pluginId: String?,
        sampleRate: Int?,
        targetPluginId: String?,
    ) -> Unit,
    onDelete: (pluginId: String, groupLabel: String?) -> Unit,
) {
    var selectedPluginKey by remember { mutableStateOf<Any>("") }
    // AppSpinner 需要 Any 非空值：用 "none"/"auto"/Int/"具体pluginId" 作为哨兵
    var rateSelKey by remember { mutableStateOf<Any>("none") }
    var targetPluginKey by remember { mutableStateOf<Any>("none") }
    // 分组展开状态：默认全收起，点分组行才展开
    var expandedGroups by remember { mutableStateOf<Set<String>>(emptySet()) }
    val selectedPluginId = selectedPluginKey as? String ?: ""
    val targetCount = pluginItemCounts[selectedPluginKey] ?: 0
    val buckets = entries.filter { it.pluginId == selectedPluginId }
        .groupBy { it.groupLabel }
        .toList()
    // 删除只对**具体插件**开放：选中「全部（不按插件筛选）」时为 0，按钮禁用，
    // 避免一手滑把整个池子删空（沿用用户 09-12 拍板口径）
    val deletableCount = if (selectedPluginId.isEmpty()) 0 else buckets.sumOf { it.second.size }

    AppDialog(
        title = { Text("批量配置操作") },
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
                // 用户 09-12：数字挪到插件框正下方（它说的是"当前选中的插件有多少项"），作用域只留一句
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

                Spacer(modifier = Modifier.height(2.dp))
                // 清单：按实际分组折叠，展开后列出该分组下的配置项（用户 09-12 晚：只显示项名）
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    items(buckets, key = { it.first }) { (groupLabel, groupItems) ->
                        val expanded = groupLabel in expandedGroups
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedGroups = if (expanded)
                                            expandedGroups - groupLabel
                                        else
                                            expandedGroups + groupLabel
                                    }
                                    .padding(vertical = 6.dp),
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
                                // 整组删除（用户 09-12 晚要求）：删该分组下全部项，确认后才落库
                                TextButton(onClick = { onDelete(selectedPluginId, groupLabel) }) {
                                    Text(
                                        stringResource(R.string.delete),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            if (expanded) {
                                groupItems.forEach { entry ->
                                    Text(
                                        text = entry.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 28.dp, top = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    "采样率",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                val rateEntries = listOf("不修改", "采样率自动识别") + sampleRateOptions.map { "$it Hz" }
                val rateValues: List<Any> = listOf("none", "auto") + sampleRateOptions
                AppSpinner(
                    modifier = Modifier.fillMaxWidth(),
                    labelText = "采样率",
                    value = rateSelKey,
                    values = rateValues,
                    entries = rateEntries,
                    onSelectedChange = { key, _ -> rateSelKey = key }
                )

                Text(
                    "来源插件",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                val targetValues: List<Any> = listOf("none") + targetPluginOptions.map { it.first }
                AppSpinner(
                    modifier = Modifier.fillMaxWidth(),
                    labelText = "目标插件",
                    value = targetPluginKey,
                    values = targetValues,
                    entries = listOf("不修改") + targetPluginOptions.map { it.second },
                    onSelectedChange = { key, _ -> targetPluginKey = key }
                )
            }
        },
        buttons = {
            // 按钮排布与「音频参数设置」弹窗统一（用户 09-12 晚拍板）：取消（左）｜ 删除 · 应用（右）。
            // 删除键文案精简为「删除」——作用对象与数量由二次确认弹窗说明，不在按钮上堆"全部 N 项"
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(Modifier.weight(1f))
                // 删除该插件匹配的全部项（破坏性操作，红色 + 二次确认）；
                // 选中「全部（不按插件筛选）」时置灰，防一手滑把池子删空
                TextButton(
                    onClick = { onDelete(selectedPluginId, null) },
                    enabled = deletableCount > 0
                ) {
                    Text(
                        stringResource(R.string.delete),
                        color = if (deletableCount > 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = {
                    onApply(
                        (selectedPluginKey as? String)?.takeIf { it.isNotEmpty() },
                        when (val k = rateSelKey) { "none" -> null; "auto" -> -1; else -> k as? Int },
                        (targetPluginKey as? String)?.takeIf { it != "none" },
                    )
                }) {
                    Text("应用")
                }
            }
        },
        onDismissRequest = onDismissRequest
    )
}

/**
 * 批量删除二次确认（用户 09-12 晚定：破坏性操作必须有确认，原删除弹窗点一下就直接删）。
 * [label] 删除对象：如「插件「剪映最新官方中文774_免登」」或「分组「旁白 › 通用旁白」」。
 * [count] 待删配置项数。
 */
@Composable
fun BatchDeleteConfirmDialog(
    label: String,
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AppDialog(
        title = { Text("删除确认") },
        content = {
            Text("将删除$label 下的 $count 项配置。\n\n此操作不可恢复。")
        },
        buttons = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(onClick = onConfirm) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        onDismissRequest = onDismiss
    )
}
