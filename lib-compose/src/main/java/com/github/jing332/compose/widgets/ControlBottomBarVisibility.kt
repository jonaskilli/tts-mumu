package com.github.jing332.compose.widgets

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect


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

    // 列表回到顶部或内容不足以向上滚动时，确保底部栏可见；
    // 修复：原条件 !(canScrollBackward || canScrollForward) 过于严格，
    // 仅在列表完全不可滚动时才复位，导致用户滚动隐藏后回到顶部时底部栏不再出现。
    LaunchedEffect(listState.canScrollBackward, listState.canScrollForward) {
        if (!listState.canScrollBackward) {
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
