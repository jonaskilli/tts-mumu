package com.github.jing332.tts.speech

import com.github.jing332.database.entities.systts.source.TextToSpeechSource
import com.github.jing332.tts.synthesizer.SystemParams
import java.io.InputStream

abstract class TextToSpeechProvider<in T : TextToSpeechSource> : ILifeState {
    abstract var state: EngineState
    open fun isSyncPlay(source: T): Boolean {
        return false
    }

    override fun onStop() {
    }


    abstract suspend fun getStream(params: SystemParams, source: T): InputStream

    open suspend fun syncPlay(params: SystemParams, source: T) {
        TODO("not yet implemented")
    }
}