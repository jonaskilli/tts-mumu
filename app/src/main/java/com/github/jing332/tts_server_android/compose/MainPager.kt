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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.semantics.Role
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
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
                    // 自绘微信式底栏（替代 M3 NavigationBar）：M3 最低 80dp（32dp 胶囊撑高），
                    // 微信/QQ同款 60dp：24dp 图标+3dp 图文缝+中文常显，选中态无胶囊、图标文字同染 primary。
                    // 10-10 接上滚动隐藏：exitAlwaysScrollBehavior 一直在记 heightOffset（往下滚→60dp、
                    // 上滚→0），但底栏此前没消费它，永远全高——本 Modifier 是缺失的那半截。
                    // offset 为负=向上平移；Scaffold bottomBar 槽不会自动裁剪，加了 clip 防止平移后露出底边。
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            // 10-10 接上滚动隐藏：exitAlwaysScrollBehavior 的引擎早已在
                            // （nestedScroll 已接、无障碍控制也在），但底栏此前没消费 heightOffset，
                            // 永远全高——这里补上消费端：往下滚→底栏平移出屏，往上滚→立刻回弹。
                            // heightOffset 单位 px、负值=隐藏量（M3 官方 BottomAppBar 同款消费逻辑）。
                            .bottomBarOffset({ scrollBehavior.state.heightOffset })
                            .clipToBounds(),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (destination in PagerDestination.routes) {
                                    val isSelected =
                                        pagerState.currentPage == destination.index
                                    // 微信式：无胶囊，选中态图标文字同染 primary，未选中 onSurfaceVariant
                                    val contentColor =
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(MaterialTheme.shapes.medium)
                                            .clickable(
                                                role = Role.Tab,
                                                onClick = {
                                                    scope.launch {
                                                        pagerState.animateScrollToPage(destination.index)
                                                    }
                                                }
                                            )
                                            .padding(vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        // 图文缝 3dp+垂直居中：微信松弛感的关键，贴死会显得紧巴巴
                                        verticalArrangement = Arrangement.spacedBy(
                                            3.dp,
                                            Alignment.CenterVertically
                                        )
                                    ) {
                                        CompositionLocalProvider(
                                            LocalContentColor provides contentColor
                                        ) {
                                            Box(Modifier.size(24.dp)) {
                                                destination.icon()
                                            }
                                        }
                                        Text(
                                            stringResource(destination.strId),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = contentColor,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            // 手势条区域与底栏同色：Android14+ 关闭导航栏半透明后，
                            // 底栏下方会露出一条 Scaffold 背景色的缝（微信=白栏白手势区，无缝）
                            Spacer(
                                Modifier.height(
                                    WindowInsets.navigationBars.asPaddingValues()
                                        .calculateBottomPadding()
                                )
                            )
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
 * 按 [heightOffset]（负值=隐藏量，单位 px）向上平移——自绘底栏对
 * BottomAppBarScrollBehavior 的消费端。M3 官方 BottomAppBar 内部即此逻辑：
 * behavior 滚动时只记 heightOffset 值，官方控件靠这个 modifier 消费它；
 * lambda 每帧在 offset 块内读值（State），滚动时逐帧平移、无需重组。
 * Scaffold 的 bottomBar 槽不会自动裁剪平移出的部分，配合 clipToBounds 兜底。
 */
private fun Modifier.bottomBarOffset(heightOffset: () -> Float): Modifier =
    offset { IntOffset(0, heightOffset().roundToInt()) }
