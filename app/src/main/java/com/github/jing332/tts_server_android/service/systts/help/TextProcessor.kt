package com.github.jing332.tts_server_android.service.systts.help

import android.content.Context
import com.github.jing332.common.utils.StringUtils
import com.github.jing332.database.constants.ReplaceExecution
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.SpeechRuleInfo
import com.github.jing332.tts.ConfigType
import com.github.jing332.tts.error.TextProcessorError
import com.github.jing332.tts.synthesizer.ITextProcessor
import com.github.jing332.tts.synthesizer.TextSegment
import com.github.jing332.tts.synthesizer.TtsConfiguration
import com.github.jing332.common.LogEntry
import com.github.jing332.common.LogLevel
import com.github.jing332.script.runtime.console.Console
import com.github.jing332.tts_server_android.conf.SystemTtsConfig
import com.github.jing332.tts_server_android.model.rhino.speech_rule.SpeechRuleEngine
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.random.Random

class TextProcessor : ITextProcessor {
    companion object {
        private val logger = KotlinLogging.logger { this::class.java.name }
    }

    private var isMultiVoice: Boolean = false

    private val isSplitSentence: Boolean
        get() = SystemTtsConfig.isSplitEnabled.value

    private val isReplaceEnabled: Boolean
        get() = SystemTtsConfig.isReplaceEnabled.value

    private lateinit var engine: SpeechRuleEngine
    private val textReplacer = TextReplacer()

    private var configs: List<TtsConfiguration> = emptyList()
    private var speechRules: List<SpeechRuleInfo> = emptyList()
    private val random by lazy { Random(System.currentTimeMillis()) }

    override fun init(
        context: Context,
        configs: Map<Long, TtsConfiguration>,
    ): Result<Unit, TextProcessorError> {
        isMultiVoice = SystemTtsConfig.isMultiVoiceEnabled.value
        logger.debug { "isMultiVoice=$isMultiVoice, configs.size=${configs.size}" }
        if (isMultiVoice) {
            val ruleId = configs.values.toList().component1().speechInfo.tagRuleId
            val speechRule =
                dbm.speechRuleDao.getByRuleId(ruleId)
                    ?: return Err(TextProcessorError.MissingRule(ruleId))
            logger.debug { "speechRule loaded: ruleId=${speechRule.ruleId}, name=${speechRule.name}" }
            engine = SpeechRuleEngine(context, speechRule)
            // 必须在 eval() 之前设置 console，否则 JavaScript 绑定的是默认 Console
            logger.debug { "Before setting console: source=${engine.console.source}" }
            engine.console = Console(Console.LogSource.SPEECH_RULE)
            logger.debug { "After setting console: source=${engine.console.source}" }
            engine.eval()
            // 测试朗读规则日志是否工作 - 使用引擎的console
            engine.console.info("[测试] 朗读规则引擎初始化完成: ruleId=${speechRule.ruleId}")
            // 额外的直接测试
            Console.globalSpeechRuleLogListener?.let {
                it(LogEntry(level=LogLevel.INFO, message="[直接测试] globalSpeechRuleLogListener可用", isSpeechRuleLog=true))
            }
            this.configs =
                configs.entries.map { it.value.copy(speechInfo = it.value.speechInfo.copy(configId = it.key)) }
            speechRules = this.configs.map { it.speechInfo }
        } else {
            this.configs = configs.values.toList()
            if (this.configs.isEmpty())
                return Err(TextProcessorError.MissingConfig(ConfigType.SINGLE_VOICE))
        }

        loadReplacer()
        return Ok(Unit)
    }

    fun loadReplacer() {
        textReplacer.load()
    }

    private fun splitText(text: String): List<String> {
        return if (!isSplitSentence) listOf(text)
        else if (isMultiVoice) {
            try {
                engine.splitText(text).map { it.toString() }
            } catch (_: NoSuchMethodException) {
                StringUtils.splitSentences(text)
            }
        } else {
            StringUtils.splitSentences(text)
        }
    }

    private fun replace(text: String, @ReplaceExecution execution: Int): String {
        return if (isReplaceEnabled)
            textReplacer.replace(text, execution)
        else
            text
    }

    override fun process(
        text: String,
        presetConfig: TtsConfiguration?,
    ): Result<List<TextSegment>, TextProcessorError> {
        val resultList = mutableListOf<TextSegment>()
        val replacedText = replace(text, ReplaceExecution.BEFORE)

        fun add(vararg fragments: TextSegment) {
            fragments.forEach { f ->
                resultList.add(
                    TextSegment(text = replace(f.text, ReplaceExecution.AFTER), f.tts)
                )
            }
        }

        fun splitAndAdd(text: String, config: TtsConfiguration) {
            splitText(text).forEach {
                add(TextSegment(text = it, tts = config))
            }
        }

        try {

            if (presetConfig != null) {
                splitAndAdd(text, presetConfig)
            } else if (isMultiVoice) {
                val fragments = engine.handleText(replacedText, speechRules)

                fragments.forEach { txtWithTag ->
                    if (txtWithTag.text.isNotBlank()) {
                        val sameTagList = configs.filter {
                            !it.speechInfo.isStandby && it.speechInfo.tag == txtWithTag.tag
                        }
                        val configFromId =
                            sameTagList.find { it.speechInfo.configId == txtWithTag.id }

                        // Exact match ID > random match in tag > random match in all
                        val config = configFromId
                            ?: sameTagList.randomOrNull(random)
                            ?: configs.randomOrNull(random)
                            ?: return Err(
                                TextProcessorError.MissingConfig(
                                    ConfigType.TAG,
                                    "tag=${txtWithTag.tag}, id=${txtWithTag.id}"
                                )
                            )
                        splitAndAdd(txtWithTag.text, config)
                    }
                }
            } else {
                val singleVoice = configs.randomOrNull(random) ?: return Err(
                    TextProcessorError.MissingConfig(
                        ConfigType.SINGLE_VOICE, "single voice"
                    )
                )
                splitAndAdd(replacedText, singleVoice)
            }
        } catch (e: UninitializedPropertyAccessException) {
            return Err(TextProcessorError.Initialization)
        } catch (e: Exception) {
            return Err(TextProcessorError.HandleText(e))
        }

        return Ok(resultList)
    }
}