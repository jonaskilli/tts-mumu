package com.github.jing332.tts.speech

sealed class EngineState {
    data class Uninitialized(val reason: Throwable? = null) : EngineState()
    data object Initializing : EngineState()
    data object Initialized : EngineState()
}
