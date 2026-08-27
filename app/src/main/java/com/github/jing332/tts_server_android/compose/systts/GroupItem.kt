package com.github.jing332.tts_server_android.compose.systts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExpandCircleDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Output
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.jing332.tts_server_android.R

fun Int.sizeToToggleableState(total: Int): ToggleableState = when (this) {
    0 -> ToggleableState.Off
    total -> ToggleableState.On
    else -> ToggleableState.Indeterminate
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GroupItem(
    modifier: Modifier,
    isExpanded: Boolean,
    name: String,
    toggleableState: ToggleableState,
    onToggleableStateChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    inSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    itemCount: Int = -1,
    // 是否含子分组：用于视觉区分含子分组/不含子分组的一级分组
    hasSubGroups: Boolean = false,
    // 序号徽章：>=1 时在名字前渲染，按列表顺序自动编号，纯UI不写进分组名
    index: Int = -1,
    actions: @Composable ColumnScope.(() -> Unit) -> Unit,
    extraActions: (@Composable ColumnScope.(() -> Unit) -> Unit)? = null,
    // 多选模式下点击分组名/行是否切换选中：true=点击行选中(系统TTS默认), false=仅右侧方框选中, 点击名/行展开(替换规则)
    selectOnRowClick: Boolean = true,
) {
    val view = LocalView.current
    val context = LocalContext.current

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(false) }
    var showExtraOptions by remember { mutableStateOf(false) }
    if (showDeleteDialog)
        ConfigDeleteDialog(
            onDismissRequest = { showDeleteDialog = false }, content = name, onConfirm = onDelete
        )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .semantics(true) {
                stateDescription =  when (toggleableState) {
                    ToggleableState.On -> context.getString(R.string.group_all_enabled, "")
                    ToggleableState.Off -> context.getString(R.string.group_all_disabled, "")
                    else -> context.getString(R.string.group_part_enabled, "")
                }

                customActions =
                    listOf(
                        CustomAccessibilityAction(
                            label = context.getString(R.string.delete), action = { onDelete();true }
                        ),

                        CustomAccessibilityAction(
                            label = context.getString(R.string.export_config), action = { onExport();true }
                        )
                    )


                if (isExpanded) {
                    collapse(context.getString(R.string.desc_collapse_group, name)) {
                        onClick()
                        true
                    }
                } else
                    expand(context.getString(R.string.desc_expand_group, name)) {
                        onClick()
                        true
                    }

            }
            .clickable {
                if (!showOptions && !showExtraOptions) {
                    if (inSelectionMode) {
                        // 替换规则: 多选时点击行=展开; 系统TTS: 点击行=选中
                        if (selectOnRowClick) onToggleSelect() else onClick()
                    } else onClick()
                }
            }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val rotationAngle by animateFloatAsState(
            targetValue = if (isExpanded) 0f else -45f,
            label = ""
        )
        Icon(
            Icons.Default.ExpandCircleDown,
            contentDescription = stringResource(if (isExpanded) R.string.desc_collapse_group else R.string.desc_expand_group, name),
            modifier = Modifier
                .rotate(rotationAngle)
                .graphicsLayer { rotationZ = rotationAngle }
                .clickable { onClick() }
        )

        // 序号徽章：按当前列表顺序自动编号，排序变化自动重编
        // 纯信息元素不用 primaryContainer 抢交互元素的注意力，用低饱和 surfaceVariant 同色系
        if (index >= 1) {
            Text(
                "$index",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 6.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Text(
            name,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .weight(1f)
        )
        // 含子分组的一级分组：标题旁显示子分组图标作为视觉区分
        if (hasSubGroups) {
            Icon(
                imageVector = Icons.Default.AccountTree,
                contentDescription = "含子分组",
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(18.dp)
                    .padding(end = 4.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        // 分组内配置项数量
        if (itemCount >= 0) {
            Text(
                "($itemCount)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 4.dp)
            )
        }
        Row {
            if (inSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() }
                )
            } else {
            TriStateCheckbox(
                state = toggleableState,
                onClick = {
                    onToggleableStateChange(toggleableState != ToggleableState.On)
                },
                modifier = Modifier.semantics {
                    stateDescription = context.getString(
                        when (toggleableState) {
                            ToggleableState.On -> R.string.group_all_enabled
                            ToggleableState.Off -> R.string.group_all_disabled
                            else -> R.string.group_part_enabled
                        }, name
                    )
                }
            )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .combinedClickable(
                        onClick = { showOptions = true },
                        onLongClick = { if (extraActions != null) showExtraOptions = true }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(id = R.string.more_options_desc, name)
                )

                // 长按扩展菜单：底部弹窗形式（动作少但重要性高的批量操作）
                if (showExtraOptions) {
                    ModalBottomSheet(onDismissRequest = { showExtraOptions = false }) {
                        Text(
                            name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
                        )
                        HorizontalDivider()
                        if (extraActions != null) extraActions!! { showExtraOptions = false }
                    }
                }

                // 主菜单：底部弹窗形式，拇指热区 + 长动作列表更友好
                if (showOptions) {
                    ModalBottomSheet(onDismissRequest = { showOptions = false }) {
                        Text(
                            name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
                        )
                        HorizontalDivider()
                        actions { showOptions = false }

                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.export_config)) },
                        onClick = {
                            showOptions = false
                            onExport()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Output, stringResource(R.string.export_config))
                        }
                    )

                    HorizontalDivider()

                    DropdownMenuItem(text = {
                        Text(
                            stringResource(id = R.string.delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                        leadingIcon = {
                            Icon(
                                Icons.Default.DeleteForever,
                                stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showOptions = false
                            showDeleteDialog = true
                        }
                    )
                    }
                }
            }
        }

    }
}