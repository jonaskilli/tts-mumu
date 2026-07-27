package com.github.jing332.script

import com.github.jing332.script.source.ScriptSource

interface ScriptEngine {
    fun init()
    fun destroy()
    fun execute(source: ScriptSource): Any?
}