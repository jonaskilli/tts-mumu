package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.sp
import com.drake.net.utils.withIO
import com.github.jing332.common.utils.toParamText
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.SoftSegmentedTextToggle
import com.github.jing332.tts_server_android.compose.systts.list.ui.PluginDescriptor
import com.github.jing332.tts_server_android.conf.SysTtsConfig
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import kotlinx.coroutines.launch

/**
 * 编辑页「音频参数」区（用户 09-11 定稿，最终形态）：
 *
 * **软槽分段一行三项**：与卡片⋮弹窗/日志快捷面板同款 [SoftSegmentedTextToggle]（16sp），
 * 槽内 `语速 1.0`　`音量 1.3`　`音高 1.0`（各带该维**终值**＝配置×插件×全局，FOLLOW 视为 1，
 * 跟随草稿实时变）；选中（=正在展开）项浮起胶囊高亮。**默认展开语速、永不全收起**（方案A）：
 * 浮块本身就是可点示范，且与弹窗"永远有一维被选中"的行为完全一致。
 *
 * **展开区只留滑条**：该维三层滑杆（本项/插件/全局，无插件源自动只有两层）+ 重置/应用，
 * 无底框平铺（与弹窗/日志面板展开区一致）——
 * 滑条外的信息（终值行、▶试听）一律不放：值已经在槽里、也在滑杆标签里，
 * **试听直接用试听文本行的 🎧**（[AuditionTextField]）。
 *
 * 应用＝该维三层一起落库：配置层双写页面内存（防"应用后再保存"被旧内存覆盖）、插件层失效卡片缓存、
 * 全局写 SysTtsConfig、notifyUpdateConfig 立即生效，toast 反馈；重置＝该维三层草稿回 1.0（不落库）。
 *
 * 卡片⋮入口与日志快捷面板仍走共用的 [AudioParamsDialog]（那边要发音人上下文与面板排版，是弹窗形态）。
 *
 * 维度下标：0=语速 1=音量 2=音高。
 */

@Composable
fun AudioParamsDimRows(
    modifier: Modifier = Modifier,
    systemTts: SystemTtsV2,
    onSysttsChange: (SystemTtsV2) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val config = systemTts.config as? TtsConfigurationDTO ?: return
    val source = config.source as? PluginTtsSource
    val plugin = source?.let { dbm.pluginDao.getByPluginId(it.pluginId) }
    val hasPluginLayer = source != null

    // 展开的维度（用户 09-11 方案A：默认展开语速，永不全收起——浮块常驻即可点性示范）
    var expanded by remember(systemTts.id) { mutableStateOf(0) }

    // 三层草稿（三维共用一份；展开哪维就调哪维）
    var speed by remember(systemTts.id) { mutableStateOf(config.audioParams.speed) }
    var volume by remember(systemTts.id) { mutableStateOf(config.audioParams.volume) }
    var pitch by remember(systemTts.id) { mutableStateOf(config.audioParams.pitch) }
    var pluginSpeed by remember { mutableStateOf(plugin?.audioParams?.speed ?: 1f) }
    var pluginVolume by remember { mutableStateOf(plugin?.audioParams?.volume ?: 1f) }
    var pluginPitch by remember { mutableStateOf(plugin?.audioParams?.pitch ?: 1f) }
    var globalSpeed by remember { mutableStateOf(SysTtsConfig.audioParamsSpeed) }
    var globalVolume by remember { mutableStateOf(SysTtsConfig.audioParamsVolume) }
    var globalPitch by remember { mutableStateOf(SysTtsConfig.audioParamsPitch) }

    // 按维度脏标记：该维任一层滑杆改动置 true，应用成功清除（应用键带 ●）
    var speedDirty by remember(systemTts.id) { mutableStateOf(false) }
    var volumeDirty by remember(systemTts.id) { mutableStateOf(false) }
    var pitchDirty by remember(systemTts.id) { mutableStateOf(false) }
    fun isDirty(dim: Int) = when (dim) { 0 -> speedDirty; 1 -> volumeDirty; else -> pitchDirty }
    fun clearDirty(dim: Int) {
        when (dim) { 0 -> speedDirty = false; 1 -> volumeDirty = false; else -> pitchDirty = false }
    }

    /** 该维终值（走草稿，跟随滑杆实时变） */
    fun finalOf(dim: Int): Float {
        val c = when (dim) { 0 -> speed; 1 -> volume; else -> pitch }
        val p = if (hasPluginLayer) when (dim) { 0 -> pluginSpeed; 1 -> pluginVolume; else -> pluginPitch } else 1f
        val g = when (dim) { 0 -> globalSpeed; 1 -> globalVolume; else -> globalPitch }
        return c * p * g
    }

    /** 该维三层一起落库（与 AudioParamsDialog.applyDim 完全同源） */
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
                    PluginDescriptor.invalidatePluginParamsCache(plugin.pluginId)
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
            clearDirty(dim)
            Toast.makeText(
                context,
                context.getString(R.string.audio_params_apply_dim_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val tagCfg = stringResource(R.string.audio_params_tag_config)
    val tagPlugin = stringResource(R.string.audio_params_tag_plugin)
    val tagGlobal = stringResource(R.string.audio_params_tag_global)

    Column(modifier.fillMaxWidth()) {
        // 软槽分段（用户 09-11 定稿）：与弹窗/日志面板同款，选中项浮起胶囊；
        // 16sp 与编辑页其他字段齐平；顶距 12dp 与上方试听文本行拉开又相融。
        // 槽内文字=维度名+该维终值（toParamText 与卡片/弹窗/日志同口径）
        SoftSegmentedTextToggle(
            options = audioParamsDimNames.mapIndexed { dim, name ->
                "$name " + finalOf(dim).toParamText()
            },
            selectedIndex = expanded,
            onSelect = { expanded = it },
            labelFontSize = 16.sp,
            modifier = Modifier.padding(top = 12.dp),
        )

        // 展开区：该维三层滑杆 + 重置/应用，无底框平铺（与弹窗/日志面板一致）；
        // 滑条外的信息一律不放；试听走试听文本行的 🎧
        val dim = expanded
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
                when (dim) {
                    0 -> {
                        LayerSlider(tagCfg, speed) { speed = it; speedDirty = true }
                        if (hasPluginLayer) {
                            LayerSlider(tagPlugin, pluginSpeed) { pluginSpeed = it; speedDirty = true }
                        }
                        LayerSlider(tagGlobal, globalSpeed) { globalSpeed = it; speedDirty = true }
                    }
                    1 -> {
                        LayerSlider(tagCfg, volume) { volume = it; volumeDirty = true }
                        if (hasPluginLayer) {
                            LayerSlider(tagPlugin, pluginVolume) { pluginVolume = it; volumeDirty = true }
                        }
                        LayerSlider(tagGlobal, globalVolume) { globalVolume = it; volumeDirty = true }
                    }
                    else -> {
                        LayerSlider(tagCfg, pitch) { pitch = it; pitchDirty = true }
                        if (hasPluginLayer) {
                            LayerSlider(tagPlugin, pluginPitch) { pluginPitch = it; pitchDirty = true }
                        }
                        LayerSlider(tagGlobal, globalPitch) { globalPitch = it; pitchDirty = true }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        when (dim) {
                            0 -> { speed = 1f; pluginSpeed = 1f; globalSpeed = 1f }
                            1 -> { volume = 1f; pluginVolume = 1f; globalVolume = 1f }
                            else -> { pitch = 1f; pluginPitch = 1f; globalPitch = 1f }
                        }
                    }) { Text(stringResource(R.string.reset)) }
                    TextButton(onClick = { applyDim(dim) }) {
                        Text((if (isDirty(dim)) "● " else "") + stringResource(R.string.audio_params_apply))
                    }
                }
            }
    }
}
