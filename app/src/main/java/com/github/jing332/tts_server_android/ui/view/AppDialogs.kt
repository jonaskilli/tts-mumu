package com.github.jing332.tts_server_android.ui.view

import android.content.Context
import com.github.jing332.common.utils.runOnUI
import com.github.jing332.tts_server_android.R

object AppDialogs {
    fun Context.displayErrorDialog(t: Throwable, title: String = getString(R.string.error)) {
        // 过滤 Compose 的 LeftCompositionCancellationException 及其包装异常：
        // 这是 Compose 协程离开 composition 时的正常取消行为，不是真正的错误，
        // 不应弹错误对话框打扰用户。遍历 cause 链以捕获被 runCatching 包装的情况。
        var cur: Throwable? = t
        while (cur != null) {
            if (cur::class.java.simpleName == "LeftCompositionCancellationException"
                || cur is kotlinx.coroutines.CancellationException) {
                return
            }
            cur = cur.cause
        }
        runOnUI {
            ErrorDialogActivity.start(this, title, t = t)
        }
    }
}