package com.github.jing332.tts.speech.plugin

/**
 * Determines which side owns each audio parameter.
 * Known plugin behavior takes precedence; legacy persisted flags remain a fallback.
 * Unknown plugins stay local so a user adjustment cannot silently disappear.
 */
internal data class PluginParameterRoute(
    val pluginSpeed: Boolean,
    val pluginVolume: Boolean,
    val pluginPitch: Boolean,
)

internal fun parameterRoute(
    pluginId: String,
    legacySpeed: Boolean,
    legacyVolume: Boolean,
    legacyPitch: Boolean,
): PluginParameterRoute {
    return when (pluginId.trim().lowercase()) {
        // JRead Qianwen adapter locks provider speed and does not consume numeric pitch.
        "qianwen.tts.guagua_taozi" -> PluginParameterRoute(
            pluginSpeed = false,
            pluginVolume = true,
            pluginPitch = false,
        )
        else -> PluginParameterRoute(
            pluginSpeed = legacySpeed,
            pluginVolume = legacyVolume,
            pluginPitch = legacyPitch,
        )
    }
}
