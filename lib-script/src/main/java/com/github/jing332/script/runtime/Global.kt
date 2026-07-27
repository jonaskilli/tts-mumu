package com.github.jing332.script.runtime

import com.github.jing332.script.runtime.GlobalWebview.Companion.NAME
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

abstract class Global : ScriptableObject() {
    fun init(scope: Scriptable, name: String, sealed: Boolean) {
        this.prototype = ScriptableObject.getObjectPrototype(scope)
        this.parentScope = scope

        if (sealed) sealObject()
        ScriptableObject.defineProperty(scope, NAME, this, DONTENUM or READONLY)
    }

    companion object {

//        inline fun <reified T : Global> init(
//            cx: Context,
//            scope: Scriptable,
//            sealed: Boolean,
//            propertyName: String,
//            block: T.() -> Unit,
//        ) {
//            val obj = T::class.java.getDeclaredConstructor().newInstance() as T
//            obj.prototype = getObjectPrototype(scope)
//            obj.parentScope = scope
//            block(obj)
//
//            if (sealed) obj.sealObject()
//            ScriptableObject.defineProperty(obj, propertyName, obj, ScriptableObject.DONTENUM)
//        }
    }

    override fun getClassName(): String = "Global"
}