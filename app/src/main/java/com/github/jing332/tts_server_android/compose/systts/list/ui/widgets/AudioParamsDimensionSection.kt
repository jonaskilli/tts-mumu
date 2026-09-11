package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.jing332.compose.widgets.LabelSlider
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.SoftSegmentedTextToggle

/**
 * 音频参数「按维度」编辑区（用户 09-10 定稿，AudioParamsDialog 与日志快捷面板共用）：
 * - 第二级分段=语速/音量/音高，每段内同屏列出该维 配置→插件→全局 三层滑杆，调一维三层一起看一起调；
 * - 无插件源（系统TTS直连）只有 配置/全局 两行；
 * - 插件接管判定已废除（09-10 用户拍板：判定机制不真实——人工表已删、库标记恒 false），
 *   三层恒显示可调，调节不生效时以实际听感为准；
 * - 重置/应用按维度一组（09-10 ②A）：应用=该维三层一起落库（由调用方 onApplyDim 实现）；
 *   按钮前的 ● 脏标记已撤（09-11 用户：应用前不放圆点），isDirty 入参保留但不再用于渲染;
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
    // 初始选中维度（0=语速 1=音量 2=音高）：编辑页三键点哪个键就以哪个维度打开（用户 09-10 晚）
    initialDim: Int = 0,
    // 重置/应用按钮行最左侧的自定义按钮（可选）。仅配置项音频参数弹窗传入「取消」，
    // 实现全 app 统一排布「取消（左）｜ 重置 · 应用（右）」（见 GlobalAudioParamsDialog 同款约定）；
    // 日志快捷面板不传 → 参数为 null，按钮行维持纯右对齐不受影响。用户 09-11 夜要求取消置最左。
    leadingAction: (@Composable () -> Unit)? = null,
) {
    val tagCfg = stringResource(R.string.audio_params_tag_config)
    val tagPlugin = stringResource(R.string.audio_params_tag_plugin)
    val tagGlobal = stringResource(R.string.audio_params_tag_global)
    val dimNames = audioParamsDimNames

    // 该维三层滑杆 + 重置/应用（本组件唯一形态共用）。
    // 行距 4dp（用户 09-10 晚）：此前三层紧贴，± 触摸区 48dp 上下相接显得挤；重置/应用行同样获得间隔
    val DimContent: @Composable (Int) -> Unit = { dim ->
        // 顶距 8dp（用户 09-11 二轮：4dp 仍显近，软槽与滑杆区分开一点）；
        // 左缩进 8dp（用户 09-11：三排滑杆离弹窗左缘太近，整体右移一点，± 键与轨道一起动）
        Column(
            Modifier.padding(top = 8.dp, start = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
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
            // leadingAction（配置项弹窗传「取消」）占最左，weight Spacer 把重置/应用顶到最右
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (leadingAction != null) {
                    leadingAction()
                    Spacer(Modifier.weight(1f))
                }
                TextButton(onClick = { onResetDim(dim) }) { Text(stringResource(R.string.reset)) }
                TextButton(onClick = { onApplyDim(dim) }) {
                    // 绿点脏标记已撤（用户 09-11：应用前不放圆点）
                    Text(stringResource(R.string.audio_params_apply))
                }
            }
        }
    }

    // 维度分段（语速/音量/音高）+ 当前维三层滑杆，平铺（唯一形态，用户 09-10 恢复）。
    // 09-10 晚定形态：本处是**第二级（子级）**，用「无描边浅底槽 + 三等分文字」的软槽分段（14sp），
    // 与日志面板外层分区切换（第一级父级：描边胶囊 16sp、宽度随文字）在形态/字号/宽度行为上全不同。
    // 09-11 起选中项带浮起胶囊（见 SoftSegmentedTextToggle）；槽顶 8dp 顶距，与上方内容拉开不贴脸
    // 本组件被日志面板与配置项弹窗共用，两处一起变（编辑页单维弹窗无选择器，不受影响）
    var dim by remember(initialDim) { mutableStateOf(initialDim.coerceIn(0, dimNames.lastIndex)) }
    Column(Modifier.fillMaxWidth()) {
        SoftSegmentedTextToggle(
            options = dimNames,
            selectedIndex = dim,
            onSelect = { dim = it },
            modifier = Modifier.padding(top = 8.dp),
        )
        DimContent(dim)
    }
}

/** 维度名（0=语速 1=音量 2=音高），编辑页软槽与弹窗/日志面板共用（用户 09-10 定稿） */
internal val audioParamsDimNames = listOf("语速", "音量", "音高")

/**
 * 层滑杆：标签=层名（本项/插件/全局，见 audio_params_tag_* 串），维度已由分段表达，滑杆只标层与当前值。
 *
 * 尺寸（用户 09-11 下午终裁「完全 MD3 版」）：不传任何尺寸——`LabelSlider` 已回归官方 MD3
 * 默认（官方轨道/手柄画法、标签 12sp=bodySmall、± 键 48dp 触摸区 24dp 图标），
 * 09-11 深夜的胶囊轨道 10dp/竖条 thumb 4×22 自绘规格已撤销；本包装只负责标签文案与取值范围/步进。
 * 标签列不定宽（09-11 撤 44dp 定宽）：层名改「本项」后三行均 2 字、数值均 4 字符，天然对齐。
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
    )
}

/**
 * 滑杆值去噪：0.01 步进对齐，避免浮点尾数（如 1.2000001）写进 JSON。
 * 原定义在 RemoteAudioParamsSection.kt（编辑页内嵌旧滑杆区，09-10 已删），
 * 因 AudioParamsDialog 仍依赖而随该文件一并删会断链，故迁到共用组件文件；
 * 同包顶层函数，调用方（弹窗/面板）无需 import 直接使用。
 */
internal fun snap(v: Float): Float = (kotlin.math.round(v * 100f) / 100f)
