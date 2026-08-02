package com.github.jing332.tts_server_android.compose

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.jing332.common.utils.toast
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.nav.NavTopAppBar
import com.github.jing332.tts_server_android.compose.systts.list.ui.PluginTtsUI
import com.github.jing332.tts_server_android.compose.systts.speechrule.SpeechRuleManagerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolBoxScreen(sharedVM: SharedViewModel) {
    // 角色管理插件：按 pluginId 查找
    val plugin = remember { dbm.pluginDao.getByPluginId("mingwuyan") }
    val isRoleManagementPlugin = plugin != null

    val flow = remember { dbm.systemTtsV2.flowAllGroupWithTts().conflate() }
    val groups by flow.collectAsStateWithLifecycle(emptyList())

    // 查找已开启「仅界面模式」的角色管理配置项（用于初始加载和检测新增）
    // 注意：Room 2.6.1 的 @Relation 不支持 orderBy，list 顺序由 SQLite 决定可能不稳定，
    // 因此这里用 minByOrNull(id) 选 id 最小的那个，保证 flow 重发时 firstOrNull 不漂移
    val uiOnlyTts = remember(groups, isRoleManagementPlugin) {
        if (!isRoleManagementPlugin) null
        else groups.flatMap { it.list }
            .filter { tts ->
                val config = tts.config
                config is TtsConfigurationDTO &&
                    (config.source as? PluginTtsSource)?.let {
                        it.pluginId == "mingwuyan" && it.isUiOnly
                    } == true
            }
            .minByOrNull { it.id }
    }

    // 本地编辑状态：只在首次或切换到不同 id 的 uiOnly 配置时更新，
    // 不随 flow 抖动重置，避免用户在 ToolBox 内切换开关时 UI 重建丢失插件UI
    var currentTts by remember { mutableStateOf<SystemTtsV2?>(null) }
    LaunchedEffect(uiOnlyTts?.id) {
        // 仅当存在新的 uiOnly 配置且与当前不同时切换（不覆盖用户本地编辑如关闭开关）
        if (uiOnlyTts != null && currentTts?.id != uiOnlyTts.id) {
            currentTts = uiOnlyTts
        }
    }
    // 检测当前配置是否已被删除（在别处删除），若删除则清除本地状态
    LaunchedEffect(groups) {
        val currentId = currentTts?.id
        if (currentId != null && groups.flatMap { it.list }.none { it.id == currentId }) {
            currentTts = null
        }
    }

    val context = LocalContext.current
    // 朗读规则列表（轻量查询，不含 code，避免 Cursor 窗口溢出）
    var showSpeechRulePicker by remember { mutableStateOf(false) }
    val speechRules = remember { dbm.speechRuleDao.getAllWithoutCode() }
    if (showSpeechRulePicker) {
        AlertDialog(
            onDismissRequest = { showSpeechRulePicker = false },
            title = { Text("选择朗读规则运行") },
            text = {
                if (speechRules.isEmpty()) {
                    Text("暂无朗读规则")
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        speechRules.forEach { rule ->
                            TextButton(
                                onClick = {
                                    showSpeechRulePicker = false
                                    context.startActivity(
                                        Intent(context, SpeechRuleManagerActivity::class.java).apply {
                                            putExtra("ruleDbId", rule.id)
                                            putExtra("autoDebug", true)
                                        }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(rule.name.ifBlank { rule.ruleId })
                                    Text(
                                        "${rule.author.ifBlank { "未知" }} - v${rule.version}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSpeechRulePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    val scope = rememberCoroutineScope()
    // 顶部「仅界面模式」简易开关：与配置项编辑页内的开关功能一致，仅切换 isUiOnly 字段
    val isUiOnlyOn = currentTts?.let { tts ->
        ((tts.config as? TtsConfigurationDTO)?.source as? PluginTtsSource)?.isUiOnly == true
    } ?: false

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NavTopAppBar(
                title = { Text(stringResource(R.string.toolbox)) },
                scrollBehavior = scrollBehavior,
                actions = {
                    // 仅界面模式开关：仅角色管理插件(mingwuyan)安装后显示
                    if (isRoleManagementPlugin && plugin != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.plugin_ui_only_mode),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Switch(
                                checked = isUiOnlyOn,
                                onCheckedChange = { enabled ->
                                    val tts = currentTts
                                    if (tts == null) {
                                        // 没有可切换的配置项：提示去配置列表添加
                                        context.toast("请先在系统TTS添加角色管理配置项")
                                    } else {
                                        val src = (tts.config as? TtsConfigurationDTO)
                                            ?.source as? PluginTtsSource
                                        if (src == null) {
                                            context.toast("当前配置项非插件类型，无法切换")
                                        } else {
                                            val updated = tts.copy(
                                                config = (tts.config as TtsConfigurationDTO).copy(
                                                    source = src.copy(isUiOnly = enabled)
                                                )
                                            )
                                            currentTts = updated
                                            scope.launch(Dispatchers.IO + NonCancellable) {
                                                dbm.systemTtsV2.insert(updated)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                    // 运行朗读规则 JS 快捷键：选择规则后自动进入编辑器并运行
                    IconButton(onClick = { showSpeechRulePicker = true }) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "运行朗读规则"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        val activeTts = currentTts
        when {
            !isRoleManagementPlugin || plugin == null -> {
                // 插件未安装
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            stringResource(R.string.toolbox_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            activeTts != null -> {
                // 显示角色管理配置项的编辑页面（不管 isUiOnly 状态，始终显示完整内容）
                var systts by remember(activeTts.id) { mutableStateOf(activeTts) }
                val latestSystts by rememberUpdatedState(systts)

                // 离开页面时保存，持久化变更；用结构化协程替代裸 Thread，保证写入时机可控
                DisposableEffect(activeTts.id) {
                    onDispose {
                        scope.launch(Dispatchers.IO + NonCancellable) {
                            dbm.systemTtsV2.insert(latestSystts)
                        }
                    }
                }

                val ui = remember { PluginTtsUI() }
                ui.EditContentScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState()),
                    systts = systts,
                    onSysttsChange = { systts = it },
                    showBasicInfo = false,
                    plugin = plugin,
                    showPluginSelector = false,
                )
            }
            else -> {
                // 插件已安装但未开启仅界面模式
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            stringResource(R.string.toolbox_no_ui_only_config),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
