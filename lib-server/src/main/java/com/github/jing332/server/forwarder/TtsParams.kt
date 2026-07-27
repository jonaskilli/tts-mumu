package com.github.jing332.server.forwarder

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class TtsParams(
    val text: String,
    val engine: String = "",
    val locale: String = "",
    val voice: String = "",
    val speed: Int = 50,
    val pitch: Int = 100,
)