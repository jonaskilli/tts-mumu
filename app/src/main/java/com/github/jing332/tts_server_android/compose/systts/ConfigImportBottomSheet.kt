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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
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
    // suspend：写库等全部完成后才返回，遮罩覆盖「读取→解析→写库」全程
    onImport: suspend (json: String) -> Unit,
    // 导入结果回调：null 表示进行中；非 null 为结果文案（成功/失败）。
    // 由调用方决定如何展示（如 AlertDialog），实现单一结果出口。
    onResult: (String?) -> Unit = {},
    // 为 true 时：选好文件/填好URL后无需再点「导入」按钮，直接触发导入。
    // TTS 配置列表导入用 true（全自动），插件/替换规则等仍需手动确认(默认 false)。
    autoImport: Boolean = false,
    // 导入开始回调：launchImport 真正开始前触发（在「读取」动作之前）。
    // 调用方据此关闭 BottomSheet 并展示全屏「导入中」遮罩——避免遮罩被面板挡住。
    onImportStart: () -> Unit = {},
    // 承载导入协程的 scope。需由调用方传入（而非内部 scope），
    // 否则 onImportStart 关闭 BottomSheet 后本组合销毁、协程被取消，导入中断。
    importScope: CoroutineScope? = null,
    // 底部面板是否可见。导入开始（onImportStart）后由调用方置为 false 仅收起面板，
    // 但本组合保持挂载，使 importScope / 全屏遮罩 / 结果弹窗得以存活，导入不被取消。
    sheetVisible: Boolean = true,
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

    // 统一的导入触发逻辑，文件选择回调和「导入」按钮共用。
    // 注意：导入过程中的「导入中」遮罩由调用方承载（见 onImportStart），
    // 因为 BottomSheet 关闭后本组合会因 composition 销毁而丢失内部状态，
    // 且普通 Dialog 遮罩会被 Material3 BottomSheet 的高层级挡住。
    fun launchImport(src: Int, urlStr: String? = null, uri: Uri? = null) {
        onImportStart()
        (importScope ?: scope).launch {
            // 只读取一次：失败时复用同一次的结果取异常，避免重复网络请求/文件读取
            val result = runCatching { getConfig(src = src, url = urlStr, uri = uri) }
            val jsonStr = result.getOrNull()
            if (jsonStr == null) {
                val err = result.exceptionOrNull()
                    ?: Exception(context.getString(R.string.import_failed))
                onResult(null)
                context.displayErrorDialog(err)
                return@launch
            }
            // 直接传原始内容：截断检测与遗留格式兼容统一由 doAutoImport 处理。
            // 此前先 toJsonListString() 补括号会把截断的 JSON "修"成合法 JSON，静默导入部分数据
            onImport(jsonStr)
            // 导入结束（无论成败，结果已在 onImport 内部处理）通知调用方关闭遮罩
            onResult(null)
        }
    }

    var source by remember { mutableIntStateOf(0) }
    var path by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    // 外部自动导入且已带预设（文件/URL）时，跳过底部面板，直接进入状态机。
    // 用 LaunchedEffect 确保只触发一次，避免 recompose 重复启动。
    val presetUrl = LocalImportRemoteUrl.current.value
    val presetPath = LocalImportFilePath.current.value
    val hasPreset = autoImport && (presetUrl.isNotBlank() || presetPath.isNotBlank())
    if (hasPreset) {
        LocalImportRemoteUrl.current.value = ""
        LocalImportFilePath.current.value = ""
        androidx.compose.runtime.LaunchedEffect(Unit) {
            if (presetUrl.isNotBlank()) launchImport(ImportSource.URL, urlStr = presetUrl)
            else launchImport(ImportSource.FILE, uri = Uri.parse(presetPath))
        }
        return
    }

    if (sheetVisible) ModalBottomSheet(onDismissRequest = onDismissRequest) {
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
                                            // autoImport 模式下选好文件直接导入，无需再点「导入」按钮
                                            if (autoImport) launchImport(ImportSource.FILE, uri = uri)
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
                        launchImport(source, urlStr = url, uri = Uri.parse(path))
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