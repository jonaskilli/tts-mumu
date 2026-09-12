package com.github.jing332.tts

import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.BgmConfiguration
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.tts.synthesizer.ITtsRepository
import com.github.jing332.tts.synthesizer.TtsConfiguration

internal class TtsRepository(
    val context: SynthesizerContext,
) : ITtsRepository {

    override fun init() = Unit

    override fun destroy() = Unit

    /**
     * Specified-configuration playback and normal playback resolve exactly the same three scopes:
     * plugin × configuration × global.
     */
    override fun getTts(id: Long): TtsConfiguration? {
        val systts = dbm.systemTtsV2.get(id) ?: return null
        return resolveTtsPlayback(systts, context.cfg.audioParams())?.configuration
    }

    override fun getAllTts(): Map<Long, TtsConfiguration> {
        val globalParams = context.cfg.audioParams()
        val groups = dbm.systemTtsV2.getAllGroupWithTts()
        val allItems = groups.flatMap { it.list }

        // 一次轻量查全部插件元数据（code 为空串）建缓存：消灭逐条配置全量查插件的
        // N+1 与大 code CursorWindow OOM（09-07 崩溃实锤：数千配置 × 5MB 插件 JS）
        val pluginMeta = dbm.pluginDao.getAllMeta().associateBy { it.pluginId }

        // Resolve once for every **enabled** item only (user 09-07: 未启用项永不播放，解析纯属浪费).
        // Standby/rollback configs use the same final three-layer parameters as a direct request,
        // even when they belong to another group.
        val resolvedById = allItems.asSequence()
            .filter { it.isEnabled }
            .associate { item ->
                item.id to resolveTtsPlayback(item, globalParams, pluginMeta)
            }
        fun configurationFor(item: SystemTtsV2): TtsConfiguration? = resolvedById[item.id]?.configuration

        val standbyConfigs = allItems
            .asSequence()
            .filter { it.isEnabled }
            .filter { (it.config as? TtsConfigurationDTO)?.speechRule?.isStandby == true }
            .mapNotNull(::configurationFor)
            .toList()

        // Gender fallback configs are separate from the normal standby switch but use the same
        // final parameters and source routing as their own persisted target configuration.
        val genderFallbackConfigs = allItems
            .asSequence()
            .filter { it.isEnabled }
            .filter {
                (it.config as? TtsConfigurationDTO)?.speechRule?.tag in
                    setOf("duihuaA", "duihuaB", "duihua")
            }
            .mapNotNull(::configurationFor)
            .toList()

        val result = linkedMapOf<Long, TtsConfiguration>()
        for (group in groups) {
            for (tts in group.list.sortedBy { it.order }) {
                if (!tts.isEnabled) continue
                val dto = tts.config as? TtsConfigurationDTO ?: continue
                val resolved = configurationFor(tts) ?: continue

                val standby = standbyConfigs.find {
                    it.speechInfo.target == dto.speechRule.target &&
                        it.speechInfo.tagRuleId == dto.speechRule.tagRuleId &&
                        it.speechInfo.tagName == dto.speechRule.tagName
                }
                val genderStandby = run {
                    val originalTag = dto.speechRule.tag
                    if (originalTag in setOf("duihuaA", "duihuaB", "duihua")) return@run null
                    // 性别兜底：男*→duihuaA、女*→duihuaB；性别未知原投中性 duihua——
                    // 用户 09-12 拍板：duihua 逐步弃用，中性备用改投 duihuaA（tagName=男）；
                    // 找不到 duihuaA 配置时回落 duihua，以前只用 duihua 的老用户不断声
                    val genderTag = when {
                        originalTag.startsWith("男") || originalTag.startsWith("少年") ||
                            originalTag == "特殊男" -> "duihuaA"
                        originalTag.startsWith("女") || originalTag.startsWith("少女") ||
                            originalTag == "特殊女" -> "duihuaB"
                        else -> "duihuaA"
                    }
                    genderFallbackConfigs.find {
                        it.speechInfo.target == dto.speechRule.target &&
                            it.speechInfo.tagRuleId == dto.speechRule.tagRuleId &&
                            it.speechInfo.tag == genderTag
                    } ?: genderFallbackConfigs.find {
                        it.speechInfo.target == dto.speechRule.target &&
                            it.speechInfo.tagRuleId == dto.speechRule.tagRuleId &&
                            it.speechInfo.tag == "duihua"
                    }
                }
                val isFallbackTag = dto.speechRule.tag in setOf("duihua", "duihuaA", "duihuaB")
                val effectiveStandby = if (isFallbackTag) null else standby ?: genderStandby

                result[tts.id] = resolved.copy(standbyConfig = effectiveStandby)
            }
        }
        return result
    }

    override fun getAllBgm(): List<BgmConfiguration> =
        dbm.systemTtsV2.allEnabled.map { it.config }.filterIsInstance<BgmConfiguration>()
}
