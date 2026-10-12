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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.github.jing332.tts_server_android.compose.systts.role.softContainerColor
import com.github.jing332.tts_server_android.compose.LocalBottomBarBehavior
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

// SystemTtsService 拼接次级信息所用哨兵色，此处按主题重映射
private val MetaColorSentinel = Color(0xFFFF00FF)       // 获取成功前缀 → 石板灰
private val VoiceMetaSentinel = Color(0xFF00FFFF)       // 发音人信息 → 雾紫
private val RoleChipSentinel = Color(0xFF0F0F0F)        // 角色名小牌 → 灰绿牌字色
private val PluginChipSentinel = Color(0xFF0E0E0E)      // 插件名小牌 → 灰青牌字色

// 排版实验 1008（用户 10-08 拍板「撤吧，灰」）：请求行"请求音频："前缀撤级别绿——
// 每卡必有、信息量为零的词占最显眼色正好反了（9-13 去彩色化同方向），改中性灰，
// 与时间行同档；黄警/红错的对比度由此上来。字号分层保留（前缀 13sp）
private val RequestPrefixColorLight = Color(0xFF79747E)
private val RequestPrefixColorDark = Color(0xFF938F99)

// 把命中哨兵色的段落整体换成目标色，让"请求音频"正文(纯绿)与
// 获取成功前缀(石板灰)/发音人信息(雾紫)/角色牌(灰绿)/插件牌(灰青)层次分明但不抢眼
private fun AnnotatedString.remapMetaColor(
    metaColor: Color,
    voiceColor: Color,
    roleChipColor: Color,
    pluginChipColor: Color,
): AnnotatedString {
    if (spanStyles.none {
            it.item.color == MetaColorSentinel || it.item.color == VoiceMetaSentinel ||
                    it.item.color == RoleChipSentinel || it.item.color == PluginChipSentinel
        }) return this
    return buildAnnotatedString {
        append(this@remapMetaColor.text)
        spanStyles.forEach { r ->
            val newColor = when (r.item.color) {
                MetaColorSentinel -> metaColor
                VoiceMetaSentinel -> voiceColor
                RoleChipSentinel -> roleChipColor
                PluginChipSentinel -> pluginChipColor
                else -> r.item.color
            }
            addStyle(
                r.item.copy(color = newColor),
                r.start,
                r.end
            )
            // 角色名小牌补底色+字重（10-11 用户令「角色名不突出」二轮加强）：HTML font
            // 标签给不了背景，仅字色牌感太弱。此处按哨兵段落叠同色底 span + Medium 字重
            //（SpanStyle 背景是直角矩形，无圆角——行内小牌可接受）。
            // 底色取牌字色 alpha 0.22（0.14 太淡，真机几乎看不出）：白页上=浅灰绿/浅灰青，
            // 深色页=字色本已调亮，同式成立。
            // 插件名不叠底（10-10 用户拍板）：改括号备注语缀尾，附属信息不与角色名平级
            when (r.item.color) {
                RoleChipSentinel -> addStyle(
                    SpanStyle(
                        background = roleChipColor.copy(alpha = 0.22f),
                        fontWeight = FontWeight.Medium,
                    ), r.start, r.end
                )
                else -> {}
            }
        }
    }
}

/**
 * 日志归组模型（10-06 全链卡改版，10-10 行流改版）。渲染单位从"逐条日志"升级为归组项：
 * - Group：一次请求管线。head="请求音频"主行（configId≠0，MDC 只在请求行携带）；
 *   members=结果子行(indent>0)/插件过程行/卡打开期间的其他主行(重试/备用)。
 *   收组条件：SUCCESS 结果、主行 ERROR（失败终点）、下一请求开组。
 *   朗读规则日志不进组（10-10 用户令）：一律落裸行——原「卡外悬置→下一张卡做前置区」
 *   在搜索/筛选的收窄列表里等不到开卡，是「勾了朗读规则搜不到」的根因。
 *   10-10 行流改版：渲染不再画卡（容器全撤），归组仅用于①头行药丸推导（重试/切备用/
 *   失败状态从 members 推）②搜索跳转落整组 ③多选整组勾选。
 * - Bare：与请求挨不上的条目——插件/规则散条、系统消息、头部被级别筛掉的孤儿子行。
 * - Header：日期签，每天第一条上方出现一次。
 *
 * 纯函数、确定性；TtsLogScreen 与 LogScreen 共用同一实例（搜索跳转要用条目→列表项
 * 映射）。转发器日志无 configId/indent/插件标记，全部落裸行、零影响。
 */
internal class LogGroups(
    val items: List<Item>,
    val entryToList: IntArray,
    // 以失败收场的组头行下标（10-10 行级丸配套）：请求行本身是 INFO，失败丸「失败」的
    // 依据是「该组以失败收场」——ERROR 子行/主行收卡时由 build 记入
    val failedHeads: Set<Int> = emptySet(),
) {
    sealed class Item {
        class Header(val date: String) : Item()
        class Bare(val index: Int) : Item()

        // 一次请求管线的归组（10-10 行流改版：渲染层不再画卡，只保留归组供
        // 搜索跳转/多选粒度/头行药丸推导用）。head=「请求音频」主行；
        // members=结果子行(indent>0)/插件过程行/其他主行(重试/备用/失败)。
        // 收组条件与旧卡一致：SUCCESS 结果、主行 ERROR（失败终点）、下一请求开组。
        // absorbError=被本组吸收的源错误条目下标（10-11 老版式定稿）：源错误条目含
        // 请求全文，失败时以它为主体渲染（胶囊源错误/英文红/正文红粗/发音人不红），
        // 请求主行正文不再重复显示——同一次失败一条说完
        class Group(
            val head: Int,
            val members: List<Int>,
            val absorbError: Int = -1,
            // 本组内发生过的重试次数（10-12 用户令「重试和失败是紧密联系的，别单独占一行」）：
            // 重试行不再落成独立行，改在组头行灯位后挂一枚「重试N」小丸；
            // 组已收卡时挂到刚收的那张组上（见 build 里 retry 分支）
            val retryCount: Int = 0,
        ) : Item()
    }

    // 条目下标 → 列表项下标（搜索跳转/滚动定位用）；越界或未入组时兜底直用条目下标
    fun listItemFor(entryIndex: Int): Int =
        if (entryIndex in entryToList.indices && entryToList[entryIndex] >= 0) entryToList[entryIndex]
        else entryIndex

    fun entriesOf(item: Item): List<Int> = when (item) {
        is Item.Header -> emptyList()
        is Item.Bare -> listOf(item.index)
        // ⚠️ 吸收条必须算进本组（10-12 用户实报「有些信息无法复制了」）：absorbError 是
        // 失败全文条（真因 + 正文 + 发音人信息），10-11 起由组头代画、**不进 members**——
        // 于是组的勾选集合（原 head+members）拿不到它，勾上整组复制/搜索定位都少了真因那一行。
        is Item.Group -> listOf(item.head) + item.members +
            listOfNotNull(item.absorbError.takeIf { it >= 0 })
    }

    companion object {
        fun build(list: List<LogEntry>): LogGroups {
            val raw = ArrayList<Item>(list.size)
            var head = -1
            var members = ArrayList<Int>()
            // 失败组头行标记（10-10 行级丸配套）：请求行本身是 INFO，失败丸（失败）的依据
            // 是「该组以失败收场」——ERROR 子行/主行收卡时把 head 记进来，渲染侧查此表
            val failedHeads = HashSet<Int>()
            var groupFailed = false
            // 连续同文源错误去重（10-11 老版式定稿配套）：记住上一条 ERROR 子行去 HTML
            // 后的全文，重试风暴同文即丢；跨请求/文案变化重置
            var lastErrorStripped: String? = null
            // 老版式失败条（10-11 定稿）：本次开卡以来最近一条 ERROR 子行下标——
            // 源错误条目含请求全文，收卡时它是组的渲染主体（absorbError）
            var errMemberIdx = -1
            // 本组内重试次数（10-12）：重试行不单独占行，挂到组头做小丸
            var retryCount = 0

            fun closeCard() {
                if (head >= 0) {
                    if (groupFailed) {
                        failedHeads.add(head)
                        raw.add(Item.Group(head, members.toList(), absorbError = errMemberIdx, retryCount = retryCount))
                    } else {
                        raw.add(Item.Group(head, members.toList(), retryCount = retryCount))
                    }
                }
                head = -1
                members = ArrayList()
                groupFailed = false
                errMemberIdx = -1
                retryCount = 0
            }
            fun openCard(requestIdx: Int) {
                closeCard()
                head = requestIdx
            }

            list.forEachIndexed { i, e ->
                when {
                    // ⚠️ 插件日志判断必须在 indent 分支之前：插件行天然 indent=1（子行形态），
                    // 走 indent 分支会进组——与「一律独立裸行」的终令相悖
                    // 插件日志一律独立裸行（10-10 用户终令「插件日志要拆开，一条归一条，
                    // 不并入请求组」）：无论何时产生（请求中/失败链期间）都不进组、不进
                    // 悬置链——与其他日志同格式独立成行（时间头/12sp/分隔线/定位键全同款）。
                    e.isPluginLog -> raw.add(Item.Bare(i))
                    // ⚠️ ERROR 结果子行（源错误）=失败终点（10-11 用户定稿「拿不准就按老版本」）：
                    // 源错误条目本就含请求全文（服务端老版格式：错误+<br>+正文+发音人信息），
                    // 归组成员区隐藏主行只渲染本条——即老版观感「胶囊源错误/英文红/正文红粗/
                    // 发音人不红」一条说完。收卡=失败终点（groupFailed，渲染失败丸）。
                    // 连续同文源错误去重（真机实锤：重试风暴同一句错误+正文原样刷两轮）：
                    // 与上一条 ERROR 同文（去 HTML 后）则整条丢弃——重试必然同死法零新信息，
                    // 文案变了（超时→401）照常显示。
                    e.indent > 0 && e.level == LogLevel.ERROR -> {
                        val hadHead = head >= 0
                        if (hadHead) {
                            groupFailed = true
                            errMemberIdx = i
                        }
                        val stripped = e.message.replace(Regex("<[^>]*>"), "")
                        val isDup = !stripped.isNullOrBlank() && stripped == lastErrorStripped
                        lastErrorStripped = stripped
                        closeCard()
                        // 有卡=已被组吸收（absorbError 渲染），不再落裸行避免双显；
                        // 无卡=请求头被筛掉的孤儿，照旧裸行兜底
                        if (!hadHead && !isDup) raw.add(Item.Bare(i))
                    }
                    // 结果/子行：有卡归卡（SUCCESS 顺带收卡）；无卡=头部被筛掉的孤儿 → 裸行
                    e.indent > 0 -> {
                        if (head >= 0) {
                            members.add(i)
                            if (e.level == LogLevel.SUCCESS) closeCard()
                        } else raw.add(Item.Bare(i))
                    }
                    // 规则日志一律落裸行（10-10 用户令：不进卡）。原「卡外悬置→下一张卡
                    // 做前置区」的机制整拆：搜索/筛选时收窄列表里经常没有跟得上的请求行，
                    // 悬置行等不到开卡就渲染不出来（表现为「勾了朗读规则也搜不到」）；
                    // 卡内也不再吸收规则行，避免分析行挂在无关请求卡顶造成误导
                    e.isSpeechRuleLog -> raw.add(Item.Bare(i))
                    // "请求音频"主行：开新卡（旧卡先收）
                    e.configId != 0L -> openCard(i)
                    // 其他主行（重试/备用TTS/兜底发音人/系统消息）：有卡归卡，ERROR=失败
                    // 终点收卡（标记 groupFailed）；无卡落裸行——失败链不再悬置并组（10-10 终案）
                    else -> {
                        // ⚠️ 重试行先拦（10-12 用户令「重试没别的信息，单独占一行浪费空间，
                        // 它和失败是紧密联系的」）：不落行、只把次数挂到组头做「重试N」小丸。
                        // 两种落点：①本组还开着 → 记在本次开卡上且**不关组**（后面那次失败与
                        // 本次重试同属一条失败链）；②上一次失败已收卡 → 挂到刚收的那张组上。
                        // 前面既没开卡也没有组（头部被筛掉的孤儿）→ 退回裸行，不至于把日志吞掉
                        val isRetryRow = e.level == LogLevel.WARN && e.message.contains("次重试")
                        if (isRetryRow) {
                            if (head >= 0) {
                                retryCount++
                            } else {
                                // ⚠️ 挂到「上一张组」前要验明身份（10-12 自审）：重试必然跟在一次
                                // 失败之后，所以只认**失败组**（failedHeads 记过），且与它最后一条
                                // 相隔不超过 60 秒——否则请求行被筛选掉时，重试会挂到上一张不相干
                                // 的组上（跨句错挂）。两条都不满足就退回裸行，宁可多一行也别错挂
                                val last = raw.lastOrNull()
                                val lastIdx = (last as? Item.Group)?.let { g ->
                                    if (failedHeads.contains(g.head)) {
                                        val tailIdx = if (g.absorbError >= 0) g.absorbError
                                        else g.members.lastOrNull() ?: g.head
                                        list[tailIdx].time
                                    } else null
                                }
                                if (last is Item.Group && lastIdx != null && withinSeconds(lastIdx, e.time, 60)) {
                                    raw[raw.size - 1] = Item.Group(
                                        last.head, last.members, last.absorbError, last.retryCount + 1
                                    )
                                } else {
                                    raw.add(Item.Bare(i))
                                }
                            }
                        } else if (head >= 0) {
                            // 老版式失败全文条（10-11）：本条含请求全文（错误 + <br> +
                            // 正文 + 发音人信息）时，让它当本组渲染主体（absorbError）——
                            // 渲染侧改画它、不再画请求主行正文，避免同一段正文出现两遍。
                            // ⚠️ 此时**不进 members**：absorbError 与成员区都会画一遍
                            //（成员区只跳过 SUCCESS），进了就双重渲染。
                            // 用「含 <br>」判别：配置为空/系统消息这类无正文的 ERROR
                            // 不进吸收（保持原样：主行正文照旧显示）。
                            val isFullFail = e.level == LogLevel.ERROR && e.message.contains("<br>")
                            if (isFullFail) {
                                groupFailed = true
                                errMemberIdx = i
                            } else {
                                members.add(i)
                                if (e.level == LogLevel.ERROR) groupFailed = true
                            }
                            closeCard()
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
                    is Item.Group -> item.head
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
                    is Item.Group -> (listOf(item.head) + item.members)
                        .forEach { map[it] = finalItems.size }
                    is Item.Header -> {}
                }
                finalItems.add(item)
            }
            return LogGroups(finalItems, map, failedHeads)
        }
    }
}

@OptIn(
    ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class // FlowRow（头部药丸链换行）
)
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
        // 回底键出现时机 A+B（10-10 装机反馈用户拍板）：
        // A=离底超过 15 条目（约一屏半）才显示——翻一两行找东西不再闪键，翻远了才给一键直达；
        // B=不在底部时有新日志到达（size 超过「上次在底部时」的基线）也显示——翻旧日志时
        //   新请求进来，箭头提示「下面有新的」，一按即回。
        // 旧逻辑=只要不在最底就常驻（出现太勤，用户反馈「时机该改改」）。
        // 基线 lastSeenSizeAtBottom 在 isAtBottom=true 时同步；按键回底后 isFarFromBottom
        // 转否且基线已同步，键自然消失。
        var lastSeenSizeAtBottom by remember { mutableStateOf(list.size) }
        val isFarFromBottom by remember {
            derivedStateOf {
                val layoutInfo = listState.layoutInfo
                val visibleItemsInfo = layoutInfo.visibleItemsInfo
                if (layoutInfo.totalItemsCount <= 0 || visibleItemsInfo.isEmpty()) {
                    false
                } else {
                    val lastVisibleItem = visibleItemsInfo.last()
                    // 离底 15 条目以上=一屏开外（行流单条约 40~60dp，15 条≈一屏半）
                    layoutInfo.totalItemsCount - 1 - lastVisibleItem.index > 15
                }
            }
        }
        val isAtBottom by remember {
            derivedStateOf {
                val layoutInfo = listState.layoutInfo
                val visibleItemsInfo = layoutInfo.visibleItemsInfo
                if (layoutInfo.totalItemsCount <= 0 || visibleItemsInfo.isEmpty()) true
                else visibleItemsInfo.last().index >= layoutInfo.totalItemsCount - 1
            }
        }
        // 滚回底部（或本来就在底）时同步基线：此后 size 增长才被视为「新到达」
        LaunchedEffect(isAtBottom) {
            if (isAtBottom) lastSeenSizeAtBottom = list.size
        }
        val hasNewBelow = list.size > lastSeenSizeAtBottom && !isAtBottom
        val showJumpToBottom = isFarFromBottom || hasNewBelow
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
        // 退役——行流恢复级别色/来源色（插件灰青/规则灰紫可见）
        val darkTheme = isSystemInDarkTheme()
        // 10-10 行流改版：卡片容器全撤，cardBg 三档随卡同亡；报错信号改由
        // 失败红丸+红原因字承担（StatusPill）
        // 获取成功前缀：石板灰 Blue Grey 800/200
        // 发音人信息：棕褐 #7D6B5D / 深色主题 #A08B7A
        val metaColor = if (darkTheme) Color(0xFFB0BEC5) else Color(0xFF37474F)
        val voiceColor = if (darkTheme) Color(0xFFA08B7A) else Color(0xFF7D6B5D)
        // 角色名小牌：灰绿（10-10 预览拍板色）；插件名小牌：灰青
        val roleChipColor = if (darkTheme) Color(0xFF9FB8A4) else Color(0xFF4E6E57)
        val pluginChipColor = if (darkTheme) Color(0xFF9AA0A8) else Color(0xFF5B6472)

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
                                        // 10-10 纯白表归位：日期签=信息标签走灰阶 #EEEEEE，
                                        // surfaceVariant 未被新管线覆写、绿主题下带绿灰
                                        MaterialTheme.colorScheme.surfaceContainerHighest,
                                        RoundedCornerShape(9.dp)
                                    )
                                    .padding(horizontal = 9.dp, vertical = 2.dp)
                            )
                        }

                        is LogGroups.Item.Bare -> {
                            val log = list[item.index]
                            val checked = log in checkedEntries
                            val isMatch = isMatchEntry(log, searchQuery)
                            // 裸行内容一份定义，滑删/非滑删两分支共用（避免复制整段 Column）
                            val bareContent: @Composable () -> Unit = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // 左滑删除底层透底修复（10-10 装机截图实锤）：裸行无底色时
                                    // SwipeToDismissBox 的 errorContainer 粉底+垃圾桶整行常驻透出。
                                    // 前景补页底色（background=页底，浅色纯白/深色锚同色）盖住
                                    // 底层，左滑时才从右侧露出删除提示
                                    .background(MaterialTheme.colorScheme.background)
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
                                        // 搜索命中底色整撤（10-10 装机用户令）：图二搜索态命中项
                                        // 整行淡绿块太重，命中信息由「N 处匹配」计数与跳转承担，
                                        // 行本身保持素底
                                        else Modifier
                                    )
                                    // 10-10 B 案：裸行左缘 4→16，与卡内正文/日期签文字同落
                                    // 全站 ListGutter 16dp 线（原本 4dp 是卡片带外边距时代的
                                    // 「贴边族」口径，卡片改通边后失效）。纵向 3.5 未动。
                                    .padding(horizontal = 16.dp, vertical = 3.5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // 行内元素套 Row(CenterVertically)：与组头行同因——FlowRow
                                    // 行内元素顶对齐，丸比时间文字高，中心错位（10-11 用户令）
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
                                        // 时间去日期去毫秒（10-10 用户令，与请求组头行同口径）：
                                        // 日期签在列顶、毫秒无人看；裸行时间染次级灰与头行一致。
                                        // 合并组走 headTimeText 出「首末完整时间」（10-11 用户令）。
                                        Text(
                                            text = headTimeText(log),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        // 行级丸（10-10 终案）：重试 W 主行=重试N、兜底/备用主行=
                                        // 切备用、失败主行=失败——失败链单条化后丸跟着各行走；
                                        // 插件/规则/系统消息/源错误子行裸行 pillOf 返回 null 不渲染
                                        pillOf(log, item.index)?.let {
                                            Box(Modifier.padding(start = 6.dp)) { StatusPill(it) }
                                        }
                                    }
                                    // ⚠️ 级别字母（I/W/E）整删（10-10 用户令「W,I 这种字母都删除」）：
                                    // 丸已承担状态表达,字母冗余
                                    // 「回原位」定位键（10-10 装机反馈用户令，10-11 用户令换显示）：
                                    // 筛选/搜索态裸行同样可跳回完整流原位（此前只有请求组头行有，
                                    // 插件/规则日志搜到后没键回不去）；弹簧顶最右，样式同组头行。
                                    // 10-11 换法：①撤 ⟲ 符号（"撤销/重来"语义，与"跳回原位"不搭）；
                                    // ②颜色沿用 primary 字 + 淡紫底（softContainerColor，与全站分类胶囊同款）——
                                    // 底色把"可点的钮"和正文隔开，比裸灰字醒目、又不与日志正文混读（10-11 用户定稿）
                                    if (showLocateKey) {
                                        Spacer(Modifier.weight(1f))
                                        Text(
                                            text = "回原位",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(softContainerColor())
                                                .clickable { onLocateOriginal(log) }
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                LogEntryBody(
                                    entry = log,
                                    darkTheme = darkTheme,
                                    metaColor = metaColor,
                                    voiceColor = voiceColor,
                                    roleChipColor = roleChipColor,
                                    pluginChipColor = pluginChipColor,
                                    // 裸行字号终版二轮（10-11 用户令）：12 还是小，提到 13——
                                    // 与成员行/主行前缀同档，四档收敛为三档（13/14/前缀13）
                                    fontSize = 13.sp,
                                    lineHeight = 16.9.sp, // 13×1.3
                                    // 命中高亮已在整行背景，正文不再叠一层
                                    isMatch = false,
                                    highlight = log == locateHighlight,
                                    // 重试行不画正文（10-11 用户令修正）：服务端保留「开始第 N 次
                                    // 重试」原文只为让「重试N」丸解析得到，行头丸已表达同一信息，
                                    // 正文再画一遍是重复。此前把正文撤成空串导致丸也推不出来
                                    //（真机表现为「只有时间、下面空白」的一行），此处改为
                                    // 服务端留原文 + 渲染层不画正文，既不重复也不空行
                                    suppressBody = pillOf(log, item.index)?.kind == LogPillKind.RETRY,
                                )
                            }
                            }
                            // 左滑删除整撤（10-10 装机用户令「只是看日志而已，为啥要有删除」
                            // +「上次都让你改结果还是这样」——10-10 白天 M3 三件套批加的该功能
                            // 本身不该存在：日志是只读流，删除走顶栏垃圾桶清空+多选勾删两通道）。
                            // SwipeToDismissBox 包装层、透底修复（前景补 background）、
                            // onRemoveEntry 参数与 VM.removeEntry 已于 10-11 一并清干净。
                            bareContent()
                            // 条目间分隔线（10-10 用户令「分隔线需要加」）：行流撤卡后行间只剩
                            // 空白，加发丝线恢复条目边界；日期签前不画（签自带灰底已是边界）。
                            // 口径沿用顶栏分隔线 outlineVariant@50%
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }

                        is LogGroups.Item.Group -> {
                            val head = list[item.head]
                            val memberEntries = item.members.map { list[it] }
                            val groupLogs = currentGroups.entriesOf(item).map { list[it] }
                            val groupChecked = groupLogs.all { it in checkedEntries }
                            // 老版式失败条（10-11 用户定稿「拿不准就按老版本，毕竟是起源」）：
                            // absorbError≥0 = 本组以源错误收场，渲染主体换成源错误条目
                            //（服务端已拼回请求全文：错误+正文粗+发音人信息）——
                            // 胶囊「源错误」+英文错误红+正文红粗+发音人信息不红，
                            // 请求主行正文不再重复（同一次失败一条说完，老版观感）
                            val absorbed = if (item.absorbError >= 0) list[item.absorbError] else null
                            // 行级丸：成功=「成功」（从组内 SUCCESS 成员推导——成功子行隐藏
                            // 信息上头行，主行是 INFO 推不出，aad9867 修）；失败=「源错误」
                            //（有吸收条）/「失败」（其他死法：超时/获取失败等，文案跟错误类型走）。
                            // ⚠️ 未决不兜底（10-11 用户实报「请求音频刚发出就标失败，成功后翻绿」）：
                            // 请求行发出到结果子行到达有几秒空窗——组开着、SUCCESS 没到、
                            // failedHeads 也没记，旧兜底 `?: LogPill(FAIL,"失败")` 把空窗误画成红丸。
                            // 改为没有失败证据就不出丸（结果到了由 SUCCESS/failedHeads/源错误
                            // 接管），等待期行头只有时间，与「过程未知」的语义一致
                            val successMember = memberEntries.firstOrNull { it.level == LogLevel.SUCCESS }
                            val pill = remember(item.head, successMember, absorbed, currentGroups) {
                                when {
                                    successMember != null -> LogPill(LogPillKind.OK, "成功")
                                    // 丸只报状态（10-12 用户令）：原来写「源错误」——那是**原因**，
                                    // 不该占丸位。现在一律「失败」，原因交给正文首行（服务端
                                    // 已改成「源错误：…」开头，见 SystemTtsService.errorEvent）
                                    absorbed != null -> LogPill(LogPillKind.FAIL, "失败")
                                    else -> pillOf(head, item.head, currentGroups.failedHeads)
                                }
                            }
                            // 失败次数进丸（10-11 用户令「相同原因的失败视为相同，日志可以×2、×3」）：
                            // 组的失败次数取**渲染主体那条**的 repeatCount——有吸收条时是吸收条
                            //（失败全文条，重试风暴都折在它身上），否则是组头请求行自己
                            //（应用层失败行也参与折叠，见 TtsLogViewModel.mergeInto 的主列表分支）
                            val failCount = (absorbed ?: head).repeatCount
                            // pill 可空（未决无丸，见上）：×N 后缀只在有丸时追加
                            val pillShown = pill?.let {
                                if (failCount > 1 && it.kind != LogPillKind.OK)
                                    it.copy(text = "${it.text} ×$failCount") else it
                            }
                            // 老版式失败条无丸后灰字（错误全文在正文区），成功尾段照旧
                            val tailText = remember(item.head, successMember, absorbed) {
                                if (absorbed != null) null else tailTextOfRow(successMember ?: head)
                            }
                            // 组头时间（10-12 用户令「起始时间到结束时间都会有」）：成功组=请求→成功，
                            // 失败组=请求→失败（合并过的取末次失败时间）。起点统一是**请求行**，
                            // 不再是「吸收条自己的时间」——同一页所有成对行都读成「从发起到有结果」
                            val groupHeadTime = groupTimeText(head, successMember ?: absorbed)
                            // 行流定稿（10-10 用户令）：成功=行头绿丸+灰数字，成员区整撤
                            // （结果信息已上头）；失败/重试/切备用的过程行仍要露出——
                            // 有非成功成员行时显示成员区（成功行本身不上，数字已在丸后）
                            val showMembers = memberEntries.any { it.level != LogLevel.SUCCESS }
                            Column(
                                modifier = Modifier
                                    // 行流：容器全撤，左缘直接落全站 16dp ListGutter 线；
                                    // 行间用纵向间距分隔（首行顶 6dp，行与行 8dp 由外层间隔表达——
                                    // 这里用统一 vertical 6dp，视觉等同旧行距但无线）
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .then(
                                        if (selectionMode) Modifier.clickable {
                                            // 组=多选粒度：整组勾/整组取消
                                            if (groupChecked)
                                                onCheckedChange(checkedEntries - groupLogs.toSet())
                                            else
                                                onCheckedChange(checkedEntries + groupLogs.toSet())
                                        }
                                        // 非多选：点行弹快捷面板（换发音人），锚定请求主行
                                        else Modifier.clickable { quickPanelEntry = head }
                                    )
                            ) {
                                // 头部行（10-10 装机反馈两令）：①撤丸间「→」——成功一行
                                // 只有「时间 成功 大小·耗时」，箭头是多余符号（用户不认），
                                // 保留 FlowRow 换行（药丸链过长仍折行不截断）；②对齐——
                                // ⚠️ FlowRow 没有 verticalAlignment 参数（CI 37959867285 实锤，
                                // 静守卫盲区），主轴对齐走 verticalArrangement=Arrangement.Center；
                                // 丸与文字垂直同轴另由丸内衬对齐（labelSmall 自身高度小）
                                FlowRow(
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // 行内元素套 Row(CenterVertically)（10-11 用户令「药丸内的文字
                                    // 和时间不在一个水平线上」）：FlowRow 的 verticalArrangement
                                    // 只管**行与行**的纵向排布，行内元素是顶对齐的——丸（labelSmall
                                    // +2dp 内衬）比时间文字高，顶对齐后中心错位。套一层 Row 纵向居中即齐。
                                    // 「回原位」键留在 FlowRow 里用 weight 顶右（Row 内 weight 只管行内）。
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
                                        Text(
                                            // 时间字号 13→12sp（10-12 用户令「时间栏字号缩小一号」）：
                                            // 与正文 14sp 拉开两档，进一步让行内重心落在丸上
                                            text = groupHeadTime,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        pillShown?.let {
                                            Box(Modifier.padding(start = 6.dp)) { StatusPill(it) }
                                        }
                                        // 重试次数小丸（10-12 用户令「重试和失败紧密联系，别单独占一行」）：
                                        // 重试行已不落行，次数挂在这里跟在状态丸后面。
                                        // 文案口径同用户令：**只有 1 次就写「重试」，2 次以上才写「重试×N」**
                                        if (item.retryCount > 0) {
                                            Box(Modifier.padding(start = 6.dp)) {
                                                StatusPill(
                                                    LogPill(
                                                        LogPillKind.RETRY,
                                                        if (item.retryCount == 1) "重试"
                                                        else "重试×${item.retryCount}"
                                                    )
                                                )
                                            }
                                        }
                                        // 丸后灰字（成功=大小·秒/失败主行=截短原因），与丸留 8dp 隙。
                                        // 失败原因红=旧正宗红 #F44336（10-10 用户令「错误的红色要用
                                        // 以前正宗的红色」——colorScheme.error 在动态取色下偏暗紫红）
                                        if (tailText != null) {
                                            Text(
                                                text = tailText,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 13.sp,
                                                color = if (pillShown?.kind == LogPillKind.FAIL)
                                                    Color(if (darkTheme) 0xFFE57373 else 0xFFF44336)
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
                                    }
                                    // 「回原位」定位键（筛选/搜索态）：弹簧顶到最右，动作留右手位。
                                    // 10-11 换显示：淡紫底胶囊（与裸行同口径）
                                    if (showLocateKey) {
                                        Spacer(Modifier.weight(1f))
                                        Text(
                                            text = "回原位",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(softContainerColor())
                                                .clickable { onLocateOriginal(head) }
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                // 老版式失败条（10-11 定稿）：渲染主体换成被吸收的源错误/
                                // 超时条目（服务端已拼请求全文），请求主行正文不再重复——
                                // 成功组不变（head 请求行照旧）。13sp 与成员行/裸行同档
                                if (absorbed != null) {
                                    LogEntryBody(
                                        entry = absorbed,
                                        darkTheme = darkTheme,
                                        metaColor = metaColor,
                                        voiceColor = voiceColor,
                                        roleChipColor = roleChipColor,
                                        pluginChipColor = pluginChipColor,
                                        fontSize = 14.sp,
                                        lineHeight = 18.2.sp, // 14×1.3
                                        isMatch = isMatchEntry(absorbed, searchQuery),
                                        emphasizeError = true,
                                        oldStyleFail = true,
                                        highlight = absorbed == locateHighlight,
                                    )
                                } else {
                                LogEntryBody(
                                    entry = head,
                                    darkTheme = darkTheme,
                                    metaColor = metaColor,
                                    voiceColor = voiceColor,
                                    roleChipColor = roleChipColor,
                                    pluginChipColor = pluginChipColor,
                                    // 请求主行回 14sp（10-10 字号令纠偏：「缩到与声音信息一样」
                                    // 只指插件/规则裸行；主行恢复排版实验 1008 定版 14sp，
                                    // 内三段分层回前缀13/正文14/声音12——见 LogEntryBody isRequestHead）
                                    fontSize = 14.sp,
                                    lineHeight = 18.2.sp, // 14×1.3
                                    isMatch = isMatchEntry(head, searchQuery),
                                    // 排版实验 1008（C）：只染"请求音频："前缀，正文回默认色
                                    isRequestHead = true,
                                    highlight = head == locateHighlight,
                                )
                                }
                                // 过程成员行（10-10 用户令终版）：失败丸行（时间+丸，无文字）
                                // 之后，**失败的详细信息单独列一行**——成员区 ERROR 行（完整
                                // 「源错误： xxx」）必须显示；成功行不上（数字已进丸后）。
                                // WARN 重试/兜底等过程行不在成员区（失败链单条化后它们已
                                // 独立成裸行，见裸行分支）
                                if (showMembers) {
                                    item.members.forEach { mIdx ->
                                        val m = list[mIdx]
                                        if (m.level == LogLevel.SUCCESS) return@forEach
                                        Column(Modifier.padding(top = 2.dp)) {
                                            LogEntryBody(
                                                entry = m,
                                                darkTheme = darkTheme,
                                                metaColor = metaColor,
                                                voiceColor = voiceColor,
                                                roleChipColor = roleChipColor,
                                                pluginChipColor = pluginChipColor,
                                                fontSize = 13.sp,
                                                lineHeight = 17.sp, // 13×1.3≈16.9
                                                isMatch = isMatchEntry(m, searchQuery),
                                                emphasizeError = true,
                                                highlight = m == locateHighlight,
                                            )
                                        }
                                    }
                                }
                            }
                            // 条目间分隔线（10-10 用户令）：与裸行分支同款，组与组之间画线
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                item {
                    // 尾部 Spacer 曾用 navigationBarsPadding（手势条避让）——与本页上游
                    // MainPager bottomPad（含手势条）+ TtsLogScreen calculateBottomPadding
                    // 两层避让叠加，列表末尾与底栏之间多出一条空白（10-10 装机截图实锤）。
                    // 改固定 8dp 只做行尾呼吸，手势条高度由上游两层负责
                    Spacer(Modifier.height(8.dp))
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
            visible = showJumpToBottom,
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

// 行级药丸推导（10-10 终案「失败链按以前单条处理+丸保留」）：每条日志按自己的
// 级别/文案出一颗丸——重试 W 主行=重试N、兜底/备用 I 主行=切备用、成功请求行=成功、
// 失败请求行=失败（依据 failedHeads：请求行本身是 INFO，失败标记由归组记入）、
// ERROR 主行（无卡区失败终点）=失败。成员行（源错误等结果子行）已独立裸行=无丸。
// 丸字=汉字「成功/失败」（10-11 用户令：✔/✕ 符号加了不好看，换回文字）
//
// 10-11 用户令「相同原因的失败视为相同，日志可以×2、×3」：合并组的丸带次数后缀
//（`失败 ×3`）。次数由 ViewModel 折叠时记入 repeatCount；=1 时不加后缀（与现状一致）。
// 重试/切备用丸也吃同一后缀（重试2 ×3 = 同一次重试重复出现 3 次）。
private fun pillOf(e: LogEntry, entryIndex: Int, failedHeads: Set<Int> = emptySet()): LogPill? {
    val msg = e.message.replace(Regex("<[^>]*>"), "")
    val base = when {
        e.configId != 0L -> when {
            e.level == LogLevel.SUCCESS -> LogPill(LogPillKind.OK, "成功")
            entryIndex in failedHeads -> LogPill(LogPillKind.FAIL, "失败")
            else -> null
        }
        msg.contains("次重试") || msg.contains(" retry", true) ->
            // 重试丸文案（10-12 用户令）：只有 1 次就写「重试」，2 次以上才带次数
            LogPill(LogPillKind.RETRY, run {
                val n = Regex("第 (\\d+) 次").find(msg)?.groupValues?.get(1)
                if (n == null || n == "1") "重试" else "重试$n"
            })
        msg.startsWith("使用备用") || msg.startsWith("使用兜底") ->
            LogPill(LogPillKind.STANDBY, "切备用")
        e.level == LogLevel.ERROR && e.indent == 0 -> LogPill(LogPillKind.FAIL, "失败")
        else -> null
    } ?: return null
    return if (e.repeatCount > 1) base.copy(text = "${base.text} ×${e.repeatCount}") else base
}

/**
 * 行头时间文案（10-11 用户令「首末完整」，10-12 用户令「所有日志时间统一 + 起始→结束」）：
 * 全站只有一种写法：`HH:mm:ss`（单点）或 `HH:mm:ss→HH:mm:ss`（区间，一律箭头）。
 * 历史遗留的 en dash（合并失败曾用 `–`）已撤——同一页不该出现两种连接符。
 * 时间源格式 `yyyy-MM-dd HH:mm:ss.SSS`，去日期去毫秒后长度恰为 8。
 */
private fun headTimeText(e: LogEntry): String {
    val start = e.time.drop(11).dropLast(4)
    val end = e.endTime.takeIf { it.length >= 19 }?.drop(11)?.dropLast(4)
    return if (e.repeatCount > 1 && !end.isNullOrBlank() && end != start) "$start→$end" else start
}

/**
 * 成对行的时间区间（10-12 用户令「起始时间到结束时间会有吗」）：起点=请求行时间，
 * 终点=结果行时间（成功子行 / 失败全文条；合并过的取它的末条时间 endTime）。
 * 只有终点确实晚于起点才出区间；取不到终点或同一秒 → 退回单时间。
 */
private fun groupTimeText(startEntry: LogEntry, endEntry: LogEntry?): String {
    val start = startEntry.time.drop(11).dropLast(4)
    if (start.length != 8) return headTimeText(startEntry)
    val endRaw = endEntry?.let { e ->
        e.endTime.takeIf { it.length >= 19 } ?: e.time.takeIf { it.length >= 19 }
    } ?: return start
    val end = endRaw.drop(11).dropLast(4)
    return if (end != start) "$start→$end" else start
}

/** 两个日志时间是否相隔不超过 seconds 秒（重试挂「上一张失败组」的身份校验用；格式 yyyy-MM-dd HH:mm:ss.SSS） */
private fun withinSeconds(a: String, b: String, seconds: Int): Boolean {
    if (a.length < 19 || b.length < 19) return false
    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US)
    return runCatching {
        val ta = fmt.parse(a)?.time ?: return@runCatching false
        val tb = fmt.parse(b)?.time ?: return@runCatching false
        kotlin.math.abs(tb - ta) <= seconds * 1000L
    }.getOrDefault(false)
}

// 行级丸后灰字：只服务成功行=「大小 · 耗时秒」（10-10 用户令：失败丸后不跟
// IOException 文案、失败丸单独一行——失败行的异常细节走成员区完整源错误行）。
// 截短 40 全角字逻辑保留（成功行数字不长，此处主要防御）。
private fun tailTextOfRow(e: LogEntry): String? {
    val msg = e.message.replace(Regex("<[^>]*>"), "")
    val t = when {
        e.level == LogLevel.SUCCESS ->
            msg.substringAfter("获取成功：", "").trim().ifEmpty { null }
        else -> null // 失败行不放原因（用户令）
    } ?: return null
    var w = 0f
    val out = StringBuilder()
    var clipped = false
    for (ch in t) {
        val cw = if (ch.code < 0x2E80) 0.55f else 1.0f
        if (w + cw > 40f) { clipped = true; break }
        w += cw
        out.append(ch)
    }
    return if (clipped) out.toString().trimEnd() + "…" else t
}

internal enum class LogPillKind { OK, FAIL, RETRY, STANDBY }
internal data class LogPill(val kind: LogPillKind, val text: String)

// 状态药丸（10-10 行流定稿）：丸只装状态字，成功绿/失败红/重试灰/切备用黄；
// 丸后灰字（耗时·大小/失败原因）由调用方另排。
// ⚠️ 不用 Pair 解构（when 分支混用 to 与 if 表达式时编译器推成 Any）——分两个 when
@Composable
private fun StatusPill(pill: LogPill) {    val dark = isSystemInDarkTheme()
    // 丸配色「淡底重字」（10-11 用户从三案中拍板）：底再淡一档、字色加重一档——
    // 底只做「有个胶囊形状」的暗示，状态信号全交给字色。对比另两案：
    //   · 原案（M3 淡底+常规字）底浅字也浅，在米白页上信号偏弱
    //   · 另一案「加深」（底深一档+字加粗）更醒目，但丸的色块感重、一屏多丸时吵
    // 深色主题的 RETRY/STANDBY 原本就有各自的深底值，维持不动（本次只调浅色口径）
    val bg = when (pill.kind) {
        LogPillKind.OK -> Color(0xFFF2F8F4)
        LogPillKind.FAIL -> Color(0xFFFDEEED)
        LogPillKind.RETRY -> if (dark) Color(0xFF2A2D33) else Color(0xFFF3F5F8)
        LogPillKind.STANDBY -> if (dark) Color(0xFF3A3226) else Color(0xFFFDF7E7)
    }
    val fg = when (pill.kind) {
        LogPillKind.OK -> Color(0xFF2E7D46)
        LogPillKind.FAIL -> Color(0xFFC62828)
        LogPillKind.RETRY -> if (dark) Color(0xFF9AA0A8) else Color(0xFF4A5568)
        LogPillKind.STANDBY -> if (dark) Color(0xFFC9A94E) else Color(0xFF9A7B14)
    }
    // 丸内文字用 Box 居中（10-12 用户令「字一定要居中对齐」）：Text 直接挂 padding 时
    // 字形上下留白随字重/字体基线浮动，视觉偏上；Box(contentAlignment=Center) + Text 无内衬
    // 再把内衬给 Box，字在胶囊里几何居中。
    // 丸尺寸同步缩一档（10-12 用户令「时间栏字号缩小一号，胶囊也要调整」）：
    // 12.5sp → 12sp（与缩小后的时间 12sp 同档）、内衬 11/3 → 10/3（宽度随之下收）
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = pill.text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = fg,
        )
    }
}

// 单条日志正文渲染：HTML → AnnotatedString + 哨兵色重映射，级别色或指定色
@Composable
private fun LogEntryBody(
    entry: LogEntry,
    darkTheme: Boolean,
    metaColor: Color,
    voiceColor: Color,
    // 角色名/插件名小牌字色（10-10 行流定稿；背景色 span 由 HTML font 标签来不了，
    // 牌感靠字色+前后空隙表达，字号 12sp 由声音信息段统一压）
    roleChipColor: Color,
    pluginChipColor: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    isMatch: Boolean,
    // 非空=成员行统一用次级色（石板灰/暗红），级别色让位
    forceColor: Color? = null,
    // 排版实验 1008（C）：请求主行——"请求音频："前缀染深绿、其余回默认色。
    // 请求行 HTML 结构固定：`请求音频：` + <b>正文</b>（+ 哨兵色次级段），
    // 据此把首段（正文之前的裸文本）与前缀分开着色
    isRequestHead: Boolean = false,
    // 排版实验 1008（E）：错误行加粗 + ⚠ 行首标，让错误在行流里突出于成功行
    emphasizeError: Boolean = false,
    // 老版式失败条分段染红（10-11 用户定稿「拿不准就按老版本」）：源错误/超时条目含
    // 全文（错误 + <br> + <b>正文</b> + <br> + 发音人信息），老版观感=错误行与正文
    // 正文红、发音人信息不红——条目级级别色做不到分段，这里按首个 <br> 分段实现：
    // 发音人信息段（第二个 <br> 起）保持原哨兵色，前两段染老红
    oldStyleFail: Boolean = false,
    // 「原文」定位命中（用户 10-08 甲方案）：黄底高亮闪现，由上层定时清除
    highlight: Boolean = false,
    // 不画正文（10-11 用户令）：重试行用——服务端保留「开始第 N 次重试」原文供
    // pillOf 解析出「重试N」丸，丸已表达同一信息，正文再画一遍是重复。
    // 行头（时间+丸）照常渲染，故不会出现「只有时间、下面空白」的空行
    suppressBody: Boolean = false,
) {
    if (suppressBody) return
    val spanned = remember(entry.message, darkTheme, metaColor, voiceColor, roleChipColor, pluginChipColor, isRequestHead, emphasizeError, oldStyleFail) {
        val base = HtmlCompat.fromHtml(entry.message, HtmlCompat.FROM_HTML_MODE_COMPACT)
            .toAnnotatedString()
            .remapMetaColor(metaColor, voiceColor, roleChipColor, pluginChipColor)
        var s = base
        if (isRequestHead) {
            // 字号分层回 1008 定版（10-10 用户令纠偏：「缩到与声音信息一样」只指插件/
            // 规则裸行；主行三段=前缀 13 / 正文 14（<b> 加粗承担字重）/ 声音信息 12）。
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
                        // 声音信息：哨兵色已换好目标色。字号 12→13sp（10-11 用户令
                        // 「时间和下方声音条的字号太小」）——与裸行/成员行同档，
                        // 与正文 14sp 仍保一档差
                        item = item.copy(fontSize = 13.sp)
                    }
                    addStyle(item, r.start, r.end)
                }
                // 前缀段可能无 span 覆盖（级别色是 Text 整体 color，不是 span）：
                // 显式补一个 13sp span
                if (prefixEnd > 0) addStyle(SpanStyle(fontSize = 13.sp), 0, prefixEnd)
                // 声音信息段若无 span 覆盖（整段哨兵色必有 span，此处兜底）：补 13sp
                if (metaStart < base.text.length) {
                    addStyle(SpanStyle(fontSize = 13.sp), metaStart, base.text.length)
                }
            }
        }
        // 报错三件套②收口（10-10 用户令「最后的源错误信息加粗了，不应该」）：行首 ✖/⚠ 撤 +
        // **W/E 行加粗也撤**——用户明确不加粗；级别色（红/黄）本身就是信号，不再叠加任何强调
        // 老版式失败条（10-11 定稿）：条目结构固定「错误串 + <br> + <b>正文</b> + <br> +
        // 发音人信息」——第二段（正文）染老红，第一段（错误串）由条目级级别色染红，
        // 第三段（发音人信息）保持哨兵重映射色不红。段界=换行符在文本中的下标
        //（fromHtml 后 <br> 变 '\n'）
        if (oldStyleFail) {
            val nl1 = s.text.indexOf('\n')
            val nl2 = if (nl1 >= 0) s.text.indexOf('\n', nl1 + 1) else -1
            if (nl1 >= 0 && nl2 > nl1) {
                val failRed = if (darkTheme) Color(0xFFE57373) else Color(0xFFF44336)
                s = buildAnnotatedString {
                    append(s.text)
                    s.spanStyles.forEach { r -> addStyle(r.item, r.start, r.end) }
                    // 正文段（两个换行之间）整体染老红，压过 <b> 的字重色
                    addStyle(SpanStyle(color = failRed, fontWeight = FontWeight.Bold), nl1 + 1, nl2)
                }
            }
        }
        s
    }
    val bodyColor = forceColor ?: when {
        // ⚠️「成功回旧绿」纠偏（10-10 用户令）：只指头行**药丸里「成功」两字**（药丸
        // 自身绿未动过），SUCCESS 正文行维持原口径=石板灰 metaColor——上版把正文也
        // 染成 INFO 正绿是理解偏了，回退
        entry.level == LogLevel.SUCCESS -> metaColor
        // 排版实验 1008（C）：请求主行正文回默认色（前缀已单独染色），其余行照旧级别色
        isRequestHead -> MaterialTheme.colorScheme.onSurface
        else -> Color(entry.level.toArgb(isDarkTheme = darkTheme))
    }
    val bodyStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = fontSize,
        lineHeight = lineHeight,
        // 字号真分层后撤主行整体半粗：正文 <b> 自带字重，先前 SemiBold 叠加
        // 是"主行三段看着没分层"的根源（前缀/声音信息被衬得过轻）
    )
    val hlModifier = if (highlight) Modifier
        .clip(RoundedCornerShape(4.dp))
        .background(Color(0xFFFFE082))
    // 搜索命中底色整撤（10-10 装机用户令）：与裸行整行底同批，命中不再染行
    else Modifier

    // 角色名做成**真圆角丸**（10-11 用户令「那个人名也做成丸吧，不然不突出」）：
    // 行内 SpanStyle 的背景只能画直角矩形、给不了圆角（见 remapMetaColor 注释），
    // 所以只靠加底色永远做不出状态丸那种圆角胶囊。这里把角色名那一段从正文里切出来
    //（AnnotatedString.subSequence 保留 span 样式），单独渲染成一颗圆角 Surface 丸，
    // 其余文本照旧走 Text。角色名恒为发音人信息段的**首元素**（服务端 configInfo 里
    // 角色牌拼在 voiceText 之前），故「丸 + 其余」并排即原读序，不动字序与语义。
    val chip = spanned.spanStyles.firstOrNull { it.item.color == roleChipColor }
    if (chip != null && chip.start < chip.end && chip.end <= spanned.text.length) {
        val chipName = spanned.text.substring(chip.start, chip.end)
        var before = spanned.subSequence(0, chip.start)
        val after = spanned.subSequence(chip.end, spanned.text.length)
        // 段末换行去掉：下面用 Column 显式分行（丸必须独占一行开头），留着会多出空行
        if (before.text.endsWith("\n")) before = before.subSequence(0, before.text.length - 1)
        Column(modifier = hlModifier) {
            if (before.text.isNotEmpty()) {
                Text(text = before, color = bodyColor, style = bodyStyle)
            }
            // 丸与后随文字**按基线对齐**（10-12 用户问「能跟其他文字对齐吗」）：原来
            // Row(Top) + 丸是 Box，Box 不报文字基线，只能顶边对齐 ⇒ 丸里那两个字比旁边
            // 发音人信息低一个内衬（3dp），看着像没对齐。改用 alignByBaseline：两边第一行
            // 的基线重合，后随文字换行也不影响
            Row {
                RoleNamePill(
                    chipName,
                    roleChipColor,
                    fontSize,
                    modifier = Modifier.alignByBaseline(),
                )
                if (after.text.isNotEmpty()) {
                    Text(
                        text = after,
                        color = bodyColor,
                        style = bodyStyle,
                        modifier = Modifier
                            .weight(1f)
                            .alignByBaseline(),
                    )
                }
            }
        }
    } else {
        Text(
            text = spanned,
            color = bodyColor,
            style = bodyStyle,
            modifier = hlModifier,
        )
    }
}

/**
 * 角色名丸（10-11 用户令「那个人名也做成丸吧，不然不突出」；10-12 用户令「跟时间旁边一样的胶囊」）：
 * 与状态丸（StatusPill）**同一套几何**——RoundedCornerShape(50) + 12sp Medium + 内衬 10/3，
 * 底色同取「淡底重字」的淡底；差别只在字色=角色牌色（灰绿/亮灰绿）。
 * 此前底色取牌字色 alpha 0.22，比状态丸实一档、大小也不齐——用户要求两者看起来是同一种丸。
 *
 * ⚠️ 必须用带底色的 **Text** 而不是 Box 包 Text（10-12 二修）：Box 不向上报文字基线，
 * 外层 Row 的 alignByBaseline 会退化成顶边对齐，丸里字比旁边文字低一个内衬。
 * Text 自己会报基线，padding 由修饰符给（基线随顶内衬下移），基线对齐才算真的对上。
 * lineHeight 钉成 fontSize：行盒不高出字身，胶囊上下内衬才是真正的对称。
 */
@Composable
private fun RoleNamePill(
    name: String,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    Text(
        text = name,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 12.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Medium,
        ),
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            // 淡底与状态丸 OK 档同源（#F2F8F4 / 深色 #2A2D33）——「这是谁」用中性淡底，
            // 状态信号才用彩色淡底，同一页两种丸一眼分得开
            .background(if (dark) Color(0xFF2A2D33) else Color(0xFFF2F8F4))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}
