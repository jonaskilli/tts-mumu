@file:OptIn(ExperimentalMaterial3Api::class)
package com.github.jing332.compose.widgets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // ===== 尺寸默认值（用户 09-11 终裁：全 app 滑条统一为音频参数同款）=====
    // 09-10 曾定「默认值保持中间档、只让音频参数经 LayerSlider 传大一号」；09-11 用户要求
    // 「其他地方用到滑条的也全统一（设置/背景音乐/内播/批量等）」，规则反转——
    // 默认值即统一值：胶囊轨道 10dp + 竖条 thumb 4×22 + ± 48dp 触摸区/22dp 图标 + 标签 12sp。
    // LayerSlider 不再传尺寸，一处默认全 app 生效。
    labelFontSize: TextUnit = 12.sp,
    buttonSize: Dp = 48.dp,
    iconSize: Dp = 22.dp,
    trackHeight: Dp = 10.dp,
    thumbSize: DpSize = DpSize(4.dp, 22.dp),
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
                style = MaterialTheme.typography.bodySmall.copy(fontSize = labelFontSize),
                maxLines = 1,
            )
            if (valuePart.isNotEmpty())
                Text(
                    text = valuePart,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = labelFontSize),
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
                thumb = {
                    SliderDefaults.Thumb(
                        interactionSource = remember { MutableInteractionSource() },
                        colors = SliderDefaults.colors(),
                        enabled = enabled,
                        thumbSize = thumbSize
                    )
                },
                track = { sliderState ->
                    // 胶囊轨道（用户 09-11 定稿：回最早 M3 观感，高度由 trackHeight 驱动收细一档）：
                    // 圆角胶囊轨道 + 未选中段尾端小圆点（M3 stop indicator 观感），
                    // thumb 仍为竖条（thumbSize 参数）；09-10 晚曾改纯细线画法，被用户否掉（显细又不美观）
                    val colors = SliderDefaults.colors()
                    val frac = if (sliderState.valueRange.endInclusive > sliderState.valueRange.start)
                        ((sliderState.value - sliderState.valueRange.start) /
                            (sliderState.valueRange.endInclusive - sliderState.valueRange.start))
                            .coerceIn(0f, 1f)
                    else 0f
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(trackHeight)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(trackHeight / 2))
                    ) {
                        Box(Modifier.fillMaxSize().background(colors.inactiveTrackColor))
                        Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(colors.activeTrackColor))
                        Box(
                            Modifier
                                .align(androidx.compose.ui.Alignment.CenterEnd)
                                .padding(end = trackHeight / 4)
                                .size(trackHeight / 2)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
                        )
                    }
                }
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
                    thumb = {
                        SliderDefaults.Thumb(
                            interactionSource = remember { MutableInteractionSource() },
                            colors = SliderDefaults.colors(),
                            enabled = enabled,
                            thumbSize = DpSize(4.dp, 24.dp)
                        )
                    }
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