package com.github.jing332.tts_server_android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 文字下划线标签页（M3 TabRow 视觉同款，用户 09-10 晚定稿）。
 *
 * 用途：音频参数区的**第二级**维度切换（语速/音量/音高）——它上面还有日志面板的
 * 「更换发音人/音频参数」分区切换（[SegmentedTextToggle] 胶囊）。两级必须一眼分家，故：
 * - 父级（分区切换，日志面板）= **重形态**：胶囊底 + 描边，**[SegmentedTextToggle] 传 16sp**；
 * - 子级（维度切换，本组件）= **轻形态**：无底无边、只有文字 + 选中项主色下划线与一条整行基线，14sp。
 * 形态（胶囊 vs 下划线）+ 字号（16 vs 14）+ 颜色（次级灰 vs 主色）三重区分。
 *
 * 与 [SegmentedTextToggle] 共用调用签名（options/selectedIndex/onSelect/modifier），可直接替换。
 * 本组件只用于子级；父级仍用胶囊，勿混用。
 */
@Composable
fun UnderlineTextToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    labelFontSize: TextUnit = 14.sp,
    indicatorHeight: Dp = 2.dp,
) {
    Column(modifier) {
        // 宽度自适应文字、整体居中（沿用 09-09「不均分、不浪费宽度」的口径）；
        // 指示器每个标签都占位（未选中画透明），保证各标签等高、指示器正好压在基线上
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Column(
                    Modifier
                        .clickable { if (!selected) onSelect(index) }
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = labelFontSize),
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(indicatorHeight)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                    )
                }
            }
        }

        // 整行基线：未选中项坐在它上面，选中项被主色下划线压住（M3 TabRow 同款）
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}
