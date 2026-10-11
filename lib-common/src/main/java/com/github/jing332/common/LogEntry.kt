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
    // 相同原因合并计数（10-11 用户令「相同原因的失败视为相同，日志可以×2、×3」）：
    // 相邻且原因指纹相同的失败/警告合并为一条，本字段记出现次数（1=未合并，不显示 ×N）。
    // 只作用于 ERROR/WARN；成功行、正文行不折叠
    val repeatCount: Int = 1,
    // 合并组的末条时间（与 time 同格式）：行头显示「首条–末条」完整时间用
    //（10-11 用户令：两头都要完整，不能只写秒钟）。repeatCount<=1 时为空串
    val endTime: String = "",
) :
    Parcelable {
    fun getLevelChar(): String = level.toLogLevelChar()
}