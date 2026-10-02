package com.github.jing332.tts_server_android.compose.systts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    // 多选模式（1002）：顶栏 ☑ 进入（与密钥页同款）。跨条拖选会触发 compose 1.7
    // selection 崩溃（滚出视口的条目被回收后选区仍悬挂其 id），故跨条复制走本模式；
    // 条目内长按选字照旧保留（SelectionContainer 按条目独立，不与本模式抢手势）
    selectionMode: Boolean = false,
    // 勾选的日志（以条目对象为键：对列表增删/筛选重排免疫；time 毫秒级，同值碰撞可忽略）
    checkedEntries: Set<LogEntry> = emptySet(),
    onToggleCheck: (LogEntry) -> Unit = {},
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
        LazyColumn(Modifier.fillMaxSize(), state = listState) {
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

                    // 每条日志之间画分隔线
                    if (index > 0)
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                    // 多选：点条目=勾选（与密钥页同款，越权操作不进快捷面板）；非多选：点带
                    // configId 的请求主行弹快捷面板。多选入口在前置 Scaffold 顶栏 ☑
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
                            .padding(
                                start = 4.dp,
                                end = 4.dp,
                                top = 3.5.dp,
                                bottom = 3.5.dp
                            )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectionMode) {
                                // 勾选框替代时间戳左缘（与密钥页行首 Checkbox 同型）
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { onToggleCheck(log) },
                                    modifier = Modifier.padding(end = 2.dp)
                                )
                            }
                            // 完整时间戳(年月日+时分秒+毫秒)，等级字母跟在时间后
                            Text(text = log.time, style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "\t${log.level.toLogLevelChar()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        // 正文：非多选=条目内长按选字（选择容器按条目独立，容器随条目销毁，
                        // 悬挂 selectableId 崩溃不可能发生）；多选=纯文本（整行是勾选行，
                        // 不给选字柄抢手势）
                        if (selectionMode) {
                            Text(
                                text = display,
                                color = bodyColor,
                                style = style,
                                lineHeight = style.lineHeight * 0.9f,
                            )
                        } else {
                            SelectionContainer {
                                Text(
                                    text = display,
                                    // 获取成功(SUCCESS)整行石板灰同字重(用户:冒号前后一致不加粗)；加粗仅保留请求文本正文
                                    color = bodyColor,
                                    style = style,
                                    lineHeight = style.lineHeight * 0.9f,
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.navigationBarsPadding())
                }
            }

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(48.dp),
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
