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
import com.github.jing332.tts.speech.plugin.engine.VoiceCatalogFilterGroup
import com.github.jing332.tts.speech.plugin.engine.VoiceCatalogFilterOption
import com.github.jing332.tts.speech.plugin.engine.VoiceCatalogItem
import com.github.jing332.tts_server_android.JsConsoleManager
import com.github.jing332.tts_server_android.app
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PluginTtsViewModel(app: Application) : AndroidViewModel(app) {
    companion object {
        private val logger = KotlinLogging.logger { PluginTtsViewModel::class.java.name }

        /** 音色广场单页条数（插件侧上限 100，取 30 让首页更快回来） */
        const val CATALOG_PAGE_SIZE = 30
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
            // 换插件/重载：广场状态整体作废（含把在途请求的代数顶掉，晚到的结果一律丢弃）
            withMain { resetCatalog() }
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

                // 插件是否声明音色广场（opt-in 协议）：决定编辑页要不要显示入口。
                // 探测失败按不支持处理，不影响其它功能。
                val catalogSupported = engine.supportsVoiceCatalog()
                withMain { supportsVoiceCatalog = catalogSupported }

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

    fun updateCustomUI(locale: String, voice: String) {
        try {
            engine.onVoiceChanged(locale, voice)
        } catch (_: NoSuchMethodException) {
        }
    }

    // ===== 音色广场（opt-in 协议 searchVoiceCatalog；协议见 jread docs/plugin-editor-schema-v1.md）=====
    // 入口只在 supportsVoiceCatalog 为真时出现；链路 = 广场弹窗 → 本 VM → 插件 JS。

    /** 当前插件是否声明了音色广场（load() 时探测一次） */
    var supportsVoiceCatalog by mutableStateOf(false)
        private set

    var catalogLoading by mutableStateOf(false)
        private set
    var catalogLoaded by mutableStateOf(false)
        private set
    var catalogHasMore by mutableStateOf(false)
        private set
    var catalogTotal by mutableStateOf<Int?>(null)
        private set
    var catalogError by mutableStateOf<String?>(null)
        private set
    var catalogSortOptions by mutableStateOf<List<VoiceCatalogFilterOption>>(emptyList())
        private set

    val catalogItems = mutableStateListOf<VoiceCatalogItem>()
    val catalogQuickFilters = mutableStateListOf<VoiceCatalogFilterOption>()
    val catalogFilterGroups = mutableStateListOf<VoiceCatalogFilterGroup>()

    /**
     * 请求代次（jread 文档明写：过滤/分页结果不可混用不同请求代次）。
     * 切关键词/标签/排序都算新一代，旧请求即使晚到也按代次丢弃，绝不覆盖新结果。
     */
    private var catalogGeneration = 0
    private var catalogPage = 1

    /** 串行化插件 JS 调用：Rhino 实例非线程安全，且「加载更多」与「重新搜索」可能交叠 */
    private val catalogMutex = Mutex()

    private fun resetCatalog() {
        catalogGeneration++
        catalogItems.clear()
        catalogQuickFilters.clear()
        catalogFilterGroups.clear()
        catalogSortOptions = emptyList()
        catalogTotal = null
        catalogHasMore = false
        catalogLoaded = false
        catalogError = null
        catalogLoading = false
        catalogPage = 1
    }

    /**
     * 查询插件音色广场。
     *
     * @param append true=「加载更多」（续下一页、保留已有列表）；false=新条件第一页
     *
     * 三处刻意写法：
     * ① 插件 JS 的异常在**协程体内**收敛——runCatching 包在协程外面等于没包（09-17 闪退那次的教训，
     *    本函数整体即收敛点，调用方无需再包）；错误原文直接上抛给弹窗展示（未填 Key 之类是用户可解的）；
     * ② 锁内先比代次再跑 JS：被新一代取代的排队请求连 JS 都不必进；
     * ③ append 按 id 去重、零新增即视为到底，免得插件 hasMore 恒真时前端续页空转。
     */
    suspend fun searchCatalog(
        locale: String,
        keyword: String,
        tags: List<String>,
        sortBy: String,
        append: Boolean,
    ) {
        val generation = ++catalogGeneration
        val requestPage = if (append) catalogPage + 1 else 1
        withMain {
            catalogLoading = true
            if (!append) catalogError = null
        }
        try {
            catalogMutex.withLock {
                if (generation != catalogGeneration) return@withLock
                val result = withIO {
                    engine.searchVoiceCatalog(
                        locale = locale,
                        keyword = keyword,
                        tags = tags,
                        sortBy = sortBy,
                        page = requestPage,
                        pageSize = CATALOG_PAGE_SIZE,
                    )
                }
                if (generation != catalogGeneration) return@withLock
                withMain {
                    if (append) {
                        val known = catalogItems.map { it.id }.toHashSet()
                        val fresh = result.items.filter { it.id !in known }
                        catalogItems.addAll(fresh)
                        catalogHasMore = result.hasMore && fresh.isNotEmpty()
                    } else {
                        catalogItems.clear()
                        catalogItems.addAll(result.items)
                        catalogHasMore = result.hasMore && result.items.isNotEmpty()
                    }
                    catalogPage = result.page
                    catalogTotal = result.total
                    catalogLoaded = true
                    // 筛选结构每轮同源；插件某轮没返回就沿用上一轮，别让筛选区凭空消失
                    if (result.quickFilters.isNotEmpty() && (!append || catalogQuickFilters.isEmpty())) {
                        catalogQuickFilters.clear()
                        catalogQuickFilters.addAll(result.quickFilters)
                    }
                    if (result.filterGroups.isNotEmpty() && (!append || catalogFilterGroups.isEmpty())) {
                        catalogFilterGroups.clear()
                        catalogFilterGroups.addAll(result.filterGroups)
                    }
                    if (result.sortOptions.isNotEmpty() && (!append || catalogSortOptions.isEmpty())) {
                        catalogSortOptions = result.sortOptions
                    }
                }
            }
        } catch (t: Throwable) {
            // 协程取消不是"查询失败"：原样抛出去，别把 "Job was cancelled" 当错误文案弹给用户
            if (t is CancellationException) throw t
            if (generation == catalogGeneration) withMain {
                catalogError = t.message ?: t.toString()
                if (append) catalogHasMore = false
            }
        } finally {
            // NonCancellable：条件变更会把上一个查询协程取消，普通 withMain 在取消态下直接抛
            // CancellationException，loading 会永远卡住（与 load() 尾部同一处理）
            if (generation == catalogGeneration) {
                withContext(NonCancellable) { withMain { catalogLoading = false } }
            }
        }
    }
}
