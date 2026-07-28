package com.github.jing332.tts_server_android.compose.systts.plugin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.list.FloatSlider

@Composable
fun PluginAudioParamsDialog(
    initialParams: AudioParams,
    onDismissRequest: () -> Unit,
    onConfirm: (AudioParams) -> Unit
) {
    // 0 表示跟随，显示时转为 1.0，保存时再转回
    var speed by remember { mutableFloatStateOf(if (initialParams.speed == 0f) 1f else initialParams.speed) }
    var volume by remember { mutableFloatStateOf(if (initialParams.volume == 0f) 1f else initialParams.volume) }
    var pitch by remember { mutableFloatStateOf(if (initialParams.pitch == 0f) 1f else initialParams.pitch) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.plugin_audio_params)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 语速
                FloatSlider(
                    label = "语速",
                    value = speed,
                    onValueChange = { speed = it },
                    valueRange = 0f..3f,
                    steps = 29,
                    valueFormatter = { "%.1f".format(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 音量
                FloatSlider(
                    label = "音量",
                    value = volume,
                    onValueChange = { volume = it },
                    valueRange = 0f..3f,
                    steps = 29,
                    valueFormatter = { "%.1f".format(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 音调
                FloatSlider(
                    label = "音高",
                    value = pitch,
                    onValueChange = { pitch = it },
                    valueRange = 0f..3f,
                    steps = 29,
                    valueFormatter = { "%.1f".format(it) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(AudioParams(speed = speed, volume = volume, pitch = pitch))
            }) {
                Text(stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}
