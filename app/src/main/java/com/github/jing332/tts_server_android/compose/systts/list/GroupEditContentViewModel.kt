package com.github.jing332.tts_server_android.compose.systts.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GroupEditContentViewModel : ViewModel() {
    private val _availableConfigs = MutableStateFlow<List<SystemTtsV2>>(emptyList())
    val availableConfigs: StateFlow<List<SystemTtsV2>> = _availableConfigs.asStateFlow()
    
    private val _pluginNameCache = MutableStateFlow<Map<String, String>>(emptyMap())
    val pluginNameCache: StateFlow<Map<String, String>> = _pluginNameCache.asStateFlow()

    private val _currentGroupSubPaths = MutableStateFlow<List<String>>(emptyList())
    val currentGroupSubPaths: StateFlow<List<String>> = _currentGroupSubPaths.asStateFlow()

    private var currentGroupId: Long = 0
    private var allConfigs: List<SystemTtsV2> = emptyList()

    fun load(groupId: Long) {
        viewModelScope.launch {
            currentGroupId = groupId
            withContext(Dispatchers.IO) {
                // 加载配置
                allConfigs = dbm.systemTtsV2.all
                _availableConfigs.value = allConfigs.filter { it.groupId != groupId }

                // 获取当前分组已有的所有子分组路径
                _currentGroupSubPaths.value = allConfigs
                    .filter { it.groupId == groupId && it.categoryPath.isNotBlank() }
                    .map { it.categoryPath }
                    .distinct()
                    .sorted()

                // 缓存所有插件名称
                val plugins = dbm.pluginDao.getAllWithoutCode()
                _pluginNameCache.value = plugins.associate { it.pluginId to it.name }
            }
        }
    }
    
    fun filterConfigs(
        configs: List<SystemTtsV2>,
        query: String,
        searchType: GroupSearchType,
        pluginCache: Map<String, String> = emptyMap()
    ): List<SystemTtsV2> {
        if (query.isBlank()) return configs
        
        return configs.filter { config ->
            // 安全获取 TtsConfigurationDTO，如果不是则跳过
            val ttsConfig = config.config as? TtsConfigurationDTO ?: return@filter false
            
            when (searchType) {
                GroupSearchType.NAME -> {
                    config.displayName.contains(query, ignoreCase = true)
                }
                GroupSearchType.TAG -> {
                    val speechRule = ttsConfig.speechRule
                    speechRule.tagName.contains(query, ignoreCase = true) ||
                    speechRule.tag.contains(query, ignoreCase = true) ||
                    speechRule.tagData.values.any { it.contains(query, ignoreCase = true) }
                }
                GroupSearchType.PLUGIN -> {
                    when (val source = ttsConfig.source) {
                        is PluginTtsSource -> {
                            // 搜索 pluginId 或使用缓存的插件名称
                            val pluginName = pluginCache[source.pluginId] ?: source.pluginId
                            source.pluginId.contains(query, ignoreCase = true) ||
                            pluginName.contains(query, ignoreCase = true)
                        }
                        is LocalTtsSource -> 
                            "本地".contains(query, ignoreCase = true) || 
                            "local".contains(query, ignoreCase = true)
                        else -> false
                    }
                }
            }
        }
    }
    
    suspend fun moveConfigsToGroup(configs: List<SystemTtsV2>) {
        withContext(Dispatchers.IO) {
            configs.forEach { config ->
                // 修复：移动到新分组时清空子分组路径，避免保留旧分组的子分组归属
                val updatedConfig = config.copy(groupId = currentGroupId, categoryPath = "")
                dbm.systemTtsV2.update(updatedConfig)
            }
        }
    }
    
    fun refresh() {
        load(currentGroupId)
    }
}
