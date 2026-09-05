package com.github.jing332.tts_server_android.compose.systts.speechrule

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Checkbox
import com.github.jing332.compose.widgets.AppDropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.github.jing332.compose.rememberLazyListReorderCache
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.LazyListIndexStateSaver
import com.github.jing332.compose.widgets.ShadowedDraggableItem
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.script.JsMetadataSyncer
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.LocalNavController
import com.github.jing332.tts_server_android.compose.SharedViewModel
import com.github.jing332.tts_server_android.compose.systts.ConfigDeleteDialog
import com.github.jing332.tts_server_android.utils.MyTools
import com.drake.net.utils.withIO
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeechRuleManagerScreen(sharedVM: SharedViewModel, finish: () -> Unit) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 多选删除
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showMultiDeleteDialog by remember { mutableStateOf(false) }

    if (showMultiDeleteDialog) {
        AppDialog(
            onDismissRequest = { showMultiDeleteDialog = false },
            title = { Text(stringResource(id = R.string.delete)) },
            content = {
                Text(context.getString(R.string.selected_count, selectedIds.size))
            },
            buttons = {
                androidx.compose.material3.TextButton(onClick = { showMultiDeleteDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
                androidx.compose.material3.TextButton(onClick = {
                    val toDelete = selectedIds
                    scope.launch {
                        withIO {
                            dbm.speechRuleDao.all.forEach {
                                if (it.id in toDelete) dbm.speechRuleDao.delete(it)
                            }
                        }
                    }
                    selectedIds = emptySet()
                    selectionMode = false
                    showMultiDeleteDialog = false
                }) { Text(stringResource(id = R.string.confirm)) }
            }
        )
    }

    var showImportSheet by remember { mutableStateOf(false) }
    if (showImportSheet)
        SpeechRuleImportBottomSheet(onDismissRequest = { showImportSheet = false })

    var showExportSheet by remember { mutableStateOf<List<SpeechRule>?>(null) }
    if (showExportSheet != null)
        SpeechRuleExportBottomSheet(
            onDismissRequest = { showExportSheet = null },
            list = showExportSheet!!,
        )

    var showDeleteDialog by remember { mutableStateOf<SpeechRule?>(null) }
    if (showDeleteDialog != null)
        ConfigDeleteDialog(
            onDismissRequest = { showDeleteDialog = null },
            content = showDeleteDialog!!.name
        ) {
            dbm.speechRuleDao.delete(showDeleteDialog!!)
            showDeleteDialog = null
        }

    // 编辑元数据弹窗：name/ruleId/author/version + 同步JS
    var showEditMetadataDialog by remember { mutableStateOf<SpeechRule?>(null) }
    if (showEditMetadataDialog != null) {
        val cur = showEditMetadataDialog!!
        var editName by remember(cur.id) { mutableStateOf(cur.name) }
        var editRuleId by remember(cur.id) { mutableStateOf(cur.ruleId) }
        var editAuthor by remember(cur.id) { mutableStateOf(cur.author) }
        var editVersion by remember(cur.id) { mutableStateOf(cur.version.toString()) }
        AppDialog(
            onDismissRequest = { showEditMetadataDialog = null },
            title = { Text("编辑元数据") },
            content = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        label = { Text("name") },
                        value = editName,
                        onValueChange = { editName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        label = { Text("ruleId (JS: id)") },
                        value = editRuleId,
                        onValueChange = { editRuleId = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        label = { Text("author") },
                        value = editAuthor,
                        onValueChange = { editAuthor = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        label = { Text("version") },
                        value = editVersion,
                        onValueChange = { editVersion = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        singleLine = true
                    )
                }
            },
            buttons = {
                TextButton(onClick = { showEditMetadataDialog = null }) {
                    Text(stringResource(id = R.string.cancel))
                }
                TextButton(onClick = {
                    val newVersion = editVersion.toIntOrNull() ?: cur.version
                    // 同步更新 JS 代码里的元数据字面量，保证下次 eval 一致
                    var newCode = cur.code
                    newCode = JsMetadataSyncer.updateStringField(newCode, "name", editName)
                    newCode = JsMetadataSyncer.updateStringField(newCode, "id", editRuleId)
                    newCode = JsMetadataSyncer.updateStringField(newCode, "author", editAuthor)
                    newCode = JsMetadataSyncer.updateIntField(newCode, "version", newVersion)
                    dbm.speechRuleDao.update(
                        cur.copy(
                            name = editName,
                            ruleId = editRuleId,
                            author = editAuthor,
                            version = newVersion,
                            code = newCode
                        )
                    )
                    showEditMetadataDialog = null
                }) { Text(stringResource(id = R.string.save)) }
            }
        )
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    // 第11项修复: list 原本声明在 content lambda 内，但 actions 也引用它，
    // 作用域不通会编译失败。提到 Scaffold 外层，actions 与 content 均可访问。
    val flowAll = remember { dbm.speechRuleDao.flowAll().conflate() }
    val list by flowAll.collectAsState(initial = emptyList())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    if (selectionMode) {
                        Text(context.getString(R.string.selected_count, selectedIds.size))
                    } else {
                        Text(stringResource(id = R.string.speech_rule_manager))
                    }
                },
                navigationIcon = {
                    if (selectionMode) {
                        IconButton(onClick = {
                            selectionMode = false
                            selectedIds = emptySet()
                        }) {
                            Icon(Icons.Default.Close, stringResource(id = R.string.cancel))
                        }
                    } else {
                        IconButton(onClick = finish) {
                            Icon(
                                Icons.AutoMirrored.Default.ArrowBack,
                                stringResource(id = R.string.nav_back)
                            )
                        }
                    }
                },

                actions = {
                    if (selectionMode) {
                        // 全选
                        val allSelected by remember(list) {
                            derivedStateOf { list.isNotEmpty() && selectedIds.size == list.size }
                        }
                        IconButton(onClick = {
                            selectedIds = if (allSelected) emptySet()
                            else list.map { it.id }.toSet()
                        }) {
                            Icon(Icons.Default.SelectAll, stringResource(id = R.string.select_all))
                        }
                        // 删除选中
                        IconButton(
                            enabled = selectedIds.isNotEmpty(),
                            onClick = { showMultiDeleteDialog = true }
                        ) {
                            Icon(
                                Icons.Default.DeleteForever,
                                stringResource(id = R.string.delete),
                                tint = if (selectedIds.isNotEmpty())
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                    IconButton(onClick = {
                        navController.navigate(NavRoutes.SpeechRuleEdit.id)
                    }) {
                        Icon(Icons.Default.Add, stringResource(id = R.string.add_config))
                    }

                    // 第10项: 顶部整理图标(批量多选删除),不再藏在更多菜单里
                    IconButton(onClick = { selectionMode = true }) {
                        Icon(
                            Icons.Default.Checklist,
                            stringResource(id = R.string.multi_select)
                        )
                    }

                    var showOptions by remember { mutableStateOf(false) }
                    IconButton(onClick = { showOptions = true }) {
                        Icon(Icons.Default.MoreVert, stringResource(id = R.string.more_options))

                        AppDropdownMenu(
                            expanded = showOptions,
                            onDismissRequest = { showOptions = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.import_config)) },
                                onClick = {
                                    showOptions = false
                                    showImportSheet = true
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Default.Input, null) }
                            )

                            DropdownMenuItem(
                                text = { Text(text = stringResource(id = R.string.export_config)) },
                                onClick = {
                                    showOptions = false
                                    scope.launch {
                                        showExportSheet = withIO {
                                            dbm.speechRuleDao.allEnabled
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Output, null)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.desktop_shortcut)) },
                                onClick = {
                                    showOptions = false
                                    MyTools.addShortcut(
                                        context,
                                        context.getString(R.string.speech_rule_manager),
                                        "speech_rule",
                                        R.drawable.ic_shortcut_speech_rule,
                                        Intent(context, SpeechRuleManagerActivity::class.java)
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.AppShortcut, null) }
                            )
                        }
                    }
                    } // end else (non-selection mode)
                }

            )
        }
    ) { paddingValues ->
//        LaunchedEffect(Unit) {
//            dbm.speechRuleDao.all.forEachIndexed { index, speechRule ->
//                dbm.speechRuleDao.update(speechRule.copy(order = index))
//            }
//        }

        val listState = remember { LazyListState() }
        LazyListIndexStateSaver(
            models = list,
            listState = listState,
        )

        val cache = rememberLazyListReorderCache(list)
        val reorderState = rememberReorderableLazyListState(
            listState = listState,
            onMove = { from, to ->
                cache.move(from.index, to.index)
            }, onDragEnd = { from, to ->
                cache.list.forEachIndexed { index, value ->
                    if (index != value.order)
                        dbm.speechRuleDao.update(value.copy(order = index))
                }
                cache.ended()
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .reorderable(reorderState),
            state = reorderState.listState,
        ) {
            itemsIndexed(cache.list, key = { _, v -> v.id }) { index, item ->
                ShadowedDraggableItem(reorderableState = reorderState, key = item.id) {
                    val isSelected = remember(item.id) {
                        derivedStateOf { item.id in selectedIds }
                    }.value
                    Item(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .then(if (!selectionMode) { Modifier.detectReorderAfterLongPress(reorderState) } else Modifier),
                        name = item.name,
                        desc = "${item.author} - v${item.version}",
                        isEnabled = item.isEnabled,
                        onEnabledChange = { dbm.speechRuleDao.update(item.copy(isEnabled = it)) },
                        isSelectionMode = selectionMode,
                        isSelected = isSelected,
                        onToggleSelection = {
                            selectedIds = if (item.id in selectedIds)
                                selectedIds - item.id
                            else selectedIds + item.id
                        },
                        onClick = {
                            if (selectionMode) {
                                selectedIds = if (item.id in selectedIds)
                                    selectedIds - item.id
                                else selectedIds + item.id
                            }
                        },
                        onEdit = {
                            sharedVM.put(NavRoutes.SpeechRuleEdit.KEY_DATA, item)
                            navController.navigate(NavRoutes.SpeechRuleEdit.id)
                        },
                        onExport = { showExportSheet = listOf(item) },
                        onDelete = { showDeleteDialog = item },
                        // 第11项: 内联展开编辑 + 运行键（跳编辑器并自动调试）
                        rule = item,
                        onUpdateRule = { dbm.speechRuleDao.update(it) },
                        onEditMetadata = { showEditMetadataDialog = item }
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun Item(
    modifier: Modifier,
    name: String,
    desc: String,
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelection: () -> Unit = {},
    // 第11项: 列表项内联展开编辑元数据
    rule: SpeechRule? = null,
    onUpdateRule: ((SpeechRule) -> Unit)? = null,
    // 编辑元数据（弹窗）：name/ruleId/author/version + 同步JS
    onEditMetadata: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    ElevatedCard(
        modifier = modifier.combinedClickable(
            onClick = { if (isSelectionMode) onToggleSelection() else onClick() },
            onLongClick = { if (!isSelectionMode) onToggleSelection() }
        )
    ) {
        // 第11项修复: Box会堆叠子项导致展开面板与Row重叠,改用Column使展开面板下移
        Column(modifier = Modifier.padding(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() },
                    )
                } else {
                Checkbox(
                    checked = isEnabled,
                    onCheckedChange = onEnabledChange,
                    modifier = Modifier.semantics {
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
                    }
                )
                }
                Column(Modifier.weight(1f)) {
                    // 名称14sp、author等次要信息12sp,与插件管理列表一致
                    Text(text = name, style = MaterialTheme.typography.bodyMedium)
                    Text(text = desc, style = MaterialTheme.typography.bodySmall)
                }
                if (!isSelectionMode) {
                Row {
                    var showOptions by remember { mutableStateOf(false) }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Code, stringResource(id = R.string.edit_code_desc, name))
                    }
                    IconButton(onClick = { showOptions = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            stringResource(id = R.string.more_options_desc, name)
                        )
                        AppDropdownMenu(
                            expanded = showOptions,
                            onDismissRequest = { showOptions = false }) {

                            // 编辑元数据（弹窗）：name/ruleId/author/version + 同步JS
                            if (onEditMetadata != null) {
                                DropdownMenuItem(
                                    text = { Text("编辑元数据") },
                                    onClick = {
                                        showOptions = false
                                        onEditMetadata()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.EditNote, "编辑元数据")
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.export_config)) },
                                onClick = {
                                    showOptions = false
                                    onExport()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Output, stringResource(R.string.export_config))
                                }
                            )

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(id = R.string.delete),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showOptions = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.DeleteForever,
                                        stringResource(R.string.delete),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }

                }
                }
            }
        }
    }
}