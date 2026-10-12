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
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
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
    // 底栏满高的像素值：官方 NavigationBar 内容 80dp + 自带 navigationBarsPadding（手势条）。
    // ⚠️ 旧值写死 80dp 漏了手势条 inset——带手势条的机型上真实满高=80dp+导航条高度，
    // 于是①隐藏上限不够（永远剩导航条那么高收不回去=「卡一半」）②内容避让同错
    // （paddingValues.calculateBottomPadding() 短一截=列表末尾被栏压住看不全）。
    // 10-12 用户实机复现，改动态量测：包一层 Box 记下实际量到的高度（首帧后生效）
    val density = LocalDensity.current
    // 实测满高（含手势条 inset）：旧值写死 80dp 漏了手势条——带手势条机型上「卡一半」的根因之一。
    // 初始给估算值，onSizeChanged 量到真实值即替换（首帧后生效）
    val bottomBarHeightState = remember { mutableStateOf(with(density) { 128.dp.roundToPx() }) }

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
                            .onSizeChanged { size ->
                                // 实测满高（含手势条）：比写死常量准。变化才写，避免每帧重组
                                if (size.height > 0 && bottomBarHeightState.intValue != size.height) {
                                    bottomBarHeightState.intValue = size.height
                                }
                            }
                            .bottomBarHeight({ scrollBehavior.state.heightOffset }, bottomBarHeightState.intValue),
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
                                    alwaysShowLabel = false,
                                    // 10-10 用户令：去掉选中项的 secondaryContainer 胶囊（active indicator）——
                                    // 该胶囊虽是 M3 官方 NavigationBar 规范形态（02c0cca 回官方组件时带来），
                                    // 用户不喜欢；选中态改由官方默认的图标/文字变色（primary vs onSurfaceVariant）承担
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = Color.Transparent,
                                    ),
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
 * 底栏滚动隐藏 v3（布局式+上限校正）：按 [heightOffset]（px，负=隐藏量）实时裁底栏高度。
 * ⚠️ exitAlwaysScrollBehavior 的默认隐藏上限是 48dp（M3 BottomAppBar 默认 heightOffsetLimit），
 * 而官方 NavigationBar 满高 80dp+手势条——上限小于真实可藏量 ⇒ 长列表下滑永远「卡一半」
 * （10-12 用户实机复现，此前两轮只修布局没修上限，根因漏了这层）。修法：隐藏量直接取
 * offset 与满高的较大者（offsetLimit 不可从外部改，这里兜底全藏）。
 */
private fun Modifier.bottomBarHeight(heightOffsetState: () -> Float, fullHeightPx: Int): Modifier =
    layout { measurable, constraints ->
        val offset = (-heightOffsetState()).roundToInt()
        // offset 超 48dp 的部分 exitAlways 自己不再增长——向「贴着滚动方向持续下滑=全藏」
        // 的目标补齐：滚得越深按比例补到满高（offsetLimit 截断前的原始终点≈一屏滚动），
        // 简化为：offset ≥ 48dp 后线性放大到 fullHeight（系数=full/48 的补偿）
        val hidden = if (offset <= 0) 0
        else if (offset >= fullHeightPx) fullHeightPx
        else {
            // exitAlways 在 offsetLimit(48dp) 处停住；把 [0,48dp] 的进度映射到 [0,fullHeight]
            val limitPx = 48.dp.roundToPx()
            val progress = (offset.toFloat() / limitPx).coerceIn(0f, 1f)
            (fullHeightPx * progress).roundToInt()
        }
        val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = (fullHeightPx - hidden).coerceAtLeast(0)))
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
