package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.github.jing332.common.utils.longToast
import com.github.jing332.common.utils.toast
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.GroupWithSystemTts
import com.github.jing332.database.entities.systts.SystemTtsMigration
import com.github.jing332.database.entities.systts.v1.GroupWithV1TTS
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.ConfigImportBottomSheet
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import com.drake.net.utils.withIO
import kotlinx.coroutines.launch

@Composable
fun ListImportBottomSheet(onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ConfigImportBottomSheet(onDismissRequest = onDismissRequest,
        onImport = { json ->
            // 自识别JSON格式并直接导入，无需手动选择
            context.toast(R.string.import_in_progress)
            scope.launch {
                val count = withIO {
                    val list = try {
                        getImportList(json, false)
                    } catch (e: Exception) {
                        return@withIO -2
                    }
                    if (list.isNullOrEmpty()) return@withIO -1
                    val baseId = System.currentTimeMillis()
                    var nextOrder = dbm.systemTtsV2.groupCount
                    var groupSeq = 0
                    var ttsSeq = 0
                    // 旧 groupId → (新 groupId, 新 order)
                    val oldToNewGroupId = mutableMapOf<Long, Pair<Long, Int>>()
                    for (groupWithTts in list) {
                        if (groupWithTts.group.id !in oldToNewGroupId) {
                            oldToNewGroupId[groupWithTts.group.id] = (baseId + groupSeq) to nextOrder
                            groupSeq++
                            nextOrder++
                        }
                    }
                    var imported = 0
                    for (groupWithTts in list) {
                        val (group, ttsList) = groupWithTts
                        val (newGroupId, newOrder) = oldToNewGroupId[group.id]!!
                        dbm.systemTtsV2.insertGroup(group.copy(id = newGroupId, order = newOrder))
                        for (tts in ttsList) {
                            dbm.systemTtsV2.insert(tts.copy(id = baseId + 100000 + ttsSeq, groupId = newGroupId))
                            ttsSeq++
                            imported++
                        }
                    }
                    imported
                }
                when {
                    count > 0 -> {
                        SystemTtsService.notifyUpdateConfig()
                        context.longToast(R.string.config_import_success_msg, count)
                        onDismissRequest()
                    }
                    count == -2 -> context.longToast(R.string.import_failed)
                    else -> context.longToast(R.string.import_no_valid_config)
                }
            }
        }
    )
}

private fun getImportList(
    json: String,
    fromLegado: Boolean,
): List<GroupWithSystemTts>? {
    if (fromLegado) {
        return null
    } else {
        return if (json.contains("\"group\"")) { // 新版数据结构
            if (json.contains("\"config\"") && json.contains("\"source\"")) {
                AppConst.jsonBuilder.decodeFromString<List<GroupWithSystemTts>>(json)
            } else {
                val old = AppConst.jsonBuilder.decodeFromString<List<GroupWithV1TTS>>(json)
                old.map {
                    GroupWithSystemTts(
                        it.group,
                        it.list.map { tts -> SystemTtsMigration.v1Tov2(tts) }.filterNotNull()
                    )
                }
            }

        } else {
            null
        }
    }
}
