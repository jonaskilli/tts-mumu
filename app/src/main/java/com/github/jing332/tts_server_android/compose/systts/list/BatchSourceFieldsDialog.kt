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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.AppSpinner

/**
 * 批量修改来源字段：启用/停用、采样率、locale（仅插件型配置生效）。
 * [sampleRateOptions] 由调用方提供（含「自动识别格式」=null 语义由调用方解释）。
 * 每个维度均可跳过（不选=不改）。
 */
@Composable
fun BatchSourceFieldsDialog(
    itemCount: Int,
    scopeDesc: String,
    sampleRateOptions: List<Int>,
    localeOptions: List<String>,
    onDismissRequest: () -> Unit,
    onApply: (enabled: Boolean?, sampleRate: Int?, locale: String?) -> Unit,
) {
    var enabledSel by remember { mutableStateOf<Boolean?>(null) }
    // AppSpinner 需要 Any 非空值：用 "none"/"auto"/Int/"具体locale" 作为哨兵
    var rateSelKey by remember { mutableStateOf<Any>("none") }
    var localeSelKey by remember { mutableStateOf<Any>("none") }

    AppDialog(
        title = { Text("批量修改来源字段") },
        content = {
            Column {
                Text(
                    "作用域：$scopeDesc（共 $itemCount 项）。未选择的维度保持原值；仅插件型配置项会被修改。",
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
                    "采样率（自动识别=由音频头探测）",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                val rateEntries = listOf("不修改", "自动识别格式") + sampleRateOptions.map { "$it Hz" }
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
                    "locale（音色分类名）",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                val localeValues: List<Any> = listOf("none") + localeOptions
                AppSpinner(
                    modifier = Modifier.fillMaxWidth(),
                    labelText = "locale",
                    value = localeSelKey,
                    values = localeValues,
                    entries = listOf("不修改") + localeOptions,
                    onSelectedChange = { key, _ -> localeSelKey = key }
                )
            }
        },
        buttons = {
            Row {
                TextButton(onClick = {
                    onApply(
                        enabledSel,
                        when (val k = rateSelKey) { "none" -> null; "auto" -> -1; else -> k as? Int },
                        if (localeSelKey == "none") null else localeSelKey as String,
                    )
                }) {
                    Text("应用")
                }
            }
        },
        onDismissRequest = onDismissRequest
    )
}
