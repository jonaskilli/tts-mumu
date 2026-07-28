package com.github.jing332.tts_server_android.compose.backup

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.TextCheckBox
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.conf.AppConfig
import com.github.jing332.tts_server_android.ui.AppActivityResultContracts
import com.github.jing332.tts_server_android.ui.FilePickerActivity
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun BackupDialog(
    onDismissRequest: () -> Unit,
    vm: BackupRestoreViewModel = viewModel(),
) {
    val filePicker =
        rememberLauncherForActivityResult(contract = AppActivityResultContracts.filePickerActivity())
        {}

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uploadToWebDav by remember { mutableStateOf(false) }
    
    val checkedList = remember {
        mutableStateListOf(
            Type.Preference,
            Type.List,
            Type.ReplaceRule,
            Type.SpeechRule,
            Type.Plugin
        )
    }
    
    AppDialog(onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.backup)) },
        content = {
            LazyColumn(Modifier.fillMaxWidth()) {
                items(Type.typeList) {
                    TextCheckBox(
                        modifier = Modifier.fillMaxWidth(),
                        text = { Text(stringResource(id = it.nameStrId)) },
                        checked = checkedList.contains(it),
                        onCheckedChange = { check ->
                            if (check) {
                                if (it == Type.PluginVars) {
                                    checkedList.contains(Type.Plugin) || checkedList.add(Type.Plugin)
                                }
                                checkedList.add(it)
                            } else {
                                if (it == Type.Plugin) checkedList.remove(Type.PluginVars)
                                checkedList.remove(it)
                            }
                        },
                        horizontalArrangement = Arrangement.Start // 👈 统一对齐
                    )
                }
                
                item {
                    val configFirst = stringResource(R.string.config_webdav_first)
                    TextCheckBox(
                        modifier = Modifier.fillMaxWidth(),
                        text = { Text(stringResource(R.string.backup_to_webdav)) },
                        checked = uploadToWebDav,
                        onCheckedChange = { 
                             if (it && AppConfig.webDavUrl.value.isBlank()) {
                                 Toast.makeText(context, configFirst, Toast.LENGTH_SHORT).show()
                             } else {
                                 uploadToWebDav = it 
                             }
                        },
                        horizontalArrangement = Arrangement.Start // 👈 修复排列不一致
                    )
                }
            }
        },
        buttons = {
            // 👈 按钮顺序：取消在左，确定在右
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(id = R.string.cancel))
            }

            TextButton(onClick = {
                scope.launch {
                    runCatching {
                        val data = vm.backup(checkedList)
                        val fileName = "ttsrv-backup-" + SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date()) + ".zip"
                        
                        if (uploadToWebDav) {
                            vm.uploadToWebDav(data, fileName)
                            Toast.makeText(context, context.getString(R.string.backup_uploaded_success), Toast.LENGTH_LONG).show()
                        } else {
                            filePicker.launch(
                                FilePickerActivity.RequestSaveFile(
                                    fileName = fileName,
                                    fileMime = "application/zip",
                                    fileBytes = data
                                )
                            )
                        }
                    }.onFailure {
                        context.displayErrorDialog(it, context.getString(R.string.backup))
                    }
                    onDismissRequest()
                }
            }) {
                Text(stringResource(id = R.string.confirm))
            }
        }
    )
}
