package com.github.jing332.tts_server_android.model.rhino.direct_link_upload

import com.github.jing332.script.runtime.console.Console
import com.github.jing332.script.simple.SimpleScriptEngine
import com.github.jing332.script.source.StringScriptSource
import com.github.jing332.tts_server_android.conf.DirectUploadConfig
import org.mozilla.javascript.Function
import org.mozilla.javascript.NativeObject

class DirectUploadEngine(
    context: android.content.Context,
    var code: String = DirectUploadConfig.code.value,
) {
    val engine = SimpleScriptEngine(context, "direct_link_upload")
    val console: Console
        get() = engine.runtime.console


    companion object {
        private const val TAG = "DirectUploadEngine"
        const val OBJ_DIRECT_UPLOAD = "DirectUploadJS"
    }


    private val jsObject: NativeObject
        get() = engine.get(OBJ_DIRECT_UPLOAD) as NativeObject

    /**
     * 获取所有方法
     */
    fun obtainFunctionList(): List<DirectUploadFunction> {
        engine.execute(StringScriptSource(code))
        val scope = engine.scope ?: throw IllegalStateException("engine.scope is null")
        return jsObject.map {
            val func = it.value
            if (func is Function)
                return@map DirectUploadFunction(
                    function = func,
                    name = it.key.toString(),
                    scope = scope,
                    thisObj = jsObject
                )

            null
        }.filterNotNull()
    }

}