package com.github.jing332.tts_server_android.compose.systts.replace

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.ConfigDeleteDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun Item(
    name: String,
    modifier: Modifier,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveTop: () -> Unit,
    onMoveBottom: () -> Unit,
    onExport: () -> Unit = {},
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    // 第3项: 多选删除支持(与朗读规则/插件一致)
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelection: () -> Unit = {},
) {
    val context = LocalContext.current
    var deleteDialog by remember { mutableStateOf(false) }
    if (deleteDialog)
        ConfigDeleteDialog(onDismissRequest = { deleteDialog = false }, content = name) {
            onDelete()
        }

    // 第3项: 多选模式下卡片点击切换选中, 长按进入多选; 非多选模式保持原行为
    val cardModifier = if (isSelectionMode) {
        modifier.combinedClickable(
            onClick = onToggleSelection,
            onLongClick = onToggleSelection
        )
    } else {
        // 非多选模式: ElevatedCard 的 onClick 仍由参数提供
        modifier
    }

    ElevatedCard(
        onClick = if (isSelectionMode) ({ /* 点击由 combinedClickable 处理 */ }) else onClick,
        modifier = cardModifier
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 多选模式显示选中状态, 非多选模式显示启用开关
            if (isSelectionMode) {
                Checkbox(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() }
                )
            } else {
                Checkbox(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .semantics {
                            role = Role.Switch
                            context
                                .getString(
                                    if (isEnabled) R.string.rule_enabled_desc else R.string.rule_disabled_desc,
                                    name
                                )
                                .let {
                                    contentDescription = it
                                    stateDescription = it
                                }
                        },
                    checked = isEnabled,
                    onCheckedChange = onCheckedChange
                )
            }
            Text(
                name,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterVertically),
            )
            // 多选模式隐藏编辑/更多操作
            if (!isSelectionMode) {
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, stringResource(id = R.string.edit_desc, name))
                    }
                    var isMoreOptionsVisible by remember { mutableStateOf(false) }
                    IconButton(onClick = {
                        isMoreOptionsVisible = true
                    }, modifier = Modifier.padding(end = 10.dp)) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(id = R.string.more_options_desc, name),
                            tint = MaterialTheme.colorScheme.onBackground
                        )

                        DropdownMenu(expanded = isMoreOptionsVisible,
                            onDismissRequest = { isMoreOptionsVisible = false }) {

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.move_to_top)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.VerticalAlignTop,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    onMoveTop()
                                    isMoreOptionsVisible = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.move_to_bottom)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.VerticalAlignBottom,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    onMoveBottom()
                                    isMoreOptionsVisible = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_config)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.IosShare,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    onExport()
                                    isMoreOptionsVisible = false
                                }
                            )

                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.DeleteForever,
                                        null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    isMoreOptionsVisible = false
                                    deleteDialog = true
                                }
                            )

                        }
                    }
                }
            }
        }
    }

}
