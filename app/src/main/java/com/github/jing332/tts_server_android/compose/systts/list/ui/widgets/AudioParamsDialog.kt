package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.drake.net.utils.withIO
import com.github.jing332.compose.widgets.LabelSlider
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.app
import com.github.jing332.tts_server_android.conf.SysTtsConfig
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import kotlinx.coroutines.launch

/**
 * 配置项「音频参数」弹窗：总名"音频参数"，下分三块。
 *
 * - 终值行置顶（用户定稿）：播放链同源 resolveTtsPlayback，实时反映三处草稿；
 * - 配置项音频参数（仅本条）：滑杆 + 重置 + [应用]——写库同时回写页面内存
 *   （防"应用后再拖动→右上角保存→旧内存覆盖"），立即生效不随页面取消回退；
 * - 插件音频参数（该插件全部配置项）：折叠，拖动+[应用]（语义同编辑页折叠区）；
 * - 全局音频参数（全部配置项·谨慎）：折叠，同上；音高不出现于这两层（用户定稿）。
 *
 * [onSysttsChange] 由调用方传编辑页内存回调，保证双写一致。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AudioParamsDialog(
    onDismissRequest: () -> Unit,
    systemTts: SystemTtsV2,
    onSysttsChange: (SystemTtsV2) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val config = systemTts.config as? TtsConfigurationDTO ?: return
    val source = config.source as? PluginTtsSource
    val plugin = source?.let { dbm.pluginDao.getByPluginId(it.pluginId) }

    // 配置层草稿
    var speed by remember(systemTts.id) { mutableStateOf(config.audioParams.speed) }
    var volume by remember(systemTts.id) { mutableStateOf(config.audioParams.volume) }
    var pitch by remember(systemTts.id) { mutableStateOf(config.audioParams.pitch) }

    // 远端层草稿（用户 09-07 定稿：默认展开且常驻，无收起键）
    var pluginSpeed by remember { mutableStateOf(plugin?.audioParams?.speed ?: 1f) }
    var pluginVolume by remember { mutableStateOf(plugin?.audioParams?.volume ?: 1f) }
    var globalSpeed by remember { mutableStateOf(SysTtsConfig.audioParamsSpeed) }
    var globalVolume by remember { mutableStateOf(SysTtsConfig.audioParamsVolume) }

    // 未保存标记：滑杆改动后置 true，「应用」成功清除（● 提示哪块有待保存）
    var configDirty by remember(systemTts.id) { mutableStateOf(false) }
    var pluginDirty by remember(systemTts.id) { mutableStateOf(false) }
    var globalDirty by remember(systemTts.id) { mutableStateOf(false) }

    // 底部弹窗（用户 09-08 定稿）：三层内容长，sheet 比 AppDialog 合适（与发音人调整面板同形态）
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                // verticalScroll：插件/全局层展开后内容超屏可上下滑动查看（用户 09-07 反馈）
                .verticalScroll(rememberScrollState())
        ) {
            // 总标题
            Text(
                stringResource(R.string.audio_params),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
                // ===== 终值置顶（用户定稿）：配置层草稿 × 插件层现值 × 全局层现值 =====
                // 值为 1.0 的维度不显示；三维全默认显示「语速、音量、音高无设置」（09-07 与日志面板同步格式）
                val finalParams = computeFinalParams(
                    config, source, plugin,
                    snap(speed), snap(volume), snap(pitch),
                    snap(pluginSpeed), snap(pluginVolume),
                    snap(globalSpeed), snap(globalVolume),
                )
                val finalDims = buildList {
                    if (kotlin.math.abs(finalParams.speed - 1f) > 0.005f)
                        add("语速%.2fx".format(finalParams.speed))
                    if (kotlin.math.abs(finalParams.volume - 1f) > 0.005f)
                        add("音量%.2fx".format(finalParams.volume))
                    if (kotlin.math.abs(finalParams.pitch - 1f) > 0.005f)
                        add("音高%.2fx".format(finalParams.pitch))
                }
                Text(
                    text = if (finalDims.isEmpty()) stringResource(R.string.audio_params_none)
                    else "最终：" + finalDims.joinToString("，"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )

                // ===== 配置项音频参数（仅本条）=====
                SectionTitle(stringResource(R.string.audio_params_config_layer))
                LabelSlider(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.label_speech_rate, "%.2f".format(speed)),
                    value = speed,
                    onValueChange = { speed = snap(it); configDirty = true },
                    valueRange = 0.1f..3f,
                    step = 0.05f,
                )
                LabelSlider(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.label_speech_volume, "%.2f".format(volume)),
                    value = volume,
                    onValueChange = { volume = snap(it); configDirty = true },
                    valueRange = 0.1f..3f,
                    step = 0.05f,
                )
                LabelSlider(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.label_speech_pitch, "%.2f".format(pitch)),
                    value = pitch,
                    onValueChange = { pitch = snap(it); configDirty = true },
                    valueRange = 0.1f..3f,
                    step = 0.05f,
                )
                // 重置/应用同一行（用户 09-07 反馈：分行太散），与插件/全局层 Row2Buttons 同款
                Row2Buttons(
                    applyText = (if (configDirty) "● " else "") + stringResource(R.string.audio_params_apply),
                    onReset = { speed = 1f; volume = 1f; pitch = 1f },
                    onApply = {
                        // 双写：落库 + 回写页面内存，防"应用后再保存"被旧内存覆盖
                        scope.launch {
                            withIO {
                                val newConfig = config.copy(
                                    audioParams = config.audioParams.copy(
                                        speed = snap(speed), volume = snap(volume), pitch = snap(pitch)
                                    )
                                )
                                dbm.systemTtsV2.update(systemTts.copy(config = newConfig))
                                SystemTtsService.notifyUpdateConfig()
                            }
                            onSysttsChange(
                                systemTts.copy(
                                    config = config.copy(
                                        audioParams = config.audioParams.copy(
                                            speed = snap(speed), volume = snap(volume), pitch = snap(pitch)
                                        )
                                    )
                                )
                            )
                            Toast.makeText(
                                context,
                                context.getString(R.string.audio_params_apply_config_toast),
                                Toast.LENGTH_SHORT,
                            ).show()
                            configDirty = false
                        }
                    },
                )

                HorizontalDivider(Modifier.padding(vertical = 4.dp))

                // ===== 插件音频参数（常驻展开，无收起键）=====
                if (source != null) {
                    SectionTitle(stringResource(R.string.audio_params_plugin_layer))
                        LabelSlider(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.label_speech_rate, "%.2f".format(pluginSpeed)),
                            value = pluginSpeed,
                            onValueChange = { pluginSpeed = snap(it); pluginDirty = true },
                            valueRange = 0.1f..3f,
                            step = 0.05f,
                        )
                        LabelSlider(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.label_speech_volume, "%.2f".format(pluginVolume)),
                            value = pluginVolume,
                            onValueChange = { pluginVolume = snap(it); pluginDirty = true },
                            valueRange = 0.1f..3f,
                            step = 0.05f,
                        )
                        Row2Buttons(
                            applyText = (if (pluginDirty) "● " else "") + stringResource(R.string.audio_params_apply),
                            onReset = { pluginSpeed = 1f; pluginVolume = 1f },
                            onApply = {
                                val p = plugin ?: return@Row2Buttons
                                scope.launch {
                                    withIO {
                                        dbm.pluginDao.update(
                                            p.copy(
                                                audioParams = p.audioParams.copy(
                                                    speed = snap(pluginSpeed), volume = snap(pluginVolume)
                                                )
                                            )
                                        )
                                        // 卡片"插件语速/音量"显示缓存失效，应用后重查
                                        com.github.jing332.tts_server_android.compose.systts.list.ui.PluginDescriptor
                                            .invalidatePluginParamsCache(p.pluginId)
                                        SystemTtsService.notifyUpdateConfig()
                                        pluginDirty = false
                                    }
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.audio_params_apply_plugin_toast),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            },
                        )
                }

                HorizontalDivider(Modifier.padding(vertical = 4.dp))

                // ===== 全局音频参数（常驻展开，无收起键）=====
                SectionTitle(stringResource(R.string.audio_params_global_layer))
                LabelSlider(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.label_speech_rate, "%.2f".format(globalSpeed)),
                    value = globalSpeed,
                    onValueChange = { globalSpeed = snap(it); globalDirty = true },
                    valueRange = 0.1f..3f,
                    step = 0.05f,
                )
                LabelSlider(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.label_speech_volume, "%.2f".format(globalVolume)),
                    value = globalVolume,
                    onValueChange = { globalVolume = snap(it); globalDirty = true },
                    valueRange = 0.1f..3f,
                    step = 0.05f,
                )
                Row2Buttons(
                    applyText = (if (globalDirty) "● " else "") + stringResource(R.string.audio_params_apply),
                    onReset = { globalSpeed = 1f; globalVolume = 1f },
                    onApply = {
                        scope.launch {
                            SysTtsConfig.audioParamsSpeed = snap(globalSpeed)
                            SysTtsConfig.audioParamsVolume = snap(globalVolume)
                            SystemTtsService.notifyUpdateConfig()
                            globalDirty = false
                            Toast.makeText(
                                context,
                                context.getString(R.string.audio_params_apply_global_toast),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                )

                // 底部取消（sheet 无 buttons 槽，自行放行尾）
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    )
}

/** 三层乘积（尊重 pluginHandles 路由：由插件处理的维度，插件/全局层不参与叠加）。
 *  三维最终值恒为 配置×插件×全局（弹窗有无某滑杆不影响计算） */
private fun computeFinalParams(
    config: TtsConfigurationDTO,
    source: PluginTtsSource?,
    plugin: com.github.jing332.database.entities.plugin.Plugin?,
    cfgSpeed: Float, cfgVolume: Float, cfgPitch: Float,
    pluginSpeed: Float, pluginVolume: Float,
    globalSpeed: Float, globalVolume: Float,
): AudioParams {
    val isPlugin = source != null
    val handlesSpeed = isPlugin && plugin?.pluginHandlesSpeed == true
    val handlesVolume = isPlugin && plugin?.pluginHandlesVolume == true
    val handlesPitch = isPlugin && plugin?.pluginHandlesPitch == true
    val pSpeed = if (isPlugin) pluginSpeed else 1f
    val pVolume = if (isPlugin) pluginVolume else 1f
    val pPitch = if (isPlugin) plugin?.audioParams?.pitch ?: 1f else 1f
    val globalPitch = com.github.jing332.tts_server_android.conf.SysTtsConfig.audioParamsPitch
    return AudioParams(
        speed = if (handlesSpeed) cfgSpeed else cfgSpeed * pSpeed * globalSpeed,
        volume = if (handlesVolume) cfgVolume else cfgVolume * pVolume * globalVolume,
        pitch = if (handlesPitch) cfgPitch else cfgPitch * pPitch * globalPitch,
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun Row2Buttons(onReset: () -> Unit, onApply: () -> Unit, applyText: String = stringResource(R.string.audio_params_apply)) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
    ) {
        TextButton(onClick = onReset) { Text(stringResource(R.string.reset)) }
        TextButton(onClick = onApply) { Text(applyText) }
    }
}
// snap() 复用同包 RemoteAudioParamsSection.kt 的顶层定义（勿在本文件重复定义，同包重名会重载歧义）
