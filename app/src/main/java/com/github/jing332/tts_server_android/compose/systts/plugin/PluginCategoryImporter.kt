package com.github.jing332.tts_server_android.compose.systts.plugin

import android.content.Context
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.BasicAudioFormat
import com.github.jing332.database.entities.systts.JReadConfigMigration
import com.github.jing332.database.entities.systts.SystemTtsGroup
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.SpeechRuleInfo
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts.speech.plugin.engine.TtsPluginUiEngineV2
import com.github.jing332.tts_server_android.constant.SpeechTarget
import com.github.jing332.tts_server_android.model.rhino.speech_rule.SpeechRuleEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * 插件「按插件音色分类入库」：
 * 1. 以插件名自动新建分组（不占用用户已有分组）
 * 2. 逐个入库调用方指定的音色；分类 = 试听时手选的分类（categoryOverride）
 *    优先，否则按池名做字面映射；可映射走标准人群名（打标签+子分组），
 *    不可映射则原样入库、不打标签
 *
 * 采样率使用插件声明的请求/裸 PCM 兜底值；MP3/WAV/Opus 等实际输入格式在播放时自动识别。
 */
object PluginCategoryImporter {

    /** 标准人群关键词：与列表页整理标签共用同一套词表（取最长匹配） */
    private val TAG_KEYWORDS = listOf(
        "女童", "少女", "女青年", "女中年", "女老年",
        "男童", "少年", "男青年", "男中年", "男老年",
        "特殊女", "特殊男", "女主", "男主", "旁白"
    )

    /**
     * 插件分类名 → 标准人群名；不可映射返回 null（调用方原样入库且不打标签）。
     *
     * 三步，顺序不能换：
     * 1. 剥常见修饰后缀（通用/发音人/音色）；
     * 2. 长名式先查 [JReadConfigMigration.LONG_TO_SHORT_PREFIX]（十组，与 jread 导入共用同一张表）。
     *    **必须在归一化之前查**：「女性儿童」压成「女儿童」会同时丢掉「女童」的连续子串，
     *    而「女性少年」压成「女少年」更会命中男性少的「少年」——归类直接错（用户 09-17 指出）；
     * 3. 兜底：归一化后按最长关键词命中，覆盖带修饰的长名变体
     *    （「女性儿童声线」→「女童声线」→ 女童）。
     */
    internal fun mapTagCategory(raw: String): String? {
        val s0 = raw.trim().removeSuffix("通用").removeSuffix("发音人").removeSuffix("音色").trim()
        JReadConfigMigration.LONG_TO_SHORT_PREFIX[s0]?.let { return it }
        val s = s0.replace("女性", "女").replace("男性", "男")
            .replace("儿童", "童")      // 女儿童→女童、男儿童→男童
            .replace("女少年", "少女")  // 语序相反："女性少年"→"女少年"→少女（"少年"=男性少）
        return TAG_KEYWORDS.filter { s.contains(it) }.maxByOrNull { it.length }
    }

    /**
     * 待导入的一条音色（10-05 迁移：声音列表在弹窗里选好再交给本对象落库）。
     *
     * @param poolId 所属语言池/分类 id（无分类插件为 ""）
     * @param poolName 池显示名（categoryOverride 为空时用做字面映射/子分组名）
     * @param categoryOverride 试听时手动分配的分类（女童…旁白）；null = 未手选，
     *        按 [poolName] 字面映射（旧「整池盲入」行为）
     */
    data class VoiceItem(
        val poolId: String,
        val poolName: String,
        val voiceId: String,
        val voiceName: String,
        val categoryOverride: String? = null,
    )

    /**
     * @param items 待入库音色（已由弹窗按勾选筛好）
     * @param onProgress 进度回调，可从任意线程安全地更新 Compose 状态
     * @return 成功插入的配置项数量
     */
    suspend fun importVoices(
        context: Context,
        plugin: Plugin,
        items: List<VoiceItem>,
        onProgress: (String) -> Unit = {},
    ): Int {
        val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        try {
            return withContext(dispatcher) {
                importInternal(context, plugin, items, onProgress)
            }
        } finally {
            dispatcher.close()
        }
    }

    private suspend fun importInternal(
        context: Context,
        plugin: Plugin,
        items: List<VoiceItem>,
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

            // 以插件名自动新建分组
            val groupName = plugin.name.ifBlank { "未命名插件" }
            val newGroup = SystemTtsGroup(name = groupName)
            withContext(Dispatchers.IO) {
                dbm.systemTtsV2.insertGroup(newGroup)
            }
            val targetGroupId = newGroup.id

            val baseId = System.currentTimeMillis()
            var idSeq = 0
            val baseOrder = 0
            var orderSeq = 0
            val categoryCountMap = mutableMapOf<String, Int>()
            var processed = 0

            items.forEach { item ->
                val rawName = item.poolName.trim()
                // 分类优先级（10-05）：试听手选的分类 > 池名字面映射；
                // 都拿不到 → 原名当子分组、不打标签（旧盲入行为）
                val category = item.categoryOverride?.takeIf { it.isNotBlank() }
                    ?: mapTagCategory(rawName)
                val subGroupName = category ?: rawName.ifBlank { "未分类" }

                processed++
                onProgress("正在导入 ${processed}：${item.voiceName}")

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
                        ?: JReadConfigMigration.buildTag(category, seq)
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
                    engine.isNeedDecode(item.poolId, item.voiceId)
                }.getOrNull() ?: true

                val src = PluginTtsSource(pluginId = plugin.pluginId, locale = item.poolId)
                val templateConfig = TtsConfigurationDTO(
                    source = src,
                    speechRule = newRuleData,
                    audioFormat = BasicAudioFormat(isNeedDecode = needDecode)
                )

                // 插件声明值对应用户在插件界面选定的请求/裸 PCM 兜底格式。
                // 可解码音频会在播放时从音频头识别实际输入格式，无需逐声音合成测率。
                val sampleRate = runCatching {
                    engine.getSampleRate(item.poolId, item.voiceId)
                }.getOrNull()?.takeIf { it > 0 } ?: templateConfig.audioFormat.sampleRate

                val newConfig = templateConfig.copy(
                    audioFormat = templateConfig.audioFormat.copy(sampleRate = sampleRate),
                    source = src.copy(voice = item.voiceId)
                )

                // 批量导入默认不启用：避免未知发音人立刻影响当前朗读
                withContext(Dispatchers.IO) {
                    dbm.systemTtsV2.insert(
                        SystemTtsV2(
                            id = baseId + idSeq++,
                            displayName = item.voiceName,
                            groupId = targetGroupId,
                            isEnabled = false,
                            order = baseOrder + orderSeq++,
                            categoryPath = subGroupName,
                            config = newConfig
                        )
                    )
                }
            }
            return idSeq
        } finally {
            runCatching { engine.destroy() }
        }
    }
}
