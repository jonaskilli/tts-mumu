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
private val OverlayControllerSaver = Saver<OverlayController, Boolean>(
    save = { it.visible },
    restore = { savedVisibility ->
        OverlayController().apply {
            visible = savedVisibility
        }
    }
)
