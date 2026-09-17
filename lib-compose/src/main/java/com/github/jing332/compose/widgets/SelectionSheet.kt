package com.github.jing332.compose.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.jing332.compose.R

/**
 * 列表选择弹窗的外壳：**底部大弹窗**（用户 09-17 定，与音色广场 / 换声弹窗同一形态）。
 *
 * 原先是 MD3 居中 AlertDialog：宽度被规范限死（两侧各留 24dp）、高度也由 MD3 说了算，
 * 长列表一眼能看到的行数偏少。改成满宽、底边贴屏、仅顶部两角圆角的底部面板后宽度吃满；
 * 高度按可见条数估（上限 92% 屏高）—— 条目少则面板跟着矮，条目多则撑到上限再由列表内部
 * 滚动，长短列表都不浪费屏幕。
 *
 * 全 app 的列表型选择弹窗（插件 / 分组 / 分类 / 音色 / 规则 / 主题 / BGM…约 19 个入口）
 * 都经由本组件，改这一处即全部生效。
 *
 * @param maxSheetHeight 面板高度上限（调用方按屏高比例算好）
 * @param maxListHeight 内容区高度上限（调用方按可见条数估好）
 */
@Composable
internal fun SelectionSheet(
    onDismissRequest: () -> Unit,
    maxSheetHeight: Dp,
    maxListHeight: Dp,
    title: @Composable () -> Unit,
    content: @Composable BoxScope.() -> Unit,
    buttons: @Composable BoxScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        // 满宽 + 底对齐：窗口不再被系统栏裁掉，面板底边贴屏
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                // 键盘让位：decorFitsSystemWindows=false 后窗口不再被 IME 顶起，
                // 搜索一弹键盘会把列表下半截盖住；imePadding 让整块浮上去
                .imePadding(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // 关闭热区：面板以外点一下即关（满屏方案下没有 scrim 可点）。
            // 不填色——压暗交给 Dialog 窗口自带的 dim，自绘一层会与它叠加成过暗
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { onDismissRequest() }
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxSheetHeight),
                // M3 底部面板：仅顶部两角 28dp 圆角、底边贴屏——系统栏间距交给内层 Column 的
                // navigationBarsPadding，面板本体不缩，视觉上仍是「从底部升起」
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                // 面板本体先吃掉落在空白处的点击：否则会穿透到下面那层关闭热区，点个缝就把窗关了；
                // 无指示色、无动作，纯占位（子级自己消费过的点击不受影响）
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {}
                ) {
                    // 拖拽把：底部面板的识别特征（本面板是 Dialog 自绘的，只是形态标记、不可拖动）
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(width = 32.dp, height = 4.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(2.dp),
                                )
                        )
                    }

                    // 标题行：标题 + ✕（✕ 与底部按钮互为冗余，是底部面板的通用形态）
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 标题样式跟着底部面板走：原 MD3 标题槽是 headlineSmall（24sp），
                        // 放进底部面板偏大，降为 titleMedium 加粗，与音色广场标题同级
                        ProvideTextStyle(
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        ) {
                            Box(Modifier.weight(1f, fill = false)) { title() }
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.Filled.Close, stringResource(R.string.close))
                        }
                    }

                    // 内容区（开关行 / 搜索框 / 列表 / 空提示全在里面）：
                    // weight(fill=false) 让它只占实际需要的高度——条目少时面板跟着矮；
                    // 上限交给 maxListHeight，列表比它高时由 LazyColumn 自己滚
                    Box(
                        Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = maxListHeight)
                    ) {
                        content()
                    }

                    // 按钮行：与 MD3 一致横排右对齐（多个按钮不层叠）
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        val boxScope: BoxScope = this
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            boxScope.buttons()
                        }
                    }
                }
            }
        }
    }
}
