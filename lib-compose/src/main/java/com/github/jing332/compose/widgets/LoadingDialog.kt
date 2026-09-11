package com.github.jing332.compose.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * 全局加载弹窗（用户 09-11 终裁：完全 MD3）。
 *
 * 旧版为 Dialog + Surface(tonalElevation 4dp) 自绘壳 + 自绘渐变扫掠转圈（ProgressIndicatorLoading，
 * 600ms 旋转的 sweepGradient 边框）；已整体换成官方 AlertDialog 壳 + 官方不确定态
 * [CircularProgressIndicator]，视觉随主题与 MD3 规范。
 *
 * 壳选择说明：MD3 无"加载弹窗"官方组件，官方件组合即 AlertDialog + 进度指示器；
 * AlertDialog 的 confirmButton 为必选槽，本弹窗无按钮，传空槽即可（dismissButton 可省）。
 *
 * LinearProgressIndicator 确定态（progress 非空）用于备份/导入等有百分比的场景，宽度随 text 槽。
 */
@Composable
fun LoadingDialog(
    onDismissRequest: () -> Unit,
    dismissOnBackPress: Boolean = false,
    // 可选进度：0f~1f。为 null 时显示不确定（官方转圈）；非 null 时显示线性进度条
    progress: Float? = null,
    // 可选文字：为 null 时显示默认“加载中”
    text: String? = null
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = dismissOnBackPress),
        confirmButton = {},
        text = {
            // 用户 2026-09-11 反馈：进度与提示文字应居中（原左上角排布观感差）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (progress == null) {
                    CircularProgressIndicator()
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = text ?: "加载中",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (progress != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    )
}

@Preview
@Composable
fun PreviewDialog() {
    var isShow by remember { mutableStateOf(true) }
    if (isShow)
        LoadingDialog(onDismissRequest = { isShow = false })
}
