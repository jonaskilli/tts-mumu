package com.github.jing332.tts.synthesizer

import com.github.jing332.tts.error.RequesterError
import java.io.InputStream

interface ITtsRequester {
    suspend fun request(
        params: SystemParams,
        tts: TtsConfiguration
    ): com.github.michaelbull.result.Result<Response, RequesterError>

    fun destroy()

    data class Response @JvmOverloads constructor(
        val callback: ISyncPlayCallback? = null,
        val stream: InputStream? = null
    ) {
        inline fun <R> onStream(block: (InputStream) -> R): Response {
            if (stream != null) block.invoke(stream)

            return this
        }

        inline fun <R> onCallback(block: (ISyncPlayCallback) -> R): Response {
            if (callback != null) block.invoke(callback)

            return this
        }
    }

    fun interface ISyncPlayCallback {
        suspend fun play()
    }
}

