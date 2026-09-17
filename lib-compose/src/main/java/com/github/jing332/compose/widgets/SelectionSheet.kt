package com.github.jing332.compose.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.jing332.compose.R

/**
 * 面板内容的左右内边距：标题行 / 内容区 / 按钮行共用这一条基准。
 *
 * 定 24dp（16→24→32→24 四轮，09-17 定稿）：32dp 实机证据是标题
 * 「🔊 声音（点击此处可试听后分类）」被挤成两行、长声音名每行少 16dp；
 * 24dp 与居中卡片（MD3 AlertDialog 两侧各留 24dp）同一档，360dp 屏上内容区 312dp。
 * 早期 16dp 被「太窄」否决——那时三套左缘线未对齐，观感差不只因为数值。
 */
private val PANEL_HORIZONTAL_PADDING = 24.dp

/**
 * 面板高 = 弹窗窗口真实可用高 × 此比例（用户 09-17 定 92%）。
 * ⚠️ 不能用 `LocalConfiguration.screenHeightDp`：它不含状态栏/导航栏（800dp 屏只算 728dp），
 * 而本窗口 decorFitsSystemWindows=false 铺满全屏 ⇒ 之前实机看起来只有 ~85%（用户说「像 88%」）。
 *
 * ⚠️ 实现必须用 `Modifier.fillMaxHeight(fraction)`（09-17 实机教训）：此前用
 * `BoxWithConstraints.maxHeight × fraction` 再 `height(固定 dp)`，实机上按钮行整体
 * 下沉约一个导航栏高、被屏幕底缘裁半（09172116 包实锤）——固定 dp 是从外层约束算出来的
 * 死值，与 Surface 实际分到的空间脱节；fillMaxHeight 直接吃「实际可用高」的比例，
 * 与音色广场面板（同结构、底部完好）完全一致，不再依赖任何外层算术。
 */
private const val SHEET_HEIGHT_FRACTION = 0.92f

/**
 * 列表选择弹窗的外壳：**底部大弹窗**（用户 09-17 定，与音色广场 / 换声弹窗同一形态）。
 *
 * 原先是 MD3 居中 AlertDialog：宽度被规范限死（两侧各留 24dp）、高度也由 MD3 说了算，
 * 长列表一眼能看到的行数偏少。改成满宽、底边贴屏、仅顶部两角圆角的底部面板后宽度吃满。
 *
 * 高度口径：**固定为窗口真实高度的 92%**。只有 >20 条的长列表才会走到本外壳，
 * 21 条 × 48dp ≈ 1008dp 必然超屏，固定高不会产生「半屏空白」；换来的是内容区
 * 用 weight 吃剩余空间、按钮行固定在底部——按钮行在布局上**不可能**被列表挤出
 * 可视区（此前两版按钮行靠「估算预留」让位，标题折行/导航栏一吃就不够，
 * 09171748/09171924 两包实锤「保存」不可见）。
 *
 * 全 app 的列表型选择弹窗（插件 / 分组 / 分类 / 音色 / 规则 / 主题 / BGM…约 19 个入口）
 * 都经由本组件，改这一处即全部生效。
 *
 * 左右边距口径：**面板内容统一 24dp**（标题行 / 内容区 / 按钮行共用 `PANEL_HORIZONTAL_PADDING`；
 * ✕ 的 48dp 触摸区自带 12dp 内缩、标题行给 end=12，图标正好落在 24dp 右缘线上）。
 * 内层组件（搜索框、列表条目）在底部形态下**不要再自加横向内边距**，由本外壳一处说了算。
 *
 * 按钮行口径：**只有传了 `buttons` 的弹窗才渲染**（纯单选弹窗点行即选即关，右上 ✕ 已够关）；
 * 「关闭」由 ✕ 兼任，底部不重复放。
 *
 * @param buttons 底部动作键槽位（如试听分类的「保存」）；null = 不渲染按钮行
 */
@Composable
internal fun SelectionSheet(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    content: @Composable BoxScope.() -> Unit,
    buttons: (@Composable RowScope.() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        // 满宽 + 底对齐：窗口不再被系统栏裁掉，面板底边贴屏
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        // 窗口钉成全屏+底部对齐（PinDialogWindowToScreen.kt 注释：09-17 实机
        // 窗口被排版到屏幕下方 ~134px，按钮行跟着出屏被裁——比例怎么改都没用的根因）
        PinDialogWindowToScreen()
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
                    // 比例填「实际可用高」（见 SHEET_HEIGHT_FRACTION 注释：勿改回固定 dp 算术）
                    .fillMaxHeight(SHEET_HEIGHT_FRACTION),
                // M3 底部面板：仅顶部两角 28dp 圆角、底边贴屏——系统栏间距交给内层 Column 的
                // navigationBarsPadding，面板本体不缩，视觉上仍是「从底部升起」
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                // 面板本体先吃掉落在空白处的点击：否则会穿透到下面那层关闭热区，点个缝就把窗关了；
                // 无指示色、无动作，纯占位（子级自己消费过的点击不受影响）。
                // fillMaxSize 与音色广场面板同构：Column 撑满 Surface，导航栏间距由
                // navigationBarsPadding 从撑满后的高度里收——按钮行恒在可视区（09-17 实机）
                Column(
                    Modifier
                        .fillMaxSize()
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

                    // 标题行：标题 + ✕。
                    // end=12 是给 ✕ 的 48dp 触摸区留的：IconButton 自带 12dp 内缩，
                    // 12 + 12 = 24 ⇒ ✕ 图标正好落在与内容区同一条右缘线上
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = PANEL_HORIZONTAL_PADDING,
                                end = 12.dp,
                                top = 8.dp,
                                bottom = 4.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 标题样式跟着底部面板走：原 MD3 标题槽是 headlineSmall（24sp），
                        // 放进底部面板偏大，降为 titleMedium 加粗，与音色广场标题同级。
                        // weight(1f) 独占剩余宽度（原先多一个 Spacer(weight(1f)) 把宽对半分，
                        // 长标题被折成两行，09-17 实机）
                        ProvideTextStyle(
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        ) {
                            Box(Modifier.weight(1f)) { title() }
                        }
                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.Filled.Close, stringResource(R.string.close))
                        }
                    }

                    // 内容区（开关行 / 搜索框 / 列表 / 空提示全在里面）：weight(1f) 吃掉
                    // 拖拽把+标题+按钮行之后的**全部剩余**空间，列表在内部滚动——
                    // 面板总高固定，按钮行在布局上不可能被内容挤走（方案 B 的核心）。
                    //
                    // 左右内边距**只有这里一处说了算**（PANEL_HORIZONTAL_PADDING）：
                    // 内层（搜索框的 8dp、条目文字的 16dp）在底部形态下必须让位
                    // （AppSelectionDialog 的 hp / 搜索框 padding 都按 useSheet 置 0），
                    // 否则叠出来还是多套左缘线。数值口径见常量注释（16→24→32→24 定稿 24）
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = PANEL_HORIZONTAL_PADDING)
                    ) {
                        content()
                    }

                    // 底部按钮行：只有调用方传了动作键才渲染（纯单选弹窗没有这行，
                    // 多出一行列表）。左右与内容区同一条 24dp 基准，右对齐与 MD3 一致。
                    // 面板高固定 + 内容 weight ⇒ 这行恒在面板底部可视区，不靠估算
                    if (buttons != null) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = PANEL_HORIZONTAL_PADDING,
                                    end = PANEL_HORIZONTAL_PADDING,
                                    top = 4.dp,
                                    bottom = 10.dp
                                ),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            buttons?.invoke(this)
                        }
                    }
                }
            }
        }
    }
}
