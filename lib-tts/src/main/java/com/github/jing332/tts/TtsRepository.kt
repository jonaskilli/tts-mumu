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


    // 带插件处理标志的 toVO：备用/兜底配置构造时同步插件表的 pluginHandlesXxx
    private fun TtsConfigurationDTO.toVOWithPluginFlags(): TtsConfiguration {
        val pluginRecord = (source as? com.github.jing332.database.entities.systts.source.PluginTtsSource)?.let {
            dbm.pluginDao.getByPluginId(it.pluginId)
        }
        return toVO().copy(
            pluginHandlesSpeed = pluginRecord?.pluginHandlesSpeed == true,
            pluginHandlesVolume = pluginRecord?.pluginHandlesVolume == true,
            pluginHandlesPitch = pluginRecord?.pluginHandlesPitch == true
        )
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
                    config.toVOWithPluginFlags().copy(tag = it)
                }

        // 性别兜底配置池：duihuaA(男)/duihuaB(女)/duihua(中性)三个标签的配置，不依赖 isStandby 标记
        // 发音人获取失败时，按原tag性别回退到这里，无需用户额外标记为备用
        val genderFallbackConfigs =
            groupWithTts.flatMap { it.list }
                .filter {
                    it.isEnabled && (it.config as? TtsConfigurationDTO)?.speechRule?.tag in
                        listOf("duihuaA", "duihuaB", "duihua")
                }
                .map {
                    val config = it.config as TtsConfigurationDTO
                    config.toVOWithPluginFlags().copy(tag = it)
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
                // 性别兜底：发音人获取失败时，按原tag的性别特征回退到 duihuaA(男)/duihuaB(女)/duihua(中性)
                // 兜底标签自身跳过此层，避免自引用；直接用三个标签的配置，无需 isStandby 标记
                val genderStandby = run {
                    val origTag = c.speechRule.tag
                    if (origTag == "duihuaA" || origTag == "duihuaB" || origTag == "duihua") return@run null
                    val genderTag = when {
                        origTag.startsWith("男") || origTag.startsWith("少年") || origTag == "特殊男" -> "duihuaA"
                        origTag.startsWith("女") || origTag.startsWith("少女") || origTag == "特殊女" -> "duihuaB"
                        else -> "duihua"
                    }
                    genderFallbackConfigs.find {
                        it.speechInfo.target == c.speechRule.target &&
                                it.speechInfo.tagRuleId == c.speechRule.tagRuleId &&
                                it.speechInfo.tag == genderTag
                    }
                }
                // 同标签备用 > 性别兜底；兜底发音人(duihua/duihuaA/duihuaB)不挂任何备用，失败直接报错
                val isFallbackTag = c.speechRule.tag in listOf("duihua", "duihuaA", "duihuaB")
                val effectiveStandby = when {
                    isFallbackTag -> null
                    standby != null -> standby
                    genderStandby != null -> genderStandby
                    else -> null
                }

                // 获取插件级音频参数（新增）
                val pluginRecord = (c.source as? com.github.jing332.database.entities.systts.source.PluginTtsSource)?.let {
                    dbm.pluginDao.getByPluginId(it.pluginId)
                }
                val pluginParams = pluginRecord?.audioParams ?: AudioParams()

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
                    pluginHandlesSpeed = pluginRecord?.pluginHandlesSpeed == true,
                    pluginHandlesVolume = pluginRecord?.pluginHandlesVolume == true,
                    pluginHandlesPitch = pluginRecord?.pluginHandlesPitch == true,
                    standbyConfig = effectiveStandby,
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