package com.github.jing332.compose.widgets

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlBottomBarVisibility(
    state: Boolean,
    bottomBarBehavior: BottomAppBarScrollBehavior,
) {
    val bottomAppBarState = bottomBarBehavior.state

    LaunchedEffect(state) {
        if (state) {
            animateBottomBarToShow(bottomAppBarState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlBottomBarVisibility(
    listState: LazyListState,
    bottomBarBehavior: BottomAppBarScrollBehavior,
) {
    val bottomAppBarState = bottomBarBehavior.state

    // 「在顶或在底都常显」：列表滚到最顶或最底时强制回显底栏。
    // 只有「在顶」的旧版（10-10 用户报障）：日志页常停在最底（最新日志），底栏被上滑
    // 藏掉后要往回滑过一整条底栏高度（exitAlways 的回弹阈值，80dp 比旧 60dp 更明显）
    // 才肯回来——「往上滑有时候看不到底栏，得到一定程度才能看到」即此。
    // atTop 用 canScrollBackward，atBottom 用「末条可见且无向前余量」；
    // 两键任一成立即瞬间复位（与 animateBottomBarToShow 同款，不做动画防掉帧）。
    val atBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val visible = info.visibleItemsInfo
            info.totalItemsCount <= 0 || visible.isEmpty() ||
                (visible.last().index >= info.totalItemsCount - 1 &&
                    visible.last().offset + visible.last().size <= info.viewportEndOffset)
        }
    }
    LaunchedEffect(listState.canScrollBackward, atBottom) {
        if (!listState.canScrollBackward || atBottom) {
            animateBottomBarToShow(bottomAppBarState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private suspend fun animateBottomBarToShow(
    bottomAppBarState: androidx.compose.material3.BottomAppBarState,
) {
    // 瞬间复位，避免切换页面时 300ms 动画与首次组合叠加导致掉帧
    if (bottomAppBarState.heightOffset != 0f) {
        bottomAppBarState.heightOffset = 0f
    }
}
