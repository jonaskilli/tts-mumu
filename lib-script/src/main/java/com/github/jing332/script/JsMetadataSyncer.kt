package com.github.jing332.script

/**
 * 第11项: JS 对象字面量元数据同步工具。
 *
 * 用于在列表页内联编辑 name / id / author / version 等字段后，把新值回写到
 * JS 源码里对应的字面量，保证下次 eval 求值回填时与实体字段一致。
 *
 * 修复: 只匹配顶层元数据对象(PluginJS / SpeechRuleJS)内的字段，
 * 而不是全局第一个匹配——避免误伤代码中其他同名对象字面量(如 name: "未知")。
 */
object JsMetadataSyncer {

    /**
     * 支持的顶层元数据对象名（插件 / 朗读规则）。
     * 优先匹配 `var X = {` 声明形式。
     */
    private val TOP_LEVEL_OBJECTS = listOf("PluginJS", "SpeechRuleJS")

    /**
     * 查找目标对象字面量的字段区间: 返回 [start, end)，end 为该对象第一个闭合大括号之后。
     * 找不到声明时返回 null。
     */
    private fun findObjectRange(code: String): IntRange? {
        for (obj in TOP_LEVEL_OBJECTS) {
            val decl = Regex("""var\s+$obj\s*=\s*\{""").find(code) ?: continue
            val start = decl.range.last + 1 // 跳过 '{'
            // 从 start 开始扫描, 用深度计数找到配对的 '}'
            var depth = 1
            var i = start
            var inString: Char? = null
            while (i < code.length) {
                val c = code[i]
                if (inString != null) {
                    if (c == '\\') { i += 2; continue }
                    if (c == inString) inString = null
                } else {
                    when (c) {
                        '"', '\'' -> inString = c
                        '{' -> depth++
                        '}' -> {
                            depth--
                            if (depth == 0) return IntRange(decl.range.first, i)
                        }
                    }
                }
                i++
            }
        }
        return null
    }

    /**
     * 更新 JS 中字符串字段（如 name: "xxx" / author: 'yyy'）。
     * 新值统一用双引号包裹，内部双引号与反斜杠自动转义。
     *
     * 仅在该字段所属的顶层元数据对象内查找并替换第一个匹配项。
     * 支持带引号 key ("field": / 'field':) 和不带引号 key (field:)。
     * 使用 \b 词边界防止 "id" 误匹配 "ruleId" / "pluginId" 等子串。
     */
    fun updateStringField(code: String, field: String, newValue: String): String {
        val escaped = newValue.replace("\\", "\\\\").replace("\"", "\\\"")
        val keyPattern = """(?:"$field"|'$field'|\b$field)"""
        val pattern = Regex("""($keyPattern\s*:\s*)(?:"[^"\\]*(?:\\.[^"\\]*)*"|'[^'\\]*(?:\\.[^'\\])*')""", RegexOption.IGNORE_CASE)
        val range = findObjectRange(code) ?: return code
        val match = pattern.find(code, range.first) ?: return code
        if (match.range.last > range.last) return code // 匹配超出对象范围则不替换
        return code.replaceRange(match.range, "${match.groupValues[1]}\"$escaped\"")
    }

    /**
     * 更新 JS 中整数字段（如 version: 3）。
     *
     * 同样限定在顶层元数据对象内查找, 防止误匹配。
     */
    fun updateIntField(code: String, field: String, newValue: Int): String {
        val keyPattern = """(?:"$field"|'$field'|\b$field)"""
        val pattern = Regex("""($keyPattern\s*:\s*)\d+""", RegexOption.IGNORE_CASE)
        val range = findObjectRange(code) ?: return code
        val match = pattern.find(code, range.first) ?: return code
        if (match.range.last > range.last) return code // 匹配超出对象范围则不替换
        return code.replaceRange(match.range, "${match.groupValues[1]}$newValue")
    }
}
