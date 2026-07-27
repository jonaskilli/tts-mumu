package com.github.jing332.tts_server_android.compose.systts

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drake.net.utils.withMain
import com.github.jing332.common.LogEntry
import com.github.jing332.common.LogLevel
import com.github.jing332.common.toLogLevel
import com.github.jing332.common.utils.runOnUI
import com.github.jing332.script.runtime.console.Console
import com.github.jing332.tts_server_android.SysttsLogger
import com.github.jing332.tts_server_android.constant.AppConst
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter

class TtsLogViewModel : ViewModel() {
    companion object {
        const val TAG = "TtsLogViewModel"

        // 日志总上限，达到后自动清空
        const val MAX_LOGS_BEFORE_CLEAR = 500000

        // 支持的日志级别
        val LOG_LEVELS = listOf(
            LogLevel.ERROR,
            LogLevel.WARN,
            LogLevel.INFO,
            LogLevel.SUCCESS,
            LogLevel.DEBUG,
            LogLevel.TRACE
        )

        // 修改点：路径从 files/log 指向 cache/log
        // AppConst.externalFilesDir 指向 .../files，parentFile 指向 .../包名，再 resolve cache 即为 cache 目录
        val file = File(AppConst.externalFilesDir.parentFile, "cache/log/system_tts.log")
    }

    val logs = mutableStateListOf<LogEntry>()
    val searchQuery = mutableStateOf("")
    
    // 日志级别筛选（存储选中的日志级别 Int 值）
    val selectedLevels = mutableStateListOf<Int>()
    val showFilterDialog = mutableStateOf(false)
    
    // 调试模式开关 - 显示/隐藏插件日志（默认隐藏，用户手动开启）
    val showPluginLogs = mutableStateOf(false)

    // 调试模式开关 - 显示/隐藏朗读规则日志（默认隐藏，用户手动开启）
    val showSpeechRuleLogs = mutableStateOf(false)

    // 实时滚动开关 - 勾选后新日志自动滚动到底部（默认不勾选）
    val autoScrollToBottom = mutableStateOf(false)
    
    val filteredLogs: List<LogEntry>
        get() {
            var filtered = logs.toList()
            
            // 按日志级别筛选
            if (selectedLevels.isNotEmpty()) {
                filtered = filtered.filter { it.level in selectedLevels }
            }
            
            // 按搜索词筛选
            val query = searchQuery.value.trim()
            if (query.isNotEmpty()) {
                filtered = filtered.filter { log ->
                    log.message.contains(query, ignoreCase = true) ||
                    log.time.contains(query, ignoreCase = true)
                }
            }
            
            // 调试模式：控制是否显示插件日志
            if (!showPluginLogs.value) {
                filtered = filtered.filter { !it.isPluginLog }
            }
            
            // 调试模式：控制是否显示朗读规则日志
            if (!showSpeechRuleLogs.value) {
                filtered = filtered.filter { !it.isSpeechRuleLog }
            }
            
            return filtered
        }
    
    fun toggleLevel(level: Int) {
        if (level in selectedLevels) {
            selectedLevels.remove(level)
        } else {
            selectedLevels.add(level)
        }
    }
    
    fun clearFilter() {
        selectedLevels.clear()
    }

    fun clear() {
        logs.clear()
        runCatching {
            FileWriter(file, false).use { it.write(CharArray(0)) }
        }.onFailure {
            logs.add(LogEntry(level = LogLevel.ERROR, message = it.stackTraceToString()))
            Log.e(TAG, "clear: ", it) 
        }
    }


    private fun toLogEntry(line: String): LogEntry {
        return line.split(" | ").let {
            val time = it[0]
            val level = it[1]
            val msg = it[2]
            LogEntry(
                level = level.toLogLevel(), time = time, message = msg
            )
        }
    }

    fun logDir(): String {
        return file.absolutePath
    }

    init {
        try {
            viewModelScope.launch(Dispatchers.IO) {
                pull()

                // 统一的日志添加函数，满5000条自动清空
                fun addLog(entry: LogEntry) {
                    runOnUI {
                        // 达到上限时自动清空日志
                        if (logs.size >= MAX_LOGS_BEFORE_CLEAR) {
                            logs.clear()
                            logs.add(LogEntry(
                                level = LogLevel.WARN,
                                message = "日志达到上限，已自动清空"
                            ))
                        }
                        logs.add(entry)
                    }
                }

                SysttsLogger.register({ log ->
                    addLog(log)
                })

                // 注册插件日志监听器
                Console.globalPluginLogListener = { logEntry ->
                    addLog(logEntry)
                }

                // 注册朗读规则日志监听器
                Console.globalSpeechRuleLogListener = { logEntry ->
                    Log.d(TAG, "globalSpeechRuleLogListener: ${logEntry.message}")
                    addLog(logEntry)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "init: ", e)
        }
    }

    fun add(line: String) {
        try {
            val logEntry = toLogEntry(line)
            logs.add(logEntry)
        } catch (e: Exception) {
            Log.e(TAG, "add: ", e)
        }
    }

    @Suppress("DEPRECATION")
    suspend fun pull() {
        runCatching {
            if (file.exists()) {
                // 最多读取最近 1500 行
                file.readLines().takeLast(1500).apply {
                    withMain {
                        forEach { add(it) }
                    }
                }
            }
        }.onFailure {
            logs.add(LogEntry(level = LogLevel.ERROR, message = it.stackTraceToString()))
            Log.e(TAG, "pull: ", it) 
        }

    }
}
