package com.github.jing332.compose.widgets

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 全项目统一的下拉菜单：容器色固定 surface（纯页面底色，用户指定要最浅）。
 * M3 1.4.0-alpha09 的 DropdownMenu 用独立 containerColor 参数（旧版 MenuColors 已移除），
 * 默认 surfaceContainerHighest（tint 17%，全主题最深档）过深；
 * 浮层层次由自带阴影保证。16 处调用点（分组/卡片/搜索/设置等）全部经此组件，改色一处生效。
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
        containerColor = MaterialTheme.colorScheme.surface,
        content = content,
    )
}
