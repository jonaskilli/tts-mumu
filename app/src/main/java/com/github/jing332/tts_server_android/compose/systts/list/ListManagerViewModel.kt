package com.github.jing332.tts_server_android.compose.systts.list

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jing332.database.dbm
import com.github.jing332.tts_server_android.conf.AppConfig
import com.github.jing332.database.entities.AbstractListGroup.Companion.DEFAULT_GROUP_ID
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.GroupWithSystemTts
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.database.entities.systts.SystemTtsGroup
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.conf.SystemTtsConfig
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.burnoutcrew.reorderable.ItemPosition
import java.util.Collections

class ListManagerViewModel : ViewModel() {
    companion object {
        const val TAG = "ListManagerViewModel"

        // 刷新链诊断:logger 名须加入 SysttsFilter 的 SYSTTS_INTERNAL_LOGGER_NAMES 白名单才会进日志页
        private val listTraceLogger = io.github.oshai.kotlinlogging.KotlinLogging.logger(TAG)

        // 进程内缓存最近一次全量列表（关键词为空时）。列表数据一直在数据库里，
        // 但每次进页都要等「打开库→整表查询→JSON 反序列化上千条」后才首次出列表，
        // 缓存让同一进程内再次进页/重建 Activity 时立即显示，数据库查完无缝替换
        private var cachedFullList: List<GroupWithSystemTts>? = null
    }

    private val _keyword = MutableStateFlow("")
    val keyword: StateFlow<String> get() = _keyword

    private val _searchType = MutableStateFlow(SearchType.NAME)
    val searchType: StateFlow<SearchType> get() = _searchType

    private val _list = MutableStateFlow<List<GroupWithSystemTts>>(emptyList())
    val list: StateFlow<List<GroupWithSystemTts>> get() = _list

    // 首次数据库查询是否完成：完成前列表区显示加载中而不是误导性的「暂无分组」
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> get() = _isInitialized

    // 缓存插件名称：响应式订阅插件表，插件新增/改名/pluginId变更/切换引用后自动刷新
    // 插件名缓存（pluginId → 展示名），供列表与批量修复来源选择使用
    private val _pluginNameCache = MutableStateFlow<Map<String, String>>(emptyMap())
    val pluginNameCache: StateFlow<Map<String, String>> get() = _pluginNameCache
    // 已启用插件的 pluginId 集合，用于判断配置项是否失效
    private val _enabledPluginIds = MutableStateFlow<Set<String>>(emptySet())
    val enabledPluginIds: StateFlow<Set<String>> get() = _enabledPluginIds
    // 失效配置项数量
    private val _invalidCount = MutableStateFlow(0)
    val invalidCount: StateFlow<Int> get() = _invalidCount

    // 失效配置项按源插件分组：pluginId → 数量，用于批量修复时逐源选择目标插件
    private val _invalidSourceCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val invalidSourceCounts: StateFlow<Map<String, Int>> get() = _invalidSourceCounts

    // 失效配置项按源插件分组：pluginId → 失效配置项名称列表，用于详情弹窗逐源展开
    private val _invalidSourceItems = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val invalidSourceItems: StateFlow<Map<String, List<String>>> get() = _invalidSourceItems

    init {
        migrateExpandedGroupIds()
        // 有缓存先立即显示，不等数据库冷启动
        cachedFullList?.let { _list.value = it }
        viewModelScope.launch(Dispatchers.IO) {
            // 顺序整理挪到独立协程：它要整表读一遍并可能逐行写，此前串在列表链路前面，
            // 首次显示必须等它跑完，是开页白屏几秒的主因之一。
            // 包一层事务：原实现逐行 update 各自开事务，大库时数千次写入=
            // 数千次 Room 失效波，每次都触发全列表重查+JSON 反序列化
            launch {
                runCatching {
                    dbm.runInTransaction { dbm.systemTtsV2.updateAllOrder() }
                }
            }

            // 插件信息 Flow：插件表任何变化都会重新生成 pluginId→name 映射 + 已启用id集合
            val pluginInfoFlow = dbm.pluginDao.flowAllWithoutCode()
                .map { plugins ->
                    Pair(
                        plugins.associate { it.pluginId to it.name },
                        plugins.filter { it.isEnabled }.map { p -> p.pluginId }.toSet()
                    )
                }

            dbm.systemTtsV2.flowAllGroupWithTts().conflate()
                .combine(_keyword) { list, key -> Pair(list, key) }
                .combine(_searchType) { pair, type -> Triple(pair.first, pair.second, type) }
                .combine(pluginInfoFlow) { triple, (nameMap, enabledIds) ->
                    Quad(triple.first, triple.second, triple.third, nameMap, enabledIds)
                }
                .collect { (list, key, searchType, nameMap, enabledIds) ->
                    // 容错:collect 内任何一条脏数据抛异常都会杀死整个收集协程,
                    // 此后所有保存的列表刷新全部静默失效(表现为「改了参数要重启才生效」),
                    // 必须捕获保证协程存活,等待下一次 Flow 重发
                    runCatching {
                        _pluginNameCache.value = nameMap
                        _enabledPluginIds.value = enabledIds
                        // 计算失效项数量与按源插件的分组统计
                        val srcCounts = mutableMapOf<String, Int>()
                        val srcItems = mutableMapOf<String, MutableList<String>>()
                        var invalid = 0
                        list.forEach { groupWithTts ->
                            groupWithTts.list.forEach { item ->
                                val src = (item.config as? TtsConfigurationDTO)?.source
                                if (src is PluginTtsSource && src.pluginId !in enabledIds) {
                                    invalid++
                                    srcCounts[src.pluginId] = (srcCounts[src.pluginId] ?: 0) + 1
                                    srcItems.getOrPut(src.pluginId) { mutableListOf() }
                                        .add(item.displayName.ifBlank { "(#${item.id})" })
                                }
                            }
                        }
                        _invalidCount.value = invalid
                        _invalidSourceCounts.value = srcCounts
                        _invalidSourceItems.value = srcItems
                        // 默认分组（id=1）只在其中有配置项时显示：它永远留在库里做兜底，
                        // 但空着的时候列出来只会"突然冒出来"让人困惑；其余分组空壳照常保留
                        val visible = list.filter { it.group.id != DEFAULT_GROUP_ID || it.list.isNotEmpty() }
                        // 关键词为空时直接返回全量列表，否则按类型过滤
                        val result = if (key.isBlank()) {
                            visible
                        } else {
                            filterList(visible, key, searchType, enabledIds)
                        }
                        listTraceLogger.info { "update list: ${result.size}" }
                        _list.value = result
                        _isInitialized.value = true
                        if (key.isBlank()) cachedFullList = result
                    }.onFailure { e ->
                        listTraceLogger.error(e) { "列表重算失败(跳过本次,等待下次数据重发): ${e.message}" }
                    }
                }
        }
    }

    /** 四元组，用于 combine 后承载搜索输入 */
    private data class Quad(
        val list: List<GroupWithSystemTts>,
        val key: String,
        val searchType: SearchType,
        val pluginNames: Map<String, String>,
        val enabledPluginIds: Set<String>,
    )

    private fun filterList(
        list: List<GroupWithSystemTts>,
        key: String,
        searchType: SearchType,
        enabledIds: Set<String> = emptySet(),
    ): List<GroupWithSystemTts> {
        return when (searchType) {
            SearchType.NAME -> {
                list.mapNotNull { groupWithTts ->
                    val filteredItems = groupWithTts.list.filter {
                        it.displayName.contains(key, ignoreCase = true)
                    }
                    if (filteredItems.isNotEmpty()) {
                        groupWithTts.copy(
                            list = filteredItems,
                            group = groupWithTts.group.copy(isExpanded = true)
                        )
                    } else null
                }
            }
            SearchType.TAG -> {
                list.mapNotNull { groupWithTts ->
                    val filteredItems = groupWithTts.list.filter { item ->
                        val ttsConfig = item.config as? TtsConfigurationDTO
                        if (ttsConfig != null) {
                            val speechRule = ttsConfig.speechRule
                            speechRule.tagName.contains(key, ignoreCase = true) ||
                            speechRule.tag.contains(key, ignoreCase = true) ||
                            speechRule.tagData.values.any { it.contains(key, ignoreCase = true) }
                        } else false
                    }
                    if (filteredItems.isNotEmpty()) {
                        groupWithTts.copy(
                            list = filteredItems,
                            group = groupWithTts.group.copy(isExpanded = true)
                        )
                    } else null
                }
            }
            SearchType.PLUGIN -> {
                list.mapNotNull { groupWithTts ->
                    val filteredItems = groupWithTts.list.filter { item ->
                        val ttsConfig = item.config as? TtsConfigurationDTO
                        if (ttsConfig != null) {
                            when (val source = ttsConfig.source) {
                                is PluginTtsSource -> {
                                    val pluginName = _pluginNameCache.value[source.pluginId] ?: source.pluginId
                                    source.pluginId.contains(key, ignoreCase = true) ||
                                    pluginName.contains(key, ignoreCase = true)
                                }
                                is LocalTtsSource ->
                                    "本地".contains(key, ignoreCase = true) ||
                                    "local".contains(key, ignoreCase = true)
                                else -> false
                            }
                        } else false
                    }
                    if (filteredItems.isNotEmpty()) {
                        groupWithTts.copy(
                            list = filteredItems,
                            group = groupWithTts.group.copy(isExpanded = true)
                        )
                    } else null
                }
            }
            SearchType.GROUP -> {
                list.filter {
                    it.group.name.contains(key, ignoreCase = true)
                }.map {
                    it.copy(group = it.group.copy(isExpanded = true))
                }
            }
        }
    }

    fun setSearchKeyword(key: String) {
        _keyword.value = key
    }

    fun setSearchType(type: SearchType) {
        _searchType.value = type
    }

    /**
     * 批量修复失效配置项
     * @param newPluginId 目标插件id
     * @param sourcePluginId 源插件id；null=修复全部失效项（单来源场景），指定=只修复引用该插件的项
     */
    fun batchFixInvalidItems(newPluginId: String, sourcePluginId: String? = null) =
        viewModelScope.launch(Dispatchers.IO) {
            val enabledIds = _enabledPluginIds.value
            val allItems = dbm.systemTtsV2.getAllGroupWithTts().flatMap { it.list }
            // 单事务批量更新：逐条 update 每条一个事务且各触发一次列表Flow重发射，N项=N次列表重算导致卡顿
            val toUpdate = allItems.mapNotNull { item ->
                val config = item.config as? TtsConfigurationDTO ?: return@mapNotNull null
                val src = config.source
                val isInvalid = src is PluginTtsSource && src.pluginId !in enabledIds
                val matchSource = sourcePluginId == null || (src as? PluginTtsSource)?.pluginId == sourcePluginId
                if (isInvalid && matchSource) {
                    item.copy(config = config.copy(source = src.copy(pluginId = newPluginId)))
                } else null
            }
            if (toUpdate.isNotEmpty()) {
                dbm.runInTransaction {
                    dbm.systemTtsV2.update(*toUpdate.toTypedArray())
                }
            }
        }

    /**
     * 批量删除失效配置项。
     * @param sourcePluginId 源插件id；null=删除全部失效项，指定=只删引用该插件的项
     */
    fun batchDeleteInvalidItems(sourcePluginId: String? = null) =
        viewModelScope.launch(Dispatchers.IO) {
            val enabledIds = _enabledPluginIds.value
            val allItems = dbm.systemTtsV2.getAllGroupWithTts().flatMap { it.list }
            val toDelete = allItems.filter { item ->
                val src = (item.config as? TtsConfigurationDTO)?.source as? PluginTtsSource
                val isInvalid = src != null && src.pluginId !in enabledIds
                val matchSource = sourcePluginId == null || src?.pluginId == sourcePluginId
                isInvalid && matchSource
            }
            if (toDelete.isNotEmpty()) {
                dbm.runInTransaction {
                    dbm.systemTtsV2.delete(*toDelete.toTypedArray())
                    // 删完后变空的分组一并删除，不留空壳（默认分组保留）
                    dbm.systemTtsV2.allGroup.forEach { g ->
                        if (g.id != DEFAULT_GROUP_ID &&
                            dbm.systemTtsV2.getByGroup(g.id).isEmpty()
                        ) dbm.systemTtsV2.deleteGroup(g)
                    }
                }
            }
        }

    fun updateTtsEnabled(
        item: SystemTtsV2,
        enabled: Boolean,
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (!SystemTtsConfig.isVoiceMultipleEnabled.value && enabled) {
            val itemConfig = (item.config as? TtsConfigurationDTO)
            if (itemConfig != null)
                dbm.systemTtsV2.allEnabled.forEach { systts ->
                    if (systts.config is TtsConfigurationDTO) {
                        val config = systts.config as TtsConfigurationDTO
                        if (config.speechRule.target == itemConfig.speechRule.target) {
                            if (config.speechRule.tagRuleId == itemConfig.speechRule.tagRuleId
                                && config.speechRule.tag == itemConfig.speechRule.tag
                                && config.speechRule.tagName == itemConfig.speechRule.tagName
                                && config.speechRule.isStandby == itemConfig.speechRule.isStandby
                            )
                                dbm.systemTtsV2.update(systts.copy(isEnabled = false))
                        }
                    }
                }
        }

        dbm.systemTtsV2.update(item.copy(isEnabled = enabled))
    }

    /**
     * 提取标签字符串（空返回null）
     */
    private fun extractTag(systts: SystemTtsV2): String? {
        val config = systts.config as? TtsConfigurationDTO ?: return null
        val tag = config.speechRule.tag
        return if (tag.isBlank()) null else tag
    }

    fun updateGroupEnable(
        item: GroupWithSystemTts,
        enabled: Boolean,
    ) = viewModelScope.launch(Dispatchers.IO) {
        val allUpdates = mutableListOf<SystemTtsV2>()
        val affectedGroupIds = mutableSetOf<Long>()

        if (!SystemTtsConfig.isGroupMultipleEnabled.value && enabled) {
            // 非多选模式：禁用所有其他已启用项
            list.value.forEach { gwt ->
                gwt.list.forEach { systts ->
                    if (systts.isEnabled) {
                        allUpdates.add(systts.copy(isEnabled = false))
                        affectedGroupIds.add(gwt.group.id)
                    }
                }
            }
        }

        // 启用/禁用当前分组的所有项
        val groupUpdates = item.list.filter { it.isEnabled != enabled }
            .map { it.copy(isEnabled = enabled) }
        allUpdates.addAll(groupUpdates)
        affectedGroupIds.add(item.group.id)

        // 3.⑨: 分组多选时同tag去重——新启用分组中item的tag若与其他已启用分组中item的tag相同，
        // 则将其他分组中相同tag的item置为不启用（保留新启用的，仅不启用，不删除）
        if (enabled && SystemTtsConfig.isGroupMultipleEnabled.value) {
            val newEnabledTags = groupUpdates.mapNotNull { extractTag(it) }.toSet()
            if (newEnabledTags.isNotEmpty()) {
                list.value.forEach { gwt ->
                    if (gwt.group.id != item.group.id) {
                        gwt.list.forEach { systts ->
                            if (systts.isEnabled) {
                                val tag = extractTag(systts)
                                if (tag != null && tag in newEnabledTags) {
                                    allUpdates.add(systts.copy(isEnabled = false))
                                    affectedGroupIds.add(gwt.group.id)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (allUpdates.isNotEmpty()) {
            dbm.systemTtsV2.update(*allUpdates.toTypedArray())
            // 立即更新内存列表，使UI即时响应
            updateMultipleGroupsInMemory(affectedGroupIds, allUpdates)
        }
        if (enabled) SystemTtsService.notifyUpdateConfig()
    }

    /**
     * 子分组批量启用/禁用（3.⑥）：批量更新数据库并即时刷新内存列表。
     */
    fun updateSubGroupEnable(
        groupId: Long,
        subItems: List<SystemTtsV2>,
        enabled: Boolean,
    ) = viewModelScope.launch(Dispatchers.IO) {
        val subIds = subItems.map { it.id }.toSet()
        val updates = subItems.filter { it.isEnabled != enabled }
            .map { it.copy(isEnabled = enabled) }
        if (updates.isNotEmpty()) {
            dbm.systemTtsV2.update(*updates.toTypedArray())
            // 立即更新内存列表
            val currentList = findListInGroup(groupId)
            val newItems = currentList.map { systts ->
                if (systts.id in subIds && systts.isEnabled != enabled)
                    systts.copy(isEnabled = enabled)
                else systts
            }
            updateGroupListInMemory(groupId, newItems)
        }
        if (enabled) SystemTtsService.notifyUpdateConfig()
    }

    /**
     * 批量音频参数：把作用域内配置项的单条 audioParams 统一改为给定值。
     * [speed]/[volume]/[pitch] 为 null 表示该维度保持原值不动；重置传 1f。
     * 覆盖 LOCAL 与 PLUGIN 两类配置（LOCAL 滑块本来就写 audioParams）。
     * 完成后通知服务刷新；内存列表由 Room 全量重发自然更新。
     */
    fun updateAudioParamsBatch(
        items: List<SystemTtsV2>,
        speed: Float?,
        volume: Float?,
        pitch: Float?,
        onDone: (Int) -> Unit = {},
    ) = viewModelScope.launch(Dispatchers.IO) {
        val updates = items.mapNotNull { item ->
            val c = item.config as? TtsConfigurationDTO ?: return@mapNotNull null
            val p = c.audioParams
            val newParams = p.copy(
                speed = speed ?: p.speed,
                volume = volume ?: p.volume,
                pitch = pitch ?: p.pitch,
            )
            if (newParams != p) item.copy(config = c.copy(audioParams = newParams)) else null
        }
        if (updates.isNotEmpty()) {
            dbm.systemTtsV2.update(*updates.toTypedArray())
            SystemTtsService.notifyUpdateConfig()
        }
        withContext(Dispatchers.Main) { onDone(updates.size) }
    }

    /**
     * 批量启用/停用：作用域内配置项统一 isEnabled。
     */
    fun updateEnabledBatch(
        items: List<SystemTtsV2>,
        enabled: Boolean,
        onDone: (Int) -> Unit = {},
    ) = viewModelScope.launch(Dispatchers.IO) {
        val updates = items.filter { it.isEnabled != enabled }
            .map { it.copy(isEnabled = enabled) }
        if (updates.isNotEmpty()) {
            dbm.systemTtsV2.update(*updates.toTypedArray())
            SystemTtsService.notifyUpdateConfig()
        }
        withContext(Dispatchers.Main) { onDone(updates.size) }
    }

    /**
     * 批量采样率：只影响插件型配置。
     * [sampleRate] 为 null 表示保持原值；jread 占位 16000 可批量改为真实值或自动识别标志。
     */
    fun updateSourceFieldsBatch(
        items: List<SystemTtsV2>,
        sampleRate: Int?,
        onDone: (Int) -> Unit = {},
    ) = viewModelScope.launch(Dispatchers.IO) {
        val updates = items.mapNotNull { item ->
            val c = item.config as? TtsConfigurationDTO ?: return@mapNotNull null
            if (c.source !is PluginTtsSource) return@mapNotNull null
            val newFormat = if (sampleRate != null)
                c.audioFormat.copy(sampleRate = sampleRate) else c.audioFormat
            val newConfig = c.copy(audioFormat = newFormat)
            if (newConfig != c) item.copy(config = newConfig) else null
        }
        if (updates.isNotEmpty()) {
            dbm.systemTtsV2.update(*updates.toTypedArray())
            SystemTtsService.notifyUpdateConfig()
        }
        withContext(Dispatchers.Main) { onDone(updates.size) }
    }

    /**
     * 批量切换来源插件：把作用域内插件型配置项的 pluginId 改指向另一插件（发音人/locale 等保持原值）。
     * 与 batchFixInvalidItems 不同：不要求原插件已失效，凡 pluginId 不同的都改。
     */
    fun updateSourcePluginBatch(
        items: List<SystemTtsV2>,
        newPluginId: String,
        onDone: (Int) -> Unit = {},
    ) = viewModelScope.launch(Dispatchers.IO) {
        val updates = items.mapNotNull { item ->
            val c = item.config as? TtsConfigurationDTO ?: return@mapNotNull null
            val src = c.source as? PluginTtsSource ?: return@mapNotNull null
            if (src.pluginId == newPluginId) return@mapNotNull null
            item.copy(config = c.copy(source = src.copy(pluginId = newPluginId)))
        }
        if (updates.isNotEmpty()) {
            dbm.systemTtsV2.update(*updates.toTypedArray())
            SystemTtsService.notifyUpdateConfig()
        }
        withContext(Dispatchers.Main) { onDone(updates.size) }
    }

    /**
     * 旧版展开状态写在分组表 isExpanded 列，切换会触发 Room 全量重发，
     * 连带分池判定与分组树全量重建（大分组数千项时展开/折叠明显卡顿）。
     * 现展开态走 AppConfig.expandedGroupIds 轻量集合；此处只做一次性迁移（只增不减）。
     */
    fun migrateExpandedGroupIds() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val saved = AppConfig.expandedGroupIds.value
                val fromDb = dbm.systemTtsV2.allGroup
                    .filter { it.isExpanded }
                    .map { it.id.toString() }
                    .toSet()
                if (fromDb.isNotEmpty() && !saved.containsAll(fromDb)) {
                    AppConfig.expandedGroupIds.value = saved + fromDb
                }
            }
        }
    }

    fun reorder(from: ItemPosition, to: ItemPosition) {
        if (_keyword.value.isNotEmpty()) return

        val fromKey = from.key as? String ?: return
        val toKey = to.key as? String ?: return

        // 子分组拖动：交换两个子分组的整组内容顺序
        if (fromKey.startsWith("sub_") || toKey.startsWith("sub_")) {
            if (!fromKey.startsWith("sub_") || !toKey.startsWith("sub_")) return

            val fromGroupId = fromKey.removePrefix("sub_").substringBefore("_").toLong()
            val toGroupId = toKey.removePrefix("sub_").substringBefore("_").toLong()
            if (fromGroupId != toGroupId) return

            val fromPath = fromKey.removePrefix("sub_${fromGroupId}_")
            val toPath = toKey.removePrefix("sub_${toGroupId}_")
            if (fromPath == toPath) return

            val allItems = findListInGroup(fromGroupId).toMutableList()
            if (allItems.isEmpty()) return

            data class Block(val path: String, val items: MutableList<SystemTtsV2>)
            val blocks = mutableListOf<Block>()
            var currentBlock: Block? = null
            for (item in allItems) {
                val path = item.categoryPath
                if (currentBlock == null || currentBlock.path != path) {
                    currentBlock = Block(path, mutableListOf())
                    blocks.add(currentBlock)
                }
                currentBlock.items.add(item)
            }

            val fromBlockIndex = blocks.indexOfFirst { it.path == fromPath }
            val toBlockIndex = blocks.indexOfFirst { it.path == toPath }
            if (fromBlockIndex == -1 || toBlockIndex == -1) return

            val movedBlock = blocks.removeAt(fromBlockIndex)
            // 修复: 向下拖动时之前用 toBlockIndex-1 导致移动无效或方向错误;
            // 移除 from 后, 直接用原始 toBlockIndex 即可正确落到目标位置(向上同理)
            val insertIndex = toBlockIndex.coerceAtMost(blocks.size)
            blocks.add(insertIndex, movedBlock)

            var order = 0
            val toUpdate = mutableListOf<SystemTtsV2>()
            val newAllItems = mutableListOf<SystemTtsV2>()
            for (block in blocks) {
                for (item in block.items) {
                    if (item.order != order) {
                        toUpdate.add(item.copy(order = order))
                        newAllItems.add(item.copy(order = order))
                    } else {
                        newAllItems.add(item)
                    }
                    order++
                }
            }
            if (toUpdate.isNotEmpty()) {
                updateGroupListInMemory(fromGroupId, newAllItems)
                viewModelScope.launch(Dispatchers.IO) {
                    dbm.systemTtsV2.update(*toUpdate.toTypedArray())
                }
            }
            return
        }

        // 子分组内配置项拖动：确保在同一子分组内交换
        if (fromKey.startsWith("item_") || toKey.startsWith("item_")) {
            if (!fromKey.startsWith("item_") || !toKey.startsWith("item_")) return

            // 解析 key 格式: item_${groupId}_${categoryPath}_${itemId}
            // categoryPath 可能含下划线，所以从两端解析：第一个下划线前是groupId，最后一个下划线后是itemId
            val fromBody = fromKey.removePrefix("item_")
            val toBody = toKey.removePrefix("item_")
            
            val fromFirstUnder = fromBody.indexOf('_')
            val fromLastUnder = fromBody.lastIndexOf('_')
            if (fromFirstUnder == -1 || fromLastUnder == -1 || fromFirstUnder == fromLastUnder) return
            
            val fromGroupId = fromBody.substring(0, fromFirstUnder).toLongOrNull() ?: return
            val fromCategoryPath = fromBody.substring(fromFirstUnder + 1, fromLastUnder)
            val fromItemId = fromBody.substring(fromLastUnder + 1).toLongOrNull() ?: return

            val toFirstUnder = toBody.indexOf('_')
            val toLastUnder = toBody.lastIndexOf('_')
            if (toFirstUnder == -1 || toLastUnder == -1 || toFirstUnder == toLastUnder) return
            
            val toGroupId = toBody.substring(0, toFirstUnder).toLongOrNull() ?: return
            val toCategoryPath = toBody.substring(toFirstUnder + 1, toLastUnder)
            val toItemId = toBody.substring(toLastUnder + 1).toLongOrNull() ?: return

            // 确保在同一分组和同一子分组内
            if (fromGroupId != toGroupId || fromCategoryPath != toCategoryPath) return

            val allItems = findListInGroup(fromGroupId).toMutableList()
            val fromIndex = allItems.indexOfFirst { it.id == fromItemId && it.categoryPath == fromCategoryPath }
            val toIndex = allItems.indexOfFirst { it.id == toItemId && it.categoryPath == toCategoryPath }
            if (fromIndex == -1 || toIndex == -1) return

            try {
                Collections.swap(allItems, fromIndex, toIndex)
            } catch (_: IndexOutOfBoundsException) {
                return
            }

            val itemUpdates = allItems.mapIndexedNotNull { index, systts ->
                if (systts.order != index) systts.copy(order = index) else null
            }
            if (itemUpdates.isNotEmpty()) {
                val newItems = allItems.mapIndexed { index, systts ->
                    if (systts.order != index) systts.copy(order = index) else systts
                }
                updateGroupListInMemory(fromGroupId, newItems)
                viewModelScope.launch(Dispatchers.IO) {
                    dbm.systemTtsV2.update(*itemUpdates.toTypedArray())
                }
            }
            return
        }

        if (fromKey.startsWith("g_") && toKey.startsWith("g_")) {
            val mList = list.value.map { it.group }.toMutableList()

            val fromId = fromKey.substring(2).toLong()
            val fromIndex = mList.indexOfFirst { it.id == fromId }

            val toId = toKey.substring(2).toLong()
            val toIndex = mList.indexOfFirst { it.id == toId }

            try {
                Collections.swap(mList, fromIndex, toIndex)
            } catch (_: IndexOutOfBoundsException) {
                return
            }
            val groupUpdates = mList.mapIndexedNotNull { index, group ->
                if (group.order != index) group.copy(order = index) else null
            }
            if (groupUpdates.isNotEmpty()) {
                // 立即更新内存中分组顺序
                val swappedList = mList.toMutableList()
                _list.value = _list.value.map { gwt ->
                    val newOrder = swappedList.indexOfFirst { it.id == gwt.group.id }
                    if (newOrder != -1 && gwt.group.order != newOrder) {
                        gwt.copy(group = gwt.group.copy(order = newOrder))
                    } else gwt
                }
                viewModelScope.launch(Dispatchers.IO) {
                    groupUpdates.forEach { dbm.systemTtsV2.updateGroup(it) }
                }
            }
        } else if (!fromKey.startsWith("g_") && !toKey.startsWith("g_")) {
            val (fromGId, fromId) = fromKey.split("_").map { it.toLong() }
            val (toGId, toId) = toKey.split("_").map { it.toLong() }
            if (fromGId != toGId) return

            val listInGroup = findListInGroup(fromGId).toMutableList()
            val fromIndex = listInGroup.indexOfFirst { it.id == fromId }
            val toIndex = listInGroup.indexOfFirst { it.id == toId }

            try {
                Collections.swap(listInGroup, fromIndex, toIndex)
            } catch (_: IndexOutOfBoundsException) {
                return
            }

            val itemUpdates = listInGroup.mapIndexedNotNull { index, systts ->
                if (systts.order != index) systts.copy(order = index) else null
            }
            if (itemUpdates.isNotEmpty()) {
                val newItems = listInGroup.mapIndexed { index, systts ->
                    if (systts.order != index) systts.copy(order = index) else systts
                }
                updateGroupListInMemory(fromGId, newItems)
                viewModelScope.launch(Dispatchers.IO) {
                    dbm.systemTtsV2.update(*itemUpdates.toTypedArray())
                }
            }
        }
    }

    private fun findListInGroup(groupId: Long): List<SystemTtsV2> {
        return list.value.find { it.group.id == groupId }?.list?.sortedBy { it.order }
            ?: emptyList()
    }

    /**
     * 立即更新内存中的分组列表，使UI即时响应而不必等待数据库flow。
     */
    private fun updateGroupListInMemory(groupId: Long, newItems: List<SystemTtsV2>) {
        _list.value = _list.value.map { gwt ->
            if (gwt.group.id == groupId) gwt.copy(list = newItems) else gwt
        }
    }

    /**
     * 批量更新多个分组的内存列表（用于跨分组去重等场景）。
     */
    private fun updateMultipleGroupsInMemory(groupIds: Set<Long>, updates: List<SystemTtsV2>) {
        val updatesById = updates.associateBy { it.id }
        _list.value = _list.value.map { gwt ->
            if (gwt.group.id in groupIds) {
                gwt.copy(list = gwt.list.map { item ->
                    updatesById[item.id] ?: item
                })
            } else gwt
        }
    }

    suspend fun checkListData(context: Context) {
        // 仅在分组表完全为空时（首次安装/清数据）创建默认分组；用户主动删除后不再自动重建
        if (dbm.systemTtsV2.groupCount == 0) {
            dbm.systemTtsV2.insertGroup(
                com.github.jing332.database.entities.systts.SystemTtsGroup(
                    DEFAULT_GROUP_ID,
                    context.getString(R.string.default_group),
                    0
                )
            )
        }

        // 一次性修复：旧版曾把规则外标签（jread 性格/群杂等）的显示名经 JS 兜底误写成「旁白」，
        // 升级后首启强制重算一遍（未知标签回写原始 tag，见 TagNameUtils 护栏），不依赖导入触发
        if (!AppConfig.tagNameUnknownRepairDone.value) {
            migrateTagNamesIfNeed(context, force = true)
            AppConfig.tagNameUnknownRepairDone.value = true
        }

        // tagName 一次性迁移：重算所有 tagName 并清理废弃的 personality 字段
        migrateTagNamesIfNeed(context)

        // 单条音频参数折叠：把旧版 source.* 调节迁入唯一生效的 audioParams 层
        collapseAudioParamsIfNeed()
    }
}
