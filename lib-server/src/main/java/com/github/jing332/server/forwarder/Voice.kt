package com.github.jing332.server.forwarder

import kotlinx.serialization.Serializable

@Serializable
data class Voice(
    val name: String,
    val locale: String,
    val localeName: String,
    val features: List<String>? = null
)