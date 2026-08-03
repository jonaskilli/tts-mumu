package com.github.jing332.tts_server_android.compose.systts.plugin

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandCircleDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.jing332.common.utils.longToast
import com.github.jing332.compose.rememberLazyListReorderCache
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.ShadowedDraggableItem
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.script.JsMetadataSyncer
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.AppDefaultProperties
import com.github.jing332.tts_server_android.compose.LocalNavController
import com.github.jing332.tts_server_android.compose.SharedViewModel
import com.github.jing332.tts_server_android.compose.systts.ConfigDeleteDialog
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import com.github.jing332.tts_server_android.utils.MyTools
import com.drake.net.utils.withIO
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PluginManagerScreen(sharedVM: SharedViewModel, onFinishActivity: () -> Unit) {
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
            content = { Text(context.getString(R.string.selected_count, selectedIds.size)) },
            buttons = {
                androidx.compose.material3.TextButton(onClick = { showMultiDeleteDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
                androidx.compose.material3.TextButton(onClick = {
                    val toDelete = selectedIds
                    scope.launch {
                        withIO {
                            dbm.pluginDao.all.forEach {
                                if (it.id in toDelete) dbm.pluginDao.delete(it)
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

    var showImportConfig by remember { mutableStateOf(false) }
    if (showImportConfig) {
        PluginImportBottomSheet(onDismissRequest = { showImportConfig = false })
    }

    var showExportConfig by remember { mutableStateOf<List<Plugin>?>(null) }
    if (showExportConfig != null) {
        val pluginList = showExportConfig!!
        PluginExportBottomSheet(
            fileName = if (pluginList.size == 1) "ttsrv-plugin-${pluginList[0].name}.json" else "ttsrv-plugins.json",
            onDismissRequest = { showExportConfig = null }) { isExportVars ->
            // 修复: 导出用 prettyPrint, 每个配置项独立一行可读
            if (isExportVars) {
                AppConst.jsonBuilder.encodeToString(pluginList)
            } else {
                AppConst.jsonBuilder.encodeToString(pluginList.map { it.copy(userVars = mutableMapOf()) })
            }
        }
    }

    var showDeleteDialog by remember { mutableStateOf<Plugin?>(null) }
    if (showDeleteDialog != null) {
        val plugin = showDeleteDialog!!
        ConfigDeleteDialog(onDismissRequest = { showDeleteDialog = null }, content = plugin.name) {
            dbm.pluginDao.delete(plugin)
            showDeleteDialog = null
        }
    }


    var showVarsSettings by remember { mutableStateOf<Plugin?>(null) }
    if (showVarsSettings != null) {
        var plugin by remember { mutableStateOf(showVarsSettings!!) }
        if (plugin.defVars.isEmpty()) {
            showVarsSettings = null
        }
        PluginVarsBottomSheet(onDismissRequest = {
            dbm.pluginDao.update(plugin)
            showVarsSettings = null
        }, plugin = plugin) {
            plugin = it
        }
    }

    val navController = LocalNavController.current

    // 插件音频参数对话框
    var showAudioParamsDialog by remember { mutableStateOf<Plugin?>(null) }
    if (showAudioParamsDialog != null) {
        val plugin = showAudioParamsDialog!!
        PluginAudioParamsDialog(
            initialParams = plugin.audioParams,
            onDismissRequest = { showAudioParamsDialog = null },
            onConfirm = { newParams ->
                dbm.pluginDao.update(plugin.copy(audioParams = newParams))
                // 通知服务更新配置，使插件音频参数立即生效
                SystemTtsService.notifyUpdateConfig()
                showAudioParamsDialog = null
                context.longToast(R.string.plugin_audio_params_saved)
            }
        )
    }

    fun onEdit(plugin: Plugin = Plugin()) {
        sharedVM.put(NavRoutes.PluginEdit.KEY_DATA, plugin)
        navController.navigate(NavRoutes.PluginEdit.id)
    }

    // 切换引用配置：选择目标插件，把所有引用源插件id的配置项批量改为目标插件id
    var showSwitchPluginRefsDialog by remember { mutableStateOf<Plugin?>(null) }
    // 二次确认：选好目标插件后，在此确认是否执行批量切换
    var pendingSwitch by remember { mutableStateOf<Pair<Plugin, Plugin>?>(null) }
    if (showSwitchPluginRefsDialog != null) {
        val sourcePlugin = showSwitchPluginRefsDialog!!
        // 用全部插件(不止已启用)，排除源插件本身
        val candidates = remember(sourcePlugin.id) {
            dbm.pluginDao.all.filter { it.pluginId != sourcePlugin.pluginId }
        }
        AppDialog(
            onDismissRequest = { showSwitchPluginRefsDialog = null },
            title = { Text("切换引用配置到其他插件") },
            content = {
                Column {
                    Text(
                        "将所有引用「${sourcePlugin.name}（${sourcePlugin.pluginId}」的配置项改为使用下方所选插件：",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    if (candidates.isEmpty()) {
                        Text(
                            "没有其他可切换的插件",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(candidates, { it.id }) { target ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable {
                                            pendingSwitch = sourcePlugin to target
                                            showSwitchPluginRefsDialog = null
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    PluginImage(model = target.iconUrl, name = target.name)
                                    Column(Modifier.padding(horizontal = 8.dp)) {
                                        Text(
                                            target.name,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            target.pluginId,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            buttons = {
                TextButton(onClick = { showSwitchPluginRefsDialog = null }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    // 二次确认 + 执行批量切换
    if (pendingSwitch != null) {
        val (sourcePlugin, target) = pendingSwitch!!
        AppDialog(
            onDismissRequest = { pendingSwitch = null },
            title = { Text("确认切换") },
            content = {
                Text(
                    "将把所有引用插件「${sourcePlugin.name}」的配置项改为使用「${target.name}」。\n" +
                        "源插件本身不会被修改或删除。"
                )
            },
            buttons = {
                TextButton(onClick = { pendingSwitch = null }) {
                    Text(stringResource(id = R.string.cancel))
                }
                TextButton(onClick = {
                    val srcId = sourcePlugin.pluginId
                    val newId = target.pluginId
                    pendingSwitch = null
                    if (srcId.isEmpty()) return@TextButton
                    scope.launch {
                        var count = 0
                        withIO {
                            dbm.systemTtsV2.getAllGroupWithTts()
                                .flatMap { it.list }
                                .forEach { tts ->
                                    val config = tts.config
                                    if (config is TtsConfigurationDTO) {
                                        val src = config.source
                                        if (src is PluginTtsSource && src.pluginId == srcId) {
                                            dbm.systemTtsV2.update(
                                                tts.copy(
                                                    config = config.copy(
                                                        source = src.copy(pluginId = newId)
                                                    )
                                                )
                                            )
                                            count++
                                        }
                                    }
                                }
                        }
                        SystemTtsService.notifyUpdateConfig()
                        context.longToast("已切换 $count 项配置到「${target.name}」")
                    }
                }) { Text(stringResource(id = R.string.confirm)) }
            }
        )
    }

    // 编辑元数据弹窗：name/pluginId/author/version + 同步JS + pluginId变更检测
    var showEditMetadataDialog by remember { mutableStateOf<Plugin?>(null) }
    // pluginId 变更后，提示一键更新引用旧 id 的配置项
    var pendingPluginIdUpdate by remember { mutableStateOf<Triple<String, String, Int>?>(null) }
    if (showEditMetadataDialog != null) {
        val cur = showEditMetadataDialog!!
        var editName by remember(cur.id) { mutableStateOf(cur.name) }
        var editPluginId by remember(cur.id) { mutableStateOf(cur.pluginId) }
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
                        label = { Text("pluginId (JS: id)") },
                        value = editPluginId,
                        onValueChange = { editPluginId = it },
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
                    newCode = JsMetadataSyncer.updateStringField(newCode, "id", editPluginId)
                    newCode = JsMetadataSyncer.updateStringField(newCode, "author", editAuthor)
                    newCode = JsMetadataSyncer.updateIntField(newCode, "version", newVersion)
                    dbm.pluginDao.update(
                        cur.copy(
                            name = editName,
                            pluginId = editPluginId,
                            author = editAuthor,
                            version = newVersion,
                            code = newCode
                        )
                    )
                    showEditMetadataDialog = null

                    // pluginId 变更后, 检测引用旧 id 的配置项并提示一键更新
                    if (editPluginId != cur.pluginId) {
                        val oldId = cur.pluginId
                        val newId = editPluginId
                        scope.launch {
                            val count = withIO {
                                dbm.systemTtsV2.getAllGroupWithTts()
                                    .flatMap { it.list }
                                    .count { tts ->
                                        val config = tts.config
                                        config is TtsConfigurationDTO &&
                                            (config.source as? PluginTtsSource)?.pluginId == oldId
                                    }
                            }
                            if (count > 0) {
                                pendingPluginIdUpdate = Triple(oldId, newId, count)
                            }
                        }
                    }
                }) { Text(stringResource(id = R.string.save)) }
            }
        )
    }

    // 一键更新引用旧 pluginId 的配置项到新 id
    if (pendingPluginIdUpdate != null) {
        val (oldId, newId, count) = pendingPluginIdUpdate!!
        AppDialog(
            onDismissRequest = { pendingPluginIdUpdate = null },
            title = { Text("插件 id 已变更") },
            content = {
                Text(
                    "插件 pluginId 已由「$oldId」改为「$newId」。\n" +
                        "检测到 $count 个配置项仍引用旧 id，是否一键更新为新 id？"
                )
            },
            buttons = {
                TextButton(onClick = { pendingPluginIdUpdate = null }) {
                    Text("暂不")
                }
                TextButton(onClick = {
                    val pending = pendingPluginIdUpdate!!
                    pendingPluginIdUpdate = null
                    scope.launch {
                        withIO {
                            dbm.systemTtsV2.getAllGroupWithTts()
                                .flatMap { it.list }
                                .forEach { tts ->
                                    val config = tts.config
                                    if (config is TtsConfigurationDTO) {
                                        val src = config.source
                                        if (src is PluginTtsSource && src.pluginId == pending.first) {
                                            dbm.systemTtsV2.update(
                                                tts.copy(
                                                    config = config.copy(source = src.copy(pluginId = pending.second))
                                                )
                                            )
                                        }
                                    }
                                }
                        }
                        SystemTtsService.notifyUpdateConfig()
                    }
                }) {
                    Text("一键更新")
                }
            }
        )
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    // 第11项修复: list 原本声明在 content lambda 内，但 actions 也引用它，
    // 作用域不通会编译失败。提到 Scaffold 外层，actions 与 content 均可访问。
    val flowAll = remember { dbm.pluginDao.flowAll().conflate() }
    val list by flowAll.collectAsStateWithLifecycle(emptyList())

    Scaffold(contentWindowInsets = WindowInsets(0),
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    if (selectionMode) {
                        Text(context.getString(R.string.selected_count, selectedIds.size))
                    } else {
                        Text(stringResource(id = R.string.plugin_manager))
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
                        IconButton(onClick = onFinishActivity) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                stringResource(id = R.string.nav_back)
                            )
                        }
                    }
                },
                actions = {
                    if (selectionMode) {
                        IconButton(onClick = {
                            selectedIds = if (selectedIds.size == list.size) emptySet()
                            else list.map { it.id }.toSet()
                        }) {
                            Icon(Icons.Default.SelectAll, stringResource(id = R.string.select_all))
                        }
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
                        onEdit()
                    }) {
                        Icon(Icons.Default.Add, stringResource(id = R.string.add_config))
                    }

                    // 第10项: 顶部整理图标(批量多选删除),不再藏在更多菜单里
                    IconButton(onClick = { selectionMode = true }) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            stringResource(id = R.string.select_delete)
                        )
                    }

                    var showOptions by remember { mutableStateOf(false) }
                    IconButton(onClick = {
                        showOptions = true
                    }) {
                        Icon(Icons.Default.MoreVert, stringResource(id = R.string.more_options))

                        DropdownMenu(
                            expanded = showOptions,
                            onDismissRequest = { showOptions = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.import_config)) },
                                onClick = {
                                    showOptions = false
                                    showImportConfig = true
                                },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.Input, stringResource(R.string.import_config))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.export_config)) },
                                onClick = {
                                    showOptions = false
                                    scope.launch {
                                        showExportConfig = withIO {
                                            dbm.pluginDao.allEnabled
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Output, stringResource(R.string.export_config))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.desktop_shortcut)) },
                                onClick = {
                                    showOptions = false
                                    MyTools.addShortcut(
                                        context,
                                        context.getString(R.string.plugin_manager),
                                        "plugin",
                                        R.drawable.ic_shortcut_plugin,
                                        Intent(context, PluginManagerActivity::class.java)
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.AppShortcut, null) }
                            )
                        }
                    }
                    } // end else
                }
            )
        }) { paddingValues ->
        val cache = rememberLazyListReorderCache(list)

        val reorderState = rememberReorderableLazyListState(onMove = { from, to ->
            cache.move(from.index, to.index)
        }, onDragEnd = { from, to ->
            cache.list.forEachIndexed { index, plugin ->
                if (index != plugin.order)
                    dbm.pluginDao.update(plugin.copy(order = index))
            }
        })


        LazyColumn(
            state = reorderState.listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .reorderable(reorderState)
        ) {
            itemsIndexed(cache.list, key = { _, item -> item.id }) { _, item ->
                val desc = "${item.author} - v${item.version}"
                ShadowedDraggableItem(reorderableState = reorderState, key = item.id) {
                    val isSelected = remember(item.id) {
                        derivedStateOf { item.id in selectedIds }
                    }.value
                    Item(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .then(if (!selectionMode) { Modifier.detectReorderAfterLongPress(reorderState) } else Modifier),
                        hasDefVars = item.defVars.isNotEmpty(),
                        needSetVars = item.defVars.isNotEmpty() && item.userVars.isEmpty(),
                        name = item.name,
                        desc = desc,
                        iconUrl = item.iconUrl,
                        isEnabled = item.isEnabled,
                        onEnabledChange = {
                            dbm.pluginDao.update(item.copy(isEnabled = it))
                        },
                        isSelectionMode = selectionMode,
                        isSelected = isSelected,
                        onToggleSelection = {
                            selectedIds = if (item.id in selectedIds)
                                selectedIds - item.id
                            else selectedIds + item.id
                        },
                        onEdit = { onEdit(item) },
                        onSetVars = { showVarsSettings = item },
                        onAudioParams = { showAudioParamsDialog = item },
                        onDelete = { showDeleteDialog = item },
                        onClear = {
                            PluginManager(item).clearCache()
                            context.longToast(R.string.clear_cache_ok)
                        },
                        onExport = { showExportConfig = listOf(item) },
                        // 第11项: 内联展开编辑 + 运行键（跳编辑器并自动调试）
                        plugin = item,
                        onUpdatePlugin = { dbm.pluginDao.update(it) },
                        onSwitchPluginRefs = { showSwitchPluginRefsDialog = item },
                        onEditMetadata = { showEditMetadataDialog = item }
                    )
                }
            }

            item {
                Spacer(Modifier.padding(bottom = AppDefaultProperties.LIST_END_PADDING))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun Item(
    modifier: Modifier,
    hasDefVars: Boolean,
    needSetVars: Boolean,
    name: String,
    desc: String,
    iconUrl: String?,
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onClear: () -> Unit,
    onEdit: () -> Unit,
    onSetVars: () -> Unit,
    onAudioParams: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelection: () -> Unit = {},
    // 第11项: 列表项内联展开编辑元数据
    plugin: Plugin? = null,
    onUpdatePlugin: ((Plugin) -> Unit)? = null,
    // 切换引用配置：把所有引用当前插件id的配置项批量改为目标插件id
    onSwitchPluginRefs: (() -> Unit)? = null,
    // 编辑元数据（弹窗）：name/pluginId/author/version + 同步JS
    onEditMetadata: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    ElevatedCard(modifier = modifier
        .combinedClickable(
            onClick = { if (isSelectionMode) onToggleSelection() else if (hasDefVars) onSetVars() },
            onLongClick = { if (!isSelectionMode) onToggleSelection() }
        )
        .semantics {
            if (!isSelectionMode) {
                customActions = listOf(
                    CustomAccessibilityAction(context.getString(R.string.edit_desc, name)) { onEdit(); true },
                    CustomAccessibilityAction(context.getString(R.string.plugin_set_vars, name)) { onSetVars(); true },
                    CustomAccessibilityAction(context.getString(R.string.export_config)) { onExport(); true },
                    CustomAccessibilityAction(context.getString(R.string.clear_cache, name)) { onClear(); true },
                    CustomAccessibilityAction(context.getString(R.string.delete, name)) { onDelete(); true },
                )
            }
        }
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
                                if (isEnabled) R.string.plugin_enabled_desc else R.string.plugin_disabled_desc,
                                name
                            )
                            .let {
                                contentDescription = it
                                stateDescription = it
                            }
                    }
                )
                }

                PluginImage(model = iconUrl, name = name)

                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                        .fillMaxWidth(),
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                if (!isSelectionMode) {
                Row {
                    var showOptions by remember { mutableStateOf(false) }
                    IconButton(onClick = { showOptions = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            stringResource(id = R.string.more_options_desc, name)
                        )
                        DropdownMenu(
                            expanded = showOptions,
                            onDismissRequest = { showOptions = false }) {

                            // 第11项修复: 编辑/运行移入菜单,释放顶部空间给插件名
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.edit)) },
                                onClick = {
                                    showOptions = false
                                    onEdit()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, stringResource(R.string.edit))
                                }
                            )

                            // 编辑元数据（弹窗）：name/pluginId/author/version + 同步JS
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

                            if (hasDefVars)
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.plugin_set_vars)) },
                                    onClick = {
                                        showOptions = false
                                        onSetVars()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.EditNote, stringResource(R.string.plugin_set_vars))
                                    }
                                )

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

                            // 音频参数菜单项（位于导出下方）
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.plugin_audio_params)) },
                                onClick = {
                                    showOptions = false
                                    onAudioParams()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.VolumeUp, stringResource(R.string.plugin_audio_params))
                                }
                            )

                            HorizontalDivider()

                            // 切换引用配置：把所有引用当前插件id的配置项改为目标插件id
                            if (onSwitchPluginRefs != null) {
                                DropdownMenuItem(
                                    text = { Text("切换引用配置") },
                                    onClick = {
                                        showOptions = false
                                        onSwitchPluginRefs()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.SwapHoriz, "切换引用配置")
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.clear_cache)) },
                                onClick = {
                                    showOptions = false
                                    onClear()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.CleaningServices, stringResource(R.string.clear_cache))
                                }
                            )

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

            if (needSetVars && !isSelectionMode)
                Text(
                    text = stringResource(id = R.string.systts_plugin_please_set_vars),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
        }
    }
}