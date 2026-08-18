package com.github.jing332.tts_server_android.service.keepalive

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.github.jing332.common.utils.startForegroundCompat
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.MainActivity
import com.github.jing332.tts_server_android.conf.SysTtsConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 进程保活服务
 * 通过多种机制防止应用被系统杀死（墓碑冻结）
 */
class KeepAliveService : Service() {

    companion object {
        const val TAG = "KeepAliveService"
        const val NOTIFICATION_CHANNEL_ID = "keep_alive_channel"
        const val NOTIFICATION_ID = 9999
        const val ACTION_KEEP_ALIVE = "ACTION_KEEP_ALIVE"
        const val ACTION_STOP_KEEP_ALIVE = "ACTION_STOP_KEEP_ALIVE"

        // 保活检查间隔（毫秒）
        const val CHECK_INTERVAL_MS = 5000L
        // 唤醒锁持有时间（毫秒）
        const val WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1000L // 10分钟

        @Volatile
        var isRunning = false

        fun start(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java).apply {
                action = ACTION_KEEP_ALIVE
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, KeepAliveService::class.java))
        }
    }

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var keepAliveJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())

    // 屏幕状态监听 - 仅用于息屏时获取唤醒锁，不抢占音频焦点
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // 屏幕关闭时，获取唤醒锁（不抢占音频焦点）
                    acquireWakeLock()
                }
                Intent.ACTION_SCREEN_ON -> {
                    // 屏幕开启时，释放唤醒锁
                    releaseWakeLock()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true

        // 创建高优先级通知渠道
        createNotificationChannel()
        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification())
        // 注册屏幕状态监听
        registerScreenStateReceiver()
        // 启动保活机制
        startKeepAliveMechanism()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_KEEP_ALIVE -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY // 被杀死后会自动重启
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        keepAliveJob?.cancel()
        releaseWakeLock()
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (_: Exception) {}

        // 如果配置仍需要保活，尝试重启服务
        if (SysTtsConfig.isKeepAliveEnabled) {
            sendBroadcast(Intent(ACTION_KEEP_ALIVE).apply {
                setPackage(packageName)
            })
        }
    }

    /**
     * 创建高优先级通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.keep_alive_service),
                NotificationManager.IMPORTANCE_HIGH // 高优先级，不易被系统清理
            ).apply {
                description = getString(R.string.keep_alive_service_desc)
                setShowBadge(false)
                // 设置为重要渠道，降低被系统清理的概率
                importance = NotificationManager.IMPORTANCE_HIGH
                // 启用振动和提示灯
                enableVibration(true)
                enableLights(true)
                lightColor = ContextCompat.getColor(this@KeepAliveService, R.color.md_theme_light_primary)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 创建前台服务通知
     */
    private fun createNotification(): Notification {
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            pendingIntentFlags
        )

        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, KeepAliveService::class.java).apply {
                action = ACTION_STOP_KEEP_ALIVE
            },
            pendingIntentFlags
        )

        return Notification.Builder(this).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setChannelId(NOTIFICATION_CHANNEL_ID)
            }
            setContentTitle(getString(R.string.keep_alive_service_title))
            setContentText(getString(R.string.keep_alive_service_content))
            setSmallIcon(R.mipmap.ic_app_notification)
            setContentIntent(pendingIntent)
            setOngoing(true) // 持续通知，不易被用户清除
            setShowWhen(false)
            addAction(0, getString(R.string.stop_keep_alive), stopIntent)
            // 设置高优先级
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setPriority(Notification.PRIORITY_HIGH)
            }
            setColor(ContextCompat.getColor(this@KeepAliveService, R.color.md_theme_light_primary))
        }.build()
    }

    /**
     * 注册屏幕状态监听
     */
    private fun registerScreenStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(
            this, screenStateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    /**
     * 启动保活机制
     */
    private fun startKeepAliveMechanism() {
        keepAliveJob = scope.launch {
            while (isActive) {
                // 定期执行保活操作
                performKeepAliveActions()
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    /**
     * 执行保活操作
     * 注意：不抢占音频焦点，避免影响其他TTS/音乐应用
     */
    private fun performKeepAliveActions() {
        // 1. 检查并更新通知，防止被系统清理
        updateNotification()

        // 2. 如果启用了唤醒锁，确保它被持有
        if (SysTtsConfig.isWakeLockEnabled) {
            acquireWakeLock()
        }

        // 3. 调用系统闹钟API保持CPU活跃
        SystemClock.sleep(1)
    }

    /**
     * 更新通知（刷新前台服务状态）
     */
    private fun updateNotification() {
        handler.post {
            val notification = createNotification()
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    /**
     * 获取唤醒锁
     */
    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TTS_SERVER_ANDROID::KeepAliveWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(WAKE_LOCK_TIMEOUT_MS)
        }
    }

    /**
     * 释放唤醒锁
     */
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }
}
