package com.github.jing332.tts_server_android

import androidx.annotation.Keep
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import cn.hutool.core.date.LocalDateTimeUtil
import com.github.jing332.common.LogEntry
import com.github.jing332.common.toLogLevel
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import java.time.format.DateTimeFormatter
import java.util.TimeZone

@Keep
class SysttsFilter : Filter<ILoggingEvent>() {
    companion object {
        const val TAG = "SysttsFilter"
        const val ACTION_ON_LOG = "SystemFilter.SYSTTS_ON_LOG"
        private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        
        // 插件相关的 logger 名称列表
        private val PLUGIN_LOGGER_NAMES = listOf(
            "TtsPluginUiEngineV2",
            "TtsPluginEngineV2",
            "PluginTtsProvider",
            "PluginPreviewActivity",
            "PluginEditViewModel",
            "PluginLoginActivity",
            "JsBridgeInputStream"
        )

        // 系统 TTS 内部模块的 logger 名称（非插件，归入系统日志，默认显示在日志栏）
        private val SYSTTS_INTERNAL_LOGGER_NAMES = listOf(
            "SpeakerLoudnessManager"
        )
    }

    override fun decide(event: ILoggingEvent): FilterReply {
        val isPluginLog = PLUGIN_LOGGER_NAMES.any { event.loggerName.contains(it) }
        val isSysttsInternal = SYSTTS_INTERNAL_LOGGER_NAMES.any { event.loggerName.contains(it) }

        return if (event.loggerName == SystemTtsService.TAG || isPluginLog || isSysttsInternal) {
            SysttsLogger.log(
                LogEntry(
                    level = event.level.toString().toLogLevel(),
                    time = LocalDateTimeUtil.of(event.timeStamp, TimeZone.getDefault())
                        .format(dateFormatter),
                    message = event.message,
                    isPluginLog = isPluginLog
                )
            )

            FilterReply.ACCEPT
        } else FilterReply.DENY
    }
}