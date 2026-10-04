package com.github.jing332.tts_server_android.compose.systts.plugin

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.jing332.compose.widgets.AppSelectionDialog
import com.github.jing332.compose.widgets.LocalSelectionRowHorizontalPadding
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.tts_server_android.R

@Composable
fun PluginSelectionDialog(onDismissRequest: () -> Unit, onSelect: (Plugin) -> Unit) {
    val plugins = dbm.pluginDao.allEnabled
    if (plugins.isEmpty()) {
        AlertDialog(onDismissRequest = onDismissRequest,
            title = { Text(stringResource(id = R.string.select_plugin)) },
            text = {
                Text(
                    stringResource(id = R.string.no_plugins),
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error
                )
            },
            confirmButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
        return
    }

    // 与配置项编辑页的插件选择同款形态（AppSelectionDialog：顶部常显搜索框 + 条目
    // bodyMedium 14sp + PluginImage 头像，字号与该页 itemContent 一致）。原自绘
    // AlertDialog 的列表无搜索、跟随正文槽字号（用户字体放大后大得离谱）。
    // 搜索按插件名匹配（与参照页同口径，不搜 pluginId）。
    AppSelectionDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.select_plugin)) },
        // 纯选择弹窗无「当前项」：给一个永不命中的值，打开不定位、无高亮
        value = -1L,
        values = plugins.map { it.id },
        entries = plugins.map { it.name },
        icons = plugins.map { it.iconUrl },
        searchEnabled = true,
        onClick = { id, _ -> plugins.find { it.id == id }?.let(onSelect) },
        itemContent = { _, entry, icon, _ ->
            PluginImage(model = icon, name = entry)
            Text(
                entry,
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        horizontal = LocalSelectionRowHorizontalPadding.current,
                        vertical = 12.dp
                    ),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}
