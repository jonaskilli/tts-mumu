package com.github.jing332.tts_server_android.compose

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.jing332.common.utils.longToast
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.plugin.PluginPreviewActivity
import com.github.jing332.tts_server_android.compose.AppDefaultProperties
import kotlinx.coroutines.flow.conflate

@Composable
fun ToolBoxScreen(sharedVM: SharedViewModel) {
    val context = LocalContext.current
    val flow = remember { dbm.systemTtsV2.flowAllGroupWithTts().conflate() }
    val groups by flow.collectAsStateWithLifecycle(emptyList())
    val tools = remember(groups) {
        groups.flatMap { it.list }.filter { tts ->
            val config = tts.config
            if (config !is TtsConfigurationDTO) false
            else {
                val source = config.source
                source is PluginTtsSource && source.isUiOnly
            }
        }
    }

    Scaffold { paddingValues ->
        if (tools.isEmpty()) {
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
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(tools, key = { it.id }) { tts ->
                    ToolItem(tts) { openTool(context, tts) }
                }
                item {
                    androidx.compose.foundation.layout.Spacer(
                        Modifier.padding(bottom = AppDefaultProperties.LIST_END_PADDING)
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolItem(tts: SystemTtsV2, onClick: () -> Unit) {
    val name = tts.displayName ?: ""
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    name.ifBlank { stringResource(R.string.unnamed) },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    stringResource(R.string.plugin_ui_only_mode_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun openTool(context: Context, tts: SystemTtsV2) {
    val source = (tts.config as TtsConfigurationDTO).source as PluginTtsSource
    val plugin = dbm.pluginDao.getByPluginId(source.pluginId)
    if (plugin == null) {
        context.longToast(R.string.plugin_not_found)
        return
    }
    val intent = Intent(context, PluginPreviewActivity::class.java).apply {
        putExtra(PluginPreviewActivity.KEY_SOURCE, source)
        putExtra(PluginPreviewActivity.KEY_PLUGIN, plugin)
    }
    context.startActivity(intent)
}
