package com.github.jing332.compose.widgets

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 全项目统一的下拉菜单：容器色固定 surfaceContainerLow。
 * M3 DropdownMenu 默认用 surfaceContainerHighest（tint 17%，全主题最深档），
 * 用户反馈与底栏(surfaceContainerLow, tint 5%)不一致、观感过深；
 * 统一降到与底栏同一"容器深浅带"，浮层层次由自带阴影保证。
 * 16 处调用点（分组/卡片/搜索/设置等）全部经此组件，改色一处生效。
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
        colors = MenuDefaults.menuColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        content = content,
    )
}
