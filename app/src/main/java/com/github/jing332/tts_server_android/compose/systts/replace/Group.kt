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