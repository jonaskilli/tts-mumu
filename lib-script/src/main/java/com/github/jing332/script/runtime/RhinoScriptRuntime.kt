package com.github.jing332.script.runtime

import com.github.jing332.script.runtime.console.Console
import com.github.jing332.script.runtime.console.ConsoleUtils.Companion.putLogger
import com.github.jing332.script.runtime.console.ConsoleUtils.Companion.writeFormat
import com.github.jing332.script.withRhinoContext
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeConsole
import org.mozilla.javascript.ScriptStackElement
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined

open class RhinoScriptRuntime(
    var environment: Environment,
    var console: Console = Console(),
) {
    companion object {
        val sharedTopLevelScope: ScriptableObject by lazy {
            withRhinoContext { cx ->
                RhinoTopLevel(cx)
            }
        }
    }

    val globalScope: ScriptableObject by lazy {
        withRhinoContext { cx ->
            (cx.newObject(sharedTopLevelScope) as ScriptableObject).apply {
                prototype =  sharedTopLevelScope
                parentScope = null
            }
        }
    }

    private fun putLogger(
        obj: ScriptableObject, name: String, level: NativeConsole.Level,

    ) {
        obj.defineProperty(name, object : BaseFunction() {
            override fun call(
                cx: Context,
                scope: Scriptable,
                thisObj: Scriptable,
                args: Array<out Any?>,
            ): Any {
                android.util.Log.d("RhinoScriptRuntime", "logger.$name called, console.source=${console.source}")
                console.writeFormat(cx, scope, args, level, null)
                return Undefined.instance
            }
        }, ScriptableObject.READONLY)
    }

    private fun initLogger(){
        val logger = withRhinoContext { it.newObject(this.globalScope) } as ScriptableObject
        putLogger(logger, "log", NativeConsole.Level.INFO)
        putLogger(logger, "t", NativeConsole.Level.TRACE)
        putLogger(logger, "d", NativeConsole.Level.DEBUG)
        putLogger(logger, "i", NativeConsole.Level.INFO)
        putLogger(logger, "w", NativeConsole.Level.WARN)
        putLogger(logger, "e", NativeConsole.Level.ERROR)
        putLogger(globalScope, "println", NativeConsole.Level.INFO)
        globalScope.defineProperty("logger", logger, ScriptableObject.READONLY)
    }

    /**
     * Call from [com.github.jing332.script.engine.RhinoScriptEngine.setRuntime]
     */
    open fun init() {
        NativeConsole.init(globalScope, false, object : NativeConsole.ConsolePrinter {
            override fun print(
                cx: org.mozilla.javascript.Context,
                scope: Scriptable,
                level: NativeConsole.Level,
                args: Array<out Any?>,
                stack: Array<out ScriptStackElement?>?,
            ) {
                android.util.Log.d("RhinoScriptRuntime", "NativeConsole.print called, console.source=${console.source}")
                console.writeFormat(cx, scope, args, level, stack)
            }
        })
        initLogger()

        globalScope.defineGetter("environment", ::environment)
    }

    protected fun ScriptableObject.defineGetter(key: String, getter: () -> Any) {
        // getter 返回值必须经 javaToJS 包装成 Scriptable 再交给 JS：
        // 此前直接把 Supplier 交给 defineProperty，JS 读到的是未包装的原始 Java 对象，
        // 插件里普遍的 `typeof ttsrv !== "undefined"` 会抛
        // 「TtsEngineContext 类型的 JavaScript 值无效」（jread 插件集 16/33 在用），
        // 表现为该插件的鉴权/试听分支全部失效
        defineProperty(key, object : BaseFunction() {
            override fun call(
                cx: Context,
                scope: Scriptable,
                thisObj: Scriptable,
                args: Array<out Any?>,
            ): Any {
                return Context.javaToJS(getter(), scope)
            }
        }, null, ScriptableObject.READONLY)
    }
}
