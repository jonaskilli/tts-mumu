package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalSaveCallBack =
    staticCompositionLocalOf<MutableList<SaveCallBack>> { mutableListOf() }

internal fun interface SaveCallBack {
    suspend fun onSave(): Boolean
}

@Composable
internal fun rememberSaveCallBacks() = remember { mutableListOf<SaveCallBack>() }

@Composable
internal fun SaveActionHandler(cb: SaveCallBack) {
    val cbs = LocalSaveCallBack.current
    val currentCb = rememberUpdatedState(cb)
    DisposableEffect(Unit) {
        val wrapper = SaveCallBack { currentCb.value.onSave() }
        cbs.add(wrapper)
        onDispose {
            cbs.remove(wrapper)
        }
    }
}