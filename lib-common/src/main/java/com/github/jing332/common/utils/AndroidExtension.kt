package com.github.jing332.common.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.content.res.Resources
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.jvm.Throws

fun Context.clearWebViewData() {
    WebView(this).apply {
        clearCache(true)
        clearFormData()
        clearSslPreferences()
        destroy()
    }
    CookieManager.getInstance().apply {
        removeAllCookies(null)
        flush()
    }
    WebStorage.getInstance().deleteAllData()
}

private val logger by lazy { KotlinLogging.logger("AndroidExtension") }

@SuppressLint("MissingPermission")
@Throws(ForegroundServiceStartNotAllowedException::class)
fun Service.startForegroundCompat(
    notificationId: Int,
    notification: Notification,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // A14
        try {
            startForeground(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } catch (e: Exception) {
            logger.error(e) { "startForegroundCompat" }
            NotificationManagerCompat.from(this)
                .notify(notificationId, notification)
        }
    } else {
        startForeground(notificationId, notification)
    }
}

fun Context.startForegroundServiceCompat(intent: Intent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // A12
        try {
            startForegroundService(intent)
        } catch (e: ForegroundServiceStartNotAllowedException/* S */) {
            logger.error(e) { "startForegroundServiceCompat" }
            startService(intent)
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //A8
        startForegroundService(intent)
    } else {
        startService(intent)
    }
}


fun Context.registerGlobalReceiver(
    actions: List<String>,
    receiver: BroadcastReceiver,
) {
    ContextCompat.registerReceiver(this, receiver, IntentFilter().apply {
        actions.forEach { addAction(it) }
    }, ContextCompat.RECEIVER_EXPORTED)
}

fun View.performLongPress() {
    this.isHapticFeedbackEnabled = true
    this.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}

fun Context.startActivity(clz: Class<*>) {
    startActivity(Intent(this, clz).apply { action = Intent.ACTION_VIEW })
}

fun Uri.grantReadWritePermission(contentResolver: ContentResolver) {
    contentResolver.takePersistableUriPermission(
        this,
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    )
}


fun Intent.getBinder(): IBinder? {
    val bundle = getBundleExtra("bundle")
    return bundle?.getBinder("bigData")
}

fun Intent.setBinder(binder: IBinder) {
    putExtra(
        "bundle",
        Bundle().apply {
            putBinder("bigData", binder)
        })
}

val Context.layoutInflater: LayoutInflater
    get() = LayoutInflater.from(this)

fun ViewGroup.setMarginMatchParent() {
    this.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
}

/**
 * 重启当前 Activity
 */
fun Activity.restart() {
    finish()
    ContextCompat.startActivity(this, intent, null)
}

@Suppress("DEPRECATION")
val Activity.displayHeight: Int
    get() {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            windowMetrics.bounds.height() - insets.bottom - insets.top
        } else
            windowManager.defaultDisplay.height
    }

/**
 * 点击防抖动
 */
fun View.clickWithThrottle(throttleTime: Long = 600L, action: (v: View) -> Unit) {
    this.setOnClickListener(object : View.OnClickListener {
        private var lastClickTime: Long = 0

        override fun onClick(v: View) {
            if (SystemClock.elapsedRealtime() - lastClickTime < throttleTime) return
            else action(v)

            lastClickTime = SystemClock.elapsedRealtime()
        }
    })
}

/**
 * View 是否在屏幕上可见
 */
fun View.isVisibleOnScreen(): Boolean {
    if (!isShown) {
        return false
    }
    val actualPosition = Rect()
    val isGlobalVisible = getGlobalVisibleRect(actualPosition)
    val screenWidth = Resources.getSystem().displayMetrics.widthPixels
    val screenHeight = Resources.getSystem().displayMetrics.heightPixels
    val screen = Rect(0, 0, screenWidth, screenHeight)
    return isGlobalVisible && Rect.intersects(actualPosition, screen)
}


/**
 * 绑定返回键回调（建议使用该方法）
 * @param owner Receive callbacks to a new OnBackPressedCallback when the given LifecycleOwner is at least started.
 * This will automatically call addCallback(OnBackPressedCallback) and remove the callback as the lifecycle state changes. As a corollary, if your lifecycle is already at least started, calling this method will result in an immediate call to addCallback(OnBackPressedCallback).
 * When the LifecycleOwner is destroyed, it will automatically be removed from the list of callbacks. The only time you would need to manually call OnBackPressedCallback.remove() is if you'd like to remove the callback prior to destruction of the associated lifecycle.
 * @param onBackPressed 回调方法；返回true则表示消耗了按键事件，事件不会继续往下传递，相反返回false则表示没有消耗，事件继续往下传递
 * @return 注册的回调对象，如果想要移除注册的回调，直接通过调用[OnBackPressedCallback.remove]方法即可。
 */
fun androidx.activity.ComponentActivity.addOnBackPressed(
    owner: LifecycleOwner,
    onBackPressed: () -> Boolean,
): OnBackPressedCallback {
    return backPressedCallback(onBackPressed).also {
        onBackPressedDispatcher.addCallback(owner, it)
    }
}

/**
 * 绑定返回键回调，未关联生命周期，建议使用关联生命周期的办法（尤其在fragment中使用，应该关联fragment的生命周期）
 */
fun androidx.activity.ComponentActivity.addOnBackPressed(onBackPressed: () -> Boolean): OnBackPressedCallback {
    return backPressedCallback(onBackPressed).also {
        onBackPressedDispatcher.addCallback(it)
    }
}

private fun androidx.activity.ComponentActivity.backPressedCallback(onBackPressed: () -> Boolean): OnBackPressedCallback {
    return object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!onBackPressed()) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
    }
}