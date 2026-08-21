package com.github.jing332.tts_server_android.compose.systts

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.CompositionLocalProvider
import com.github.jing332.common.LogLevel
import com.github.jing332.tts_server_android.R
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TtsLogScreen(vm: TtsLogViewModel = viewModel()) {
    val context = LocalContext.current
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // 用 derivedStateOf 缓存过滤结果，避免 HorizontalPager 滑动时每次重组都重算 filteredLogs（O(n)）
    val filteredLogs by remember(vm) { derivedStateOf { vm.filteredLogs } }

    // 搜索=定位而非过滤：跳到最近一条匹配项，高亮由 LogScreen 渲染，
    // 列表保持完整，上下滑动即可查看前后文
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.isNotEmpty()) {
            val idx = filteredLogs.indexOfLast {
                it.message.contains(q, ignoreCase = true) ||
                        it.time.contains(q, ignoreCase = true)
            }
            if (idx >= 0) listState.animateScrollToItem(idx)
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    scrollBehavior = scrollBehavior,
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
                                Text(text = stringResource(id = R.string.log), textAlign = TextAlign.Center)
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
                        
                        // 筛选按钮
                        IconButton(onClick = { vm.showFilterDialog.value = true }) {
                            Icon(Icons.Default.FilterList, stringResource(R.string.filter))
                        }
                        
                        // 文件夹按钮 - 用文件管理器打开日志文件
                        IconButton(onClick = {
                            val logFile = File(vm.logDir())
                            
                            kotlin.runCatching {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    logFile
                                )
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "text/plain")
                                    flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                            android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }.onFailure {
                                // 降级：使用通用类型
                                kotlin.runCatching {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        logFile
                                    )
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "*/*")
                                        flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        }) {
                            Icon(Icons.Default.FolderOpen, stringResource(R.string.open_log_folder))
                        }
                        
                        // 清空按钮
                        IconButton(onClick = { vm.clear() }) {
                            Icon(Icons.Default.DeleteOutline, stringResource(id = R.string.clear_log))
                        }
                    }
                )
                
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
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.filter),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            vm.selectedLevels.forEach { level ->
                                FilterChip(
                                    selected = true,
                                    onClick = { vm.toggleLevel(level) },
                                    label = { Text(getLevelName(level)) },
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
                            // 清除所有筛选
                            if (vm.selectedLevels.isNotEmpty()) {
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(onClick = { vm.clearFilter() }) {
                                    Icon(Icons.Default.Clear, null)
                                }
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
                .padding(top = paddingValues.calculateTopPadding()),
            list = filteredLogs,
            listState = listState,
            // 搜索定位期间不自动滚底，避免与跳转互相拉扯
            autoScrollToBottom = vm.autoScrollToBottom.value && searchQuery.isEmpty(),
            searchQuery = searchQuery
        )
    }

    // 筛选对话框
    if (vm.showFilterDialog.value) {
        LogFilterDialog(
            selectedLevels = vm.selectedLevels,
            onLevelToggle = { vm.toggleLevel(it) },
            showPluginLogs = vm.showPluginLogs.value,
            onPluginLogsToggle = { vm.showPluginLogs.value = !vm.showPluginLogs.value },
            showSpeechRuleLogs = vm.showSpeechRuleLogs.value,
            onSpeechRuleLogsToggle = { vm.showSpeechRuleLogs.value = !vm.showSpeechRuleLogs.value },
            autoScrollToBottom = vm.autoScrollToBottom.value,
            onAutoScrollToggle = { vm.autoScrollToBottom.value = !vm.autoScrollToBottom.value },
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
