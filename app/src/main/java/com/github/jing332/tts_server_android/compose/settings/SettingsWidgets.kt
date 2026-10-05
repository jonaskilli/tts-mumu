package com.github.jing332.tts_server_android.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
 * 设置分区：**只出文字小标题，不出卡片壳**。
 * 历史：050a759「返璞归真」把本组件从「卡片壳+标题」删成纯直通（无标题），
 * 设置页遂成一片平铺；10-05 用户要求分类整理（参考墨听设置页），
 * 只恢复**标题**（用户明确不要卡片壳）——条目仍平铺，靠标题分段跳读。
 * 保留 [show]：搜索模式下调用方以 show=false 退平铺（不显示标题），行为不变。
 *
 * 10-05 追加折叠：[collapsible]=true 时标题行整行可点，右侧（箭头在标题左，与分组/子分组头同习语）
 * 出 ▾/▸ 指示；[defaultExpanded]=false 即默认收起（用户令：设置页条目太多，
 * 「数据与关于」无关紧要，折叠起来）。搜索态（show=false）**一律平铺直出**——
 * 搜到的条目不能被折叠藏起来。
 */
@Composable
internal fun SettingsGroup(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    show: Boolean = true,
    collapsible: Boolean = false,
    defaultExpanded: Boolean = true,
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
                        start = horizontalPadding + 4.dp,
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
        if (!collapsible || expanded) Column(content = content)
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
        .padding(horizontal = horizontalPadding, vertical = verticalPadding)
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