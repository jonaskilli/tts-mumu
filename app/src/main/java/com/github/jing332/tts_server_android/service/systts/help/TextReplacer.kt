package com.github.jing332.tts_server_android.service.systts.help

import com.github.jing332.database.dbm
import com.github.jing332.database.entities.replace.ReplaceRule
import io.github.oshai.kotlinlogging.KotlinLogging

class TextReplacer {
    companion object {
        const val TAG = "ReplaceHelper"
        val logger = KotlinLogging.logger { this::class.java.name }
    }

    private var map: MutableMap<Int, MutableList<ReplaceRule>> = mutableMapOf()

    fun load() {
        map.clear()
        dbm.replaceRuleDao.allGroupWithReplaceRules().forEach { groupWithRules ->
            if (map[groupWithRules.group.onExecution] == null)
                map[groupWithRules.group.onExecution] = mutableListOf()

            map[groupWithRules.group.onExecution]?.addAll(
                groupWithRules.list.filter { it.isEnabled }
            )
        }
    }

    /**
     * 执行替换
     */
    fun replace(
        text: String,
        onExecution: Int,
    ): String {
        var s = text
        map[onExecution]?.forEach { rule ->
            kotlin.runCatching {
                s = if (rule.isRegex)
                    s.replace(Regex(rule.pattern), rule.replacement)
                else
                    s.replace(rule.pattern, rule.replacement)
            }.onFailure {
                logger.error { "Text replace failed: text=${text}, rule=$rule" }
            }
        }

        return s
    }
}