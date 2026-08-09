package com.github.jing332.tts_server_android.compose.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.StackedLineChart
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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

@Composable
internal fun ColumnScope.SysttsSettingsScreen(modifier: Modifier = Modifier) {
    DividerPreference {
        Text(stringResource(id = R.string.system_tts))
    }

    var loudnessEnabled by remember { SystemTtsConfig.isLoudnessEnabled }
    SwitchPreference(
        title = { Text(stringResource(R.string.loudness_balance)) },
        subTitle = { Text(stringResource(R.string.loudness_balance_summary)) },
        checked = loudnessEnabled,
        onCheckedChange = { loudnessEnabled = it },
        icon = { Icon(Icons.Default.Audiotrack, null) }
    )

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
    BasePreferenceWidget(
        onClick = { showResetLoudnessDialog = true },
        icon = { Icon(Icons.Default.Audiotrack, null) },
        title = { Text(stringResource(R.string.loudness_reset)) },
        subTitle = { Text(stringResource(R.string.loudness_reset_summary, learnedCount)) }
    )

    var silenceAudio by remember { SystemTtsConfig.isSilenceSkipAudio }
    SwitchPreference(
        title = { Text(stringResource(R.string.silent_audio)) },
        subTitle = { Text(stringResource(R.string.silent_audio_summary)) },
        checked = silenceAudio,
        onCheckedChange = {
            silenceAudio = it
        },
        icon = { Icon(Icons.Default.StackedLineChart, null) }
    )

    var streamPlay by remember { SystemTtsConfig.isStreamPlayModeEnabled }
    SwitchPreference(
        title = { Text(stringResource(id = R.string.stream_audio_mode)) },
        subTitle = { Text(stringResource(id = R.string.stream_audio_mode_summary)) },
        checked = streamPlay,
        onCheckedChange = { streamPlay = it },
        icon = { Icon(Icons.Default.Waves, null) }
    )

    var foregroundService by remember { SystemTtsConfig.isForegroundServiceEnabled }
    SwitchPreference(
        title = { Text(stringResource(id = R.string.foreground_service_and_notification)) },
        subTitle = { Text(stringResource(id = R.string.foreground_service_and_notification_summary)) },
        checked = foregroundService,
        onCheckedChange = { foregroundService = it },
        icon = { Icon(Icons.Default.NotificationsNone, null) }
    )

    var wakeLock by remember { SystemTtsConfig.isWakeLockEnabled }
    SwitchPreference(
        title = { Text(stringResource(id = R.string.wake_lock)) },
        subTitle = { Text(stringResource(id = R.string.wake_lock_summary)) },
        checked = wakeLock,
        onCheckedChange = { wakeLock = it },
        icon = { Icon(Icons.Default.Lock, null) }
    )

    var maxRetry by remember { SystemTtsConfig.maxRetryCount }
    val maxRetryValue =
        if (maxRetry == 0) stringResource(id = R.string.no_retries) else maxRetry.toString()
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
    BasePreferenceWidget(
        onClick = { showRetryAppendDialog = true },
        icon = { Icon(Icons.Default.Repeat, null) },
        title = { Text(stringResource(id = R.string.retry_append_text)) },
        subTitle = {
            Text(
                if (retryAppendText.isEmpty())
                    stringResource(id = R.string.retry_append_text_off)
                else
                    stringResource(id = R.string.retry_append_text_on, retryAppendText)
            )
        }
    ) {
        Text(if (retryAppendText.isEmpty()) "OFF" else retryAppendText)
    }

    var restartOnMaxRetryMode by remember { SystemTtsConfig.restartOnMaxRetryMode }
    var restartMenuExpanded by remember { mutableStateOf(false) }
    DropdownPreference(
        expanded = restartMenuExpanded,
        onExpandedChange = { restartMenuExpanded = it },
        icon = { Icon(Icons.Default.Repeat, null) },
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

    var standbyTriggeredIndex by remember { SystemTtsConfig.standbyTriggeredRetryIndex }
    val standbyTriggeredIndexValue = standbyTriggeredIndex.toString()
    SliderPreference(
        title = { Text(stringResource(id = R.string.systts_standby_triggered_retry_index)) },
        subTitle = { Text(stringResource(id = R.string.systts_standby_triggered_retry_index_summary)) },
        value = standbyTriggeredIndex.toFloat(),
        onValueChange = { standbyTriggeredIndex = it.fastRoundToInt() },
        valueRange = 0f..10f,
        steps = 9,
        icon = { Icon(Icons.Default.Repeat, null) },
        label = standbyTriggeredIndexValue
    )


    var requestTimeout by remember { SystemTtsConfig.requestTimeout }
    val requestTimeoutValue = "${requestTimeout / 1000}s"
    SliderPreference(
        title = { Text(stringResource(id = R.string.request_timeout)) },
        subTitle = { Text(stringResource(id = R.string.request_timeout_summary)) },
        value = (requestTimeout / 1000).toFloat(),
        onValueChange = { requestTimeout = it.toInt() * 1000 },
        valueRange = 1f..300f,
        icon = { Icon(Icons.Default.AccessTime, null) },
        label = requestTimeoutValue
    )

    var watchdogSeconds by remember { SystemTtsConfig.timeoutWatchdogSeconds }
    val watchdogValue = if (watchdogSeconds == 0) stringResource(id = R.string.disabled) else "${watchdogSeconds}s"
    SliderPreference(
        title = { Text(stringResource(id = R.string.timeout_watchdog)) },
        subTitle = { Text(stringResource(id = R.string.timeout_watchdog_summary)) },
        value = watchdogSeconds.toFloat(),
        onValueChange = { watchdogSeconds = it.toInt() },
        valueRange = 0f..120f,
        icon = { Icon(Icons.Default.AccessTime, null) },
        label = watchdogValue
    )

    DividerPreference {
        Text(stringResource(id = R.string.systts_interface_preference))
    }

    var limitTagLen by remember { AppConfig.limitTagLength }
    val limitTagLenString =
        if (limitTagLen == 0) stringResource(id = R.string.unlimited) else limitTagLen.toString()
    SliderPreference(
        title = { Text(stringResource(id = R.string.limit_tag_length)) },
        subTitle = { Text(stringResource(id = R.string.limit_tag_length_summary)) },
        value = limitTagLen.toFloat(),
        onValueChange = { limitTagLen = it.toInt() },
        valueRange = 0f..50f,
        icon = { Icon(Icons.Default.Tag, null) },
        label = limitTagLenString
    )

    var limitNameLen by remember { AppConfig.limitNameLength }
    val limitNameLenString =
        if (limitNameLen == 0) stringResource(id = R.string.unlimited) else limitNameLen.toString()
    SliderPreference(
        title = { Text(stringResource(id = R.string.limit_name_length)) },
        subTitle = { Text(stringResource(id = R.string.limit_name_length_summary)) },
        value = limitNameLen.toFloat(),
        onValueChange = { limitNameLen = it.toInt() },
        valueRange = 0f..50f,
        icon = { Icon(Icons.Default.TextFields, null) },
        label = limitNameLenString
    )

    var wrapButton by remember { AppConfig.isSwapListenAndEditButton }
    SwitchPreference(
        title = { Text(stringResource(id = R.string.pref_swap_listen_and_edit_button)) },
        subTitle = {},
        checked = wrapButton,
        onCheckedChange = { wrapButton = it },
        icon = {
            Icon(Icons.Default.Headset, contentDescription = null)
        }
    )

    var targetMultiple by remember { SystemTtsConfig.isVoiceMultipleEnabled }
    SwitchPreference(
        title = { Text(stringResource(id = R.string.voice_multiple_option)) },
        subTitle = { Text(stringResource(id = R.string.voice_multiple_summary)) },
        checked = targetMultiple,
        onCheckedChange = { targetMultiple = it },
        icon = {
            Icon(Icons.Default.SelectAll, contentDescription = null)
        }
    )

    var groupMultiple by remember { SystemTtsConfig.isGroupMultipleEnabled }
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
