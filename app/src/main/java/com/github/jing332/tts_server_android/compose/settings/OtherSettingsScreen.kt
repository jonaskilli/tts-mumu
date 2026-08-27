package com.github.jing332.tts_server_android.compose.settings

import android.content.Intent
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.github.jing332.common.utils.clearWebViewData
import com.github.jing332.common.utils.toast
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.AboutDialog
import com.github.jing332.tts_server_android.compose.LocalUpdateCheckTrigger
import com.github.jing332.tts_server_android.ui.AppHelpDocumentActivity
import java.io.File

@Composable
internal fun ColumnScope.OtherSettingsScreen(search: SettingsSearch) {
    if (!search.active())
        DividerPreference { Text(stringResource(R.string.other)) }
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

    val context = LocalContext.current
    SettingItem(search, "帮助", "help", "文档", "教程") {
        BasePreferenceWidget(
            onClick = {
                context.startActivity(
                    Intent(
                        context,
                        AppHelpDocumentActivity::class.java
                    ).apply { action = Intent.ACTION_VIEW }
                )
            },
            title = { Text(stringResource(R.string.app_help_document)) },
            icon = {
                Icon(Icons.AutoMirrored.Default.HelpOutline, null)
            }
        )
    }


    val updateCheckTrigger = LocalUpdateCheckTrigger.current
    SettingItem(search, "检查更新", "更新", "update", "升级") {
        BasePreferenceWidget(
            onClick = { updateCheckTrigger.value = true },
            title = { Text(stringResource(R.string.check_update)) },
            icon = {
                Icon(Icons.Default.Refresh, null)
            }
        )
    }


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
