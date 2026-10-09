package com.github.jing332.tts_server_android.compose.systts.plugin

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import com.github.jing332.compose.widgets.AppDropdownMenu
import com.github.jing332.compose.widgets.DenseTextField
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import com.github.jing332.tts_server_android.compose.nav.NavTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.jing332.common.utils.longToast
import com.github.jing332.common.utils.toast
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import com.github.jing332.compose.rememberLazyListReorderCache
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.ShadowedDraggableItem
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.database.entities.systts.source.TextToSpeechSource
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.tts.speech.TextToSpeechProvider
import com.github.jing332.tts.speech.plugin.PluginTtsProvider
import com.github.jing332.tts_server_android.compose.systts.AuditionDialog
import com.github.jing332.tts_server_android.compose.systts.list.SourceSwitchCheckDialog
import com.github.jing332.script.JsMetadataSyncer
import com.github.jing332.tts.speech.plugin.engine.TtsPluginUiEngineV2
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.AppDefaultProperties
import com.github.jing332.tts_server_android.conf.AppConfig
import com.github.jing332.tts_server_android.compose.LocalNavController
import com.github.jing332.tts_server_android.compose.SharedViewModel
import com.github.jing332.tts_server_android.compose.systts.ConfigDeleteDialog
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import com.github.jing332.tts_server_android.utils.MyTools
import com.drake.net.utils.withIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            onDismissRequest = { showExportConfig = null }) { isExportVars ->
            // 导出统一为 TTS 原生格式（JRead 格式导出已移除：只保留 JRead→TTS 单向导入）
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
        // defVars 为空的 jread 系插件可能只在 onLoadUI 自绘表单里定义变量(已扫描兜底),
        // 只有连自绘表单字段都没有时才不弹
        if (plugin.defVars.isEmpty() &&
            scanSelfDrawnFormVars(plugin.code, plugin.defVars.keys).isEmpty()
        ) {
            showVarsSettings = null
        }
        PluginVarsBottomSheet(onDismissRequest = {
            dbm.pluginDao.update(plugin)
            // 变量(如api key)保存后立即失效该插件的引擎缓存：
            // 合成引擎是「访问即续期」的缓存，不清的话主界面试听仍提示“请先填写变量”
            com.github.jing332.tts.CachedEngineManager.removeByPluginId(plugin.pluginId)
            com.github.jing332.tts.speech.plugin.TtsPluginEngineManager.remove(plugin.pluginId)
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
                dbm.pluginDao.update(
                    plugin.copy(audioParams = newParams)
                )
                // 卡片"插件语速/音量"显示缓存失效（PluginDescriptor）
                com.github.jing332.tts_server_android.compose.systts.list.ui.PluginDescriptor
                    .invalidatePluginParamsCache(plugin.pluginId)
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
    // 待切换项（用户 09-12：音色校验在共用的 SourceSwitchCheckDialog 里做，这里只负责扫出范围）
    var switchCandidates by remember { mutableStateOf<List<SystemTtsV2>>(emptyList()) }
    if (pendingSwitch != null) {
        val scanSourceId = pendingSwitch!!.first.pluginId
        LaunchedEffect(pendingSwitch) {
            switchCandidates = emptyList()
            switchCandidates = withIO {
                dbm.systemTtsV2.getAllGroupWithTts().flatMap { it.list }
                    .filter { tts ->
                        val src = (tts.config as? TtsConfigurationDTO)?.source
                        src is PluginTtsSource && src.pluginId == scanSourceId
                    }
            }
        }
    }
    if (showSwitchPluginRefsDialog != null) {
        val sourcePlugin = showSwitchPluginRefsDialog!!
        // 用全部插件(不止已启用)，排除源插件本身
        val candidates = remember(sourcePlugin.id) {
            dbm.pluginDao.all.filter { it.pluginId != sourcePlugin.pluginId }
        }
        AppDialog(
            onDismissRequest = { showSwitchPluginRefsDialog = null },
            title = { Text("切换配置项至其他插件") },
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

    // 二次确认 + 执行批量切换（用户 09-12：切换前按 voice 严格校验，只切目标插件确实有的音色；
    // 不做名字兜底、不留强制全切口子；校验弹窗三处共用 = SourceSwitchCheckDialog）
    if (pendingSwitch != null) {
        val (sourcePlugin, target) = pendingSwitch!!
        SourceSwitchCheckDialog(
            title = "确认切换",
            message = "将把引用插件「${sourcePlugin.name}」的配置项改用「${target.name}」。\n" +
                "源插件本身不会被修改或删除。",
            items = switchCandidates,
            targetPluginId = target.pluginId,
            targetPluginName = target.name,
            onDismiss = { pendingSwitch = null },
            onConfirm = { matched ->
                pendingSwitch = null
                if (matched.isNotEmpty()) {
                    val newId = target.pluginId
                    scope.launch {
                        withIO {
                            // 单事务批量更新（逐条 update 会 N 次触发列表 Flow 重发射）
                            val toUpdate = matched.mapNotNull { tts ->
                                val config = tts.config
                                if (config is TtsConfigurationDTO) {
                                    val src = config.source
                                    if (src is PluginTtsSource)
                                        tts.copy(config = config.copy(source = src.copy(pluginId = newId)))
                                    else null
                                } else null
                            }
                            switchProgress = 0 to toUpdate.size
                            if (toUpdate.isNotEmpty()) {
                                dbm.runInTransaction {
                                    dbm.systemTtsV2.update(*toUpdate.toTypedArray())
                                }
                            }
                            switchProgress = null
                        }
                        SystemTtsService.notifyUpdateConfig()
                        snackbarHostState.showSnackbar(
                            "已切换 ${matched.size} 项配置到「${target.name}」",
                            duration = SnackbarDuration.Long
                        )
                    }
                }
            },
        )
    }

    // 切换进度对话框：数据库事务在后台执行，UI 立即反馈，避免误以为没成功
    if (switchProgress != null) {
        val (done, total) = switchProgress!!
        AppDialog(
            onDismissRequest = {},
            title = { Text("正在切换配置项…") },
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

    // 编辑元数据弹窗（09-12 曾改名「修改插件信息」，09-13 定：两处统一换回「编辑元数据」）：name/pluginId/author/version + 同步JS + pluginId变更检测
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
                        modifier = Modifier.fillMaxWidth()
                        // 插件名可很长（如"角色管理v9_模型拉取_密钥导出导入"）：不锁单行，
                        // 自动换行完整显示便于修改；弹窗内容区本身可竖向滚动
                    )
                    OutlinedTextField(
                        label = { Text("pluginId (JS: id)") },
                        value = editPluginId,
                        onValueChange = { editPluginId = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                    OutlinedTextField(
                        label = { Text("author") },
                        value = editAuthor,
                        onValueChange = { editAuthor = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                    OutlinedTextField(
                        label = { Text("version") },
                        value = editVersion,
                        onValueChange = { editVersion = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
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

    // 搜索过滤:插件多时肉眼难找,按 名称/pluginId 模糊匹配(用户:一般只搜名称,不匹配作者)
    // 搜索态在顶栏内完成(与主列表页一致)，不弹窗
    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredList = remember(list, searchQuery) {
        if (searchQuery.isBlank()) list
        else list.filter {
            it.name.contains(searchQuery, true) ||
                    it.pluginId.contains(searchQuery, true)
        }
    }

    BackHandler(enabled = isSearchMode) {
        isSearchMode = false
        searchQuery = ""
    }

    Scaffold(contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // M3 TopAppBar → 全站自绘 NavTopAppBar（56dp、动作键热区贴边）：顶栏 ⋮ 与列表行 ⋮ 同列
            NavTopAppBar(
                title = {
                    when {
                        // 搜索态：顶栏标题位换成圆角搜索框，输入即过滤背后列表
                        isSearchMode -> {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(CircleShape),
                                // 10-10 纯白表统一：与列表/设置/替换规则页搜索框同灰档 #F3F3F3
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = CircleShape
                            ) {
                                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.titleMedium) {
                                    DenseTextField(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("名称 / pluginId", maxLines = 1) },
                                        singleLine = true,
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { searchQuery = "" }) {
                                                    Icon(Icons.Default.Close, "清除")
                                                }
                                            }
                                        },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        shape = MaterialTheme.shapes.extraLarge
                                    )
                                }
                            }
                        }
                        selectionMode -> Text(context.getString(R.string.selected_count, selectedIds.size))
                        else -> Text(stringResource(id = R.string.plugin_manager))
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
                    when {
                        // 搜索态只留关闭：退出并恢复全量列表
                        isSearchMode -> {
                            IconButton(onClick = {
                                isSearchMode = false
                                searchQuery = ""
                            }) {
                                Icon(Icons.Default.Close, stringResource(id = R.string.close))
                            }
                        }
                        selectionMode -> {
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
                    } // end selectionMode
                    else -> {
                    IconButton(onClick = { isSearchMode = true }) {
                        Icon(
                            Icons.Default.Search, "搜索插件",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

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

                        AppDropdownMenu(
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
                    } // end else ->
                    } // end when
                }
            )
        }) { paddingValues ->
        // 过滤时禁用拖拽排序:onDragEnd 按显示顺序重写 order,子集顺序写库会打乱全部排序
        val cache = rememberLazyListReorderCache(filteredList)

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
            if (searchQuery.isNotBlank()) {
                item(key = "search_filter_bar") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // 10-06：12→8，对齐本页卡片盒线 8（容器 0 + 卡片 8）
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "筛选「${searchQuery}」:${filteredList.size}/${list.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { searchQuery = "" }) {
                            Text(stringResource(id = R.string.cancel))
                        }
                    }
                }
            }
            itemsIndexed(cache.list, key = { _, item -> item.id }) { _, item ->
                val desc = "${item.author} - v${item.version}"
                ShadowedDraggableItem(reorderableState = reorderState, key = item.id) {
                    val isSelected = remember(item.id) {
                        derivedStateOf { item.id in selectedIds }
                    }.value
                    // defVars 为空但 onLoadUI 自绘表单里有字面量变量的插件(火山豆包v7等)也算有变量入口;
                    // 正则扫 code 有成本,按插件缓存避免列表滚动重复扫
                    val hasVars = remember(item.id, item.code, item.defVars) {
                        item.defVars.isNotEmpty() ||
                                scanSelfDrawnFormVars(item.code, item.defVars.keys).isNotEmpty()
                    }
                    Item(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .then(if (!selectionMode && searchQuery.isBlank()) { Modifier.detectReorderAfterLongPress(reorderState) } else Modifier),
                        hasDefVars = hasVars,
                        needSetVars = hasVars && item.userVars.isEmpty(),
                        name = item.name,
                        desc = desc,
                        iconUrl = item.iconUrl,
                        isEnabled = item.isEnabled,
                        // 同一 pluginId 只允许启用一个（10-05 用户令）：启用本条时先停用同 pluginId 的
                        // 其它已启用条目。依据：插件选择器数据源是「全部已启用插件」，同 pluginId 多启用
                        // 会让选择器出现两个同名条目；运行侧 getEnabled(pluginId) 取哪条不确定。
                        // ⚠ 必须用 allEnabled（SELECT *，含完整 code），不能用 getAllEnabledWithoutCode()：
                        // 后者 code=''，交给 update() 会把插件 JS 覆盖成空串。
                        onEnabledChange = { enabled ->
                            scope.launch(Dispatchers.IO) {
                                if (enabled && item.pluginId.isNotBlank()) {
                                    val others = dbm.pluginDao.allEnabled.filter {
                                        it.id != item.id && it.pluginId == item.pluginId
                                    }
                                    if (others.isNotEmpty()) {
                                        dbm.pluginDao.update(
                                            *others.map { it.copy(isEnabled = false) }.toTypedArray()
                                        )
                                        withContext(Dispatchers.Main) {
                                            context.longToast(
                                                context.getString(R.string.plugin_only_one_enabled, others.size)
                                            )
                                        }
                                    }
                                }
                                dbm.pluginDao.update(item.copy(isEnabled = enabled))
                            }
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
    // 按插件音色分类入库：目标分组选择 + 导入进度
    var showImportByCategory by remember { mutableStateOf(false) }
    // 入库来源双选（10-10 用户令「留个口」）：null=待选 / "market"=音色广场 / "list"=现有列表
    var importSource by remember { mutableStateOf<String?>(null) }
    var showMarketplace by remember { mutableStateOf(false) }
    // 该插件是否声明音色广场协议（引擎实测探测一次；决定入库入口分不走广场）
    var supportsMarket by remember(plugin?.id) { mutableStateOf(false) }
    LaunchedEffect(plugin?.id) {
        val p = plugin ?: return@LaunchedEffect
        supportsMarket = withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val e = TtsPluginUiEngineV2(context, p)
                try { e.eval(); e.supportsVoiceCatalog() } finally { runCatching { e.destroy() } }
            }.getOrDefault(false)
        }
    }
    // 音色广场「入库」跳转自动弹出（10-10 衔接）：直接走广场路，不问来源
    LaunchedEffect(plugin?.id) {
        if (plugin != null && VoiceCatalogHandoff.peek(plugin.pluginId)) {
            importSource = "market"
            showMarketplace = true
        }
    }
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
        // 水平归零（10-04 ⋮ 对齐）：卡内容贴卡缘，末键 ⋮ 热区落卡缘——
        // 与主页配置项卡（卡外缘 8dp + 图标贴卡缘）同构，跨页 ⋮ 列一致
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelectionMode) {
                    // 视觉 17dp（10-06 用户令 0.85 缩放）、触摸 48dp（10-07 治「点勾不跟手」）：
                    // scale 连命中区一起缩（48→≈41dp），改外层 48dp 盒吃触摸、内层 Checkbox
                    // 只画不摸（onCheckedChange=null，密钥页行首对勾同款写法）
                    Box(
                        modifier = Modifier
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
                            .size(48.dp)
                            .toggleable(
                                value = isEnabled,
                                role = Role.Switch,
                                onValueChange = { onEnabledChange(it) }
                            )
                            .semantics {
                                context
                                    .getString(
                                        if (isEnabled) R.string.plugin_enabled_desc else R.string.plugin_disabled_desc,
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

                PluginImage(model = iconUrl, name = name)

                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                        .fillMaxWidth(),
                ) {
                    // 特例许可（用户 09-11 终裁"一并回原版"）：插件名回原版手调 15sp（让一让换更多内容，仍限两行）
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
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
                        AppDropdownMenu(
                            expanded = showOptions,
                            onDismissRequest = { showOptions = false }) {

                            // 编辑元数据（弹窗）：name/pluginId/author/version + 同步JS
                            // 用户 09-12 追加：排在「设置变量」之前
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

                            // 设置变量：插件声明了可设置变量时显示
                            // （用户 09-12：与相邻的「编辑元数据」原同用 EditNote 易混，改 Tune 调节旋钮）
                            if (hasDefVars)
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.plugin_set_vars)) },
                                    onClick = {
                                        showOptions = false
                                        onSetVars()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Tune, stringResource(R.string.plugin_set_vars))
                                    }
                                )

                            // 插件音频参数（用户 09-12 拍板：紧跟编辑类，有「设置变量」时为第3、否则第2）
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

                            // 音色分类入库（原「按插件音色分类入库」，10-10 用户令改名）：遍历插件的全部音色分类，将各分类下音色批量导入所选分组
                            if (plugin != null)
                                DropdownMenuItem(
                                    text = { Text("音色分类入库") },
                                    onClick = {
                                        showOptions = false
                                        showImportByCategory = true
                                    },
                                    leadingIcon = {
                                        // 用户 09-12：原 Input 与「导出 Output」同菜单易混，改「入库」语义
                                        Icon(Icons.Default.LibraryAdd, "音色分类入库")
                                    }
                                )

                            // 切换配置项至其他插件（定名，原「切换引用配置项至其他插件」）：把所有引用当前插件id的配置项改为目标插件id（用户 09-12 拍板：提到上栏、导出前）
                            if (onSwitchPluginRefs != null) {
                                DropdownMenuItem(
                                    text = { Text("切换配置项至其他插件") },
                                    onClick = {
                                        showOptions = false
                                        onSwitchPluginRefs()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.SwapHoriz, "切换配置项至其他插件")
                                    }
                                )
                            }

                            // 导出（用户 09-12 拍板：上栏最后一格）
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
                    // 13sp（10-06 用户令）：原 15sp 与插件名同级、比它服务的描述行(bodySmall 12sp)
                    // 还大 3sp，层级倒挂——看着比版本信息更抢眼。降到 13sp（全站既有档：
                    // 描述行/列表层标同档），读作"次级提示"而非"标题"；
                    // primary 高亮保留（它是可操作提示，染色正确）
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.primary
                )

            // 按插件音色分类入库：选分组 → 批量导入
            ImportByCategoryDialog(
                plugin = plugin,
                visible = showImportByCategory && importSource == "list" && plugin != null,
                onDismiss = { showImportByCategory = false; importSource = null },
                scope = scope,
                context = context
            )

            // 入库来源分流（10-10 用户两令合并）：普通插件（未声明音色广场协议）直接进
            // 现有声音列表（不问——广场路对它没有独有价值，数据与列表路同源）；
            // 广场协议插件保留双选（两个口都留：联网广场 / 现有列表）。
            // 广场跳转 handoff 自动走广场路（音色直接带着走，不问）
            if (showImportByCategory && importSource == null && plugin != null && !supportsMarket) {
                // 普通插件：直进现有列表（老弹窗 visible 条件吃 importSource=="list"）
                importSource = "list"
            }
            if (showImportByCategory && importSource == null && plugin != null && supportsMarket) {
                SourcePickerDialog(
                    onDismiss = { showImportByCategory = false },
                    onPick = { source ->
                        importSource = source
                        if (source == "market") showMarketplace = true
                        // "list" = 走老弹窗（visible 条件在上面）
                    }
                )
            }

            // 入库模式的音色广场（10-10 广场合一）：勾选/分类/试听后「导入」=入下拉+入库两动作一步
            if (showMarketplace && plugin != null) {
                MarketplaceImportDialog(
                    plugin = plugin,
                    onDismiss = { showMarketplace = false; importSource = null }
                )
            }
        }
    }
}

/**
 * 入库模式的音色广场宿主（10-10 广场合一）：广场面板 importMode=true，
 * 「导入(N)」= 勾选音色**入下拉列表 + 入库两动作一步**（用户拍板）：
 *  - 已点分类的按分类进子分组打标签；未点分类的按原名落子分组不打标签（照导，不拦）。
 *  - 导入完成后 Toast 汇总，面板关闭。
 * 引擎用 Activity 作用域共享 VM（与编辑页同一实例；本页首次用会走 load() 初始化）。
 */
@Composable
private fun MarketplaceImportDialog(
    plugin: Plugin,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vm: com.github.jing332.tts_server_android.compose.systts.list.ui.PluginTtsViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()
    // 引擎初始化（广场搜索依赖 engine.scope）：空 LinearLayout 只走 eval，无 UI 挂载
    val dummyLayout = remember { android.widget.LinearLayout(context) }
    LaunchedEffect(plugin.id) {
        runCatching {
            vm.load(
                context, plugin,
                com.github.jing332.database.entities.systts.source.PluginTtsSource(pluginId = plugin.pluginId),
                dummyLayout
            )
        }.onFailure { context.displayErrorDialog(it) }
    }
    // 两开关（等待分类/自动下一个）经广场 onSwitchesChanged 上来；试听弹窗在本宿主弹
    var waitCategory by remember { mutableStateOf(false) }
    var autoNext by remember { mutableStateOf(false) }
    var auditionItem by remember { mutableStateOf<com.github.jing332.tts.speech.plugin.engine.VoiceCatalogItem?>(null) }
    var importing by remember { mutableStateOf(false) }

    // 试听弹窗（复用 AuditionDialog：等待分类/自动下一个/分类点选/失败重播全都在）
    auditionItem?.let { item ->
        // 本地分类音色带 locale 段（面板经 item.description 传 "locale:xxx"）；联网音色无段=空
        val auditionLocale = item.description.takeIf { it.startsWith("locale:") }?.removePrefix("locale:").orEmpty()
        val auditionSystts = remember(item.id, auditionLocale) {
            com.github.jing332.database.entities.systts.SystemTtsV2(
                displayName = item.name,
                config = com.github.jing332.database.entities.systts.TtsConfigurationDTO(
                    source = com.github.jing332.database.entities.systts.source.PluginTtsSource(
                        pluginId = plugin.pluginId,
                        locale = auditionLocale,
                        voice = item.id
                    )
                )
            )
        }
        val engine = remember { vm.engine }
        AuditionDialog(
            systts = auditionSystts,
            engine = null, // 用 CachedEngineManager 路径（同编辑页行内 🎧 口径）
            voiceId = item.id,
            autoDismiss = !waitCategory,
            onCategoryAssigned = { _, category ->
                // 试听里点的分类写回广场卡片状态（MarketplaceDialog 的 categories map
                // 由 onSwitchesChanged 同款回调传递不了 map——直接走重播态卡片改派：
                // 这里通过 handoff 式内存单例回传）
                MarketplaceCategoryOverride.put(item.id, category)
                if (category != null && autoNext) {
                    // 自动下一个：跳到列表中当前项的下一个（广场列表顺序）
                    val items = vm.catalogItems
                    val idx = items.indexOfFirst { it.id == item.id }
                    if (idx in 0 until items.size - 1) auditionItem = items[idx + 1]
                }
            },
            onDismissRequest = { auditionItem = null }
        )
    }

    com.github.jing332.tts_server_android.compose.systts.list.ui.PluginVoiceMarketplaceDialog(
        vm = vm,
        // locale 空（本页没有语言选择）回落插件第一个语言，防按 locale 过滤的插件回空
        locale = "",
        onDismissRequest = onDismiss,
        onAudition = { item -> auditionItem = item },
        onPick = { picked ->
            // 「加入列表(N)」在入库模式=只补下拉不入库（导入键才是两动作）。本宿主没有
            // 当前配置的下拉可补——入库模式藏这键没必要，直接视为同导入。用户在编辑页
            // 大厅模式才有"只加下拉"诉求（那路由编辑页自己的 onPick 处理）。
            doMarketplaceImport(plugin, vm, picked, context, scope, onDismiss)
        },
        onImport = { picked ->
            doMarketplaceImport(plugin, vm, picked, context, scope, onDismiss)
        },
        onSwitchesChanged = { w, a -> waitCategory = w; autoNext = a },
    )
}

/** 广场勾选的分类改派（试听弹窗 → 宿主内存桥）：导入时读（与卡片点选的 map 合并语义） */
object MarketplaceCategoryOverride {
    val map = mutableMapOf<String, String>()
    fun put(voiceId: String, category: String?) {
        // null=取消分类=移键（map 只存「已分类」项）
        if (category == null) map.remove(voiceId) else map[voiceId] = category
    }
    fun takeAll(): Map<String, String> {
        val m = map.toMap()
        map.clear()
        return m
    }
}

/** 从 VM 引擎读某 poolId 的分类显示名（导入侧给本地音色定子分组名用；拉不到回空串） */
private fun localPoolsNameOf(
    vm: com.github.jing332.tts_server_android.compose.systts.list.ui.PluginTtsViewModel,
    poolId: String,
): String =
    runCatching { vm.engine.getLocales()[poolId] }.getOrNull().orEmpty()

/** 入库模式「导入/加入列表」执行体：勾选音色 → PluginCategoryImporter.importVoices 落库（未分类照导） */
private fun doMarketplaceImport(
    plugin: Plugin,
    vm: com.github.jing332.tts_server_android.compose.systts.list.ui.PluginTtsViewModel,
    picked: List<com.github.jing332.tts.speech.plugin.engine.VoiceCatalogItem>,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onDone: () -> Unit,
) {
    if (picked.isEmpty()) return
    scope.launch {
        // 手选分类（试听弹窗里点的）优先，否则广场标签映射
        val overrides = MarketplaceCategoryOverride.takeAll()
        val items = picked.map {
            // 本地分类音色带 "locale:xxx" 段（面板勾选时写入）→ poolId=分类 id、poolName=分类名
            // （importVoices 用 poolName 做未分类项的子分组名）；联网音色无段，走原口径
            val localLocale = it.description.takeIf { d -> d.startsWith("locale:") }?.removePrefix("locale:")
            val localPoolName = localLocale?.let { pid -> localPoolsNameOf(vm, pid) }.orEmpty()
            PluginCategoryImporter.VoiceItem(
                poolId = localLocale.orEmpty(),
                poolName = localPoolName.ifBlank { it.tags.firstOrNull().orEmpty() },
                voiceId = it.id,
                voiceName = it.name,
                categoryOverride = overrides[it.id]
                    ?: it.tags.firstOrNull()?.let { t -> PluginCategoryImporter.mapTagCategory(t) }
            )
        }
        val count = runCatching {
            PluginCategoryImporter.importVoices(context, plugin, items) { }
        }.fold(
            onSuccess = { it },
            onFailure = { e ->
                context.longToast("导入失败: ${e.message}")
                return@launch
            }
        )
        context.longToast("已导入 $count 个音色，已自动创建分组「${plugin.name}」")
        onDone()
    }
}

/** 入库来源双选弹窗（10-10 用户令「留个口」）：音色广场 / 现有声音列表 */
@Composable
private fun SourcePickerDialog(
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择音色来源") },
        text = {
            Column {
                listOf(
                    "market" to ("音色广场" to "联网搜索/筛选/试听，勾选后一键导入"),
                    "list" to ("现有声音列表" to "老方式：按插件分类勾池子后逐个试听分类"),
                ).forEach { (key, pair) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onPick(key) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(pair.first, style = MaterialTheme.typography.bodyLarge)
                            Text(pair.second, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
internal fun ImportByCategoryDialog(
    plugin: Plugin?,
    visible: Boolean,
    onDismiss: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    context: Context,
) {
    if (!visible || plugin == null) return

    // 音色广场交接（10-10 衔接）：非空=从广场「入库」跳过来，直接以这批音色进声音列表阶段
    val handoffItems = remember(plugin.id) { VoiceCatalogHandoff.take(plugin.pluginId) }

    // 插件音色分类列表：poolId → poolName
    data class CategoryItem(val poolId: String, val poolName: String, val mappedName: String?)

    // 声音行：key = poolId+voiceId（跨池不撞），用于勾选/分类集合的键
    data class VoiceRow(val poolId: String, val poolName: String, val voiceId: String, val voiceName: String) {
        val key: String get() = "$poolId\u0000$voiceId"
    }

    var categories by remember { mutableStateOf<List<CategoryItem>>(emptyList()) }
    var selectedPoolIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    // 0=选分类（勾池子）；1=选声音（试听/分类/勾选/导入）。无分类插件直接进 1
    var stage by remember { mutableStateOf(0) }
    var voices by remember { mutableStateOf<List<VoiceRow>>(emptyList()) }
    var selectedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    // 试听时手动分配的分类：voiceKey → 分类名（女童…旁白）
    var categoryOverrides by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var loadingVoices by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    // 正在试听的声音行下标（-1 = 未试听）；弹窗内上一个/下一个按此下标走
    var auditionIndex by remember { mutableStateOf(-1) }
    // 等待分类/自动下一个（10-10 用户令恢复旧版两开关，语义照 10-05 迁移前原样）：
    // 等待分类=试听播完不自动关弹窗（autoDismiss=false），留时间点分类；
    // 自动下一个=点了分类（真正分配，取消不算）后自动切下一个继续播
    var waitCategorySwitch by remember { mutableStateOf(false) }
    var autoNextSwitch by remember { mutableStateOf(false) }

    // 引擎贯穿弹窗生命周期：既用于拉池/声音，也用于试听合成；关闭时销毁
    val engine = remember(plugin.id) { TtsPluginUiEngineV2(context, plugin) }
    DisposableEffect(plugin.id) {
        onDispose { runCatching { engine.destroy() } }
    }

    fun loadVoices(pools: List<CategoryItem>) {
        loadingVoices = true
        scope.launch(Dispatchers.IO) {
            val rows = pools.flatMap { p ->
                runCatching { engine.getVoices(p.poolId) }.getOrNull().orEmpty()
                    .filter { it.id.isNotBlank() }
                    .map { VoiceRow(p.poolId, p.poolName, it.id, it.name) }
            }
            withContext(Dispatchers.Main) {
                voices = rows
                // 默认全不选（10-10 用户令「只导入自己试听分类过的」）：勾选由「点分类」驱动
                // ——试听里点了分类标签才自动勾上；盲入流（全选直导）由全选行手动触发
                selectedKeys = emptySet()
                categoryOverrides = emptyMap()
                loadingVoices = false
                stage = 1
            }
        }
    }

    // 引擎初始化与首批拉取；handoff 非空时走广场交接分支（不 eval 引擎——音色清单已在手）
    LaunchedEffect(plugin.id) {
        // 广场交接优先（10-10）：带音色来就直接进声音列表阶段，不查插件、不走勾池子
        if (handoffItems != null) {
            // 伪池 poolId=""（key="\u0000<voiceId>"）；importVoices 只用 voiceId/voiceName/分类
            voices = handoffItems.map { VoiceRow("", "", it.voiceId, it.voiceName) }
            // 广场带进来的音色本身就是用户在广场勾过的 → 保留勾选（与普通入口默认全不选不同）
            selectedKeys = voices.map { it.key }.toSet()
            // 广场标签能映射成标准人群名的，预填为该音色的分类（试听/胶囊可改）
            categoryOverrides = handoffItems.mapNotNull { item ->
                item.tag?.let { PluginCategoryImporter.mapTagCategory(it) }?.let { tag ->
                    "\u0000${item.voiceId}" to tag
                }
            }.toMap()
            stage = 1
            return@LaunchedEffect
        }
        // 初始化引擎并拉分类列表（getLocales/getVoices 为纯数据方法，不涉及合成）
        runCatching {
            engine.eval()
            engine.onLoad()
            categories = engine.getLocales().map { (id, name) ->
                CategoryItem(id, name, PluginCategoryImporter.mapTagCategory(name))
            }
            // 无分类插件：跳过勾池子，直接给全量声音列表（伪池 id=""，插件不认则该列表为空）
            if (categories.isEmpty()) loadVoices(listOf(CategoryItem("", "全部音色", null)))
        }
    }

    val allSelected = categories.isNotEmpty() && selectedPoolIds.size == categories.size
    val hasSelection = selectedPoolIds.isNotEmpty()
    val allVoicesSelected = voices.isNotEmpty() && selectedKeys.size == voices.size
    // 导入只认「已分类」的（10-10 用户令「只导入自己试听分类过的」）：勾了但没分类不算数——
    // 未分类项入库会按原名落子分组不打标签（盲入产物），正是用户不要的
    val importCount = voices.count { it.key in selectedKeys && categoryOverrides[it.key] != null }

    // 试听弹窗：复用编辑页同一组件（🎧 试听 + 三列分类标签 + 上一个/下一个 + 进度）
    if (auditionIndex in voices.indices) {
        val row = voices[auditionIndex]
        val auditionSystts = remember(row) {
            SystemTtsV2(
                displayName = row.voiceName,
                config = TtsConfigurationDTO(
                    source = PluginTtsSource(
                        pluginId = plugin.pluginId,
                        locale = row.poolId,
                        voice = row.voiceId
                    )
                )
            )
        }
        @Suppress("UNCHECKED_CAST")
        val provider = remember {
            PluginTtsProvider(context, plugin).also { it.engine = engine }
                as TextToSpeechProvider<TextToSpeechSource>
        }
        AuditionDialog(
            systts = auditionSystts,
            text = AppConfig.testSampleText.value,
            engine = provider,
            voiceId = row.voiceId,
            // 等待分类开关（10-10 用户令恢复旧逻辑）：开=播完不自动关，留时间点分类
            autoDismiss = !waitCategorySwitch,
            hasPrev = auditionIndex > 0,
            hasNext = auditionIndex < voices.size - 1,
            onCategoryAssigned = { _, category ->
                categoryOverrides = if (category == null) categoryOverrides - row.key
                else categoryOverrides + (row.key to category)
                // 手选分类即视为待导入项（否则分好类却没勾、导入漏掉它）
                if (category != null) {
                    selectedKeys = selectedKeys + row.key
                    // 自动下一个开关（旧逻辑原样）：真正分配分类才跳下一个，取消不跳
                    if (autoNextSwitch && auditionIndex < voices.size - 1) auditionIndex++
                }
            },
            onPrev = { if (auditionIndex > 0) auditionIndex-- },
            onNext = { if (auditionIndex < voices.size - 1) auditionIndex++ },
            assignedCategory = categoryOverrides[row.key],
            progressText = "${auditionIndex + 1}/${voices.size}",
            // 完成（10-10 用户令「没有保存键」）：分类点标签即已实时写入，此键=明确出口回列表
            onFinish = { auditionIndex = -1 },
            onDismissRequest = { auditionIndex = -1 }
        )
    }

    AlertDialog(
        onDismissRequest = { if (!importing) onDismiss() },
        // 标题只留固定功能名：插件名长（如"墨听_阿里云QwenAudio…桥接版_v2"）会把大字标题撑出五六行
        title = { Text(if (importing) "正在音色分类入库" else "音色分类入库") },
        text = {
            if (importing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(progressText, style = MaterialTheme.typography.bodyMedium)
                }
            } else if (stage == 0) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    // 插件名：次级信息行，小字最多两行省略，归属可见又不抢标题
                    Text(
                        text = plugin.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    // 全选行与分类行共用 CheckRow，勾选框绝对同列
                    CheckRow(
                        checked = allSelected,
                        onChecked = {
                            selectedPoolIds = if (allSelected) emptySet() else categories.map { it.poolId }.toSet()
                        },
                        label = if (allSelected) "取消全选" else "全选",
                        trailing = {
                            Text(
                                "已选 ${selectedPoolIds.size}/${categories.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    HorizontalDivider(Modifier.padding(bottom = 2.dp))
                    categories.forEach { item ->
                        CheckRow(
                            checked = item.poolId in selectedPoolIds,
                            onChecked = null,
                            label = item.poolName,
                            onClick = {
                                selectedPoolIds = if (item.poolId in selectedPoolIds) {
                                    selectedPoolIds - item.poolId
                                } else {
                                    selectedPoolIds + item.poolId
                                }
                            },
                            trailing = item.mappedName?.takeIf { it != item.poolName }?.let { mapped ->
                                @Composable {
                                    Text(
                                        text = "→ $mapped",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }
                }
            } else if (loadingVoices) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("正在读取音色…", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (voices.isEmpty()) {
                Text("该插件无音色")
            } else {
                // 声音列表：每行 🎧 试听 + 勾选；顶部全选行。默认全选（省事流直接导入）
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    item {
                        // 插件名：与阶段 0 同款次级行（10-10 用户实机：进列表阶段不知道在给哪个插件入库）
                        Text(
                            text = plugin.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        // 等待分类/自动下一个（10-10 用户令恢复旧版两开关）：
                        // 整行可点切对应开关，Toast 口径照 10-05 迁移前原样
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            SwitchRow(
                                label = "等待分类",
                                checked = waitCategorySwitch,
                                onToggle = {
                                    waitCategorySwitch = it
                                    context.toast(if (it) "已开启：试听后等待选择分类" else "已关闭：试听后自动关闭")
                                }
                            )
                            SwitchRow(
                                label = "自动下一个",
                                checked = autoNextSwitch,
                                onToggle = {
                                    autoNextSwitch = it
                                    context.toast(if (it) "已开启：选分类后自动试听下一个" else "已关闭：选分类后不自动切换")
                                }
                            )
                        }
                        CheckRow(
                            checked = allVoicesSelected,
                            onChecked = {
                                selectedKeys = if (allVoicesSelected) emptySet() else voices.map { it.key }.toSet()
                            },
                            label = if (allVoicesSelected) "取消全选" else "全选",
                            trailing = {
                                // 已分类计数（10-10 用户令「只导入自己试听分类过的」）：
                                // 导入只认已分类的，这个数才是真正会导的数量
                                Text(
                                    "已分类 ${importCount}/${voices.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                        HorizontalDivider(Modifier.padding(bottom = 2.dp))
                    }
                    items(voices, key = { it.key }) { row ->
                        val idx = voices.indexOfFirst { it.key == row.key }
                        // 行版式 10-10 搬旧编辑页多选弹窗（10-05 迁链时只搬了功能没搬版式，用户实机否决）：
                        // 勾选框独立点击；名字单行省略（插件原始长名不折行）；已分类行尾挂可点胶囊
                        // （点胶囊直接重选分类，不必重新进试听）；未分类行尾留 🎧。
                        VoiceImportRow(
                            checked = row.key in selectedKeys,
                            name = row.voiceName,
                            category = categoryOverrides[row.key],
                            onChecked = {
                                selectedKeys = if (row.key in selectedKeys) selectedKeys - row.key
                                else selectedKeys + row.key
                            },
                            onCategoryChange = { cat ->
                                categoryOverrides = if (cat == null) categoryOverrides - row.key
                                else categoryOverrides + (row.key to cat)
                                // 手选分类即视为待导入项（否则分好类却没勾、导入漏掉它）
                                if (cat != null) selectedKeys = selectedKeys + row.key
                            },
                            onAudition = { if (idx >= 0) auditionIndex = idx }
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (stage == 0) {
                // 有分类：先勾池子 → 下一步拉声音列表
                TextButton(
                    enabled = !importing && hasSelection,
                    onClick = {
                        val pools = categories.filter { it.poolId in selectedPoolIds }
                        if (pools.isNotEmpty()) loadVoices(pools)
                    }
                ) { Text("下一步") }
            } else {
                TextButton(
                    enabled = !importing && importCount > 0,
                    onClick = {
                        importing = true
                        // 与 importCount 同口径：只导已分类的（勾了但没分类的不进这批）
                        val items = voices.filter {
                            it.key in selectedKeys && categoryOverrides[it.key] != null
                        }.map {
                            PluginCategoryImporter.VoiceItem(
                                poolId = it.poolId,
                                poolName = it.poolName,
                                voiceId = it.voiceId,
                                voiceName = it.voiceName,
                                categoryOverride = categoryOverrides[it.key]
                            )
                        }
                        scope.launch {
                            val result = runCatching {
                                PluginCategoryImporter.importVoices(context, plugin, items) { progressText = it }
                            }
                            result.fold(
                                onSuccess = { count -> context.longToast("已导入 $count 个音色，已自动创建分组「${plugin.name}」") },
                                onFailure = { e -> context.longToast("导入失败: ${e.message}") }
                            )
                            importing = false
                            onDismiss()
                        }
                    }
                ) { Text(if (importing) "导入中" else "导入 $importCount 个音色") }
            }
        },
        dismissButton = {
            // 声音列表阶段且插件有分类：可退回勾池子
            if (stage == 1 && categories.isNotEmpty()) {
                TextButton(enabled = !importing, onClick = { stage = 0; auditionIndex = -1 }) { Text("上一步") }
            } else {
                TextButton(enabled = !importing, onClick = onDismiss) { Text("取消") }
            }
        }
    )
}

/**
 * 分类入库的开关行（10-10 恢复旧版两开关的行形态）：Switch + 标签一行，供「等待分类/自动下一个」。
 */
@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable { onToggle(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(checked = checked, onCheckedChange = { onToggle(it) })
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

/**
 * 分类入库对话框的统一选择行：勾选框 + 标签（占满）+ 右侧元信息。
 * 全选行与分类行共用同一构件，勾选框/内边距绝对同列对齐。
 * [onChecked] 为勾选框自身回调（可为 null 只读）；[onClick] 为整行点击（可为 null）。
 */
@Composable
private fun CheckRow(
    checked: Boolean,
    label: String,
    onChecked: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            // 行间距：分类多时上下行不贴死
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 勾选框恒可交互：M3 Checkbox 只在 onCheckedChange!=null 时套 48dp 触摸盒，
        // 可空会令该行勾选框贴左、与可交互行错位 4dp（全选行缩进的真根因）
        Checkbox(
            checked = checked,
            onCheckedChange = { if (onChecked != null) onChecked() else onClick?.invoke() }
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(start = 4.dp)
                .weight(1f)
        )
        trailing?.invoke()
    }
}

/**
 * 分类入库声音行（版式 10-10 搬自旧编辑页多选弹窗，当时 10-05 迁链只搬功能没搬版式被否）：
 * 勾选框（独立点击）+ 名字（单行省略）+ 行尾 🎧 + 胶囊：已分类=tertiary 底分类名，
 * 未分类=描边「分类」占位；点胶囊弹菜单选/换/取消分类，不必进试听。
 */
@Composable
private fun VoiceImportRow(
    checked: Boolean,
    name: String,
    category: String?,
    onChecked: () -> Unit,
    onCategoryChange: (String?) -> Unit,
    onAudition: () -> Unit,
) {
    var showCategoryMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { onChecked() })
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = 4.dp)
                .weight(1f)
        )
        IconButton(onClick = onAudition) {
            Icon(Icons.Default.Headset, stringResource(id = R.string.audition))
        }
        Surface(
            shape = MaterialTheme.shapes.small,
            // 标签类胶囊统一淡绿：softContainerColor 单源（10-10 统一，原内联 lerp 0.4 已岔色）；
            // 原 tertiaryContainer 青蓝是全站唯一的第三色相，退役
            color = if (category != null) com.github.jing332.tts_server_android.compose.systts.role.softContainerColor()
            else MaterialTheme.colorScheme.surface,
            tonalElevation = if (category != null) 2.dp else 0.dp,
            border = if (category == null)
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            else null,
            modifier = Modifier
                .padding(end = 12.dp)
                .clip(MaterialTheme.shapes.small)
                .clickable { showCategoryMenu = true }
        ) {
            Text(
                category ?: "分类",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = if (category != null) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AppDropdownMenu(
            expanded = showCategoryMenu,
            onDismissRequest = { showCategoryMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("默认") },
                onClick = {
                    showCategoryMenu = false
                    onCategoryChange(null)
                }
            )
            com.github.jing332.compose.widgets.VoiceCategories.COLUMNS.forEach { column ->
                column.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = {
                            showCategoryMenu = false
                            onCategoryChange(cat)
                        }
                    )
                }
            }
        }
    }
}