package com.github.jing332.tts_server_android.compose.systts.replace

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.ConfigImportBottomSheet
import com.github.jing332.tts_server_android.compose.systts.list.AutoImportResult
import com.github.jing332.tts_server_android.compose.systts.list.doAutoImport
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import com.drake.net.utils.withIO
import kotlinx.coroutines.launch

@Composable
fun ReplaceRuleImportBottomSheet(onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 结果以模态对话框展示，导入过程的连续状态机由 ConfigImportBottomSheet 承载，
    // 用户确认后再 onDismissRequest，避免 AlertDialog 叠在 ModalBottomSheet 上被遮挡。
    var successMsg by remember { mutableStateOf<String?>(null) }
    if (successMsg != null) {
        val msg = successMsg
        AlertDialog(
            onDismissRequest = { successMsg = null; onDismissRequest() },
            confirmButton = {
                TextButton(onClick = { successMsg = null; onDismissRequest() }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            text = { Text(msg) }
        )
        return
    }

    ConfigImportBottomSheet(onDismissRequest = onDismissRequest,
        autoImport = true,
        onResult = { msg -> if (msg != null) successMsg = msg },
        onImport = { json ->
            // 自动识别 JSON 类型并直接导入，无需手动选择/确认
            scope.launch {
                val result = withIO { doAutoImport(json) }
                when (result) {
                    AutoImportResult.EmptyOrUnrecognized -> {
                        successMsg = context.getString(R.string.import_no_valid_config)
                    }
                    is AutoImportResult.Truncated -> {
                        context.displayErrorDialog(
                            Exception(result.detail),
                            title = context.getString(R.string.import_failed)
                        )
                    }
                    is AutoImportResult.Success -> {
                        successMsg = "已导入 ${result.count} 项${result.typeName}"
                    }
                }
            }
        }
    )
}
