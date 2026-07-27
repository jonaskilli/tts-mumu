package com.github.jing332.tts.error

sealed interface RequesterError {
    data class RequestError(val error: Throwable) : RequesterError
    data class StateError(val message: String) : RequesterError
}