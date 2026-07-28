package com.github.jing332.script.exception

import org.mozilla.javascript.Context

fun <R> runScriptCatching(block: () -> R): R {
    return try {
        block()
    } catch (e: Throwable) {
        throw ScriptException.from(e)
    }
}

open class ScriptException(
    val sourceName: String = "",
    val lineNumber: Int = 0,
    val columnNumber: Int = 0,

    override val message: String? = "",
    override val cause: Throwable? = null,
) : RuntimeException() {
    companion object {
        fun from(throwable: Throwable): ScriptException {
            val err = Context.reportRuntimeError(throwable.message ?: throwable.toString())

            return ScriptException(
                err.sourceName(),
                err.lineNumber(),
                err.columnNumber(),
                err.toString(),
                throwable
            )
        }
    }
}