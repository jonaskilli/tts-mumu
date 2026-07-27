package com.github.jing332.database.entities.systts.source

import com.github.jing332.database.entities.systts.BasicAudioFormat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("local")
data class LocalTtsSource(
    val engine: String = "",
    override val locale: String = "",
    override val voice: String = "",

    val extraParams: MutableList<LocalTtsParameter>? = null,
    val isDirectPlayMode: Boolean = true,
) : TextToSpeechSource() {

    override fun getKey(): String = engine

    override fun isSyncPlayMode(): Boolean = isDirectPlayMode
    override fun shouldDecode(format: BasicAudioFormat): Boolean {
        return if (!isDirectPlayMode)
            false
        else
            super.shouldDecode(format)
    }
}