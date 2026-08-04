package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.github.jing332.common.utils.StringUtils
import com.github.jing332.common.utils.longToast
import com.github.jing332.common.utils.toast
import com.github.jing332.common.utils.toJsonListString
import com.github.jing332.compose.widgets.LoadingDialog
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.replace.GroupWithReplaceRule
import com.github.jing332.database.entities.replace.ReplaceRule
import com.github.jing332.database.entities.replace.ReplaceRuleGroup
import com.github.jing332.database.entities.systts.GroupWithSystemTts
import com.github.jing332.database.entities.systts.SystemTtsGroup
import com.github.jing332.database.entities.systts.SystemTtsMigration
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.v1.GroupWithV1TTS
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.ConfigImportBottomSheet
import com.github.jing332.tts_server_android.compose.systts.plugin.parsePluginsJson
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import com.github.jing332.tts_server_android.ui.systts.ImportConfigFactory
import com.github.jing332.tts_server_android.ui.systts.ImportType
import com.drake.net.utils.withIO
import kotlinx.coroutines.launch

@Composable
fun ListImportBottomSheet(onDismissRequest: () -> Unit, showSuccessDialog: Boolean = false) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 导入进行中遮罩：避免大文件解析/写入期间界面无反馈，用户以为"没有直接导入"
    var importing by remember { mutableStateOf(false) }
    if (importing) {
        LoadingDialog(onDismissRequest = { /* 不可取消，等待导入完成 */ })
    }

    // 外部文件打开导入（ImportConfigActivity）时，Activity 会立即 finish，
    // Toast 会被 Activity 销毁流程压制导致滞后几秒才显示。
    // 改用 AlertDialog 模态提示，立即渲染，用户确认后再 finish。
    var successMsg by remember { mutableStateOf<String?>(null) }
    successMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { successMsg = null; onDismissRequest() },
            confirmButton = {
                TextButton(onClick = { successMsg = null; onDismissRequest() }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            text = { Text(msg) }
        )
    }

    ConfigImportBottomSheet(onDismissRequest = onDismissRequest,
        autoImport = true,
        onImport = { json ->
            // 自识别 JSON 类型并直接导入，无需手动选择/确认
            importing = true
            scope.launch {
                val result = withIO { doAutoImport(json) }
                importing = false
                when (result) {
                    AutoImportResult.EmptyOrUnrecognized -> {
                        context.longToast(R.string.import_no_valid_config)
                    }
                    is AutoImportResult.Truncated -> {
                        // JSON 解析失败：可能是大文件读取被截断，或源文件本身损坏
                        context.longToast(R.string.import_truncated_hint, result.detail)
                    }
                    is AutoImportResult.Success -> {
                        // 仅配置列表需要通知 TTS 服务刷新
                        if (result.type == ImportType.LIST) {
                            SystemTtsService.notifyUpdateConfig()
                        }
                        if (showSuccessDialog) {
                            successMsg = "已导入 ${result.count} 项${result.typeName}"
                        } else {
                            context.toast("已导入 ${result.count} 项${result.typeName}")
                            onDismissRequest()
                        }
                    }
                }
            }
        }
    )
}

/** 自动导入结果 */
internal sealed class AutoImportResult {
    object EmptyOrUnrecognized : AutoImportResult()
    data class Truncated(val detail: String) : AutoImportResult()
    data class Success(val count: Int, val type: ImportType, val typeName: String) : AutoImportResult()
}

/**
 * 自动识别 JSON 类型并直接导入，无需手动选择/确认。
 * 支持：配置列表 / 插件 / 朗读规则 / 替换规则。
 */
internal fun doAutoImport(json: String): AutoImportResult {
    val trimmed = json.trim()
    if (trimmed.isEmpty()) return AutoImportResult.EmptyOrUnrecognized

    val type = try {
        ImportConfigFactory.detectType(trimmed)
    } catch (e: Exception) {
        return AutoImportResult.Truncated(e.message ?: e.toString())
    } ?: return AutoImportResult.EmptyOrUnrecognized

    return when (type) {
        ImportType.LIST -> doImportList(json).let { result ->
            when (result) {
                ListImportResult.EmptyOrUnrecognized -> AutoImportResult.EmptyOrUnrecognized
                is ListImportResult.Truncated -> AutoImportResult.Truncated(result.detail)
                is ListImportResult.Success -> AutoImportResult.Success(result.count, type, "配置列表")
            }
        }
        ImportType.PLUGIN -> doImportPlugin(json, type)
        ImportType.SPEECH_RULE -> doImportSpeechRule(json, type)
        ImportType.REPLACE_RULE -> doImportReplaceRule(json, type)
    }
}

/** 插件：解析并直接写库，不弹确认对话框 */
private fun doImportPlugin(json: String, type: ImportType): AutoImportResult {
    val plugins = runCatching { parsePluginsJson(json) }.getOrDefault(emptyList<Plugin>())
    if (plugins.isEmpty()) return AutoImportResult.EmptyOrUnrecognized
    dbm.pluginDao.insert(*plugins.toTypedArray())
    return AutoImportResult.Success(plugins.size, type, "插件")
}

/** 朗读规则：解析并直接写库，不弹确认对话框 */
private fun doImportSpeechRule(json: String, type: ImportType): AutoImportResult {
    val rules = runCatching {
        AppConst.jsonBuilder.decodeFromString<List<SpeechRule>>(json)
    }.getOrDefault(emptyList())
    if (rules.isEmpty()) return AutoImportResult.EmptyOrUnrecognized
    dbm.speechRuleDao.insert(*rules.toTypedArray())
    return AutoImportResult.Success(rules.size, type, "朗读规则")
}

/** 替换规则：解析并直接写库，不弹确认对话框 */
private fun doImportReplaceRule(json: String, type: ImportType): AutoImportResult {
    val pairs = mutableListOf<Pair<ReplaceRuleGroup, ReplaceRule>>()
    if (json.contains("\"group\"")) {
        runCatching {
            AppConst.jsonBuilder.decodeFromString<List<GroupWithReplaceRule>>(json.toJsonListString())
        }.getOrDefault(emptyList()).forEach { gwt ->
            val group = gwt.group
            gwt.list.forEach { rule -> pairs.add(group to rule) }
        }
    } else {
        // 单条/裸数组：生成一个新分组
        val groupName = StringUtils.formattedDate()
        val group = ReplaceRuleGroup(name = groupName)
        runCatching {
            AppConst.jsonBuilder.decodeFromString<List<ReplaceRule>>(json.toJsonListString())
        }.getOrDefault(emptyList()).forEach { rule ->
            pairs.add(group to rule.apply { groupId = group.id })
        }
    }
    if (pairs.isEmpty()) return AutoImportResult.EmptyOrUnrecognized
    pairs.forEach {
        dbm.replaceRuleDao.insert(it.second)
        dbm.replaceRuleDao.insertGroup(it.first)
    }
    dbm.replaceRuleDao.updateAllOrder()
    return AutoImportResult.Success(pairs.size, type, "替换规则")
}

/** 配置列表导入结果 */
private sealed class ListImportResult {
    object EmptyOrUnrecognized : ListImportResult()
    data class Truncated(val detail: String) : ListImportResult()
    data class Success(val count: Int) : ListImportResult()
}

/**
 * 配置列表导入逻辑：解析 → 校验完整性 → 写库。
 */
private fun doImportList(json: String): ListImportResult {
    val trimmed = json.trim()
    if (trimmed.isEmpty()) return ListImportResult.EmptyOrUnrecognized

    // 1. 粗校验：必须是 JSON 数组开头/结尾，否则很可能被截断
    if (!trimmed.startsWith("[")) return ListImportResult.EmptyOrUnrecognized
    if (!trimmed.endsWith("]")) {
        return ListImportResult.Truncated("JSON 未以 ']' 结尾")
    }

    // 2. 解析：捕获 JSON 异常并区分截断
    val list = try {
        getImportList(json, false)
    } catch (e: kotlinx.serialization.SerializationException) {
        return ListImportResult.Truncated(e.message ?: e.toString())
    } catch (e: Exception) {
        return ListImportResult.Truncated(e.message ?: e.toString())
    }
    if (list.isNullOrEmpty()) return ListImportResult.EmptyOrUnrecognized

    // 3. 写库：单事务批量插入，避免逐条 fsync
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
    dbm.runInTransaction {
        val groupsToInsert = mutableListOf<SystemTtsGroup>()
        val ttsToInsert = mutableListOf<SystemTtsV2>()
        for (groupWithTts in list) {
            val (group, ttsList) = groupWithTts
            val (newGroupId, newOrder) = oldToNewGroupId[group.id]!!
            groupsToInsert.add(group.copy(id = newGroupId, order = newOrder))
            for (tts in ttsList) {
                ttsToInsert.add(tts.copy(id = baseId + 100000 + ttsSeq, groupId = newGroupId))
                ttsSeq++
                imported++
            }
        }
        dbm.systemTtsV2.insertGroup(*groupsToInsert.toTypedArray())
        dbm.systemTtsV2.insert(*ttsToInsert.toTypedArray())
    }
    return ListImportResult.Success(imported)
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
