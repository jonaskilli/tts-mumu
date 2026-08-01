package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.ExpandCircleDown
import androidx.compose.material.icons.automirrored.filled.Label
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
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
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
    onReleaseItems: () -> Unit = {},
    onMoveToOtherGroup: () -> Unit = {},
    onMoveEnabledToGroup: () -> Unit = {},
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
    var showExtraOptions by remember { mutableStateOf(false) }
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
            .clickable { if (!showOptions && !showExtraOptions) onClick() }
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
                0 -> MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp)
                1 -> MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp)
                else -> MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp)
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

        Box(
            modifier = Modifier
                .size(48.dp)
                .combinedClickable(
                    onClick = { showOptions = true },
                    onLongClick = { showExtraOptions = true }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "更多选项"
            )

            // 长按菜单：仅启用相关功能
            DropdownMenu(
                expanded = showExtraOptions,
                onDismissRequest = { showExtraOptions = false }
            ) {
                DropdownMenuItem(
                    text = { Text("删除启用的配置") },
                    onClick = {
                        showExtraOptions = false
                        onDeleteEnabled()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.DeleteForever, null)
                    }
                )

                DropdownMenuItem(
                    text = { Text("删除未启用的配置") },
                    onClick = {
                        showExtraOptions = false
                        onDeleteDisabled()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.DeleteForever, null)
                    }
                )

                DropdownMenuItem(
                    text = { Text("移动启用配置到其他分组") },
                    onClick = {
                        showExtraOptions = false
                        onMoveEnabledToGroup()
                    },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.DriveFileMove, null)
                    }
                )
            }

            // 主菜单：与启用状态无关的功能
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

                if (hasTagKeyword) {
                    DropdownMenuItem(
                        text = { Text("一键整理标签") },
                        onClick = {
                            showOptions = false
                            onReassignTagsByGroupName()
                        },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.Label, null)
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text("释放配置项") },
                    onClick = {
                        showOptions = false
                        onReleaseItems()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.AccountTree, null)
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
                    text = { Text("移动到其他一级分组") },
                    onClick = {
                        showOptions = false
                        onMoveToOtherGroup()
                    },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.DriveFileMove, null)
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
