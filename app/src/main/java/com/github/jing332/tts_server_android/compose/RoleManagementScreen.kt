package com.github.jing332.tts_server_android.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.drake.net.utils.withIO
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.nav.NavTopAppBar
import com.github.jing332.tts_server_android.compose.systts.list.expandSpeechRuleTagsIfNeeded
import com.github.jing332.tts_server_android.compose.systts.role.BackupCenterDialog
import com.github.jing332.tts_server_android.compose.systts.role.KeyManagerActivity
import com.github.jing332.tts_server_android.compose.systts.role.RoleListScreen
import com.github.jing332.tts_server_android.conf.SpeechRuleConfig
import com.github.jing332.tts_server_android.model.rhino.speech_rule.SpeechRuleEngine
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import kotlinx.coroutines.flow.conflate

/** 朗读规则 id（角色数据/标签池所属规则，与插件约定一致） */
private const val ROLE_RULE_ID = "mingwuyan"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleManagementScreen(sharedVM: SharedViewModel, pagerState: PagerState) {
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

    // 历史遗留的「仅界面模式」宿主配置项由 cleanupRoleHostConfigItems() 启动/恢复备份后清理。

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

    // 运行朗读规则后回到本页（ON_RESUME）时 reloadKey++ 重建内置列表，刷新角色标签
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
    // 密钥管理=独立全屏页面（09-14 由弹窗改页，不再需要开关状态）；备份恢复仍是弹窗
    var showBackupCenter by remember { mutableStateOf(false) }
    var backupVersion by remember { mutableIntStateOf(0) } // 恢复/导入等大动作后强制内置列表重读
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NavTopAppBar(
                title = { Text(stringResource(R.string.role_management)) },
                actions = {
                    // 密钥 / 备份（目目 09-14 三次定稿：emoji → 单色图标 + 文字。Key=现代钥匙、
                    // Backup=云+上箭头，均为 Material 官方语义字形，比旧 VpnKey/Save 贴切；
                    // 文字保留，避免图标并排时语义靠猜。紧凑动作，热区 ≥48dp 高；
                    // 原页内 48dp 按钮行已退役）
                    Box(
                        Modifier
                            .heightIn(min = 48.dp)
                            .clickable { KeyManagerActivity.start(context, ROLE_RULE_ID) }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.role_entry_key),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    Box(
                        Modifier
                            .heightIn(min = 48.dp)
                            .clickable { showBackupCenter = true }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Backup,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.role_entry_backup),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
                // 书籍入口仍在书籍卡（▾管理）
            )
        }
    ) { paddingValues ->
        // 内置原生列表（目目 09-15 晚拍板清死代码：插件宿主分支与开关常量已整删，
        // 回退走 git 历史；数据文件由朗读规则运行生成，不依赖插件安装）
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
    }

    // ===== 备份恢复弹窗（入口=顶栏图标；onRestored 走 reloadKey++ 让 RoleListScreen 重读文件）=====
    // 密钥管理已改独立页面（KeyManagerActivity），不在此渲染；恢复备份后角色列表重读
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
