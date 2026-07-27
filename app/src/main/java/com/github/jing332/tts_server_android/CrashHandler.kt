package com.github.jing332.tts_server_android

import android.content.Context
import android.os.Build
import ch.qos.logback.classic.LoggerContext
import com.github.jing332.common.utils.ClipboardUtils
import com.github.jing332.common.utils.longToast
import com.github.jing332.common.utils.runOnUI
import com.github.jing332.tts_server_android.constant.AppConst
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.LoggerFactory
import java.time.LocalDateTime


class CrashHandler(val context: Context) : Thread.UncaughtExceptionHandler {
    companion object{
        const val TAG = "CrashHandler"
    }
    private var mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    private val logger = KotlinLogging.logger(TAG)

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        handleException(e)
        val ctx = LoggerFactory.getILoggerFactory() as LoggerContext
        ctx.stop()

        mDefaultHandler?.uncaughtException(t, e)
    }

    private fun handleException(e: Throwable) {
        val versionName = BuildConfig.VERSION_NAME
        val versionCode = BuildConfig.VERSION_CODE
        val brand = Build.BRAND
        val device = Build.DEVICE
        val androidVersion = Build.VERSION.RELEASE
        val modelName = Build.MODEL

        logger.error (e) {
            "$brand / $device / $modelName / $androidVersion" +
                    "\n$versionName ($versionCode)"
        }
        if (BuildConfig.DEBUG) return

        context.longToast("TTS Server已崩溃 上传日志中 稍后将会复制到剪贴板")
        val log = "\n${LocalDateTime.now()}" +
                "\n版本代码：${AppConst.appInfo.versionCode}， 版本名称：${AppConst.appInfo.versionName}\n" +
                "崩溃详情：\n${e.stackTraceToString()}"

        runOnUI {
            ClipboardUtils.copyText("TTS-Server崩溃日志", log)
            context.longToast("已将日志复制到剪贴板")
        }
    }
}