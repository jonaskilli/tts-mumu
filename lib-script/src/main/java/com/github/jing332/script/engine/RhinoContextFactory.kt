package com.github.jing332.script.engine

import org.mozilla.javascript.Callable
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.Wrapper
import java.util.Locale

object RhinoContextFactory : ContextFactory() {

    init {
        initGlobal(this)
    }

    override fun makeContext(): Context {
        return super.makeContext().apply {
            isInterpretedMode = true // optimizationLevel = -1
            languageVersion = Context.VERSION_ES6
            setClassShutter(RhinoClassShutter)
            wrapFactory = RhinoWrapFactory
            locale = Locale.getDefault()
        }
    }

    override fun hasFeature(cx: Context?, featureIndex: Int): Boolean {
        return when (featureIndex) {
//            Context.FEATURE_ENHANCED_JAVA_ACCESS -> true
            Context.FEATURE_ENABLE_JAVA_MAP_ACCESS -> true
            else -> super.hasFeature(cx, featureIndex)
        }
    }

    override fun doTopCall(
        callable: Callable?,
        cx: Context?,
        scope: Scriptable?,
        thisObj: Scriptable?,
        args: Array<out Any>?,
    ): Any {
        return when (val ret = super.doTopCall(callable, cx, scope, thisObj, args)) {
            is Wrapper -> ret.unwrap()
            else -> ret
        }
    }
}