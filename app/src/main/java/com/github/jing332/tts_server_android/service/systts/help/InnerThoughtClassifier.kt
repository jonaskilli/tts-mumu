package com.github.jing332.tts_server_android.service.systts.help

internal object InnerThoughtClassifier {

    const val INNER_THOUGHT_TAG = "__inner_thought__"

    private val whitespace = Regex("\\s+")
    private val hardContextBoundary = setOf('。', '！', '？', '!', '?', '；', ';', '\n', '\r')

    private val immediateInnerThoughtCue = Regex(
        "(?:心想|心道|暗道|默念|心里想着?|心中想着?|内心想着?|" +
            "心中暗想|心里暗想|脑海中想(?:道)?|念头.{0,16}(?:闪过|闪而过|浮现))" +
            "[：:,，]?\\s*$",
    )

    private const val trailingAttributionPrefix =
        "^[，,。.!！?？\\s"」』'"']*(?:[^，。！？!?：:\\n"""''']{0,20})?"

    private const val trailingAttributionBoundary = "(?=$|[，,。.!！?？；;\\s])"

    private val trailingInnerThoughtAttribution = Regex(
        trailingAttributionPrefix +
            "(?:心想|心道|暗道|心里想着|心中想着|内心想着)" +
            trailingAttributionBoundary,
    )

    private fun immediateBeforeClause(beforeText: String): String {
        val tail = beforeText.takeLast(240).trimEnd()
        val lastBoundary = tail.indexOfLast { it in hardContextBoundary }
        return tail.substring(lastBoundary + 1)
            .replace(whitespace, " ")
            .trim()
            .takeLast(96)
    }

    fun isInnerThought(innerText: String, beforeText: String, afterText: String): Boolean {
        val text = innerText.trim()
        if (text.isBlank()) return false

        val before = immediateBeforeClause(beforeText)
        val after = afterText.take(240).replace(whitespace, " ").trim()

        if (immediateInnerThoughtCue.containsMatchIn(before)) return true
        if (trailingInnerThoughtAttribution.containsMatchIn(after)) return true

        return false
    }
}
