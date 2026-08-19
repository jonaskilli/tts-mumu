package com.github.jing332.tts_server_android.compose.systts.plugin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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
    initialHandlesSpeed: Boolean = false,
    initialHandlesVolume: Boolean = false,
    initialHandlesPitch: Boolean = false,
    onDismissRequest: () -> Unit,
    onConfirm: (params: AudioParams, handlesSpeed: Boolean, handlesVolume: Boolean, handlesPitch: Boolean) -> Unit
) {
    // 0 表示跟随，显示时转为 1.0，保存时再转回
    var speed by remember { mutableFloatStateOf(if (initialParams.speed == 0f) 1f else initialParams.speed) }
    var volume by remember { mutableFloatStateOf(if (initialParams.volume == 0f) 1f else initialParams.volume) }
    var pitch by remember { mutableFloatStateOf(if (initialParams.pitch == 0f) 1f else initialParams.pitch) }

    var handlesSpeed by remember { mutableStateOf(initialHandlesSpeed) }
    var handlesVolume by remember { mutableStateOf(initialHandlesVolume) }
    var handlesPitch by remember { mutableStateOf(initialHandlesPitch) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.plugin_audio_params)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                // 语速
                FloatSlider(
                    label = "语速",
                    value = speed,
                    onValueChange = { speed = it },
                    valueRange = 0.1f..3f,
                    step = 0.05f,
                    valueFormatter = { "%.2f".format(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 音量
                FloatSlider(
                    label = "音量",
                    value = volume,
                    onValueChange = { volume = it },
                    valueRange = 0.1f..3f,
                    step = 0.05f,
                    valueFormatter = { "%.2f".format(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 音调
                FloatSlider(
                    label = "音高",
                    value = pitch,
                    onValueChange = { pitch = it },
                    valueRange = 0.1f..3f,
                    step = 0.05f,
                    valueFormatter = { "%.2f".format(it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    stringResource(id = R.string.plugin_handles_params_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    stringResource(id = R.string.plugin_handles_params_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                HandlesParamRow(
                    label = stringResource(id = R.string.plugin_handles_speed),
                    checked = handlesSpeed,
                    onCheckedChange = { handlesSpeed = it }
                )
                HandlesParamRow(
                    label = stringResource(id = R.string.plugin_handles_volume),
                    checked = handlesVolume,
                    onCheckedChange = { handlesVolume = it }
                )
                HandlesParamRow(
                    label = stringResource(id = R.string.plugin_handles_pitch),
                    checked = handlesPitch,
                    onCheckedChange = { handlesPitch = it }
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = {
                    speed = 1f
                    volume = 1f
                    pitch = 1f
                }) {
                    Text(stringResource(id = R.string.reset))
                }
                TextButton(onClick = {
                    onConfirm(
                        AudioParams(speed = speed, volume = volume, pitch = pitch),
                        handlesSpeed, handlesVolume, handlesPitch
                    )
                }) {
                    Text(stringResource(id = R.string.confirm))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
private fun HandlesParamRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
