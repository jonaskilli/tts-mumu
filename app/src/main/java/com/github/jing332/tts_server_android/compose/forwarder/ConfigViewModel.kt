package com.github.jing332.tts_server_android.compose.forwarder

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.github.jing332.common.LogEntry

class ConfigViewModel : ViewModel() {
    val logs = mutableStateListOf<LogEntry>()
    val logState by lazy { LazyListState() }
}
