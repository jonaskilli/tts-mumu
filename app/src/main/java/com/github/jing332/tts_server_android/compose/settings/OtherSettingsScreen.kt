package com.github.jing332.tts_server_android.compose.settings

import android.content.Intent
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
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
import com.github.jing332.tts_server_android.compose.systts.directlink.LinkUploadRuleActivity
import java.io.File

@Composable
internal fun ColumnScope.OtherSettingsScreen(
    search: SettingsSearch,
    // 子页形态（10-05 用户令：「其他」由"页面内折叠"改为设置页上的入口行 → 独立子页）时，
    // 标题由子页顶栏承担，卡内不再出标题
    showGroupHeader: Boolean = true,
) {
    // 「其他」（原名「数据与关于」；10-05 用户令改名——本区已含语言，原名的"数据与关于"盖不住）
    SettingsGroup(
        title = { Text("其他") },
        show = !search.active(),
        showHeader = showGroupHeader,
    ) {
    val context = LocalContext.current

    // 关于（10-05 用户令：放本区第一个）
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

    // 最近任务排除（10-05 用户令：已迁往「后台与保活」区——它改的是系统"最近任务"里是否显示本 App）

    // 两个长度限制（10-05 用户令：自「其他」迁回「朗读与播放」区末尾——它们改的是列表页
    // "标签/名称"的显示截断，属朗读与列表呈现；消费点见 SysttsSettingsScreen 同项注释）

    // 「直链设置」（10-05 用户令：自「服务与网络」区迁来，并排在「清除网页数据」之前）
    // 本质是"配置导出 → 上传到直链（网盘）"那条链的 JS 规则编辑器，放任何"功能分区"里都不贴切，归「其他」
    SettingItem(search, "直链", "directlink", "链接", "direct", "上传", "网盘") {
        BasePreferenceWidget(
            icon = { Icon(Icons.Default.Link, null) },
            onClick = {
                context.startActivity(
                    Intent(
                        context, LinkUploadRuleActivity::class.java
                    ).apply { action = Intent.ACTION_VIEW })
            },
            title = { Text(stringResource(id = R.string.direct_link_settings)) },
        )
    }

    // —— 以下项目已退役（留记录，避免将来被"重新发现"）——
    // 语言：10-05 用户令只留中文（外语 strings.xml 五个目录一并删除；AppLocale 启动套用逻辑保留）
    // 自动检查更新 / 检查更新：应用内更新功能整体退役
    // 下拉框内容最大数：假开关（AppConfig.spinnerMaxDropDownCount 全仓无消费点，字段已删）
    // 帮助：帮助文档整页退役（AppHelpDocumentActivity + app_help_document 串 + assets/help 共 8 份文件）

    SettingItem(search, "清除网页数据", "缓存", "cache", "webview") {
        BasePreferenceWidget(
            onClick = {
                context.clearWebViewData()
                context.toast(R.string.clear_cache_ok)
            },
            title = { Text(stringResource(R.string.clear_web_data)) },
            icon = {
                Icon(Icons.Default.CleaningServices, null)
            },
            // 纯动作（就地清缓存），不是「进下一页」：不给右侧 ›
            showChevron = false,
        )
    }

    // 清空数据：效果同长按软件-清除该软件数据。**危险动作**（10-05 用户令：改红色字），
    // 排在区内最末；标题+图标都用 error 色，与列表页/插件页的删除类菜单同习语
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
            title = { Text("清空数据", color = MaterialTheme.colorScheme.error) },
            subTitle = { Text("清除本应用的所有数据") },
            icon = {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            // 危险行动作与图标都是 error 系，底衬跟着同色，不跟着全站主题绿（10-10 圆底批）
            iconContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
        )
    }
    }
}
