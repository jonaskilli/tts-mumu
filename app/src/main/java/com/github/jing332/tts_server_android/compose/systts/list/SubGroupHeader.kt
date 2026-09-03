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
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.AutoFixHigh
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
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import com.github.jing332.compose.widgets.AppDropdownMenu

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
    onSort: () -> Unit = {},
    onBatchAssignTags: () -> Unit = {},
    hasTagKeyword: Boolean = false,
    onReassignTagsByGroupName: () -> Unit = {},
    onDelete: () -> Unit = {},
    onExport: () -> Unit = {},
    onDeleteEnabled: () -> Unit = {},
    onDeleteDisabled: () -> Unit = {},
    onExtractToGroup: () -> Unit = {},
    onMoveToOtherGroup: () -> Unit = {},
    onMoveEnabledToGroup: () -> Unit = {},
    itemCount: Int = -1,
    // 该子分组下是否还有下级子分组（与 itemCount>0 一起决定树图标显示）
    hasSubGroups: Boolean = false,
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 0f else -90f,
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
            // 统一淡面板色(与编辑页SectionCard同款)：不再按层级阶梯涂色,层级靠缩进体现
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .clickable { if (!showOptions && !showExtraOptions) onClick() }
            .padding(
                // 按层级水平缩进，每级 12dp
                start = (8 + level * 12).dp,
                top = paddingTop,
                bottom = paddingBottom,
                end = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "收起" else "展开",
            modifier = Modifier
                .size(20.dp)
                .rotate(rotationAngle),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = name,
            style = when (level) {
                0 -> MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp)
                1 -> MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp)
                else -> MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
            },
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        )

        // 含下级子分组（下方可展开）的子分组：行右侧树图标（15dp 主题色，与一级分组行一致）；直接是配置项的不带
        if (hasSubGroups) {
            Icon(
                imageVector = Icons.Default.AccountTree,
                contentDescription = "含下级内容",
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(15.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // 子分组内配置项数量
        // （行内魔棒快捷键已删：单击立即整理无确认，易误触把整组标签改掉；功能保留在右侧菜单「一键整理标签」）
        if (itemCount >= 0) {
            Text(
                "($itemCount)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        TriStateCheckbox(
            state = toggleableState,
            onClick = {
                // 半选态单击直接全部取消（旧逻辑半选→先全选，会触发跨分组标签去重顶掉其他分组）
                    onToggleableStateChange(toggleableState == ToggleableState.Off)
            },
        )

        Box(
            modifier = Modifier
                .size(48.dp)
                .combinedClickable(
                    onClick = { showOptions = true },
                    onLongClick = { if (itemCount > 0) showExtraOptions = true }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "更多选项"
            )

            // 长按菜单：仅启用相关功能，仅组内非空时显示
            AppDropdownMenu(
                expanded = showExtraOptions,
                onDismissRequest = { showExtraOptions = false }
            ) {
                if (itemCount > 0) {
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
            }

            // 主菜单：与启用状态无关的功能
            AppDropdownMenu(
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

                if (itemCount > 0) DropdownMenuItem(
                    text = { Text("排序") },
                    onClick = {
                        showOptions = false
                        onSort()
                    },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Default.Sort, null)
                    }
                )

                // 一键整理标签：子分组名含关键词且组内非空时显示
                if (hasTagKeyword && itemCount > 0) {
                    DropdownMenuItem(
                        text = { Text("一键整理标签") },
                        onClick = {
                            showOptions = false
                            onReassignTagsByGroupName()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.AutoFixHigh, null)
                        }
                    )
                }

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

                if (itemCount > 0) DropdownMenuItem(
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
