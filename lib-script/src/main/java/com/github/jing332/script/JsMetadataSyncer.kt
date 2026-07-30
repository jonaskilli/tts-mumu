package com.github.jing332.script

/**
 * 第11项: JS 对象字面量元数据同步工具。
 *
 * 用于在列表页内联编辑 name / id / author / version 等字段后，把新值回写到
 * JS 源码里对应的字面量，保证下次 eval 求值回填时与实体字段一致。
 *
 * 仅替换第一个匹配项，避免误伤代码中同名的其他标识符。
 */
object JsMetadataSyncer {

    /**
     * 更新 JS 中字符串字段（如 name: "xxx" / author: 'yyy'）。
     * 新值统一用双引号包裹，内部双引号与反斜杠自动转义。
     */
    fun updateStringField(code: String, field: String, newValue: String): String {
        val escaped = newValue.replace("\\", "\\\\").replace("\"", "\\\"")
        // group1 = "field: "（字段名+冒号+空白），后面跟双引号或单引号字符串字面量
        val pattern = Regex("""($field\s*:\s*)(?:"[^"\\]*(?:\\.[^"\\]*)*"|'[^'\\]*(?:\\.[^'\\])*')""", RegexOption.IGNORE_CASE)
        val match = pattern.find(code) ?: return code
        return code.replaceRange(match.range, "${match.groupValues[1]}\"$escaped\"")
    }

    /**
     * 更新 JS 中整数字段（如 version: 3）。
     */
    fun updateIntField(code: String, field: String, newValue: Int): String {
        val pattern = Regex("""($field\s*:\s*)\d+""", RegexOption.IGNORE_CASE)
        val match = pattern.find(code) ?: return code
        return code.replaceRange(match.range, "${match.groupValues[1]}$newValue")
    }
}
