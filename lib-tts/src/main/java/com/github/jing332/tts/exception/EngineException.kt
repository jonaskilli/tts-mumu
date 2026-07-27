package com.github.jing332.tts.exception

open class EngineException(override val message: String? = "", override val cause: Throwable? = null) :
    TtsException() {
}