package com.github.jing332.tts_server_android.compose.codeeditor

import androidx.activity.compose.rememberLauncherForActivityResult
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.jing332.common.utils.longToast
import com.github.jing332.compose.ComposeExtensions.clickableRipple
import com.github.jing332.compose.widgets.AppTooltip
import com.github.jing332.compose.widgets.CheckedMenuItem
import com.github.jing332.compose.widgets.LongClickIconButton
import com.github.jing332.script.JsBeautify
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.conf.CodeEditorConfig
import com.github.jing332.tts_server_android.ui.AppActivityResultContracts
import com.github.jing332.tts_server_android.ui.FilePickerActivity
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentListener
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher
import kotlinx.coroutines.launch


fun CodeEditor.string(): String = this.text.toString()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CodeEditorScreen(
    title: @Composable () -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onLongClickSave: () -> Unit = {},
    onUpdate: (CodeEditor) -> Unit,
    onSaveFile: (() -> Pair<String, ByteArray>)?,

    onDebug: () -> Unit,
    onRemoteAction: (name: String, body: ByteArray?) -> Unit = { _, _ -> },

    vm: CodeEditorViewModel = viewModel(),

    // 折叠状态持久化标识（如 "plugin_123"、"speechRule_456"），为 null 时不持久化
    foldStateKey: String? = null,

    debugIconContent: @Composable () -> Unit = {},
    onLongClickMore: () -> Unit = {},
    onLongClickMoreLabel: String? = null,
    actions: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit = {},
) {
    var codeEditor by remember { mutableStateOf<CodeEditor?>(null) }
    var foldingManager by remember { mutableStateOf<CodeFoldingManager?>(null) }

    // 搜索状态（内置搜索栏，朗读规则和插件页共用）
    var showSearchBar by remember { mutableStateOf(false) }
    var searchKeyword by remember { mutableStateOf("") }

    // 函数匹配状态（搜索框输入多行代码时，按第一行匹配同名函数）
    var functionMatcher by remember { mutableStateOf<FunctionMatcher?>(null) }
    var functionMatches by remember { mutableStateOf<List<FunctionMatcher.FunctionBlock>>(emptyList()) }
    var currentMatchIndex by remember { mutableStateOf(-1) }

    var showThemeDialog by remember { mutableStateOf(false) }
    if (showThemeDialog)
        ThemeSettingsDialog { showThemeDialog = false }

    var showRemoteSyncDialog by remember { mutableStateOf(false) }
    if (showRemoteSyncDialog)
        RemoteSyncSettings { showRemoteSyncDialog = false }

    val fileSaver =
        rememberLauncherForActivityResult(AppActivityResultContracts.filePickerActivity()) {
        }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 第12项: 从文件导入覆盖当前编辑器代码
    val fileLoader =
        rememberLauncherForActivityResult(AppActivityResultContracts.filePickerActivity()) { (_, uri) ->
            if (uri != null) {
                runCatching {
                    val content = context.contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes().toString(Charsets.UTF_8)
                    } ?: ""
                    codeEditor?.setText(content)
                    // 导入新代码后重置折叠状态（旧 foldMarks 已失效）
                    foldingManager?.let { fm ->
                        foldStateKey?.let { key ->
                            CodeEditorConfig.saveFoldStates(key, emptyList())
                        }
                        fm.unfoldAll()
                    }
                    context.longToast("已导入 ${content.lineSequence().count()} 行")
                }.onFailure {
                    context.displayErrorDialog(it)
                }
            }
        }
    LaunchedEffect(vm) {
        if (CodeEditorConfig.isRemoteSyncEnabled.value)
            vm.startSyncServer(
                port = CodeEditorConfig.remoteSyncPort.value,
                onPush = { codeEditor?.setText(it) },
                onPull = { codeEditor?.text.toString() },
                onDebug = onDebug,
                onAction = onRemoteAction
            )

        scope.launch {
            vm.error.collect {
                when (it) {
                    Error.Empty -> {}
                    is Error.Other -> {
                        context.displayErrorDialog(t = it.e)
                    }

                    Error.PortConflict -> {
                        context.longToast("RemoteSync: port conflict!")
                    }

                    is Error.Socket -> {
                        context.longToast("RemoteSync: ${it.message}")
                    }
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0,0,0,0),
        topBar = {
            TopAppBar(title = title, navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.nav_back)
                    )
                }
            },
                actions = {
                    IconButton(onClick = onDebug) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = stringResource(id = R.string.debug)
                        )
                        debugIconContent()
                    }

                    LongClickIconButton(
                        onClick = { codeEditor?.undo() },
                        onLongClickLabel = stringResource(R.string.redo),
                        onLongClick = { codeEditor?.redo() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = stringResource(id = R.string.undo)
                        )
                    }

                    AppTooltip(tooltip = if (showSearchBar) "隐藏搜索栏" else "搜索") {
                        IconButton(onClick = {
                            showSearchBar = !showSearchBar
                            if (!showSearchBar) {
                                codeEditor?.searcher?.stopSearch()
                            }
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                    }

                    // 复制选区（自动展开折叠区域，还原原始代码后再复制）
                    AppTooltip(tooltip = "复制（展开折叠）") {
                        IconButton(onClick = {
                            codeEditor?.let { editor ->
                                val cursor = editor.cursor
                                if (!cursor.isSelected()) {
                                    context.longToast("请先选择要复制的代码")
                                    return@let
                                }
                                val text = if (foldingManager != null) {
                                    foldingManager!!.getUnfoldedSelectionText(
                                        cursor.leftLine, cursor.leftColumn,
                                        cursor.rightLine, cursor.rightColumn
                                    )
                                } else {
                                    editor.text.subSequence(
                                        editor.text.getCharIndex(cursor.leftLine, cursor.leftColumn),
                                        editor.text.getCharIndex(cursor.rightLine, cursor.rightColumn)
                                    ).toString()
                                }
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cb.setPrimaryClip(ClipData.newPlainText("code", text))
                                context.longToast("已复制${text.lineSequence().count()}行")
                            }
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "复制（展开折叠）")
                        }
                    }

                    // 保存前：持久化折叠状态 + 展开所有折叠（确保保存的是完整原始代码）
                    val performSave = {
                        foldingManager?.let { fm ->
                            foldStateKey?.let { key ->
                                CodeEditorConfig.saveFoldStates(key, fm.exportFoldMarks())
                            }
                            fm.unfoldAll()
                        }
                    }

                    LongClickIconButton(
                        onClick = { performSave(); onSave() },
                        onLongClick = { performSave(); onLongClickSave() }
                    ) {
                        Icon(
                            Icons.Filled.Save,
                            contentDescription = stringResource(id = R.string.save)
                        )
                    }

                    var showOptions by remember { mutableStateOf(false) }

                    LongClickIconButton(
                        onClick = { showOptions = true },
                        onLongClick = onLongClickMore,
                        onLongClickLabel = onLongClickMoreLabel
                    ) {
                        Icon(Icons.Default.MoreVert, stringResource(id = R.string.more_options))

                        DropdownMenu(
                            expanded = showOptions,
                            onDismissRequest = { showOptions = false }) {
                            if (onSaveFile != null)
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.save_as_file)) },
                                    onClick = {
                                        onSaveFile.invoke().let {
                                            fileSaver.launch(
                                                FilePickerActivity.RequestSaveFile(
                                                    fileName = it.first,
                                                    fileBytes = it.second
                                                )
                                            )
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.InsertDriveFile,
                                            null
                                        )
                                    }
                                )

                            // 第12项: 从文件导入覆盖当前编辑器代码
                            DropdownMenuItem(
                                text = { Text("从文件导入") },
                                onClick = {
                                    showOptions = false
                                    fileLoader.launch(
                                        FilePickerActivity.RequestSelectFile(
                                            fileMimes = listOf(
                                                "application/javascript",
                                                "text/javascript",
                                                "text/plain",
                                                "*"
                                            )
                                        )
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Input,
                                        "从文件导入"
                                    )
                                }
                            )

                            var syncEnabled by remember { CodeEditorConfig.isRemoteSyncEnabled }
                            CheckedMenuItem(
                                text = { Text(stringResource(id = R.string.remote_sync_service)) },
                                checked = syncEnabled,
                                onClick = { showRemoteSyncDialog = true },
                                onClickCheckBox = { syncEnabled = it },
                                leadingIcon = {
                                    Icon(Icons.Default.SettingsRemote, null)
                                }
                            )

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.theme)) },
                                onClick = { showThemeDialog = true },
                                leadingIcon = { Icon(Icons.Default.ColorLens, null) }
                            )

                            var wordWrap by remember { CodeEditorConfig.isWordWrapEnabled }
                            CheckedMenuItem(
                                text = { Text(stringResource(id = R.string.word_wrap)) },
                                checked = wordWrap,
                                onClick = { wordWrap = it },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Default.WrapText, null)
                                }
                            )

                            // 格式化（从搜索选项移到菜单）
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.format_code)) },
                                onClick = {
                                    showOptions = false
                                    codeEditor?.let {
                                        val newCode = vm.formatCode(it.string())
                                        it.setText(newCode)
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Code, stringResource(id = R.string.format_code))
                                }
                            )

                            // 还原剪贴板折叠代码（内置）
                            DropdownMenuItem(
                                text = { Text("还原剪贴板折叠代码") },
                                onClick = {
                                    showOptions = false
                                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clipText = cb.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                                    if (!clipText.isNullOrEmpty()) {
                                        runCatching {
                                            val beautifier = JsBeautify(context)
                                            val formatted = beautifier.format(clipText)
                                            cb.setPrimaryClip(ClipData.newPlainText("code", formatted))
                                            context.longToast("已还原并复制到剪贴板")
                                        }.onFailure {
                                            context.longToast("还原失败：${it.message}")
                                        }
                                    } else {
                                        context.longToast("剪贴板为空")
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.ContentPaste, "还原剪贴板折叠代码")
                                }
                            )

                            // 折叠全部代码（逐个折叠，间隔 50ms 避免代码量大时闪退）
                            DropdownMenuItem(
                                text = { Text("折叠全部代码") },
                                onClick = {
                                    showOptions = false
                                    codeEditor?.let { editor ->
                                        editor.setSelection(
                                            editor.text.lineCount - 1,
                                            editor.text.getColumnCount(editor.text.lineCount - 1)
                                        )
                                    }
                                    foldingManager?.let { fm ->
                                        scope.launch {
                                            fm.foldAllProgressive()
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.UnfoldLess, "折叠全部代码")
                                }
                            )

                            // 展开全部代码（逐个展开，间隔 50ms）
                            DropdownMenuItem(
                                text = { Text("展开全部代码") },
                                onClick = {
                                    showOptions = false
                                    codeEditor?.let { editor ->
                                        editor.setSelection(
                                            editor.text.lineCount - 1,
                                            editor.text.getColumnCount(editor.text.lineCount - 1)
                                        )
                                    }
                                    foldingManager?.let { fm ->
                                        scope.launch {
                                            fm.unfoldAllProgressive()
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.UnfoldMore, "展开全部代码")
                                }
                            )

                            actions { showOptions = false }
                        }

                    }
                }
            )
        }
    ) { paddingValues ->
        val theme by remember { CodeEditorConfig.theme }
        LaunchedEffect(codeEditor, theme) {
            codeEditor?.helper()?.setTheme(theme)
        }

        val wordWrap by remember { CodeEditorConfig.isWordWrapEnabled }
        LaunchedEffect(codeEditor, wordWrap) {
            codeEditor?.isWordwrap = wordWrap
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 搜索栏（内置，TopAppBar 之下，状态栏下方）
            if (showSearchBar && codeEditor != null) {
                val searcher = codeEditor!!.searcher
                // 函数匹配模式：搜索框含换行（多行代码），按第一行匹配同名函数
                val isFunctionMode = searchKeyword.contains("\n")
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchKeyword,
                                onValueChange = {
                                    searchKeyword = it
                                    // 内容变化时重置函数匹配结果
                                    if (functionMatches.isNotEmpty()) {
                                        functionMatches = emptyList()
                                        currentMatchIndex = -1
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("输入关键词或粘贴函数代码搜索") },
                                singleLine = false,
                                maxLines = 3,
                                trailingIcon = {
                                    if (searchKeyword.isNotEmpty()) {
                                        IconButton(onClick = {
                                            searchKeyword = ""
                                            searcher.stopSearch()
                                            functionMatches = emptyList()
                                            currentMatchIndex = -1
                                        }) {
                                            Icon(Icons.Default.Clear, "清除")
                                        }
                                    }
                                }
                            )
                            IconButton(onClick = {
                                if (searchKeyword.isNotEmpty()) {
                                    if (isFunctionMode) {
                                        // 函数匹配模式：按第一行查找所有同名函数
                                        val fm = functionMatcher
                                        val editor = codeEditor
                                        if (fm != null && editor != null) {
                                            val signature = fm.extractFunctionSignature(searchKeyword)
                                            val matches = fm.findFunctions(signature)
                                            functionMatches = matches
                                            if (matches.isNotEmpty()) {
                                                currentMatchIndex = 0
                                                val block = matches[0]
                                                editor.setSelection(block.startLine, 0)
                                                editor.ensurePositionVisible(block.startLine, 0)
                                            } else {
                                                currentMatchIndex = -1
                                                context.longToast("未找到匹配的函数")
                                            }
                                        }
                                    } else {
                                        // 普通搜索模式
                                        searcher.search(searchKeyword, EditorSearcher.SearchOptions(false, false))
                                        codeEditor!!.postDelayed({
                                            if (searcher.hasQuery()) {
                                                runCatching { searcher.gotoNext() }
                                            }
                                        }, 100)
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Search, "搜索")
                            }
                            IconButton(onClick = {
                                if (isFunctionMode) {
                                    // 函数模式：上一个匹配
                                    if (functionMatches.isNotEmpty() && currentMatchIndex > 0) {
                                        currentMatchIndex--
                                        val block = functionMatches[currentMatchIndex]
                                        codeEditor?.setSelection(block.startLine, 0)
                                        codeEditor?.ensurePositionVisible(block.startLine, 0)
                                    }
                                } else {
                                    if (searcher.hasQuery()) {
                                        runCatching { searcher.gotoPrevious() }
                                    }
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Undo, "上一个")
                            }
                            IconButton(onClick = {
                                if (isFunctionMode) {
                                    // 函数模式：下一个匹配
                                    if (functionMatches.isNotEmpty() && currentMatchIndex < functionMatches.size - 1) {
                                        currentMatchIndex++
                                        val block = functionMatches[currentMatchIndex]
                                        codeEditor?.setSelection(block.startLine, 0)
                                        codeEditor?.ensurePositionVisible(block.startLine, 0)
                                    }
                                } else {
                                    if (searcher.hasQuery()) {
                                        runCatching { searcher.gotoNext() }
                                    }
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Redo, "下一个")
                            }
                            IconButton(onClick = {
                                showSearchBar = false
                                searcher.stopSearch()
                                functionMatches = emptyList()
                                currentMatchIndex = -1
                            }) {
                                Icon(Icons.Default.Clear, "关闭搜索")
                            }
                        }
                        // 函数匹配模式：显示替换按钮和匹配信息
                        if (isFunctionMode && functionMatches.isNotEmpty() && currentMatchIndex >= 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "匹配 ${currentMatchIndex + 1}/${functionMatches.size} (第${functionMatches[currentMatchIndex].startLine + 1}行)",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                TextButton(onClick = {
                                    val fm = functionMatcher
                                    val editor = codeEditor
                                    val foldMgr = foldingManager
                                    if (fm != null && editor != null) {
                                        val block = functionMatches[currentMatchIndex]
                                        // 替换前：若该函数处于折叠状态，先展开
                                        val wasFolded = foldMgr?.isFolded(block.startLine) == true
                                        if (wasFolded) {
                                            foldMgr!!.unfoldAt(block.startLine)
                                        }
                                        // 展开后行号已变化，必须重新查找当前函数的真实范围再替换
                                        val signature = fm.extractFunctionSignature(searchKeyword)
                                        val refreshedMatches = fm.findFunctions(signature)
                                        // 从刷新结果中找到 startLine 与原 block 相同的项（或最近的）
                                        val refreshedBlock = refreshedMatches.firstOrNull {
                                            it.startLine == block.startLine
                                        } ?: refreshedMatches.getOrNull(currentMatchIndex) ?: block
                                        fm.replaceFunction(refreshedBlock.startLine, refreshedBlock.endLine, searchKeyword)
                                        // 替换后重新查找匹配（行号已变化）
                                        functionMatches = fm.findFunctions(signature)
                                        currentMatchIndex = 0
                                        if (functionMatches.isNotEmpty()) {
                                            val newBlock = functionMatches[0]
                                            editor.setSelection(newBlock.startLine, 0)
                                            editor.ensurePositionVisible(newBlock.startLine, 0)
                                            // 替换前是折叠状态则重新折叠
                                            if (wasFolded) {
                                                foldMgr?.fold(newBlock.startLine, newBlock.endLine)
                                            }
                                        }
                                        context.longToast("已替换")
                                    }
                                }) {
                                    Icon(Icons.Default.FindReplace, null, Modifier.size(18.dp))
                                    Text("替换此函数")
                                }
                            }
                        }
                    }
                }
            }

            // 用自定义 Layout 叠加编辑器和 overlay，overlay 不参与高度测量
            // CodeEditor 直接获得 weight 的 EXACTLY 约束，不会被 sora-editor 撑高
            androidx.compose.ui.layout.Layout(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                content = {
                    CodeEditor(
                        modifier = Modifier, onUpdate = {
                        it.isUndoEnabled = true
                        it.text.addContentListener(object : ContentListener {
                            override fun beforeReplace(content: Content) {
                            }

                            override fun afterInsert(
                                content: Content,
                                startLine: Int,
                                startColumn: Int,
                                endLine: Int,
                                endColumn: Int,
                                insertedContent: CharSequence,
                            ) {
                            }

                            override fun afterDelete(
                                content: Content,
                                startLine: Int,
                                startColumn: Int,
                                endLine: Int,
                                endColumn: Int,
                                deletedContent: CharSequence,
                            ) {
                            }
                        })
                        codeEditor = it
                        if (foldingManager == null) {
                            val fm = CodeFoldingManager(it)
                            foldingManager = fm
                            functionMatcher = FunctionMatcher(it)
                            // 恢复持久化的折叠状态
                            foldStateKey?.let { key ->
                                val marks = CodeEditorConfig.loadFoldStates(key)
                                if (marks.isNotEmpty()) fm.scheduleRestore(marks)
                            }
                        }

                        onUpdate(it)
                    }
                    )

                    val fm = foldingManager
                    val ce = codeEditor
                    if (fm != null && ce != null) {
                        CodeFoldingOverlay(
                            editor = ce,
                            foldingManager = fm,
                        )
                    }
                }
            ) { measurables, constraints ->
                // 关键修复：强制给 CodeEditor 传 EXACTLY 约束
                // sora-editor 的 onMeasure 在 AT_MOST 模式下会返回 rowHeight×lineCount
                // 代码越多返回越高，导致空白。强制 EXACTLY 后高度固定为分配空间。
                val fixedConstraints = androidx.compose.ui.unit.Constraints(
                    minWidth = constraints.maxWidth,
                    minHeight = constraints.maxHeight,
                    maxWidth = constraints.maxWidth,
                    maxHeight = constraints.maxHeight
                )
                val editorPlaceable = measurables[0].measure(fixedConstraints)
                // overlay 用编辑器实际尺寸测量
                val overlayPlaceable = if (measurables.size > 1) {
                    val overlayConstraints = androidx.compose.ui.unit.Constraints(
                        minWidth = 0,
                        minHeight = 0,
                        maxWidth = editorPlaceable.width,
                        maxHeight = editorPlaceable.height
                    )
                    measurables[1].measure(overlayConstraints)
                } else null
                layout(editorPlaceable.width, editorPlaceable.height) {
                    editorPlaceable.place(0, 0)
                    overlayPlaceable?.place(0, 0)
                }
            }

            val symbolMap = remember {
                linkedMapOf(
                    "\t" to "TAB",
                    "=" to "=",
                    ">" to ">",
                    "{" to "{",
                    "}" to "}",
                    "(" to "(",
                    ")" to ")",
                    "," to ",",
                    "." to ".",
                    ";" to ";",
                    "'" to "'",
                    "\"" to "\"",
                    "?" to "?",
                    "+" to "+",
                    "-" to "-",
                    "*" to "*",
                    "/" to "/",
                )
            }

            HorizontalDivider(thickness = 1.dp)
            LazyRow(
                Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .imePadding()
                    .then(
                        if (WindowInsets.isImeVisible) Modifier else Modifier.navigationBarsPadding()
                    )
            ) {
                items(symbolMap.toList()) {
                    Box(
                        Modifier
                            .clickableRipple {
                                codeEditor?.let { editor ->
                                    val text = it.first
                                    if (editor.isEditable)
                                        if ("\t" == text && editor.snippetController.isInSnippet())
                                            editor.snippetController.shiftToNextTabStop()
                                        else
                                            editor.insertText(text, 1)
                                }
                            }) {
                        Text(
                            text = it.second,
                            Modifier
                                .minimumInteractiveComponentSize()
                                .align(Alignment.Center)
                        )
                    }
                }

            }
        }
    }
}