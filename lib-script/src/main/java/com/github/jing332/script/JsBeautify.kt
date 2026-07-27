package com.github.jing332.script

import android.content.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.ScriptableObject

class JsBeautify(context: Context) {
    private val scope: ScriptableObject
    private var formatJsFunc: Function

    init {
        org.mozilla.javascript.Context.enter().use { cx ->
            scope = cx.initStandardObjects()
            cx.isInterpretedMode = true
            cx.languageVersion = org.mozilla.javascript.Context.VERSION_ES6

            context.assets.open("js/beautifier.js").bufferedReader().use { reader ->
                cx.evaluateReader(
                    scope, reader, "<beautifier.js>", 1, null
                )
                formatJsFunc = (scope.get("js_beautify") as ScriptableObject) as Function
            }
        }
    }

    fun format(code: String): String = withRhinoContext { cx ->
        formatJsFunc.call(cx, scope, scope, arrayOf(code)) as String
    }
}