package com.github.jing332.tts_server_android.service.keepalive

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.IBinder
import android.os.PowerManager
import com.github.jing332.common.utils.toast
import com.github.jing332.tts_server_android.R

/**
 * 网络连接保活服务
 * 通过保持网络连接活跃来防止被系统清理
 * 对TTS转发器特别有用，因为转发器需要网络连接
 */
class NetworkKeepAliveService : Service() {

    companion object {
        fun isRunning(): Boolean {
            return instance != null
        }

        fun start(context: Context) {
            val intent = Intent(context, NetworkKeepAliveService::class.java)
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, NetworkKeepAliveService::class.java)
            context.stopService(intent)
        }

        private var instance: NetworkKeepAliveService? = null
    }

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startNetworkKeepAlive()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopNetworkKeepAlive()
        instance = null
    }

    private fun startNetworkKeepAlive() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        // 请求唤醒锁保持CPU运行
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TTS Server:NetworkKeepAlive"
        ).apply {
            setReferenceCounted(false)
            acquire(30 * 60 * 1000L) // 30分钟
        }

        // 注册网络回调以保持网络连接活跃（不需要特殊权限）
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                // 只监听网络状态，不绑定进程（需要CHANGE_NETWORK_STATE权限）
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                // 网络丢失时只记录，不主动请求（避免权限问题）
            }
        }

        try {
            connectivityManager?.registerNetworkCallback(networkRequest, networkCallback!!)
            toast(R.string.network_keep_alive_started)
        } catch (e: Exception) {
            toast("网络保活启动失败: ${e.message}")
            stopSelf()
        }
    }

    private fun stopNetworkKeepAlive() {
        try {
            networkCallback?.let {
                connectivityManager?.unregisterNetworkCallback(it)
            }
            connectivityManager?.bindProcessToNetwork(null)
        } catch (_: Exception) {
        }

        try {
            wakeLock?.release()
        } catch (_: Exception) {
        }

        toast(R.string.network_keep_alive_stopped)
    }
}
