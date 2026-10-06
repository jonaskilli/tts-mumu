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
import androidx.compose.ui.unit.sp

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
            // 字号随圆径按 62.5% 走（≈M3 图标在占位盒里的填充感）：32dp 圆 20sp、44dp 圆
            // 27.5sp。原固定 titleMedium 16sp 在 32/44dp 圆里只占一半，emoji 当图标用显小
            // （10-06 用户指认「图标没跟文字大小匹配」）
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = (size.value * 0.625f).sp
            ),
        )
    }

}