package com.github.jing332.deepseekproxy

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.github.jing332.deepseekproxy.proxy.CnbClient
import com.github.jing332.deepseekproxy.proxy.LogStore
import android.graphics.BitmapFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val vm: ProxyViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App(vm) }
    }
}

// ── 紧凑 UI 尺寸：减小按钮高宽与内边距，降低整体占用空间 ──
private val CompactBtnPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
private val CompactBtnHeight = 30.dp
private val CompactBtnFont = 12.sp

// ── 输入框 / 选择框：紧凑高度 + 减小内边距，保证文字完整显示 ──
private val FieldHeight = 40.dp
private val InputAreaHeight = 52.dp
private val FieldFont = 12.sp
// 关键：默认 OutlinedTextField 上下内边距约 16dp，在固定小高度下会把文字挤出可视区；此处收紧到 4dp。
private val FieldContentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)

/**
 * 紧凑输入框：基于 BasicTextField + OutlinedTextField 装饰盒，
 * 通过 contentPadding 大幅减小文字与边框的内边距，使小高度下文字仍可完整显示。
 * 不使用浮动 label（改用 placeholder），避免占用顶部空间。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val textStyle = LocalTextStyle.current.copy(
        fontSize = FieldFont,
        color = MaterialTheme.colorScheme.onSurface
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        readOnly = readOnly,
        singleLine = singleLine,
        maxLines = maxLines,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        interactionSource = interaction,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { inner ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = inner,
                enabled = true,
                singleLine = singleLine,
                visualTransformation = VisualTransformation.None,
                interactionSource = interaction,
                placeholder = placeholder?.let { { Text(it, fontSize = FieldFont) } },
                trailingIcon = trailingIcon,
                contentPadding = FieldContentPadding,
                container = {
                    OutlinedTextFieldDefaults.ContainerBox(
                        enabled = true,
                        isError = false,
                        interactionSource = interaction,
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                }
            )
        }
    )
}

/** 紧凑实心按钮：更小的高度、内边距与字号。 */
@Composable
private fun CBtn(onClick: () -> Unit, text: String) {
    Button(
        onClick = onClick,
        contentPadding = CompactBtnPadding,
        modifier = Modifier.heightIn(min = CompactBtnHeight)
    ) { Text(text, fontSize = CompactBtnFont) }
}

/** 紧凑描边按钮：更小的高度、内边距与字号，可选自定义配色。
 *  使用 Surface + pointerInput 实现，避免 OutlinedButton 自带的 clickable 抢占长按手势。
 *  传入 onLongClick 时：单击=onClick，长按=onLongClick。 */
/** 浏览器拦截 LongCat 请求时暂存下来的请求头（mtgsig / m-traceid / base_url），由「提取」按钮再合并 CookieManager 里的 Cookie。 */
private data class LongCatCaptured(val mtgsig: String, val mTraceId: String, val baseUrl: String)

@Composable
private fun COutBtn(
    onClick: () -> Unit,
    text: String,
    colors: ButtonColors? = null,
    onLongClick: (() -> Unit)? = null
) {
    val c = colors ?: ButtonDefaults.outlinedButtonColors()
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = c.containerColor,
        contentColor = c.contentColor,
        border = BorderStroke(1.dp, c.contentColor),
        modifier = Modifier
            .heightIn(min = CompactBtnHeight)
            .then(
                if (onLongClick != null) Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { onLongClick() }
                    )
                } else Modifier.clickable { onClick() }
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(CompactBtnPadding)
        ) { Text(text, fontSize = CompactBtnFont) }
    }
}

/** 将字号整体缩小 2sp。 */
private fun androidx.compose.ui.text.TextStyle.shrink2(): androidx.compose.ui.text.TextStyle =
    this.copy(fontSize = (this.fontSize.value - 2).coerceAtLeast(8f).sp)

/** 生成整体缩小 2 号的排版方案。 */
private fun compactTypography(): Typography {
    val b = Typography()
    return b.copy(
        displayLarge = b.displayLarge.shrink2(),
        displayMedium = b.displayMedium.shrink2(),
        displaySmall = b.displaySmall.shrink2(),
        headlineLarge = b.headlineLarge.shrink2(),
        headlineMedium = b.headlineMedium.shrink2(),
        headlineSmall = b.headlineSmall.shrink2(),
        titleLarge = b.titleLarge.shrink2(),
        titleMedium = b.titleMedium.shrink2(),
        titleSmall = b.titleSmall.shrink2(),
        bodyLarge = b.bodyLarge.shrink2(),
        bodyMedium = b.bodyMedium.shrink2(),
        bodySmall = b.bodySmall.shrink2(),
        labelLarge = b.labelLarge.shrink2(),
        labelMedium = b.labelMedium.shrink2(),
        labelSmall = b.labelSmall.shrink2()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(vm: ProxyViewModel) {
    val cookiesFlow by vm.cookies.collectAsState()
    val selectedIdx by vm.selectedIndex.collectAsState()
    val providerFlow by vm.provider.collectAsState()
    val modelFlow by vm.chatModel.collectAsState()
    val portFlow by vm.port.collectAsState()
    val running by vm.serverRunning.collectAsState()
    val streamFlow by vm.streamMode.collectAsState()
    val deleteSessionFlow by vm.deleteSessionAfterReply.collectAsState()
    val messages by vm.chatMessages.collectAsState()
    val logs by vm.logs.collectAsState()
    val dsTokenFlow by vm.deepseekToken.collectAsState()
    val dsTokensFlow by vm.deepseekTokensFlow.collectAsState()
    val dsSelIdx by vm.deepseekSelectedIndex.collectAsState()
    val longCatConfigFlow by vm.longCatConfig.collectAsState()
    var longCatConfig by remember { mutableStateOf(longCatConfigFlow) }
    LaunchedEffect(longCatConfigFlow) { longCatConfig = longCatConfigFlow }
    val longCatConfigsFlow by vm.longCatConfigsFlow.collectAsState()
    val longCatSelIdx by vm.longCatSelectedIndex.collectAsState()

    var provider by remember { mutableStateOf(providerFlow) }
    var cookieText by remember { mutableStateOf(cookiesFlow.getOrNull(selectedIdx) ?: "") }
    LaunchedEffect(selectedIdx, cookiesFlow) { cookieText = cookiesFlow.getOrNull(selectedIdx) ?: "" }
    var dsToken by remember { mutableStateOf(dsTokenFlow) }
    LaunchedEffect(dsTokenFlow) { dsToken = dsTokenFlow }

    var model by remember { mutableStateOf(modelFlow) }
    var port by remember { mutableStateOf(portFlow.toString()) }
    var input by remember { mutableStateOf("") }
    var streamMode by remember { mutableStateOf(streamFlow) }
    var deleteSessionAfterReply by remember { mutableStateOf(deleteSessionFlow) }
    var tab by remember { mutableStateOf(0) }
    var showCookieDialog by remember { mutableStateOf(false) }
    var showTokenDialog by remember { mutableStateOf(false) }
    var showLongCatDialog by remember { mutableStateOf(false) }

    val appContext = LocalContext.current
    // 每次打开 APP 强制刷新一次混元凭证（最多 8 小时限频由 CnbClient 内部保证，此处为强制触发）
    LaunchedEffect(Unit) {
        CnbClient.storageDir = appContext.cacheDir
        withContext(Dispatchers.IO) {
            runCatching { CnbClient.ensureAuth(force = true) }
        }
    }

    if (showCookieDialog) {
        CredentialPickerDialog(
            title = "选择 ${if (provider == "kimi") "Kimi" else "豆包"} Cookie",
            items = cookiesFlow,
            selectedIndex = selectedIdx,
            labelOf = { i -> if (i == 0) "默认Cookie" else "Cookie ${i + 1}" },
            addLabel = "新增Cookie",
            onSelect = { vm.setSelectedIndex(it); showCookieDialog = false },
            onAdd = { vm.addCookie() },
            onDelete = { vm.removeCookie(it) },
            onDismiss = { showCookieDialog = false }
        )
    }
    if (showTokenDialog) {
        CredentialPickerDialog(
            title = "选择 DeepSeek Token",
            items = dsTokensFlow,
            selectedIndex = dsSelIdx,
            labelOf = { i -> if (i == 0) "默认Token" else "Token ${i + 1}" },
            addLabel = "新增Token",
            onSelect = { vm.setDeepSeekSelectedIndex(it); showTokenDialog = false },
            onAdd = { vm.addDeepSeekToken() },
            onDelete = { vm.removeDeepSeekToken(it) },
            onDismiss = { showTokenDialog = false }
        )
    }
    if (showLongCatDialog) {
        CredentialPickerDialog(
            title = "选择 LongCat 配置",
            items = longCatConfigsFlow,
            selectedIndex = longCatSelIdx,
            labelOf = { i -> if (i == 0) "默认配置" else "配置 ${i + 1}" },
            addLabel = "新增配置",
            onSelect = { vm.setLongCatSelectedIndex(it); showLongCatDialog = false },
            onAdd = { vm.addLongCatConfig() },
            onDelete = { vm.removeLongCatConfig(it) },
            onDismiss = { showLongCatDialog = false }
        )
    }

    MaterialTheme(typography = compactTypography()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    actions = {
                        TabRow(
                            selectedTabIndex = tab,
                            modifier = Modifier.width(300.dp)
                        ) {
                            Tab(selected = tab == 0, onClick = { tab = 0 }) { Text("中转") }
                            Tab(selected = tab == 1, onClick = { tab = 1 }) { Text("日志") }
                            Tab(selected = tab == 2, onClick = { tab = 2 }) { Text("浏览器") }
                        }
                    }
                )
            }
        ) { padding ->
            if (tab == 0) {
                ProxyScreen(
                    vm, cookieText, model, port, running, streamMode, messages, input,
                    provider = provider,
                    onProviderChange = { provider = it; vm.setProvider(it) },
                    cookieLabel = vm.cookieLabel(selectedIdx),
                    onCookieChange = {
                        cookieText = it
                        vm.updateSelectedCookie(it)
                    },
                    onOpenCookieDialog = { showCookieDialog = true },
                    onOpenTokenDialog = { showTokenDialog = true },
                    onOpenLongCatDialog = { showLongCatDialog = true },
                    onModelChange = {
                        model = it
                        // 运行中切换模型立即生效：同步给 ViewModel，聊天框发送时会用新模型请求服务端
                        vm.setChatModel(it)
                    },
                    longCatConfig = longCatConfig,
                    onLongCatConfigChange = {
                        longCatConfig = it
                        vm.setLongCatConfig(it)
                    },
                    onPortChange = { port = it },
                    onInputChange = { input = it },
                    deepseekToken = dsToken,
                    onDeepSeekTokenChange = {
                        dsToken = it
                        vm.setDeepSeekToken(it.trim())
                    },
                    onDeepSeekModeChange = { vm.setDeepSeekMode(it) },
                    onStreamModeChange = {
                        streamMode = it
                        vm.setStreamMode(it)
                    },
                    deleteSessionAfterReply = deleteSessionAfterReply,
                    onDeleteSessionChange = {
                        deleteSessionAfterReply = it
                        vm.setDeleteSessionAfterReply(it)
                    },
                    onStart = {
                        vm.updateSelectedCookie(cookieText.trim())
                        if (provider == "deepseek") vm.setDeepSeekToken(dsToken.trim())
                        vm.setChatModel(model)
                        vm.startServer()
                    },
                    onStop = { vm.stopServer() },
                    onSavePort = { vm.setPort(port.toIntOrNull() ?: 8801) },
                    onSend = {
                        vm.chatInput.value = input
                        vm.sendChat()
                        input = ""
                    },
                    onGenerate = {
                        vm.chatInput.value = input
                        vm.generateImage()
                        input = ""
                    },
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp, vertical = 2.dp)
                )
            } else if (tab == 1) {
                LogScreen(
                    logs = logs,
                    onClear = { LogStore.clear() },
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            } else {
                BrowserScreen(
                    provider = provider,
                    onProviderChange = { provider = it; vm.setProvider(it) },
                    onLongCatConfigClear = { vm.setLongCatConfig("") },
                    onLongCatCaptured = { m, t, c, u -> vm.setLongCatFromRequest(m, t, c, u) },
                    onCookieExtracted = {
                        vm.updateSelectedCookie(it)
                        cookieText = it
                    },
                    onDeepSeekTokenExtracted = {
                        vm.setDeepSeekToken(it.trim())
                        dsToken = it
                    },
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProxyScreen(
    vm: ProxyViewModel,
    cookie: String,
    model: String,
    port: String,
    running: Boolean,
    streamMode: Boolean,
    messages: List<ChatMessageItem>,
    input: String,
    provider: String,
    onProviderChange: (String) -> Unit,
    cookieLabel: String,
    onCookieChange: (String) -> Unit,
    onOpenCookieDialog: () -> Unit,
    onOpenTokenDialog: () -> Unit,
    onOpenLongCatDialog: () -> Unit,
    onModelChange: (String) -> Unit,
    longCatConfig: String,
    onLongCatConfigChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onInputChange: (String) -> Unit,
    deepseekToken: String,
    onDeepSeekTokenChange: (String) -> Unit,
    onDeepSeekModeChange: (String) -> Unit,
    onStreamModeChange: (Boolean) -> Unit,
    deleteSessionAfterReply: Boolean,
    onDeleteSessionChange: (Boolean) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSavePort: () -> Unit,
    onSend: () -> Unit,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val portFlow by vm.port.collectAsState()
    val context = LocalContext.current

    val modelOptions = listOf(
        "hy3-preview" to "混元3",
        "doubao" to "豆包",
        "doubao-think" to "豆包-思考",
        "doubao-auto" to "豆包-自动",
        "doubao-expert" to "豆包-专家",
        "doubao-pro" to "豆包-Pro",
        "kimi" to "Kimi",
        "qwen" to "千问",
        "qwen-think" to "千问-思考",
        "deepseek" to "DeepSeek",
        "zhipu" to "智谱",
        "glm-image-hd" to "智谱-电影级生图",
        "longcat" to "龙猫",
        "longcat-search" to "龙猫-搜索"
    )
    var modelExpanded by remember { mutableStateOf(false) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        LogStore.i("App", "通知权限授予=$granted")
    }

    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun requestBatteryExemption() {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val pkg = context.packageName
        if (!pm.isIgnoringBatteryOptimizations(pkg)) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$pkg")
                    }
                )
            } catch (_: Exception) {
            }
        }
    }

    fun onStartClick() {
        ensureNotificationPermission()
        requestBatteryExemption()
        onStart()
    }

    Column(modifier) {
        // 服务商选择：豆包 / Kimi / DeepSeek
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("服务商:", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(6.dp))
            listOf("doubao" to "豆包", "kimi" to "Kimi", "qwen" to "千问", "deepseek" to "DeepSeek", "longcat" to "龙猫").forEach { (id, label) ->
                val selected = provider == id
                COutBtn(
                    onClick = {
                        onProviderChange(id)
                    },
                    text = label,
                    colors = if (selected) ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) else ButtonDefaults.outlinedButtonColors()
                )
                Spacer(Modifier.width(6.dp))
            }
        }
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (provider) {
                "deepseek" -> {
                    // DeepSeek 使用 Bearer Token 鉴权
                    CField(
                        value = deepseekToken, onValueChange = onDeepSeekTokenChange,
                        placeholder = "粘贴 DeepSeek 的 Bearer Token（Authorization 头）",
                        singleLine = false, maxLines = 2,
                        modifier = Modifier.weight(1f).height(InputAreaHeight)
                    )
                    Spacer(Modifier.width(6.dp))
                    CBtn(onClick = onOpenTokenDialog, text = "Token")
                }
                "longcat" -> {
                    // 龙猫为抓包配置（mtgsig / m_traceid / Cookie），原样粘贴 JSON；右侧「配置」按钮多账号切换
                    CField(
                        value = longCatConfig, onValueChange = onLongCatConfigChange,
                        placeholder = "粘贴 LongCat 配置 JSON（含 mtgsig / m_traceid / Cookie）",
                        singleLine = false, maxLines = 3,
                        modifier = Modifier.weight(1f).height(InputAreaHeight)
                    )
                    Spacer(Modifier.width(6.dp))
                    CBtn(onClick = onOpenLongCatDialog, text = "配置")
                }
                else -> {
                    CField(
                        value = cookie, onValueChange = onCookieChange,
                        placeholder = when (provider) {
                            "kimi" -> "粘贴 kimi.com 的 Cookie（含 JWT 自动提取）"
                            "qwen" -> "粘贴 qianwen.com 的 Cookie（含 tongyi_sso_ticket）"
                            else -> "粘贴 doubao.com 的完整 Cookie 字符串"
                        },
                        singleLine = false, maxLines = 2,
                        modifier = Modifier.weight(1f).height(InputAreaHeight)
                    )
                    Spacer(Modifier.width(6.dp))
                    CBtn(onClick = onOpenCookieDialog, text = "Cookie")
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        if (provider == "deepseek") {
            val dsModeFlow by vm.deepseekMode.collectAsState()
            var dsMode by remember { mutableStateOf(dsModeFlow) }
            LaunchedEffect(dsModeFlow) { dsMode = dsModeFlow }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("DeepSeek模式:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(6.dp))
                listOf("default" to "快速模式", "expert" to "专家模式").forEach { (id, label) ->
                    val selected = dsMode == id
                    COutBtn(
                        onClick = { dsMode = id; onDeepSeekModeChange(id) },
                        text = label,
                        colors = if (selected) ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ) else ButtonDefaults.outlinedButtonColors()
                    )
                    Spacer(Modifier.width(6.dp))
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        FlowRow(
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CField(
                value = port, onValueChange = { onPortChange(it.filter { c -> c.isDigit() }) },
                placeholder = "端口",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(70.dp).height(FieldHeight)
            )
            CBtn(onClick = onSavePort, text = "保存")
            CBtn(onClick = { onStartClick() }, text = if (running) "重启" else "启动")
            if (running) {
                COutBtn(onClick = onStop, text = "停止")
            }
        }
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("模型:", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(6.dp))
            ExposedDropdownMenuBox(
                expanded = modelExpanded,
                onExpandedChange = { modelExpanded = it },
                modifier = Modifier.width(140.dp)
            ) {
                CField(
                    value = modelOptions.firstOrNull { it.first == model }?.second ?: model,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth().height(FieldHeight),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = modelExpanded,
                    onDismissRequest = { modelExpanded = false }
                ) {
                    modelOptions.forEach { (id, label) ->
                        DropdownMenuItem(
                            text = { Text(label, fontSize = FieldFont) },
                            onClick = {
                                onModelChange(id)
                                modelExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            Text("流式回复", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(6.dp))
            Switch(checked = streamMode, onCheckedChange = onStreamModeChange)
        }
        Spacer(Modifier.height(2.dp))
        FlowRow(
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            COutBtn(onClick = { ensureNotificationPermission() }, text = "通知权限")
            COutBtn(onClick = { requestBatteryExemption() }, text = "电池优化")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("完成回复后删除会话", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(4.dp))
                Switch(
                    checked = deleteSessionAfterReply,
                    onCheckedChange = onDeleteSessionChange
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("生图尺寸:", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(6.dp))
            val imageSizeFlow by vm.imageSize.collectAsState()
            var sizeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = sizeExpanded,
                onExpandedChange = { sizeExpanded = it },
                modifier = Modifier.width(200.dp)
            ) {
                CField(
                    value = IMAGE_SIZE_OPTIONS.firstOrNull { it.second == imageSizeFlow }?.first ?: imageSizeFlow,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth().height(FieldHeight),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sizeExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = sizeExpanded,
                    onDismissRequest = { sizeExpanded = false }
                ) {
                    IMAGE_SIZE_OPTIONS.forEach { (label, id) ->
                        DropdownMenuItem(
                            text = { Text(label, fontSize = FieldFont) },
                            onClick = {
                                vm.setImageSize(id)
                                sizeExpanded = false
                            }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            "聊天: http://127.0.0.1:$portFlow/v1/chat/completions   生图: http://127.0.0.1:$portFlow/v1/images/generations",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(2.dp))
        HorizontalDivider()
        Spacer(Modifier.height(2.dp))

        val listState = rememberLazyListState()
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState) {
            items(messages) { msg ->
                val isUser = msg.role == "user"
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (isUser) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.widthIn(max = 300.dp).padding(4.dp)
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            if (msg.text.isNotBlank()) {
                                Text(msg.text)
                                if (msg.images.isNotEmpty()) Spacer(Modifier.height(6.dp))
                            }
                            msg.images.forEach { url ->
                                Spacer(Modifier.height(6.dp))
                                UrlImage(url, Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Row {
            CField(
                value = input, onValueChange = onInputChange,
                placeholder = "输入消息（生图时作为提示词）",
                singleLine = false, maxLines = 2,
                modifier = Modifier.weight(1f).height(InputAreaHeight)
            )
            Spacer(Modifier.width(6.dp))
            CBtn(onClick = onSend, text = "发送")
            Spacer(Modifier.width(6.dp))
            COutBtn(onClick = onGenerate, text = "生图")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialPickerDialog(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    labelOf: (Int) -> String,
    addLabel: String,
    onSelect: (Int) -> Unit,
    onAdd: () -> Unit,
    onDelete: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            ) {
                itemsIndexed(items) { i, c ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(i) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                labelOf(i),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                if (c.isBlank()) "（空）" else (c.take(40) + if (c.length > 40) "…" else ""),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        if (i == selectedIndex) {
                            Text("当前", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.width(8.dp))
                        }
                        if (items.size > 1) {
                            TextButton(onClick = { onDelete(i) }) { Text("删除") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onAdd) { Text(addLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
fun UrlImage(url: String, modifier: Modifier = Modifier) {
    val viewRef = remember { mutableStateOf<ImageView?>(null) }
            AndroidView(
                factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
            }.also { viewRef.value = it }
        },
        modifier = modifier
    )
    LaunchedEffect(url) {
        val iv = viewRef.value ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                    if (resp.isSuccessful) {
                        resp.body?.bytes()?.let { bytes ->
                            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bmp != null) withContext(Dispatchers.Main) { iv.setImageBitmap(bmp) }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }
}

@Composable
fun LogScreen(
    logs: List<String>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.lastIndex)
    }
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("运行日志 (${logs.size})", style = MaterialTheme.typography.titleSmall)
            Row {
                OutlinedButton(onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("log", logs.joinToString("\n")))
                }) { Text("复制") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onClear) { Text("清空") }
            }
        }
        HorizontalDivider()
        SelectionContainer(Modifier.fillMaxSize().padding(8.dp)) {
            LazyColumn(Modifier.fillMaxSize(), state = listState) {
                items(logs) { line ->
                    Text(
                        line,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * 内置浏览器：默认打开豆包主页，登录后可一键提取 Cookie 并写入设置输入框。
 * 顶部可切换「豆包 / Kimi」服务商，分别打开对应站点并提取对应凭证
 * （豆包=Cookie，Kimi=Bearer Token/JWT）。Cookie 由系统级 CookieManager 管理。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    provider: String,
    onProviderChange: (String) -> Unit,
    onLongCatConfigClear: () -> Unit,
    onLongCatCaptured: (String, String, String, String) -> Unit,
    onCookieExtracted: (String) -> Unit,
    onDeepSeekTokenExtracted: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    // DeepSeek 的 Bearer 在请求头 authorization 里（不在 cookie），靠 shouldInterceptRequest 拦截抓取
    val capturedToken = remember { mutableStateOf<String?>(null) }
    val capturedLongCat = remember { mutableStateOf<LongCatCaptured?>(null) }
    var url by remember(provider) {
        mutableStateOf(
            when (provider) {
                "kimi" -> "https://www.kimi.com/"
                "qwen" -> "https://www.qianwen.com/"
                "deepseek" -> "https://chat.deepseek.com/"
                "longcat" -> "https://longcat.chat/t"
                else -> "https://www.doubao.com/chat/"
            }
        )
    }
    var progress by remember { mutableStateOf(0) }
    var toast by remember { mutableStateOf<String?>(null) }

    // 切换服务商时跳转到对应站点
    LaunchedEffect(provider) {
        capturedToken.value = null
        capturedLongCat.value = null
        val target = when (provider) {
            "kimi" -> "https://www.kimi.com/"
            "qwen" -> "https://www.qianwen.com/"
            "deepseek" -> "https://chat.deepseek.com/"
            "longcat" -> "https://longcat.chat/t"
            else -> "https://www.doubao.com/chat/"
        }
        url = target
        webViewRef.value?.loadUrl(target)
    }

    toast?.let {
        LaunchedEffect(it) {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            toast = null
        }
    }

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 选择框：选择模型/服务商即进入对应站点；旁边「清空」按钮清空该站点 Cookie（龙猫为清空配置）
            fun clearProviderCookies(p: String) {
                if (p == "longcat") {
                    onLongCatConfigClear()
                    // 同时清掉 longcat.chat 网站的登录 Cookie，彻底退出登录（否则重新打开仍已登录）
                    val cm = CookieManager.getInstance()
                    cm.flush()
                    listOf("https://longcat.chat", "https://www.longcat.chat").forEach { host ->
                        cm.getCookie(host)?.split(";")?.forEach { piece ->
                            val name = piece.substringBefore("=").trim()
                            if (name.isNotEmpty() && !name.startsWith(".")) {
                                cm.setCookie(host, "$name=; expires=Thu, 01 Jan 1970 00:00:00 GMT; Max-Age=0; path=/")
                            }
                        }
                    }
                    cm.flush()
                    toast = "已清空龙猫配置并退出网站登录"
                    webViewRef.value?.loadUrl(url)
                    return
                }
                val cm = CookieManager.getInstance()
                // 该服务商对应的 base 域（即“关键词”）。含该关键词的所有网址 Cookie 都清
                val bases = when (p) {
                    "kimi" -> listOf("kimi.com")
                    "qwen" -> listOf("qianwen.com")
                    "deepseek" -> listOf("deepseek.com")
                    else -> listOf("doubao.com")
                }
                // 常见子域前缀，拼出“网址里带该关键词”的所有可能 host
                val subPrefixes = listOf("", "www.", "api.", "account.", "accounts.", "auth.", "login.", "platform.", "chat.", "m.")
                val hosts = bases.flatMap { base -> subPrefixes.map { "https://$it$base" } }

                val expired = "expires=Thu, 01 Jan 1970 00:00:00 GMT; Max-Age=0; path=/"
                // host -> 该 host 上的 cookie 名集合（用于删 host-only cookie）
                val hostCookieNames = mutableMapOf<String, MutableSet<String>>()
                // 全量 cookie 名（用于删 domain 级 cookie，覆盖所有子域）
                val allNames = mutableSetOf<String>()
                hosts.forEach { host ->
                    cm.getCookie(host)?.split(";")?.forEach { piece ->
                        val name = piece.substringBefore("=").trim()
                        // 跳过非法 cookie 名（如以 . 开头的 .thumbcache_xxx）
                        if (name.isNotEmpty() && !name.startsWith(".")) {
                            hostCookieNames.getOrPut(host) { mutableSetOf() }.add(name)
                            allNames.add(name)
                        }
                    }
                }
                val label = when (p) { "kimi" -> "Kimi"; "qwen" -> "千问"; "deepseek" -> "DeepSeek"; else -> "豆包" }
                if (allNames.isEmpty()) {
                    webViewRef.value?.loadUrl(url)
                    toast = "没有可清空的 $label Cookie"
                    return
                }
                // 1) 删 host-only cookie（各具体 host）
                hostCookieNames.forEach { (host, names) ->
                    names.forEach { n -> cm.setCookie(host, "$n=; $expired", null) }
                }
                // 2) 删 domain 级 cookie：Android 旧版把 "deepseek.com" 与 ".deepseek.com" 视为不同域，
                //    两种都删；domain 级删除后，该关键词下所有子域的会话 Cookie 一并失效
                bases.forEach { base ->
                    allNames.forEach { n ->
                        cm.setCookie("https://$base", "$n=; domain=.$base; $expired", null)
                        cm.setCookie("https://$base", "$n=; domain=$base; $expired", null)
                    }
                }
                // 不依赖 setCookie 回调（部分机型不触发），统一延时后 flush + 清 localStorage + 刷新
                Handler(Looper.getMainLooper()).postDelayed({
                    cm.flush()
                    webViewRef.value?.evaluateJavascript(
                        "try{localStorage.clear();sessionStorage.clear();}catch(e){}",
                        null
                    )
                    webViewRef.value?.loadUrl(url)
                    toast = "已清空 $label 的浏览器 Cookie，可重新登录另一个账号"
                }, 400)
            }
            // 服务商选择框：选择即进入对应站点；旁边「清空」按钮清该站点 Cookie（龙猫为清空配置）
            val provItems = listOf("doubao" to "豆包", "kimi" to "Kimi", "qwen" to "千问", "deepseek" to "DeepSeek", "longcat" to "龙猫")
            var provExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = provExpanded,
                onExpandedChange = { provExpanded = it },
                modifier = Modifier.width(120.dp)
            ) {
                CField(
                    value = when (provider) {
                        "kimi" -> "Kimi"; "qwen" -> "千问"; "deepseek" -> "DeepSeek"; "longcat" -> "龙猫"; else -> "豆包"
                    },
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth().height(FieldHeight),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = provExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = provExpanded,
                    onDismissRequest = { provExpanded = false }
                ) {
                    provItems.forEach { (id, label) ->
                        DropdownMenuItem(
                            text = { Text(label, fontSize = FieldFont) },
                            onClick = { onProviderChange(id); provExpanded = false }
                        )
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            CBtn(onClick = { clearProviderCookies(provider) }, text = "清空")
            Spacer(Modifier.width(6.dp))
            CField(
                value = url, onValueChange = { url = it },
                placeholder = "网址",
                singleLine = true,
                modifier = Modifier.weight(1f).height(FieldHeight)
            )
            Spacer(Modifier.width(6.dp))
            CBtn(onClick = { webViewRef.value?.loadUrl(url) }, text = "前往")
            Spacer(Modifier.width(6.dp))
            if (provider == "deepseek") {
                Button(onClick = {
                    // DeepSeek：从拦截到的请求头里取 Bearer（cookie 里没有）
                    val t = capturedToken.value
                    if (t.isNullOrBlank()) {
                        toast = "尚未捕获到 Authorization 头，请先在 chat.deepseek.com 登录并发一条消息"
                    } else {
                        onDeepSeekTokenExtracted(t)
                        toast = "已提取 DeepSeek Bearer 并填入设置"
                    }
                }, contentPadding = CompactBtnPadding, modifier = Modifier.heightIn(min = CompactBtnHeight)) {
                    Text("提取Token", fontSize = CompactBtnFont)
                }
            } else if (provider == "longcat") {
                Button(onClick = {
                    val cap = capturedLongCat.value
                    if (cap == null || cap.mtgsig.isBlank()) {
                        toast = "请先在 longcat.chat 登录并发一条消息，以便捕获请求头"
                        return@Button
                    }
                    val cm = CookieManager.getInstance()
                    cm.flush()
                    val cookie = listOf("https://longcat.chat", "https://www.longcat.chat")
                        .flatMap { cm.getCookie(it).orEmpty().split(';') }
                        .map { it.trim() }.filter { it.isNotBlank() }
                        .distinct().joinToString("; ")
                    if (cookie.isBlank()) {
                        toast = "未获取到 Cookie，请先在 longcat.chat 登录"
                        return@Button
                    }
                    onLongCatCaptured(cap.mtgsig, cap.mTraceId, cookie, cap.baseUrl)
                    toast = "已提取龙猫配置（mtgsig + Cookie）"
                }, contentPadding = CompactBtnPadding, modifier = Modifier.heightIn(min = CompactBtnHeight)) {
                    Text("提取", fontSize = CompactBtnFont)
                }
            } else {
                Button(onClick = {
                    val cm = CookieManager.getInstance()
                    cm.flush()
                    // 豆包 / Kimi 采用完全相同的 Cookie 提取方式：
                    // 合并对应站点两个域（带 www 与不带）下的 Cookie，去重后以 "; " 拼接填入设置。
                    val (host1, host2, siteName) = when (provider) {
                        "kimi" -> Triple("https://www.kimi.com", "https://kimi.com", "Kimi")
                        "qwen" -> Triple("https://www.qianwen.com", "https://qianwen.com", "千问")
                        else -> Triple("https://www.doubao.com", "https://doubao.com", "豆包")
                    }
                    val cookies = listOf(
                        cm.getCookie(host1).orEmpty(),
                        cm.getCookie(host2).orEmpty()
                    ).flatMap { s -> s.split(';') }
                        .map { it.trim() }.filter { it.isNotBlank() }
                        .distinct()
                        .joinToString("; ")
                    if (cookies.isBlank()) {
                        toast = "未获取到 Cookie，请先在页面登录 $siteName"
                    } else {
                        onCookieExtracted(cookies)
                        toast = "已提取 $siteName Cookie 并填入设置（中转时自动提取 Token 鉴权）"
                    }
                }, contentPadding = CompactBtnPadding, modifier = Modifier.heightIn(min = CompactBtnHeight)) {
                    Text("提取Cookie", fontSize = CompactBtnFont)
                }
            }
        }
        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier.fillMaxWidth().height(3.dp)
            )
        } else {
            HorizontalDivider()
        }
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    // 关键：AndroidView 默认给子 View 设 WRAP_CONTENT，WebView 在内容未布局前会塌成 0 高度导致空白。
                    // 必须显式 MATCH_PARENT 让其撑满父容器。
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    // 使用手机版 UA，让豆包返回移动端页面（桌面版会超出屏幕且只显示左上角）
                    settings.userAgentString =
                        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    // 按设备宽度渲染，避免桌面宽版面只显示左上角、无法左右移动
                    settings.useWideViewPort = false
                    settings.loadWithOverviewMode = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.mixedContentMode =
                        android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(
                            view: WebView?,
                            urlStr: String?,
                            favicon: android.graphics.Bitmap?
                        ) {
                            urlStr?.let { url = it }
                        }

                        override fun onPageFinished(view: WebView?, urlStr: String?) {
                            progress = 0
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: android.webkit.WebResourceRequest?,
                            error: android.webkit.WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) {
                                LogStore.e("Browser", "页面加载错误: ${error?.errorCode} ${error?.description}")
                                toast = "页面加载失败: ${error?.description}"
                            }
                        }

                        override fun onReceivedHttpError(
                            view: WebView?,
                            request: android.webkit.WebResourceRequest?,
                            errorResponse: android.webkit.WebResourceResponse?
                        ) {
                            if (request?.isForMainFrame == true) {
                                LogStore.e("Browser", "HTTP 错误: ${errorResponse?.statusCode}")
                            }
                        }
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: android.webkit.WebResourceRequest?
                        ): android.webkit.WebResourceResponse? {
                            // DeepSeek 的 Bearer 在请求头 authorization 中（不在 cookie）。
                            // 拦截所有发往 chat.deepseek.com 的请求，抠出 Bearer 存起来，
                            // 直接按请求 URL 判断，不依赖 provider 闭包（避免 WebView 不重建时取值过期）。
                            if (request?.url?.host?.contains("chat.deepseek.com") == true) {
                                val headers = request?.requestHeaders
                                val auth = headers?.get("authorization") ?: headers?.get("Authorization")
                                if (!auth.isNullOrBlank()) {
                                    val token = auth.removePrefix("Bearer ").trim()
                                    if (token.isNotBlank()) capturedToken.value = token
                                }
                            }
                            // 龙猫：拦截 longcat.chat 的请求，记下请求头里的 mtgsig / m-traceid（Cookie 由「提取」按钮从 CookieManager 取，与 DeepSeek 同一机制）。
                            if (request?.url?.host?.contains("longcat.chat") == true) {
                                val h = request?.requestHeaders
                                val mg = h?.get("mtgsig") ?: h?.get("Mtgsig")
                                if (!mg.isNullOrBlank()) {
                                    val tid = h?.get("m-traceid") ?: h?.get("M-Traceid") ?: ""
                                    capturedLongCat.value = LongCatCaptured(mg, tid, request.url.toString())
                                }
                            }
                            return null
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                        }
                    }
                    val wv = this
                    CookieManager.getInstance().apply {
                        setAcceptCookie(true)
                        setAcceptThirdPartyCookies(wv, true)
                    }
                    loadUrl(url)
                }.also { webViewRef.value = it }
            },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            onRelease = { it.destroy() }
        )
    }
}
