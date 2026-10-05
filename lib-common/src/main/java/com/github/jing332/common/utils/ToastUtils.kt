@file:Suppress("unused")
/* https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/utils/ToastUtils.kt */
package com.github.jing332.common.utils

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment

/**
 * 取串：**没传参时不要走 format**（10-05 崩溃事故的根因防复发）
 *
 * `getString(id, *空数组)` 也会过一次 `String.format`：串里只要留着 `%1$s`、
 * 而调用点漏给参数，就抛 `MissingFormatArgumentException`（密钥页「测试」必崩即此）。
 * 无参调用改走非 vararg 重载 `getString(id)`（不过 format）——最差把 `%1$s` 原文显示出来，不崩。
 */
private fun Context.getStringSafe(@StringRes message: Int, args: Array<out Any>): String =
    if (args.isEmpty()) getString(message) else getString(message, *args)

fun Context.toast(@StringRes message: Int, vararg args: Any) {
    runOnUI {
        kotlin.runCatching {
            Toast.makeText(this, getStringSafe(message, args), Toast.LENGTH_SHORT).show()
        }
    }
}

fun Context.toast(message: CharSequence?) {
    runOnUI {
        kotlin.runCatching {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}

fun Context.longToast(@StringRes message: Int, vararg args: Any) {
    runOnUI {
        kotlin.runCatching {
            Toast.makeText(this, getStringSafe(message, args), Toast.LENGTH_LONG).show()
        }
    }
}

fun Context.longToast(message: CharSequence?) {
    runOnUI {
        kotlin.runCatching {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}


fun Fragment.toast(@StringRes message: Int) = requireActivity().toast(message)

fun Fragment.toast(message: CharSequence) = requireActivity().toast(message)

fun Fragment.longToast(@StringRes message: Int) = requireContext().longToast(message)

fun Fragment.longToast(message: CharSequence) = requireContext().longToast(message)