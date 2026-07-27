package com.github.jing332.script.runtime

import com.github.jing332.common.utils.firstCharUpperCase
import com.github.jing332.script.withRhinoContext
import io.github.oshai.kotlinlogging.KotlinLogging
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.ScriptRuntime
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined

class NativeEventTarget(val scope: ScriptableObject) {
    companion object {
        val logger = KotlinLogging.logger("NativeEventTarget")
    }

    val functions = hashMapOf<String, Function>()

    private fun addEventListener(eventName: String, function: Function) {
        functions[eventName] = function
    }

    init {
        install(scope)
    }

    private fun install(scope: ScriptableObject) {
        scope.defineProperty("on", object : BaseFunction() {
            override fun call(
                cx: Context?,
                scope: Scriptable?,
                thisObj: Scriptable?,
                args: Array<out Any>?,
            ): Any {
                val eventName = args!![0].toString()
                val function = args[1] as Function
                addEventListener(eventName, function)

                return Undefined.instance
            }
        }, ScriptableObject.READONLY)

        scope.defineProperty("addEventListener", object : BaseFunction() {
            override fun call(
                cx: Context?,
                scope: Scriptable?,
                thisObj: Scriptable?,
                args: Array<out Any>?,
            ): Any {
                val eventName = args!![0].toString()
                val function = args[1] as Function
                addEventListener(eventName, function)
                return Undefined.instance
            }
        }, ScriptableObject.READONLY)
    }

    fun emit(eventName: String, vararg args: Any?) {
//        logger.debug { "emit: $eventName, ${args.contentToString()}" }
        val function =
            functions[eventName] ?: scope.get("on${eventName.firstCharUpperCase()}") as? Function

        withRhinoContext { cx ->
            try {
                function?.call(cx, scope, scope, args)
            } catch (e: Exception) {
                logger.error(e) { "emit error " }

                runCatching {
                    functions["error"]?.apply { call(cx, scope, scope, arrayOf(e)) }
                }
            }
        }
    }


}