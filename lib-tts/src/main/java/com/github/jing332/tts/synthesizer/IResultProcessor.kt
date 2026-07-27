package com.github.jing332.tts.synthesizer

import android.content.Context
import com.github.jing332.tts.error.StreamProcessorError
import com.github.michaelbull.result.Result
import java.io.InputStream

interface IResultProcessor {
    suspend fun processStream(
        ins: InputStream,
        request: RequestPayload,
        targetSampleRate: Int,
        callback: PcmAudioDataListener
    ): Result<Unit, StreamProcessorError>

    suspend fun destroy() {}
    suspend fun init(context: Context)
}