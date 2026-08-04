package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.SystemTtsGroup
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupEditContentDialog(
    group: SystemTtsGroup,
    onDismissRequest: () -> Unit,
    vm: GroupEditContentViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedConfigs by remember { mutableStateOf<Set<SystemTtsV2>>(emptySet()) }
    var searchType by remember { mutableStateOf(GroupSearchType.NAME) }
    val availableConfigs by vm.availableConfigs.collectAsStateWithLifecycle()
    val pluginNameCache by vm.pluginNameCache.collectAsStateWithLifecycle()

    LaunchedEffect(group.id) {
        vm.load(group.id)
    }

    val filteredConfigs = remember(searchQuery, searchType, availableConfigs, pluginNameCache) {
        vm.filterConfigs(availableConfigs, searchQuery, searchType, pluginNameCache)
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    if (showDeleteConfirm && selectedConfigs.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text("确定要删除选中的 ${selectedConfigs.size} 个音色吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            dbm.systemTtsV2.delete(*selectedConfigs.toTypedArray())
                            showDeleteConfirm = false
                            onDismissRequest()
                        }
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.9f),
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "${stringResource(R.string.edit_group_content)} - ${group.name}",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                // 搜索类型选择 + 全选按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                SearchTypeChip(
                    text = stringResource(R.string.name),
                    selected = searchType == GroupSearchType.NAME,
                    onClick = { searchType = GroupSearchType.NAME }
                )
                SearchTypeChip(
                    text = stringResource(R.string.tag),
                    selected = searchType == GroupSearchType.TAG,
                    onClick = { searchType = GroupSearchType.TAG }
                )
                SearchTypeChip(
                    text = stringResource(R.string.plugin),
                    selected = searchType == GroupSearchType.PLUGIN,
                    onClick = { searchType = GroupSearchType.PLUGIN }
                )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // 全选/取消全选按钮
                    val allSelected = filteredConfigs.isNotEmpty() && filteredConfigs.all { it in selectedConfigs }
                    IconButton(
                        onClick = {
                            selectedConfigs = if (allSelected) {
                                // 取消全选：移除当前过滤列表中的所有项目
                                selectedConfigs - filteredConfigs.toSet()
                            } else {
                                // 全选：添加当前过滤列表中的所有项目
                                selectedConfigs + filteredConfigs
                            }
                        },
                        enabled = filteredConfigs.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = if (allSelected) Icons.Default.Clear else Icons.Default.DoneAll,
                            contentDescription = if (allSelected) stringResource(R.string.clear) else stringResource(R.string.select_all)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 搜索框
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.search)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, stringResource(R.string.clear))
                            }
                        }
                    },
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 配置列表
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredConfigs, key = { it.id }) { config ->
                        val isSelected = config in selectedConfigs
                        ConfigItem(
                            config = config,
                            isSelected = isSelected,
                            onToggleSelection = {
                                selectedConfigs = if (isSelected) {
                                    selectedConfigs - config
                                } else {
                                    selectedConfigs + config
                                }
                            },
                            searchType = searchType,
                            pluginNameCache = pluginNameCache
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        scope.launch {
                            vm.moveConfigsToGroup(selectedConfigs.toList())
                            onDismissRequest()
                        }
                    },
                    enabled = selectedConfigs.isNotEmpty()
                ) {
                    Text(
                        stringResource(
                            R.string.move_to_group,
                            selectedConfigs.size,
                            group.name
                        )
                    )
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    enabled = selectedConfigs.isNotEmpty()
                ) {
                    Icon(
                        Icons.Default.DeleteForever,
                        null,
                        modifier = Modifier.padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}

@Composable
private fun SearchTypeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) }
    )
}

@Composable
private fun ConfigItem(
    config: SystemTtsV2,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    searchType: GroupSearchType,
    pluginNameCache: Map<String, String> = emptyMap()
) {
    // 安全获取 TtsConfigurationDTO
    val ttsConfig = config.config as? TtsConfigurationDTO
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelection() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = config.displayName,
                style = MaterialTheme.typography.bodyMedium
            )
            
            // 显示额外信息
            val extraInfo = when (searchType) {
                GroupSearchType.TAG -> {
                    if (ttsConfig != null && ttsConfig.speechRule.tagName.isNotEmpty()) {
                        "${stringResource(R.string.tag)}: ${com.github.jing332.tts_server_android.compose.systts.list.ui.ItemDescriptor.formatTagName(ttsConfig.speechRule.tagName)}"
                    } else null
                }
                GroupSearchType.PLUGIN -> {
                    when (val source = ttsConfig?.source) {
                        is PluginTtsSource -> {
                            // 使用缓存的插件名称
                            val pluginName = pluginNameCache[source.pluginId] ?: source.pluginId
                            "${stringResource(R.string.plugin)}: $pluginName"
                        }
                        is LocalTtsSource -> stringResource(R.string.local_tts_engine)
                        else -> null
                    }
                }
                GroupSearchType.NAME -> null
            }
            
            if (extraInfo != null) {
                Text(
                    text = extraInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


