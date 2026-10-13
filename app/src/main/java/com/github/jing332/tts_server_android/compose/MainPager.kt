package com.github.jing332.tts_server_android.compose

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.rememberBottomAppBarState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.github.jing332.compose.widgets.ControlBottomBarVisibility
import com.github.jing332.compose.widgets.rememberA11TouchEnabled
import com.github.jing332.tts_server_android.compose.settings.SettingsScreen
import com.github.jing332.tts_server_android.compose.theme.softStructureColor
import com.github.jing332.tts_server_android.compose.systts.MigrationTips
import com.github.jing332.tts_server_android.compose.systts.TtsLogScreen
import com.github.jing332.tts_server_android.compose.systts.list.ListManagerScreen
import com.github.jing332.tts_server_android.compose.RoleManagementScreen
import com.github.jing332.tts_server_android.conf.AppConfig
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
val LocalBottomBarBehavior =
    compositionLocalOf<BottomAppBarScrollBehavior>() { error("LocalBottomBarBehavior not initialized") }
val LocalOverlayController =
    compositionLocalOf<OverlayController> { error("LocalOverlayController not initialized") }

@OptIn(
    ExperimentalFoundationApi::class, ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun AnimatedContentScope.MainPager(sharedVM: SharedViewModel) {
    val pagerState =
        rememberPagerState(
            initialPage = AppConfig.fragmentIndex.value
                .coerceAtMost(PagerDestination.routes.size - 1)
        ) { PagerDestination.routes.size }
    DisposableEffect(pagerState) {
        onDispose {
            AppConfig.fragmentIndex.value = pagerState.currentPage
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    MigrationTips()

    val a11yTouchEnabled = rememberA11TouchEnabled()
    val density = LocalDensity.current
    // 底栏满高 = 官方 NavigationBar 内容 80dp + 手势条 inset（官方组件自带 navigationBarsPadding）。
    // ⚠️ 三修定稿（10-12 装机二轮回滚/到底不出现）：前两版用 onSizeChanged 实测——但底栏一旦
    // 开始隐藏，外层 bottomBarHeight 就把可用高度裁到 (满高−已藏)，量到的高度跟着缩水 ⇒
    // 「越藏越藏不动、回滚也算不对」的反馈环（挂 Surface 或挂 NavigationBar 都不行，两者同受约束）。
    // 改为确定性计算：WindowInsets.navigationBars 取手势条高度，不加任何测量依赖
    val navBarInsetPx = WindowInsets.navigationBars.getBottom(density)
    val bottomBarHeightPx = with(density) { 80.dp.roundToPx() } + navBarInsetPx
    // 自建 state 把 heightOffsetLimit 设为**满高**（默认 exitAlways 的 limit=48dp——BottomAppBarDefaults
    // 反编译实锤，首参可传自建 state 且 limit 可写）：limit=满高后隐藏/回弹/出现全走官方原生逻辑
    val bottomAppBarState = rememberBottomAppBarState(
        initialHeightOffsetLimit = -bottomBarHeightPx.toFloat(),
        initialHeightOffset = 0f,
        initialContentOffset = 0f
    )
    androidx.compose.runtime.LaunchedEffect(bottomBarHeightPx) {
        // 手势条高度变化（横竖屏/三键导航切换）时同步 limit
        bottomAppBarState.heightOffsetLimit = -bottomBarHeightPx.toFloat()
    }
    val scrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior(
        state = bottomAppBarState,
        canScroll = { !a11yTouchEnabled }
    )
    ControlBottomBarVisibility(a11yTouchEnabled, scrollBehavior)

    val overlayController = rememberOverlayController()

    CompositionLocalProvider(
        LocalBottomBarBehavior provides scrollBehavior,
        LocalOverlayController provides overlayController,
    ) {
        Box(Modifier.fillMaxSize()) {
            val backgroundColor by animateColorAsState(
                targetValue = if (overlayController.visible) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                } else {
                    Color.Transparent
                },
                animationSpec = tween(durationMillis = 600, easing = LinearEasing),
                label = "background color animation"
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .then(
                        if (overlayController.visible) {
                            Modifier.clickable(
                                interactionSource = null,
                                indication = null,
                                onClick = { overlayController.hide() }
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {}

            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                bottomBar = {
                    // 10-10 用户拍板「回 M3 官方 NavigationBar」：自绘微信式 60dp 退役——
                    // 反正滚动时底栏会藏起来（浏览态不占屏），展开态用官方 80dp 胶囊式
                    // 白得全套官方视觉/热区/无障碍（选中项图标包 secondaryContainer 胶囊+文字，
                    // 未选中项只图标——官方原味）。滚动隐藏 v2 布局式裁剪继续适用
                    // （bottomBarHeight 裁的是整体高度，官方 NavigationBar 当内容装进去即可）。
                    // 手势条区域官方组件自带 navigationBarsPadding 处理，不再自拼 Spacer。
                    // 10-13 晚用户令「底栏也按方案一」：softStructureColor()（Container 往页底混六成），
                    // 比卡底更贴页底；顶缘 1dp outlineVariant 浅线压住「底栏与页底几乎齐平后没分界」。
                    // 线走 overlay 画（不占布局），80dp+inset 的滚动隐藏量法不动；深浅色同口径。
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bottomBarHeight({ scrollBehavior.state.heightOffset }, bottomBarHeightPx),
                        color = softStructureColor()
                    ) {
                        Box {
                        NavigationBar(
                            containerColor = Color.Transparent // 色由外层 Surface 统一给（含手势条区）
                        ) {
                            for (destination in PagerDestination.routes) {
                                val isSelected = pagerState.currentPage == destination.index
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        scope.launch {
                                            pagerState.animateScrollToPage(destination.index)
                                        }
                                    },
                                    icon = { Box(Modifier.size(24.dp)) { destination.icon() } },
                                    label = { Text(stringResource(destination.strId), maxLines = 1) },
                                    // 官方默认 always；四项文字常显与否由官方形态接管（只亮选中项）
                                    alwaysShowLabel = false,
                                    // 10-10 用户令：去掉选中项的 secondaryContainer 胶囊（active indicator）——
                                    // 该胶囊虽是 M3 官方 NavigationBar 规范形态（02c0cca 回官方组件时带来），
                                    // 用户不喜欢；选中态改由官方默认的图标/文字变色（primary vs onSurfaceVariant）承担
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = Color.Transparent,
                                    ),
                                )
                            }
                            // 浅分隔线：overlay 叠在底栏顶缘，不参与布局，滚动隐藏量法不受影响
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                        }
                    }
                }
            ) { paddingValues ->
                val bottomPad = paddingValues.calculateBottomPadding()
                HorizontalPager(
                    modifier = Modifier
                        // 系统TTS列表页不收缩视口（其列表用 contentPadding 自行避让底栏，
                        // 展开项可填满整屏不被"白条"截断）；其余页维持外层收缩
                        .padding(bottom = if (pagerState.currentPage == PagerDestination.SystemTts.index) 0.dp else bottomPad)
                        .fillMaxSize(),
                    state = pagerState,
                    // 恢复左右滑动手势
                    userScrollEnabled = true,
                    // 保留相邻1页状态，避免角色管理栏UI被销毁重建导致状态丢失
                    beyondViewportPageCount = 1,
                ) { index ->
                    when (index) {
                        PagerDestination.SystemTts.index -> ListManagerScreen(sharedVM, listBottomPadding = bottomPad)
                        PagerDestination.Tool.index -> RoleManagementScreen(sharedVM, pagerState)
                        PagerDestination.SystemTtsLog.index -> TtsLogScreen()
                        PagerDestination.Settings.index -> SettingsScreen()
                    }
                }
            }
        }
    }
}

/**
 * 底栏滚动隐藏（布局式裁剪，v4 简化）：按 [heightOffset]（px，负=隐藏量）实时裁底栏高度，
 * Scaffold 槽位随高度同步缩小，滚动内容真的多出空间。
 * 隐藏上限已改在 state 上（自建 BottomAppBarState，limit=实测满高，见上方注释）——
 * offset 天然能走到全藏，本函数回归纯裁剪，不再做任何映射/兜底换算。
 */
private fun Modifier.bottomBarHeight(heightOffsetState: () -> Float, fullHeightPx: Int): Modifier =
    layout { measurable, constraints ->
        val hidden = (-heightOffsetState()).roundToInt().coerceIn(0, fullHeightPx)
        val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = (fullHeightPx - hidden).coerceAtLeast(0)))
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
