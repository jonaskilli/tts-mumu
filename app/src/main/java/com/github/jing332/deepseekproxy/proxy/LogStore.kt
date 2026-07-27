package com.github.jing332.deepseekproxy.proxy

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 进程内日志缓冲，供中转服务与界面共享。
 * lines 保留最近 maxLines 条，避免无限增长。
 */
object LogStore {
    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    private const val maxLines = 50
    private val sdf = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault())

    @Synchronized
    private fun append(level: String, tag: String, msg: String) {
        val t = sdf.format(Date())
        val line = "$t $level/$tag: $msg"
        val list = _lines.value.toMutableList()
        list.add(line)
        if (list.size > maxLines) list.subList(0, list.size - maxLines).clear()
        _lines.value = list
    }

    fun i(tag: String, msg: String) = append("I", tag, msg)
    fun w(tag: String, msg: String) = append("W", tag, msg)
    fun e(tag: String, msg: String) = append("E", tag, msg)
    fun d(tag: String, msg: String) = append("D", tag, msg)
    fun raw(tag: String, msg: String) = append("RAW", tag, msg)

    fun clear() {
        _lines.value = emptyList()
    }
}
