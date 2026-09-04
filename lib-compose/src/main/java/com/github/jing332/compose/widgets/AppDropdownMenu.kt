package com.github.jing332.compose.widgets

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** 菜单容器淡青（用户比选定稿 #EEF4F4）；固定值不随主题，冷调与绿系页面拉开色相 */
val MenuCyan = Color(0xEEF4F4)

/**
 * 全项目统一的下拉菜单：容器色固定淡青 #EEF4F4（用户五方向比选定稿）。
 * M3 1.4.0-alpha09 的 DropdownMenu 用独立 containerColor 参数（旧版 MenuColors 已移除），
 * 默认 surfaceContainerHighest（17%，全主题最深档）过深；
 * 灰绿6%(tint插值)被否("闷闷的")，亮绿25%被否；冷调淡青与绿系页面形成唯一清爽对比浮层。
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
        containerColor = MenuCyan,
        content = content,
    )
}
