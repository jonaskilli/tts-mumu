package com.github.jing332.tts_server_android.service.keepalive

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/**
 * 无障碍保活服务
 * 利用 AccessibilityService 的高系统优先级来保活
 * 仅作为保活用途，不监听任何无障碍事件
 */
class AccessibilityKeepAliveService : AccessibilityService() {

    companion object {
        const val TAG = "AccessibilityKeepAlive"

        var isRunning = false
            private set

        fun isEnabled(context: Context): Boolean {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val componentName = context.packageName + "/" + AccessibilityKeepAliveService::class.java.name
            return enabledServices.contains(componentName)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        android.util.Log.d(TAG, "Accessibility keep alive service created")
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        android.util.Log.d(TAG, "Accessibility keep alive service destroyed")

        // 如果用户仍启用了无障碍服务，尝试重启
        if (isEnabled(this)) {
            val intent = Intent(this, AccessibilityKeepAliveService::class.java)
            startService(intent)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 不处理任何事件，仅用于保活
    }

    override fun onInterrupt() {
        // 不处理中断
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        android.util.Log.d(TAG, "Accessibility service connected")

        // 配置服务
        serviceInfo = serviceInfo.apply {
            // 不监听任何事件类型
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED
            // 不反馈类型
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            // 不通知其他服务
            notificationTimeout = 100
        }
    }
}
