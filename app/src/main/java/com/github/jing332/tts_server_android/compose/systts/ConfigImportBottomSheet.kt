package com.github.jing332.tts_server_android.compose.systts

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.drake.net.Net
import com.drake.net.okhttp.trustSSLCertificate
import com.drake.net.utils.withMain
import com.github.jing332.common.utils.ClipboardUtils
import com.github.jing332.common.utils.FileUtils.readAllText
import com.github.jing332.common.utils.longToast
import com.github.jing332.common.utils.toJsonListString
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.ui.AppActivityResultContracts
import com.github.jing332.tts_server_android.ui.FilePickerActivity
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Response

class ImportSource {
    companion object {
        const val CLIPBOARD = 0
        const val FILE = 1
        const val URL = 2
    }
}

val LocalImportRemoteUrl = compositionLocalOf { mutableStateOf("") }
val LocalImportFilePath = compositionLocalOf { mutableStateOf("") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigImportBottomSheet(
    content: @Composable ColumnScope.() -> Unit = {},
    onDismissRequest: () -> Unit,
    onImport: (json: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    suspend fun getConfig(
        src: Int,
        url: String? = null,
        uri: Uri? = null,
    ): String {
        return when (src) {
            ImportSource.URL -> withContext(Dispatchers.IO) {
                val resp: Response = Net.get(url.toString()) {
                    setClient { trustSSLCertificate() }
                }.execute()
                resp.use {
                    val str = resp.body?.string()
                    if (resp.isSuccessful && !str.isNullOrBlank()) str
                    else throw Exception("GET $url failed: code=${resp.code}, message=${resp.message}, body=${str}")
                }
            }

            ImportSource.FILE -> withContext(Dispatchers.IO) {
                uri?.readAllText(context) ?: throw Exception("file uri is null!")
            }

            ImportSource.CLIPBOARD -> withMain { ClipboardUtils.text.toString() } // CLIPBOARD

            else -> throw IllegalArgumentException("unknown source: $src")
        }
    }

    var source by remember { mutableIntStateOf(0) }
    var path by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            Modifier
                .padding(horizontal = 8.dp)
                .fillMaxHeight()
        ) {
            Column(
                Modifier
                    .weight(weight = 1f, fill = false)
                    .align(Alignment.Start)
            ) {
                Text(
                    stringResource(id = R.string.import_config),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.displayMedium
                )

                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    content()

                    Text(
                        stringResource(id = R.string.source),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        style = MaterialTheme.typography.titleMedium
                    )

                    SingleChoiceSegmentedButtonRow(
                        Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        val items = remember {
                            listOf(
                                R.string.clipboard to Icons.Default.ContentCopy,
                                R.string.file to Icons.Default.FileCopy,
                                R.string.url_net to Icons.Default.Link
                            )
                        }
                        items.forEachIndexed { index, item ->
                            SegmentedButton(
                                selected = source == index,
                                onClick = { source = index },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    items.size
                                ),
                                icon = { Icon(item.second, stringResource(item.first)) }
                            ) {
                                Text(stringResource(item.first), maxLines = 1)
                            }
                        }
                    }

                    if (LocalImportRemoteUrl.current.value.isNotBlank()) {
                        source = ImportSource.URL
                        url = LocalImportRemoteUrl.current.value
                        LocalImportRemoteUrl.current.value = ""
                    } else if (LocalImportFilePath.current.value.isNotBlank()) {
                        source = ImportSource.FILE
                        path = LocalImportFilePath.current.value
                        LocalImportFilePath.current.value = ""
                    }

                    AnimatedVisibility(
                        modifier = Modifier.animateContentSize(),
                        visible = source != ImportSource.CLIPBOARD
                    ) {
                        when (source) {
                            ImportSource.URL -> OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = url,
                                onValueChange = { url = it },
                                label = { Text(stringResource(R.string.url_net)) },
                            )

                            ImportSource.FILE -> {
                                val filePicker =
                                    rememberLauncherForActivityResult(contract = AppActivityResultContracts.filePickerActivity()) {
                                        it.second?.let { uri ->
                                            path = uri.toString()
                                        }
                                    }

                                OutlinedTextField(
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    value = path,
                                    onValueChange = { path = it },
                                    label = { Text(stringResource(R.string.file)) },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            filePicker.launch(
                                                FilePickerActivity.RequestSelectFile(
                                                    listOf("application/json", "text/*")
                                                )
                                            )
                                        }) {
                                            Icon(
                                                Icons.Default.FileOpen,
                                                stringResource(id = R.string.select_file)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }


            Box(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.End)
                    .padding(top = 8.dp)
            ) {
                TextButton(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onClick = {
                        scope.launch {
                            runCatching {
                                val jsonStr =
                                    getConfig(src = source, url = url, uri = Uri.parse(path))
                                onImport(jsonStr.toJsonListString())
                            }.onFailure {
                                context.displayErrorDialog(it)
                            }
                        }
                    }) {
                    Row {
                        Icon(Icons.AutoMirrored.Default.Input, stringResource(R.string.import_config))
                        Text(stringResource(id = R.string.import_config))
                    }
                }
            }
        }
    }
}

data class ConfigModel(
    val isSelected: Boolean,
    val title: String,
    val subtitle: String,
    val data: Any,
)

@Composable
fun SelectImportConfigDialog(
    onDismissRequest: () -> Unit,
    models: List<ConfigModel>,
    onSelectedList: (list: List<Any>) -> Int,
) {
    val context = LocalContext.current
    val modelsState = remember { mutableStateListOf(*models.toTypedArray()) }
    AppDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.select_import)) },
        content = {
            LazyColumn {
                itemsIndexed(modelsState, key = { i, _ -> i }) { index, item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clip(MaterialTheme.shapes.small)
                            .clickable(role = Role.Checkbox) {
                                modelsState[index] = item.copy(isSelected = !item.isSelected)
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = item.isSelected,
                            onCheckedChange = null,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Column(Modifier.padding(start = 4.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Text(item.subtitle, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        buttons = {
            TextButton(onClick = {
                val count =
                    onSelectedList.invoke(modelsState.filter { it.isSelected }.map { it.data })
                if (count > 0) {
                    onDismissRequest()
                    context.longToast(R.string.config_import_success_msg, count)
                }
            }) {
                Text(stringResource(id = R.string.import_config))
            }
        }
    )
}