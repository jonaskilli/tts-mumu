package com.github.jing332.tts.speech.plugin

/**
 * Determines which side owns each audio parameter.
 * Formerly carried a hand-maintained known-plugin table (only entry: qianwen volume,
 * removed 09-10 by user decision — host-side processing is uniformly trusted now);
 * legacy persisted flags remain the sole source and are all false in practice,
 * so every dimension is host-owned and always user-adjustable.
 */
data class PluginParameterRoute(
    val pluginSpeed: Boolean,
    val pluginVolume: Boolean,
    val pluginPitch: Boolean,
)

fun parameterRoute(
    pluginId: String,
    legacySpeed: Boolean,
    legacyVolume: Boolean,
    legacyPitch: Boolean,
): PluginParameterRoute = PluginParameterRoute(
    pluginSpeed = legacySpeed,
    pluginVolume = legacyVolume,
    pluginPitch = legacyPitch,
)
