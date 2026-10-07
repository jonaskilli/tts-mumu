package com.github.jing332.compose.widgets

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun LazyListIndexStateSaver(
    models: Any?,
    listState: LazyListState,

    onIndexUpdate: suspend (Int, Int) -> Unit = { index, offset ->
        listState.scrollToItem(index, offset)
    },
) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    var offset by rememberSaveable { mutableIntStateOf(0) }
    // 本次进页面是否已恢复过（不用 saveable：进程重建后要允许再恢复一次）。
    // 恢复一次即收手：models 是 Room 数据流，页面上勾选/编辑写库也会重发，
    // 旧实现在停在顶部时每次重发都把上次离开时的旧位置恢复一遍——
    // 实机表现为勾选一条、整个列表跳一行（10-08）。
    var restored by remember { mutableStateOf(false) }

    LaunchedEffect(models) {
        if (!restored && models != null) {
            restored = true
            if (listState.firstVisibleItemIndex <= 0 && listState.firstVisibleItemScrollOffset <= 0) {
                onIndexUpdate(index, offset)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            index = listState.firstVisibleItemIndex
            offset = listState.firstVisibleItemScrollOffset
        }
    }
}