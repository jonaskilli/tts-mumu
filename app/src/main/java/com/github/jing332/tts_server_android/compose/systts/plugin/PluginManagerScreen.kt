package com.github.jing332.tts_server_android.compose.systts.plugin

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Output
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.jing332.common.utils.longToast
import com.github.jing332.compose.rememberLazyListReorderCache
import com.github.jing332.compose.widgets.ShadowedDraggableItem
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.AppDefaultProperties
import com.github.jing332.tts_server_android.compose.LocalNavController
import com.github.jing332.tts_server_android.compose.SharedViewModel
import com.github.jing332.tts_server_android.compose.systts.ConfigDeleteDialog
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import com.github.jing332.tts_server_android.utils.MyTools
import kotlinx.coroutines.flow.conflate
import kotlinx.serialization.encodeToString
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PluginManagerScreen(sharedVM: SharedViewModel, onFinishActivity: () -> Unit) {
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
    val context = LocalContext.current

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

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(contentWindowInsets = WindowInsets(0),
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(stringResource(id = R.string.plugin_manager)) },
                navigationIcon = {
                    IconButton(onClick = onFinishActivity) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(id = R.string.nav_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onEdit()
                    }) {
                        Icon(Icons.Default.Add, stringResource(id = R.string.add_config))
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
                                    showExportConfig = dbm.pluginDao.allEnabled
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
                }
            )
        }) { paddingValues ->
        val flowAll = remember { dbm.pluginDao.flowAll().conflate() }
        val list by flowAll.collectAsStateWithLifecycle(emptyList())

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
                val desc = remember { "${item.author} - v${item.version}" }
                ShadowedDraggableItem(reorderableState = reorderState, key = item.id) {
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                        .detectReorderAfterLongPress(reorderState)
                    Item(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .detectReorderAfterLongPress(reorderState),
                        hasDefVars = item.defVars.isNotEmpty(),
                        needSetVars = item.defVars.isNotEmpty() && item.userVars.isEmpty(),
                        name = item.name,
                        desc = desc,
                        iconUrl = item.iconUrl,
                        isEnabled = item.isEnabled,
                        onEnabledChange = {
                            dbm.pluginDao.update(item.copy(isEnabled = it))
                        },
                        onEdit = { onEdit(item) },
                        onSetVars = { showVarsSettings = item },
                        onAudioParams = { showAudioParamsDialog = item },
                        onDelete = { showDeleteDialog = item },
                        onClear = {
                            PluginManager(item).clearCache()
                            context.longToast(R.string.clear_cache_ok)
                        },
                        onExport = { showExportConfig = listOf(item) }
                    )
                }
            }

            item {
                Spacer(Modifier.padding(bottom = AppDefaultProperties.LIST_END_PADDING))
            }
        }
    }
}

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
) {
    val context = LocalContext.current
    ElevatedCard(modifier = modifier.semantics {
        customActions =
            listOf(
                CustomAccessibilityAction(
                    context.getString(R.string.edit_desc, name)
                ) { onEdit();true },
                CustomAccessibilityAction(
                    context.getString(R.string.plugin_set_vars, name)
                ) { onSetVars();true },
                CustomAccessibilityAction(
                    context.getString(R.string.export_config)
                ) { onExport();true },

                CustomAccessibilityAction(
                    context.getString(R.string.clear_cache, name)
                ) { onClear();true },
                CustomAccessibilityAction(
                    context.getString(R.string.delete, name)
                ) { onDelete();true },
            )
    }, onClick = {
        if (hasDefVars) onSetVars()
    }) {
        Box(modifier = Modifier.padding(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, stringResource(id = R.string.edit_desc, name))
                    }

                    var showOptions by remember { mutableStateOf(false) }
                    IconButton(onClick = { showOptions = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            stringResource(id = R.string.more_options_desc, name)
                        )
                        DropdownMenu(
                            expanded = showOptions,
                            onDismissRequest = { showOptions = false }) {

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

            if (needSetVars)
                Text(
                    text = stringResource(id = R.string.systts_plugin_please_set_vars),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
        }
    }
}