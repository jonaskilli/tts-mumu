package com.github.jing332.tts_server_android.compose.systts.role

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.drake.net.utils.withIO
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.tts.TaggedTtsPreviewPlayer
import com.github.jing332.tts.PreviewState
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.conf.AppConfig
import com.github.jing332.tts_server_android.compose.systts.common.VoicePickerDialog
import com.github.jing332.tts_server_android.service.systts.help.CharacterRecordsFile
import com.github.jing332.tts_server_android.service.systts.help.VoiceMarksFile
import kotlinx.coroutines.launch

/**
 * 内置角色列表·全套版（目目 09-13 拍板「干脆全套改好」）：
 * **按发音人分组**（同旧插件骨架，MD3 化）——组=同一发音人标签，组头=标签 chip+▶+⋮（组级），
 * 组员=绑定该标签的角色名列表（主名+展开的别名）。点标签=整组换声（角色卡弹窗，
 * groupBindingKeys 逐个 rebind）；点主名=单角色角色卡；长按主名=改名/设为主角/删除；
 * 长按别名=移出合并；组头 ⋮=标记/整组删除。搜索框右侧「全选」进多选模式：
 * 底部操作条 合并（选中≥2，别名并入目标 aliases）/ 删除所选。
 * 密钥管理与书籍切换在顶栏（KeyManagerScreen / BookManageDialog），数据与插件同文件互通。
 */

/** 组员行：主名记录或别名（别名依附 owner 记录存在） */
private class MemberRow(val rec: CharacterRecordsFile.RoleRecord?, val aliasName: String, val owner: CharacterRecordsFile.RoleRecord?) {
    val displayName: String get() = rec?.name ?: aliasName
    val isAlias: Boolean get() = rec == null
    val voice: String get() = rec?.voice ?: owner?.voice.orEmpty()
}

/** 发音人组：同 tag 的全部主名+别名，rows 保持文件顺序 */
private class VoiceGroup(val tag: String, val rows: List<MemberRow>)

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

    // ===== 数据：角色记录 + 标记 + 显示名缓存（文件通道无观察者，靠 version 手动刷新） =====
    var version by remember { mutableIntStateOf(0) }
    var records by remember { mutableStateOf<List<CharacterRecordsFile.RoleRecord>>(emptyList()) }
    var marks by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    // tag → 发音人显示名；tag → 大分类显示名（rule.tags 现查，与换声弹窗同口径）
    var voiceNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var categories by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    // tag → 可试听的启用配置项（▶ 用）；键=speechRule.tag（勿用 PluginTtsSource.voice，两码事）
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
                    // 试听目标只收启用项（禁用条不进；同 tag 多条时首个启用生效）
                    if (item.isEnabled && tag !in targets) targets[tag] = item
                }
            }
            // 大分类=tag 查 rule.tags 剥尾序号（女青年01→女青年；括号系不合并）
            val ruleTags = dbm.speechRuleDao.getByRuleIdAll(tagRuleId)?.tags ?: emptyMap()
            val catMap = HashMap<String, String>()
            (recs.map { it.voice } + ruleTags.keys).forEach { tag ->
                if (tag.isBlank() || tag in catMap) return@forEach
                catMap[tag] = extractTagCategory(ruleTags[tag] ?: tag)
            }
            listOf(recs, nameMap, catMap, targets)
        }
        @Suppress("UNCHECKED_CAST")
        records = loaded[0] as List<CharacterRecordsFile.RoleRecord>
        @Suppress("UNCHECKED_CAST")
        voiceNames = loaded[1] as Map<String, String>
        @Suppress("UNCHECKED_CAST")
        categories = loaded[2] as Map<String, String>
        @Suppress("UNCHECKED_CAST")
        previewTargets = loaded[3] as Map<String, SystemTtsV2>
        marks = VoiceMarksFile.readAll(tagRuleId)
    }

    // ===== 分组构建：voice 相同的记录+别名归一组（"" 组=未分配） =====
    fun buildGroups(): List<VoiceGroup> {
        val map = LinkedHashMap<String, MutableList<MemberRow>>()
        records.forEach { rec ->
            val list = map.getOrPut(rec.voice) { mutableListOf() }
            list.add(MemberRow(rec, "", null))
            CharacterRecordsFile.splitAliases(rec.aliases).forEach { alias ->
                list.add(MemberRow(null, alias, rec))
            }
        }
        return map.map { (tag, rows) -> VoiceGroup(tag, rows) }
    }

    // ===== 筛选 + 搜索（行级过滤：命中的组员所在组保留，只显示命中行） =====
    var filter by rememberSaveable { mutableIntStateOf(0) } // 0=全部 1=已分配 2=标记
    var keyword by rememberSaveable { mutableStateOf("") }
    val allGroups = buildGroups()
    val groups = allGroups.mapNotNull { g ->
        if (filter == 1 && g.tag.isBlank()) return@mapNotNull null
        if (filter == 2 && VoiceMarksFile.emojiOf(marks[g.tag].orEmpty()).isEmpty()) return@mapNotNull null
        val rows = if (keyword.isBlank()) g.rows
        else g.rows.filter { it.displayName.contains(keyword, ignoreCase = true) }
        if (rows.isEmpty()) null else VoiceGroup(g.tag, rows)
    }
    val assignedCount = allGroups.count { it.tag.isNotBlank() }
    val markedCount = allGroups.count { VoiceMarksFile.emojiOf(marks[it.tag].orEmpty()).isNotEmpty() }

    // ===== 试听状态（纯状态推导，与换声弹窗/日志面板同款：IDLE 恒▶，禁 LaunchedEffect 抹 key） =====
    val previewState by TaggedTtsPreviewPlayer.state.collectAsState()
    var previewingKey by remember { mutableStateOf<Any?>(null) }
    fun previewLabel(key: Any?): String = when {
        previewingKey != key -> "▶"
        previewState == PreviewState.PLAYING -> "■"
        previewState == PreviewState.SYNTHESIZING -> "…"
        else -> "▶"
    }
    fun startPreview(tag: String) {
        val entity = previewTargets[tag]
        if (entity == null) {
            android.widget.Toast.makeText(
                context, context.getString(R.string.role_list_preview_missing), android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }
        previewingKey = tag
        TaggedTtsPreviewPlayer.play(context, entity, AppConfig.testSampleText.value)
    }

    // ===== 多选（主名记录参与；别名依附记录不单选） =====
    var selectMode by remember { mutableStateOf(false) }
    val selectedNames = remember { mutableStateOf<Set<String>>(emptySet()) }
    fun toggleSelect(name: String) {
        selectedNames.value = selectedNames.value.toMutableSet().apply {
            if (!add(name)) remove(name)
        }
    }

    // ===== 弹窗状态 =====
    var pickerFor by remember { mutableStateOf<CharacterRecordsFile.RoleRecord?>(null) }
    var groupPickerTag by remember { mutableStateOf<String?>(null) } // 整组换声：tag+组内主名
    var groupPickerKeys by remember { mutableStateOf<List<String>>(emptyList()) }
    var menuFor by remember { mutableStateOf<MemberRow?>(null) }
    var menuForGroup by remember { mutableStateOf<VoiceGroup?>(null) }
    var renameFor by remember { mutableStateOf<String?>(null) }
    var deleteNames by remember { mutableStateOf<Set<String>?>(null) }
    var mergeCandidates by remember { mutableStateOf<List<String>?>(null) }

    fun toast(resId: Int, vararg args: Any) {
        android.widget.Toast.makeText(context, context.getString(resId, *args), android.widget.Toast.LENGTH_SHORT).show()
    }

    Column(modifier) {
        // 搜索框 + 全选（同一行，全选贴右）
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = {
                    Text(
                        stringResource(R.string.role_list_search_hint),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(24.dp),
            )
            TextButton(onClick = {
                if (selectMode) {
                    selectMode = false
                    selectedNames.value = emptySet()
                } else {
                    selectMode = true
                    selectedNames.value = groups.flatMap { g -> g.rows.mapNotNull { it.rec?.name } }.toSet()
                }
            }) {
                Text(
                    if (selectMode) stringResource(R.string.cancel)
                    else stringResource(R.string.role_list_select_all)
                )
            }
        }
        // 筛选 chips
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipRow(
                selected = filter, onSelect = { filter = it },
                items = listOf(
                    Triple(0, stringResource(R.string.role_list_filter_all), allGroups.size),
                    Triple(1, stringResource(R.string.role_list_filter_assigned), assignedCount),
                    Triple(2, stringResource(R.string.role_list_filter_marked), markedCount),
                )
            )
        }

        if (groups.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    stringResource(R.string.role_list_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 12.dp, end = 12.dp, top = 2.dp, bottom = bottomPadding + 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groups.forEach { group ->
                    // key 带组序位置（index）：同名同声记录不罕见，name 键会撞——用 item(index) 结构
                    item(key = "grp_${group.tag}_${group.rows.firstOrNull()?.displayName.orEmpty()}_${group.rows.size}") {
                        GroupCard(
                            group = group,
                            tagName = if (group.tag.isBlank()) stringResource(R.string.role_list_unassigned)
                            else buildString {
                                categories[group.tag]?.let { if (it.isNotBlank()) append("$it · ") }
                                append(voiceNames[group.tag] ?: group.tag)
                            },
                            marksEmoji = VoiceMarksFile.emojiOf(marks[group.tag].orEmpty()),
                            previewLabel = previewLabel(group.tag),
                            selectMode = selectMode,
                            selectedNames = selectedNames.value,
                            onPreview = { startPreview(group.tag) },
                            onGroupPicker = {
                                if (group.tag.isNotBlank()) {
                                    groupPickerTag = group.tag
                                    groupPickerKeys = group.rows.mapNotNull { it.rec?.name }.distinct()
                                }
                            },
                            onGroupMenu = { menuForGroup = group },
                            onRowClick = { row ->
                                when {
                                    selectMode && !row.isAlias -> toggleSelect(row.rec!!.name)
                                    row.isAlias -> menuFor = row
                                    else -> pickerFor = row.rec
                                }
                            },
                            onRowLongClick = { row -> if (!selectMode) menuFor = row },
                        )
                    }
                }
            }
        }

        // 多选操作条
        if (selectMode) {
            Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.role_list_selected_count, selectedNames.value.size),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        enabled = selectedNames.value.size >= 2,
                        onClick = { mergeCandidates = selectedNames.value.toList() }
                    ) { Text(stringResource(R.string.role_list_merge)) }
                    TextButton(
                        enabled = selectedNames.value.isNotEmpty(),
                        onClick = { deleteNames = selectedNames.value }
                    ) {
                        Text(
                            stringResource(R.string.role_list_delete_selected),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    // ===== 单角色换声（角色卡） =====
    pickerFor?.let { rec ->
        VoicePickerDialog(
            anchorConfigId = null,
            anchorTag = rec.voice,
            bindingKey = rec.name,
            titleBadge = rec.name,
            onChanged = { _, _ -> version++ },
            onDismissRequest = { pickerFor = null },
        )
    }
    // ===== 整组换声（组头标签/组菜单；groupBindingKeys 逐个 rebind） =====
    groupPickerTag?.let { tag ->
        VoicePickerDialog(
            anchorConfigId = null,
            anchorTag = tag,
            bindingKey = groupPickerKeys.firstOrNull().orEmpty(),
            titleBadge = "", // 整组无单一角色名 → 「信息卡」
            groupBindingKeys = groupPickerKeys,
            onChanged = { _, _ -> version++ },
            onDismissRequest = { groupPickerTag = null },
        )
    }

    // ===== 组员长按/点击菜单（主名：改名/设为主角/删除；别名：移出合并） =====
    menuFor?.let { row ->
        DropdownMenu(expanded = true, onDismissRequest = { menuFor = null }) {
            if (row.isAlias) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.role_list_release_alias)) },
                    onClick = {
                        menuFor = null
                        scope.launch {
                            val ok = withIO {
                                CharacterRecordsFile.releaseAlias(tagRuleId, row.owner!!.name, row.aliasName)
                            }
                            toast(if (ok) R.string.role_list_release_toast else R.string.role_list_failed, row.aliasName)
                            if (ok) version++
                        }
                    }
                )
            } else {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.role_list_menu_rename)) },
                    onClick = { menuFor = null; renameFor = row.rec!!.name }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.role_list_menu_set_main)) },
                    onClick = {
                        menuFor = null
                        scope.launch {
                            val ok = withIO { CharacterRecordsFile.setMainCharacter(tagRuleId, row.rec!!.name) }
                            toast(
                                if (ok) R.string.role_list_set_main_toast else R.string.role_list_failed,
                                row.rec!!.name
                            )
                            if (ok) version++
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.role_list_menu_delete), color = MaterialTheme.colorScheme.error) },
                    onClick = { menuFor = null; deleteNames = setOf(row.rec!!.name) }
                )
            }
        }
    }
    // ===== 组头 ⋮ 菜单（标记 + 整组删除 + 整组换声） =====
    menuForGroup?.let { group ->
        DropdownMenu(expanded = true, onDismissRequest = { menuForGroup = null }) {
            if (group.tag.isNotBlank()) {
                VoiceMarksFile.MARK_ITEMS.forEach { (key, emoji, label) ->
                    DropdownMenuItem(
                        text = { Text("$emoji $label") },
                        onClick = {
                            menuForGroup = null
                            scope.launch {
                                withIO { VoiceMarksFile.toggle(tagRuleId, group.tag, key) }
                                marks = VoiceMarksFile.readAll(tagRuleId)
                            }
                        },
                        trailingIcon = if (emoji in VoiceMarksFile.emojiOf(marks[group.tag].orEmpty())) {
                            { Icon(Icons.Default.Done, contentDescription = null) }
                        } else null,
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.role_list_group_rebind)) },
                    onClick = {
                        menuForGroup = null
                        groupPickerTag = group.tag
                        groupPickerKeys = group.rows.mapNotNull { it.rec?.name }.distinct()
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.role_list_group_delete), color = MaterialTheme.colorScheme.error) },
                onClick = {
                    menuForGroup = null
                    deleteNames = group.rows.mapNotNull { it.rec?.name }.toSet()
                }
            )
        }
    }

    // ===== 改名 =====
    renameFor?.let { oldName ->
        RenameDialog(
            initial = oldName,
            onDismiss = { renameFor = null },
            onConfirm = { newName ->
                scope.launch {
                    val ok = withIO { CharacterRecordsFile.renameCharacter(tagRuleId, oldName, newName) > 0 }
                    toast(if (ok) R.string.role_list_rename_toast else R.string.role_list_failed)
                    renameFor = null
                    if (ok) version++
                }
            }
        )
    }

    // ===== 删除（单个/组/批量共用） =====
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
                        selectedNames.value = emptySet()
                        if (n > 0) version++
                    }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteNames = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // ===== 合并：选保留名 =====
    mergeCandidates?.let { candidates ->
        MergeTargetDialog(
            candidates = candidates,
            onDismiss = { mergeCandidates = null },
            onConfirm = { target ->
                scope.launch {
                    val others = candidates.filter { it != target }.toSet()
                    val n = withIO { CharacterRecordsFile.mergeCharacters(tagRuleId, target, others) }
                    toast(if (n > 0) R.string.role_list_merge_toast else R.string.role_list_failed, n)
                    mergeCandidates = null
                    selectedNames.value = emptySet()
                    selectMode = false
                    if (n > 0) version++
                }
            }
        )
    }
}

@Composable
private fun FilterChipRow(
    selected: Int,
    onSelect: (Int) -> Unit,
    items: List<Triple<Int, String, Int>>,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (value, label, count) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text("$label $count") },
                leadingIcon = if (selected == value) {
                    { Icon(Icons.Default.Done, contentDescription = null, Modifier.width(18.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(),
            )
        }
    }
}

/** 发音人组卡片：组头（标签 chip+▶+⋮）+ 组员列表（主名/别名） */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupCard(
    group: VoiceGroup,
    tagName: String,
    marksEmoji: String,
    previewLabel: String,
    selectMode: Boolean,
    selectedNames: Set<String>,
    onPreview: () -> Unit,
    onGroupPicker: () -> Unit,
    onGroupMenu: () -> Unit,
    onRowClick: (MemberRow) -> Unit,
    onRowLongClick: (MemberRow) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            // 组头：标签 chip + ▶ + ⋮
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onGroupPicker,
                    enabled = group.tag.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    color = if (group.tag.isBlank()) MaterialTheme.colorScheme.surfaceContainerHighest
                    else MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            tagName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (group.tag.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (marksEmoji.isNotEmpty()) {
                            Spacer(Modifier.width(6.dp))
                            Text(marksEmoji, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onPreview, enabled = group.tag.isNotBlank()) {
                    Text(previewLabel, style = MaterialTheme.typography.titleMedium)
                }
                androidx.compose.material3.IconButton(onClick = onGroupMenu) {
                    Icon(Icons.Default.MoreVert, contentDescription = null)
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = 12.dp))
            // 组员：角色名列表（主名加粗/别名常规淡色；多选模式主名行前 checkbox）
            group.rows.forEach { row ->
                val selected = !row.isAlias && row.rec!!.name in selectedNames
                Row(
                    Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onRowClick(row) },
                            onLongClick = { onRowLongClick(row) },
                        )
                        .padding(start = 16.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectMode && !row.isAlias) {
                        Checkbox(checked = selected, onCheckedChange = { onRowClick(row) })
                    } else {
                        Spacer(Modifier.width(8.dp))
                        Text("•", color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        row.displayName,
                        style = if (row.isAlias) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                        fontWeight = if (row.isAlias) FontWeight.Normal else FontWeight.SemiBold,
                        color = if (row.isAlias) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!row.isAlias && row.rec!!.isMain) {
                        Text("👑", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RenameDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.role_list_rename_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank() && text.trim() != initial,
                onClick = { onConfirm(text.trim()) }
            ) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/** 合并目标选择：单选保留哪个角色名（其余并入其 aliases 后删除） */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MergeTargetDialog(
    candidates: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var picked by remember { mutableStateOf(candidates.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.role_list_merge_target_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.role_list_merge_target_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                candidates.forEach { name ->
                    Row(
                        Modifier.fillMaxWidth().combinedClickable(onClick = { picked = name }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = picked == name, onClick = { picked = name })
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(picked) }) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * 大分类=标签显示名剥尾部数字（女青年01→女青年、本地音效1→本地音效）；
 * 不带尾序号的整名独立成类（旁白、男、女、括号系——目目 09-13 定）。
 * 与 VoicePickerDialog.extractTagCategory 同口径（那边 private，此处同实现）。
 */
private fun extractTagCategory(name: String): String {
    val m = Regex("^(.+?)(\\d+)$").find(name)
    return m?.groupValues?.get(1) ?: name
}
