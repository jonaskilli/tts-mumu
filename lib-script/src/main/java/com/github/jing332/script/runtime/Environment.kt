package com.github.jing332.script.runtime

import org.mozilla.javascript.NativeJavaObject
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

data class Environment(val cacheDir: String, val id: String) {
    companion object {
        fun Scriptable.environment(
        ): Environment {
            val value = ScriptableObject.getProperty(this, "environment")
            // 宿主用 defineGetter(Supplier + Context.javaToJS) 注入，取值是包了层的
            // NativeJavaObject（与 ttsrv 0e2d505 同一机制）；fs 系列函数读环境需拆包
            val unwrapped = (value as? NativeJavaObject)?.unwrap() ?: value
            return unwrapped as Environment
        }
    }
}