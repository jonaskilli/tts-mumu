package com.github.jing332.tts_server_android.compose.systts.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandCircleDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drake.net.utils.withIO
import com.github.jing332.common.utils.longToast
import com.github.jing332.common.utils.toast
import com.github.jing332.compose.widgets.ControlBottomBarVisibility
import com.github.jing332.compose.widgets.LazyListIndexStateSaver
import com.github.jing332.compose.widgets.ShadowedDraggableItem
import com.github.jing332.compose.widgets.TextFieldDialog
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.AbstractListGroup
import com.github.jing332.database.entities.systts.BgmConfiguration
import com.github.jing332.database.entities.systts.GroupWithSystemTts
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.SystemTtsGroup
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.database.entities.systts.source.PluginTtsSource
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
import com.github.jing332.tts_server_android.compose.systts.ConfigExportBottomSheet
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import java.io.File


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
    
    var isSearchMode by rememberSaveable { mutableStateOf(false) }

    // 子分组展开状态：存储已展开的子分组完整路径（持久化，默认全部折叠）
    var expandedSubGroups by remember { AppConfig.expandedSubGroups }

    BackHandler(enabled = isSearchMode) {
        isSearchMode = false
        vm.setSearchKeyword("")
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
            scope.launch {
                val newPath = if (oldPath.contains('/')) {
                    oldPath.substringBeforeLast('/') + "/" + newName
                } else newName
                items.forEach { item ->
                    dbm.systemTtsV2.update(item.copy(categoryPath = newPath))
                }
                showSubGroupRename = null
            }
        }
    }

    var showSubGroupAudioParams by remember { mutableStateOf<Pair<SystemTtsGroup, String>?>(null) }
    if (showSubGroupAudioParams != null) {
        val (group, path) = showSubGroupAudioParams!!
        val subGroupMap = group.subGroupAudioParamsJson.let { jsonStr ->
            if (jsonStr.isBlank() || jsonStr == "{}") emptyMap()
            else SystemTtsV2.Converters.json.decodeFromString<Map<String, com.github.jing332.database.entities.systts.AudioParams>>(jsonStr)
        }
        val currentParams = subGroupMap[path] ?: com.github.jing332.database.entities.systts.AudioParams()
        GroupAudioParamsDialog(
            onDismissRequest = { showSubGroupAudioParams = null },
            params = currentParams,
            onConfirm = { params ->
                scope.launch {
                    val newMap = subGroupMap.toMutableMap().apply { put(path, params) }
                    val newJson = SystemTtsV2.Converters.json.encodeToString(newMap)
                    dbm.systemTtsV2.updateGroup(group.copy(subGroupAudioParamsJson = newJson))
                    SystemTtsService.notifyUpdateConfig()
                    showSubGroupAudioParams = null
                }
            }
        )
    }

    var showSubGroupBatchTag by remember { mutableStateOf<List<SystemTtsV2>?>(null) }
    if (showSubGroupBatchTag != null) {
        BatchTagDialog(
            groupItems = showSubGroupBatchTag!!,
            onDismissRequest = { showSubGroupBatchTag = null }
        )
    }

    // 子分组转为大分组确认对话框
    var showSubGroupExtractToGroup by remember { mutableStateOf<Pair<SystemTtsGroup, String>?>(null) }
    if (showSubGroupExtractToGroup != null) {
        val (group, path) = showSubGroupExtractToGroup!!
        AlertDialog(
            onDismissRequest = { showSubGroupExtractToGroup = null },
            title = { Text("转为大分组") },
            text = { Text("将子分组 \"${path.substringAfterLast('/')}\" 移出为独立的大分组？") },
            confirmButton = {
                TextButton(onClick = {
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
                        itemsToMove.forEach { item ->
                            dbm.systemTtsV2.update(
                                item.copy(
                                    groupId = newGroup.id,
                                    categoryPath = ""
                                )
                            )
                        }
                        showSubGroupExtractToGroup = null
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
            dbm.systemTtsV2.insert(showQuickEdit!!)
            if (showQuickEdit?.isEnabled == true) SystemTtsService.notifyUpdateConfig()
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
     * 计算 tagName：若配置有 tagRuleId 且能查到对应的 SpeechRule，
     * 则用 [SpeechRuleEngine.getTagName] 按实际配置计算出名字（与"批量分配标签"一致）；
     * 若查不到规则或计算异常，则回退到 [fallback]（即直接用 tag 名字）。
     *
     * 必须在 IO 线程调用（内部有数据库查询和脚本执行）。
     *
     * @param context Android Context
     * @param ruleData 已经更新好 tag 的 SpeechRuleInfo（会原地修改其 tagName）
     * @param fallback 计算失败时的回退 tagName
     */
    suspend fun computeTagNameOrFallback(
        context: android.content.Context,
        ruleData: com.github.jing332.database.entities.systts.SpeechRuleInfo,
        fallback: String,
    ) {
        val ruleId = ruleData.tagRuleId
        if (ruleId.isBlank()) {
            ruleData.tagName = fallback
            return
        }
        val computed = withContext(Dispatchers.IO) {
            runCatching {
                val speechRule = dbm.speechRuleDao.getByRuleId(ruleId)
                if (speechRule != null) {
                    SpeechRuleEngine.getTagName(context, speechRule, ruleData)
                } else {
                    null
                }
            }.getOrNull()
        }
        ruleData.tagName = computed?.takeIf { it.isNotBlank() } ?: fallback
    }

    /**
     * 标签重排：按 categoryPath 分组，同前缀的启用配置重排标签序号
     * @param fromZero true=从01开始连续编号，false=用原有序号集合重新分配
     */
    suspend fun resortTags(list: List<SystemTtsV2>, fromZero: Boolean) {
        val tagPattern = Regex("^(.+?)(\\d+)$")
        // narration 特殊标签：重排时第一项保留原 tag，后续用其 tagName(旁白)+序号
        val NARRATION_TAG = "narration"
        // 按 categoryPath 分组
        list.groupBy { it.categoryPath }.forEach { (path, items) ->
            // 只处理启用的、且有 TtsConfigurationDTO 的配置
            val enabledItems = items.filter { it.isEnabled && it.config is TtsConfigurationDTO }
                .sortedBy { it.order }
            if (enabledItems.isEmpty()) return@forEach

            // 按标签前缀分组
            val byPrefix = enabledItems.groupBy { item ->
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
                        computeTagNameOrFallback(context, firstRule, NARRATION_TAG)
                        dbm.systemTtsV2.update(first.copy(
                            config = firstConfig.copy(speechRule = firstRule)
                        ))
                        // 后续项：tag = 旁白01、旁白02...（用第一项计算出的 tagName 作为前缀）
                        val subPrefix = firstRule.tagName
                        prefixItems.drop(1).forEachIndexed { idx, item ->
                            val newTag = subPrefix + String.format("%02d", idx + 1)
                            val config = item.config as TtsConfigurationDTO
                            val newRule = config.speechRule.copy(tag = newTag)
                            computeTagNameOrFallback(context, newRule, newTag)
                            dbm.systemTtsV2.update(item.copy(
                                config = config.copy(speechRule = newRule)
                            ))
                        }
                    } else {
                        // 其他标签：从01开始连续编号
                        prefixItems.forEachIndexed { idx, item ->
                            val newTag = prefix + String.format("%02d", idx + 1)
                            val config = item.config as TtsConfigurationDTO
                            val newRule = config.speechRule.copy(tag = newTag)
                            computeTagNameOrFallback(context, newRule, newTag)
                            dbm.systemTtsV2.update(item.copy(
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
                        computeTagNameOrFallback(context, newRule, newTag)
                        dbm.systemTtsV2.update(item.copy(
                            config = config.copy(speechRule = newRule)
                        ))
                    }
                }
            }
        }
        SystemTtsService.notifyUpdateConfig()
    }

    /**
     * 重新分配标签：用指定前缀，按当前位置顺序从01开始连续编号（仅启用配置）
     */
    suspend fun reassignTagsWithPrefix(list: List<SystemTtsV2>, prefix: String) {
        // 按 categoryPath 分组，每个独立处理
        list.groupBy { it.categoryPath }.forEach { (path, items) ->
            val enabledItems = items.filter { it.isEnabled && it.config is TtsConfigurationDTO }
                .sortedBy { it.order }
            if (enabledItems.isEmpty()) return@forEach

            enabledItems.forEachIndexed { idx, item ->
                val newTag = prefix + String.format("%02d", idx + 1)
                val config = item.config as TtsConfigurationDTO
                val newRule = config.speechRule.copy(tag = newTag)
                computeTagNameOrFallback(context, newRule, newTag)
                dbm.systemTtsV2.update(item.copy(
                    config = config.copy(speechRule = newRule)
                ))
            }
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
                    scope.launch {
                        selectedItems.forEach { item ->
                            dbm.systemTtsV2.update(
                                item.copy(categoryPath = subGroupName)
                            )
                        }
                        showCreateSubGroup = null
                    }
                }
            )
        }
    }

    var showMoveToSubGroup by remember { mutableStateOf<SystemTtsV2?>(null) }
    if (showMoveToSubGroup != null) {
        val targetItem = showMoveToSubGroup!!
        val currentGroup = models.find { it.group.id == targetItem.groupId }
        val existingPaths = remember(currentGroup?.group?.id) {
            currentGroup?.list?.map { it.categoryPath }
                ?.filter { it.isNotBlank() }
                ?.distinct()
                ?.sorted()
                ?: emptyList()
        }
        MoveToSubGroupDialog(
            existingPaths = existingPaths,
            onDismissRequest = { showMoveToSubGroup = null },
            onConfirm = { path ->
                scope.launch {
                    dbm.systemTtsV2.update(targetItem.copy(categoryPath = path))
                    showMoveToSubGroup = null
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

    // === 子分组操作对话框 ===
    var showReleaseSubGroup by remember { mutableStateOf<SystemTtsGroup?>(null) }
    if (showReleaseSubGroup != null) {
        val targetGroup = showReleaseSubGroup!!
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
            onDismissRequest = { showReleaseSubGroup = null },
            title = { Text("释放子分组") },
            text = {
                Column {
                    if (subPaths.isEmpty()) {
                        Text("当前分组没有子分组")
                    } else {
                        Text("选择要释放的子分组，内容将移回根目录：", modifier = Modifier.padding(bottom = 8.dp))
                        subPaths.forEach { path ->
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        currentGroupWithTts?.list
                                            ?.filter { it.categoryPath == path }
                                            ?.forEach { item ->
                                                dbm.systemTtsV2.update(item.copy(categoryPath = ""))
                                            }
                                        showReleaseSubGroup = null
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(path)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showReleaseSubGroup = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
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
                                    scope.launch {
                                        // 将原大分组的音频参数作为子分组参数保存到目标分组
                                        val subMap = otherGroup.subGroupAudioParamsJson.let { jsonStr ->
                                            if (jsonStr.isBlank() || jsonStr == "{}") emptyMap()
                                            else SystemTtsV2.Converters.json.decodeFromString<Map<String, com.github.jing332.database.entities.systts.AudioParams>>(jsonStr)
                                        }.toMutableMap()
                                        subMap[targetGroup.name] = targetGroup.audioParams
                                        val newJson = SystemTtsV2.Converters.json.encodeToString(subMap)
                                        dbm.systemTtsV2.updateGroup(otherGroup.copy(subGroupAudioParamsJson = newJson))

                                        currentGroupWithTts?.list?.forEach { item ->
                                            dbm.systemTtsV2.update(
                                                item.copy(
                                                    groupId = otherGroup.id,
                                                    categoryPath = targetGroup.name
                                                )
                                            )
                                        }
                                        // 当前分组已为空，直接删除
                                        dbm.systemTtsV2.deleteGroup(targetGroup)
                                        showConvertToSubGroup = null
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

    var showExtractSubGroup by remember { mutableStateOf<SystemTtsGroup?>(null) }

    // 长按菜单：移动启用配置到其他分组
    var showMoveEnabledDialog by remember { mutableStateOf<GroupWithSystemTts?>(null) }

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
                scope.launch {
                    reassignTagsWithPrefix(sourceGwt.list, prefix)
                    showReassignTagDialog = null
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
            title = { Text("移出子分组") },
            text = {
                Column {
                    if (subPaths.isEmpty()) {
                        Text("当前分组没有子分组")
                    } else {
                        Text("选择要移出的子分组，将创建为独立分组：", modifier = Modifier.padding(bottom = 8.dp))
                        subPaths.forEach { path ->
                            TextButton(
                                onClick = {
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
                                        itemsToMove.forEach { item ->
                                            dbm.systemTtsV2.update(
                                                item.copy(
                                                    groupId = newGroup.id,
                                                    categoryPath = ""
                                                )
                                            )
                                        }
                                        showExtractSubGroup = null
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(path)
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
            title = "新建大分组并移动",
            text = newGroupNameForMove,
            onTextChange = { newGroupNameForMove = it },
            onDismissRequest = { showNewGroupForMove = false }
        ) {
            val sourceGwt = showMoveEnabledDialog
            if (sourceGwt != null) {
                val enabledItems = sourceGwt.list.filter { it.isEnabled }
                scope.launch {
                    val newGroup = SystemTtsGroup(id = System.currentTimeMillis(), name = newGroupNameForMove)
                    dbm.systemTtsV2.insertGroup(newGroup)
                    enabledItems.forEachIndexed { idx, item ->
                        dbm.systemTtsV2.update(item.copy(
                            groupId = newGroup.id,
                            categoryPath = "",
                            order = idx
                        ))
                    }
                    SystemTtsService.notifyUpdateConfig()
                    showNewGroupForMove = false
                    showMoveEnabledDialog = null
                }
            }
        }
    }

    if (showMoveEnabledDialog != null && !showNewGroupForMove) {
        val sourceGwt = showMoveEnabledDialog!!
        val sourceGroup = sourceGwt.group
        val enabledItems = sourceGwt.list.filter { it.isEnabled }
        val allGroups = remember { dbm.systemTtsV2.getAllGroupWithTts() }
        // 展开的大分组ID集合
        var expandedMoveGroups by remember { mutableStateOf<Set<Long>>(emptySet()) }

        AlertDialog(
            onDismissRequest = { showMoveEnabledDialog = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.92f),
            title = { Text("移动启用配置 (${enabledItems.size}个)") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("移动到：", modifier = Modifier.padding(bottom = 8.dp))
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val otherGroups = allGroups.filter { it.group.id != sourceGroup.id }
                        items(otherGroups, key = { "g_${it.group.id}" }) { gwt ->
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
                                            val maxOrder = gwt.list.maxOfOrNull { it.order } ?: -1
                                            enabledItems.forEachIndexed { idx, item ->
                                                dbm.systemTtsV2.update(item.copy(
                                                    groupId = grp.id,
                                                    categoryPath = "",
                                                    order = maxOrder + 1 + idx
                                                ))
                                            }
                                            SystemTtsService.notifyUpdateConfig()
                                            showMoveEnabledDialog = null
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
                                                val subItems = gwt.list.filter { it.categoryPath == path }
                                                val maxOrder = subItems.maxOfOrNull { it.order } ?: -1
                                                enabledItems.forEachIndexed { idx, item ->
                                                    dbm.systemTtsV2.update(item.copy(
                                                        groupId = grp.id,
                                                        categoryPath = path,
                                                        order = maxOrder + 1 + idx
                                                    ))
                                                }
                                                SystemTtsService.notifyUpdateConfig()
                                                showMoveEnabledDialog = null
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 56.dp)
                                    ) { Text(path) }
                                }
                            }
                        }
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            TextButton(
                                onClick = {
                                    newGroupNameForMove = ""
                                    showNewGroupForMove = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("新建大分组") }
                        }
                    }
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

    var showTagClearDialog by remember { mutableStateOf<SystemTtsV2?>(null) }
    if (showTagClearDialog != null) {
        val systts = showTagClearDialog!!
        val config = systts.config as TtsConfigurationDTO
        TagDataClearConfirmDialog(
            tagData = config.speechRule.tagData.toString(),
            onDismissRequest = { showTagClearDialog = null },
            onConfirm = {
                dbm.systemTtsV2.update(
                    systts.copy(
                        config = config.copy(
                            speechRule = config.speechRule.copy(
                                target = SpeechTarget.ALL,
                            ).apply { resetTag() },
                        )
                    )
                )
                if (systts.isEnabled) SystemTtsService.notifyUpdateConfig()
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

        dbm.systemTtsV2.update(systts.copy(config = systts.ttsConfig.copy(speechRule = ruleData)))
        if (systts.isEnabled) SystemTtsService.notifyUpdateConfig()
    }

    var deleteTts by remember { mutableStateOf<SystemTtsV2?>(null) }
    if (deleteTts != null) {
        ConfigDeleteDialog(
            onDismissRequest = { deleteTts = null }, content = deleteTts?.displayName ?: ""
        ) {
            dbm.systemTtsV2.delete(deleteTts!!)
            deleteTts = null
        }
    }

    var groupAudioParamsDialog by remember { mutableStateOf<SystemTtsGroup?>(null) }
    if (groupAudioParamsDialog != null) {
        GroupAudioParamsDialog(onDismissRequest = { groupAudioParamsDialog = null },
            params = groupAudioParamsDialog!!.audioParams,
            onConfirm = {
                dbm.systemTtsV2.updateGroup(
                    groupAudioParamsDialog!!.copy(audioParams = it)
                )
                // 通知服务更新配置
                SystemTtsService.notifyUpdateConfig()
                groupAudioParamsDialog = null
            })
    }

    val listState = rememberLazyListState()
    LazyListIndexStateSaver(models = models, listState = listState)

    val reorderState = rememberReorderableLazyListState(
        listState = listState, onMove = vm::reorder
    )

    var addGroupDialog by remember { mutableStateOf(false) }
    if (addGroupDialog) {
        var name by remember { mutableStateOf("") }
        TextFieldDialog(title = stringResource(id = R.string.add_group),
            text = name,
            onTextChange = { name = it },
            onDismissRequest = { addGroupDialog = false }) {
            addGroupDialog = false
            dbm.systemTtsV2.insertGroup(SystemTtsGroup(name = name))
        }
    }

    var showGroupExportSheet by remember { mutableStateOf<List<GroupWithSystemTts>?>(null) }
    if (showGroupExportSheet != null) {
        val list = showGroupExportSheet!!
        ListExportBottomSheet(onDismissRequest = { showGroupExportSheet = null }, list = list)
    }

    var showExportSheet by remember { mutableStateOf<List<SystemTtsV2>?>(null) }
    if (showExportSheet != null) {
        val jStr = remember { AppConst.jsonBuilder.encodeToString(showExportSheet!!) }
        ConfigExportBottomSheet(json = jStr) { showExportSheet = null }
    }

    var addPluginDialog by remember { mutableStateOf(false) }
    if (addPluginDialog) {
        PluginSelectionDialog(onDismissRequest = { addPluginDialog = false }) {
            navigateToEdit(
                SystemTtsV2(
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

    var showAuditionDialog by remember { mutableStateOf<SystemTtsV2?>(null) }
    if (showAuditionDialog != null) AuditionDialog(systts = showAuditionDialog!!) {
        showAuditionDialog = null
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
                    if (isSearchMode) {
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
                    } else {
                        Text(stringResource(id = R.string.system_tts))
                    }
                }, actions = {
                    if (isSearchMode) {
                        IconButton(onClick = { 
                            isSearchMode = false 
                            vm.setSearchKeyword("")
                        }) {
                            Icon(Icons.Default.Close, stringResource(id = R.string.close))
                        }
                    } else {
                        IconButton(onClick = { showRestartDialog = true }) {
                            Icon(Icons.Default.Refresh, stringResource(id = R.string.restart))
                        }
                        IconButton(onClick = { isSearchMode = true }) {
                            Icon(Icons.Default.Search, stringResource(id = R.string.search))
                        }
                        IconButton(onClick = { showOptions = true }) {
                            Icon(Icons.Default.MoreVert, stringResource(id = R.string.more_options))
                            MenuMoreOptions(
                                expanded = showOptions,
                                onDismissRequest = { showOptions = false },
                                onExportAll = { showGroupExportSheet = models },
                            )
                        }
                    }
                })
        },
    ) { paddingValues ->
        Box(Modifier.padding(top = paddingValues.calculateTopPadding())) {
            ControlBottomBarVisibility(listState, LocalBottomBarBehavior.current)
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .reorderable(state = reorderState),
                state = listState
            ) {
                models.forEachIndexed { _, groupWithSystemTts ->
                    val g = groupWithSystemTts.group
                    val key = "g_${g.id}"
                    
                    val groupDragModifier = if (searchKeyword.isNotEmpty()) Modifier 
                                            else Modifier.detectReorderAfterLongPress(reorderState)

                    stickyHeader(key = key) {
                        val checkState =
                            groupWithSystemTts.list.filter { it.isEnabled }.size.sizeToToggleableState(
                                groupWithSystemTts.list.size
                            )
                        
                        ShadowedDraggableItem(reorderableState = reorderState, key = key) {
                            Group(modifier = groupDragModifier,
                                name = g.name,
                                group = g,
                                isExpanded = g.isExpanded,
                                toggleableState = checkState,
                                onToggleableStateChange = {
                                    vm.updateGroupEnable(groupWithSystemTts, it)
                                },
                                onClick = {
                                    dbm.systemTtsV2.updateGroup(g.copy(isExpanded = !g.isExpanded))
                                },
                                onDelete = {
                                    dbm.systemTtsV2.delete(*groupWithSystemTts.list.toTypedArray())
                                    dbm.systemTtsV2.deleteGroup(g)
                                },
                                onRename = {
                                    dbm.systemTtsV2.updateGroup(g.copy(name = it))
                                },
                                onCopy = {
                                    scope.launch {
                                        val group = g.copy(id = System.currentTimeMillis(),
                                            name = it.ifBlank { context.getString(R.string.unnamed) })
                                        dbm.systemTtsV2.insertGroup(group)
                                        dbm.systemTtsV2.getByGroup(g.id)
                                            .forEachIndexed { index, tts ->
                                                dbm.systemTtsV2.insert(
                                                    tts.copy(
                                                        id = System.currentTimeMillis() + index,
                                                        groupId = group.id
                                                    )
                                                )
                                            }
                                    }
                                },
                                onEditAudioParams = {
                                    groupAudioParamsDialog = g
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
                                onBatchAssignTags = {
                                    showBatchTagDialog = groupWithSystemTts.list
                                },
                                onReleaseSubGroup = {
                                    showReleaseSubGroup = g
                                },
                                onConvertToSubGroup = {
                                    showConvertToSubGroup = g
                                },
                                onExtractSubGroup = {
                                    showExtractSubGroup = g
                                },
                                onDeleteEnabled = {
                                    scope.launch {
                                        groupWithSystemTts.list.filter { it.isEnabled }.forEach {
                                            dbm.systemTtsV2.delete(it)
                                        }
                                        if (groupWithSystemTts.list.any { it.isEnabled })
                                            SystemTtsService.notifyUpdateConfig()
                                    }
                                },
                                onDeleteDisabled = {
                                    scope.launch {
                                        groupWithSystemTts.list.filter { !it.isEnabled }.forEach {
                                            dbm.systemTtsV2.delete(it)
                                        }
                                    }
                                },
                                onMoveEnabledToGroup = {
                                    showMoveEnabledDialog = groupWithSystemTts
                                },
                                onResortTagsByExisting = {
                                    scope.launch {
                                        resortTags(groupWithSystemTts.list, fromZero = false)
                                    }
                                },
                                onResortTagsFromZero = {
                                    scope.launch {
                                        resortTags(groupWithSystemTts.list, fromZero = true)
                                    }
                                },
                                onReassignTags = {
                                    reassignTagPrefix = ""
                                    showReassignTagDialog = groupWithSystemTts
                                }
                            )
                        }
                    }

                    if (g.isExpanded) {
                        val hasSubGroups = groupWithSystemTts.list.any { it.categoryPath.isNotBlank() }

                        if (!hasSubGroups) {
                            // 无子分组时保持原有扁平渲染（支持拖拽排序）
                            itemsIndexed(groupWithSystemTts.list.sortedBy { it.order },
                                key = { _, v -> "${g.id}_${v.id}" }) { _, item ->
                                if (g.id == 1L) println(item.displayName + ", " + item.order)

                                ShadowedDraggableItem(
                                    reorderableState = reorderState,
                                    key = "${g.id}_${item.id}"
                                ) {
                                    val descriptor = remember(item) {
                                        ItemDescriptorFactory.from(context, item)
                                    }
                                    Item(reorderState = reorderState,
                                        modifier = (if (searchKeyword.isNotEmpty()) Modifier
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
                                            showExportSheet =
                                                listOf(item.copy(groupId = AbstractListGroup.DEFAULT_GROUP_ID))
                                        },
                                        onMoveToSubGroup = {
                                            showMoveToSubGroup = item
                                        }
                                    )
                                }
                            }
                        } else {
                            // 有子分组时使用树形渲染
                            val tree = buildSubCategoryTree(groupWithSystemTts.list)
                            val flattened = flattenSubCategoryTree(tree)

                            // 过滤掉折叠的子分组内容
                            val visibleItems = mutableListOf<FlattenedCategoryItem>()
                            var skipLevel = Int.MAX_VALUE
                            for (fItem in flattened) {
                                when (fItem) {
                                    is FlattenedCategoryItem.SubGroupHeader -> {
                                        // 遇到同级或上级的 header 时重置跳过状态
                                        if (fItem.node.level <= skipLevel) {
                                            skipLevel = Int.MAX_VALUE
                                        }
                                        // 被跳过的子分组 header 不显示
                                        if (fItem.node.level > skipLevel) {
                                            continue
                                        }
                                        visibleItems.add(fItem)
                                        if (!expandedSubGroups.contains(fItem.node.fullPath)) {
                                            skipLevel = fItem.node.level
                                        }
                                    }
                                    is FlattenedCategoryItem.TtsItem -> {
                                        // 修复：displayLevel 必须 <= skipLevel 才显示，
                                        // 之前 <= skipLevel + 1 导致折叠子分组后其直接内容仍然显示
                                        if (fItem.displayLevel <= skipLevel) {
                                            visibleItems.add(fItem)
                                        }
                                    }
                                }
                            }

                            itemsIndexed(visibleItems,
                                key = { _, v ->
                                    when (v) {
                                        is FlattenedCategoryItem.SubGroupHeader -> "sub_${g.id}_${v.node.fullPath}"
                                        is FlattenedCategoryItem.TtsItem -> "item_${g.id}_${v.categoryPath}_${v.item.id}"
                                    }
                                }) { _, fItem ->
                                when (fItem) {
                                    is FlattenedCategoryItem.SubGroupHeader -> {
                                        val subKey = "sub_${g.id}_${fItem.node.fullPath}"
                                        val subDragModifier = if (searchKeyword.isNotEmpty()) Modifier
                                            else Modifier.detectReorderAfterLongPress(reorderState)
                                        ShadowedDraggableItem(
                                            reorderableState = reorderState,
                                            key = subKey
                                        ) { _ ->
                                            val subItems = fItem.node.items
                                            val subCheckState = subItems.filter { it.isEnabled }.size.sizeToToggleableState(subItems.size)
                                            SubGroupHeader(
                                                modifier = subDragModifier,
                                                name = fItem.node.name,
                                                level = fItem.node.level,
                                                isExpanded = expandedSubGroups.contains(fItem.node.fullPath),
                                                toggleableState = subCheckState,
                                                onToggleableStateChange = { enabled ->
                                                    scope.launch {
                                                        subItems.forEach { item ->
                                                            if (item.isEnabled != enabled) {
                                                                dbm.systemTtsV2.update(
                                                                    item.copy(isEnabled = enabled)
                                                                )
                                                            }
                                                        }
                                                        if (enabled) SystemTtsService.notifyUpdateConfig()
                                                    }
                                                },
                                                onClick = {
                                                    expandedSubGroups = if (expandedSubGroups.contains(fItem.node.fullPath)) {
                                                        expandedSubGroups - fItem.node.fullPath
                                                    } else {
                                                        expandedSubGroups + fItem.node.fullPath
                                                    }
                                                },
                                                onRename = {
                                                    showSubGroupRename = subItems to fItem.node.fullPath
                                                },
                                                onEditAudioParams = {
                                                    showSubGroupAudioParams = g to fItem.node.fullPath
                                                },
                                                onSort = {
                                                    showSortDialog = subItems to groupWithSystemTts.list
                                                },
                                                onBatchAssignTags = {
                                                    showSubGroupBatchTag = subItems
                                                },
                                                onDelete = {
                                                    scope.launch {
                                                        dbm.systemTtsV2.delete(*subItems.toTypedArray())
                                                    }
                                                },
                                                onExport = {
                                                    showExportSheet = subItems.map {
                                                        it.copy(groupId = AbstractListGroup.DEFAULT_GROUP_ID)
                                                    }
                                                },
                                                onExtractToGroup = {
                                                    showSubGroupExtractToGroup = g to fItem.node.fullPath
                                                }
                                            )
                                        }
                                    }
                                    is FlattenedCategoryItem.TtsItem -> {
                                        val item = fItem.item
                                        val itemKey = "item_${g.id}_${fItem.categoryPath}_${item.id}"
                                        val itemDragModifier = if (searchKeyword.isNotEmpty()) Modifier
                                            else Modifier.detectReorderAfterLongPress(reorderState)
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
                                                    showExportSheet =
                                                        listOf(item.copy(groupId = AbstractListGroup.DEFAULT_GROUP_ID))
                                                },
                                                onMoveToSubGroup = {
                                                    showMoveToSubGroup = item
                                                }
                                            )
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

            FloatingAddConfigButtonGroup(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                visible = true,
                addBgm = {
                    navigateToEdit(SystemTtsV2(config = BgmConfiguration()))
                },
                addLocal = {
                    navigateToEdit(
                        SystemTtsV2(
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
