package com.github.jing332.tts_server_android.model.rhino.direct_link_upload

import com.github.jing332.script.withRhinoContext
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable

data class DirectUploadFunction(
    val function: Function,
    val name: String,
    val scope: Scriptable,
    val thisObj: Scriptable,
) {
    fun invoke(config: String): String? = withRhinoContext { cx ->
        function.call(cx, scope, thisObj, arrayOf(config))?.toString() ?: ""
    }

}