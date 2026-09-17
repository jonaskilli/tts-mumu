package com.github.jing332.tts_server_android.compose.systts.role

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
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
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * 密钥管理页（KeyManagerActivity）：接口分组密钥列表 + 备份恢复 + 书籍管理。
 * 分组 = 一个接口一张卡；条目两行 = 显示名/当前徽章 + 动作图标居右。
 * 当前密钥用状态点 + scheme.secondary 强调（primary 各主题太淡，染了看不出）。
 */

/** 分组后的密钥组（照插件 buildKeyGroups：接口组 + 未分组 + 直连密钥） */
private class KeyGroup(
    val title: String,
    val entries: List<KeyListFile.KeyEntry>,
    val ifc: KeyListFile.ApiInterface? = null,
    /** 未分组 / 直连组的身份说明（接口组此位置显示网址） */
    val hintRes: Int? = null,
)

private fun buildKeyGroups(keys: List<KeyListFile.KeyEntry>, ifaces: List<KeyListFile.ApiInterface>): List<KeyGroup> {
    val groups = mutableListOf<KeyGroup>()
    val assigned = mutableSetOf<String>()
        ifaces.forEach { ifc ->
            val entries = keys.filter { KeyListFile.keyBelongsTo(it, ifc) }
            // 建组即渲染：空分组也显示，否则建完组页面不出现、点不到「拉取模型」
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

/**
 * 裸文本动作键（全选 / 取消 / 删除不要外框）。
 *
 * 原先照插件 createSmallButton 用「透明底 + 彩色描边」小 chip，三个并排像三枚胶囊，
 * 与本页其余无框动作（组头图标区、条目行名字）语言不统一；去描边后只留文字 + 36dp 热区，
 * 红色删除键靠颜色本身表意（不再靠框）。左右内边距由本函数给，调用侧只用 Spacer 控间距。
 */
@Composable
private fun FlatTextAction(text: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

/**
 * 测试通过的绿点：M3 没有 success 槽，也不能跟随主题走（红主题下「通过」会变红），
 * 只能用固定语义绿——这里的固定是有意的，别顺手换成 colorScheme。
 */
private val TEST_PASS_COLOR = Color(0xFF2E7D32)

/** 扁平图标动作：无描边无底色，18dp onSurfaceVariant 灰、36dp 热区；删除模式随组头转红 */
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
 * 密钥条目 = 一张卡片（**排布一行不动**，只把每个模型包成卡片）：
 * 状态点 + 显示名 +「当前」徽章 + 动作图标 ⚡⧉✏🗑 同行居右；显示名放不下自己换行。
 *
 * ElevatedCard 照主界面 Item.kt:113 同款（M3 默认 surfaceContainerLow 底 + 1dp 阴影）；
 * 组卡已撤（组头裸排），本卡是页面唯一容器层，阴影负责把卡片从页面底上顶出来。
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
    // 勾选反馈（原先整行毫无变化、只有小方块在动，看着像设置列表不像多选）——
    // 勾中整卡染 8% error 浅红，与左侧复选框一起给出「这条被选走了」。
    // compositeOver：底色近似半透明红叠在卡面上，避免半透明直接给 ElevatedCard 透出页面底色
    val cardColor = if (deleteMode && checked)
        MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
            .compositeOver(MaterialTheme.colorScheme.surfaceContainerLow)
    else MaterialTheme.colorScheme.surfaceContainerLow

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
        // 缩进 = 归属关系：左缘 15dp 与组头折叠箭头同列、右缘 6dp 与组头图标区
        // 同列——上版卡左缘在 6dp（与色条同列），比组头内容还靠左，看着像与组头平级的另一行；
        // 缩进后卡片明确挂在组头之下。上下 3 ⇒ 相邻两张卡之间 6dp
        // start/end 与 vertical 分属不同 padding 重载，写在一起没有匹配的候选，故分两次
        modifier = Modifier.fillMaxWidth()
            .padding(start = 15.dp, end = 6.dp)
            .padding(vertical = 3.dp)
    ) {
        Row(
            // 卡内 9 ⇒ 状态点左缘 24dp（卡左缘 15 + 9），与「删除密钥」标题左缘同列
            Modifier.fillMaxWidth()
                .padding(start = 9.dp, end = 9.dp, top = 8.dp, bottom = 8.dp),
            // 名字换行成两行时图标垂直居中，不再用 Top 咬行
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (deleteMode) {
                Checkbox(checked = checked, onCheckedChange = { onToggleCheck() })
            }
            // 删除模式不显示状态点（☐ 旁边再跟个 ○ 像两组选择圈打架，纯粹干扰；
            // 「当前」徽章仍在名字后保留）
            if (!deleteMode) {
                // 状态点固定 14dp 位宽、24dp 高（对齐 bodyMedium 行高）
                Box(Modifier.width(14.dp).height(24.dp), contentAlignment = Alignment.CenterStart) {
                    val dot = when {
                        // 当前密钥 = 组头色条同款 primary（跟小竖线一个颜色）；
                        // 名字与「当前」徽章仍走 accent(secondary)，实机看着别扭再统一
                        isCurrent -> MaterialTheme.colorScheme.primary
                        testOk == true -> TEST_PASS_COLOR
                        testOk == false -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                    if (isCurrent || testOk != null) {
                        Box(Modifier.size(8.dp).background(dot, CircleShape))
                    } else {
                        // 未测 = 空心圆环，和「测过但红/绿」区分开
                        Box(Modifier.size(8.dp).border(1.dp, dot, CircleShape))
                    }
                }
                Spacer(Modifier.width(8.dp))
            }
            // 名字区 weight(1f)：短名贴左、「当前」徽章跟在名字后，右侧空隙由固定图标区兜底
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 显示名取**值里的真实模型名**（条目名可能带跨组共存的去重后缀，那是内部标识）。
                // weight(fill=false)：短名贴左侧，长名吃满剩余宽度后省略，图标不被挤出去
                Text(
                    KeyListFile.displayName(entry),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else null,
                    color = if (isCurrent) accent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    // 删除模式下点名字也算勾选（整行即复选框的延伸，不必非要点中那个小方块）
                    modifier = Modifier.weight(1f, fill = false)
                        .clickable { if (deleteMode) onToggleCheck() else onSwitch() }
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
                // 固定宽图标区（方案 A）：144dp=4×36dp 热区，与组头行图标垂直成列；
                // 动作图标按使用频次（方案一）：⚡测试 ⧉复制 ✏编辑 🗑删除
                //（📋 复制的是模型名，编辑弹窗里才是完整密钥串）
                Row(
                    Modifier.width(144.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FlatIconAction(Icons.Default.Bolt, stringResource(R.string.role_key_test)) { onTest() }
                    FlatIconAction(Icons.Default.ContentCopy, stringResource(R.string.copy)) { onCopy() }
                    FlatIconAction(Icons.Default.Edit, stringResource(R.string.role_key_edit)) { onEdit() }
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
    // 列表行 📋 复制模型名用
    val clipboard = LocalClipboardManager.current
    var version by remember { mutableIntStateOf(0) }
    var keys by remember { mutableStateOf<List<KeyListFile.KeyEntry>>(emptyList()) }
    var ifaces by remember { mutableStateOf<List<KeyListFile.ApiInterface>>(emptyList()) }
    var currentRaw by remember { mutableStateOf("") }
    // 测试结果记忆（条目名 → 通/不通）
    var testResults by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    // 组折叠状态
    var collapsed by remember { mutableStateOf<Set<String>>(emptySet()) }
    // 测试中（单条 / 整组批量）
    var testingName by remember { mutableStateOf<String?>(null) }
    var testingGroup by remember { mutableStateOf<String?>(null) }
    // 删除选择模式（照插件 deleteMode）：组头变红字 + 全选/取消/删除(N)
    var deleteModeGroup by remember { mutableStateOf<String?>(null) }
    var deleteChecked by remember { mutableStateOf<Set<String>>(emptySet()) }
    var deleteConfirmGroup by remember { mutableStateOf<String?>(null) }
    var menuGroup by remember { mutableStateOf<String?>(null) }          // 组头 🗑 展开的两项菜单
    var deleteGroupConfirm by remember { mutableStateOf<String?>(null) } // 「删除整组」二次确认

    LaunchedEffect(version) {
        // 分组自愈：匹配不上分组的 @@ 条目按（网址 + 密钥）自动建组
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
            // 传原始值：@@串走对话端点、纯 Key 走智谱 /models（照插件 testModelKey）
            val r = withIO { KeyListFile.testKey(entry.value) }
            testingName = null
            testResults = testResults + (entry.name to r.first)
            toast(
                if (r.first) R.string.role_key_test_ok_toast else R.string.role_key_test_fail_toast,
                KeyListFile.displayName(entry), r.second
            )
        }
    }
    // 整组测试并发 4 路（原为串行）。限 4：同站点共用额度，全发会撞 429、被记成「测试失败」；
    // withIO 是真并行、testKey 纯阻塞且每次新建连接 ⇒ 并发安全，灯谁先回谁先亮
    fun testGroup(grp: KeyGroup) {
        val targets = grp.entries.filter { it.value.isNotBlank() }
        if (targets.isEmpty()) {
            toast(R.string.role_key_test_none)
            return
        }
        scope.launch {
            testingGroup = grp.title
            toast(R.string.role_key_test_batch_start, grp.title, targets.size)
            val gate = Semaphore(4)
            val results = targets.map { e ->
                async {
                    gate.withPermit {
                        val r = withIO { KeyListFile.testKey(e.value) }
                        testResults = testResults + (e.name to r.first)
                        r.first
                    }
                }
            }.awaitAll()
            testingGroup = null
            val okCount = results.count { it }
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

    // 删除整组：接口组连 api_center.json 的接口一起删；未分组 / 直连组只删条目
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

    // 独立全屏页（照替换管理 / 插件管理模式）：返回键退页，导入/导出在顶栏
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
                // 导入/导出：FileDownload/FileUpload 单色图标 + 文字；热区 ≥48dp（IconButton 最小宽会挤标题）
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
                // 操作行：一主一次——「拉取模型」是主路径（建分组必经），用 FilledTonalButton 强调，
                // 「新增密钥」保持描边。不用实心 Button：各主题 primary 是 *_seed，
                // 橙/pink 主题上白字对比不足，tonal 走 primaryContainer 由 M3 保证对比度。
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
                    FilledTonalButton(
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
                    // 当前密钥强调色用 scheme.secondary：primary 是各主题的 *_seed，浅底染了看不出
                    val accent = MaterialTheme.colorScheme.secondary
                    groups.forEach { grp ->
                        val isCollapsed = grp.title in collapsed
                        val isDeleting = deleteModeGroup == grp.title
                        val grpHasCurrent = currentRaw.isNotEmpty() &&
                                grp.entries.any { it.value.trim() == currentRaw }
                        val selCount = grp.entries.count { it.name in deleteChecked }
                        // 组不做容器（照主界面 GroupItem.kt:98：组头 background(surface) 裸排、层级靠排版）。
                        // 旧版组卡 surfaceContainerLow 比页面底只暗 4/255、包裹根本不成形，反而把
                        // surfaceContainerLowest 的条目卡衬成全页最亮的「白条」（质疑后查实）。
                        // 条目 ElevatedCard 成为页面唯一容器层；归属感靠组头排版 + 组间 16dp 间距表达。
                        // （内层 Column 保留原缩进壳，避免整块内容平移缩进）
                        Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                            Column(Modifier.padding(vertical = 4.dp)) {
                                // ———— 组头 + 元信息行 ————
                                // 色条只跟**组头行**同高（二改）：上版拉长到元信息行、
                                // 用 IntrinsicSize.Min 让色条撑满两行，结果折叠箭头随两行高度垂直居中、
                                // 从「组名左侧」坠到两行中间——箭头锚的是组名，不是整块。
                                // 现把色条放回组头行内（3×16dp），箭头与组名恢复同行居中
                                Column(Modifier.fillMaxWidth()) {
                                    // ———— 组头 ————（删除模式不渲染：动作行挪到卡片底部，见条目之后）
                                    Row(
                                        Modifier.fillMaxWidth()
                                            .padding(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (!isDeleting) {
                                            // 组头色条（09-15 定、09-16 二改回本行高）：只有本组含
                                            // 当前使用中的密钥才亮——色条=「这个组正在用」的信号，不是装饰。
                                            // 16dp ≈ 组名文字高，随箭头同行居中；不亮时留等宽空位（3+6=9dp），
                                            // 组名与箭头位置不漂。放在可点区**外**：点色条不该触发折叠
                                            Box(
                                                Modifier
                                                    .width(3.dp)
                                                    .height(16.dp)
                                                    .background(
                                                        if (grpHasCurrent) MaterialTheme.colorScheme.primary
                                                        else Color.Transparent,
                                                        RoundedCornerShape(2.dp)
                                                    )
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            // 组头可点区：折叠箭头 + 组名 + (N)；「本组含当前密钥」由左侧色条承担
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
                                                    overflow = TextOverflow.Ellipsis,
                                                    // weight(fill=false)：组名超长时吃满剩余宽度后省略，
                                                    // 没有它长组名会把后面的 (N) 挤成一字宽、逐字竖排
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    "(${grp.entries.size})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                // 「本组含当前密钥」不再跟 ✓（多余），
                                                // 信号由组头左侧色条单独承担
                                            }
                                            // 固定宽图标区（方案 A）：144dp=4×36dp 热区，组头与
                                            // 模型行两行图标垂直成列；不足 4 键（未分组/直连组）右对齐留空
                                            Row(
                                                Modifier.width(144.dp),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // 组级四图标全部常驻；仅接口组有前三个（未分组 / 直连点了只是白弹提示）。
                                                // 顺序按使用频次（方案一）：+拉取 ⚡测组 ✏编辑接口 🗑删除
                                                grp.ifc?.let { ifc ->
                                                    FlatIconAction(
                                                        // 拉取模型 = 往组里加模型（三轮：Sync 云同步也别扭，
                                                        // 改 +「添加」语义；放大镜易与页内搜索混淆，弃）
                                                        Icons.Default.Add,
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
                                                    FlatIconAction(
                                                        Icons.Default.Edit,
                                                        stringResource(R.string.role_key_interface_edit)
                                                    ) { ifcFormFor = ifc }
                                                }
                                                // 组头 🗑 展开两项：删除整组 / 多选删除子项
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
                                                            // 警示交给红色图标承载，标题不再整行红字（原样太扎眼）
                                                            leadingIcon = {
                                                                Icon(
                                                                    Icons.Default.DeleteOutline,
                                                                    contentDescription = null,
                                                                    modifier = Modifier.size(18.dp),
                                                                    tint = MaterialTheme.colorScheme.error
                                                                )
                                                            },
                                                            text = {
                                                                Column {
                                                                    Text(
                                                                        stringResource(R.string.role_key_group_delete_all),
                                                                        style = MaterialTheme.typography.bodyMedium
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
                                                            // 两项各带一枚 18dp 前置图标，文字左缘才对得齐：
                                                            // 删除整组=红🗑（警示），多选删除=灰🧹（组保留、非毁灭）
                                                            leadingIcon = {
                                                                Icon(
                                                                    Icons.Default.DeleteSweep,
                                                                    contentDescription = null,
                                                                    modifier = Modifier.size(18.dp),
                                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            },
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
                                            } // 固定宽图标区收尾
                                        }
                                    }
                                    // 元信息行：接口组 = 网址 + 尾号小块；未分组 / 直连组 = 一句身份说明
                                    if (!isDeleting) {
                                        val ifc = grp.ifc
                                        // 元信息行左缘 = 组名文字左缘（43）：卡左缘 15 + 折叠箭头 22 + 间距 6。
                                        // 原先是 34（09-14 与组名同列），09-16 给组头加色条把组名推到 43 之后
                                        // 这行落了单，于是组头两行文字各站一条竖线——现在共线，且零宽度代价
                                        // （这行右边只有一个尾号小块）。不改的话卡片缩进方案又得压模型名宽度。
                                        if (ifc != null) {
                                            Row(
                                                Modifier.fillMaxWidth()
                                                    .padding(start = 43.dp, end = 10.dp, bottom = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    ifc.baseUrl,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    // 网址放不下换行（原单行省略号会吃掉长网址）
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                // 尾号独立小块（原先挤在网址尾巴上，网址一长就被省略号吃掉）
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
                                                    // 与网址分支同列（43），两分支是同一个元素的两个形态
                                                    modifier = Modifier.padding(start = 43.dp, end = 10.dp, bottom = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (isDeleting) {
                                    // 删除模式标题行（定稿）：标题回到标题位（09-15 从底部
                                    // 挪回来是对的，但上次端的是 24sp 弹窗标题架子，压得卡片头重），
                                    // 现在降到 16sp 与右端「全选」共一行；底部只留「取消 / 删除(N)」。
                                    // 于是动作分两条带：顶部选谁、底部执行或退出，视线不在卡片里跑两趟。
                                    // 左右缩进对齐条目卡内容：24 = 卡左缘 15 + 卡内 9；end 5 让「全选」文字
                                    // 右缘落在 15dp（= 5 + FlatTextAction 自带 10dp），与条目图标成列
                                    Row(
                                        Modifier.fillMaxWidth()
                                            .padding(start = 24.dp, end = 5.dp, top = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            stringResource(R.string.role_key_delete_title),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        FlatTextAction(
                                            stringResource(R.string.select_all),
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        ) {
                                            val allSel = grp.entries.all { it.name in deleteChecked }
                                            val names = grp.entries.map { it.name }.toSet()
                                            deleteChecked =
                                                if (allSel) deleteChecked - names else deleteChecked + names
                                        }
                                    }
                                }
                                if (!isCollapsed) {
                                    // 条目各自成卡：条目间的浅分隔线随之删掉，
                                    // 间距改由卡片自身的 3dp 上下外边距给出（相邻两张之间 6dp）
                                    grp.entries.forEach { entry ->
                                        val isCurrent = currentRaw.isNotEmpty() &&
                                                entry.value.trim() == currentRaw
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
                                            // 📋 列表行复制模型名（编辑弹窗里复制的才是完整密钥串）
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
                                // ———— 删除模式动作行（卡片底部）：只有 取消 / 删除(N) ————
                                // 全选已并到顶部标题行，此处不再重复；两个键都是
                                // 无框文字键，下方留 4dp 让红键不贴着卡片圆角；end 5 同标题行的「全选」
                                if (isDeleting) {
                                    Row(
                                        Modifier.fillMaxWidth()
                                            .padding(start = 24.dp, end = 5.dp, top = 2.dp, bottom = 4.dp),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FlatTextAction(
                                            stringResource(R.string.cancel),
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        ) {
                                            deleteModeGroup = null
                                            deleteChecked = emptySet()
                                        }
                                        FlatTextAction(
                                            stringResource(R.string.role_key_delete_n, selCount),
                                            MaterialTheme.colorScheme.error
                                        ) {
                                            if (selCount == 0) toast(R.string.role_key_delete_none)
                                            else deleteConfirmGroup = grp.title
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    // 批量删除确认（组内选中 N 条 → 二次确认）
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
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmGroup = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 删除整组确认（显示将删条数）
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
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteGroupConfirm = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 新增（名称留空自动生成；同组重名 → 覆盖确认，跨组撞名 → 加序号另存）
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
                    // 照插件：只有当前密钥为空时才把它设为当前
                    // ⚠️ 旧版无条件三写 miyue ⇒ 新增 B 会把正在朗读用的密钥静默切走
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
    // 改名（重名 → 拒绝并提示，照插件 showKeyNameDialog）
    renameFor?.let { entry ->
        KeyEditDialog(
            initial = entry,
            existing = keys,
            onDismiss = { renameFor = null },
            onDelete = { renameFor = null; deleteFor = entry },
            onConfirm = { name, value, overwrite ->
                renameFor = null
                if (overwrite && name != entry.name) {
                    // 改成已存在的名字 → 拒绝（照插件密钥详情页「保存」）
                    // ⚠️ 旧版走覆盖分支：只覆盖同名条目、旧名条目没删 ⇒ 列表里两条并存
                    toast(R.string.role_key_name_dup, name)
                } else {
                    save(keys.map { if (it.name == entry.name) it.copy(name = name, value = value) else it })
                    // 改的是当前密钥 → 同步 miyue 三写
                    // ⚠️ 旧版只写 key_list.json ⇒ miyue 留旧值，重进页面被兜底切到第一条
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
                    // 覆盖的是当前密钥 → 同步 miyue；否则照「仅当前为空才启用」
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
                    // 与批量删除同一条路（照插件 deleteMultipleBooks：删当前密钥后切到剩余第一条）
                    // ⚠️ 旧版只 save(filter)，miyue 仍指向已删的 key，重进页面被兜底切走
                    deleteNames(listOf(entry.name))
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteFor = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    // 接口表单（照插件 showInterfaceFormDialog）：名称 / 地址 / Key + 校验 + 🗑级联删除；
    // 「新建接口」入口已并进拉取弹窗（ensureGroup 自动建档），这里只剩编辑
    ifcFormFor?.let { ifc ->
        InterfaceFormDialog(
            tagRuleId = tagRuleId,
            initial = ifc,
            onDismiss = { ifcFormFor = null },
            onSaved = { ifcFormFor = null; version++ },
            onRefresh = { version++ },
        )
    }
    // 拉取模型：顶部 = 填网址+Key 建组/并入；分组卡 🔍 = 给本组拉。两入口共用，落库走 ensureGroup
    if (showPullModels) {
        ModelPullDialog(
            keys = keys,
            ifaces = ifaces,
            initialIfcName = pullForIfc,
            onDismiss = { showPullModels = false; pullForIfc = null },
            onConfirm = { url, apiKey, groupName, pickedModels ->
                showPullModels = false
                pullForIfc = null
                scope.launch {
                    // groupName 非空 = 用户手填（撞名已被弹窗拦下）；空 = 按网址短名自动提取
                    val ifc = withIO { KeyListFile.ensureGroup(tagRuleId, url, apiKey, groupName) }
                    if (ifc == null) {
                        toast(R.string.role_list_failed)
                    } else {
                        // 名字逐个占位去重（跨组重名只加序号）
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
                        // 组内去重：同（站点 + 密钥 + 模型）已在组里 → 不再多出一条
                        val toAdd = entries.filterNot { e ->
                            val p = KeyListFile.parseKeyValue(e.value)
                            p != null && !p.isDirect && KeyListFile.hasModel(keys, ifc, p.model)
                        }
                        val merged = toAdd.fold(keys) { acc, e ->
                            acc + e.copy(keyCode = KeyListFile.nextKeyCode(acc))
                        }
                        withIO {
                            if (toAdd.isNotEmpty()) KeyListFile.saveKeys(tagRuleId, merged)
                            // 拉到的模型登记进分组 models
                            KeyListFile.addModelsToInterface(tagRuleId, ifc.name, pickedModels)
                        }
                        toast(R.string.role_key_pull_done, toAdd.size, entries.size - toAdd.size)
                    }
                    version++
                }
            }
        )
    }
    // 导入（密钥备份_*.json / 密钥导出_*.json，照插件 importKeysDialog）
    if (showImport) {
        ImportKeysDialog(
            tagRuleId = tagRuleId,
            onDismiss = { showImport = false },
            onDone = { showImport = false; version++ },
        )
    }
}

/**
 * 密钥编辑（条目 ✏️ / 新增共用）：第一行「模型」（留空从密钥串抽模型名）、
 * 第二行「密钥」（整串 网址@@模型@@Key）；底部 取消 / 删除 / 复制 / 确定。
 * 两处复制不同：列表行 📋 = 模型名，本弹窗「复制」= 完整密钥串。
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
    // 预填光标落末尾（照插件 setSelection）；「模型」框预填显示名，不是内部条目名
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
                // 第一行「模型」：留空则从密钥串里抽模型名，抽不出用 key01
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
                // 第二行「密钥」（整串 网址@@模型名@@API key；智谱可直填裸 key）
                Text(
                    stringResource(R.string.role_key_value),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 密钥串很长，允许多行（原 singleLine 会把中段吞掉）
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
            // 底部四键定位：M3 按钮区按「dismiss 槽 → confirm 槽」排，拆两槽即得 取消/删除 + 复制/确定
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 「复制」= 第二行的完整密钥串（列表行 📋 复制的是模型名）
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
                        // 留空自动生成（照插件 defaultName）：从密钥串里抽模型名，抽不出用 key01
                        val auto = KeyListFile.parseKeyValue(raw)?.let { p ->
                            if (!p.isDirect && p.model.isNotEmpty()) p.model else "key" + (existing.size + 1)
                        } ?: "key" + (existing.size + 1)
                        val wanted = name.text.trim().ifEmpty { auto }
                        // 编辑态模型名没动 ⇒ 保留条目内部名、不触发改名判定（跨组共存时易误报「名称已存在」）
                        if (initial != null && (wanted == initName || wanted == initial.name)) {
                            onConfirm(initial.name, raw, false)
                            return@TextButton
                        }
                        val clash = existing.firstOrNull { it.name == wanted && it.name != initial?.name }
                        // 撞名判定：新增时只有同一分组（同网址 + 同密钥）才算真重复 → 覆盖确认；
                        // 跨组撞名加序号另存；改名照插件「名字被占就用不了」
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
                // 删除入口留在编辑弹窗内（红字）
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
    // 只刷新外面列表、不关本弹窗（手动加模型用）
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 新建时名称框随网址自动填短名；人改过的不覆盖；预填值光标落末尾
    var name by remember {
        val init = initial?.name.orEmpty()
        mutableStateOf(TextFieldValue(init, TextRange(init.length)))
    }
    // 名称被手改过就不再被网址覆盖；编辑已有分组时视为已改
    var nameTouched by remember { mutableStateOf(initial != null) }
    // 地址框用 TextFieldValue：失焦补协议头、预填都要把光标挪到末尾
    var url by remember {
        val init = initial?.baseUrl.orEmpty()
        mutableStateOf(TextFieldValue(init, TextRange(init.length)))
    }
    var key by remember { mutableStateOf(initial?.apiKey.orEmpty()) }
    // 手动加模型：点确定直接落库（拉取弹窗里那个只进候选列表）
    var addModelVisible by remember { mutableStateOf(false) }
    var addModelText by remember { mutableStateOf("") }
    fun toast(resId: Int, vararg args: Any) {
        android.widget.Toast.makeText(context, context.getString(resId, *args), android.widget.Toast.LENGTH_SHORT).show()
    }

    // 保存：名称留空按网址短名兜底；改网址/密钥时同步改写组内条目的 value
    // ⚠️ 只改 api_center.json 不改条目 ⇒ 旧条目仍指旧地址，整组掉进「未分组」
    fun doSave() {
        val u = KeyListFile.normalizeBaseUrl(url.text.trim())
        val k = key.trim()
        val n = name.text.trim().ifEmpty {
            runCatching { KeyListFile.shortName(u) }.getOrDefault("")
        }
        if (n.isEmpty()) { toast(R.string.role_key_ifc_name_empty); return }
        if (!u.startsWith("http")) { toast(R.string.role_key_ifc_url_bad); return }
        if (k.isEmpty()) { toast(R.string.role_key_ifc_key_empty); return }
        scope.launch {
            val ifaces = withIO { KeyListFile.readInterfaces(tagRuleId) }
            // 不许改成别的接口已有的名字
            val dup = ifaces.any { it.name == n && (initial == null || it.name != initial.name) }
            if (dup) { toast(R.string.role_key_ifc_name_dup); return@launch }
            // 改成与另一个接口完全同组（同网址 + 同密钥）⇒ 拦住报错，不静默合并
            val collide = ifaces.firstOrNull {
                it.name != (initial?.name ?: "") &&
                    KeyListFile.sameApiSite(it.baseUrl, u) && it.apiKey.trim() == k
            }
            if (collide != null) { toast(R.string.role_key_ifc_group_dup, collide.name); return@launch }
            val updated = if (initial == null) {
                ifaces + KeyListFile.ApiInterface(n, u, k, emptyList())
            } else {
                ifaces.map { if (it.name == initial.name) KeyListFile.ApiInterface(n, u, k, it.models) else it }
            }
            withIO { KeyListFile.saveInterfaces(tagRuleId, updated) }
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

    // 删除该接口和它下面的密钥条目
    fun doDelete() {
        val cur = initial ?: return
        scope.launch {
            val (kept, removed) = withIO {
                KeyListFile.deleteInterfaceCascade(tagRuleId, cur, KeyListFile.readKeys(tagRuleId))
            }
            withIO { KeyListFile.saveKeys(tagRuleId, kept) }
            toast(R.string.role_key_ifc_deleted, removed)
            onSaved()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            // 标题行右上角 = 手动加模型；只在编辑已有分组时给（新组还没落盘，条目无处归属）
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(
                        if (initial == null) R.string.role_key_interface_new else R.string.role_key_interface_edit
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (initial != null) {
                    TextButton(onClick = { addModelText = ""; addModelVisible = true }) {
                        Text(
                            "＋ " + stringResource(R.string.role_key_manual_model),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
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
                    // 清空后灰字摆出将用的短名（替代原来标签里那句括号说明）
                    placeholder = {
                        Text(runCatching { KeyListFile.shortName(url.text) }.getOrDefault(""))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.role_key_ifc_url),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 地址可能很长，允许多行
                OutlinedTextField(
                    value = url,
                    onValueChange = { v ->
                        url = v
                        // 改地址时同步刷新名称（取短名）；人改过的不覆盖
                        if (!nameTouched || name.text.isBlank()) {
                            val s = KeyListFile.shortName(v.text)
                            name = TextFieldValue(s, TextRange(s.length))
                        }
                    },
                    singleLine = false, minLines = 1, maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                        // 失焦补协议头并回写（与拉取弹窗同口径）
                        .onFocusChanged { st ->
                            if (!st.isFocused) {
                                val fixed = KeyListFile.withScheme(url.text)
                                if (fixed != url.text) {
                                    url = TextFieldValue(fixed, TextRange(fixed.length))
                                }
                            }
                        },
                )
                // 未填显示填写提示、填了显示实际请求地址（两行互补）
                val previewBase = if (url.text.isBlank()) "" else
                    runCatching { KeyListFile.openAiBaseUrl(url.text.trim()) }.getOrDefault("")
                if (previewBase.isEmpty()) {
                    Text(
                        stringResource(R.string.role_key_ifc_url_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                } else {
                    Text(
                        stringResource(R.string.role_key_will_request, "$previewBase/models"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.role_key_ifc_key),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Key 可能很长，允许多行
                OutlinedTextField(
                    value = key, onValueChange = { key = it },
                    singleLine = false, minLines = 1, maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            // 删除靠左、取消/保存靠右（同一行）；删除字数多，窄屏靠收缩+省略号保证不换行
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (initial != null) {
                    TextButton(
                        onClick = { doDelete() },
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            stringResource(R.string.role_key_ifc_delete),
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Spacer(Modifier)
                }
                Row {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = { doSave() }) { Text(stringResource(R.string.confirm)) }
                }
            }
        }
    )
    // 手动加模型：填名字点确定即落库（建条目 + 登记 models）；加完不关编辑弹窗，只刷新外层列表
    if (addModelVisible) {
        val target = initial
        AlertDialog(
            onDismissRequest = { addModelVisible = false },
            title = { Text(stringResource(R.string.role_key_manual_model)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = addModelText, onValueChange = { addModelText = it },
                        label = { Text(stringResource(R.string.role_key_model_input_hint)) },
                        singleLine = true, textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (target != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.role_key_add_model_to, target.name),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = addModelText.trim().isNotEmpty() && target != null,
                    onClick = {
                        // 用局部非空变量接住再进协程（嵌套 lambda 里智能转换不稳）
                        val t = target ?: return@TextButton
                        val m = addModelText.trim()
                        scope.launch {
                            val r = withIO { KeyListFile.addManualModel(tagRuleId, t, m) }
                            addModelVisible = false
                            if (r.first) {
                                toast(R.string.role_key_saved)
                                onRefresh()
                            } else {
                                toast(
                                    if (r.second == "exist") R.string.role_key_model_exist
                                    else R.string.role_list_failed
                                )
                            }
                        }
                    }
                ) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { addModelVisible = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

/**
 * 拉取模型：两个入口共用——顶部 = 新建（填网址 + Key，确认时 ensureGroup 找组 / 建组）；
 * 分组卡 🔍 = 分组模式（进来即按该组网址+密钥自动拉）。手动添加模型在标题行右上角。
 * 「已在组内」只按目标分组算（同站点 + 同密钥 + 同模型），别的接口拉过同一模型照样能存。
 */
@Composable
private fun ModelPullDialog(
    keys: List<KeyListFile.KeyEntry>,
    ifaces: List<KeyListFile.ApiInterface>,
    initialIfcName: String? = null,
    onDismiss: () -> Unit,
    // onConfirm(网址, 密钥, 分组名, 选中的模型)；分组名留空 = 按网址短名自动提取
    onConfirm: (String, String, String, List<String>) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 分组模式取回接口本体；组已被删则退回新建模式
    val initialIfc = remember(ifaces, initialIfcName) {
        initialIfcName?.let { n -> ifaces.firstOrNull { it.name == n } }
    }
    val forGroup = initialIfc != null
    // 分组名框只有新建模式用（分组模式整框隐藏）
    var nameText by remember { mutableStateOf(TextFieldValue("")) }
    var urlText by remember { mutableStateOf(TextFieldValue(initialIfc?.baseUrl.orEmpty())) }
    var keyText by remember { mutableStateOf(TextFieldValue(initialIfc?.apiKey.orEmpty())) }
    var models by remember { mutableStateOf<List<String>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("") }
    var manualVisible by remember { mutableStateOf(false) }
    // 拉取成功后把表单折成一行摘要，把高度让给模型列表；点 ✏ 重新展开可改
    var formCollapsed by remember { mutableStateOf(false) }

    val url = urlText.text.trim()
    val key = keyText.text.trim()
    // 目标分组：分组模式=点进来的那个；新建模式=网址+密钥命中已有分组就并入
    val targetIfc = initialIfc ?: ifaces.firstOrNull {
        KeyListFile.sameApiSite(it.baseUrl, url) && it.apiKey.trim() == key
    }
    val ready = forGroup || (url.isNotEmpty() && key.isNotEmpty())
    // 分组名留空 ⇒ 网址短名；手填撞名不当场改字，预览行变红 + 确认时 Toast 拦下
    val autoName = remember(url) { runCatching { KeyListFile.shortName(url) }.getOrDefault("") }
    val typedName = nameText.text.trim()
    val finalName = typedName.ifEmpty { autoName }
    // ⚠️ 只在要新建组时判重名（命中已有分组时这个框用不上，别误拦）
    val nameTaken = targetIfc == null && typedName.isNotEmpty() && ifaces.any { it.name == typedName }

    fun fetch() {
        if (!ready) return
        val u = targetIfc?.baseUrl ?: url
        val k = targetIfc?.apiKey ?: key
        scope.launch {
            loading = true; error = ""
            val r = withIO { KeyListFile.fetchModels(u, k) }
            loading = false
            if (r.first == null) error = r.second
            else {
                models = r.first ?: emptyList()
                selected = emptySet() // 默认不勾选
                formCollapsed = true
            }
        }
    }
    // 分组模式进来就拉
    LaunchedEffect(Unit) { if (forGroup) fetch() }
    val visibleModels = models.filter { filter.isBlank() || it.contains(filter, true) }
    // 「已在组内」只按目标分组算（同站点 + 同密钥 + 同模型），别的分组拉过不算
    fun inGroup(m: String) = targetIfc != null && KeyListFile.hasModel(keys, targetIfc, m)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                // 标题行右上角 = 手动添加模型；操作行只留「拉取」。
                // 标题只留字段名（填写提示挪到各自字段标题后，标题后不带括号注记）
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.role_key_fetch),
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                // 表单只在「没拉到模型」时显示（拉取成功后表单直接收起，
                // 不折摘要行——分组名/地址/尾号那行删掉，要改就取消重开；分组模式无表单）
                if (!forGroup && !formCollapsed) {
                    // 新建模式：字段顺序 分组名 → 接口地址 → API Key
                    // 提示跟在字段标题后（不放标题下、不放弹窗底部）
                    Text(
                        stringResource(R.string.role_key_group_name_label) +
                            stringResource(R.string.role_key_group_name_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = nameText, onValueChange = { nameText = it },
                        singleLine = true,
                        // 空框时把将用的短名当占位显示；网址没填就空着，不写解释
                        placeholder = { Text(autoName) },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.role_key_ifc_url) +
                            stringResource(R.string.role_key_url_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // 地址可能很长，允许多行；框内不放示例（使用提示在标题下那行）
                    OutlinedTextField(
                        value = urlText, onValueChange = { urlText = it },
                        singleLine = false, minLines = 1, maxLines = 3,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                            // 失焦补协议头并回写（与编辑弹窗同口径）
                            .onFocusChanged { st ->
                                if (!st.isFocused) {
                                    val fixed = KeyListFile.withScheme(urlText.text)
                                    if (fixed != urlText.text) {
                                        urlText = TextFieldValue(fixed, TextRange(fixed.length))
                                    }
                                }
                            },
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
                    // 底部「请求 /models / 并入分组」预览行已删（没事儿别占地方）；
                    // 手填分组名撞车仍由确认键 Toast 拦下
                }
                Spacer(Modifier.height(4.dp))
                // 「拉取」按钮只在没拉到模型时显示（标题已有「拉取模型」四字，
                // 列表出来了按钮就多余）；loading 转圈也在这一行——分组模式首次拉取 / 失败重试都覆盖
                if (models.isEmpty()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { fetch() }, enabled = !loading && ready) {
                            Text(stringResource(if (loading) R.string.role_key_fetching else R.string.role_key_fetch))
                        }
                    }
                }
                if (error.isNotEmpty()) {
                    Text(
                        stringResource(R.string.role_key_fetch_fail, error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                // 搜索过滤（全选改到各分类标题行，见下方 LazyColumn）
                if (models.isNotEmpty()) {
                    OutlinedTextField(
                        value = filter,
                        onValueChange = { filter = it },
                        placeholder = { Text(stringResource(R.string.role_key_model_filter)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // 五类分组列表
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    val byCat = linkedMapOf<String, MutableList<String>>()
                    visibleModels.sorted().forEach { m ->
                        byCat.getOrPut(KeyListFile.classifyModel(m)) { mutableListOf() }.add(m)
                    }
                    byCat.forEach { (cat, list) ->
                        item(key = "cat_$cat") {
                            // 全选按分类（全局全选「不能把所有模型都选」不对头，
                            // 位置也浮在搜索框和列表之间没有归属）——挪进分类标题行，只作用本分类可加项
                            Row(
                                // 间距放整行（原来 top=8 只压在标题上，
                                // 标题被顶下去而全选按钮垂直居中，两截错位不像同一行）
                                Modifier.fillMaxWidth().padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "$cat (${list.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                val catSelectable = list.filter { !inGroup(it) }
                                TextButton(onClick = {
                                    selected = if (catSelectable.any { it in selected }) {
                                        selected - catSelectable.toSet()
                                    } else {
                                        selected + catSelectable.toSet()
                                    }
                                }) {
                                    Text(
                                        if (catSelectable.any { it in selected }) stringResource(R.string.role_select_all_cancel)
                                        else stringResource(R.string.role_list_select_all)
                                    )
                                }
                            }
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
                                        // 浅 primary 底徽章（与「当前」徽章同款式）：灰字扫一眼注意不到
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                        ) {
                                            Text(
                                                stringResource(R.string.role_key_model_in_group),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // 确认时不在这里写条目：只把（网址 + 密钥 + 选中模型 + 分组名）交回上层。
                // 取消/添加是「拉取之后」的事，没拉到模型前不占这行高度
                if (models.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                        TextButton(
                            enabled = selected.isNotEmpty() && ready,
                            onClick = {
                                // 手填分组名撞已有组 ⇒ 拦住确认（不当场改字，Toast 提示）
                                if (nameTaken) {
                                    android.widget.Toast.makeText(
                                        context,
                                        context.getString(R.string.role_key_group_name_exists, typedName),
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                    return@TextButton
                                }
                                val u = targetIfc?.baseUrl ?: url
                                val k = targetIfc?.apiKey ?: key
                                // 并入已有组 ⇒ 名字交空
                                onConfirm(u, k, if (targetIfc != null) "" else finalName, selected.sorted())
                            }
                        ) {
                            Text(stringResource(R.string.role_key_add_selected, selected.size))
                        }
                    }
                }
            }
        }
    }
    // 手动添加模型：输入模型名进候选列表（不直接入库）；归属由（网址 + 密钥）决定
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
    // 单选态：先勾文件再点「导入」，视觉自说明，不靠提示文案
    var picked by remember { mutableStateOf<String?>(null) }
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
                                // 单选行： RadioButton 给足「可选中」的视觉，行距拉开不再挤成一坨
                                Row(
                                    Modifier.fillMaxWidth()
                                        .heightIn(min = 44.dp)
                                        .clickable { picked = fn },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = picked == fn, onClick = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        fn,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                // 取消 / 导入 靠右（对齐 M3 弹窗按钮位）；导入先读文件，再进确认弹窗
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(
                        enabled = picked != null,
                        onClick = {
                            val fn = picked ?: return@TextButton
                            scope.launch {
                                val list = withIO { KeyListFile.readExportFile(tagRuleId, fn) }
                                if (list == null) toast(R.string.role_list_failed)
                                else pending = fn to list
                            }
                        }
                    ) { Text(stringResource(R.string.role_key_import)) }
                }
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
                        // v2 含分组 → importAll；插件时代的扁平数组 interfaces 为空，等价 importKeys
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
    // 备份概况与「还原前现场」都摆出来：否则你看不出这份备份是刚才的还是上个月的、有没有后悔药
    var backupInfo by remember { mutableStateOf<CharacterRecordsFile.BackupInfo?>(null) }
    var beforeExists by remember { mutableStateOf(false) }
    // 待确认动作："restore"（完整还原）/ "undo"（撤销上次还原）——两者都覆盖整目录，先问一句
    var confirmAction by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(version) {
        autoOn = withIO { CharacterRecordsFile.readAutoBackupEnabled(tagRuleId) }
        backupInfo = withIO { CharacterRecordsFile.readBackupInfo(tagRuleId) }
        beforeExists = withIO { CharacterRecordsFile.hasBeforeRestore(tagRuleId) }
    }
    fun toast(resId: Int, vararg args: Any) {
        android.widget.Toast.makeText(
            context, context.getString(resId, *args), android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    // 完整还原 / 撤销还原共用：只是数据源不同（fullBackup.json vs fullBackup.before.json）
    fun runRestore(fromBefore: Boolean) {
        scope.launch {
            val n = withIO {
                if (fromBefore) CharacterRecordsFile.restoreFromBefore(tagRuleId)
                else CharacterRecordsFile.restoreAllFiles(tagRuleId)
            }
            toast(
                when {
                    n < 0 -> R.string.backup_none
                    n > 0 -> if (fromBefore) R.string.backup_undo_done else R.string.backup_restore_done
                    else -> R.string.role_list_failed
                }, n
            )
            beforeExists = withIO { CharacterRecordsFile.hasBeforeRestore(tagRuleId) }
            if (n > 0) onRestored()
        }
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
                BackupOptionRow(
                    Icons.Default.Save,
                    stringResource(R.string.backup_export_all),
                    // 摆出这份备份是什么时候的、装了几个文件——只有一份、看不见时间就等于闭眼点
                    subtitle = backupInfo?.let { info ->
                        stringResource(
                            R.string.backup_last_time,
                            info.time.ifBlank { stringResource(R.string.backup_time_unknown) },
                            info.fileCount
                        )
                    } ?: stringResource(R.string.backup_none_yet)
                ) {
                    scope.launch {
                        val n = withIO { CharacterRecordsFile.backupAllFiles(tagRuleId) }
                        backupInfo = withIO { CharacterRecordsFile.readBackupInfo(tagRuleId) }
                        toast(if (n > 0) R.string.backup_done else R.string.role_list_failed, n)
                    }
                }
                BackupOptionRow(Icons.Default.Restore, stringResource(R.string.backup_restore_all)) {
                    confirmAction = "restore"
                }
                // 撤销入口只在真有现场可退时才摆出来（还原过一次之后才有）
                if (beforeExists) {
                    BackupOptionRow(
                        Icons.AutoMirrored.Filled.Undo,
                        stringResource(R.string.backup_undo_restore)
                    ) { confirmAction = "undo" }
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
    // 覆盖整目录前的确认：把「这份备份是什么时候的」「有后悔药」一并说清
    confirmAction?.let { action ->
        val undo = action == "undo"
        val unknownTime = stringResource(R.string.backup_time_unknown)
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = {
                Text(
                    stringResource(
                        if (undo) R.string.backup_undo_restore else R.string.backup_restore_all
                    )
                )
            },
            text = {
                Text(
                    if (undo) stringResource(R.string.backup_undo_confirm)
                    else stringResource(
                        R.string.backup_restore_confirm,
                        backupInfo?.time?.ifBlank { unknownTime } ?: unknownTime,
                        backupInfo?.fileCount ?: 0
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmAction = null
                    runRestore(fromBefore = undo)
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    // 自动备份设置（标题显当前状态 → 开启/关闭）
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

    // 从剪贴板 / 文本导入书籍（照插件 restoreFromText）
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
                                    // 统一入口（照插件 restoreFromText）：liebiao / cunfang / characterRecords / shuming.<书> / gengxin 五写
                                    // ⚠️ 旧版只写 3 个 ⇒ 书切走再回来会从书架消失；不写 gengxin 则朗读规则内存是旧角色表
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

/** 选项行：无框行 + 语义图标（原「白卡片 + 彩色圆点」压在弹窗底上 = 框中框） */
/** 备份中心选项行；subtitle 用来摆「上次备份于 X · N 个文件」这类事实，不解释了就换行显示 */
@Composable
private fun BackupOptionRow(
    icon: ImageVector,
    text: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
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
        if (subtitle == null) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
        } else {
            Column(Modifier.weight(1f)) {
                Text(text, style = MaterialTheme.typography.bodyMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 书籍行左侧圆点配色（照插件 switchColors：8 色循环） */
private val BOOK_DOT_COLORS = listOf(
    Color(0xFF7E57C2), Color(0xFF5C6BC0), Color(0xFF26A69A), Color(0xFF8D6E63),
    Color(0xFF66BB6A), Color(0xFFEC407A), Color(0xFFFF7043), Color(0xFF42A5F5),
)

/** 默认书籍名：不可删除（与 CharacterRecordsFile 同源） */
private const val DEFAULT_BOOK_NAME = "默认"

/** 书籍列表弹窗（照插件 showBookSwitchDialog）：当前书主题浅底 + 描边 + ✓，仅非「默认」书给 ✕（二次确认） */
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
                            // 不要粗体——TextButton 默认 labelLarge 自带 w500，
                            // 换 bodyMedium（同为 14sp，常规字重）与弹窗正文统一
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = { multiVisible = true }) {
                        Text(
                            stringResource(R.string.role_book_multi_delete_mode),
                            style = MaterialTheme.typography.bodyMedium,
                            // 删除类入口统一 error 红（照插件 #EF6C00 橙已废——
                            // 它不随主题走，且与全 app 「删除=红」口径冲突）
                            color = MaterialTheme.colorScheme.error,
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
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
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

/** 书籍列表行（无框行，与密钥条目行同一口径） */
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
                // 5dp（原 7dp→6dp 仍嫌大；取 5 不取 4——4 与「当前书」竖条 3dp 宽度太近，
                // 两种标记的体量差会被抹平）
                Box(Modifier.size(5.dp).background(dotColor, CircleShape))
            }
        }
        Text(
            name,
            // 字号统一为角色列表档（方案 C）：bodyLarge ＋显式 16sp，与角色名同款。
            // 原 14sp(bodyMedium) 是 M3 的「次要信息」档，而书籍名与角色名同为「列表主对象名」；
            // 与顶部书籍栏书名（16sp 加粗）也正好同档，只差字重
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            // 当前书不再加粗（不喜欢粗体）——行首主色竖条 + 主色书名已够表达
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
