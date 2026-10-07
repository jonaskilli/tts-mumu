package com.github.jing332.tts_server_android.compose.systts.replace

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.ConfigDeleteDialog
import com.github.jing332.compose.widgets.AppDropdownMenu

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
            // 10-07 统一插件卡/朗读规则页写法：视觉 17dp（0.85 缩放）、外层 48dp 盒吃触摸，
            // 内层 Checkbox 只画不摸（onCheckedChange=null）
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onToggleSelection() },
                    contentAlignment = Alignment.Center
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null,
                        modifier = Modifier.scale(0.85f),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(48.dp)
                        .toggleable(
                            value = isEnabled,
                            role = Role.Switch,
                            onValueChange = { onCheckedChange(it) }
                        )
                        .semantics {
                            context
                                .getString(
                                    if (isEnabled) R.string.rule_enabled_desc else R.string.rule_disabled_desc,
                                    name
                                )
                                .let { contentDescription = it }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Checkbox(
                        checked = isEnabled,
                        onCheckedChange = null,
                        modifier = Modifier.scale(0.85f),
                    )
                }
            }
            Text(
                name,
                // 特例许可（用户 2026-09-11 终裁）：回原版 15sp
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    // start 4 撤（10-05 审计：主页/朗读规则/插件共用的 list/Item 名字都在
                    // 对勾后 0 间距=卡内 48dp 线，替换页独多 4dp 成游离线）
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
                    // end=10 移除（10-04 ⋮ 对齐）：末键 48dp 热区贴卡缘，与主页配置项卡同构、跨页一致
                    IconButton(onClick = {
                        isMoreOptionsVisible = true
                    }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(id = R.string.more_options_desc, name),
                            tint = MaterialTheme.colorScheme.onBackground
                        )

                        AppDropdownMenu(expanded = isMoreOptionsVisible,
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
