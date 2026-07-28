package com.github.jing332.tts_server_android.service.keepalive

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * 通知监听保活服务
 * 利用 NotificationListenerService 的系统优先级来保活
 * 不实际处理通知，仅用于提升进程优先级
 */
class NotificationKeepAliveService : NotificationListenerService() {

    companion object {
        const val TAG = "NotificationKeepAlive"

        var isRunning = false
            private set

        fun isEnabled(context: Context): Boolean {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val componentName = context.packageName + "/" + NotificationKeepAliveService::class.java.name
            return enabledListeners.contains(componentName)
        }

        fun openSettings(context: Context) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            context.startActivity(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        android.util.Log.d(TAG, "Notification keep alive service created")
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        android.util.Log.d(TAG, "Notification keep alive service destroyed")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isRunning = true
        android.util.Log.d(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isRunning = false
        android.util.Log.d(TAG, "Notification listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // 不处理通知，仅用于保活
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // 不处理通知移除
    }
}
