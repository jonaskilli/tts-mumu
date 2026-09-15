package com.github.jing332.tts_server_android.compose.nav

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
//    drawerState: DrawerState = LocalDrawerState.current,
    // 返回/导航图标（可空=不占位）。09-14 从密钥弹窗改独立页面时启用，
    // 内部套一层 Surface 会改变顶栏高度，故直接透传给 M3 TopAppBar
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
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val scope = rememberCoroutineScope()
    TopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon ?: {},
        actions = actions,
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior
    )
}