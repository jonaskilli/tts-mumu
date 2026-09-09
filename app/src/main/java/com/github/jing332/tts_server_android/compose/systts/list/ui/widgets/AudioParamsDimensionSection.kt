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
 * - 插件接管该维时（pluginHandlesX，软件自动判定）隐藏插件/全局滑杆，显示说明行，避免调了不生效；
 * - 无插件源（系统TTS直连）只有 配置/全局 两行；
 * - 重置/应用按维度一组（09-10 ②A）：应用=该维未接管的三层一起落库（由调用方 onApplyDim 实现），
 *   草稿被改动后调用方置脏，按钮带 ● 提示该维有待保存；
 * - 值与脏状态全部由调用方持有（hoisted state），本组件无业务逻辑。
 *
 * 维度下标：0=语速 1=音量 2=音高。
 */
@Composable
fun AudioParamsDimensionSection(
    hasPluginLayer: Boolean,
    handlesSpeed: Boolean,
    handlesVolume: Boolean,
    handlesPitch: Boolean,
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
) {
    var dim by remember { mutableStateOf(0) }
    val tagCfg = stringResource(R.string.audio_params_tag_config)
    val tagPlugin = stringResource(R.string.audio_params_tag_plugin)
    val tagGlobal = stringResource(R.string.audio_params_tag_global)
    val dimNames = listOf("语速", "音量", "音高")

    Column(Modifier.fillMaxWidth()) {
        SegmentedTextToggle(
            options = dimNames,
            selectedIndex = dim,
            onSelect = { dim = it },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        // 该维三层滑杆：配置层恒显示；插件层被接管→说明行；无插件源→跳过插件行
        when (dim) {
            0 -> {
                LayerSlider(tagCfg, cfgSpeed, onCfgSpeed)
                if (hasPluginLayer) {
                    if (handlesSpeed) TakenOverHint(dimNames[0])
                    else {
                        LayerSlider(tagPlugin, pluginSpeed, onPluginSpeed)
                        LayerSlider(tagGlobal, globalSpeed, onGlobalSpeed)
                    }
                } else LayerSlider(tagGlobal, globalSpeed, onGlobalSpeed)
            }
            1 -> {
                LayerSlider(tagCfg, cfgVolume, onCfgVolume)
                if (hasPluginLayer) {
                    if (handlesVolume) TakenOverHint(dimNames[1])
                    else {
                        LayerSlider(tagPlugin, pluginVolume, onPluginVolume)
                        LayerSlider(tagGlobal, globalVolume, onGlobalVolume)
                    }
                } else LayerSlider(tagGlobal, globalVolume, onGlobalVolume)
            }
            else -> {
                LayerSlider(tagCfg, cfgPitch, onCfgPitch)
                if (hasPluginLayer) {
                    if (handlesPitch) TakenOverHint(dimNames[2])
                    else {
                        LayerSlider(tagPlugin, pluginPitch, onPluginPitch)
                        LayerSlider(tagGlobal, globalPitch, onGlobalPitch)
                    }
                } else LayerSlider(tagGlobal, globalPitch, onGlobalPitch)
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

/** 层滑杆：标签=层名（配置/插件/全局），维度已由分段表达，滑杆只标层与当前值 */
@Composable
private fun LayerSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    LabelSlider(
        modifier = Modifier.fillMaxWidth(),
        text = "$label：%.2f".format(value),
        value = value,
        onValueChange = onValueChange,
        valueRange = 0.1f..3f,
        step = 0.05f,
    )
}

/** 插件接管说明（09-10 ③）：该维插件/全局层不参与叠加，调了也不生效，故隐藏滑杆只留说明 */
@Composable
private fun TakenOverHint(dimName: String) {
    Text(
        "该插件自行处理$dimName（插件/全局层不参与）",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}
