package com.github.jing332.tts_server_android.compose.systts.plugin

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.github.jing332.tts.speech.plugin.engine.TtsPluginUiEngineV2
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
    val snackbarHostState = remember { SnackbarHostState() }

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
            fileName = if (pluginList.size == 1) "插件-${pluginList[0].name}.json" else "插件-${pluginList.size}项.json",
            onDismissRequest = { showExportConfig = null }) { isExportVars, isJReadFormat ->
            if (isJReadFormat) {
                toJReadBundleJson(pluginList, isExportVars)
            } else {
                // 修复: 导出用 prettyPrint, 每个配置项独立一行可读
                if (isExportVars) {
                    AppConst.jsonBuilder.encodeToString(pluginList)
                } else {
                    AppConst.jsonBuilder.encodeToString(pluginList.map { it.copy(userVars = mutableMapOf()) })
                }
            }
        }
    }

    var showDeleteDialog by remember { mutableStateOf<Plugin?>(null) }
    if (showDeleteDialog != null) {
        val plugin = showDeleteDialog!!
        // 实时统计引用该插件的配置项数量
        val refCount = remember(plugin.pluginId) {
            dbm.systemTtsV2.getByPluginId(plugin.pluginId).size
        }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(id = R.string.delete)) },
            text = {
                Column {
                    Text("删除「${plugin.name}」？")
                    if (refCount > 0) Text("它正被 $refCount 个配置项使用。")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(id = R.string.cancel))
                }
            },
            confirmButton = {
                Row {
                    if (refCount > 0) {
                        // 慎重操作放左侧：插件+全部关联配置项一起删
                        TextButton(onClick = {
                            dbm.runInTransaction {
                                dbm.systemTtsV2.delete(*dbm.systemTtsV2.getByPluginId(plugin.pluginId).toTypedArray())
                                dbm.pluginDao.delete(plugin)
                            }
                            SystemTtsService.notifyUpdateConfig()
                            showDeleteDialog = null
                        }) {
                            Text("全部删除", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = {
                            dbm.pluginDao.delete(plugin)
                            SystemTtsService.notifyUpdateConfig()
                            showDeleteDialog = null
                        }) {
                            Text("仅删插件")
                        }
                    } else {
                        TextButton(onClick = {
                            dbm.pluginDao.delete(plugin)
                            showDeleteDialog = null
                        }) {
                            Text(stringResource(id = R.string.delete), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        )
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
            initialHandlesSpeed = plugin.pluginHandlesSpeed,
            initialHandlesVolume = plugin.pluginHandlesVolume,
            initialHandlesPitch = plugin.pluginHandlesPitch,
            onDismissRequest = { showAudioParamsDialog = null },
            onConfirm = { newParams, handlesSpeed, handlesVolume, handlesPitch ->
                dbm.pluginDao.update(
                    plugin.copy(
                        audioParams = newParams,
                        pluginHandlesSpeed = handlesSpeed,
                        pluginHandlesVolume = handlesVolume,
                        pluginHandlesPitch = handlesPitch
                    )
                )
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
    // 切换进度：null=未在切换，Pair(已处理, 总数)=切换中
    var switchProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
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
                            // 先收集需要切换的配置项，单事务批量更新：
                            // 逐条 update 每条一个事务且各触发一次列表Flow重发射，N项=卡顿N次
                            val toUpdate = dbm.systemTtsV2.getAllGroupWithTts()
                                .flatMap { it.list }
                                .mapNotNull { tts ->
                                    val config = tts.config
                                    if (config is TtsConfigurationDTO) {
                                        val src = config.source
                                        if (src is PluginTtsSource && src.pluginId == srcId)
                                            tts.copy(config = config.copy(source = src.copy(pluginId = newId)))
                                        else null
                                    } else null
                                }
                            switchProgress = 0 to toUpdate.size
                            if (toUpdate.isNotEmpty()) {
                                dbm.runInTransaction {
                                    dbm.systemTtsV2.update(*toUpdate.toTypedArray())
                                }
                                count = toUpdate.size
                            }
                            switchProgress = null
                        }
                        SystemTtsService.notifyUpdateConfig()
                        val msg = if (count == 0)
                            "没有找到引用「${sourcePlugin.name}」的配置项，无需切换"
                        else
                            "已切换 $count 项配置到「${target.name}」"
                        snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Long)
                    }
                }) { Text(stringResource(id = R.string.confirm)) }
            }
        )
    }

    // 切换进度对话框：数据库事务在后台执行，UI 立即反馈，避免误以为没成功
    if (switchProgress != null) {
        val (done, total) = switchProgress!!
        AppDialog(
            onDismissRequest = {},
            title = { Text("正在切换引用配置…") },
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("共 $total 项，正在批量更新…")
                }
            },
            buttons = {}
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
                            // 单事务批量更新，理由同切换引用配置
                            val toUpdate = dbm.systemTtsV2.getAllGroupWithTts()
                                .flatMap { it.list }
                                .mapNotNull { tts ->
                                    val config = tts.config
                                    if (config is TtsConfigurationDTO) {
                                        val src = config.source
                                        if (src is PluginTtsSource && src.pluginId == pending.first)
                                            tts.copy(config = config.copy(source = src.copy(pluginId = pending.second)))
                                        else null
                                    } else null
                                }
                            if (toUpdate.isNotEmpty()) {
                                dbm.runInTransaction {
                                    dbm.systemTtsV2.update(*toUpdate.toTypedArray())
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        // 多选导出：把选中的插件(按列表顺序)交给导出底栏
                        IconButton(
                            enabled = selectedIds.isNotEmpty(),
                            onClick = {
                                showExportConfig = list.filter { it.id in selectedIds }
                                selectionMode = false
                                selectedIds = emptySet()
                            }
                        ) {
                            Icon(Icons.Default.Output, stringResource(id = R.string.export_config))
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

                    // 多选入口(导出/删除都在多选模式里),用清单图标而非删除图标
                    IconButton(onClick = { selectionMode = true }) {
                        Icon(
                            Icons.Default.Checklist,
                            stringResource(id = R.string.multi_select)
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
    val scope = rememberCoroutineScope()
    // 按分类入库：目标分组选择 + 导入进度
    var showImportByCategory by remember { mutableStateOf(false) }
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
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, stringResource(id = R.string.edit_code_desc, name))
                    }
                    IconButton(onClick = { showOptions = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            stringResource(id = R.string.more_options_desc, name)
                        )
                        DropdownMenu(
                            expanded = showOptions,
                            onDismissRequest = { showOptions = false }) {

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

                            // 按分类入库：遍历插件的全部语言分类，将各分类下音色批量导入所选分组
                            if (plugin != null)
                                DropdownMenuItem(
                                    text = { Text("按分类入库") },
                                    onClick = {
                                        showOptions = false
                                        showImportByCategory = true
                                    },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Filled.Input, "按分类入库")
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

            // 按分类入库：选分组 → 批量导入
            ImportByCategoryDialog(
                plugin = plugin,
                visible = showImportByCategory && plugin != null,
                onDismiss = { showImportByCategory = false },
                scope = scope,
                context = context
            )
        }
    }
}

@Composable
private fun ImportByCategoryDialog(
    plugin: Plugin?,
    visible: Boolean,
    onDismiss: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    context: Context,
) {
    if (!visible || plugin == null) return

    // 插件音色分类列表：poolId → poolName
    data class CategoryItem(val poolId: String, val poolName: String, val mappedName: String?)
    var categories by remember { mutableStateOf<List<CategoryItem>>(emptyList()) }
    var selectedPoolIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var importing by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }

    LaunchedEffect(plugin.id) {
        // 在主线程初始化引擎拉取分类列表（getLocales 是纯数据方法，不涉及耗时合成）
        val engine = TtsPluginUiEngineV2(context, plugin)
        runCatching {
            engine.eval()
            engine.onLoad()
            categories = engine.getLocales().map { (id, name) ->
                CategoryItem(id, name, PluginCategoryImporter.mapTagCategory(name))
            }
            engine.destroy()
        }
    }

    val allSelected = categories.isNotEmpty() && selectedPoolIds.size == categories.size
    val hasSelection = selectedPoolIds.isNotEmpty()

    AlertDialog(
        onDismissRequest = { if (!importing) onDismiss() },
        title = { Text(if (importing) "正在按分类入库" else "按分类入库 - ${plugin.name}") },
        text = {
            if (importing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(progressText, style = MaterialTheme.typography.bodyMedium)
                }
            } else if (categories.isEmpty()) {
                Text("该插件无音色分类")
            } else {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    // 全选/取消全选
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = allSelected,
                            onCheckedChange = { checked ->
                                selectedPoolIds = if (checked) categories.map { it.poolId }.toSet() else emptySet()
                            }
                        )
                        Text(
                            text = if (allSelected) "取消全选" else "全选",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        Text(
                            text = "（已选 ${selectedPoolIds.size}/${categories.size}）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    categories.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedPoolIds = if (item.poolId in selectedPoolIds) {
                                    selectedPoolIds - item.poolId
                                } else {
                                    selectedPoolIds + item.poolId
                                }
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.poolId in selectedPoolIds,
                                onCheckedChange = null
                            )
                            Column(modifier = Modifier.padding(start = 4.dp).weight(1f)) {
                                // 原名
                                Text(
                                    text = item.poolName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                // 映射预览
                                item.mappedName?.let { mapped ->
                                    if (mapped != item.poolName) {
                                        Text(
                                            text = "→ $mapped",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !importing && hasSelection,
                onClick = {
                    importing = true
                    val poolIds = selectedPoolIds.toList()
                    scope.launch {
                        val result = runCatching {
                            PluginCategoryImporter.import(context, plugin, poolIds) { progressText = it }
                        }
                        result.fold(
                            onSuccess = { count -> context.longToast("已导入 $count 个音色，已自动创建分组「${plugin.name}」") },
                            onFailure = { e -> context.longToast("导入失败: ${e.message}") }
                        )
                        importing = false
                        onDismiss()
                    }
                }
            ) { Text(if (importing) "导入中" else "开始导入") }
        },
        dismissButton = {
            TextButton(enabled = !importing, onClick = onDismiss) { Text("取消") }
        }
    )
}