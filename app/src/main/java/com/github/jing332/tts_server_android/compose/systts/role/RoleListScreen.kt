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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.drake.net.utils.withIO
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts.TaggedTtsPreviewPlayer
import com.github.jing332.tts.PreviewState
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.conf.AppConfig
import com.github.jing332.tts_server_android.compose.systts.common.VoicePickerDialog
import com.github.jing332.tts_server_android.service.systts.help.CharacterRecordsFile
import com.github.jing332.tts_server_android.service.systts.help.VoiceMarksFile
import kotlinx.coroutines.launch

/**
 * 内置角色列表（目目 09-13 拍板「角色管理内置，分阶段搬」第一阶段）：
 * 原生 MD3 渲染角色列表，直读 characterRecords.json / voice_marks.json（与角色管理插件
 * 同源同文件，数据互通）；点发音人标签弹**现成**的 VoicePickerDialog（角色卡+音频参数）；
 * 长按/⋮ 菜单 = 标记/改名/设为主角/删除（写入口径四文件齐落，见 CharacterRecordsFile.saveRecords）。
 * 合并、批量分配、密钥管理、书籍切换留插件，第二阶段再搬。
 */
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
    // voice(tag) → 发音人显示名；tag → 大分类显示名（rule.tags 现查，与换声弹窗同口径）
    var voiceNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var categories by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    // voice(tag) → 可试听的启用配置项（▶ 用）
    var previewTargets by remember { mutableStateOf<Map<String, SystemTtsV2>>(emptyMap()) }

    LaunchedEffect(version, reloadKey) {
        val (recs, nameMap, catMap, targets) = withIO {
            val recs = CharacterRecordsFile.readRecords(tagRuleId)
            val groups = dbm.systemTtsV2.getAllGroupWithTts()
            val nameMap = LinkedHashMap<String, String>()
            val targets = LinkedHashMap<String, SystemTtsV2>()
            groups.forEach { g ->
                g.list.forEach { item ->
                    val cfg = item.config as? TtsConfigurationDTO ?: return@forEach
                    val voice = (cfg.source as? PluginTtsSource)?.voice?.trim().orEmpty()
                    if (voice.isEmpty() || voice in nameMap) return@forEach
                    nameMap[voice] = item.displayName
                    if (item.isEnabled) targets[voice] = item
                }
            }
            // 大分类=tag 查 rule.tags 剥尾序号（旁白/女青年01→女青年；括号系不合并）
            val ruleTags = dbm.speechRuleDao.getByRuleIdAll(tagRuleId)?.tags ?: emptyMap()
            val catMap = HashMap<String, String>()
            (recs.map { it.voice } + ruleTags.keys).forEach { tag ->
                if (tag.isBlank() || tag in catMap) return@forEach
                catMap[tag] = extractTagCategory(ruleTags[tag] ?: tag)
            }
            listOf(recs, nameMap, catMap, targets)
        }
        @Suppress("UNCHECKED_CAST")
        records = recs as List<CharacterRecordsFile.RoleRecord>
        @Suppress("UNCHECKED_CAST")
        voiceNames = nameMap as Map<String, String>
        @Suppress("UNCHECKED_CAST")
        categories = catMap as Map<String, String>
        @Suppress("UNCHECKED_CAST")
        previewTargets = targets as Map<String, SystemTtsV2>
        marks = VoiceMarksFile.readAll(tagRuleId)
    }

    // ===== 筛选 + 搜索 =====
    var filter by rememberSaveable { mutableIntStateOf(0) } // 0=全部 1=已分配 2=标记
    var keyword by rememberSaveable { mutableStateOf("") }
    val filtered = records.filter { rec ->
        when (filter) {
            1 -> rec.voice.isNotBlank()
            2 -> VoiceMarksFile.emojiOf(marks[rec.voice].orEmpty()).isNotEmpty()
            else -> true
        } && (keyword.isBlank()
                || rec.name.contains(keyword, ignoreCase = true)
                || rec.aliases.contains(keyword, ignoreCase = true))
    }
    val assignedCount = records.count { it.voice.isNotBlank() }
    val markedCount = records.count { VoiceMarksFile.emojiOf(marks[it.voice].orEmpty()).isNotEmpty() }

    // ===== 试听状态（纯状态推导，与换声弹窗/日志面板同款：IDLE 恒▶，禁 LaunchedEffect 抹 key） =====
    val previewState by TaggedTtsPreviewPlayer.state.collectAsState()
    var previewingKey by remember { mutableStateOf<Any?>(null) }
    fun previewLabel(key: Any?): String = when {
        previewingKey != key -> "▶"
        previewState == PreviewState.PLAYING -> "■"
        previewState == PreviewState.SYNTHESIZING -> "…"
        else -> "▶"
    }
    fun startPreview(rec: CharacterRecordsFile.RoleRecord) {
        val entity = previewTargets[rec.voice]
        if (entity == null) {
            android.widget.Toast.makeText(
                context, context.getString(R.string.role_list_preview_missing), android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }
        previewingKey = rec.voice
        TaggedTtsPreviewPlayer.play(context, entity, AppConfig.testSampleText.value)
    }

    // ===== 弹窗状态 =====
    var pickerFor by remember { mutableStateOf<CharacterRecordsFile.RoleRecord?>(null) }
    var renameFor by remember { mutableStateOf<CharacterRecordsFile.RoleRecord?>(null) }
    var deleteFor by remember { mutableStateOf<CharacterRecordsFile.RoleRecord?>(null) }

    Column(modifier) {
        // 搜索框
        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
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
                    Triple(0, stringResource(R.string.role_list_filter_all), records.size),
                    Triple(1, stringResource(R.string.role_list_filter_assigned), assignedCount),
                    Triple(2, stringResource(R.string.role_list_filter_marked), markedCount),
                )
            )
        }

        if (filtered.isEmpty()) {
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
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 12.dp, end = 12.dp, top = 2.dp, bottom = bottomPadding + 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // key 带 index：合并产生的同名同声记录不罕见（index 防重复 key 崩溃）
                itemsIndexed(filtered) { index, rec ->
                    RoleRow(
                        rec = rec,
                        marksEmoji = VoiceMarksFile.emojiOf(marks[rec.voice].orEmpty()),
                        category = categories[rec.voice].orEmpty(),
                        voiceName = voiceNames[rec.voice],
                        previewLabel = previewLabel(rec.voice),
                        onPreview = { startPreview(rec) },
                        onOpenPicker = { pickerFor = rec },
                        onRename = { renameFor = rec },
                        onDelete = { deleteFor = rec },
                        onSetMain = {
                            scope.launch {
                                val ok = withIO { CharacterRecordsFile.setMainCharacter(tagRuleId, rec.name) }
                                android.widget.Toast.makeText(
                                    context,
                                    if (ok) context.getString(R.string.role_list_set_main_toast, rec.name)
                                    else context.getString(R.string.role_list_failed),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                if (ok) version++
                            }
                        },
                        onToggleMark = { mark ->
                            scope.launch {
                                withIO { VoiceMarksFile.toggle(tagRuleId, rec.voice, mark) }
                                marks = VoiceMarksFile.readAll(tagRuleId)
                            }
                        },
                    )
                }
            }
        }
    }

    // 换声弹窗：与日志面板/插件桥同一组件（角色卡标题 + 音频参数分段）
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

    renameFor?.let { rec ->
        RenameDialog(
            initial = rec.name,
            onDismiss = { renameFor = null },
            onConfirm = { newName ->
                scope.launch {
                    val ok = withIO { CharacterRecordsFile.renameCharacter(tagRuleId, rec.name, newName) > 0 }
                    android.widget.Toast.makeText(
                        context,
                        if (ok) context.getString(R.string.role_list_rename_toast)
                        else context.getString(R.string.role_list_failed),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    renameFor = null
                    if (ok) version++
                }
            }
        )
    }

    deleteFor?.let { rec ->
        AlertDialog(
            onDismissRequest = { deleteFor = null },
            title = { Text(stringResource(R.string.role_list_delete_title)) },
            text = { Text(stringResource(R.string.role_list_delete_text, rec.name)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val n = withIO { CharacterRecordsFile.deleteCharacters(tagRuleId, setOf(rec.name)) }
                        android.widget.Toast.makeText(
                            context,
                            if (n > 0) context.getString(R.string.role_list_delete_toast, n)
                            else context.getString(R.string.role_list_failed),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        deleteFor = null
                        if (n > 0) version++
                    }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteFor = null }) { Text(stringResource(R.string.cancel)) }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoleRow(
    rec: CharacterRecordsFile.RoleRecord,
    marksEmoji: String,
    category: String,
    voiceName: String?,
    previewLabel: String,
    onPreview: () -> Unit,
    onOpenPicker: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onSetMain: () -> Unit,
    onToggleMark: (String) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onOpenPicker,
                onLongClick = { menuOpen = true },
            )
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                // 角色名行：名字加粗 + 主角👑 + 已点亮标记 emoji
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        rec.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (rec.isMain) {
                        Spacer(Modifier.width(4.dp))
                        Text("👑", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (marksEmoji.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        Text(marksEmoji, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(2.dp))
                // 发音人行：分类·显示名（点它=换声弹窗入口），未分配灰显
                Text(
                    if (rec.voice.isBlank()) stringResource(R.string.role_list_unassigned)
                    else buildString {
                        if (category.isNotBlank()) append("$category · ")
                        append(voiceName ?: rec.voice)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (rec.voice.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.combinedClickable(onClick = onOpenPicker),
                )
            }
            Spacer(Modifier.width(8.dp))
            // ▶ 试听
            TextButton(onClick = onPreview, enabled = rec.voice.isNotBlank()) {
                Text(
                    previewLabel,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.width(4.dp))
            // ⋮ 菜单（长按行同款）
            androidx.compose.material3.IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = null)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                VoiceMarksFile.MARK_ITEMS.forEach { (key, emoji, label) ->
                    DropdownMenuItem(
                        text = { Text("$emoji $label") },
                        onClick = {
                            menuOpen = false
                            onToggleMark(key)
                        },
                        trailingIcon = if (emoji in marksEmoji) {
                            { Icon(Icons.Default.Done, contentDescription = null) }
                        } else null,
                    )
                }
                androidx.compose.material3.HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.role_list_menu_rename)) },
                    onClick = { menuOpen = false; onRename() }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.role_list_menu_set_main)) },
                    onClick = { menuOpen = false; onSetMain() }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.role_list_menu_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = { menuOpen = false; onDelete() }
                )
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

/**
 * 大分类=标签显示名剥尾部数字（女青年01→女青年、本地音效1→本地音效）；
 * 不带尾序号的整名独立成类（旁白、男、女、括号系——目目 09-13 定）。
 * 与 VoicePickerDialog.extractTagCategory 同口径（那边 private，此处同实现）。
 */
private fun extractTagCategory(name: String): String {
    val m = Regex("^(.+?)(\\d+)$").find(name)
    return m?.groupValues?.get(1) ?: name
}
