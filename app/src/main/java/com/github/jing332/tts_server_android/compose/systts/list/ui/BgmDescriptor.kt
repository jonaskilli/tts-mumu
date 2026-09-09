package com.github.jing332.tts_server_android.compose.systts.list.ui

import android.content.Context
import com.github.jing332.common.utils.toParamText
import com.github.jing332.database.entities.systts.BgmConfiguration
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.tts_server_android.R

data class BgmDescriptor(val context: Context, val systemTts: SystemTtsV2) : ItemDescriptor() {
    private val config = systemTts.config as BgmConfiguration

    override val tagName: String = ""
    override val name: String
        get() = systemTts.displayName

    // 参数行（用户 09-10 C 案）：BGM 不参与三层 audioParams 体系，卡片只显示自己的音量；
    // 写法与插件/本地卡片同款——「音量1.0」不带冒号、不加粗、按实际精度（toParamText）
    override val desc: String
        get() = context.getString(R.string.bgm_card_volume, config.volume.toParamText())

    override val type: String
        get() = "BGM"

    override val bottom: String
        get() = context.getString(R.string.total_n_folders, config.musicList.size.toString())


}