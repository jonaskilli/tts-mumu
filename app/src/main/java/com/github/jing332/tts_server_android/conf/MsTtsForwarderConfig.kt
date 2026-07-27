package com.github.jing332.tts_server_android.conf

import android.content.Context
import com.funny.data_saver.core.DataSaverPreferences
import com.funny.data_saver.core.mutableDataSaverStateOf
import com.github.jing332.tts_server_android.app

object MsTtsForwarderConfig {
    private val pref by lazy { DataSaverPreferences((app as Context).getSharedPreferences("server", 0)) }

    val port by lazy {
        mutableDataSaverStateOf(
            dataSaverInterface = pref,
            key = "port",
            initialValue = 1233
        )
    }

    val isWakeLockEnabled by lazy {
        mutableDataSaverStateOf(
            dataSaverInterface = pref,
            key = "isWakeLockEnabled",
            initialValue = false
        )
    }

    val token by lazy {
        mutableDataSaverStateOf(
            dataSaverInterface = pref,
            key = "token",
            initialValue = "",
        )
    }
}
