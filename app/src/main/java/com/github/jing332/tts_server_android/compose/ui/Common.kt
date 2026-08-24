package com.github.jing332.tts_server_android.compose.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.jing332.common.LogLevel

/**
 * 日志级别配色令牌：明暗主题各一套，全应用统一取色入口。
 * 正文色用于日志正文与级别徽章前景；容器色用于徽章/Chip 背景。
 */
object LogLevelColors {

    data class Palette(
        val content: Color,
        val container: Color,
        val onContainer: Color,
    )

    @Composable
    fun palette(level: Int): Palette {
        val dark = isSystemInDarkTheme()
        return when (level) {
            LogLevel.ERROR -> if (dark)
                Palette(Color(0xFFFFB4AB), Color(0xFF93000A), Color(0xFFFFDAD6))
            else
                Palette(Color(0xFFC00011), Color(0xFFFFDAD6), Color(0xFF410002))

            LogLevel.WARN -> if (dark)
                Palette(Color(0xFFF9BE45), Color(0xFF5F4200), Color(0xFFFFE08C))
            else
                Palette(Color(0xFF7A5900), Color(0xFFFFE08C), Color(0xFF4A3500))

            LogLevel.INFO -> if (dark)
                Palette(Color(0xFF93D694), Color(0xFF1D5521), Color(0xFFB0F0AF))
            else
                Palette(Color(0xFF256E29), Color(0xFFB8EFB4), Color(0xFF103C17))

            LogLevel.DEBUG -> if (dark)
                Palette(Color(0xFF9FCBFF), Color(0xFF00458F), Color(0xFFD3E4FF))
            else
                Palette(Color(0xFF1B56A5), Color(0xFFD7E3FF), Color(0xFF002952))

            LogLevel.TRACE -> if (dark)
                Palette(Color(0xFFB7C9D6), Color(0xFF3F4952), Color(0xFFD7E3EA))
            else
                Palette(Color(0xFF54616D), Color(0xFFDEE3EA), Color(0xFF222B33))

            else -> Palette(
                content = MaterialTheme.colorScheme.onSurface,
                container = MaterialTheme.colorScheme.surfaceVariant,
                onContainer = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 通用空状态：圆形底座图标 + 标题 + 说明文字，居中展示。
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        if (!title.isNullOrEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
        if (!message.isNullOrEmpty()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

/**
 * 表单分区：小号加粗标题 + 左侧 tertiaryContainer 竖色带，上下留白分隔逻辑区块。
 */
@Composable
fun FormSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .background(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small
                )
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
