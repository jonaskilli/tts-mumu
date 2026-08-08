package com.github.jing332.tts_server_android.compose.systts.list

import android.content.Context
import com.github.jing332.common.utils.StringUtils
import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.database.entities.systts.SpeechRuleInfo
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
