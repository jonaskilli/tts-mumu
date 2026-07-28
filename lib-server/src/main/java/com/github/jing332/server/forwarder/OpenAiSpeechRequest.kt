package com.github.jing332.server.forwarder

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class OpenAiSpeechRequest(
    val model: String = "",
    val input: String,
    val voice: String = "",
    val response_format: String = "mp3",
    val speed: Double = 1.0,
)
