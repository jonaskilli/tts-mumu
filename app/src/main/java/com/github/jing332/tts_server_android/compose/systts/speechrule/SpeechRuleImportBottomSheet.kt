package com.github.jing332.tts_server_android.compose.systts.speechrule

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
import com.github.jing332.compose.widgets.LoadingDialog
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.ConfigImportBottomSheet
import com.github.jing332.tts_server_android.compose.systts.list.AutoImportResult
import com.github.jing332.tts_server_android.compose.systts.list.doAutoImport
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import com.drake.net.utils.withIO
import kotlinx.coroutines.CoroutineScope

@Composable
fun SpeechRuleImportBottomSheet(onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    // 导入协程作用域：独立于 BottomSheet 生命周期，关闭面板后导入仍继续
    val importScope = rememberCoroutineScope()

    // 导入进行中遮罩（全屏，在 BottomSheet 关闭后由本 composable 承载，自然置于最上层）
    var isImporting by remember { mutableStateOf(false) }
    if (isImporting) {
        LoadingDialog(onDismissRequest = {}, text = context.getString(R.string.importing))
    }

    // 结果以模态对话框展示，导入完成后展示，避免叠在 ModalBottomSheet 上被遮挡。
    var successMsg = remember { mutableStateOf<String?>(null) }
    // 先取局部 val 再判空：局部 val 支持 smart cast，MutableState.value 属性不支持
    val msgText = successMsg.value
    if (msgText != null) {
        AlertDialog(
            onDismissRequest = { successMsg.value = null; onDismissRequest() },
            confirmButton = {
                TextButton(onClick = { successMsg.value = null; onDismissRequest() }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            text = { Text(msgText) }
        )
        return
    }

    ConfigImportBottomSheet(onDismissRequest = onDismissRequest,
        autoImport = true,
        importScope = importScope,
        // 开始导入：立即关闭 BottomSheet + 显示全屏遮罩，避免遮罩被面板挡住、也无需保留面板
        onImportStart = { isImporting = true; onDismissRequest() },
        onResult = {
            isImporting = false
            if (it != null) successMsg.value = it
        },
        onImport = { json ->
            // 自动识别 JSON 类型并直接导入，无需手动选择/确认
            // （suspend lambda：在 importScope 内执行，勿再自起协程）
            val result = withIO { doAutoImport(json, context = context) }
            when (result) {
                is AutoImportResult.EmptyOrUnrecognized -> {
                    context.displayErrorDialog(
                        Exception(result.reason),
                        title = context.getString(R.string.import_no_valid_config)
                    )
                }
                is AutoImportResult.Truncated -> {
                    context.displayErrorDialog(
                        Exception(result.detail),
                        title = context.getString(R.string.import_failed)
                    )
                }
                is AutoImportResult.Success -> {
                    successMsg.value = "已导入 ${result.count} 项${result.typeName}"
                }
            }
        }
    )
}
