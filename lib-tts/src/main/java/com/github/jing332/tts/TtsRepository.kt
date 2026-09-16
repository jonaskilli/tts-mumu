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
                    setOf("duihuaA", "duihuaB", "duihua", "括号4")
            }
            .mapNotNull(::configurationFor)
            .toList()

        val result = linkedMapOf<Long, TtsConfiguration>()
        for (group in groups) {
            for (tts in group.list.sortedBy { it.order }) {
                if (!tts.isEnabled) continue
                val dto = tts.config as? TtsConfigurationDTO ?: continue
                val resolved = configurationFor(tts) ?: continue

                // 备用=用户显式标记「作为备用引擎」的条目，按 tagName 认亲。
                // 同性别兜底：target 只是作用域标记（整理类操作不写它），不作硬条件；
                // 优先同规则同 target，其次同规则，避免跨规则误配。
                val standby = standbyConfigs.find {
                    it.speechInfo.tagName == dto.speechRule.tagName &&
                        it.speechInfo.tagRuleId == dto.speechRule.tagRuleId &&
                        it.speechInfo.target == dto.speechRule.target
                } ?: standbyConfigs.find {
                    it.speechInfo.tagName == dto.speechRule.tagName &&
                        it.speechInfo.tagRuleId == dto.speechRule.tagRuleId
                }
                val genderStandby = run {
                    val originalTag = dto.speechRule.tag
                    if (originalTag in setOf("duihuaA", "duihuaB", "duihua", "括号4")) return@run null
                    // 性别兜底：男*→duihuaA、女*→duihuaB；性别未知原投中性 duihua——
                    // 用户 09-12 拍板：duihua 弃用，中性备用改投 duihuaA（tagName=男）；
                    // 用户 09-13 改定：中性兜底改投 括号4（tagName=『对话旁白』，新闻腔旁白女声，
                    // 比男声贴合叙述类文本）。括号4 本职是『』括号发音人，仅借用其配置当中性兜底，
                    // 其同标签备用/失败报错口径不变（不在 isFallbackTag 名单，防自引用见上方排除）
                    val genderTag = when {
                        originalTag.startsWith("男") || originalTag.startsWith("少年") ||
                            originalTag == "特殊男" -> "duihuaA"
                        originalTag.startsWith("女") || originalTag.startsWith("少女") ||
                            originalTag == "特殊女" -> "duihuaB"
                        else -> "括号4"
                    }
                    // 查找口径必须与朗读匹配一致：TextProcessor 选配置只用 tag
                    // （!isStandby && speechInfo.tag == effectiveTag），既不看 target 也不看 tagRuleId。
                    // 而标签整理类操作（resortTags / reassignTagsWithPrefix / reassignNarrationTags /
                    // reassignTagsForAllSubGroups）只写 tag/tagName、不写 target，会产出
                    // 「target=全部 但带 tag」的配置：朗读照常命中，兜底查找硬比 target 则静默落空。
                    // 09-17 实锤：旁白（tag=narration）明明有启用的 括号4 兜底项，standbyConfig 仍为 null，
                    // 重试到上限后直接静音跳过，兜底与备用双双无入口。
                    // 故逐级放宽、命中即止：同规则同 target > 同规则 > 仅 tag。
                    genderFallbackConfigs.find {
                        it.speechInfo.tag == genderTag &&
                            it.speechInfo.tagRuleId == dto.speechRule.tagRuleId &&
                            it.speechInfo.target == dto.speechRule.target
                    } ?: genderFallbackConfigs.find {
                        it.speechInfo.tag == genderTag &&
                            it.speechInfo.tagRuleId == dto.speechRule.tagRuleId
                    } ?: genderFallbackConfigs.find {
                        it.speechInfo.tag == genderTag
                    }
                }
                val isFallbackTag = dto.speechRule.tag in setOf("duihua", "duihuaA", "duihuaB")
                // 显式备用优先，没有显式备用时才轮到性别兜底；并记下是不是兜底来的
                // （日志要区分「备用发音人 / 兜底发音人」，见 TtsConfiguration.standbyIsFallback）
                val explicitStandby = if (isFallbackTag) null else standby
                val effectiveStandby = explicitStandby ?: if (isFallbackTag) null else genderStandby

                result[tts.id] = resolved.copy(
                    standbyConfig = effectiveStandby,
                    standbyIsFallback = explicitStandby == null && effectiveStandby != null,
                )
            }
        }
        return result
    }

    override fun getAllBgm(): List<BgmConfiguration> =
        dbm.systemTtsV2.allEnabled.map { it.config }.filterIsInstance<BgmConfiguration>()
}
