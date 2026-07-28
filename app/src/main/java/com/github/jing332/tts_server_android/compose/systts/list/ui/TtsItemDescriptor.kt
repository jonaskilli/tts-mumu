package com.github.jing332.tts_server_android.compose.systts.list.ui

import com.github.jing332.database.entities.systts.IConfiguration
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.TextToSpeechSource

abstract class TtsItemDescriptor<T : TextToSpeechSource>(config: IConfiguration) : ItemDescriptor() {
    val config: TtsConfigurationDTO = config as TtsConfigurationDTO
    @Suppress("UNCHECKED_CAST")
    val source: T = this.config.source as T

    override val tagName: String
        get() = config.speechRule.tagName

    override val standby: Boolean
        get() = config.speechRule.isStandby
}