package com.github.jing332.tts.speech

import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.source.TextToSpeechSource

interface DecoderInfo<T : TextToSpeechSource> {
    fun apply(source: T): AudioParams
}