package com.github.jing332.script.simple.ext

import android.view.View
import android.view.ViewGroup
import com.github.jing332.common.utils.dp
import com.github.jing332.common.utils.longToast
import com.github.jing332.common.utils.toast
import com.github.jing332.script.annotation.ScriptInterface
import splitties.init.appCtx


interface JsUserInterface {
    @ScriptInterface
    fun toast(msg: CharSequence) = appCtx.toast(msg)
    @ScriptInterface
    fun longToast(msg: CharSequence) = appCtx.longToast(msg)

    @ScriptInterface
    fun setMargins(v: View, left: Int, top: Int, right: Int, bottom: Int) {
        (v.layoutParams as ViewGroup.MarginLayoutParams).setMargins(
            left.dp,
            top.dp,
            right.dp,
            bottom.dp
        )
    }
//
}