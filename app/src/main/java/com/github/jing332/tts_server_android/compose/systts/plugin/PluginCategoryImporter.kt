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
 * 拉取每个分类下所有音色（getVoices）落为所选分组下的子分组。
 * 分类名可映射到标准人群词（男女童少青中老/特殊/旁白/男女主）则归一命名并打朗读标签；
 * 无法映射的性格类等分类原样入库，不带标签。
 *
 * 与编辑页保存共用 resolveSampleRateBySynth；本类独立实例化引擎，不依赖任何已打开的编辑界面。
 */
object PluginCategoryImporter {

    /** 标准人群关键词：与列表页整理标签共用同一套词表（取最长匹配） */
    private val TAG_KEYWORDS = listOf(
        "女童", "少女", "女青年", "女中年", "女老年",
        "男童", "少年", "男青年", "男中年", "男老年",
        "特殊女", "特殊男", "女主", "男主", "旁白"
    )

    /** 这些前缀的标签在朗读规则里不补零（男主1…男主20），与其余两位补零一致 */
    private val NO_ZERO_PAD_PREFIXES = setOf("男主", "特殊男", "特殊女")

    /**
     * 插件分类名 → 标准人群名；不可映射返回 null（调用方原样入库且不打标签）。
     * 先归一"女性/男性"与常见修饰后缀，再按最长关键词命中。
     */
    internal fun mapTagCategory(raw: String): String? {
        var s = raw.trim().removeSuffix("通用").removeSuffix("发音人").removeSuffix("音色").trim()
        s = s.replace("女性", "女").replace("男性", "男")
        return TAG_KEYWORDS.filter { s.contains(it) }.maxByOrNull { it.length }
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
                val rawName = poolName.trim()
                if (rawName.isBlank()) return@forEach
                // 可映射 → 标准人群名（序号+标签）；不可映射 → 原样名、无标签
                val category = mapTagCategory(rawName)
                val subGroupName = category ?: rawName

                val voices = runCatching { engine.getVoices(poolId) }.getOrNull().orEmpty()
                    .filter { it.id.isNotBlank() }
                voices.forEach { voice ->
                    processed++
                    onProgress("正在导入 ${processed}：${voice.name}")

                    // 各子分组序号起点接库中已有数量，避免重号
                    val cachedCount = categoryCountMap.getOrDefault(subGroupName, 0)
                    val existing = if (cachedCount == 0) {
                        withContext(Dispatchers.IO) {
                            dbm.systemTtsV2.getByGroup(targetGroupId).count { it.categoryPath == subGroupName }
                        }
                    } else cachedCount
                    val seq = existing + 1
                    categoryCountMap[subGroupName] = seq

                    // 无映射分类不打标签（空 SpeechRuleInfo）；旁白为单一角色分类不带序号
                    val newRuleData = if (category == null) SpeechRuleInfo()
                    else {
                        val tagLabel = if (category == "旁白") category
                        else ruleEngine?.getCategoryTag(category, seq)
                            ?: (category + if (category in NO_ZERO_PAD_PREFIXES)
                                seq.toString()
                            else String.format(java.util.Locale.US, "%02d", seq))
                        SpeechRuleInfo(
                            target = SpeechTarget.TAG,
                            tag = tagLabel,
                            tagName = runCatching {
                                ruleEngine?.getTagName(tagLabel, emptyMap())
                            }.getOrNull()?.takeIf { it.isNotBlank() } ?: tagLabel,
                            tagRuleId = speechRule?.ruleId ?: ""
                        )
                    }

                    val needDecode = runCatching {
                        engine.isNeedDecode(poolId, voice.id)
                    }.getOrNull() ?: true

                    // 模板配置：DTO 的 source 声明为基类 TextToSpeechSource，
                    // 具体类型用局部变量持有（基类无 copy），合成解析与落库各取所需
                    val src = PluginTtsSource(pluginId = plugin.pluginId, locale = poolId)
                    val templateConfig = TtsConfigurationDTO(
                        source = src,
                        speechRule = newRuleData,
                        audioFormat = BasicAudioFormat(isNeedDecode = needDecode)
                    )

                    // 采样率解析链：JS getAudioSampleRate → 实际合成解析
                    val sampleRate = runCatching {
                        engine.getSampleRate(poolId, voice.id) ?: 0
                    }.getOrNull()?.takeIf { it > 0 } ?: resolveSampleRateBySynth(
                        provider = provider,
                        config = templateConfig,
                        tts = src,
                        voiceId = voice.id
                    )

                    val newConfig = templateConfig.copy(
                        audioFormat = templateConfig.audioFormat.copy(sampleRate = sampleRate),
                        source = src.copy(voice = voice.id)
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
                                categoryPath = subGroupName,
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
