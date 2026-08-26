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

    // 面板可见性：导入开始后置 false 仅收起 ModalBottomSheet，本 composable 保持挂载，
    // 使 importScope / 全屏遮罩 / 结果弹窗存活，避免协程被取消导致导入静默失败。
    var sheetVisible by remember { mutableStateOf(true) }
    // 导入进行中遮罩（全屏，面板收起后由本 composable 承载，自然置于最上层）
    var isImporting by remember { mutableStateOf(false) }
    // 导入结果文案（成功/失败原因），非 null 时弹出模态对话框
    var successMsg = remember { mutableStateOf<String?>(null) }

    // 先取局部 val 再判空：局部 val 支持 smart cast，MutableState.value 属性不支持
    val msgText = successMsg.value
    if (msgText != null) {
        AlertDialog(
            onDismissRequest = {
                successMsg.value = null
                sheetVisible = false
                onDismissRequest()
            },
            confirmButton = {
                TextButton(onClick = {
                    successMsg.value = null
                    sheetVisible = false
                    onDismissRequest()
                }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            text = { Text(msgText) }
        )
        return
    }

    if (isImporting) {
        LoadingDialog(onDismissRequest = {}, text = context.getString(R.string.importing))
    }

    if (sheetVisible) {
        ConfigImportBottomSheet(onDismissRequest = { sheetVisible = false },
            autoImport = true,
            importScope = importScope,
            sheetVisible = sheetVisible,
            // 开始导入：仅收起面板 + 显示全屏遮罩（不卸载本 composable），
            // 否则 importScope 随组合销毁被取消，导入静默失败。
            onImportStart = { isImporting = true; sheetVisible = false },
            onResult = {
                isImporting = false
                if (it != null) {
                    successMsg.value = it
                } else {
                    // 无结果文案 = 读取/识别/解析失败（错误对话框已另行弹出）：
                    // 重新弹出面板供换源重试，避免「面板不可见但组合仍挂载」导致导入入口卡死
                    sheetVisible = true
                }
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
                        // doAutoImport 已生成完整文案（含数量），直接展示
                        successMsg.value = result.typeName
                    }
                }
            }
        )
    }
}
