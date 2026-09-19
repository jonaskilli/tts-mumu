package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drake.net.utils.withIO
import com.github.jing332.common.utils.toParamText
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.list.ui.PluginDescriptor
import com.github.jing332.tts_server_android.conf.SysTtsConfig
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import kotlinx.coroutines.launch

/**
 * 编辑页「音频参数」区（用户 0919 定稿，按作用域版——与弹窗/换声面板对齐）：
 *
 * **收起态（默认，保留 0912 省地方的决策）**：一行「音频参数」+ 三维终值摘要
 * （语速0.97 · 音量1.10 · 音高1.00，实时跟随草稿）+ 展开箭头——
 * 终值收起也一眼可见（用户 0919 关注点：对调后最终值不能没地方看）。
 *
 * **展开态**：终值行 + 作用域分段（本项→插件→全局，无插件源自动两段，共用
 * [AudioParamsDimensionSection]）+ 该层 语速/音量/音高 三条滑杆 + 重置/应用
 * （应用=该层三维一起落库：本项层双写页面内存、插件层失效卡片缓存、全局写 SysTtsConfig）。
 *
 * 试听仍走试听文本行的 🎧（[AudioParamsDraft] 草稿快照上报保留：滑杆调完即听，不必先应用）。
 * 层号：0=本项(配置) 1=插件 2=全局。
 */

/**
 * 编辑页音频参数三层草稿快照（用户 09-17：滑杆调完 🎧 就能听，不用先点应用）。
 * 由 [AudioParamsDimRows] 在组合与每次滑杆改动时上报给宿主；🎧 试听用它拼草稿实体 + 两层 override。
 * [plugin] 为 null = 该配置无插件层——此时试听 override 必须传 null（传 1.0 会把真实插件参数抹掉）。
 */
data class AudioParamsDraft(
    val config: AudioParams,
    val plugin: AudioParams?,
    val global: AudioParams,
)

/** 用配置层草稿参数拷贝实体（🎧 试听带草稿用，其余字段原样） */
fun SystemTtsV2.withAudioParams(p: AudioParams): SystemTtsV2 {
    val dto = config as? TtsConfigurationDTO ?: return this
    return copy(config = dto.copy(audioParams = p))
}

@Composable
fun AudioParamsDimRows(
    modifier: Modifier = Modifier,
    systemTts: SystemTtsV2,
    onSysttsChange: (SystemTtsV2) -> Unit,
    // 三层草稿快照上报（用户 09-17：滑杆调完 🎧 即听草稿效果，不必先点应用）：
    // 组合即报初值（=库值，等价无 override），此后每次滑杆改动随重组上报
    onDraftChange: (AudioParamsDraft) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val config = systemTts.config as? TtsConfigurationDTO ?: return
    val source = config.source as? PluginTtsSource
    val plugin = source?.let { dbm.pluginDao.getByPluginId(it.pluginId) }
    val hasPluginLayer = source != null

    // 展开状态（收起=默认，保留 0912 省地方的决策；展开后与弹窗/换声面板同款）
    var expanded by remember(systemTts.id) { mutableStateOf(false) }

    // 三层草稿（三维共用一份；展开哪层就调哪层）
    var speed by remember(systemTts.id) { mutableStateOf(config.audioParams.speed) }
    var volume by remember(systemTts.id) { mutableStateOf(config.audioParams.volume) }
    var pitch by remember(systemTts.id) { mutableStateOf(config.audioParams.pitch) }
    var pluginSpeed by remember { mutableStateOf(plugin?.audioParams?.speed ?: 1f) }
    var pluginVolume by remember { mutableStateOf(plugin?.audioParams?.volume ?: 1f) }
    var pluginPitch by remember { mutableStateOf(plugin?.audioParams?.pitch ?: 1f) }
    var globalSpeed by remember { mutableStateOf(SysTtsConfig.audioParamsSpeed) }
    var globalVolume by remember { mutableStateOf(SysTtsConfig.audioParamsVolume) }
    var globalPitch by remember { mutableStateOf(SysTtsConfig.audioParamsPitch) }

    // 按层脏标记（0919 按作用域版）：该层任一滑杆改动置 true，应用成功清除
    var cfgDirty by remember(systemTts.id) { mutableStateOf(false) }
    var pluginDirty by remember(systemTts.id) { mutableStateOf(false) }
    var globalDirty by remember(systemTts.id) { mutableStateOf(false) }

    // 草稿快照上报：组合即报初值，滑杆任一层改动随重组再报（值已 snap，宿主直接可用）
    LaunchedEffect(
        speed, volume, pitch,
        pluginSpeed, pluginVolume, pluginPitch,
        globalSpeed, globalVolume, globalPitch, hasPluginLayer,
    ) {
        onDraftChange(
            AudioParamsDraft(
                config = AudioParams(speed = snap(speed), volume = snap(volume), pitch = snap(pitch)),
                plugin = if (hasPluginLayer) AudioParams(
                    speed = snap(pluginSpeed), volume = snap(pluginVolume), pitch = snap(pluginPitch)
                ) else null,
                global = AudioParams(
                    speed = snap(globalSpeed), volume = snap(globalVolume), pitch = snap(globalPitch)
                ),
            )
        )
    }

    /** 作用域应用（0919 按作用域版）：该层三维一起落库——本项层双写页面内存（防旧值覆盖）、
     *  插件层失效卡片缓存、全局写 SysTtsConfig；notifyUpdateConfig 立即生效 */
    fun applyScope(layer: Int) {
        scope.launch {
            val newConfig = withIO {
                val nc = if (layer == 0) config.copy(
                    audioParams = config.audioParams.copy(
                        speed = snap(speed), volume = snap(volume), pitch = snap(pitch),
                    )
                ) else config
                dbm.systemTtsV2.update(systemTts.copy(config = nc))
                if (layer == 1 && plugin != null) {
                    dbm.pluginDao.update(
                        plugin.copy(
                            audioParams = plugin.audioParams.copy(
                                speed = snap(pluginSpeed),
                                volume = snap(pluginVolume),
                                pitch = snap(pluginPitch),
                            )
                        )
                    )
                    PluginDescriptor.invalidatePluginParamsCache(plugin.pluginId)
                }
                if (layer == 2) {
                    SysTtsConfig.audioParamsSpeed = snap(globalSpeed)
                    SysTtsConfig.audioParamsVolume = snap(globalVolume)
                    SysTtsConfig.audioParamsPitch = snap(globalPitch)
                }
                SystemTtsService.notifyUpdateConfig()
                nc
            }
            // 本项层双写：回写页面内存，防"应用后再保存"被旧内存覆盖
            if (layer == 0) onSysttsChange(systemTts.copy(config = newConfig))
            when (layer) {
                0 -> cfgDirty = false
                1 -> pluginDirty = false
                else -> globalDirty = false
            }
            Toast.makeText(
                context,
                context.getString(R.string.audio_params_apply_dim_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    Column(modifier.fillMaxWidth()) {
        // 收起/展开切换行：标题 + 三维终值摘要（实时跟随草稿，收起也能一眼看到最终值）+ 箭头
        val arrowAngle by animateFloatAsState(
            targetValue = if (expanded) 0f else -90f, label = ""
        )
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.audio_params),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            // 三维终值摘要（本项×插件×全局实时乘积，用户 0919 实机：此前误显本项层原值）。
            // 绿色 = 与终值行/绿字口径一致
            val summary = computeFinalParams(
                snap(speed), snap(volume), snap(pitch),
                snap(pluginSpeed), snap(pluginVolume), snap(pluginPitch),
                snap(globalSpeed), snap(globalVolume), snap(globalPitch),
                hasPluginLayer,
            )
            Text(
                text = "语速${summary.speed.toParamText()} · 音量${summary.volume.toParamText()} · 音高${summary.pitch.toParamText()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
            Spacer(Modifier.size(8.dp))
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp).rotate(arrowAngle)
            )
        }

        if (expanded) {
            // 作用域分段 + 该层 语速/音量/音高 三条滑杆 + 重置/应用（与弹窗/换声面板同款组件）。
            // 终值不再重复摆一行：收起行摘要 + 滑杆标签已覆盖（用户 0919 实机反馈）
            AudioParamsDimensionSection(
                hasPluginLayer = hasPluginLayer,
                firstScopeLabel = stringResource(R.string.audio_params_tag_config_item),
                cfgSpeed = speed, onCfgSpeed = { speed = it; cfgDirty = true },
                cfgVolume = volume, onCfgVolume = { volume = it; cfgDirty = true },
                cfgPitch = pitch, onCfgPitch = { pitch = it; cfgDirty = true },
                pluginSpeed = pluginSpeed, onPluginSpeed = { pluginSpeed = it; pluginDirty = true },
                pluginVolume = pluginVolume, onPluginVolume = { pluginVolume = it; pluginDirty = true },
                pluginPitch = pluginPitch, onPluginPitch = { pluginPitch = it; pluginDirty = true },
                globalSpeed = globalSpeed, onGlobalSpeed = { globalSpeed = it; globalDirty = true },
                globalVolume = globalVolume, onGlobalVolume = { globalVolume = it; globalDirty = true },
                globalPitch = globalPitch, onGlobalPitch = { globalPitch = it; globalDirty = true },
                isDirty = { when (it) { 0 -> cfgDirty; 1 -> pluginDirty; else -> globalDirty } },
                onResetScope = { layer ->
                    when (layer) {
                        0 -> { speed = 1f; volume = 1f; pitch = 1f }
                        1 -> { pluginSpeed = 1f; pluginVolume = 1f; pluginPitch = 1f }
                        else -> { globalSpeed = 1f; globalVolume = 1f; globalPitch = 1f }
                    }
                },
                onApplyScope = { applyScope(it) },
            )
        }
    }
}
