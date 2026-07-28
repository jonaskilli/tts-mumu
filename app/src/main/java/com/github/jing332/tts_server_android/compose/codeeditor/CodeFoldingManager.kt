package com.github.jing332.tts_server_android.compose.codeeditor

import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentListener
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

/**
 * 折叠区域的内容指纹标记（与行号无关，用于持久化和恢复折叠状态）
 *
 * @param firstLine 折叠块首行内容（trim 后，用于在代码中定位）
 * @param lastLine  折叠块尾行内容（trim 后，用于验证匹配）
 * @param lineCount 首行到尾行的总行数（含），用于计算尾行位置
 */
@Serializable
data class FoldMark(
    val firstLine: String,
    val lastLine: String,
    val lineCount: Int,
)

/**
 * 代码折叠管理器（去换行折叠方案）
 *
 * 折叠机制：保留块的首行和尾行，把中间所有行的换行替换为空格，合并成一行长代码显示。
 * 因为只去换行，信息无损，展开时能完美还原原始内容。
 *
 * 行号定位（解决闪退）：用动态更新的当前行号 currentStart 做定位，
 * 每次 fold/unfold 后同步更新所有后续块的行号，避免用"折叠前原始行号"导致越界崩溃。
 */
class CodeFoldingManager(private val editor: CodeEditor) {

    /**
     * 单个折叠区域
     * @param currentStart 当前行号（动态更新，0-based，含），用于定位替换位置
     * @param currentEnd   当前行号（动态更新，0-based，含），折叠后 = currentStart + 2（首行+合并行+尾行）
     * @param originalMiddle 原始中间行内容（首行尾行之外的，按原样保存，含换行）
     * @param collapsedMiddle 折叠后的中间合并行（中间行去掉换行，用空格连接）
     */
    data class FoldRegion(
        var currentStart: Int,
        var currentEnd: Int,
        val originalFirst: String,
        val originalMiddle: String,
        val originalLast: String,
        val collapsedMiddle: String,
    )

    // 按当前行号排序的折叠区域列表
    private val regions = mutableListOf<FoldRegion>()

    /**
     * 分析代码，找出所有可折叠的代码块（基于括号匹配）
     * 支持 {} 对象/函数块 和 [] 数组块，含嵌套
     * @return 代码块列表，每个含 startLine 和 endLine（0-based，含）
     */
    fun findFoldableBlocks(): List<IntRange> {
        val text = editor.text
        val blocks = mutableListOf<IntRange>()
        // 带类型的栈：存 Pair(行号, 括号类型)
        val stack = ArrayDeque<Pair<Int, Char>>()

        for (lineIndex in 0 until text.lineCount) {
            val line = text.getLine(lineIndex).toString()
            // 忽略字符串和注释中的括号（简易处理）
            var inString = false
            var stringChar: Char = ' '
            var inLineComment = false
            for (c in line) {
                if (inLineComment) break
                when {
                    inString -> {
                        if (c == stringChar) inString = false
                    }
                    c == '"' || c == '\'' -> {
                        inString = true
                        stringChar = c
                    }
                    c == '/' -> { /* 简易处理，不识别块注释 */ }
                    c == '{' || c == '[' -> stack.addLast(lineIndex to c)
                    c == '}' || c == ']' -> {
                        val openChar = if (c == '}') '{' else '['
                        // 弹出直到匹配到对应类型的开括号
                        while (stack.isNotEmpty() && stack.last().second != openChar) {
                            stack.removeLast()
                        }
                        if (stack.isNotEmpty()) {
                            val (startLine, _) = stack.removeLast()
                            // 至少4行才折叠：折叠后固定为3行（首行+合并中间行+尾行），
                            // 3行折叠无缩减效果，故要求 >= 3（即首尾行间距≥3，共≥4行）
                            if (lineIndex - startLine >= 3) {
                                blocks.add(startLine..lineIndex)
                            }
                        }
                    }
                }
            }
        }
        return blocks
    }

    /**
     * 找出最外层的可折叠代码块（过滤掉被其他块包含的嵌套块）
     *
     * 用于"折叠全部"：只折叠最外层大函数，不折叠内部小函数。
     * 因为折叠外层时内部的折叠会被先展开（见 fold()），内部折叠是浪费操作。
     * @return 不互相嵌套的最外层块列表，按 startLine 升序
     */
    fun findTopLevelFoldableBlocks(): List<IntRange> {
        val blocks = findFoldableBlocks().sortedBy { it.first }
        val result = mutableListOf<IntRange>()
        for (block in blocks) {
            // 若当前块被已选中的外层块包含，则跳过（它是内层块）
            if (result.any { block.first in (it.first + 1)..(it.last - 1) }) continue
            result.add(block)
        }
        return result
    }

    /**
     * 折叠指定代码块（去换行方案）
     * @param startLine 起始行（0-based，含）
     * @param endLine 结束行（0-based，含）
     */
    fun fold(startLine: Int, endLine: Int) {
        val text = editor.text
        // 已折叠则跳过
        if (regions.any { it.currentStart == startLine }) return

        // 关键：折叠大函数前，必须先展开其内部所有已折叠的子函数
        // 否则保存的"原始内容"是子函数的折叠形态，展开大函数时会导致错乱
        // 从后往前展开子区域，每次展开后外层 endLine 相应后移
        var adjustedEndLine = endLine
        val subRegions = regions.filter {
            it.currentStart > startLine && it.currentEnd <= endLine
        }.sortedByDescending { it.currentStart }
        for (region in subRegions) {
            val addedLines = region.originalMiddle.count { it == '\n' }
            unfold(region)
            adjustedEndLine += addedLines
        }

        // 保存原始中间行内容（startLine+1 到 adjustedEndLine-1）
        val middleLines = (startLine + 1 until adjustedEndLine).map { text.getLine(it).toString() }
        val originalMiddle = middleLines.joinToString("\n")
        // 折叠：去掉换行，用空格连接（保持 JS 合法性）
        // 处理注释：含 // 单行注释的行会"吞掉"后续行，折叠时需去掉注释部分
        // （原始内容已保存在 originalMiddle，unfold 时 100% 还原，注释不会真丢）
        // 处理两种情况：整行注释(以//开头)直接丢弃；行内注释(code; // comment)截掉注释部分
        // 注意：字符串内的 //（如 "http://..."）不能误删，需简易字符串感知
        val collapsedMiddle = middleLines
            .map { stripLineComment(it.trim()) }
            .filter { it.isNotEmpty() }
            .joinToString(" ")

        // 构建替换内容：首行 + 折叠中间行 + 尾行（尾行后追加折叠行数注释）
        val firstLine = text.getLine(startLine).toString()
        val lastLine = text.getLine(adjustedEndLine).toString()
        val foldedCount = adjustedEndLine - startLine - 1 // 被折叠的行数（含中间行+尾行-1）
        // 在尾行后追加注释说明折叠了多少行，方便了解折叠内容规模
        val lastLineWithHint = "$lastLine  // 折叠了${foldedCount}行"
        val newContent = firstLine + "\n" + collapsedMiddle + "\n" + lastLineWithHint

        // 用 sora-editor 文本替换
        val start = text.getCharIndex(startLine, 0)
        val end = text.getCharIndex(adjustedEndLine, text.getColumnCount(adjustedEndLine))
        text.replace(start, end, newContent)

        // 记录折叠区域：当前 endLine = startLine + 2（首行+合并行+尾行）
        val reducedLines = adjustedEndLine - startLine - 2 // 减少的行数
        regions.add(
            FoldRegion(
                currentStart = startLine,
                currentEnd = startLine + 2,
                originalFirst = firstLine,
                originalMiddle = originalMiddle,
                originalLast = lastLine,
                collapsedMiddle = collapsedMiddle
            )
        )
        // 同步更新后续折叠区域的行号（都前移 reducedLines）
        regions.sortBy { it.currentStart }
        val idx = regions.indexOfFirst { it.currentStart == startLine }
        for (i in (idx + 1) until regions.size) {
            regions[i].currentStart -= reducedLines
            regions[i].currentEnd -= reducedLines
        }
    }

    /**
     * 展开指定折叠区域
     * @param region 要展开的区域
     */
    fun unfold(region: FoldRegion) {
        val text = editor.text
        val startLine = region.currentStart
        val endLine = region.currentEnd // 折叠后 = currentStart + 2

        // 构建还原内容：用保存的原始首行 + 原始中间行 + 原始尾行
        // （折叠后尾行被加了注释，必须用保存的原始内容还原）
        val originalContent = region.originalFirst + "\n" + region.originalMiddle + "\n" + region.originalLast

        val start = text.getCharIndex(startLine, 0)
        val end = text.getCharIndex(endLine, text.getColumnCount(endLine))
        text.replace(start, end, originalContent)

        // 增加的行数 = 原始中间行数 - 1（折叠时是1行，还原后是多行）
        val addedLines = region.originalMiddle.count { it == '\n' }
        // 从列表移除
        val idx = regions.indexOf(region)
        if (idx >= 0) {
            regions.removeAt(idx)
            // 同步更新后续折叠区域的行号（都后移 addedLines）
            for (i in idx until regions.size) {
                regions[i].currentStart += addedLines
                regions[i].currentEnd += addedLines
            }
        }
    }

    /**
     * 展开指定起始行对应的折叠区域
     */
    fun unfoldAt(startLine: Int): Boolean {
        val region = regions.firstOrNull { it.currentStart == startLine } ?: return false
        unfold(region)
        return true
    }

    /**
     * 是否已折叠
     */
    fun isFolded(startLine: Int): Boolean = regions.any { it.currentStart == startLine }

    /**
     * 折叠所有最外层可折叠代码块（不折叠内部嵌套块）
     *
     * 只折叠最外层大函数，内部小函数不折叠。因为折叠外层时内部的折叠会被先展开
     * （见 fold()），内部折叠是浪费操作。
     * 最外层块互不嵌套，从后往前折叠避免行号影响，无需每次重新查找。
     */
    fun foldAll() {
        val blocks = findTopLevelFoldableBlocks()
        for (block in blocks.sortedByDescending { it.first }) {
            fold(block.first, block.last)
        }
    }

    /**
     * 展开所有折叠区域
     * 从后往前展开，每步位置都准确（依赖动态行号同步）
     */
    fun unfoldAll() {
        // 按 currentStart 降序，从后往前展开
        val sorted = regions.sortedByDescending { it.currentStart }.toList()
        for (region in sorted) {
            unfold(region)
        }
    }

    /**
     * 渐进式折叠所有最外层可折叠块（逐个折叠，每次间隔 intervalMs，避免代码量大时一次性操作导致 ANR/闪退）
     * 必须在主线程调用（sora-editor 文本操作要求主线程），delay 不会阻塞主线程
     *
     * 只折叠最外层大函数，内部小函数不折叠。最外层块互不嵌套，
     * 从后往前折叠避免行号影响，无需每次重新查找。
     */
    suspend fun foldAllProgressive(intervalMs: Long = 50L) {
        val blocks = findTopLevelFoldableBlocks()
        for (block in blocks.sortedByDescending { it.first }) {
            fold(block.first, block.last)
            delay(intervalMs)
        }
    }

    /**
     * 渐进式展开所有折叠区域（逐个展开，每次间隔 intervalMs）
     */
    suspend fun unfoldAllProgressive(intervalMs: Long = 50L) {
        val sorted = regions.sortedByDescending { it.currentStart }.toList()
        for (region in sorted) {
            unfold(region)
            delay(intervalMs)
        }
    }

    /**
     * 获取所有已折叠区域（按当前行号排序）
     */
    fun getRegions(): List<FoldRegion> = regions.toList()

    /**
     * 导出当前所有折叠区域为内容指纹标记（与行号无关）
     * 用于持久化保存折叠状态，代码修改后可重新定位匹配的区域
     */
    fun exportFoldMarks(): List<FoldMark> {
        return regions.map { region ->
            FoldMark(
                firstLine = region.originalFirst.trim(),
                lastLine = region.originalLast.trim(),
                // 首行 + 中间行(originalMiddle中的换行数+1) + 尾行
                lineCount = region.originalMiddle.count { it == '\n' } + 3
            )
        }
    }

    /**
     * 根据内容指纹标记恢复折叠状态
     *
     * 对每个标记在当前代码中搜索匹配的首行，验证尾行后折叠；
     * 代码被修改/删除的区域会因不匹配而自动跳过（不会错误折叠）
     *
     * 从后往前处理：先恢复代码中靠后的折叠区域，靠前区域的行号不受影响
     */
    fun importFoldMarks(marks: List<FoldMark>) {
        for (mark in marks.asReversed()) {
            if (mark.firstLine.isEmpty()) continue
            val text = editor.text
            var foundStart = -1
            for (lineIndex in 0 until text.lineCount) {
                if (text.getLine(lineIndex).toString().trim() == mark.firstLine) {
                    val expectedEndLine = lineIndex + mark.lineCount - 1
                    if (expectedEndLine < text.lineCount &&
                        text.getLine(expectedEndLine).toString().trim() == mark.lastLine
                    ) {
                        foundStart = lineIndex
                        break
                    }
                }
            }
            if (foundStart >= 0) {
                fold(foundStart, foundStart + mark.lineCount - 1)
            }
        }
    }

    // 折叠状态恢复标志（防止重复恢复）
    private var foldRestored = false

    /**
     * 延迟恢复折叠状态：若代码已加载则立即恢复，否则注册监听等代码加载后恢复
     * 解决折叠管理器创建时代码可能尚未 setText 的问题
     */
    fun scheduleRestore(marks: List<FoldMark>) {
        if (marks.isEmpty() || foldRestored) return
        if (tryRestoreNow(marks)) return
        // 代码尚未加载，监听文本变化，加载后自动恢复
        editor.text.addContentListener(object : ContentListener {
            override fun beforeReplace(content: Content) {}

            override fun afterInsert(
                content: Content,
                startLine: Int,
                startColumn: Int,
                endLine: Int,
                endColumn: Int,
                insertedContent: CharSequence,
            ) {
                tryRestoreNow(marks)
            }

            override fun afterDelete(
                content: Content,
                startLine: Int,
                startColumn: Int,
                endLine: Int,
                endColumn: Int,
                deletedContent: CharSequence,
            ) {
            }
        })
    }

    private fun tryRestoreNow(marks: List<FoldMark>): Boolean {
        if (foldRestored) return true
        val text = editor.text
        if (text.lineCount > 0 && text.getLine(0).isNotEmpty()) {
            foldRestored = true
            importFoldMarks(marks)
            return true
        }
        return false
    }

    /**
     * 获取选区文本，自动把折叠区域还原为原始内容（展开后复制）
     * @param startLine 起始行(0-based)
     * @param startCol  起始列
     * @param endLine   结束行(0-based)
     * @param endCol    结束列
     * @return 还原折叠后的选区文本
     */
    fun getUnfoldedSelectionText(startLine: Int, startCol: Int, endLine: Int, endCol: Int): String {
        val text = editor.text
        val result = StringBuilder()

        var line = startLine
        while (line <= endLine) {
            // 检查当前行是否在某个折叠区域内
            val region = regions.firstOrNull { line in it.currentStart..it.currentEnd }

            val originalContent = if (region != null) {
                // 当前行在折叠区域内，还原为原始内容
                when (line) {
                    region.currentStart -> {
                        // 首行：处理起始列
                        val first = region.originalFirst
                        if (line == startLine && startCol > 0) {
                            if (startCol < first.length) first.substring(startCol) else ""
                        } else first
                    }
                    region.currentStart + 1 -> {
                        // 合并行 → 还原为原始中间多行内容
                        region.originalMiddle
                    }
                    else -> {
                        // 尾行：处理结束列
                        val last = region.originalLast
                        if (line == endLine && endCol < last.length) last.substring(0, endCol) else last
                    }
                }
            } else {
                // 普通行：取当前文本，处理首尾列
                var content = text.getLineString(line)
                if (line == startLine && startCol > 0) {
                    content = if (startCol < content.length) content.substring(startCol) else ""
                }
                if (line == endLine && endCol < content.length) {
                    content = content.substring(0, endCol)
                }
                content
            }

            if (result.isNotEmpty()) result.append("\n")
            result.append(originalContent)
            line++
        }
        return result.toString()
    }

    /**
     * 获取指定逻辑行所在的可折叠块（用于光标处折叠）
     * @return 未折叠的最外层块，或 null
     */
    fun findBlockAtLine(line: Int): IntRange? {
        val blocks = findFoldableBlocks()
        // 找包含该行且最内层（最小）的未折叠块
        var best: IntRange? = null
        for (block in blocks) {
            if (line in block) {
                // 跳过已折叠的
                if (regions.any { it.currentStart == block.first }) continue
                if (best == null || (block.last - block.first) < (best.last - best.first)) {
                    best = block
                }
            }
        }
        return best
    }

    /**
     * 去掉单行行内注释（// 及之后内容），保留注释前的代码
     * - 整行注释 "// xxx" → 返回 ""（被后续 filter 丢弃）
     * - 行内注释 "code; // xxx" → 返回 "code;"
     * - 无注释 "code;" → 返回 "code;"
     * - 字符串内的 // 不误删，如 "http://x" 中的 // 保留
     *   简易处理：遍历时跟踪是否在字符串内（单/双引号），字符串内的 // 视为普通字符
     */
    private fun stripLineComment(line: String): String {
        var inString = false
        var stringChar = ' '
        var i = 0
        while (i < line.length - 1) {
            val c = line[i]
            if (inString) {
                if (c == stringChar && line[i - 1] != '\\') inString = false
            } else {
                when {
                    c == '"' || c == '\'' -> { inString = true; stringChar = c }
                    c == '/' && line[i + 1] == '/' -> return line.substring(0, i).trim()
                }
            }
            i++
        }
        return line.trim()
    }
}
