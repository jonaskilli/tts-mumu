package com.github.jing332.tts.speech.plugin.engine

import android.content.Context
import android.util.Log // 👈 使用原生 Log 替代 KotlinLogging
import android.widget.LinearLayout
import com.github.jing332.common.utils.dp
import com.github.jing332.common.utils.toCountryFlagEmoji
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.script.toMap
import com.github.jing332.script.withRhinoContext
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.javascript.ScriptRuntime
import org.mozilla.javascript.ScriptableObject
import java.util.Locale

class TtsPluginUiEngineV2(context: Context, plugin: Plugin) : TtsPluginEngineV2(context, plugin) {
    companion object {
        private const val TAG = "TtsPluginUiEngineV2"

        const val FUNC_SAMPLE_RATE = "getAudioSampleRate"
        const val FUNC_IS_NEED_DECODE = "isNeedDecode"

        const val FUNC_LOCALES = "getLocales"
        const val FUNC_VOICES = "getVoices"

        const val FUNC_ON_LOAD_UI = "onLoadUI"
        const val FUNC_ON_LOAD_DATA = "onLoadData"
        const val FUNC_ON_VOICE_CHANGED = "onVoiceChanged"

        const val OBJ_UI_JS = "EditorJS"

        /** 音色广场（opt-in 协议，jread docs/plugin-editor-schema-v1.md「分类选择链路」） */
        const val FUNC_SEARCH_VOICE_CATALOG = "searchVoiceCatalog"

        /** 单页条数上限：协议侧就是 1..100（Fish Audio 的 pageSize 校验区间），越界钳住 */
        const val MAX_CATALOG_PAGE_SIZE = 100

        private const val DEFAULT_LOCALE = "zh-CN"
        private const val DEFAULT_CATALOG_SORT = "score"
        private const val CATALOG_SCRIPT_NAME = "TtsVoiceCatalog"
    }

    fun dp(px: Int): Int {
        return px.dp
    }

    private val editUiJsObject: ScriptableObject by lazy {
        engine.get(OBJ_UI_JS) as? ScriptableObject
            ?: throw IllegalStateException("$OBJ_UI_JS not found")
    }

    override fun execute(script: String): Any? {
        return super.execute(PackageImporter.default + script)
    }


    fun getSampleRate(locale: String, voice: String): Int? {
        return engine.invokeMethod(
            editUiJsObject,
            FUNC_SAMPLE_RATE,
            locale,
            voice
        )?.run {
            return if (this is Int) this
            else (this as Double).toInt()
        }
    }

    fun isNeedDecode(locale: String, voice: String): Boolean {
        return try {
            engine.invokeMethod(editUiJsObject, FUNC_IS_NEED_DECODE, locale, voice)?.run {
                if (this is Boolean) this
                else (this as Double).toInt() == 1
            } ?: true
        } catch (_: NoSuchMethodException) {
            true
        }
    }

    fun getLocales(): Map<String, String> {
        return engine.invokeMethod(editUiJsObject, FUNC_LOCALES).run {
            when (this) {
                is List<*> -> this.associate {
                    val raw = it.toString()
                    val locale = Locale.forLanguageTag(raw)
                    // 插件可能直接返回中文池名（如猫箱 VV 核心的"旁白/性格无通用"），
                    // forLanguageTag 解析不出语言时 displayName 为空，兜底显示原文
                    val displayName = locale.displayName.ifBlank { raw }
                    raw to (locale.country.toCountryFlagEmoji() + " " + displayName).trim()
                }

                is Map<*, *> -> {
                    this.map { (key, value) ->
                        key.toString() to value.toString()
                    }.toMap()
                }

                else -> emptyMap()
            }
        }
    }

    fun getVoices(locale: String): List<Voice> {
        return engine.invokeMethod(editUiJsObject, FUNC_VOICES, locale).run {
            when (this) {
                is ScriptableObject -> {
                    toMap<Any, Any>().map { (key, value) ->
                        ScriptRuntime.toString(key) to value
                    }.map { (key, value) ->
                        var icon: String? = null
                        var name: String = if (value is CharSequence) value.toString() else ""

                        if (value is ScriptableObject) {
                            icon = value.get("iconUrl")?.toString()
                                ?: value.get("icon")?.toString()

                            name = value.get("name")?.toString() ?: name
                        }


                        Voice(key.toString(), name.toString(), icon)
                    }
                }

                else -> emptyList()
            }
        }
    }

    fun onLoadData() {
        try {
            engine.invokeMethod(editUiJsObject, FUNC_ON_LOAD_DATA)
        } catch (_: NoSuchMethodException) {
        }
    }

    fun onLoadUI(context: Context, container: LinearLayout) {
        try {
            engine.invokeMethod(
                editUiJsObject,
                FUNC_ON_LOAD_UI,
                context,
                container
            )
        } catch (_: NoSuchMethodException) {
        }
    }

    fun onVoiceChanged(locale: String, voice: String) {
        try {
            engine.invokeMethod(
                editUiJsObject,
                FUNC_ON_VOICE_CHANGED,
                locale,
                voice
            )
        } catch (_: NoSuchMethodException) {
        }
    }

    // ===== 音色广场（opt-in 协议 searchVoiceCatalog）=====

    /**
     * 插件是否声明了音色广场——决定编辑页要不要显示「音色广场」入口。
     * 用引擎实测而不是正则扫源码：插件经 PackageImporter 注入后的真实形态以引擎为准。
     */
    fun supportsVoiceCatalog(): Boolean = runCatching {
        evalInPluginScope(
            "(typeof EditorJS !== 'undefined' && !!EditorJS && typeof EditorJS.searchVoiceCatalog === 'function')" +
                " || (typeof PluginJS !== 'undefined' && !!PluginJS && typeof PluginJS.searchVoiceCatalog === 'function')"
        ) as? Boolean ?: false
    }.getOrDefault(false)

    /**
     * 查询插件音色广场（opt-in 协议，见 jread docs/plugin-editor-schema-v1.md「分类选择链路」）。
     *
     * 桥的两头都用 JSON 说话（query 以 JSON 注入、结果 JSON.stringify 回传），一次绕开 Rhino 的
     * NativeArray 整数下标、NativeObject vs Map、数值型 Double 等全部形态差异——插件刻意把
     * items/quickFilters/filterGroups 返成 keyed object，就是被这些坑逼出来的（见插件源码注释）。
     *
     * 插件自身抛错（未填 API Key、网络失败、未实现协议）会原样冒出来，由调用方在**协程体内**收敛。
     */
    fun searchVoiceCatalog(
        locale: String,
        keyword: String,
        tags: List<String>,
        sortBy: String,
        page: Int,
        pageSize: Int,
    ): VoiceCatalogPage {
        val queryJson = JSONObject()
            .put("locale", locale.ifBlank { DEFAULT_LOCALE })
            .put("keyword", keyword.trim())
            .put("tags", JSONArray(tags))
            .put("sortBy", sortBy.trim().ifBlank { DEFAULT_CATALOG_SORT })
            .put("page", page.coerceAtLeast(1))
            .put("pageSize", pageSize.coerceIn(1, MAX_CATALOG_PAGE_SIZE))
            .toString()
        val raw = evalInPluginScope(catalogBridgeScript(queryJson))?.toString().orEmpty()
        return VoiceCatalogJson.parse(raw)
    }

    /**
     * 在**插件自己的作用域**里求值一段一次性脚本。
     *
     * 为什么不走 [execute]：RhinoScriptEngine.execute 每次都重建作用域
     * （`scope = cx.newObject(globalScope)`），跑完之后再取 PluginJS 就是 NOT_FOUND——
     * 那条路只有初始化 eval() 能走，之后再 execute 会把 getAudio 整条链打断。
     * 这里直接借现成的 scope 求值，不碰作用域。
     */
    private fun evalInPluginScope(script: String): Any? {
        val scope = engine.scope ?: throw IllegalStateException("插件引擎尚未初始化")
        return withRhinoContext { cx ->
            cx.evaluateString(scope, script, CATALOG_SCRIPT_NAME, 1, null)
        }
    }

    /**
     * 桥脚本：解析 query → 交给插件 → 结果 stringify。
     * 兼容两点：① 插件只挂 PluginJS 或直接挂全局函数；② 旧 searchVoices 回的是 voices。
     */
    private fun catalogBridgeScript(queryJson: String): String = """
        (function () {
            var q = JSON.parse(${JSONObject.quote(queryJson)});
            var owner = null;
            if (typeof EditorJS !== 'undefined' && EditorJS && typeof EditorJS.searchVoiceCatalog === 'function') {
                owner = EditorJS;
            } else if (typeof PluginJS !== 'undefined' && PluginJS && typeof PluginJS.searchVoiceCatalog === 'function') {
                owner = PluginJS;
            }
            if (!owner) throw new Error('插件未提供音色广场接口 searchVoiceCatalog(query)');
            var r = owner.searchVoiceCatalog(q) || {};
            if (r.items === undefined && r.voices !== undefined) {
                r = {
                    items: r.voices, page: r.page, hasMore: r.hasMore, total: r.total,
                    quickFilters: r.quickFilters, filterGroups: r.filterGroups, sortOptions: r.sortOptions
                };
            }
            return JSON.stringify(r);
        })()
    """.trimIndent()

    data class Voice(val id: String, val name: String, val icon: String? = null)
}
