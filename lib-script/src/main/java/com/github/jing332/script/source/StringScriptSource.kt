package com.github.jing332.script.source

open class StringScriptSource(
    open val script: String,
    override val sourceName: String = "",
    override val sourceUri: String = "",
) : ScriptSource {
}

fun String.toScriptSource(sourceName: String = "", sourceUri: String = ""): ScriptSource {
    return StringScriptSource(this, sourceName, sourceUri)
}