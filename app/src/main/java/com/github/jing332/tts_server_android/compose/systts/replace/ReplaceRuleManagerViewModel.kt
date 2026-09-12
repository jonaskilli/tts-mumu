package com.github.jing332.tts_server_android.compose.systts.replace

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.app
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.AbstractListGroup.Companion.DEFAULT_GROUP_ID
import com.github.jing332.database.entities.replace.GroupWithReplaceRule
import com.github.jing332.database.entities.replace.ReplaceRule
import com.github.jing332.database.entities.replace.ReplaceRuleGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal class ReplaceRuleManagerViewModel : ViewModel() {
    private var allList = listOf<GroupWithReplaceRule>()
    private val _list = MutableStateFlow<List<GroupWithReplaceRule>>(emptyList())
    val list: MutableStateFlow<List<GroupWithReplaceRule>> get() = _list
    var searchType by mutableStateOf(SearchType.NAME)
    var searchText by mutableStateOf("")

    init {
        viewModelScope.launch(Dispatchers.IO) {
            dbm.replaceRuleDao.getGroup(DEFAULT_GROUP_ID) ?: run {
                dbm.replaceRuleDao.insertGroup(
                    ReplaceRuleGroup(
                        DEFAULT_GROUP_ID,
                        (app as Context).getString(R.string.default_group)
                    )
                )
            }
            dbm.replaceRuleDao.updateAllOrder()
            dbm.replaceRuleDao.flowAllGroupWithReplaceRules().collectLatest {
                allList = it
                updateSearchResult()
            }
        }
    }

    fun updateSearchResult(
        text: String = searchText,
        type: SearchType = searchType,
        src: List<GroupWithReplaceRule> = allList
    ) {
        // 用户 09-12：默认分组按主界面同款口径处理（简化版——替换规则无子分组、无移动类功能）：
        // 空的默认分组不显示（免得"突然冒出来"让人困惑），库里仍保留兜底（init 已确保存在）
        val visible = src.filter { it.group.id != DEFAULT_GROUP_ID || it.list.isNotEmpty() }
        if (visible.isEmpty() || text.isBlank()) {
            _list.value = visible
            return
        }
        val resultList = mutableListOf<GroupWithReplaceRule>()
        visible.forEach {
            val subList = mutableListOf<ReplaceRule>()
            val groupWithRules = GroupWithReplaceRule(it.group, subList)
            resultList.add(groupWithRules)
            it.list.forEach { rule ->
                when (type) {
                    SearchType.GROUP_NAME -> if (it.group.name.contains(text)) subList.add(rule)
                    SearchType.NAME -> if (rule.name.contains(text)) subList.add(rule)
                    SearchType.PATTERN -> if (rule.pattern.contains(text)) subList.add(rule)
                    SearchType.REPLACEMENT -> if (rule.replacement.contains(text)) subList.add(rule)
                }
            }
            if (subList.isEmpty()) resultList.remove(groupWithRules)
        }
        _list.value = resultList
    }

    fun moveTop(rule: ReplaceRule) { dbm.replaceRuleDao.update(rule.copy(order = 0)) }
    fun moveBottom(rule: ReplaceRule) { dbm.replaceRuleDao.update(rule.copy(order = dbm.replaceRuleDao.count)) }
    fun deleteRule(rule: ReplaceRule) { dbm.replaceRuleDao.delete(rule) }
    fun deleteGroup(groupWithRules: GroupWithReplaceRule) {
        dbm.replaceRuleDao.delete(*groupWithRules.list.toTypedArray())
        dbm.replaceRuleDao.deleteGroup(groupWithRules.group)
    }
}
