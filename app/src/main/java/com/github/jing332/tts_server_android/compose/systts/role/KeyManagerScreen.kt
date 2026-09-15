package com.github.jing332.tts_server_android.compose.systts.role

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
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
 * 密钥页外观（目目 09-15「卡片分组」改版定稿，非插件原貌）：
 * **承载 = 独立全屏页面**（KeyManagerActivity，原 Dialog 左右各留 24dp、内容区仅约 272dp 太窄）；
 * **分组 = 卡片**：一个接口一张 surfaceContainer(#F2F2EA) 圆角 12 卡，未分组 / 直连密钥各一张，
 *   卡片边界即分组边界；组内条目靠浅分隔线分区，条目缩进与组名左缘对齐（无搜索框，目目 09-15 ①）；
 * 组头 = 22dp 折叠箭头（展开↓/折叠→ 旋转）+ 组名 16sp SemiBold + (N) 灰字 + ✓（本组含当前密钥）；
 *   组级四图标（编辑接口 / 拉取模型 / 测本组 / 删除）**全部常驻**（目目定：不收进 ⋮），
 *   仅接口组有前三个（未分组 / 直连点了只是白弹提示）；
 * 元信息行 = 接口组「网址（放不下换行，不再单行省略）+ 尾号独立小块」；
 *   未分组 / 直连组 = 一句身份说明（原该行为空）；
 * 当前密钥 = 行首**状态点**染主题强调色 + 名称同色加粗。强调色取 `scheme.secondary` 而**不是 primary**：
 *   10 个手写主题的 primary 全是各自的 *_seed（默认档 #7B8B70，很淡），浅底染色等于没染——这正是
 *   改版前「当前密钥找不出来」的根因；secondary 各主题都是中深档（默认档 #55624C），读得出来。
 * 状态点四态：当前=主题色实心 / 绿=测通 / 红=测不通 / 空心=未测（取代原先按序取色的装饰圆点）；
 * 条目行 = 状态点 + 名称（放不下**换行**，不再单行省略）+ ⚡测试 / ✏️编辑（两个都常驻）；
 * 组级/条目级图标统一扁平灰（onSurfaceVariant、18dp、36dp 热区，无常驻描边）。
 */

/** 分组后的密钥组（照插件 buildKeyGroups：接口组 + 未分组 + 直连密钥） */
private class KeyGroup(
    val title: String,
    val entries: List<KeyListFile.KeyEntry>,
    val ifc: KeyListFile.ApiInterface? = null,
    /** 未分组 / 直连组的身份说明（接口组的元信息行让给网址，不需要） */
    val hintRes: Int? = null,
)

private fun buildKeyGroups(keys: List<KeyListFile.KeyEntry>, ifaces: List<KeyListFile.ApiInterface>): List<KeyGroup> {
    val groups = mutableListOf<KeyGroup>()
    val assigned = mutableSetOf<String>()
        ifaces.forEach { ifc ->
            val entries = keys.filter { KeyListFile.keyBelongsTo(it, ifc) }
            // 空分组**照样显示**（目目 09-15「先建组、再拉模型」）。旧版在这里 `if (entries.isNotEmpty())`
            // 把空组整个滤掉 ⇒ 建完分组页面上根本不出现、点不到它的「拉取模型」——是条真断路（P0）。
            groups.add(KeyGroup(ifc.name, entries, ifc))
            entries.forEach { assigned.add(it.name) }
        }
    val direct = keys.filter { k ->
        val p = KeyListFile.parseKeyValue(k.value)
        p != null && p.isDirect && k.name !in assigned
    }
    val ungrouped = keys.filter { it.name !in assigned && it !in direct }
    if (ungrouped.isNotEmpty()) groups.add(
        KeyGroup("未分组", ungrouped, hintRes = R.string.role_key_group_ungrouped_hint)
    )
    if (direct.isNotEmpty()) groups.add(
        KeyGroup("直连密钥", direct, hintRes = R.string.role_key_group_direct_hint)
    )
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

/**
 * 密钥条目行（目目 09-15 卡片改版）：行内无框，边界由所属卡片承担；
 * 行首 **状态点**取代原「按序号取色」的装饰圆点（那是纯噪音，同接口下每条颜色都不同）：
 *   当前 = 主题强调色实心 / 测通 = 绿实心 / 测不通 = 红实心 / 未测 = 空心圆环。
 * 两行布局：
 *   行1 = 显示名 + 「当前」徽章（点这一行 = 切为当前）；
 *   行2 = 动作图标**居右**排：✏️ 编辑 / ⚡ 测试 / 📋 复制 / 🗑 删除，扁平灰、热区 36dp。
 * 名字放不下就**换行**（原为单行省略号，`nex-agi/nex-n2.5-mini` 这类长模型名直接被截）。
 */
@Composable
private fun KeyEntryRow(
    entry: KeyListFile.KeyEntry,
    isCurrent: Boolean,
    accent: Color,
    testOk: Boolean?,
    testing: Boolean,
    deleteMode: Boolean,
    checked: Boolean,
    onToggleCheck: () -> Unit,
    onSwitch: () -> Unit,
    onCopy: () -> Unit,
    onTest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        // 卡片内边距 10dp + 状态点 14dp + 间距 8dp ⇒ 名字左缘 32dp，
        // 与组名左缘（组头起 6 + 箭头 22 + 间距 6 = 34dp）基本对齐 → 从属关系一眼可见
        Modifier.fillMaxWidth().padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        // 普通态是两行 ⇒ 必须用 Top 让状态点咬住**第一行**文字（用 CenterVertically 会掉到两行中间）；
        // 删除多选态只剩一行，保持原来的居中
        verticalAlignment = if (deleteMode) Alignment.CenterVertically else Alignment.Top
    ) {
        if (deleteMode) {
            Checkbox(checked = checked, onCheckedChange = { onToggleCheck() })
        }
        // 状态点固定 14dp 位宽：名字左缘始终对齐；高度咬第一行（bodyLarge 行高 24sp），
        // 这样两行态下它仍与模型名同一条水平线
        Box(Modifier.width(14.dp).height(24.dp), contentAlignment = Alignment.CenterStart) {
            val dot = when {
                isCurrent -> accent
                testOk == true -> Color(0xFF2E7D32)
                testOk == false -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            if (isCurrent || testOk != null) {
                Box(Modifier.size(8.dp).background(dot, CircleShape))
            } else {
                // 未测 = 空心圆环（描边），一眼区分「没测过」与「测过但红/绿」
                Box(Modifier.size(8.dp).border(1.dp, dot, CircleShape))
            }
        }
        Spacer(Modifier.width(8.dp))
        // 两行（目目 09-15）：
        //   行1 = 显示名 + 「当前」徽章（点这行 = 切为当前），完整 @@ 串已不上列表；
        //   行2 = 动作图标居右：✏️编辑 ⚡测试 📋复制 🗑删除。
        // 显示名取**值里的真实模型名**：条目名可能带 `@组名` 去重后缀（跨组同模型共存用），
        // 那是内部标识、不该给人看。
        Column(Modifier.weight(1f)) {
            Row(
                Modifier.fillMaxWidth().clickable { onSwitch() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    KeyListFile.displayName(entry),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else null,
                    color = if (isCurrent) accent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isCurrent) {
                    Spacer(Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = accent.copy(alpha = 0.14f)) {
                        Text(
                            stringResource(R.string.role_key_current_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
                if (testing) {
                    Spacer(Modifier.width(6.dp))
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
            if (!deleteMode) {
                // 动作行**居右**（目目 09-15：编辑 / 测试 / 复制 / 删除）
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    FlatIconAction(Icons.Default.Edit, stringResource(R.string.role_key_edit)) { onEdit() }
                    FlatIconAction(Icons.Default.Bolt, stringResource(R.string.role_key_test)) { onTest() }
                    // 📋 复制的是**模型名**（编辑弹窗里的「复制」才复制完整密钥串，两处不一样）
                    FlatIconAction(Icons.Default.ContentCopy, stringResource(R.string.copy)) { onCopy() }
                    FlatIconAction(Icons.Default.DeleteOutline, stringResource(R.string.delete)) { onDelete() }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyManagerScreen(tagRuleId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 条目行 📋 复制用（目目 09-15）
    val clipboard = LocalClipboardManager.current
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
    var menuGroup by remember { mutableStateOf<String?>(null) }          // 组头 🗑 展开的两项菜单
    var deleteGroupConfirm by remember { mutableStateOf<String?>(null) } // 「删除整组」二次确认

    LaunchedEffect(version) {
        // 分组自愈先跑（目目 09-15 ④「未分组自动收编」）：匹配不上任何分组的 @@ 条目
        // 按（归一化网址 + 密钥）自动建组，这样「加一条 @@ 密钥」就自动落到合适的分组下。
        withIO { KeyListFile.heal(tagRuleId) }
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
            toast(R.string.role_key_is_current, KeyListFile.displayName(entry))
            return
        }
        if (entry.value.isBlank()) {
            toast(R.string.role_key_value_empty)
            return
        }
        scope.launch {
            withIO { KeyListFile.saveCurrentRaw(tagRuleId, entry.value) }
            toast(R.string.role_key_switched, KeyListFile.displayName(entry))
            version++
        }
    }
    fun testKey(entry: KeyListFile.KeyEntry) {
        if (entry.value.isBlank()) {
            toast(R.string.role_key_value_empty)
            return
        }
        scope.launch {
            testingName = entry.name
            // 传**原始值**：@@串走对话端点、纯 Key 走智谱 /models（照插件 testModelKey 的两个分支）。
            // 旧版在这里就把纯 Key 挡掉了，直连条目一直没有可用性检验入口。
            val r = withIO { KeyListFile.testKey(entry.value) }
            testingName = null
            testResults = testResults + (entry.name to r.first)
            toast(
                if (r.first) R.string.role_key_test_ok_toast else R.string.role_key_test_fail_toast,
                KeyListFile.displayName(entry), r.second
            )
        }
    }
    // 整组测试（照插件组头 ⚡：逐条测完逐条标记；直连组同样可测）
    fun testGroup(grp: KeyGroup) {
        val targets = grp.entries.filter { it.value.isNotBlank() }
        if (targets.isEmpty()) {
            toast(R.string.role_key_test_none)
            return
        }
        scope.launch {
            testingGroup = grp.title
            toast(R.string.role_key_test_batch_start, grp.title, targets.size)
            var okCount = 0
            targets.forEach { e ->
                val r = withIO { KeyListFile.testKey(e.value) }
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

    // 删除整组（目目 09-15）：接口组 → 连 api_center.json 的该接口一起删；未分组/直连组 → 只删其下条目。
    // 当前密钥若在被删集合里，deleteNames 会回落到剩余第一条（照插件 deleteMultipleBooks 口径）。
    fun deleteGroupAll(grp: KeyGroup) {
        val names = grp.entries.map { it.name }
        val ifc = grp.ifc
        scope.launch {
            if (ifc != null) {
                withIO {
                    KeyListFile.saveInterfaces(
                        tagRuleId,
                        KeyListFile.readInterfaces(tagRuleId).filter { it.name != ifc.name }
                    )
                }
            }
            deleteNames(names)
        }
    }

    // 弹窗状态
    var showAdd by remember { mutableStateOf(false) }
    var renameFor by remember { mutableStateOf<KeyListFile.KeyEntry?>(null) }
    var overwriteFor by remember { mutableStateOf<Pair<String, String>?>(null) } // (新名, 值) 覆盖确认
    var deleteFor by remember { mutableStateOf<KeyListFile.KeyEntry?>(null) }
    var ifcFormFor by remember { mutableStateOf<KeyListFile.ApiInterface?>(null) } // 组头 ✏️ 编辑该接口
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
                // 导入/导出（目目 09-14 三次定稿）：emoji → FileDownload/FileUpload 单色图标
                // + 文字（托盘竖箭头字形目目点名回归；语义「从文件取 / 存到文件」）。
                // 紧凑动作：热区 ≥48dp 高，不用 IconButton/TextButton（自带最小宽会挤标题）。
                actions = {
                    Box(
                        Modifier
                            .heightIn(min = 48.dp)
                            .clickable { showImport = true }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.role_key_action_import),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    Box(
                        Modifier
                            .heightIn(min = 48.dp)
                            .clickable {
                                scope.launch {
                                    val name = withIO { KeyListFile.exportKeys(tagRuleId, keys) }
                                    if (name != null) toast(R.string.role_key_exported, keys.size, name)
                                    else toast(R.string.role_list_failed)
                                }
                            }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.role_key_action_export),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
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
                // 操作行：新增密钥 / 拉取模型（目目 09-14 定稿 MD3 化：官方 OutlinedButton，
                // 去掉文字里的 ＋/🔍 emoji 与硬编码紫色，颜色统一 primary）
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showAdd = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.role_key_add))
                    }
                    OutlinedButton(
                        onClick = { showPullModels = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.role_key_fetch))
                    }
                }
                val groups = buildKeyGroups(keys, ifaces)
                // 有分组（哪怕全是空组）就渲染卡片：先建组、再拉模型这条路必须走得通
                if (groups.isEmpty()) {
                    Text(
                        stringResource(R.string.role_key_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                    )
                } else {
                    // 当前密钥强调色（目目 09-15 ②「你想换就换吧」）：用 scheme.secondary。
                    // MD3 里 secondary 就是「次强调」的定位，各主题都是中深档（默认档 #55624C）→ 浅底读得出。
                    // 不能用 primary：10 个手写主题的 primary 全是各自的 *_seed（默认档 #7B8B70，很淡），
                    // 染色等于没染，这正是改版前「当前密钥找不出来」的根因。
                    val accent = MaterialTheme.colorScheme.secondary
                    groups.forEach { grp ->
                        val isCollapsed = grp.title in collapsed
                        val isDeleting = deleteModeGroup == grp.title
                        val grpHasCurrent = currentRaw.isNotEmpty() &&
                                grp.entries.any { it.value.trim() == currentRaw }
                        val selCount = grp.entries.count { it.name in deleteChecked }
                        // 卡片分组（目目 09-15 ①定稿）：一个接口一张浅底卡（surfaceContainer + 圆角 12），
                        // 卡片边界 = 分组边界；未分组 / 直连密钥各一张。组间 10dp 留白区隔、组内条目紧凑。
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        ) {
                            Column(Modifier.padding(vertical = 4.dp)) {
                                // ———— 组头 ————
                                Row(
                                    Modifier.fillMaxWidth()
                                        .padding(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
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
                                        // 组头可点区：折叠箭头 + 组名 16sp SemiBold + (N) 灰字；
                                        // 折叠态 = 箭头旋转朝右，与主界面折叠分组同一语汇
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
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "(${grp.entries.size})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (grpHasCurrent) {
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    "✓",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = accent
                                                )
                                            }
                                        }
                                        // 组级图标（目目 09-15 ①第三条：四个**全部保留在外**，不收进 ⋮）。
                                        // 仅接口组有前三个动作——未分组 / 直连组点了也只是白弹提示。
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
                                        // 组头 🗑（目目 09-15）：点开是**两项**而非单一动作——
                                        // 「删除整组」连带子项、「多选删除子项」只在本组范围内勾选。
                                        Box {
                                            FlatIconAction(
                                                Icons.Default.DeleteOutline,
                                                stringResource(R.string.delete)
                                            ) { menuGroup = grp.title }
                                            DropdownMenu(
                                                expanded = menuGroup == grp.title,
                                                onDismissRequest = { menuGroup = null }
                                            ) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text(
                                                                stringResource(R.string.role_key_group_delete_all),
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.error
                                                            )
                                                            Text(
                                                                stringResource(
                                                                    R.string.role_key_group_delete_all_sub,
                                                                    grp.entries.size
                                                                ),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    },
                                                    onClick = {
                                                        menuGroup = null
                                                        deleteGroupConfirm = grp.title
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text(
                                                                stringResource(R.string.role_key_group_delete_multi),
                                                                style = MaterialTheme.typography.bodyMedium
                                                            )
                                                            Text(
                                                                stringResource(R.string.role_key_group_delete_multi_sub),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    },
                                                    onClick = {
                                                        menuGroup = null
                                                        deleteModeGroup = grp.title
                                                        deleteChecked = emptySet()
                                                        collapsed = collapsed - grp.title
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                // ———— 元信息行 ————
                                // 接口组 = 网址（放不下就换行）+ 尾号独立小块；未分组 / 直连组 = 一句身份说明。
                                if (!isDeleting) {
                                    val ifc = grp.ifc
                                    if (ifc != null) {
                                        Row(
                                            Modifier.fillMaxWidth()
                                                .padding(start = 34.dp, end = 10.dp, bottom = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                ifc.baseUrl,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                // 目目 09-15：一栏放不下就换行（原先单行省略号，长网址会被吃掉）
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            // 尾号做成独立小块：原先挤在网址尾巴上，网址一长尾号先进省略号
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                                            ) {
                                                Text(
                                                    stringResource(
                                                        R.string.role_key_tail, ifc.apiKey.takeLast(4)
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        grp.hintRes?.let { hint ->
                                            Text(
                                                stringResource(hint),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(start = 34.dp, end = 10.dp, bottom = 6.dp)
                                            )
                                        }
                                    }
                                }
                                if (!isCollapsed) {
                                    grp.entries.forEachIndexed { idx, entry ->
                                        val isCurrent = currentRaw.isNotEmpty() &&
                                                entry.value.trim() == currentRaw
                                        // 条目靠卡内浅分隔线分区；首条不加，避免紧贴元信息行
                                        if (idx > 0) {
                                            HorizontalDivider(
                                                thickness = 0.6.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.padding(horizontal = 10.dp)
                                            )
                                        }
                                        KeyEntryRow(
                                            entry = entry,
                                            isCurrent = isCurrent,
                                            accent = accent,
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
                                            // 📋 列表行复制 = **模型名**（目目 09-15：两处复制不一样，
                                            // 列表行给模型名、编辑弹窗里复制的才是完整密钥；插件也是这么分的
                                            // ——列表行 copyName、详情弹窗 copyValue）
                                            onCopy = {
                                                clipboard.setText(AnnotatedString(KeyListFile.displayName(entry)))
                                                toast(R.string.role_key_copied_model)
                                            },
                                            onTest = { testKey(entry) },
                                            onEdit = { renameFor = entry },
                                            onDelete = { deleteFor = entry }
                                        )
                                    }
                                }
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

    // 删除整组确认（目目 09-15：组头 🗑 →「删除整组」，连带子项，先确认并显示条数）
    deleteGroupConfirm?.let { gTitle ->
        val grp = buildKeyGroups(keys, ifaces).firstOrNull { it.title == gTitle }
        val n = grp?.entries?.size ?: 0
        AlertDialog(
            onDismissRequest = { deleteGroupConfirm = null },
            title = { Text(stringResource(R.string.role_key_delete_group_title)) },
            text = { Text(stringResource(R.string.role_key_delete_group_text, gTitle, n)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteGroupConfirm = null
                    deleteModeGroup = null
                    deleteChecked = emptySet()
                    if (grp != null) deleteGroupAll(grp)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteGroupConfirm = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 新增（名称留空自动生成；**同组**重名→覆盖确认，跨组撞名→加序号另存，照插件 showAddKeyDialog）
    if (showAdd) {
        KeyEditDialog(
            initial = null,
            existing = keys,
            onDismiss = { showAdd = false },
            onConfirm = { name, value, overwrite ->
                if (overwrite) {
                    showAdd = false
                    overwriteFor = name to value
                } else {
                    showAdd = false
                    save(keys + KeyListFile.KeyEntry(name = name, keyCode = KeyListFile.nextKeyCode(keys), value = value))
                    // 照插件 showAddKeyDialog：**只有当前密钥为空**时才把它设为当前。
                    // ⚠️ 旧版无条件三写 miyue/gengxin/miyue_backup ⇒ 正在用 A 朗读时新增 B，
                    //    当前密钥被静默切走（朗读时用的密钥换了都不知道）。
                    if (currentRaw.isBlank()) {
                        scope.launch { withIO { KeyListFile.saveCurrentRaw(tagRuleId, value) }; version++ }
                        toast(R.string.role_key_add_first, name)
                    } else {
                        toast(R.string.role_key_saved)
                    }
                }
            }
        )
    }
    // 改名（重名→覆盖确认，照插件 showKeyNameDialog）
    renameFor?.let { entry ->
        KeyEditDialog(
            initial = entry,
            existing = keys,
            onDismiss = { renameFor = null },
            onDelete = { renameFor = null; deleteFor = entry },
            onConfirm = { name, value, overwrite ->
                renameFor = null
                if (overwrite && name != entry.name) {
                    // 照插件密钥详情页「保存」(1625-1628)：改成**已存在**的名字 → 直接拒绝。
                    // ⚠️ 旧版走「覆盖」分支：只覆盖那个同名条目、**旧名条目没被删** ⇒ 列表里两条并存，
                    //    再改一次又多一条（与书籍改名同一类坑：改名没把旧名清掉）。
                    toast(R.string.role_key_name_dup, name)
                } else {
                    save(keys.map { if (it.name == entry.name) it.copy(name = name, value = value) else it })
                    // 照插件密钥详情页「保存」：改的正是当前密钥 → 同步 miyue 三写。
                    // ⚠️ 旧版只写 key_list.json，miyue.txt 留旧值 —— 重进页面即被兜底切到第一条。
                    if (entry.value.trim() == currentRaw && value.isNotBlank()) {
                        scope.launch {
                            withIO { KeyListFile.saveCurrentRaw(tagRuleId, value) }
                            version++
                        }
                    }
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
                    // 覆盖**当前密钥**本身 → 同步 miyue；否则照插件「仅当前为空才启用」的口径处理
                    val wasCurrent = old != null && old.value.trim() == currentRaw
                    if (wasCurrent || currentRaw.isBlank()) {
                        scope.launch { withIO { KeyListFile.saveCurrentRaw(tagRuleId, value) }; version++ }
                    }
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
            text = { Text(stringResource(R.string.role_key_delete_text, KeyListFile.displayName(entry))) },
            confirmButton = {
                TextButton(onClick = {
                    deleteFor = null
                    // 与批量删除同一条路（照插件 deleteMultipleBooks：删掉当前密钥 → 自动切到剩余第一条）。
                    // ⚠️ 旧版只 save(filter)：miyue.txt 仍指向已删掉的 key，重进页面被兜底切走。
                    deleteNames(listOf(entry.name))
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteFor = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    // 接口表单（编辑 / 级联删除，照插件 showInterfaceFormDialog）。
    // 「新建接口」的独立入口已撤（目目 09-15）：新建并进「拉取模型」弹窗——顶部直接填网址 + Key，
    // 确认时由 [KeyListFile.ensureGroup] 自动建档（组名取网址短名），所以这里只剩编辑
    ifcFormFor?.let { ifc ->
        InterfaceFormDialog(
            tagRuleId = tagRuleId,
            initial = ifc,
            onDismiss = { ifcFormFor = null },
            onSaved = { ifcFormFor = null; version++ },
        )
    }
    // 拉取模型（目目 09-15 改版：顶部=填网址+Key 去建组/并入；分组卡 🔍=给本组拉）。
    // 两个入口共用这一个弹窗，差别只在 initialIfcName；落库统一走 ensureGroup——判据与「未分组收编」
    // 同一套（同网址 + 同密钥），所以不会建出两个同网址同密钥的分组来
    if (showPullModels) {
        ModelPullDialog(
            keys = keys,
            ifaces = ifaces,
            initialIfcName = pullForIfc,
            onDismiss = { showPullModels = false; pullForIfc = null },
            onConfirm = { url, apiKey, pickedModels ->
                showPullModels = false
                pullForIfc = null
                scope.launch {
                    val ifc = withIO { KeyListFile.ensureGroup(tagRuleId, url, apiKey) }
                    if (ifc == null) {
                        toast(R.string.role_list_failed)
                    } else {
                        // 名字逐个占位去重：同一批里也互不撞名（跨组重名只加序号，忽略另一个组）
                        val used = keys.map { it.name }.toMutableSet()
                        val entries = pickedModels.map { m ->
                            val n = KeyListFile.dedupName(m, used)
                            used.add(n)
                            KeyListFile.KeyEntry(
                                name = n,
                                keyCode = "",
                                value = "${ifc.baseUrl}@@$m@@${ifc.apiKey}",
                            )
                        }
                        // 组内去重（目目 09-15）：同（站点 + 密钥 + 模型）已在组里 → 不再多出一条
                        val toAdd = entries.filterNot { e ->
                            val p = KeyListFile.parseKeyValue(e.value)
                            p != null && !p.isDirect && KeyListFile.hasModel(keys, ifc, p.model)
                        }
                        val merged = toAdd.fold(keys) { acc, e ->
                            acc + e.copy(keyCode = KeyListFile.nextKeyCode(acc))
                        }
                        withIO {
                            if (toAdd.isNotEmpty()) KeyListFile.saveKeys(tagRuleId, merged)
                            // 拉到的模型登记进分组 models（旧版从不回写 ⇒ api_center.models 恒空，
                            // 分组下有几条模型这个信息根本没落盘）
                            KeyListFile.addModelsToInterface(tagRuleId, ifc.name, pickedModels)
                        }
                        toast(R.string.role_key_pull_done, toAdd.size, entries.size - toAdd.size)
                    }
                    version++
                }
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

/**
 * 密钥编辑（条目 ✏️ / 新增共用）：第一行「模型」（留空自动从密钥串里抽模型名）、
 * 第二行「密钥」（整串 网址@@模型@@Key），底部 取消 / 删除 / 复制 / 确定；
 * 返回 overwrite=同组重名、待覆盖确认。
 *
 * **两处复制语义不同**（目目 09-15 明确）：
 *  - 列表行右侧 📋 = 复制**模型名**（第一行那个东西）；
 *  - 本弹窗里的「复制」= 复制**完整密钥**（第二行的整串 网址@@模型@@Key，照插件密钥详情页 1531）。
 */
@Composable
private fun KeyEditDialog(
    initial: KeyListFile.KeyEntry?,
    existing: List<KeyListFile.KeyEntry>,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onConfirm: (String, String, Boolean) -> Unit,
) {
    val existingNames = existing.map { it.name }.toSet()
    // 光标默认落在末尾（照插件 v10 密钥详情：setText 后 setSelection(len)，1486/1507）。
    // 第一行=模型 ⇒ 预填**显示名**（值里的真实模型名），不是内部条目名——内部名可能带跨组共存
    // 用的去重序号（glm-5.3-flash2），摆进「模型」框里只会让人以为是另一个模型（目目 09-15）
    val initName = initial?.let { KeyListFile.displayName(it) }.orEmpty()
    val initValue = initial?.value.orEmpty()
    var name by remember { mutableStateOf(TextFieldValue(initName, TextRange(initName.length))) }
    var value by remember { mutableStateOf(TextFieldValue(initValue, TextRange(initValue.length))) }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    fun toast(resId: Int) {
        android.widget.Toast.makeText(
            context, context.getString(resId), android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (initial == null) R.string.role_key_add else R.string.role_key_rename))
        },
        text = {
            Column {
                // 第一行 = 模型（目目 09-15：「第一行直接叫模型」）；留空照样自动生成，
                // 生成口径照插件 defaultName：从**完整密钥串**里抽模型名，抽不出才用 key01…
                Text(
                    stringResource(R.string.role_key_model_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(stringResource(R.string.role_key_name_auto)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                // 第二行 = 密钥（整串 网址@@模型@@Key；纯 Key 为直连）
                Text(
                    stringResource(R.string.role_key_value),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 密钥串（网址@@模型@@Key）很长，一栏放不下就换行（目目 09-15；原 singleLine
                // 会把中段吞掉，只能左右横拖）
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = { Text(stringResource(R.string.role_key_value_hint)) },
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            // 底部按钮行（目目 09-15）：取消 / 删除 / 复制 / 确定。M3 的按钮区是 AlertDialogFlowRow，
            // 按「dismiss 槽 → confirm 槽」的顺序排，所以拆成两槽就能得到这个左右顺序；
            // 拆槽还有个好处：每槽最多两个按钮，窄屏（360dp 下可用宽约 304dp）不会整体被挤去第二行。
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 「复制」= 第二行的**完整密钥串**（列表行右侧 📋 复制的才是模型名，两处不一样）
                TextButton(
                    enabled = value.text.isNotBlank(),
                    onClick = {
                        clipboard.setText(AnnotatedString(value.text.trim()))
                        toast(R.string.copied)
                    }
                ) { Text(stringResource(R.string.copy)) }
                TextButton(
                    enabled = value.text.isNotBlank(),
                    onClick = {
                        val raw = value.text.trim()
                        // 留空自动生成（照插件 defaultName=模型或 key01）：从**完整密钥串**里抽模型名
                        val auto = KeyListFile.parseKeyValue(raw)?.let { p ->
                            if (!p.isDirect && p.model.isNotEmpty()) p.model else "key" + (existing.size + 1)
                        } ?: "key" + (existing.size + 1)
                        val wanted = name.text.trim().ifEmpty { auto }
                        // 编辑态「模型名没动」（含清空后自动取回模型名）⇒ 原样保留条目内部名、不触发改名判定：
                        // 跨组共存时这个模型名可能正被另一组占着（那边没序号），白点一下会误报「名称已存在」
                        if (initial != null && (wanted == initName || wanted == initial.name)) {
                            onConfirm(initial.name, raw, false)
                            return@TextButton
                        }
                        val clash = existing.firstOrNull { it.name == wanted && it.name != initial?.name }
                        // 撞名判定（目目 09-15「跨组重名就忽略另一个组」）：
                        //  · 新增：只有**同一分组**（同网址 + 同密钥）撞名才算真重复 ⇒ 走覆盖确认（照插件 2327/2384）；
                        //    跨组撞名不是冲突 ⇒ 悄悄加序号另存，既不许覆盖人家的条目，也不拿别组当命名依据。
                        //  · 改名：照插件密钥详情页保持「名字被占就用不了」（上层弹「名称已存在」），不做静默改名。
                        val overwrite =
                            if (initial != null) clash != null
                            else clash != null && KeyListFile.sameGroupValue(clash.value, raw)
                        val finalName =
                            if (initial == null && clash != null && !overwrite)
                                KeyListFile.dedupName(wanted, existingNames)
                            else wanted
                        onConfirm(finalName, raw, overwrite)
                    }
                ) { Text(stringResource(R.string.confirm)) }
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                // 照插件条目 ✏️ 弹窗：删除入口留在编辑弹窗内（红字）
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
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
    // 目目 09-15：新建分组时名称框随网址**自动填短名**（KeyListFile.shortName 的掐头去尾口径）；
    // 人改过、或编辑已有分组时不覆盖，免得冲掉人家起的名。
    // 名称框：预填值一律让光标落末尾（照 0fa317e 口径），否则 Compose 的 String 值会把光标置于开头
    var name by remember {
        val init = initial?.name.orEmpty()
        mutableStateOf(TextFieldValue(init, TextRange(init.length)))
    }
    // 名称被用户手改过就不再被网址覆盖；编辑已有分组时视为「已改」，不许动人家改过的名字
    var nameTouched by remember { mutableStateOf(initial != null) }
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
                    value = name,
                    onValueChange = { name = it; nameTouched = true },
                    singleLine = true, textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.role_key_ifc_url),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 接口地址可能很长，一栏放不下就换行（目目 09-15）
                OutlinedTextField(
                    value = url,
                    onValueChange = { v ->
                        url = v
                        // 新建分组时名称框跟着网址走：取「掐头去尾的短名」（口径见 KeyListFile.shortName），
                        // 如 https://cavoti.com/v1 → cavoti、https://xiaoqun.lyzm.xyz/v1 → xiaoqun。
                        // 人改过（或原本就是空的）不覆盖，免得把人家的名字冲掉。
                        if (!nameTouched || name.text.isBlank()) {
                            val s = KeyListFile.shortName(v)
                            name = TextFieldValue(s, TextRange(s.length))
                        }
                    },
                    singleLine = false, minLines = 1, maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.role_key_ifc_key),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Key 可能很长，一栏放不下就换行（目目 09-15）
                OutlinedTextField(
                    value = key, onValueChange = { key = it },
                    singleLine = false, minLines = 1, maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
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
                    val n = name.text.trim()
                    val u = KeyListFile.normalizeBaseUrl(url.trim())
                    val k = key.trim()
                    if (n.isEmpty()) { toast(R.string.role_key_ifc_name_empty); return@TextButton }
                    if (!u.startsWith("http")) { toast(R.string.role_key_ifc_url_bad); return@TextButton }
                    if (k.isEmpty()) { toast(R.string.role_key_ifc_key_empty); return@TextButton }
                    scope.launch {
                        val ifaces = withIO { KeyListFile.readInterfaces(tagRuleId) }
                        // 重名校验：新建必查；编辑也不许改成**别的**接口已有的名字（照插件 showEditInterfaceDialog）
                        val dup = ifaces.any { it.name == n && (initial == null || it.name != initial.name) }
                        if (dup) {
                            toast(R.string.role_key_ifc_name_dup)
                            return@launch
                        }
                        // 分组 = （归一化网址 + 密钥）唯一（目目 09-15）。改成与另一个接口完全同组
                        // ⇒ 会凭空多出一个「同组」的分组；按目目定的「拦住报错」处理，不静默合并。
                        val collide = ifaces.firstOrNull {
                            it.name != (initial?.name ?: "") &&
                                KeyListFile.sameApiSite(it.baseUrl, u) && it.apiKey.trim() == k
                        }
                        if (collide != null) {
                            toast(R.string.role_key_ifc_group_dup, collide.name)
                            return@launch
                        }
                        val updated = if (initial == null) {
                            ifaces + KeyListFile.ApiInterface(n, u, k, emptyList())
                        } else {
                            ifaces.map { if (it.name == initial.name) KeyListFile.ApiInterface(n, u, k, it.models) else it }
                        }
                        withIO { KeyListFile.saveInterfaces(tagRuleId, updated) }
                        // 照插件 showEditInterfaceDialog：网址/密钥变了，把**组内**（同站 && 同旧 key）
                        // 的密钥条目 value 一起改写（模型名不变）。
                        // ⚠️ 旧版只改 api_center.json：旧条目仍指旧地址/旧 key，整组掉进「未分组」，
                        //    用户还得逐条手改。
                        if (initial != null) {
                            val oldUrl = initial.baseUrl
                            val oldKey = initial.apiKey
                            val entryList = withIO { KeyListFile.readKeys(tagRuleId) }
                            var changed = false
                            val rewritten = entryList.map { e ->
                                val p = KeyListFile.parseKeyValue(e.value)
                                if (p != null && !p.isDirect && p.key == oldKey &&
                                    KeyListFile.sameApiSite(p.url, oldUrl)
                                ) {
                                    changed = true
                                    e.copy(value = "$u@@${p.model}@@$k")
                                } else e
                            }
                            if (changed) withIO { KeyListFile.saveKeys(tagRuleId, rewritten) }
                        }
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
 * 拉取模型（目目 09-15 改版）：**两个入口共用这一个弹窗**。
 *  - 顶部「拉取模型」= 新建模式：直接填【接口 URL + API Key】（不再列接口单选）→ 拉取；
 *    确认时按（归一化网址 + 密钥）找分组，找到就并入、没找到就用网址短名建一个（[KeyListFile.ensureGroup]）。
 *  - 分组卡 🔍 = 分组模式（[initialIfcName] 非空）：进来即以该组网址+密钥自动拉取，不用再选一次。
 * 手动添加模型 = 标题行**右上角**的一个入口（两模式共用，目目 09-15 定；原先挤在操作行里）。
 * 「已在组内」**只按目标分组（= 这一个接口）算**：判据 = 同站点 + 同密钥 + 同模型，别的接口拉过同一个
 * 模型**不算**，照样能勾、能存 ⇒ 同一个模型（如 glm-5.3-flash）可以在多个接口各存一份
 * （cavoti 一条、openrouter 一条）；只有「同接口 + 同模型」才算真重复、才挡。
 */
@Composable
private fun ModelPullDialog(
    keys: List<KeyListFile.KeyEntry>,
    ifaces: List<KeyListFile.ApiInterface>,
    initialIfcName: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, List<String>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    // 分组模式：上层给的是接口名，取回接口本体；组已被删则退回新建模式（不至于弹个空壳）
    val initialIfc = remember(ifaces, initialIfcName) {
        initialIfcName?.let { n -> ifaces.firstOrNull { it.name == n } }
    }
    val forGroup = initialIfc != null
    var urlText by remember { mutableStateOf(TextFieldValue(initialIfc?.baseUrl.orEmpty())) }
    var keyText by remember { mutableStateOf(TextFieldValue(initialIfc?.apiKey.orEmpty())) }
    var models by remember { mutableStateOf<List<String>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("") }
    var manualVisible by remember { mutableStateOf(false) }

    val url = urlText.text.trim()
    val key = keyText.text.trim()
    // 目标分组：分组模式=点进来的那个；新建模式=网址+密钥命中已有分组时就并入（否则为 null，确认时才建组）
    val targetIfc = initialIfc ?: ifaces.firstOrNull {
        KeyListFile.sameApiSite(it.baseUrl, url) && it.apiKey.trim() == key
    }
    val ready = forGroup || (url.isNotEmpty() && key.isNotEmpty())

    fun fetch() {
        if (!ready) return
        val u = targetIfc?.baseUrl ?: url
        val k = targetIfc?.apiKey ?: key
        scope.launch {
            loading = true; error = ""
            val r = withIO { KeyListFile.fetchModels(u, k) }
            loading = false
            if (r.first == null) error = r.second
            else { models = r.first ?: emptyList(); selected = emptySet() } // 默认不勾选
        }
    }
    // 分组模式：进来就拉，省掉「再点一次拉取」
    LaunchedEffect(Unit) { if (forGroup) fetch() }
    val visibleModels = models.filter { filter.isBlank() || it.contains(filter, true) }
    // 「已在组内」只按**目标分组**算（同站点 + 同密钥 + 同模型）：
    // 别的分组拉过同一个模型**不算**（目目 09-15「只要网址不一样、或同网址不同密钥，就能拉同样的模型」）
    fun inGroup(m: String) = targetIfc != null && KeyListFile.hasModel(keys, targetIfc, m)
    val selectableModels = visibleModels.filter { !inGroup(it) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                // 标题行：左＝标题，右＝「手动添加模型」入口（目目 09-15：从操作行挪到右上角，
                // 操作行只留「拉取」；[ready] 守卫照旧——没网址+密钥就不知道该归给谁，灰着）
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.role_key_fetch),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { manualVisible = true }, enabled = ready) {
                        Text(
                            "＋ " + stringResource(R.string.role_key_manual_model),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (forGroup) {
                    // 分组模式：不再让你选一次，直接把「给谁拉」摆出来（组名 · 网址）
                    Text(
                        stringResource(
                            R.string.role_key_pull_group_sub,
                            initialIfc?.name.orEmpty(), initialIfc?.baseUrl.orEmpty()
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                } else {
                    // 新建模式（目目 09-15「顶部就让填这两个值」）：只两个字段——接口 URL + API Key。
                    // 组名不用你起，按网址短名自动生成（想改去组头 ✏️）。
                    Text(
                        stringResource(R.string.role_key_ifc_url),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // 地址可能很长，一栏放不下就换行（目目 09-15）
                    OutlinedTextField(
                        value = urlText, onValueChange = { urlText = it },
                        singleLine = false, minLines = 1, maxLines = 3,
                        placeholder = { Text("https://api.example.com/v1") },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.role_key_ifc_key),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = keyText, onValueChange = { keyText = it },
                        singleLine = false, minLines = 1, maxLines = 3,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // 网址 + 密钥命中已有分组 ⇒ 这批并进去，不是另起一个（摆出来，不让你猜）
                    targetIfc?.let { hit ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.role_key_pull_group_sub, hit.name, hit.baseUrl),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                // 操作行只剩「拉取」（分组模式=重试；新建模式=首次拉取）；手动添加已挪到标题行右上角
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { fetch() }, enabled = !loading && ready) {
                        Text(stringResource(if (loading) R.string.role_key_fetching else R.string.role_key_fetch))
                    }
                    if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
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
                        // 全选只作用**可见且可加**的（组内已有的、被搜索滤掉的不算）
                        selected = if (selectableModels.any { it in selected }) {
                            selected - selectableModels.toSet()
                        } else {
                            selected + selectableModels.toSet()
                        }
                    }) {
                        Text(
                            if (selectableModels.any { it in selected }) stringResource(R.string.role_select_all_cancel)
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
                                val already = inGroup(m)
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clickable(enabled = !already) {
                                            selected = if (m in selected) selected - m else selected + m
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = m in selected,
                                        enabled = !already,
                                        onCheckedChange = { selected = if (it) selected + m else selected - m }
                                    )
                                    Text(
                                        m,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (already) MaterialTheme.colorScheme.onSurfaceVariant
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (already) {
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.role_key_model_in_group),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                // 页脚：确认时**不在这里写条目**，只把（网址 + 密钥 + 选中的模型）交回上层——
                // 由 [KeyListFile.ensureGroup] 定夺并入哪个分组 / 是否新建，条目名与 value 也在那一步生成
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(
                        enabled = selected.isNotEmpty() && ready,
                        onClick = {
                            val u = targetIfc?.baseUrl ?: url
                            val k = targetIfc?.apiKey ?: key
                            onConfirm(u, k, selected.sorted())
                        }
                    ) {
                        Text(stringResource(R.string.role_key_add_selected, selected.size))
                    }
                }
            }
        }
    }
    // 手动添加模型（照插件 showManualModelDialog：输入模型名直接入库）。
    // 归属由（网址 + 密钥）决定：分组模式=本组；新建模式=你填的那对值（[ready] 已保证两项都非空）。
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
                    enabled = manual.trim().isNotEmpty() && ready,
                    onClick = {
                        val m = manual.trim()
                        manualVisible = false
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
    var pending by remember { mutableStateOf<Pair<String, KeyListFile.ExportData>?>(null) }
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
                Text(stringResource(R.string.role_key_import), style = MaterialTheme.typography.headlineSmall)
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
    pending?.let { (fn, data) ->
        var counts by remember(fn) { mutableStateOf(0 to 0) }
        LaunchedEffect(fn) {
            val names = withIO { KeyListFile.readKeys(tagRuleId).map { it.name }.toSet() }
            counts = data.keys.count { it.name !in names } to data.keys.count { it.name in names }
        }
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.role_key_import_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.role_key_import_confirm,
                        fn, data.keys.size, counts.first, counts.second, data.interfaces.size
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    scope.launch {
                        // v2 含分组 → importAll（密钥合并 + 接口按站点+密钥去重合并）；
                        // 插件时代的扁平数组文件 interfaces 为空，等价于原来的 importKeys。
                        val (added, skipped, addedIfc) = withIO { KeyListFile.importAll(tagRuleId, data) }
                        toast(R.string.role_key_import_done, added, skipped, addedIfc)
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
                    style = MaterialTheme.typography.headlineSmall
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
                                    // 统一入口（照插件 restoreFromText）：①书名补进 liebiao.json
                                    // ②cunfang ③characterRecords ④shuming.<书> ⑤gengxin.json。
                                    // ⚠️ 旧版只写 3 个文件：书切走再回来会从书架消失；不写 gengxin.json
                                    //    则朗读规则内存仍是旧角色表，下次朗读把导入数据覆盖回去（导入白做）。
                                    //    那段还硬编码了绝对路径，没走 BASE_DIR。
                                    CharacterRecordsFile.importBook(tagRuleId, bookName, data.toString())
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
        Text(text, style = MaterialTheme.typography.bodyMedium)
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
                    style = MaterialTheme.typography.headlineSmall,
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
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = { multiVisible = true }) {
                        Text(
                            stringResource(R.string.role_book_multi_delete_mode),
                            color = BOOK_MULTI_DELETE_COLOR,
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
                        style = MaterialTheme.typography.headlineSmall,
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
                                    style = MaterialTheme.typography.bodyMedium,
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
            style = MaterialTheme.typography.bodyMedium,
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
