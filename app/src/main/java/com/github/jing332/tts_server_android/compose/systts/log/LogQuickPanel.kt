package com.github.jing332.tts_server_android.compose.systts.log

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.drake.net.utils.withIO
import com.github.jing332.common.LogEntry
import com.github.jing332.compose.widgets.AppSpinner
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts.TaggedTtsPreviewPlayer
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.app
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.RemoteAudioParamsSection
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import kotlinx.coroutines.launch

/**
 * 日志快捷面板：点带 configId 的"请求音频"主行弹出。
 * - 发音人下拉（来自该配置所属插件的 getVoices 引擎缓存），每项带▶试听；
 *   未保存的候选也先听（临时实体走 TaggedTtsPreviewPlayer，同一条统一试听链）
 * - 配置层语速/音量滑杆（只影响本条）；
 * - 插件层/全局层折叠区可直接调（拖动+应用确认，语义同编辑页）；音高不出现（用户定稿）；
 * - 底部终值行走 resolveTtsPlayback 与播放链同源；
 * - 全部改动点「应用」一次性落库并通知服务刷新，下一条请求日志即反映新值。
 */
@Composable
fun LogQuickPanel(
    onDismissRequest: () -> Unit,
    entry: LogEntry,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val entity = remember(entry.configId) { dbm.systemTtsV2.get(entry.configId) }
    if (entity == null) {
        // 配置项已被删除：提示后由外层关闭
        Toast.makeText(context, context.getString(R.string.log_panel_config_missing), Toast.LENGTH_SHORT).show()
        onDismissRequest()
        return
    }
    val config = entity.config as? TtsConfigurationDTO ?: run {
        Toast.makeText(context, context.getString(R.string.log_panel_config_missing), Toast.LENGTH_SHORT).show()
        onDismissRequest()
        return
    }
    val source = config.source as? PluginTtsSource

    // ===== 本地编辑草稿：应用才落库 =====
    var displayName by remember(entity.id) { mutableStateOf(entity.displayName) }
    var voice by remember(entity.id) { mutableStateOf(source?.voice ?: "") }
    var speed by remember(entity.id) { mutableStateOf(config.audioParams.speed) }
    var volume by remember(entity.id) { mutableStateOf(config.audioParams.volume) }
    var appliedTick by remember(entity.id) { mutableStateOf(0) }

    // 发音人候选：插件配置走引擎 getVoices（引擎缓存，秒回）；本地引擎不提供下拉
    var voices by remember(entity.id) { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    val vm = remember {
        com.github.jing332.tts_server_android.compose.systts.list.ui.PluginTtsViewModel(app)
    }
    if (source != null) {
        androidx.compose.runtime.LaunchedEffect(entity.id) {
            runCatching {
                val plugin = dbm.pluginDao.getByPluginId(source.pluginId) ?: return@runCatching
                withIO {
                    vm.load(context, plugin, source, android.widget.LinearLayout(context).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                    })
                }
                com.drake.net.utils.withMain { voices = vm.voices.map { it.id.toString() to it.name } }
            }
        }
    }

    /** 用当前草稿构造临时实体试听：未保存候选也先听，走统一试听链 */
    fun draftEntity(candidateVoice: String? = null): SystemTtsV2 {
        val v = candidateVoice ?: voice
        val renamed = if (v.isNotBlank() && v != (source?.voice ?: "")) v else displayName
        return entity.copy(
            displayName = renamed,
            config = config.copy(
                audioParams = config.audioParams.copy(speed = speed, volume = volume),
                source = source?.copy(voice = v) ?: config.source,
            ),
        )
    }

    fun apply() {
        scope.launch {
            withIO {
                val sourceNow = (entity.config as? TtsConfigurationDTO)?.source as? PluginTtsSource
                val newConfig = config.copy(
                    audioParams = config.audioParams.copy(
                        speed = snapParam(speed),
                        volume = snapParam(volume),
                    ),
                    source = if (sourceNow != null) sourceNow.copy(voice = voice) else config.source,
                )
                dbm.systemTtsV2.update(entity.copy(displayName = displayName, config = newConfig))
                SystemTtsService.notifyUpdateConfig()
            }
            onDismissRequest()
        }
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        // 总名与编辑页弹窗一致(用户定稿)：音频参数，下分配置项/插件/全局三区
        title = { Text(stringResource(R.string.audio_params)) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                // ===== 发音人 =====
                if (source != null && voices.isNotEmpty()) {
                    Text(
                        stringResource(R.string.log_panel_voice_label),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppSpinner(
                            modifier = Modifier.weight(1f),
                            value = voice,
                            values = voices.map { it.first },
                            entries = voices.map { it.second },
                            onSelectedChange = { key, _ -> voice = key as String },
                        )
                        TextButton(onClick = {
                            TaggedTtsPreviewPlayer.play(context, draftEntity(), "你好，这是试听语音。")
                        }) {
                            Text("▶")
                        }
                    }
                }

                // ===== 配置层滑杆（仅本条）=====
                Text(
                    stringResource(R.string.log_panel_config_layer),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                SliderRow(
                    label = stringResource(R.string.label_speech_rate, "%.2f".format(speed)),
                    value = speed,
                    onChange = { speed = it },
                )
                SliderRow(
                    label = stringResource(R.string.label_speech_volume, "%.2f".format(volume)),
                    value = volume,
                    onChange = { volume = it },
                )

                // ===== 插件层/全局层（折叠直接调，无音高）=====
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                RemoteAudioParamsSection(
                    layer = com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.Layer.PLUGIN,
                    pluginId = source?.pluginId,
                    onApplied = { appliedTick++ },
                )
                RemoteAudioParamsSection(
                    layer = com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.Layer.GLOBAL,
                    onApplied = { appliedTick++ },
                )

                // ===== 终值（与播放链同源）=====
                val resolved = remember(entity.id, speed, volume, appliedTick, voice) {
                    runCatching {
                        com.github.jing332.tts.resolveTtsPlayback(
                            draftEntity(),
                            AudioParams(
                                speed = com.github.jing332.tts_server_android.conf.SysTtsConfig.audioParamsSpeed,
                                volume = com.github.jing332.tts_server_android.conf.SysTtsConfig.audioParamsVolume,
                                pitch = com.github.jing332.tts_server_android.conf.SysTtsConfig.audioParamsPitch,
                            ),
                        )
                    }.getOrNull()
                }
                val p = resolved?.configuration?.audioParams
                if (p != null) {
                    Text(
                        text = "最终：语速%.2fx 音量%.2fx".format(p.speed, p.volume),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { apply() }) { Text(stringResource(R.string.audio_params_apply)) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun SliderRow(label: String, value: Float, onChange: (Float) -> Unit) {
    com.github.jing332.compose.widgets.LabelSlider(
        modifier = Modifier.fillMaxWidth(),
        text = label,
        value = value,
        onValueChange = { onChange(snapParam(it)) },
        valueRange = 0.1f..3f,
        step = 0.05f,
    )
}

private fun snapParam(v: Float): Float = (kotlin.math.round(v * 100f) / 100f)
