package com.github.jing332.tts_server_android.compose.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
//    drawerState: DrawerState = LocalDrawerState.current,
    // 返回/导航图标（可空=不占位）。09-14 从密钥弹窗改独立页面时启用
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    // 顶栏底色=页面底色（定案）：原用 surface，但 background 与 surface 是两个值
    // （如绿主题 FBFDF8 vs F8FAF5），顶栏和内容区之间断出一层色差；一律 background 归平。
    // scrolledContainerColor 同值：滚动时不加深（用户此前已明确不要滚动变色）
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.background,
        scrolledContainerColor = MaterialTheme.colorScheme.background,
    ),
    // 保留参数兼容旧调用，但不再消费：滚动变色已被 colors 定死为 background
    scrollBehavior: TopAppBarScrollBehavior? = null,
    // 标题槽左缩进（10-06 密钥页独立 tab：用户嫌标题离返回键远，收紧到 4）。
    // 默认 12 = 既有口径原样；只有显式传入的页面变，其他页面零影响
    titleStartPadding: Dp = 12.dp,
) {
    // 自绘顶栏（拍板 56dp）：M3 TopAppBar 内部布局写死 64dp
    // （heightFrom(TopAppBarHeight)），外面套 height(56) 只会把标题裁掉半截
    // （09-15 实机实锤「系统TTS」字被切），M3 又不开放内容高度参数 → 照 M3
    // 排版自绘：状态栏 insets 单独占位 + 56dp 内容行，标题样式沿用 titleLarge。
    Surface(color = colors.containerColor, modifier = modifier.fillMaxWidth()) {
        Column {
            Spacer(Modifier.windowInsetsTopHeight(windowInsets))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    // 左 4 = 返回键热区内缩、字形视觉 ≈16 与内容页 16 左线齐；
                    // 右 8 = ⋮ 字形与全站「30dp 列」同列（10-05 用户实机指认：顶栏 ⋮ 与下方列表行
                    // 没对齐）。算式：行内距 8 + ⋮ 字形在 48dp 热区内的居中偏移 22 = 字形右缘距屏 30。
                    // 曾为 end=0：最右键热区贴边，⋮ 字形落在 22dp、其下拉菜单右缘也贴到屏幕边（0dp），
                    // 与列表/卡片 ⋮ 菜单的 8dp 留白不一致（用户 10-05「菜单右边距是 0，给统一下」）。
                    .padding(start = 4.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // navigationIcon 与 M3 同签名为无参 lambda（不吃 RowScope），直接 invoke
                navigationIcon?.invoke()
                Box(
                    // 标题槽 start 12（10-05 用户：顶栏文字/图标左右边距审计——原 8 让标题
                    // 文字落在 4+8=12dp，偏在返回键字形线 16 与卡内容线之外；改 12 后
                    // 无返回键时标题文字=16dp 内容线，与列表正文同一条竖线）
                    // titleStartPadding 可由页面覆写（10-06 密钥页传 4 收紧与返回键的距离）
                    Modifier.weight(1f).padding(start = titleStartPadding, end = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // M3 TopAppBar 的标题默认吃 titleLarge，自绘后手动补上，
                    // 各页面标题字号与原生档一致
                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.titleLarge
                    ) { title() }
                }
                // actions 必须自成一条**宽度随内容**的 Row：DropdownMenu 的锚点是它的父布局节点，
                // 直接挂在上面那条 fillMaxWidth 的行上，菜单会锚到整行左缘（弹出到屏幕左侧）；
                // 套一层随内容收缩的 Row 后，锚点落在右侧动作区，与 M3 TopAppBar 行为一致
                Row(verticalAlignment = Alignment.CenterVertically) { actions() }
            }
        }
    }
}
