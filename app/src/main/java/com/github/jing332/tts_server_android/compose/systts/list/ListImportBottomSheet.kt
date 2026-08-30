package com.github.jing332.tts_server_android.compose.systts.list

import android.content.Context
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
import com.github.jing332.common.utils.toJsonListString
import com.github.jing332.compose.widgets.LoadingDialog
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.replace.GroupWithReplaceRule
import com.github.jing332.database.entities.replace.ReplaceRule
import com.github.jing332.database.entities.replace.ReplaceRuleGroup
import com.github.jing332.database.entities.systts.GroupWithSystemTts
import com.github.jing332.database.entities.systts.JReadConfigMigration
import com.github.jing332.database.entities.systts.SystemTtsGroup
import com.github.jing332.database.entities.systts.SystemTtsMigration
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.v1.GroupWithV1TTS
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.ConfigImportBottomSheet
import com.github.jing332.tts_server_android.compose.systts.plugin.parsePluginsJson
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import com.github.jing332.tts_server_android.ui.systts.ImportConfigFactory
import com.github.jing332.tts_server_android.ui.systts.ImportType
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import com.drake.net.utils.withIO
import kotlinx.coroutines.CoroutineScope

@Composable
fun ListImportBottomSheet(onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    // 导入协程作用域：独立于 BottomSheet 生命周期，关闭面板后导入仍继续
    val importScope = rememberCoroutineScope()

    // 面板可见性：导入开始后置 false 仅收起 ModalBottomSheet，本 composable 保持挂载，
    // 使 importScope / 全屏遮罩 / 结果弹窗存活，避免协程被取消导致导入静默失败。
    var sheetVisible by remember { mutableStateOf(true) }
    // 导入进行中遮罩（全屏，面板收起后由本 composable 承载，自然置于最上层）
    var isImporting by remember { mutableStateOf(false) }
    // 导入结果文案（成功/失败原因），非 null 时弹出模态对话框
    var successMsg = remember { mutableStateOf<String?>(null) }

    // 先取局部 val 再判空：局部 val 支持 smart cast，MutableState.value 属性不支持
    val msgText = successMsg.value
    if (msgText != null) {
        AlertDialog(
            onDismissRequest = {
                successMsg.value = null
                sheetVisible = false
                onDismissRequest()
            },
            confirmButton = {
                TextButton(onClick = {
                    successMsg.value = null
                    sheetVisible = false
                    onDismissRequest()
                }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            text = { Text(msgText) }
        )
        return
    }

    if (isImporting) {
        LoadingDialog(onDismissRequest = {}, text = context.getString(R.string.importing))
    }

    if (sheetVisible) {
        // 导入过程（读取→解析→写库）由本 composable 承载全屏遮罩，
        // 这里仅负责异步写库与结果回传，结果通过 onResult 抛回。
        ConfigImportBottomSheet(
            onDismissRequest = { sheetVisible = false },
            autoImport = true,
            importScope = importScope,
            sheetVisible = sheetVisible,
            // 开始导入：仅收起面板 + 显示全屏遮罩（不卸载本 composable），
            // 否则 importScope 随组合销毁被取消，导入静默失败。
            onImportStart = { isImporting = true; sheetVisible = false },
            onResult = {
                isImporting = false
                if (it != null) {
                    successMsg.value = it
                } else {
                    // 无结果文案 = 读取/识别/解析失败（错误对话框已另行弹出）：
                    // 重新弹出面板供换源重试，避免「面板不可见但组合仍挂载」
                    // 导致导入入口卡死（内部导入）或页面空白（外部导入）
                    sheetVisible = true
                }
            },
            onImport = { json ->
                // 自识别 JSON 类型并直接导入，无需手动选择/确认
                // （suspend lambda：在 importScope 内执行，勿再自起协程）
                val result = withIO {
                    doAutoImport(
                        json,
                        context = context,
                        onProgress = { _, _ -> }
                    )
                }
                when (result) {
                    is AutoImportResult.EmptyOrUnrecognized -> {
                        // 识别不出类型：与解析失败一致，用错误对话框展示原因（可滚动/复制）
                        context.displayErrorDialog(
                            Exception(result.reason),
                            title = context.getString(R.string.import_no_valid_config)
                        )
                    }
                    is AutoImportResult.Truncated -> {
                        // JSON 解析失败：用错误对话框展示完整信息，支持滚动和复制
                        context.displayErrorDialog(
                            Exception(result.detail),
                            title = context.getString(R.string.import_failed)
                        )
                    }
                    is AutoImportResult.Success -> {
                        // 仅配置列表需要通知 TTS 服务刷新，并强制重算 tagName（防止导入旧格式）
                        if (result.type == ImportType.LIST) {
                            withIO { migrateTagNamesIfNeed(context, force = true) }
                            SystemTtsService.notifyUpdateConfig()
                        }
                        successMsg.value = result.typeName
                    }
                }
            }
        )
    }
}

/** 自动导入结果 */
internal sealed class AutoImportResult {
    // reason：识别不出类型时的可读原因（如「无法判断配置类型…」），用于向用户展示而非只给一句空话
    data class EmptyOrUnrecognized(val reason: String) : AutoImportResult()
    data class Truncated(val detail: String) : AutoImportResult()
    data class Success(val count: Int, val type: ImportType, val typeName: String) : AutoImportResult()
}

/**
 * 自动识别 JSON 类型并直接导入，无需手动选择/确认。
 * 支持：配置列表 / 插件 / 朗读规则 / 替换规则。
 */
internal fun doAutoImport(
    json: String,
    context: Context,
    onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
): AutoImportResult {
    val trimmed = json.trim()
    if (trimmed.isEmpty()) return AutoImportResult.EmptyOrUnrecognized(
        context.getString(R.string.import_no_valid_config_reason_empty)
    )

    // 截断检测（必须在补括号之前、针对原始内容）：
    // 数组开头但（去掉尾逗号后）未以 ']' 结尾 → 大概率被截断，直接报错。
    // 不能先 toJsonListString() 补 ']' 再解析：那会把截断内容"修"成合法 JSON，静默导入部分数据
    if (trimmed.startsWith("[") && !trimmed.removeSuffix(",").endsWith("]")) {
        return AutoImportResult.Truncated(
            context.getString(R.string.import_truncated_hint, "JSON 未以 ']' 结尾，内容可能被截断")
        )
    }

    // 先按原始内容识别；识别不出再按遗留格式（逗号分隔对象/缺外层括号）包裹后重试
    var effective = trimmed
    var type: ImportType? = null
    try {
        type = ImportConfigFactory.detectType(trimmed)
        if (type == null) {
            effective = trimmed.toJsonListString()
            type = ImportConfigFactory.detectType(effective)
        }
    } catch (e: Exception) {
        return AutoImportResult.Truncated(e.message ?: e.toString())
    }

    if (type == null) {
        return AutoImportResult.EmptyOrUnrecognized(
            context.getString(R.string.import_no_valid_config_reason_unknown)
        )
    }

    return when (type) {
        ImportType.LIST -> doImportList(effective, onProgress).let { result ->
            when (result) {
                is ListImportResult.EmptyOrUnrecognized -> AutoImportResult.EmptyOrUnrecognized(
                    context.getString(R.string.import_no_valid_config_reason_unknown)
                )
                is ListImportResult.Truncated -> AutoImportResult.Truncated(result.detail)
                is ListImportResult.Success -> {
                    var msg = "已导入 ${result.groupCount} 组配置列表，共 ${result.count} 项"
                    if (result.skipped > 0) {
                        msg += "，跳过 ${result.skipped} 项"
                        // 按原因分类计数，帮助定位缺插件/直连型配置
                        val reasons = mutableListOf<String>()
                        if (result.skippedNoPlugin > 0) reasons += "${result.skippedNoPlugin} 项缺少对应插件"
                        if (result.skippedUrlDirect > 0) reasons += "${result.skippedUrlDirect} 项 URL 直连型"
                        if (reasons.isNotEmpty()) msg += "（${reasons.joinToString("、")}）"
                    }
                    AutoImportResult.Success(result.count, type, msg)
                }
            }
        }
        ImportType.PLUGIN -> doImportPlugin(effective, type)
        ImportType.SPEECH_RULE -> doImportSpeechRule(effective, type)
        ImportType.REPLACE_RULE -> doImportReplaceRule(effective, type)
    }
}

/** 插件：解析并直接写库，不弹确认对话框 */
private fun doImportPlugin(json: String, type: ImportType): AutoImportResult {
    var parseError: String? = null
    val plugins = runCatching {
        parsePluginsJson(json)
    }.onFailure { e ->
        parseError = "${e::class.java.simpleName}: ${e.message}"
        android.util.Log.e("PluginImport", "parse plugins failed", e)
    }.getOrDefault(emptyList<Plugin>())
    if (plugins.isEmpty()) return AutoImportResult.Truncated(parseError ?: "未识别到有效配置")
    dbm.pluginDao.insert(*plugins.toTypedArray())
    return AutoImportResult.Success(plugins.size, type, "已导入 ${plugins.size} 个插件")
}

/** 朗读规则：解析并直接写库，不弹确认对话框 */
private fun doImportSpeechRule(json: String, type: ImportType): AutoImportResult {
    var parseError: String? = null
    val rules = runCatching {
        // 单对象/裸数组统一包裹成数组（对合法数组幂等）
        AppConst.jsonBuilder.decodeFromString<List<SpeechRule>>(json.trim().toJsonListString())
    }.onFailure { e ->
        parseError = "${e::class.java.simpleName}: ${e.message}"
        android.util.Log.e("SpeechRuleImport", "decode SpeechRule failed", e)
    }.getOrDefault(emptyList())
    if (rules.isEmpty()) return AutoImportResult.Truncated(parseError ?: "未识别到有效配置")
    dbm.speechRuleDao.insert(*rules.toTypedArray())
    return AutoImportResult.Success(rules.size, type, "已导入 ${rules.size} 条朗读规则")
}

/** 替换规则：解析并直接写库，不弹确认对话框 */
private fun doImportReplaceRule(json: String, type: ImportType): AutoImportResult {
    val pairs = mutableListOf<Pair<ReplaceRuleGroup, ReplaceRule>>()
    var parseError: String? = null
    // detectType 已验证 JSON 合法性；此处按需补外层括号（单对象→数组）。
    // toJsonListString 对已是合法数组的串是幂等的（先判断头尾再补，不会产生 "]]"）
    val safeJson = json.trim().toJsonListString()
    if (safeJson.contains("\"group\"")) {
        runCatching {
            AppConst.jsonBuilder.decodeFromString<List<GroupWithReplaceRule>>(safeJson)
        }.onFailure { e ->
            parseError = "${e::class.java.simpleName}: ${e.message}"
            android.util.Log.e("ReplaceRuleImport", "decode GroupWithReplaceRule failed", e)
        }.getOrDefault(emptyList()).forEach { gwt ->
            val group = gwt.group
            gwt.list.forEach { rule -> pairs.add(group to rule) }
        }
    } else {
        // 单条/裸数组：生成一个新分组
        val groupName = StringUtils.formattedDate()
        val group = ReplaceRuleGroup(name = groupName)
        runCatching {
            AppConst.jsonBuilder.decodeFromString<List<ReplaceRule>>(safeJson)
        }.onFailure { e ->
            parseError = "${e::class.java.simpleName}: ${e.message}"
            android.util.Log.e("ReplaceRuleImport", "decode ReplaceRule failed", e)
        }.getOrDefault(emptyList()).forEach { rule ->
            pairs.add(group to rule.apply { groupId = group.id })
        }
    }
    if (pairs.isEmpty()) return AutoImportResult.Truncated(parseError ?: "未识别到有效配置")
    pairs.forEach {
        dbm.replaceRuleDao.insert(it.second)
        dbm.replaceRuleDao.insertGroup(it.first)
    }
    dbm.replaceRuleDao.updateAllOrder()
    val groups = pairs.map { it.first.name }.distinct()
    return AutoImportResult.Success(
        pairs.size, type, "已导入 ${groups.size} 组替换规则，共 ${pairs.size} 条"
    )
}

/** 配置列表导入结果 */
private sealed class ListImportResult {
    object EmptyOrUnrecognized : ListImportResult()
    data class Truncated(val detail: String) : ListImportResult()
    data class Success(
        val count: Int, val skipped: Int = 0,
        val skippedNoPlugin: Int = 0, val skippedUrlDirect: Int = 0,
        val groupCount: Int = 0,
    ) : ListImportResult()
}

/**
 * 配置列表导入逻辑：解析 → 校验完整性 → 写库。
 */
private fun doImportList(
    json: String,
    onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
): ListImportResult {
    val trimmed = json.trim()
    if (trimmed.isEmpty()) return ListImportResult.EmptyOrUnrecognized

    // 1. 粗校验（对原始内容、补括号之前）：数组缺 ']' 很可能被截断；
    //    单对象 '{"group":...}' 形式允许，下面包裹后按数组解析
    if (trimmed.startsWith("[")) {
        if (!trimmed.removeSuffix(",").endsWith("]"))
            return ListImportResult.Truncated("JSON 未以 ']' 结尾，内容可能被截断")
    } else if (!trimmed.startsWith("{")) {
        return ListImportResult.EmptyOrUnrecognized
    }

    // 2. 解析：单对象/遗留格式统一包裹成数组（对合法数组幂等），捕获 JSON 异常并区分截断
    val list = try {
        getImportList(trimmed.toJsonListString(), false)
    } catch (e: kotlinx.serialization.SerializationException) {
        return ListImportResult.Truncated(e.message ?: e.toString())
    } catch (e: Exception) {
        return ListImportResult.Truncated(e.message ?: e.toString())
    }
    if (list.isNullOrEmpty()) {
        // 原生格式识别不出 → 尝试按 JRead 配置包解析
        val parsed = JReadConfigMigration.parse(trimmed)
            ?: return ListImportResult.EmptyOrUnrecognized
        return insertJReadItems(parsed)
    }

    // 3. 写库：单事务批量插入，避免逐条 fsync；按批次更新进度反馈
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
    // 总项数（用于进度分母）
    val total = list.sumOf { it.list.size }
    var imported = 0
    dbm.runInTransaction {
        val groupsToInsert = mutableListOf<SystemTtsGroup>()
        val ttsToInsert = mutableListOf<SystemTtsV2>()
        for (groupWithTts in list) {
            val (group, ttsList) = groupWithTts
            val (newGroupId, newOrder) = oldToNewGroupId[group.id]!!
            groupsToInsert.add(group.copy(id = newGroupId, order = newOrder))
            for (tts in ttsList) {
                ttsToInsert.add(
                    tts.copy(
                        id = baseId + 100000 + ttsSeq,
                        groupId = newGroupId,
                        // jread 风格长名子分组 / 发音人标签导入时统一归一，无法映射则原样保留
                        categoryPath = JReadConfigMigration.normalizeCategoryPath(tts.categoryPath),
                        config = when (val c = tts.config) {
                            is TtsConfigurationDTO -> {
                                val newTag = JReadConfigMigration.normalizeTag(c.speechRule.tag)
                                if (newTag != c.speechRule.tag) {
                                    c.copy(speechRule = c.speechRule.copy(tag = newTag))
                                } else c
                            }
                            else -> tts.config
                        }
                    )
                )
                ttsSeq++
                imported++
                // 每 10 项回报一次进度，使进度条更跟手
                if (imported % 10 == 0) {
                    onProgress(imported, total)
                }
            }
        }
        dbm.systemTtsV2.insertGroup(*groupsToInsert.toTypedArray())
        dbm.systemTtsV2.insert(*ttsToInsert.toTypedArray())
    }
    onProgress(imported, total)
    return ListImportResult.Success(imported, groupCount = oldToNewGroupId.size)
}

/** JRead 配置写入：一级组名做 mumu 分组(同名复用)，二三级留在 categoryPath */
private fun insertJReadItems(parsed: JReadConfigMigration.Parsed): ListImportResult {
    val baseId = System.currentTimeMillis()
    // 绑定当前启用的朗读规则：tagRuleId 为空时编辑页会提示「该配置项未绑定朗读规则，无法切换标签」；
    // 仅绑定有标签的项，无标签项（群杂等未映射）保持未绑定不打扰
    val enabledRuleId = dbm.speechRuleDao.getAllEnabledWithoutCode().firstOrNull()?.ruleId ?: ""
    // 按 groupName 分桶，保持首次出现顺序（携带全局索引保证 ID 唯一）
    val buckets = linkedMapOf<String, MutableList<Pair<SystemTtsV2, Int>>>()
    parsed.items.forEachIndexed { i, item ->
        val name = parsed.groupNames[i].trim().ifBlank { StringUtils.formattedDate() }
        buckets.getOrPut(name) { mutableListOf() }.add(item to i)
    }
    dbm.runInTransaction {
        var groupOrder = dbm.systemTtsV2.groupCount
        val allGroups = dbm.systemTtsV2.allGroup
        for ((name, bucket) in buckets) {
            // 同名分组复用，避免重复导入产生多套同名组
            val existing = allGroups.firstOrNull { it.name == name }
            val groupId = existing?.id ?: run {
                val newId = baseId + buckets.keys.indexOf(name) * 1000000L
                dbm.systemTtsV2.insertGroup(
                    SystemTtsGroup(id = newId, name = name, order = groupOrder++)
                )
                newId
            }
            dbm.systemTtsV2.insert(
                *bucket.map { (item, i) ->
                    val bound = item.copy(id = baseId + 100000L + i, groupId = groupId)
                    val cfg = bound.config as? TtsConfigurationDTO
                    if (enabledRuleId.isNotBlank() && cfg != null && cfg.speechRule.tag.isNotBlank()) {
                        bound.copy(config = cfg.copy(speechRule = cfg.speechRule.copy(tagRuleId = enabledRuleId)))
                    } else bound
                }.toTypedArray()
            )
        }
    }
    return ListImportResult.Success(
        parsed.items.size, parsed.skipped,
        parsed.skippedNoPlugin, parsed.skippedUrlDirect,
        groupCount = buckets.size
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
