package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ExpandCircleDown
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
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
    // 分组列表与主界面一致：按 order 排序
    val groups = remember { dbm.systemTtsV2.allGroup.sortedBy { it.order } }
    // 子分组顺序与主界面一致：按该子分组内首个配置项的 order 排序（拖动整块子分组时的实际顺序）
    val subPathsByGroup = remember(groups) {
        groups.associate { group ->
            group.id to dbm.systemTtsV2.getByGroup(group.id)
                .filter { it.categoryPath.isNotBlank() }
                .sortedBy { it.order }
                .map { it.categoryPath }
                .distinct()
        }
    }

    var selectedGroupId by remember { mutableStateOf(currentGroupId) }
    var selectedCategoryPath by remember { mutableStateOf(currentCategoryPath) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var newSubGroupName by remember { mutableStateOf("") }
    // 默认展开当前所在大分组，便于识别当前位置
    var expandedGroups by remember { mutableStateOf(setOf(currentGroupId)) }

    // 初始校验：若当前所在大分组已含子分组，但当前项却在根目录(历史数据/混放)，
    // 则清空根目录选中，强制用户选一个子分组，避免继续混放
    LaunchedEffect(Unit) {
        val curPaths = subPathsByGroup[currentGroupId] ?: emptyList()
        if (curPaths.isNotEmpty() && currentCategoryPath.isBlank()) {
            isCreatingNew = true
        }
    }

    val finalCategoryPath = if (isCreatingNew) newSubGroupName.trim() else selectedCategoryPath
    val selGroupName = groups.firstOrNull { it.id == selectedGroupId }?.name ?: ""
    // 目标大分组含子分组时，禁止确认根目录选中（只能选子分组或新建）
    val targetHasSubGroups = (subPathsByGroup[selectedGroupId] ?: emptyList()).isNotEmpty()
    val isRootSelectedInvalid = targetHasSubGroups && !isCreatingNew && selectedCategoryPath.isBlank()

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
                    // 含子分组的大分组禁止选择根目录（避免配置项与子分组混放），只能选子分组或新建
                    val canSelectRoot = paths.isEmpty()
                    val isGroupRootSelected = selectedGroupId == group.id &&
                        !isCreatingNew && selectedCategoryPath.isBlank()

                    // 大分组标题行: 可选根目录时点击选中并展开; 不可选根目录(含子分组)时点击仅展开/收起
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isGroupRootSelected,
                                enabled = canSelectRoot,
                                onClick = {
                                    selectedGroupId = group.id
                                    selectedCategoryPath = ""
                                    isCreatingNew = false
                                    newSubGroupName = ""
                                    expandedGroups = setOf(group.id)
                                }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val rotationAngle by animateFloatAsState(
                            targetValue = if (isExpanded) 0f else -45f,
                            label = "groupExpandRotation"
                        )
                        Icon(
                            Icons.Default.ExpandCircleDown,
                            contentDescription = if (isExpanded) "收起" else "展开",
                            modifier = Modifier
                                .clickable {
                                    // 手风琴: 同时只展开一个一级分组
                                    expandedGroups = if (isExpanded) expandedGroups - group.id
                                    else setOf(group.id)
                                }
                                .rotate(rotationAngle)
                                .graphicsLayer { rotationZ = rotationAngle }
                        )
                        RadioButton(
                            selected = isGroupRootSelected,
                            enabled = canSelectRoot,
                            onClick = {
                                selectedGroupId = group.id
                                selectedCategoryPath = ""
                                isCreatingNew = false
                                newSubGroupName = ""
                                expandedGroups = setOf(group.id)
                            }
                        )
                        Text(
                            text = group.name.ifBlank { "默认分组" },
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    if (isExpanded) {
                        // 根目录选项(该大分组本身, 不设子分组): 仅当该大分组无子分组时才显示
                        if (canSelectRoot) {
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
                                    .padding(start = 64.dp, top = 2.dp, bottom = 2.dp),
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
                        }

                        // 已有子分组
                        paths.forEach { path ->
                            val isPathSelected = selectedGroupId == group.id &&
                                !isCreatingNew && selectedCategoryPath == path
                            // 层级越深(路径中 / 越多)，向右缩进越多，树状层级更明显
                            val pathDepth = path.count { it == '/' }
                            val subIndent = 64.dp + 20.dp * pathDepth
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
                                    .padding(start = subIndent, top = 2.dp, bottom = 2.dp),
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
                                .padding(start = 64.dp, top = 2.dp, bottom = 2.dp),
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
                                label = { Text("子分组名称") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 64.dp, top = 4.dp),
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
                enabled = !isRootSelectedInvalid && (!isCreatingNew || newSubGroupName.isNotBlank())
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
