package com.github.jing332.tts.synthesizer

import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.BasicAudioFormat
import com.github.jing332.database.entities.systts.SpeechRuleInfo
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.TextToSpeechSource

data class TtsConfiguration(
    val speechInfo: SpeechRuleInfo = SpeechRuleInfo(),
    val audioParams: AudioParams = AudioParams(),
    val audioFormat: BasicAudioFormat = BasicAudioFormat(),
    val source: TextToSpeechSource,
    val tag: Any? = null,

    val standbyConfig: TtsConfiguration? = null,
) {
    fun shouldDecode(): Boolean {
        return source.shouldDecode(audioFormat)
    }

    companion object {
        fun TtsConfigurationDTO.toVO(): TtsConfiguration {
            return TtsConfiguration(
                speechInfo = speechRule,
                audioParams = audioParams,
                audioFormat = audioFormat,
                source = source,
                standbyConfig = null,
            )
        }

        fun TtsConfiguration.toDTO(): TtsConfigurationDTO {
            return TtsConfigurationDTO(
                speechRule = speechInfo,
                audioParams = audioParams,
                audioFormat = audioFormat,
                source = source,
            )
        }
    }
}