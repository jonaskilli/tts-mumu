package com.github.jing332.tts_server_android.compose.systts.list

import android.content.Context
import android.util.Log
import com.github.jing332.common.utils.StringUtils
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.database.entities.systts.SpeechRuleInfo
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.tts_server_android.conf.AppConfig
import com.github.jing332.tts_server_android.model.rhino.speech_rule.SpeechRuleEngine

/**
 * 统一计算 tagName（供「一键分配/重排」与「批量分配标签」对话框共用）：
 * 1. 规则能算出名字则用（与朗读规则编辑器一致）；
 * 2. 算不出时回退到规则自身的 tags 映射(带 ⚠ 提示)。
 * 必须在 IO 线程调用。
 *
 * 第12项性能优化: 传入 engineCache 后, 同一 ruleId 的 JS 引擎只编译执行一次,
 * 后续调用直接复用已 eval 的引擎调用 getTagName, 避免逐条重新编译 JS 造成卡顿。
 */
suspend fun computeTagName(
    context: Context,
    speechRule: SpeechRule?,
    ruleData: SpeechRuleInfo,
    fallback: String,
    engineCache: MutableMap<String, SpeechRuleEngine>? = null,
): String {
    // 规则外标签（jread 未映射的性格/群杂等）：JS getTagName 对表外标签一律兜底「旁白」，
    // 会把未映射项显示名误标成旁白——跳过 JS，原样返回 fallback
    if (speechRule != null && speechRule.tags[ruleData.tag] == null) return fallback
    val computed = if (speechRule != null) {
        runCatching {
            if (engineCache != null) {
                // 复用已编译引擎: 同 ruleId 只 eval 一次
                val engine = engineCache.getOrPut(speechRule.ruleId) {
                    SpeechRuleEngine(context, speechRule).also { it.eval() }
                }
                engine.getTagName(ruleData.tag, ruleData.tagData)
            } else {
                SpeechRuleEngine.getTagName(context, speechRule, ruleData)
            }
        }.getOrNull()
    } else null
    // 与朗读规则编辑器(SpeechRuleEditScreen)保持一致：
    // 规则能算出名字则用；算不出时回退到规则自身的 tags 映射(带 ⚠ 提示)，
    // 保证界面显示名始终与朗读规则一致。
    val base = computed?.takeIf { it.isNotBlank() }
        ?: if (speechRule != null) {
            StringUtils.WARNING_EMOJI + (speechRule.tags[ruleData.tag] ?: fallback)
        } else {
            fallback
        }
    return base
}

private const val TAG_MIGRATE = "TagNameMigrator"

/**
 * tagName 迁移：用 getTagName 重算所有配置项的 tagName，并从 tagData 中删除废弃的 personality 字段。
 * - [force]=false（默认）：仅在 AppConfig.tagNameMigrated=false 时执行，完成后置位。
 * - [force]=true：忽略标记强制执行（用于导入旧配置后重算），不重置标记。
 * 已是新格式的项重算结果与当前值一致，不会重复写 DB。必须在 IO 线程调用。
 */
suspend fun migrateTagNamesIfNeed(context: Context, force: Boolean = false) {
    if (!force && AppConfig.tagNameMigrated.value) return

    val updated = mutableListOf<SystemTtsV2>()
    runCatching {
        val ruleCache = mutableMapOf<String, SpeechRule?>()
        val engineCache = mutableMapOf<String, SpeechRuleEngine>()

        for (systts in dbm.systemTtsV2.all) {
            val config = systts.config as? TtsConfigurationDTO ?: continue
            var ruleData = config.speechRule
            var ruleId = ruleData.tagRuleId
            if (ruleId.isBlank()) {
                // 旧导入（如 jread）有标签但未绑定规则：回填当前启用规则并继续走正常重算，
                // 否则编辑页会提示「该配置项未绑定朗读规则，无法切换标签」
                val enabled = if (ruleData.tag.isNotBlank()) {
                    dbm.speechRuleDao.getAllEnabledWithoutCode().firstOrNull()
                } else null
                if (enabled != null) {
                    ruleData = ruleData.copy(tagRuleId = enabled.ruleId)
                    ruleId = enabled.ruleId
                    updated.add(
                        systts.copy(config = config.copy(speechRule = ruleData))
                    )
                } else {
                    // 未关联朗读规则：无法用 JS 算出显示名，则把已分配的原始 tag 直接作为显示名（如 jread 导入的女青年01）
                    if (ruleData.tag.isNotBlank() && ruleData.tagName.isBlank()) {
                        updated.add(
                            systts.copy(
                                config = config.copy(speechRule = ruleData.copy(tagName = ruleData.tag))
                            )
                        )
                    }
                    continue
                }
            }

            val speechRule = ruleCache.getOrPut(ruleId) {
                runCatching { dbm.speechRuleDao.getByRuleId(ruleId) }.getOrNull()
            } ?: continue

            // 规则外标签（jread 未映射的性格/群杂等）：JS getTagName 会兜底「旁白」，
            // 已被误标的项在此修复——显示名强制回写原始 tag
            val newTagName = if (speechRule.tags[ruleData.tag] == null) {
                ruleData.tag
            } else runCatching {
                val engine = engineCache.getOrPut(ruleId) {
                    SpeechRuleEngine(context, speechRule).also { it.eval() }
                }
                engine.getTagName(ruleData.tag, ruleData.tagData)
            }.getOrNull().orEmpty()

            val newTagData = ruleData.tagData.filterKeys { it != "personality" }
            val tagDataChanged = newTagData.size != ruleData.tagData.size
            val tagNameChanged = newTagName.isNotBlank() && newTagName != ruleData.tagName

            if (tagNameChanged || tagDataChanged) {
                val finalTagName = if (newTagName.isBlank()) ruleData.tagName else newTagName
                val newRule = ruleData.copy(tagName = finalTagName, tagData = newTagData)
                updated.add(systts.copy(config = config.copy(speechRule = newRule)))
            }
        }
    }.onFailure {
        Log.e(TAG_MIGRATE, "tagName 迁移失败", it)
    }

    // 非强制模式才置位（强制模式不改变标记状态）
    if (!force) {
        AppConfig.tagNameMigrated.value = true
    }

    if (updated.isNotEmpty()) {
        dbm.systemTtsV2.update(*updated.toTypedArray())
        Log.d(TAG_MIGRATE, "tagName 迁移完成: 更新 ${updated.size} 项" + if (force) "(强制)" else "")
    }
}

/**
 * 标签扩容：扫描配置列表里所有 tag，按前缀分组找最大序号，
 * 若超过朗读规则 tags 里的基础数量，补齐缺失标签到 tags 并写回数据库。
 * 数据源用所有配置项（不限启用），确保未启用的标签也能触发扩容。
 *
 * 供工具箱页、标签切换弹窗、批量标签弹窗共用，确保点标签时列表覆盖全部序号。
 */
internal fun expandSpeechRuleTagsIfNeeded(
    rule: SpeechRule,
    allTtsList: List<SystemTtsV2>,
) {
    val tagRegex = Regex("^(.+?)(\\d+)$")
    val configMaxSeq = mutableMapOf<String, Int>()
    allTtsList.forEach { tts ->
        val tag = (tts.config as? TtsConfigurationDTO)?.speechRule?.tag ?: return@forEach
        val match = tagRegex.matchEntire(tag) ?: return@forEach
        val prefix = match.groupValues[1]
        val seq = match.groupValues[2].toIntOrNull() ?: return@forEach
        configMaxSeq[prefix] = maxOf(configMaxSeq[prefix] ?: 0, seq)
    }
    if (configMaxSeq.isEmpty()) return

    val ruleMaxSeq = mutableMapOf<String, Int>()
    rule.tags.keys.forEach { key ->
        val match = tagRegex.matchEntire(key) ?: return@forEach
        val prefix = match.groupValues[1]
        val seq = match.groupValues[2].toIntOrNull() ?: return@forEach
        ruleMaxSeq[prefix] = maxOf(ruleMaxSeq[prefix] ?: 0, seq)
    }

    var changed = false
    val newTags = rule.tags.toMutableMap()
    configMaxSeq.forEach { (prefix, needMax) ->
        val curMax = ruleMaxSeq[prefix] ?: 0
        if (needMax > curMax) {
            val sampleKey = rule.tags.keys.firstOrNull { it.startsWith(prefix) }
            val sampleValue = sampleKey?.let { rule.tags[it] } ?: ""
            for (i in (curMax + 1)..needMax) {
                val seqStr = String.format("%02d", i)
                val newKey = prefix + seqStr
                if (!newTags.containsKey(newKey)) {
                    val newValue = if (sampleValue.isNotEmpty()) {
                        sampleValue.replace(Regex("\\d+"), seqStr)
                    } else {
                        newKey
                    }
                    newTags[newKey] = newValue
                    changed = true
                }
            }
        }
    }

    if (changed) {
        rule.tags = newTags
        dbm.speechRuleDao.update(rule)
    }
}
