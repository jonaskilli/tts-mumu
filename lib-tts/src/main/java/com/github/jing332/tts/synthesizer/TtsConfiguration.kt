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

    /**
     * standbyConfig 的来路，仅当 standbyConfig != null 时有意义：
     * true  = 性别/中性兜底（借用 duihuaA/duihuaB/括号4 的配置顶班），
     * false = 用户显式勾选「作为备用引擎」的备用配置。
     * 两者共用 standbyConfig 一个字段（重试切换入口只有一处），但语义不同：
     * 兜底借来的是平时正常在用的配置（如括号4），日志若一并叫「备用发音人」，
     * 会让人误以为日常发音人变成了替补。
     */
    val standbyIsFallback: Boolean = false,
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