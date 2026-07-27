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
    NAME(R.string.name),
    TAG_NAME(R.string.tag),
    TYPE(R.string.type),
    ENABLE(R.string.enabled),
    ID(R.string.created_time_id)
}

private fun getTypeString(systts: SystemTtsV2): String {
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
                        dbm.pluginDao.getEnabled(pluginId)?.name ?: pluginId
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
                val sortedList = when (SortFields.values()[index]) {
                    SortFields.NAME -> list.sortedBy { it.displayName }
                    SortFields.TAG_NAME -> list.sortedBy { (it.config as? TtsConfigurationDTO)?.speechRule?.tagName ?: "" }
                    SortFields.TYPE -> list.sortedBy { getTypeString(it) }
                    SortFields.ENABLE -> list.sortedBy { it.isEnabled }
                    SortFields.ID -> list.sortedBy { it.id }
                }.run {
                    if (descending) this.reversed() else this
                }

                if (groupList != null) {
                    // 子分组排序：保持子分组在大分组中的相对位置，只改变子分组内部顺序
                    val subIds = list.map { it.id }.toSet()
                    val allSorted = groupList.sortedBy { it.order }.toMutableList()
                    val firstSubIndex = allSorted.indexOfFirst { it.id in subIds }
                        .coerceAtLeast(0)
                    allSorted.removeAll { it.id in subIds }
                    allSorted.addAll(firstSubIndex, sortedList)
                    allSorted.forEachIndexed { i, systts ->
                        dbm.systemTtsV2.update(systts.copy(order = i))
                    }
                } else {
                    sortedList.forEachIndexed { i, systemTts ->
                        dbm.systemTtsV2.update(systemTts.copy(order = i))
                    }
                }
            }
        }
    )
}