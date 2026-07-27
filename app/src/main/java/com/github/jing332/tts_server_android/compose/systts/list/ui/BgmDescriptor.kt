package com.github.jing332.tts_server_android.compose.systts.list.ui

import android.content.Context
import com.github.jing332.database.entities.systts.BgmConfiguration
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.tts_server_android.R

data class BgmDescriptor(val context: Context, val systemTts: SystemTtsV2) : ItemDescriptor() {
    private val config = systemTts.config as BgmConfiguration

    override val tagName: String = ""
    override val name: String
        get() = systemTts.displayName

    override val desc: String
        get() = context.getString(R.string.label_speech_volume, config.volume.toString())

    override val type: String
        get() = "BGM"

    override val bottom: String
        get() = context.getString(R.string.total_n_folders, config.musicList.size.toString())


}