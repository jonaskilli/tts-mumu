package com.github.jing332.tts_server_android.conf

import android.content.Context
import com.funny.data_saver.core.DataSaverPreferences
import com.funny.data_saver.core.mutableDataSaverStateOf
import com.github.jing332.tts_server_android.app

object SystemTtsForwarderConfig {
    private val pref by lazy { DataSaverPreferences((app as Context).getSharedPreferences("systts_forwarder", 0)) }

    val port = mutableDataSaverStateOf(
        dataSaverInterface = pref,
        key = "port",
        initialValue = 3331
    )

    val isWakeLockEnabled = mutableDataSaverStateOf(
        dataSaverInterface = pref,
        key = "isWakeLockEnabled",
        initialValue = false
    )

    val isAutoStart = mutableDataSaverStateOf(
        dataSaverInterface = pref,
        key = "isAutoStart",
        initialValue = false
    )
}
