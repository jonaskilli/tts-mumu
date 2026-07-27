package com.github.jing332.tts_server_android.service.forwarder

import android.content.Context
import android.content.Intent
import com.github.jing332.tts_server_android.service.forwarder.system.SysTtsForwarderService

object ForwarderServiceManager {
    fun Context.switchSysTtsForwarder() {
        if (SysTtsForwarderService.isRunning) {
            closeSysTtsForwarder()
        } else {
            startSysTtsForwarder()
        }
    }

    fun Context.startSysTtsForwarder() {
        val intent = Intent(this, SysTtsForwarderService::class.java)
        // Possible crash: ForegroundServiceDidNotStartInTimeException
        // startForegroundServiceCompat(intent)
        startService(intent)

    }

    fun closeSysTtsForwarder() {
        SysTtsForwarderService.instance?.close()
    }
}