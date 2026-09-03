package com.github.jing332.compose.widgets

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp

/**
 * 全项目统一的下拉菜单：容器色固定 tint 6%（surface×surfaceTint 插值，与底栏同档）。
 * M3 1.4.0-alpha09 的 DropdownMenu 用独立 containerColor 参数（旧版 MenuColors 已移除），
 * 默认 surfaceContainerHighest（tint 17%，全主题最深档）过深；
 * surface(0%) 与页面底色融为一体，surfaceContainerLow(3%) 与分区面板同色，
 * 6% 浮于页面底(0%)与分区(3%)之上自成一层。16 处调用点（分组/卡片/搜索/设置等）全部经此组件，改色一处生效。
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
        containerColor = lerp(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceTint, 0.06f
        ),
        content = content,
    )
}
