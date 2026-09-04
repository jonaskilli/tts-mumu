package com.github.jing332.tts_server_android.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
 * 设置分组（返璞归真批次改为直通渲染）：不再出卡片壳与组标题，
 * 内容直接平铺进设置页——原版设置页就是无分组结构。
 * 保留 [show] 语义：搜索模式下调用方本就以 show=false 退平铺，行为不变。
 */
@Composable
internal fun SettingsGroup(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    show: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(content = content)
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
    content: @Composable RowScope.() -> Unit = {},
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
            content()
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
        ModalBottomSheet(onDismissRequest = { show = false }) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxHeight(0.4f)
            ) {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.titleLarge
                ) { title() }
                LabelSlider(
                    modifier = Modifier
                        .padding(vertical = 16.dp),
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    steps = steps,
                    buttonSteps = buttonSteps,
                    buttonLongSteps = buttonLongSteps,
                    text = label
                )
            }
        }

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
            Column {
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