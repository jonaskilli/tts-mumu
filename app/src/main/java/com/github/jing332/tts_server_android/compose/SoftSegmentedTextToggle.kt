package com.github.jing332.tts_server_android.compose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

/**
 * 分组分段切换（用户 09-11 下午终裁：自定义观感不好，回归官方 MD3 组件）。
 *
 * 内部即官方 [SegmentedButton]（描边连通、选中项 secondaryContainer 底色 + 勾选图标、
 * labelLarge 排版、官方高度/间距），**不再有任何自绘样式**——
 * 旧的「无描边浅底软槽 + 浮起胶囊」画法（09-10 晚定稿、09-11 加浮块）已整体撤销，勿再改回。
 *
 * 布局恒为**均分撑满**（每项 Modifier.weight(1f)）——官方 SegmentedButton 内部 label 用
 * weight 布局，只支持定宽/均分，"宽度随文字收缩"不在官方支持范围（曾试过，文字被压成省略号，
 * 用户 09-11 晚终裁撤收缩模式，equalWidth 参数随之删除，勿再加回）。
 *
 * 三处调用点自动同步换装：
 * - 卡片⋮弹窗与日志快捷面板共用的 [com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.AudioParamsDimensionSection]
 * - 编辑页 [com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.AudioParamsDimRows]
 * - 备份弹窗 BackupDialog（模式切换）
 * - 日志快捷面板顶部父级「更换发音人/音频参数」（09-11 晚起同款均分）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoftSegmentedTextToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth()
    ) {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                selected = index == selectedIndex,
                // 重复点击已选中项也回调（用户 09-12：编辑页音频参数区"再点同维收起"需要）；
                // 其余调用点均自带"值不变不动作"保护（BackupDialog 有 if 挡、其余为幂等赋值），行为不变
                onClick = { onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                modifier = Modifier.weight(1f),
            ) {
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
