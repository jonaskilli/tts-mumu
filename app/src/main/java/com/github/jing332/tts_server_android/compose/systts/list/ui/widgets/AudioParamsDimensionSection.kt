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
import androidx.compose.ui.unit.dp
import com.github.jing332.compose.widgets.LabelSlider
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.SoftSegmentedTextToggle

/**
 * 音频参数「按作用域」编辑区（用户 0919 定稿，AudioParamsDialog 与换声面板共用）：
 * - 分段=作用域（本项（宿主可改叫「配置项」）→插件→全局），每段内同屏列出该层
 *   语速/音量/音高 三条滑杆——调一层三维一起看一起调；
 *   与旧「按维度」形态（0910 定，维度在分段、层在滑杆行）互为转置，0919 用户拍板调换；
 * - 无插件源（系统TTS直连）分段隐藏「插件」，只有 本项/全局 两段；
 * - 插件接管判定已废除（0910 用户拍板），三层恒显示可调，调节不生效时以实际听感为准；
 * - 重置/应用按作用域一组：应用=该层三维一起落库（由调用方 onApplyScope 实现）；
 *   isDirty 入参保留但不再用于渲染（0911 起按钮前无脏标记）；
 * - 值与脏状态全部由调用方持有（hoisted state），本组件无业务逻辑。
 *
 * 层号（回调入参）：0=本项(配置) 1=插件 2=全局。
 */
@Composable
fun AudioParamsDimensionSection(
    hasPluginLayer: Boolean,
    // 第一段的标签：null=「本项」（配置项弹窗）；换声面板传「配置项」（用户 0919：视宿主而定）
    firstScopeLabel: String? = null,
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
    onResetScope: (Int) -> Unit,
    onApplyScope: (Int) -> Unit,
    // 重置/应用按钮行最左侧的自定义按钮（可选）。仅配置项音频参数弹窗传入「取消」，
    // 实现「取消（左）｜ 重置 · 应用（右）」统一排布；换声面板不传 → 按钮行纯右对齐。
    leadingAction: (@Composable () -> Unit)? = null,
) {
    val tagCfg = firstScopeLabel ?: stringResource(R.string.audio_params_tag_config)
    val tagPlugin = stringResource(R.string.audio_params_tag_plugin)
    val tagGlobal = stringResource(R.string.audio_params_tag_global)
    val dimNames = audioParamsDimNames

    // 作用域分段（显示序）→ 层号映射：0=本项 1=插件 2=全局；无插件源隐藏「插件」段
    val scopeOptions = buildList {
        add(tagCfg)
        if (hasPluginLayer) add(tagPlugin)
        add(tagGlobal)
    }
    val scopeLayers = buildList {
        add(0)
        if (hasPluginLayer) add(1)
        add(2)
    }
    var scope by remember { mutableStateOf(0) }
    val safeScope = scope.coerceIn(0, scopeOptions.lastIndex)
    val layer = scopeLayers[safeScope]

    Column(Modifier.fillMaxWidth()) {
        // 作用域分段（软槽分段，形态与字号沿用 0911 定稿；顶距 8dp 与上方内容拉开）
        SoftSegmentedTextToggle(
            options = scopeOptions,
            selectedIndex = safeScope,
            onSelect = { scope = it },
            modifier = Modifier.padding(top = 8.dp),
        )
        // 该层 语速/音量/音高 三条滑杆 + 重置/应用（行距 4dp、顶距 8dp、左缩进 8dp，沿用 0911 定稿）
        Column(
            Modifier.padding(top = 8.dp, start = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            when (layer) {
                0 -> {
                    LayerSlider(dimNames[0], cfgSpeed, onCfgSpeed)
                    LayerSlider(dimNames[1], cfgVolume, onCfgVolume)
                    LayerSlider(dimNames[2], cfgPitch, onCfgPitch)
                }
                1 -> {
                    LayerSlider(dimNames[0], pluginSpeed, onPluginSpeed)
                    LayerSlider(dimNames[1], pluginVolume, onPluginVolume)
                    LayerSlider(dimNames[2], pluginPitch, onPluginPitch)
                }
                else -> {
                    LayerSlider(dimNames[0], globalSpeed, onGlobalSpeed)
                    LayerSlider(dimNames[1], globalVolume, onGlobalVolume)
                    LayerSlider(dimNames[2], globalPitch, onGlobalPitch)
                }
            }

            // 重置=该层三维草稿回 1.0（不落库）；应用=该层三维一起落库
            // leadingAction（配置项弹窗传「取消」）占最左，weight Spacer 把重置/应用顶到最右
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (leadingAction != null) {
                    leadingAction()
                    Spacer(Modifier.weight(1f))
                }
                TextButton(onClick = { onResetScope(layer) }) { Text(stringResource(R.string.reset)) }
                TextButton(onClick = { onApplyScope(layer) }) {
                    Text(stringResource(R.string.audio_params_apply))
                }
            }
        }
    }
}

/** 维度名（0=语速 1=音量 2=音高），滑杆行标签共用（0919 调换后维度在滑杆行） */
internal val audioParamsDimNames = listOf("语速", "音量", "音高")

/**
 * 滑杆：标签=维度名（语速/音量/音高，0919 调换后维度在滑杆行）+ 当前值，作用域已由分段表达。
 *
 * 尺寸（用户 09-11 终裁「完全 MD3 版」）：不传任何尺寸——`LabelSlider` 官方 MD3 默认；
 * 标签列不定宽：三维行标签均 2 字、数值均 4 字符，天然对齐。
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
 * 同包顶层函数，调用方（弹窗/面板）无需 import 直接使用。
 */
internal fun snap(v: Float): Float = (kotlin.math.round(v * 100f) / 100f)
