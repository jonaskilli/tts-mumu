package com.github.jing332.tts_server_android.compose.backup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color 
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.github.jing332.common.utils.FileUtils.readBytes
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.LoadingDialog
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.ComposeActivity
import com.github.jing332.tts_server_android.compose.settings.BasePreferenceWidget
import com.github.jing332.tts_server_android.compose.theme.AppTheme
import com.github.jing332.tts_server_android.conf.AppConfig
import com.github.jing332.tts_server_android.ui.AppActivityResultContracts
import com.github.jing332.tts_server_android.ui.FilePickerActivity
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import com.thegrizzlylabs.sardineandroid.DavResource 
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRestoreActivity : ComposeActivity() {
    private var showFromFileRestoreDialog = mutableStateOf<ByteArray?>(null)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val vm: BackupRestoreViewModel = viewModel()
                var showBackupDialog by remember { mutableStateOf(false) }
                var showRestoreMenu by remember { mutableStateOf(false) }
                var showWebDavSettings by remember { mutableStateOf(false) }
                var showUrlInputDialog by remember { mutableStateOf(false) }
                var showWebDavListDialog by remember { mutableStateOf(false) }
                var isLoading by remember { mutableStateOf(false) }

                fun backupFileName(profile: BackupProfile): String = when (profile) {
                    BackupProfile.PERSONAL_FULL -> "ttsrv-personal-backup-"
                    BackupProfile.SHARE_SANITIZED -> "ttsrv-share-backup-"
                } + SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date()) + ".zip"

                if (isLoading) LoadingDialog(onDismissRequest = { isLoading = false })

                val context = LocalContext.current
                val saveFilePicker = rememberLauncherForActivityResult(
                    contract = AppActivityResultContracts.filePickerActivity(),
                ) {}
                // 本地备份文件夹选择（10-03）：选定即记住（persistable 权限），
                // 之后本地备份直写该目录、不再每次弹「另存为」
                val dirPicker = rememberLauncherForActivityResult(
                    contract = AppActivityResultContracts.filePickerActivity(),
                ) { result ->
                    if (result.first is FilePickerActivity.RequestSelectDir) {
                        val uri = result.second
                        if (uri != null) {
                            AppConfig.backupDirUri.value = uri.toString()
                            AppConfig.backupDirLabel.value = backupDirLabelOf(context, uri)
                        }
                    }
                }
                val scope = rememberCoroutineScope()

                if (showBackupDialog) {
                    BackupDialog(
                        onDismissRequest = { showBackupDialog = false },
                        localDirLabel = AppConfig.backupDirLabel.value,
                        onPickDir = { dirPicker.launch(FilePickerActivity.RequestSelectDir()) },
                        onBackupRequested = { profile, types, saveToLocal, uploadToWebDav ->
                            if (uploadToWebDav && !AppConfig.isWebDavConfigured) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.config_webdav_first),
                                    Toast.LENGTH_SHORT,
                                ).show()
                                return@BackupDialog
                            }
                            isLoading = true
                            scope.launch {
                                runCatching {
                                    val data = vm.backup(profile, types)
                                    if (uploadToWebDav) {
                                        vm.uploadToWebDav(data, backupFileName(profile))
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.backup_uploaded_success),
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    }
                                    if (saveToLocal) {
                                        val dirUri = AppConfig.backupDirUri.value
                                        if (dirUri.isNotBlank()) {
                                            // 已设文件夹：直写该目录（SAF），不再弹「另存为」（10-03 用户令）
                                            val label = AppConfig.backupDirLabel.value
                                            withContext(Dispatchers.IO) {
                                                writeToBackupDir(context, Uri.parse(dirUri), backupFileName(profile), data)
                                            }
                                            Toast.makeText(
                                                context,
                                                context.getString(
                                                    R.string.backup_dir_saved,
                                                    label.ifBlank { context.getString(R.string.backup) }
                                                ),
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        } else {
                                            saveFilePicker.launch(
                                                FilePickerActivity.RequestSaveFile(
                                                    fileName = backupFileName(profile),
                                                    fileMime = "application/zip",
                                                    fileBytes = data,
                                                )
                                            )
                                        }
                                    }
                                }.onFailure {
                                    context.displayErrorDialog(it, context.getString(R.string.backup))
                                }
                                isLoading = false
                            }
                        },
                    )
                }

                if (showRestoreMenu) {
                    AlertDialog(
                        onDismissRequest = { showRestoreMenu = false },
                        title = { Text(stringResource(R.string.restore)) },
                        text = {
                            Column(Modifier.fillMaxWidth()) {
                                // 从文件恢复：系统单文件选择器，不过滤类型（10-05 用户令「选择不了就别筛选了，
                                // 去掉这个功能」）。RequestSelectFile 默认 listOf("*") → */* 显示全部文件。
                                val filePicker = rememberLauncherForActivityResult(contract = AppActivityResultContracts.filePickerActivity()) { result ->
                                    showRestoreMenu = false
                                    result?.second?.let { uri -> showFromFileRestoreDialog.value = uri.readBytes(this@BackupRestoreActivity) }
                                }
                                ListItem(
                                    modifier = Modifier.clickable {
                                        filePicker.launch(FilePickerActivity.RequestSelectFile())
                                    },
                                    headlineContent = { Text(stringResource(R.string.file_picker_mode_system)) },
                                    leadingContent = { Icon(Icons.Default.FolderOpen, null) },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                                ListItem(
                                    modifier = Modifier.clickable { showRestoreMenu = false; showUrlInputDialog = true },
                                    headlineContent = { Text(stringResource(R.string.restore_from_url_net)) },
                                    leadingContent = { Icon(Icons.Default.Link, null) },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                                val context = LocalContext.current
                                ListItem(
                                    modifier = Modifier.clickable {
                                        showRestoreMenu = false
                                        if (!AppConfig.isWebDavConfigured) {
                                            Toast.makeText(context, context.getString(R.string.config_webdav_first), Toast.LENGTH_SHORT).show()
                                            showWebDavSettings = true
                                        } else { showWebDavListDialog = true }
                                    },
                                    headlineContent = { Text(stringResource(R.string.restore_from_webdav)) },
                                    leadingContent = { Icon(Icons.Default.CloudDownload, null) },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }
                        },
                        confirmButton = { 
                            TextButton(onClick = { showRestoreMenu = false }) { Text(stringResource(R.string.cancel)) } 
                        }
                    )
                }

                if (showUrlInputDialog) {
                    var url by remember { mutableStateOf("") }
                    AppDialog(
                        onDismissRequest = { showUrlInputDialog = false },
                        title = { Text(stringResource(R.string.restore_from_url_dialog_title)) },
                        content = {
                            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text(stringResource(R.string.restore_from_url_dialog_title)) }, modifier = Modifier.fillMaxWidth())
                        },
                        buttons = {
                            TextButton(onClick = { showUrlInputDialog = false }) { Text(stringResource(R.string.cancel)) }
                            TextButton(onClick = {
                                if (url.isBlank()) return@TextButton
                                showUrlInputDialog = false
                                isLoading = true
                                vm.viewModelScope.launch {
                                    runCatching {
                                        val bytes = vm.downloadFromInput(url)
                                        showFromFileRestoreDialog.value = bytes
                                    }.onFailure { displayErrorDialog(it) }
                                    isLoading = false
                                }
                            }) { Text(stringResource(R.string.confirm)) }
                        }
                    )
                }

                if (showWebDavSettings) WebDavSettingsDialog(onDismissRequest = { showWebDavSettings = false }, vm = vm)

                if (showWebDavListDialog) {
                    WebDavListDialog(onDismissRequest = { showWebDavListDialog = false }, vm = vm) { bytes ->
                        showFromFileRestoreDialog.value = bytes
                    }
                }

                if (showFromFileRestoreDialog.value != null) {
                    RestoreDialog(bytes = showFromFileRestoreDialog.value!!, onDismissRequest = { showFromFileRestoreDialog.value = null })
                }

                Scaffold(topBar = {
                    TopAppBar(
                        title = { Text(stringResource(id = R.string.backup_restore)) },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.nav_back))
                            }
                        })
                }) { padding ->
                    Column(Modifier.padding(padding)) {
                        BasePreferenceWidget(
                            onClick = { showBackupDialog = true },
                            title = { Text(stringResource(R.string.backup)) },
                            subTitle = { Text(stringResource(R.string.backup_entry_summary)) },
                            icon = { Icon(Icons.Default.Output, null) },
                            // 弹窗行不出 ›（10-09 口径：进新页面才加）
                            showChevron = false,
                        )
                        BasePreferenceWidget(onClick = { showRestoreMenu = true }, title = { Text(stringResource(id = R.string.restore)) }, icon = { Icon(Icons.AutoMirrored.Filled.Input, null) }, showChevron = false)
                        BasePreferenceWidget(
                            onClick = { showWebDavSettings = true },
                            title = { Text(stringResource(R.string.webdav_settings)) },
                            subTitle = { Text(if (AppConfig.isWebDavConfigured) AppConfig.webDavUrl.value else stringResource(R.string.not_configured)) },
                            icon = { Icon(Icons.Default.Settings, null) },
                            showChevron = false,
                        )
                    }
                }
            }
        }
        restoreFromIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        restoreFromIntent(intent)
    }

    private fun restoreFromIntent(intent: Intent?) {
        intent?.data?.let { uri -> showFromFileRestoreDialog.value = uri.readBytes(this) }
        intent?.data = null
    }

    @Composable
    fun WebDavSettingsDialog(onDismissRequest: () -> Unit, vm: BackupRestoreViewModel) {
        var url by remember { mutableStateOf(AppConfig.webDavUrl.value.ifBlank { AppConfig.DEFAULT_WEBDAV_URL }) }
        var user by remember { mutableStateOf(AppConfig.webDavUser.value) }
        var pass by remember { mutableStateOf(AppConfig.webDavPass.value) }
        var path by remember { mutableStateOf(AppConfig.webDavPath.value) }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        AppDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(stringResource(R.string.webdav_settings)) },
            content = {
                // 水平再让 4dp（叠加 AppDialog 自带 12dp ≈16dp）：与音频参数弹窗同款（边距定调起源，用户 09-09）
                Column(Modifier.padding(horizontal = 4.dp)) {
                    OutlinedTextField(
                        value = url, onValueChange = { url = it }, label = { Text("WebDAV 服务器地址") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                    OutlinedTextField(
                        value = user, onValueChange = { user = it }, label = { Text(stringResource(R.string.account)) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                    OutlinedTextField(
                        value = pass, onValueChange = { pass = it }, label = { Text(stringResource(R.string.password)) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                    OutlinedTextField(
                        value = path, onValueChange = { path = it }, label = { Text(stringResource(R.string.backup_folder)) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                }
            },
            buttons = {
                TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
                TextButton(onClick = {
                    AppConfig.webDavUrl.value = url
                    AppConfig.webDavUser.value = user
                    AppConfig.webDavPass.value = pass
                    AppConfig.webDavPath.value = path
                    scope.launch {
                        runCatching {
                            vm.testWebDav()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, context.getString(R.string.connection_success), Toast.LENGTH_SHORT).show()
                                onDismissRequest()
                            }
                        }.onFailure { context.displayErrorDialog(it) }
                    }
                }) { Text(stringResource(R.string.save_and_test)) }
            }
        )
    }

    @Composable
    fun WebDavListDialog(onDismissRequest: () -> Unit, vm: BackupRestoreViewModel, onFileSelected: (ByteArray) -> Unit) {
        var list by remember { mutableStateOf<List<DavResource>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            runCatching { list = vm.getWebDavBackupFiles() }.onFailure { context.displayErrorDialog(it); onDismissRequest() }
            isLoading = false
        }

        if (isLoading) { LoadingDialog(onDismissRequest = { }) } 
        else {
            AlertDialog(
                onDismissRequest = onDismissRequest,
                title = { Text(stringResource(R.string.select_cloud_backup)) },
                text = {
                    androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxWidth()) {
                        if (list.isEmpty()) { item { Text(stringResource(R.string.empty_folder)) } }
                        items(list.size) { index ->
                            val item = list[index]
                            ListItem(
                                modifier = Modifier.clickable {
                                    scope.launch {
                                        isLoading = true
                                        runCatching {
                                            val bytes = vm.downloadFromWebDav(item.name)
                                            onFileSelected(bytes)
                                            onDismissRequest()
                                        }.onFailure { context.displayErrorDialog(it) }
                                        isLoading = false
                                    }
                                },
                                headlineContent = { Text(item.name) },
                                supportingContent = { Text(Formatter.formatFileSize(context, item.contentLength)) },
                                leadingContent = { Icon(Icons.Default.Cloud, null) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                },
                confirmButton = { TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) } }
            )
        }
    }
}

private fun backupDirLabelOf(context: android.content.Context, uri: Uri): String {
    val path = runCatching {
        com.github.jing332.common.utils.ASFUriUtils.getPathFromTree(context, uri)
    }.getOrNull()
    if (!path.isNullOrBlank()) return path
    val displayName = runCatching {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(
            uri, DocumentsContract.getTreeDocumentId(uri)
        )
        context.contentResolver.query(
            docUri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull()
    return displayName ?: uri.toString()
}

/** 直写已设备份文件夹（10-03 用户令）：同名覆盖、无则新建，不弹系统「另存为」。
 *  写入模式 "w" 与 FilePickerActivity.saveToUri 同款（minSdk 21 不启用 API 26+ 的 "wt"）。 */
private fun writeToBackupDir(context: android.content.Context, dirUri: Uri, fileName: String, bytes: ByteArray) {
    val resolver = context.contentResolver
    val treeDocId = DocumentsContract.getTreeDocumentId(dirUri)
    val parentDoc = DocumentsContract.buildDocumentUriUsingTree(dirUri, treeDocId)
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(dirUri, treeDocId)
    // 目录内找同名文件（有则覆盖，无则新建）
    val existingId = run {
        var id: String? = null
        resolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            ),
            null, null, null
        )?.use { c ->
            while (c.moveToNext()) {
                if (c.getString(1) == fileName) {
                    id = c.getString(0)
                    break
                }
            }
        }
        id
    }
    val target = if (existingId != null) {
        DocumentsContract.buildDocumentUriUsingTree(dirUri, existingId)
    } else {
        DocumentsContract.createDocument(resolver, parentDoc, "application/zip", fileName)
            ?: throw Exception("无法在备份文件夹中创建文件")
    }
    val out = resolver.openOutputStream(target, "w") ?: throw Exception("无法写入备份文件夹")
    out.use { it.write(bytes) }
}
