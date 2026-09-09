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

        // 插件/朗读规则日志独立缓冲上限（用户 09-08：不再混入主列表刷屏/挤占窗口）。
        // 条数 500 + 字符总量熔断 30 万（双保险：单条可长达数KB~十几KB，纯条数上限挡不住堆积）
        const val AUX_MAX = 500

        // 独立缓冲裁剪批量
        const val AUX_PRUNE = 100

        // 独立缓冲字符总量熔断线（message 字符数合计；超限按最旧裁，UTF-16 下约 0.6MB/缓冲）
        const val AUX_CHAR_BUDGET = 300_000L

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

    // 插件/朗读规则日志独立缓冲（防刷屏/防OOM，09-08 保留）：始终记录、不挤占主列表窗口；
    // 勾选"插件日志/朗读规则日志"= 在主时间流中追加显示该缓冲（用户 09-09：恢复 09-08 改前的
    // 混排行为——看过程必须与"请求音频/获取成功"等结果流对照，"切换显示"把两者切断了）
    val pluginLogs = mutableStateListOf<LogEntry>()
    val speechRuleLogs = mutableStateListOf<LogEntry>()

    // 日志级别筛选（存储选中的日志级别 Int 值）
    val selectedLevels = mutableStateListOf<Int>()
    val showFilterDialog = mutableStateOf(false)

    // 调试模式开关 - 在主时间流中追加/隐藏插件日志（默认隐藏，用户手动开启）
    val showPluginLogs = mutableStateOf(false)

    // 调试模式开关 - 在主时间流中追加/隐藏朗读规则日志（默认隐藏，用户手动开启）
    val showSpeechRuleLogs = mutableStateOf(false)

    // 实时滚动开关 - 勾选后新日志自动滚动到底部（默认不勾选）
    val autoScrollToBottom = mutableStateOf(false)

    // 与系统日志同款时间戳格式：合并排序按字符串比较即可保持时序（等宽、字典序=时间序）
    private val auxTimeFormatter =
        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US)

    val filteredLogs: List<LogEntry>
        get() {
            // 勾选=追加混排（用户 09-09）：主列表与勾选的辅助缓冲按时间戳合并，
            // 过程（插件/规则）与结果（请求音频/获取成功）交错可见；都不勾=纯主列表
            val showPlugin = showPluginLogs.value
            val showRule = showSpeechRuleLogs.value
            val merged: List<LogEntry> = if (showPlugin || showRule) {
                val source = ArrayList<LogEntry>(logs.size + 8)
                source.addAll(logs)
                if (showPlugin) source.addAll(pluginLogs)
                if (showRule) source.addAll(speechRuleLogs)
                // 稳定排序：同毫秒内保持各缓冲内的到达顺序
                source.sortedBy { it.time }
            } else {
                logs
            }
            val levels = selectedLevels
            return merged.filter {
                levels.isEmpty() || it.level in levels
            }
            // 注：搜索词不做过滤——搜索是定位(跳转+高亮)，由 TtsLogScreen/LogScreen 处理，
            // 保留完整列表便于查看匹配项的前后文
        }

    // 按类型路由日志到对应缓冲（主列表 / 插件缓冲 / 规则缓冲）。
    // 双保险裁剪：条数上限 + 字符总量熔断（超长条目堆积时按最旧裁，与条数无关）
    private val pluginChars = java.util.concurrent.atomic.AtomicLong()
    private val ruleChars = java.util.concurrent.atomic.AtomicLong()

    // Console 通道（插件 JS / 朗读规则 JS）的 LogEntry 不带 time，到达时补打时间戳，
    // 供勾选后与主列表按时间混排（logback 通道的日志自带时间戳，走 else 分支不覆盖）
    @Synchronized
    private fun stampTime(entry: LogEntry): LogEntry =
        if (entry.time.isEmpty())
            entry.copy(time = auxTimeFormatter.format(System.currentTimeMillis()))
        else entry

    private fun routeEntry(entry: LogEntry) {
        val isPlugin = entry.isPluginLog
        val isRule = entry.isSpeechRuleLog
        when {
            isPlugin || isRule -> {
                val stamped = stampTime(entry)
                val target = if (isPlugin) pluginLogs else speechRuleLogs
                val chars = if (isPlugin) pluginChars else ruleChars
                target.add(stamped)
                chars.addAndGet(stamped.message.length.toLong())
                // 条数裁剪
                val overflow = target.size - AUX_MAX
                if (overflow >= AUX_PRUNE) {
                    repeat(overflow) {
                        val removed = target.removeAt(0)
                        chars.addAndGet(-removed.message.length.toLong())
                    }
                }
                // 字符总量熔断（防超长条目堆积）
                while (chars.get() > AUX_CHAR_BUDGET && target.size > 1) {
                    val removed = target.removeAt(0)
                    chars.addAndGet(-removed.message.length.toLong())
                }
            }
            else -> {
                logs.add(entry)
                val overflow = logs.size - MAX_LOGS
                if (overflow >= PRUNE_BATCH) {
                    repeat(overflow) { logs.removeAt(0) }
                }
            }
        }
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

    /** 显示范围=全部（用户 09-08 简化：级别键退役，只留三态） */
    fun showAllLevels() {
        selectedLevels.clear()
    }

    /** 显示范围=只看错误+警告（排障态） */
    fun showErrorsOnly() {
        selectedLevels.clear()
        selectedLevels.add(LogLevel.ERROR)
        selectedLevels.add(LogLevel.WARN)
    }

    /** 当前是否为"只看错误"态 */
    fun isErrorsOnly(): Boolean =
        selectedLevels.toList() == listOf(LogLevel.ERROR, LogLevel.WARN)

    fun clear() {
        logs.clear()
        pluginLogs.clear()
        speechRuleLogs.clear()
        runCatching {
            FileWriter(file, false).use { it.write(CharArray(0)) }
            // 连同其余日志文件与滚动备份一起清理（用户 09-08：删除键应清全部本地日志）
            file.parentFile?.listFiles()?.forEach { f ->
                if (!f.isFile) return@forEach
                val n = f.name
                if (n.startsWith("debug") || n.startsWith("crash") || n.startsWith("system_tts_")) {
                    f.delete()
                }
            }
        }.onFailure {
            logs.add(LogEntry(level = LogLevel.ERROR, message = it.stackTraceToString()))
            Log.e(TAG, "clear: ", it)
        }
    }


    private fun toLogEntry(line: String): LogEntry {
        // 新格式（09-08）：time | LEVEL | configId | roleName | message —— configId/roleName
        // 随 MDC 落盘，重启后旧日志行仍可点开快捷面板；configId 恒为数字作判别，兼容旧三段格式
        Regex(
            "^(\\d{4}-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d\\.\\d{3}) \\|\\s*([A-Z]+)\\s*\\|\\s*(\\d+)\\s*\\|\\s*(.*?)\\s*\\|\\s*(.*)$"
        ).find(line)?.let { m ->
            return LogEntry(
                level = m.groupValues[2].toLogLevel(),
                time = m.groupValues[1],
                message = m.groupValues[5],
                configId = m.groupValues[3].toLongOrNull() ?: 0L,
                roleName = m.groupValues[4],
            )
        }
        // MDC 全空的变体（无 configId 的系统级日志）：configId/roleName 落盘为空段，
        // 形如 time | LEVEL |  |  | message。上面的数字判别匹配不上，若掉进旧三段解析
        // 会把空段拼进 message（用户 09-09：屏幕上出现「D | | xxx」竖杠）；单独吃掉空段
        Regex(
            "^(\\d{4}-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d\\.\\d{3}) \\|\\s*([A-Z]+)\\s*\\|\\s*\\|\\s*\\|\\s*(.*)$"
        ).find(line)?.let { m ->
            return LogEntry(
                level = m.groupValues[2].toLogLevel(),
                time = m.groupValues[1],
                message = m.groupValues[3],
            )
        }
        // 旧格式：time | LEVEL | message（message 可含 " | "，需全量重组）
        return line.split(" | ").let {
            val time = it[0]
            val level = it[1]
            val message = it.drop(2).joinToString(" | ")
            LogEntry(
                level = level.toLogLevel(), time = time, message = message
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
                // 统一入口：按类型路由（主列表/插件缓冲/规则缓冲），各自限高
                SysttsLogger.register { log ->
                    runOnUI { routeEntry(log) }
                }

                // 注册插件日志监听器
                Console.globalPluginLogListener = { logEntry ->
                    runOnUI { routeEntry(logEntry) }
                }

                // 注册朗读规则日志监听器
                Console.globalSpeechRuleLogListener = { logEntry ->
                    runOnUI { routeEntry(logEntry) }
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
                // 最多读取最近 1500 行，解析后按类型路由批量添加，避免逐条触发多次重组
                val entries = readTailLines(file, 1500).mapNotNull { line ->
                    runCatching { toLogEntry(line) }.getOrNull()
                }
                withMain {
                    entries.forEach { routeEntry(it) }
                }
            }
        }.onFailure {
            logs.add(LogEntry(level = LogLevel.ERROR, message = it.stackTraceToString()))
            Log.e(TAG, "pull: ", it)
        }

    }

    /**
     * 只读文件尾部 N 行。
     * 旧实现 file.readLines() 把整个文件一次性载入内存再 takeLast——日志文件按天滚动
     * 无大小上限，单日可达数十 MB，瞬时分配与文件大小成正比，是启动后首次进日志栏
     * 的 OOM 爆点。尾部 1MB 按平均 ~700B/行覆盖 1500 行绰绰有余。
     */
    private fun readTailLines(file: File, maxLines: Int): List<String> {
        val fileLen = file.length()
        val tailLen = minOf(fileLen, 1_000_000L).toInt()
        val buf = ByteArray(tailLen)
        java.io.RandomAccessFile(file, "r").use { raf ->
            raf.seek(fileLen - tailLen)
            raf.readFully(buf)
        }
        val lines = String(buf, Charsets.UTF_8).split('\n')
        // 起点不在文件头时，首行大概率是被截断的半行，丢弃
        val start = if (tailLen < fileLen) 1 else 0
        return lines.drop(start).filter { it.isNotBlank() }.takeLast(maxLines)
    }
}
