package com.github.jing332.tts_server_android.compose.systts.replace

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.github.jing332.common.utils.longToast
import com.github.jing332.common.utils.toast
import com.github.jing332.compose.widgets.LoadingDialog
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.ConfigImportBottomSheet
import com.github.jing332.tts_server_android.compose.systts.list.AutoImportResult
import com.github.jing332.tts_server_android.compose.systts.list.doAutoImport
import com.drake.net.utils.withIO
import kotlinx.coroutines.launch

@Composable
fun ReplaceRuleImportBottomSheet(onDismissRequest: () -> Unit, showSuccessDialog: Boolean = false) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var importing by remember { mutableStateOf(false) }
    if (importing) {
        LoadingDialog(onDismissRequest = { /* 不可取消，等待导入完成 */ })
    }

    var successMsg by remember { mutableStateOf<String?>(null) }
    successMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { successMsg = null; onDismissRequest() },
            confirmButton = {
                TextButton(onClick = { successMsg = null; onDismissRequest() }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            text = { Text(msg) }
        )
    }

    ConfigImportBottomSheet(onDismissRequest = onDismissRequest,
        autoImport = true,
        onImport = { json ->
            // 自动识别 JSON 类型并直接导入，无需手动选择/确认
            importing = true
            scope.launch {
                val result = withIO { doAutoImport(json) }
                importing = false
                when (result) {
                    AutoImportResult.EmptyOrUnrecognized -> {
                        context.longToast(R.string.import_no_valid_config)
                    }
                    is AutoImportResult.Truncated -> {
                        context.longToast(R.string.import_truncated_hint, result.detail)
                    }
                    is AutoImportResult.Success -> {
                        if (showSuccessDialog) {
                            successMsg = "已导入 ${result.count} 项${result.typeName}"
                        } else {
                            context.toast("已导入 ${result.count} 项${result.typeName}")
                            onDismissRequest()
                        }
                    }
                }
            }
        }
    )
}
