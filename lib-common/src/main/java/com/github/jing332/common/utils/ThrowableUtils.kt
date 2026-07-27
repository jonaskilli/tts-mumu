package com.github.jing332.common.utils

import cn.hutool.core.exceptions.ExceptionUtil


object ThrowableUtils {
}

val Throwable.rootCause: Throwable
    get() = ExceptionUtil.getRootCause(this)

val Throwable.messageChain: String
    get() = ExceptionUtil.getThrowableList(this).joinToString("\n")

val Throwable.readableString: String
    get() = "${rootCause}\n⬇ More:\n${stackTraceToString()}"
