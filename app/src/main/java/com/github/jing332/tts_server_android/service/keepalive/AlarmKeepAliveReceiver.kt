package com.github.jing332.tts_server_android.service.keepalive

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings

/**
 * 闹钟定时唤醒保活
 * 使用 AlarmManager 实现更频繁的唤醒（最短1分钟）
 */
class AlarmKeepAliveReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "AlarmKeepAlive"
        private const val ACTION_KEEP_ALIVE = "com.github.jing332.tts_server_android.KEEP_ALIVE_ALARM"
        private const val REQUEST_CODE = 1002

        // 唤醒间隔（毫秒）- 1分钟
        private const val INTERVAL_MS = 60 * 1000L

        fun canScheduleExactAlarms(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
        }

        fun requestExactAlarmPermission(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }

        fun schedule(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmKeepAliveReceiver::class.java).apply {
                action = ACTION_KEEP_ALIVE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 设置精确唤醒（需电池优化白名单）
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + INTERVAL_MS,
                pendingIntent
            )
        }

        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmKeepAliveReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_KEEP_ALIVE) {
            android.util.Log.d(TAG, "Alarm keep alive triggered")

            // 如果保活服务未运行但配置启用了，启动它
            if (!KeepAliveService.isRunning && com.github.jing332.tts_server_android.conf.SysTtsConfig.isKeepAliveEnabled) {
                KeepAliveService.start(context)
            }

            // 重新调度下一次
            schedule(context)
        }
    }
}
