package com.github.jing332.tts_server_android.compose.systts.role

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drake.net.utils.withIO
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.tts.TaggedTtsPreviewPlayer
import com.github.jing332.tts.PreviewState
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.common.VoicePickerDialog
import com.github.jing332.tts_server_android.conf.AppConfig
import com.github.jing332.tts_server_android.service.systts.help.CharacterRecordsFile
import com.github.jing332.tts_server_android.service.systts.help.VoiceMarksFile
import kotlinx.coroutines.launch

/**
 * 角色管理·1:1 复刻（目目 09-13 拍板「照 v10 插件原样搬，之后他再改」）：
 * 对照 角色管理v10_主题密钥增强.js 逐函数复刻——
 * **平铺列表**（非分组）：每行=左名字列（主名+别名每行一个、性别色圆点、收藏【】、主角👑）
 * + 右侧（发音人标签框 / ⋮ 发音人管理 / ▶ 试听）。
 * 点击名字=勾选（选中背景高亮）；长按名字=操作菜单（合并+跟随/合并+选发音人（标记≥2）、
 * 释放删除已合并角色（有别名）、修改角色名（单选）、删除角色、设为主角）。
 * 发音人标签框点击=换声弹窗（与日志弹窗同款，桥接已通过）。
 * 顶部：书籍栏（书名/切换/修改书名）+ 密钥管理与备份恢复按钮；列表下方「+ 添加角色」。
 * 主题🎨按钮不搬（原生即 MD3 主题）。
 */

/** 性别圆点色（照插件：少年/男=青蓝，女=粉红，其余=灰） */
private fun genderDotColor(tag: String): Color = when {
    tag.contains("少年") -> Color(0xFF1976D2)
    tag.contains("女") -> Color(0xFFE91E63)
    tag.contains("男") -> Color(0xFF1976D2)
    else -> Color(0xFF9E9E9E)
}

/** 释放并固定的彩色圆点阵（照插件 releaseDotColors 顺序） */
private val releaseDotColors = listOf(
    0xFF7E57C2, 0xFF5C6BC0, 0xFF26A69A, 0xFF8D6E63,
    0xFF66BB6A, 0xFFEC407A, 0xFFFF7043, 0xFF42A5F5,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RoleListScreen(
    tagRuleId: String,
    reloadKey: Int,
    bottomPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ===== 数据 =====
    var version by remember { mutableIntStateOf(0) }
    var records by remember { mutableStateOf<List<CharacterRecordsFile.RoleRecord>>(emptyList()) }
    var marks by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var voiceNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var currentBook by remember { mutableStateOf("") }
    // tag → 可试听的启用配置项（▶ 用）；键=speechRule.tag
    var previewTargets by remember { mutableStateOf<Map<String, SystemTtsV2>>(emptyMap()) }

    LaunchedEffect(version, reloadKey) {
        val loaded = withIO {
            val recs = CharacterRecordsFile.readRecords(tagRuleId)
            val groups = dbm.systemTtsV2.getAllGroupWithTts()
            val nameMap = LinkedHashMap<String, String>()
            val targets = LinkedHashMap<String, SystemTtsV2>()
            groups.forEach { g ->
                g.list.forEach { item ->
                    val cfg = item.config as? TtsConfigurationDTO ?: return@forEach
                    val tag = cfg.speechRule?.tag?.trim().orEmpty()
                    if (tag.isEmpty()) return@forEach
                    if (tag !in nameMap) nameMap[tag] = item.displayName
                    if (item.isEnabled && tag !in targets) targets[tag] = item
                }
            }
            listOf(recs, nameMap, targets)
        }
        @Suppress("UNCHECKED_CAST")
        records = loaded[0] as List<CharacterRecordsFile.RoleRecord>
        @Suppress("UNCHECKED_CAST")
        voiceNames = loaded[1] as Map<String, String>
        @Suppress("UNCHECKED_CAST")
        previewTargets = loaded[2] as Map<String, SystemTtsV2>
        marks = VoiceMarksFile.readAll(tagRuleId)
        currentBook = CharacterRecordsFile.readCurrentBook(tagRuleId)
    }

    fun reload() { version++ }

    // 自动备份（照插件 onLoadUI：进入页面且开关开启时执行一次）
    LaunchedEffect(reloadKey) {
        withIO {
            if (CharacterRecordsFile.readAutoBackupEnabled(tagRuleId)) {
                CharacterRecordsFile.backupAllFiles(tagRuleId)
            }
        }
    }

    fun toast(resId: Int, vararg args: Any) {
        android.widget.Toast.makeText(context, context.getString(resId, *args), android.widget.Toast.LENGTH_SHORT).show()
    }

    // ===== 搜索 + 多选标记 =====
    var keyword by rememberSaveable { mutableStateOf("") }
    var markedNames by remember { mutableStateOf<Set<String>>(emptySet()) }
    fun toggleMark(name: String) {
        markedNames = if (name in markedNames) markedNames - name else markedNames + name
    }
    val filtered = records.filter { rec ->
        keyword.isBlank() || rec.name.contains(keyword, true) ||
            CharacterRecordsFile.splitAliases(rec.aliases).any { it.contains(keyword, true) }
    }
    val selectableNames = filtered.map { it.name }.toSet()

    // ===== 试听 =====
    val previewState by TaggedTtsPreviewPlayer.state.collectAsState()
    var previewingTag by remember { mutableStateOf<String?>(null) }
    fun previewLabel(tag: String): String = when {
        previewingTag != tag -> "▶"
        previewState == PreviewState.PLAYING -> "■"
        previewState == PreviewState.SYNTHESIZING -> "…"
        else -> "▶"
    }
    fun startPreview(tag: String) {
        val entity = previewTargets[tag]
        if (entity == null) {
            toast(R.string.role_list_preview_missing)
            return
        }
        previewingTag = tag
        TaggedTtsPreviewPlayer.play(context, entity, AppConfig.testSampleText.value)
    }

    // ===== 弹窗状态 =====
    var menuFor by remember { mutableStateOf<CharacterRecordsFile.RoleRecord?>(null) }
    var deleteNames by remember { mutableStateOf<Set<String>?>(null) }
    var editFor by remember { mutableStateOf<CharacterRecordsFile.RoleRecord?>(null) }
    var releaseFor by remember { mutableStateOf<CharacterRecordsFile.RoleRecord?>(null) }
    var keywordPickFor by remember { mutableStateOf<Pair<String, String>?>(null) } // (ownerName, releaseName) 释放并固定选关键词
    var addKeywordPick by remember { mutableStateOf<Boolean?>(null) } // 添加角色：先输名字后选关键词
    var addCharName by remember { mutableStateOf("") }
    var mergeFollowFor by remember { mutableStateOf<List<String>?>(null) } // 标记的角色名列表，选目标
    var mergeVoiceTarget by remember { mutableStateOf<String?>(null) } // 合并+选择发音人：目标角色
    var manageVoiceTag by remember { mutableStateOf<String?>(null) }
    var pickerFor by remember { mutableStateOf<CharacterRecordsFile.RoleRecord?>(null) } // 换声（标签框点击）
    var showBookDialog by remember { mutableStateOf(false) }
    var showKeyManager by remember { mutableStateOf(false) }
    var showBackupCenter by remember { mutableStateOf(false) }
    var backupVersion by remember { mutableIntStateOf(0) } // 恢复/导入等大动作后强制刷

    Column(modifier) {
        // ===== 密钥/备份 按钮行（照插件：主按钮行在书籍栏上方，48dp 高 + 加粗 + 均分；🎨主题按钮不搬=原生即 MD3）=====
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showKeyManager = true },
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("🔑  ${stringResource(R.string.role_key_title)}", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { showBackupCenter = true },
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("💾  ${stringResource(R.string.backup_title)}", fontWeight = FontWeight.Bold)
            }
        }
        // ===== 书籍栏（照插件：圆角卡片 = 📖 + 书名 + ✎ 行内改名 + ▾ 管理；
        //      点书名与▾管理都开书籍管理弹窗（插件 showBookSwitchDialog 同入口））=====
        var editingBook by remember { mutableStateOf(false) }
        var bookEditName by remember { mutableStateOf("") }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 10.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📖", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                if (editingBook) {
                    // 行内编辑（照插件 editBookBtn：✎ → 书名框可编辑 + ✓ 结束；不做5秒超时，点✓即存）
                    OutlinedTextField(
                        value = bookEditName,
                        onValueChange = { bookEditName = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            TextButton(
                                enabled = bookEditName.trim().isNotEmpty() && bookEditName.trim() != currentBook,
                                onClick = {
                                    val target = bookEditName.trim()
                                    editingBook = false
                                    scope.launch {
                                        val ok = withIO { CharacterRecordsFile.renameCurrentBook(tagRuleId, target) }
                                        toast(if (ok) R.string.role_book_renamed else R.string.role_list_failed, target)
                                        if (ok) reload()
                                    }
                                }
                            ) { Text("✓") }
                        },
                    )
                } else {
                    Text(
                        currentBook,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showBookDialog = true },
                    )
                    TextButton(onClick = { bookEditName = currentBook; editingBook = true }) {
                        Text("✎", color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    TextButton(onClick = { showBookDialog = true }) {
                        Text(
                            "▾ ${stringResource(R.string.role_book_manage)}",
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
        // ===== 角色区头（照插件：14sp 加粗主题色标题 + 搜索框（12dp圆角、hint自带🔍、无放大镜图标），
        //      全选描边小胶囊内嵌搜索框右端，选中态换警示色显「取消全选」）=====
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.role_list_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            val allSelected = markedNames.containsAll(selectableNames) && selectableNames.isNotEmpty()
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = {
                    Text(stringResource(R.string.role_search_hint), style = MaterialTheme.typography.bodyMedium)
                },
                shape = RoundedCornerShape(12.dp),
                suffix = {
                    // 照插件 selectAllBtn：8dp 圆角描边胶囊，全选=主色系 / 取消全选=警示色系
                    Surface(
                        onClick = {
                            markedNames = if (allSelected) emptySet() else selectableNames
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (allSelected) MaterialTheme.colorScheme.errorContainer
                                else MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(
                            1.dp,
                            if (allSelected) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(end = 4.dp).heightIn(min = 26.dp),
                    ) {
                        Text(
                            stringResource(if (allSelected) R.string.role_select_all_cancel else R.string.role_list_select_all),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (allSelected) MaterialTheme.colorScheme.onErrorContainer
                                    else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                        )
                    }
                },
            )
        }
        // 操作提示行（照插件 longPressHint）
        Text(
            stringResource(R.string.role_hint_line),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
        )

        // ===== 平铺角色列表（完全展开）=====
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 12.dp, end = 12.dp, top = 2.dp, bottom = bottomPadding + 96.dp
            )
        ) {
            if (filtered.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.role_list_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            filtered.forEachIndexed { idx, rec ->
                val key = "row_${rec.name}_${rec.voice}"
                item(key = key) {
                    RoleRow(
                        rec = rec,
                        voiceName = voiceTagText(rec.voice, voiceNames),
                        marked = rec.name in markedNames,
                        previewLabel = if (rec.voice.isBlank()) "▶" else previewLabel(rec.voice),
                        onNameClick = { toggleMark(rec.name) },
                        onNameLongClick = { menuFor = rec },
                        onTagClick = { pickerFor = rec },
                        onManageClick = { if (rec.voice.isNotBlank()) manageVoiceTag = rec.voice },
                        onPreviewClick = { if (rec.voice.isNotBlank()) startPreview(rec.voice) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
            // 「+ 添加角色」行（照插件：列表下方）
            item(key = "add_row") {
                Text(
                    stringResource(R.string.role_add_character),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            addCharName = ""
                            addKeywordPick = false // false=先弹名字输入
                        }
                        .padding(16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }

    // ===== 换声（发音人标签框点击，与日志弹窗同款）=====
    pickerFor?.let { rec ->
        VoicePickerDialog(
            anchorConfigId = null,
            anchorTag = rec.voice,
            bindingKey = rec.name,
            titleBadge = rec.name,
            onChanged = { _, _ -> reload() },
            onDismissRequest = { pickerFor = null },
        )
    }
    // ===== 合并+选择发音人：目标=长按角色，选完发音人后把其余标记角色并入 =====
    mergeVoiceTarget?.let { target ->
        val others = markedNames.filter { it != target }.toSet()
        VoicePickerDialog(
            anchorConfigId = null,
            anchorTag = records.firstOrNull { it.name == target }?.voice.orEmpty(),
            bindingKey = target,
            titleBadge = target,
            onChanged = { _, _ ->
                scope.launch {
                    val n = withIO { CharacterRecordsFile.mergeCharacters(tagRuleId, target, others) }
                    toast(if (n > 0) R.string.role_list_merge_toast else R.string.role_list_failed, n)
                    markedNames = emptySet()
                    reload()
                }
            },
            onDismissRequest = { mergeVoiceTarget = null },
        )
    }

    // ===== 长按操作菜单（照插件 showFirstDialog 动态项）=====
    menuFor?.let { rec ->
        val markCount = markedNames.size + if (rec.name in markedNames) 0 else 1
        val hasMerged = CharacterRecordsFile.splitAliases(rec.aliases)
            .any { it.trim() != rec.name.trim() }
        AlertDialog(
            onDismissRequest = { menuFor = null },
            title = { Text(stringResource(R.string.role_menu_title)) },
            text = {
                Column {
                    if (markCount >= 2) {
                        MenuActionRow(stringResource(R.string.role_menu_merge_follow), MaterialTheme.colorScheme.primary) {
                            menuFor = null
                            mergeFollowFor = (markedNames + rec.name).toList()
                        }
                        MenuActionRow(stringResource(R.string.role_menu_merge_voice), Color(0xFF7E57C2)) {
                            menuFor = null
                            markedNames = markedNames + rec.name
                            mergeVoiceTarget = rec.name
                        }
                    }
                    if (hasMerged) {
                        MenuActionRow(stringResource(R.string.role_menu_release), Color(0xFFFB8C00)) {
                            menuFor = null
                            releaseFor = rec
                        }
                    }
                    if (markCount < 2) {
                        MenuActionRow(stringResource(R.string.role_list_menu_rename), Color(0xFF00838F)) {
                            menuFor = null
                            editFor = rec
                        }
                    }
                    MenuActionRow(stringResource(R.string.role_list_menu_delete), MaterialTheme.colorScheme.error) {
                        menuFor = null
                        deleteNames = markedNames + rec.name
                    }
                    MenuActionRow(stringResource(R.string.role_list_menu_set_main), Color(0xFFF57F17)) {
                        menuFor = null
                        scope.launch {
                            val ok = withIO { CharacterRecordsFile.setMainCharacter(tagRuleId, rec.name) }
                            toast(
                                if (ok) R.string.role_list_set_main_toast else R.string.role_list_failed,
                                rec.name
                            )
                            if (ok) reload()
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { menuFor = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    // ===== 合并+跟随角色：在标记里选目标 =====
    mergeFollowFor?.let { candidates ->
        AlertDialog(
            onDismissRequest = { mergeFollowFor = null },
            title = { Text(stringResource(R.string.role_merge_follow_title)) },
            text = {
                Column {
                    candidates.forEach { name ->
                        MenuActionRow(name, MaterialTheme.colorScheme.primary) {
                            mergeFollowFor = null
                            val others = candidates.filter { it != name }.toSet()
                            scope.launch {
                                val n = withIO { CharacterRecordsFile.mergeCharacters(tagRuleId, name, others) }
                                toast(if (n > 0) R.string.role_list_merge_toast else R.string.role_list_failed, n)
                                markedNames = emptySet()
                                reload()
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { mergeFollowFor = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // ===== 释放/删除已合并角色（照插件：名字列表逐行 释放并固定/删除）=====
    releaseFor?.let { rec ->
        ReleaseDialog(
            rec = rec,
            onDismiss = { releaseFor = null },
            onFix = { owner, name ->
                keywordPickFor = owner to name
            },
            onDelete = { owner, name ->
                scope.launch {
                    val ok = withIO { CharacterRecordsFile.removeNameFromRecord(tagRuleId, owner, name) }
                    toast(if (ok) R.string.role_list_delete_toast else R.string.role_list_failed, 1)
                    if (ok) reload()
                }
            },
        )
    }
    // 释放并固定 → 选关键词
    keywordPickFor?.let { (owner, name) ->
        KeywordPickerDialog(
            tagRuleId = tagRuleId,
            onDismiss = { keywordPickFor = null },
            onPicked = { kw ->
                keywordPickFor = null
                scope.launch {
                    val ok = withIO { CharacterRecordsFile.releaseAndFix(tagRuleId, owner, name, kw) }
                    toast(
                        if (ok) R.string.role_release_fixed_toast else R.string.role_list_failed,
                        name, kw
                    )
                    if (ok) reload()
                }
            },
        )
    }

    // ===== 修改角色名（多行名称编辑器：主名+别名）=====
    editFor?.let { rec ->
        EditNamesDialog(
            initialNames = listOf(rec.name) + CharacterRecordsFile.splitAliases(rec.aliases).filter { it != rec.name },
            onDismiss = { editFor = null },
            onConfirm = { names ->
                scope.launch {
                    val ok = withIO { CharacterRecordsFile.editCharacterNames(tagRuleId, rec.name, names) }
                    toast(if (ok) R.string.role_list_rename_toast else R.string.role_list_failed)
                    editFor = null
                    if (ok) reload()
                }
            }
        )
    }

    // ===== 删除角色（单/多共用，对全部标记生效）=====
    deleteNames?.let { names ->
        AlertDialog(
            onDismissRequest = { deleteNames = null },
            title = { Text(stringResource(R.string.role_list_delete_title)) },
            text = { Text(stringResource(R.string.role_list_delete_text, names.size, names.take(5).joinToString("、"))) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val n = withIO { CharacterRecordsFile.deleteCharacters(tagRuleId, names) }
                        toast(if (n > 0) R.string.role_list_delete_toast else R.string.role_list_failed, n)
                        deleteNames = null
                        markedNames = emptySet()
                        if (n > 0) reload()
                    }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteNames = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // ===== 添加角色（照插件：输名字 → 选关键词 → 新记录）=====
    if (addKeywordPick != null) {
        AlertDialog(
            onDismissRequest = { addKeywordPick = null },
            title = { Text(stringResource(R.string.role_add_char_title)) },
            text = {
                OutlinedTextField(
                    value = addCharName,
                    onValueChange = { addCharName = it },
                    label = { Text(stringResource(R.string.role_add_char_input)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = addCharName.trim().isNotEmpty(),
                    onClick = { addKeywordPick = true }
                ) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { addKeywordPick = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    // addKeywordPick == true 时弹关键词选择
    if (addKeywordPick == true) {
        KeywordPickerDialog(
            tagRuleId = tagRuleId,
            onDismiss = { addKeywordPick = null },
            onPicked = { kw ->
                addKeywordPick = null
                scope.launch {
                    val ok = withIO { CharacterRecordsFile.addCharacter(tagRuleId, addCharName, kw) }
                    toast(
                        if (ok) R.string.role_add_char_ok else R.string.role_add_char_exists,
                        addCharName.trim(), kw
                    )
                    if (ok) reload()
                }
            },
        )
    }

    // ===== 行 ⋮：发音人管理（标记 + 删除该发音人）=====
    manageVoiceTag?.let { tag ->
        VoiceManageDialog(
            tagRuleId = tagRuleId,
            tag = tag,
            displayName = voiceNames[tag] ?: tag,
            marks = marks[tag].orEmpty(),
            onMarksChanged = { marks = it },
            onDismiss = { manageVoiceTag = null },
            onDelete = {
                scope.launch {
                    val n = withIO { CharacterRecordsFile.unbindVoice(tagRuleId, tag) }
                    toast(
                        if (n >= 0) R.string.role_voice_unbind_toast else R.string.role_list_failed,
                        n
                    )
                    manageVoiceTag = null
                    if (n >= 0) reload()
                }
            },
        )
    }

    // ===== 书籍管理弹窗（切换/✕删除/新增/多选删除）=====
    if (showBookDialog) {
        BookManagerDialog(
            tagRuleId = tagRuleId,
            onDismiss = { showBookDialog = false },
            onSwitched = { reload() },
        )
    }
    // 修改书名：已改为书籍栏 ✎ 行内编辑（照插件），弹窗通道退役

    // ===== 密钥管理 / 备份恢复 =====
    if (showKeyManager) {
        KeyManagerDialog(tagRuleId = tagRuleId, onDismiss = { showKeyManager = false })
    }
    if (showBackupCenter) {
        BackupCenterDialog(
            tagRuleId = tagRuleId,
            version = backupVersion,
            onDismiss = { showBackupCenter = false },
            onRestored = { backupVersion++; reload() },
        )
    }
}

/** 菜单动作行（彩色圆点 + 文字，照插件 showFirstDialog 行样式） */
@Composable
private fun MenuActionRow(text: String, dotColor: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            Modifier.size(7.dp).background(dotColor, CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * 角色行（照插件 createListRow）：
 * 左=名字列（主名+别名每行 [4dp 性别色圆点 + 名字]；收藏【】、主名主角👑）；
 * 右=发音人标签框（点击换声）+ ⋮（发音人管理）+ ▶（试听）。
 * 点击名字区=勾选（背景高亮），长按=操作菜单。
 */
/**
 * 发音人标签文本（照插件 generateVoiceTag 口径）：tag 前缀 + 显示名连写
 * （目目定稿「男主1晓伊」式）；显示名以 tag 开头时不重复拼（防"男主1男主1"）；
 * 显示名超 12 字截断加省略号；查不到配置返回 null（RoleRow 回落 tag + ⚠）。
 */
private fun voiceTagText(tag: String, nameMap: Map<String, String>): String? {
    val disp = nameMap[tag] ?: return null
    val prefix = if (disp.startsWith(tag)) "" else tag
    val shown = if (disp.length > 12) disp.take(12) + "…" else disp
    return prefix + shown
}

/**
 * 角色行（照插件 createListRow）：左名字列（主名+别名各一行、性别圆点、收藏【】、主角👑），
 * 右动作列（发音人标签框 / ⋮ 发音人管理 / ▶ 试听）。
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RoleRow(
    rec: CharacterRecordsFile.RoleRecord,
    voiceName: String?,
    marked: Boolean,
    previewLabel: String,
    onNameClick: () -> Unit,
    onNameLongClick: () -> Unit,
    onTagClick: () -> Unit,
    onManageClick: () -> Unit,
    onPreviewClick: () -> Unit,
) {
    val isFav = rec.obj.optInt("usageCount", 0) == 50
    val isProtagonist = rec.isMain
    val nameList = (listOf(rec.name) + CharacterRecordsFile.splitAliases(rec.aliases))
        .distinctBy { it.trim() }
    Surface(
        color = if (marked) MaterialTheme.colorScheme.surfaceContainerHighest
        else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 名字列
            Column(
                Modifier
                    .weight(1f)
                    .combinedClickable(onClick = onNameClick, onLongClick = onNameLongClick)
                    .heightIn(min = 44.dp),
            ) {
                nameList.forEachIndexed { idx, name ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.size(4.dp).background(genderDotColor(rec.voice), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = buildString {
                                if (isFav) append("【$name】") else append(name)
                                if (idx == 0 && isProtagonist) append(" 👑")
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (idx < nameList.lastIndex) Spacer(Modifier.height(2.dp))
                }
            }
            // 右侧动作列（voice 空不渲染，照插件）
            if (rec.voice.isNotBlank()) {
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 发音人标签框（失效标签加 ⚠）
                        Surface(
                            onClick = onTagClick,
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                (voiceName ?: rec.voice) + if (voiceName == null) " ⚠" else "",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .widthIn(max = 180.dp)
                            )
                        }
                        TextButton(onClick = onManageClick) {
                            Text("⋮", style = MaterialTheme.typography.titleMedium)
                        }
                        TextButton(onClick = onPreviewClick) {
                            Text(previewLabel, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

/** 释放/删除已合并角色弹窗（照插件 doReleaseOperation：逐名字行 释放并固定/删除） */
@Composable
private fun ReleaseDialog(
    rec: CharacterRecordsFile.RoleRecord,
    onDismiss: () -> Unit,
    onFix: (ownerName: String, name: String) -> Unit,
    onDelete: (ownerName: String, name: String) -> Unit,
) {
    val processed = remember { mutableStateOf<Set<String>>(emptySet()) }
    val allNames = (listOf(rec.name) + CharacterRecordsFile.splitAliases(rec.aliases))
        .distinctBy { it.trim() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.role_release_title)) },
        text = {
            Column {
                allNames.forEachIndexed { i, name ->
                    val done = name in processed.value
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.size(8.dp).background(
                            Color(releaseDotColors[i % releaseDotColors.size]), CircleShape
                        ))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (done) "$name ✓" else name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (done) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (!done) {
                            TextButton(onClick = { onFix(rec.name, name) }) {
                                Text(stringResource(R.string.role_release_fix), color = Color(0xFF2E7D32))
                            }
                            TextButton(onClick = {
                                processed.value = processed.value + name
                                onDelete(rec.name, name)
                            }) {
                                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.confirm)) }
        },
    )
}

/**
 * 关键词选择弹窗（照插件 showKeywordSelectionDialog）：
 * 固定 12 关键词 + 自定义关键词（添加/管理，存 custom_keywords.json 与插件互通）。
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun KeywordPickerDialog(
    tagRuleId: String,
    onDismiss: () -> Unit,
    onPicked: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var custom by remember { mutableStateOf<List<String>>(emptyList()) }
    var customInputVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        custom = withIO { CharacterRecordsFile.readCustomKeywords(tagRuleId) }
    }
    fun pick(kw: String) { onPicked(kw) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.role_keyword_title)) },
        text = {
            Column {
                listOf("女童", "男童", "少女", "少年", "女青年", "男青年", "女中年", "男中年", "女老年", "男老年", "女主", "男主")
                    .chunked(2)
                    .forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { kw ->
                                Surface(
                                    onClick = { pick(kw) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        kw,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                Text(
                    stringResource(R.string.role_keyword_custom),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                custom.forEach { kw ->
                    Row(
                        Modifier.fillMaxWidth().clickable { pick(kw) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(kw, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            scope.launch {
                                custom = withIO { CharacterRecordsFile.removeCustomKeyword(tagRuleId, kw) }
                            }
                        }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
                    }
                }
                TextButton(onClick = {
                    scope.launch {
                        // 简化输入：复用系统对话框不可行，直接用输入弹窗状态
                        customInputVisible = true
                    }
                }) { Text(stringResource(R.string.role_keyword_add)) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
    // 自定义关键词输入
    if (customInputVisible) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { customInputVisible = false },
            title = { Text(stringResource(R.string.role_keyword_add)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.role_keyword_input_hint)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = text.trim().isNotEmpty(),
                    onClick = {
                        val kw = text.trim()
                        scope.launch {
                            val (list, ok) = withIO {
                                val cur = CharacterRecordsFile.readCustomKeywords(tagRuleId)
                                if (kw in cur) cur to false
                                else CharacterRecordsFile.saveCustomKeyword(tagRuleId, kw) to true
                            }
                            custom = list
                            customInputVisible = false
                        }
                    }
                ) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { customInputVisible = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}


/** 修改角色名称：多行名称编辑器（第1个=主名，其余=别名） */
@Composable
private fun EditNamesDialog(
    initialNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    val names = remember { mutableStateOf(initialNames.toMutableList()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.role_list_menu_rename)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.role_edit_names_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                names.value.forEachIndexed { idx, value ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = value,
                            onValueChange = { run ->
                                names.value = names.value.toMutableList().also { it[idx] = run }
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (names.value.size > 1) {
                            TextButton(onClick = {
                                names.value = names.value.toMutableList().also { it.removeAt(idx) }
                            }) {
                                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                TextButton(onClick = {
                    names.value = (names.value + "").toMutableList()
                }) { Text(stringResource(R.string.role_edit_names_add)) }
            }
        },
        confirmButton = {
            val cleaned = names.value.map { it.trim() }.filter { it.isNotEmpty() }
            TextButton(
                enabled = cleaned.isNotEmpty(),
                onClick = { onConfirm(cleaned) }
            ) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/** 发音人管理弹窗（照插件 showVoiceManageDialog：信息 + 多选标记 + 删除该发音人） */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun VoiceManageDialog(
    tagRuleId: String,
    tag: String,
    displayName: String,
    marks: List<String>,
    onMarksChanged: (Map<String, List<String>>) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var curMarks by remember(tag) { mutableStateOf(marks) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.role_voice_manage_title)) },
        text = {
            Column {
                Text(
                    stringResource(
                        R.string.role_voice_manage_info,
                        tag,
                        displayName,
                        curMarks.mapNotNull { key -> VoiceMarksFile.MARK_ITEMS.firstOrNull { m -> m.first == key }?.third }
                            .joinToString(" ").ifEmpty { stringResource(R.string.role_voice_unmarked) }
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VoiceMarksFile.MARK_ITEMS.forEach { (key, emoji, label) ->
                        val selected = key in curMarks
                        Surface(
                            onClick = {
                                scope.launch {
                                    withIO { VoiceMarksFile.toggle(tagRuleId, tag, key) }
                                    curMarks = withIO { VoiceMarksFile.get(tagRuleId, tag) }
                                    onMarksChanged(mapOf(tag to curMarks))
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(emoji)
                                Spacer(Modifier.width(4.dp))
                                Text(label, style = MaterialTheme.typography.bodySmall)
                                if (selected) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Done, contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.role_voice_manage_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
