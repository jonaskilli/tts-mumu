package com.github.jing332.common

import android.graphics.Color
import android.util.Log
import androidx.annotation.IntDef

@IntDef(
    LogLevel.ERROR,
    LogLevel.WARN,
    LogLevel.INFO,
    LogLevel.DEBUG,
    LogLevel.TRACE,
    LogLevel.SUCCESS
)
annotation class LogLevel {
    companion object {
        const val ERROR = Log.ERROR
        const val WARN = Log.WARN
        const val INFO = Log.INFO
        const val DEBUG = Log.DEBUG
        const val TRACE = Log.VERBOSE
        const val SUCCESS = 100
    }
}

fun Int.toArgb(isDarkTheme: Boolean = false): Long =
    when (this) {
        LogLevel.TRACE -> if (isDarkTheme) 0xFFB0BEC5 else 0xFF9E9E9E
        LogLevel.DEBUG -> if (isDarkTheme) 0xFF64B5F6 else 0xFF2196F3
        LogLevel.INFO -> if (isDarkTheme) 0xFF81C784 else 0xFF4CAF50
        LogLevel.WARN -> if (isDarkTheme) 0xFFFFD54F else 0xFFFFC107
        LogLevel.ERROR -> if (isDarkTheme) 0xFFE57373 else 0xFFF44336
        // SUCCESS 跟随主题：深色用白、浅色用黑
        LogLevel.SUCCESS -> if (isDarkTheme) Color.WHITE.toLong() else Color.BLACK.toLong()
        else -> if (isDarkTheme) Color.WHITE.toLong() else Color.BLACK.toLong()
    }

fun String.toLogLevel():Int{
    return when (this.first().toString()) {
        "V" -> LogLevel.TRACE
        "D" -> LogLevel.DEBUG
        "I" -> LogLevel.INFO
        "W" -> LogLevel.WARN
        "E" -> LogLevel.ERROR
        "S" -> LogLevel.SUCCESS
        else -> LogLevel.INFO
    }
}

fun Int.toLogLevelChar(): String {
    return when (this) {
        LogLevel.TRACE -> "V"
        LogLevel.DEBUG -> "D"
        LogLevel.INFO -> "I"
        LogLevel.WARN -> "W"
        LogLevel.ERROR -> "E"
        LogLevel.SUCCESS -> "S"
        else -> ""
    }
}