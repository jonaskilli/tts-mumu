package com.github.jing332.tts_server_android.compose.systts.list

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.drake.net.utils.withIO
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.ListSortSettingsDialog
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.BgmConfiguration
import com.github.jing332.database.entities.systts.EmptyConfiguration
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.LocalTtsSource

internal enum class SortFields(@StringRes val strResId: Int) {
    TAG_NAME(R.string.tag),
    NAME(R.string.name),
    TYPE(R.string.type),
    ENABLE(R.string.enabled),
    ID(R.string.created_time_id)
}

/**
 * 自然排序比较：将字符串中的数字部分按数值大小比较。
 * 例如 "女青年09" < "女青年10" < "女青年100"（而非字典序 "女青年09" < "女青年100" < "女青年10"）
 */
private fun naturalCompare(a: String, b: String): Int {
    var i = 0
    var j = 0
    while (i < a.length && j < b.length) {
        if (a[i].isDigit() && b[j].isDigit()) {
            var numA = 0
            while (i < a.length && a[i].isDigit()) {
                numA = numA * 10 + (a[i] - '0')
                i++
            }
            var numB = 0
            while (j < b.length && b[j].isDigit()) {
                numB = numB * 10 + (b[j] - '0')
                j++
            }
            if (numA != numB) return numA - numB
        } else {
            val cmp = a[i].compareTo(b[j])
            if (cmp != 0) return cmp
            i++
            j++
        }
    }
    return a.length - b.length
}

private val naturalStringComparator = Comparator<String> { a, b -> naturalCompare(a, b) }

private fun getTypeString(systts: SystemTtsV2, pluginNameCache: MutableMap<String, String>): String {
    val config = systts.config
    return when (config) {
        is BgmConfiguration -> "BGM"
        is EmptyConfiguration -> "Empty"
        is TtsConfigurationDTO -> {
            val source = config.source
            when (source) {
                is LocalTtsSource -> "Local"
                else -> {
                    val pluginId = (source as? com.github.jing332.database.entities.systts.source.PluginTtsSource)?.pluginId
                    if (pluginId != null) {
                        pluginNameCache.getOrPut(pluginId) {
                            dbm.pluginDao.getEnabled(pluginId)?.name ?: pluginId
                        }
                    } else "Unknown"
                }
            }
        }
        else -> "Unknown"
    }
}

@Composable
internal fun SortDialog(
    onDismissRequest: () -> Unit,
    list: List<SystemTtsV2>,
    groupList: List<SystemTtsV2>? = null,
) {
    var index by remember { mutableIntStateOf(0) }
    ListSortSettingsDialog(
        name = list.size.toString(),
        index = index,
        onIndexChange = { index = it },
        onDismissRequest = onDismissRequest,
        entries = SortFields.values().map { stringResource(id = it.strResId) },
        onConfirm = { _, descending ->
            withIO {
                val pluginNameCache = mutableMapOf<String, String>()
                val field = SortFields.values()[index]
                val comparator = when (field) {
                    SortFields.NAME -> compareBy<SystemTtsV2> { it.displayName }
                    SortFields.TAG_NAME -> compareBy<SystemTtsV2, String>(naturalStringComparator) {
                        (it.config as? TtsConfigurationDTO)?.speechRule?.tag ?: ""
                    }
                    SortFields.TYPE -> compareBy<SystemTtsV2> { getTypeString(it, pluginNameCache) }
                    SortFields.ENABLE -> compareBy<SystemTtsV2> { it.isEnabled }
                    SortFields.ID -> compareBy<SystemTtsV2> { it.id }
                }.let {
                    if (descending) it.reversed() else it
                }

                val toUpdate = mutableListOf<SystemTtsV2>()

                if (groupList != null) {
                    // 子分组排序：保持子分组在大分组中的相对位置，只改变子分组内部顺序
                    val sortedList = list.sortedWith(comparator)
                    val subIds = list.map { it.id }.toSet()
                    val allSorted = groupList.sortedBy { it.order }.toMutableList()
                    val firstSubIndex = allSorted.indexOfFirst { it.id in subIds }
                        .coerceAtLeast(0)
                    allSorted.removeAll { it.id in subIds }
                    allSorted.addAll(firstSubIndex, sortedList)
                    allSorted.forEachIndexed { i, systts ->
                        if (systts.order != i) {
                            toUpdate.add(systts.copy(order = i))
                        }
                    }
                } else {
                    val hasSubGroups = list.any { it.categoryPath.isNotBlank() }
                    if (hasSubGroups) {
                        // 3.⑦: 一级分组含子分组时，保持子分组位置不变，只排子分组内配置项
                        val grouped = list.groupBy { it.categoryPath }
                        val groupOrder = grouped.entries.sortedBy { (_, items) ->
                            items.minOf { it.order }
                        }
                        var i = 0
                        for ((_, items) in groupOrder) {
                            for (item in items.sortedWith(comparator)) {
                                if (item.order != i) {
                                    toUpdate.add(item.copy(order = i))
                                }
                                i++
                            }
                        }
                    } else {
                        list.sortedWith(comparator).forEachIndexed { i, systemTts ->
                            if (systemTts.order != i) {
                                toUpdate.add(systemTts.copy(order = i))
                            }
                        }
                    }
                }

                if (toUpdate.isNotEmpty()) {
                    dbm.systemTtsV2.update(*toUpdate.toTypedArray())
                }
            }
        }
    )
}
