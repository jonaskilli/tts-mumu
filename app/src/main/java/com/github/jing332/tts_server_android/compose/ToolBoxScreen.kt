package com.github.jing332.tts_server_android.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.nav.NavTopAppBar
import com.github.jing332.tts_server_android.compose.systts.list.ui.PluginTtsUI
import kotlinx.coroutines.flow.conflate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolBoxScreen(sharedVM: SharedViewModel) {
    // 角色管理插件：按 pluginId 查找
    val plugin = remember { dbm.pluginDao.getByPluginId("mingwuyan") }
    val isRoleManagementPlugin = plugin != null

    val flow = remember { dbm.systemTtsV2.flowAllGroupWithTts().conflate() }
    val groups by flow.collectAsStateWithLifecycle(emptyList())

    // 查找已开启「仅界面模式」的角色管理配置项（旧方法：由用户在编辑页手动开启）
    val uiOnlyTts = remember(groups, isRoleManagementPlugin) {
        if (!isRoleManagementPlugin) null
        else groups.flatMap { it.list }.firstOrNull { tts ->
            val config = tts.config
            config is TtsConfigurationDTO &&
                (config.source as? PluginTtsSource)?.let {
                    it.pluginId == "mingwuyan" && it.isUiOnly
                } == true
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NavTopAppBar(
                title = { Text(stringResource(R.string.toolbox)) },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { paddingValues ->
        if (!isRoleManagementPlugin || plugin == null) {
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
        } else if (uiOnlyTts != null) {
            // 显示已开启仅界面模式的角色管理配置项编辑页面（与编辑界面一致，变更联通）
            var systts by remember(uiOnlyTts.id) { mutableStateOf(uiOnlyTts) }
            val latestSystts by rememberUpdatedState(systts)

            // 离开页面时保存，持久化变更
            DisposableEffect(uiOnlyTts.id) {
                onDispose {
                    Thread {
                        dbm.systemTtsV2.insert(latestSystts)
                    }.start()
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
        } else {
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
