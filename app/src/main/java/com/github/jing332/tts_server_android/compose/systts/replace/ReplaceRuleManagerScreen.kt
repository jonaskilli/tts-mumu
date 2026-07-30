package com.github.jing332.tts_server_android.compose.systts.replace

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.LazyListIndexStateSaver
import com.github.jing332.compose.widgets.ShadowedDraggableItem
import com.github.jing332.compose.widgets.TextFieldDialog
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.replace.GroupWithReplaceRule
import com.github.jing332.database.entities.replace.ReplaceRule
import com.github.jing332.database.entities.replace.ReplaceRuleGroup
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.AppDefaultProperties
import com.github.jing332.tts_server_android.compose.LocalNavController
import com.github.jing332.tts_server_android.compose.SharedViewModel
import com.github.jing332.tts_server_android.compose.systts.sizeToToggleableState
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import com.github.jing332.tts_server_android.utils.MyTools
import com.drake.net.utils.withIO
import kotlinx.coroutines.launch
import okhttp3.internal.toLongOrDefault
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun ReplaceRuleManagerScreen(
    sharedVM: SharedViewModel,
    vm: ReplaceRuleManagerViewModel = viewModel(),
    finish: () -> Unit,
) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    val models by vm.list.collectAsStateWithLifecycle()

    fun navigateToEdit(rule: ReplaceRule = ReplaceRule()) {
        sharedVM.put(NavRoutes.Edit.KEY_DATA, rule)
        navController.navigate(NavRoutes.Edit.id)
    }

    var showImportSheet by remember { mutableStateOf(false) }
    if (showImportSheet)
        ReplaceRuleImportBottomSheet(onDismissRequest = { showImportSheet = false })

    var showExportSheet by remember { mutableStateOf<List<GroupWithReplaceRule>?>(null) }
    if (showExportSheet != null)
        ReplaceRuleExportBottomSheet(
            onDismissRequest = { showExportSheet = null },
            list = showExportSheet!!,
        )

    var showAddGroupDialog by remember { mutableStateOf(false) }
    if (showAddGroupDialog) {
        var text by remember { mutableStateOf("") }
        TextFieldDialog(
            title = stringResource(id = R.string.add_group),
            text = text,
            onTextChange = { text = it },
            onDismissRequest = { showAddGroupDialog = false },
            onConfirm = {
                scope.launch { withIO { dbm.replaceRuleDao.insertGroup(ReplaceRuleGroup(name = text)) } }
            }
        )
    }

    var showGroupEditDialog by remember { mutableStateOf<ReplaceRuleGroup?>(null) }
    if (showGroupEditDialog != null) {
        var group by remember { mutableStateOf(showGroupEditDialog!!) }
        GroupEditDialog(
            onDismissRequest = {
                showGroupEditDialog = null
            },
            group = group,
            onGroupChange = { group = it },
            onConfirm = { scope.launch { withIO { dbm.replaceRuleDao.updateGroup(group) } } }
        )
    }

    var showSortDialog by remember { mutableStateOf<List<ReplaceRule>?>(null) }
    if (showSortDialog != null) {
        SortDialog(
            onDismissRequest = { showSortDialog = null },
            list = showSortDialog!!
        )
    }

    // 统一多选删除: 同时支持分组和规则
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) } // 规则ID
    var selectedGroupIds by remember { mutableStateOf<Set<Long>>(emptySet()) } // 分组ID

    var showMultiDeleteDialog by remember { mutableStateOf(false) }
    if (showMultiDeleteDialog) {
        val groupCount = selectedGroupIds.size
        val ruleCount = selectedIds.size
        AppDialog(
            onDismissRequest = { showMultiDeleteDialog = false },
            title = { Text(stringResource(id = R.string.delete)) },
            content = {
                Text(if (groupCount > 0 && ruleCount > 0)
                    "将删除 $groupCount 个分组(含其下规则)和 $ruleCount 个规则"
                else if (groupCount > 0)
                    context.getString(R.string.delete_selected_groups_confirm, groupCount)
                else
                    context.getString(R.string.selected_count, ruleCount)
                )
            },
            buttons = {
                androidx.compose.material3.TextButton(onClick = { showMultiDeleteDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
                androidx.compose.material3.TextButton(onClick = {
                    val toDeleteGroups = selectedGroupIds
                    val toDeleteRules = selectedIds
                    scope.launch {
                        withIO {
                            // 先删选中的分组(含其下规则)
                            val enabledGroupDeleted = models.any { gwt ->
                                gwt.group.id in toDeleteGroups && gwt.list.any { it.isEnabled }
                            }
                            models.filter { it.group.id in toDeleteGroups }.forEach { gwt ->
                                vm.deleteGroup(gwt)
                            }
                            // 再删剩余的选中规则(排除已随分组删除的)
                            val rulesInDeletedGroups = models
                                .filter { it.group.id in toDeleteGroups }
                                .flatMap { it.list }
                                .map { it.id }
                                .toSet()
                            val remainingRules = toDeleteRules - rulesInDeletedGroups
                            val enabledRuleDeleted = dbm.replaceRuleDao.all.any {
                                it.id in remainingRules && it.isEnabled
                            }
                            dbm.replaceRuleDao.all.forEach {
                                if (it.id in remainingRules) dbm.replaceRuleDao.delete(it)
                            }
                            if (enabledGroupDeleted || enabledRuleDeleted)
                                SystemTtsService.notifyUpdateConfig(isOnlyReplacer = true)
                        }
                    }
                    selectedIds = emptySet()
                    selectedGroupIds = emptySet()
                    selectionMode = false
                    showMultiDeleteDialog = false
                }) { Text(stringResource(id = R.string.confirm)) }
            }
        )
    }

    // 多选模式按返回键退出选择而非退出页面
    androidx.activity.compose.BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedIds = emptySet()
        selectedGroupIds = emptySet()
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    if (selectionMode) {
                        val total = selectedIds.size + selectedGroupIds.size
                        Text(context.getString(R.string.selected_count, total))
                    } else {
                        LaunchedEffect(vm.searchText, vm.searchType) {
                            vm.updateSearchResult()
                        }
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerLow),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SearchTextField(
                                modifier = Modifier.weight(1f),
                                value = vm.searchText,
                                onValueChange = { vm.searchText = it },
                                searchType = vm.searchType,
                                onSearchTypeChange = { vm.searchType = it }
                            )
                            var showAddOptions by remember { mutableStateOf(false) }
                            IconButton(onClick = { showAddOptions = true }) {
                                Icon(Icons.Default.Add, stringResource(id = R.string.add_config))
                                DropdownMenu(
                                    expanded = showAddOptions,
                                    onDismissRequest = { showAddOptions = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(id = R.string.add_config)) },
                                        onClick = {
                                            showAddOptions = false
                                            navigateToEdit()
                                        },
                                        leadingIcon = {
                                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, stringResource(R.string.add_config))
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(id = R.string.add_group)) },
                                        onClick = {
                                            showAddOptions = false
                                            showAddGroupDialog = true
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.AddCard, stringResource(R.string.add_group))
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (selectionMode) {
                        IconButton(onClick = {
                            selectionMode = false
                            selectedIds = emptySet()
                            selectedGroupIds = emptySet()
                        }) {
                            Icon(Icons.Default.Close, stringResource(id = R.string.cancel))
                        }
                    } else {
                        IconButton(onClick = finish) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                stringResource(id = R.string.nav_back)
                            )
                        }
                    }
                },
                actions = {
                    if (selectionMode) {
                        // 全选: 所有分组 + 所有规则
                        val allGroupIds = remember(models) { models.map { it.group.id }.toSet() }
                        val allRuleIds = remember(models) { models.flatMap { it.list }.map { it.id }.toSet() }
                        val allSelected by remember(selectedIds, selectedGroupIds, allGroupIds, allRuleIds) {
                            derivedStateOf {
                                allGroupIds.isNotEmpty() &&
                                allGroupIds.all { it in selectedGroupIds } &&
                                allRuleIds.all { it in selectedIds }
                            }
                        }
                        IconButton(onClick = {
                            if (allSelected) {
                                selectedIds = emptySet()
                                selectedGroupIds = emptySet()
                            } else {
                                selectedIds = allRuleIds
                                selectedGroupIds = allGroupIds
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, stringResource(id = R.string.select_all))
                        }
                        // 删除选中(分组+规则)
                        IconButton(
                            enabled = selectedIds.isNotEmpty() || selectedGroupIds.isNotEmpty(),
                            onClick = { showMultiDeleteDialog = true }
                        ) {
                            Icon(
                                Icons.Default.DeleteForever,
                                stringResource(id = R.string.delete),
                                tint = if (selectedIds.isNotEmpty() || selectedGroupIds.isNotEmpty())
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        // 多选删除入口
                        IconButton(onClick = { selectionMode = true }) {
                            Icon(Icons.Default.DeleteSweep, stringResource(id = R.string.select_delete))
                        }
                        var showOptions by remember { mutableStateOf(false) }
                        IconButton(onClick = { showOptions = true }) {
                            Icon(Icons.Default.MoreVert, stringResource(id = R.string.more_options))

                            DropdownMenu(
                                expanded = showOptions,
                                onDismissRequest = { showOptions = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.import_config)) },
                                    onClick = {
                                        showOptions = false
                                        showImportSheet = true
                                    },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Input, null) }
                                )

                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.export_config)) },
                                    onClick = {
                                        showOptions = false
                                        showExportSheet = models
                                    },
                                    leadingIcon = { Icon(Icons.Default.Output, null) }
                                )

                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.desktop_shortcut)) },
                                    onClick = {
                                        showOptions = false
                                        MyTools.addShortcut(
                                            context,
                                            context.getString(R.string.replace_rule_manager),
                                            "replace",
                                            R.drawable.ic_shortcut_replace,
                                            Intent(context, ReplaceManagerActivity::class.java)
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.AppShortcut, null) }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        val listState = rememberLazyListState()
        LazyListIndexStateSaver(models = models, listState = listState)

        val reorderState =
            rememberReorderableLazyListState(listState = listState, onMove = { from, to ->
                val fromKey = from.key.toString()
                val toKey = to.key.toString()
                scope.launch {
                    withIO {
                        if (fromKey.startsWith("g_") && toKey.startsWith("g_")) {
                            val src = dbm.replaceRuleDao.getGroup(fromKey.substring(2).toLong())
                                ?: return@withIO
                            val target = dbm.replaceRuleDao.getGroup(toKey.substring(2).toLong())
                                ?: return@withIO

                            dbm.replaceRuleDao.updateGroup(
                                src.copy(order = target.order),
                                target.copy(order = src.order)
                            )
                        } else {
                            val src = dbm.replaceRuleDao.get(fromKey.toLongOrDefault(Long.MIN_VALUE))
                                ?: return@withIO
                            val target = dbm.replaceRuleDao.get(toKey.toLongOrDefault(Long.MIN_VALUE))
                                ?: return@withIO

                            dbm.replaceRuleDao.update(
                                src.copy(order = target.order),
                                target.copy(order = src.order)
                            )
                        }
                    }
                }
            })

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .reorderable(reorderState),
            state = listState,
        ) {
            models.forEachIndexed { _, groupWithRules ->
                val g = groupWithRules.group
                val toggleableState =
                    groupWithRules.list.filter { it.isEnabled }.size.sizeToToggleableState(
                        groupWithRules.list.size
                    )
                val key = "g_${g.id}"
                val isGroupSelected = g.id in selectedGroupIds
                stickyHeader(key = key) {
                    ShadowedDraggableItem(reorderableState = reorderState, key = key) {
                        Group(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (!selectionMode) Modifier.detectReorderAfterLongPress(reorderState) else Modifier),
                            name = g.name,
                            isExpanded = g.isExpanded,
                            toggleableState = toggleableState,
                            onToggleableStateChange = { enabled ->
                                scope.launch {
                                    withIO {
                                        groupWithRules.list.filter { it.isEnabled != enabled }
                                            .forEach { dbm.replaceRuleDao.update(it.copy(isEnabled = enabled)) }
                                    }
                                }
                            },
                            onClick = {
                                scope.launch { withIO { dbm.replaceRuleDao.updateGroup(g.copy(isExpanded = !g.isExpanded)) } }
                            },
                            onEdit = { showGroupEditDialog = g },
                            onDelete = {
                                vm.deleteGroup(groupWithRules)
                                if (groupWithRules.list.find { it.isEnabled } != null)
                                    SystemTtsService.notifyUpdateConfig(isOnlyReplacer = true)
                            },
                            onExport = { showExportSheet = listOf(groupWithRules) },
                            onSort = { showSortDialog = groupWithRules.list },
                            inSelectionMode = selectionMode,
                            isSelected = isGroupSelected,
                            onToggleSelect = {
                                selectedGroupIds = if (g.id in selectedGroupIds) selectedGroupIds - g.id
                                else selectedGroupIds + g.id
                            }
                        )
                    }
                }

                if (g.isExpanded) {
                    items(groupWithRules.list, key = { it.id }) { rule ->
                        val isSelected = rule.id in selectedIds
                        ShadowedDraggableItem(
                            reorderableState = reorderState,
                            key = rule.id
                        ) { _ ->
                            Item(
                                name = rule.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .then(if (!selectionMode) Modifier.detectReorderAfterLongPress(reorderState) else Modifier),
                                isEnabled = rule.isEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        withIO { dbm.replaceRuleDao.update(rule.copy(isEnabled = enabled)) }
                                        if (enabled) SystemTtsService.notifyUpdateConfig(
                                            isOnlyReplacer = true
                                        )
                                    }
                                },
                                onClick = { },
                                onEdit = { navigateToEdit(rule) },
                                onDelete = {
                                    vm.deleteRule(rule)
                                    if (rule.isEnabled)
                                        SystemTtsService.notifyUpdateConfig(isOnlyReplacer = true)
                                },
                                onMoveTop = { vm.moveTop(rule) },
                                onMoveBottom = { vm.moveBottom(rule) },
                                // 第3项: 多选支持
                                isSelectionMode = selectionMode,
                                isSelected = isSelected,
                                onToggleSelection = {
                                    selectedIds = if (rule.id in selectedIds) selectedIds - rule.id
                                    else selectedIds + rule.id
                                }
                            )
                        }
                    }
                }

            }

            item {
                Spacer(Modifier.padding(bottom = AppDefaultProperties.LIST_END_PADDING))
            }
        }
    }
}
