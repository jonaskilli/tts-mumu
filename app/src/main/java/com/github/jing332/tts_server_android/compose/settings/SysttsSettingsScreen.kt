package com.github.jing332.tts_server_android.compose.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.StackedLineChart
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import com.github.jing332.compose.widgets.TextFieldDialog
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.conf.AppConfig
import com.github.jing332.tts_server_android.conf.SystemTtsConfig
import com.github.jing332.tts.loudness.SpeakerLoudnessManager

/**
 * 本文件负责的两个区（10-05 用户令：稳定性不再是"页面内折叠"，改为设置页上的一个入口行，
 * 点开是一个独立子页）——故按 [part] 二选一渲染：
 * - [Loudness] 朗读与播放：留在设置主页，带分区标题
 * - [Stability] 稳定性：子页形态，标题由子页顶栏承担，卡内不再出标题也不折叠
 */
internal enum class SysttsSettingsPart { Loudness, Stability }

@Composable
internal fun ColumnScope.SysttsSettingsScreen(
    search: SettingsSearch,
    part: SysttsSettingsPart = SysttsSettingsPart.Loudness,
) {
    if (part == SysttsSettingsPart.Loudness) {
    SettingsGroup(title = { Text("朗读与播放") }, show = !search.active()) {
    var loudnessEnabled by remember { SystemTtsConfig.isLoudnessEnabled }
    SettingItem(search, "音量平衡", "响度", "loudness", "平衡") {
        SwitchPreference(
            title = { Text(stringResource(R.string.loudness_balance)) },
            subTitle = { Text(stringResource(R.string.loudness_balance_summary)) },
            checked = loudnessEnabled,
            onCheckedChange = { loudnessEnabled = it },
            icon = { Icon(Icons.Default.Audiotrack, null) }
        )
    }

    var learnedCount by remember { mutableIntStateOf(SpeakerLoudnessManager.learnedSpeakerCount()) }
    var showResetLoudnessDialog by remember { mutableStateOf(false) }
    if (showResetLoudnessDialog) {
        AlertDialog(
            onDismissRequest = { showResetLoudnessDialog = false },
            title = { Text(stringResource(R.string.loudness_reset)) },
            text = { Text(stringResource(R.string.loudness_reset_confirm, learnedCount)) },
            confirmButton = {
                TextButton(onClick = {
                    SpeakerLoudnessManager.reset()
                    learnedCount = 0
                    showResetLoudnessDialog = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetLoudnessDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    SettingItem(search, "重置", "reset", "学习", "音量平衡", "响度") {
        BasePreferenceWidget(
            onClick = { showResetLoudnessDialog = true },
            icon = { Icon(Icons.Default.Audiotrack, null) },
            title = { Text(stringResource(R.string.loudness_reset)) },
            subTitle = { Text(stringResource(R.string.loudness_reset_summary, learnedCount)) }
        )
    }

    var silenceAudio by remember { SystemTtsConfig.isSilenceSkipAudio }
    SettingItem(search, "静音", "silence", "空音频", "跳过") {
        SwitchPreference(
            title = { Text(stringResource(R.string.silent_audio)) },
            subTitle = { Text(stringResource(R.string.silent_audio_summary)) },
            checked = silenceAudio,
            onCheckedChange = {
                silenceAudio = it
            },
            icon = { Icon(Icons.Default.StackedLineChart, null) }
        )
    }

    var segmentPause by remember { SystemTtsConfig.segmentPauseMs }
    val segmentPauseLabel = "${segmentPause}ms"
    SettingItem(search, "分段停顿", "停顿", "pause", "segment") {
        // 与其他滑杆设置统一：点卡片弹 AppDialog 居中对话框，即调即存（用户 09-08 方案A 定稿）
        SliderPreference(
            title = { Text(stringResource(id = R.string.segment_pause)) },
            subTitle = { Text(stringResource(id = R.string.segment_pause_summary)) },
            value = segmentPause.toFloat(),
            // LabelSlider 内部 Slider 是连续的(steps仅用于无障碍)，50ms步进靠这里吸附实现
            onValueChange = { segmentPause = (it / 50f).fastRoundToInt() * 50 },
            valueRange = 0f..1000f,
            steps = 19,
            buttonSteps = 50f,
            buttonLongSteps = 100f,
            icon = { Icon(Icons.Default.AccessTime, null) },
            label = segmentPauseLabel,
        )
    }

    var streamPlay by remember { SystemTtsConfig.isStreamPlayModeEnabled }
    SettingItem(search, "流式", "stream", "播放模式") {
        SwitchPreference(
            title = { Text(stringResource(id = R.string.stream_audio_mode)) },
            subTitle = { Text(stringResource(id = R.string.stream_audio_mode_summary)) },
            checked = streamPlay,
            onCheckedChange = { streamPlay = it },
            icon = { Icon(Icons.Default.Waves, null) }
        )
    }

    // 交换试听/编辑按钮位置（10-05 用户令：从「常用」挪回本区，并排在「多语音」之前）
    var wrapButton by remember { AppConfig.isSwapListenAndEditButton }
    SettingItem(search, "交换", "按钮", "button", "试听", "编辑") {
        SwitchPreference(
            title = { Text(stringResource(id = R.string.pref_swap_listen_and_edit_button)) },
            subTitle = {},
            checked = wrapButton,
            onCheckedChange = { wrapButton = it },
            icon = {
                Icon(Icons.Default.Headset, contentDescription = null)
            }
        )
    }

    var targetMultiple by remember { SystemTtsConfig.isVoiceMultipleEnabled }
    SettingItem(search, "多语音", "voice", "并行", "多角色") {
        SwitchPreference(
            title = { Text(stringResource(id = R.string.voice_multiple_option)) },
            subTitle = { Text(stringResource(id = R.string.voice_multiple_summary)) },
            checked = targetMultiple,
            onCheckedChange = { targetMultiple = it },
            icon = {
                Icon(Icons.Default.SelectAll, contentDescription = null)
            }
        )
    }

    var groupMultiple by remember { SystemTtsConfig.isGroupMultipleEnabled }
    SettingItem(search, "多组", "groups", "并行", "多角色") {
        SwitchPreference(
            title = { Text(stringResource(id = R.string.groups_multiple)) },
            subTitle = { Text(stringResource(id = R.string.groups_multiple_summary)) },
            checked = groupMultiple,
            onCheckedChange = { groupMultiple = it },
            icon = {
                Icon(Icons.Default.Groups, contentDescription = null)
            }
        )
    }

    // 心声 AI 判定（10-05 用户令：原「心声」区撤并入本区；同日再令排在本区末位）
    var aiEnabled by remember { SystemTtsConfig.isInnerThoughtAiEnabled }
    SettingItem(search, "心声", "ai", "心理活动", "inner", "内心") {
        SwitchPreference(
            title = { Text("启用心声 AI 判定") },
            subTitle = { Text("正则拿不准时调用 AI 判断") },
            checked = aiEnabled,
            onCheckedChange = { aiEnabled = it },
            icon = { Icon(Icons.Default.Psychology, null) }
        )
    }

    // 两个长度限制（10-05 用户令：自「其他」区迁回本区末尾——它们改的是列表页"标签/名称"的
    // 显示截断，属朗读与列表呈现，放「其他」不贴切）
    // 消费点：Item.kt 里对**显示**的标签/名字做截断（不改数据、不影响朗读匹配）
    var limitTagLen by remember { AppConfig.limitTagLength }
    val limitTagLenString =
        if (limitTagLen == 0) stringResource(id = R.string.unlimited) else limitTagLen.toString()
    SettingItem(search, "标签", "tag", "限制长度", "长度") {
        SliderPreference(
            title = { Text(stringResource(id = R.string.limit_tag_length)) },
            subTitle = { Text(stringResource(id = R.string.limit_tag_length_summary)) },
            value = limitTagLen.toFloat(),
            onValueChange = { limitTagLen = it.toInt() },
            valueRange = 0f..20f,
            // 整数吸附：拖动值按 1 吸附，与音频参数滑杆同款档位手感（10-06 用户反馈）
            step = 1f,
            icon = { Icon(Icons.Default.Tag, null) },
            label = limitTagLenString
        )
    }

    var limitNameLen by remember { AppConfig.limitNameLength }
    val limitNameLenString =
        if (limitNameLen == 0) stringResource(id = R.string.unlimited) else limitNameLen.toString()
    SettingItem(search, "名称", "name", "限制长度", "长度") {
        SliderPreference(
            title = { Text(stringResource(id = R.string.limit_name_length)) },
            subTitle = { Text(stringResource(id = R.string.limit_name_length_summary)) },
            value = limitNameLen.toFloat(),
            onValueChange = { limitNameLen = it.toInt() },
            // 上限 30（10-06 用户令：「阳光甜妹」这类格外长的名字 20 不够挑）；默认仍 20
            valueRange = 0f..30f,
            // 整数吸附：拖动值按 1 吸附（顺手；原 it.toInt() 向下取整导致值与滑块错半格）
            step = 1f,
            icon = { Icon(Icons.Default.TextFields, null) },
            label = limitNameLenString
        )
    }
    } // 朗读与播放区收尾（10-05 分区）
    } else {
    // 稳定性（子页形态，10-05 用户令：由"页面内折叠"改为独立子页；标题在子页顶栏，卡内不出标题）
    SettingsGroup(
        title = { Text("稳定性") },
        show = !search.active(),
        showHeader = false,
    ) {
    var maxRetry by remember { SystemTtsConfig.maxRetryCount }
    val maxRetryValue =
        if (maxRetry == 0) stringResource(id = R.string.no_retries) else maxRetry.toString()
    SettingItem(search, "最大重试", "重试", "retry", "次数") {
        SliderPreference(
            title = { Text(stringResource(id = R.string.max_retry_count)) },
            subTitle = { Text(stringResource(id = R.string.max_retry_count_summary)) },
            value = maxRetry.toFloat(),
            onValueChange = { maxRetry = it.fastRoundToInt() },
            valueRange = 0f..10f,
            steps = 9,
            icon = { Icon(Icons.Default.Repeat, null) },
            label = maxRetryValue,
        )
    }

    var retryAppendText by remember { SystemTtsConfig.retryAppendText }
    var showRetryAppendDialog by remember { mutableStateOf(false) }
    if (showRetryAppendDialog) {
        var text by remember { mutableStateOf(retryAppendText) }
        TextFieldDialog(
            title = stringResource(id = R.string.retry_append_text),
            text = text,
            onTextChange = { text = it },
            onDismissRequest = { showRetryAppendDialog = false },
            onConfirm = {
                retryAppendText = text
                showRetryAppendDialog = false
            }
        )
    }
    SettingItem(search, "重试附加", "retry append", "附加文本") {
        // 右侧不再重复第二个值（10-05 用户令）：本来副标题已写「未开启 / 已开启：xxx」，
        // 行尾又摆同一个值，同一信息两遍；去掉后本行按"可点进弹窗"自动带 ›
        BasePreferenceWidget(
            onClick = { showRetryAppendDialog = true },
            icon = { Icon(Icons.Default.EditNote, null) },
            title = { Text(stringResource(id = R.string.retry_append_text)) },
            subTitle = {
                Text(
                    if (retryAppendText.isEmpty())
                        stringResource(id = R.string.retry_append_text_off)
                    else
                        stringResource(id = R.string.retry_append_text_on, retryAppendText)
                )
            }
        )
    }

    var restartOnMaxRetryMode by remember { SystemTtsConfig.restartOnMaxRetryMode }
    var restartMenuExpanded by remember { mutableStateOf(false) }
    SettingItem(search, "重启", "restart", "最大重试后", "崩溃") {
        DropdownPreference(
            expanded = restartMenuExpanded,
            onExpandedChange = { restartMenuExpanded = it },
            icon = { Icon(Icons.Default.RestartAlt, null) },
            title = { Text(stringResource(id = R.string.restart_on_max_retry)) },
            subTitle = {
                Text(
                    when (restartOnMaxRetryMode) {
                        1 -> stringResource(id = R.string.restart_on_max_retry_direct)
                        2 -> stringResource(id = R.string.restart_on_max_retry_after_empty)
                        else -> stringResource(id = R.string.restart_on_max_retry_off)
                    }
                )
            },
            actions = {
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.restart_on_max_retry_off)) },
                    onClick = {
                        restartMenuExpanded = false
                        restartOnMaxRetryMode = 0
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.restart_on_max_retry_direct)) },
                    onClick = {
                        restartMenuExpanded = false
                        restartOnMaxRetryMode = 1
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.restart_on_max_retry_after_empty)) },
                    onClick = {
                        restartMenuExpanded = false
                        restartOnMaxRetryMode = 2
                    }
                )
            }
        )
    }

    var standbyTriggeredIndex by remember { SystemTtsConfig.standbyTriggeredRetryIndex }
    val standbyTriggeredIndexValue = standbyTriggeredIndex.toString()
    SettingItem(search, "待机", "standby", "重试序号", "序号") {
        SliderPreference(
            title = { Text(stringResource(id = R.string.systts_standby_triggered_retry_index)) },
            subTitle = { Text(stringResource(id = R.string.systts_standby_triggered_retry_index_summary)) },
            value = standbyTriggeredIndex.toFloat(),
            onValueChange = { standbyTriggeredIndex = it.fastRoundToInt() },
            valueRange = 0f..10f,
            steps = 9,
            icon = { Icon(Icons.Default.NightsStay, null) },
            label = standbyTriggeredIndexValue
        )
    }


    var requestTimeout by remember { SystemTtsConfig.requestTimeout }
    val requestTimeoutValue = "${requestTimeout / 1000}s"
    SettingItem(search, "请求超时", "超时", "timeout", "request") {
        SliderPreference(
            title = { Text(stringResource(id = R.string.request_timeout)) },
            subTitle = { Text(stringResource(id = R.string.request_timeout_summary)) },
            value = (requestTimeout / 1000).toFloat(),
            onValueChange = { requestTimeout = it.toInt() * 1000 },
            valueRange = 1f..300f,
            icon = { Icon(Icons.Default.AccessTime, null) },
            label = requestTimeoutValue
        )
    }

    var watchdogSeconds by remember { SystemTtsConfig.timeoutWatchdogSeconds }
    val watchdogValue = if (watchdogSeconds == 0) stringResource(id = R.string.disabled) else "${watchdogSeconds}s"
    SettingItem(search, "看门狗", "watchdog", "超时保护", "超时") {
        SliderPreference(
            title = { Text(stringResource(id = R.string.timeout_watchdog)) },
            subTitle = { Text(stringResource(id = R.string.timeout_watchdog_summary)) },
            value = watchdogSeconds.toFloat(),
            onValueChange = { watchdogSeconds = it.toInt() },
            valueRange = 0f..120f,
            icon = { Icon(Icons.Default.Timer, null) },
            label = watchdogValue
        )
    }

    } // 稳定性区收尾
    }
}
