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
    // 顶栏底色=页面底色（目目 09-15 定案）：原用 surface，但 background 与 surface 是两个值
    // （如绿主题 FBFDF8 vs F8FAF5），顶栏和内容区之间断出一层色差；一律 background 归平。
    // scrolledContainerColor 同值：滚动时不加深（用户此前已明确不要滚动变色）
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.background,
        scrolledContainerColor = MaterialTheme.colorScheme.background,
    ),
    // 保留参数兼容旧调用，但不再消费：滚动变色已被 colors 定死为 background
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    // 自绘顶栏（目目 09-15 晚拍板 56dp）：M3 TopAppBar 内部布局写死 64dp
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
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                navigationIcon?.invoke(this)
                Box(
                    Modifier.weight(1f).padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // M3 TopAppBar 的标题默认吃 titleLarge，自绘后手动补上，
                    // 各页面标题字号与原生档一致
                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.titleLarge
                    ) { title() }
                }
                actions()
            }
        }
    }
}
