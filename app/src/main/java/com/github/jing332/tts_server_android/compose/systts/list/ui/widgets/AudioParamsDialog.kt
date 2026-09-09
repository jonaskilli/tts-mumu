package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.drake.net.utils.withIO
import com.github.jing332.common.utils.toParamText
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts.PreviewState
import com.github.jing332.tts.TaggedTtsPreviewPlayer
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.conf.SysTtsConfig
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import kotlinx.coroutines.launch

/**
 * 配置项「音频参数」弹窗（卡片菜单 / 编辑页顶部按钮共用）。
 * 结构（用户 09-10 定稿，按维度改版）：
 * - 顶部：当前发音人 + ▶试听（草稿试听，应用才落库）+ 终值行（播放链同源三层乘积，实时跟随草稿）；
 * - 主体：[AudioParamsDimensionSection] 第二级分段=语速/音量/音高，每段内三层滑杆同屏；
 *   重置/应用按维度一组，应用=该维三层一起落库（配置层双写页面内存防旧值覆盖）；
 * - 插件接管的维度只落配置层（09-10 ③）；
 * - 音高进插件/全局层（09-10 翻掉 09-07「音高不出现于插件/全局层」旧决定）。
 *
 * [onSysttsChange] 由调用方传编辑页内存回调，保证双写一致。
 */
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

    // 三层草稿（音高三层齐全，09-10）
    var speed by remember(systemTts.id) { mutableStateOf(config.audioParams.speed) }
    var volume by remember(systemTts.id) { mutableStateOf(config.audioParams.volume) }
    var pitch by remember(systemTts.id) { mutableStateOf(config.audioParams.pitch) }
    var pluginSpeed by remember { mutableStateOf(plugin?.audioParams?.speed ?: 1f) }
    var pluginVolume by remember { mutableStateOf(plugin?.audioParams?.volume ?: 1f) }
    var pluginPitch by remember { mutableStateOf(plugin?.audioParams?.pitch ?: 1f) }
    var globalSpeed by remember { mutableStateOf(SysTtsConfig.audioParamsSpeed) }
    var globalVolume by remember { mutableStateOf(SysTtsConfig.audioParamsVolume) }
    var globalPitch by remember { mutableStateOf(SysTtsConfig.audioParamsPitch) }

    // 按维度脏标记（09-10 ②A）：该维任一层滑杆改动置 true，应用成功清除
    var speedDirty by remember(systemTts.id) { mutableStateOf(false) }
    var volumeDirty by remember(systemTts.id) { mutableStateOf(false) }
    var pitchDirty by remember(systemTts.id) { mutableStateOf(false) }

    // ===== 顶部试听状态机（同日志快捷面板：▶ →(点击)… →(出声)■ →(播完复位)▶）=====
    val previewState by TaggedTtsPreviewPlayer.state.collectAsState()
    var previewing by remember(systemTts.id) { mutableStateOf(false) }
    LaunchedEffect(previewState) { if (previewState == PreviewState.IDLE) previewing = false }

    val hasPluginLayer = source != null

    /** 试听实体=本配置项+配置层草稿（语速/音量/音高）：未应用也能先听效果；
     *  插件/全局层草稿播放时取库值，试听主要反映配置层与所选声音的组合 */
    fun draftEntity(): SystemTtsV2 = systemTts.copy(
        config = config.copy(
            audioParams = config.audioParams.copy(
                speed = snap(speed), volume = snap(volume), pitch = snap(pitch)
            )
        )
    )

    /** 维度应用（09-10 ②A）：该维三层一起落库——配置层双写（库+页面内存），
     *  插件/全局层照常写入（接管判定已废除，所有维度恒可调）；立即生效不关弹窗 */
    fun applyDim(dim: Int) {
        scope.launch {
            val newConfig = withIO {
                val nc = config.copy(
                    audioParams = config.audioParams.copy(
                        speed = if (dim == 0) snap(speed) else config.audioParams.speed,
                        volume = if (dim == 1) snap(volume) else config.audioParams.volume,
                        pitch = if (dim == 2) snap(pitch) else config.audioParams.pitch,
                    )
                )
                dbm.systemTtsV2.update(systemTts.copy(config = nc))
                if (plugin != null) {
                    dbm.pluginDao.update(
                        plugin.copy(
                            audioParams = plugin.audioParams.copy(
                                speed = if (dim == 0) snap(pluginSpeed) else plugin.audioParams.speed,
                                volume = if (dim == 1) snap(pluginVolume) else plugin.audioParams.volume,
                                pitch = if (dim == 2) snap(pluginPitch) else plugin.audioParams.pitch,
                            )
                        )
                    )
                    // 卡片"插件语速/音量"显示缓存失效，应用后重查
                    com.github.jing332.tts_server_android.compose.systts.list.ui.PluginDescriptor
                        .invalidatePluginParamsCache(plugin.pluginId)
                }
                when (dim) {
                    0 -> SysTtsConfig.audioParamsSpeed = snap(globalSpeed)
                    1 -> SysTtsConfig.audioParamsVolume = snap(globalVolume)
                    else -> SysTtsConfig.audioParamsPitch = snap(globalPitch)
                }
                SystemTtsService.notifyUpdateConfig()
                nc
            }
            // 配置层双写：回写页面内存，防"应用后再保存"被旧内存覆盖
            onSysttsChange(systemTts.copy(config = newConfig))
            when (dim) {
                0 -> speedDirty = false
                1 -> volumeDirty = false
                else -> pitchDirty = false
            }
            Toast.makeText(
                context,
                context.getString(R.string.audio_params_apply_dim_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    AppDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.audio_params)) },
        content = {
            // verticalScroll：矮屏/大字体内容超屏可滑动；水平让 4dp（叠加弹窗自带 12dp≈16dp）滑条不贴边
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // ===== 顶部：当前发音人 + 试听（用户 09-10 ⑤）=====
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "当前发音人",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            systemTts.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                        )
                    }
                    TextButton(onClick = {
                        // 试听=草稿参数+当前声音；播放中/合成中再点=停止复位（同日志快捷面板）
                        if (previewing && previewState != PreviewState.IDLE) {
                            TaggedTtsPreviewPlayer.stop()
                            previewing = false
                            return@TextButton
                        }
                        previewing = true
                        scope.launch {
                            TaggedTtsPreviewPlayer.play(context, draftEntity(), "你好，这是试听语音。")
                        }
                    }) {
                        Text(
                            when {
                                previewing && previewState == PreviewState.PLAYING -> "■"
                                previewing -> "…"
                                else -> "▶"
                            },
                            color = if (previewing) MaterialTheme.colorScheme.tertiary else Color.Unspecified,
                        )
                    }
                }

                // ===== 终值行：实时跟随三层草稿；三维恒显（用户 09-10），与卡片参数行同口径 =====
                val finalParams = computeFinalParams(
                    snap(speed), snap(volume), snap(pitch),
                    snap(pluginSpeed), snap(pluginVolume), snap(pluginPitch),
                    snap(globalSpeed), snap(globalVolume), snap(globalPitch),
                    hasPluginLayer,
                )
                Text(
                    // 与卡片参数行口径不同（用户 09-10 二稿）：
                    // 卡片=管道+1 位+加粗，弹窗=逗号+2 位+无后缀（删除 x 乘号，与日志/试听保持一致）
                    text = stringResource(
                        R.string.audio_params_final,
                        finalParams.speed.toParamText(),
                        finalParams.volume.toParamText(),
                        finalParams.pitch.toParamText(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
                )

                // ===== 按维度编辑区（09-10）：每维三层滑杆同屏，重置/应用按维度 =====
                AudioParamsDimensionSection(
                    hasPluginLayer = hasPluginLayer,
                    cfgSpeed = speed, onCfgSpeed = { speed = it; speedDirty = true },
                    cfgVolume = volume, onCfgVolume = { volume = it; volumeDirty = true },
                    cfgPitch = pitch, onCfgPitch = { pitch = it; pitchDirty = true },
                    pluginSpeed = pluginSpeed, onPluginSpeed = { pluginSpeed = it; speedDirty = true },
                    pluginVolume = pluginVolume, onPluginVolume = { pluginVolume = it; volumeDirty = true },
                    pluginPitch = pluginPitch, onPluginPitch = { pluginPitch = it; pitchDirty = true },
                    globalSpeed = globalSpeed, onGlobalSpeed = { globalSpeed = it; speedDirty = true },
                    globalVolume = globalVolume, onGlobalVolume = { globalVolume = it; volumeDirty = true },
                    globalPitch = globalPitch, onGlobalPitch = { globalPitch = it; pitchDirty = true },
                    isDirty = { when (it) { 0 -> speedDirty; 1 -> volumeDirty; else -> pitchDirty } },
                    onResetDim = { dim ->
                        when (dim) {
                            0 -> { speed = 1f; pluginSpeed = 1f; globalSpeed = 1f }
                            1 -> { volume = 1f; pluginVolume = 1f; globalVolume = 1f }
                            else -> { pitch = 1f; pluginPitch = 1f; globalPitch = 1f }
                        }
                    },
                    onApplyDim = { applyDim(it) },
                )
            }
        },
        buttons = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/** 三层乘积：三维最终值恒为 配置×插件×全局（09-10 接管判定废除，无插件源插件层按 1.0 计） */
private fun computeFinalParams(
    cfgSpeed: Float, cfgVolume: Float, cfgPitch: Float,
    pluginSpeed: Float, pluginVolume: Float, pluginPitch: Float,
    globalSpeed: Float, globalVolume: Float, globalPitch: Float,
    hasPluginLayer: Boolean,
): AudioParams {
    val pSpeed = if (hasPluginLayer) pluginSpeed else 1f
    val pVolume = if (hasPluginLayer) pluginVolume else 1f
    val pPitch = if (hasPluginLayer) pluginPitch else 1f
    return AudioParams(
        speed = cfgSpeed * pSpeed * globalSpeed,
        volume = cfgVolume * pVolume * globalVolume,
        pitch = cfgPitch * pPitch * globalPitch,
    )
}
// snap() 复用同包 AudioParamsDimensionSection.kt 的顶层定义（勿在本文件重复定义，同包重名会重载歧义）
