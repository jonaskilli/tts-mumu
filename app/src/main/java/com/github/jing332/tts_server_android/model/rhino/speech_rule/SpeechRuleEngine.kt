package com.github.jing332.tts_server_android.model.rhino.speech_rule

import android.content.Context
import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.database.entities.TagsDataMap
import com.github.jing332.database.entities.systts.SpeechRuleInfo

import com.github.jing332.script.runtime.console.Console
import com.github.jing332.script.simple.SimpleScriptEngine
import com.github.jing332.script.source.toScriptSource
import com.github.jing332.tts_server_android.R
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.ScriptRuntime

class SpeechRuleEngine(
    val context: Context,
    private val rule: SpeechRule
) {
    companion object {
        const val OBJ_JS = "SpeechRuleJS"

        const val FUNC_GET_TAG_NAME = "getTagName"
        const val FUNC_HANDLE_TEXT = "handleText"
        const val FUNC_SPLIT_TEXT = "splitText"

        fun getTagName(context: Context, speechRule: SpeechRule, info: SpeechRuleInfo): String {
            val engine = SpeechRuleEngine(context, speechRule)
            engine.eval()

            val tagName = try {
                engine.getTagName(info.tag, info.tagData)
            } catch (_: NoSuchMethodException) {
                speechRule.tags[info.tag] ?: ""
            }

            return tagName
        }
    }

    val engine = SimpleScriptEngine(context, rule.ruleId)
    var console: Console
        get() = engine.runtime.console
        set(value) {
            engine.runtime.console = value
        }


    private val objJS
        get() = engine.get(OBJ_JS) as NativeObject

    fun eval() {
        engine.execute(rule.code.toScriptSource())
    }

    @Suppress("UNCHECKED_CAST")
    fun evalInfo() {
        eval()
        objJS.apply {
            rule.name = get("name").toString()
            rule.ruleId = get("id").toString()
            rule.author = get("author").toString()

            rule.tags = get("tags") as Map<String, String>

            rule.tagsData =
                getOrDefault(
                    "tagsData",
                    emptyMap<String, Map<String, Map<String, String>>>()
                ) as TagsDataMap

            runCatching {
                rule.version = ScriptRuntime.toNumber(get("version")).toInt()
            }.onFailure {
                throw NumberFormatException(context.getString(R.string.plugin_bad_format))
            }
        }
    }

    fun getTagName(tag: String, tagMap: Map<String, String>): String {
        return engine.invokeMethod(objJS, FUNC_GET_TAG_NAME, tag, tagMap).toString()
    }

    data class TagData(val id: String, val value: String)

    fun handleText(text: String, list: List<SpeechRuleInfo> = emptyList()): List<TextWithTag> {
        val tagsDataMap: MutableMap<String, MutableMap<String, MutableList<Map<String, String>>>> =
            mutableMapOf()
        list.forEach { info ->
            if (tagsDataMap[info.tag] == null)
                tagsDataMap[info.tag] = mutableMapOf()

            info.tagData.forEach {
                if (tagsDataMap[info.tag]!![it.key] == null)
                    tagsDataMap[info.tag]!![it.key] = mutableListOf()

                tagsDataMap[info.tag]!![it.key]!!.add(
                    mapOf(
                        "id" to info.configId.toString(),
                        "value" to it.value
                    )
                )
            }
        }
        return handleText(text, tagsDataMap)
    }

    /** ['dialogue']['role']= List<TagData>
     *@param tagsDataSet 例： key: dialogue, value: map(key: role, value: [{tagDataId:111, 张三, 李四])
     */
    fun handleText(
        text: String,
        tagsDataMap: Map<String, Map<String, List<Map<String, String>>>>
    ): List<TextWithTag> {
        val resultList: MutableList<TextWithTag> = mutableListOf()
        engine.invokeMethod(objJS, FUNC_HANDLE_TEXT, text, tagsDataMap)
            ?.run { this as List<*> }
            ?.let { list ->
                list.forEach {
                    if (it is Map<*, *>) {
                        resultList.add(
                            TextWithTag(
                                it["text"].toString(),
                                it["tag"].toString(),
                                it.getOrDefault("id", 0).toString().toLong(),
                            )
                        )
                    }
                }

            }
        return resultList
    }

    @Suppress("UNCHECKED_CAST")
    fun splitText(text: String): List<CharSequence> {
        return engine.invokeMethod(
            objJS,
            FUNC_SPLIT_TEXT,
            text
        ) as List<CharSequence>
    }

    data class TextWithTag(val text: String, val tag: String, val id: Long)
}