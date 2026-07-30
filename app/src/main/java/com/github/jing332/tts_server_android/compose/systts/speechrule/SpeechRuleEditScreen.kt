package com.github.jing332.tts_server_android.compose.systts.speechrule

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.jing332.common.utils.FileUtils.readAllText
import com.github.jing332.common.utils.longToast
import com.github.jing332.compose.widgets.TextFieldDialog
import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.LocalNavController
import com.github.jing332.tts_server_android.compose.codeeditor.CodeEditorScreen
import com.github.jing332.tts_server_android.compose.codeeditor.LoggerBottomSheet
import com.github.jing332.tts_server_android.compose.codeeditor.string
import com.github.jing332.tts_server_android.conf.SpeechRuleConfig
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher

@Composable
internal fun SpeechRuleEditScreen(
    rule: SpeechRule,
    onSave: (SpeechRule) -> Unit,
    vm: SpeechRuleEditViewModel = viewModel(),
    // 第11项: 由列表项"运行键"传入，true 时等代码加载完成后自动弹出调试面板并执行
    autoDebug: Boolean = false,
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    var codeEditor by remember { mutableStateOf<CodeEditor?>(null) }

    LaunchedEffect(vm, codeEditor) {
        vm.init(rule, context.assets.open("defaultData/speech_rule.js").readAllText())
    }

    val code by vm.codeLiveData.asFlow().collectAsState(initial = "")
    LaunchedEffect(code, codeEditor) {
        if (codeEditor != null && code.isNotEmpty())
            codeEditor?.setText(code)
    }

    var showTextParamDialog by remember { mutableStateOf(false) }
    if (showTextParamDialog) {
        var textParam by remember { mutableStateOf(SpeechRuleConfig.textParam.value) }
        TextFieldDialog(
            title = stringResource(id = R.string.set_sample_text_param),
            text = textParam,
            onDismissRequest = { showTextParamDialog = false },
            onTextChange = { textParam = it },
            onConfirm = {
                SpeechRuleConfig.textParam.value = textParam
                showTextParamDialog = false
            }
        )
    }

    // 搜索和还原剪贴板功能已内置到 CodeEditorScreen，此处无需重复

    var showDebugLogger by remember { mutableStateOf(false) }
    if (showDebugLogger) {
        LoggerBottomSheet(registry = vm.getConsole(), onDismissRequest = { showDebugLogger = false }) {
            runCatching {
                vm.updateCode(codeEditor!!.string())
                vm.debug(SpeechRuleConfig.textParam.value)
            }.onFailure {
                context.displayErrorDialog(it)
            }
        }
    }

    // 第11项: autoDebug 模式下，等代码加载完成自动弹出调试面板（onLaunched 会立即执行 debug）
    var autoDebugTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(autoDebug, code, codeEditor) {
        if (autoDebug && !autoDebugTriggered && code.isNotEmpty() && codeEditor != null) {
            autoDebugTriggered = true
            showDebugLogger = true
        }
    }

    CodeEditorScreen(
        title = { Text("规则") },
        onBack = { navController.popBackStack() },
        onDebug = { showDebugLogger = true },
        onSave = {
            runCatching {
                vm.evalRuleInfo(codeEditor!!.string())

                onSave(vm.speechRule)
                navController.popBackStack()
            }.onFailure {
                context.displayErrorDialog(it)
            }
        },
        onUpdate = { codeEditor = it },
        onSaveFile = {
            "ttsrv-speechRule-${vm.speechRule.name}.js" to codeEditor!!.text.toString()
                .toByteArray()
        },
        foldStateKey = "speechRule_${rule.id}"
    ) { dismiss ->
        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.set_sample_text_param)) },
            onClick = {
                dismiss()
                showTextParamDialog = true
            },
            leadingIcon = {
                Icon(Icons.Default.TextFields, stringResource(R.string.set_sample_text_param))
            }
        )
    }
}