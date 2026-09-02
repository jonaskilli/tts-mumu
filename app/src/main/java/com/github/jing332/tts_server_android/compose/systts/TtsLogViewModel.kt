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

        // 内存日志滑动窗口上限：旧值50万条，插件日志每条可达数KB，长会话直接吃满256MB堆
        // （装机实测 OOM：画列表项时连80字节都分配失败）。完整历史仍由磁盘
        // cache/log/system_tts.log 承载，内存只保最近段。超限后按批裁最旧，不清空全部
        const val MAX_LOGS = 20000

        // 裁剪批量：让 size 长到 MAX+PRUNE_BATCH 再一次裁回 MAX，均摊掉逐条删头部的数组搬移开销
        const val PRUNE_BATCH = 1000

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
            // 单遍过滤：旧实现 toList 后最多再 filter 三次，高频日志下每次重组都全量复制三份列表
            val levels = selectedLevels
            val showPlugin = showPluginLogs.value
            val showRule = showSpeechRuleLogs.value
            return logs.filter {
                (levels.isEmpty() || it.level in levels) &&
                        (showPlugin || !it.isPluginLog) &&
                        (showRule || !it.isSpeechRuleLog)
            }
            // 注：搜索词不做过滤——搜索是定位(跳转+高亮)，由 TtsLogScreen/LogScreen 处理，
            // 保留完整列表便于查看匹配项的前后文
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

                // 统一的日志添加函数：滑动窗口，超限裁掉最旧的
                fun addLog(entry: LogEntry) {
                    runOnUI {
                        logs.add(entry)
                        val overflow = logs.size - MAX_LOGS
                        if (overflow >= PRUNE_BATCH) {
                            repeat(overflow) { logs.removeAt(0) }
                        }
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
                // 最多读取最近 1500 行，解析后批量添加，避免逐条 add 触发多次重组
                val entries = file.readLines().takeLast(1500).mapNotNull { line ->
                    runCatching { toLogEntry(line) }.getOrNull()
                }
                withMain {
                    logs.addAll(entries)
                }
            }
        }.onFailure {
            logs.add(LogEntry(level = LogLevel.ERROR, message = it.stackTraceToString()))
            Log.e(TAG, "pull: ", it)
        }

    }
}
