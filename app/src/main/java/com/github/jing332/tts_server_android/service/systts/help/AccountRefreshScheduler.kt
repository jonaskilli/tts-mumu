package com.github.jing332.tts_server_android.service.systts.help

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 账号池静默续期调度（10-08 移植插件 refresh-scheduler 语义）：
 *  - 每 30 分钟一轮检查（插件 REFRESH_INTERVAL_MS=30min），到期前 1h 内（插件
 *    REFRESH_LEAD_MS=1h，shouldRefreshNow 同判据）才真正发续期请求——不是每轮都打上游；
 *  - 防重入：一轮没跑完不叠第二轮（插件 MultiAccountRefreshScheduler 同款闸门）；
 *  - 失败只记日志不停摆（下轮照查）；refresh_token 失效（400/401 类业务失败）不改
 *    expiresAt，账号在 UI 上保持「已过期」，用户重登即可；
 *  - app 侧触发点：TTS 服务 onCreate（朗读时进程活着就会检查）+ 每日签到闹钟顺带
 *    一轮（AccountCheckinReceiver 已有「签到前过期先续期」，互为兜底）。
 *
 * 为什么不用 AlarmManager：续期是「空闲时顺手」的低频内务，Handler 轮询在进程活着
 * 的窗口内完成即可；进程死了没续成时，对话链的 401 现场续期（SseAggregator）兜底。
 */
object AccountRefreshScheduler {
    private const val TAG = "AccountRefresh"

    /** 检查周期：30 分钟（插件 REFRESH_INTERVAL_MS 同值） */
    private const val CHECK_INTERVAL_MS = 30 * 60_000L

    /** 提前量：距过期 ≤1h 才发续期请求（插件 REFRESH_LEAD_MS 同值） */
    private const val REFRESH_LEAD_MS = 60 * 60_000L

    private val running = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())
    private var started = false

    fun start(context: Context) {
        if (started) return
        started = true
        handler.postDelayed({ tick(context) }, 5_000L) // 启动后 5s 首查（短寿命凭据补续）
    }

    private fun tick(context: Context) {
        runRound()
        handler.postDelayed({ tick(context) }, CHECK_INTERVAL_MS)
    }

    /** 一轮续期：启用中的账号，距过期 ≤1h（或已过期）才发请求。返回 "续期x/共y" */
    fun runRound(): String {
        if (!running.compareAndSet(false, true)) return "上一轮进行中"
        try {
            val accounts = AccountPool.load().filter { it.enabled }
            if (accounts.isEmpty()) return "无账号"
            val now = System.currentTimeMillis()
            var ok = 0
            var due = 0
            accounts.forEach { acc ->
                // 判据照插件 shouldRefreshNow：无过期时刻（0）视为「需要查」；≤1h 内到期才动手
                val needsRefresh = acc.expiresAt == 0L || acc.expiresAt - now <= REFRESH_LEAD_MS
                if (!needsRefresh) return@forEach
                due++
                val (ref, err) = AccountPool.refresh(acc)
                if (ref != null) {
                    ok++
                    Log.i(TAG, "续期成功：${acc.nickname}（新过期 ${ref.expiresAt}）")
                } else {
                    Log.w(TAG, "续期失败：${acc.nickname}：$err")
                }
            }
            return "续期 $ok/$due（共 ${accounts.size} 账号）"
        } finally {
            running.set(false)
        }
    }
}
