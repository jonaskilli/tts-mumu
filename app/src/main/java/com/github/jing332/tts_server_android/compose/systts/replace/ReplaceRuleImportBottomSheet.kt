package com.github.jing332.tts_server_android.compose.systts.replace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
fun ReplaceRuleImportBottomSheet(onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var importing by remember { mutableStateOf(false) }
    if (importing) {
        LoadingDialog(onDismissRequest = { /* 不可取消，等待导入完成 */ })
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
                        context.toast("已导入 ${result.count} 项${result.typeName}")
                        onDismissRequest()
                    }
                }
            }
        }
    )
}
