package com.github.jing332.tts_server_android.conf

object SysTtsConfig {
    var isInAppPlayAudio: Boolean
        get() = SystemTtsConfig.isInternalPlayerEnabled.value
        set(value) {
            SystemTtsConfig.isInternalPlayerEnabled.value = value
        }

    var inAppPlaySpeed: Float
        get() = SystemTtsConfig.inAppPlaySpeed.value
        set(value) {
            SystemTtsConfig.inAppPlaySpeed.value = value
        }

    var inAppPlayVolume: Float
        get() = SystemTtsConfig.inAppPlayVolume.value
        set(value) {
            SystemTtsConfig.inAppPlayVolume.value = value
        }

    var inAppPlayPitch: Float
        get() = SystemTtsConfig.inAppPlayPitch.value
        set(value) {
            SystemTtsConfig.inAppPlayPitch.value = value
        }

    var audioParamsSpeed: Float
        get() = SystemTtsConfig.audioParamsSpeed.value
        set(value) {
            SystemTtsConfig.audioParamsSpeed.value = value
        }

    var audioParamsPitch: Float
        get() = SystemTtsConfig.audioParamsPitch.value
        set(value) {
            SystemTtsConfig.audioParamsPitch.value = value
        }

    var audioParamsVolume: Float
        get() = SystemTtsConfig.audioParamsVolume.value
        set(value) {
            SystemTtsConfig.audioParamsVolume.value = value
        }

    var isBgmShuffleEnabled: Boolean
        get() = SystemTtsConfig.isBgmShuffleEnabled.value
        set(value) {
            SystemTtsConfig.isBgmShuffleEnabled.value = value
        }

    var isMultiVoiceEnabled: Boolean
        get() = SystemTtsConfig.isMultiVoiceEnabled.value
        set(value) {
            SystemTtsConfig.isMultiVoiceEnabled.value = value
        }

    var isWakeLockEnabled: Boolean
        get() = SystemTtsConfig.isWakeLockEnabled.value
        set(value) {
            SystemTtsConfig.isWakeLockEnabled.value = value
        }

    var isForegroundServiceEnabled: Boolean
        get() = SystemTtsConfig.isForegroundServiceEnabled.value
        set(value) {
            SystemTtsConfig.isForegroundServiceEnabled.value = value
        }

    var isReplaceEnabled: Boolean
        get() = SystemTtsConfig.isReplaceEnabled.value
        set(value) {
            SystemTtsConfig.isReplaceEnabled.value = value
        }

    var isSplitEnabled: Boolean
        get() = SystemTtsConfig.isSplitEnabled.value
        set(value) {
            SystemTtsConfig.isSplitEnabled.value = value
        }

    var requestTimeout: Long
        get() = SystemTtsConfig.requestTimeout.value.toLong()
        set(value) {
            SystemTtsConfig.requestTimeout.value = value.toInt()
        }

    var maxRetryCount: Int
        get() = SystemTtsConfig.maxRetryCount.value
        set(value) {
            SystemTtsConfig.maxRetryCount.value = value
        }

    /** 重试时在文本末尾追加的字符（次数 = 当前重试次数），默认空格 */
    var retryAppendText: String
        get() = SystemTtsConfig.retryAppendText.value
        set(value) {
            SystemTtsConfig.retryAppendText.value = value
        }

    var standbyTriggeredRetryIndex: Int
        get() = SystemTtsConfig.standbyTriggeredRetryIndex.value
        set(value) {
            SystemTtsConfig.standbyTriggeredRetryIndex.value = value
        }

    var maxEmptyAudioRetryCount: Int
        get() = SystemTtsConfig.maxEmptyAudioRetryCount.value
        set(value) {
            SystemTtsConfig.maxEmptyAudioRetryCount.value = value
        }

    var isSkipSilentAudio: Boolean
        get() = SystemTtsConfig.isSilenceSkipAudio.value
        set(value) {
            SystemTtsConfig.isSilenceSkipAudio.value = value
        }

    // ========== 响度均衡配置 ==========

    var loudnessMaxGain: Float
        get() = SystemTtsConfig.loudnessMaxGain.value
        set(value) {
            SystemTtsConfig.loudnessMaxGain.value = value
        }

    var isStreamPlayModeEnabled: Boolean
        get() = SystemTtsConfig.isStreamPlayModeEnabled.value
        set(value) {
            SystemTtsConfig.isStreamPlayModeEnabled.value = value
        }

    // ========== 后台保活配置 ==========

    var isKeepAliveEnabled: Boolean
        get() = SystemTtsConfig.isKeepAliveEnabled.value
        set(value) {
            SystemTtsConfig.isKeepAliveEnabled.value = value
        }

    var isAccessibilityKeepAliveEnabled: Boolean
        get() = SystemTtsConfig.isAccessibilityKeepAliveEnabled.value
        set(value) {
            SystemTtsConfig.isAccessibilityKeepAliveEnabled.value = value
        }

    var isNotificationKeepAliveEnabled: Boolean
        get() = SystemTtsConfig.isNotificationKeepAliveEnabled.value
        set(value) {
            SystemTtsConfig.isNotificationKeepAliveEnabled.value = value
        }

    var isAlarmKeepAliveEnabled: Boolean
        get() = SystemTtsConfig.isAlarmKeepAliveEnabled.value
        set(value) {
            SystemTtsConfig.isAlarmKeepAliveEnabled.value = value
        }

    var isAutoStartEnabled: Boolean
        get() = SystemTtsConfig.isAutoStartEnabled.value
        set(value) {
            SystemTtsConfig.isAutoStartEnabled.value = value
        }

    var restartOnMaxRetryMode: Int
        get() = SystemTtsConfig.restartOnMaxRetryMode.value
        set(value) {
            SystemTtsConfig.restartOnMaxRetryMode.value = value
        }

    /** 超时看门狗阈值（秒）：0=不重启，其他=相应秒数后重启 */
    var timeoutWatchdogSeconds: Int
        get() = SystemTtsConfig.timeoutWatchdogSeconds.value
        set(value) {
            SystemTtsConfig.timeoutWatchdogSeconds.value = value
        }
}