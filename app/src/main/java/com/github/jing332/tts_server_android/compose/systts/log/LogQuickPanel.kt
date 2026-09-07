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
import com.github.jing332.compose.widgets.LabelSlider
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts.TaggedTtsPreviewPlayer
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.app
import com.github.jing332.tts_server_android.compose.systts.list.ui.PluginDescriptor
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import kotlinx.coroutines.launch

/**
 * 日志快捷面板「发音人调整」：点带 configId 的"请求音频"主行弹出。
 * 结构对齐配置项音频参数弹窗（用户 09-07：面板 = 弹窗 + 顶部换发音人）：
 * - 发音人下拉（引擎 getVoices 缓存）+ ▶ 试听，未保存候选也先听（统一试听链）；
 * - 终值行：播放链同源三层乘积，值为 1.0 的维度不显示；
 * - 三块调节与弹窗同款且均为**无音高版**（用户定稿）：
 *   配置项音频参数（仅本条，应用含发音人）/ 插件音频参数 / 全局音频参数，
 *   各自带重置/应用，应用即落库生效不关面板。
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

    // ===== 本地编辑草稿：各块应用才落库 =====
    var displayName by remember(entity.id) { mutableStateOf(entity.displayName) }
    var voice by remember(entity.id) { mutableStateOf(source?.voice ?: "") }
    var speed by remember(entity.id) { mutableStateOf(config.audioParams.speed) }
    var volume by remember(entity.id) { mutableStateOf(config.audioParams.volume) }

    // 发音人候选：插件配置走引擎 getVoices（引擎缓存，秒回）；本地引擎不提供下拉
    var voices by remember(entity.id) { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    val vm = remember {
        com.github.jing332.tts_server_android.compose.systts.list.ui.PluginTtsViewModel(app)
    }

    // 插件元数据（轻量）：插件层草稿初值 + 终值路由判断用
    var plugin by remember(entity.id) {
        mutableStateOf(source?.let { dbm.pluginDao.getMetaByPluginId(it.pluginId) })
    }
    var pluginSpeed by remember(entity.id) { mutableStateOf(1f) }
    var pluginVolume by remember(entity.id) { mutableStateOf(1f) }
    LaunchedEffectOnce(entity.id) {
        source?.let { s ->
            runCatching {
                val loaded = dbm.pluginDao.getByPluginId(s.pluginId) ?: return@runCatching
                plugin = loaded
                withIO {
                    vm.load(context, loaded, s, android.widget.LinearLayout(context).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                    })
                }
                com.drake.net.utils.withMain {
                    voices = vm.voices.map { it.id.toString() to it.name }
                    pluginSpeed = snapParam(loaded.audioParams.speed)
                    pluginVolume = snapParam(loaded.audioParams.volume)
                }
            }
        }
    }

    var globalSpeed by remember { mutableStateOf(com.github.jing332.tts_server_android.conf.SysTtsConfig.audioParamsSpeed) }
    var globalVolume by remember { mutableStateOf(com.github.jing332.tts_server_android.conf.SysTtsConfig.audioParamsVolume) }

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

    /** 配置层应用：发音人 + 语速 + 音量 落库本条，即时生效不关面板 */
    fun applyConfigLayer() {
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
            Toast.makeText(
                context,
                context.getString(R.string.audio_params_apply_config_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        // 总标题（用户 09-07 定稿）：发音人调整
        title = { Text(stringResource(R.string.log_panel_title)) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                // ===== 换发音人（最上方，无标题字）=====
                if (source != null && voices.isNotEmpty()) {
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

                // ===== 终值（播放链同源三层乘积；值为 1.0 的维度不显示）=====
                val handlesSpeed = plugin?.pluginHandlesSpeed == true
                val handlesVolume = plugin?.pluginHandlesVolume == true
                val handlesPitch = plugin?.pluginHandlesPitch == true
                val finalSpeed = if (handlesSpeed) speed else speed * pluginSpeed * globalSpeed
                val finalVolume = if (handlesVolume) volume else volume * pluginVolume * globalVolume
                // 音高为相乘最终值：配置层 × 插件层（面板无音高滑杆，全局层不参与音高）
                val finalPitch = if (handlesPitch) config.audioParams.pitch
                else config.audioParams.pitch * (pluginParams?.pitch ?: 1f)
                val finalDims = buildList {
                    if (kotlin.math.abs(finalSpeed - 1f) > 0.005f) add("语速%.2fx".format(finalSpeed))
                    if (kotlin.math.abs(finalVolume - 1f) > 0.005f) add("音量%.2fx".format(finalVolume))
                    if (kotlin.math.abs(finalPitch - 1f) > 0.005f) add("音高%.2fx".format(finalPitch))
                }
                Text(
                    text = if (finalDims.isEmpty()) stringResource(R.string.audio_params_none)
                    else "最终：" + finalDims.joinToString("，"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
                )

                // ===== 配置项音频参数（仅本条）=====
                Text(
                    stringResource(R.string.audio_params_config_layer),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                LabelSlider(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.label_speech_rate, "%.2f".format(speed)),
                    value = speed,
                    onValueChange = { speed = it },
                    valueRange = 0.1f..3f,
                    step = 0.05f,
                )
                LabelSlider(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.label_speech_volume, "%.2f".format(volume)),
                    value = volume,
                    onValueChange = { volume = it },
                    valueRange = 0.1f..3f,
                    step = 0.05f,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { speed = 1f; volume = 1f }) {
                        Text(stringResource(R.string.reset))
                    }
                    TextButton(onClick = { applyConfigLayer() }) {
                        Text(stringResource(R.string.audio_params_apply))
                    }
                }

                // ===== 插件音频参数（影响该插件全部配置项）=====
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                if (source != null) {
                    Text(
                        stringResource(R.string.audio_params_plugin_layer),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    LabelSlider(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.label_speech_rate, "%.2f".format(pluginSpeed)),
                        value = pluginSpeed,
                        onValueChange = { pluginSpeed = it },
                        valueRange = 0.1f..3f,
                        step = 0.05f,
                    )
                    LabelSlider(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.label_speech_volume, "%.2f".format(pluginVolume)),
                        value = pluginVolume,
                        onValueChange = { pluginVolume = it },
                        valueRange = 0.1f..3f,
                        step = 0.05f,
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { pluginSpeed = 1f; pluginVolume = 1f }) {
                            Text(stringResource(R.string.reset))
                        }
                        TextButton(onClick = {
                            val p = plugin ?: return@TextButton
                            scope.launch {
                                withIO {
                                    dbm.pluginDao.update(
                                        p.copy(
                                            audioParams = p.audioParams.copy(
                                                speed = snapParam(pluginSpeed),
                                                volume = snapParam(pluginVolume),
                                            )
                                        )
                                    )
                                    // 卡片"插件语速/音量"显示缓存失效
                                    PluginDescriptor.invalidatePluginParamsCache(p.pluginId)
                                    SystemTtsService.notifyUpdateConfig()
                                }
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.audio_params_apply_plugin_toast),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }) {
                            Text(stringResource(R.string.audio_params_apply))
                        }
                    }
                }

                // ===== 全局音频参数（影响全部配置项·谨慎）=====
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                Text(
                    stringResource(R.string.audio_params_global_layer),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                LabelSlider(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.label_speech_rate, "%.2f".format(globalSpeed)),
                    value = globalSpeed,
                    onValueChange = { globalSpeed = it },
                    valueRange = 0.1f..3f,
                    step = 0.05f,
                )
                LabelSlider(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.label_speech_volume, "%.2f".format(globalVolume)),
                    value = globalVolume,
                    onValueChange = { globalVolume = it },
                    valueRange = 0.1f..3f,
                    step = 0.05f,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { globalSpeed = 1f; globalVolume = 1f }) {
                        Text(stringResource(R.string.reset))
                    }
                    TextButton(onClick = {
                        scope.launch {
                            com.github.jing332.tts_server_android.conf.SysTtsConfig.audioParamsSpeed =
                                snapParam(globalSpeed)
                            com.github.jing332.tts_server_android.conf.SysTtsConfig.audioParamsVolume =
                                snapParam(globalVolume)
                            SystemTtsService.notifyUpdateConfig()
                            Toast.makeText(
                                context,
                                context.getString(R.string.audio_params_apply_global_toast),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }) {
                        Text(stringResource(R.string.audio_params_apply))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** 单次 LaunchedEffect 简写：key 变化时只执行一次 */
@Composable
private fun LaunchedEffectOnce(key: Any?, block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {
    androidx.compose.runtime.LaunchedEffect(key) { block() }
}

private fun snapParam(v: Float): Float = (kotlin.math.round(v * 100f) / 100f)
