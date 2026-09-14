package com.github.jing332.tts_server_android

import com.github.jing332.common.LogEntry
import com.github.jing332.common.LogLevel
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean

object SysttsLogger {
    private val listeners = CopyOnWriteArraySet<LogListener>()

    // 降级兜底「继承日志」用（用户 09-14 定）：兜底原因不反查接口，直接继承上方已打出的
    // 原因日志——在此维护最近 W/E 主行环形缓冲；朗读规则/插件 console 的 W/E 经
    // Console.extraListeners 一并汇入（hookConsole 幂等挂钩，不抢 UI 的单值全局监听）
    private const val REASON_CAP = 32
    private val recentReasons = ArrayDeque<LogEntry>(REASON_CAP)
    private val consoleHooked = AtomicBoolean(false)

    fun log(entry: LogEntry) {
        // 超时看门狗监测点：所有进入日志界面的日志都经过这里，
        // 在此通知看门狗（无锁、非阻塞），用于检测"超时后卡死"并自动重启。
        // 仅 WARN 级别（黄色）且含"超时"二字才触发计时，精确匹配真正的超时日志。
        TtsTimeoutWatchdog.onLog(entry.level, entry.message)
        recordReason(entry)
        listeners.forEach { it.log(entry) }
    }

    /** 服务启动时挂一次（幂等）：朗读规则/插件 console 日志也进原因缓冲 */
    fun hookConsole() {
        if (consoleHooked.compareAndSet(false, true)) {
            com.github.jing332.script.runtime.console.Console.extraListeners.add { recordReason(it) }
        }
    }

    private fun recordReason(entry: LogEntry) {
        if (entry.level != LogLevel.WARN && entry.level != LogLevel.ERROR) return
        if (entry.indent != 0) return // 子行从属于主行，不作为独立原因
        synchronized(recentReasons) {
            if (recentReasons.size >= REASON_CAP) recentReasons.removeFirst()
            recentReasons.addLast(entry)
        }
    }

    /** 最近一条 W/E 主行（朗读规则 console 优先，其次插件，再次主流程），无则 null；
     *  返回纯文本（剥离 HTML 标签、压空白、截 40 字），供兜底子行拼接 */
    fun lastWarnReason(): String? {
        val e = synchronized(recentReasons) {
            recentReasons.lastOrNull { it.isSpeechRuleLog }
                ?: recentReasons.lastOrNull { it.isPluginLog }
                ?: recentReasons.lastOrNull()
        } ?: return null
        val plain = e.message
            .removePrefix("[SpeechRule] ").removePrefix("[Plugin] ")
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("\\s+"), " ").trim()
        if (plain.isEmpty()) return null
        return if (plain.length > 40) plain.take(40) + "…" else plain
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
