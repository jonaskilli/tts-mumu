package com.github.jing332.tts_server_android.compose.systts

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import com.github.jing332.tts_server_android.compose.nav.NavTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.CompositionLocalProvider
import com.github.jing332.common.LogEntry
import com.github.jing332.common.LogLevel
import com.github.jing332.common.utils.toast
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.role.FlatTextAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.withFrameNanos
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun TtsLogScreen(vm: TtsLogViewModel = viewModel()) {
    val context = LocalContext.current
    // 剪贴板：多选「复制(N)」用（1002）
    val clipboard = LocalClipboardManager.current
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    // 日志文件列表弹窗（用户 09-08：文件夹点开自由选择文件，不再直接扔给外部查看器）
    var showLogFilesDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    // 「只看匹配」退役（10-08 log-ui-1008 ②拍板）：搜索即筛选——输入立刻只剩
    // 匹配条目，不再两段式开关绕弯；跳转目标直接在过滤后的列表里
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun LogEntry.matchesQuery(q: String): Boolean =
        message.contains(q, ignoreCase = true) || time.contains(q, ignoreCase = true)

    // 基础列表 = 等级/插件筛选结果；搜索词非空即按关键字实时过滤（10-08 ②拍板：
    // 打字列表跟着缩，不再是「搜完只跳转、列表纹丝不动」）
    val displayLogs by remember(vm) {
        derivedStateOf {
            val q = searchQuery.trim()
            if (q.isNotEmpty())
                vm.filteredLogs.filter { it.matchesQuery(q) }
            else
                vm.filteredLogs
        }
    }

    // 匹配统计(控制行展示用)
    val matchCount by remember(vm) {
        derivedStateOf {
            val q = searchQuery.trim()
            if (q.isEmpty()) 0
            else displayLogs.count { it.matchesQuery(q) }
        }
    }

    // 归组结果（10-06 全链卡）：与 LogScreen 共用同一实例——搜索跳转要把条目下标
    // 换算成列表项下标（一张卡=一个列表项），必须用同一份归组才不会错位
    val logGroups = remember(displayLogs) { LogGroups.build(displayLogs) }

    // ——「原文」定位（用户 10-08 午后拍板 甲方案）——
    // 筛选/搜索态下列表是收窄视图；点卡上的「⟲ 原文」= 清全部筛选回到完整流，
    // 跳到该条目所在卡并高亮。完整流归组单独建（与 displayLogs 归组不同实例）。
    val isFiltered by remember(vm) {
        derivedStateOf {
            searchQuery.trim().isNotEmpty() ||
                vm.selectedLevels.isNotEmpty() ||
                vm.showPluginLogs.value || vm.showSpeechRuleLogs.value
        }
    }
    val fullGroups = remember(vm.filteredLogs) { LogGroups.build(vm.filteredLogs) }
    var locateHighlight by remember { mutableStateOf<LogEntry?>(null) }

    fun locateOriginal(entry: LogEntry) {
        // 1) 清全部筛选（搜索词/级别勾选/插件/规则缓冲开关）
        searchQuery = ""
        vm.clearFilter()
        vm.showPluginLogs.value = false
        vm.showSpeechRuleLogs.value = false
        // 2) 在完整流里找到该条目的卡头，滚动过去并高亮闪现
        val fullList = vm.filteredLogs
        val idx = fullList.indexOfFirst { it == entry }.let { found ->
            if (found >= 0) found else return
        }
        locateHighlight = entry
        scope.launch {
            // 状态更新后重组出完整列表再滚：等一帧让 displayLogs 生效
            withFrameNanos { }
            listState.animateScrollToItem(fullGroups.listItemFor(idx))
            // 高亮停留约 2s 后撤掉（LogEntryBody 按引用比对）
            kotlinx.coroutines.delay(2000)
            if (locateHighlight == entry) locateHighlight = null
        }
    }

    // 搜索词变化 → 跳到最近一条匹配(高亮由 LogScreen 渲染)；列表本身已实时过滤，
    // 跳转目标按归组映射换算：命中卡内任一行即落到整张卡
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.isNotEmpty()) {
            val idx = displayLogs.indexOfLast { it.matchesQuery(q) }
            if (idx >= 0) listState.animateScrollToItem(logGroups.listItemFor(idx))
        }
    }

    // 上一处/下一处：以当前可视位置为锚点跳转，到头自动绕回；条目下标经归组映射
    // 换算成列表项下标再滚动
    fun jumpToMatch(forward: Boolean) {
        val q = searchQuery.trim()
        if (q.isEmpty()) return
        val matches = displayLogs.indices.filter { displayLogs[it].matchesQuery(q) }
        if (matches.isEmpty()) return
        // 可视首项（可能是卡/裸行/日期签）→ 取该列表项的首个条目下标做锚
        val firstVisible = listState.firstVisibleItemIndex
        val anchorEntry = when (val item = logGroups.items.getOrNull(firstVisible)) {
            is LogGroups.Item.Bare -> item.index
            is LogGroups.Item.Card -> item.head
            else -> firstVisible
        }
        val target = if (forward)
            matches.firstOrNull { it > anchorEntry } ?: matches.first()
        else
            matches.lastOrNull { it < anchorEntry } ?: matches.last()
        scope.launch { listState.animateScrollToItem(logGroups.listItemFor(target)) }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // ———— 页面级多选（1003 终态：长按条目即进，1002 的顶栏 ☑ 入口已删）：跨条勾选 → 底栏「复制(N)」————
    // 跨条拖选是 compose 1.7 selection 的崩溃源（滚动回收条目后选区悬挂 selectableId），
    // 跨条复制一律走本模式；勾选以条目对象为键，筛选/重排不错位。退多选=返回键/复制后自动退。
    // 两个状态都用 remember（非 saveable）：Set<LogEntry> 不落 Bundle，旋转屏一并重置、
    // 保持「模式开着但勾选丢了」的不一致不会出现
    var selectionMode by remember { mutableStateOf(false) }
    var checkedEntries by remember { mutableStateOf<Set<LogEntry>>(emptySet()) }
    fun exitSelection() {
        selectionMode = false
        checkedEntries = emptySet()
    }
    fun toggleCheckAll() {
        val all = displayLogs.toSet()
        checkedEntries = if (checkedEntries.containsAll(all)) emptySet() else all
    }
    // 返回键先退多选（页面级；钥匙=与密钥页同一交互口径）
    androidx.activity.compose.BackHandler(enabled = selectionMode) { exitSelection() }
    // 复制：按列表顺序拼接「时间 | 级别 | message」，多条之间以空行分隔
    fun copyChecked() {
        if (checkedEntries.isEmpty()) {
            context.toast(R.string.log_select_none)
            return
        }
        val text = displayLogs.filter { it in checkedEntries }
            .joinToString(separator = "\n\n") { "${it.time} | ${it.getLevelChar()} | ${it.message}" }
        clipboard.setText(AnnotatedString(text))
        exitSelection()
        context.toast(R.string.copied)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        bottomBar = {
            // 多选底栏（照密钥页）：全选 |（右）复制(N)。放 Scaffold bottomBar：列表自动让位。
            // 不加 navigationBarsPadding——本页在 MainPager 的 pager 里，外层已让出底栏+手势条高度
            if (selectionMode) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FlatTextAction(
                            stringResource(R.string.select_all),
                            MaterialTheme.colorScheme.onSurfaceVariant
                        ) { toggleCheckAll() }
                        Spacer(Modifier.weight(1f))
                        // 「复制(N)」改填充主色胶囊（10-04 用户：原灰/主色文字键不突出）。
                        // 全选=次要文字键、复制=主操作填充键，主次一眼可分（填色=动作的全局语汇）
                        Button(
                            onClick = { copyChecked() },
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(percent = 50)
                        ) {
                            Text(
                                stringResource(R.string.log_copy_n, checkedEntries.size),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        },
        topBar = {
            Column {
                // M3 TopAppBar → 全站自绘 NavTopAppBar（56dp、动作键热区贴边）：顶栏 ⋮ 与列表行 ⋮ 同列
                NavTopAppBar(
                    title = {
                        AnimatedContent(
                            targetState = isSearchActive,
                            transitionSpec = {
                                fadeIn() + slideInHorizontally { it } togetherWith
                                        fadeOut() + slideOutHorizontally { it }
                            },
                            label = "TitleAnimation"
                        ) { isSearch ->
                            if (!isSearch) {
                                // 双击标题栏空白区=回日志顶部（10-04 用户定：替代原浮动 ↑ 键）。
                                // Box 撑满标题槽（NavTopAppBar 里是 weight(1f) 的 Box）捕获整个标题区。
                                // 左对齐（10-05 用户：标题居中是四页独一份，「都跑中间了」——
                                // 撤掉居中，走 NavTopAppBar 默认 CenterStart，与主页/密钥页同构）
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerInput(Unit) {
                                            detectTapGestures(onDoubleTap = {
                                                scope.launch {
                                                    runCatching { listState.scrollToItem(0) }
                                                }
                                            })
                                        },                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(text = stringResource(id = R.string.log))
                                }
                            } else {
                                // 搜索框 - 使用 DockedSearchBar，沉浸式样式，bodyLarge 字体
                                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyLarge) {
                                    DockedSearchBar(
                                        inputField = {
                                            SearchBarDefaults.InputField(
                                                query = searchQuery,
                                                onQueryChange = { searchQuery = it },
                                                onSearch = { },
                                                expanded = false,
                                                onExpandedChange = { },
                                                placeholder = { 
                                                    Text(stringResource(R.string.search_logs))
                                                },
                                                trailingIcon = {
                                                    if (searchQuery.isNotEmpty()) {
                                                        IconButton(onClick = { searchQuery = "" }) {
                                                            Icon(Icons.Default.Clear, stringResource(R.string.clear))
                                                        }
                                                    }
                                                }
                                            )
                                        },
                                        expanded = false,
                                        onExpandedChange = { },
                                        // 搜索态专属框（log-ui-1008 ④拍板：搜索态顶栏只留
                                        // 搜索+返回，三键隐藏）——框回 0.95 宽 ≈280dp，
                                        // placeholder 四字+清除键放得下
                                        modifier = Modifier.fillMaxWidth(0.95f)
                                    ) {}
                                }
                            }
                        }
                    },
                    actions = {
                        // 搜索按钮
                        IconButton(onClick = { 
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) searchQuery = ""
                        }) {
                            Icon(
                                if (isSearchActive) Icons.AutoMirrored.Default.ArrowBack else Icons.Default.Search,
                                if (isSearchActive) stringResource(R.string.nav_back) else stringResource(R.string.search)
                            )
                        }
                        
                        // 漏斗/文件夹/清空三键：非搜索态才显示（log-ui-1008 ④拍板
                        // 回归 10-06 口径——搜索态顶栏只留搜索框+返回；级别筛选改由
                        // 搜索控制行里的漏斗键承担，不因隐藏而失入口）。
                        // 退出搜索三键原位恢复。整个 if 挂在 actions 里，无孤儿 lambda
                        // （37733831376 教训：撤门控时条件与大括号必须一起动）
                        if (!isSearchActive) {
                            // 筛选按钮
                            IconButton(onClick = { vm.showFilterDialog.value = true }) {
                                Icon(Icons.Default.FilterList, stringResource(R.string.filter))
                            }

                            // 文件夹按钮 - 先弹日志文件列表自由选择（用户 09-08），点击文件再用外部查看器打开
                            IconButton(onClick = { showLogFilesDialog = true }) {
                                Icon(Icons.Default.FolderOpen, stringResource(R.string.open_log_folder))
                            }

                            // 清空按钮
                            IconButton(onClick = { vm.clear() }) {
                                Icon(Icons.Default.DeleteOutline, stringResource(id = R.string.clear_log))
                            }
                        }
                    }
                )

                // 顶栏与列表之间的分隔线（10-06）：滚动的日志/卡片贴到顶栏时有了"底"，
                // 不再像 UI 被拦腰截断
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // 搜索控制行：漏斗 + 匹配数 + 上一处/下一处跳转（log-ui-1008 ④拍板：
                // 漏斗住进搜索控制行——顶栏三键隐藏后级别筛选仍可用，搜索和等级筛选同开；
                // 「只看匹配」chip 随②搜索即筛选一并退役）
                AnimatedVisibility(
                    visible = isSearchActive && searchQuery.isNotBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { vm.showFilterDialog.value = true }) {
                            Icon(Icons.Default.FilterList, stringResource(R.string.filter))
                        }
                        Text(
                            text = "${matchCount} 处匹配",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = { jumpToMatch(false) }) {
                            Icon(Icons.Default.KeyboardArrowUp, "上一处")
                        }
                        IconButton(onClick = { jumpToMatch(true) }) {
                            Icon(Icons.Default.KeyboardArrowDown, "下一处")
                        }
                    }
                }

                // 筛选标签显示
                AnimatedVisibility(
                    visible = vm.selectedLevels.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.filter),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            // chips 流式换行：一行放不下自动折第二行（用户 09-08：不想左右滑）
                            FlowRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                vm.selectedLevels.forEach { level ->
                                    FilterChip(
                                        selected = true,
                                        onClick = { vm.toggleLevel(level) },
                                        label = { Text(getLevelName(level), maxLines = 1) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = null,
                                                modifier = Modifier.height(16.dp).width(16.dp)
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = getLevelColor(level)
                                        )
                                    )
                                }
                            }
                            // 清除所有筛选（"筛选"标签与清除键固定两侧，chips 在中间滚动）
                            IconButton(onClick = { vm.clearFilter() }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    ) { paddingValues ->
        LogScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .padding(bottom = paddingValues.calculateBottomPadding()),
            list = displayLogs,
            listState = listState,
            // 搜索定位期间不自动滚底，避免与跳转互相拉扯；多选中暂停（1003）——
            // 拖选途中新日志一到就把列表拉到尾，指尖下的行会整批选错
            autoScrollToBottom = vm.autoScrollToBottom.value && searchQuery.isEmpty() && !selectionMode,
            searchQuery = searchQuery,
            selectionMode = selectionMode,
            checkedEntries = checkedEntries,
            onToggleCheck = { e -> checkedEntries = if (e in checkedEntries) checkedEntries - e else checkedEntries + e },
            // 长按拖动多选（1003）：长按即进多选并勾上该条，拖动连选、拖回缩小；
            // 整集替换（非逐条 toggle），底栏「复制(N)」计数拖动中实时跟着走
            dragSelectEnabled = true,
            onEnterSelection = { selectionMode = true },
            onCheckedChange = { checkedEntries = it },
            // 共享归组（10-06 全链卡）：与上面搜索跳转用同一实例，条目↔列表项不错位
            groups = logGroups,
            // 「原文」定位（用户 10-08 午后 甲方案）：筛选/搜索态显示，点击回调清筛选+跳原位
            showLocateKey = isFiltered,
            onLocateOriginal = { entry -> locateOriginal(entry) },
            locateHighlight = locateHighlight,
        )
    }

    // 日志文件列表（用户 09-08：自由选择要打开的日志文件）
    if (showLogFilesDialog) {
        val logFiles = remember {
            File(vm.logDir()).listFiles()
                ?.filter { it.isFile }
                ?.sortedByDescending { it.lastModified() } ?: emptyList()
        }
        val timeFmt = remember {
            java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
        }
        AlertDialog(
            onDismissRequest = { showLogFilesDialog = false },
            title = { Text("日志文件") },
            text = {
                Column(
                    Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (logFiles.isEmpty()) {
                        Text(
                            "日志目录为空",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    logFiles.forEach { f ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showLogFilesDialog = false
                                    openLogFileWithViewer(context, f)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    f.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1
                                )
                                Text(
                                    "${f.length() / 1024} KB · " + timeFmt.format(f.lastModified()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showLogFilesDialog = false
                    openLogFileWithViewer(context, File(vm.logDir()))
                }) {
                    Text("打开整个目录")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogFilesDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 筛选对话框
    if (vm.showFilterDialog.value) {
        LogFilterDialog(
            errorOnly = vm.isErrorsOnly(),
            onShowAll = { vm.showAllLevels() },
            onErrorsOnly = { vm.showErrorsOnly() },
            showPluginLogs = vm.showPluginLogs.value,
            onPluginLogsToggle = { vm.showPluginLogs.value = !vm.showPluginLogs.value },
            showSpeechRuleLogs = vm.showSpeechRuleLogs.value,
            onSpeechRuleLogsToggle = { vm.showSpeechRuleLogs.value = !vm.showSpeechRuleLogs.value },
            autoScrollToBottom = vm.autoScrollToBottom.value,
            onAutoScrollToggle = { vm.autoScrollToBottom.value = !vm.autoScrollToBottom.value },
            showDebugLogs = vm.showDebugLogs.value,
            onDebugLogsToggle = { vm.showDebugLogs.value = !vm.showDebugLogs.value },
            onDismiss = { vm.showFilterDialog.value = false }
        )
    }
}

@Composable
private fun getLevelName(level: Int): String {
    return when (level) {
        LogLevel.ERROR -> "ERROR"
        LogLevel.WARN -> "WARN"
        LogLevel.INFO -> "INFO"
        LogLevel.DEBUG -> "DEBUG"
        LogLevel.TRACE -> "VERBOSE"
        else -> "UNKNOWN"
    }
}

@Composable
private fun getLevelColor(level: Int): Color {
    return when (level) {
        LogLevel.ERROR -> MaterialTheme.colorScheme.errorContainer
        LogLevel.WARN -> Color(0xFFFFF3E0)
        LogLevel.INFO -> MaterialTheme.colorScheme.secondaryContainer
        LogLevel.DEBUG -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
}

// 用外部查看器打开文件/目录（text/plain 优先，通用类型兜底）
// 目录必须走 */*（text/plain 会把目录 URI 丢给文本编辑器，打不开）
private fun openLogFileWithViewer(context: android.content.Context, file: java.io.File) {
    val mime = if (file.isDirectory) "*/*" else "text/plain"
    kotlin.runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }.onFailure {
        kotlin.runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            context.startActivity(
                android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
            )
        }
    }
}
