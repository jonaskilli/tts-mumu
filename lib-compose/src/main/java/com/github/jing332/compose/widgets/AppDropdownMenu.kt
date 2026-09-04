package com.github.jing332.compose.widgets

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 全项目统一的下拉菜单（统一入口，便于以后一处调色）。
 * 容器色走 M3 DropdownMenu 默认（tonalElevation 派生）——返璞归真批次：
 * 灰绿6%插值/亮绿25%/淡青固定值三轮试验均被否，回归组件默认。
 * 16 处调用点（分组/卡片/搜索/设置等）全部经此组件。
 */
@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        content = content,
    )
}
