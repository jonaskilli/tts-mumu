package com.github.jing332.tts_server_android

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Process 
import com.github.jing332.compose.widgets.AsyncCircleImageSettings
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.deepseekproxy.ProxyService
import com.github.jing332.tts_server_android.conf.SystemTtsConfig
import com.github.jing332.tts_server_android.conf.SystemTtsForwarderConfig
import com.github.jing332.tts_server_android.conf.SysTtsConfig
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.tts_server_android.model.hanlp.HanlpManager
import com.github.jing332.tts_server_android.service.forwarder.ForwarderServiceManager.switchSysTtsForwarder
import com.github.jing332.tts_server_android.service.forwarder.system.SysTtsForwarderService
import com.github.jing332.tts.loudness.SpeakerLoudnessManager
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.request.crossfade
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
// 👇 新增：NetConfig 配置所需的包
import com.drake.net.NetConfig
import java.util.concurrent.TimeUnit

val app: App
    inline get() = App.instance

@Suppress("DEPRECATION")
class App : Application() {
    companion object {
        const val TAG = "App"
        lateinit var instance: App
            private set

        val context: Context by lazy { instance }
    }

    override fun attachBaseContext(base: Context) {
        instance = this
        super.attachBaseContext(base.apply { AppLocale.setLocale(base) })
    }

    @SuppressLint("SdCardPath")
    @OptIn(DelicateCoroutinesApi::class, DelicateCoilApi::class)
    override fun onCreate() {
        super.onCreate()

        // 启动超时看门狗：独立守护线程，监测"超时后卡死"并自动重启 APP
        TtsTimeoutWatchdog.start()

        // 过滤 Compose 的 LeftCompositionCancellationException，避免页面快速切换时崩溃
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            if (e::class.java.simpleName == "LeftCompositionCancellationException") return@setDefaultUncaughtExceptionHandler
            previousHandler?.uncaughtException(t, e)
        }

        // 🛠️ 拔掉引线：暂时关闭 CrashHandler，它会触发崩溃的日志初始化
        // CrashHandler(this) 

        // 👇 新增：初始化 NetConfig 并设置全局超时时间为 300秒
        // 这将覆盖默认的 10秒 限制，适用于所有使用 Net 库的请求
        NetConfig.initialize("", this) {
            connectTimeout(300, TimeUnit.SECONDS)
            readTimeout(300, TimeUnit.SECONDS)
            writeTimeout(300, TimeUnit.SECONDS)
        }

        SystemTtsV2.Converters.json = AppConst.jsonBuilder
        AsyncCircleImageSettings.interceptor = AsyncImageInterceptor

        // 初始化响度均衡管理器
        SpeakerLoudnessManager.init(
            context = this,
            enabledProvider = { true },
            maxGainProvider = { SystemTtsConfig.loudnessMaxGain.value }
        )

        SingletonImageLoader.setUnsafe(
            ImageLoader
                .Builder(context)
                .crossfade(true)
                .build()
        )

        GlobalScope.launch {
            HanlpManager.initDir(
                context.getExternalFilesDir("hanlp")?.absolutePath
                    ?: "/data/data/$packageName/files/hanlp"
            )

            if (SystemTtsForwarderConfig.isAutoStart.value && !SysTtsForwarderService.isRunning) {
                switchSysTtsForwarder()
            }

            // 混元太极：若上次为「已开启」状态，App 重启后自动按原状态恢复服务
            if (ProxyService.isSavedRunning(this@App)) {
                ProxyService.startFromSaved(this@App)
            }
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun restart() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)!!
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        Process.killProcess(Process.myPid())
    }
}
