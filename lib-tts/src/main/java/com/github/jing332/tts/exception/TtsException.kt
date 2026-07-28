package com.github.jing332.tts.exception

open class TtsException(override val message: String? = null, override val cause: Throwable? = null) :
    Exception()