package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 树形缩进引导线（文件树同款）：在行左侧画 [depth] 条竖线，第 i 条位于 x = [startPaddingDp] + i×[stepDp] dp。
 * 须放在 padding 之前挂（drawBehind 覆盖行内边距区域），行与行之间视觉上连成贯穿子树的引线。
 * 配套内容缩进 = 8 + depth×12dp（见 SubGroupHeader / ListManagerScreen 调用处）。
 * 层次区分交给结构（线+缩进）而非底色，多层嵌套时每多一级多一条线，深度一目了然。
 */
fun Modifier.indentGuides(
    depth: Int,
    stepDp: Dp = 12.dp,
    startPaddingDp: Dp = 12.dp,
    color: Color,
    lineWidthDp: Dp = 1.dp,
): Modifier = if (depth <= 0) this else drawBehind {
    val step = stepDp.toPx()
    val x0 = startPaddingDp.toPx()
    val w = lineWidthDp.toPx()
    val top = 0f
    val bottom = size.height
    repeat(depth) { i ->
        val x = x0 + i * step + w / 2
        drawLine(
            color = color,
            start = Offset(x, top),
            end = Offset(x, bottom),
            strokeWidth = w,
            cap = StrokeCap.Butt,
        )
    }
}
