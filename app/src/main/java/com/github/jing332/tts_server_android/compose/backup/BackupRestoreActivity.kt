package com.github.jing332.tts_server_android.compose.backup

import android.content.Intent
import android.os.Bundle
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.github.jing332.compose.widgets.TextCheckBox
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

                val scope = rememberCoroutineScope()

                if (showRestoreMenu) {
                    AlertDialog(
                        onDismissRequest = { showRestoreMenu = false },
                        title = { Text(stringResource(R.string.restore)) },
                        text = {
                            Column(Modifier.fillMaxWidth()) {
                                val filePicker = rememberLauncherForActivityResult(contract = AppActivityResultContracts.filePickerActivity()) { result ->
                                    showRestoreMenu = false
                                    result?.second?.let { uri -> showFromFileRestoreDialog.value = uri.readBytes(this@BackupRestoreActivity) }
                                }
                                ListItem(
                                    modifier = Modifier.clickable {
                                        // 🛠️ 修复：传入 ZIP 专用 MIME 类型，确保系统选择器可以选中 ZIP 文件
                                        filePicker.launch(FilePickerActivity.RequestSelectFile(listOf("application/zip", "application/x-zip-compressed")))
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
                    Column(
                        Modifier
                            .padding(padding)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // ===== 备份顶部分区（用户 09-09）：完整备份 | 分享备份 两区，
                        // SegmentedButton 同配置项编辑页「朗读全部/标签」样式；原备份弹窗内容内联进各区 =====
                        var backupTab by remember { mutableStateOf(0) }
                        val profile =
                            if (backupTab == 0) BackupProfile.PERSONAL_FULL else BackupProfile.SHARE_SANITIZED
                        val checkedTypes = remember { mutableStateListOf<Type>() }
                        // 切区重置为该区默认勾选集（原备份弹窗行为）
                        LaunchedEffect(backupTab) {
                            checkedTypes.clear()
                            checkedTypes.addAll(defaultTypes(profile))
                        }
                        var saveToLocal by remember { mutableStateOf(true) }
                        var uploadToWebDav by remember { mutableStateOf(false) }

                        SingleChoiceSegmentedButtonRow(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                        ) {
                            SegmentedButton(
                                selected = backupTab == 0,
                                onClick = { backupTab = 0 },
                                shape = SegmentedButtonDefaults.itemShape(0, 2),
                            ) { Text(stringResource(R.string.personal_complete_backup), maxLines = 1) }
                            SegmentedButton(
                                selected = backupTab == 1,
                                onClick = { backupTab = 1 },
                                shape = SegmentedButtonDefaults.itemShape(1, 2),
                            ) { Text(stringResource(R.string.share_backup), maxLines = 1) }
                        }
                        Text(
                            text = stringResource(modeWarningRes(profile)),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        Text(
                            text = stringResource(R.string.backup_included_content),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                        )
                        availableTypes(profile).forEach { type ->
                            TextCheckBox(
                                modifier = Modifier.fillMaxWidth(),
                                text = { Text(stringResource(type.nameStrId)) },
                                checked = type in checkedTypes,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        checkedTypes.add(type)
                                    } else {
                                        // 取消"插件"时连带取消"插件变量"
                                        if (type == Type.Plugin) checkedTypes.remove(Type.PluginVars)
                                        if (type == Type.Preference) checkedTypes.remove(Type.WebDav)
                                        checkedTypes.remove(type)
                                    }
                                },
                                horizontalArrangement = Arrangement.Start,
                            )
                        }

                        // 保存到：本地默认 + WebDAV 可选，可同时（原备份弹窗行为）
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Text(
                            text = stringResource(R.string.backup_save_to),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                        TextCheckBox(
                            modifier = Modifier.fillMaxWidth(),
                            text = { Text(stringResource(R.string.backup_save_local)) },
                            checked = saveToLocal,
                            onCheckedChange = { saveToLocal = it },
                            horizontalArrangement = Arrangement.Start,
                        )
                        TextCheckBox(
                            modifier = Modifier.fillMaxWidth(),
                            text = { Text(stringResource(R.string.backup_to_webdav)) },
                            checked = uploadToWebDav,
                            onCheckedChange = { uploadToWebDav = it },
                            horizontalArrangement = Arrangement.Start,
                        )
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = {
                                if (checkedTypes.isEmpty()) {
                                    Toast.makeText(context, context.getString(R.string.backup_need_content), Toast.LENGTH_SHORT).show()
                                    return@TextButton
                                }
                                if (!saveToLocal && !uploadToWebDav) {
                                    Toast.makeText(context, context.getString(R.string.backup_need_save_target), Toast.LENGTH_SHORT).show()
                                    return@TextButton
                                }
                                isLoading = true
                                scope.launch {
                                    runCatching {
                                        val data = vm.backup(profile, checkedTypes.toList())
                                        if (uploadToWebDav) {
                                            vm.uploadToWebDav(data, backupFileName(profile))
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.backup_uploaded_success),
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        }
                                        if (saveToLocal) {
                                            saveFilePicker.launch(
                                                FilePickerActivity.RequestSaveFile(
                                                    fileName = backupFileName(profile),
                                                    fileMime = "application/zip",
                                                    fileBytes = data,
                                                )
                                            )
                                        }
                                    }.onFailure {
                                        context.displayErrorDialog(it, context.getString(R.string.backup))
                                    }
                                    isLoading = false
                                }
                            }) {
                                Text(stringResource(R.string.backup))
                            }
                        }

                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        BasePreferenceWidget(onClick = { showRestoreMenu = true }, title = { Text(stringResource(id = R.string.restore)) }, icon = { Icon(Icons.AutoMirrored.Filled.Input, null) })
                        BasePreferenceWidget(
                            onClick = { showWebDavSettings = true },
                            title = { Text(stringResource(R.string.webdav_settings)) },
                            subTitle = { Text(if (AppConfig.isWebDavConfigured) AppConfig.webDavUrl.value else stringResource(R.string.not_configured)) },
                            icon = { Icon(Icons.Default.Settings, null) }
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

// ===== 备份分区辅助（原 BackupDialog 私有辅助，随内容内联迁入） =====

private fun availableTypes(profile: BackupProfile): List<Type> = when (profile) {
    BackupProfile.PERSONAL_FULL -> Type.typeList
    BackupProfile.SHARE_SANITIZED -> listOf(
        Type.Preference,
        Type.List,
        Type.SpeechRule,
        Type.ReplaceRule,
        Type.Plugin,
    )
}

private fun defaultTypes(profile: BackupProfile): List<Type> = availableTypes(profile)

private fun modeWarningRes(profile: BackupProfile): Int = when (profile) {
    BackupProfile.PERSONAL_FULL -> R.string.personal_backup_sensitive_warning
    BackupProfile.SHARE_SANITIZED -> R.string.share_backup_privacy_warning
}
