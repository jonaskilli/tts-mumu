package com.github.jing332.tts_server_android.compose

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentHashMap

class SharedViewModel : ViewModel() {
    private val dataStore = ConcurrentHashMap<String, Any>()

    fun put(key: String, value: Any) {
        dataStore[key] = value
    }

    fun get(key: String): Any? {
        return dataStore[key]
    }

    fun remove(key: String) {
        dataStore.remove(key)
    }

    inline fun <reified T>getOnce(key: String): T? {
        val value = get(key)
        remove(key)
        return if (value is T) value else null
    }

    /**
     * 主列表定位信号：日志快捷面板换旁白发音人落库后写入被改配置项 id，
     * 主界面（SystemTts 页）收集到后展开所在分组、滚动到该项并短暂高亮（用户 09-09）。
     * 消费即清空；null=无待定位项。
     */
    val pendingLocateConfigId = MutableStateFlow<Long?>(null)
}