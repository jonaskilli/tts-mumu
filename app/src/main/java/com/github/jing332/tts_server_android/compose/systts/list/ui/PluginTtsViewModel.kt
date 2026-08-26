package com.github.jing332.tts_server_android.compose.systts.list.ui

import android.app.Application
import android.content.Context
import android.widget.LinearLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drake.net.utils.withIO
import com.drake.net.utils.withMain
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.database.entities.systts.source.TextToSpeechSource
import com.github.jing332.tts.speech.TextToSpeechProvider
import com.github.jing332.tts.speech.plugin.PluginTtsProvider
import com.github.jing332.tts.speech.plugin.TtsPluginEngineManager
import com.github.jing332.tts.speech.plugin.engine.TtsPluginUiEngineV2
import com.github.jing332.tts_server_android.JsConsoleManager
import com.github.jing332.tts_server_android.app
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PluginTtsViewModel(app: Application) : AndroidViewModel(app) {
    companion object {
        private val logger = KotlinLogging.logger { PluginTtsViewModel::class.java.name }
    }

    private val engineLock = Any()

    @Volatile
    lateinit var engine: TtsPluginUiEngineV2
    val pluginList = mutableStateListOf<Plugin>()

    fun loadPluginList() {
        viewModelScope.launch(Dispatchers.IO) {
            val plugins = dbm.pluginDao.allEnabled
            withMain {
                pluginList.clear()
                pluginList.addAll(plugins)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun service(): TextToSpeechProvider<TextToSpeechSource> {
        return PluginTtsProvider(getApplication<Application>() as Context, engine.plugin).also {
            it.engine = engine
        } as TextToSpeechProvider<TextToSpeechSource>
    }

    private fun initEngine(plugin: Plugin?, source: PluginTtsSource) {
        synchronized(engineLock) {
            if (this::engine.isInitialized) {
                val samePlugin = if (plugin != null) engine.plugin.pluginId == plugin.pluginId
                else engine.plugin.pluginId == source.pluginId
                if (samePlugin) {
                    // 早返回时也更新 source，避免 JS 读写旧对象导致数据丢失
                    engine.source = source
                    return
                }
            }

            engine = if (plugin == null)
                TtsPluginEngineManager.get(getApplication<Application>() as Context, getPluginFromDB(source.pluginId))
            else TtsPluginUiEngineV2(getApplication<Application>() as Context, plugin).apply { eval() }

            engine.console = JsConsoleManager.ui
            engine.source = source
        }
    }

    private fun getPluginFromDB(id: String) =
        dbm.pluginDao.getEnabled(pluginId = id)
            ?: throw IllegalStateException("Plugin $id not found from database")

    // 修正：初始化为 false，确保 UI 层能初步渲染容器以触发 load
    var isLoading by mutableStateOf(false)
    val locales = mutableStateListOf<Pair<String, String>>()
    val voices = mutableStateListOf<TtsPluginUiEngineV2.Voice>()

    suspend fun load(
        context: Context,
        plugin: Plugin?,
        source: PluginTtsSource,
        linearLayout: LinearLayout,
    ) =
        withIO {
            withMain { isLoading = true }
            try {
                initEngine(plugin, source)
                engine.onLoadData()

                withMain {
                    linearLayout.removeAllViews() // 修正：清理可能残留的旧插件 UI
                    engine.onLoadUI(context, linearLayout)
                }

                updateLocales()
                // 切换插件后 locale 为空：自动选中第一个语言，避免声音列表残留上一插件内容
                val effectiveLocale = if (source.locale.isBlank()) {
                    locales.firstOrNull()?.first ?: ""
                } else source.locale
                updateVoices(effectiveLocale)

                // 修正：初始加载时如果已选择声音，触发 onVoiceChanged 以加载风格选项等自定义UI
                if (source.voice.isNotBlank() && source.locale.isNotBlank()) {
                    updateCustomUI(source.locale, source.voice)
                }
            } catch (t: Throwable) {
                throw t
            } finally {
                // 用 NonCancellable 确保协程取消时也能清理 loading 状态，避免卡在加载态
                withContext(NonCancellable) { withMain { isLoading = false } }
            }
        }

    private suspend fun updateLocales() {
        val list = engine.getLocales().toList()
        withMain {
            locales.clear()
            locales.addAll(list)
        }
    }

    suspend fun updateVoices(locale: String) {
        if (locale.isBlank()) return // 修正：空语言不触发更新
        val list = engine.getVoices(locale).toList()
        withMain {
            voices.clear()
            voices.addAll(list)
        }
    }

    // 逐分类（语言池）拉取全部音色：返回 (localeId, 分类显示名, 该池音色列表)，
    // 供「全部分类入库」一次性遍历所有分类，无需逐个切换语言栏；
    // 分类显示名已映射为标准分类名（映射不出时保留插件原名）
    suspend fun allLocalesVoices(): List<Triple<String, String, List<TtsPluginUiEngineV2.Voice>>> {
        val out = ArrayList<Triple<String, String, List<TtsPluginUiEngineV2.Voice>>>()
        locales.forEach { (localeId, displayName) ->
            runCatching { engine.getVoices(localeId).toList() }.getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?.let { out.add(Triple(localeId, mapToStandardCategory(displayName), it)) }
        }
        return out
    }

    fun updateCustomUI(locale: String, voice: String) {
        try {
            engine.onVoiceChanged(locale, voice)
        } catch (_: NoSuchMethodException) {
        }
    }
}

/** 插件分类显示名 → 应用标准分类名的归一化：别名表 → 标准词包含 → 兜底原名 */
private val CATEGORY_ALIASES = listOf(
    "女性青年" to "女青年", "女青年通用" to "女青年",
    "男性青年" to "男青年", "男青年通用" to "男青年",
    "女特殊" to "特殊女", "女性特殊" to "特殊女",
    "男特殊" to "特殊男", "男性特殊" to "特殊男"
)

private fun mapToStandardCategory(raw: String): String {
    // 去掉常见修饰后缀，便于精确命中标准名
    val s = raw.trim().removeSuffix("通用").removeSuffix("发音人").trim()
    if (s in com.github.jing332.compose.widgets.VoiceCategories.ALL) return s
    CATEGORY_ALIASES.firstOrNull { raw.contains(it.first) || s.contains(it.first) }
        ?.let { return it.second }
    // 分类名里直接出现标准词即采用；完全不含时保留插件原名作为新子分组
    com.github.jing332.compose.widgets.VoiceCategories.ALL.firstOrNull { s.contains(it) }
        ?.let { return it }
    return raw.trim()
}
