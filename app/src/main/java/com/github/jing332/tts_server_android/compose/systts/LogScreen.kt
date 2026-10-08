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

// 排版实验 1008（用户拍板 A/C/D/E，回退基线 7f0d647）：请求行"请求音频："前缀专用色
// （深绿），正文不再跟级别色——纯绿满屏太抢，只染前缀、书名/正文回默认色
private val RequestPrefixColorLight = Color(0xFF2E7D32)
private val RequestPrefixColorDark = Color(0xFF81C784)

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
 *   pre=开卡前悬置的朗读规则分析行（时间邻接归因：先有分析后有请求）；
 *   members=结果子行(indent>0)/插件过程行/卡打开期间的其他主行(重试/备用)。
 *   收卡条件：SUCCESS 结果、主行 ERROR（失败终点）、下一张卡开卡。
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
            val pre: List<Int>,
            val head: Int,
            val members: List<Int>,
            // 卡内任一成员 W/E（获取失败/重试/规则报警）→ 整卡粉底
            val isError: Boolean,
        ) : Item()
    }

    // 条目下标 → 列表项下标（搜索跳转/滚动定位用）；越界或未入组时兜底直用条目下标
    fun listItemFor(entryIndex: Int): Int =
        if (entryIndex in entryToList.indices && entryToList[entryIndex] >= 0) entryToList[entryIndex]
        else entryIndex

    fun entriesOf(item: Item): List<Int> = when (item) {
        is Item.Header -> emptyList()
        is Item.Bare -> listOf(item.index)
        is Item.Card -> item.pre + item.head + item.members
    }

    companion object {
        fun build(list: List<LogEntry>): LogGroups {
            val raw = ArrayList<Item>(list.size)
            var pre = ArrayList<Int>()
            var head = -1
            var members = ArrayList<Int>()
            var cardError = false

            fun closeCard() {
                if (head >= 0)
                    raw.add(Item.Card(pre.toList(), head, members.toList(), cardError))
                head = -1
                members = ArrayList()
                pre = ArrayList()
                cardError = false
            }
            fun markError(e: LogEntry) {
                if (e.level == LogLevel.WARN || e.level == LogLevel.ERROR) cardError = true
            }
            // 排版实验 1008（P2，用户 10-08 午后拍板）：规则分析行的"时间邻接"判定——
            // 只有望距新请求 ≤10s 的悬置分析行才配做该卡前置区；超 10s = AI 慢思考/隔批
            // 残留，与那张卡无因果，落裸行（修"分析成功挂在 29 秒前的旧请求卡里"误导）
            fun timeGapOk(analysisIdx: Int, requestIdx: Int): Boolean {
                fun millis(t: String): Long = runCatching {
                    val h = t.substring(11, 13).toLong()
                    val m = t.substring(14, 16).toLong()
                    val s = t.substring(17, 19).toLong()
                    val ms = t.substring(20, 23).toLong()
                    ((h * 60 + m) * 60 + s) * 1000 + ms
                }.getOrDefault(0L)
                val a = millis(list[analysisIdx].time)
                val r = millis(list[requestIdx].time)
                if (a == 0L || r == 0L) return true // 时间缺失不设防，走旧归组
                return r - a in 0..10_000
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
                    // 规则日志：卡内=过程行；卡外=悬置，等下一张卡做前置区
                    e.isSpeechRuleLog -> {
                        if (head >= 0) {
                            members.add(i); markError(e)
                        } else pre.add(i)
                    }
                    // "请求音频"主行：开新卡（旧卡先收，悬置分析行随卡归入前置区；
                    // P2 收紧：悬置行里距本请求超 10s 的先落裸行，不进前置区）
                    e.configId != 0L -> {
                        closeCard()
                        if (pre.isNotEmpty()) {
                            val attached = ArrayList<Int>()
                            pre.forEach { pIdx ->
                                if (timeGapOk(pIdx, i)) attached.add(pIdx)
                                else raw.add(Item.Bare(pIdx))
                            }
                            pre = attached
                        }
                        head = i
                    }
                    // 其他主行（重试/备用TTS/系统消息）：有卡归卡，ERROR=失败终点收卡；
                    // 无卡时先落袋悬置分析行（保持时序）再落裸行
                    else -> {
                        if (head >= 0) {
                            members.add(i); markError(e)
                            if (e.level == LogLevel.ERROR) closeCard()
                        } else {
                            pre.forEach { raw.add(Item.Bare(it)) }
                            pre = ArrayList()
                            raw.add(Item.Bare(i))
                        }
                    }
                }
            }
            closeCard()
            // 收尾悬置的分析行（后面没有请求跟上）落裸行，防丢失
            pre.forEach { raw.add(Item.Bare(it)) }

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
                    is Item.Card -> (item.pre + item.head + item.members)
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

        val darkTheme = isSystemInDarkTheme()
        // 卡片底色与成员字色（浅/深）
        val cardBgOk = if (darkTheme) Color(0xFF232527) else Color(0xFFF6F5F8)
        val cardBgErr = if (darkTheme) Color(0xFF3A2626) else Color(0xFFFDF0F0)
        val kidBodyColor = if (darkTheme) Color(0xFFA8B0B8) else Color(0xFF5F6A72)
        val kidBodyErrColor = if (darkTheme) Color(0xFFD99090) else Color(0xFF8C4A4A)
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
            // 左右基准线：维持 10-05 恢复的旧版口径（容器 0），卡自带 6dp 外距、
            // 裸行/日期签 4~8dp 内距，不再额外加容器 gutter
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
                itemsIndexed(currentGroups.items, key = { index, _ -> index }) { _, item ->
                    when (item) {
                        is LogGroups.Item.Header -> {
                            // 日期签：每天第一条上方出现一次（10-06：日期不再逐行重复）
                            Text(
                                text = item.date,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(start = 8.dp, top = 10.dp, bottom = 4.dp)
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
                                    .padding(horizontal = 4.dp, vertical = 3.5.dp)
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
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.15f, // 排版实验 1008（P1）：0.9→1.15 解正文/发音人段挤压
                                    // 命中高亮已在整行背景，正文不再叠一层
                                    isMatch = false,
                                    highlight = log == locateHighlight,
                                )
                            }
                        }

                        is LogGroups.Item.Card -> {
                            val head = list[item.head]
                            val groupLogs = (item.pre + item.head + item.members).map { list[it] }
                            val groupChecked = groupLogs.all { it in checkedEntries }
                            val cardBg = if (item.isError) cardBgErr else cardBgOk
                            Column(
                                modifier = Modifier
                                    // 排版实验 1008（D）：卡外距 6→10dp，两侧留白与密钥页口径靠拢
                                    // 用户 10-08 午后补令：底色必须撑满右缘——Column 缺 fillMaxWidth
                                    // 时按内容收缩，短文本卡右侧露底色空档
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                                    .clip(RoundedCornerShape(12.dp))
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
                                    .padding(start = 11.dp, end = 11.dp, top = 8.dp, bottom = 8.dp)
                            ) {
                                // 前置区：本次请求前的规则分析行（级别色照旧，字号小一档）
                                if (item.pre.isNotEmpty()) {
                                    item.pre.forEach { preIdx ->
                                        val p = list[preIdx]
                                        LogEntryBody(
                                            entry = p,
                                            darkTheme = darkTheme,
                                            metaColor = metaColor,
                                            voiceColor = voiceColor,
                                            // 排版实验 1008（A）：前置分析行 13→12sp（第三档）
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                            isMatch = isMatchEntry(p, searchQuery),
                                            highlight = p == locateHighlight,
                                        )
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                                    )
                                }
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
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.15f, // 排版实验 1008（P1）：0.9→1.15 解正文/发音人段挤压
                                    isMatch = isMatchEntry(head, searchQuery),
                                    // 排版实验 1008（C）：只染"请求音频："前缀，正文回默认色
                                    isRequestHead = true,
                                    highlight = head == locateHighlight,
                                )
                                // 主行与成员区分隔线（用户 10-08 午后追问补）：请求正文与
                                // 获取成功/插件过程行之间此前只有缩进，加一条与前置区同款
                                // 细线（10% 透明度）标出"请求→结果"的内容分界
                                if (item.members.isNotEmpty()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                                    )
                                }
                                // 成员行：结果子行/插件过程行，缩进+小一档+卡内次级色
                                item.members.forEach { mIdx ->
                                    val m = list[mIdx]
                                    Column(Modifier.padding(start = 10.dp, top = 6.dp)) {
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
                                            lineHeight = 18.sp,
                                            isMatch = isMatchEntry(m, searchQuery),
                                            forceColor = if (item.isError) kidBodyErrColor else kidBodyColor,
                                            // 排版实验 1008（E）：卡内错误行加粗+⚠，突出于成功行
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
                .padding(48.dp),
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
        if (emphasizeError && entry.level == LogLevel.ERROR) {
            s = buildAnnotatedString {
                append("⚠ ")
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
