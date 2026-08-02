package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.github.jing332.common.utils.longToast
import com.github.jing332.common.utils.toast
import com.github.jing332.compose.widgets.LoadingDialog
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

    // 导入进行中遮罩：避免大文件解析/写入期间界面无反馈，用户以为"没有直接导入"
    var importing by remember { mutableStateOf(false) }
    if (importing) {
        LoadingDialog(onDismissRequest = { /* 不可取消，等待导入完成 */ })
    }

    ConfigImportBottomSheet(onDismissRequest = onDismissRequest,
        onImport = { json ->
            // 自识别JSON格式并直接导入，无需手动选择
            importing = true
            context.toast(R.string.import_in_progress)
            scope.launch {
                val result = withIO { doImport(json) }
                importing = false
                when (result) {
                    ImportResult.EmptyOrUnrecognized -> {
                        context.longToast(R.string.import_no_valid_config)
                    }
                    is ImportResult.Truncated -> {
                        // JSON 解析失败：可能是大文件读取被截断，或源文件本身损坏
                        context.longToast(R.string.import_truncated_hint, result.detail)
                    }
                    is ImportResult.Success -> {
                        SystemTtsService.notifyUpdateConfig()
                        context.longToast(R.string.config_import_success_msg, result.count)
                        onDismissRequest()
                    }
                }
            }
        }
    )
}

/** 导入结果 */
private sealed class ImportResult {
    object EmptyOrUnrecognized : ImportResult()
    data class Truncated(val detail: String) : ImportResult()
    data class Success(val count: Int) : ImportResult()
}

/**
 * 实际导入逻辑：解析 → 校验完整性 → 写库。
 * 返回 [ImportResult] 以便 UI 区分"未识别"、"被截断"、"成功"。
 */
private fun doImport(json: String): ImportResult {
    val trimmed = json.trim()
    if (trimmed.isEmpty()) return ImportResult.EmptyOrUnrecognized

    // 1. 粗校验：必须是 JSON 数组开头/结尾，否则很可能被截断
    if (!trimmed.startsWith("[")) return ImportResult.EmptyOrUnrecognized
    if (!trimmed.endsWith("]")) {
        // 尾部缺失 ] —— 典型的截断特征（大文件读取到一半）
        return ImportResult.Truncated("JSON 未以 ']' 结尾")
    }

    // 2. 解析：捕获 JSON 异常并区分截断
    val list = try {
        getImportList(json, false)
    } catch (e: kotlinx.serialization.SerializationException) {
        // 解析中途失败：可能是字段缺失/类型不匹配，也可能是大文件截断
        return ImportResult.Truncated(e.message ?: e.toString())
    } catch (e: Exception) {
        return ImportResult.Truncated(e.message ?: e.toString())
    }
    if (list.isNullOrEmpty()) return ImportResult.EmptyOrUnrecognized

    // 3. 写库
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
    return ImportResult.Success(imported)
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
