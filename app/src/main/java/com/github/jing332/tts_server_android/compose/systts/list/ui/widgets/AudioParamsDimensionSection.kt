package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * - 形态只有一种：维度分段选择器（语速/音量/音高）+ 当前维三层滑杆，平铺展示。
 *   09-10 曾试过 collapsedAccordion 折叠手风琴形态供配置项弹窗用，同日用户裁定撤销
 *   （弹窗须与日志快捷面板同款排版），该形态代码已删，勿再引入。
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
) {
    val tagCfg = stringResource(R.string.audio_params_tag_config)
    val tagPlugin = stringResource(R.string.audio_params_tag_plugin)
    val tagGlobal = stringResource(R.string.audio_params_tag_global)
    val dimNames = audioParamsDimNames

    // 该维三层滑杆 + 重置/应用（本组件唯一形态共用）。
    // 行距 4dp（用户 09-10 晚）：此前三层紧贴，± 触摸区 48dp 上下相接显得挤；重置/应用行同样获得间隔
    val DimContent: @Composable (Int) -> Unit = { dim ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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

    // 维度分段（语速/音量/音高）+ 当前维三层滑杆，平铺（唯一形态，用户 09-10 恢复）
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

/** 维度名（0=语速 1=音量 2=音高），编辑页三键直出 chips 与单维弹窗标题共用（用户 09-10 定稿） */
internal val audioParamsDimNames = listOf("语速", "音量", "音高")

/**
 * 层滑杆：标签=层名（发音人/插件/全局，见 audio_params_tag_* 串），维度已由分段表达，滑杆只标层与当前值。
 *
 * 尺寸（用户 09-10 晚定稿）：音频参数三处——卡片⋮弹窗 `AudioParamsDialog`、日志快捷面板 `LogQuickPanel`、
 * 编辑页单维弹窗 `AudioParamsDimDialog`——全部经本函数取滑杆，故在此统一传大一号尺寸：
 * - 标签 14sp：与上方维度选择区「语速/音量/音高」(labelLarge 14sp) 齐平，避免子项压住父项
 *   （严格按最早是 16sp，会比选择区还大且等于顶部发音人名 titleMedium，故不取）；
 * - 加减 48dp 触摸区 / **22dp 图标**：触摸区恢复改造前 M3 默认（09-08 收窄版是 32dp/20dp）；
 *   图标取 22dp 而非改造前的 24dp——24dp 视觉重量≈20sp 的字，配 14sp 标签偏重（改造前是 24dp+16sp 配对），
 *   22dp 与 14sp 成对更协调；
 * - 轨道 3.5dp：介于最早 4dp 与 09-08 的 3dp 之间；thumb 同步 3.5×22dp；
 * - 标签列 `labelMinWidth = 44.dp`：定宽后三行 −按钮与轨道对齐（见行内注释）。
 * 其他界面滑杆直接调 `LabelSlider`、不传这些参数，保持原样（默认 13sp/32dp/20dp/3dp/3×20dp/不定宽）。
 */
@Composable
internal fun LayerSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    LabelSlider(
        modifier = Modifier.fillMaxWidth(),
        text = "$label：%.2f".format(value),
        value = value,
        onValueChange = onValueChange,
        valueRange = 0.1f..3f,
        step = 0.05f,
        labelFontSize = 14.sp,
        buttonSize = 48.dp,
        iconSize = 22.dp,
        trackHeight = 3.5.dp,
        thumbSize = DpSize(3.5.dp, 22.dp),
        // 三行标签字数不同（发音人 3 字 / 插件·全局 2 字），不定宽会让 −按钮与轨道左右错开约 11dp；
        // 44dp 最小宽（+末尾 8dp 间距＝52dp）让三行左边界一条线，英文 Voice/Plugin/Global 同样对齐
        labelMinWidth = 44.dp,
    )
}

/**
 * 滑杆值去噪：0.01 步进对齐，避免浮点尾数（如 1.2000001）写进 JSON。
 * 原定义在 RemoteAudioParamsSection.kt（编辑页内嵌旧滑杆区，09-10 已删），
 * 因 AudioParamsDialog 仍依赖而随该文件一并删会断链，故迁到共用组件文件；
 * 同包顶层函数，调用方（弹窗/面板）无需 import 直接使用。
 */
internal fun snap(v: Float): Float = (kotlin.math.round(v * 100f) / 100f)
