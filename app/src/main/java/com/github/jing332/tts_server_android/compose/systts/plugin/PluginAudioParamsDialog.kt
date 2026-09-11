package com.github.jing332.tts_server_android.compose.systts.plugin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.tts_server_android.R
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.tts_server_android.compose.systts.list.FloatSlider

@Composable
fun PluginAudioParamsDialog(
    initialParams: AudioParams,
    onDismissRequest: () -> Unit,
    onConfirm: (params: AudioParams) -> Unit
) {
    // 0 表示跟随，显示时转为 1.0，保存时保持现有全局/插件层语义
    var speed by remember { mutableFloatStateOf(if (initialParams.speed == 0f) 1f else initialParams.speed) }
    var volume by remember { mutableFloatStateOf(if (initialParams.volume == 0f) 1f else initialParams.volume) }
    var pitch by remember { mutableFloatStateOf(if (initialParams.pitch == 0f) 1f else initialParams.pitch) }

    // 外壳换 AppDialog（用户 09-11：与配置项音频参数弹窗统一——原先 material3 AlertDialog
    // 是另一套观感：白底圆角/标题/按钮排布都不同）；内容水平 +4dp 同 BasicAudioParamsDialog（全局弹窗）
    AppDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.plugin_audio_params)) },
        content = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                // 语速
                FloatSlider(
                    label = "语速",
                    value = speed,
                    onValueChange = { speed = it },
                    valueRange = 0.1f..3f,
                    step = 0.05f,
                    valueFormatter = { "%.2f".format(it) }
                )
                // 音量
                FloatSlider(
                    label = "音量",
                    value = volume,
                    onValueChange = { volume = it },
                    valueRange = 0.1f..3f,
                    step = 0.05f,
                    valueFormatter = { "%.2f".format(it) }
                )
                // 音高
                FloatSlider(
                    label = "音高",
                    value = pitch,
                    onValueChange = { pitch = it },
                    valueRange = 0.1f..3f,
                    step = 0.05f,
                    valueFormatter = { "%.2f".format(it) }
                )
            }
        },
        buttons = {
            TextButton(onClick = {
                speed = 1f
                volume = 1f
                pitch = 1f
            }) {
                Text(stringResource(id = R.string.reset))
            }
            TextButton(onClick = {
                onConfirm(
                    AudioParams(speed = speed, volume = volume, pitch = pitch)
                )
            }) {
                Text(stringResource(id = R.string.confirm))
            }
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}
