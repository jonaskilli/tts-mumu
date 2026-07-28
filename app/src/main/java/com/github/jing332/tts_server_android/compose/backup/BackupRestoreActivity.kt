package com.github.jing332.tts_server_android.compose.backup

import android.content.Intent
import android.os.Bundle
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

                if (isLoading) LoadingDialog(onDismissRequest = { isLoading = false })

                if (showBackupDialog) BackupDialog(onDismissRequest = { showBackupDialog = false })

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
                                        if (AppConfig.webDavUrl.value.isBlank()) {
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
                        title = { Text(stringResource(R.string.import_from_url)) },
                        content = {
                            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, modifier = Modifier.fillMaxWidth())
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
                        BasePreferenceWidget(onClick = { showBackupDialog = true }, title = { Text(stringResource(id = R.string.backup)) }, icon = { Icon(Icons.Default.Output, null) })
                        BasePreferenceWidget(onClick = { showRestoreMenu = true }, title = { Text(stringResource(id = R.string.restore)) }, icon = { Icon(Icons.AutoMirrored.Filled.Input, null) })
                        BasePreferenceWidget(
                            onClick = { showWebDavSettings = true },
                            title = { Text(stringResource(R.string.webdav_settings)) },
                            subTitle = { Text(if (AppConfig.webDavUrl.value.isBlank()) stringResource(R.string.not_configured) else AppConfig.webDavUrl.value) },
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
        var url by remember { mutableStateOf(AppConfig.webDavUrl.value) }
        var user by remember { mutableStateOf(AppConfig.webDavUser.value) }
        var pass by remember { mutableStateOf(AppConfig.webDavPass.value) }
        var path by remember { mutableStateOf(AppConfig.webDavPath.value) }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        AppDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(stringResource(R.string.webdav_settings)) },
            content = {
                Column {
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
