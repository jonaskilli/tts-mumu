package com.github.jing332.tts

import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.BgmConfiguration
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.tts.synthesizer.ITtsRepository
import com.github.jing332.tts.synthesizer.TtsConfiguration
import com.github.jing332.tts.synthesizer.TtsConfiguration.Companion.toVO

/**
 * 计算叠加参数值
 * 规则：0(跟随)视为1.0，最终值 = 插件总参数 × 配置值 × 子分组值 × 分组值 × 全局值
 */
private fun calculateParam(
    pluginValue: Float,
    configValue: Float,
    subGroupValue: Float,
    groupValue: Float,
    globalValue: Float
): Float {
    val pv = if (pluginValue == 0f) 1f else pluginValue
    val cv = if (configValue == 0f) 1f else configValue
    val sv = if (subGroupValue == 0f) 1f else subGroupValue
    val gv = if (groupValue == 0f) 1f else groupValue
    val tv = if (globalValue == 0f) 1f else globalValue
    return pv * cv * sv * gv * tv
}

internal class TtsRepository(
    val context: SynthesizerContext,
) : ITtsRepository {

    override fun init() {

    }

    override fun destroy() {
    }

    override fun getTts(id: Long): TtsConfiguration? {
        val systts = dbm.systemTtsV2.get(id)
        return if (systts.config is TtsConfigurationDTO)
            (systts.config as TtsConfigurationDTO).toVO().copy(tag = systts)
        else
            null
    }


    override fun getAllTts(): Map<Long, TtsConfiguration> {
        val tp = context.cfg.audioParams()
        val groupWithTts = dbm.systemTtsV2.getAllGroupWithTts()
        val map = linkedMapOf<Long, TtsConfiguration>()
        val standbyConfigs =
            groupWithTts.flatMap { it.list }
                .filter { it.isEnabled && (it.config as? TtsConfigurationDTO)?.speechRule?.isStandby == true }
                .map {
                    val config = it.config as TtsConfigurationDTO
                    config.toVO().copy(tag = it)
                }
        for (group in groupWithTts) {
            val gp = group.group.audioParams
            val subGroupMap: Map<String, AudioParams> = group.group.subGroupAudioParamsJson.let { jsonStr ->
                if (jsonStr.isBlank() || jsonStr == "{}") emptyMap()
                else SystemTtsV2.Converters.json.decodeFromString(jsonStr)
            }
            for (tts in group.list.sortedBy { it.order }) {
                if (!tts.isEnabled) continue
                val c = tts.config; if (c !is TtsConfigurationDTO) continue

                val standby = standbyConfigs.find {
                    it.speechInfo.target == c.speechRule.target &&
                            it.speechInfo.tagRuleId == c.speechRule.tagRuleId &&
                            it.speechInfo.tagName == c.speechRule.tagName
                }

                // 获取插件级音频参数（新增）
                val pluginParams = (c.source as? com.github.jing332.database.entities.systts.source.PluginTtsSource)?.let {
                    dbm.pluginDao.getByPluginId(it.pluginId)?.audioParams
                        ?: AudioParams()
                } ?: AudioParams()

                // 子分组参数
                val subGroupParams = subGroupMap[tts.categoryPath] ?: AudioParams()

                // 叠加计算：插件总参数 × 配置值 × 子分组值 × 分组值 × 全局值
                val configParams = c.audioParams
                val finalSpeed = calculateParam(pluginParams.speed, configParams.speed, subGroupParams.speed, gp.speed, tp.speed)
                val finalVolume = calculateParam(pluginParams.volume, configParams.volume, subGroupParams.volume, gp.volume, tp.volume)
                val finalPitch = calculateParam(pluginParams.pitch, configParams.pitch, subGroupParams.pitch, gp.pitch, tp.pitch)

                map[tts.id] = TtsConfiguration(
                    speechInfo = c.speechRule,
                    audioParams = AudioParams(
                        speed = finalSpeed,
                        volume = finalVolume,
                        pitch = finalPitch
                    ),
                    audioFormat = c.audioFormat,
                    source = c.source,
                    standbyConfig = standby,
                    tag = tts
                )
            }
        }

        return map
    }


    override fun getAllBgm(): List<BgmConfiguration> {
        return dbm.systemTtsV2.allEnabled.map { it.config }.filterIsInstance<BgmConfiguration>()
    }
}