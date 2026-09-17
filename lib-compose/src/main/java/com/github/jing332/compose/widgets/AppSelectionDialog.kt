package com.github.jing332.compose.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.jing332.compose.ComposeExtensions.clickableRipple
import com.github.jing332.compose.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** 底部大弹窗占屏高比例（用户 09-17 定：**统一 92%**，与音色广场同体量） */
private const val SELECTION_SHEET_HEIGHT = 0.92f

/**
 * 分档阈值（用户 09-17 定 C 方案）：条目数超过此值才用底部大弹窗，否则维持原来的居中卡片。
 * 底部大弹窗满宽、高度可达屏高 92%，只有「几百上千条」的声音 / 插件列表才划算；
 * 语言、主题、音色来源这类十几个的列表用居中卡片更紧凑，不至于变成半屏空白页。
 *
 * 定 20 而不是 12：发音人分类是 15 项 +「默认」= 16 条，按 12 会被推进底部面板并撑满
 * 半屏——正是用户嫌的那种「内容不多却几乎满屏」。20 让 16 条的分类留在居中卡片里。
 */
private const val SELECTION_SHEET_THRESHOLD = 20

/**
 * 条目高度估算值，用于给面板定高（条目少则面板矮、多则撑到上限再由列表内部滚）。
 * `minimumInteractiveComponentSize` 是 48dp：条目本体 12dp 上下内边距 + 14sp 文字实测约 43dp，
 * 被这个下限兜住，故 48dp 就是绝大多数条目的真实高度。
 */
private val SELECTION_ROW_HEIGHT = 48.dp

/** 列表自身的内边距（LoadingContent 上下各 16dp），估高时补上 */
private val SELECTION_LIST_PADDING = 32.dp

/**
 * 底部面板固定区的估算高度：拖拽把 + 标题行 + 按钮行 + 导航栏让位。
 * 内容区上限必须为它让位——否则长列表会把面板占满，按钮行被挤出面板可视范围
 * （目目 09-17 实机：试听分类弹窗底部看不到「保存」）。取值偏大无害，
 * 只是长列表少显两行。
 */
private val SELECTION_SHEET_CHROME_HEIGHT = 200.dp

/** 列表之外固定区的估算高度：搜索框 / 开关行 / 空提示 */
private val SELECTION_FIELD_HEIGHT = 72.dp
private val SELECTION_SWITCH_ROW_HEIGHT = 56.dp
private val SELECTION_EMPTY_HINT_HEIGHT = 48.dp

/**
 * 列表条目的横向内边距，外壳按形态提供：居中卡片 16dp、底部面板 0dp
 * （面板已统一给 24dp，条目再自加就叠出第二套左缘线）。
 * 自定义 itemContent（插件选择器等）取这个值，不要写死 16dp。
 */
val LocalSelectionRowHorizontalPadding = staticCompositionLocalOf { 16.dp }

@Composable
fun AppSelectionDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    value: Any,
    values: List<Any>,
    entries: List<String>,
    icons: List<Any?> = emptyList(),
    isLoading: Boolean = false,
    searchEnabled: Boolean = values.size > 5,

    itemContent: (@Composable RowScope.(Boolean, String, Any?, Any) -> Unit)? = null,

    // null = 调用方没有额外动作键（纯单选弹窗，点行即选即关）；传了（如试听分类的「保存」）
    // 则属于功能性出口，底部形态照样渲染并补「关闭」作显式结束键（见 effectiveButtons）
    extraButtons: (@Composable BoxScope.() -> Unit)? = null,
    // 传 null 走默认按钮：额外按钮 +「关闭」——底部形态下不再重复给「关闭」
    // （面板右上已有 ✕、面板外点击也能关，底部多一行按钮就少露一行列表，见 effectiveButtons）。
    // 调用方传了自定义 buttons 则完全替换默认，不改语义
    buttons: (@Composable BoxScope.() -> Unit)? = null,

    onValueSame: (Any, Any) -> Boolean = { a, b -> a == b },
    onClick: (Any, String) -> Unit,
    onLongClick: ((Any, String) -> Unit)? = null,
    trailingContent: (@Composable RowScope.(itemValue: Any, entry: String, onHighlight: () -> Unit) -> Unit)? = null,
    selectedMultiValues: Set<Any> = emptySet(),
    onMultiSelectedChange: ((Set<Any>) -> Unit)? = null,
    categoryMap: Map<Any, String> = emptyMap(),
    onCategoryChange: ((itemValue: Any, category: String?) -> Unit)? = null,
    waitCategorySwitch: Boolean = false,
    onWaitCategorySwitchChange: ((Boolean) -> Unit)? = null,
    autoNextSwitch: Boolean = false,
    onAutoNextSwitchChange: ((Boolean) -> Unit)? = null,
) {
    // 外壳分档：判定在打开那一刻定住（按原始条数，不受搜索过滤影响），
    // 免得开着开着列表变短、形态跟着跳。声明放在条目渲染之前——底部形态下
    // 条目不自带左右内边距，改由 SelectionSheet 统一给 24dp（见下面 hp 的用法）
    val useSheet = entries.size > SELECTION_SHEET_THRESHOLD

    // 底部形态下由面板统一给左右 24dp，内层这份横向内边距必须让位：
    // 否则 8(搜索框)/16(条目文字) 各自叠在 24dp 之上，又变成多套左缘线
    val hp = if (useSheet) 0.dp else 16.dp

    // null 时走默认渲染（icons 圆图+文字）；调用方可传自定义渲染（如插件选择器传 PluginImage,
    // 加载失败回退名称首字徽章——与插件管理页头像一致,仅插件栏补首字,其他栏无图标不补不留空）
    val content = itemContent ?: { isSelected, entry, icon, _ ->
        // 无图标条目不造首字徽章(用户:除插件栏外其他栏补徽章太冗余),文字直接顶格,
        // 有图标的条目加载失败才回退首字,避免已声明图标的位置留空白圆位
        val hasIcon = when (icon) {
            null -> false
            is CharSequence -> icon.isNotBlank()
            else -> true
        }
        if (hasIcon)
            SubcomposeAsyncImage(
                icon,
                null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(32.dp),
                error = { CenterTextImage(entry.getOrElse(0) { '-' }.toString()) }
            )
        Text(
            entry,
            // bodyMedium 14sp（用户 09-11）：AlertDialog 正文槽 LocalTextStyle=bodyMedium，
            // 触发它的字段值全是 14sp，列表条目原显式 bodyLarge 16sp 与字段错位一圈；
            // 降到 14sp 后全 app 选择弹窗（插件/分组/分类）字段与列表对齐，长列表也更紧凑
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = hp, vertical = 12.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
    }

    // 搜索框固定在顶部常显，无需点击图标再展开
    // 当前高亮的条目值（点击试听或多选按钮时设置，单选式：点另一个即转移）
    var highlightedValue by remember { mutableStateOf<Any?>(null) }

    val focusRequester = remember { FocusRequester() }

    // 「输入中的文本」与「已生效的搜索词」提到外壳这层：外壳要按**过滤后的可见条数**估面板高度，
    // 而可见条数取决于搜索词；500ms 轮询赋值的逻辑仍留在 content 里
    var text by rememberSaveable { mutableStateOf("") }
    var searchText by rememberSaveable { mutableStateOf("") }

    // 面板高度按可见条数估：条目少时面板随内容收缩，条目多时撑到 92% 屏高、由列表内部滚动
    val sheetMaxHeight = LocalConfiguration.current.screenHeightDp.dp * SELECTION_SHEET_HEIGHT
    val visibleCount = if (searchEnabled && searchText.isNotBlank())
        entries.count { it.contains(searchText, ignoreCase = true) } else entries.size
    val listMaxHeight = (
            // 写成 Dp * Int 而不是 Int * Dp：后者是 Compose 的顶层扩展 `Int.times(Dp)`，
            // 需显式 import，否则只剩 Int 自带那几个数值重载、直接编译不过
            SELECTION_ROW_HEIGHT * visibleCount +
                    SELECTION_LIST_PADDING +
                    (if (searchEnabled) SELECTION_FIELD_HEIGHT else 0.dp) +
                    (if (onWaitCategorySwitchChange != null || onAutoNextSwitchChange != null)
                        SELECTION_SWITCH_ROW_HEIGHT else 0.dp) +
                    (if (searchEnabled && searchText.isNotBlank() && visibleCount == 0)
                        SELECTION_EMPTY_HINT_HEIGHT else 0.dp)
            )
            // 关键：上限要再减掉面板固定区——内容把 92% 吃满时按钮行会被挤出面板外
            .coerceAtMost(sheetMaxHeight - SELECTION_SHEET_CHROME_HEIGHT)

    // 底部形态的按钮行：纯单选弹窗（extraButtons=null）不渲染——右上 ✕、面板外点击已够关，
    // 多一行按钮就少一行列表。调用方给了额外动作键（如试听分类的「保存」）则必须渲染：
    // 保存只有勾选后才点亮，光靠右上 ✕ 又没有「不保存就退出」的显式出口
    // （目目 09-17：试听完不想继续分类了没有结束键）⇒ 补一个「关闭」。
    // 调用方自定义 buttons 时完全替换这套默认
    val effectiveButtons: @Composable BoxScope.() -> Unit = buttons ?: {
        extraButtons?.invoke(this)
        if (!useSheet || extraButtons != null)
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(id = R.string.close))
            }
    }

    // 弹窗内容（开关行 / 搜索框 / 列表 / 空提示）：两种外壳共用同一份，只换外面的容器
    val dialogContent: @Composable BoxScope.() -> Unit = {
            // 打开时定位到「当前值」那一条（09-17 与目目确认：这是预期行为——
            // 打开就落在当前声音所在的位置，不要改成从第一条开始）
            val state = rememberLazyListState()
            LaunchedEffect(values) {
                val index = values.indexOfFirst { onValueSame(it, value) }
                if (index >= 0 && index < entries.size)
                    state.scrollToItem(index)
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                // 开关行：等待分类 / 自动下一个，带文字标签，置于列表上方避免与标题挤在一起
                if (onWaitCategorySwitchChange != null || onAutoNextSwitchChange != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        onWaitCategorySwitchChange?.let { onChange ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = waitCategorySwitch, onCheckedChange = onChange)
                                Text(
                                    stringResource(R.string.wait_for_category),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                        if (onWaitCategorySwitchChange != null && onAutoNextSwitchChange != null) {
                            Spacer(Modifier.width(16.dp))
                        }
                        onAutoNextSwitchChange?.let { onChange ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = autoNextSwitch, onCheckedChange = onChange)
                                Text(
                                    stringResource(R.string.auto_next),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }

                if (searchEnabled) {
                    val keyboardController = LocalSoftwareKeyboardController.current

                    // 搜索框固定在顶部常显，无需点击搜索图标再展开
                    DenseOutlinedField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (useSheet) 0.dp else 8.dp)
                            .focusRequester(focusRequester),
                        value = text, onValueChange = { text = it },
                        label = { Text(stringResource(id = R.string.search) + " ${values.size}") },
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { keyboardController?.hide() }
                        )
                    )

                    LaunchedEffect(Unit) {
                        while (coroutineContext.isActive) {
                            delay(500)
                            searchText = text
                        }
                    }
                }

                // 用过滤后的条目数判断空，避免依赖 viewport 布局时机导致"空列表"红字闪现
                // （外壳那层按可见条数估高，名字用 filteredCount 以免遮蔽）
                val filteredCount by remember {
                    derivedStateOf {
                        if (!searchEnabled || searchText.isBlank()) entries.size
                        else entries.count { it.contains(searchText, ignoreCase = true) }
                    }
                }

                if (searchText.isNotBlank() && filteredCount == 0)
                    Text(
                        modifier = Modifier
                            .padding(
                                horizontal = if (useSheet) 0.dp else 8.dp,
                                vertical = 4.dp
                            )
                            .minimumInteractiveComponentSize()
                            .align(Alignment.CenterHorizontally),
                        text = stringResource(id = R.string.empty_list),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )


                LoadingContent(
                    modifier = Modifier.padding(vertical = 16.dp),
                    isLoading = isLoading
                ) {

                    LazyColumn(
                        state = state,
                        modifier = Modifier
                    ) {
                        itemsIndexed(entries) { i, entry ->
                            if (searchEnabled && searchText.isNotBlank() &&
                                !entry.contains(searchText, ignoreCase = true)
                            ) return@itemsIndexed

                            val icon = icons.getOrNull(i)
                            val current = values[i]
                            val isSelected = onValueSame(value, current)
                            val isHighlighted = highlightedValue != null && onValueSame(highlightedValue!!, current)
                            // 不再用绿色(高亮)/蓝色(选中)背景着色，颜色保持默认
                            val rowBg = Color.Unspecified
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(rowBg)
                                    .clickableRipple(
                                        onClick = { onClick(current, entry) },
                                        onLongClick = onLongClick?.let { { it(current, entry) } }
                                    )
                                    .minimumInteractiveComponentSize()
                                    .focusable()
                                    .semantics(mergeDescendants = true) {
                                        selected = isSelected

                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                content(isSelected, entry, icon, value)
                                if (trailingContent != null) trailingContent(current, entry) {
                                    highlightedValue = current
                                }
                                if (onMultiSelectedChange != null) {
                                    val isMultiSelected = current in selectedMultiValues
                                    val category = categoryMap[current]
                                    if (category != null) {
                                        // 已分配分类：显示分类名标签，点击可重新选择
                                        var showCategoryMenu by remember { mutableStateOf(false) }
                                        val allCategories = remember {
                                            listOf("默认") + VoiceCategories.ALL
                                        }
                                        Box {
                                            Surface(
                                                modifier = Modifier
                                                    .padding(start = 4.dp)
                                                    .clickableRipple(onClick = { showCategoryMenu = true }),
                                                shape = MaterialTheme.shapes.small,
                                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                                tonalElevation = 2.dp
                                            ) {
                                                Text(
                                                    category,
                                                    modifier = Modifier.padding(
                                                        horizontal = 8.dp,
                                                        vertical = 4.dp
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = showCategoryMenu,
                                                onDismissRequest = { showCategoryMenu = false }
                                            ) {
                                                allCategories.forEach { cat ->
                                                    DropdownMenuItem(
                                                        text = { Text(cat) },
                                                        onClick = {
                                                            showCategoryMenu = false
                                                            if (onCategoryChange != null) {
                                                                onCategoryChange.invoke(
                                                                    current,
                                                                    if (cat == "默认") null else cat
                                                                )
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        // 未分配分类：显示多选圆点，长按可弹出分类选择
                                        var showCategoryMenu by remember { mutableStateOf(false) }
                                        val allCategories = remember {
                                            listOf("默认") + VoiceCategories.ALL
                                        }
                                        Box {
                                            Box(
                                                Modifier
                                                    .padding(start = 4.dp)
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isMultiSelected) MaterialTheme.colorScheme.primary
                                                        else Color.Transparent
                                                    )
                                                    .border(
                                                        2.dp,
                                                        if (isMultiSelected) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.outline,
                                                        CircleShape
                                                    )
                                                    .clickableRipple(
                                                        onClick = {
                                                            highlightedValue = current
                                                            onMultiSelectedChange.invoke(
                                                                if (isMultiSelected) selectedMultiValues - current
                                                                else selectedMultiValues + current
                                                            )
                                                        },
                                                        onLongClick = {
                                                            if (onCategoryChange != null) showCategoryMenu = true
                                                        }
                                                    )
                                            )
                                            DropdownMenu(
                                                expanded = showCategoryMenu,
                                                onDismissRequest = { showCategoryMenu = false }
                                            ) {
                                                allCategories.forEach { cat ->
                                                    DropdownMenuItem(
                                                        text = { Text(cat) },
                                                        onClick = {
                                                            showCategoryMenu = false
                                                            if (onCategoryChange != null) {
                                                                onCategoryChange.invoke(
                                                                    current,
                                                                    if (cat == "默认") null else cat
                                                                )
                                                            }
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
                }
            }
    }

    // 分档（用户 09-17 定 C 方案）：条目少 → 原来的居中卡片（紧凑，与语言 / 主题那些一致）；
    // 条目多 → 底部大弹窗（满宽 + 上限 92% 屏高，一眼看到更多行）。
    // 顺手把条目横向内边距按形态广播出去，自定义 itemContent 同步对齐
    CompositionLocalProvider(LocalSelectionRowHorizontalPadding provides hp) {
        if (useSheet)
            SelectionSheet(
                onDismissRequest = onDismissRequest,
                maxSheetHeight = sheetMaxHeight,
                maxListHeight = listMaxHeight,
                title = title,
                content = dialogContent,
                buttons = effectiveButtons,
            )
        else
            AppDialog(
                onDismissRequest = onDismissRequest,
                title = title,
                content = dialogContent,
                buttons = effectiveButtons,
            )
    }
}

/**
 * 发音人分类的公共常量：试听分类弹窗（AuditionDialog）与
 * 声音列表长按分类菜单（AppSelectionDialog）共用，避免两处硬编码不同步。
 */
object VoiceCategories {
    /** 全部分类（按性别年龄排列，后四项为朗读规则 2.87 中新增的主角/特殊分类） */
    val ALL: List<String> = listOf(
        "女童", "少女", "女青年", "女中年", "女老年",
        "男童", "少年", "男青年", "男中年", "男老年",
        "男主", "女主", "特殊男", "特殊女", "旁白"
    )

    /** 分类弹窗的三列布局：女性列 / 男性列 / 主角特殊旁白列，每列内部竖向堆叠 */
    val COLUMNS: List<List<String>> = listOf(
        // 女性列
        listOf("女童", "少女", "女青年", "女中年", "女老年"),
        // 男性列
        listOf("男童", "少年", "男青年", "男中年", "男老年"),
        // 主角特殊旁白列：旁白在最前
        listOf("旁白", "男主", "女主", "特殊男", "特殊女")
    )
}