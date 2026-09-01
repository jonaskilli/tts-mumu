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
import androidx.compose.material3.Switch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.github.jing332.database.entities.systts.JReadConfigMigration
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.ui.text.style.TextAlign
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
 * 按分组名一键分配标签时使用的固定关键词（性别 + 年龄段 + 主角 + 旁白），
 * 用于从分组名匹配出标签前缀（取最长匹配）。
 * 旁白为单一角色：整理时不带序号，统一 narration/旁白（见 reassignNarrationTags）。
 */
private val GROUP_TAG_KEYWORDS = listOf(
    "女童", "少女", "女青年", "女中年", "女老年",
    "男童", "少年", "男青年", "男中年", "男老年",
    "男主", "女主", "特殊男", "特殊女", "旁白"
)

/** 男主/特殊男/特殊女在朗读规则里不补零(男主1…男主20、特殊女1…)；女主仍两位补零(女主01…)，与规则一致 */
private val NO_ZERO_PAD_PREFIXES = setOf("男主", "特殊男", "特殊女")


private data class DetectedKeyword(val prefix: String, val zeroPad: Boolean)

/**
 * 分组树跨重组缓存：Room 全量重发产生全新 models 对象图，逐组内容签名未变的组直接复用
 * 上一轮树实例，树重建量从 O(全库) 降到 O(变化的组)。纯主线程访问（remember 块内），
 * 不涉及任何跨线程状态；容量超限整表重建防泄漏。
 */
private val groupTreeCache = java.util.concurrent.ConcurrentHashMap<Long, GroupDerivedEntry>()

/**
 * 逐组内容签名：组内 size/每项 id+order+categoryPath+enabled + paramsJson 参与摘要。
 * 任何增删改/排序/移动/标签改动都会变；分池与树缓存共用，签名未变即整组跳过重算。
 * 顺序敏感（order 参与），拖动排序后签名必然变化，与树内排序语义一致。
 */
private fun groupContentSignature(gwt: GroupWithSystemTts): Long {
    var sig: Long = 1125899906842597L
    sig = sig * 31 + gwt.list.size
    for (item in gwt.list) {
        sig = sig * 31 + item.id
        sig = sig * 31 + item.order.toLong()
        sig = sig * 31 + item.categoryPath.hashCode()
        sig = sig * 31 + item.isEnabled.hashCode()
    }
    sig = sig * 31 + gwt.group.subGroupAudioParamsJson.hashCode()
    return sig
}

/**
 * 分组派生缓存条目：签名 + 扁平树（null=无子分组）+ 是否高级池。
 * Room 全量重发后签名命中的组：树直接复用实例、分池直接复用结论，两者都零重算。
 */
private class GroupDerivedEntry(
    val signature: Long,
    val flattened: List<FlattenedCategoryItem>?,
    val isAdvanced: Boolean,
)


/**
 * 从分组名匹配固定关键词（取最长匹配）。
 * 返回前缀与是否补零：男主/特殊男/特殊女不补零，其余两位补零，与朗读规则一致。
 * 无匹配时回退为原分组名作为前缀。
 */
private fun detectTagKeyword(name: String): DetectedKeyword? {
    // 「女性少年/男性少年」是长名分组，不是「少年」组：明确排除，不触发关键词整理（用户指定仅「少年」名可触发）
    if (name.contains("女性少年") || name.contains("男性少年")) return null
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
    val isInitialized by vm.isInitialized.collectAsStateWithLifecycle()

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
    // 大分组展开集合：轻量态，切换不写库（写库会触发 Room 全量重发 → 分池/树重建卡顿）
    var expandedGroupIds by remember { AppConfig.expandedGroupIds }

    // 池划分：只看配置项的朗读标签——全部标签都是朗读规则标签表内的标签（女青年01/男主1/narration/括号2/localSound1 等，
    // 含能转换成这些的 jread 标签式；空白标签也算通用）→ 通用池；
    // 任一配置项标签为规则外标签（性格词、群杂式等）→ 整组高级池。分组名、子分组路径与参数字典键不参与判定
    // （曾改为 produceState 后台预计算，实测 jread 树形分组会整体不显示，回退同步计算保显示正确）。
    // 优化（纯主线程，无跨线程状态）：此处统一做"逐组签名 → 命中则整组复用树实例+池结论"，
    // 未命中才重算该组；结果写 groupTreeCache 供下方树块直接取用。
    // Room 全量重发的新 models 中内容未变的组零重算，数千项时刷新/展开不再整库陪跑
    val (advancedPoolModels, normalPoolModels) = remember(models) {
        val advanced = mutableListOf<GroupWithSystemTts>()
        val normal = mutableListOf<GroupWithSystemTts>()
        val nextCache = HashMap<Long, GroupDerivedEntry>(groupTreeCache.size)
        for (gwt in models) {
            val sig = groupContentSignature(gwt)
            val cached = groupTreeCache[gwt.group.id]
            val entry: GroupDerivedEntry = if (cached != null && cached.signature == sig) {
                cached
            } else {
                val subPaths: Set<String> = gwt.group.subGroupAudioParamsJson.let { jsonStr ->
                    if (jsonStr.isBlank() || jsonStr == "{}") emptySet()
                    else SystemTtsV2.Converters.json.decodeFromString<Map<String, AudioParams>>(jsonStr).keys
                }
                val hasSubInList = gwt.list.any { it.categoryPath.isNotBlank() }
                val flat = if (hasSubInList || subPaths.isNotEmpty())
                    flattenSubCategoryTree(buildSubCategoryTree(gwt.list, subPaths))
                else null
                // 空列表组视为通用池（无标签可判）
                val isAdvanced = gwt.list.any {
                    val tag = (it.config as? TtsConfigurationDTO)?.speechRule?.tag ?: ""
                    !JReadConfigMigration.isNormalTag(tag)
                }
                GroupDerivedEntry(sig, flat, isAdvanced)
            }
            nextCache[gwt.group.id] = entry
            if (entry.isAdvanced) advanced.add(gwt) else normal.add(gwt)
        }
        // 清理已删除分组的缓存，防大库下无界增长
        groupTreeCache.clear()
        groupTreeCache.putAll(nextCache)
        advanced to normal
    }
    // 当前所在的池页签：false=通用池，true=高级池
    var showAdvancedPool by rememberSaveable { mutableStateOf(false) }
    // 只有一池有内容时直接展示该池；两池都有内容才显示页签
    val hasAdvancedPool = advancedPoolModels.isNotEmpty()
    val hasNormalPool = normalPoolModels.isNotEmpty()
    val showPoolTabs = hasAdvancedPool && hasNormalPool
    val effectiveAdvanced = showAdvancedPool || !hasNormalPool
    val displayedModels =
        if (effectiveAdvanced && hasAdvancedPool) advancedPoolModels else normalPoolModels

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
    var showSubGroupRename by remember { mutableStateOf<Triple<SystemTtsGroup, List<SystemTtsV2>, String>?>(null) }
    if (showSubGroupRename != null) {
        val (sgroup, items, oldPath) = showSubGroupRename!!
        var newName by remember(oldPath) { mutableStateOf(oldPath.substringAfterLast('/')) }
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
                    // 子分组定义与音频参数键同步迁移（空子分组仅有键）
                    val subMap = sgroup.subGroupAudioParamsJson.let { jsonStr ->
                        if (jsonStr.isBlank() || jsonStr == "{}") emptyMap<String, AudioParams>()
                        else SystemTtsV2.Converters.json.decodeFromString<Map<String, AudioParams>>(jsonStr)
                    }
                    if (subMap.keys.any { it == oldPath || it.startsWith("$oldPath/") }) {
                        val newSubMap = subMap.entries.associate { (k, v) ->
                            when {
                                k == oldPath -> newPath to v
                                k.startsWith("$oldPath/") -> newPath + k.removePrefix(oldPath) to v
                                else -> k to v
                            }
                        }
                        dbm.systemTtsV2.updateGroup(
                            sgroup.copy(subGroupAudioParamsJson = SystemTtsV2.Converters.json.encodeToString(newSubMap))
                        )
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
    ): Int {
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
                // 标签已正确则跳过，避免大规模分组(数百项)全量重算 JS 与无效写库
                if (config.speechRule.tag == newTag) return@forEachIndexed
                val newRule = config.speechRule.copy(tag = newTag)
                computeTagNameOrFallback(context, newRule, newTag, ruleCache, engineCache)
                toUpdate.add(item.copy(config = config.copy(speechRule = newRule)))
            }
        }
        if (toUpdate.isNotEmpty()) {
            dbm.systemTtsV2.update(*toUpdate.toTypedArray())
            SystemTtsService.notifyUpdateConfig()
        }
        return toUpdate.size
    }

    /**
     * 旁白整理：旁白为单一角色不带序号，组内全部配置统一打 narration（显示名 旁白）。
     * 标签名固定，无需 JS 评估；已正确的项跳过。
     * 防误转：只统一旁白族（旁白X/narration）与空白标签的项；
     * 人物标签（如 jread 的 女青年01）即使出现在旁白命名的分组里也不改，避免一键整理毁掉人物标签。
     */
    suspend fun reassignNarrationTags(list: List<SystemTtsV2>): Int {
        val toUpdate = mutableListOf<SystemTtsV2>()
        list.filter { it.config is TtsConfigurationDTO }.forEach { item ->
            val config = item.config as TtsConfigurationDTO
            val tag = config.speechRule.tag
            if (tag == "narration") return@forEach
            val isNarrationFamily = tag.isBlank() ||
                tag.equals("narration", true) || tag.startsWith("旁白")
            if (!isNarrationFamily) return@forEach
            val newRule = config.speechRule.copy(tag = "narration", tagName = "旁白")
            toUpdate.add(item.copy(config = config.copy(speechRule = newRule)))
        }
        if (toUpdate.isNotEmpty()) {
            dbm.systemTtsV2.update(*toUpdate.toTypedArray())
            SystemTtsService.notifyUpdateConfig()
        }
        return toUpdate.size
    }

    /**
     * 按分组名一键分配标签：从分组名匹配固定关键词(女童/少女/…/男主/女主/旁白)，
     * 用该关键词作为前缀编号。男主不补零，其余两位补零；旁白不带序号统一 narration。
     * @return 实际整理的标签数量
     */
    suspend fun reassignTagsByGroupName(list: List<SystemTtsV2>, groupName: String): Int {
        val detected = detectTagKeyword(groupName) ?: return 0
        if (detected.prefix == "旁白") return reassignNarrationTags(list)
        return reassignTagsWithPrefix(list, detected.prefix, detected.zeroPad)
    }

    /**
     * 整理某大分组下全部子分组：每个子分组按其名称匹配关键词后重新编号。
     * 仅处理名称含关键词的子分组（无关键词的子分组跳过，避免误改）。
     * 优化：收集所有子分组的更新后一次性批量写入数据库，提升整理速度。
     * 性能优化：按 ruleId 分组并行评估 JS（不同 ruleId 各自独立 engine，
     * 可并行；同 ruleId 复用同一 engine 串行调用，Rhino 非线程安全）。
     */
    suspend fun reassignTagsForAllSubGroups(list: List<SystemTtsV2>): Int {
        val tree = buildSubCategoryTree(list)
        val flattened = flattenSubCategoryTree(tree)
        // 旁白子分组单一角色不带序号，先统一处理并从编号流程中排除
        val narrationItems = flattened.filterIsInstance<FlattenedCategoryItem.SubGroupHeader>()
            .filter { detectTagKeyword(it.node.name)?.prefix == "旁白" }
            .flatMap { it.node.allItems }
        val narrationCount = if (narrationItems.isEmpty()) 0
        else reassignNarrationTags(narrationItems)
        // 先收集所有待处理项 (item, newRule, fallbackTag)
        data class PendingUpdate(val item: SystemTtsV2, val newRule: com.github.jing332.database.entities.systts.SpeechRuleInfo, val fallback: String)
        val pending = mutableListOf<PendingUpdate>()
        flattened.filterIsInstance<FlattenedCategoryItem.SubGroupHeader>().forEach { header ->
            val detected = detectTagKeyword(header.node.name) ?: return@forEach
            if (detected.prefix == "旁白") return@forEach  // 已由 reassignNarrationTags 处理
            val subItems = header.node.allItems.filter { it.config is TtsConfigurationDTO }
                .sortedBy { it.order }
            if (subItems.isEmpty()) return@forEach
            subItems.forEachIndexed { idx, item ->
                val seq = if (detected.zeroPad) String.format("%02d", idx + 1) else (idx + 1).toString()
                val newTag = detected.prefix + seq
                val config = item.config as TtsConfigurationDTO
                // 标签已正确则跳过，避免大规模分组(数百项)全量重算 JS 与无效写库
                if (config.speechRule.tag == newTag) return@forEachIndexed
                val newRule = config.speechRule.copy(tag = newTag)
                pending.add(PendingUpdate(item, newRule, newTag))
            }
        }
        if (pending.isEmpty()) return 0

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
        if (allUpdates.isEmpty() && narrationCount == 0) return 0
        if (allUpdates.isNotEmpty()) {
            dbm.systemTtsV2.update(*allUpdates.toTypedArray())
            SystemTtsService.notifyUpdateConfig()
        }
        return allUpdates.size + narrationCount
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
     * 子分组前缀一键修改：仅替换 categoryPath 第一段开头的匹配文字（查找/替换式）。
     * - 查找为空 = 给所有子分组加前缀；替换为空 = 去掉匹配前缀；都非空 = 换前缀
     * - 只碰第一段开头，多级路径(如 女青年/软萌)的后续层级不动
     * - subGroupAudioParamsJson 的键同步迁移，音频参数不丢
     * @return 实际改名的子分类数量
     */
    suspend fun renameSubGroupPrefix(
        group: SystemTtsGroup,
        list: List<SystemTtsV2>,
        find: String,
        replace: String,
    ): Int {
        if (find == replace) return 0
        val affected = list.filter { it.config is TtsConfigurationDTO && it.categoryPath.isNotBlank() }

        // 候选首段 = 配置项路径 ∪ JSON 键，空子分组（仅存在于键中）也参与改名
        val jsonSubPaths = group.subGroupAudioParamsJson.let { jsonStr ->
            if (jsonStr.isBlank() || jsonStr == "{}") emptyList()
            else SystemTtsV2.Converters.json.decodeFromString<Map<String, AudioParams>>(jsonStr).keys.toList()
        }
        // 旧第一段 -> 新第一段 的映射（仅含发生变化的）
        val segRename = mutableMapOf<String, String>()
        ((affected.map { it.categoryPath } + jsonSubPaths).mapNotNull { p ->
            val firstSlash = p.indexOf('/')
            if (firstSlash == -1) p else p.substring(0, firstSlash)
        }).distinct().forEach { seg ->
            val newSeg = when {
                find.isEmpty() -> replace + seg
                seg.startsWith(find) -> replace + seg.removePrefix(find)
                else -> null
            } ?: return@forEach
            // 新名为空=整个子分组被清名，跳过防止路径/键损坏
            if (newSeg.isNotBlank() && newSeg != seg) segRename[seg] = newSeg
        }
        if (segRename.isEmpty()) return 0

        // 配置项 categoryPath 整体替换第一段
        val updates = affected.mapNotNull { item ->
            val path = item.categoryPath
            val firstSlash = path.indexOf('/')
            val seg = if (firstSlash == -1) path else path.substring(0, firstSlash)
            val newSeg = segRename[seg] ?: return@mapNotNull null
            val newPath = if (firstSlash == -1) newSeg else newSeg + path.substring(firstSlash)
            item.copy(categoryPath = newPath)
        }
        if (updates.isNotEmpty()) {
            dbm.systemTtsV2.update(*updates.toTypedArray())
        }

        // 音频参数键同步迁移
        val subMap = group.subGroupAudioParamsJson.let { jsonStr ->
            if (jsonStr.isBlank() || jsonStr == "{}") emptyMap()
            else SystemTtsV2.Converters.json.decodeFromString<Map<String, AudioParams>>(jsonStr)
        }.toMutableMap()
        var subChanged = false
        segRename.forEach { (oldSeg, newSeg) ->
            // 键可能以第一段开头(多级路径)或恰为第一段
            subMap.keys.toList().forEach { key ->
                val newKey = when {
                    key == oldSeg -> newSeg
                    key.startsWith("$oldSeg/") -> newSeg + key.removePrefix(oldSeg)
                    else -> null
                }
                if (newKey != null && newKey !in subMap) {
                    subMap[newKey] = subMap.remove(key)!!
                    subChanged = true
                }
            }
        }
        if (subChanged) {
            dbm.systemTtsV2.updateGroup(
                group.copy(subGroupAudioParamsJson = SystemTtsV2.Converters.json.encodeToString(subMap))
            )
        }
        SystemTtsService.notifyUpdateConfig()
        return segRename.size
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
                            } else {
                                // 空子分组：把名字注册到 subGroupAudioParamsJson（仅当该子分组尚未定义时），
                                // 使其作为空子分组头出现，之后可往里拖入音色；不覆盖已存在的音频参数
                                val subMap = targetGroup.subGroupAudioParamsJson.let { jsonStr ->
                                    if (jsonStr.isBlank() || jsonStr == "{}") emptyMap<String, AudioParams>()
                                    else SystemTtsV2.Converters.json.decodeFromString<Map<String, AudioParams>>(jsonStr)
                                }
                                if (!subMap.containsKey(subGroupName)) {
                                    val newSubMap = subMap.toMutableMap().apply { put(subGroupName, AudioParams()) }
                                    dbm.systemTtsV2.updateGroup(
                                        targetGroup.copy(
                                            subGroupAudioParamsJson = SystemTtsV2.Converters.json.encodeToString(newSubMap)
                                        )
                                    )
                                }
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
        val hasSubGroups = (currentGroupWithTts?.list?.any { it.categoryPath.isNotBlank() } == true) ||
            targetGroup.subGroupAudioParamsJson.let { it.isNotBlank() && it != "{}" }
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
                        if (otherGroups.isEmpty()) {
                            Text("没有其他分组可选，请先创建其他分组。", color = MaterialTheme.colorScheme.outline)
                        } else {
                            Text("选择目标分组，当前分组将作为其子分组：", modifier = Modifier.padding(bottom = 8.dp))
                        }
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
                                            // 转为子分组后删除原分组（含空分组）
                                            dbm.systemTtsV2.deleteGroup(targetGroup)
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

    // 合并同类配置项到其他分组：源分组配置项整体迁入目标分组并按关键词归一化，迁完删除源分组
    var showMergeGroup by remember { mutableStateOf<SystemTtsGroup?>(null) }
    var showRenamePrefix by remember { mutableStateOf<SystemTtsGroup?>(null) }
    var mergeInsertFront by remember { mutableStateOf(false) }
    if (showMergeGroup != null) {
        val sourceGroup = showMergeGroup!!
        val sourceGwt = models.find { it.group.id == sourceGroup.id }
        val otherGroups = remember(models, sourceGroup.id) {
            models.filter { it.group.id != sourceGroup.id }.map { it.group }
        }
        AlertDialog(
            onDismissRequest = { showMergeGroup = null },
            title = { Text("合并同类配置项到其他分组") },
            text = {
                Column {
                    Text(
                        "子分组与目标分组有同类型标签则并入对应子分组，无则整个移入目标分组。请选择目标分组：",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Switch(checked = mergeInsertFront, onCheckedChange = { mergeInsertFront = it })
                        Text("插入到开头", modifier = Modifier.padding(start = 4.dp))
                    }
                    otherGroups.forEach { targetGroup ->
                        TextButton(
                            onClick = {
                                showMergeGroup = null
                                showTagOrganizeLoading = true
                                val insertFront = mergeInsertFront
                                scope.launch {
                                    withIO {
                                        val sourceItems = sourceGwt?.list ?: emptyList()
                                        val targetGwt = models.find { it.group.id == targetGroup.id }
                                        // 全量迁移：源分组所有配置项整体搬入目标分组；
                                        // categoryPath 按关键词归一化映射到目标同分类（如"纳米/女青年"→"剪映/女青年"），
                                        // 无同关键词分类的项保留原路径（作为目标分组下的新子分类），
                                        // 标签由迁移后的统一重编号理顺
                                        if (sourceItems.isNotEmpty()) {
                                            val kwToTargetPath = mutableMapOf<String, String>()
                                            targetGwt?.list?.forEach { item ->
                                                val kw = detectTagKeyword(item.categoryPath)?.prefix
                                                if (kw != null && kw !in kwToTargetPath) {
                                                    kwToTargetPath[kw] = item.categoryPath
                                                }
                                            }
                                            // 位置：末尾追加 or 插入到开头
                                            val baseOrder = if (insertFront)
                                                (targetGwt?.list?.minOfOrNull { it.order } ?: 0) - sourceItems.size
                                            else
                                                (targetGwt?.list?.maxOfOrNull { it.order } ?: -1) + 1
                                            val updates = sourceItems.mapIndexed { i, item ->
                                                val kw = detectTagKeyword(item.categoryPath)?.prefix
                                                val newPath = kw?.let { kwToTargetPath[it] } ?: item.categoryPath
                                                item.copy(
                                                    groupId = targetGroup.id,
                                                    order = baseOrder + i,
                                                    categoryPath = newPath
                                                )
                                            }
                                            dbm.systemTtsV2.update(*updates.toTypedArray())
                                            // 仅对受影响的分类重新编号，避免无关分类被改动
                                            val affectedPaths = updates.map { it.categoryPath }.toSet()
                                            val combined = (targetGwt?.list ?: emptyList()) + updates
                                            reassignTagsForAllSubGroups(combined.filter { it.categoryPath in affectedPaths })
                                        }
                                        // 子分组定义与音频参数并入目标（键不覆盖目标已有，空子分组定义不丢）
                                        val srcSubMap = sourceGroup.subGroupAudioParamsJson.let { jsonStr ->
                                            if (jsonStr.isBlank() || jsonStr == "{}") emptyMap()
                                            else SystemTtsV2.Converters.json.decodeFromString<Map<String, AudioParams>>(jsonStr)
                                        }
                                        if (srcSubMap.isNotEmpty()) {
                                            val dstSubMap = targetGroup.subGroupAudioParamsJson.let { jsonStr ->
                                                if (jsonStr.isBlank() || jsonStr == "{}") mutableMapOf<String, AudioParams>()
                                                else SystemTtsV2.Converters.json.decodeFromString<Map<String, AudioParams>>(jsonStr).toMutableMap()
                                            }
                                            srcSubMap.forEach { (k, v) -> if (k !in dstSubMap) dstSubMap[k] = v }
                                            dbm.systemTtsV2.updateGroup(
                                                targetGroup.copy(subGroupAudioParamsJson = SystemTtsV2.Converters.json.encodeToString(dstSubMap))
                                            )
                                        }
                                        // 源分组整体迁空后必删
                                        dbm.systemTtsV2.deleteGroup(sourceGroup)
                                        withContext(Dispatchers.Main) {
                                            showTagOrganizeLoading = false
                                            context.toast("已合并 ${sourceItems.size} 项到「${targetGroup.name}」，空分组已删除")
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

    // 修改子分组前缀：查找/替换式批量改名，实时预览
    if (showRenamePrefix != null) {
        val renameGroup = showRenamePrefix!!
        val renameGwt = models.find { it.group.id == renameGroup.id }
        var newText by remember(renameGroup.id) { mutableStateOf("") }
        // 候选首段（配置项路径 ∪ JSON 键），空子分组也参与
        val baseSegs = remember(renameGwt) {
            val jsonPaths = renameGwt?.group?.subGroupAudioParamsJson?.let { jsonStr ->
                if (jsonStr.isBlank() || jsonStr == "{}") emptyList()
                else SystemTtsV2.Converters.json.decodeFromString<Map<String, AudioParams>>(jsonStr).keys.toList()
            } ?: emptyList()
            ((renameGwt?.list?.map { it.categoryPath } ?: emptyList()) + jsonPaths)
                .filter { it.isNotBlank() }
                .map { p -> if (p.indexOf('/') == -1) p else p.substring(0, p.indexOf('/')) }
                .distinct()
                .sorted()
        }
        // 自动识别最长公共前缀：非空则只替换该段，为空则视为加前缀模式
        val detectedLcp = remember(baseSegs) {
            if (baseSegs.isEmpty()) ""
            else baseSegs.reduce { a, b ->
                var i = 0
                while (i < a.length && i < b.length && a[i] == b[i]) i++
                a.substring(0, i)
            }
        }
        // 删前缀会清空整个名称的子分组（首段恰为前缀本身）需跳过并提示
        val removalBlocked = detectedLcp.isNotEmpty() && newText.trim().isEmpty() &&
            baseSegs.any { it == detectedLcp }
        val previewRenames = remember(baseSegs, detectedLcp, newText) {
            val t = newText.trim()
            baseSegs.mapNotNull { seg ->
                val newSeg = when {
                    detectedLcp.isNotEmpty() && seg.startsWith(detectedLcp) -> t + seg.removePrefix(detectedLcp)
                    detectedLcp.isEmpty() && t.isNotEmpty() -> t + seg
                    else -> null
                }
                if (!newSeg.isNullOrBlank() && newSeg != seg) seg to newSeg else null
            }.toMap()
        }
        AlertDialog(
            onDismissRequest = { showRenamePrefix = null },
            title = { Text("修改子分组前缀") },
            text = {
                Column {
                    Text(
                        if (detectedLcp.isEmpty())
                            "未检测到公共前缀，输入的文字将作为统一前缀加到所有子分组开头："
                        else
                            "已自动识别公共前缀「${detectedLcp}」（共 ${baseSegs.size} 个子分组），只需输入新前缀：",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = newText,
                        onValueChange = { newText = it },
                        label = {
                            Text(
                                if (detectedLcp.isEmpty()) "要添加的前缀"
                                else "新前缀（留空则去掉「${detectedLcp}」）"
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (removalBlocked) {
                        Text(
                            "有子分组的完整名称就是「${detectedLcp}」，删除前缀将跳过它们",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else if (previewRenames.isNotEmpty()) {
                        Text(
                            "将修改 ${previewRenames.size} 个子分组：",
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Column(modifier = Modifier.heightIn(max = 200.dp)) {
                            previewRenames.forEach { (old, new) ->
                                Text(
                                    "$old → $new",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    } else if (newText.isNotBlank()) {
                        Text(
                            "没有需要修改的子分组",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = previewRenames.isNotEmpty(),
                    onClick = {
                        showRenamePrefix = null
                        showTagOrganizeLoading = true
                        val newPrefix = newText.trim()
                        scope.launch {
                            val count = withContext(Dispatchers.IO) {
                                renameSubGroupPrefix(
                                    renameGroup,
                                    renameGwt?.list ?: emptyList(),
                                    detectedLcp,
                                    newPrefix
                                )
                            }
                            showTagOrganizeLoading = false
                            context.toast("已修改 $count 个子分组名")
                        }
                    }
                ) { Text("执行") }
            },
            dismissButton = {
                TextButton(onClick = { showRenamePrefix = null }) {
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
            // 候选并入 JSON 键，空子分组也可被转出为一级分组
            val jsonPaths = sourceGwt?.group?.subGroupAudioParamsJson?.let { jsonStr ->
                if (jsonStr.isBlank() || jsonStr == "{}") emptyList()
                else SystemTtsV2.Converters.json.decodeFromString<Map<String, AudioParams>>(jsonStr).keys.toList()
            } ?: emptyList()
            ((sourceGwt?.list?.map { it.categoryPath } ?: emptyList()) + jsonPaths)
                .filter { it.isNotBlank() }.distinct().sorted()
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

    // 多选删除分组：简单确认对话框（直接删除已选中的分组，不再重复选择）
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    if (showDeleteSelectedDialog) {
        val deleteTargets = models.filter { it.group.id in selectedGroupIds }
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = { Text("删除分组") },
            text = { Text("确定删除选中的 ${deleteTargets.size} 个分组及其音色配置吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteSelectedDialog = false
                    showTagOrganizeLoading = true
                    scope.launch {
                        withIO {
                            deleteTargets.forEach { gwt ->
                                dbm.systemTtsV2.delete(*gwt.list.toTypedArray())
                                dbm.systemTtsV2.deleteGroup(gwt.group)
                            }
                        }
                        selectedGroupIds = emptySet()
                        selectionMode = false
                        showTagOrganizeLoading = false
                    }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
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
                            }
                            // 操作按钮放第二行右对齐，第一行完整展示分组名与数量
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
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
                // 多选模式底部操作栏：全选 + 删除 + 转为子分组（条件显示，范围限定当前池）
                val selectedGroups = displayedModels.filter { it.group.id in selectedGroupIds }
                val allSelected = displayedModels.isNotEmpty() &&
                    selectedGroupIds.size == displayedModels.size
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
                            selectedGroupIds = if (allSelected) emptySet() else displayedModels.map { it.group.id }.toSet()
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
        // 树结构缓存：上方分池块已按"逐组签名→命中复用/未命中重算"写好 groupTreeCache，
        // 此处直接取每组的扁平树实例（签名命中时与上一轮同一实例，树身份跨重组稳定）。
        // 注意：空子分组仅靠 subGroupAudioParamsJson 注册（签名已含该字段），
        // 新建空子分组时签名必变、树必重建，不会被漏显示
        val subGroupTrees = remember(models) {
            HashMap<Long, List<FlattenedCategoryItem>?>(models.size).apply {
                for (gwt in models) put(gwt.group.id, groupTreeCache[gwt.group.id]?.flattened)
            }
        }
        // 可见项过滤：轻量操作，每次展开/折叠时执行（仅遍历已缓存的扁平树做过滤，不重建树）
        val subGroupVisibleItemsMap = remember(models, expandedSubGroups, expandedGroupIds) {
            models.associate { gwt ->
                val g = gwt.group
                val flattened = subGroupTrees[g.id]
                if (expandedGroupIds.contains(g.id.toString()) && flattened != null) {
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
        Column(Modifier.fillMaxSize()) {
        // 池页签：通用池 / 高级池；切换时清空搜索与多选状态，保证各池操作独立
        // 仅一池有内容时不显示页签，直接展示该池
        if (showPoolTabs) TabRow(selectedTabIndex = if (effectiveAdvanced) 1 else 0) {
            Tab(
                selected = !effectiveAdvanced,
                onClick = {
                    if (showAdvancedPool) {
                        showAdvancedPool = false
                        if (isSearchMode) { isSearchMode = false; vm.setSearchKeyword("") }
                        if (selectionMode) { selectionMode = false; selectedGroupIds = emptySet() }
                        scope.launch { listState.scrollToItem(0) }
                    }
                },
                text = { Text("通用池 (${normalPoolModels.size})") }
            )
            Tab(
                selected = effectiveAdvanced,
                onClick = {
                    if (!showAdvancedPool) {
                        showAdvancedPool = true
                        if (isSearchMode) { isSearchMode = false; vm.setSearchKeyword("") }
                        if (selectionMode) { selectionMode = false; selectedGroupIds = emptySet() }
                        scope.launch { listState.scrollToItem(0) }
                    }
                },
                text = { Text("高级池 (${advancedPoolModels.size})") }
            )
        }
        if (displayedModels.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (!isInitialized) {
                    // 首次数据库查询完成前显示加载中；否则「暂无分组」会闪一下再变出列表，像是不稳定
                    CircularProgressIndicator()
                } else {
                    Text(
                        if (showAdvancedPool)
                            "暂无高级池分组\n含规则外朗读标签（如性格词、群杂式）的分组会自动显示在这里"
                        else "暂无通用池分组",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .reorderable(state = reorderState),
                state = listState
            ) {
                displayedModels.forEachIndexed { groupIndex, groupWithSystemTts ->
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
                                isExpanded = expandedGroupIds.contains(g.id.toString()),
                                toggleableState = checkState,
                                onToggleableStateChange = {
                                    vm.updateGroupEnable(groupWithSystemTts, it)
                                },
                                onClick = {
                                    val wasExpanded = expandedGroupIds.contains(g.id.toString())
                                    expandedGroupIds = if (wasExpanded) expandedGroupIds - g.id.toString() else expandedGroupIds + g.id.toString()
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
                                hasSubGroups = groupWithSystemTts.list.any { it.categoryPath.isNotBlank() } ||
                                    g.subGroupAudioParamsJson.let { it.isNotBlank() && it != "{}" },
                                hasTagKeyword = detectTagKeyword(g.name) != null,
                                hasSubGroupTagKeyword = groupWithSystemTts.list
                                    .filter { it.categoryPath.isNotBlank() }
                                    .any { detectTagKeyword(it.categoryPath) != null },
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
                                            when {
                                                count == 0 -> context.toast("标签均已正确，无需整理")
                                                else -> context.toast("已按「${detected.prefix}」重新编号 $count 个标签")
                                            }
                                        }
                                    }
                                },
                                onReassignAllSubGroups = {
                                    val subPaths = groupWithSystemTts.list
                                        .filter { it.categoryPath.isNotBlank() }
                                        .map { it.categoryPath }.distinct()
                                    when {
                                        subPaths.isEmpty() ->
                                            context.toast("该分组下没有子分组")
                                        subPaths.none { detectTagKeyword(it) != null } ->
                                            context.toast("子分组名未包含关键词，无法自动整理")
                                        else -> {
                                            showTagOrganizeLoading = true
                                            scope.launch {
                                                val count = withContext(Dispatchers.IO) {
                                                    reassignTagsForAllSubGroups(groupWithSystemTts.list)
                                                }
                                                showTagOrganizeLoading = false
                                                when {
                                                    count == 0 -> context.toast("标签均已正确，无需整理")
                                                    else -> context.toast("已按各子分组关键词重新编号 $count 个标签")
                                                }
                                            }
                                        }
                                    }
                                },
                                onMergeGroup = {
                                    showMergeGroup = g
                                },
                                onRenameSubPrefix = {
                                    showRenamePrefix = g
                                },
                                // 搜索过滤时序号会失真，隐藏
                                index = if (searchKeyword.isEmpty()) groupIndex + 1 else -1,
                                inSelectionMode = selectionMode,
                                isSelected = remember(g.id) { derivedStateOf { g.id in selectedGroupIds } }.value,
                                onToggleSelect = {
                                    selectedGroupIds = if (g.id in selectedGroupIds)
                                        selectedGroupIds - g.id else selectedGroupIds + g.id
                                }
                            )
                        }
                    }

                    if (expandedGroupIds.contains(g.id.toString())) {
                        val hasSubGroups = groupWithSystemTts.list.any { it.categoryPath.isNotBlank() } ||
                            g.subGroupAudioParamsJson.let { it.isNotBlank() && it != "{}" }

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
                                    val descriptor = remember(item, pluginNameCache) {
                                        ItemDescriptorFactory.from(context, item, pluginNameCache)
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
                                                        showSubGroupRename = Triple(g, subItems, fItem.node.fullPath)
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
                                                                when {
                                                                    count == 0 -> context.toast("标签均已正确，无需整理")
                                                                    else -> context.toast("已按「${detected.prefix}」重新编号 $count 个标签")
                                                                }
                                                            }
                                                        }
                                                    },
                                                    onDelete = {
                                                        scope.launch {
                                                            withIO {
                                                                if (subItems.isNotEmpty())
                                                                    dbm.systemTtsV2.delete(*subItems.toTypedArray())
                                                                // 子分组不是独立记录：删光配置项后还剩
                                                                // subGroupAudioParamsJson 里的键（空子分组仅靠键存在），
                                                                // 不清键则空壳子分组头永远显示=「分组删不掉」。
                                                                // 同步移除该路径精确键与 path/ 前缀的子孙键
                                                                val fullPath = fItem.node.fullPath
                                                                val subMap = g.subGroupAudioParamsJson.let { jsonStr ->
                                                                    if (jsonStr.isBlank() || jsonStr == "{}") emptyMap()
                                                                    else SystemTtsV2.Converters.json
                                                                        .decodeFromString<Map<String, AudioParams>>(jsonStr)
                                                                }
                                                                val staleKeys = subMap.keys.filter { it == fullPath || it.startsWith("$fullPath/") }
                                                                if (staleKeys.isNotEmpty()) {
                                                                    dbm.systemTtsV2.updateGroup(
                                                                        g.copy(subGroupAudioParamsJson = SystemTtsV2.Converters.json
                                                                            .encodeToString(subMap.filterKeys { it !in staleKeys.toSet() }))
                                                                    )
                                                                }
                                                            }
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
                                                    itemCount = subItems.size,
                                                    hasSubGroups = fItem.node.children.isNotEmpty()
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
                                                val descriptor = remember(item, pluginNameCache) {
                                                    ItemDescriptorFactory.from(context, item, pluginNameCache)
                                                }
                                                Item(
                                                    reorderState = reorderState,
                                                    modifier = itemDragModifier.padding(
                                                        // 配置项随所属子分组层级缩进，与二级及更深的子分组头对齐
                                                        start = (8 + (fItem.displayLevel - 1).coerceAtLeast(0) * 12).dp,
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
