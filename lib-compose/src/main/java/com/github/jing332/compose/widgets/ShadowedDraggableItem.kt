package com.github.jing332.compose.widgets

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.ReorderableState

@Composable
fun LazyItemScope.ShadowedDraggableItem(
    reorderableState: ReorderableState<*>,
    key: Any,
    content: @Composable LazyItemScope.(isDragging: Boolean) -> Unit,
) {
    val view = LocalView.current
    ReorderableItem(reorderableState, key) { isDragging ->
        LaunchedEffect(isDragging) {
            if (isDragging) {
                view.isHapticFeedbackEnabled = true
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }

        val tweenSpec = tween<Dp>(
            durationMillis = 250,
            easing = FastOutSlowInEasing
        )

        val elevation =
            animateDpAsState(if (isDragging) 24.dp else 0.dp, label = "", animationSpec = tweenSpec)
        Box(
            modifier = Modifier
                .shadow(
                    elevation.value,
                    MaterialTheme.shapes.small,
                    ambientColor = DefaultShadowColor.copy(alpha = 1f),
                    spotColor = DefaultShadowColor.copy(alpha = 0.3f),
                )
        ) {
            content(isDragging)
        }
    }
}