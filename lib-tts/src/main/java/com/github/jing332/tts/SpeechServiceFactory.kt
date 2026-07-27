package com.github.jing332.tts

import android.content.Context
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.source.TextToSpeechSource
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts.speech.TextToSpeechProvider
import com.github.jing332.tts.speech.local.LocalTtsProvider
import com.github.jing332.tts.speech.plugin.PluginTtsProvider

@Suppress("UNCHECKED_CAST")
object SpeechServiceFactory {
    fun createEngine(context: Context, source: TextToSpeechSource): TextToSpeechProvider<TextToSpeechSource>? {
        return when (source) {
            is LocalTtsSource -> LocalTtsProvider(context, source.engine)
            is PluginTtsSource -> {
                PluginTtsProvider(
                    context,
                    source.plugin ?: dbm.pluginDao.getEnabled(source.pluginId) ?: return null
                )
            }

            else -> null
        } as TextToSpeechProvider<TextToSpeechSource>?
    }
}