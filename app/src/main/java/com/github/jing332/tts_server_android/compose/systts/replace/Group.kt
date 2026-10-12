package com.github.jing332.tts_server_android.compose.systts.replace

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.GroupItem

@Composable
internal fun Group(
    modifier: Modifier,
    name: String,
    isExpanded: Boolean,
    toggleableState: ToggleableState,
    onToggleableStateChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onSort:()->Unit,
    inSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
) {
    val context = LocalContext.current
    GroupItem(
        modifier = modifier.semantics{
            customActions = listOf(
                CustomAccessibilityAction(
                    label = context.getString(R.string.edit_desc, name),
                    action = { onEdit();true }
                ),
                CustomAccessibilityAction(
                    label = context.getString(R.string.sort),
                    action = { onSort();true }
                ),
                CustomAccessibilityAction(
                    label = context.getString(R.string.delete),
                    action = { onDelete();true }
                ),
                CustomAccessibilityAction(
                    label = context.getString(R.string.export_config),
                    action = { onExport();true }
                ),
            )
        },
        isExpanded = isExpanded,
        name = name,
        toggleableState = toggleableState,
        onToggleableStateChange = onToggleableStateChange,
        onClick = onClick,
        onExport = onExport,
        onDelete = onDelete,
        inSelectionMode = inSelectionMode,
        isSelected = isSelected,
        onToggleSelect = onToggleSelect,
        // 第9项: 替换规则多选时, 点击右侧方框=选中, 点击分组名/图标=展开
        selectOnRowClick = false,
        // 组名文字线对齐条目行（10-12 用户令「分组的文字和下面规则的名字，到底这几次都改的啥啊」）：
        // 条目行对勾盒写 `.size(24/32).requiredSize(48)`——**requiredSize 真的占 48dp 布局宽**
        //（依赖源码 SizeNode.measure 自证：返回 layout(placeable.width…)，placeable 按 48 约束量出），
        // 加卡外距 8 ⇒ 规则名文字线恒在 56dp；组头盒必须同样 48 才齐（默认 32 会让组名偏左 16dp）。
        arrowBoxWidth = 48.dp,
        actions = { dismiss ->
            DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        Icons.Filled.Edit, stringResource(R.string.edit),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                },
                text = { Text(stringResource(R.string.edit)) },
                onClick = {
                    dismiss()
                    onEdit.invoke()
                }
            )

            DropdownMenuItem(
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Default.Sort, stringResource(R.string.sort))
                },
                text = { Text(stringResource(R.string.sort)) },
                onClick = {
                    dismiss()
                    onSort()
                }
            )
        }
    )
}