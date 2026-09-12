package com.github.jing332.compose.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.github.jing332.compose.R

/**
 * 全 app 弹窗外壳（用户 09-11 终裁：**彻底 MD3**）。
 *
 * 本组件曾是自绘外壳（白底+零海拔+居中标题+自绘按钮 FlowRow），是用户 09-03/09-04 反馈
 * "弹窗过深/发绿"后的定制产物；09-11 用户对比两套壳后裁定回归 MD3 标准——组件**签名保持**、
 * 内部换成 material3 原生 [AlertDialog]，29 个调用方一处改全部生效。
 *
 * 09-11 彻底化（用户"直接彻底按 md3 来"）：
 * - 撤 dialogContentPadding 参数（MD3 自带规范内边距，任何叠加都算自定义，AuditionDialog 的
 *   自定义值一并撤）；
 * - 撤正文 LocalTextStyle 覆盖（MD3 text 槽自带 bodyMedium）；
 * - 删自绘 AppDialogFlowRow（换行是旧壳行为，MD3 按钮行官方不换行）；
 * - 白底+零海拔防染绿定制废弃（MD3 色调海拔 3dp，绿色混色肉眼无感，用户 09-11 核实确认）。
 *
 * 维护约定：**本组件只是过渡期兼容垫片，永远不再加自定义样式**——要改弹窗观感，改主题
 * （colorScheme/typography）或等 MD3 官方规范演进，勿再往这里堆魔法数字。
 */
@Preview
@Composable
fun PreviewAppDialog() {
    var show by remember { mutableStateOf(true) }
    if (show) {
        AppDialog(title = {
            Text("Title")
        }, content = {
            Text("Content")
        }, buttons = {
            TextButton(onClick = {
                show = false
            }) {
                Text("Cancel")
            }
            TextButton(onClick = {
                show = false
            }) {
                Text("OK")
            }
        }, onDismissRequest = {
            show = false
        })
    }

}

@Composable
fun AppDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    title: @Composable () -> Unit,
    content: @Composable BoxScope.() -> Unit,
    buttons: @Composable BoxScope.() -> Unit = {
        TextButton(onClick = onDismissRequest) { Text(stringResource(id = R.string.close)) }
    },
) = AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismissRequest,
    properties = properties,
    title = { title() },
    text = {
        // content 的接收者是 BoxScope：包一层 Box 保持旧签名兼容（调用方可无视，语义不变）。
        // 正文样式/内边距全部交给 MD3 规范（bodyMedium + 24dp）
        Box(Modifier.fillMaxWidth()) {
            content()
        }
    },
    // buttons 的签名是 BoxScope 接收者：confirmButton 槽是 RowScope，用 Box 捕获接收者传入。
    // 按钮本体必须横排（Row + 8dp 间距，MD3 官方按钮行做法）——09-12 修复：直接 Box 会把
    // 多个按钮层叠渲染（插件管理多选删除弹窗「取消/删除」文字重叠实锤），Row 换横排、签名不动。
    confirmButton = {
        Box {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { buttons() }
        }
    },
)
