package com.github.jing332.compose.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

/**
 * 可拖动的垂直滚动条：跟随 LazyListState 滚动位置显示滑块，并支持拖拽滚动列表。
 */
@Composable
fun DraggableVerticalScrollbar(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    thumbColor: Color = Color(0xFF888888).copy(alpha = 0.6f),
    thumbWidth: Float = 6f,
    minThumbHeight: Float = 32f,
) {
    val scope = rememberCoroutineScope()

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                val layoutInfo = listState.layoutInfo
                val viewportHeight = layoutInfo.viewportSize.height.toFloat()
                val visibleItemsInfo = layoutInfo.visibleItemsInfo

                if (visibleItemsInfo.isNotEmpty() && viewportHeight > 0f) {
                    val avgItemHeight =
                        visibleItemsInfo.sumOf { it.size }.toFloat() / visibleItemsInfo.size
                    val totalItems = layoutInfo.totalItemsCount
                    val totalContentHeight = totalItems * avgItemHeight
                    val scrollRange = (totalContentHeight - viewportHeight).coerceAtLeast(0f)

                    val trackHeight = size.height.toFloat()
                    val thumbHeight = (viewportHeight / totalContentHeight * trackHeight)
                        .coerceIn(minThumbHeight, trackHeight)
                    val draggableRange = (trackHeight - thumbHeight).coerceAtLeast(0f)

                    if (draggableRange > 0f && scrollRange > 0f) {
                        val scrollPerPixel = scrollRange / draggableRange
                        val scrollDelta = dragAmount.y * scrollPerPixel
                        scope.launch {
                            listState.scrollBy(scrollDelta)
                        }
                    }
                }
            }
        }
    ) {
        val layoutInfo = listState.layoutInfo
        val viewportHeight = layoutInfo.viewportSize.height.toFloat()
        val visibleItemsInfo = layoutInfo.visibleItemsInfo

        if (visibleItemsInfo.isNotEmpty() && viewportHeight > 0f) {
            val avgItemHeight =
                visibleItemsInfo.sumOf { it.size }.toFloat() / visibleItemsInfo.size
            val totalItems = layoutInfo.totalItemsCount
            val totalContentHeight = totalItems * avgItemHeight

            if (totalContentHeight > viewportHeight) {
                val trackHeight = size.height
                val thumbHeight = (viewportHeight / totalContentHeight * trackHeight)
                    .coerceIn(minThumbHeight, trackHeight)

                val firstVisible = visibleItemsInfo.first()
                val scrollOffset =
                    firstVisible.index * avgItemHeight - firstVisible.offset.toFloat()
                val scrollRange = totalContentHeight - viewportHeight
                val scrollProgress = (scrollOffset / scrollRange).coerceIn(0f, 1f)
                val thumbY = scrollProgress * (trackHeight - thumbHeight)

                drawRoundRect(
                    color = thumbColor,
                    topLeft = Offset(x = size.width - thumbWidth - 2f, y = thumbY),
                    size = Size(width = thumbWidth, height = thumbHeight),
                    cornerRadius = CornerRadius(thumbWidth / 2f, thumbWidth / 2f)
                )
            }
        }
    }
}
