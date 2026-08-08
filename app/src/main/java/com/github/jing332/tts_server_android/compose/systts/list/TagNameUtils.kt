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
            val ruleData = config.speechRule
            val ruleId = ruleData.tagRuleId
            if (ruleId.isBlank()) continue

            val speechRule = ruleCache.getOrPut(ruleId) {
                runCatching { dbm.speechRuleDao.getByRuleId(ruleId) }.getOrNull()
            } ?: continue

            val newTagName = runCatching {
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
