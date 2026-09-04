package com.github.jing332.tts_server_android.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.drake.net.utils.withIO
import com.github.jing332.common.utils.toast
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.nav.NavTopAppBar
import com.github.jing332.tts_server_android.conf.SpeechRuleConfig
import com.github.jing332.tts_server_android.constant.SpeechTarget
import com.github.jing332.tts_server_android.model.rhino.speech_rule.SpeechRuleEngine
import com.github.jing332.tts_server_android.compose.systts.list.expandSpeechRuleTagsIfNeeded
import com.github.jing332.tts_server_android.compose.systts.list.ui.PluginTtsUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleManagementScreen(sharedVM: SharedViewModel, pagerState: PagerState) {
    // 角色管理插件：优先按固定 id 查找；兼容插件换 pluginId 后按名称回退
    val plugin = remember {
        dbm.pluginDao.getByPluginId("mingwuyan")
            ?: dbm.pluginDao.getAllWithoutCode().firstOrNull {
                it.name.contains("角色管理") || it.pluginId.contains("mingwuyan")
            }
    }
    val isRoleManagementPlugin = plugin != null
    // 插件当前实际 id（兼容换 id 后配置项已一键更新的场景）
    val rolePluginId = remember(plugin) { plugin?.pluginId ?: "mingwuyan" }

    // 仅在当前页时收集 Flow，避免后台页数据变化触发重组导致滑动卡顿
    val isPageVisible = remember { derivedStateOf { pagerState.currentPage == PagerDestination.Tool.index } }
    val flow = remember { dbm.systemTtsV2.flowAllGroupWithTts().conflate() }
    // 同步预取一次作为初始值，避免 flow 首帧空列表导致顶栏「仅界面模式」开关
    // 先显示为关、数据到达后再跳变为开的视觉闪烁。
    var cachedGroups by remember { mutableStateOf(dbm.systemTtsV2.getAllGroupWithTts()) }
    val groups by flow.collectAsStateWithLifecycle(
        initialValue = cachedGroups
    )
    // 页面不可见时冻结 groups，避免后台重组
    val effectiveGroups = if (isPageVisible.value) groups else cachedGroups
    // 页面可见时更新缓存
    LaunchedEffect(groups, isPageVisible.value) {
        if (isPageVisible.value) cachedGroups = groups
    }

    // 查找角色管理插件(mingwuyan)配置项：本栏专为外置角色管理 UI 而设，全局只应有一个。
    // 优先取已开启 isUiOnly 的；没有则取任意一个（用于顶栏开关目标）。
    // 用 minByOrNull(id) 保证 flow 重发时不漂移。
    val mingwuyanTts = remember(effectiveGroups, isRoleManagementPlugin, rolePluginId) {
        if (!isRoleManagementPlugin) null
        else effectiveGroups.flatMap { it.list }
            .filter { tts ->
                val config = tts.config
                config is TtsConfigurationDTO &&
                    ((config.source as? PluginTtsSource)?.pluginId == rolePluginId ||
                        // 兼容尚未一键更新、仍引用旧 id 的配置项
                        (config.source as? PluginTtsSource)?.pluginId == "mingwuyan")
            }
            .minByOrNull { it.id }
    }
    val uiOnlyTts = mingwuyanTts?.takeIf {
        ((it.config as? TtsConfigurationDTO)?.source as? PluginTtsSource)?.isUiOnly == true
    }

    // 本地编辑状态：仅在 uiOnly 配置项出现/消失/切换 id 时更新，
    // 不随 flow 抖动重置，避免用户在角色管理栏内编辑时 UI 重建丢失插件 UI
    var currentTts by remember { mutableStateOf<SystemTtsV2?>(null) }
    LaunchedEffect(uiOnlyTts?.id) {
        if (uiOnlyTts == null) {
            // 没有 uiOnly 配置项（被外部关闭或删除）：清空本地状态，UI 收起
            currentTts = null
        } else if (currentTts?.id != uiOnlyTts.id) {
            // 切换到不同的 uiOnly 配置项
            currentTts = uiOnlyTts
        }
        // 否则保持当前（同 id 不重置，避免覆盖用户本地编辑）
    }
    // 检测当前配置是否已被删除（在别处删除），若删除则清除本地状态
    LaunchedEffect(effectiveGroups) {
        val currentId = currentTts?.id
        if (currentId != null && effectiveGroups.flatMap { it.list }.none { it.id == currentId }) {
            currentTts = null
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 顶部「仅界面模式」开关目标 = 角色管理配置项；没有时开关禁用（不自动创建）
    val switchTarget = mingwuyanTts
    val isUiOnlyOn = switchTarget?.let { tts ->
        ((tts.config as? TtsConfigurationDTO)?.source as? PluginTtsSource)?.isUiOnly == true
    } ?: false

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NavTopAppBar(
                title = { Text(stringResource(R.string.role_management)) },
                actions = {
                    // 🎨 插件配色切换：仅角色管理插件安装后显示，读写插件数据目录 theme.json
                    // （/Download/chajian/<pluginId>/theme.json，与插件JS共享；色单来自文件，加色免改源码）
                    if (isRoleManagementPlugin && plugin != null) {
                        var showThemeDialog by remember { mutableStateOf(false) }
                        if (showThemeDialog) {
                            PluginThemeDialog(
                                pluginId = rolePluginId,
                                onDismiss = { showThemeDialog = false }
                            )
                        }
                        IconButton(
                            onClick = { showThemeDialog = true },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = "插件配色")
                        }
                    }
                    // 仅界面模式开关：仅角色管理插件(mingwuyan)安装后显示，无文字提示
                    if (isRoleManagementPlugin && plugin != null) {
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = isUiOnlyOn,
                            // 仅当存在 mingwuyan 配置项且当前未开启 isUiOnly 时可点（只管 ON，不管 OFF）
                            enabled = switchTarget != null && !isUiOnlyOn,
                            onCheckedChange = {
                                val tts = switchTarget ?: return@Switch
                                val src = (tts.config as? TtsConfigurationDTO)
                                    ?.source as? PluginTtsSource
                                if (src == null) {
                                    context.toast("当前配置项非插件类型，无法切换")
                                } else {
                                    val updated = tts.copy(
                                        config = (tts.config as TtsConfigurationDTO).copy(
                                            source = src.copy(isUiOnly = true)
                                        )
                                    )
                                    scope.launch(Dispatchers.IO + NonCancellable) {
                                        dbm.systemTtsV2.insert(updated)
                                    }
                                }
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        val activeTts = currentTts
        when {
            !isRoleManagementPlugin || plugin == null -> {
                // 插件未安装
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            stringResource(R.string.role_management_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            activeTts != null -> {
                // 显示角色管理配置项的编辑页面（不管 isUiOnly 状态，始终显示完整内容）
                var systts by remember(activeTts.id) { mutableStateOf(activeTts) }
                val latestSystts by rememberUpdatedState(systts)

                // 离开页面时保存，持久化变更；用结构化协程替代裸 Thread，保证写入时机可控
                DisposableEffect(activeTts.id) {
                    onDispose {
                        scope.launch(Dispatchers.IO + NonCancellable) {
                            dbm.systemTtsV2.insert(latestSystts)
                        }
                    }
                }

                // 自动刷新：启用配置项签名变化（增删/改名/改标签/换分组）时，
                // 后台自动运行朗读规则重新生成角色文件，确保打开角色列表时与前台配置一致。
                val enabledSig = remember(effectiveGroups) {
                    effectiveGroups.flatMap { it.list }
                        .filter { it.isEnabled }
                        .map { it.id to (it.config as? TtsConfigurationDTO)?.speechRule?.tagName to it.order }
                        .hashCode()
                }
                // lastSig 持久化到 SharedPreferences：签名与上次一致时跳过耗时的
                // JS eval + handleText，直接复用磁盘已生成的角色文件，避免每次进入都卡在加载遮罩。
                var lastSig by remember { mutableStateOf(SpeechRuleConfig.lastRoleSig.value) }
                // 签名匹配则初始即就绪，不显示加载遮罩；不匹配则需重新生成文件，先显示遮罩。
                var roleFilesReady by remember { mutableStateOf(lastSig == enabledSig) }
                LaunchedEffect(enabledSig, activeTts.id) {
                    if (lastSig != enabledSig) {
                        lastSig = enabledSig
                        SpeechRuleConfig.lastRoleSig.value = enabledSig
                        roleFilesReady = false
                        withIO {
                            runCatching {
                                val rule = dbm.speechRuleDao.getByRuleIdAll("mingwuyan")
                                if (rule != null) {
                                    val engine = SpeechRuleEngine(context, rule)
                                    engine.eval()
                                    val rules = dbm.systemTtsV2.getEnabledListForSort(SpeechTarget.TAG).map { systts ->
                                        val cfg = systts.config as TtsConfigurationDTO
                                        cfg.speechRule.apply {
                                            configId = systts.id
                                            voice = cfg.source.voice
                                            displayName = systts.displayName
                                        }
                                    }
                                    // 标签扩容：扫描所有配置项（不限启用），补齐超出基础数量的标签
                                    expandSpeechRuleTagsIfNeeded(rule, effectiveGroups.flatMap { it.list })
                                    engine.handleText(SpeechRuleConfig.textParam.value, rules)
                                }
                            }
                        }
                        roleFilesReady = true
                    } else {
                        // 签名匹配时仍检查标签扩容：用户可能只是切换到已有大量标签的分组，
                        // 签名没变但朗读规则 tags 可能还没扩容到足够数量
                        withIO {
                            runCatching {
                                val rule = dbm.speechRuleDao.getByRuleIdAll("mingwuyan")
                                if (rule != null) {
                                    expandSpeechRuleTagsIfNeeded(rule, effectiveGroups.flatMap { it.list })
                                }
                            }
                        }
                        roleFilesReady = true
                    }
                }

                // 运行朗读规则后回到本页（ON_RESUME）时重建插件 UI，刷新角色列表标签
                // 跳过首次 ON_RESUME（首次进入由 LaunchedEffect 初始加载，避免重复 load）
                var reloadKey by remember { mutableIntStateOf(0) }
                var firstResume by remember { mutableStateOf(true) }
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            if (firstResume) {
                                firstResume = false
                            } else {
                                reloadKey++
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                if (roleFilesReady && isPageVisible.value) {
                    val ui = remember { PluginTtsUI() }
                    // 底部避让改为内容内安全垫：仅顶部吃 Scaffold 边距，底部垫底栏高+8dp，
                    // 让长 UI 能滚进底栏上方（同系统TTS列表 contentPadding 思路，消除白条截断）
                    val bottomPad = paddingValues.calculateBottomPadding()
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(top = paddingValues.calculateTopPadding())
                            .verticalScroll(rememberScrollState())
                    ) {
                        ui.EditContentScreen(
                            modifier = Modifier.fillMaxWidth(),
                            systts = systts,
                            onSysttsChange = { systts = it },
                            showBasicInfo = false,
                            plugin = plugin,
                            showPluginSelector = false,
                            showUiOnlySwitch = false,
                            reloadKey = reloadKey,
                        )
                        Spacer(Modifier.height(bottomPad + 8.dp))
                    }
                } else if (!roleFilesReady) {
                    Box(
                        Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            else -> {
                // 插件已安装但未开启仅界面模式
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            stringResource(R.string.role_management_no_ui_only_config),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * 标签扩容函数已移至 [com.github.jing332.tts_server_android.compose.systts.list.expandSpeechRuleTagsIfNeeded]，
 * 供工具箱页与标签切换/批量标签弹窗共用。
 */

/**
 * 插件配色切换弹窗：读写插件数据目录 theme.json（/Download/chajian/<pluginId>/theme.json），
 * 与插件 JS 共享同一存储。色单（key+label 清单）来自文件内 "themes" 数组，
 * app 不感知具体颜色——插件侧加色只改文件，无需改 app 源码。
 * 文件结构: {"current":"green","themes":[{"key":"green","label":"勾选绿A"},...]}
 */
@Composable
private fun PluginThemeDialog(pluginId: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val file = remember(pluginId) { java.io.File("/storage/emulated/0/Download/chajian/$pluginId/theme.json") }

    // 读文件拿主题清单与当前值；文件缺失/异常时给内置兜底清单（与插件默认色表一致）
    var themeKey by remember { mutableStateOf("green") }
    var themes by remember {
        mutableStateOf(
            listOf("green" to "勾选绿A", "mint" to "勾选绿B", "leaf" to "勾选绿C",
                "gray" to "中性灰绿", "purple" to "紫韵")
        )
    }
    LaunchedEffect(pluginId) {
        withIO {
            runCatching {
                if (file.exists()) {
                    val obj = org.json.JSONObject(file.readText())
                    themeKey = obj.optString("current", "green")
                    val arr = obj.optJSONArray("themes")
                    if (arr != null && arr.length() > 0) {
                        val list = mutableListOf<Pair<String, String>>()
                        for (i in 0 until arr.length()) {
                            val o = arr.optJSONObject(i) ?: continue
                            val k = o.optString("key")
                            val l = o.optString("label")
                            if (k.isNotBlank()) list.add(k to l)
                        }
                        if (list.isNotEmpty()) themes = list
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("插件配色") },
        text = {
            Column {
                themes.forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = themeKey == key,
                            onClick = { themeKey = key }
                        )
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // 写回同一文件，保留 themes 清单只更新 current；插件 JS 下次进入页面读取生效
                scope.launch {
                    withIO {
                        runCatching {
                            val obj = if (file.exists())
                                org.json.JSONObject(file.readText())
                            else org.json.JSONObject()
                            obj.put("current", themeKey)
                            if (!obj.has("themes")) {
                                val arr = org.json.JSONArray()
                                themes.forEach { (k, l) ->
                                    arr.put(org.json.JSONObject().put("key", k).put("label", l))
                                }
                                obj.put("themes", arr)
                            }
                            file.writeText(obj.toString())
                        }.onSuccess {
                            withContext(Dispatchers.Main) { context.toast("已保存，重新进入角色管理页生效") }
                        }.onFailure {
                            withContext(Dispatchers.Main) { context.toast("保存失败: ${it.message}") }
                        }
                    }
                }
                onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
