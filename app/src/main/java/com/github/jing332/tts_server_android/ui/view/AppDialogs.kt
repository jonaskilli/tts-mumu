package com.github.jing332.tts_server_android.ui.view

import android.content.Context
import com.github.jing332.common.utils.runOnUI
import com.github.jing332.tts_server_android.R

object AppDialogs {
    fun Context.displayErrorDialog(t: Throwable, title: String = getString(R.string.error)) {
        runOnUI {
            ErrorDialogActivity.start(this, title, t = t)
        }
    }
}