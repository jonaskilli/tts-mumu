package com.github.jing332.compose.widgets

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue


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

    // 「在顶/在底常显 + 下滑即回显」（10-10 用户三轮反馈收敛）：
    // ①exitAlways 的回弹阈值=一整条底栏高度（80dp）——长列表中间只滑一小段不回弹，
    //   「往上滑一段时间再往下滑要滑过一页多一点才出现」即此（用户实测复述）；
    // ②atTop（canScrollBackward=false）/ atBottom（末条贴视口底）两位置强制回显——
    //   「本来在日志底部、底栏在，再往下滑一直在」即此。
    // ③新增**下滑即回显**：监听列表滚动，捕获「向下滚动」（内容上移、往回看方向）的
    //   位移即瞬间复位底栏，不等 80dp 阈值——与 Android 原生设置页行为一致。
    //   firstVisibleItemIndex 减小或同条目 offset 减小都算向下滚；用 derivedStateOf
    //   比较前后值，浅色 LaunchedEffect 触发复位。
    val atTop = !listState.canScrollBackward
    val atBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val visible = info.visibleItemsInfo
            info.totalItemsCount <= 0 || visible.isEmpty() ||
                (visible.last().index >= info.totalItemsCount - 1 &&
                    visible.last().offset + visible.last().size <= info.viewportEndOffset)
        }
    }
    // 向下滚动检测：记录上一帧的（首可见下标，首可见偏移），有减小即向回滚
    var lastScrollPos by remember { mutableLongStateOf(Long.MAX_VALUE) }
    val scrollingDown by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val visible = info.visibleItemsInfo
            if (visible.isEmpty()) return@derivedStateOf false
            val pos = visible.first().index.toLong() * 1_000_000L +
                (visible.first().offset.coerceAtLeast(0)).toLong()
            val down = pos < lastScrollPos
            lastScrollPos = pos
            down
        }
    }
    LaunchedEffect(atTop, atBottom, scrollingDown) {
        if (atTop || atBottom || scrollingDown) {
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
