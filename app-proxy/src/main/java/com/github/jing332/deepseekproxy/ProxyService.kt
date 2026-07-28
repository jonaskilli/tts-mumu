package com.github.jing332.deepseekproxy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.github.jing332.deepseekproxy.proxy.DeepSeekPowWebView
import com.github.jing332.deepseekproxy.proxy.LogStore
import com.github.jing332.deepseekproxy.proxy.ProxyServer

/**
 * 前台服务：在后台保活本地中转服务，并通过常驻通知告知用户。
 * 配合通知权限与电池优化豁免，可避免进入后台后被系统回收/限制导致无响应。
 */
class ProxyService : android.app.Service() {
    companion object {
        const val CHANNEL_ID = "proxy_foreground"
        const val NOTIF_ID = 1
        const val EXTRA_PORT = "port"
        const val EXTRA_DOUBAO_COOKIES = "doubao_cookies"
        const val EXTRA_KIMI_COOKIES = "kimi_cookies"
        const val EXTRA_QWEN_COOKIES = "qwen_cookies"
        const val EXTRA_STREAM = "stream"

        /** 混元太极是否处于「已开启」状态（供 App 重启后自动恢复判断）。 */
        fun isSavedRunning(context: Context): Boolean =
            context.getSharedPreferences("ds_proxy", Context.MODE_PRIVATE)
                .getBoolean("running", false)

        /**
         * 按上次保存的配置启动混元太极前台服务（用于 App 重启后自动恢复）。
         * 从 "ds_proxy" 读取端口、各凭证列表、流式与模式，完整还原到 ProxyServer 后拉起服务。
         */
        fun startFromSaved(context: Context) {
            val prefs = context.getSharedPreferences("ds_proxy", Context.MODE_PRIVATE)
            val port = prefs.getInt("port", 8800)
            val doubao = loadList(prefs, "doubao_cookies", "doubao_cookie")
            val kimi = loadList(prefs, "kimi_cookies", "kimi_tokens")
            val qwen = loadList(prefs, "qwen_cookies", "qwen_cookie")
            val deepseek = loadList(prefs, "deepseek_tokens", "deepseek_token")
            val longcat = loadList(prefs, "longcat_configs", "longcat_config")
            val stream = prefs.getBoolean("stream_mode", true)
            val mode = prefs.getString("deepseek_mode", "default") ?: "default"
            val imageSize = prefs.getString("image_size", "9:16") ?: "9:16"

            // 先把配置还原到 ProxyServer（deepseek token 只能经此设置）
            ProxyServer.streamMode = stream
            ProxyServer.defaultImageSize = imageSize
            ProxyServer.deepseekMode = mode
            ProxyServer.setDoubaoCookieList(doubao)
            ProxyServer.setKimiCookieList(kimi)
            ProxyServer.setQwenCookieList(qwen)
            ProxyServer.setDeepSeekTokenList(deepseek)
            ProxyServer.setLongCatConfigList(longcat)

            val intent = Intent(context, ProxyService::class.java).apply {
                putExtra(EXTRA_PORT, port)
                putStringArrayListExtra(EXTRA_DOUBAO_COOKIES, ArrayList(doubao))
                putStringArrayListExtra(EXTRA_KIMI_COOKIES, ArrayList(kimi))
                putStringArrayListExtra(EXTRA_QWEN_COOKIES, ArrayList(qwen))
                putExtra(EXTRA_STREAM, stream)
            }
            ContextCompat.startForegroundService(context, intent)
            LogStore.i("Proxy", "↻ App 重启后自动恢复混元太极服务（端口 $port）")
        }

        /** 与 ProxyViewModel.loadList 一致：读取 JSON 列表，兼容旧的单值 key。 */
        private fun loadList(
            prefs: android.content.SharedPreferences,
            key: String,
            legacyKey: String
        ): List<String> {
            val legacy = prefs.getString(legacyKey, null)
            val raw = prefs.getString(key, null)
            return if (raw != null) {
                try {
                    val arr = org.json.JSONArray(raw)
                    val list = (0 until arr.length()).map { arr.optString(it, "") }
                    if (list.isEmpty()) listOf("") else list
                } catch (_: Exception) {
                    listOf(legacy ?: "")
                }
            } else if (!legacy.isNullOrEmpty()) {
                listOf(legacy)
            } else {
                listOf("")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        // 初始化 DeepSeek PoW：用 App 内 WebView 直接加载官方 wasm 计算签名。
        try {
            val wasmBytes = assets.open("sha3_wasm_bg.wasm").use { it.readBytes() }
            DeepSeekPowWebView.init(this, wasmBytes)
            LogStore.i("DeepSeek", "PoW WebView 初始化完成（wasm ${wasmBytes.size} 字节）")
        } catch (e: Exception) {
            LogStore.e("DeepSeek", "PoW WebView 初始化失败: ${e.stackTraceToString()}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val port = intent?.getIntExtra(EXTRA_PORT, 8800) ?: 8800
        val doubaoCookies = intent?.getStringArrayListExtra(EXTRA_DOUBAO_COOKIES) ?: arrayListOf()
        val kimiCookies = intent?.getStringArrayListExtra(EXTRA_KIMI_COOKIES) ?: arrayListOf()
        val qwenCookies = intent?.getStringArrayListExtra(EXTRA_QWEN_COOKIES) ?: arrayListOf()
        val stream = intent?.getBooleanExtra(EXTRA_STREAM, true) ?: true

        val lcPrefs = getSharedPreferences("ds_proxy", Context.MODE_PRIVATE)
        val longcat = loadList(lcPrefs, "longcat_configs", "longcat_config")
        ProxyServer.setLongCatConfigList(longcat)

        startForeground(NOTIF_ID, buildNotification("服务运行中 · 端口 $port · ${if (stream) "流式" else "非流式"}"))

        ProxyServer.streamMode = stream
        // csrfkey 留空由 CNB 自动获取；豆包 / Kimi / 千问均使用 Cookie，并自动轮换
        ProxyServer.start(
            port, { "" },
            { doubaoCookies }, { kimiCookies }, { qwenCookies }, cacheDir
        )
        LogStore.i("Proxy", "▶ 前台服务已启动，端口 $port，流式=$stream")
        return START_STICKY
    }

    override fun onDestroy() {
        ProxyServer.stop()
        DeepSeekPowWebView.destroy()
        stopForeground(true)
        LogStore.i("Proxy", "■ 前台服务已停止")
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    "混元太极服务",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "保持本地中转服务在后台运行"
                    setShowBadge(false)
                }
                mgr.createNotificationChannel(ch)
            }
        }
    }

    private fun buildNotification(text: String): Notification {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        val pi = PendingIntent.getActivity(
            this, 0, launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("混元太极")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }
}
