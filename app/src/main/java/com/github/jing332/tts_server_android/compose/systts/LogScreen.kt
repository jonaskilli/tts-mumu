package com.github.jing332.tts_server_android.compose.systts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
) {
    ControlBottomBarVisibility(listState, LocalBottomBarBehavior.current)
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val context = LocalContext.current
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
                    // 发音人信息：雾紫 #6D4C5E / 深色主题 #B088A0
                    val metaColor = if (darkTheme) Color(0xFFB0BEC5) else Color(0xFF37474F)
                    val voiceColor = if (darkTheme) Color(0xFFB088A0) else Color(0xFF6D4C5E)
                    val style = MaterialTheme.typography.bodyMedium
                    val spanned = remember(log.message, darkTheme, metaColor, voiceColor) {
                        HtmlCompat.fromHtml(log.message, HtmlCompat.FROM_HTML_MODE_COMPACT)
                            .toAnnotatedString()
                            // 获取成功前缀→石板灰，发音人信息→雾紫
                            .remapMetaColor(metaColor, voiceColor)
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
                            text = spanned,
                            // 获取成功(SUCCESS)与次级信息同用石板灰；"获取成功"前缀与"大小·耗时"在 SystemTtsService 内均已加 <b> 加粗
                            color = if (log.level == LogLevel.SUCCESS)
                                metaColor
                            else Color(log.level.toArgb(isDarkTheme = darkTheme)),
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
