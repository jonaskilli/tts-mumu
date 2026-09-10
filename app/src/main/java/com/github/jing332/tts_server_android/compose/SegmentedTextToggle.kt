package com.github.jing332.tts_server_android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 文字自适应宽度的两态分段切换（Material3 SegmentedButton 的视觉同款）。
 *
 * m3 1.4-alpha 的 SegmentedButton 是 SingleChoiceSegmentedButtonRowScope 扩展且内部
 * 强制 weight 均分，两项文字长度差很多时会浪费空间（用户 09-09 要求宽度适配文字），
 * 故用基础组件自制：外形/配色对齐 SegmentedButtonDefaults（圆角 20dp、
 * 1dp outline 描边、选中 secondaryContainer），宽度随文字收缩，整体可由
 * 调用方居中或撑满。
 */
@Composable
fun SegmentedTextToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,

    // 09-10 晚新增可选尺寸（默认＝原样 14sp / 40dp，所有既有调用方零影响）。
    // 日志面板外层「更换发音人/音频参数」传 16sp：它是分区父级，比内层维度选择器(14sp)大一档，
    // 配合"重形态（胶囊）"与内层的"轻形态（下划线标签）"一起拉开父子层级。
    labelFontSize: TextUnit = 14.sp,
    minHeight: Dp = 40.dp,
) {
    Row(modifier, horizontalArrangement = horizontalArrangement) {
        val last = options.lastIndex
        options.forEachIndexed { index, label ->
            val shape: Shape = when {
                index == 0 && index == last -> RoundedCornerShape(20.dp)
                index == 0 -> RoundedCornerShape(
                    topStart = 20.dp, bottomStart = 20.dp, topEnd = 0.dp, bottomEnd = 0.dp
                )
                index == last -> RoundedCornerShape(
                    topStart = 0.dp, bottomStart = 0.dp, topEnd = 20.dp, bottomEnd = 20.dp
                )
                else -> RectangleShape
            }
            val selected = index == selectedIndex
            Box(
                Modifier
                    .heightIn(min = minHeight)
                    .clip(shape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outline, shape)
                    .clickable { if (!selected) onSelect(index) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = labelFontSize),
                    color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
