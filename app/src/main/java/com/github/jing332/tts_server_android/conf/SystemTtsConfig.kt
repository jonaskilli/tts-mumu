package com.github.jing332.tts_server_android.conf

import android.content.Context
import com.funny.data_saver.core.DataSaverPreferences
import com.funny.data_saver.core.mutableDataSaverStateOf
import com.github.jing332.tts_server_android.app

object SystemTtsConfig {

    private val dataSaverPref by lazy { DataSaverPreferences((app as Context).getSharedPreferences("systts", 0)) }

    val isInternalPlayerEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isInAppPlayAudio",
        initialValue = false
    )

    val inAppPlaySpeed = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "inAppPlaySpeed",
        initialValue = 1f
    )

    val inAppPlayVolume = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "inAppPlayVolume",
        initialValue = 1f
    )

    val inAppPlayPitch = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "inAppPlayPitch",
        initialValue = 1f
    )

    val audioParamsSpeed = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "audioParamsSpeed",
        initialValue = 1f
    )

    val audioParamsPitch = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "audioParamsPitch",
        initialValue = 1f
    )

    val audioParamsVolume = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "audioParamsVolume",
        initialValue = 1f
    )

    val isBgmShuffleEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isBgmShuffleEnabled",
        initialValue = false
    )

    val isMultiVoiceEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isMultiVoiceEnabled",
        initialValue = false
    )

    val isVoiceMultipleEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isVoiceMultipleEnabled",
        initialValue = false
    )

    val isGroupMultipleEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isGroupMultipleEnabled",
        initialValue = false
    )

    val isWakeLockEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isWakeLockEnabled",
        initialValue = false
    )

    val isForegroundServiceEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isForegroundServiceEnabled",
        initialValue = false
    )

    val isReplaceEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isReplaceEnabled",
        initialValue = false
    )

    val isSplitEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isSplitEnabled",
        initialValue = false
    )

    val requestTimeout = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "requestTimeout",
        initialValue = 60000
    )

    val maxRetryCount = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "maxRetryCount",
        initialValue = 3
    )

    /** 重试时在文本末尾追加的字符（次数 = 当前重试次数），默认空格 */
    val retryAppendText = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "retryAppendText",
        initialValue = " "
    )

    val standbyTriggeredRetryIndex = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "standbyTriggeredRetryIndex",
        initialValue = 3
    )

    val maxEmptyAudioRetryCount = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "maxEmptyAudioRetryCount",
        initialValue = 1
    )

    val isSkipSilentText = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isSkipSilentText",
        initialValue = true
    )

    val isStreamPlayModeEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isStreamPlayModeEnabled",
        initialValue = false
    )

    val isExoDecoderEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isExoDecoderEnabled",
        initialValue = true
    )

    val isSilenceSkipAudio = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isSilenceSkipAudio",
        initialValue = false
    )

    /** 段间停顿毫秒数，0=关闭；插入的是直写播放通道的静音PCM，固定墙钟时长，不受语速影响 */
    val segmentPauseMs = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "segmentPauseMs",
        initialValue = 0
    )

    // ========== 响度均衡配置 ==========

    /** 响度均衡总开关，默认开启 */
    val isLoudnessEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isLoudnessEnabled",
        initialValue = true
    )

    val loudnessMaxGain = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "loudnessMaxGain",
        initialValue = 1.35f
    )

    // ========== 后台保活配置 ==========

    /** 启用进程保活服务 */
    val isKeepAliveEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isKeepAliveEnabled",
        initialValue = false
    )

    /** 启用无障碍保活 */
    val isAccessibilityKeepAliveEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isAccessibilityKeepAliveEnabled",
        initialValue = false
    )

    /** 启用通知监听保活 */
    val isNotificationKeepAliveEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isNotificationKeepAliveEnabled",
        initialValue = false
    )

    /** 启用定时唤醒保活 */
    val isAlarmKeepAliveEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isAlarmKeepAliveEnabled",
        initialValue = false
    )

    /** 启用自启动 */
    val isAutoStartEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isAutoStartEnabled",
        initialValue = false
    )

    /** 启用网络连接保活（对转发器有用） */
    val isNetworkKeepAliveEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isNetworkKeepAliveEnabled",
        initialValue = false
    )

    /** 启用像素保活（1像素悬浮窗） */
    val isPixelKeepAliveEnabled = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "isPixelKeepAliveEnabled",
        initialValue = false
    )

    /** 超重试后自动重启应用 (0=关闭, 1=直接重启, 2=生成空音频后重启) */
    val restartOnMaxRetryMode = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "restartOnMaxRetryMode",
        initialValue = 0
    )

    /** 超时看门狗阈值（秒）：超时日志出现后N秒无新日志则重启APP，0=不重启 */
    val timeoutWatchdogSeconds = mutableDataSaverStateOf(
        dataSaverInterface = dataSaverPref,
        key = "timeoutWatchdogSeconds",
        initialValue = 20
    )
}
