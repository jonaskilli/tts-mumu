package com.github.jing332.tts_server_android.compose.systts.speechrule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.script.runtime.console.Console
import com.github.jing332.tts_server_android.model.rhino.speech_rule.SpeechRuleEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SpeechRuleEditViewModel(val app: Application) : AndroidViewModel(app) {
    private val _codeLiveData: MutableLiveData<String> = MutableLiveData()
    val codeLiveData: LiveData<String>
        get() = _codeLiveData

    private lateinit var mSpeechRule: SpeechRule
    private lateinit var mRuleEngine: SpeechRuleEngine
    private val console = Console(Console.LogSource.SPEECH_RULE)

    val speechRule: SpeechRule
        get() = mSpeechRule

    fun init(speechRule: SpeechRule, defaultCode: String) {
        if (speechRule.code.isBlank()) speechRule.code = defaultCode
        updateRule(speechRule) }

    fun updateRule(rule: SpeechRule) {
        mSpeechRule = rule
        _codeLiveData.value = rule.code
        mRuleEngine = SpeechRuleEngine(app, rule)
        mRuleEngine.console = console
    }

    fun updateCode(code: String) {
        updateRule(speechRule.copy(code = code))
    }

    fun getConsole(): Console {
        return console
    }

    fun evalRuleInfo(code: String) {
        updateCode(code)
        mRuleEngine.evalInfo()
    }

    fun debug(text: String) {
        evalRuleInfo(codeLiveData.value ?: throw IllegalStateException("code is null"))
        viewModelScope.launch(Dispatchers.IO) {
            kotlin.runCatching {
                getConsole().info("handleText()...")

                // 与朗读链 TtsRepository.getAllTts() 同口径：**全部启用项**（不限 target、含备用）。
                // 曾用 getEnabledListForSort(TAG)（仅自定义标签且非备用）→ 点一次「运行/调试」
                // 就会把 fayinren.json 整份覆写成该子集，发音人池当场塌房。字段也照朗读链补齐。
                val rules = dbm.systemTtsV2.getAllGroupWithTts()
                    .flatMap { it.list.sortedBy { t -> t.order } }
                    .filter { it.isEnabled }
                    .mapNotNull { systts ->
                        val cfg = systts.config as? TtsConfigurationDTO
                            ?: return@mapNotNull null
                        cfg.speechRule.apply {
                            configId = systts.id
                            voice = cfg.source.voice
                            displayName = systts.displayName
                        }
                    }
                val list = mRuleEngine.handleText(text, rules)
                try {
                    list.forEach {
                        val texts = mRuleEngine.splitText(it.text)
                        getConsole().info(
                            "\ntag=${it.tag}, id=${it.id}, text=${it.text.trim()}, splittedTexts=${
                                texts.joinToString(" | ").trim()
                            }"
                        )
                    }
                } catch (_: NoSuchMethodException) {
                }
            }.onFailure {
                getConsole().error(it.stackTraceToString())
            }
        }
    }

}