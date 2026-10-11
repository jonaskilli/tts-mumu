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

        // 主流程日志监听器是否已全局注册（见 RouteSink 注释：防重复注册导致同一条日志多份）
        private val registered = java.util.concurrent.atomic.AtomicBoolean(false)

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

    // DEBUG(D) 级开关（用户 09-10 定稿）：**默认开启**——插件开发靠 console.debug 调试；
    // 嫌吵的用户在筛选里关掉后 D 级完全不进列表。持久化到配置，避免每次启动重关。
    val showDebugLogs = com.github.jing332.tts_server_android.conf.SystemTtsConfig.isDebugLogEnabled

    // 与系统日志同款时间戳格式：合并排序按字符串比较即可保持时序（等宽、字典序=时间序）
    private val auxTimeFormatter =
        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US)

    val filteredLogs: List<LogEntry>
        get() {
            // 勾选=全量混排（用户 09-09）：主列表与勾选的辅助缓冲按时间戳合并。
            // 不勾选=不混入（用户 10-03 拍板，推翻 09-09 的 ERROR/WARN 旁路放行）：
            // 插件/规则的黄警红错常年混在主时间流里搅乱主日志。唯一例外=朗读规则的
            // ERROR 红字仍放行（10-03 二次拍板）——「无可用API密钥/请求失败/降级兜底」
            // 这类规则侧报障是排障主线索；WARN 黄字与插件日志（含 ERROR）一律不混。
            // app 侧失败文案（获取失败/源错误/超时）本就在主日志，报障链不中断
            val showPlugin = showPluginLogs.value
            val showRule = showSpeechRuleLogs.value
            val levels = selectedLevels
            // DEBUG 开关关闭时：主列表的 D 级一并隐藏（插件/规则缓冲只在勾选时参与）
            val showDebug = showDebugLogs.value
            fun keep(e: LogEntry) =
                (levels.isEmpty() || e.level in levels) && (showDebug || e.level != LogLevel.DEBUG)

            val pluginPart = if (showPlugin) pluginLogs else emptyList<LogEntry>()
            val rulePart =
                if (showRule) speechRuleLogs
                else speechRuleLogs.filter { it.level == LogLevel.ERROR }

            // 都不勾（或勾了但缓冲为空）：纯主列表直通，不建列表不排序
            if (pluginPart.isEmpty() && rulePart.isEmpty())
                return logs.filter { keep(it) }

            val source = ArrayList<LogEntry>(logs.size + pluginPart.size + rulePart.size)
            source.addAll(logs)
            source.addAll(pluginPart)
            source.addAll(rulePart)
            // 稳定排序：同毫秒内保持各缓冲内的到达顺序
            val merged = source.sortedBy { it.time }
            return merged.filter { keep(it) }
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
                // 相邻同因合并（10-11 用户令「相同原因的失败视为相同，日志可以×2、×3」）：
                // 与末条同指纹则并入计数，不再新增条目。10-05 曾因「×2 让人困惑」撤掉折叠，
                // 但那时的真根因是 SysttsLogger 监听器重复注册（同一条日志被投递两遍，
                // 已在 1bf6a74 修为 RouteSink 单例）——现在折叠反映的是真实重复次数。
                val merged = mergeInto(target, stamped)
                if (merged == null) {
                    target.add(stamped)
                    chars.addAndGet(stamped.message.length.toLong())
                } else {
                    // 并入末条：先扣旧串长度、再加新串长度（message 保留首条原文，
                    // 只更新计数与末条时间，避免长度记账漂移）。
                    // ⚠️ chars 是 AtomicLong，addAndGet 只收 Long——两个 Int 相减必须显式 toLong()
                    //（CI 38138740502 实锤：Argument type mismatch: actual Int, but Long was expected）
                    chars.addAndGet((merged.newLen - merged.oldLen).toLong())
                }
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
                // 主流程日志同样折叠（10-11 用户令「不管是插件还是朗读规则还是正常的日志」）：
                // 应用层失败链重试风暴同因刷屏也收成一条 ×N
                val merged = mergeInto(logs, entry)
                if (merged == null) {
                    logs.add(entry)
                }
                val overflow = logs.size - MAX_LOGS
                if (overflow >= PRUNE_BATCH) {
                    repeat(overflow) { logs.removeAt(0) }
                }
            }
        }
    }

    /** 合并结果：新长度与旧长度之差（供字符记账） */
    private class MergeResult(val oldLen: Int, val newLen: Int)

    /** 重试行判据（「开始第 N 次重试」）：折叠时它不算「打断相邻」的日志 */
    private fun isRetryRow(e: LogEntry): Boolean =
        e.level == LogLevel.WARN && e.message.contains("次重试")

    /**
     * 相邻同因合并（10-11 用户令）：
     * 与本条**同级别**、且**原因指纹相同**的最近一条失败/警告，把本条并入它——
     * 计数 +1、记下末条时间（行头显示「首末完整时间」），message 保留首条原文。
     * 返回 null 表示未合并（调用方照常 add）。
     *
     * 「相邻」的口径（关键）：**重试行不打断相邻**。真实失败链是
     *   请求音频 → 获取失败 → 「开始第 1 次重试」 → 获取失败 → 到达上限 → 切兜底 → 成功
     * 两次「获取失败」中间夹着重试行；若按字面严格相邻，用户要的 ×2 永远折不出来
     *（10-11 模拟实锤：严格口径下两条获取失败各 ×1）。重试行属于同一条失败链的
     * 过程噪声，故向后越过它去找上一条失败；合并时把被越过的重试行一并吸收
     *（次数已由 ×N 表达，留着重试行反而是同一件事说两遍）。
     * 非重试行的其他日志（切备用/成功/请求音频）照旧打断相邻——这正是用户要的
     *「相邻」的本意：不跨事件合并。
     *
     * 指纹口径（`failureFingerprint`）：去 HTML 标签 → 去首尾空白 → 去掉插件内部
     * 重试序号 `[0]`/`[1]`（序号不是失败原因，不去掉则同一次失败的各次重试会被
     * 当成不同原因而折不起来）。数字本身保留：HTTP-404 与 HTTP-401 是不同原因。
     */
    private fun mergeInto(target: MutableList<LogEntry>, entry: LogEntry): MergeResult? {
        if (entry.level != LogLevel.ERROR && entry.level != LogLevel.WARN) return null
        val fp = failureFingerprint(entry.message) ?: return null
        // 向后越过重试行，找最近一条非重试日志
        var i = target.lastIndex
        while (i >= 0 && isRetryRow(target[i])) i--
        if (i < 0) return null
        val last = target[i]
        if (last.level != entry.level) return null
        if (fp != failureFingerprint(last.message)) return null
        val oldLen = last.message.length
        val merged = last.copy(
            repeatCount = last.repeatCount + 1,
            // 末条时间：首末范围用。末条本身也可能是合并来的，它的 endTime 已是更新的那个
            endTime = entry.time.ifEmpty { last.endTime },
        )
        target[i] = merged
        // 吸收被越过的重试行（i 之后的都只可能是重试行——上面循环保证了这一点）
        if (i < target.lastIndex) {
            repeat(target.size - 1 - i) { target.removeAt(target.lastIndex) }
        }
        return MergeResult(oldLen, merged.message.length)
    }

    /** 失败原因指纹（同因判据）；空正文返回 null=不参与合并 */
    private fun failureFingerprint(message: String): String? {
        val s = message
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("\\[\\d+]"), "") // 插件内部重试序号 [0]/[1]
            .trim()
        return s.ifEmpty { null }
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

    // 日志目录路径。注意：成员 file 是日志文件本身（.../cache/log/system_tts.log），
    // 上游此处直接返回 file.absolutePath，导致调用方 File(logDir()).listFiles() 恒为 null
    // （listFiles 对非目录返回 null）→ 日志文件列表永远显示"日志目录为空"（用户 09-13 报）
    fun logDir(): String {
        return file.parentFile?.absolutePath ?: file.absolutePath
    }

    // 主流程日志的分发落点。⚠️ 必须是「固定单例回调 + 可变目标」，不能每次 init 直接
    // SysttsLogger.register { routeEntry(...) }：
    // SysttsLogger 内部是 CopyOnWriteArraySet<LogListener>，而每次 register 传入的都是新的
    // lambda 实例（equals 不成立）⇒ **重复注册不会被去重**，监听器只增不减；且旧 lambda
    // 捕获的是旧 VM ⇒ 同一条日志被投递 N 次，表现成「日志里同一行出现两遍」（用户 10-05
    // 看到的 ×2 即此；×N 折叠只是把症状合了起来，根因在此）。
    // 现在：监听器**全局只注册一次**（见 registered 标志），回调只做一件事——把日志交给
    // 当前活着的那份 VM。
    private object RouteSink {
        @Volatile
        var target: TtsLogViewModel? = null

        fun emit(entry: LogEntry) {
            val vm = target ?: return
            runOnUI { vm.routeEntry(entry) }
        }
    }

    init {
        // 无论本 VM 是第几次创建，都把自己登记为当前分发目标（旧 VM 由 onCleared 摘除）
        RouteSink.target = this
        try {
            viewModelScope.launch(Dispatchers.IO) {
                pull()

                // 主流程日志监听器：全局幂等注册（这是修掉"同一条日志出现两遍"的关键）
                if (registered.compareAndSet(false, true)) {
                    SysttsLogger.register { log -> RouteSink.emit(log) }
                }

                // 插件/朗读规则日志监听器：Console 侧是**单值静态赋值**（覆盖语义），
                // 每次重新指向当前 VM 即可，不存在累加问题
                Console.globalPluginLogListener = { logEntry ->
                    runOnUI { routeEntry(logEntry) }
                }
                Console.globalSpeechRuleLogListener = { logEntry ->
                    runOnUI { routeEntry(logEntry) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "init: ", e)
        }
    }

    /** 本 VM 退出时摘掉分发目标，避免已销毁的 VM 继续接收（并让下一个 VM 接手） */
    override fun onCleared() {
        if (RouteSink.target === this) RouteSink.target = null
        super.onCleared()
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
