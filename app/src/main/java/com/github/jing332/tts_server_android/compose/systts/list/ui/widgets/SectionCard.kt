package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 编辑页分区卡片：图标+标题的头部行，内容区竖排。
 * 用于把配置项编辑页按「基本信息/音色来源/朗读与标签/音频参数」分块，
 * 长页面一眼看清结构；标题栏 trailing 可放区块级操作。
 * 内容区自带 padding 由调用方决定（字段类内容建议 horizontal 12dp 与标题对齐）。
 *
 * 视觉：用低透明度 surfaceVariant 底色+无边框，避免「大框套小框」——
 * 内部 OutlinedTextField 的描边在柔和底色上仍清晰分层，且底色随主题走。
 */
@Composable
internal fun SectionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    // false 时不渲染图标+标题行：如仅界面模式下「音色来源」卡只剩插件自定义UI，省一行高度
    showHeader: Boolean = true,
    trailing: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            // 原版配色 surfaceVariant@35% 灰调绿底（返璞归真批次恢复）
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
    ) {
        Column(Modifier.padding(vertical = 6.dp)) {
            if (showHeader) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        title,
                        modifier = Modifier.padding(start = 6.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.weight(1f))
                    trailing()
                }
            }
            content()
        }
    }
}
