package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jing332.compose.widgets.LabelSlider
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.SegmentedTextToggle

/**
 * 音频参数「按维度」编辑区（用户 09-10 定稿，AudioParamsDialog 与日志快捷面板共用）：
 * - 第二级分段=语速/音量/音高，每段内同屏列出该维 配置→插件→全局 三层滑杆，调一维三层一起看一起调；
 * - 无插件源（系统TTS直连）只有 配置/全局 两行；
 * - 插件接管判定已废除（09-10 用户拍板：判定机制不真实——人工表已删、库标记恒 false），
 *   三层恒显示可调，调节不生效时以实际听感为准；
 * - 重置/应用按维度一组（09-10 ②A）：应用=该维三层一起落库（由调用方 onApplyDim 实现），
 *   草稿被改动后调用方置脏，按钮带 ● 提示该维有待保存；
 * - 值与脏状态全部由调用方持有（hoisted state），本组件无业务逻辑。
 * - collapsedAccordion=true（09-10 弹窗折叠改版，仅 AudioParamsDialog 用）：
 *   默认全收起只显示三个维度键，单开手风琴——点键展开该维滑杆，再点收起，点其他键切换；
 *   展开键高亮，脏维度带 ● 提示。日志快捷面板不受影响（保持平铺）。
 *
 * 维度下标：0=语速 1=音量 2=音高。
 */
@Composable
fun AudioParamsDimensionSection(
    hasPluginLayer: Boolean,
    cfgSpeed: Float, onCfgSpeed: (Float) -> Unit,
    cfgVolume: Float, onCfgVolume: (Float) -> Unit,
    cfgPitch: Float, onCfgPitch: (Float) -> Unit,
    pluginSpeed: Float, onPluginSpeed: (Float) -> Unit,
    pluginVolume: Float, onPluginVolume: (Float) -> Unit,
    pluginPitch: Float, onPluginPitch: (Float) -> Unit,
    globalSpeed: Float, onGlobalSpeed: (Float) -> Unit,
    globalVolume: Float, onGlobalVolume: (Float) -> Unit,
    globalPitch: Float, onGlobalPitch: (Float) -> Unit,
    isDirty: (Int) -> Boolean,
    onResetDim: (Int) -> Unit,
    onApplyDim: (Int) -> Unit,
    collapsedAccordion: Boolean = false,
) {
    val tagCfg = stringResource(R.string.audio_params_tag_config)
    val tagPlugin = stringResource(R.string.audio_params_tag_plugin)
    val tagGlobal = stringResource(R.string.audio_params_tag_global)
    val dimNames = audioParamsDimNames

    // 该维三层滑杆 + 重置/应用（两种形态共用）
    val DimContent: @Composable (Int) -> Unit = { dim ->
        Column {
            when (dim) {
                0 -> {
                    LayerSlider(tagCfg, cfgSpeed, onCfgSpeed)
                    if (hasPluginLayer) {
                        LayerSlider(tagPlugin, pluginSpeed, onPluginSpeed)
                    }
                    LayerSlider(tagGlobal, globalSpeed, onGlobalSpeed)
                }
                1 -> {
                    LayerSlider(tagCfg, cfgVolume, onCfgVolume)
                    if (hasPluginLayer) {
                        LayerSlider(tagPlugin, pluginVolume, onPluginVolume)
                    }
                    LayerSlider(tagGlobal, globalVolume, onGlobalVolume)
                }
                else -> {
                    LayerSlider(tagCfg, cfgPitch, onCfgPitch)
                    if (hasPluginLayer) {
                        LayerSlider(tagPlugin, pluginPitch, onPluginPitch)
                    }
                    LayerSlider(tagGlobal, globalPitch, onGlobalPitch)
                }
            }

            // 重置=该维三层草稿回 1.0（不落库）；应用=该维三层一起落库
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { onResetDim(dim) }) { Text(stringResource(R.string.reset)) }
                TextButton(onClick = { onApplyDim(dim) }) {
                    Text((if (isDirty(dim)) "● " else "") + stringResource(R.string.audio_params_apply))
                }
            }
        }
    }

    if (collapsedAccordion) {
        // 折叠手风琴：默认 -1 全收起；单开，点已展开键收起、点其他键切换
        var expandedDim by remember { mutableStateOf(-1) }
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                dimNames.forEachIndexed { i, name ->
                    FilterChip(
                        selected = expandedDim == i,
                        onClick = { expandedDim = if (expandedDim == i) -1 else i },
                        label = { Text((if (isDirty(i)) "● " else "") + name) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (expandedDim >= 0) DimContent(expandedDim)
        }
    } else {
        var dim by remember { mutableStateOf(0) }
        Column(Modifier.fillMaxWidth()) {
            SegmentedTextToggle(
                options = dimNames,
                selectedIndex = dim,
                onSelect = { dim = it },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            DimContent(dim)
        }
    }
}

/** 维度名（0=语速 1=音量 2=音高），编辑页三键直出 chips 与单维弹窗标题共用（用户 09-10 定稿） */
internal val audioParamsDimNames = listOf("语速", "音量", "音高")

/** 层滑杆：标签=层名（配置/插件/全局），维度已由分段表达，滑杆只标层与当前值 */
@Composable
internal fun LayerSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    LabelSlider(
        modifier = Modifier.fillMaxWidth(),
        text = "$label：%.2f".format(value),
        value = value,
        onValueChange = onValueChange,
        valueRange = 0.1f..3f,
        step = 0.05f,
    )
}

/**
 * 滑杆值去噪：0.01 步进对齐，避免浮点尾数（如 1.2000001）写进 JSON。
 * 原定义在 RemoteAudioParamsSection.kt（编辑页内嵌旧滑杆区，09-10 已删），
 * 因 AudioParamsDialog 仍依赖而随该文件一并删会断链，故迁到共用组件文件；
 * 同包顶层函数，调用方（弹窗/面板）无需 import 直接使用。
 */
internal fun snap(v: Float): Float = (kotlin.math.round(v * 100f) / 100f)
