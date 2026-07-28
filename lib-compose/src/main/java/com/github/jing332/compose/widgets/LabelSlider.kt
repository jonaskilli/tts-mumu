package com.github.jing332.compose.widgets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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

    text: String,
) {
    LabelSlider(
        modifier = modifier,
        enabled = enabled,
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        showButton = showButton,
        buttonSteps = buttonSteps,
        buttonLongSteps = buttonLongSteps,
        valueChange = valueChange,
        onValueRemove = onValueRemove,
        onValueAdd = onValueAdd,
        a11yDescription = text,
    ) {
        Text(text = text, modifier = Modifier.semantics { invisibleToUser() })
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
                        .semantics {
                            contentDescription = a11yDescription
                        },
                    enabled = value > valueRange.start,
                    onClick = { onValueRemove(false) },
                    onLongClick = { onValueRemove(true) }
                ) {
                    Icon(Icons.Default.Remove, stringResource(id = R.string.desc_seekbar_remove))
                }

            Column(
                Modifier
                    .weight(1f)
                    .clearAndSetSemantics {
                        focused = true
                        if (!enabled) disabled()

                        stateDescription = a11yDescription
                        contentDescription = a11yDescription

                        println("value: $value, valueRange: $valueRange, steps: $steps")
                        progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange, steps)
                        setProgress {
                            println("setProgress $it")
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
                        .padding(horizontal = 8.dp)
                        .semantics { invisibleToUser() },
                    value = value,
                    onValueChange = {
                        onValueChange(it)

                        if (it == valueRange.start || it == valueRange.endInclusive)
                            view.performLongPress()
                    },
                    enabled = enabled,
                    valueRange = valueRange,
                    steps = steps,
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
                        .semantics {
                            contentDescription = a11yDescription
                        },
                    enabled = value < valueRange.endInclusive,
                    onClick = { onValueAdd(false) },
                    onLongClick = { onValueAdd(true) }
                ) {
                    Icon(Icons.Default.Add, stringResource(id = R.string.desc_seekbar_add))
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
        buttonSteps = 0.1f,
    ) {
        Text(str)
    }
}