@file:OptIn(ExperimentalMaterial3Api::class)
package com.github.jing332.compose.widgets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.jing332.common.utils.performLongPress
import com.github.jing332.compose.R
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LabelSlider(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,

    showButton: Boolean = true,
    buttonSteps: Float = 0.01f,
    buttonLongSteps: Float = 0.1f,

    step: Float = 0f,

    valueChange: (Float) -> Unit = {
        if (it < valueRange.start) onValueChange(valueRange.start)
        else if (it > valueRange.endInclusive) onValueChange(valueRange.endInclusive)
        else onValueChange(it)
    },

    onValueRemove: (longClick: Boolean) -> Unit = {
        valueChange(value - (if (it) buttonLongSteps else buttonSteps))
    },
    onValueAdd: (longClick: Boolean) -> Unit = {
        valueChange(value + if (it) buttonLongSteps else buttonSteps)
    },

    a11yDescription: String = "",
    text: String,

    // ===== 尺寸默认值（用户 09-11 下午终裁：自定义观感不好，滑条回归官方 MD3 默认）=====
    // 撤自绘胶囊轨道/竖条 thumb（trackHeight/thumbSize 参数一并删除），Slider 直接用
    // SliderDefaults 官方默认画法（粗轨道+缺口+端点圆点+官方手柄，颜色随主题，零自定义）。
    // 标签走官方 bodySmall（原 labelFontSize 手调参数已撤，MD3 规范约定：字号只用 typography token）；
    // ± 键 48dp 触摸区/24dp 图标=官方尺寸。± 键是功能结构，用户拍板保留（长按快调不受影响）。
    // LayerSlider 不传尺寸，一处默认全 app 生效。
    buttonSize: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    // 竖条手柄高度（用户 09-11 晚终裁：官方 44dp 比粗轨道上下高出太多，降到 24dp）。
    // 官方 Slider 不暴露手柄尺寸参数，此处为最小自绘（仅尺寸；形状圆角/颜色随主题沿用官方画法），
    // 全 app 一处默认统一。轨道仍走官方 SliderDefaults 粗轨道，不自定义。
    thumbHeight: Dp = 24.dp,
    // 标签列最小宽度（0.dp = 不定宽，随文字收缩，即旧行为）。
    // 用途：同一组滑杆的多行标签字数不同（发音人 3 字 / 插件 2 字），不定宽会让各行的
    // −按钮与轨道左右错开；传 44dp 可让"标签+末尾 8dp 间距"一律 ≥52dp，各行左边界对齐。
    labelMinWidth: Dp = 0.dp,
) {
    // 单行式布局（09-07 用户定稿，参考 JRead 图2）：左列竖排[标签/数值]小字，
    // 右侧 −/滑杆/＋ 同行。text 按首个全角/半角冒号拆分为「标签」「数值」两部分，
    // 无冒号则整串作为标签。保留：长按 −/＋ 快调、step 吸附、无障碍进度语义。
    val updatedValue = rememberUpdatedState(value)
    val view = LocalView.current
    val sliderSteps =
        if (step > 0f) max(0, ((valueRange.endInclusive - valueRange.start) / step).toInt())
        else steps
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(value) {
        if (first) {
            first = false
            return@LaunchedEffect
        }

        view.announceForAccessibility(a11yDescription)
    }

    val sepIdx = remember(text) { text.indexOfFirst { it == '：' || it == ':' } }
    val labelPart = if (sepIdx > 0) text.substring(0, sepIdx) else text
    val valuePart = if (sepIdx > 0) text.substring(sepIdx + 1).trim() else ""

    Row(
        modifier,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Column(Modifier.padding(end = 8.dp).widthIn(min = labelMinWidth)) {
            Text(
                text = labelPart,
                // 标签/数值 14sp（用户 09-11 晚终裁：12sp 偏小，升一档，全 app 滑杆统一）
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
            if (valuePart.isNotEmpty())
                Text(
                    text = valuePart,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
        }

        if (showButton)
            LongClickIconButton(
                modifier = Modifier
                    .size(buttonSize)
                    .semantics {
                        contentDescription = a11yDescription
                    },
                enabled = value > valueRange.start,
                onClick = { onValueRemove(false) },
                onLongClick = { onValueRemove(true) }
            ) {
                Icon(
                        Icons.Default.Remove,
                        stringResource(id = R.string.desc_seekbar_remove),
                        modifier = Modifier.size(iconSize),
                    )
            }

        Box(
            Modifier
                .weight(1f)
                .clearAndSetSemantics {
                    focused = true
                    if (!enabled) disabled()

                    stateDescription = a11yDescription
                    contentDescription = a11yDescription

                    progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange, sliderSteps)
                    setProgress {
                        onValueChange(it)
                        true
                    }
                },
        ) {
            Slider(
                modifier = Modifier.fillMaxWidth(),
                value = value,
                onValueChange = {
                    val snapped = if (step > 0f) {
                        val stepsCount = ((it - valueRange.start) / step).roundToInt()
                        (valueRange.start + stepsCount * step).coerceIn(
                            valueRange.start,
                            valueRange.endInclusive
                        )
                    } else it
                    onValueChange(snapped)

                    if (snapped == valueRange.start || snapped == valueRange.endInclusive)
                        view.performLongPress()
                },
                enabled = enabled,
                valueRange = valueRange,
                steps = 0,
                onValueChangeFinished = onValueChangeFinished,
                // 官方 MD3 默认（用户 09-11 下午终裁）：轨道不传，走 SliderDefaults 粗轨道。
                // 手柄竖条高度降为 24dp（用户 09-11 晚终裁：官方 44dp 过高）；形状/颜色沿用官方画法
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(width = 4.dp, height = thumbHeight)
                            .background(
                                if (enabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                RoundedCornerShape(2.dp)
                            )
                    )
                },
            )
        }

        if (showButton) {
            LongClickIconButton(
                modifier = Modifier
                    .size(buttonSize)
                    .semantics {
                        contentDescription = a11yDescription
                    },
                enabled = value < valueRange.endInclusive,
                onClick = { onValueAdd(false) },
                onLongClick = { onValueAdd(true) }
            ) {
                Icon(
                        Icons.Default.Add,
                        stringResource(id = R.string.desc_seekbar_add),
                        modifier = Modifier.size(iconSize),
                    )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LabelSlider(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,

    showButton: Boolean = true,
    buttonSteps: Float = 0.01f,
    buttonLongSteps: Float = 0.1f,

    step: Float = 0f,

    valueChange: (Float) -> Unit = {
        if (it < valueRange.start) onValueChange(valueRange.start)
        else if (it > valueRange.endInclusive) onValueChange(valueRange.endInclusive)
        else onValueChange(it)
    },

    onValueRemove: (longClick: Boolean) -> Unit = {
        valueChange(value - (if (it) buttonLongSteps else buttonSteps))
    },
    onValueAdd: (longClick: Boolean) -> Unit = {
        valueChange(value + if (it) buttonLongSteps else buttonSteps)
    },

    a11yDescription: String = "",
    text: @Composable BoxScope.() -> Unit,
) {

    val updatedValue = rememberUpdatedState(value)
    val view = LocalView.current
    val sliderSteps =
        if (step > 0f) max(0, ((valueRange.endInclusive - valueRange.start) / step).toInt())
        else steps
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(value) {
        if (first) {
            first = false
            return@LaunchedEffect
        }

        view.announceForAccessibility(a11yDescription)
    }
    Box(modifier) {
        Row(Modifier, verticalAlignment = Alignment.Bottom) {
            if (showButton)
                LongClickIconButton(
                    modifier = Modifier
                    .size(32.dp)
                        .semantics {
                            contentDescription = a11yDescription
                        },
                    enabled = value > valueRange.start,
                    onClick = { onValueRemove(false) },
                    onLongClick = { onValueRemove(true) }
                ) {
                    Icon(
                        Icons.Default.Remove,
                        stringResource(id = R.string.desc_seekbar_remove),
                        modifier = Modifier.size(20.dp),
                    )
                }

            Column(
                Modifier
                    .weight(1f)
                    .clearAndSetSemantics {
                        focused = true
                        if (!enabled) disabled()

                        stateDescription = a11yDescription
                        contentDescription = a11yDescription

                        progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange, sliderSteps)
                        setProgress {
                            onValueChange(it)
                            true
                        }

                    },

                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.offset(y = (8).dp)) {
                    text()
                }
                Slider(
                    modifier = Modifier
                    .size(32.dp)
                        .padding(horizontal = 8.dp)
                        .semantics { invisibleToUser() },
                    value = value,
                    onValueChange = {
                        val snapped = if (step > 0f) {
                            val stepsCount = ((it - valueRange.start) / step).roundToInt()
                            (valueRange.start + stepsCount * step).coerceIn(
                                valueRange.start,
                                valueRange.endInclusive
                            )
                        } else it
                        onValueChange(snapped)

                        if (snapped == valueRange.start || snapped == valueRange.endInclusive)
                            view.performLongPress()
                    },
                    enabled = enabled,
                    valueRange = valueRange,
                    steps = 0,
                    onValueChangeFinished = onValueChangeFinished,
                    // 手柄竖条同样降为 24dp（用户 09-11 晚终裁，与主重载统一，全 app 一致）
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(width = 4.dp, height = 24.dp)
                                .background(
                                    if (enabled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    },
                )
            }

            if (showButton) {
                LongClickIconButton(
                    modifier = Modifier
                    .size(32.dp)
                        .semantics {
                            contentDescription = a11yDescription
                        },
                    enabled = value < valueRange.endInclusive,
                    onClick = { onValueAdd(false) },
                    onLongClick = { onValueAdd(true) }
                ) {
                    Icon(
                        Icons.Default.Add,
                        stringResource(id = R.string.desc_seekbar_add),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewSlider() {
    var value by remember { mutableFloatStateOf(0f) }
    val str = "语速: $value"
    LabelSlider(
        value = value,
        onValueChange = { value = it },
        valueRange = 0.1f..3.0f,
        a11yDescription = str,
        buttonSteps = 0.01f,
    ) {
        Text(str)
    }
}