package com.github.jing332.common

import android.graphics.Color
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LogEntry(
    val level: Int,
    val message: String,
    val time: String = "",
    val wrapLine: Boolean = true,
    val isPluginLog: Boolean = false,
    val isSpeechRuleLog: Boolean = false,
    // 渲染层级：0=主行；>0=从属于上一主行的子行(缩进显示)。用于“请求→获取结果”这类成对日志的分组表达
    val indent: Int = 0
) :
    Parcelable {
    fun getLevelChar(): String = level.toLogLevelChar()
}