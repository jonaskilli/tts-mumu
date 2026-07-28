package com.github.jing332.database.entities.systts.source

import androidx.annotation.Keep
import com.github.jing332.database.entities.plugin.Plugin
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Keep
@Serializable
@SerialName("plugin")
data class PluginTtsSource(
    override val locale: String = "",
    override val voice: String = "",
    val pluginId: String = "",
    val speed: Float = 1f,
    val volume: Float = 1f,
    val pitch: Float = 1f,
    val data: Map<String, String> = mutableMapOf(),

    @Transient
    @IgnoredOnParcel
    val plugin: Plugin? = null,
) : TextToSpeechSource() {
    companion object{
        const val SPEED_FOLLOW = 0f
        const val PITCH_FOLLOW = 0f
        const val VOLUME_FOLLOW = 0f
    }



    override fun getKey(): String {
        // 防止 CachedEngineManager 创建单例 Engine
        return pluginId
    }
}