package com.github.jing332.compose.widgets

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset

/**
 * 全项目统一的下拉菜单（统一入口，便于以后一处调色）。
 * 容器色走 M3 DropdownMenu 默认（tonalElevation 派生）——返璞归真批次：
 * 灰绿6%插值/亮绿25%/淡青固定值三轮试验均被否，回归组件默认。
 * 16 处调用点（分组/卡片/搜索/设置等）全部经此组件。
 *
 * [offset]：菜单相对锚点的位移。默认 Zero（不位移，调用点原样）。
 * 已验证（material3 1.4.0-alpha09 MenuPosition.kt）：行尾 ⋮ 的宽菜单走
 * 「菜单右缘对齐锚点右缘」候选，offset 照加——即菜单右缘 = 锚点右缘 + offset.x。
 * 列表行/卡片 ⋮ 热区右缘本就距屏 8dp（10-05 三行 ⋮ 统一 30dp 列），菜单右缘
 * 天然留 8dp；顶栏 ⋮ 原为 end=0（菜单右缘贴屏 0dp，用户 10-05 实机指认「菜单右边距
 * 是 0」），同日 NavTopAppBar 改 end=8 后同样天然留 8dp。此参数仅为该类场景预留。
 */
@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        content = content,
    )
}
