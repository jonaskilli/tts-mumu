package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.jing332.compose.widgets.LabelSlider

@Composable
fun FloatSlider(
    modifier: Modifier = Modifier,
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    step: Float = 0f,
    valueFormatter: (Float) -> String = { "%.2f".format(it) }
) {
    LabelSlider(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        step = step,
        text = "$label: ${valueFormatter(value)}",
        buttonSteps = 0.1f,
        buttonLongSteps = 0.5f
    )
}
