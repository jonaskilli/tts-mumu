package com.github.jing332.tts_server_android.compose

import androidx.lifecycle.ViewModel
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
}