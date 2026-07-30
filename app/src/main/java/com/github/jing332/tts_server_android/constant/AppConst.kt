package com.github.jing332.tts_server_android.constant

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.jing332.tts_server_android.BuildConfig
import com.github.jing332.tts_server_android.app
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale

@SuppressLint("SimpleDateFormat")
@Suppress("DEPRECATION")
object AppConst {
    val fileProviderAuthor = BuildConfig.APPLICATION_ID + ".fileprovider"
    
    val localBroadcast: LocalBroadcastManager by lazy { LocalBroadcastManager.getInstance(app as Context) }
    val externalFilesDir: File by lazy { checkNotNull((app as Context).getExternalFilesDir("")) { "getExternalFilesDir() == null" } }
    val externalCacheDir: File by lazy { checkNotNull((app as Context).externalCacheDir) { "externalCacheDir == null" } }

    var isSysTtsLogEnabled = true
    var isServerLogEnabled = false

    @OptIn(ExperimentalSerializationApi::class)
    val jsonBuilder: Json by lazy {
        Json {
            allowStructuredMapKeys = true
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
            explicitNulls = false
        }
    }

    // 第1项: 导出文件用紧凑 JSON, 关闭 prettyPrint。
    // prettyPrint 会让 3-4MB JSON 膨胀 30~60% 且拖慢序列化, 紧凑版体积更小、速度更快。
    // 导出页已改为只预览前 32KB, 显示不再依赖格式; 文件/复制/上传均用紧凑版。
    @OptIn(ExperimentalSerializationApi::class)
    val compactJsonBuilder: Json by lazy {
        Json {
            allowStructuredMapKeys = true
            ignoreUnknownKeys = true
            prettyPrint = false
            isLenient = true
            explicitNulls = false
        }
    }

    val isCnLocale: Boolean
        get() = (app as Context).resources.configuration.locale.language.endsWith("zh")

    val locale: Locale
        get() = (app as Context).resources.configuration.locale

    val localeCode: String
        get() = locale.run { "$language-$country" }

    val appInfo: AppInfo by lazy {
        val appInfo = AppInfo()
        val ctx = app as Context
        try {
            val info: PackageInfo = ctx.packageManager.getPackageInfo(
                ctx.packageName, 
                PackageManager.GET_ACTIVITIES
            )
            appInfo.versionName = info.versionName ?: ""
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                appInfo.versionCode = info.longVersionCode
            } else {
                appInfo.versionCode = info.versionCode.toLong()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        appInfo
    }

    data class AppInfo(
        var versionCode: Long = 0L,
        var versionName: String = "",
    )
}
