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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.DpSize
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
        Column(Modifier.padding(end = 8.dp)) {
            Text(
                text = labelPart,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
            if (valuePart.isNotEmpty())
                Text(
                    text = valuePart,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
        }

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
                        modifier = Modifier.size(18.dp),
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
                        thumbSize = DpSize(3.dp, 18.dp)
                    )
                },
                track = { sliderState ->
                    // 自绘细轨道 2.5dp（用户 09-07：滑杆调细、整体和谐）
                    val colors = SliderDefaults.colors()
                    val frac = if (sliderState.valueRange.endInclusive > sliderState.valueRange.start)
                        ((sliderState.value - sliderState.valueRange.start) /
                            (sliderState.valueRange.endInclusive - sliderState.valueRange.start))
                            .coerceIn(0f, 1f)
                    else 0f
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                    ) {
                        Box(Modifier.fillMaxSize().background(colors.inactiveTrackColor))
                        Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(colors.activeTrackColor))
                    }
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
                        modifier = Modifier.size(18.dp),
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
                        modifier = Modifier.size(18.dp),
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
                        modifier = Modifier.size(18.dp),
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