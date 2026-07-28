package com.github.jing332.tts_server_android.conf

import android.content.Context
import com.funny.data_saver.core.DataSaverPreferences
import com.funny.data_saver.core.mutableDataSaverStateOf
import com.github.jing332.tts_server_android.app

object PluginConfig {
    private val pref by lazy { DataSaverPreferences((app as Context).getSharedPreferences("plugin", 0)) }

    val textParam by lazy { mutableDataSaverStateOf(pref, key = "sampleText", "示例文本。 Sample text.") }
}
