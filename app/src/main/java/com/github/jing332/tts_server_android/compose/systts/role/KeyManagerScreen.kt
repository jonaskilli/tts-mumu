package com.github.jing332.tts_server_android.compose.systts.role

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.drake.net.utils.withIO
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.service.systts.help.CharacterRecordsFile
import com.github.jing332.tts_server_android.service.systts.help.KeyListFile
import kotlinx.coroutines.launch

/**
 * 密钥管理 + 书籍管理（目目 09-13「全套一次到位」）：
 * 与角色管理插件同文件互通——key_list.json（密钥池）/ api_center.json（接口中心）/
 * miyue|gengxin|miyue_backup.txt（当前密钥三写）/ liebiao.json + cunfang.txt + shuming.<书>.json（书籍）。
 * 密钥：增删改/设为当前/通断测试/从接口中心批量拉取模型入库。
 * 书籍：切换（当前数据先存档再载入新书存档）/新增/删除（当前书不可删）。
 */

@Composable
fun KeyManagerDialog(tagRuleId: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var version by remember { mutableIntStateOf(0) }
    var keys by remember { mutableStateOf<List<KeyListFile.KeyEntry>>(emptyList()) }
    var currentRaw by remember { mutableStateOf("") }

    LaunchedEffect(version) {
        val loaded = withIO {
            Pair(KeyListFile.readKeys(tagRuleId), KeyListFile.readCurrentRaw(tagRuleId).trim())
        }
        keys = loaded.first
        currentRaw = loaded.second
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
    fun setCurrent(entry: KeyListFile.KeyEntry) {
        scope.launch {
            val ok = withIO { KeyListFile.saveCurrentRaw(tagRuleId, entry.value) }
            toast(if (ok) R.string.role_key_current_toast else R.string.role_list_failed)
            if (ok) version++
        }
    }
    fun testKey(entry: KeyListFile.KeyEntry) {
        val parsed = KeyListFile.parseKeyValue(entry.value)
        if (parsed == null || parsed.isDirect) {
            toast(R.string.role_key_test_direct)
            return
        }
        scope.launch {
            val result = withIO { KeyListFile.testKey(parsed.url, parsed.key, parsed.model) }
            toast(
                if (result.first) R.string.role_key_test_ok_toast else R.string.role_key_test_fail_toast,
                entry.name, result.second
            )
        }
    }

    var showAdd by remember { mutableStateOf(false) }
    var editFor by remember { mutableStateOf<KeyListFile.KeyEntry?>(null) }
    var deleteFor by remember { mutableStateOf<KeyListFile.KeyEntry?>(null) }
    var menuFor by remember { mutableStateOf<KeyListFile.KeyEntry?>(null) }
    var showInterfaceCenter by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                // 头部：标题 + 关闭
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
                // 动作行：新增 + 接口中心
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = { showAdd = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.role_key_add))
                    }
                    TextButton(onClick = { showInterfaceCenter = true }) {
                        Text(stringResource(R.string.role_key_from_interface))
                    }
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
                    LazyColumn(
                        Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)
                    ) {
                        keys.forEach { entry ->
                            val isCurrent = entry.value.trim() == currentRaw && currentRaw.isNotEmpty()
                            item(key = entry.name) {
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clickable { menuFor = entry }
                                        .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                entry.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
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
                                            onClick = { menuFor = null; setCurrent(entry) },
                                            trailingIcon = if (isCurrent) {
                                                { Icon(Icons.Default.Done, contentDescription = null) }
                                            } else null
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.role_key_edit)) },
                                            onClick = { menuFor = null; editFor = entry }
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

    // 新增 / 编辑
    if (showAdd) {
        KeyEditDialog(
            initial = null,
            existingNames = keys.map { it.name }.toSet(),
            onDismiss = { showAdd = false },
            onConfirm = { name, value ->
                showAdd = false
                save(
                    keys + KeyListFile.KeyEntry(
                        name = name,
                        keyCode = KeyListFile.nextKeyCode(keys),
                        value = value
                    )
                )
            }
        )
    }
    editFor?.let { entry ->
        KeyEditDialog(
            initial = entry,
            existingNames = keys.map { it.name }.toSet(),
            onDismiss = { editFor = null },
            onConfirm = { name, value ->
                editFor = null
                save(
                    keys.map { if (it.name == entry.name) it.copy(name = name, value = value) else it }
                )
            }
        )
    }
    // 删除
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
    // 接口中心批量拉取
    if (showInterfaceCenter) {
        InterfaceCenterDialog(
            tagRuleId = tagRuleId,
            existingNames = keys.map { it.name }.toSet(),
            onDismiss = { showInterfaceCenter = false },
            onConfirm = { entries ->
                showInterfaceCenter = false
                val merged = entries.fold(keys) { acc, e ->
                    acc + e.copy(keyCode = KeyListFile.nextKeyCode(acc))
                }
                save(merged)
            }
        )
    }
}

/** 密钥编辑（新增/编辑共用）：名称 + 值 */
@Composable
private fun KeyEditDialog(
    initial: KeyListFile.KeyEntry?,
    existingNames: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var value by remember { mutableStateOf(initial?.value.orEmpty()) }
    val nameOk = name.trim().isNotEmpty() && (initial != null || name.trim() !in existingNames)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (initial == null) R.string.role_key_add else R.string.role_key_edit))
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.role_key_name)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.size(8.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.role_key_value)) },
                    placeholder = { Text(stringResource(R.string.role_key_value_hint)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = nameOk && value.isNotBlank(),
                onClick = { onConfirm(name.trim(), value.trim()) }
            ) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/** 接口中心：选接口 → 拉模型 → 勾选 → 确认入库（value=网址@@模型@@Key） */
@Composable
private fun InterfaceCenterDialog(
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

    LaunchedEffect(Unit) {
        val list = withIO { KeyListFile.readInterfaces(tagRuleId) }
        ifaces = list
        pickedName = list.firstOrNull()?.name.orEmpty()
    }
    fun fetch() {
        val p = ifaces.firstOrNull { it.name == pickedName } ?: return
        scope.launch {
            loading = true
            error = ""
            val r = withIO { KeyListFile.fetchModels(p.baseUrl, p.apiKey) }
            loading = false
            if (r.first == null) {
                error = r.second
            } else {
                models = r.first ?: emptyList()
                selected = models.toSet()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.role_key_interface_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.size(8.dp))
                if (ifaces.isEmpty()) {
                    Text(
                        stringResource(R.string.role_key_interface_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(Modifier.weight(1f, fill = false)) {
                        // 接口单选
                        ifaces.forEach { ifc ->
                            item(key = "ifc_${ifc.name}") {
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clickable { pickedName = ifc.name },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = pickedName == ifc.name,
                                        onClick = { pickedName = ifc.name }
                                    )
                                    Column(Modifier.padding(start = 4.dp)) {
                                        Text(ifc.name, style = MaterialTheme.typography.bodyLarge)
                                        if (ifc.baseUrl.isNotBlank()) {
                                            Text(
                                                ifc.baseUrl,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // 拉取按钮 + 错误
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { fetch() }, enabled = !loading) {
                                    Text(
                                        stringResource(
                                            if (loading) R.string.role_key_fetching else R.string.role_key_fetch
                                        )
                                    )
                                }
                                if (loading) {
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                }
                            }
                            if (error.isNotEmpty()) {
                                Text(
                                    stringResource(R.string.role_key_fetch_fail, error),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        // 模型勾选
                        models.forEach { m ->
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
                                        onCheckedChange = {
                                            selected = if (it) selected + m else selected - m
                                        }
                                    )
                                    Text(m, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
                // 页脚
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    if (ifaces.isNotEmpty()) {
                        TextButton(
                            enabled = selected.isNotEmpty() && pickedName.isNotEmpty() && !loading,
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
    }
}

/**
 * 书籍管理：切换（当前数据先存档再载入新书存档）/新增/删除。
 * [onSwitched] 在切换成功后回调（宿主借此刷新角色列表）。
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

    var showAdd by remember { mutableStateOf(false) }
    var switchTo by remember { mutableStateOf<String?>(null) }
    var deleteFor by remember { mutableStateOf<String?>(null) }

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
                    stringResource(R.string.role_book_current, current),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                if (books.size <= 1) {
                    Column(
                        Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            stringResource(R.string.role_book_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(Modifier.weight(1f)) {
                        books.forEach { book ->
                            val isCurrent = book == current
                            item(key = book) {
                                Row(
                                    Modifier.fillMaxWidth()
                                        .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            book,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
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
                                    if (!isCurrent) {
                                        TextButton(onClick = { switchTo = book }) {
                                            Text(stringResource(R.string.role_book_switch))
                                        }
                                        IconButton(onClick = { deleteFor = book }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = null)
                                        }
                                    }
                                }
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
                TextButton(
                    onClick = { showAdd = true },
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.role_book_add))
                }
            }
        }
    }

    // 新增书籍
    if (showAdd) {
        var bookName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
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
                        showAdd = false
                        scope.launch {
                            val ok = withIO { CharacterRecordsFile.addBook(tagRuleId, n) }
                            toast(if (ok) R.string.role_key_saved else R.string.role_list_failed)
                            if (ok) version++
                        }
                    }
                ) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    // 切换书籍（二次确认）
    switchTo?.let { target ->
        AlertDialog(
            onDismissRequest = { switchTo = null },
            title = { Text(stringResource(R.string.role_book_switch)) },
            text = { Text(stringResource(R.string.role_book_switch_confirm, target)) },
            confirmButton = {
                TextButton(onClick = {
                    switchTo = null
                    scope.launch {
                        val ok = withIO { CharacterRecordsFile.switchBook(tagRuleId, target) }
                        toast(if (ok) R.string.role_key_saved else R.string.role_list_failed)
                        if (ok) {
                            version++
                            onSwitched()
                        }
                    }
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { switchTo = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    // 删除书籍（当前书不可删，入口已挡；双保险提示）
    deleteFor?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteFor = null },
            title = { Text(stringResource(R.string.role_book_delete_title)) },
            text = { Text(stringResource(R.string.role_book_delete_text, target)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteFor = null
                    scope.launch {
                        val ok = withIO { CharacterRecordsFile.deleteBook(tagRuleId, target) }
                        toast(
                            if (ok) R.string.role_key_saved
                            else R.string.role_book_cannot_delete_current
                        )
                        if (ok) version++
                    }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteFor = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
