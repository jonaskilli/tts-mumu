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
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
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
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.core.text.HtmlCompat
import com.github.jing332.common.LogEntry
import com.github.jing332.common.LogLevel
import com.github.jing332.common.toArgb
import com.github.jing332.common.toLogLevelChar
import com.github.jing332.compose.ComposeExtensions.toAnnotatedString
import com.github.jing332.compose.widgets.ControlBottomBarVisibility
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.ListGutter
import com.github.jing332.tts_server_android.compose.LocalBottomBarBehavior
import kotlinx.coroutines.launch

// SystemTtsService 拼接次级信息所用哨兵色，此处按主题重映射
private val MetaColorSentinel = Color(0xFFFF00FF)       // 获取成功前缀 → 石板灰
private val VoiceMetaSentinel = Color(0xFF00FFFF)       // 发音人信息 → 雾紫

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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
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
    // 长按拖动多选（1003，用户实机反馈「不符合正常操作习惯」后改）：长按任意条目＝
    // 立刻进多选并勾上它，按住拖动＝划过的条目连续勾选、拖回缩小范围；条目内「拖选
    // 几个字」随本次改动取消（用户确认基本不用，长按手势让位给多选）。
    // 仅 dragSelectEnabled=true 的页面启用——转发器日志没有多选 UI，开了只有触感没反应
    dragSelectEnabled: Boolean = false,
    onEnterSelection: () -> Unit = {},
    onCheckedChange: (Set<LogEntry>) -> Unit = {},
) {
    ControlBottomBarVisibility(listState, LocalBottomBarBehavior.current)
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val context = LocalContext.current
    // 非空时显示日志快捷面板（点带 configId 的请求主行触发）
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

    // 拖选笔画共享态：手势跑在 AwaitPointerEventScope（受限挂起作用域）里，编译器禁止
    // 在其中 launch 协程，边缘自动滚只能挂外部协程，靠这组状态桥接手势与滚动手
    val dragActive = remember { mutableStateOf(false) }
    val dragPointerY = remember { mutableStateOf(0f) }
    val dragAnchor = remember { mutableStateOf(0) }
    val dragBase = remember { mutableStateOf<Set<LogEntry>>(emptySet()) }
    val dragLastIdx = remember { mutableStateOf(0) }

    // 指尖纵坐标 → 可视行下标；只认日志行（收尾 Spacer 不算），落在空档/Spacer 上就近取边界行
    fun logIndexAtY(y: Float): Int? {
        val items = listState.layoutInfo.visibleItemsInfo
            .filter { it.index < currentList.size }
        if (items.isEmpty()) return null
        val hit = items.firstOrNull { y >= it.offset && y < it.offset + it.size }
        return hit?.index
            ?: if (y < items.first().offset) items.first().index
            else items.last().index
    }

    // 以长按起点为锚的连续区间=追加勾选；拖回缩小只收回本次加的，之前勾的行不受影响
    fun emitDragSelection() {
        val l = currentList
        if (l.isEmpty()) return
        val lo = minOf(dragAnchor.value, dragLastIdx.value).coerceIn(0, l.lastIndex)
        val hi = maxOf(dragAnchor.value, dragLastIdx.value).coerceIn(0, l.lastIndex)
        currentOnCheckedChange(dragBase.value + l.subList(lo, hi + 1).toSet())
    }

    fun updateDragSelection(y: Float) {
        logIndexAtY(y)?.let { idx ->
            if (idx != dragLastIdx.value) {
                dragLastIdx.value = idx
                emitDragSelection()
            }
        }
    }

    // 边缘自动滚：拖选中指尖压上下边缘时逐帧匀速滚，扫选屏外行；滚动后指尖下的行
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
        // 是否停靠在日志最开始（决定 ↑ 键显隐）：真正到顶（第 0 条且未偏移）才隐藏
        val isAtTop by remember {
            derivedStateOf {
                val layoutInfo = listState.layoutInfo
                val first = layoutInfo.visibleItemsInfo.firstOrNull()
                layoutInfo.totalItemsCount <= 0 ||
                        (first != null && first.index == 0 && first.offset == 0)
            }
        }

        LaunchedEffect(list.size) {
            if (autoScrollToBottom && list.isNotEmpty())
                listState.animateScrollToItem(list.size - 1)
        }

        if (list.isEmpty())
            Box(Modifier.align(Alignment.Center)) {
                Text(
                    text = stringResource(R.string.empty_list),
                    style = MaterialTheme.typography.titleMedium
                )
            }

        val darkTheme = isSystemInDarkTheme()
        LazyColumn(
            Modifier
                .fillMaxSize()
                .then(
                    if (dragSelectEnabled) Modifier.pointerInput(Unit) {
                        // 手写 awaitEachGesture：等待长按期间不消费任何事件，滑动一旦被
                        // 滚动容器消费即取消（不抢正常翻页）；长按成立后改在 Initial 段
                        // 消费事件（先于子节点），行点击与滚动都让位给拖动多选。
                        // 受限挂起作用域：循环里只允许本作用域的挂起调用与普通函数，
                        // 边缘自动滚在外部 LaunchedEffect（见 dragActive 桥接）
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            // null＝滚动先启动或提前抬手，本次不做多选
                            val longPress = awaitLongPressOrCancellation(down.id)
                                ?: return@awaitEachGesture
                            val anchor = logIndexAtY(longPress.position.y)
                                ?: return@awaitEachGesture
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentOnEnterSelection()
                            dragAnchor.value = anchor
                            dragBase.value = currentChecked
                            dragLastIdx.value = anchor
                            dragPointerY.value = longPress.position.y
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
            // 左右基准线 16（v3 边距统一）：正文不再贴屏，与顶部搜索控制行（16）同线；
            // 行内水平 padding 随之归零（下方），裸内容直接落 gutter 线
            contentPadding = PaddingValues(horizontal = ListGutter)
        ) {
                itemsIndexed(list, key = { index, _ -> index }) { index, log ->
                    // 获取成功前缀：石板灰 Blue Grey 800/200
                    // 发音人信息：棕褐 #7D6B5D / 深色主题 #A08B7A
                    val metaColor = if (darkTheme) Color(0xFFB0BEC5) else Color(0xFF37474F)
                    val voiceColor = if (darkTheme) Color(0xFFA08B7A) else Color(0xFF7D6B5D)
                    val style = MaterialTheme.typography.bodyMedium
                    val spanned = remember(log.message, darkTheme, metaColor, voiceColor) {
                        HtmlCompat.fromHtml(log.message, HtmlCompat.FROM_HTML_MODE_COMPACT)
                            .toAnnotatedString()
                            // 获取成功前缀→石板灰，发音人信息→棕褐
                            .remapMetaColor(metaColor, voiceColor)
                    }

                    // 折叠计数（用户 09-09）：连续同模式的插件/规则日志显示「… ×N」，
                    // N 为被合并的行数；message 已是该串最后一条，内容仍是最新的
                    val display = if (log.repeatCount > 1) buildAnnotatedString {
                        append(spanned)
                        append("\u2002×${log.repeatCount}")
                    } else spanned

                    // 正文着色（09-13 终版：插件/规则日志与「请求音频」等普通 INFO 完全同色——
                    // 即级别色 INFO=绿；此前 onSurface/彩色方案两轮被否后仍不齐，
                    // 要求"跟请求音频一模一样"，直接取消特判。SUCCESS 维持石板灰）
                    val bodyColor = when {
                        log.level == LogLevel.SUCCESS -> metaColor
                        else -> Color(log.level.toArgb(isDarkTheme = darkTheme))
                    }

                    // 搜索命中项加背景高亮；搜索是定位不是过滤，列表保持完整可上下翻看前后文
                    val isMatch = searchQuery.isNotEmpty() &&
                            (log.message.contains(searchQuery, ignoreCase = true) ||
                                    log.time.contains(searchQuery, ignoreCase = true))

                    // 每条日志之间画分隔线（无水平 inset：与行文字同跨 gutter 全宽，左右缘对齐）
                    if (index > 0)
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                    // 多选：点条目=勾选（越权操作不进快捷面板）；非多选：点带
                    // configId 的请求主行弹快捷面板。多选入口=长按拖动（容器手势）
                    val checked = log in checkedEntries
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (selectionMode) Modifier.clickable { onToggleCheck(log) }
                                else if (log.configId != 0L) Modifier.clickable {
                                    quickPanelEntry = log
                                } else Modifier
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
                            // 水平内边距归零（v3 边距统一）：容器 gutter 16 即裸内容线，
                            // 行内不再叠加；高亮圆角块随行全宽（16..344）。纵向 3.5 保留不动
                            .padding(
                                top = 3.5.dp,
                                bottom = 3.5.dp
                            )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectionMode) {
                                // 行首勾选指示：16dp 图标（=bodySmall 行高）而非 M3 Checkbox——
                                // ① Checkbox 强制 48dp 最小触控尺寸，会把日志行撑高、进多选整列跳位；
                                // ② 图标不进正文行、只在时间戳行左缘，长日志正文宽度不变不重排。
                                // 位处不改行高，进多选后正在看的位置原地不动（用户 1002 点名）
                                Icon(
                                    imageVector = if (checked) Icons.Default.CheckBox
                                    else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 4.dp).size(16.dp),
                                    tint = if (checked) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // 完整时间戳(年月日+时分秒+毫秒)，等级字母跟在时间后
                            Text(text = log.time, style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "\t${log.level.toLogLevelChar()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        // 正文纯文本（1003）：条目内选字取消——长按让位给拖动多选（用户
                        // 确认基本不用），跨条复制全走多选模式
                        Text(
                            text = display,
                            // 获取成功(SUCCESS)整行石板灰同字重(用户:冒号前后一致不加粗)；加粗仅保留请求文本正文
                            color = bodyColor,
                            style = style,
                            lineHeight = style.lineHeight * 0.9f,
                        )
                    }
                }
                item {
                    Spacer(Modifier.navigationBarsPadding())
                }
            }

        // 侧边浮动键（用户 1002）：向下=回底部（原键），向上=到日志最开始（新增）。
        // 竖排堆叠、各按需显隐：不在底部才显示↓，不在顶部才显示↑，都在中间时两键都可见。
        // 外层 48dp + 各键 8dp 与原先单键位置的算法保持一致（↓ 单独显示时位置不变）
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = !isAtTop,
                enter = fadeIn() + expandIn(expandFrom = Alignment.BottomCenter),
                exit = shrinkOut(shrinkTowards = Alignment.BottomCenter) + fadeOut(),
            ) {
                FloatingActionButton(
                    modifier = Modifier.padding(8.dp),
                    shape = CircleShape,
                    onClick = {
                        scope.launch {
                            kotlin.runCatching {
                                listState.scrollToItem(0)
                            }
                        }
                    }) {
                    Icon(
                        Icons.Default.KeyboardDoubleArrowUp,
                        stringResource(id = R.string.move_to_top)
                    )
                }
            }
            AnimatedVisibility(
                visible = !isAtBottom,
                enter = fadeIn() + expandIn(expandFrom = Alignment.BottomCenter),
                exit = shrinkOut(shrinkTowards = Alignment.BottomCenter) + fadeOut(),
            ) {
                FloatingActionButton(
                    modifier = Modifier.padding(8.dp),
                    shape = CircleShape,
                    onClick = {
                        scope.launch {
                            kotlin.runCatching {
                                listState.scrollToItem(list.size - 1)
                            }
                        }
                    }) {
                    Icon(
                        Icons.Default.KeyboardDoubleArrowDown,
                        stringResource(id = R.string.move_to_bottom)
                    )
                }
            }
        }
    }
}
