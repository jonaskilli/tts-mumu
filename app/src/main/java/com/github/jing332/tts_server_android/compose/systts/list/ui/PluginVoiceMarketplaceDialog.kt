package com.github.jing332.tts_server_android.compose.systts.list.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.SubcomposeAsyncImage
import com.github.jing332.compose.widgets.CenterTextImage
import com.github.jing332.compose.widgets.DenseOutlinedField
import com.github.jing332.compose.widgets.PinDialogWindowToScreen
import com.github.jing332.tts.speech.plugin.engine.VoiceCatalogFilterGroup
import com.github.jing332.tts.speech.plugin.engine.VoiceCatalogFilterOption
import com.github.jing332.tts.speech.plugin.engine.VoiceCatalogItem
import com.github.jing332.tts_server_android.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 内置排序档位（插件未声明 sortOptions 时用）——取值与插件侧 sort_by 白名单一致 */
private const val CATALOG_SORT_SCORE = "score"
private const val CATALOG_SORT_HOT = "task_count"
private const val CATALOG_SORT_NEW = "created_at"

/** 输入防抖：与 jread 的 350ms 同量级——打字过程中不逐字发请求 */
private const val CATALOG_SEARCH_DEBOUNCE_MS = 400L

/** 底部大弹窗占屏高比例（用户 09-17 定：**统一 92%**，与列表选择弹窗同体量） */
private const val CATALOG_SHEET_HEIGHT = 0.92f

/**
 * 音色广场弹窗（opt-in 协议 `EditorJS.searchVoiceCatalog(query)`，协议见 jread
 * `docs/plugin-editor-schema-v1.md`「分类选择链路」与 `PluginVoiceMarketplaceModels.kt`）。
 *
 * 为什么必须有它：这类插件（Fish Audio 官网音色广场等）的 `getVoices()` 只回吐自己写的本地缓存，
 * 而**只有 `searchVoiceCatalog()` 会写这个缓存** ⇒ 没有广场入口时声音下拉恒空、批量导入 0 条。
 *
 * 形态：**底部大弹窗**（占屏高 `CATALOG_SHEET_HEIGHT`，仅顶部两角圆角，与换声弹窗同一范式）。
 * 内容按用户 09-17 拍板的方案（两图）：搜索框（服务端搜索）+ 排序 + 「筛」；下面 quickFilters
 * 横滑 chips；再下面是结果摘要与卡片列表（圆形封面 / 名字·作者 / 使用次数 / 描述两行 / 标签）；
 * 滚到底自动续页；底部「选用（N）」把勾中的音色交给调用方回填声音列表与批量保存链路。
 *
 * @param onAudition 卡片上的 🎧 —— 交给调用方的现有试听弹窗（同一个 AuditionDialog），弹窗本身不关
 * @param onPick 点「选用」时回调勾中的音色（调用方负责并进声音列表 + 勾选，随后自行关闭本弹窗）
 */
@Composable
fun PluginVoiceMarketplaceDialog(
    vm: PluginTtsViewModel,
    locale: String,
    onDismissRequest: () -> Unit,
    onAudition: (VoiceCatalogItem) -> Unit,
    onPick: (List<VoiceCatalogItem>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    var keyword by remember { mutableStateOf("") }
    var submittedKeyword by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var sortBy by remember { mutableStateOf(CATALOG_SORT_SCORE) }
    var filterSheetOpen by remember { mutableStateOf(false) }
    // 勾选集合用 map 而不是 id 集合：翻页/重搜会换掉 catalogItems，勾过的条目本体得留住
    val picked = remember { mutableStateMapOf<String, VoiceCatalogItem>() }
    // 列表滚动：换条件/重开都从头（索引 0）开始，别让上一次的位置或续页锚点把视口带偏
    val listState = rememberLazyListState()

    // 输入防抖：keyboard 的 Search 动作会立刻提交；不打字时这条只在停顿后落地
    LaunchedEffect(keyword) {
        if (keyword == submittedKeyword) return@LaunchedEffect
        delay(CATALOG_SEARCH_DEBOUNCE_MS)
        submittedKeyword = keyword
    }

    // 条件变化即重查第一页（首次进入也走这里：打开广场立刻有一屏内容可看）；先把视口拉回顶部
    LaunchedEffect(submittedKeyword, tags, sortBy, locale) {
        listState.scrollToItem(0)
        vm.searchCatalog(locale, submittedKeyword, tags.sorted(), sortBy, append = false)
    }

    val fallbackSorts = listOf(
        VoiceCatalogFilterOption(CATALOG_SORT_SCORE, stringResource(R.string.voice_catalog_sort_score)),
        VoiceCatalogFilterOption(CATALOG_SORT_HOT, stringResource(R.string.voice_catalog_sort_hot)),
        VoiceCatalogFilterOption(CATALOG_SORT_NEW, stringResource(R.string.voice_catalog_sort_new)),
    )
    val sortOptions = vm.catalogSortOptions.ifEmpty { fallbackSorts }

    Dialog(
        onDismissRequest = onDismissRequest,
        // 底部大弹窗（与换声弹窗同一范式）：窗口不再被系统栏裁掉，面板满宽、底边贴屏
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        // 窗口钉成全屏+底部对齐（lib-compose PinDialogWindowToScreen）：09-17 实机
        // 窗口被排版到屏幕下方 ~134px，贴底内容全部跟着出屏
        PinDialogWindowToScreen()
        Box(
            Modifier
                .fillMaxSize()
                // 键盘让位：decorFitsSystemWindows=false 后窗口不再被 IME 顶起，
                // 搜索一弹键盘、面板又底对齐，列表下半截会被键盘盖住；imePadding 让整块浮上去
                .imePadding(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // 关闭热区：面板以外的区域点一下即关（满屏方案下没有 scrim 可点，只能靠 ✕）。
            // 不填色——压暗交给 Dialog 窗口自带的 dim，自绘一层会与它叠加成过暗
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { onDismissRequest() }
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(CATALOG_SHEET_HEIGHT),
                // M3 底部面板：仅顶部两角 28dp 圆角、底边贴屏——系统栏间距交给内层 Column 的
                // navigationBarsPadding，面板本体不缩，视觉上仍是「从底部升起」
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                // 面板本体先吃掉落在空白处的点击：否则会穿透到下面那层关闭热区，点个缝就把窗关了；
                // 无指示色、无动作，纯粹占位（子级自己消费过的点击不受影响）
                Column(
                    Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {}
                ) {
                    // 拖拽把：M3 底部弹窗的识别特征（本面板是 Dialog 自绘的，只是形态标记、不可拖动）
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(width = 32.dp, height = 4.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(2.dp),
                                )
                        )
                    }
                    // ---- 标题行：标题 + 已选数 + 关闭 ----
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = CATALOG_PANEL_PADDING,
                                end = 12.dp,
                                top = 8.dp,
                                bottom = 4.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.voice_catalog),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        if (picked.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.voice_catalog_picked, picked.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.Filled.Close, stringResource(R.string.close))
                        }
                    }

                    // ---- 搜索 + 排序 + 筛选 ----
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = CATALOG_PANEL_PADDING),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DenseOutlinedField(
                            modifier = Modifier.weight(1f),
                            value = keyword,
                            onValueChange = { keyword = it },
                            singleLine = true,
                            maxLines = 1,
                            // maxLines=1（目目 09-17：label 折成两行是 DecorationBox 在未聚焦态
                            // 对 label 的宽度约束比文字实际所需更窄所致，单行后不再分两行）
                            label = {
                                Text(
                                    stringResource(R.string.voice_catalog_search),
                                    maxLines = 1,
                                    // 只 maxLines 不给 ellipsis 会把「色」硬裁掉（09-17 实机：
                                    // 未聚焦态 label 约束宽度比文字实际所需窄一字）
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            trailingIcon = {
                                if (keyword.isNotBlank()) IconButton(onClick = { keyword = "" }) {
                                    Icon(Icons.Filled.Clear, stringResource(R.string.voice_catalog_clear))
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    submittedKeyword = keyword
                                    keyboard?.hide()
                                }
                            ),
                        )
                        SortMenu(
                            options = sortOptions,
                            current = sortBy,
                            onSelect = { sortBy = it },
                        )
                        FilterEntry(
                            activeCount = countFilterSelections(vm.catalogFilterGroups, tags),
                            onClick = { filterSheetOpen = true },
                        )
                    }

                    // ---- quickFilters 横滑 chips（协议里的"官方快捷筛选"）----
                    if (vm.catalogQuickFilters.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            contentPadding = PaddingValues(horizontal = CATALOG_PANEL_PADDING),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(vm.catalogQuickFilters, key = { it.id }) { option ->
                                FilterChip(
                                    selected = option.id in tags,
                                    onClick = { tags = tags.toggle(option.id) },
                                    label = { Text(option.label, maxLines = 1) },
                                )
                            }
                        }
                    }

                    // ---- 结果摘要 / 错误 ----
                    val error = vm.catalogError
                    if (error != null) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                start = CATALOG_PANEL_PADDING,
                                end = 12.dp,
                                top = 4.dp,
                                bottom = 4.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                error,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                            TextButton(
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                onClick = {
                                    scope.launch {
                                        vm.searchCatalog(locale, submittedKeyword, tags.sorted(), sortBy, append = false)
                                    }
                                },
                            ) {
                                Text(stringResource(R.string.voice_catalog_retry))
                            }
                        }
                    } else {
                        Text(
                            text = catalogSummary(vm),
                            modifier = Modifier.padding(
                                horizontal = CATALOG_PANEL_PADDING,
                                vertical = 4.dp
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    HorizontalDivider()

                    // ---- 结果列表 ----
                    Box(Modifier.weight(1f)) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(vertical = 4.dp),
                        ) {
                            items(vm.catalogItems, key = { it.id }) { item ->
                                CatalogVoiceRow(
                                    item = item,
                                    checked = item.id in picked,
                                    onToggle = {
                                        if (picked.containsKey(item.id)) picked.remove(item.id)
                                        else picked[item.id] = item
                                    },
                                    onAudition = { onAudition(item) },
                                )
                            }

                            // 续页位：滚到底（该位进入组合）即自动续下一页；零新增时 VM 已把 hasMore
                            // 置否，不会空转。
                            //
                            // ⚠️ 空列表时**不能**挂这个带 key 的 item（09-17 实机「一打开停在中间」）：
                            // 首屏只有它一项时，LazyList 会把它记成「首个可见项的 key」，第一页 30 条
                            // 插到它前面后，滚动位置会跟着这个 key 一起走，于是开场就停在第 30 项。
                            // 故只在已有条目时才挂它；空态转圈见下方 overlay。
                            //
                            // ⚠️ 续页判断必须写在 LaunchedEffect **里面**：写在 if 上会让
                            // 自己一置 loading 就退出组合、把刚发起的请求取消掉。
                            if (vm.catalogItems.isNotEmpty()) {
                                item(key = "catalog_footer") {
                                    LaunchedEffect(vm.catalogItems.size) {
                                        if (vm.catalogHasMore && !vm.catalogLoading) {
                                            vm.searchCatalog(locale, submittedKeyword, tags.sorted(), sortBy, append = true)
                                        }
                                    }
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        when {
                                            vm.catalogLoading -> CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                            )

                                            !vm.catalogHasMore && vm.catalogItems.isNotEmpty() -> Text(
                                                stringResource(R.string.voice_catalog_no_more),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )

                                            else -> Unit
                                        }
                                    }
                                }
                            }
                        }
                        // 加载中但还没有条目：列表区居中转圈（状态文案已由上方摘要行给，不重复）
                        if (vm.catalogItems.isEmpty() && vm.catalogLoading) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // ---- 底部：选用（N）----
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = CATALOG_PANEL_PADDING, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onDismissRequest) {
                            Text(stringResource(R.string.close))
                        }
                        Spacer(Modifier.weight(1f))
                        // 动作键一律纯文字 TextButton（目目 09-17：底部按钮行不要框和填充色）
                        TextButton(
                            enabled = picked.isNotEmpty(),
                            onClick = { onPick(picked.values.toList()) },
                        ) {
                            Text(stringResource(R.string.voice_catalog_pick, picked.size))
                        }
                    }
                }
            }
        }
    }

    if (filterSheetOpen) {
        CatalogFilterSheet(
            groups = vm.catalogFilterGroups,
            selected = tags,
            onApply = {
                tags = it
                filterSheetOpen = false
            },
            onDismissRequest = { filterSheetOpen = false },
        )
    }
}

/**
 * 音色广场面板的左右内边距：广场主面板与筛选面板共用。
 * 定 24dp（目目 09-17 定稿：**全 app 底部面板统一 24dp**，与选择弹窗 SelectionSheet 同一条基准）；
 * ✕ 图标仍落右缘线：标题行 end=12 + IconButton 自带 12dp 内缩 = 24。
 */
private val CATALOG_PANEL_PADDING = 24.dp

/**
 * 筛选弹窗（用户 09-17 方案图二）：上半是「下拉类」筛选组（两列），下半是 chips 组，
 * 标签组（选项特别多的）额外带一个本地选项搜索框。底部「清空 + 应用（N）」。
 *
 * 分组依据是协议声明，不按组名硬编：`presentation == dropdown` 或 `maxSelections == 1` 的
 * 组提顶部两列（当前 Fish Audio 插件全组都是 chips，故这一段对它是空集，但协议兼容性在）。
 */
@Composable
private fun CatalogFilterSheet(
    groups: List<VoiceCatalogFilterGroup>,
    selected: Set<String>,
    onApply: (Set<String>) -> Unit,
    onDismissRequest: () -> Unit,
) {
    // 草稿态：确定前不触发查询（协议侧一次 query 带全部 tags，中途重查没意义）
    var draft by remember { mutableStateOf(selected) }

    val (dropdownGroups, chipGroups) = groups.partition {
        it.presentation.equals("dropdown", ignoreCase = true) || it.maxSelections == 1
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        // 底部大弹窗（与换声弹窗同一范式）：窗口不再被系统栏裁掉，面板满宽、底边贴屏
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        // 窗口钉成全屏+底部对齐（lib-compose PinDialogWindowToScreen）：09-17 实机
        // 窗口被排版到屏幕下方 ~134px，贴底内容全部跟着出屏
        PinDialogWindowToScreen()
        Box(
            Modifier
                .fillMaxSize()
                // 键盘让位：decorFitsSystemWindows=false 后窗口不再被 IME 顶起，
                // 搜索一弹键盘、面板又底对齐，列表下半截会被键盘盖住；imePadding 让整块浮上去
                .imePadding(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // 关闭热区：面板以外的区域点一下即关（满屏方案下没有 scrim 可点，只能靠 ✕）。
            // 不填色——压暗交给 Dialog 窗口自带的 dim，自绘一层会与它叠加成过暗
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { onDismissRequest() }
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(CATALOG_SHEET_HEIGHT),
                // M3 底部面板：仅顶部两角 28dp 圆角、底边贴屏——系统栏间距交给内层 Column 的
                // navigationBarsPadding，面板本体不缩，视觉上仍是「从底部升起」
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                // 面板本体先吃掉落在空白处的点击：否则会穿透到下面那层关闭热区，点个缝就把窗关了；
                // 无指示色、无动作，纯粹占位（子级自己消费过的点击不受影响）
                Column(
                    Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {}
                ) {
                    // 拖拽把：M3 底部弹窗的识别特征（本面板是 Dialog 自绘的，只是形态标记、不可拖动）
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(width = 32.dp, height = 4.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(2.dp),
                                )
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = CATALOG_PANEL_PADDING,
                                end = 12.dp,
                                top = 8.dp,
                                bottom = 4.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.voice_catalog_filter),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.Filled.Close, stringResource(R.string.close))
                        }
                    }
                    HorizontalDivider()

                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = CATALOG_PANEL_PADDING, vertical = 8.dp),
                    ) {
                        if (groups.isEmpty()) {
                            Text(
                                stringResource(R.string.voice_catalog_filter_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // 下拉类：两列
                        dropdownGroups.chunked(2).forEach { row ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                row.forEach { group ->
                                    DropdownFilterGroup(
                                        modifier = Modifier.weight(1f),
                                        group = group,
                                        selected = draft,
                                        onToggle = { draft = draft.toggle(it) },
                                    )
                                }
                                // 奇数个时补空位，保持两列宽度一致
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }

                        chipGroups.forEach { group ->
                            ChipFilterGroup(
                                group = group,
                                selected = draft,
                                onToggle = { draft = draft.toggle(it) },
                            )
                        }
                    }

                    HorizontalDivider()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = CATALOG_PANEL_PADDING, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            enabled = draft.isNotEmpty(),
                            onClick = { draft = emptySet() },
                        ) {
                            Text(stringResource(R.string.voice_catalog_filter_clear))
                        }
                        Spacer(Modifier.weight(1f))
                        // 动作键一律纯文字 TextButton（目目 09-17：底部按钮行不要框和填充色）
                        TextButton(onClick = { onApply(draft) }) {
                            Text(stringResource(R.string.voice_catalog_filter_apply, draft.size))
                        }
                    }
                }
            }
        }
    }
}

/** 下拉类筛选组：单选直接落值，多选（maxSelections != 1）用勾选项列表 */
@Composable
private fun DropdownFilterGroup(
    group: VoiceCatalogFilterGroup,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val chosen = group.options.filter { it.id in selected }
    val single = group.maxSelections == 1
    val label = when {
        chosen.isEmpty() -> group.name
        single -> "${group.name}: ${chosen.first().label}"
        else -> "${group.name}: ${chosen.size}"
    }
    Box(modifier) {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            onClick = { open = true },
        ) {
            Text(
                label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge,
                color = if (chosen.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.primary,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            group.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    trailingIcon = { if (option.id in selected) Icon(Icons.Filled.Check, null) },
                    onClick = {
                        if (single) {
                            // 单选：点同一个即取消（与 chips 的 toggle 语义一致）
                            onToggle(option.id)
                            open = false
                        } else {
                            onToggle(option.id)
                        }
                    },
                )
            }
        }
    }
}

/** chips 类筛选组：选项多的（标签组）带本地选项搜索，只过滤已拉到的选项，不发请求 */
@Composable
private fun ChipFilterGroup(
    group: VoiceCatalogFilterGroup,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    var optionQuery by remember(group.id) { mutableStateOf("") }
    val visible = if (optionQuery.isBlank()) group.options
    else group.options.filter { it.label.contains(optionQuery, ignoreCase = true) }

    Column(Modifier.padding(top = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                group.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (selected.count { id -> group.options.any { it.id == id } } > 0) {
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(
                        R.string.voice_catalog_filter_group_count,
                        selected.count { id -> group.options.any { it.id == id } },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (group.options.size > 12) {
            DenseOutlinedField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                value = optionQuery,
                onValueChange = { optionQuery = it },
                singleLine = true,
                maxLines = 1,
                placeholder = { Text(stringResource(R.string.voice_catalog_filter_option_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
            )
        }
        if (visible.isEmpty()) {
            Text(
                stringResource(R.string.voice_catalog_filter_option_empty),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilterChipFlow(
            options = visible,
            selected = selected,
            onToggle = onToggle,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChipFlow(
    options: List<VoiceCatalogFilterOption>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option.id in selected,
                onClick = { onToggle(option.id) },
                label = { Text(option.label, maxLines = 1) },
            )
        }
    }
}

/** 排序入口：插件给了 sortOptions 就用它的枚举，否则三档内置 */
@Composable
private fun SortMenu(
    options: List<VoiceCatalogFilterOption>,
    current: String,
    onSelect: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
            onClick = { open = true },
        ) {
            Text(
                options.firstOrNull { it.id == current }?.label ?: current,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    trailingIcon = { if (option.id == current) Icon(Icons.Filled.Check, null) },
                    onClick = {
                        open = false
                        onSelect(option.id)
                    },
                )
            }
        }
    }
}

/** 筛选入口：有生效筛选时高亮并把条数写在旁边（窄屏不塞角标，避免与排序按钮挤） */
@Composable
private fun FilterEntry(activeCount: Int, onClick: () -> Unit) {
    TextButton(
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
        onClick = onClick,
    ) {
        Icon(
            Icons.Filled.Tune,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (activeCount > 0) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            if (activeCount > 0) stringResource(R.string.voice_catalog_filter_count, activeCount)
            else stringResource(R.string.voice_catalog_filter),
            modifier = Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (activeCount > 0) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 一条音色：封面 + 名字/作者/使用次数/描述/标签 + 🎧 + 勾选 */
@Composable
private fun CatalogVoiceRow(
    item: VoiceCatalogItem,
    checked: Boolean,
    onToggle: () -> Unit,
    onAudition: () -> Unit,
) {
    val usageText = if (item.usageCount > 0) {
        stringResource(R.string.voice_catalog_usage, item.usageCount.toString())
    } else ""

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(
                start = CATALOG_PANEL_PADDING,
                end = 12.dp,
                top = 8.dp,
                bottom = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SubcomposeAsyncImage(
            model = item.icon,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape),
            error = { CenterTextImage(item.name.getOrElse(0) { '-' }.toString(), size = 44.dp) },
        )
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        ) {
            Text(
                item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = listOf(item.authorName, usageText).filter { it.isNotBlank() }.joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(
                    meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (item.description.isNotBlank()) {
                Text(
                    item.description,
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (item.tags.isNotEmpty()) {
                Row(
                    Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item.tags.take(3).forEach { tag ->
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                tag,
                                modifier = Modifier
                                    .widthIn(max = 72.dp)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (item.tags.size > 3) {
                        Text(
                            "+${item.tags.size - 3}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        IconButton(onClick = onAudition) {
            Icon(
                Icons.Filled.Headset,
                stringResource(R.string.audition),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
    }
}

/** 结果摘要：错误态另有专门一行，这里只写加载/空/总数 */
@Composable
private fun catalogSummary(vm: PluginTtsViewModel): String = when {
    vm.catalogLoading && vm.catalogItems.isEmpty() -> stringResource(R.string.voice_catalog_loading)
    vm.catalogItems.isEmpty() && vm.catalogLoaded -> stringResource(R.string.voice_catalog_empty)
    vm.catalogTotal != null -> stringResource(R.string.voice_catalog_total, vm.catalogTotal!!)
    vm.catalogItems.isNotEmpty() -> stringResource(R.string.voice_catalog_found, vm.catalogItems.size)
    else -> stringResource(R.string.voice_catalog_loading)
}

/** 选中集合的 toggle（chips / 筛选项共用一套语义：点一次切一次） */
private fun Set<String>.toggle(id: String): Set<String> =
    if (id in this) this - id else this + id

/** 生效筛选条数：只数筛选组里的（quickFilters 的 chips 本身就摆在主界面，不再重复计数） */
private fun countFilterSelections(groups: List<VoiceCatalogFilterGroup>, selected: Set<String>): Int {
    if (selected.isEmpty()) return 0
    val groupOptionIds = groups.flatMap { group -> group.options.map { it.id } }.toSet()
    return selected.count { it in groupOptionIds }
}
