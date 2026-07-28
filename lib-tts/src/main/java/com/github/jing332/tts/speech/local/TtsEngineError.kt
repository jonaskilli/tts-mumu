package com.github.jing332.tts.speech.local

sealed interface TtsEngineError {
    object Initialization : TtsEngineError
    object Engine : TtsEngineError
    object File : TtsEngineError
}