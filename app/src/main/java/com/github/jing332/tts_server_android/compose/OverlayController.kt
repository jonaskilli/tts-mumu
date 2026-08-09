package com.github.jing332.tts_server_android.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

class OverlayController() {
    private var mVisible: MutableState<Boolean> = mutableStateOf(false)

    var visible: Boolean by mVisible

    fun show() {
        mVisible.value = true
    }

    fun hide() {
        mVisible.value = false
    }
}

@Composable
fun rememberOverlayController(): OverlayController {
    return rememberSaveable(saver = OverlayControllerSaver) {
        OverlayController()
    }
}

// 自定义 Saver
// 注意：restore 时强制 visible=false。遮罩是瞬态 UI 状态，不应跨 Activity 重建/进程恢复保留，
// 否则一旦因重组异常卡在 true，会持续白屏且只能重启 App。
private val OverlayControllerSaver = Saver<OverlayController, Boolean>(
    save = { false },
    restore = { OverlayController() }
)
