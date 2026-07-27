package com.github.jing332.tts_server_android.conf

import android.content.Context
import com.funny.data_saver.core.DataSaverPreferences
import com.funny.data_saver.core.mutableDataSaverStateOf
import com.github.jing332.tts_server_android.app
import com.github.jing332.common.utils.FileUtils.readAllText

object DirectUploadConfig {
    private val pref by lazy { DataSaverPreferences((app as Context).getSharedPreferences("direct_link_upload", 0)) }

    val code by lazy {
        mutableDataSaverStateOf(
            pref,
            key = "code",
            initialValue = (app as Context).assets.open("defaultData/direct_link_upload.js").readAllText()
        )
    }
}
