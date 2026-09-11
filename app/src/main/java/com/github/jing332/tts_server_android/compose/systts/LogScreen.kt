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

// 功能日志来源色（用户 09-09 定稿方案二）：低调灰调，存在感介于主流程绿与石板灰之间，
// 避开「获取成功」石板灰与发音人棕褐的色相区间。仅接管 INFO 与 DEBUG/TRACE；
// ERROR/WARN 保持红/黄级别语义，SUCCESS 仍走石板灰
private fun pluginLogColor(isDarkTheme: Boolean) =
    if (isDarkTheme) Color(0xFF8FA9A3) else Color(0xFF4E6E68)    // 插件：灰青

private fun ruleLogColor(isDarkTheme: Boolean) =
    if (isDarkTheme) Color(0xFFA795B1) else Color(0xFF6E5E78)    // 朗读规则：灰紫

private fun pluginDebugColor(isDarkTheme: Boolean) =
    if (isDarkTheme) Color(0xFF75908A) else Color(0xFF829895)    // 插件 DEBUG：淡灰青

private fun ruleDebugColor(isDarkTheme: Boolean) =
    if (isDarkTheme) Color(0xFF90819F) else Color(0xFF9487A0)    // 规则 DEBUG：淡灰紫

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    modifier: Modifier,
    list: List<LogEntry>,
    listState: LazyListState = rememberLazyListState(),
    autoScrollToBottom: Boolean = false,
    // 非空时命中项加背景高亮(定位用，不过滤列表)
    searchQuery: String = "",
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
        SelectionContainer {
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

                    // 正文着色：SUCCESS→石板灰；功能日志按来源降调（插件灰青/规则灰紫，
                    // DEBUG 用对应淡色）；其余级别维持级别色（红/黄/绿/蓝/灰）
                    val bodyColor = when {
                        log.level == LogLevel.SUCCESS -> metaColor
                        log.isPluginLog -> when (log.level) {
                            LogLevel.INFO -> pluginLogColor(darkTheme)
                            LogLevel.DEBUG, LogLevel.TRACE -> pluginDebugColor(darkTheme)
                            else -> Color(log.level.toArgb(isDarkTheme = darkTheme))
                        }
                        log.isSpeechRuleLog -> when (log.level) {
                            LogLevel.INFO -> ruleLogColor(darkTheme)
                            LogLevel.DEBUG, LogLevel.TRACE -> ruleDebugColor(darkTheme)
                            else -> Color(log.level.toArgb(isDarkTheme = darkTheme))
                        }
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

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            // 带 configId 的请求主行可点击：弹日志快捷面板（换发音人/调参）
                            .then(
                                if (log.configId != 0L) Modifier.clickable {
                                    quickPanelEntry = log
                                } else Modifier
                            )
                            .then(
                                if (isMatch) Modifier
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
                            // 完整时间戳(年月日+时分秒+毫秒)，等级字母跟在时间后
                            Text(text = log.time, style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "\t${log.level.toLogLevelChar()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
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
