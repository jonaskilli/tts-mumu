package com.github.jing332.tts_server_android.service.systts.help

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * 账号池每日签到调度（10-06）：AlarmManager 每日 9 点 + 开机补签，
 * 遍历启用中的账号逐个签到（过期先续期再签）。跟 keepalive 的 AlarmKeepAliveReceiver
 * 同款机制（项目未引入 WorkManager，不为此单加依赖）。
 */
class AccountCheckinReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "AccountCheckin"

        /** 每日签到闹钟触发时刻：09:05（避开整点高峰） */
        const val HOUR = 9
        const val MINUTE = 5
    }

    override fun onReceive(context: Context, intent: Intent) {
        // 有序广播短生命周期：goAsync + 独立线程跑网络；总量小（3~5 账号），runBlocking 可控
        val pending = goAsync()
        runBlocking {
            try {
                withContext(Dispatchers.IO) { checkinAll() }
            } catch (e: Exception) {
                Log.w(TAG, "checkinAll: ${e.message}")
            } finally {
                pending.finish()
            }
            // 排明天的闹钟（无论本次成败）
            AccountCheckinScheduler.scheduleNext(context)
        }
    }

    /** 全量签到：启用中的账号，过期的先续期。返回 "成功x/共y" 摘要 */
    fun checkinAll(): String {
        val accounts = AccountPool.load().filter { it.enabled }
        if (accounts.isEmpty()) return "无账号"
        var ok = 0
        accounts.forEach { acc ->
            val target = if (acc.isExpired()) AccountPool.refresh(acc).first ?: acc else acc
            val (success, _) = AccountPool.checkIn(target)
            if (success) ok++
        }
        return "签到 $ok/${accounts.size}"
    }
}

/**
 * 签到闹钟排程器：setExactAndAllowWhileIdle 每日触发 + 开机接收器补排。
 */
object AccountCheckinScheduler {
    private const val TAG = "AccountCheckin"
    private const val REQUEST_CODE = 47291

    fun scheduleNext(context: Context) {
        try {
            val am = android.app.AlarmManager.getInstance(context)
            val now = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, AccountCheckinReceiver.HOUR)
                set(java.util.Calendar.MINUTE, AccountCheckinReceiver.MINUTE)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            val pi = android.app.PendingIntent.getBroadcast(
                context, REQUEST_CODE,
                Intent(context, AccountCheckinReceiver::class.java),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, now.timeInMillis, pi)
            else am.setExact(android.app.AlarmManager.RTC_WAKEUP, now.timeInMillis, pi)
            Log.d(TAG, "next checkin alarm: ${now.time}")
        } catch (e: Exception) {
            Log.w(TAG, "scheduleNext: ${e.message}")
        }
    }
}
