package com.github.jing332.tts.synthesizer

import android.content.Context
import com.github.michaelbull.result.Result

interface ITextProcessor {
    fun init(
        context: Context,
        configs: Map<Long, TtsConfiguration>,
    ): Result<Unit, com.github.jing332.tts.error.TextProcessorError>

    fun process(
        text: String,
        forceConfig: TtsConfiguration? = null,
    ): Result<List<TextSegment>, com.github.jing332.tts.error.TextProcessorError>
}