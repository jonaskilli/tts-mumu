package com.github.jing332.script.runtime.console

import android.util.Log // 👈 使用原生 Log
import com.github.jing332.common.LogEntry
import com.github.jing332.common.LogLevel

class Console(val source: LogSource = LogSource.PLUGIN) : LogListenerManager, Writeable {
    enum class LogSource {
        PLUGIN,         // 插件日志
        SPEECH_RULE     // 朗读规则日志
    }
    
    companion object {
        private const val TAG = "JS-Console"
        
        // 全局插件日志监听器，由 app 模块设置
        var globalPluginLogListener: ((LogEntry) -> Unit)? = null
        
        // 全局朗读规则日志监听器，由 app 模块设置
        var globalSpeechRuleLogListener: ((LogEntry) -> Unit)? = null
    }

    private val listeners = mutableListOf<LogListener>()

    @Synchronized
    override fun addLogListener(listener: LogListener) {
        listeners.add(listener)
    }

    @Synchronized
    override fun removeLogListener(listener: LogListener) {
        listeners.remove(listener)
    }

    override fun write(@LogLevel level: Int, str: String) {
        // 👈 使用原生 Log.i，绕过损坏的 Logback 框架
        Log.i(TAG, "[$source] $str")
        
        // 根据来源标记日志类型
        val isPluginLog = source == LogSource.PLUGIN
        val isSpeechRuleLog = source == LogSource.SPEECH_RULE
        val prefix = if (isPluginLog) "[Plugin] " else if (isSpeechRuleLog) "[SpeechRule] " else ""
        
        val logEntry = LogEntry(
            level = level,
            message = "$prefix$str",
            isPluginLog = isPluginLog,
            isSpeechRuleLog = isSpeechRuleLog
        )
        
        // 通知对应的全局监听器
        if (isPluginLog) {
            globalPluginLogListener?.invoke(logEntry)
        } else if (isSpeechRuleLog) {
            globalSpeechRuleLogListener?.invoke(logEntry)
        }
        
        listeners.forEach {
            it.onNewLog(logEntry)
        }
    }

    fun println(str: String?) = write(LogLevel.INFO, str ?: "null")
    fun debug(str: String?) = write(LogLevel.DEBUG, str ?: "null")
    fun info(str: String?) = write(LogLevel.INFO, str ?: "null")
    fun warn(str: String?) = write(LogLevel.WARN, str ?: "null")
    fun error(str: String?) = write(LogLevel.ERROR, str ?: "null")
}
