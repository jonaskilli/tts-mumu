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
    // 朗读规则实时分析出的角色名（用户 09-08）：仅多角色对话请求有值，供快捷面板精确改绑该角色
    val roleName: String = "",
    // 连续同模式折叠计数（用户 09-09）：插件/规则日志里连续数字归一化后相同的行合并为一条，
    // 显示为「… ×N」，message 保留该串最后一条的内容。1=未折叠
    val repeatCount: Int = 1,
) :
    Parcelable {
    fun getLevelChar(): String = level.toLogLevelChar()
}