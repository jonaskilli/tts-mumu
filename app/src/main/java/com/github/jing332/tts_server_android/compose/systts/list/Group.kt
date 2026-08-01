package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Label
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
    onEditAudioParams: () -> Unit,
    onSort: () -> Unit,
    onEditContent: () -> Unit = {},
    onCreateSubGroup: () -> Unit = {},
    hasSubGroups: Boolean = false,
    hasTagKeyword: Boolean = false,
    onBatchAssignTags: () -> Unit = {},
    onReleaseSubGroup: () -> Unit = {},
    onConvertToSubGroup: () -> Unit = {},
    onExtractSubGroup: () -> Unit = {},
    onDeleteEnabled: () -> Unit = {},
    onDeleteDisabled: () -> Unit = {},
    onMoveEnabledToGroup: () -> Unit = {},
    onResortTagsByExisting: () -> Unit = {},
    onResortTagsFromZero: () -> Unit = {},
    onReassignTags: () -> Unit = {},
    onReassignTagsByGroupName: () -> Unit = {},
    onReassignAllSubGroups: () -> Unit = {},
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
                CustomAccessibilityAction(context.getString(R.string.audio_params)) {
                    onEditAudioParams();true
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

            DropdownMenuItem(text = { Text(stringResource(id = R.string.copy)) },
                onClick = {
                    dismiss()
                    showCopyDialog = true
                },
                leadingIcon = {
                    Icon(Icons.Default.ContentCopy, null)
                }
            )

            DropdownMenuItem(text = { Text(stringResource(id = R.string.audio_params)) },
                onClick = {
                    dismiss()
                    onEditAudioParams()
                },
                leadingIcon = {
                    Icon(Icons.Default.Speed, null)
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

            DropdownMenuItem(text = { Text(stringResource(id = R.string.edit_group_content)) },
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

            DropdownMenuItem(text = { Text("释放子分组") },
                onClick = {
                    dismiss()
                    onReleaseSubGroup()
                },
                leadingIcon = {
                    Icon(Icons.Default.AccountTree, null)
                }
            )

            DropdownMenuItem(text = { Text("转为子分组") },
                onClick = {
                    dismiss()
                    onConvertToSubGroup()
                },
                leadingIcon = {
                    Icon(Icons.Default.AccountTree, null)
                }
            )

            DropdownMenuItem(text = { Text("移出子分组") },
                onClick = {
                    dismiss()
                    onExtractSubGroup()
                },
                leadingIcon = {
                    Icon(Icons.Default.AccountTree, null)
                }
            )

            DropdownMenuItem(text = { Text(stringResource(id = R.string.batch_assign_tags)) },
                onClick = {
                    dismiss()
                    onBatchAssignTags()
                },
                leadingIcon = {
                    Icon(Icons.Default.Label, null)
                }
            )

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
                    Icon(Icons.Default.DriveFileMove, null)
                }
            )

            DropdownMenuItem(text = { Text("按原有序号重排标签") },
                onClick = {
                    dismiss()
                    onResortTagsByExisting()
                },
                leadingIcon = {
                    Icon(Icons.Default.Label, null)
                }
            )

            DropdownMenuItem(text = { Text("从01重排标签") },
                onClick = {
                    dismiss()
                    onResortTagsFromZero()
                },
                leadingIcon = {
                    Icon(Icons.Default.Label, null)
                }
            )

            DropdownMenuItem(text = { Text("重新分配标签(输入前缀)") },
                onClick = {
                    dismiss()
                    onReassignTags()
                },
                leadingIcon = {
                    Icon(Icons.Default.Label, null)
                }
            )

            // 仅当不含子分组且分组名含关键词时显示：按分组名一键分配标签
            if (!hasSubGroups && hasTagKeyword) {
                DropdownMenuItem(text = { Text("按分组名一键分配标签") },
                    onClick = {
                        dismiss()
                        onReassignTagsByGroupName()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Label, null)
                    }
                )
            }

            // 仅当含子分组时显示：整理全部子分组标签
            if (hasSubGroups) {
                DropdownMenuItem(text = { Text("整理全部子分组标签") },
                    onClick = {
                        dismiss()
                        onReassignAllSubGroups()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Label, null)
                    }
                )
            }
        }
    )

}
