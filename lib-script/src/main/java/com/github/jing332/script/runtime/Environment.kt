package com.github.jing332.script.runtime

import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

data class Environment(val cacheDir: String, val id: String) {
    companion object {
        fun Scriptable.environment(
        ): Environment = ScriptableObject.getProperty(this, "environment") as Environment
    }
}