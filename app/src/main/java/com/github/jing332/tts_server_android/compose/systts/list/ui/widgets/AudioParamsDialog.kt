package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.conf.SysTtsConfig
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import kotlinx.coroutines.launch

/**
 * 配置项「音频参数」弹窗（卡片 ⋮ 菜单入口；编辑页走「三键直出 + 单维弹窗」另一条路径，不经过本弹窗）。
 * 结构（用户 09-10 恢复平铺版，与 8220d5b 原版一致，只少了面板顶部的发音人区）：
 * - 顶部不显示当前发音人（c817819 删）、不显示终值行（9b23435 删）、无 ▶试听键（试听归 🎧）；
 *   上下文已明确，终值在卡片参数行/日志仍可见，滑杆旁本就带实时数值；
 * - 主体：[AudioParamsDimensionSection] 平铺——维度分段（语速/音量/音高）+
 *   该维 发音人→插件→全局 三层滑杆同屏；重置/应用按维度一组，
 *   应用=该维三层一起落库（配置层双写页面内存防旧值覆盖）；
 * - 排版与日志快捷面板（LogQuickPanel）音频参数区同款，只少了面板顶部的
 *   「当前发音人 + 更换发音人/音频参数」切换区——本弹窗入口唯一，无换声诉求；
 *   09-10 试过的 collapsedAccordion 折叠手风琴形态已撤销（勿再引入）。
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

    val hasPluginLayer = source != null

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
                // ===== 按维度编辑区（09-10 平铺版，用户裁定恢复）：维度分段（语速/音量/音高）
                //      + 该维三层滑杆 + 重置/应用，与日志快捷面板同款排版；
                //      折叠手风琴形态同日撤销并删码，勿再引入 =====
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

// snap() 复用同包 AudioParamsDimensionSection.kt 的顶层定义（勿在本文件重复定义，同包重名会重载歧义）
