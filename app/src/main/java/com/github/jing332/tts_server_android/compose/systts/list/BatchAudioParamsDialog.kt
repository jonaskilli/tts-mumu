package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.AppSpinner
import com.github.jing332.compose.widgets.LabelSlider
import com.github.jing332.common.utils.toScale
import com.github.jing332.tts_server_android.R

/**
 * 批量调整音频参数：把作用域内配置项的 audioParams 统一设值。
 * [scopeDesc] 描述作用域（"搜索结果"/"当前池全部配置项"）。
 * [pluginOptions] 插件选择器候选：pluginId（""=全部，不按插件筛选）→ 显示名，仅含作用域内实际出现的插件；
 * [pluginItemCounts] pluginId → 作用域内配置项数（""=总数），供选择后实时显示影响范围。
 * [onApply] pluginId 为 null 表示全部（不筛选）；speed/volume/pitch 均可空：null=该维度保持原值；重置=三维度全 1f。
 */
@Composable
fun BatchAudioParamsDialog(
    scopeDesc: String,
    pluginOptions: List<Pair<String, String>>,
    pluginItemCounts: Map<String, Int>,
    onDismissRequest: () -> Unit,
    onApply: (pluginId: String?, speed: Float?, volume: Float?, pitch: Float?) -> Unit,
) {
    var selectedPluginKey by remember { mutableStateOf<Any>("") }
    var speed by remember { mutableFloatStateOf(1f) }
    var volume by remember { mutableFloatStateOf(1f) }
    var pitch by remember { mutableFloatStateOf(1f) }
    val targetCount = pluginItemCounts[selectedPluginKey] ?: 0

    AppDialog(
        title = { Text("批量调整音频参数") },
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
                    "作用域：$scopeDesc，选中范围内共 $targetCount 项\n" +
                            "修改后这些配置项的单条语速/音量/音高将被统一替换。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LabelSlider(
                    value = speed,
                    onValueChange = { speed = it.toScale(2) },
                    valueRange = 0.1f..3f,
                    step = 0.05f,
                    buttonLongSteps = 0.05f,
                    text = stringResource(id = R.string.label_speech_rate, "%.2f".format(speed))
                )
                LabelSlider(
                    value = volume,
                    onValueChange = { volume = it.toScale(2) },
                    valueRange = 0.1f..3f,
                    step = 0.05f,
                    buttonLongSteps = 0.05f,
                    text = stringResource(id = R.string.label_speech_volume, "%.2f".format(volume))
                )
                LabelSlider(
                    value = pitch,
                    onValueChange = { pitch = it.toScale(2) },
                    valueRange = 0.1f..3f,
                    step = 0.05f,
                    buttonLongSteps = 0.05f,
                    text = stringResource(id = R.string.label_speech_pitch, "%.2f".format(pitch))
                )
            }
        },
        buttons = {
            Row {
                TextButton(onClick = {
                    onApply((selectedPluginKey as? String)?.takeIf { it.isNotEmpty() }, 1f, 1f, 1f)
                }) {
                    Text("重置为 1.0")
                }
                TextButton(onClick = {
                    onApply((selectedPluginKey as? String)?.takeIf { it.isNotEmpty() }, speed, volume, pitch)
                }) {
                    Text("应用")
                }
            }
        },
        onDismissRequest = onDismissRequest
    )
}
