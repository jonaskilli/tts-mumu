package com.github.jing332.tts_server_android.compose.codeeditor

import io.github.rosemoe.sora.widget.CodeEditor

/**
 * 函数匹配器：根据函数签名（第一行）在代码中查找同名函数块，支持替换
 *
 * 用于编辑器搜索栏的"函数匹配模式"：当搜索框输入多行代码（完整函数）时，
 * 只匹配第一行（函数签名），找到同名函数后可整体替换。
 */
class FunctionMatcher(private val editor: CodeEditor) {

    /**
     * 函数块：起始行和结束行（0-based，含）
     */
    data class FunctionBlock(val startLine: Int, val endLine: Int)

    /**
     * 从粘贴的代码中提取函数签名行（跳过注释行和空行）
     * 例如粘贴的代码前面有好几行注释，第5行才是函数签名，会返回第5行
     * @param code 粘贴的完整代码
     * @return 函数签名行（trim 后），找不到返回空字符串
     */
    fun extractFunctionSignature(code: String): String {
        for (line in code.lineSequence()) {
            val trimmed = line.trim()
            // 跳过空行、单行注释(// 或 /* */ 或 *)、块注释开头/结尾
            if (trimmed.isEmpty()) continue
            if (trimmed.startsWith("//")) continue
            if (trimmed.startsWith("/*")) continue
            if (trimmed.startsWith("*")) continue
            if (trimmed.endsWith("*/")) continue
            // 找到第一个非注释非空行，作为函数签名
            return trimmed
        }
        return ""
    }

    /**
     * 查找所有函数签名匹配的函数块（只匹配最外层，跳过嵌套在已匹配块内部的同名函数）
     *
     * @param signature 函数签名（trim 后比较）
     * @return 匹配的函数块列表（互不嵌套）
     */
    fun findFunctions(signature: String): List<FunctionBlock> {
        val text = editor.text
        val target = signature.trim()
        if (target.isEmpty()) return emptyList()

        val results = mutableListOf<FunctionBlock>()
        var lineIndex = 0
        while (lineIndex < text.lineCount) {
            val line = text.getLine(lineIndex).toString().trim()
            if (line == target) {
                val endLine = findBlockEnd(lineIndex)
                if (endLine > lineIndex) {
                    results.add(FunctionBlock(lineIndex, endLine))
                    // 跳过整个函数块，避免匹配到内部嵌套的同名函数
                    lineIndex = endLine + 1
                    continue
                }
            }
            lineIndex++
        }
        return results
    }

    /**
     * 从指定行开始，基于括号匹配找到代码块结束行
     * 支持 {} 函数块 和 [] 数组块，自动检测首个开括号类型进行配对
     */
    private fun findBlockEnd(startLine: Int): Int {
        val text = editor.text
        // 栈结构：记录每个开括号的类型，支持嵌套（如函数体内含数组）
        val stack = ArrayDeque<Char>()
        var openType: Char? = null  // 首个开括号类型（决定配对的闭括号）

        for (lineIndex in startLine until text.lineCount) {
            val line = text.getLine(lineIndex).toString()
            var inString = false
            var stringChar: Char = ' '

            for (c in line) {
                when {
                    inString -> {
                        if (c == stringChar) inString = false
                    }
                    c == '"' || c == '\'' -> {
                        inString = true
                        stringChar = c
                    }
                    c == '/' -> { /* 简易处理，不识别块注释 */ }
                    c == '{' || c == '[' -> {
                        stack.addLast(c)
                        if (openType == null) openType = c
                    }
                    c == '}' || c == ']' -> {
                        val openChar = if (c == '}') '{' else '['
                        // 弹出直到匹配到对应类型的开括号
                        while (stack.isNotEmpty() && stack.last() != openChar) {
                            stack.removeLast()
                        }
                        if (stack.isNotEmpty()) {
                            stack.removeLast()
                            // 栈空且首个开括号已匹配，说明代码块结束
                            if (stack.isEmpty() && openType != null) {
                                val expectedClose = if (openType == '{') '}' else ']'
                                if (c == expectedClose) {
                                    return lineIndex
                                }
                            }
                        }
                    }
                }
            }
        }
        return -1
    }

    /**
     * 替换指定函数块为新代码，自动调整缩进以匹配原代码
     *
     * @param startLine 起始行（0-based，含）
     * @param endLine 结束行（0-based，含）
     * @param newCode 新的函数代码
     */
    fun replaceFunction(startLine: Int, endLine: Int, newCode: String) {
        val text = editor.text
        // 获取原函数首行的缩进（前导空格/Tab）
        val originalFirstLine = text.getLine(startLine).toString()
        val originalIndent = originalFirstLine.takeWhile { it == ' ' || it == '\t' }

        // 获取新代码首行的缩进
        val newFirstLine = newCode.substringBefore("\n")
        val newIndent = newFirstLine.takeWhile { it == ' ' || it == '\t' }

        // 按行调整缩进：每行去掉新代码原有的缩进，加上原代码的缩进
        val adjustedCode = newCode.lineSequence().mapIndexed { index, line ->
            if (line.isBlank()) {
                line  // 空行保持不变
            } else {
                // 去掉新代码该行的原有缩进（仅首行用 newIndent，其他行可能缩进更多）
                val stripped = if (index == 0) {
                    line.removePrefix(newIndent)
                } else {
                    // 非首行：去掉与首行相同的基础缩进，保留额外的嵌套缩进
                    if (line.startsWith(newIndent)) line.removePrefix(newIndent) else line.trimStart()
                }
                originalIndent + stripped
            }
        }.joinToString("\n")

        val start = text.getCharIndex(startLine, 0)
        val end = text.getCharIndex(endLine, text.getColumnCount(endLine))
        text.replace(start, end, adjustedCode)
    }
}
