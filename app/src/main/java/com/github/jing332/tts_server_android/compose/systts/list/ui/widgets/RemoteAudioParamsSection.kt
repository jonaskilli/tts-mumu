package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jing332.compose.widgets.LabelSlider
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.conf.SysTtsConfig
import com.github.jing332.tts_server_android.service.systts.SystemTtsService

/**
 * 三层音频参数中的「远端层」滑块组（插件层/全局层）。
 *
 * 与配置层滑块的区别：写入影响多条配置项，因此
 * - 默认折叠，点开才见滑块；
 * - 拖动不落库，点「应用」才写入并通知服务刷新；
 * - 带作用域警示文案。
 */
@Composable
fun RemoteAudioParamsSection(
    modifier: Modifier = Modifier,
    layer: Layer,
    pluginId: String? = null,
    onApplied: (() -> Unit)? = null,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = when (layer) {
                    Layer.PLUGIN -> stringResource(R.string.audio_params_layer_plugin)
                    Layer.GLOBAL -> stringResource(R.string.audio_params_layer_global)
                },
                style = MaterialTheme.typography.titleSmall,
            )
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) stringResource(R.string.collapse) else stringResource(R.string.expand))
            }
        }

        if (expanded) {
            Text(
                text = when (layer) {
                    Layer.PLUGIN -> stringResource(R.string.audio_params_layer_plugin_hint)
                    Layer.GLOBAL -> stringResource(R.string.audio_params_layer_global_hint)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (layer) {
                Layer.PLUGIN -> PluginLayerSliders(pluginId, onApplied)
                Layer.GLOBAL -> GlobalLayerSliders(onApplied)
            }
        }
    }
}

enum class Layer { PLUGIN, GLOBAL }

@Composable
private fun PluginLayerSliders(pluginId: String?, onApplied: (() -> Unit)?) {
    val plugin = pluginId?.let { dbm.pluginDao.getByPluginId(it) }
    if (plugin == null) {
        Text(
            stringResource(R.string.audio_params_plugin_not_found),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    // 就地编辑草稿：拖动不落库，点应用才写
    var speed by remember(plugin.id) { mutableStateOf(plugin.audioParams.speed) }
    var volume by remember(plugin.id) { mutableStateOf(plugin.audioParams.volume) }
    val scopeHint = stringResource(R.string.audio_params_apply_plugin_toast)

    Column(Modifier.fillMaxWidth()) {
        LabelSlider(
            text = stringResource(R.string.label_speech_rate, "%.2f".format(speed)),
            value = speed,
            onValueChange = { speed = snap(it) },
            valueRange = 0.1f..3f,
            step = 0.05f,
        )
        LabelSlider(
            text = stringResource(R.string.label_speech_volume, "%.2f".format(volume)),
            value = volume,
            onValueChange = { volume = snap(it) },
            valueRange = 0.1f..3f,
            step = 0.05f,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { speed = 1f; volume = 1f }) {
                Text(stringResource(R.string.reset))
            }
            TextButton(onClick = {
                val updated = plugin.copy(
                    audioParams = plugin.audioParams.copy(speed = snap(speed), volume = snap(volume))
                )
                dbm.pluginDao.update(updated)
                // 卡片"插件语速/音量"显示走 PluginDescriptor.pluginParamsCache，应用后失效重查
                com.github.jing332.tts_server_android.compose.systts.list.ui.PluginDescriptor
                    .invalidatePluginParamsCache(plugin.pluginId)
                SystemTtsService.notifyUpdateConfig()
                android.widget.Toast.makeText(
                    com.github.jing332.tts_server_android.app,
                    scopeHint,
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
                onApplied?.invoke()
            }) {
                Text(stringResource(R.string.audio_params_apply))
            }
        }
    }
}

@Composable
private fun GlobalLayerSliders(onApplied: (() -> Unit)?) {
    var speed by remember { mutableStateOf(SysTtsConfig.audioParamsSpeed) }
    var volume by remember { mutableStateOf(SysTtsConfig.audioParamsVolume) }
    val scopeHint = stringResource(R.string.audio_params_apply_global_toast)

    Column(Modifier.fillMaxWidth()) {
        LabelSlider(
            text = stringResource(R.string.label_speech_rate, "%.2f".format(speed)),
            value = speed,
            onValueChange = { speed = snap(it) },
            valueRange = 0.1f..3f,
            step = 0.05f,
        )
        LabelSlider(
            text = stringResource(R.string.label_speech_volume, "%.2f".format(volume)),
            value = volume,
            onValueChange = { volume = snap(it) },
            valueRange = 0.1f..3f,
            step = 0.05f,
        )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { speed = 1f; volume = 1f }) {
                    Text(stringResource(R.string.reset))
                }
                TextButton(onClick = {
                    SysTtsConfig.audioParamsSpeed = snap(speed)
                    SysTtsConfig.audioParamsVolume = snap(volume)
                    SystemTtsService.notifyUpdateConfig()
                    android.widget.Toast.makeText(
                        com.github.jing332.tts_server_android.app,
                        scopeHint,
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                    onApplied?.invoke()
                }) {
                    Text(stringResource(R.string.audio_params_apply))
                }
            }
        }
    }

/** 与配置层滑块一致的去噪（0.01 步进对齐，避免 JSON 噪声） */
internal fun snap(v: Float): Float = (kotlin.math.round(v * 100f) / 100f)
