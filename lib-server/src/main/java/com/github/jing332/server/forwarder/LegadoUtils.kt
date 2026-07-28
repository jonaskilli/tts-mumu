package com.github.jing332.server.forwarder

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class LegadoJson(
    @SerialName("contentType") val contentType: String,
    @SerialName("header") val header: String,
    @SerialName("id") val id: Long = System.currentTimeMillis(),
    @SerialName("lastUpdateTime") val lastUpdateTime: Long = System.currentTimeMillis(),
    @SerialName("name") val name: String,
    @SerialName("url") val url: String,
    @SerialName("concurrentRate") val concurrentRate: String,
)

object LegadoUtils {
    fun getLegadoJson(
        api: String,
        displayName: String,
        engine: String,
        voice: String,
        pitch: String,
    ): LegadoJson {
        val url = "$api?engine=$engine&text={{java.encodeURI(speakText)}}&rate={{speakSpeed * 2}}" +
                "&pitch=$pitch&voice=$voice"

        val data = LegadoJson(
            name = displayName,
            url = url,
            contentType = "audio/x-wav",
            concurrentRate = "100",
            header = ""
        )
        return data
    }
}