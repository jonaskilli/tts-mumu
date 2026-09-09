package com.github.jing332.tts

import com.github.jing332.database.dbm
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts.speech.plugin.PluginParameterRoute
import com.github.jing332.tts.speech.plugin.parameterRoute
import com.github.jing332.tts.synthesizer.TtsConfiguration
import com.github.jing332.tts.synthesizer.TtsConfiguration.Companion.toVO

/**
 * Final audio behavior for one persisted TTS configuration.
 *
 * Audio parameters deliberately have only three scopes: plugin × configuration × global.
 * Group and subgroup records are organizational metadata and never participate in playback.
 */
data class ResolvedTtsPlayback(
    val entity: SystemTtsV2,
    val configuration: TtsConfiguration,
    val plugin: Plugin?,
    val pluginRoute: PluginParameterRoute,
) {
    /**
     * Parameters sent to the provider. Plugin providers receive only dimensions they own;
     * local TTS engines own all dimensions and therefore receive the final result directly.
     */
    fun providerParams(text: String, requestTimeout: Long = 0L) =
        com.github.jing332.tts.synthesizer.SystemParams(
            text = text,
            speed = when (configuration.source) {
                is PluginTtsSource -> if (pluginRoute.pluginSpeed) configuration.audioParams.speed else 1f
                else -> configuration.audioParams.speed
            },
            volume = when (configuration.source) {
                is PluginTtsSource -> if (pluginRoute.pluginVolume) configuration.audioParams.volume else 1f
                else -> configuration.audioParams.volume
            },
            pitch = when (configuration.source) {
                is PluginTtsSource -> if (pluginRoute.pluginPitch) configuration.audioParams.pitch else 1f
                else -> configuration.audioParams.pitch
            },
            requestTimeout = requestTimeout,
        )

    /** Parameters for app-side processing after a provider has supplied audio. */
    val localPlaybackParams: AudioParams
        get() = localPlaybackParamsFor(configuration)
}

private fun multiplyParam(pluginValue: Float, configValue: Float, globalValue: Float): Float {
    val plugin = if (pluginValue == AudioParams.FOLLOW) 1f else pluginValue
    val config = if (configValue == AudioParams.FOLLOW) 1f else configValue
    val global = if (globalValue == AudioParams.FOLLOW) 1f else globalValue
    return plugin * config * global
}

fun localPlaybackParamsFor(configuration: TtsConfiguration): AudioParams {
    val source = configuration.source as? PluginTtsSource
        ?: return AudioParams()
    val route = parameterRoute(
        pluginId = source.pluginId,
        legacySpeed = configuration.pluginHandlesSpeed,
        legacyVolume = configuration.pluginHandlesVolume,
        legacyPitch = configuration.pluginHandlesPitch,
    )
    return configuration.audioParams.copy(
        speed = if (route.pluginSpeed) 1f else configuration.audioParams.speed,
        volume = if (route.pluginVolume) 1f else configuration.audioParams.volume,
        pitch = if (route.pluginPitch) 1f else configuration.audioParams.pitch,
    )
}

/**
 * Resolves the exact three-layer parameter set used by normal playback and every preview.
 *
 * [pluginCache] 传入时（getAllTts 批量解析）直接取缓存，避免逐条配置查库；
 * 单条调用（预览/弹窗）不传则轻量单查。
 *
 * 注意：返回的 [ResolvedTtsPlayback.plugin] 是**无 code 的元数据副本**——
 * 09-07 OOM 实锤：此处曾用 SELECT * 全量查插件，5MB+ 插件 JS 撑爆 CursorWindow
 * 导致点击朗读即崩。需要 JS 源码的场景（引擎加载等）请走 pluginDao.getByPluginId。
 */
fun resolveTtsPlayback(
    entity: SystemTtsV2,
    globalParams: AudioParams,
    pluginCache: Map<String, Plugin>? = null,
    pluginParamsOverride: AudioParams? = null,
): ResolvedTtsPlayback? {
    val dto = entity.config as? TtsConfigurationDTO ?: return null
    val plugin = when (val source = dto.source as? PluginTtsSource) {
        null -> null
        else -> pluginCache?.get(source.pluginId) ?: dbm.pluginDao.getMetaByPluginId(source.pluginId)
    }
    // 草稿覆盖（09-10）：预览时插件层草稿未落库，传入则替代 DB 值参与三层乘积
    val pluginParams = pluginParamsOverride ?: plugin?.audioParams ?: AudioParams()
    val finalParams = AudioParams(
        speed = multiplyParam(pluginParams.speed, dto.audioParams.speed, globalParams.speed),
        volume = multiplyParam(pluginParams.volume, dto.audioParams.volume, globalParams.volume),
        pitch = multiplyParam(pluginParams.pitch, dto.audioParams.pitch, globalParams.pitch),
        reverbEnabled = dto.audioParams.reverbEnabled,
    )
    val route = parameterRoute(
        pluginId = plugin?.pluginId.orEmpty(),
        legacySpeed = plugin?.pluginHandlesSpeed == true,
        legacyVolume = plugin?.pluginHandlesVolume == true,
        legacyPitch = plugin?.pluginHandlesPitch == true,
    )
    return ResolvedTtsPlayback(
        entity = entity,
        configuration = dto.toVO().copy(
            audioParams = finalParams,
            tag = entity,
            pluginHandlesSpeed = route.pluginSpeed,
            pluginHandlesVolume = route.pluginVolume,
            pluginHandlesPitch = route.pluginPitch,
        ),
        plugin = plugin,
        pluginRoute = route,
    )
}

/**
 * Resolves the exact three-layer parameter set used by normal playback and every preview.
 */
