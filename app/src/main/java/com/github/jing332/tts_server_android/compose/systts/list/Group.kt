package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import com.github.jing332.compose.widgets.TextFieldDialog
import com.github.jing332.database.entities.systts.SystemTtsGroup
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.GroupItem

@Composable
fun Group(
    modifier: Modifier,
    name: String,
    group: SystemTtsGroup,
    isExpanded: Boolean,
    toggleableState: ToggleableState,
    onToggleableStateChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    inSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onRename: (newName: String) -> Unit,
    onCopy: (newName: String) -> Unit = {},
    onSort: () -> Unit,
    onEditContent: () -> Unit = {},
    onCreateSubGroup: () -> Unit = {},
    hasSubGroups: Boolean = false,
    hasTagKeyword: Boolean = false,
    onBatchAssignTags: () -> Unit = {},
    onConvertToSubGroup: () -> Unit = {},
    onConvertSubGroupsToTopLevel: () -> Unit = {},
    onExtractSubGroup: () -> Unit = {},
    onMoveSubGroups: () -> Unit = {},
    onDeleteEnabled: () -> Unit = {},
    onDeleteDisabled: () -> Unit = {},
    onMoveEnabledToGroup: () -> Unit = {},
    onResortTagsByExisting: () -> Unit = {},
    onResortTagsFromZero: () -> Unit = {},
    onReassignTags: () -> Unit = {},
    onReassignTagsByGroupName: () -> Unit = {},
    onReassignAllSubGroups: () -> Unit = {},
    onMergeGroup: (() -> Unit)? = null,
    onRenameSubPrefix: (() -> Unit)? = null,
    // 一级分组序号徽章（纯UI，按列表顺序自动编号）
    index: Int = -1,
    itemCount: Int = -1,
) {

    var showRenameDialog by remember { mutableStateOf(false) }
    if (showRenameDialog) {
        var nameValue by remember { mutableStateOf(name) }
        TextFieldDialog(
            title = stringResource(id = R.string.rename),
            text = nameValue,
            onTextChange = { nameValue = it },
            onDismissRequest = { showRenameDialog = false }) {
            showRenameDialog = false
            onRename(nameValue)
        }
    }

    var showCopyDialog by remember { mutableStateOf(false) }
    if (showCopyDialog) {
        var nameValue by remember { mutableStateOf(name) }
        TextFieldDialog(
            title = stringResource(id = R.string.copy),
            text = nameValue,
            onTextChange = { nameValue = it },
            onDismissRequest = { showCopyDialog = false }) {
            showCopyDialog = false
            onCopy(nameValue)
        }
    }

    val context = LocalContext.current
    var showEditContentDialog by remember { mutableStateOf(false) }

    if (showEditContentDialog) {
        GroupEditContentDialog(
            group = group,
            onDismissRequest = { showEditContentDialog = false }
        )
    }

    GroupItem(
        modifier = modifier.semantics {
            customActions = listOf(
                CustomAccessibilityAction(context.getString(R.string.rename)) {
                    showRenameDialog = true;true
                },
                CustomAccessibilityAction(context.getString(R.string.copy)) {
                    showCopyDialog = true;true
                },
                CustomAccessibilityAction(context.getString(R.string.sort)) {
                    onSort();true
                },
                CustomAccessibilityAction(context.getString(R.string.delete)) {
                    onDelete();true
                },
                CustomAccessibilityAction(context.getString(R.string.export_config)) {
                    onExport();true
                },
                CustomAccessibilityAction(context.getString(R.string.edit_group_content)) {
                    showEditContentDialog = true;true
                },
                CustomAccessibilityAction(context.getString(R.string.batch_assign_tags)) {
                    onBatchAssignTags();true
                }
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
        itemCount = itemCount,
        hasSubGroups = hasSubGroups,
        index = index,
        extraActions = { dismiss ->
            DropdownMenuItem(text = { Text("删除启用的配置") },
                onClick = {
                    dismiss()
                    onDeleteEnabled()
                },
                leadingIcon = { Icon(Icons.Default.DeleteForever, null) }
            )

            DropdownMenuItem(text = { Text("删除未启用的配置") },
                onClick = {
                    dismiss()
                    onDeleteDisabled()
                },
                leadingIcon = { Icon(Icons.Default.DeleteForever, null) }
            )

            DropdownMenuItem(text = { Text("移动启用配置到其他分组") },
                onClick = {
                    dismiss()
                    onMoveEnabledToGroup()
                },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.DriveFileMove, null)
                }
            )
        },
        actions = { dismiss ->
            DropdownMenuItem(text = { Text(stringResource(id = R.string.rename)) },
                onClick = {
                    dismiss()
                    showRenameDialog = true
                },
                leadingIcon = {
                    Icon(Icons.Default.DriveFileRenameOutline, null)
                }
            )

            DropdownMenuItem(text = { Text(stringResource(id = R.string.sort)) },
                onClick = {
                    dismiss()
                    onSort()
                },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Default.Sort, null)
                }
            )

            DropdownMenuItem(text = { Text("编辑分组") },
                onClick = {
                    dismiss()
                    showEditContentDialog = true
                },
                leadingIcon = {
                    Icon(Icons.Default.Edit, null)
                }
            )

            DropdownMenuItem(text = { Text(stringResource(id = R.string.create_sub_group)) },
                onClick = {
                    dismiss()
                    onCreateSubGroup()
                },
                leadingIcon = {
                    Icon(Icons.Default.AccountTree, null)
                }
            )

            // 转为子分组：仅一级分组无子分组时显示
            if (!hasSubGroups) {
                DropdownMenuItem(text = { Text("转为子分组") },
                    onClick = {
                        dismiss()
                        onConvertToSubGroup()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.AccountTree, null)
                    }
                )
            }

            // 移动子分组：仅含子分组的一级分组显示，移动子分组到其他一级分组
            if (hasSubGroups) {
                DropdownMenuItem(text = { Text("移动子分组") },
                    onClick = {
                        dismiss()
                        onMoveSubGroups()
                    },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.DriveFileMove, null)
                    }
                )
            }

            // 转为一级分组：仅含子分组的一级分组显示，多选子分组各自转为独立一级分组
            if (hasSubGroups) {
                DropdownMenuItem(text = { Text("转为一级分组") },
                    onClick = {
                        dismiss()
                        onConvertSubGroupsToTopLevel()
                    },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, null)
                    }
                )
            }

            // 一键整理标签：一级分组无子分组且名字匹配关键词时显示
            if (!hasSubGroups && hasTagKeyword) {
                DropdownMenuItem(text = { Text("一键整理标签") },
                    onClick = {
                        dismiss()
                        onReassignTagsByGroupName()
                    },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.Label, null)
                    }
                )
            }

            // 整理全部子分组标签：仅含子分组时显示
            if (hasSubGroups) {
                DropdownMenuItem(text = { Text("整理全部子分组标签") },
                    onClick = {
                        dismiss()
                        onReassignAllSubGroups()
                    },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.Label, null)
                    }
                )
            }

            // 合并到其他分组：将本分组的配置项按 categoryPath 匹配归入目标分组
            if (onMergeGroup != null) {
                DropdownMenuItem(text = { Text("合并到其他分组") },
                    onClick = {
                        dismiss()
                        onMergeGroup!!()
                    },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.DriveFileMove, null)
                    }
                )
            }

            // 修改子分组前缀：批量替换子分组名开头文字(加/去/换前缀)
            if (onRenameSubPrefix != null) {
                DropdownMenuItem(text = { Text("修改子分组前缀") },
                    onClick = {
                        dismiss()
                        onRenameSubPrefix!!()
                    },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.Label, null)
                    }
                )
            }
        }
    )

}
