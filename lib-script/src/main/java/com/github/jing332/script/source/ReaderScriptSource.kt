package com.github.jing332.script.source

import java.io.Reader

class ReaderScriptSource(
    val reader: Reader,
    sourceName: String = "",
    sourceUri: String = "",
) :
    StringScriptSource(sourceName, sourceUri, "") {
    override val script: String
        get() = reader.readText()
}