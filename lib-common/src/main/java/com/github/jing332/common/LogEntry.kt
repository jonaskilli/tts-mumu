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
    val indent: Int = 0,
    // 请求发生时的配置项 id：日志快捷面板凭它定位配置项（0=无关联，如插件日志/系统日志）
    val configId: Long = 0,
) :
    Parcelable {
    fun getLevelChar(): String = level.toLogLevelChar()
}