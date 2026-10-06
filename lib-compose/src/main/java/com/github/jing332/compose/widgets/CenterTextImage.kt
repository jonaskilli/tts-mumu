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
    // 圆内字形占空比（10-06）：默认 0.625 保持既有调用点行为不变。
    // 中文是满格方块、墨迹面积远超同尺寸矢量图标，按 0.625 在小圆里会显撑（插件列表
    // 实测「图标过大」）；需要更透气时由调用方传更小值（插件列表传 16/28≈0.57）
    textRatio: Float = 0.625f,
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
            // 字号 = 圆径 × 占空比（默认 0.625）：32dp 圆 20sp、44dp 圆 27.5sp。
            // 沿革：原固定 16sp（32/44 圆里偏小、图标与文字不匹配）→ 62.5% 派生
            //（中文在小圆里撑满，用户指「过大」）→ 插件列表自定 0.57（28dp 圆 16sp）
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = (size.value * textRatio).sp
            ),
        )
    }

}