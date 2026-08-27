package com.github.jing332.tts_server_android

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Process 
import com.github.jing332.compose.widgets.AsyncCircleImageSettings
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.deepseekproxy.ProxyService
import com.github.jing332.deepseekproxy.proxy.LogStore
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
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

        // 崩溃捕获：堆栈写入本地文件 crash_last.txt，下次进混元太极页弹窗展示，
        // 便于没有 adb/logcat 的场景排查闪退原因。
        // 沿用原策略：过滤 Compose 的 LeftCompositionCancellationException，避免页面快速切换时崩溃
        CrashCapture.install()

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
            enabledProvider = { SystemTtsConfig.isLoudnessEnabled.value },
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

            // 混元太极：若上次为「已开启」状态，App 重启后自动按原状态恢复服务。
            // 加固：进程可能由后台路径拉起（如系统 TTS 引擎绑定），
            // 此时 Android 12+ 上 startForegroundService 会抛
            // ForegroundServiceStartNotAllowedException，不捕获会导致整个 App 闪退，
            // 且用户手动打开时为前台、无法复现（偶发闪退的根源）。此处降级为记录日志。
            if (ProxyService.isSavedRunning(this@App)) {
                runCatching { ProxyService.startFromSaved(this@App) }
                    .onFailure {
                        LogStore.e("Proxy", "App 重启后自动恢复混元太极服务失败: ${it.message}")
                    }
            }
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun restart() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)!!
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}

/**
 * 崩溃捕获：未捕获异常发生时，把线程名与完整堆栈写入 filesDir/crash_last.txt。
 * 下次进入混元太极界面时读取该文件并弹窗展示（可一键复制），
 * 用于没有 adb/logcat 的场景排查闪退原因；展示并关闭后删除文件避免重复弹窗。
 */
object CrashCapture {
    private const val FILE_NAME = "crash_last.txt"

    /** 安装全局未捕获异常处理器（App.onCreate 调用一次）。 */
    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            // Compose 页面快速切换的取消异常：忽略，保持进程存活（沿用原有策略）
            if (e::class.java.simpleName == "LeftCompositionCancellationException") {
                return@setDefaultUncaughtExceptionHandler
            }
            // 必须在交给系统处理器之前写文件：之后进程会终止
            runCatching { writeCrash(e) }
            previous?.uncaughtException(t, e)
        }
    }

    /** 追加一条非崩溃说明（如看门狗主动重启），下次同样会以弹窗形式展示。 */
    fun writeNote(text: String) {
        runCatching { file().appendText("———— $text\n") }
    }

    /** 读取上次崩溃记录（无则返回 null）。 */
    fun last(context: Context): String? {
        if (!file().exists()) return null
        return runCatching { file().readText() }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    /** 展示后清除记录，避免重复弹窗。 */
    fun clear(context: Context) {
        runCatching { file().delete() }
    }

    private fun file() = File(App.instance.filesDir, FILE_NAME)

    private fun writeCrash(e: Throwable) {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        file().writeText("崩溃时间: $time\n线程: ${Thread.currentThread().name}\n${sw}")
    }
}
