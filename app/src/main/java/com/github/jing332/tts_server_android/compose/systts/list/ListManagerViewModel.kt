package com.github.jing332.tts_server_android.compose.systts.list

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jing332.database.dbm
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
    }

    private val _keyword = MutableStateFlow("")
    val keyword: StateFlow<String> get() = _keyword

    private val _searchType = MutableStateFlow(SearchType.NAME)
    val searchType: StateFlow<SearchType> get() = _searchType

    private val _list = MutableStateFlow<List<GroupWithSystemTts>>(emptyList())
    val list: StateFlow<List<GroupWithSystemTts>> get() = _list

    // 缓存插件名称：响应式订阅插件表，插件新增/改名/pluginId变更/切换引用后自动刷新
    private val pluginNameCache = MutableStateFlow<Map<String, String>>(emptyMap())
    // 已启用插件的 pluginId 集合，用于判断配置项是否失效
    private val _enabledPluginIds = MutableStateFlow<Set<String>>(emptySet())
    val enabledPluginIds: StateFlow<Set<String>> get() = _enabledPluginIds
    // 失效配置项数量
    private val _invalidCount = MutableStateFlow(0)
    val invalidCount: StateFlow<Int> get() = _invalidCount

    init {
        viewModelScope.launch(Dispatchers.IO) {
            dbm.systemTtsV2.updateAllOrder()

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
                    pluginNameCache.value = nameMap
                    _enabledPluginIds.value = enabledIds
                    // 计算失效项数量
                    _invalidCount.value = countInvalidItems(list, enabledIds)
                    // 失效项筛选不依赖关键词，即使关键词为空也要过滤
                    val result = if (key.isBlank() && searchType != SearchType.INVALID) {
                        list
                    } else {
                        filterList(list, key, searchType, enabledIds)
                    }
                    Log.d(TAG, "update list: ${result.size}")
                    _list.value = result
                }
        }
    }

    /** 统计失效配置项数量 */
    private fun countInvalidItems(
        list: List<GroupWithSystemTts>,
        enabledIds: Set<String>,
    ): Int {
        return list.sumOf { groupWithTts ->
            groupWithTts.list.count { item ->
                val config = item.config as? TtsConfigurationDTO
                val src = config?.source
                src is PluginTtsSource && src.pluginId !in enabledIds
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
                                    val pluginName = pluginNameCache.value[source.pluginId] ?: source.pluginId
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
            SearchType.INVALID -> {
                // 失效项：插件已删除或被禁用的配置项，不依赖关键词
                list.mapNotNull { groupWithTts ->
                    val filteredItems = groupWithTts.list.filter { item ->
                        val config = item.config as? TtsConfigurationDTO
                        val src = config?.source
                        src is PluginTtsSource && src.pluginId !in enabledIds
                    }
                    if (filteredItems.isNotEmpty()) {
                        groupWithTts.copy(
                            list = filteredItems,
                            group = groupWithTts.group.copy(isExpanded = true)
                        )
                    } else null
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
     * 批量修复失效配置项：将所有引用了已删除/已禁用插件的配置项的 pluginId 替换为新插件
     */
    fun batchFixInvalidItems(newPluginId: String) = viewModelScope.launch(Dispatchers.IO) {
        val enabledIds = _enabledPluginIds.value
        val allItems = dbm.systemTtsV2.getAllGroupWithTts().flatMap { it.list }
        val invalidItems = allItems.filter { item ->
            val config = item.config as? TtsConfigurationDTO
            val src = config?.source
            src is PluginTtsSource && src.pluginId !in enabledIds
        }
        invalidItems.forEach { item ->
            val config = item.config as TtsConfigurationDTO
            val source = config.source as PluginTtsSource
            val newSource = source.copy(pluginId = newPluginId)
            val newConfig = config.copy(source = newSource)
            dbm.systemTtsV2.update(item.copy(config = newConfig))
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
     * 切换一级分组展开/折叠状态：立即更新内存列表使UI即时响应，后台异步写入DB持久化。
     */
    fun toggleGroupExpanded(group: SystemTtsGroup) {
        _list.value = _list.value.map { gwt ->
            if (gwt.group.id == group.id) {
                gwt.copy(group = gwt.group.copy(isExpanded = !gwt.group.isExpanded))
            } else gwt
        }
        viewModelScope.launch(Dispatchers.IO) {
            dbm.systemTtsV2.updateGroup(group.copy(isExpanded = !group.isExpanded))
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

        // tagName 一次性迁移：重算所有 tagName 并清理废弃的 personality 字段
        migrateTagNamesIfNeed(context)
    }
}
