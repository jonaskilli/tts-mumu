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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
    val scrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior(canScroll = {
        !a11yTouchEnabled
    })
    ControlBottomBarVisibility(a11yTouchEnabled, scrollBehavior)
    // 底栏满高的像素值（10-10 回官方 NavigationBar=80dp）：布局式滚动隐藏用它算裁剪量
    val density = LocalDensity.current
    val bottomBarHeightPx = with(density) { 80.dp.toPx() }.roundToInt()

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
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bottomBarHeight({ scrollBehavior.state.heightOffset }, bottomBarHeightPx),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
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
                                    alwaysShowLabel = false
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
 * 底栏滚动隐藏 v2（布局式）：按 [heightOffset]（px，负=隐藏量）实时裁底栏高度——
 * heightRequired(60dp+offset)，内容随高度收走（Row 顶对齐、贴边裁掉底行），
 * Scaffold 槽位随高度同步缩小，滚动内容真的多出空间。
 * v1（offset 平移+clipToBounds）真机实锤失效：offset 不改布局高度、clip 先于 offset
 * 执行裁在原位，底栏悬在内容中间（用户截图 19:08）。
 */
private fun Modifier.bottomBarHeight(heightOffsetState: () -> Float, fullHeightPx: Int): Modifier =
    layout { measurable, constraints ->
        val hidden = (-heightOffsetState()).roundToInt().coerceIn(0, fullHeightPx)
        val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = fullHeightPx - hidden))
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
