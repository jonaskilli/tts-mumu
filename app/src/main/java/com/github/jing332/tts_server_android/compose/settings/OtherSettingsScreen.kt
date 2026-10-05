package com.github.jing332.tts_server_android.compose.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.github.jing332.common.utils.clearWebViewData
import com.github.jing332.common.utils.toast
import com.github.jing332.tts_server_android.AppLocale
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.app
import com.github.jing332.tts_server_android.compose.AboutDialog
import com.github.jing332.tts_server_android.conf.AppConfig
import java.io.File

@Composable
internal fun ColumnScope.OtherSettingsScreen(search: SettingsSearch) {
    // 「数据与关于」（10-05 分区；同日用户令：本区无关紧要，默认**折叠**——设置页条目太多）
    SettingsGroup(
        title = { Text("数据与关于") },
        show = !search.active(),
        collapsible = true,
        defaultExpanded = false,
    ) {
    val context = LocalContext.current

    // 「语言」（10-05 用户令：语言不能放常用，放最后的关于里）——自 MainSettingsScreen 常用区整块迁来，
    // languageKeys / languageNames / langMenu 一并搬（行为与关键词完全不变）
    val languageKeys = remember {
        mutableListOf("").apply { addAll(AppLocale.localeMap.keys.toList()) }
    }

    val languageNames = remember {
        AppLocale.localeMap.map { "${it.value.displayName} - ${it.value.getDisplayName(it.value)}" }
            .toMutableList()
            .apply { add(0, context.getString(R.string.follow_system)) }
    }

    var langMenu by remember { mutableStateOf(false) }
    SettingItem(search, "语言", "language", "locale", "地区") {
    DropdownPreference(
        Modifier.minimumInteractiveComponentSize(),
        expanded = langMenu,
        onExpandedChange = { langMenu = it },
        icon = {
            Icon(Icons.Default.Language, null)
        },
        title = { Text(stringResource(id = R.string.language)) },
        subTitle = {
            Text(
                if (AppLocale.getLocaleCodeFromFile(context).isEmpty()) {
                    stringResource(id = R.string.follow_system)
                } else {
                    AppLocale.getLocaleFromFile(context).displayName
                }
            )
        }) {
        languageNames.forEachIndexed { index, name ->
            DropdownMenuItem(
                text = {
                    Text(name)
                }, onClick = {
                    langMenu = false

                    AppLocale.saveLocaleCodeToFile(context, languageKeys[index])
                    AppLocale.setLocale(app as Context)
                }
            )
        }
    }
    }

    // 「自动检查更新」已删（10-05 用户令：应用内更新功能整体退役，AppConfig.isAutoCheckUpdateEnabled 一并拆除）

    SettingItem(search, "最近任务", "排除", "recent", "后台") {
        var excludeFromRecent by remember { AppConfig.isExcludeFromRecent }
        SwitchPreference(
            title = { Text(stringResource(id = R.string.exclude_from_recent)) },
            subTitle = { Text(stringResource(id = R.string.exclude_from_recent_summary)) },
            checked = excludeFromRecent,
            onCheckedChange = { excludeFromRecent = it },
            icon = { Icon(Icons.Default.HideSource, contentDescription = null) }
        )
    }

    // 「下拉框内容最大数」已删（10-05 用户令）：该设置写 AppConfig.spinnerMaxDropDownCount，
    // 但全仓无任何消费点——AppSpinner 用的是 lib-compose 的 ComposeWidgetSettings.maxDropDownCount
    // （硬编码 3），两处从未接线，即拨动此开关不产生任何效果（假开关）。删后下拉阈值仍恒为 3。

    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    if (showAboutDialog)
        AboutDialog { showAboutDialog = false }
    SettingItem(search, "关于", "about", "版本", "作者") {
        BasePreferenceWidget(
            onClick = {
                showAboutDialog = true
            }, title = {
                Text(stringResource(R.string.about))
            }, icon = {
                Icon(Icons.Default.Info, null)
            }
        )
    }

    // 「帮助」入口与帮助文档整页已删（10-05 用户令：帮助文档取消，给删了）
    // —— AppHelpDocumentActivity + manifest 声明 + app_help_document 串 + assets/help/app.md 一并退役


    // 「检查更新」入口已删（10-05 用户令：应用内更新功能整体退役）

    SettingItem(search, "清除网页数据", "缓存", "cache", "webview") {
        BasePreferenceWidget(
            onClick = {
                context.clearWebViewData()
                context.toast(R.string.clear_cache_ok)
            },
            title = { Text(stringResource(R.string.clear_web_data)) },
            icon = {
                Icon(Icons.Default.CleaningServices, null)
            }
        )
    }

    // 清空数据：效果同长按软件-清除该软件数据
    var showClearDataDialog by rememberSaveable { mutableStateOf(false) }
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("清空数据") },
            text = { Text("此操作将清除本应用的所有数据（包括配置、数据库、缓存等），效果等同于系统设置中的「清除数据」。操作不可恢复，确定继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDataDialog = false
                    // 清除应用所有内部数据
                    context.cacheDir.deleteRecursively()
                    context.filesDir.deleteRecursively()
                    context.databaseList().forEach { context.deleteDatabase(it) }
                    File(context.filesDir.parentFile, "shared_prefs").deleteRecursively()
                    // 直接重启
                    com.github.jing332.tts_server_android.App.instance.restart()
                }) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    SettingItem(search, "清空数据", "clear", "data", "重置应用") {
        BasePreferenceWidget(
            onClick = { showClearDataDialog = true },
            title = { Text("清空数据") },
            subTitle = { Text("清除本应用的所有数据") },
            icon = {
                Icon(Icons.Default.DeleteSweep, null)
            }
        )
    }
    }
}
