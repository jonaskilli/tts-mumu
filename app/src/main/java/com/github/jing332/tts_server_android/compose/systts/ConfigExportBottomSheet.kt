package com.github.jing332.tts_server_android.compose.systts

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.jing332.common.utils.ClipboardUtils
import com.github.jing332.common.utils.toast
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.directlink.LinkUploadSelectionDialog
import com.github.jing332.tts_server_android.ui.AppActivityResultContracts
import com.github.jing332.tts_server_android.ui.FilePickerActivity
import com.github.jing332.tts_server_android.ui.view.BigTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 第1项: 大文本只预览前 32KB, 避免 TextView 对 MB 级全文排版导致卡顿。
private const val PREVIEW_MAX_CHARS = 32 * 1024

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigExportBottomSheet(
    json: String,
    fileName: String = "config.json",
    content: @Composable ColumnScope.() -> Unit = {},
    onDismissRequest: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val fileSaver =
        rememberLauncherForActivityResult(AppActivityResultContracts.filePickerActivity()) {
        }

    var showSelectUploadTargetDialog by remember { mutableStateOf(false) }
    if (showSelectUploadTargetDialog)
        LinkUploadSelectionDialog(
            onDismissRequest = { showSelectUploadTargetDialog = false },
            json = json
        )

    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            Modifier
                .fillMaxHeight()
                .padding(horizontal = 8.dp)
        ) {
            content()
            Row(Modifier.align(Alignment.CenterHorizontally)) {
                TextButton(
                    onClick = {
                        ClipboardUtils.copyText(json)
                        context.toast(R.string.copied)
                    }
                ) {
                    Text(stringResource(id = R.string.copy))
                }

                TextButton(
                    onClick = {
                        showSelectUploadTargetDialog = true
                    }
                ) {
                    Text(stringResource(id = R.string.upload_to_url))
                }

                TextButton(
                    onClick = {
                        // 第1项: toByteArray 移到 IO 线程, 避免大 JSON 在主线程转 ByteArray 卡顿
                        val src = json
                        val name = fileName
                        scope.launch {
                            val bytes = withContext(Dispatchers.IO) { src.toByteArray() }
                            fileSaver.launch(
                                FilePickerActivity.RequestSaveFile(
                                    fileName = name,
                                    fileMime = "application/json",
                                    fileBytes = bytes
                                )
                            )
                        }
                    }) {
                    Text(stringResource(id = R.string.save_as_file))
                }
            }

            // 第1项: 大文本截断提示
            val isTruncated = json.length > PREVIEW_MAX_CHARS
            if (isTruncated) {
                Text(
                    text = "内容过长(${json.length}字符), 仅显示前 ${PREVIEW_MAX_CHARS} 字符预览, 完整内容请复制或保存为文件",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            var tv by remember {
                mutableStateOf<BigTextView?>(null)
            }

            AndroidView(modifier = Modifier.verticalScroll(rememberScrollState()), factory = {
                tv = BigTextView(it)
                tv!!
            })

            // 第1项: 只把预览子串交给 TextView, 避免全文排版卡顿
            LaunchedEffect(key1 = json) {
                tv?.setText(if (isTruncated) json.substring(0, PREVIEW_MAX_CHARS) else json)
            }
        }
    }
}
