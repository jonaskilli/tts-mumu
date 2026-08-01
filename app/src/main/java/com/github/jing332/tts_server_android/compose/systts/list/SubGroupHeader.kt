package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.ExpandCircleDown
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.github.jing332.tts_server_android.compose.systts.ConfigDeleteDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp

@Composable
fun SubGroupHeader(
    modifier: Modifier = Modifier,
    name: String,
    level: Int,
    isExpanded: Boolean,
    onClick: () -> Unit,
    toggleableState: ToggleableState = ToggleableState.Off,
    onToggleableStateChange: (Boolean) -> Unit = {},
    onRename: () -> Unit = {},
    onEditAudioParams: () -> Unit = {},
    onSort: () -> Unit = {},
    onBatchAssignTags: () -> Unit = {},
    hasTagKeyword: Boolean = false,
    onReassignTagsByGroupName: () -> Unit = {},
    onDelete: () -> Unit = {},
    onExport: () -> Unit = {},
    onDeleteEnabled: () -> Unit = {},
    onDeleteDisabled: () -> Unit = {},
    onExtractToGroup: () -> Unit = {},
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 0f else -45f,
        label = ""
    )

    // 按层级分配不同高度，让整体更协调
    val (paddingTop, paddingBottom) = when (level) {
        0 -> 10.dp to 8.dp
        1 -> 8.dp to 6.dp
        else -> 6.dp to 4.dp
    }

    var showOptions by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog)
        ConfigDeleteDialog(
            onDismissRequest = { showDeleteDialog = false }, content = name, onConfirm = onDelete
        )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (level == 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surface
            )
            .clickable { if (!showOptions) onClick() }
            .padding(
                start = 8.dp,
                top = paddingTop,
                bottom = paddingBottom,
                end = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.ExpandCircleDown,
            contentDescription = if (isExpanded) "收起" else "展开",
            modifier = Modifier
                .size(20.dp)
                .rotate(rotationAngle)
                .graphicsLayer { rotationZ = rotationAngle },
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = name,
            style = when (level) {
                0 -> MaterialTheme.typography.titleMedium
                1 -> MaterialTheme.typography.bodyLarge
                else -> MaterialTheme.typography.bodyMedium
            },
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        )

        TriStateCheckbox(
            state = toggleableState,
            onClick = {
                onToggleableStateChange(toggleableState != ToggleableState.On)
            },
        )

        Box {
            IconButton(onClick = { showOptions = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "更多选项"
                )
            }

            DropdownMenu(
                expanded = showOptions,
                onDismissRequest = { showOptions = false }
            ) {
                DropdownMenuItem(
                    text = { Text("重命名") },
                    onClick = {
                        showOptions = false
                        onRename()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.DriveFileRenameOutline, null)
                    }
                )

                DropdownMenuItem(
                    text = { Text("音频参数") },
                    onClick = {
                        showOptions = false
                        onEditAudioParams()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Speed, null)
                    }
                )

                DropdownMenuItem(
                    text = { Text("排序") },
                    onClick = {
                        showOptions = false
                        onSort()
                    },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Default.Sort, null)
                    }
                )

                DropdownMenuItem(
                    text = { Text("批量分配标签") },
                    onClick = {
                        showOptions = false
                        onBatchAssignTags()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Label, null)
                    }
                )

                if (hasTagKeyword) {
                    DropdownMenuItem(
                        text = { Text("按分组名一键分配标签") },
                        onClick = {
                            showOptions = false
                            onReassignTagsByGroupName()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Label, null)
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text("删除启用的配置") },
                    onClick = {
                        showOptions = false
                        onDeleteEnabled()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.DeleteForever, null)
                    }
                )

                DropdownMenuItem(
                    text = { Text("删除未启用的配置") },
                    onClick = {
                        showOptions = false
                        onDeleteDisabled()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.DeleteForever, null)
                    }
                )

                DropdownMenuItem(
                    text = { Text("转为一级分组") },
                    onClick = {
                        showOptions = false
                        onExtractToGroup()
                    },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, null)
                    }
                )

                DropdownMenuItem(
                    text = { Text("导出") },
                    onClick = {
                        showOptions = false
                        onExport()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Output, null)
                    }
                )

                HorizontalDivider()

                DropdownMenuItem(
                    text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showOptions = false
                        showDeleteDialog = true
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.DeleteForever,
                            null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
            }
        }
    }
}
