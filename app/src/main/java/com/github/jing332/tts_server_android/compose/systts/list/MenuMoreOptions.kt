package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.github.jing332.common.utils.startActivity
import com.github.jing332.compose.widgets.AppDropdownMenu
import com.github.jing332.compose.widgets.CheckedMenuItem
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.asAppCompatActivity
import com.github.jing332.tts_server_android.compose.systts.plugin.PluginManagerActivity
import com.github.jing332.tts_server_android.compose.systts.replace.ReplaceManagerActivity
import com.github.jing332.tts_server_android.compose.systts.speechrule.SpeechRuleManagerActivity
import com.github.jing332.tts_server_android.conf.SystemTtsConfig
import com.github.jing332.tts_server_android.service.systts.SystemTtsService

@Composable
internal fun MenuMoreOptions(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onExportAll: () -> Unit,
    onBatchConfig: () -> Unit = {},
) {
    var showBgmSettingsDialog  by remember { mutableStateOf(false) }
    if (showBgmSettingsDialog)
        BgmSettingsDialog { showBgmSettingsDialog = false }

    var showImportSheet by remember { mutableStateOf(false) }
    if (showImportSheet)
        ListImportBottomSheet(onDismissRequest = { showImportSheet = false })

    val context = LocalContext.current
    val activity = remember { context.asAppCompatActivity() }

    var showAudioParamsDialog by remember { mutableStateOf(false) }
    if (showAudioParamsDialog)
        GlobalAudioParamsDialog {
            showAudioParamsDialog = false
            SystemTtsService.notifyUpdateConfig()
        }

    AppDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {

        var isSplit by remember { SystemTtsConfig.isSplitEnabled }
        CheckedMenuItem(
            text = { Text(stringResource(id = R.string.systts_split_long_sentences)) },
            checked = isSplit,
            onClick = {
                isSplit = it
            },
            leadingIcon = {
                Icon(Icons.Default.ContentCut, null)
            }
        )

        var isMultiVoice by remember { SystemTtsConfig.isMultiVoiceEnabled }
        CheckedMenuItem(
            text = { Text(stringResource(id = R.string.systts_multi_voice_option)) },
            checked = isMultiVoice,
            onClick = {
                isMultiVoice = it
                SystemTtsService.notifyUpdateConfig()
            },
            leadingIcon = {
                Icon(Icons.Default.Group, null)
            },
        )

        // 替换规则/朗读规则/插件（10-07 晚回退：恢复 10-06 前的老样子——用户确认这三条
        // 「以前一直都没改过，就这两天开始改的」，甲案上移+收敛删除整个撤销）。
        // 替换规则仍是混合体：点行进替换规则页、点勾选框控总开关（唯一落点）
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.audio_params_settings)) },
            onClick = { showAudioParamsDialog = true },
            leadingIcon = {
                Icon(Icons.Default.Speed, null)
            }
        )

        // 批量配置操作（用户 09-12 晚定稿：原「批量修改配置」「批量删除插件配置项」「批量调整音频参数」
        // 三个弹窗合并为本项，弹窗内胶囊分三段——音频参数 / 换插件 / 删除项；菜单入口随之只留这一个）
        DropdownMenuItem(
            text = { Text("批量配置操作") },
            onClick = {
                onDismissRequest()
                onBatchConfig()
            },
            leadingIcon = {
                Icon(Icons.Default.Build, null)
            }
        )

        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.bgm_settings)) },
            onClick = { showBgmSettingsDialog = true },
            leadingIcon = {
                Icon(Icons.Default.Audiotrack, null)
            }
        )

        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.speech_rule_manager)) },
            onClick = {
                onDismissRequest()
                context.startActivity(SpeechRuleManagerActivity::class.java)
            },
            leadingIcon = {
                Icon(Icons.AutoMirrored.Default.MenuBook, null)
            }
        )

        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.plugin_manager)) },
            onClick = {
                onDismissRequest()
                context.startActivity(PluginManagerActivity::class.java)
            },
            leadingIcon = {
                Icon(painterResource(id = R.drawable.ic_shortcut_plugin), null)
            }
        )

        // 「按插件音色分类入库」主界面直达入口已撤（10-10 用户令：不该放这，入库走插件管理页 ⋮）

        CheckedMenuItem(
            text = { Text(stringResource(id = R.string.replace_rule_manager)) },
            checked = SystemTtsConfig.isReplaceEnabled.value,
            onClick = {
                onDismissRequest()
                context.startActivity(ReplaceManagerActivity::class.java)
            },
            onClickCheckBox = {
                SystemTtsConfig.isReplaceEnabled.value = it
            },
            leadingIcon = {
                Icon(Icons.AutoMirrored.Default.ManageSearch, null)
            }
        )

        HorizontalDivider()
        DropdownMenuItem(text = {
            Text(stringResource(id = R.string.import_config))
        }, onClick = {
            onDismissRequest()
            showImportSheet = true },
            leadingIcon = {
                Icon(Icons.AutoMirrored.Default.Input, null)
            }
        )

        DropdownMenuItem(text = {
            Text(stringResource(id = R.string.export_config))
        }, onClick = {
            onDismissRequest()
            onExportAll()
        }, leadingIcon = {
            Icon(Icons.Default.Output, null)
        })
    }
}