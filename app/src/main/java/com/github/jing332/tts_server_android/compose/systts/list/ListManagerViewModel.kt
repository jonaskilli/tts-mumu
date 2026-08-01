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

    // 跨分组拖拽：记录待执行的转移意图，真正执行在 onDragEnd
    data class CrossMoveIntent(val fromKey: String, val toKey: String)
    private val _pendingCrossMove = MutableStateFlow<CrossMoveIntent?>(null)
    val pendingCrossMove: StateFlow<CrossMoveIntent?> get() = _pendingCrossMove

    // 缓存插件名称
    private val pluginNameCache = MutableStateFlow<Map<String, String>>(emptyMap())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // 加载插件名称缓存
            val plugins = dbm.pluginDao.getAllWithoutCode()
            pluginNameCache.value = plugins.associate { it.pluginId to it.name }
            
            dbm.systemTtsV2.updateAllOrder()
            
            dbm.systemTtsV2.flowAllGroupWithTts().conflate()
                .combine(_keyword) { list, key -> Pair(list, key) }
                .combine(_searchType) { pair, type -> Triple(pair.first, pair.second, type) }
                .collect { (list, key, searchType) ->
                    val result = if (key.isBlank()) {
                        list
                    } else {
                        filterList(list, key, searchType)
                    }
                    Log.d(TAG, "update list: ${result.size}")
                    _list.value = result
                }
        }
    }

    private fun filterList(
        list: List<GroupWithSystemTts>,
        key: String,
        searchType: SearchType
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
        }
    }

    fun setSearchKeyword(key: String) {
        _keyword.value = key
    }

    fun setSearchType(type: SearchType) {
        _searchType.value = type
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

    fun reorder(from: ItemPosition, to: ItemPosition) {
        if (_keyword.value.isNotEmpty()) return

        val fromKey = from.key as? String ?: return
        val toKey = to.key as? String ?: return

        // 跨分组拖拽：记录意图，交由 onDragEnd 执行结构性转移
        if (isCrossMove(fromKey, toKey)) {
            _pendingCrossMove.value = CrossMoveIntent(fromKey, toKey)
            return
        }

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
            val insertIndex = if (toBlockIndex > fromBlockIndex) toBlockIndex - 1 else toBlockIndex
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
            val fromParts = fromKey.removePrefix("item_").split("_", limit = 3)
            val toParts = toKey.removePrefix("item_").split("_", limit = 3)
            if (fromParts.size < 3 || toParts.size < 3) return

            val fromGroupId = fromParts[0].toLongOrNull() ?: return
            val fromCategoryPath = fromParts[1]
            val fromItemId = fromParts.drop(2).joinToString("_").toLongOrNull() ?: return

            val toGroupId = toParts[0].toLongOrNull() ?: return
            val toCategoryPath = toParts[1]
            val toItemId = toParts.drop(2).joinToString("_").toLongOrNull() ?: return

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

    /**
     * 判断是否为跨分组拖拽（需要结构性转移，而非同组内排序）。
     */
    private fun isCrossMove(fromKey: String, toKey: String): Boolean {
        // 分组拖到不同分组 → 把源分组降级为目标分组的子分组
        if (fromKey.startsWith("g_") && toKey.startsWith("g_")) {
            return fromKey.substring(2) != toKey.substring(2)
        }
        // 配置项拖到其它分组 / 其它分组的子分组 → 转移配置项
        if (fromKey.startsWith("item_") && (toKey.startsWith("g_") || toKey.startsWith("sub_"))) {
            val fromGId = runCatching {
                fromKey.removePrefix("item_").split("_", limit = 3)[0].toLong()
            }.getOrNull() ?: return false
            val toGId = if (toKey.startsWith("g_")) {
                runCatching { toKey.substring(2).toLong() }.getOrNull()
            } else {
                runCatching { toKey.removePrefix("sub_").substringBefore("_").toLong() }.getOrNull()
            } ?: return false
            return fromGId != toGId
        }
        return false
    }

    /**
     * 拖拽结束时执行真正的跨分组转移。返回非空字符串表示失败原因（需提示）。
     */
    suspend fun handleCrossMove(): String? = withContext(Dispatchers.IO) {
        val intent = _pendingCrossMove.value ?: return@withContext null
        _pendingCrossMove.value = null
        val (fromKey, toKey) = intent

        // 分组 → 分组：把源分组降级为目标分组的子分组
        if (fromKey.startsWith("g_") && toKey.startsWith("g_")) {
            val fromId = fromKey.substring(2).toLong()
            val toId = toKey.substring(2).toLong()
            val src = dbm.systemTtsV2.getGroup(fromId) ?: return@withContext "未找到源分组"
            val target = dbm.systemTtsV2.getGroup(toId) ?: return@withContext "未找到目标分组"
            return@withContext convertGroupToSubGroup(src, target)
        }

        // 配置项 → 分组 / 子分组
        if (fromKey.startsWith("item_") && (toKey.startsWith("g_") || toKey.startsWith("sub_"))) {
            val fromParts = fromKey.removePrefix("item_").split("_", limit = 3)
            if (fromParts.size < 3) return@withContext "拖拽数据异常"
            val fromItemId = runCatching { fromParts.drop(2).joinToString("_").toLong() }.getOrNull()
                ?: return@withContext "拖拽数据异常"
            val item = runCatching { dbm.systemTtsV2.get(fromItemId) }.getOrNull()
                ?: return@withContext "未找到配置项"

            val (targetGroupId, targetCategoryPath) = if (toKey.startsWith("g_")) {
                toKey.substring(2).toLong() to ""
            } else {
                val gId = toKey.removePrefix("sub_").substringBefore("_").toLong()
                val path = toKey.removePrefix("sub_${gId}_")
                gId to path
            }
            return@withContext moveItemToGroup(item, targetGroupId, targetCategoryPath)
        }
        null
    }

    private fun convertGroupToSubGroup(src: SystemTtsGroup, target: SystemTtsGroup): String? {
        val srcItems = dbm.systemTtsV2.getTtsListByGroupId(src.id)
        if (srcItems.any { it.categoryPath.isNotBlank() }) {
            return "分组「${src.name}」包含子分组，无法转为子分组"
        }

        // 目标分组已有同名子分组则合并，避免覆盖其音频参数
        val subName = src.name
        val subMap: Map<String, AudioParams> =
            if (target.subGroupAudioParamsJson.isBlank() || target.subGroupAudioParamsJson == "{}") emptyMap()
            else runCatching {
                SystemTtsV2.Converters.json.decodeFromString<Map<String, AudioParams>>(target.subGroupAudioParamsJson)
            }.getOrNull() ?: emptyMap()

        if (subName !in subMap) {
            val newMap = subMap.toMutableMap().apply { this[subName] = src.audioParams }
            dbm.systemTtsV2.updateGroup(
                target.copy(subGroupAudioParamsJson = SystemTtsV2.Converters.json.encodeToString(newMap))
            )
        }

        srcItems.forEach { item ->
            dbm.systemTtsV2.update(item.copy(groupId = target.id, categoryPath = subName))
        }
        dbm.systemTtsV2.deleteGroup(src)
        dbm.systemTtsV2.updateAllOrder()
        return null
    }

    private fun moveItemToGroup(
        item: SystemTtsV2,
        targetGroupId: Long,
        targetCategoryPath: String,
    ): String? {
        if (item.groupId == targetGroupId && item.categoryPath == targetCategoryPath) return null
        val maxOrder = dbm.systemTtsV2.getTtsListByGroupId(targetGroupId).maxOfOrNull { it.order } ?: -1
        dbm.systemTtsV2.update(
            item.copy(groupId = targetGroupId, categoryPath = targetCategoryPath, order = maxOrder + 1)
        )
        dbm.systemTtsV2.updateAllOrder()
        return null
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

    fun checkListData(context: Context) {
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
    }
}
