package com.github.jing332.tts_server_android

import android.os.Process
import android.util.Log
import com.github.jing332.common.LogLevel
import com.github.jing332.tts_server_android.conf.SysTtsConfig
import com.github.jing332.tts_server_android.constant.AppConst
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong

/**
 * 超时看门狗。
 *
 * 背景：合成音频时若发生超时，正常应触发重试并打印后续日志；
 * 但部分情况下流处理同步阻塞 IO 不响应协程取消，导致重试逻辑不执行、
 * 也不再打印任何日志，程序卡死在超时处。
 *
 * 策略：监测所有进入日志界面的日志，一旦出现"超时"字样就开始计时；
 * 若此后阈值内再无任何新日志，判定为卡死，直接重启 APP；
 * 若期间打印了新日志（说明已恢复/重试中），则清除超时状态继续运行。
 *
 * 阈值由设置项 [SysTtsConfig.timeoutWatchdogSeconds] 控制（单位秒）：
 * - 0 = 不重启（看门狗检测到 0 时跳过卡死判定）
 * - 其他值 = 相应秒数后重启
 *
 * 设计要点（确保不被其他程序阻碍）：
 * - 运行在独立的守护线程上，不依赖任何协程调度器/线程池，避免被卡住的合成流程阻塞。
 * - 状态用 AtomicLong 共享，无锁，[onLog] 调用极轻量，不影响日志打印性能。
 * - 重启走系统调用 Process.killProcess，不依赖 APP 内部协程状态。
 *
 * 监测点挂在 [SysttsLogger.log]，这是所有进入日志界面日志的唯一入口
 * （logW/logI/logE 经 logback SysttsFilter 转发至此，logS 直接调用）。
 */
object TtsTimeoutWatchdog {
    private const val TAG = "TtsTimeoutWatchdog"

    /** 触发关键词：日志消息包含此字样即视为"超时"日志。 */
    private const val TIMEOUT_KEYWORD = "超时"

    /** 看门狗检查间隔（毫秒）。 */
    private const val CHECK_INTERVAL_MS = 1_000L

    /** 上次打印"超时"日志的时间戳；0 表示当前未处于"超时等待恢复"状态。 */
    private val lastTimeoutLogTime = AtomicLong(0L)

    /** 上次打印任意日志的时间戳。 */
    private val lastAnyLogTime = AtomicLong(System.currentTimeMillis())

    @Volatile
    private var thread: Thread? = null

    private val fileLock = Any()

    /** 启动看门狗（幂等，重复调用安全）。建议在 Application.onCreate 中调用。 */
    fun start() {
        if (thread?.isAlive == true) return
        synchronized(this) {
            if (thread?.isAlive == true) return
            val t = Thread({
                while (true) {
                    try {
                        Thread.sleep(CHECK_INTERVAL_MS)
                        check()
                    } catch (_: InterruptedException) {
                        Log.i(TAG, "watchdog interrupted, exit loop")
                        return@Thread
                    } catch (e: Throwable) {
                        // 看门狗自身异常不应导致崩溃，吞掉后继续
                        Log.e(TAG, "watchdog check error", e)
                    }
                }
            }, "TtsTimeoutWatchdog").apply {
                isDaemon = true
                runCatching { priority = Thread.MAX_PRIORITY }
                thread = this
                start()
            }
            Log.i(TAG, "watchdog started (threshold controlled by settings, 0=disabled)")
        }
    }

    /**
     * 由 [SysttsLogger.log] 在每条日志打印时调用。非阻塞、无锁。
     *
     * 仅当日志级别为 WARN（黄色）且消息包含"超时"二字时才视为超时日志并开始计时。
     * 这样精确对应真正的黄色超时日志（ErrorEvent.RequestTimeout → logW("超时：...")），
     * 避免成功/信息类日志中偶然出现"超时"二字（如"超时重试成功"）造成误判。
     */
    fun onLog(level: Int, message: String) {
        val now = System.currentTimeMillis()
        lastAnyLogTime.set(now)
        if (level == LogLevel.WARN && message.contains(TIMEOUT_KEYWORD)) {
            lastTimeoutLogTime.set(now)
        }
    }

    private fun check() {
        val timeoutTime = lastTimeoutLogTime.get()
        if (timeoutTime <= 0L) return // 未发生过超时，无需处理

        // 读取设置阈值（秒），0 表示关闭看门狗（不重启）
        val thresholdSeconds = SysTtsConfig.timeoutWatchdogSeconds
        if (thresholdSeconds <= 0) return // 关闭：不执行卡死判定

        val thresholdMs = thresholdSeconds * 1000L
        val now = System.currentTimeMillis()
        val anyLogTime = lastAnyLogTime.get()

        if (anyLogTime > timeoutTime) {
            // 超时后又打印了新日志 → 已恢复（如重试成功/继续重试中），清除超时状态
            lastTimeoutLogTime.compareAndSet(timeoutTime, 0L)
            return
        }

        // 超时后一直无新日志：判断是否已超过卡死阈值
        val stalled = now - timeoutTime
        if (stalled >= thresholdMs) {
            Log.e(TAG, "检测到超时后 ${stalled}ms 无任何新日志（阈值 ${thresholdSeconds}s），判定卡死，重启 APP")
            // 先清除状态，防止重启过程中被重复触发
            lastTimeoutLogTime.set(0L)
            // 留痕：下次进混元太极页可看到「看门狗主动重启」说明，与真实崩溃区分开
            CrashCapture.writeNote(
                "看门狗重启（非崩溃）：超时后 ${stalled}ms 无任何新日志（阈值 ${thresholdSeconds}s），判定卡死并主动重启 APP"
            )
            writeRestartLog(stalled)
            restartApp()
        }
    }

    /** 重启前把原因写入日志文件，便于重启后日志界面可追溯。 */
    private fun writeRestartLog(stalledMs: Long) {
        runCatching {
            val time = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
            val logFile = File(AppConst.externalFilesDir.parentFile, "cache/log/system_tts.log")
            synchronized(fileLock) {
                FileWriter(logFile, true).use {
                    it.append("$time | ERROR | [Watchdog] 超时后 ${stalledMs}ms 无新日志，判定卡死，重启APP\n")
                }
            }
        }.onFailure { Log.e(TAG, "writeRestartLog failed", it) }
    }

    /**
     * 重启 APP：优先用 [App.restart]（startActivity 拉起主界面 + killProcess），
     * 失败则强制杀进程兜底。看门狗线程调用，不依赖主线程或协程。
     */
    private fun restartApp() {
        runCatching { App.instance.restart() }.onFailure {
            Log.e(TAG, "App.restart() failed, force kill process", it)
            Process.killProcess(Process.myPid())
            Runtime.getRuntime().exit(0)
        }
    }
}
