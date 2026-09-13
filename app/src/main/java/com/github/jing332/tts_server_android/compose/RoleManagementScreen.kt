package com.github.jing332.tts_server_android.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.nav.NavTopAppBar
import com.github.jing332.tts_server_android.compose.systts.list.expandSpeechRuleTagsIfNeeded
import com.github.jing332.tts_server_android.compose.systts.list.ui.PluginTtsUI
import com.github.jing332.tts_server_android.compose.systts.role.BackupCenterDialog
import com.github.jing332.tts_server_android.compose.systts.role.KeyManagerDialog
import com.github.jing332.tts_server_android.compose.systts.role.RoleListScreen
import com.github.jing332.tts_server_android.conf.SpeechRuleConfig
import com.github.jing332.tts_server_android.model.rhino.speech_rule.SpeechRuleEngine
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import kotlinx.coroutines.flow.conflate

/** 朗读规则 id（角色数据/标签池所属规则，与插件约定一致） */
private const val ROLE_RULE_ID = "mingwuyan"

/**
 * 内置角色列表开关（目目 09-13 拍板「内置分阶段搬」）：true=页签渲染 app 原生 MD3 列表
 * （RoleListScreen，直读 chajian 文件，不依赖插件安装）；false=回退插件宿主旧路。
 * 插件 JS 保留不删——回退只需把此常量改 false 重新打包。
 * 第一阶段：列表+换声+改名/删除/设为主角/标记；合并、批量、密钥管理、书籍仍在插件（第二阶段搬）。
 */
private const val USE_NATIVE_ROLE_LIST = true

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
    // 插件当前实际 id（虚拟宿主用）
    val rolePluginId = remember(plugin) { plugin?.pluginId ?: "mingwuyan" }

    // 仅在当前页时收集 Flow，避免后台页数据变化触发重组导致滑动卡顿
    val isPageVisible = remember { derivedStateOf { pagerState.currentPage == PagerDestination.Tool.index } }
    val flow = remember { dbm.systemTtsV2.flowAllGroupWithTts().conflate() }
    // 同步预取一次作为初始值，避免 flow 首帧空列表导致签名错判触发多余重跑
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

    val context = LocalContext.current

    // 直连宿主：纯内存虚拟配置项，仅作为插件 UI 的挂载载体，绝不落库。
    // 角色管理数据全部在 chajian 本地文件（ttsrv.readTxtFile/writeTxtFile），
    // 历史上的 isUiOnly 宿主配置项由 cleanupRoleHostConfigItems() 启动/恢复后清理。
    val hostTts = remember(rolePluginId) {
        SystemTtsV2(
            displayName = "",
            isEnabled = false,
            config = TtsConfigurationDTO(
                source = PluginTtsSource(pluginId = rolePluginId, isUiOnly = true),
            ),
        )
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
    LaunchedEffect(enabledSig) {
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
                        // 口径必须与朗读链 TtsRepository.getAllTts() 完全一致：**全部启用项**
                        // （不限 target、含备用）。曾用 getEnabledListForSort(TAG)——即"仅自定义标签
                        // 且非备用"——规则会把 fayinren.json 整份覆写成该子集，发音人池当场塌房，
                        // 表现为「停止朗读/进本页后候选只剩几条」。顺序同样照朗读链：分组 order → 组内 order。
                        val rules = dbm.systemTtsV2.getAllGroupWithTts()
                            .flatMap { it.list.sortedBy { t -> t.order } }
                            .filter { it.isEnabled }
                            .mapNotNull { systts ->
                                val cfg = systts.config as? TtsConfigurationDTO
                                    ?: return@mapNotNull null
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

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    // 密钥/备份弹窗（目目 09-14：入口从页内按钮行上移顶栏，给角色区留空；弹窗状态由宿主持有）
    var showKeyManager by remember { mutableStateOf(false) }
    var showBackupCenter by remember { mutableStateOf(false) }
    var backupVersion by remember { mutableIntStateOf(0) } // 恢复/导入等大动作后强制内置列表重读
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NavTopAppBar(
                title = { Text(stringResource(R.string.role_management)) },
                actions = {
                    // 🔑 密钥管理 / 💾 备份恢复（原页内 48dp 按钮行已退役）
                    IconButton(onClick = { showKeyManager = true }) {
                        Icon(
                            Icons.Default.VpnKey,
                            contentDescription = stringResource(R.string.role_key_title)
                        )
                    }
                    IconButton(onClick = { showBackupCenter = true }) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = stringResource(R.string.backup_title)
                        )
                    }
                }
                // 书籍入口仍在书籍卡（▾管理）
            )
        }
    ) { paddingValues ->
        if (USE_NATIVE_ROLE_LIST) {
            // 内置原生列表：不依赖插件安装（数据文件由朗读规则运行生成），
            // 自动刷新签名逻辑与插件路径共用（上方 LaunchedEffect(enabledSig)）
            if (roleFilesReady && isPageVisible.value) {
                RoleListScreen(
                    tagRuleId = ROLE_RULE_ID,
                    reloadKey = reloadKey,
                    bottomPadding = paddingValues.calculateBottomPadding(),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = paddingValues.calculateTopPadding()),
                )
            } else {
                Box(
                    Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else when {
            plugin == null -> {
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
            roleFilesReady && isPageVisible.value -> {
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
                        // 虚拟宿主不落库：onSysttsChange 置空，插件数据全在 chajian 本地文件
                        systts = hostTts,
                        onSysttsChange = { },
                        showBasicInfo = false,
                        plugin = plugin,
                        showPluginSelector = false,
                        showUiOnlySwitch = false,
                        reloadKey = reloadKey,
                    )
                    Spacer(Modifier.height(bottomPad + 8.dp))
                }
            }
            else -> {
                Box(
                    Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // ===== 密钥管理 / 备份恢复弹窗（入口=顶栏图标；onRestored 走 reloadKey++ 让 RoleListScreen 重读文件）=====
    if (showKeyManager) {
        KeyManagerDialog(tagRuleId = ROLE_RULE_ID, onDismiss = { showKeyManager = false })
    }
    if (showBackupCenter) {
        BackupCenterDialog(
            tagRuleId = ROLE_RULE_ID,
            version = backupVersion,
            onDismiss = { showBackupCenter = false },
            onRestored = { backupVersion++; reloadKey++ },
        )
    }
}

/**
 * 清理历史"仅界面模式"角色管理宿主配置项。
 *
 * 直连改造后角色管理栏直接挂载插件 UI，宿主配置项只是历史遗留——其 source.data
 * 从不作为数据源（插件数据全在 chajian 本地文件），删除零损失。
 * App 启动与恢复备份后各调用一次；恢复旧备份可能带回宿主项，靠第二处兜底。
 */
fun cleanupRoleHostConfigItems() {
    val hosts = dbm.systemTtsV2.all.filter { item ->
        ((item.config as? TtsConfigurationDTO)?.source as? PluginTtsSource)?.isUiOnly == true
    }
    if (hosts.isEmpty()) return
    dbm.systemTtsV2.delete(*hosts.toTypedArray())
    SystemTtsService.notifyUpdateConfig()
}
