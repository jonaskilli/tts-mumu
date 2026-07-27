package com.github.jing332.compose.widgets

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlBottomBarVisibility(
    state: Boolean,
    bottomBarBehavior: BottomAppBarScrollBehavior,
) {
    val bottomAppBarState = bottomBarBehavior.state

    val heightOffset =
        remember { androidx.compose.animation.core.Animatable(1f) }

    LaunchedEffect(heightOffset.value) {
        bottomAppBarState.heightOffset = heightOffset.value
    }

    LaunchedEffect(state) {
        if (state) {
            heightOffset.snapTo(bottomAppBarState.heightOffset)
            heightOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            )
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

    val heightOffset =
        remember { androidx.compose.animation.core.Animatable(1f) }

     LaunchedEffect(heightOffset.value) {
        bottomAppBarState.heightOffset = heightOffset.value
    }

    LaunchedEffect(listState.canScrollBackward, listState.canScrollForward) {
        if (!(listState.canScrollBackward || listState.canScrollForward)) {
            heightOffset.snapTo(bottomAppBarState.heightOffset)
            heightOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

}