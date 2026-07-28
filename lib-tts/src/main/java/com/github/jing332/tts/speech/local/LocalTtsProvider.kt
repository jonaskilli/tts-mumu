package com.github.jing332.tts.speech.local

import android.content.Context
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.tts.exception.EngineException
import com.github.jing332.tts.speech.EngineState
import com.github.jing332.tts.speech.TextToSpeechProvider
import com.github.jing332.tts.synthesizer.SystemParams
import com.github.michaelbull.result.onFailure
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.InputStream

class LocalTtsProvider(
    private val context: Context,
    private val engine: String,
) : TextToSpeechProvider<LocalTtsSource>() {
    companion object {
        const val TAG = "LocalTtsService"
        private val logger = KotlinLogging.logger(TAG)
    }

    private var mTts: AndroidTtsEngine? = null

    private val tts: AndroidTtsEngine
        get() = mTts ?: throw EngineException("Android TTS engine is not initialized")


    private fun init(params: SystemParams): AudioParams {
        return AudioParams(
            speed = params.speed.takeIf { it > 0 } ?: 1f,
            pitch = params.pitch.takeIf { it > 0 } ?: 1f,
            volume = params.volume.takeIf { it > 0 } ?: 1f
        )
    }


    override suspend fun getStream(params: SystemParams, source: LocalTtsSource): InputStream {
        return tts.getStream(
            params.text,
            locale = source.locale,
            voice = source.voice,
            extraParams = source.extraParams ?: emptyList(),
            init(params)
        ).onFailure {
            onDestroy()
            onInit()
            throw EngineException(it.toString())
        }.value
    }

    override suspend fun syncPlay(params: SystemParams, source: LocalTtsSource) {
        tts.play(
            params.text,
            locale = source.locale,
            voice = source.voice,
            extraParams = source.extraParams ?: emptyList(),
            params = init(params)
        ).onFailure {
            onDestroy()
            onInit()
        }
    }

    override var state: EngineState = EngineState.Uninitialized()

    override fun isSyncPlay(source: LocalTtsSource): Boolean {
        return source.isDirectPlayMode
    }


    override suspend fun onInit() {
        mTts = AndroidTtsEngine(context).apply { init(engine) }

        state = EngineState.Initialized
    }

    override fun onStop() {
    }

    override fun onDestroy() {
        mTts?.release()
        mTts = null
    }
}