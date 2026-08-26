package com.github.jing332.tts_server_android.compose.systts.plugin

import android.content.Context
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.BasicAudioFormat
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.SpeechRuleInfo
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.database.entities.systts.source.TextToSpeechSource
import com.github.jing332.tts.speech.TextToSpeechProvider
import com.github.jing332.tts.speech.plugin.PluginTtsProvider
import com.github.jing332.tts.speech.plugin.engine.TtsPluginUiEngineV2
import com.github.jing332.tts_server_android.compose.systts.list.ui.resolveSampleRateBySynth
import com.github.jing332.tts_server_android.constant.SpeechTarget
import com.github.jing332.tts_server_android.model.rhino.speech_rule.SpeechRuleEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * 插件「按分类入库」：从插件管理界面入口一次性遍历插件全部分类（getLocales），
 * 拉取每个分类下所有音色（getVoices），映射为标准分类名后落为所选分组下的子分组。
 *
 * 与编辑页保存共用 resolveSampleRateBySynth；本类独立实例化引擎，不依赖任何已打开的编辑界面。
 */
object PluginCategoryImporter {

    /** 插件分类显示名 → 应用标准分类名的归一化别名表 */
    private val CATEGORY_ALIASES = listOf(
        "女性青年" to "女青年", "女青年通用" to "女青年",
        "男性青年" to "男青年", "男青年通用" to "男青年",
        "女特殊" to "特殊女", "女性特殊" to "特殊女",
        "男特殊" to "特殊男", "男性特殊" to "特殊男",
        "女性童声" to "女童", "儿童女" to "女童",
        "男性童声" to "男童", "儿童男" to "男童",
    )

    private val STANDARD_CATEGORIES =
        listOf("女童", "男童", "女青年", "男青年", "中年女", "中年男", "老年女", "老年男", "特殊女", "特殊男", "旁白")

    /** 归一化：去修饰后缀 → 精确命中标准名 → 别名表 → 标准词包含 → 兜底原名 */
    internal fun mapToStandardCategory(raw: String): String {
        val s = raw.trim().removeSuffix("通用").removeSuffix("发音人").trim()
        if (s in STANDARD_CATEGORIES) return s
        CATEGORY_ALIASES.firstOrNull { raw.contains(it.first) || s.contains(it.first) }
            ?.let { return it.second }
        STANDARD_CATEGORIES.firstOrNull { s.contains(it) }?.let { return it }
        return raw.trim()
    }

    /**
     * @param targetGroupId 目标分组（菜单入口先让用户选）
     * @param onProgress 进度回调，可从任意线程安全地更新 Compose 状态
     * @return 成功插入的配置项数量
     */
    suspend fun import(
        context: Context,
        plugin: Plugin,
        targetGroupId: Long,
        onProgress: (String) -> Unit = {},
    ): Int {
        // Rhino 引擎线程绑定：全程固定单线程执行引擎调用
        val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        try {
            return withContext(dispatcher) {
                importInternal(context, plugin, targetGroupId, onProgress)
            }
        } finally {
            dispatcher.close()
        }
    }

    private suspend fun importInternal(
        context: Context,
        plugin: Plugin,
        targetGroupId: Long,
        onProgress: (String) -> Unit,
    ): Int {
        val engine = TtsPluginUiEngineV2(context, plugin)
        try {
            engine.eval()
            engine.onLoad()

            // 标签规则：取排序最前的已启用规则；无规则则只建子分组不打朗读标签
            val speechRule: SpeechRule? = withContext(Dispatchers.IO) {
                dbm.speechRuleDao.getAllEnabledWithoutCode().firstOrNull()?.ruleId?.let {
                    dbm.speechRuleDao.getByRuleId(it)
                }
            }
            val ruleEngine = speechRule?.let {
                runCatching { SpeechRuleEngine(context, it).apply { eval() } }.getOrNull()
            }

            // 数据库操作切换 IO；引擎调用保持在当前单线程
            val provider =
                PluginTtsProvider(context, plugin).also { it.engine = engine } as TextToSpeechProvider<TextToSpeechSource>

            val baseId = System.currentTimeMillis()
            var idSeq = 0
            val baseOrder = withContext(Dispatchers.IO) {
                dbm.systemTtsV2.getByGroup(targetGroupId).maxOfOrNull { it.order } ?: -1
            } + 1
            var orderSeq = 0
            val categoryCountMap = mutableMapOf<String, Int>()
            var processed = 0

            engine.getLocales().forEach { (poolId, poolName) ->
                val category = mapToStandardCategory(poolName)
                if (category.isBlank()) return@forEach

                val voices = runCatching { engine.getVoices(poolId) }.getOrNull().orEmpty()
                    .filter { it.id.isNotBlank() }
                voices.forEach { voice ->
                    processed++
                    onProgress("正在导入 ${processed}：${voice.name}")

                    // 各分类序号起点接库中已有数量，避免重号
                    val cachedCount = categoryCountMap.getOrDefault(category, 0)
                    val existing = if (cachedCount == 0) {
                        withContext(Dispatchers.IO) {
                            dbm.systemTtsV2.getByGroup(targetGroupId).count { it.categoryPath == category }
                        }
                    } else cachedCount
                    val seq = existing + 1
                    categoryCountMap[category] = seq

                    // 标签优先由朗读规则生成；旁白为单一角色分类不带序号
                    val tagLabel = if (category == "旁白") category
                    else ruleEngine?.getCategoryTag(category, seq)
                        ?: (category + String.format(java.util.Locale.US, "%02d", seq))
                    val newRuleData = SpeechRuleInfo(
                        target = SpeechTarget.TAG,
                        tag = tagLabel,
                        tagName = runCatching {
                            ruleEngine?.getTagName(tagLabel, emptyMap())
                        }.getOrNull()?.takeIf { it.isNotBlank() } ?: tagLabel,
                        tagRuleId = speechRule?.ruleId ?: ""
                    )

                    // 采样率解析链：JS getAudioSampleRate → 实际合成解析
                    val sampleRate = runCatching {
                        engine.getSampleRate(poolId, voice.id) ?: 0
                    }.getOrNull()?.takeIf { it > 0 } ?: resolveSampleRateBySynth(
                        provider = provider,
                        config = TtsConfigurationDTO(),
                        tts = PluginTtsSource(pluginId = plugin.pluginId, locale = poolId),
                        voiceId = voice.id
                    )
                    val needDecode = runCatching {
                        engine.isNeedDecode(poolId, voice.id)
                    }.getOrNull() ?: true

                    val newConfig = TtsConfigurationDTO(
                        source = PluginTtsSource(
                            pluginId = plugin.pluginId,
                            locale = poolId,
                            voice = voice.id
                        ),
                        speechRule = newRuleData,
                        audioFormat = BasicAudioFormat(sampleRate = sampleRate, isNeedDecode = needDecode)
                    )

                    // 批量导入默认不启用：避免未知发音人立刻影响当前朗读
                    withContext(Dispatchers.IO) {
                        dbm.systemTtsV2.insert(
                            SystemTtsV2(
                                id = baseId + idSeq++,
                                displayName = voice.name,
                                groupId = targetGroupId,
                                isEnabled = false,
                                order = baseOrder + orderSeq++,
                                categoryPath = category,
                                config = newConfig
                            )
                        )
                    }
                }
            }
            return idSeq
        } finally {
            runCatching { engine.destroy() }
        }
    }
}
