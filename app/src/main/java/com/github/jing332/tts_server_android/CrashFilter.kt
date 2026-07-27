package com.github.jing332.tts_server_android

import androidx.annotation.Keep
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply

@Keep
class CrashFilter : Filter<ILoggingEvent>() {
    override fun decide(event: ILoggingEvent): FilterReply {
        return if (event.loggerName == CrashHandler.TAG) FilterReply.ACCEPT else FilterReply.DENY
    }
}