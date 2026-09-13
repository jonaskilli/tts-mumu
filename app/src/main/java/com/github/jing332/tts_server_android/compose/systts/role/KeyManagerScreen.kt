package com.github.jing332.tts_server_android.compose.systts.role

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
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
import com.github.jing332.tts_server_android.service.systts.help.CharacterRecordsFile
import com.github.jing332.tts_server_android.service.systts.help.KeyListFile
import kotlinx.coroutines.launch

/**
 * 密钥管理 + 备份恢复 + 书籍管理·1:1 复刻（对照 角色管理v10_主题密钥增强.js）：
 * 密钥弹窗：按接口分组（未分组/直连密钥）+ 组折叠 + 当前密钥 ✓（反向匹配 miyue 内容，
 * 无匹配自动启用第一个）+ 测试结果记忆 ✓通/✗不通 + 新增（名称留空自动生成、重名覆盖确认、
 * 保存即启用）+ 改名 + 导出/导入（密钥导出_日期.json）+ 接口表单（新建/编辑/级联删除）+
 * 拉取模型（五类分组/搜索过滤/默认不勾选/全选只作用可见项/手动添加模型）。
 * 备份恢复：导出当前书籍到剪贴板/从剪贴板导入/备份全部文件/完整还原/自动备份开关。
 * 书籍：点击切换 · 点✕删除（当前书删后切默认）· 新增（建档并切换）· 多选删除 · 修改书名。
 */

/** 分组后的密钥组（照插件 buildKeyGroups：接口组 + 未分组 + 直连密钥） */
private class KeyGroup(val title: String, val entries: List<KeyListFile.KeyEntry>)

private fun buildKeyGroups(keys: List<KeyListFile.KeyEntry>, ifaces: List<KeyListFile.ApiInterface>): List<KeyGroup> {
    val groups = mutableListOf<KeyGroup>()
    val assigned = mutableSetOf<String>()
    ifaces.forEach { ifc ->
        val entries = keys.filter { KeyListFile.keyBelongsTo(it, ifc) }
        if (entries.isNotEmpty()) {
            groups.add(KeyGroup(ifc.name, entries))
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

@Composable
fun KeyManagerDialog(tagRuleId: String, onDismiss: () -> Unit) {
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
    // 测试中
    var testingName by remember { mutableStateOf<String?>(null) }

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

    // 弹窗状态
    var showAdd by remember { mutableStateOf(false) }
    var renameFor by remember { mutableStateOf<KeyListFile.KeyEntry?>(null) }
    var overwriteFor by remember { mutableStateOf<Pair<String, String>?>(null) } // (新名, 值) 覆盖确认
    var deleteFor by remember { mutableStateOf<KeyListFile.KeyEntry?>(null) }
    var menuFor by remember { mutableStateOf<KeyListFile.KeyEntry?>(null) }
    var ifcFormFor by remember { mutableStateOf<KeyListFile.ApiInterface?>(null) } // null+showIfcNew=true=新建
    var showIfcNew by remember { mutableStateOf(false) }
    var showPullModels by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                // 头部
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.role_key_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
                // 动作行：新增密钥 / 新建接口 / 拉取模型 / 导入导出（照插件操作行移至列表上方）
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = { showAdd = true }) { Text(stringResource(R.string.role_key_add)) }
                    TextButton(onClick = { showIfcNew = true }) { Text(stringResource(R.string.role_key_interface_new)) }
                    TextButton(onClick = { showPullModels = true }) { Text(stringResource(R.string.role_key_fetch)) }
                    TextButton(onClick = { showImport = true }) { Text(stringResource(R.string.role_key_import)) }
                    TextButton(onClick = {
                        scope.launch {
                            val name = withIO { KeyListFile.exportKeys(tagRuleId, keys) }
                            if (name != null) toast(R.string.role_key_exported, keys.size, name)
                            else toast(R.string.role_list_failed)
                        }
                    }) { Text(stringResource(R.string.role_key_export)) }
                }
                if (keys.isEmpty()) {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            stringResource(R.string.role_key_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val groups = buildKeyGroups(keys, ifaces)
                    LazyColumn(Modifier.weight(1f)) {
                        groups.forEach { grp ->
                            val isCollapsed = grp.title in collapsed
                            item(key = "grp_${grp.title}") {
                                // 组头（点击折叠/展开）
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clickable {
                                            collapsed = if (isCollapsed) collapsed - grp.title else collapsed + grp.title
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        (if (isCollapsed) "▸ " else "▾ ") + grp.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "${grp.entries.size}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                HorizontalDivider()
                            }
                            if (!isCollapsed) {
                                grp.entries.forEach { entry ->
                                    val isCurrent = entry.value.trim() == currentRaw && currentRaw.isNotEmpty()
                                    item(key = "k_${grp.title}_${entry.name}") {
                                        Row(
                                            Modifier.fillMaxWidth()
                                                .clickable { menuFor = entry }
                                                .padding(start = 24.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        entry.name,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    // 测试结果记忆 ✓通/✗不通
                                                    testResults[entry.name]?.let { ok ->
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(
                                                            stringResource(
                                                                if (ok) R.string.role_key_test_ok_short
                                                                else R.string.role_key_test_fail_short
                                                            ),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = if (ok) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                    if (isCurrent) {
                                                        Spacer(Modifier.width(8.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = MaterialTheme.colorScheme.primaryContainer
                                                        ) {
                                                            Text(
                                                                stringResource(R.string.role_key_current),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                if (entry.brief().isNotEmpty()) {
                                                    Text(
                                                        entry.brief(),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            if (testingName == entry.name) {
                                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                                Spacer(Modifier.width(8.dp))
                                            }
                                            IconButton(onClick = { menuFor = entry }) {
                                                Icon(Icons.Default.MoreVert, contentDescription = null)
                                            }
                                            DropdownMenu(
                                                expanded = menuFor == entry,
                                                onDismissRequest = { menuFor = null }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.role_key_set_current)) },
                                                    enabled = !isCurrent,
                                                    onClick = { menuFor = null; switchTo(entry) },
                                                    trailingIcon = if (isCurrent) {
                                                        { Icon(Icons.Default.Done, contentDescription = null) }
                                                    } else null
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.role_key_rename)) },
                                                    onClick = { menuFor = null; renameFor = entry }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.role_key_test)) },
                                                    onClick = { menuFor = null; testKey(entry) }
                                                )
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            stringResource(R.string.delete),
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                    },
                                                    onClick = { menuFor = null; deleteFor = entry }
                                                )
                                            }
                                        }
                                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
            onDismiss = { showPullModels = false },
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
    onConfirm: (String, String, Boolean) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var value by remember { mutableStateOf(initial?.value.orEmpty()) }
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
            }
        },
        confirmButton = {
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

    LaunchedEffect(Unit) {
        val list = withIO { KeyListFile.readInterfaces(tagRuleId) }
        ifaces = list
        pickedName = list.firstOrNull()?.name.orEmpty()
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

    LaunchedEffect(version) {
        autoOn = withIO { CharacterRecordsFile.readAutoBackupEnabled(tagRuleId) }
    }
    fun toast(resId: Int, vararg args: Any) {
        android.widget.Toast.makeText(
            context, context.getString(resId, *args), android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                MenuActionRow2(stringResource(R.string.backup_export_book), MaterialTheme.colorScheme.primary) {
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
                MenuActionRow2(stringResource(R.string.backup_import_book), Color(0xFF00838F)) {
                    inputText = ""
                    inputVisible = true
                }
                MenuActionRow2(stringResource(R.string.backup_export_all), Color(0xFF2E7D32)) {
                    scope.launch {
                        val n = withIO { CharacterRecordsFile.backupAllFiles(tagRuleId) }
                        toast(if (n > 0) R.string.backup_done else R.string.role_list_failed, n)
                    }
                }
                MenuActionRow2(stringResource(R.string.backup_restore_all), Color(0xFFF57F17)) {
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
                MenuActionRow2(
                    stringResource(
                        R.string.backup_auto_state,
                        stringResource(if (autoOn) R.string.backup_auto_on else R.string.backup_auto_off)
                    ), Color(0xFF7B1FA2)
                ) {
                    scope.launch {
                        val newState = !autoOn
                        withIO { CharacterRecordsFile.writeAutoBackupEnabled(tagRuleId, newState) }
                        autoOn = newState
                        toast(
                            if (newState) R.string.backup_auto_enabled_toast
                            else R.string.backup_auto_disabled_toast
                        )
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        }
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

/** 备份中心菜单行（复用样式） */
@Composable
private fun MenuActionRow2(text: String, dotColor: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.size(7.dp).background(dotColor, androidx.compose.foundation.CircleShape))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * 书籍管理弹窗·1:1（照插件 showBookSwitchDialog）：
 * 「点击切换 · 点✕删除」+ 当前书在前 + 新增（建档并切换）+ 多选删除；当前书也可删（删后切默认）。
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
    var multiMode by remember { mutableStateOf(false) }
    var checked by remember { mutableStateOf<Set<String>>(emptySet()) }
    var addBookVisible by remember { mutableStateOf(false) }

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
                checked = emptySet()
                version++
                if (currentDeleted) onSwitched()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.role_book_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
                Text(
                    stringResource(R.string.role_book_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                LazyColumn(Modifier.weight(1f)) {
                    books.forEach { book ->
                        val isCurrent = book == current
                        item(key = book) {
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable {
                                        if (multiMode) {
                                            checked = if (book in checked) checked - book else checked + book
                                        } else if (!isCurrent) switchTo(book)
                                    }
                                    .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (multiMode) {
                                    Checkbox(
                                        checked = book in checked,
                                        onCheckedChange = {
                                            checked = if (it) checked + book else checked - book
                                        }
                                    )
                                }
                                Text(
                                    book,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isCurrent && !multiMode) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            stringResource(R.string.role_key_current),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (!multiMode) {
                                    TextButton(onClick = {
                                        // 当前书也可删：提示删后切默认（照插件）
                                        scope.launch {
                                            withIO { CharacterRecordsFile.deleteBooks(tagRuleId, setOf(book)) }
                                            toast(R.string.role_book_deleted_toast, 1)
                                            version++
                                            if (isCurrent) onSwitched()
                                        }
                                    }) {
                                        Text("✕", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
                // 页脚：新增 + 多选删除
                Row(Modifier.fillMaxWidth().padding(12.dp)) {
                    TextButton(onClick = { addBookVisible = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Add, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.role_book_add))
                    }
                    TextButton(
                        enabled = checked.isNotEmpty(),
                        onClick = { deleteBooks(checked) }
                    ) {
                        Text(
                            stringResource(R.string.role_book_multi_delete, checked.size),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    TextButton(onClick = {
                        if (multiMode) { multiMode = false; checked = emptySet() } else multiMode = true
                    }) {
                        Text(
                            if (multiMode) stringResource(R.string.cancel)
                            else stringResource(R.string.role_book_multi_delete_mode)
                        )
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
