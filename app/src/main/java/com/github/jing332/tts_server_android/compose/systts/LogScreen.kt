package com.github.jing332.tts_server_android.compose.systts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangedIgnoreConsumed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.github.jing332.common.LogEntry
import com.github.jing332.common.LogLevel
import com.github.jing332.common.toArgb
import com.github.jing332.common.toLogLevelChar
import com.github.jing332.compose.ComposeExtensions.toAnnotatedString
import com.github.jing332.compose.widgets.ControlBottomBarVisibility
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.LocalBottomBarBehavior
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

// SystemTtsService 拼接次级信息所用哨兵色，此处按主题重映射
private val MetaColorSentinel = Color(0xFFFF00FF)       // 获取成功前缀 → 石板灰
private val VoiceMetaSentinel = Color(0xFF00FFFF)       // 发音人信息 → 雾紫

// 排版实验 1008（用户 10-08 拍板「撤吧，灰」）：请求行"请求音频："前缀撤级别绿——
// 每卡必有、信息量为零的词占最显眼色正好反了（9-13 去彩色化同方向），改中性灰，
// 与时间行同档；黄警/红错的对比度由此上来。字号分层保留（前缀 13sp）
private val RequestPrefixColorLight = Color(0xFF79747E)
private val RequestPrefixColorDark = Color(0xFF938F99)

// 把命中哨兵色的段落整体换成目标色，让"请求音频"正文(纯绿)与
// 获取成功前缀(石板灰)/发音人信息(雾紫)层次分明但不抢眼
private fun AnnotatedString.remapMetaColor(metaColor: Color, voiceColor: Color): AnnotatedString {
    if (spanStyles.none { it.item.color == MetaColorSentinel || it.item.color == VoiceMetaSentinel }) return this
    return buildAnnotatedString {
        append(this@remapMetaColor.text)
        spanStyles.forEach { r ->
            val newColor = when (r.item.color) {
                MetaColorSentinel -> metaColor
                VoiceMetaSentinel -> voiceColor
                else -> r.item.color
            }
            addStyle(
                r.item.copy(color = newColor),
                r.start,
                r.end
            )
        }
    }
}

/**
 * 日志归组模型（10-06 全链卡改版）。渲染单位从"逐条日志"升级为归组项：
 * - Card：一次请求管线。head="请求音频"主行（configId≠0，MDC 只在请求行携带）；
 *   members=结果子行(indent>0)/插件过程行/卡打开期间的其他主行(重试/备用)。
 *   收卡条件：SUCCESS 结果、主行 ERROR（失败终点）、下一张卡开卡。
 *   朗读规则日志不进卡（10-10 用户令）：一律落裸行——原「卡外悬置→下一张卡做前置区」
 *   在搜索/筛选的收窄列表里等不到开卡，是「勾了朗读规则搜不到」的根因。
 * - Bare：与请求挨不上的条目——插件/规则散条、系统消息、头部被级别筛掉的孤儿子行。
 *   白底、级别色照旧，形制与卡片天然区分。
 * - Header：日期签，每天第一条上方出现一次。
 *
 * 纯函数、确定性；TtsLogScreen 与 LogScreen 共用同一实例（搜索跳转要用条目→列表项
 * 映射）。转发器日志无 configId/indent/插件标记，全部落裸行、零影响。
 */
internal class LogGroups(val items: List<Item>, val entryToList: IntArray) {
    sealed class Item {
        class Header(val date: String) : Item()
        class Bare(val index: Int) : Item()
        class Card(
            val head: Int,
            val members: List<Int>,
            // 排版实验 1008（报错三件套，用户 10-08 拍板）：卡内最高错误级别——
            // 0=无错（灰底）1=仅 WARN（淡琥珀底）2=含 ERROR（红粉底）。
            // 原 isError: Boolean 升级，扫一眼分清"出错"还是"只是警告"
            val errorLevel: Int,
        ) : Item()
    }

    // 条目下标 → 列表项下标（搜索跳转/滚动定位用）；越界或未入组时兜底直用条目下标
    fun listItemFor(entryIndex: Int): Int =
        if (entryIndex in entryToList.indices && entryToList[entryIndex] >= 0) entryToList[entryIndex]
        else entryIndex

    fun entriesOf(item: Item): List<Int> = when (item) {
        is Item.Header -> emptyList()
        is Item.Bare -> listOf(item.index)
        is Item.Card -> listOf(item.head) + item.members
    }

    companion object {
        fun build(list: List<LogEntry>): LogGroups {
            val raw = ArrayList<Item>(list.size)
            var head = -1
            var members = ArrayList<Int>()
            var cardErrorLevel = 0

            fun closeCard() {
                if (head >= 0)
                    raw.add(Item.Card(head, members.toList(), cardErrorLevel))
                head = -1
                members = ArrayList()
                cardErrorLevel = 0
            }
            fun markError(e: LogEntry) {
                if (e.level == LogLevel.ERROR) cardErrorLevel = 2
                else if (e.level == LogLevel.WARN && cardErrorLevel < 1) cardErrorLevel = 1
            }
            fun openCard(requestIdx: Int) {
                closeCard()
                head = requestIdx
            }

            list.forEachIndexed { i, e ->
                when {
                    // 结果/子行：有卡归卡（SUCCESS 顺带收卡），无卡=头部被筛掉的孤儿 → 裸行
                    e.indent > 0 -> {
                        if (head >= 0) {
                            members.add(i)
                            markError(e)
                            if (e.level == LogLevel.SUCCESS) closeCard()
                        } else raw.add(Item.Bare(i))
                    }
                    // 插件过程行：卡内归卡，卡外散条
                    e.isPluginLog -> {
                        if (head >= 0) {
                            members.add(i); markError(e)
                        } else raw.add(Item.Bare(i))
                    }
                    // 规则日志一律落裸行（10-10 用户令：不进卡）。原「卡外悬置→下一张卡
                    // 做前置区」的机制整拆：搜索/筛选时收窄列表里经常没有跟得上的请求行，
                    // 悬置行等不到开卡就渲染不出来（表现为「勾了朗读规则也搜不到」）；
                    // 卡内也不再吸收规则行，避免分析行挂在无关请求卡顶造成误导
                    e.isSpeechRuleLog -> raw.add(Item.Bare(i))
                    // "请求音频"主行：开新卡（旧卡先收）
                    e.configId != 0L -> openCard(i)
                    // 其他主行（重试/备用TTS/系统消息）：有卡归卡，ERROR=失败终点收卡；
                    // 无卡落裸行
                    else -> {
                        if (head >= 0) {
                            members.add(i); markError(e)
                            if (e.level == LogLevel.ERROR) closeCard()
                        } else raw.add(Item.Bare(i))
                    }
                }
            }
            closeCard()

            // 插日期签 + 建条目→列表项映射
            val finalItems = ArrayList<Item>(raw.size + 4)
            val map = IntArray(list.size) { -1 }
            var lastDate: String? = null
            raw.forEach { item ->
                val anchorIdx = when (item) {
                    is Item.Bare -> item.index
                    is Item.Card -> item.head
                    is Item.Header -> -1
                }
                if (anchorIdx >= 0) {
                    val t = list[anchorIdx].time
                    if (t.length >= 10) {
                        val d = t.substring(0, 10)
                        if (d != lastDate) {
                            finalItems.add(Item.Header(d))
                            lastDate = d
                        }
                    }
                }
                // finalItems.size 即本 item 即将占据的下标
                when (item) {
                    is Item.Bare -> map[item.index] = finalItems.size
                    is Item.Card -> (listOf(item.head) + item.members)
                        .forEach { map[it] = finalItems.size }
                    is Item.Header -> {}
                }
                finalItems.add(item)
            }
            return LogGroups(finalItems, map)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun LogScreen(
    modifier: Modifier,
    list: List<LogEntry>,
    listState: LazyListState = rememberLazyListState(),
    autoScrollToBottom: Boolean = false,
    // 非空时命中项加背景高亮(定位用，不过滤列表)
    searchQuery: String = "",
    // 多选模式：长按条目进入（1003；顶栏 ☑ 已删，退多选=返回键/复制后自动退）。跨条复制
    // 走本模式而非 compose selection——条目滚出视口被回收后选区仍悬挂其 selectableId，
    // 拖柄重算查表即崩（上游至今未修，详见 1002 记录）
    selectionMode: Boolean = false,
    // 勾选的日志（以条目对象为键：对列表增删/筛选重排免疫；time 毫秒级，同值碰撞可忽略）
    checkedEntries: Set<LogEntry> = emptySet(),
    onToggleCheck: (LogEntry) -> Unit = {},
    // 长按拖动多选（1003）：长按任意条目＝立刻进多选并勾上它，按住拖动＝划过的条目连续
    // 勾选、拖回缩小范围。仅 dragSelectEnabled=true 的页面启用——转发器日志没有多选 UI
    dragSelectEnabled: Boolean = false,
    onEnterSelection: () -> Unit = {},
    onCheckedChange: (Set<LogEntry>) -> Unit = {},
    // 归组结果（10-06 全链卡）：由 TtsLogScreen 传入同一实例（搜索跳转要用映射）；
    // 缺省内部构建。转发器按位置传参、不传此参，走内部构建、全裸行零影响
    groups: LogGroups? = null,
    // 「原文」定位键（用户 10-08 午后 甲方案）：非空=true=当前处于筛选/搜索态，
    // 卡右上角显示小键；点击回调把该条目交回上层（清筛选+跳完整流原位）
    showLocateKey: Boolean = false,
    onLocateOriginal: (LogEntry) -> Unit = {},
    // 定位高亮：命中条目黄底闪现（完整流里看到前后文），上层 2s 后清除
    locateHighlight: LogEntry? = null,
) {
    ControlBottomBarVisibility(listState, LocalBottomBarBehavior.current)
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val context = LocalContext.current
    // 非空时显示日志快捷面板（点请求卡触发，锚定"请求音频"主行）
    var quickPanelEntry by remember { mutableStateOf<LogEntry?>(null) }
    quickPanelEntry?.let { entry ->
        com.github.jing332.tts_server_android.compose.systts.log.LogQuickPanel(
            onDismissRequest = { quickPanelEntry = null },
            entry = entry,
        )
    }
    // 长按拖动手势要用到的最新值（pointerInput(Unit) 不随重组重启，一律走 State 读）
    val haptics by rememberUpdatedState(LocalHapticFeedback.current)
    val currentList by rememberUpdatedState(list)
    val currentChecked by rememberUpdatedState(checkedEntries)
    val currentOnEnterSelection by rememberUpdatedState(onEnterSelection)
    val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)

    // 归组结果：外部传入优先，否则内部构建（缓存随列表重建）
    val builtGroups = remember(list, groups) { groups ?: LogGroups.build(list) }
    val currentGroups by rememberUpdatedState(builtGroups)

    // 拖选笔画共享态：手势跑在 AwaitPointerEventScope（受限挂起作用域）里，编译器禁止
    // 在其中 launch 协程，边缘自动滚只能挂外部协程，靠这组状态桥接手势与滚动手
    val dragActive = remember { mutableStateOf(false) }
    val dragPointerY = remember { mutableStateOf(0f) }
    // 锚点/末位均为【列表项下标】（卡/裸行/日期签），不是条目下标
    val dragAnchor = remember { mutableStateOf(0) }
    val dragBase = remember { mutableStateOf<Set<LogEntry>>(emptySet()) }
    val dragLastIdx = remember { mutableStateOf(0) }

    // 指尖纵坐标 → 可视列表项下标；只认日志项（收尾 Spacer 不算），落在空档上就近取边界项
    fun logIndexAtY(y: Float): Int? {
        val items = listState.layoutInfo.visibleItemsInfo
            .filter { it.index < currentGroups.items.size }
        if (items.isEmpty()) return null
        val hit = items.firstOrNull { y >= it.offset && y < it.offset + it.size }
        return hit?.index
            ?: if (y < items.first().offset) items.first().index
            else items.last().index
    }

    // 以长按起点为锚的连续区间=追加勾选；拖回缩小只收回本次加的，之前勾的行不受影响。
    // 区间按列表项（卡/裸行）展开成条目集合：拖过一张卡=整组勾选
    fun emitDragSelection() {
        val g = currentGroups
        if (g.items.isEmpty()) return
        val lo = minOf(dragAnchor.value, dragLastIdx.value).coerceIn(0, g.items.lastIndex)
        val hi = maxOf(dragAnchor.value, dragLastIdx.value).coerceIn(0, g.items.lastIndex)
        var sel = dragBase.value
        for (k in lo..hi) {
            g.entriesOf(g.items[k]).forEach { idx ->
                if (idx in currentList.indices) sel = sel + currentList[idx]
            }
        }
        currentOnCheckedChange(sel)
    }

    fun updateDragSelection(y: Float) {
        logIndexAtY(y)?.let { idx ->
            if (idx != dragLastIdx.value) {
                dragLastIdx.value = idx
                emitDragSelection()
            }
        }
    }

    // 边缘自动滚：拖选中指尖压上下边缘时逐帧匀速滚，扫选屏外项；滚动后指尖下的项
    // 变了，选中范围跟着延伸/缩小。收笔（dragActive=false）即卸载
    val density = LocalDensity.current
    if (dragActive.value) {
        LaunchedEffect(density) {
            val edgeZone = with(density) { 48.dp.toPx() }
            val frameScroll = with(density) { 14.dp.toPx() }
            while (dragActive.value) {
                withFrameNanos { }
                val end = listState.layoutInfo.viewportEndOffset
                if (dragPointerY.value < edgeZone) listState.scrollBy(-frameScroll)
                else if (dragPointerY.value > end - edgeZone) listState.scrollBy(frameScroll)
                else continue
                updateDragSelection(dragPointerY.value)
            }
        }
    }
    Box(modifier) {
        val isAtBottom by remember {
            derivedStateOf {
                val layoutInfo = listState.layoutInfo
                val visibleItemsInfo = layoutInfo.visibleItemsInfo
                if (layoutInfo.totalItemsCount <= 0) {
                    true
                } else {
                    if (visibleItemsInfo.isEmpty()) true
                    else {
                        val lastVisibleItem = visibleItemsInfo.last()
                        lastVisibleItem.index > layoutInfo.totalItemsCount - 5
                    }
                }
            }
        }
        LaunchedEffect(list.size) {
            if (autoScrollToBottom && currentGroups.items.isNotEmpty())
                runCatching { listState.animateScrollToItem(currentGroups.items.lastIndex) }
        }

        if (list.isEmpty())
            Box(Modifier.align(Alignment.Center)) {
                Text(
                    text = stringResource(R.string.empty_list),
                    style = MaterialTheme.typography.titleMedium
                )
            }

        // 排版实验 1008（用户 10-08 拍板①）：成员行 forceColor 压制撤除，kidBody 色系
        // 退役——卡内恢复级别色/来源色（25cbf5f 的插件灰青/规则灰紫在卡内重新可见）
        val darkTheme = isSystemInDarkTheme()
        // 卡片底色（10-10 用户拍板：加深到 50%）。原值 surfaceVariant@20% 是照设置页分区卡
        // 对齐的，但那是「一张大卡占半屏」的页；日志是一屏十几条窄卡，20% 时卡底与页底
        // 亮度差仅 2.2%（#F7F8F0 vs #FDFDF6），肉眼分不出边界 ⇒ 用户反馈「卡片不清晰」。
        // 50% 与列表页卡片同浓度（亮度差 5.8%），边界一眼可见，色相仍是中性不抢正文。
        // 报错分档保留信号色：仅 WARN 淡琥珀、含 ERROR 红粉（信号色允许偏离底色系）
        val cardBgOk = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
        val cardBgWarn = if (darkTheme) Color(0xFF3A3226) else Color(0xFFFDF8E8)
        val cardBgErr = if (darkTheme) Color(0xFF3A2626) else Color(0xFFFDF0F0)
        // 获取成功前缀：石板灰 Blue Grey 800/200
        // 发音人信息：棕褐 #7D6B5D / 深色主题 #A08B7A
        val metaColor = if (darkTheme) Color(0xFFB0BEC5) else Color(0xFF37474F)
        val voiceColor = if (darkTheme) Color(0xFFA08B7A) else Color(0xFF7D6B5D)

        LazyColumn(
            Modifier
                .fillMaxSize()
                .then(
                    if (dragSelectEnabled) Modifier.pointerInput(Unit) {
                        // 手写 awaitEachGesture。等待长按阶段：本页躺在底栏横向翻页容器里，
                        // 长按期间手指轻微横漂会被外层 pager 抢走变成切页。
                        // 10-07 换栏修复：横移处理加时间闸——落指 150ms 内就越过 slop 的横移
                        // = 快速换栏意图，不消费直接放手让 pager 接管（10-06 版无条件消费，
                        // 把左右滑换栏整个废掉了）；150ms 后才慢慢漂过 slop = 按住时的轻微
                        // 横漂，就地消费保住长按。时间闸取长按超时（400ms）的前段：正常换栏
                        // 横滑几乎都在 150ms 内越过 slop，按住不动的手几乎不会。
                        // 纵向明确滚动立即放行（与 10-06 一致）。
                        // 超时未被取消 = 长按成立（withTimeoutOrNull null 返回值语义）
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val slop = viewConfiguration.touchSlop
                            val downAt = System.nanoTime()
                            val swipeGraceMs = 150L
                            val waited = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: break
                                    if (change.changedToUpIgnoreConsumed()) break
                                    if (change.isConsumed) break
                                    if (change.positionChangedIgnoreConsumed()) {
                                        val dx = change.position.x - down.position.x
                                        val dy = change.position.y - down.position.y
                                        if (abs(dy) > slop && abs(dy) >= abs(dx)) break // 纵向滚动意图 → 放行
                                        if (abs(dx) > slop || abs(dy) > slop) {
                                            val elapsedMs = (System.nanoTime() - downAt) / 1_000_000
                                            if (abs(dx) > abs(dy) && elapsedMs < swipeGraceMs)
                                                break // 快速横滑 → 不消费，pager 接管换栏
                                            change.consume() // 长按等待期横漂：pager 抢不走
                                        }
                                    }
                                }
                                "done"
                            }
                            val anchor = if (waited == null) logIndexAtY(down.position.y) else null
                            if (anchor == null) return@awaitEachGesture
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentOnEnterSelection()
                            dragAnchor.value = anchor
                            dragBase.value = currentChecked
                            dragLastIdx.value = anchor
                            dragPointerY.value = down.position.y
                            emitDragSelection()
                            dragActive.value = true
                            try {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: event.changes.first()
                                    // 抬手即收笔；UP 也消费掉，行点击（勾选/快捷面板）
                                    // 不会在松手瞬间再触发一次
                                    if (change.changedToUpIgnoreConsumed()) {
                                        change.consume()
                                        break
                                    }
                                    if (change.positionChangedIgnoreConsumed()) {
                                        change.consume()
                                        dragPointerY.value = change.position.y
                                        updateDragSelection(change.position.y)
                                    }
                                }
                            } finally {
                                dragActive.value = false
                            }
                        }
                    } else Modifier
                ),
            state = listState,
            // 左右基准线（10-10 用户拍板 B 案：统一 16）：容器仍 0，但**裸行/日期签文字/
            // 卡内正文三项同落 16dp 全站 ListGutter 内容线**。
            // 历史：10-05 恢复过「容器 0 + 行内 4」的旧版口径，当时卡片带 16dp 外边距、
            // 卡内文字在 24dp，裸行 4dp 属「贴边族」合理；10-10 卡片改底色通边（撤外边距、
            // 内衬 16）后卡内文字落到 16dp，裸行却没跟着动 ⇒ 裸行成了全页最靠左的一列
            //（比日期签还左），页面出现三条左线。此为那次改卡片漏掉的对齐。
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
                itemsIndexed(currentGroups.items, key = { index, _ -> index }) { _, item ->
                    when (item) {
                        is LogGroups.Item.Header -> {
                            // 日期签：每天第一条上方出现一次（10-06：日期不再逐行重复）。
                            // 签盒 start 7 + 签内衬 9 = 签文字 16，与裸行/卡内正文同线
                            Text(
                                text = item.date,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(start = 7.dp, top = 10.dp, bottom = 4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(9.dp)
                                    )
                                    .padding(horizontal = 9.dp, vertical = 2.dp)
                            )
                        }

                        is LogGroups.Item.Bare -> {
                            val log = list[item.index]
                            val checked = log in checkedEntries
                            val isMatch = isMatchEntry(log, searchQuery)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (selectionMode) Modifier.clickable { onToggleCheck(log) }
                                        else Modifier
                                    )
                                    .then(
                                        if (checked) Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                            )
                                        else if (isMatch) Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                            )
                                        else Modifier
                                    )
                                    // 10-10 B 案：裸行左缘 4→16，与卡内正文/日期签文字同落
                                    // 全站 ListGutter 16dp 线（原本 4dp 是卡片带外边距时代的
                                    // 「贴边族」口径，卡片改通边后失效）。纵向 3.5 未动。
                                    .padding(horizontal = 16.dp, vertical = 3.5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (selectionMode) {
                                        Icon(
                                            imageVector = if (checked) Icons.Default.CheckBox
                                            else Icons.Default.CheckBoxOutlineBlank,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 4.dp).size(16.dp),
                                            tint = if (checked) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(text = log.time, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = "\t${log.level.toLogLevelChar()}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                LogEntryBody(
                                    entry = log,
                                    darkTheme = darkTheme,
                                    metaColor = metaColor,
                                    voiceColor = voiceColor,
                                    // 排版实验 1008（A 二轮，用户 10-08 午后令）：主行 16→14sp
                                    fontSize = 14.sp,
                                    // 行距定版：统一字号×1.3 节奏（37731502442 修 CI 红：
                                    // 原 `fontSize * 1.3f` 引用的是 LogEntryBody 的命名参数，
                                    // 调用点作用域无此变量——Unresolved reference，静守卫盲区）
                                    lineHeight = 18.2.sp, // 14×1.3
                                    // 命中高亮已在整行背景，正文不再叠一层
                                    isMatch = false,
                                    highlight = log == locateHighlight,
                                )
                            }
                        }

                        is LogGroups.Item.Card -> {
                            val head = list[item.head]
                            val groupLogs = (listOf(item.head) + item.members).map { list[it] }
                            val groupChecked = groupLogs.all { it in checkedEntries }
                            // 报错三件套①：底色三档（灰/淡琥珀/红粉）
                            val cardBg = when (item.errorLevel) {
                                2 -> cardBgErr
                                1 -> cardBgWarn
                                else -> cardBgOk
                            }
                            Column(
                                modifier = Modifier
                                    // 卡片形态（10-10 用户拍板 B 案：缩进 8 + 圆角 8）：
                                    // 由「底色贴边」回到带缩进的圆角卡。圆角必须配缩进——铺满整屏时
                                    // 圆角会落在屏幕边缘被切掉，看着像缺角（两者不能并存）。
                                    // 全站规则「容器 + 行内 = 16」：卡缘 8 + 卡内衬 8 = 文字落 16dp，
                                    // 与裸行/日期签文字同一条 ListGutter 线（不破 10-10 刚统一的左线）。
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cardBg)
                                    .then(
                                        if (selectionMode) Modifier.clickable {
                                            // 卡=多选粒度：整组勾/整组取消
                                            if (groupChecked)
                                                onCheckedChange(checkedEntries - groupLogs.toSet())
                                            else
                                                onCheckedChange(checkedEntries + groupLogs.toSet())
                                        }
                                        // 非多选：点卡弹快捷面板（换发音人），锚定请求主行
                                        else Modifier.clickable { quickPanelEntry = head }
                                    )
                                    // 卡内水平衬 8dp（卡缘 8 + 此值 = 文字 16dp 全站 ListGutter 线；
                                    // 原贴边形态下这里曾直接扛 16 承担全部文字边距）
                                    .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                            ) {
                                // 请求主行（结构=时间行+正文）
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (selectionMode) {
                                        Icon(
                                            imageVector = if (groupChecked) Icons.Default.CheckBox
                                            else Icons.Default.CheckBoxOutlineBlank,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 4.dp).size(16.dp),
                                            tint = if (groupChecked) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(text = head.time, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = "\t${head.level.toLogLevelChar()}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    // 「原文」定位键（用户 10-08 甲方案）：筛选/搜索态显示，
                                    // 占行尾剩余空间靠右；点卡片主体仍是快捷面板，互不抢
                                    if (showLocateKey) {
                                        // 报错三件套③：粉卡在定位键前挂级别角标——不用展开
                                        // 就知道卡里是 E 还是 W（P2 收紧后分析行常落卡外，
                                        // 这是找报错卡最快的锚点）
                                        if (item.errorLevel > 0) {
                                            Text(
                                                text = if (item.errorLevel == 2) "E" else "W",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier
                                                    .padding(start = 6.dp)
                                                    .background(
                                                        if (item.errorLevel == 2) MaterialTheme.colorScheme.error
                                                        else Color(0xFFFFC107),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                        Spacer(Modifier.weight(1f))
                                        Text(
                                            text = "⟲ 原文",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(9.dp))
                                                .clickable { onLocateOriginal(head) }
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                LogEntryBody(
                                    entry = head,
                                    darkTheme = darkTheme,
                                    metaColor = metaColor,
                                    voiceColor = voiceColor,
                                    // 排版实验 1008（A 二轮，用户 10-08 午后令）：主行 16→14sp，
                                    // 半粗保留；与成员行 13sp/前置行 12sp 每档差 1sp 层层递减
                                    fontSize = 14.sp,
                                    // 行距定版：统一字号×1.3 节奏（37731502442 修 CI 红，同 587 行）
                                    lineHeight = 18.2.sp, // 14×1.3
                                    isMatch = isMatchEntry(head, searchQuery),
                                    // 排版实验 1008（C）：只染"请求音频："前缀，正文回默认色
                                    isRequestHead = true,
                                    highlight = head == locateHighlight,
                                )
                                // 主行与成员区分隔线（用户 10-08 午后追问补）：请求正文与
                                // 获取成功/插件过程行之间此前只有缩进，加一条 10% 透明度
                                // 细线标出"请求→结果"的内容分界
                                // 10-10 用户令：线与正文间距 6+6 太空（总隙 ≈19dp），收到 2+0
                                if (item.members.isNotEmpty()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                                    )
                                }
                                // 成员行：结果子行/插件过程行。排版实验 1008（拍板④，用户
                                // 10-08）：撤 10dp 缩进与主行平齐——归属已由卡+分隔线+字号
                                // 表达，缩进是第四重冗余，还压窄插件长句的可读宽度
                                item.members.forEach { mIdx ->
                                    val m = list[mIdx]
                                    Column(Modifier.padding(top = 2.dp)) {
                                        Row {
                                            Text(
                                                // 排版实验 1008（P3，用户 10-08 午后令）：成员行
                                                // 时间去日期——日期与卡顶日期签重复，只留时分秒毫秒
                                                text = m.time.substring(11),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "\t${m.level.toLogLevelChar()}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        LogEntryBody(
                                            entry = m,
                                            darkTheme = darkTheme,
                                            metaColor = metaColor,
                                            voiceColor = voiceColor,
                                            // 排版实验 1008（A）：成员行 14→13sp，与主行 16sp 拉开
                                            fontSize = 13.sp,
                                            lineHeight = 17.sp, // 13×1.3≈16.9
                                            isMatch = isMatchEntry(m, searchQuery),
                                            // 排版实验 1008（用户 10-08 拍板①）：撤 forceColor
                                            // 统一压制——卡内恢复各自行本来的级别色/来源色
                                            // （获取成功本就是石板灰；插件回灰青；规则回灰紫），
                                            // 层次交给字号 14/13 与分隔线；E 的错误加粗保留
                                            emphasizeError = true,
                                            highlight = m == locateHighlight,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.navigationBarsPadding())
                }
            }

        // 侧边浮动键（用户 1002）：右下浮动 ↓=回底部；回顶部走双击标题栏空白区
        // 排版实验 1008（①已拍板）：56dp 标准键 → 40dp 小键，原地缩小少盖正文
        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // 键缩小后 48dp 遗留外距把键顶离屏底太远（用户 10-08：太靠上）→20dp，
                // 拇指自然可及也不压底栏
                .padding(bottom = 20.dp),
            visible = !isAtBottom,
            enter = fadeIn() + expandIn(expandFrom = Alignment.BottomCenter),
            exit = shrinkOut(shrinkTowards = Alignment.BottomCenter) + fadeOut(),
        ) {
            SmallFloatingActionButton(
                modifier = Modifier.padding(8.dp),
                shape = CircleShape,
                onClick = {
                    scope.launch {
                        kotlin.runCatching {
                            listState.scrollToItem(currentGroups.items.lastIndex)
                        }
                    }
                }) {
                Icon(
                    Icons.Default.KeyboardDoubleArrowDown,
                    stringResource(id = R.string.move_to_bottom),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// 搜索命中判定（条目级）：正文或时间含搜索词
private fun isMatchEntry(e: LogEntry, q: String): Boolean =
    q.isNotEmpty() && (e.message.contains(q, ignoreCase = true) || e.time.contains(q, ignoreCase = true))

// 单条日志正文渲染：HTML → AnnotatedString + 哨兵色重映射，级别色或指定色
@Composable
private fun LogEntryBody(
    entry: LogEntry,
    darkTheme: Boolean,
    metaColor: Color,
    voiceColor: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    isMatch: Boolean,
    // 非空=成员行统一用卡内次级色（石板灰/粉卡暗红），级别色让位
    forceColor: Color? = null,
    // 排版实验 1008（C）：请求主行——"请求音频："前缀染深绿、其余回默认色。
    // 请求行 HTML 结构固定：`请求音频：` + <b>正文</b>（+ 哨兵色次级段），
    // 据此把首段（正文之前的裸文本）与前缀分开着色
    isRequestHead: Boolean = false,
    // 排版实验 1008（E）：卡内错误行加粗 + ⚠ 行首标，让错误在粉卡里突出于成功行
    emphasizeError: Boolean = false,
    // 「原文」定位命中（用户 10-08 甲方案）：黄底高亮闪现，由上层定时清除
    highlight: Boolean = false,
) {
    val spanned = remember(entry.message, darkTheme, metaColor, voiceColor, isRequestHead, emphasizeError) {
        val base = HtmlCompat.fromHtml(entry.message, HtmlCompat.FROM_HTML_MODE_COMPACT)
            .toAnnotatedString()
            .remapMetaColor(metaColor, voiceColor)
        var s = base
        if (isRequestHead) {
            // 排版实验 1008（字号真分层，用户 10-08 拍板）：主行内三段三个字号——
            // 前缀 13sp / 正文 14sp（<b> 加粗承担字重）/ 声音信息 12sp。
            // 段界：前缀=首个"："及之前；声音信息=哨兵色 span（remapMetaColor 后已是
            // 雾紫/石板灰目标色）；两者之间=正文。
            val prefixEnd = base.text.indexOf("：").let { if (it >= 0) it + 1 else 0 }
            // 声音信息段起点 = 首个哨兵色重映射 span 的 start（remapMetaColor 只对
            // MetaColorSentinel/VoiceMetaSentinel 换色，其余 span 不动，可靠定位）
            val metaStart = base.spanStyles
                .firstOrNull { it.item.color == voiceColor }?.start ?: base.text.length
            s = buildAnnotatedString {
                append(base.text)
                base.spanStyles.forEach { r ->
                    var item = r.item
                    if (r.start < prefixEnd) {
                        // 前缀：染深绿 + 13sp
                        item = item.copy(
                            color = if (darkTheme) RequestPrefixColorDark else RequestPrefixColorLight,
                            fontSize = 13.sp,
                        )
                    } else if (r.start >= metaStart && metaStart < base.text.length) {
                        // 声音信息：哨兵色已换好目标色，只压到 12sp
                        item = item.copy(fontSize = 12.sp)
                    }
                    addStyle(item, r.start, r.end)
                }
                // 前缀段可能无 span 覆盖（级别色是 Text 整体 color，不是 span）：
                // 显式补一个 13sp span
                if (prefixEnd > 0) addStyle(SpanStyle(fontSize = 13.sp), 0, prefixEnd)
                // 声音信息段若无 span 覆盖（整段哨兵色必有 span，此处兜底）：补 12sp
                if (metaStart < base.text.length) {
                    addStyle(SpanStyle(fontSize = 12.sp), metaStart, base.text.length)
                }
            }
        }
        // 报错三件套②：W 行与 E 行同获加粗+行首标记（E=✖ 重一级，W=⚠）——
        // 撤 forceColor 后粉底是唯一信号会漏掉黄警，行内自证
        if (emphasizeError && (entry.level == LogLevel.ERROR || entry.level == LogLevel.WARN)) {
            val mark = if (entry.level == LogLevel.ERROR) "✖ " else "⚠ "
            s = buildAnnotatedString {
                append(mark)
                append(s.text)
                s.spanStyles.forEach { addStyle(it.item, it.start + 2, it.end + 2) }
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), 0, s.text.length + 2)
            }
        }
        s
    }
    val bodyColor = forceColor ?: when {
        entry.level == LogLevel.SUCCESS -> metaColor
        // 排版实验 1008（C）：请求主行正文回默认色（前缀已单独染色），其余行照旧级别色
        isRequestHead -> MaterialTheme.colorScheme.onSurface
        else -> Color(entry.level.toArgb(isDarkTheme = darkTheme))
    }
    Text(
        text = spanned,
        color = bodyColor,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = fontSize,
            lineHeight = lineHeight,
            // 字号真分层后撤主行整体半粗：正文 <b> 自带字重，先前 SemiBold 叠加
            // 是"主行三段看着没分层"的根源（前缀/声音信息被衬得过轻）
        ),
        modifier = if (highlight) Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFFFE082))
        else if (isMatch) Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
        else Modifier
    )
}
