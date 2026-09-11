package com.github.jing332.tts_server_android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 软槽分段（用户 09-10 晚定稿，09-11 选中项加浮起胶囊）：音频参数区的**第二级**维度切换（语速/音量/音高）。
 *
 * 形态＝**无描边的浅底槽**（surfaceVariant、圆角 8dp、高 36dp）+ 槽内**三等分**；
 * 选中项＝槽内浮起一枚**胶囊浮块**（primaryContainer 底 + onPrimaryContainer 粗体字），
 * 未选中＝次要灰平躺。浮块一眼可辨（用户 09-11 指认：纯文字变色"看不出选了哪个"）。
 *
 * 与第一级（父级）的区分：父级是日志面板顶部那排 [SegmentedTextToggle]「更换发音人/音频参数」
 * ——**描边胶囊 + 宽度随文字**；本组件是**无描边软槽 + 等分撑满**，
 * 形态、宽度行为全不同，父子一眼分家（字号区分 09-11 撤销，父子统一 14sp）。
 * （09-10 曾短暂用过下划线标签页形态，用户否掉，勿再改回下划线。）
 */
@Composable
fun SoftSegmentedTextToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    labelFontSize: TextUnit = 14.sp,
    containerHeight: Dp = 36.dp,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .height(containerHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    // 槽内四周留 4dp：浮块不顶满格子，保持"软槽里浮起一块"的层次
                    .padding(4.dp)
                    .then(
                        if (selected) Modifier.background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(50),
                        ) else Modifier
                    )
                    .clickable { if (!selected) onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = labelFontSize),
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
