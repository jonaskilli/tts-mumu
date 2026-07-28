package com.github.jing332.tts

import android.content.Context
import com.github.jing332.common.utils.StringUtils
import com.github.jing332.tts.error.TextProcessorError
import com.github.jing332.tts.synthesizer.ITextProcessor
import com.github.jing332.tts.synthesizer.TextSegment
import com.github.jing332.tts.synthesizer.TtsConfiguration
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

internal class TextProcessor(val context: SynthesizerContext) : ITextProcessor {
    private var configs: Map<Long, TtsConfiguration> = mapOf()
    override fun init(
        context: Context,
        configs: Map<Long, TtsConfiguration>
    ): Result<Unit, TextProcessorError> {
        this.configs = configs
        return Ok(Unit)
    }

    override fun process(
        text: String,
        forceConfigId: TtsConfiguration?
    ): Result<List<TextSegment>, TextProcessorError> {
        return StringUtils.splitSentences(text).map {
            TextSegment(text = it, tts = configs.values.random())
        }.run { Ok(this) }
    }
}