package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
 * 选中态（"高亮版"，用户 09-12 终裁，替代旧"全员单选圈"方案）：
 * - 一级分组行=纯容器（箭头+组名+配置项数），不放任何选择控件，点行=展开/收起（手风琴）；
 *   病根：旧版给容器发单选圈，含子分组时父行圈恒禁用=满屏灰圈像全没选，且圈偏大；
 * - 目的地行（子分组/根目录/新建）不放圈，选中=整行铺 primaryContainer 底色+文字主题色
 *   （MD3 选中容器色标准用法）；
 * - 折叠定位：某组内含当前选择而该组被折叠时，组名染主题色，一眼看出选中藏在哪。
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
    // 每组配置项数（容器行右侧计数，与主界面 GroupItem "(N)" 同口径）
    val itemCountByGroup = remember(groups) {
        groups.associate { it.id to dbm.systemTtsV2.getByGroup(it.id).size }
    }

    var selectedGroupId by remember { mutableStateOf(currentGroupId) }
    var selectedCategoryPath by remember { mutableStateOf(currentCategoryPath) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var newSubGroupName by remember { mutableStateOf("") }
    // 新建一级分组模式：与组内"新建子分组"互斥，确认时真实落库并选中
    var isCreatingNewGroup by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
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
        // 用户 09-11 晚终裁：列表类弹窗宽度回宽版（0.9 屏宽，原版观感）；圆角维持 MD3 官方
        modifier = Modifier.fillMaxWidth(0.9f),
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
                    text = when {
                        isCreatingNewGroup -> "新建分组: ${newGroupName.trim().ifBlank { "(未命名)" }}"
                        isCreatingNew -> "新建于: $locationText"
                        else -> "当前选择: $locationText"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                groups.forEach { group ->
                    val paths = subPathsByGroup[group.id] ?: emptyList()
                    val isExpanded = group.id in expandedGroups
                    // 该组内含当前选择（选中的是它的子分组/正在它下面新建）：
                    // 折叠时组名染主题色，折叠也一眼看出选中藏在哪（用户 09-12）
                    val holdsSelection = selectedGroupId == group.id && !isCreatingNewGroup &&
                        (isCreatingNew || selectedCategoryPath.isNotBlank())

                    // 一级分组行：纯容器（高亮版，用户 09-12 终裁）——不放选择控件，点行=展开/收起
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isCreatingNewGroup = false
                                // 手风琴: 同时只展开一个一级分组
                                expandedGroups = if (isExpanded) expandedGroups - group.id
                                else setOf(group.id)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val rotationAngle by animateFloatAsState(
                            targetValue = if (isExpanded) 0f else -90f,
                            label = "groupExpandRotation"
                        )
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "收起" else "展开",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable {
                                    expandedGroups = if (isExpanded) expandedGroups - group.id
                                    else setOf(group.id)
                                }
                                .rotate(rotationAngle)
                        )
                        Text(
                            text = group.name.ifBlank { "默认分组" },
                            style = MaterialTheme.typography.titleMedium,
                            color = if (!isExpanded && holdsSelection) MaterialTheme.colorScheme.primary
                            else Color.Unspecified,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .weight(1f)
                        )
                        // 组内配置项数（与主界面 GroupItem "(N)" 同口径）
                        Text(
                            "(${itemCountByGroup[group.id] ?: 0})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    if (isExpanded) {
                        // 根目录目的地行: 仅当该大分组无子分组时才有（含子分组禁止回根目录，防混放——业务规则不变）
                        if (paths.isEmpty()) {
                            val isGroupRootSelected = selectedGroupId == group.id &&
                                !isCreatingNew && !isCreatingNewGroup && selectedCategoryPath.isBlank()
                            DestinationRow(
                                selected = isGroupRootSelected,
                                text = "（根目录，不设子分组）",
                                indent = 64.dp,
                                onClick = {
                                    selectedGroupId = group.id
                                    selectedCategoryPath = ""
                                    isCreatingNew = false
                                    newSubGroupName = ""
                                    isCreatingNewGroup = false
                                }
                            )
                        }

                        // 已有子分组：层级越深(路径中 / 越多)，向右缩进越多
                        paths.forEach { path ->
                            val isPathSelected = selectedGroupId == group.id &&
                                !isCreatingNew && !isCreatingNewGroup && selectedCategoryPath == path
                            val pathDepth = path.count { it == '/' }
                            DestinationRow(
                                selected = isPathSelected,
                                text = path.replace("/", " / "),
                                indent = 64.dp + 20.dp * pathDepth,
                                onClick = {
                                    selectedGroupId = group.id
                                    selectedCategoryPath = path
                                    isCreatingNew = false
                                    newSubGroupName = ""
                                    isCreatingNewGroup = false
                                }
                            )
                        }

                        // 新建子分组（目的地之一）
                        val isCreateSelected = isCreatingNew && selectedGroupId == group.id
                        DestinationRow(
                            selected = isCreateSelected,
                            text = "新建子分组",
                            indent = 64.dp,
                            leadingIcon = Icons.Default.CreateNewFolder,
                            onClick = {
                                selectedGroupId = group.id
                                isCreatingNew = true
                                isCreatingNewGroup = false
                                selectedCategoryPath = ""
                            }
                        )
                        if (isCreateSelected) {
                            OutlinedTextField(
                                value = newSubGroupName,
                                onValueChange = { newSubGroupName = it },
                                label = { Text("子分组名称") },
                                // 多级提示从无到有补齐（与 CreateSubGroupDialog 同款，用户 09-12 统一）
                                supportingText = { Text("名称含 / 时一次建多级，如 讯飞/女青年") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 64.dp, top = 4.dp),
                                singleLine = true
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }

                // 新建一级分组：独立于所有已有大分组的尾部入口，确认时落库并选中
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                val isCreateGroupSelected = isCreatingNewGroup
                DestinationRow(
                    selected = isCreateGroupSelected,
                    text = "新建分组",
                    indent = 16.dp,
                    leadingIcon = Icons.Default.CreateNewFolder,
                    onClick = {
                        isCreatingNewGroup = true
                        isCreatingNew = false
                        selectedCategoryPath = ""
                    }
                )
                if (isCreateGroupSelected) {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("分组名称") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isCreatingNewGroup) {
                        // 新建一级分组：真实落库（与主界面"添加分组"同一写入方式），
                        // 随后直接选中新分组返回；categoryPath 为空 = 该组根目录
                        val newGroup = com.github.jing332.database.entities.systts.SystemTtsGroup(
                            name = newGroupName.trim(),
                            order = dbm.systemTtsV2.groupCount
                        )
                        dbm.systemTtsV2.insertGroup(newGroup)
                        onConfirm(newGroup.id, "")
                    } else {
                        onConfirm(selectedGroupId, finalCategoryPath)
                    }
                },
                enabled = when {
                    isCreatingNewGroup -> newGroupName.isNotBlank()
                    isCreatingNew -> newSubGroupName.isNotBlank()
                    else -> !isRootSelectedInvalid
                }
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

/**
 * 目的地行（高亮版，用户 09-12 终裁）：不放单选圈。
 * 选中态 = 整行铺 primaryContainer 底色 + 文字 onPrimaryContainer（MD3 选中容器色标准用法）；
 * 未选中 = 透明底、默认文字色。
 * [indent] 控制树状缩进（子分组按路径深度递增）。
 */
@Composable
private fun DestinationRow(
    selected: Boolean,
    text: String,
    indent: Dp,
    onClick: () -> Unit,
    leadingIcon: ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .padding(start = indent, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                leadingIcon,
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else Color.Unspecified,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
