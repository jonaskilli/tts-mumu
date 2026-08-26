package com.github.jing332.tts_server_android.compose.systts.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandCircleDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.state.ToggleableState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drake.net.utils.withIO
import com.github.jing332.common.utils.StringUtils
import com.github.jing332.common.utils.longToast
import com.github.jing332.common.utils.toast
import com.github.jing332.compose.widgets.ControlBottomBarVisibility
import com.github.jing332.compose.widgets.DraggableVerticalScrollbar
import com.github.jing332.compose.widgets.LoadingDialog
import com.github.jing332.compose.widgets.LazyListIndexStateSaver
import com.github.jing332.compose.widgets.ShadowedDraggableItem
import com.github.jing332.compose.widgets.TextFieldDialog
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.AbstractListGroup.Companion.DEFAULT_GROUP_ID
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.BgmConfiguration
import com.github.jing332.database.entities.systts.GroupWithSystemTts
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.SystemTtsGroup
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.tts_server_android.AppLocale
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.AppDefaultProperties
import com.github.jing332.tts_server_android.conf.AppConfig
import com.github.jing332.tts_server_android.compose.LocalBottomBarBehavior
import com.github.jing332.tts_server_android.compose.LocalNavController
import com.github.jing332.tts_server_android.compose.SharedViewModel
import com.github.jing332.tts_server_android.compose.nav.NavRoutes
import com.github.jing332.tts_server_android.compose.nav.NavTopAppBar
import com.github.jing332.tts_server_android.compose.systts.AuditionDialog
import com.github.jing332.tts_server_android.compose.systts.ConfigDeleteDialog
import com.github.jing332.tts_server_android.compose.systts.list.ui.ItemDescriptorFactory
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.QuickEditBottomSheet
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.TagDataClearConfirmDialog
import com.github.jing332.tts_server_android.compose.systts.plugin.PluginSelectionDialog
import com.github.jing332.tts_server_android.compose.systts.list.SearchTextField
import com.github.jing332.tts_server_android.compose.systts.list.SearchType
import com.github.jing332.tts_server_android.compose.systts.list.SubGroupHeader
import com.github.jing332.tts_server_android.compose.systts.list.buildSubCategoryTree
import com.github.jing332.tts_server_android.compose.systts.list.flattenSubCategoryTree
import com.github.jing332.tts_server_android.compose.systts.list.FlattenedCategoryItem
import com.github.jing332.tts_server_android.compose.systts.sizeToToggleableState
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.tts_server_android.constant.SpeechTarget
import com.github.jing332.tts_server_android.model.rhino.speech_rule.SpeechRuleEngine
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import android.content.Intent
import com.github.jing332.tts_server_android.toCode
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.encodeToString
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import java.io.File


/**
 * 按分组名一键分配标签时使用的固定关键词（性别 + 年龄段 + 主角），
 * 用于从分组名匹配出标签前缀（取最长匹配）。
 */
private val GROUP_TAG_KEYWORDS = listOf(
    "女童", "少女", "女青年", "女中年", "女老年",
    "男童", "少年", "男青年", "男中年", "男老年",
    "男主", "女主"
)

/** 男主在朗读规则里不补零(男主1…男主20)；女主仍两位补零(女主01…)，与规则一致 */
private val NO_ZERO_PAD_PREFIXES = setOf("男主")

private data class DetectedKeyword(val prefix: String, val zeroPad: Boolean)

/**
 * 从分组名匹配固定关键词（取最长匹配）。
 * 返回前缀与是否补零：男主不补零，其余(含女主)两位补零，与朗读规则一致。
 * 无匹配时回退为原分组名作为前缀。
 */
private fun detectTagKeyword(name: String): DetectedKeyword? {
    val kw = GROUP_TAG_KEYWORDS.filter { name.contains(it) }
        .maxByOrNull { it.length } ?: return null
    return DetectedKeyword(kw, zeroPad = kw !in NO_ZERO_PAD_PREFIXES)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun ListManagerScreen(
    sharedVM: SharedViewModel,
    vm: ListManagerViewModel = viewModel(),
) {
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val models by vm.list.collectAsStateWithLifecycle()
    val searchKeyword by vm.keyword.collectAsStateWithLifecycle()
    val searchType by vm.searchType.collectAsStateWithLifecycle()
    val invalidCount by vm.invalidCount.collectAsStateWithLifecycle()
    val invalidSourceCounts by vm.invalidSourceCounts.collectAsStateWithLifecycle()
    val invalidSourceItems by vm.invalidSourceItems.collectAsStateWithLifecycle()
    val pluginNameCache by vm.pluginNameCache.collectAsStateWithLifecycle()

    var isSearchMode by rememberSaveable { mutableStateOf(false) }

    // 失效配置项修复相关状态
    var pendingPlugin by remember { mutableStateOf<Plugin?>(null) }
    // 当前正在修复的失效来源插件id；null=全部（单来源场景）
    var fixSourcePluginId by remember { mutableStateOf<String?>(null) }
    // 来源列表中点击某来源后，弹出目标插件选择器
    var pendingSourceForPicker by remember { mutableStateOf<String?>(null) }

    // 多选删除分组：选择模式与已选分组ID集合
    var selectionMode by remember { mutableStateOf(false) }
    var selectedGroupIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // 子分组展开状态：存储已展开的子分组完整路径（持久化，默认全部折叠）
    var expandedSubGroups by remember { AppConfig.expandedSubGroups }

    // 整理标签时的加载遮罩，避免主线程被 JS 引擎评估阻塞导致界面变灰卡住
    var showTagOrganizeLoading by remember { mutableStateOf(false) }
    if (showTagOrganizeLoading) {
        LoadingDialog(onDismissRequest = { showTagOrganizeLoading = false })
    }

    BackHandler(enabled = isSearchMode || selectionMode) {
        when {
            isSearchMode -> {
                isSearchMode = false
                vm.setSearchKeyword("")
            }
            selectionMode -> {
                selectionMode = false
                selectedGroupIds = emptySet()
            }
        }
    }

    var showSortDialog by remember { mutableStateOf<Pair<List<SystemTtsV2>, List<SystemTtsV2>?>?>(null) }
    if (showSortDialog != null) {
        val (list, groupList) = showSortDialog!!
        SortDialog(
            onDismissRequest = { showSortDialog = null },
            list = list,
            groupList = groupList
        )
    }

    // 子分组操作对话框状态
    var showSubGroupRename by remember { mutableStateOf<Pair<List<SystemTtsV2>, String>?>(null) }
    if (showSubGroupRename != null) {
        val (items, oldPath) = showSubGroupRename!!
        var newName by remember { mutableStateOf(oldPath.substringAfterLast('/')) }
        TextFieldDialog(
            title = "重命名子分组",
            text = newName,
            onTextChange = { newName = it },
            onDismissRequest = { showSubGroupRename = null }
        ) {
            // 立即关闭弹窗，避免操作期间“愣在那儿”
            showSubGroupRename = null
            val newPath = if (oldPath.contains('/')) {
                oldPath.substringBeforeLast('/') + "/" + newName
            } else newName
            scope.launch {
                withIO {
                    if (items.isNotEmpty()) {
                        dbm.systemTtsV2.update(*items.map { it.copy(categoryPath = newPath) }.toTypedArray())
                    }
                }
            }
        }
    }

    var showSubGroupBatchTag by remember { mutableStateOf<List<SystemTtsV2>?>(null) }
    if (showSubGroupBatchTag != null) {
        BatchTagDialog(
            groupItems = showSubGroupBatchTag!!,
            onDismissRequest = { showSubGroupBatchTag = null }
        )
    }

    // 子分组转为一级分组确认对话框
    var showSubGroupExtractToGroup by remember { mutableStateOf<Pair<SystemTtsGroup, String>?>(null) }
    if (showSubGroupExtractToGroup != null) {
        val (group, path) = showSubGroupExtractToGroup!!
        AlertDialog(
            onDismissRequest = { showSubGroupExtractToGroup = null },
            title = { Text("转为一级分组") },
            text = { Text("将子分组 \"${path.substringAfterLast('/')}\" 移出为独立的一级分组？") },
            confirmButton = {
                TextButton(onClick = {
                    // 立即关闭弹窗 + 显示加载遮罩，避免"愣在那儿"
                    showSubGroupExtractToGroup = null
                    showTagOrganizeLoading = true
                    scope.launch {
                        val currentGroupWithTts = models.find { it.group.id == group.id }
                        val itemsToMove = currentGroupWithTts?.list
                            ?.filter { it.categoryPath == path }
                            ?: emptyList()

                        val subGroupAudioParams = group.subGroupAudioParamsJson.let { jsonStr ->
                            if (jsonStr.isBlank() || jsonStr == "{}") emptyMap()
                            else SystemTtsV2.Converters.json.decodeFromString<Map<String, AudioParams>>(jsonStr)
                        }
                        val audioParamsForNewGroup = subGroupAudioParams[path] ?: AudioParams()

                        withIO {
                            if (subGroupAudioParams.containsKey(path)) {
                                val newSubMap = subGroupAudioParams.toMutableMap().apply { remove(path) }
                                val newSubJson = SystemTtsV2.Converters.json.encodeToString(newSubMap)
                                dbm.systemTtsV2.updateGroup(group.copy(subGroupAudioParamsJson = newSubJson))
                            }

                            val groupName = path.substringAfterLast('/')
                            val newGroup = SystemTtsGroup(
                                id = System.currentTimeMillis(),
                                name = groupName,
                                audioParams = audioParamsForNewGroup
                            )
                            dbm.systemTtsV2.insertGroup(newGroup)
                            // 批量更新配置项，替代逐条 forEach update
                            if (itemsToMove.isNotEmpty()) {
                                dbm.systemTtsV2.update(
                                    *itemsToMove.map {
                                        it.copy(groupId = newGroup.id, categoryPath = "")
                                    }.toTypedArray()
                                )
                            }
                        }
                        showTagOrganizeLoading = false
                    }
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubGroupExtractToGroup = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    var showQuickEdit by remember { mutableStateOf<SystemTtsV2?>(null) }
    if (showQuickEdit != null) {
        QuickEditBottomSheet(onDismissRequest = {
            val toSave = showQuickEdit
            if (toSave != null) {
                scope.launch {
                    withIO { dbm.systemTtsV2.insert(toSave) }
                    if (toSave.isEnabled) SystemTtsService.notifyUpdateConfig()
                }
            }
            showQuickEdit = null
        }, systts = showQuickEdit!!, onSysttsChange = {
            showQuickEdit = it
        })
    }

    // 辅助函数：仅扫描当前目录下的文件，忽略所有子文件夹
    fun scanOnlyFiles(dir: File, targetExts: List<String>): List<String> {
        val fileList = mutableListOf<String>()
        val items = dir.listFiles() ?: return emptyList()
        for (item in items) {
            if (!item.isDirectory) {
                val extWithDot = item.extension.let { if (it.isNotEmpty()) ".${it.lowercase()}" else "" }
                if (targetExts.contains(extWithDot)) {
                    fileList.add(item.name)
                }
            }
        }
        return fileList
    }

    // 核心扫描：仅扫描根目录 + 一级子文件夹，更深层级忽略
    fun scanGroupedFiles(dirPath: String, targetExts: List<String>): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>()
        val rootFiles = mutableListOf<String>()
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) return emptyMap()

        val allItems = dir.listFiles() ?: return emptyMap()
        for (item in allItems) {
            if (item.isDirectory) {
                // 遇到一级子文件夹：仅扫描内部文件，不继续递归深层
                val subFileList = scanOnlyFiles(item, targetExts)
                if (subFileList.isNotEmpty()) {
                    result[item.name] = subFileList
                }
            } else {
                // 根目录下的直接文件
                val extWithDot = item.extension.let { if (it.isNotEmpty()) ".${it.lowercase()}" else "" }
                if (targetExts.contains(extWithDot)) {
                    rootFiles.add(item.name)
                }
            }
        }
        if (rootFiles.isNotEmpty()) {
            result["根目录"] = rootFiles
        }
        return result
    }

    /**
     * 刷新克隆列表：扫描指定文件夹下的 MP3/WAV 音频文件，
     * 按目录分组生成 JSON 文件（仅扫描根目录 + 一级子文件夹，更深层级忽略）。
     */
    suspend fun refreshCloneAudioList(context: android.content.Context) {
        val basePath = "/sdcard/data/"
        val targetExts = listOf(".mp3", ".wav")
        val outputJson = basePath + "音频列表.json"

        val groupedResult = withContext(Dispatchers.IO) {
            scanGroupedFiles(basePath, targetExts)
        }

        if (groupedResult.isEmpty()) {
            context.toast("未找到 MP3/WAV 音频文件")
            return
        }

        var totalFiles = 0
        for (key in groupedResult.keys) {
            totalFiles += groupedResult[key]?.size ?: 0
        }

        try {
            withContext(Dispatchers.IO) {
                val jsonStr = AppConst.jsonBuilder.encodeToString(groupedResult)
                File(outputJson).writeText(jsonStr, Charsets.UTF_8)
            }
            context.longToast("扫描完成\n共${groupedResult.size}个目录，${totalFiles}个音频\n已保存：$outputJson")
        } catch (e: Exception) {
            context.toast("写入失败：${e.message}")
        }
    }

    fun navigateToEdit(systts: SystemTtsV2) {
        sharedVM.put(NavRoutes.TtsEdit.DATA, systts)
        navController.navigate(NavRoutes.TtsEdit.id)
    }

    /**
     * 先按 ruleId 取出规则，再委托 [computeTagName]（ruleId 为空时直接回退）。
     */
    suspend fun computeTagNameOrFallback(
        context: android.content.Context,
        ruleData: com.github.jing332.database.entities.systts.SpeechRuleInfo,
        fallback: String,
        ruleCache: MutableMap<String, SpeechRule?>? = null,
        engineCache: MutableMap<String, SpeechRuleEngine>? = null,
    ) {
        val ruleId = ruleData.tagRuleId
        if (ruleId.isBlank()) {
            ruleData.tagName = computeTagName(context, null, ruleData, fallback, engineCache)
            return
        }
        val speechRule = if (ruleCache != null) {
            ruleCache.getOrPut(ruleId) {
                withContext(Dispatchers.IO) {
                    runCatching { dbm.speechRuleDao.getByRuleId(ruleId) }.getOrDefault(null)
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                runCatching { dbm.speechRuleDao.getByRuleId(ruleId) }.getOrDefault(null)
            }
        }
        ruleData.tagName = computeTagName(context, speechRule, ruleData, fallback, engineCache)
    }

    /**
     * 标签重排：按 categoryPath 分组，同前缀的启用配置重排标签序号
     * @param fromZero true=从01开始连续编号，false=用原有序号集合重新分配
     */
    suspend fun resortTags(list: List<SystemTtsV2>, fromZero: Boolean) {
        val tagPattern = Regex("^(.+?)(\\d+)$")
        // narration 特殊标签：重排时第一项保留原 tag，后续用其 tagName(旁白)+序号
        val NARRATION_TAG = "narration"
        val toUpdate = mutableListOf<SystemTtsV2>()
        val ruleCache = mutableMapOf<String, SpeechRule?>()
        val engineCache = mutableMapOf<String, SpeechRuleEngine>()
        // 按 categoryPath 分组
        list.groupBy { it.categoryPath }.forEach { (path, items) ->
            val targetItems = items.filter { it.config is TtsConfigurationDTO }
                .sortedBy { it.order }
            if (targetItems.isEmpty()) return@forEach

            // 按标签前缀分组
            val byPrefix = targetItems.groupBy { item ->
                val tag = (item.config as TtsConfigurationDTO).speechRule.tag
                tagPattern.matchEntire(tag)?.groupValues?.getOrNull(1) ?: tag
            }
            byPrefix.forEach { (prefix, prefixItems) ->
                if (prefixItems.isEmpty()) return@forEach
                if (fromZero) {
                    // narration 特殊处理：第一项保留原 tag(narration) 并计算其 tagName
                    // （通常为"旁白"），后续项用该 tagName 作为前缀 + 序号（旁白01、旁白02...）
                    if (prefix == NARRATION_TAG && prefixItems.isNotEmpty()) {
                        // 第一项：tag 保持 narration 不变，仅计算 tagName（即"旁白"）
                        val first = prefixItems.first()
                        val firstConfig = first.config as TtsConfigurationDTO
                        val firstRule = firstConfig.speechRule.copy()
                        computeTagNameOrFallback(context, firstRule, NARRATION_TAG, ruleCache, engineCache)
                        toUpdate.add(first.copy(
                            config = firstConfig.copy(speechRule = firstRule)
                        ))
                        // 后续项：tag = 旁白01、旁白02...（用第一项计算出的 tagName 作为前缀）
                        val subPrefix = firstRule.tagName
                        prefixItems.drop(1).forEachIndexed { idx, item ->
                            val newTag = subPrefix + String.format("%02d", idx + 1)
                            val config = item.config as TtsConfigurationDTO
                            val newRule = config.speechRule.copy(tag = newTag)
                            computeTagNameOrFallback(context, newRule, newTag, ruleCache, engineCache)
                            toUpdate.add(item.copy(
                                config = config.copy(speechRule = newRule)
                            ))
                        }
                    } else {
                        // 其他标签：从01开始连续编号
                        prefixItems.forEachIndexed { idx, item ->
                            val newTag = prefix + String.format("%02d", idx + 1)
                            val config = item.config as TtsConfigurationDTO
                            val newRule = config.speechRule.copy(tag = newTag)
                            computeTagNameOrFallback(context, newRule, newTag, ruleCache, engineCache)
                            toUpdate.add(item.copy(
                                config = config.copy(speechRule = newRule)
                            ))
                        }
                    }
                } else {
                    // 用原有序号集合重新分配
                    val existingSeqs = prefixItems.mapNotNull { item ->
                        val tag = (item.config as TtsConfigurationDTO).speechRule.tag
                        tagPattern.matchEntire(tag)?.groupValues?.getOrNull(2)?.toIntOrNull()
                    }.sorted()
                    if (existingSeqs.size != prefixItems.size) return@forEach
                    prefixItems.forEachIndexed { idx, item ->
                        val newTag = prefix + String.format("%02d", existingSeqs[idx])
                        val config = item.config as TtsConfigurationDTO
                        val newRule = config.speechRule.copy(tag = newTag)
                        computeTagNameOrFallback(context, newRule, newTag, ruleCache, engineCache)
                        toUpdate.add(item.copy(
                            config = config.copy(speechRule = newRule)
                        ))
                    }
                }
            }
        }
        if (toUpdate.isNotEmpty()) {
            dbm.systemTtsV2.update(*toUpdate.toTypedArray())
            SystemTtsService.notifyUpdateConfig()
        }
    }

    /**
     * 重新分配标签：用指定前缀，按当前位置顺序从01开始连续编号。
     * [zeroPad]=false 时不补零（如男主1…男主N，匹配朗读规则）。
     */
    suspend fun reassignTagsWithPrefix(
        list: List<SystemTtsV2>,
        prefix: String,
        zeroPad: Boolean = true,
    ) {
        val toUpdate = mutableListOf<SystemTtsV2>()
        val ruleCache = mutableMapOf<String, SpeechRule?>()
        val engineCache = mutableMapOf<String, SpeechRuleEngine>()
        // 按 categoryPath 分组，每个独立处理
        list.groupBy { it.categoryPath }.forEach { (path, items) ->
            val targetItems = items.filter { it.config is TtsConfigurationDTO }
                .sortedBy { it.order }
            if (targetItems.isEmpty()) return@forEach

            targetItems.forEachIndexed { idx, item ->
                val seq = if (zeroPad) String.format("%02d", idx + 1) else (idx + 1).toString()
                val newTag = prefix + seq
                val config = item.config as TtsConfigurationDTO
                val newRule = config.speechRule.copy(tag = newTag)
                computeTagNameOrFallback(context, newRule, newTag, ruleCache, engineCache)
                toUpdate.add(item.copy(config = config.copy(speechRule = newRule)))
            }
        }
        if (toUpdate.isNotEmpty()) {
            dbm.systemTtsV2.update(*toUpdate.toTypedArray())
            SystemTtsService.notifyUpdateConfig()
        }
    }

    /**
     * 按分组名一键分配标签：从分组名匹配固定关键词(女童/少女/…/男主/女主)，
     * 用该关键词作为前缀编号。男主不补零，其余两位补零。
     * @return 实际整理的标签数量
     */
    suspend fun reassignTagsByGroupName(list: List<SystemTtsV2>, groupName: String): Int {
        val detected = detectTagKeyword(groupName) ?: return 0
        reassignTagsWithPrefix(list, detected.prefix, detected.zeroPad)
        return list.count { it.config is TtsConfigurationDTO }
    }

    /**
     * 整理某大分组下全部子分组：每个子分组按其名称匹配关键词后重新编号。
     * 仅处理名称含关键词的子分组（无关键词的子分组跳过，避免误改）。
     * 优化：收集所有子分组的更新后一次性批量写入数据库，提升整理速度。
     * 性能优化：按 ruleId 分组并行评估 JS（不同 ruleId 各自独立 engine，
     * 可并行；同 ruleId 复用同一 engine 串行调用，Rhino 非线程安全）。
     */
    suspend fun reassignTagsForAllSubGroups(list: List<SystemTtsV2>) {
        val tree = buildSubCategoryTree(list)
        val flattened = flattenSubCategoryTree(tree)
        // 先收集所有待处理项 (item, newRule, fallbackTag)
        data class PendingUpdate(val item: SystemTtsV2, val newRule: com.github.jing332.database.entities.systts.SpeechRuleInfo, val fallback: String)
        val pending = mutableListOf<PendingUpdate>()
        flattened.filterIsInstance<FlattenedCategoryItem.SubGroupHeader>().forEach { header ->
            val detected = detectTagKeyword(header.node.name) ?: return@forEach
            val subItems = header.node.allItems.filter { it.config is TtsConfigurationDTO }
                .sortedBy { it.order }
            if (subItems.isEmpty()) return@forEach
            subItems.forEachIndexed { idx, item ->
                val seq = if (detected.zeroPad) String.format("%02d", idx + 1) else (idx + 1).toString()
                val newTag = detected.prefix + seq
                val config = item.config as TtsConfigurationDTO
                val newRule = config.speechRule.copy(tag = newTag)
                pending.add(PendingUpdate(item, newRule, newTag))
            }
        }
        if (pending.isEmpty()) return

        // ruleId 为空的项无需 JS 评估，直接赋 fallback；其余按 ruleId 分组并行
        val ruleCache = ConcurrentHashMap<String, SpeechRule?>()
        val (noRule, withRule) = pending.partition { it.newRule.tagRuleId.isBlank() }
        val allUpdates = mutableListOf<SystemTtsV2>()
        // 无规则项：tagName 直接回退 fallback，无 JS 可任意并行快速处理
        noRule.forEach { u ->
            u.newRule.tagName = u.fallback
            val config = u.item.config as TtsConfigurationDTO
            allUpdates.add(u.item.copy(config = config.copy(speechRule = u.newRule)))
        }
        // 有规则项：按 ruleId 分组，组间并行（各自独立 engine），组内串行（复用 engine）
        coroutineScope {
            withRule.groupBy { it.newRule.tagRuleId }.values.map { group ->
                async(Dispatchers.IO) {
                    // 每个协程独享一个 engineCache，避免跨协程共享 Rhino engine
                    val localEngineCache = mutableMapOf<String, SpeechRuleEngine>()
                    group.forEach { u ->
                        computeTagNameOrFallback(context, u.newRule, u.fallback, ruleCache, localEngineCache)
                    }
                    group
                }
            }.awaitAll().flatten().forEach { u ->
                val config = u.item.config as TtsConfigurationDTO
                allUpdates.add(u.item.copy(config = config.copy(speechRule = u.newRule)))
            }
        }
        if (allUpdates.isNotEmpty()) {
            dbm.systemTtsV2.update(*allUpdates.toTypedArray())
            SystemTtsService.notifyUpdateConfig()
        }
    }

    /**
     * 第3项: 把源一级分组中选中的若干子分组(含其配置项与音频参数)移动到目标一级分组。
     * - 配置项 groupId 改为目标分组, categoryPath 保留原路径(若目标已有同名子分组则合并)
     * - 子分组音频参数从源分组 subGroupAudioParamsJson 迁移到目标分组
     * - 顺序追加到目标分组对应子分组末尾
     */
    suspend fun moveSubGroupsToGroup(
        sourceGroup: SystemTtsGroup,
        paths: Set<String>,
        targetGroup: SystemTtsGroup,
    ) {
        if (sourceGroup.id == targetGroup.id) return
        val sourceGwt = models.find { it.group.id == sourceGroup.id } ?: return
        // 待移动的配置项
        val itemsToMove = sourceGwt.list.filter { it.categoryPath in paths }
        if (itemsToMove.isEmpty()) return

        // 1. 迁移子分组音频参数: 源 -> 目标
        val srcSubMap = sourceGroup.subGroupAudioParamsJson.let { jsonStr ->
            if (jsonStr.isBlank() || jsonStr == "{}") emptyMap()
            else SystemTtsV2.Converters.json.decodeFromString<Map<String, AudioParams>>(jsonStr)
        }
        val dstSubMap = targetGroup.subGroupAudioParamsJson.let { jsonStr ->
            if (jsonStr.isBlank() || jsonStr == "{}") emptyMap()
            else SystemTtsV2.Converters.json.decodeFromString<Map<String, AudioParams>>(jsonStr)
        }.toMutableMap()
        paths.forEach { p -> srcSubMap[p]?.let { dstSubMap[p] = it } }

        // 2. 更新目标分组音频参数
        dbm.systemTtsV2.updateGroup(
            targetGroup.copy(subGroupAudioParamsJson = SystemTtsV2.Converters.json.encodeToString(dstSubMap))
        )
        // 3. 从源分组移除已迁移的子分组参数
        val newSrcMap = srcSubMap.toMutableMap().apply { paths.forEach { remove(it) } }
        dbm.systemTtsV2.updateGroup(
            sourceGroup.copy(subGroupAudioParamsJson = SystemTtsV2.Converters.json.encodeToString(newSrcMap))
        )

        // 4. 移动配置项: 按目标分组内各子分组现有最大 order 追加
        val targetGwt = dbm.systemTtsV2.getAllGroupWithTts().find { it.group.id == targetGroup.id }
        val updates = mutableListOf<SystemTtsV2>()
        paths.forEach { path ->
            val pathItems = itemsToMove.filter { it.categoryPath == path }.sortedBy { it.order }
            val maxOrder = targetGwt?.list?.filter { it.categoryPath == path }?.maxOfOrNull { it.order } ?: -1
            pathItems.forEachIndexed { idx, item ->
                updates.add(item.copy(groupId = targetGroup.id, order = maxOrder + 1 + idx))
            }
        }
        dbm.systemTtsV2.update(*updates.toTypedArray())
        SystemTtsService.notifyUpdateConfig()
    }

    /**
     * 第4项: 把源一级分组中选中的子分组各自转为独立的一级分组。
     * - 每个子分组新建一级分组(名称为子分组名), 配置项移入且 categoryPath 清空
     * - 子分组音频参数从源分组 subGroupAudioParamsJson 迁出到新分组 audioParams
     */
    suspend fun convertSubGroupsToTopLevel(
        sourceGroup: SystemTtsGroup,
        items: List<SystemTtsV2>,
        paths: Set<String>,
    ) {
        if (paths.isEmpty()) return
        val subMap = sourceGroup.subGroupAudioParamsJson.let { jsonStr ->
            if (jsonStr.isBlank() || jsonStr == "{}") emptyMap()
            else SystemTtsV2.Converters.json.decodeFromString<Map<String, AudioParams>>(jsonStr)
        }.toMutableMap()
        val updates = mutableListOf<SystemTtsV2>()
        paths.forEach { path ->
            val audioParamsForNewGroup = subMap.remove(path) ?: AudioParams()
            val groupName = path.substringAfterLast('/')
            val newGroup = SystemTtsGroup(
                id = System.currentTimeMillis(),
                name = groupName,
                audioParams = audioParamsForNewGroup
            )
            dbm.systemTtsV2.insertGroup(newGroup)
            items.filter { it.categoryPath == path }.forEach { item ->
                updates.add(item.copy(groupId = newGroup.id, categoryPath = ""))
            }
        }
        if (updates.isNotEmpty()) {
            dbm.systemTtsV2.update(*updates.toTypedArray())
        }
        // 从源分组移除已转出的子分组参数（仅当确有变更时写入，避免无谓更新）
        val newSubJson = SystemTtsV2.Converters.json.encodeToString(subMap)
        if (newSubJson != sourceGroup.subGroupAudioParamsJson) {
            dbm.systemTtsV2.updateGroup(sourceGroup.copy(subGroupAudioParamsJson = newSubJson))
        }
        SystemTtsService.notifyUpdateConfig()
    }

    var hasShownTip by rememberSaveable { mutableStateOf(false) }

    var showCreateSubGroup by remember { mutableStateOf<Long?>(null) }
    if (showCreateSubGroup != null) {
        val targetGroupWithTts = models.find { it.group.id == showCreateSubGroup }
        if (targetGroupWithTts != null) {
            val targetGroup = targetGroupWithTts.group
            val ungrouped = remember(targetGroup.id) {
                targetGroupWithTts.list.filter { it.categoryPath.isBlank() }
            }
            CreateSubGroupDialog(
                groupName = targetGroup.name,
                ungroupedItems = ungrouped,
                onDismissRequest = { showCreateSubGroup = null },
                onConfirm = { subGroupName, selectedItems ->
                    // 立即关闭弹窗，避免操作期间“愣在那儿”
                    showCreateSubGroup = null
                    scope.launch {
                        withIO {
                            if (selectedItems.isNotEmpty()) {
                                dbm.systemTtsV2.update(
                                    *selectedItems.map { it.copy(categoryPath = subGroupName) }.toTypedArray()
                                )
                            }
                        }
                    }
                }
            )
        }
    }

    var showMoveToSubGroup by remember { mutableStateOf<SystemTtsV2?>(null) }
    if (showMoveToSubGroup != null) {
        val targetItem = showMoveToSubGroup!!
        // 第6项: 列表“移动”改用统一分组树选择器，支持组内切子分组 / 组外跨大组切换 / 新建子分组
        GroupTreePickerDialog(
            currentGroupId = targetItem.groupId,
            currentCategoryPath = targetItem.categoryPath,
            onDismissRequest = { showMoveToSubGroup = null },
            onConfirm = { gid, path ->
                // 立即关闭弹窗，避免操作期间“愣在那儿”
                showMoveToSubGroup = null
                scope.launch {
                    withIO {
                        dbm.systemTtsV2.update(targetItem.copy(groupId = gid, categoryPath = path))
                    }
                }
            }
        )
    }

    var showBatchTagDialog by remember { mutableStateOf<List<SystemTtsV2>?>(null) }
    if (showBatchTagDialog != null) {
        BatchTagDialog(
            groupItems = showBatchTagDialog!!,
            onDismissRequest = { showBatchTagDialog = null }
        )
    }


    var showConvertToSubGroup by remember { mutableStateOf<SystemTtsGroup?>(null) }
    if (showConvertToSubGroup != null) {
        val targetGroup = showConvertToSubGroup!!
        val currentGroupWithTts = models.find { it.group.id == targetGroup.id }
        val hasSubGroups = currentGroupWithTts?.list?.any { it.categoryPath.isNotBlank() } == true
        val otherGroups = remember(models, targetGroup.id) {
            models.filter { it.group.id != targetGroup.id }.map { it.group }
        }

        if (hasSubGroups) {
            AlertDialog(
                onDismissRequest = { showConvertToSubGroup = null },
                title = { Text("无法转换") },
                text = { Text("当前分组包含子分组，无法转换为子分组。请先释放所有子分组。") },
                confirmButton = {
                    TextButton(onClick = { showConvertToSubGroup = null }) {
                        Text("确定")
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { showConvertToSubGroup = null },
                title = { Text("转为子分组") },
                text = {
                    Column {
                        Text("选择目标分组，当前分组将作为其子分组：", modifier = Modifier.padding(bottom = 8.dp))
                        otherGroups.forEach { otherGroup ->
                            TextButton(
                                onClick = {
                                    // 立即关闭弹窗 + 显示加载遮罩
                                    showConvertToSubGroup = null
                                    showTagOrganizeLoading = true
                                    scope.launch {
                                        withIO {
                                            // 将原大分组的音频参数作为子分组参数保存到目标分组
                                            val subMap = otherGroup.subGroupAudioParamsJson.let { jsonStr ->
                                                if (jsonStr.isBlank() || jsonStr == "{}") emptyMap()
                                                else SystemTtsV2.Converters.json.decodeFromString<Map<String, com.github.jing332.database.entities.systts.AudioParams>>(jsonStr)
                                            }.toMutableMap()
                                            subMap[targetGroup.name] = targetGroup.audioParams
                                            val newJson = SystemTtsV2.Converters.json.encodeToString(subMap)
                                            dbm.systemTtsV2.updateGroup(otherGroup.copy(subGroupAudioParamsJson = newJson))

                                            val moveItems = currentGroupWithTts?.list
                                            if (!moveItems.isNullOrEmpty()) {
                                                dbm.systemTtsV2.update(
                                                    *moveItems.map {
                                                        it.copy(groupId = otherGroup.id, categoryPath = targetGroup.name)
                                                    }.toTypedArray()
                                                )
                                            }
                                            // 空分组也保留，不删除
                                            if (currentGroupWithTts?.list?.isNotEmpty() == true) {
                                                dbm.systemTtsV2.deleteGroup(targetGroup)
                                            }
                                        }
                                        showTagOrganizeLoading = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(otherGroup.name)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showConvertToSubGroup = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }

    // 合并到其他分组：将源分组中与目标分组 categoryPath 匹配的配置项移入目标分组
    var showMergeGroup by remember { mutableStateOf<SystemTtsGroup?>(null) }
    if (showMergeGroup != null) {
        val sourceGroup = showMergeGroup!!
        val sourceGwt = models.find { it.group.id == sourceGroup.id }
        val otherGroups = remember(models, sourceGroup.id) {
            models.filter { it.group.id != sourceGroup.id }.map { it.group }
        }
        AlertDialog(
            onDismissRequest = { showMergeGroup = null },
            title = { Text("合并到其他分组") },
            text = {
                Column {
                    Text("选择目标分组，相同分类的配置项将归入：", modifier = Modifier.padding(bottom = 8.dp))
                    otherGroups.forEach { targetGroup ->
                        TextButton(
                            onClick = {
                                showMergeGroup = null
                                showTagOrganizeLoading = true
                                scope.launch {
                                    withIO {
                                        val sourceItems = sourceGwt?.list ?: emptyList()
                                        val targetGwt = models.find { it.group.id == targetGroup.id }
                                        val targetCategories = targetGwt?.list
                                            ?.map { it.categoryPath }?.toSet() ?: emptySet()
                                        // 匹配：源项的 categoryPath 在目标分组中已存在则移动
                                        val toMove = sourceItems.filter { it.categoryPath in targetCategories }
                                        if (toMove.isNotEmpty()) {
                                            val baseOrder = (targetGwt?.list?.maxOfOrNull { it.order } ?: -1) + 1
                                            dbm.systemTtsV2.update(
                                                *toMove.mapIndexed { i, item ->
                                                    item.copy(
                                                        groupId = targetGroup.id,
                                                        order = baseOrder + i
                                                    )
                                                }.toTypedArray()
                                            )
                                        }
                                        val remaining = sourceItems.size - toMove.size
                                        if (remaining == 0) {
                                            dbm.systemTtsV2.deleteGroup(sourceGroup)
                                        }
                                        withContext(Dispatchers.Main) {
                                            showTagOrganizeLoading = false
                                            val msg = if (remaining == 0)
                                                "已合并 ${toMove.size} 项，源分组已删除"
                                            else
                                                "已合并 ${toMove.size} 项，${remaining} 项无匹配分类保留原分组"
                                            context.toast(msg)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(targetGroup.name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMergeGroup = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 多选转为子分组：批量把选中的（不含子分组的）分组降级为目标分组的子分组
    var showConvertToSubGroupMulti by remember { mutableStateOf(false) }
    // 弹窗内选中的源分组（独立于列表预选的 selectedGroupIds，打开弹窗时初始化）
    var convertSourcesSelected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showNewGroupForConvertMulti by remember { mutableStateOf(false) }
    var newGroupNameForConvertMulti by remember { mutableStateOf("") }
    // 执行转换：把 srcs 中的分组作为 targetGroup 的子分组
    suspend fun performConvertToSubGroup(
        targetGroup: SystemTtsGroup,
        srcs: List<GroupWithSystemTts>,
    ) {
        withIO {
            val dstSubMap = targetGroup.subGroupAudioParamsJson.let { jsonStr ->
                if (jsonStr.isBlank() || jsonStr == "{}") emptyMap()
                else SystemTtsV2.Converters.json.decodeFromString<Map<String, AudioParams>>(jsonStr)
            }.toMutableMap()
            srcs.forEach { src -> dstSubMap[src.group.name] = src.group.audioParams }
            dbm.systemTtsV2.updateGroup(
                targetGroup.copy(subGroupAudioParamsJson = SystemTtsV2.Converters.json.encodeToString(dstSubMap))
            )
            val allMoveItems = srcs.flatMap { src ->
                src.list.map { it.copy(groupId = targetGroup.id, categoryPath = src.group.name) }
            }
            if (allMoveItems.isNotEmpty()) {
                dbm.systemTtsV2.update(*allMoveItems.toTypedArray())
            }
            srcs.forEach { src -> dbm.systemTtsV2.deleteGroup(src.group) }
        }
    }
    if (showConvertToSubGroupMulti) {
        // 已选的分组中不含子分组的可作为源（多选进入时已预选，无需再选）
        val convertibleSources = models.filter {
            it.group.id in convertSourcesSelected && it.list.none { it.categoryPath.isNotBlank() }
        }
        val targetGroups = models.filter { it.group.id !in convertSourcesSelected }.map { it.group }
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        Dialog(
            onDismissRequest = { showConvertToSubGroupMulti = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("转为子分组", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .heightIn(max = screenHeight * 0.7f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (convertibleSources.isEmpty()) {
                            Text("没有可转换的分组（所选分组均含子分组）。")
                        } else {
                            Text("将以下分组转为子分组：", modifier = Modifier.padding(bottom = 8.dp))
                            convertibleSources.forEach { gwt ->
                                Text("  • ${gwt.group.name}", style = MaterialTheme.typography.bodyMedium)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text("选择目标一级分组：", modifier = Modifier.padding(bottom = 8.dp))
                            targetGroups.forEach { target ->
                                TextButton(
                                    onClick = {
                                        showConvertToSubGroupMulti = false
                                        showTagOrganizeLoading = true
                                        scope.launch {
                                            performConvertToSubGroup(target, convertibleSources)
                                            showTagOrganizeLoading = false
                                            selectionMode = false
                                            selectedGroupIds = emptySet()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(target.name) }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            TextButton(
                                onClick = {
                                    newGroupNameForConvertMulti = ""
                                    showNewGroupForConvertMulti = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("新建一级分组") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showConvertToSubGroupMulti = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }
        }
    }

    // 多选转为子分组：新建一级分组作为目标
    if (showNewGroupForConvertMulti) {
        TextFieldDialog(
            title = "新建一级分组并移动",
            text = newGroupNameForConvertMulti,
            onTextChange = { newGroupNameForConvertMulti = it },
            onDismissRequest = { showNewGroupForConvertMulti = false }
        ) {
            val convertibleGroups = models
                .filter { it.group.id in convertSourcesSelected }
                .filter { it.list.none { it.categoryPath.isNotBlank() } }
            scope.launch {
                showNewGroupForConvertMulti = false
                showConvertToSubGroupMulti = false
                showTagOrganizeLoading = true
                val newGroup = SystemTtsGroup(id = System.currentTimeMillis(), name = newGroupNameForConvertMulti)
                withIO { dbm.systemTtsV2.insertGroup(newGroup) }
                performConvertToSubGroup(newGroup, convertibleGroups)
                showTagOrganizeLoading = false
                selectionMode = false
                selectedGroupIds = emptySet()
            }
        }
    }

    var showExtractSubGroup by remember { mutableStateOf<SystemTtsGroup?>(null) }

    // 长按菜单：移动启用配置到其他分组
    var showMoveEnabledDialog by remember { mutableStateOf<GroupWithSystemTts?>(null) }

    // 第3项: 移动子分组到其他一级分组 (一级分组菜单"移动子分组" / 子分组菜单"移动到其他一级分组" 共用)
    // Pair<源一级分组, 预选子分组路径(null=不预选)>
    var showMoveSubGroupsDialog by remember { mutableStateOf<Pair<SystemTtsGroup, String?>?>(null) }
    var showMoveSingleSubGroupDialog by remember { mutableStateOf<Pair<SystemTtsGroup, String>?>(null) }

    // 第4项: 含子分组的一级分组菜单"转为一级分组"——多选子分组, 各自转为独立一级分组
    var showConvertSubGroupsToTopLevel by remember { mutableStateOf<SystemTtsGroup?>(null) }

    // 长按菜单：重新分配标签（输入前缀，从01开始）
    var showReassignTagDialog by remember { mutableStateOf<GroupWithSystemTts?>(null) }
    var reassignTagPrefix by remember { mutableStateOf("") }
    if (showReassignTagDialog != null) {
        TextFieldDialog(
            title = "重新分配标签",
            text = reassignTagPrefix,
            onTextChange = { reassignTagPrefix = it },
            onDismissRequest = { showReassignTagDialog = null }
        ) {
            val sourceGwt = showReassignTagDialog!!
            val prefix = reassignTagPrefix.trim()
            if (prefix.isNotEmpty()) {
                showReassignTagDialog = null
                showTagOrganizeLoading = true
                scope.launch {
                    withContext(Dispatchers.IO) {
                        reassignTagsWithPrefix(sourceGwt.list, prefix)
                    }
                    showTagOrganizeLoading = false
                }
            }
        }
    }
    if (showExtractSubGroup != null) {
        val targetGroup = showExtractSubGroup!!
        val currentGroupWithTts = models.find { it.group.id == targetGroup.id }
        val subPaths = remember(currentGroupWithTts) {
            currentGroupWithTts?.list
                ?.map { it.categoryPath }
                ?.filter { it.isNotBlank() }
                ?.distinct()
                ?.sorted()
                ?: emptyList()
        }
        AlertDialog(
            onDismissRequest = { showExtractSubGroup = null },
            title = { Text("移动子分组") },
            text = {
                Column {
                    if (subPaths.isEmpty()) {
                        Text("当前分组没有子分组")
                    } else {
                        Text("移动子分组到其他一级分组，注意区域可上下滑动", modifier = Modifier.padding(bottom = 8.dp))
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            subPaths.forEach { path ->
                                TextButton(
                                    onClick = {
                                        // 立即关闭弹窗 + 显示加载遮罩
                                        showExtractSubGroup = null
                                        showTagOrganizeLoading = true
                                        scope.launch {
                                            val itemsToMove = currentGroupWithTts?.list
                                                ?.filter { it.categoryPath == path }
                                                ?: emptyList()

                                            // 读取子分组音频参数
                                            val subGroupAudioParams = targetGroup.subGroupAudioParamsJson.let { jsonStr ->
                                                if (jsonStr.isBlank() || jsonStr == "{}") emptyMap()
                                                else SystemTtsV2.Converters.json.decodeFromString<Map<String, com.github.jing332.database.entities.systts.AudioParams>>(jsonStr)
                                            }
                                            val audioParamsForNewGroup = subGroupAudioParams[path] ?: com.github.jing332.database.entities.systts.AudioParams()

                                            withIO {
                                                // 从原分组中移除该子分组参数记录
                                                if (subGroupAudioParams.containsKey(path)) {
                                                    val newSubMap = subGroupAudioParams.toMutableMap().apply { remove(path) }
                                                    val newSubJson = SystemTtsV2.Converters.json.encodeToString(newSubMap)
                                                    dbm.systemTtsV2.updateGroup(targetGroup.copy(subGroupAudioParamsJson = newSubJson))
                                                }

                                                val groupName = path.substringAfterLast('/')
                                                val newGroup = SystemTtsGroup(
                                                    id = System.currentTimeMillis(),
                                                    name = groupName,
                                                    audioParams = audioParamsForNewGroup
                                                )
                                                dbm.systemTtsV2.insertGroup(newGroup)
                                                if (itemsToMove.isNotEmpty()) {
                                                    dbm.systemTtsV2.update(
                                                        *itemsToMove.map {
                                                            it.copy(groupId = newGroup.id, categoryPath = "")
                                                        }.toTypedArray()
                                                    )
                                                }
                                            }
                                            showTagOrganizeLoading = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(path)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExtractSubGroup = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 移动启用配置到其他分组对话框
    var showNewGroupForMove by remember { mutableStateOf(false) }
    var newGroupNameForMove by remember { mutableStateOf("") }
    if (showNewGroupForMove) {
        TextFieldDialog(
            title = "新建一级分组并移动",
            text = newGroupNameForMove,
            onTextChange = { newGroupNameForMove = it },
            onDismissRequest = { showNewGroupForMove = false }
        ) {
            val sourceGwt = showMoveEnabledDialog
            if (sourceGwt != null) {
                val enabledItems = sourceGwt.list.filter { it.isEnabled }
                scope.launch {
                    showNewGroupForMove = false
                    showMoveEnabledDialog = null
                    showTagOrganizeLoading = true
                    withIO {
                        val newGroup = SystemTtsGroup(id = System.currentTimeMillis(), name = newGroupNameForMove)
                        dbm.systemTtsV2.insertGroup(newGroup)
                        if (enabledItems.isNotEmpty()) {
                            dbm.systemTtsV2.update(
                                *enabledItems.mapIndexed { idx, item ->
                                    item.copy(groupId = newGroup.id, categoryPath = "", order = idx)
                                }.toTypedArray()
                            )
                        }
                    }
                    showTagOrganizeLoading = false
                    SystemTtsService.notifyUpdateConfig()
                }
            } else if (showMoveSingleSubGroupDialog != null) {
                val (sourceGroup, subPath) = showMoveSingleSubGroupDialog!!
                scope.launch {
                    showNewGroupForMove = false
                    showMoveSingleSubGroupDialog = null
                    showTagOrganizeLoading = true
                    withIO {
                        val newGroup = SystemTtsGroup(id = System.currentTimeMillis(), name = newGroupNameForMove)
                        dbm.systemTtsV2.insertGroup(newGroup)
                        moveSubGroupsToGroup(sourceGroup, setOf(subPath), newGroup)
                    }
                    showTagOrganizeLoading = false
                }
            }
        }
    }

    if (showMoveEnabledDialog != null && !showNewGroupForMove) {
        val sourceGwt = showMoveEnabledDialog!!
        val sourceGroup = sourceGwt.group
        val enabledItems = sourceGwt.list.filter { it.isEnabled }
        // 直接复用内存列表 models，避免主线程重复查库
        val otherGroups = remember(sourceGroup.id) {
            models.filter { it.group.id != sourceGroup.id }
        }
        // 展开的大分组ID集合
        var expandedMoveGroups by remember { mutableStateOf<Set<Long>>(emptySet()) }

        AlertDialog(
            onDismissRequest = { showMoveEnabledDialog = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.92f),
            title = { Text("移动启用配置 (${enabledItems.size}个)") },
            text = {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())) {
                    Text("移动到：", modifier = Modifier.padding(bottom = 8.dp))
                    otherGroups.forEach { gwt ->
                        val grp = gwt.group
                        val subPaths = gwt.list.map { it.categoryPath }
                            .filter { it.isNotBlank() }.distinct().sorted()
                        val isExpanded = grp.id in expandedMoveGroups
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (subPaths.isNotEmpty()) {
                                IconButton(onClick = {
                                    expandedMoveGroups = if (isExpanded)
                                        expandedMoveGroups - grp.id
                                    else expandedMoveGroups + grp.id
                                }) {
                                    Icon(
                                        Icons.Default.ExpandCircleDown,
                                        contentDescription = null,
                                        modifier = Modifier.rotate(if (isExpanded) 0f else -45f)
                                    )
                                }
                            } else {
                                Spacer(Modifier.size(48.dp))
                            }
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        showMoveEnabledDialog = null
                                        showTagOrganizeLoading = true
                                        val maxOrder = gwt.list.maxOfOrNull { it.order } ?: -1
                                        withIO {
                                            if (enabledItems.isNotEmpty()) {
                                                dbm.systemTtsV2.update(
                                                    *enabledItems.mapIndexed { idx, item ->
                                                        item.copy(
                                                            groupId = grp.id,
                                                            categoryPath = "",
                                                            order = maxOrder + 1 + idx
                                                        )
                                                    }.toTypedArray()
                                                )
                                            }
                                        }
                                        showTagOrganizeLoading = false
                                        SystemTtsService.notifyUpdateConfig()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text(grp.name) }
                        }

                        if (isExpanded) {
                            subPaths.forEach { path ->
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            showMoveEnabledDialog = null
                                            showTagOrganizeLoading = true
                                            val subItems = gwt.list.filter { it.categoryPath == path }
                                            val maxOrder = subItems.maxOfOrNull { it.order } ?: -1
                                            withIO {
                                                if (enabledItems.isNotEmpty()) {
                                                    dbm.systemTtsV2.update(
                                                        *enabledItems.mapIndexed { idx, item ->
                                                            item.copy(
                                                                groupId = grp.id,
                                                                categoryPath = path,
                                                                order = maxOrder + 1 + idx
                                                            )
                                                        }.toTypedArray()
                                                    )
                                                }
                                            }
                                            showTagOrganizeLoading = false
                                            SystemTtsService.notifyUpdateConfig()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 56.dp)
                                ) { Text(path) }
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    TextButton(
                        onClick = {
                            newGroupNameForMove = ""
                            showNewGroupForMove = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("新建一级分组") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMoveEnabledDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 第3项: 移动子分组对话框 —— 多选子分组(支持全选) + 选择目标一级分组
    if (showMoveSubGroupsDialog != null) {
        val sourceGroup = showMoveSubGroupsDialog!!.first
        val preSelectPath = showMoveSubGroupsDialog!!.second
        val sourceGwt = models.find { it.group.id == sourceGroup.id }
        val subPaths = remember(sourceGwt) {
            sourceGwt?.list?.map { it.categoryPath }?.filter { it.isNotBlank() }?.distinct()?.sorted()
                ?: emptyList()
        }
        // 选中子分组路径集合
        var selectedPaths by remember(sourceGroup.id) {
            mutableStateOf<Set<String>>(preSelectPath?.let { setOf(it) } ?: emptySet())
        }
        val allSelected = subPaths.isNotEmpty() && selectedPaths.containsAll(subPaths)
        val otherGroups = remember(sourceGroup.id) {
            models.filter { it.group.id != sourceGroup.id }.map { it.group }
        }

        AlertDialog(
            onDismissRequest = { showMoveSubGroupsDialog = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.92f),
            title = { Text("移动子分组 (${selectedPaths.size}/${subPaths.size})") },
            text = {
                if (subPaths.isEmpty()) {
                    Text("当前分组没有子分组")
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 说明文字（可滚动，避免内容多时被截断）
                        Text(
                            text = "移动子分组到其他一级分组，注意区域可上下滑动",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .heightIn(max = 60.dp)
                                .verticalScroll(rememberScrollState())
                        )
                        // 上区：子分组多选（固定高度，内部滚动，避免挤压目标分组区域）
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // 全选 / 取消全选
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPaths = if (allSelected) emptySet() else subPaths.toSet()
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TriStateCheckbox(
                                    state = if (allSelected) ToggleableState.On
                                    else if (selectedPaths.isEmpty()) ToggleableState.Off
                                    else ToggleableState.Indeterminate,
                                    onClick = {
                                        selectedPaths = if (allSelected) emptySet() else subPaths.toSet()
                                    }
                                )
                                Text("全选", modifier = Modifier.padding(start = 8.dp))
                            }
                            HorizontalDivider()

                            subPaths.forEach { path ->
                                val checked = path in selectedPaths
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedPaths = if (checked) selectedPaths - path
                                            else selectedPaths + path
                                        }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = {
                                            selectedPaths = if (it) selectedPaths + path
                                            else selectedPaths - path
                                        }
                                    )
                                    Text(path, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }

                        // 下区：目标一级分组（始终可见，不会被上区挤出视口）
                        if (selectedPaths.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("选择目标一级分组：", modifier = Modifier.padding(bottom = 4.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                otherGroups.forEach { targetGroup ->
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                showMoveSubGroupsDialog = null
                                                showTagOrganizeLoading = true
                                                withIO {
                                                    moveSubGroupsToGroup(
                                                        sourceGroup = sourceGroup,
                                                        paths = selectedPaths,
                                                        targetGroup = targetGroup
                                                    )
                                                }
                                                showTagOrganizeLoading = false
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(targetGroup.name) }
                                }
                                TextButton(
                                    onClick = {
                                        newGroupNameForMove = ""
                                        showNewGroupForMove = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("新建一级分组") }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMoveSubGroupsDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 子分组"移动到其他一级分组" —— 直接选择目标分组, 无多选
    if (showMoveSingleSubGroupDialog != null) {
        val sourceGroup = showMoveSingleSubGroupDialog!!.first
        val subPath = showMoveSingleSubGroupDialog!!.second
        val otherGroups = models.filter { it.group.id != sourceGroup.id }.map { it.group }
        AlertDialog(
            onDismissRequest = { showMoveSingleSubGroupDialog = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.92f),
            title = { Text("移动子分组「$subPath」到其他一级分组") },
            text = {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())) {
                    // 说明文字（可滚动）
                    Text(
                        text = "将子分组「$subPath」及其下所有配置项整体转移到下方选择的目标一级分组下。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .heightIn(max = 60.dp)
                            .verticalScroll(rememberScrollState())
                    )
                    otherGroups.forEach { targetGroup ->
                        TextButton(
                            onClick = {
                                scope.launch {
                                    showMoveSingleSubGroupDialog = null
                                    showTagOrganizeLoading = true
                                    withIO {
                                        moveSubGroupsToGroup(
                                            sourceGroup = sourceGroup,
                                            paths = setOf(subPath),
                                            targetGroup = targetGroup
                                        )
                                    }
                                    showTagOrganizeLoading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(targetGroup.name) }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    TextButton(
                        onClick = {
                            newGroupNameForMove = ""
                            showNewGroupForMove = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("新建一级分组") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMoveSingleSubGroupDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 第4项: 含子分组的一级分组菜单"转为一级分组"——多选子分组, 各自转为独立一级分组
    if (showConvertSubGroupsToTopLevel != null) {
        val sourceGroup = showConvertSubGroupsToTopLevel!!
        val sourceGwt = models.find { it.group.id == sourceGroup.id }
        val subPaths = remember(sourceGwt) {
            sourceGwt?.list
                ?.map { it.categoryPath }
                ?.filter { it.isNotBlank() }
                ?.distinct()
                ?.sorted()
                ?: emptyList()
        }
        var selectedPaths by remember(sourceGroup.id) { mutableStateOf<Set<String>>(emptySet()) }
        val allSelected = subPaths.isNotEmpty() && selectedPaths.size == subPaths.size
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp

        AlertDialog(
            onDismissRequest = { showConvertSubGroupsToTopLevel = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.92f),
            title = { Text("转为一级分组") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "勾选子分组，每个选中的子分组将转为独立的一级分组",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .heightIn(max = 60.dp)
                            .verticalScroll(rememberScrollState())
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = screenHeight * 0.7f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 全选 / 取消全选
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPaths = if (allSelected) emptySet() else subPaths.toSet()
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TriStateCheckbox(
                                state = if (allSelected) ToggleableState.On
                                else if (selectedPaths.isEmpty()) ToggleableState.Off
                                else ToggleableState.Indeterminate,
                                onClick = {
                                    selectedPaths = if (allSelected) emptySet() else subPaths.toSet()
                                }
                            )
                            Text("全选", modifier = Modifier.padding(start = 8.dp))
                        }
                        HorizontalDivider()

                        subPaths.forEach { path ->
                            val checked = path in selectedPaths
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPaths = if (checked) selectedPaths - path
                                        else selectedPaths + path
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        selectedPaths = if (it) selectedPaths + path
                                        else selectedPaths - path
                                    }
                                )
                                Text(path, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = selectedPaths.isNotEmpty(),
                    onClick = {
                        // 立即关闭弹窗 + 显示加载遮罩
                        showConvertSubGroupsToTopLevel = null
                        showTagOrganizeLoading = true
                        scope.launch {
                            withIO {
                                convertSubGroupsToTopLevel(
                                    sourceGroup = sourceGroup,
                                    items = sourceGwt?.list ?: emptyList(),
                                    paths = selectedPaths
                                )
                            }
                            showTagOrganizeLoading = false
                        }
                    }
                ) { Text("转为一级分组") }
            },
            dismissButton = {
                TextButton(onClick = { showConvertSubGroupsToTopLevel = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    var showTagClearDialog by remember { mutableStateOf<SystemTtsV2?>(null) }
    if (showTagClearDialog != null) {
        val systts = showTagClearDialog!!
        val config = systts.config as TtsConfigurationDTO
        TagDataClearConfirmDialog(
            tagData = config.speechRule.tagData.toString(),
            onDismissRequest = { showTagClearDialog = null },
            onConfirm = {
                val updated = systts.copy(
                    config = config.copy(
                        speechRule = config.speechRule.copy(
                            target = SpeechTarget.ALL,
                        ).apply { resetTag() },
                    )
                )
                scope.launch {
                    withIO { dbm.systemTtsV2.update(updated) }
                    if (systts.isEnabled) SystemTtsService.notifyUpdateConfig()
                }
                showTagClearDialog = null
            }
        )
    }

    fun switchSpeechTarget(systts: SystemTtsV2) {
        if (!hasShownTip) {
            hasShownTip = true
            context.longToast(R.string.systts_drag_tip_msg)
        }

        val config = systts.config as TtsConfigurationDTO
        if (config.speechRule.target == SpeechTarget.BGM) return
        val ruleData = config.speechRule.copy()

        if (config.speechRule.target == SpeechTarget.TAG) dbm.speechRuleDao.getByRuleId(
            config.speechRule.tagRuleId
        )?.let { speechRule ->
            val keys = speechRule.tags.keys.toList()
            val idx = keys.indexOf(config.speechRule.tag)

            val nextIndex = (idx + 1)
            val newTag = keys.getOrNull(nextIndex)
            if (newTag == null) {
                if (ruleData.isTagDataEmpty()) {
                    ruleData.target = SpeechTarget.ALL
                    ruleData.resetTag()
                } else {
                    showTagClearDialog = systts
                    return
                }
            } else {
                ruleData.tag = newTag
                runCatching {
                    ruleData.tagName =
                        SpeechRuleEngine.getTagName(context, speechRule, info = ruleData)
                }.onFailure {
                    ruleData.tagName = ""
                    context.displayErrorDialog(it)
                }

            }
        }
        else {
            dbm.speechRuleDao.getByRuleId(ruleData.tagRuleId)?.let {
                ruleData.target = SpeechTarget.TAG
                ruleData.tag = it.tags.keys.first()
            }
        }

        val updated = systts.copy(config = systts.ttsConfig.copy(speechRule = ruleData))
        scope.launch {
            withIO { dbm.systemTtsV2.update(updated) }
            if (systts.isEnabled) SystemTtsService.notifyUpdateConfig()
        }
    }

    var deleteTts by remember { mutableStateOf<SystemTtsV2?>(null) }
    if (deleteTts != null) {
        ConfigDeleteDialog(
            onDismissRequest = { deleteTts = null }, content = deleteTts?.displayName ?: ""
        ) {
            val toDelete = deleteTts
            if (toDelete != null) {
                scope.launch {
                    withIO { dbm.systemTtsV2.delete(toDelete) }
                }
            }
            deleteTts = null
        }
    }

    // 多选删除分组：删除前确认对话框
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    // 删除弹窗内选中的分组（独立于列表预选，打开弹窗时初始化）
    var deleteSourcesSelected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    // 二次确认：首次点「删除(N)」仅进入待确认态，再点「确认删除」才真正执行
    var deleteConfirmArmed by remember { mutableStateOf(false) }
    if (showDeleteSelectedDialog) {
        val deleteTargets = models.filter { it.group.id in deleteSourcesSelected }
        // 勾选变化时重置二次确认状态，避免残留
        LaunchedEffect(deleteSourcesSelected) { deleteConfirmArmed = false }
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        Dialog(
            onDismissRequest = {
                showDeleteSelectedDialog = false
                deleteConfirmArmed = false
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("删除分组", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .heightIn(max = screenHeight * 0.7f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (models.isEmpty()) {
                            Text("没有可删除的分组。")
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("选择要删除的分组：")
                                TextButton(onClick = {
                                    deleteSourcesSelected =
                                        if (deleteSourcesSelected.size == models.size) emptySet()
                                        else models.map { it.group.id }.toSet()
                                }) {
                                    Text(if (deleteSourcesSelected.size == models.size) "取消全选" else "全选")
                                }
                            }
                            models.forEach { gwt ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            deleteSourcesSelected = if (gwt.group.id in deleteSourcesSelected)
                                                deleteSourcesSelected - gwt.group.id
                                            else deleteSourcesSelected + gwt.group.id
                                        }
                                ) {
                                    Checkbox(
                                        checked = gwt.group.id in deleteSourcesSelected,
                                        onCheckedChange = {
                                            deleteSourcesSelected = if (it) deleteSourcesSelected + gwt.group.id
                                            else deleteSourcesSelected - gwt.group.id
                                        }
                                    )
                                    Text(gwt.group.name)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(onClick = {
                            showDeleteSelectedDialog = false
                            deleteConfirmArmed = false
                        }) {
                            Text(stringResource(R.string.cancel))
                        }
                        TextButton(
                            enabled = deleteTargets.isNotEmpty(),
                            onClick = {
                                if (!deleteConfirmArmed) {
                                    deleteConfirmArmed = true
                                } else {
                                    showDeleteSelectedDialog = false
                                    deleteConfirmArmed = false
                                    showTagOrganizeLoading = true
                                    scope.launch {
                                        withIO {
                                            deleteTargets.forEach { gwt ->
                                                dbm.systemTtsV2.delete(*gwt.list.toTypedArray())
                                                dbm.systemTtsV2.deleteGroup(gwt.group)
                                            }
                                        }
                                        deleteSourcesSelected = emptySet()
                                        selectedGroupIds = emptySet()
                                        selectionMode = false
                                        showTagOrganizeLoading = false
                                    }
                                }
                            }
                        ) {
                            Text(
                                when {
                                    deleteTargets.isEmpty() -> "删除"
                                    deleteConfirmArmed -> "确认删除 (${deleteTargets.size})"
                                    else -> "删除 (${deleteTargets.size})"
                                },
                                color = if (deleteTargets.isNotEmpty()) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }

    val listState = rememberLazyListState()
    LazyListIndexStateSaver(models = models, listState = listState)

    val reorderState = rememberReorderableLazyListState(
        listState = listState,
        onMove = vm::reorder,
    )

    var addGroupDialog by remember { mutableStateOf(false) }
    if (addGroupDialog) {
        var name by remember { mutableStateOf("") }
        TextFieldDialog(title = stringResource(id = R.string.add_group),
            text = name,
            onTextChange = { name = it },
            onDismissRequest = { addGroupDialog = false }) {
            addGroupDialog = false
            scope.launch {
                withIO {
                    dbm.systemTtsV2.insertGroup(SystemTtsGroup(name = name, order = dbm.systemTtsV2.groupCount))
                }
            }
        }
    }

    var showGroupExportSheet by remember { mutableStateOf<List<GroupWithSystemTts>?>(null) }
    if (showGroupExportSheet != null) {
        val list = showGroupExportSheet!!
        ListExportBottomSheet(onDismissRequest = { showGroupExportSheet = null }, list = list)
    }

    // 多选模式下导出选中的分组
    var showExportSelected by remember { mutableStateOf(false) }
    if (showExportSelected) {
        val list = models.filter { it.group.id in selectedGroupIds }
        ListExportBottomSheet(onDismissRequest = { showExportSelected = false }, list = list)
    }

    var addPluginDialog by remember { mutableStateOf(false) }
    if (addPluginDialog) {
        PluginSelectionDialog(onDismissRequest = { addPluginDialog = false }) {
            navigateToEdit(
                SystemTtsV2(
                    // 新建配置项必须落在真实存在的分组上：groupId=0 没有对应分组，
                    // 保存后主列表（按分组 Relation 查询）将看不到该项，表现为"保存丢失"
                    groupId = DEFAULT_GROUP_ID,
                    config = TtsConfigurationDTO(
                        source = PluginTtsSource(
                            pluginId = it.pluginId,
                            locale = AppLocale.current(context).toCode()
                        )
                    )
                )
            )
        }
    }

    // 失效详情对话框：由顶栏警告按钮触发，列出各失效来源并提供“切换为其他插件”
    var showInvalidDetail by remember { mutableStateOf(false) }
    // 弹窗内逐源展开状态：记录已展开的来源 pluginId
    var expandedSources by remember { mutableStateOf<Set<String>>(emptySet()) }
    if (showInvalidDetail && invalidSourceCounts.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {
                showInvalidDetail = false
                expandedSources = emptySet()
            },
            title = {
                Text(
                    stringResource(id = R.string.invalid_items_detail_title, invalidCount)
                )
            },
            text = {
                // 不设置任何容器色，使用 AlertDialog 默认背景，避免列表项与外层色差
                LazyColumn {
                    items(invalidSourceCounts.entries.toList()) { (sourceId, count) ->
                        val displayName = pluginNameCache[sourceId] ?: sourceId
                        val items = invalidSourceItems[sourceId].orEmpty()
                        val expanded = sourceId in expandedSources
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedSources = if (expanded)
                                            expandedSources - sourceId
                                        else
                                            expandedSources + sourceId
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExpandCircleDown,
                                    contentDescription = if (expanded)
                                        stringResource(id = R.string.collapse)
                                    else stringResource(id = R.string.expand),
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .graphicsLayer {
                                            rotationZ = if (expanded) 180f else 0f
                                        },
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            stringResource(
                                                id = R.string.invalid_items_count_bracket,
                                                count
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                TextButton(onClick = {
                                    showInvalidDetail = false
                                    // 进入目标插件选择，由下方 pendingSourceForPicker 分支渲染
                                    fixSourcePluginId = sourceId
                                    pendingSourceForPicker = sourceId
                                }) {
                                    Text(stringResource(id = R.string.switch_to_other_plugin))
                                }
                                TextButton(onClick = {
                                    vm.batchDeleteInvalidItems(sourceId)
                                    showInvalidDetail = false
                                }) {
                                    Text(
                                        stringResource(id = R.string.delete),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            // 展开后列出该来源下具体失效配置项名称
                            if (expanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 28.dp, end = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp)) {
                                        items.forEach { itemName ->
                                            Text(
                                                text = itemName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showInvalidDetail = false
                    expandedSources = emptySet()
                }) {
                    Text(stringResource(id = R.string.close))
                }
            }
        )
    }
    // 目标插件选择（由失效详情中点击“切换为其他插件”触发）
    if (pendingSourceForPicker != null) {
        val sourceId = pendingSourceForPicker!!
        PluginSelectionDialog(
            onDismissRequest = {
                pendingSourceForPicker = null
                fixSourcePluginId = null
            }
        ) { plugin ->
            pendingSourceForPicker = null
            pendingPlugin = plugin
        }
    }
    if (pendingPlugin != null) {
        val plugin = pendingPlugin!!
        // 确认文案：指定来源时显示该来源的项数，否则显示总失效数
        val fixCount = fixSourcePluginId?.let { invalidSourceCounts[it] } ?: invalidCount
        AlertDialog(
            onDismissRequest = {
                pendingPlugin = null
                fixSourcePluginId = null
            },
            title = { Text(stringResource(id = R.string.batch_select_plugin)) },
            text = {
                Text(stringResource(id = R.string.batch_fix_confirm, fixCount, plugin.name))
            },
            confirmButton = {
                TextButton(onClick = {
                    val pluginId = plugin.pluginId
                    val sourceId = fixSourcePluginId
                    pendingPlugin = null
                    vm.batchFixInvalidItems(pluginId, sourceId)
                }) {
                    Text(stringResource(id = R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingPlugin = null
                    fixSourcePluginId = null
                }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    var showAuditionDialog by remember { mutableStateOf<SystemTtsV2?>(null) }
    if (showAuditionDialog != null) AuditionDialog(systts = showAuditionDialog!!) {
        showAuditionDialog = null
    }

    var showTagSwitch by remember { mutableStateOf<SystemTtsV2?>(null) }
    if (showTagSwitch != null) {
        TagSwitchDialog(item = showTagSwitch!!) {
            showTagSwitch = null
        }
    }

    var showRestartDialog by remember { mutableStateOf(false) }
    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text("选择操作") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showRestartDialog = false
                            // 完全重启应用，停止所有服务并重新启动
                            val intent = android.content.Intent(context, com.github.jing332.tts_server_android.compose.RestartActivity::class.java)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("重启") }
                    TextButton(
                        onClick = {
                            showRestartDialog = false
                            scope.launch {
                                refreshCloneAudioList(context)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("刷新克隆列表") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    var showOptions by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NavTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    when {
                        isSearchMode -> {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            ) {
                                SearchTextField(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                    value = searchKeyword,
                                    onValueChange = { vm.setSearchKeyword(it) },
                                    searchType = searchType,
                                    onSearchTypeChange = { vm.setSearchType(it) }
                                )
                            }
                        }
                        selectionMode -> Text(stringResource(id = R.string.selected_groups, selectedGroupIds.size))
                        else -> Text(stringResource(id = R.string.system_tts))
                    }
                },                 actions = {
                    when {
                        isSearchMode -> {
                            IconButton(onClick = {
                                isSearchMode = false
                                vm.setSearchKeyword("")
                            }) {
                                Icon(Icons.Default.Close, stringResource(id = R.string.close))
                            }
                        }
                        selectionMode -> {
                            IconButton(onClick = {
                                selectionMode = false
                                selectedGroupIds = emptySet()
                            }) {
                                Icon(Icons.Default.Close, stringResource(id = R.string.close))
                            }
                        }
                        else -> {
                            if (invalidCount > 0) {
                                IconButton(onClick = { showInvalidDetail = true }) {
                                    Icon(
                                        Icons.Default.Warning,
                                        stringResource(id = R.string.invalid_items_warning),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            IconButton(onClick = { selectionMode = true }) {
                                Icon(Icons.Default.CheckBox, "多选")
                            }
                            IconButton(onClick = { showRestartDialog = true }) {
                                Icon(Icons.Default.Refresh, stringResource(id = R.string.restart))
                            }
                            IconButton(onClick = { isSearchMode = true }) {
                                Icon(Icons.Default.Search, stringResource(id = R.string.search))
                            }
                            IconButton(onClick = { showOptions = true }) {
                                Icon(Icons.Default.MoreVert, stringResource(id = R.string.more_options))
                            }
                            MenuMoreOptions(
                                expanded = showOptions,
                                onDismissRequest = { showOptions = false },
                                onExportAll = { showGroupExportSheet = models }
                            )
                        }
                    }
                })
        },
        bottomBar = {
            if (selectionMode) {
                // 多选模式底部操作栏：全选 + 删除 + 转为子分组（条件显示）
                val selectedGroups = models.filter { it.group.id in selectedGroupIds }
                val allSelected = selectedGroupIds.size == models.size
                val canConvertToSubGroup = selectedGroups.isNotEmpty() &&
                    selectedGroups.none { it.list.any { tts -> tts.categoryPath.isNotBlank() } }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            selectedGroupIds = if (allSelected) emptySet() else models.map { it.group.id }.toSet()
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    if (allSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = "全选",
                                    modifier = Modifier.size(28.dp)
                                )
                                Text("全选", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        TextButton(onClick = {
                            if (selectedGroupIds.isNotEmpty()) {
                                showExportSelected = true
                            }
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Output,
                                    contentDescription = "导出",
                                    modifier = Modifier.size(28.dp)
                                )
                                Text("导出", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        TextButton(onClick = {
                            if (selectedGroupIds.isNotEmpty()) {
                                deleteSourcesSelected = selectedGroupIds
                                showDeleteSelectedDialog = true
                            }
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.DeleteForever,
                                    contentDescription = stringResource(id = R.string.delete),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    stringResource(id = R.string.delete),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        if (canConvertToSubGroup) {
                            TextButton(onClick = {
                                convertSourcesSelected = selectedGroupIds
                                showConvertToSubGroupMulti = true
                            }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.DriveFileMove,
                                        contentDescription = "转为子分组",
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text("转为子分组", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
    Box(Modifier.fillMaxSize().padding(
        top = paddingValues.calculateTopPadding(),
        bottom = paddingValues.calculateBottomPadding()
    )) {
        ControlBottomBarVisibility(listState, LocalBottomBarBehavior.current)
        // 缩短长按超时时间(默认约500ms→300ms)，使拖拽排序和长按菜单响应更快
        val defaultViewConfig = LocalViewConfiguration.current
        val fastLongPressConfig = remember(defaultViewConfig) {
            object : ViewConfiguration by defaultViewConfig {
                override val longPressTimeoutMillis: Long = 200L
            }
        }
        CompositionLocalProvider(LocalViewConfiguration provides fastLongPressConfig) {
        // 树结构缓存：仅当各分组的 TTS 项内容变化时重建（展开/折叠只改 isExpanded，不改 TTS 项，
        // 因此 hash 签名不变，树缓存命中）。避免每次展开/折叠都为所有分组重建树导致卡顿。
        val ttsItemsSignature = remember(models) { models.map { it.list.hashCode() } }
        val subGroupTrees = remember(ttsItemsSignature) {
            models.associate { gwt ->
                gwt.group.id to if (gwt.list.any { it.categoryPath.isNotBlank() }) {
                    flattenSubCategoryTree(buildSubCategoryTree(gwt.list))
                } else null
            }
        }
        // 可见项过滤：轻量操作，每次展开/折叠时执行（仅遍历已缓存的扁平树做过滤，不重建树）
        val subGroupVisibleItemsMap = remember(models, expandedSubGroups) {
            models.associate { gwt ->
                val g = gwt.group
                val flattened = subGroupTrees[g.id]
                if (g.isExpanded && flattened != null) {
                    val visItems = mutableListOf<FlattenedCategoryItem>()
                    var skipLevel = Int.MAX_VALUE
                    for (fItem in flattened) {
                        when (fItem) {
                            is FlattenedCategoryItem.SubGroupHeader -> {
                                if (fItem.node.level <= skipLevel) {
                                    skipLevel = Int.MAX_VALUE
                                }
                                if (fItem.node.level > skipLevel) {
                                    continue
                                }
                                visItems.add(fItem)
                                if (!expandedSubGroups.contains(fItem.node.fullPath)) {
                                    skipLevel = fItem.node.level
                                }
                            }
                            is FlattenedCategoryItem.TtsItem -> {
                                if (fItem.displayLevel <= skipLevel) {
                                    visItems.add(fItem)
                                }
                            }
                        }
                    }
                    g.id to visItems
                } else {
                    g.id to null
                }
            }
        }
        LazyColumn(
                Modifier
                    .fillMaxSize()
                    .reorderable(state = reorderState),
                state = listState
            ) {
                models.forEachIndexed { _, groupWithSystemTts ->
                    val g = groupWithSystemTts.group
                    val key = "g_${g.id}"
                    
                    val groupDragModifier = if (searchKeyword.isNotEmpty() || selectionMode) Modifier 
                                            else Modifier.detectReorderAfterLongPress(reorderState)

                    stickyHeader(key = key) {
                        val checkState =
                            groupWithSystemTts.list.filter { it.isEnabled }.size.sizeToToggleableState(
                                groupWithSystemTts.list.size
                            )

                        ShadowedDraggableItem(reorderableState = reorderState, key = key) {
                            Group(modifier = groupDragModifier.fillMaxWidth(),
                                name = g.name,
                                group = g,
                                isExpanded = g.isExpanded,
                                toggleableState = checkState,
                                onToggleableStateChange = {
                                    vm.updateGroupEnable(groupWithSystemTts, it)
                                },
                                onClick = {
                                    val wasExpanded = g.isExpanded
                                    vm.toggleGroupExpanded(g)
                                    if (!wasExpanded) {
                                        scope.launch {
                                            // 等待重组完成后再用新布局索引滚动，避免并发导致跳位
                                            withFrameNanos { }
                                            val headerIndex = listState.layoutInfo.visibleItemsInfo.find { it.key == key }?.index
                                            if (headerIndex != null) {
                                                listState.animateScrollToItem(headerIndex)
                                            }
                                        }
                                    }
                                },
                                onDelete = {
                                    scope.launch {
                                        withIO {
                                            dbm.systemTtsV2.delete(*groupWithSystemTts.list.toTypedArray())
                                            dbm.systemTtsV2.deleteGroup(g)
                                        }
                                    }
                                },
                                onRename = {
                                    scope.launch { withIO { dbm.systemTtsV2.updateGroup(g.copy(name = it)) } }
                                },
                                onCopy = {
                                    scope.launch {
                                        withIO {
                                            val group = g.copy(id = System.currentTimeMillis(),
                                                name = it.ifBlank { context.getString(R.string.unnamed) })
                                            dbm.systemTtsV2.insertGroup(group)
                                            val baseId = System.currentTimeMillis()
                                            val copies = dbm.systemTtsV2.getByGroup(g.id)
                                                .mapIndexed { index, tts ->
                                                    tts.copy(id = baseId + index, groupId = group.id)
                                                }
                                            if (copies.isNotEmpty()) {
                                                dbm.systemTtsV2.insert(*copies.toTypedArray())
                                            }
                                        }
                                    }
                                },
                                onExport = {
                                    showGroupExportSheet = listOf(groupWithSystemTts)
                                },
                                onSort = {
                                    showSortDialog = groupWithSystemTts.list to null
                                },
                                onCreateSubGroup = {
                                    showCreateSubGroup = g.id
                                },
                                hasSubGroups = groupWithSystemTts.list.any { it.categoryPath.isNotBlank() },
                                hasTagKeyword = detectTagKeyword(g.name) != null,
                                itemCount = groupWithSystemTts.list.size,
                                onBatchAssignTags = {
                                    showBatchTagDialog = groupWithSystemTts.list
                                },
                                onConvertToSubGroup = {
                                    showConvertToSubGroup = g
                                },
                                onExtractSubGroup = {
                                    showExtractSubGroup = g
                                },
                                onDeleteEnabled = {
                                    val enabledToDelete = groupWithSystemTts.list.filter { it.isEnabled }
                                    if (enabledToDelete.isNotEmpty()) {
                                        scope.launch {
                                            withIO {
                                                dbm.systemTtsV2.delete(*enabledToDelete.toTypedArray())
                                            }
                                            SystemTtsService.notifyUpdateConfig()
                                        }
                                    }
                                },
                                onDeleteDisabled = {
                                    val disabledToDelete = groupWithSystemTts.list.filter { !it.isEnabled }
                                    if (disabledToDelete.isNotEmpty()) {
                                        scope.launch {
                                            withIO {
                                                dbm.systemTtsV2.delete(*disabledToDelete.toTypedArray())
                                            }
                                        }
                                    }
                                },
                                onMoveEnabledToGroup = {
                                    showMoveEnabledDialog = groupWithSystemTts
                                },
                                onMoveSubGroups = {
                                    // 第3项: 一级分组"移动子分组", 进入多选移动子分组对话框(不预选)
                                    showMoveSubGroupsDialog = g to null
                                },
                                onConvertSubGroupsToTopLevel = {
                                    // 第4项: 一级分组"转为一级分组", 多选子分组转为独立一级分组
                                    showConvertSubGroupsToTopLevel = g
                                },
                                onResortTagsByExisting = {
                                    showTagOrganizeLoading = true
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            resortTags(groupWithSystemTts.list, fromZero = false)
                                        }
                                        showTagOrganizeLoading = false
                                    }
                                },
                                onResortTagsFromZero = {
                                    showTagOrganizeLoading = true
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            resortTags(groupWithSystemTts.list, fromZero = true)
                                        }
                                        showTagOrganizeLoading = false
                                    }
                                },
                                onReassignTags = {
                                    reassignTagPrefix = ""
                                    showReassignTagDialog = groupWithSystemTts
                                },
                                onReassignTagsByGroupName = {
                                    val detected = detectTagKeyword(g.name)
                                    if (detected == null) {
                                        context.toast("分组名未包含关键词")
                                    } else {
                                        showTagOrganizeLoading = true
                                        scope.launch {
                                            val count = withContext(Dispatchers.IO) {
                                                reassignTagsByGroupName(groupWithSystemTts.list, g.name)
                                            }
                                            showTagOrganizeLoading = false
                                            context.toast("已按「${detected.prefix}」整理 $count 个标签")
                                        }
                                    }
                                },
                                onReassignAllSubGroups = {
                                    showTagOrganizeLoading = true
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            reassignTagsForAllSubGroups(groupWithSystemTts.list)
                                        }
                                        showTagOrganizeLoading = false
                                        context.toast("已按各子分组关键词整理标签")
                                    }
                                },
                                onMergeGroup = {
                                    showMergeGroup = g
                                },
                                inSelectionMode = selectionMode,
                                isSelected = remember(g.id) { derivedStateOf { g.id in selectedGroupIds } }.value,
                                onToggleSelect = {
                                    selectedGroupIds = if (g.id in selectedGroupIds)
                                        selectedGroupIds - g.id else selectedGroupIds + g.id
                                }
                            )
                        }
                    }

                    if (g.isExpanded) {
                        val hasSubGroups = groupWithSystemTts.list.any { it.categoryPath.isNotBlank() }

                        if (!hasSubGroups) {
                            // 无子分组时保持原有扁平渲染（支持拖拽排序）
                            // LazyListScope 内不能调用 remember；按 order 直接排序（仅在重组时执行一次）
                            val sortedList = groupWithSystemTts.list.sortedBy { it.order }
                            itemsIndexed(sortedList,
                                key = { _, v -> "${g.id}_${v.id}" }) { _, item ->
                                ShadowedDraggableItem(
                                    reorderableState = reorderState,
                                    key = "${g.id}_${item.id}"
                                ) {
                                    val descriptor = remember(item) {
                                        ItemDescriptorFactory.from(context, item)
                                    }
                                    Item(reorderState = reorderState,
                                        modifier = (if (searchKeyword.isNotEmpty() || selectionMode) Modifier
                                            else Modifier.detectReorderAfterLongPress(reorderState)
                                            ).padding(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        ),
                                        name = item.displayName,
                                        tagName = descriptor.tagName,
                                        type = descriptor.type,
                                        standby = descriptor.standby,
                                        enabled = item.isEnabled,
                                        onEnabledChange = {
                                            vm.updateTtsEnabled(item, it)
                                            if (it) SystemTtsService.notifyUpdateConfig()
                                        },
                                        desc = descriptor.desc,
                                        params = descriptor.bottom,
                                        onClick = { showQuickEdit = item },
                                        onLongClick = { switchSpeechTarget(item) },
                                        onCopy = {
                                            navigateToEdit(item.copy(id = System.currentTimeMillis()))
                                        },
                                        onDelete = { deleteTts = item },
                                        onEdit = { navigateToEdit(item) },
                                        onAudition = {
                                            if (item.config is TtsConfigurationDTO) {
                                                // 强制创建新的对象副本，确保 Compose 检测到变化并重新触发试听
                                                showAuditionDialog = item.copy()
                                            } else
                                                context.toast(R.string.not_support_audition)
                                        },
                                        onExport = {
                                            showGroupExportSheet =
                                                listOf(GroupWithSystemTts(g, listOf(item)))
                                        },
                                        onMoveToSubGroup = {
                                            showMoveToSubGroup = item
                                        },
                                        onSwitchTag = {
                                            showTagSwitch = item
                                        }
                                    )
                                }
                            }
                        } else {
                            // 有子分组时使用树形渲染（树已在 LazyColumn 外预计算并缓存，避免重组时重复构建）
                            val visibleItems = subGroupVisibleItemsMap[g.id] ?: emptyList()

                            // 标记是否已插入"根目录配置"分隔标题
                            // 当一级分组下同时有子分组和根目录配置项时,在根目录配置项前插入分隔,
                            // 让用户能区分这些是不属于任何子分组的配置项
                            var rootSectionHeaderInserted = false
                            visibleItems.forEach { fItem ->
                                when (fItem) {
                                    is FlattenedCategoryItem.SubGroupHeader -> {
                                        val subKey = "sub_${g.id}_${fItem.node.fullPath}"
                                        val subDragModifier = if (searchKeyword.isNotEmpty() || selectionMode) Modifier
                                            else Modifier.detectReorderAfterLongPress(reorderState)
                                        // 聚合整棵子树：上级分组头的勾选/删除/导出等需覆盖其下所有层级
                                        val subItems = fItem.node.allItems
                                        val subCheckState = subItems.filter { it.isEnabled }.size.sizeToToggleableState(subItems.size)

                                        val headerContent: @Composable LazyItemScope.() -> Unit = {
                                            ShadowedDraggableItem(
                                                reorderableState = reorderState,
                                                key = subKey
                                            ) { _ ->
                                                SubGroupHeader(
                                                    modifier = subDragModifier,
                                                    name = fItem.node.name,
                                                    level = fItem.node.level,
                                                    isExpanded = expandedSubGroups.contains(fItem.node.fullPath),
                                                    toggleableState = subCheckState,
                                                    onToggleableStateChange = { enabled ->
                                                        vm.updateSubGroupEnable(g.id, subItems, enabled)
                                                    },
                                                    onClick = {
                                                        val fullPath = fItem.node.fullPath
                                                        if (expandedSubGroups.contains(fullPath)) {
                                                            expandedSubGroups = expandedSubGroups - fullPath
                                                        } else {
                                                            expandedSubGroups = expandedSubGroups + fullPath
                                                        }
                                                    },
                                                    onRename = {
                                                        showSubGroupRename = subItems to fItem.node.fullPath
                                                    },
                                                    onSort = {
                                                        showSortDialog = subItems to groupWithSystemTts.list
                                                    },
                                                    onBatchAssignTags = {
                                                        showSubGroupBatchTag = subItems
                                                    },
                                                    hasTagKeyword = detectTagKeyword(fItem.node.name) != null,
                                                    onReassignTagsByGroupName = {
                                                        val detected = detectTagKeyword(fItem.node.name)
                                                        if (detected == null) {
                                                            context.toast("分组名未包含关键词")
                                                        } else {
                                                            showTagOrganizeLoading = true
                                                            scope.launch {
                                                                val count = withContext(Dispatchers.IO) {
                                                                    reassignTagsByGroupName(subItems, fItem.node.name)
                                                                }
                                                                showTagOrganizeLoading = false
                                                                context.toast("已按「${detected.prefix}」整理 $count 个标签")
                                                            }
                                                        }
                                                    },
                                                    onDelete = {
                                                        scope.launch {
                                                            withIO { dbm.systemTtsV2.delete(*subItems.toTypedArray()) }
                                                        }
                                                    },
                                                    onDeleteEnabled = {
                                                        val enabledToDelete = subItems.filter { it.isEnabled }
                                                        if (enabledToDelete.isNotEmpty()) {
                                                            scope.launch {
                                                                withIO {
                                                                    dbm.systemTtsV2.delete(*enabledToDelete.toTypedArray())
                                                                }
                                                                SystemTtsService.notifyUpdateConfig()
                                                            }
                                                        }
                                                    },
                                                    onDeleteDisabled = {
                                                        val disabledToDelete = subItems.filter { !it.isEnabled }
                                                        if (disabledToDelete.isNotEmpty()) {
                                                            scope.launch {
                                                                withIO {
                                                                    dbm.systemTtsV2.delete(*disabledToDelete.toTypedArray())
                                                                }
                                                            }
                                                        }
                                                    },
                                                    onExport = {
                                                        showGroupExportSheet =
                                                            listOf(GroupWithSystemTts(g, subItems))
                                                    },
                                                    onExtractToGroup = {
                                                        showSubGroupExtractToGroup = g to fItem.node.fullPath
                                                    },
                                                    onMoveEnabledToGroup = {
                                                        showMoveEnabledDialog = GroupWithSystemTts(g, subItems)
                                                    },
                                                    onMoveToOtherGroup = {
                                                        showMoveSingleSubGroupDialog = g to fItem.node.fullPath
                                                    },
                                                    itemCount = subItems.size
                                                )
                                            }
                                        }

                                        item(key = subKey) { headerContent() }
                                    }
                                    is FlattenedCategoryItem.TtsItem -> {
                                        // 根目录配置项(不属于任何子分组)且之前有子分组:插入分隔标题以区分
                                        if (fItem.displayLevel == 0 && !rootSectionHeaderInserted) {
                                            rootSectionHeaderInserted = true
                                            item(key = "root_sep_${g.id}") {
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    shape = MaterialTheme.shapes.small
                                                ) {
                                                    Text(
                                                        text = "根目录配置",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }
                                        val item = fItem.item
                                        val itemKey = "item_${g.id}_${fItem.categoryPath}_${item.id}"
                                        val itemDragModifier = if (searchKeyword.isNotEmpty() || selectionMode) Modifier
                                            else Modifier.detectReorderAfterLongPress(reorderState)
                                        item(key = itemKey) {
                                            ShadowedDraggableItem(
                                                reorderableState = reorderState,
                                                key = itemKey
                                            ) { _ ->
                                                val descriptor = remember(item) {
                                                    ItemDescriptorFactory.from(context, item)
                                                }
                                                Item(
                                                    reorderState = reorderState,
                                                    modifier = itemDragModifier.padding(
                                                        start = 8.dp,
                                                        end = 8.dp,
                                                        top = 4.dp,
                                                        bottom = 4.dp
                                                    ),
                                                    name = item.displayName,
                                                    tagName = descriptor.tagName,
                                                    type = descriptor.type,
                                                    standby = descriptor.standby,
                                                    enabled = item.isEnabled,
                                                    onEnabledChange = {
                                                        vm.updateTtsEnabled(item, it)
                                                        if (it) SystemTtsService.notifyUpdateConfig()
                                                    },
                                                    desc = descriptor.desc,
                                                    params = descriptor.bottom,
                                                    onClick = { showQuickEdit = item },
                                                    onLongClick = { switchSpeechTarget(item) },
                                                    onCopy = {
                                                        navigateToEdit(item.copy(id = System.currentTimeMillis()))
                                                    },
                                                    onDelete = { deleteTts = item },
                                                    onEdit = { navigateToEdit(item) },
                                                    onAudition = {
                                                        if (item.config is TtsConfigurationDTO) {
                                                            showAuditionDialog = item.copy()
                                                        } else
                                                            context.toast(R.string.not_support_audition)
                                                    },
                                                    isInSubGroup = fItem.displayLevel > 0,
                                                    onExport = {
                                                        showGroupExportSheet =
                                                            listOf(GroupWithSystemTts(g, listOf(item)))
                                                    },
                                                    onMoveToSubGroup = {
                                                        showMoveToSubGroup = item
                                                    },
                                                    onSwitchTag = {
                                                        showTagSwitch = item
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.padding(bottom = AppDefaultProperties.LIST_END_PADDING))
                }
            }
        }

            DraggableVerticalScrollbar(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight()
                    .width(12.dp),
                listState = listState
            )

            FloatingAddConfigButtonGroup(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                visible = true,
                addBgm = {
                    navigateToEdit(SystemTtsV2(groupId = DEFAULT_GROUP_ID, config = BgmConfiguration()))
                },
                addLocal = {
                    navigateToEdit(
                        SystemTtsV2(
                            groupId = DEFAULT_GROUP_ID,
                            config = TtsConfigurationDTO(
                                source = LocalTtsSource(locale = AppConst.localeCode)
                            )
                        )
                    )
                },
                addPlugin = {
                    addPluginDialog = true
                },
                addGroup = {
                    addGroupDialog = true
                }
            )

            LaunchedEffect(key1 = Unit) {
                withIO {
                    vm.checkListData(context)
                }
            }

        }
    }
}
