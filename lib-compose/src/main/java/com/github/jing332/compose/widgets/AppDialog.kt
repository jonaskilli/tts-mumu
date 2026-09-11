package com.github.jing332.compose.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.github.jing332.compose.R
import kotlin.math.max

/**
 * 全 app 弹窗外壳（用户 09-11 终裁：回归 MD3 标准观感）。
 *
 * 历史：本组件曾是自绘外壳（白底+零海拔+居中 18sp 标题），是用户 09-03/09-04 反馈"弹窗过深/发绿"
 * 后的定制产物；09-11 用户对比两套壳后裁定回归 MD3 标准——组件**签名不变**、内部换成 material3
 * 原生 [AlertDialog]，29 个调用方一处改全部生效，观感/圆角/色调海拔/标题样式全部回到官方规范，
 * 零维护；git revert 本提交即可整体回退。
 *
 * 细节映射：
 * - title/text 走 AlertDialog 原生槽位，样式由 MD3 规范接管（headlineSmall 标题 / bodyMedium 正文）；
 * - content 原为 BoxScope 接收者，包一层 Box 保持调用方兼容；
 * - buttons 原样塞进 confirmButton 槽，外面包 [AppDialogFlowRow] 保留换行能力（原生 Row 不换行）；
 * - dialogContentPadding 默认改为 0：MD3 自带规范内边距（24dp），调用方无需再补；
 *   仅 AuditionDialog 显式传自定义值，行为不变；
 * - 曾有的"白底+零海拔防染绿"随 MD3 一起废弃：MD3 弹窗色调海拔 3dp，绿色混色肉眼无感
 *   （用户 09-11 核实确认，勿再引用"原生壳发绿"旧结论）。
 */
@Preview
@Composable
fun PreviewAppDialog() {
    var show by remember { mutableStateOf(true) }
    if (show) {
        AppDialog(title = {
            Text("Title")
        }, content = {
            Text("Content")
        }, buttons = {
            TextButton(onClick = {
                show = false
            }) {
                Text("Cancel")
            }
            TextButton(onClick = {
                show = false
            }) {
                Text("OK")
            }
        }, onDismissRequest = {
            show = false
        })
    }

}

@Composable
fun AppDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    title: @Composable () -> Unit,
    content: @Composable BoxScope.() -> Unit,
    dialogContentPadding: PaddingValues = PaddingValues(0.dp),
    buttons: @Composable BoxScope.() -> Unit = {
        TextButton(onClick = onDismissRequest) { Text(stringResource(id = R.string.close)) }
    },
) = AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismissRequest,
    properties = properties,
    title = { title() },
    text = {
        // content 的接收者是 BoxScope：包一层 Box 保持旧签名兼容（调用方可无视，语义不变）
        Box(
            Modifier
                .fillMaxWidth()
                .padding(dialogContentPadding)
        ) {
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
                content()
            }
        }
    },
    confirmButton = {
        // FlowRow 包一层保留按钮过多时的换行能力（原生按钮行不换行）；MD3 自动靠右排布
        AppDialogFlowRow(
            mainAxisSpacing = ButtonsMainAxisSpacing,
            crossAxisSpacing = ButtonsCrossAxisSpacing
        ) {
            buttons()
        }
    },
)


@Composable
internal fun AppDialogFlowRow(
    mainAxisSpacing: Dp, crossAxisSpacing: Dp, content: @Composable () -> Unit
) {
    Layout(content) { measurables, constraints ->
        val sequences = mutableListOf<List<Placeable>>()
        val crossAxisSizes = mutableListOf<Int>()
        val crossAxisPositions = mutableListOf<Int>()

        var mainAxisSpace = 0
        var crossAxisSpace = 0

        val currentSequence = mutableListOf<Placeable>()
        var currentMainAxisSize = 0
        var currentCrossAxisSize = 0

        // Return whether the placeable can be added to the current sequence.
        fun canAddToCurrentSequence(placeable: Placeable) =
            currentSequence.isEmpty() || currentMainAxisSize + mainAxisSpacing.roundToPx() + placeable.width <= constraints.maxWidth

        // Store current sequence information and start a new sequence.
        fun startNewSequence() {
            if (sequences.isNotEmpty()) {
                crossAxisSpace += crossAxisSpacing.roundToPx()
            }
            // Ensures that confirming actions appear above dismissive actions.
            sequences.add(0, currentSequence.toList())
            crossAxisSizes += currentCrossAxisSize
            crossAxisPositions += crossAxisSpace

            crossAxisSpace += currentCrossAxisSize
            mainAxisSpace = max(mainAxisSpace, currentMainAxisSize)

            currentSequence.clear()
            currentMainAxisSize = 0
            currentCrossAxisSize = 0
        }

        for (measurable in measurables) {
            // Ask the child for its preferred size.
            val placeable = measurable.measure(constraints)

            // Start a new sequence if there is not enough space.
            if (!canAddToCurrentSequence(placeable)) startNewSequence()

            // Add the child to the current sequence.
            if (currentSequence.isNotEmpty()) {
                currentMainAxisSize += mainAxisSpacing.roundToPx()
            }
            currentSequence.add(placeable)
            currentMainAxisSize += placeable.width
            currentCrossAxisSize = max(currentCrossAxisSize, placeable.height)
        }

        if (currentSequence.isNotEmpty()) startNewSequence()

        val mainAxisLayoutSize = max(mainAxisSpace, constraints.minWidth)

        val crossAxisLayoutSize = max(crossAxisSpace, constraints.minHeight)

        val layoutWidth = mainAxisLayoutSize

        val layoutHeight = crossAxisLayoutSize

        layout(layoutWidth, layoutHeight) {
            sequences.forEachIndexed { i, placeables ->
                val childrenMainAxisSizes = IntArray(placeables.size) { j ->
                    placeables[j].width + if (j < placeables.lastIndex) mainAxisSpacing.roundToPx() else 0
                }
                val arrangement = Arrangement.End
                val mainAxisPositions = IntArray(childrenMainAxisSizes.size) { 0 }
                with(arrangement) {
                    arrange(
                        mainAxisLayoutSize,
                        childrenMainAxisSizes,
                        layoutDirection,
                        mainAxisPositions
                    )
                }
                placeables.forEachIndexed { j, placeable ->
                    placeable.place(
                        x = mainAxisPositions[j], y = crossAxisPositions[i]
                    )
                }
            }
        }
    }
}

private val ButtonsMainAxisSpacing = 8.dp
private val ButtonsCrossAxisSpacing = 12.dp
