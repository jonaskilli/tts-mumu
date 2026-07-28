package com.github.jing332.compose.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CenterTextImage(
    text: String, size: Dp = 32.dp, modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.secondaryContainer,
) {
    Box(modifier) {
        Box(
            Modifier
                .size(size)
                .background(
                    backgroundColor,
                    shape = CircleShape
                ),
        )
        Text(
            text,
            modifier = Modifier
                .align(Alignment.Center)
                .semantics { invisibleToUser() },
            style = MaterialTheme.typography.titleMedium,
        )
    }

}