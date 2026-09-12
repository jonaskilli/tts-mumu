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
import androidx.compose.ui.unit.dp
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.AppSpinner

/**
 * 批量修改配置（用户 09-12 拍板改名，原「批量修改来源字段」）：插件筛选 + 采样率 + 来源插件切换。
 * 原「启用状态」（批量启用/批量停用/不改）一行已按用户 09-12 拍板删除。
 * [sampleRateOptions] 由调用方提供（「采样率自动识别」=-1 语义由调用方解释）。
 * [pluginOptions] 插件筛选候选：pluginId（""=全部，不按插件筛选）→ 显示名，仅含作用域内实际出现的插件；
 * [pluginItemCounts] pluginId → 作用域内配置项数（""=总数），供选择后实时显示影响范围。
 * [targetPluginOptions] 来源插件切换候选：全部已安装插件 pluginId → 显示名。
 * 采样率/来源插件两个维度均可跳过（不选=不改）；采样率仅插件型配置生效。
 */
@Composable
fun BatchSourceFieldsDialog(
    scopeDesc: String,
    pluginOptions: List<Pair<String, String>>,
    pluginItemCounts: Map<String, Int>,
    sampleRateOptions: List<Int>,
    targetPluginOptions: List<Pair<String, String>>,
    onDismissRequest: () -> Unit,
    onApply: (
        pluginId: String?,
        sampleRate: Int?,
        targetPluginId: String?,
    ) -> Unit,
) {
    var selectedPluginKey by remember { mutableStateOf<Any>("") }
    // AppSpinner 需要 Any 非空值：用 "none"/"auto"/Int/"具体pluginId" 作为哨兵
    var rateSelKey by remember { mutableStateOf<Any>("none") }
    var targetPluginKey by remember { mutableStateOf<Any>("none") }
    val targetCount = pluginItemCounts[selectedPluginKey] ?: 0

    AppDialog(
        title = { Text("批量修改配置") },
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
            Row {
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
