package com.github.jing332.tts_server_android.compose.systts.role

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.drake.net.utils.withIO
import org.json.JSONArray
import org.json.JSONObject
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.nav.NavTopAppBar
import com.github.jing332.tts_server_android.service.systts.help.CharacterRecordsFile
import com.github.jing332.tts_server_android.service.systts.help.KeyListFile
import kotlinx.coroutines.launch

/**
 * 密钥管理 + 备份恢复 + 书籍管理·1:1 复刻（对照 角色管理v10_主题密钥增强.js）：
 * 密钥页：按接口分组（未分组/直连密钥）+ 组折叠 + 当前密钥 ✓（反向匹配 miyue 内容，
 * 无匹配自动启用第一个）+ 测试结果记忆 ✓通/✗不通 + 新增（名称留空自动生成、重名覆盖确认、
 * 保存即启用）+ 改名 + 导出/导入（密钥导出_日期.json）+ 接口表单（新建/编辑/级联删除）+
 * 拉取模型（五类分组/搜索过滤/默认不勾选/全选只作用可见项/手动添加模型）。
 * 备份恢复：导出当前书籍到剪贴板/从剪贴板导入/备份全部文件/完整还原/自动备份开关。
 * 书籍：点击切换 · 点✕删除（当前书删后切默认）· 新增（建档并切换）· 多选删除 · 修改书名。
 *
 * 密钥页外观（目目 09-14「减噪 + 分层 + 全屏」，非插件原貌）：
 * **承载 = 独立全屏页面**（KeyManagerActivity，原 Dialog 左右各留 24dp、内容区仅约 272dp 太窄）；
 * 条目**无方框无底色**靠浅分隔线分区，缩进 20dp 与组名左缘对齐；当前密钥 = 行首 3dp 主色竖条 + 名称主色加粗；
 * 组级/条目级操作图标统一扁平灰（onSurfaceVariant、18dp、36dp 热区，无常驻描边）；
 * 层级 = 组头「淡底分组条 surfaceContainerHighest + 22dp 折叠箭头（展开↓/折叠→ 旋转动画）+ 组名 14sp」
 * > 条目 14sp 深色 > 接口地址 11sp 灰。
 */

/** 分组后的密钥组（照插件 buildKeyGroups：接口组 + 未分组 + 直连密钥） */
private class KeyGroup(
    val title: String,
    val entries: List<KeyListFile.KeyEntry>,
    val ifc: KeyListFile.ApiInterface? = null,
)

private fun buildKeyGroups(keys: List<KeyListFile.KeyEntry>, ifaces: List<KeyListFile.ApiInterface>): List<KeyGroup> {
    val groups = mutableListOf<KeyGroup>()
    val assigned = mutableSetOf<String>()
    ifaces.forEach { ifc ->
        val entries = keys.filter { KeyListFile.keyBelongsTo(it, ifc) }
        if (entries.isNotEmpty()) {
            groups.add(KeyGroup(ifc.name, entries, ifc))
            entries.forEach { assigned.add(it.name) }
        }
    }
    val direct = keys.filter { k ->
        val p = KeyListFile.parseKeyValue(k.value)
        p != null && p.isDirect && k.name !in assigned
    }
    val ungrouped = keys.filter { it.name !in assigned && it !in direct }
    if (ungrouped.isNotEmpty()) groups.add(KeyGroup("未分组", ungrouped))
    if (direct.isNotEmpty()) groups.add(KeyGroup("直连密钥", direct))
    return groups
}

/** 小圆角描边 chip（照插件 createSmallButton：透明底 + 彩色描边 + 彩色文字） */
@Composable
private fun SmallChipButton(text: String, color: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, color),
        modifier = Modifier
            .padding(start = 5.dp)
            .heightIn(min = 30.dp)
            .clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

/**
 * 扁平图标动作（目目 09-14 定案「图标要低调」）：**无描边、无底色**，统一 18dp、
 * `onSurfaceVariant` 灰（危险动作也默认同灰，进入删除模式才随组头转红）；
 * 热区仍 36dp 不缩水，按下反馈靠涟漪。
 */
@Composable
private fun FlatIconAction(
    icon: ImageVector,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(18.dp))
    }
}

/** 等宽描边按钮（照插件 createBottomBtn：白底 + 1dp 描边 + 10dp 圆角 + 居中彩色文字） */
@Composable
private fun WideOutlineButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.heightIn(min = 46.dp).clickable(onClick = onClick)
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 46.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 密钥条目行（目目 09-14 改版）：**无方框、无底色**，靠上一行的浅分隔线分区；
 * 当前密钥 = 行首 3dp 主色竖条 + 名称主色加粗（不再整块染色+描边）；
 * 操作图标扁平灰（⚡ 测试 / ✏️ 编辑），热区 36dp。
 */
@Composable
private fun KeyEntryRow(
    entry: KeyListFile.KeyEntry,
    isCurrent: Boolean,
    dotColor: Color,
    testOk: Boolean?,
    testing: Boolean,
    deleteMode: Boolean,
    checked: Boolean,
    onToggleCheck: () -> Unit,
    onSwitch: () -> Unit,
    onTest: () -> Unit,
    onEdit: () -> Unit,
) {
    Row(
        // 条目缩进 20dp（+行首 16dp 标记位 = 36dp），与组头组名 34dp 左缘基本对齐 → 从属关系一眼可见
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 6.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (deleteMode) {
            Checkbox(checked = checked, onCheckedChange = { onToggleCheck() })
        }
        // 行首标记位固定 16dp：当前=主色竖条 / 其余=彩色圆点 → 名字左缘始终对齐
        Box(Modifier.width(16.dp), contentAlignment = Alignment.CenterStart) {
            if (isCurrent) {
                Box(
                    Modifier
                        .width(3.dp)
                        .height(18.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                )
            } else {
                Box(Modifier.size(7.dp).background(dotColor, CircleShape))
            }
        }
        Text(
            entry.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isCurrent) FontWeight.SemiBold else null,
            color = if (isCurrent) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).clickable { onSwitch() }
        )
        testOk?.let { ok ->
            Text(
                stringResource(
                    if (ok) R.string.role_key_test_ok_short else R.string.role_key_test_fail_short
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (ok) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.width(2.dp))
        }
        if (testing) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(2.dp))
        }
        if (!deleteMode) {
            FlatIconAction(Icons.Default.Bolt, stringResource(R.string.role_key_test)) { onTest() }
            FlatIconAction(Icons.Default.Edit, stringResource(R.string.role_key_edit)) { onEdit() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyManagerScreen(tagRuleId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var version by remember { mutableIntStateOf(0) }
    var keys by remember { mutableStateOf<List<KeyListFile.KeyEntry>>(emptyList()) }
    var ifaces by remember { mutableStateOf<List<KeyListFile.ApiInterface>>(emptyList()) }
    var currentRaw by remember { mutableStateOf("") }
    // 测试结果记忆（条目名 → 通/不通），渲染时名称旁 ✓通/✗不通
    var testResults by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    // 组折叠状态
    var collapsed by remember { mutableStateOf<Set<String>>(emptySet()) }
    // 测试中（单条 / 整组批量）
    var testingName by remember { mutableStateOf<String?>(null) }
    var testingGroup by remember { mutableStateOf<String?>(null) }
    // 删除选择模式（照插件 deleteMode：组头变红字 + 全选/取消/删除(N)，条目行前勾选框）
    var deleteModeGroup by remember { mutableStateOf<String?>(null) }
    var deleteChecked by remember { mutableStateOf<Set<String>>(emptySet()) }
    var deleteConfirmGroup by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(version) {
        val loaded = withIO {
            Triple(
                KeyListFile.readKeys(tagRuleId),
                KeyListFile.readInterfaces(tagRuleId),
                KeyListFile.readCurrentRaw(tagRuleId).trim()
            )
        }
        keys = loaded.first
        ifaces = loaded.second
        var cur = loaded.third
        // 照插件：当前本地密钥在列表中无匹配 → 自动启用第一个
        if (keys.isNotEmpty() && keys.none { it.value.trim() == cur }) {
            val first = keys.first()
            if (first.value.isNotBlank()) {
                withIO { KeyListFile.saveCurrentRaw(tagRuleId, first.value) }
                cur = first.value.trim()
            }
        }
        currentRaw = cur
    }

    fun toast(resId: Int, vararg args: Any) {
        android.widget.Toast.makeText(
            context, context.getString(resId, *args), android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    fun save(list: List<KeyListFile.KeyEntry>) {
        scope.launch {
            val ok = withIO { KeyListFile.saveKeys(tagRuleId, list) }
            toast(if (ok) R.string.role_key_saved else R.string.role_list_failed)
            if (ok) version++
        }
    }
    fun switchTo(entry: KeyListFile.KeyEntry) {
        if (entry.value.trim() == currentRaw) {
            toast(R.string.role_key_is_current, entry.name)
            return
        }
        if (entry.value.isBlank()) {
            toast(R.string.role_key_value_empty)
            return
        }
        scope.launch {
            withIO { KeyListFile.saveCurrentRaw(tagRuleId, entry.value) }
            toast(R.string.role_key_switched, entry.name)
            version++
        }
    }
    fun testKey(entry: KeyListFile.KeyEntry) {
        val parsed = KeyListFile.parseKeyValue(entry.value)
        if (parsed == null || parsed.isDirect) {
            toast(R.string.role_key_test_direct)
            return
        }
        scope.launch {
            testingName = entry.name
            val r = withIO { KeyListFile.testKey(parsed.url, parsed.key, parsed.model) }
            testingName = null
            testResults = testResults + (entry.name to r.first)
            toast(
                if (r.first) R.string.role_key_test_ok_toast else R.string.role_key_test_fail_toast,
                entry.name, r.second
            )
        }
    }
    // 整组测试（照插件组头 ⚡：逐条测完逐条标记）
    fun testGroup(grp: KeyGroup) {
        val targets = grp.entries.filter {
            val p = KeyListFile.parseKeyValue(it.value)
            p != null && !p.isDirect && it.value.isNotBlank()
        }
        if (targets.isEmpty()) {
            toast(R.string.role_key_test_direct)
            return
        }
        scope.launch {
            testingGroup = grp.title
            toast(R.string.role_key_test_batch_start, grp.title, targets.size)
            var okCount = 0
            targets.forEach { e ->
                val p = KeyListFile.parseKeyValue(e.value) ?: return@forEach
                val r = withIO { KeyListFile.testKey(p.url, p.key, p.model) }
                testResults = testResults + (e.name to r.first)
                if (r.first) okCount++
            }
            testingGroup = null
            toast(R.string.role_key_test_batch_done, grp.title, okCount, targets.size - okCount)
        }
    }
    // 批量删除（照插件 deleteMultipleBooks：删当前密钥时自动切到剩余第一条）
    fun deleteNames(names: List<String>) {
        if (names.isEmpty()) return
        val nameSet = names.toSet()
        val remaining = keys.filter { it.name !in nameSet }
        val curWasRemoved = keys.any { it.name in nameSet && it.value.trim() == currentRaw }
        val next = remaining.firstOrNull()
        scope.launch {
            withIO { KeyListFile.saveKeys(tagRuleId, remaining) }
            if (curWasRemoved) {
                withIO {
                    KeyListFile.saveCurrentRaw(
                        tagRuleId,
                        next?.value?.takeIf { it.isNotBlank() }.orEmpty()
                    )
                }
            }
            toast(R.string.role_key_deleted_toast, names.size)
            version++
        }
    }

    // 弹窗状态
    var showAdd by remember { mutableStateOf(false) }
    var renameFor by remember { mutableStateOf<KeyListFile.KeyEntry?>(null) }
    var overwriteFor by remember { mutableStateOf<Pair<String, String>?>(null) } // (新名, 值) 覆盖确认
    var deleteFor by remember { mutableStateOf<KeyListFile.KeyEntry?>(null) }
    var ifcFormFor by remember { mutableStateOf<KeyListFile.ApiInterface?>(null) } // null+showIfcNew=true=新建
    var showIfcNew by remember { mutableStateOf(false) }
    var showPullModels by remember { mutableStateOf(false) }
    var pullForIfc by remember { mutableStateOf<String?>(null) } // 组头 🔍 预选接口
    var showImport by remember { mutableStateOf(false) }

    // 目目 09-14：由「窄弹窗」改为独立全屏页面（照替换管理/插件管理/LibrariesActivity 模式），
    // 内容区 272dp → 328dp，返回键自然退页；导入/导出上顶栏，不再占标题行
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NavTopAppBar(
                title = { Text(stringResource(R.string.role_key_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                },
                // 导入/导出改图标（目目 09-14：文字按钮各占约 48dp 宽，把「密钥管理」标题挤窄；
                // 原 📥/📤 语义 → FileDownload / FileUpload，热区 48dp 与左侧返回键同规格）
                actions = {
                    IconButton(onClick = { showImport = true }) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = stringResource(R.string.role_key_import)
                        )
                    }
                    IconButton(onClick = {
                        scope.launch {
                            val name = withIO { KeyListFile.exportKeys(tagRuleId, keys) }
                            if (name != null) toast(R.string.role_key_exported, keys.size, name)
                            else toast(R.string.role_list_failed)
                        }
                    }) {
                        Icon(
                            Icons.Default.FileUpload,
                            contentDescription = stringResource(R.string.role_key_export)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
                // 操作行：＋ 新增密钥 / 🔍 拉取模型（照插件等宽并排，放列表上方）
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WideOutlineButton(
                        "＋ " + stringResource(R.string.role_key_add),
                        MaterialTheme.colorScheme.primary,
                        Modifier.weight(1f)
                    ) { showAdd = true }
                    WideOutlineButton(
                        "🔍 " + stringResource(R.string.role_key_fetch),
                        Color(0xFF6A1B9A),
                        Modifier.weight(1f)
                    ) { showPullModels = true }
                }
                if (keys.isEmpty()) {
                    Text(
                        stringResource(R.string.role_key_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                    )
                } else {
                    val groups = buildKeyGroups(keys, ifaces)
                    groups.forEach { grp ->
                        val isCollapsed = grp.title in collapsed
                        val isDeleting = deleteModeGroup == grp.title
                        val grpHasCurrent = currentRaw.isNotEmpty() &&
                                grp.entries.any { it.value.trim() == currentRaw }
                        val selCount = grp.entries.count { it.name in deleteChecked }
                        // 组间距拉开：组头靠上方留白与前一组区隔，组内条目紧凑
                        Column(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp)) {
                            // 组头行：照主界面 GroupItem 口径 —— 淡底「分组条」（surfaceContainerHighest
                            // + 8dp 圆角 + 内边距），与前一组靠上方留白区隔；删除选择模式为瞬时态，不加底色
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isDeleting) Color.Transparent
                                        else MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isDeleting) {
                                    Text(
                                        stringResource(R.string.role_key_delete_select_title, selCount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    SmallChipButton(stringResource(R.string.select_all), Color(0xFF757575)) {
                                        val allSel = grp.entries.all { it.name in deleteChecked }
                                        val names = grp.entries.map { it.name }.toSet()
                                        deleteChecked = if (allSel) deleteChecked - names
                                        else deleteChecked + names
                                    }
                                    SmallChipButton(stringResource(R.string.cancel), Color(0xFF757575)) {
                                        deleteModeGroup = null
                                        deleteChecked = emptySet()
                                    }
                                    SmallChipButton(
                                        stringResource(R.string.role_key_delete_n, selCount),
                                        MaterialTheme.colorScheme.error
                                    ) {
                                        if (selCount == 0) toast(R.string.role_key_delete_none)
                                        else deleteConfirmGroup = grp.title
                                    }
                                } else {
                                    // 组头可点区（目目 09-14 照主界面口径改版）：大号折叠箭头 +
                                    // 组名 14sp，替换原「10dp 文本三角 + 12sp 灰字」的弱形态；
                                    // 层次靠「组头淡底条 + 字号」双重区分，不再只靠字号
                                    Row(
                                        Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                collapsed = if (isCollapsed) collapsed - grp.title
                                                else collapsed + grp.title
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 展开朝下 0° / 折叠朝右 -90°（同主界面 GroupItem 的旋转动画）
                                        val arrowAngle by animateFloatAsState(
                                            targetValue = if (isCollapsed) -90f else 0f, label = ""
                                        )
                                        Icon(
                                            Icons.Default.ExpandMore,
                                            contentDescription = stringResource(
                                                if (isCollapsed) R.string.desc_expand_group
                                                else R.string.desc_collapse_group, grp.title
                                            ),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp).rotate(arrowAngle)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            grp.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                grp.entries.size.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                        if (grpHasCurrent) {
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "✓",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    // 组级图标：扁平灰（编辑接口 / 拉取模型 / 整组测试）
                                    // 仅接口组有这三个动作——未分组/直连组点了也只是白弹提示
                                    grp.ifc?.let { ifc ->
                                        FlatIconAction(
                                            Icons.Default.Edit,
                                            stringResource(R.string.role_key_interface_edit)
                                        ) { ifcFormFor = ifc }
                                        FlatIconAction(
                                            Icons.Default.Search,
                                            stringResource(R.string.role_key_fetch)
                                        ) {
                                            pullForIfc = ifc.name
                                            showPullModels = true
                                        }
                                        if (testingGroup == grp.title) {
                                            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(
                                                    Modifier.size(18.dp), strokeWidth = 2.dp
                                                )
                                            }
                                        } else {
                                            FlatIconAction(
                                                Icons.Default.Bolt,
                                                stringResource(R.string.role_key_test)
                                            ) { testGroup(grp) }
                                        }
                                    }
                                    FlatIconAction(
                                        Icons.Default.DeleteOutline,
                                        stringResource(R.string.delete)
                                    ) {
                                        deleteModeGroup = grp.title
                                        deleteChecked = emptySet()
                                        collapsed = collapsed - grp.title
                                    }
                                }
                            }
                            // 第二行：接口地址 + Key 尾4（最淡一级灰字，缩进对齐组名）
                            if (!isDeleting) {
                                grp.ifc?.let { ifc ->
                                    Text(
                                        ifc.baseUrl + "  *尾" + ifc.apiKey.takeLast(4),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(start = 34.dp, end = 6.dp, bottom = 2.dp)
                                    )
                                }
                            }
                        }
                        if (!isCollapsed) {
                            grp.entries.forEachIndexed { idx, entry ->
                                val isCurrent = currentRaw.isNotEmpty() &&
                                        entry.value.trim() == currentRaw
                                // 条目去方框后靠浅分隔线分区；首条不加，避免紧贴组头地址行
                                if (idx > 0) {
                                    HorizontalDivider(
                                        thickness = 0.6.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                    )
                                }
                                KeyEntryRow(
                                    entry = entry,
                                    isCurrent = isCurrent,
                                    dotColor = BOOK_DOT_COLORS[idx % BOOK_DOT_COLORS.size],
                                    testOk = testResults[entry.name],
                                    testing = testingName == entry.name,
                                    deleteMode = isDeleting,
                                    checked = entry.name in deleteChecked,
                                    onToggleCheck = {
                                        deleteChecked = if (entry.name in deleteChecked)
                                            deleteChecked - entry.name
                                        else deleteChecked + entry.name
                                    },
                                    onSwitch = { switchTo(entry) },
                                    onTest = { testKey(entry) },
                                    onEdit = { renameFor = entry }
                                )
                            }
                        }
                    }
                }
            }
        }

    // 批量删除确认（照插件：组内选中 N 条 → 二次确认，不可恢复）
    deleteConfirmGroup?.let { gTitle ->
        val grp = buildKeyGroups(keys, ifaces).firstOrNull { it.title == gTitle }
        val targets = grp?.entries?.filter { it.name in deleteChecked }?.map { it.name }.orEmpty()
        AlertDialog(
            onDismissRequest = { deleteConfirmGroup = null },
            title = { Text(stringResource(R.string.role_key_delete_title)) },
            text = { Text(stringResource(R.string.role_key_delete_batch_text, gTitle, targets.size)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteConfirmGroup = null
                    deleteModeGroup = null
                    deleteChecked = emptySet()
                    deleteNames(targets)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmGroup = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 新增（名称留空自动生成；重名→覆盖确认；保存即启用为当前，照插件 showAddKeyDialog）
    if (showAdd) {
        KeyEditDialog(
            initial = null,
            existingNames = keys.map { it.name }.toSet(),
            onDismiss = { showAdd = false },
            onConfirm = { name, value, overwrite ->
                if (overwrite) {
                    showAdd = false
                    overwriteFor = name to value
                } else {
                    showAdd = false
                    save(keys + KeyListFile.KeyEntry(name = name, keyCode = KeyListFile.nextKeyCode(keys), value = value))
                    scope.launch { withIO { KeyListFile.saveCurrentRaw(tagRuleId, value) }; version++ }
                    toast(R.string.role_key_add_first, name)
                }
            }
        )
    }
    // 改名（重名→覆盖确认，照插件 showKeyNameDialog）
    renameFor?.let { entry ->
        KeyEditDialog(
            initial = entry,
            existingNames = keys.map { it.name }.toSet(),
            onDismiss = { renameFor = null },
            onDelete = { renameFor = null; deleteFor = entry },
            onConfirm = { name, value, overwrite ->
                renameFor = null
                if (overwrite && name != entry.name) {
                    overwriteFor = name to value
                } else {
                    save(keys.map { if (it.name == entry.name) it.copy(name = name, value = value) else it })
                }
            }
        )
    }
    // 覆盖确认（照插件「覆盖确认」对话框：保留原 keyCode，换 value）
    overwriteFor?.let { (name, value) ->
        AlertDialog(
            onDismissRequest = { overwriteFor = null },
            title = { Text(stringResource(R.string.role_key_overwrite_title)) },
            text = { Text(stringResource(R.string.role_key_overwrite, name)) },
            confirmButton = {
                TextButton(onClick = {
                    overwriteFor = null
                    val old = keys.firstOrNull { it.name == name }
                    save(keys.map { if (it.name == name) it.copy(value = value) else it })
                    scope.launch { withIO { KeyListFile.saveCurrentRaw(tagRuleId, value) }; version++ }
                    if (old != null) toast(R.string.role_key_saved)
                }) { Text(stringResource(R.string.role_key_overwrite_btn)) }
            },
            dismissButton = {
                TextButton(onClick = { overwriteFor = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    // 删除密钥
    deleteFor?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteFor = null },
            title = { Text(stringResource(R.string.role_key_delete_title)) },
            text = { Text(stringResource(R.string.role_key_delete_text, entry.name)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteFor = null
                    save(keys.filter { it.name != entry.name })
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteFor = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    // 接口表单（新建/编辑/级联删除，照插件 showInterfaceFormDialog）
    if (showIfcNew) {
        InterfaceFormDialog(
            tagRuleId = tagRuleId,
            initial = null,
            onDismiss = { showIfcNew = false },
            onSaved = { showIfcNew = false; version++ },
        )
    }
    ifcFormFor?.let { ifc ->
        InterfaceFormDialog(
            tagRuleId = tagRuleId,
            initial = ifc,
            onDismiss = { ifcFormFor = null },
            onSaved = { ifcFormFor = null; version++ },
        )
    }
    // 拉取模型（接口单选 → 拉取 → 分类勾选入库，照插件 showModelSelectDialog）
    if (showPullModels) {
        ModelPullDialog(
            tagRuleId = tagRuleId,
            existingNames = keys.map { it.name }.toSet(),
            initialIfcName = pullForIfc,
            onDismiss = { showPullModels = false; pullForIfc = null },
            onConfirm = { entries ->
                showPullModels = false
                val merged = entries.fold(keys) { acc, e ->
                    acc + e.copy(keyCode = KeyListFile.nextKeyCode(acc))
                }
                save(merged)
            }
        )
    }
    // 导入（选择密钥导出_*.json，照插件 importKeysDialog）
    if (showImport) {
        ImportKeysDialog(
            tagRuleId = tagRuleId,
            onDismiss = { showImport = false },
            onDone = { showImport = false; version++ },
        )
    }
}

/** 密钥编辑（新增/改名共用）：名称（留空自动生成）+ 值；返回 overwrite=重名待覆盖 */
@Composable
private fun KeyEditDialog(
    initial: KeyListFile.KeyEntry?,
    existingNames: Set<String>,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onConfirm: (String, String, Boolean) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var value by remember { mutableStateOf(initial?.value.orEmpty()) }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (initial == null) R.string.role_key_add else R.string.role_key_rename))
        },
        text = {
            Column {
                Text(
                    stringResource(R.string.role_key_name_auto),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.role_key_name)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.role_key_value),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = { Text(stringResource(R.string.role_key_value_hint)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
                // 照插件条目 ✏️ 弹窗：删除入口留在编辑弹窗内
                if (onDelete != null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Row {
                // 照插件密钥详情弹窗的「复制」键：把当前密钥内容一键送剪贴板
                TextButton(
                    enabled = value.isNotBlank(),
                    onClick = {
                        clipboard.setText(AnnotatedString(value.trim()))
                        android.widget.Toast.makeText(
                            context, context.getString(R.string.copied),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                ) { Text(stringResource(R.string.copy)) }
                TextButton(
                    enabled = value.isNotBlank(),
                    onClick = {
                        val finalName = name.trim().ifEmpty {
                            // 留空自动生成（照插件 defaultName=模型或 key01）
                            KeyListFile.parseKeyValue(value.trim())?.let { p ->
                                if (!p.isDirect && p.model.isNotEmpty()) p.model else "key" + (existingNames.size + 1)
                            } ?: "key" + (existingNames.size + 1)
                        }
                        onConfirm(finalName, value.trim(), finalName in existingNames && finalName != initial?.name)
                    }
                ) { Text(stringResource(R.string.confirm)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/** 接口表单（照插件 showInterfaceFormDialog：名称/地址/Key + 校验 + 🗑级联删除） */
@Composable
private fun InterfaceFormDialog(
    tagRuleId: String,
    initial: KeyListFile.ApiInterface?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var url by remember { mutableStateOf(initial?.baseUrl.orEmpty()) }
    var key by remember { mutableStateOf(initial?.apiKey.orEmpty()) }
    fun toast(resId: Int, vararg args: Any) {
        android.widget.Toast.makeText(context, context.getString(resId, *args), android.widget.Toast.LENGTH_SHORT).show()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initial == null) R.string.role_key_interface_new else R.string.role_key_interface_edit
                )
            )
        },
        text = {
            Column {
                Text(
                    stringResource(R.string.role_key_ifc_name),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    singleLine = true, textStyle = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.role_key_ifc_url),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = url, onValueChange = { url = it },
                    singleLine = true, textStyle = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.role_key_ifc_key),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = key, onValueChange = { key = it },
                    singleLine = true, textStyle = MaterialTheme.typography.bodyMedium,
                )
                if (initial != null) {
                    TextButton(onClick = {
                        scope.launch {
                            val (kept, removed) = withIO {
                                KeyListFile.deleteInterfaceCascade(tagRuleId, initial, KeyListFile.readKeys(tagRuleId))
                            }
                            withIO { KeyListFile.saveKeys(tagRuleId, kept) }
                            toast(R.string.role_key_ifc_deleted, removed)
                            onSaved()
                        }
                    }) {
                        Text(
                            stringResource(R.string.role_key_ifc_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = true,
                onClick = {
                    val n = name.trim()
                    val u = KeyListFile.normalizeBaseUrl(url.trim())
                    val k = key.trim()
                    if (n.isEmpty()) { toast(R.string.role_key_ifc_name_empty); return@TextButton }
                    if (!u.startsWith("http")) { toast(R.string.role_key_ifc_url_bad); return@TextButton }
                    if (k.isEmpty()) { toast(R.string.role_key_ifc_key_empty); return@TextButton }
                    scope.launch {
                        val ifaces = withIO { KeyListFile.readInterfaces(tagRuleId) }
                        if (initial == null && ifaces.any { it.name == n }) {
                            toast(R.string.role_key_ifc_name_dup)
                            return@launch
                        }
                        val updated = if (initial == null) {
                            ifaces + KeyListFile.ApiInterface(n, u, k, emptyList())
                        } else {
                            ifaces.map { if (it.name == initial.name) KeyListFile.ApiInterface(n, u, k, it.models) else it }
                        }
                        withIO { KeyListFile.saveInterfaces(tagRuleId, updated) }
                        toast(R.string.role_key_saved)
                        onSaved()
                    }
                }
            ) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * 拉取模型（照插件 showModelSelectDialog）：
 * 接口单选 → 拉取 → 五类分组+搜索过滤+默认不勾选（全选只作用可见项）→ 手动添加模型 → 入库。
 */
@Composable
private fun ModelPullDialog(
    tagRuleId: String,
    existingNames: Set<String>,
    initialIfcName: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (List<KeyListFile.KeyEntry>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var ifaces by remember { mutableStateOf<List<KeyListFile.ApiInterface>>(emptyList()) }
    var pickedName by remember { mutableStateOf("") }
    var models by remember { mutableStateOf<List<String>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("") }
    var manualVisible by remember { mutableStateOf(false) }
    var ifcNewVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val list = withIO { KeyListFile.readInterfaces(tagRuleId) }
        ifaces = list
        pickedName = initialIfcName?.takeIf { n -> list.any { it.name == n } }
            ?: list.firstOrNull()?.name.orEmpty()
    }
    fun fetch() {
        val p = ifaces.firstOrNull { it.name == pickedName } ?: return
        scope.launch {
            loading = true; error = ""
            val r = withIO { KeyListFile.fetchModels(p.baseUrl, p.apiKey) }
            loading = false
            if (r.first == null) error = r.second
            else { models = r.first ?: emptyList(); selected = emptySet() } // 默认不勾选
        }
    }
    val visibleModels = models.filter { filter.isBlank() || it.contains(filter, true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.role_key_fetch),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                // 接口单选
                ifaces.forEach { ifc ->
                    Row(
                        Modifier.fillMaxWidth().clickable { pickedName = ifc.name },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = pickedName == ifc.name, onClick = { pickedName = ifc.name })
                        Text(ifc.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                // 新建接口（照插件：接口挂在拉取流程上，主弹窗只留两个按钮）
                TextButton(onClick = { ifcNewVisible = true }) {
                    Text("＋ " + stringResource(R.string.role_key_interface_new))
                }
                // 操作行：拉取 / 手动添加
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { fetch() }, enabled = !loading && pickedName.isNotEmpty()) {
                        Text(stringResource(if (loading) R.string.role_key_fetching else R.string.role_key_fetch))
                    }
                    if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { manualVisible = true }) {
                        Text(stringResource(R.string.role_key_manual_model))
                    }
                }
                if (error.isNotEmpty()) {
                    Text(
                        stringResource(R.string.role_key_fetch_fail, error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                // 搜索过滤 + 全选（只作用可见项，照插件）
                if (models.isNotEmpty()) {
                    OutlinedTextField(
                        value = filter,
                        onValueChange = { filter = it },
                        placeholder = { Text(stringResource(R.string.role_key_model_filter)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(onClick = {
                        selected = if (visibleModels.all { it in selected }) {
                            selected - visibleModels.toSet()
                        } else {
                            selected + visibleModels.toSet()
                        }
                    }) {
                        Text(
                            if (visibleModels.all { it in selected }) stringResource(R.string.role_select_all_cancel)
                            else stringResource(R.string.role_list_select_all)
                        )
                    }
                }
                // 五类分组列表
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    val byCat = linkedMapOf<String, MutableList<String>>()
                    visibleModels.sorted().forEach { m ->
                        byCat.getOrPut(KeyListFile.classifyModel(m)) { mutableListOf() }.add(m)
                    }
                    byCat.forEach { (cat, list) ->
                        item(key = "cat_$cat") {
                            Text(
                                "$cat (${list.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                        }
                        list.forEach { m ->
                            item(key = "m_$m") {
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clickable {
                                            selected = if (m in selected) selected - m else selected + m
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = m in selected,
                                        onCheckedChange = { selected = if (it) selected + m else selected - m }
                                    )
                                    Text(m, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
                // 页脚
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(
                        enabled = selected.isNotEmpty() && pickedName.isNotEmpty(),
                        onClick = {
                            val p = ifaces.firstOrNull { it.name == pickedName } ?: return@TextButton
                            onConfirm(
                                selected.sorted().map { m ->
                                    KeyListFile.KeyEntry(
                                        name = KeyListFile.uniqueKeyName(m, p.name, existingNames),
                                        keyCode = "",
                                        value = "${KeyListFile.openAiBaseUrl(p.baseUrl)}@@$m@@${p.apiKey}",
                                    )
                                }
                            )
                        }
                    ) {
                        Text(stringResource(R.string.role_key_add_selected, selected.size))
                    }
                }
            }
        }
    }
    // 手动添加模型（照插件 showManualModelDialog：输入模型名直接入库）
    if (manualVisible) {
        var manual by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { manualVisible = false },
            title = { Text(stringResource(R.string.role_key_manual_model)) },
            text = {
                OutlinedTextField(
                    value = manual, onValueChange = { manual = it },
                    label = { Text(stringResource(R.string.role_key_model_input_hint)) },
                    singleLine = true, textStyle = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = manual.trim().isNotEmpty() && pickedName.isNotEmpty(),
                    onClick = {
                        val m = manual.trim()
                        manualVisible = false
                        val p = ifaces.firstOrNull { it.name == pickedName } ?: return@TextButton
                        models = models + m
                        selected = selected + m
                    }
                ) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { manualVisible = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    // 新建接口（照插件 showNewInterfaceDialog：建档后回到本弹窗继续拉取）
    if (ifcNewVisible) {
        InterfaceFormDialog(
            tagRuleId = tagRuleId,
            initial = null,
            onDismiss = { ifcNewVisible = false },
            onSaved = {
                ifcNewVisible = false
                scope.launch {
                    val list = withIO { KeyListFile.readInterfaces(tagRuleId) }
                    ifaces = list
                    if (pickedName.isEmpty() || list.none { it.name == pickedName }) {
                        pickedName = list.lastOrNull()?.name ?: list.firstOrNull()?.name.orEmpty()
                    }
                }
            }
        )
    }
}

/** 导入密钥（照插件 importKeysDialog：选 密钥导出_*.json → 确认新增/跳过 → 导入） */
@Composable
private fun ImportKeysDialog(
    tagRuleId: String,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var files by remember { mutableStateOf<List<String>>(emptyList()) }
    var pending by remember { mutableStateOf<Pair<String, List<KeyListFile.KeyEntry>>?>(null) }
    LaunchedEffect(Unit) {
        files = withIO { KeyListFile.listExportFiles(tagRuleId) }
    }
    fun toast(resId: Int, vararg args: Any) {
        android.widget.Toast.makeText(context, context.getString(resId, *args), android.widget.Toast.LENGTH_SHORT).show()
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.role_key_import), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                if (files.isEmpty()) {
                    Text(
                        stringResource(R.string.role_key_no_export),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(Modifier.weight(1f, fill = false)) {
                        files.forEach { fn ->
                            item(key = fn) {
                                Row(
                                    Modifier.fillMaxWidth().clickable {
                                        scope.launch {
                                            val list = withIO { KeyListFile.readExportFile(tagRuleId, fn) }
                                            if (list == null) toast(R.string.role_list_failed)
                                            else pending = fn to list
                                        }
                                    },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(fn, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
    // 导入确认
    pending?.let { (fn, list) ->
        var counts by remember(fn) { mutableStateOf(0 to 0) }
        LaunchedEffect(fn) {
            val names = withIO { KeyListFile.readKeys(tagRuleId).map { it.name }.toSet() }
            counts = list.count { it.name !in names } to list.count { it.name in names }
        }
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.role_key_import_confirm_title)) },
            text = {
                Text(stringResource(R.string.role_key_import_confirm, fn, list.size, counts.first, counts.second))
            },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    scope.launch {
                        val (added, skipped) = withIO { KeyListFile.importKeys(tagRuleId, list) }
                        toast(R.string.role_key_import_done, added, skipped)
                        onDone()
                    }
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

/**
 * 备份恢复中心（照插件 showBackupRestoreDialog 五项 + 自动备份设置）：
 * 导出当前书籍到剪贴板 / 从剪贴板导入书籍 / 备份全部文件(fullBackup.json) /
 * 从备份完整还原 / 自动备份开关（开启后进角色管理页自动备份）。
 */
@Composable
fun BackupCenterDialog(
    tagRuleId: String,
    version: Int,
    onDismiss: () -> Unit,
    onRestored: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var autoOn by remember { mutableStateOf(false) }
    var inputVisible by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var autoSettingVisible by remember { mutableStateOf(false) }

    LaunchedEffect(version) {
        autoOn = withIO { CharacterRecordsFile.readAutoBackupEnabled(tagRuleId) }
    }
    fun toast(resId: Int, vararg args: Any) {
        android.widget.Toast.makeText(
            context, context.getString(resId, *args), android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)) {
                Text(
                    stringResource(R.string.backup_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                BackupOptionRow(Icons.Default.ContentCopy, stringResource(R.string.backup_export_book)) {
                    scope.launch {
                        val recs = withIO { CharacterRecordsFile.readRecords(tagRuleId) }
                        val book = withIO { CharacterRecordsFile.readCurrentBook(tagRuleId) }
                        val arr = JSONArray()
                        recs.forEach { arr.put(it.obj) }
                        val payload = JSONObject()
                            .put("bookName", book)
                            .put("characterData", arr)
                        clipboard.setText(AnnotatedString(payload.toString(2)))
                        toast(R.string.backup_clip_ok)
                    }
                }
                BackupOptionRow(Icons.Default.ContentPaste, stringResource(R.string.backup_import_book)) {
                    inputText = ""
                    inputVisible = true
                }
                BackupOptionRow(Icons.Default.Save, stringResource(R.string.backup_export_all)) {
                    scope.launch {
                        val n = withIO { CharacterRecordsFile.backupAllFiles(tagRuleId) }
                        toast(if (n > 0) R.string.backup_done else R.string.role_list_failed, n)
                    }
                }
                BackupOptionRow(Icons.Default.Restore, stringResource(R.string.backup_restore_all)) {
                    scope.launch {
                        val n = withIO { CharacterRecordsFile.restoreAllFiles(tagRuleId) }
                        toast(
                            when {
                                n < 0 -> R.string.backup_none
                                n > 0 -> R.string.backup_restore_done
                                else -> R.string.role_list_failed
                            }, n
                        )
                        if (n > 0) onRestored()
                    }
                }
                BackupOptionRow(
                    Icons.Default.Schedule, stringResource(R.string.backup_auto_enable)
                ) { autoSettingVisible = true }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                }
            }
        }
    }
    // 自动备份设置（照插件 showAutoBackupSettingDialog：标题显当前状态 → 开启/关闭）
    if (autoSettingVisible) {
        AlertDialog(
            onDismissRequest = { autoSettingVisible = false },
            title = {
                Text(
                    stringResource(
                        R.string.backup_auto_state_title,
                        stringResource(if (autoOn) R.string.backup_auto_on else R.string.backup_auto_off)
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    autoSettingVisible = false
                    scope.launch {
                        withIO { CharacterRecordsFile.writeAutoBackupEnabled(tagRuleId, true) }
                        autoOn = true
                        toast(R.string.backup_auto_enabled_toast)
                    }
                }) { Text(stringResource(R.string.backup_auto_turn_on)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        autoSettingVisible = false
                        scope.launch {
                            withIO { CharacterRecordsFile.writeAutoBackupEnabled(tagRuleId, false) }
                            autoOn = false
                            toast(R.string.backup_auto_disabled_toast)
                        }
                    }) { Text(stringResource(R.string.backup_auto_turn_off)) }
                    TextButton(onClick = { autoSettingVisible = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        )
    }

    // 从剪贴板/文本导入书籍（照插件 restoreFromText：{bookName, characterData} → 建档并切换）
    if (inputVisible) {
        AlertDialog(
            onDismissRequest = { inputVisible = false },
            title = { Text(stringResource(R.string.backup_import_book)) },
            text = {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(stringResource(R.string.backup_import_hint)) },
                    textStyle = MaterialTheme.typography.bodySmall,
                    minLines = 3,
                    maxLines = 8,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = inputText.isNotBlank(),
                    onClick = {
                        scope.launch {
                            val ok = withIO {
                                runCatching {
                                    val obj = JSONObject(inputText)
                                    val bookName = obj.optString("bookName").trim()
                                    val data = obj.optJSONArray("characterData")
                                        ?: throw IllegalArgumentException("bad format")
                                    require(bookName.isNotEmpty()) { "empty book" }
                                    val d = java.io.File(
                                        "/storage/emulated/0/Download/chajian", tagRuleId
                                    )
                                    val json = data.toString(2)
                                    java.io.File(d, "cunfang.txt").writeText(bookName)
                                    java.io.File(d, "characterRecords.json").writeText(json)
                                    java.io.File(d, "shuming.$bookName.json").writeText(json)
                                    true
                                }.getOrDefault(false)
                            }
                            if (ok) {
                                inputVisible = false
                                onRestored()
                                toast(R.string.backup_restore_book_ok)
                            } else {
                                toast(R.string.backup_clip_bad)
                            }
                        }
                    }
                ) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { inputVisible = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

/**
 * 备份恢复选项行（目目 09-14 定案：**去白卡片与描边** → 无框行 + 语义图标）。
 * 原「圆角卡片 + 彩色圆点」是插件语汇：白卡片压在弹窗淡紫底上 = 框中框，
 * 而彩色圆点只在卡片里才不显飘、本身又无信息量 → 换成 18dp 灰色图标（语义=这是什么操作）。
 * 5 行是异质动作（不是同质密集条目），靠行内纵 padding 留白分段，不画分隔线。
 */
@Composable
private fun BackupOptionRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

/** 书籍行左侧圆点配色（照插件 switchColors：8 色循环） */
private val BOOK_DOT_COLORS = listOf(
    Color(0xFF7E57C2), Color(0xFF5C6BC0), Color(0xFF26A69A), Color(0xFF8D6E63),
    Color(0xFF66BB6A), Color(0xFFEC407A), Color(0xFFFF7043), Color(0xFF42A5F5),
)

/** 「多选删除」文字色（照插件 #EF6C00 橙） */
private val BOOK_MULTI_DELETE_COLOR = Color(0xFFEF6C00)

/** 默认书籍名：不可删除（与 CharacterRecordsFile 同源） */
private const val DEFAULT_BOOK_NAME = "默认"

/**
 * 书籍列表弹窗·1:1（照插件 showBookSwitchDialog，图一样式）：
 * 紧凑弹窗；每本书 = 圆角卡片行（当前书 = 主题浅底 + 主题描边 + ✓；其他 = 浅底 + 描边 + 彩色圆点）；
 * 仅非「默认」书显示 ✕（二次确认，删当前书后切默认）；底部「+ 新增书籍 / 多选删除」。
 */
@Composable
fun BookManagerDialog(
    tagRuleId: String,
    onDismiss: () -> Unit,
    onSwitched: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var version by remember { mutableIntStateOf(0) }
    var books by remember { mutableStateOf<List<String>>(emptyList()) }
    var current by remember { mutableStateOf("") }
    var addBookVisible by remember { mutableStateOf(false) }
    var multiVisible by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(version) {
        val loaded = withIO {
            Pair(CharacterRecordsFile.readBookList(tagRuleId), CharacterRecordsFile.readCurrentBook(tagRuleId))
        }
        books = loaded.first
        current = loaded.second
    }
    fun toast(resId: Int, vararg args: Any) {
        android.widget.Toast.makeText(
            context, context.getString(resId, *args), android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    fun switchTo(book: String) {
        scope.launch {
            val ok = withIO { CharacterRecordsFile.switchBook(tagRuleId, book) }
            toast(if (ok) R.string.role_key_saved else R.string.role_list_failed)
            if (ok) { version++; onSwitched() }
        }
    }
    fun deleteBooks(target: Set<String>) {
        scope.launch {
            val (n, currentDeleted) = withIO { CharacterRecordsFile.deleteBooks(tagRuleId, target) }
            toast(
                if (n > 0) R.string.role_book_deleted_toast else R.string.role_list_failed, n
            )
            if (n > 0) {
                version++
                if (currentDeleted) onSwitched()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Column(
                Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
                    .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.85f).dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    stringResource(R.string.role_book_list_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                // 清单上限按屏高推算（不写死 dp）；行间靠 0.6dp 浅分隔线分区（首行不加，同密钥弹窗）
                Column(Modifier.fillMaxWidth()) {
                    books.forEachIndexed { idx, book ->
                        if (idx > 0) {
                            HorizontalDivider(
                                thickness = 0.6.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                        }
                        val isCurrent = book == current
                        BookRow(
                            name = book,
                            isCurrent = isCurrent,
                            dotColor = BOOK_DOT_COLORS[idx % BOOK_DOT_COLORS.size],
                            deletable = book != DEFAULT_BOOK_NAME,
                            onClick = { if (!isCurrent) switchTo(book) },
                            onDelete = { pendingDelete = book },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                // 底部操作行：+ 新增书籍 / 多选删除（照插件居中双按钮）
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { addBookVisible = true }) {
                        Text(
                            "+  " + stringResource(R.string.role_book_add),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = { multiVisible = true }) {
                        Text(
                            stringResource(R.string.role_book_multi_delete_mode),
                            color = BOOK_MULTI_DELETE_COLOR,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }

    // 单本删除二次确认（照插件：提示删当前书会切默认）
    pendingDelete?.let { name ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.role_book_delete_title)) },
            text = {
                Text(
                    stringResource(R.string.role_book_delete_text, name) + "\n" +
                            stringResource(
                                if (name == current) R.string.role_book_del_confirm_current
                                else R.string.role_book_del_confirm_other
                            )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    deleteBooks(setOf(name))
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    // 批量删除（照插件 showMultiSelectBookDialog：卡片行 + 复选框 + 当前徽章）
    if (multiVisible) {
        var checked by remember { mutableStateOf<Set<String>>(emptySet()) }
        val allChecked = books.isNotEmpty() && checked.size == books.size
        Dialog(
            onDismissRequest = { multiVisible = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
            ) {
                Column(
                    Modifier
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
                        .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.85f).dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        stringResource(R.string.role_book_multi_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.fillMaxWidth()) {
                        books.forEachIndexed { idx, book ->
                            if (idx > 0) {
                                HorizontalDivider(
                                    thickness = 0.6.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )
                            }
                            val isCurrent = book == current
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        checked = if (book in checked) checked - book else checked + book
                                    }
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = book in checked,
                                    onCheckedChange = {
                                        checked = if (it) checked + book else checked - book
                                    },
                                )
                                Text(
                                    book,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (isCurrent) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                    ) {
                                        Text(
                                            stringResource(R.string.role_key_current),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = {
                            checked = if (allChecked) emptySet() else books.toSet()
                        }) {
                            Text(
                                stringResource(
                                    if (allChecked) R.string.deselect_all else R.string.select_all
                                )
                            )
                        }
                        TextButton(onClick = { multiVisible = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                        TextButton(
                            enabled = checked.isNotEmpty(),
                            onClick = {
                                val target = checked
                                multiVisible = false
                                deleteBooks(target)
                            },
                        ) {
                            Text(
                                stringResource(R.string.role_book_multi_delete, checked.size),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }

    // 新增书籍（照插件：输入书名 → 建档并直接切换）
    if (addBookVisible) {
        var bookName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addBookVisible = false },
            title = { Text(stringResource(R.string.role_book_add)) },
            text = {
                OutlinedTextField(
                    value = bookName,
                    onValueChange = { bookName = it },
                    label = { Text(stringResource(R.string.role_book_name)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = bookName.trim().isNotEmpty() && bookName.trim() !in books,
                    onClick = {
                        val n = bookName.trim()
                        addBookVisible = false
                        scope.launch {
                            val ok = withIO {
                                CharacterRecordsFile.addBook(tagRuleId, n) &&
                                    CharacterRecordsFile.switchBook(tagRuleId, n)
                            }
                            toast(if (ok) R.string.role_book_added_switch else R.string.role_list_failed, n)
                            if (ok) { version++; onSwitched() }
                        }
                    }
                ) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { addBookVisible = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

/** 书籍列表行（目目 09-14：去描边卡片改无框行，与密钥条目行同一套口径） */
@Composable
private fun BookRow(
    name: String,
    isCurrent: Boolean,
    dotColor: Color,
    deletable: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 行首标记位固定 16dp：当前书=主色竖条 / 其余=彩色圆点 → 书名左缘始终对齐
        Box(Modifier.width(16.dp), contentAlignment = Alignment.CenterStart) {
            if (isCurrent) {
                Box(
                    Modifier
                        .width(3.dp)
                        .height(18.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                )
            } else {
                Box(Modifier.size(7.dp).background(dotColor, CircleShape))
            }
        }
        Text(
            name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isCurrent) FontWeight.SemiBold else null,
            color = if (isCurrent) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (deletable) {
            // 扁平灰叉（进批量删除弹窗才用红色表达）
            FlatIconAction(Icons.Default.Close, contentDescription = stringResource(R.string.delete)) {
                onDelete()
            }
        }
    }
}
