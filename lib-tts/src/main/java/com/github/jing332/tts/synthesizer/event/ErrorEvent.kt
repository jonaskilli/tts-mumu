package com.github.jing332.tts.synthesizer.event

import com.github.jing332.tts.error.StreamProcessorError
import com.github.jing332.tts.error.TextProcessorError
import com.github.jing332.tts.synthesizer.RequestPayload

sealed class ErrorEvent(open val cause: Throwable? = null) : Event {

    data class Request(
        val request: RequestPayload,
        override val cause: Throwable?,
    ) : ErrorEvent(cause)

    data class DirectPlay(val payload: RequestPayload, override val cause: Throwable) :
        ErrorEvent(cause)

    data class RequestTimeout(
        val request: RequestPayload,
    ) : ErrorEvent()

    data class TextProcessor(
        val error: TextProcessorError,
    ) : ErrorEvent()

    data class Repository(
        override val cause: Throwable?,
    ) : ErrorEvent(cause)

    data class ResultProcessor(
        val request: RequestPayload,
        val error: StreamProcessorError,
    ) : ErrorEvent()

    data object ConfigEmpty : ErrorEvent()

    data class BgmLoading(override val cause: Throwable?) : ErrorEvent(cause)


}