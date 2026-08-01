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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.AppLocale
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.list.ui.PluginTtsUI
import com.github.jing332.tts_server_android.toCode
import kotlinx.coroutines.flow.conflate

@Composable
fun ToolBoxScreen(sharedVM: SharedViewModel) {
    val context = LocalContext.current

    // 角色管理栏专属：仅展示 pluginId为mingwuyan 且 name含"角色管理" 的插件
    val plugin = remember { dbm.pluginDao.getByPluginId("mingwuyan") }
    val isRoleManagementPlugin = plugin != null && plugin.name.contains("角色管理")

    val flow = remember { dbm.systemTtsV2.flowAllGroupWithTts().conflate() }
    val groups by flow.collectAsStateWithLifecycle(emptyList())

    // 查找已有的角色管理配置
    val existingTts = remember(groups, isRoleManagementPlugin) {
        if (!isRoleManagementPlugin) null
        else groups.flatMap { it.list }.firstOrNull { tts ->
            val config = tts.config
            config is TtsConfigurationDTO &&
                (config.source as? PluginTtsSource)?.pluginId == "mingwuyan"
        }
    }

    Scaffold { paddingValues ->
        if (!isRoleManagementPlugin || plugin == null) {
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
        } else {
            // 直接展示角色管理插件UI（无需点击卡片再打开Activity）
            val initialTts = remember(existingTts, plugin) {
                existingTts ?: SystemTtsV2(
                    config = TtsConfigurationDTO(
                        source = PluginTtsSource(
                            pluginId = "mingwuyan",
                            locale = AppLocale.current(context).toCode(),
                            plugin = plugin
                        )
                    )
                )
            }
            var systts by remember { mutableStateOf(initialTts) }

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
    }
}
