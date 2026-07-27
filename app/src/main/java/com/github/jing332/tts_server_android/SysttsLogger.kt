package com.github.jing332.tts_server_android

import com.github.jing332.common.LogEntry
import java.util.concurrent.CopyOnWriteArraySet

object SysttsLogger {
    private val listeners = CopyOnWriteArraySet<LogListener>()
    fun log(entry: LogEntry) {
        // 超时看门狗监测点：所有进入日志界面的日志都经过这里，
        // 在此通知看门狗（无锁、非阻塞），用于检测"超时后卡死"并自动重启。
        // 仅 WARN 级别（黄色）且含"超时"二字才触发计时，精确匹配真正的超时日志。
        TtsTimeoutWatchdog.onLog(entry.level, entry.message)
        listeners.forEach { it.log(entry) }
    }

    fun register(listener: LogListener) {
        listeners.add(listener)
    }

    fun unregister(listener: LogListener) {
        listeners.remove(listener)
    }

    fun interface LogListener {
        fun log(entry: LogEntry)
    }
}
