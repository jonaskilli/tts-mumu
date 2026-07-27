package com.github.jing332.script.runtime.console

import com.github.jing332.common.LogLevel
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeConsole
import org.mozilla.javascript.ScriptStackElement
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined

internal class ConsoleUtils {
    companion object {
        fun toLogLevel(l: NativeConsole.Level) = when (l) {
            NativeConsole.Level.TRACE -> LogLevel.TRACE
            NativeConsole.Level.DEBUG -> LogLevel.DEBUG
            NativeConsole.Level.INFO -> LogLevel.INFO
            NativeConsole.Level.WARN -> LogLevel.WARN
            NativeConsole.Level.ERROR -> LogLevel.ERROR
        }


        fun putLogger(
            obj: ScriptableObject, name: String, level: NativeConsole.Level,
            write: (
                cx: Context,
                scope: Scriptable,
                args: Array<out Any?>,
                level: NativeConsole.Level,
                stack: Array<out ScriptStackElement?>?,
            ) -> Unit,
        ) {
            obj.defineProperty(name, object : BaseFunction() {
                override fun call(
                    cx: Context,
                    scope: Scriptable,
                    thisObj: Scriptable,
                    args: Array<out Any?>,
                ): Any {
                    write(cx, scope, args, level, null)
                    return Undefined.instance
                }
            }, ScriptableObject.READONLY)
        }

        fun Console.writeFormat(
            cx: Context,
            scope: Scriptable,
            args: Array<out Any?>,
            level: NativeConsole.Level,
            stack: Array<out ScriptStackElement?>? = null,
        ) {
            val str = NativeConsole.format(cx, scope, args)
            write(
                toLogLevel(level),
                if (level == NativeConsole.Level.TRACE) str + "\n" + stack.contentToString() else str
            )
        }
    }
}