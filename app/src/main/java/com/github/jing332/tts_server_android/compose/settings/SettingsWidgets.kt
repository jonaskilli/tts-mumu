package com.github.jing332.tts_server_android.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.jing332.compose.widgets.AppDropdownMenu
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.LabelSlider
import com.github.jing332.tts_server_android.R

internal val horizontalPadding: Dp = 16.dp
internal val verticalPadding: Dp = 12.dp

/**
 * 分区卡片：卡边到屏幕边的外边距。
 * 历史：10-05 加底色时定 8dp（当时为凑「卡边8+卡内8=16=原行内距」的零位移）；
 * 用户实机反馈框左右太贴屏（「没加框正好，加了框边距变窄」）→ 同日改 16dp：
 * 框线回到原来行内容线 16dp 的位置。卡内行距不再随卡边联动（见 SettingsGroup 内固定 8dp）。
 */
internal val sectionCardMargin: Dp = 16.dp

/**
 * 行内左右内距。默认 [horizontalPadding]（16dp，未套卡片的散行照旧）；
 * 卡片内由 [SettingsGroup] 提供固定 [cardRowHorizontalPadding]=8dp——
 * 卡边 16 + 行内距 8 = 内容线 24dp，卡片看起来有内衬不贴边。
 * （曾用 `horizontalPadding - sectionCardMargin` 凑「内容零位移」，卡边改 16 后会算成 0、
 * 文字贴卡缘，故改为固定值。）
 */
internal val LocalPreferenceRowHorizontalPadding = compositionLocalOf { horizontalPadding }

/** 卡片内行的左右内距（与 [sectionCardMargin] 解耦；见 [LocalPreferenceRowHorizontalPadding]） */
internal val cardRowHorizontalPadding: Dp = 8.dp

@Composable
internal fun DropdownPreference(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    subTitle: @Composable () -> Unit,
    actions: @Composable ColumnScope. () -> Unit = {},
) {
    BasePreferenceWidget(modifier = modifier, icon = icon, onClick = {
        onExpandedChange(true)
    }, title = title, subTitle = subTitle) {
        // 下拉行自带菜单锚点（零尺寸），右侧原本也是空白 —— 与其它可点行一致补 ›
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AppDropdownMenu(
            modifier = Modifier.align(Alignment.Top),
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }) {
            actions()
        }
    }
}

@Composable
internal fun DividerPreference(title: @Composable () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(top = verticalPadding + 4.dp)
            .minimumInteractiveComponentSize()
    ) {
        Row(
            Modifier
                .padding(vertical = 8.dp)
                .align(Alignment.Start)
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.titleSmall.copy(
                    color = MaterialTheme.colorScheme.primary
                ),
            ) {
                title()
            }
        }
    }

}

/**
 * 设置分区：文字小标题（卡片外）+ 条目卡片底（10-05 用户拍板：照 QQ 那套）。
 * 历史：050a759「返璞归真」把本组件从「卡片壳+标题」删成纯直通（无标题）；
 * 10-05 先恢复**标题**（当时明确不要卡片壳），同日再拍板"加底色"——
 * 采用 QQ 式：**标题留在卡片外**、只给条目套一张与配置项编辑页同款底色的卡片。
 * 保留 [show]：搜索模式下调用方以 show=false 退平铺（不显示标题、不套卡片），行为不变。
 *
 * 折叠：[collapsible]=true 时标题行整行可点，箭头在标题左（与分组/子分组头同习语）；
 * [defaultExpanded]=false 即默认收起。收起时**不出空卡片**；搜索态（show=false）一律平铺直出——
 * 搜到的条目不能被折叠藏起来，也不套壳（命中项是跨区散项，套壳会变成一堆无名小块）。
 *
 * 零位移约定：[sectionCardMargin] 8dp + 卡内行内距 8dp = 原来的 16dp 行内距
 * ⇒ 套壳后图标/文字/右侧控件的横坐标与套壳前完全一致（只是多了一层底色）。
 */
@Composable
internal fun SettingsGroup(
    // showHeader=false 时不需要标题（套一张无标题卡片，如「后台保活设置」页顶部两行）
    title: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    show: Boolean = true,
    collapsible: Boolean = false,
    defaultExpanded: Boolean = true,
    // 逃生阀：需要纯平铺（不套卡片底）时传 false
    card: Boolean = true,
    // false = 不出分区标题行（仍然套卡片底）
    showHeader: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!show) {
        Column(content = content)
        return
    }
    // 折叠态用 rememberSaveable：转屏/进程重启后保持用户当场的选择
    var expanded by rememberSaveable { mutableStateOf(defaultExpanded) }
    Column(modifier.fillMaxWidth()) {
        // 分区小标题：titleSmall + primary（与 DividerPreference 同款，本页现成的分区习语）
        // 卡片外、左缘与卡片边对齐（卡边 8 + 4）
        if (showHeader) {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.titleSmall.copy(
                    color = MaterialTheme.colorScheme.primary
                ),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .then(
                            if (collapsible) Modifier.clickable { expanded = !expanded }
                            else Modifier
                        )
                        .padding(
                            start = sectionCardMargin + 4.dp,
                            top = verticalPadding + 6.dp,
                            bottom = 4.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (collapsible) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            // 纯装饰（标题文字已表意）：展开朝下 / 折叠朝右，与列表页分组头同一习语
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(if (expanded) 0f else -90f),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    title()
                }
            }
        } else {
            // 无标题卡片：只补一点上间距，避免与前一项贴死
            Spacer(Modifier.height(verticalPadding))
        }
        // 收起时不出空卡片；展开才渲染卡片本体
        if (!collapsible || expanded) {
            if (card) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = sectionCardMargin),
                    colors = CardDefaults.cardColors(
                        // 与配置项编辑页 SectionCard 同款底色（surfaceVariant@20%）：
                        // 与编辑页视觉统一，且比页面底略深一眼认出分区、不抢内容
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f),
                    ),
                ) {
                    CompositionLocalProvider(
                        // 卡内行距固定 8dp（不再用 horizontalPadding - sectionCardMargin 的联动算法：
                        // 卡边改 16 后那个式子会得 0，行内容会贴到卡缘上）
                        LocalPreferenceRowHorizontalPadding provides cardRowHorizontalPadding
                    ) {
                        Column(content = content)
                    }
                }
            } else {
                Column(content = content)
            }
        }
    }
}

@Composable
internal fun SwitchPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subTitle: @Composable () -> Unit,
    icon: @Composable () -> Unit = {},

    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    BasePreferenceWidget(
        modifier = modifier
            .focusable()
            .toggleable(
                value = checked,
                role = Role.Switch,
                enabled = true,
                interactionSource = interactionSource,
                indication = ripple(),
                onValueChange = { onCheckedChange(!checked) }),

        title = title,
        subTitle = subTitle,
        icon = icon,
        content = {
            Switch(
                checked = checked,
                interactionSource = interactionSource,
                onCheckedChange = null,
                modifier = Modifier.align(Alignment.CenterVertically)

            )
        }
    )
}

@Composable
internal fun BasePreferenceWidget(
    modifier: Modifier = Modifier,
    role: Role? = null,
    onClick: (() -> Unit)? = null,
    title: @Composable () -> Unit,
    subTitle: @Composable () -> Unit = {},
    icon: @Composable () -> Unit = {},
    /** 行可点又没有自带右侧控件时是否补 ›（纯动作行可关掉，见调用点） */
    showChevron: Boolean = true,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(modifier = Modifier
        .minimumInteractiveComponentSize()
        .defaultMinSize(minHeight = 64.dp)
        .clip(MaterialTheme.shapes.extraSmall)
        .then(
            if (onClick == null) Modifier else Modifier.clickable(
                role = role,
                onClick = onClick
            )
        )
        .then(modifier)
        .padding(horizontal = LocalPreferenceRowHorizontalPadding.current, vertical = verticalPadding)
        .semantics(true) {}
    ) {
        Column(
            Modifier.align(Alignment.CenterVertically)
        ) {
            icon()
        }

        Column(
            Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
                .padding(horizontal = 8.dp)
        ) {
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.titleMedium) {
                title()
            }

            // 副标题弱化为 onSurfaceVariant：与标题拉开主次，长描述不再糊成一团
            // 特例许可（用户 09-11 终裁"一并回原版"）：回原版手调 15sp（介于官方 14/16 之间；
            // MD3 token 化时曾收为 bodyMedium 14sp，现按用户要求撤回）
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )) {
                subTitle()
            }
        }

        Row(
            Modifier
                .align(Alignment.CenterVertically)
        ) {
            if (content != null) {
                content.invoke(this)
            } else if (onClick != null && showChevron) {
                // 10-05 用户实机反馈「右边空空的」：可点的行原来右侧什么都没有（标题列 weight(1f)
                // 把空白全留在右边），看着像没做完。补一个 › 作「进下一页／弹窗」的指示——
                // 常规设置页习语；自带右侧控件（开关/滑杆值/下拉菜单）的行不受影响。
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SliderPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subTitle: @Composable () -> Unit,
    icon: @Composable () -> Unit = {},
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    buttonSteps: Float = 1f,
    buttonLongSteps: Float = 2f,
    label: String,
) {
    var show by rememberSaveable { mutableStateOf(false) }
    if (show)
        // 点击卡片弹居中对话框（AppDialog，与音频参数/发音人调整同弹窗语言）：
        // 内容自适应不再撑 40% 屏高；即调即存，关闭即完成（用户 09-08 方案A 定稿，
        // 取代上游遗产的底部弹窗——40% 空壳托一条滑杆且文字贴边，非本项目的考量设计）
        AppDialog(
            onDismissRequest = { show = false },
            title = title,
            // AppDialog 的 content 在参数表中间（非末位），尾随 lambda 绑不上，
            // 必须显式传参（CI: "No value passed for parameter 'content'"）
            content = {
                // 水平再让 4dp（叠加 AppDialog 自带 12dp ≈16dp）：滑杆 −/+ 不贴边。
                // 边距定调起源是音频参数弹窗（用户 09-09），设置区滑杆弹窗跟随同款
                LabelSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .padding(top = 8.dp),
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    steps = steps,
                    buttonSteps = buttonSteps,
                    buttonLongSteps = buttonLongSteps,
                    text = label
                )
            },
        )

    BasePreferenceWidget(modifier, onClick = {
        show = true
    }, title = title, icon = icon, subTitle = subTitle) {
        // trailing 当前值：弱化色，与标题基线视觉呼应，不再与描述抢眼
        Text(label, style = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
        ))
    }
}

@Composable
internal fun PreferenceDialog(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subTitle: @Composable () -> Unit,
    icon: @Composable () -> Unit,

    dialogContent: @Composable ColumnScope.() -> Unit,
    endContent: @Composable RowScope.() -> Unit = {},
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AppDialog(title = title, content = {
            // 与音频参数弹窗同款水平 4dp（≈16dp，音频参数弹窗为边距定调起源）
            Column(Modifier.padding(horizontal = 4.dp)) {
                dialogContent()
            }
        }, buttons = {
            TextButton(onClick = { showDialog = false }) {
                Text(stringResource(id = R.string.close))
            }
        }, onDismissRequest = { showDialog = false })
    }
    BasePreferenceWidget(modifier, onClick = {
        showDialog = true
    }, title = title, icon = icon, subTitle = subTitle) {
        endContent()
    }
}