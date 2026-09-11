package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.conf.SysTtsConfig
import com.github.jing332.tts_server_android.service.systts.SystemTtsService

@Composable
fun GlobalAudioParamsDialog(onDismissRequest: () -> Unit) {
    // 草稿制（用户 09-11 拍板，与插件音频参数弹窗统一）：拖动只改本地草稿，
    // 点「应用」才写库并通知服务，「取消」可丢弃改动。
    // 原实现滑条直接绑定 DataSaver 状态——拖动即存盘、无法反悔，且服务要等弹窗关闭才收到通知
    var speed by remember { mutableFloatStateOf(SysTtsConfig.audioParamsSpeed) }
    var volume by remember { mutableFloatStateOf(SysTtsConfig.audioParamsVolume) }
    var pitch by remember { mutableFloatStateOf(SysTtsConfig.audioParamsPitch) }
    BasicAudioParamsDialog(
        title = { Text(stringResource(id = R.string.audio_params_settings)) },
        onDismissRequest = onDismissRequest,

        // 重置目标=1.0（跟随），全 1.0 时重置键置灰
        resetValue = 1f,
        speedRange = 0.1f..3f,
        speed = speed,
        onSpeedChange = { speed = it },

        volumeRange = 0.1f..3f,
        volume = volume,
        onVolumeChange = { volume = it },

        pitchRange = 0.1f..3f,
        pitch = pitch,
        onPitchChange = { pitch = it },

        onReset = {
            speed = 1f
            volume = 1f
            pitch = 1f
        },
        buttons = {
            // 按钮排布与插件弹窗统一：取消（左）｜ 重置 · 应用（右）
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                    speed = 1f
                    volume = 1f
                    pitch = 1f
                }) {
                    Text(stringResource(R.string.reset))
                }
                TextButton(onClick = {
                    SysTtsConfig.audioParamsSpeed = speed
                    SysTtsConfig.audioParamsVolume = volume
                    SysTtsConfig.audioParamsPitch = pitch
                    // 与插件弹窗同口径：点应用即通知服务生效（不再等弹窗关闭）
                    SystemTtsService.notifyUpdateConfig()
                    onDismissRequest()
                }) {
                    Text(stringResource(R.string.audio_params_apply))
                }
            }
        }
    )
}
