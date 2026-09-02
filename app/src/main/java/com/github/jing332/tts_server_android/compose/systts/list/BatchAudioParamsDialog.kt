package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.LabelSlider
import com.github.jing332.common.utils.toScale
import com.github.jing332.tts_server_android.R

/**
 * 批量调整音频参数：把作用域内所有配置项的 audioParams 统一设值。
 * [itemCount] 仅为提示文案；[scopeDesc] 描述作用域（"搜索结果"/"全部配置项"）。
 * [onApply] 三参数均可空：null=该维度保持原值；重置=三维度全 1f。
 */
@Composable
fun BatchAudioParamsDialog(
    itemCount: Int,
    scopeDesc: String,
    onDismissRequest: () -> Unit,
    onApply: (speed: Float?, volume: Float?, pitch: Float?) -> Unit,
) {
    var speed by remember { mutableFloatStateOf(1f) }
    var volume by remember { mutableFloatStateOf(1f) }
    var pitch by remember { mutableFloatStateOf(1f) }

    AppDialog(
        title = { Text("批量调整音频参数") },
        content = {
            Column {
                Text(
                    "作用域：$scopeDesc（共 $itemCount 项）\n" +
                            "修改后这些配置项的单条语速/音量/音高将被统一替换。",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
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
                    onApply(1f, 1f, 1f)
                }) {
                    Text("重置为 1.0")
                }
                TextButton(onClick = {
                    onApply(speed, volume, pitch)
                }) {
                    Text("应用")
                }
            }
        },
        onDismissRequest = onDismissRequest
    )
}
