package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.github.jing332.database.dbm

/**
 * 第6项: 统一分组树选择器
 *
 * 支持"一级分组(大分组) + 子分组(categoryPath)"两级层级切换：
 * - 组内切换: 同一大分组下选择已有子分组，或新建子分组；
 * - 组外切换: 选择别的大分组，再选其子分组或新建子分组；
 * - 识别当前层级: 顶部摘要展示当前已选位置，默认展开并选中当前所在大分组/子分组。
 *
 * 选中结果通过 onConfirm(groupId, categoryPath) 回调。
 * categoryPath 为空字符串表示放在该大分组根目录(不设子分组)。
 */
@Composable
fun GroupTreePickerDialog(
    currentGroupId: Long,
    currentCategoryPath: String,
    onDismissRequest: () -> Unit,
    onConfirm: (groupId: Long, categoryPath: String) -> Unit,
) {
    val groups = remember { dbm.systemTtsV2.allGroup }
    // 每个大分组下已有的子分组路径
    val subPathsByGroup = remember(groups) {
        groups.associate { it.id to dbm.systemTtsV2.getCategoryPathsByGroup(it.id) }
    }

    var selectedGroupId by remember { mutableStateOf(currentGroupId) }
    var selectedCategoryPath by remember { mutableStateOf(currentCategoryPath) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var newSubGroupName by remember { mutableStateOf("") }
    // 默认展开当前所在大分组，便于识别当前位置
    var expandedGroups by remember { mutableStateOf(setOf(currentGroupId)) }

    val finalCategoryPath = if (isCreatingNew) newSubGroupName.trim() else selectedCategoryPath
    val selGroupName = groups.firstOrNull { it.id == selectedGroupId }?.name ?: ""

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth(0.9f),
        shape = RoundedCornerShape(16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text("选择分组") },
        text = {
            Column(modifier = Modifier
                .heightIn(max = 500.dp)
                .verticalScroll(rememberScrollState())
            ) {
                // 顶部摘要: 识别当前/已选层级位置
                val locationText = buildString {
                    append(selGroupName.ifBlank { "(未分组)" })
                    if (finalCategoryPath.isNotBlank()) {
                        append(" > ")
                        append(finalCategoryPath.replace("/", " > "))
                    }
                }
                Text(
                    text = if (isCreatingNew) "新建于: $locationText" else "当前选择: $locationText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                groups.forEach { group ->
                    val paths = subPathsByGroup[group.id] ?: emptyList()
                    val isExpanded = group.id in expandedGroups
                    val isGroupRootSelected = selectedGroupId == group.id &&
                        !isCreatingNew && selectedCategoryPath.isBlank()

                    // 大分组标题行: 点击选中该大分组(根目录); 左侧图标展开/收起其子分组
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isGroupRootSelected,
                                onClick = {
                                    selectedGroupId = group.id
                                    selectedCategoryPath = ""
                                    isCreatingNew = false
                                    newSubGroupName = ""
                                }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                expandedGroups = if (isExpanded) expandedGroups - group.id
                                else expandedGroups + group.id
                            }
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = if (isExpanded) "收起" else "展开",
                                modifier = Modifier.rotate(if (isExpanded) 90f else 0f)
                            )
                        }
                        RadioButton(
                            selected = isGroupRootSelected,
                            onClick = {
                                selectedGroupId = group.id
                                selectedCategoryPath = ""
                                isCreatingNew = false
                                newSubGroupName = ""
                            }
                        )
                        Text(
                            text = group.name.ifBlank { "默认分组" },
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    if (isExpanded) {
                        // 根目录选项(该大分组本身, 不设子分组)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = isGroupRootSelected,
                                    onClick = {
                                        selectedGroupId = group.id
                                        selectedCategoryPath = ""
                                        isCreatingNew = false
                                        newSubGroupName = ""
                                    }
                                )
                                .padding(start = 48.dp, top = 2.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isGroupRootSelected,
                                onClick = {
                                    selectedGroupId = group.id
                                    selectedCategoryPath = ""
                                    isCreatingNew = false
                                    newSubGroupName = ""
                                }
                            )
                            Text(
                                text = "（根目录，不设子分组）",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        // 已有子分组
                        paths.forEach { path ->
                            val isPathSelected = selectedGroupId == group.id &&
                                !isCreatingNew && selectedCategoryPath == path
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = isPathSelected,
                                        onClick = {
                                            selectedGroupId = group.id
                                            selectedCategoryPath = path
                                            isCreatingNew = false
                                            newSubGroupName = ""
                                        }
                                    )
                                    .padding(start = 48.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isPathSelected,
                                    onClick = {
                                        selectedGroupId = group.id
                                        selectedCategoryPath = path
                                        isCreatingNew = false
                                        newSubGroupName = ""
                                    }
                                )
                                Text(
                                    text = path.replace("/", " / "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }

                        // 新建子分组
                        val isCreateSelected = isCreatingNew && selectedGroupId == group.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = isCreateSelected,
                                    onClick = {
                                        selectedGroupId = group.id
                                        isCreatingNew = true
                                        selectedCategoryPath = ""
                                    }
                                )
                                .padding(start = 48.dp, top = 2.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isCreateSelected,
                                onClick = {
                                    selectedGroupId = group.id
                                    isCreatingNew = true
                                    selectedCategoryPath = ""
                                }
                            )
                            Icon(
                                Icons.Default.CreateNewFolder,
                                contentDescription = null,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                            Text(
                                text = "新建子分组",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                        if (isCreateSelected) {
                            OutlinedTextField(
                                value = newSubGroupName,
                                onValueChange = { newSubGroupName = it },
                                label = { Text("子分组名称 (支持 a/b 多级)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 48.dp, top = 4.dp),
                                singleLine = true
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedGroupId, finalCategoryPath) },
                enabled = !isCreatingNew || newSubGroupName.isNotBlank()
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}
