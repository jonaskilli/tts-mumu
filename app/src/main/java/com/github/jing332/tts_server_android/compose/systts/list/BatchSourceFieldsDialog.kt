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
 * 批量修改来源字段：插件筛选 + 启用/停用 + 采样率 + 来源插件切换。
 * [sampleRateOptions] 由调用方提供（「跟随音源格式」=-1 语义由调用方解释）。
 * [pluginOptions] 插件筛选候选：pluginId（""=全部，不按插件筛选）→ 显示名，仅含作用域内实际出现的插件；
 * [pluginItemCounts] pluginId → 作用域内配置项数（""=总数），供选择后实时显示影响范围。
 * [targetPluginOptions] 来源插件切换候选：全部已安装插件 pluginId → 显示名。
 * 启用/采样率/来源插件三个维度均可跳过（不选=不改）；采样率与启用仅插件型配置生效。
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
        enabled: Boolean?,
        sampleRate: Int?,
        targetPluginId: String?,
    ) -> Unit,
) {
    var selectedPluginKey by remember { mutableStateOf<Any>("") }
    var enabledSel by remember { mutableStateOf<Boolean?>(null) }
    // AppSpinner 需要 Any 非空值：用 "none"/"auto"/Int/"具体pluginId" 作为哨兵
    var rateSelKey by remember { mutableStateOf<Any>("none") }
    var targetPluginKey by remember { mutableStateOf<Any>("none") }
    val targetCount = pluginItemCounts[selectedPluginKey] ?: 0

    AppDialog(
        title = { Text("批量修改来源字段") },
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
                    "作用域：$scopeDesc，选中范围内共 $targetCount 项。未选择的维度保持原值。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    "启用状态（当前：${when (enabledSel) {
                        true -> "启用"
                        false -> "停用"
                        null -> "不修改"
                    }}）",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Row(Modifier.fillMaxWidth()) {
                    TextButton(onClick = { enabledSel = true }) { Text("批量启用") }
                    TextButton(onClick = { enabledSel = false }) { Text("批量停用") }
                    TextButton(onClick = { enabledSel = null }) { Text("不改") }
                }

                Text(
                    "采样率（跟随音源格式=由音频头探测）",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                val rateEntries = listOf("不修改", "跟随音源格式") + sampleRateOptions.map { "$it Hz" }
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
                    "来源插件（把配置项改指向另一个插件，发音人等字段保持原值）",
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
                        enabledSel,
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
