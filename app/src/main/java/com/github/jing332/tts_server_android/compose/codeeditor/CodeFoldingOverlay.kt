package com.github.jing332.tts_server_android.compose.codeeditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.rosemoe.sora.widget.CodeEditor
import kotlin.math.max
import kotlin.math.min

/**
 * 代码折叠按钮 overlay 层
 *
 * 只覆盖编辑器行号区（gutter，左侧固定宽度），不挡代码区域，不影响光标和滚动。
 *  - ▶ 右指三角：可折叠但未折叠，点击折叠
 *  - ▼ 下指三角：已折叠，点击展开
 *
 * 通过 sora-editor 的 layout.getCharLayoutOffset / offsetY / rowHeight 计算 Y 坐标，
 * 自动兼容 wordwrap 与非 wordwrap 模式。
 */
@Composable
fun CodeFoldingOverlay(
    editor: CodeEditor,
    foldingManager: CodeFoldingManager,
    modifier: Modifier = Modifier,
) {
    // 触发重绘的计数器（滚动/文本变化时递增）
    var redrawTrigger by remember { mutableIntStateOf(0) }

    // 轮询编辑器滚动 offset 和行数变化，触发重绘
    LaunchedEffect(editor) {
        var lastOffsetY = -1
        var lastLineCount = -1
        while (true) {
            val curOffsetY = editor.offsetY
            val curLineCount = editor.text.lineCount
            if (curOffsetY != lastOffsetY || curLineCount != lastLineCount) {
                lastOffsetY = curOffsetY
                lastLineCount = curLineCount
                redrawTrigger++
            }
            kotlinx.coroutines.delay(50)
        }
    }

    // 可折叠按钮：橙色醒目；已折叠按钮：蓝色；折叠行背景：半透明蓝
    val primaryColor = Color(0xFFFF6B35)
    val accentColor = Color(0xFF4A90D9)
    val foldedBgColor = Color(0x404A90D9)

    // gutter 固定宽度 56dp，overlay 只占这一条，不挡代码区
    val gutterWidthDp = 56.dp

    // overlay 在自定义 Layout 内部测量，其高度由 Layout 传入的 constraints 决定，
    // 不会影响外部布局高度（外部高度由 CodeEditor 的 EXACTLY 约束决定）。
    // fillMaxHeight 让 Canvas 使用 Layout 分配的高度来绘制，否则 Canvas 高度为0。
    Canvas(
        modifier = modifier
            .width(gutterWidthDp)
            .fillMaxHeight()
            .pointerInput(redrawTrigger) {
                detectTapGestures { offset ->
                    // 点击时 Y 坐标即 offset.y（overlay 内坐标，无需额外减偏移）
                    handleClick(editor, foldingManager, offset)
                }
            }
    ) {
        redrawTrigger // 读取以建立依赖

        drawFoldIndicators(editor, foldingManager, primaryColor, accentColor, foldedBgColor)
    }
}

/**
 * 绘制折叠指示器
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFoldIndicators(
    editor: CodeEditor,
    foldingManager: CodeFoldingManager,
    primaryColor: Color,
    accentColor: Color,
    foldedBgColor: Color,
) {
    val rowHeight = editor.rowHeight.toFloat()
    val offsetY = editor.offsetY.toFloat()
    val firstVisibleLine = max(0, editor.firstVisibleLine)
    val lastVisibleLine = min(editor.text.lineCount - 1, editor.lastVisibleLine)
    if (lastVisibleLine < firstVisibleLine) return

    val density = editor.resources.displayMetrics.density
    val gutterWidth = 56f * density
    // 按钮尺寸：从 0.18 增大到 0.32，更易点击
    val btnHalfSize = rowHeight * 0.32f

    // 已折叠区域：绘制折叠行背景 + ▶（点击展开，折叠后箭头向右）
    for (region in foldingManager.getRegions()) {
        val line = region.currentStart
        if (line < firstVisibleLine - 1 || line > lastVisibleLine + 1) continue
        val y = getLineY(editor, line) - offsetY
        if (y < -rowHeight || y > size.height) continue
        // 折叠行（首行+合并行+尾行）绘制半透明蓝背景
        drawRect(
            color = foldedBgColor,
            topLeft = Offset(0f, y),
            size = androidx.compose.ui.geometry.Size(size.width, rowHeight * 3)
        )
        drawTriangle(
            centerX = gutterWidth / 2f,
            centerY = y + rowHeight / 2f,
            halfSize = btnHalfSize,
            color = accentColor,
            pointingDown = false
        )
    }

    // 可折叠未折叠区域：绘制 ▼（点击折叠，未折叠时箭头向下）
    val blocks = foldingManager.findFoldableBlocks()
    for (block in blocks) {
        val line = block.first
        if (line < firstVisibleLine - 1 || line > lastVisibleLine + 1) continue
        if (foldingManager.isFolded(line)) continue
        val y = getLineY(editor, line) - offsetY
        if (y < -rowHeight || y > size.height) continue
        drawTriangle(
            centerX = gutterWidth / 2f,
            centerY = y + rowHeight / 2f,
            halfSize = btnHalfSize,
            color = primaryColor,
            pointingDown = true
        )
    }
}

/**
 * 获取指定逻辑行在内容坐标中的 Y（顶部）
 * 优先用 layout.getCharLayoutOffset（返回 float[]，[0]=字符底部），
 * 取该行第0列底部减去行高近似得到行顶部；失败则 fallback 用 rowHeight*line
 */
private fun getLineY(editor: CodeEditor, line: Int): Float {
    return try {
        val offsets = editor.layout.getCharLayoutOffset(line, 0)
        // [0] 是字符底部位置，减去行高得到行顶部
        offsets[0] - editor.rowHeight.toFloat()
    } catch (e: Exception) {
        (editor.rowHeight * line).toFloat()
    }
}

/**
 * 绘制三角形（▶ 右指 / ▼ 下指）
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTriangle(
    centerX: Float,
    centerY: Float,
    halfSize: Float,
    color: Color,
    pointingDown: Boolean,
) {
    val path = Path()
    if (pointingDown) {
        path.moveTo(centerX - halfSize, centerY - halfSize)
        path.lineTo(centerX + halfSize, centerY - halfSize)
        path.lineTo(centerX, centerY + halfSize)
    } else {
        path.moveTo(centerX - halfSize, centerY - halfSize)
        path.lineTo(centerX + halfSize, centerY)
        path.lineTo(centerX - halfSize, centerY + halfSize)
    }
    path.close()
    drawPath(path, color)
}

/**
 * 处理点击：判断是否点在某个折叠按钮上
 * offset 是 overlay 内坐标（overlay 只占 gutter 宽度，x 始终在 gutter 内）
 */
private fun handleClick(
    editor: CodeEditor,
    foldingManager: CodeFoldingManager,
    offset: Offset,
) {
    val rowHeight = editor.rowHeight.toFloat()
    val offsetY = editor.offsetY.toFloat()
    // overlay 顶部对齐编辑器顶部，offset.y + 滚动偏移 = 内容坐标
    val clickContentY = offset.y + offsetY
    val firstVisibleLine = max(0, editor.firstVisibleLine)
    val lastVisibleLine = min(editor.text.lineCount - 1, editor.lastVisibleLine)

    // 已折叠区域：点击展开
    // 关键：展开前记住滚动偏移，展开后立即恢复，避免 text.replace 导致屏幕跳到末尾再跳回
    for (region in foldingManager.getRegions()) {
        val line = region.currentStart
        if (line < firstVisibleLine - 1 || line > lastVisibleLine + 1) continue
        val y = getLineY(editor, line)
        if (clickContentY in y..(y + rowHeight)) {
            // 记住展开前的滚动偏移
            val savedOffsetY = editor.offsetY
            editor.setSelection(line, 0, true)
            foldingManager.unfoldAt(line)
            // 展开后 text.replace 会把光标和滚动都移到替换内容末尾，
            // 用 scroller 立即恢复滚动位置，保持屏幕视觉位置不变
            runCatching {
                val scroller = editor.scroller
                // startScroll(startX, startY, dx, dy, duration)
                // 从 savedOffsetY 开始 0 距离 0 时长滚动 = 瞬间设置滚动位置
                scroller.startScroll(0, savedOffsetY, 0, 0, 0)
                scroller.forceFinished(true)
            }
            // 光标定位到首行开头（不触发滚动）
            editor.setSelection(line, 0, true)
            editor.invalidate()
            return
        }
    }

    // 可折叠未折叠区域：点击折叠（先把光标移到该行，确保按钮生效）
    val blocks = foldingManager.findFoldableBlocks()
    for (block in blocks) {
        val line = block.first
        if (line < firstVisibleLine - 1 || line > lastVisibleLine + 1) continue
        if (foldingManager.isFolded(line)) continue
        val y = getLineY(editor, line)
        if (clickContentY in y..(y + rowHeight)) {
            editor.setSelection(line, 0, true)
            foldingManager.fold(block.first, block.last)
            return
        }
    }

    // 没命中折叠按钮：点击行号区，光标移到该行开头
    // 遍历可见行，找到点击 Y 对应的行
    for (line in firstVisibleLine..lastVisibleLine) {
        val y = getLineY(editor, line)
        if (clickContentY in y..(y + rowHeight)) {
            editor.setSelection(line, 0, true)
            return
        }
    }
    // 若点在可见行之外（顶部/底部留白），定位到最接近的可见行
    if (lastVisibleLine >= 0) {
        val targetLine = when {
            clickContentY < getLineY(editor, firstVisibleLine) -> firstVisibleLine
            else -> lastVisibleLine
        }
        editor.setSelection(targetLine, 0, true)
    }
}
