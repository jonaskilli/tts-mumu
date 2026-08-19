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

    // 插件JS已自行处理该项参数时为 true（来自插件表设置），
    // 朗读时 Sonic 不再叠加该项，避免双重生效
    val pluginHandlesSpeed: Boolean = false,
    val pluginHandlesVolume: Boolean = false,
    val pluginHandlesPitch: Boolean = false,

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