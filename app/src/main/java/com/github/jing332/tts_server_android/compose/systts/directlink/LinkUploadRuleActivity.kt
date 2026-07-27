package com.github.jing332.tts_server_android.compose.systts.directlink

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.ComposeActivity
import com.github.jing332.tts_server_android.compose.codeeditor.CodeEditorScreen
import com.github.jing332.tts_server_android.compose.codeeditor.LoggerBottomSheet
import com.github.jing332.tts_server_android.compose.theme.AppTheme
import com.github.jing332.tts_server_android.conf.DirectUploadConfig
import com.github.jing332.tts_server_android.model.rhino.direct_link_upload.DirectUploadFunction
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LinkUploadRuleActivity : ComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                LinkUploadRuleScreen()
            }
        }
    }


    @Composable
    private fun LinkUploadRuleScreen(vm: LinkUploadRuleViewModel = viewModel()) {
        var editor by remember { mutableStateOf<CodeEditor?>(null) }
        var targets by remember { mutableStateOf<List<DirectUploadFunction>?>(null) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(editor) {
            editor?.setText(DirectUploadConfig.code.value)
        }

        var showDebugLogger by remember { mutableStateOf<DirectUploadFunction?>(null) }
        if (showDebugLogger != null)
            LoggerBottomSheet(
                registry = vm.console,
                onDismissRequest = { showDebugLogger = null }) {
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        vm.invoke(showDebugLogger!!)
                    }.onFailure {
                        this@LinkUploadRuleActivity.displayErrorDialog(it)
                    }
                }
            }

        CodeEditorScreen(
            title = { Text(stringResource(id = R.string.direct_link_settings)) },
            onBack = { finishAfterTransition() },
            onSave = {
                runCatching {
                    vm.updateCode(editor!!.text.toString())
                    vm.save()
                    DirectUploadConfig.code.value = editor!!.text.toString()

                    finishAfterTransition()
                }.onFailure {
                    this.displayErrorDialog(it)
                }
            },
            onUpdate = { editor = it },
            onDebug = {
                kotlin.runCatching {
                    vm.updateCode(editor!!.text.toString())
                    targets = vm.debug()
                    println(targets)
                }.onFailure {
                    this.displayErrorDialog(it)
                }
            },
            debugIconContent = {
                DropdownMenu(expanded = targets != null, onDismissRequest = { targets = null }) {
                    targets?.forEach {
                        DropdownMenuItem(text = { Text(it.name) }, onClick = {
                            targets = null
                            showDebugLogger = it
                        })
                    }
                }
            },
            onSaveFile = {
                "ttsrv-directLink.js" to editor!!.text.toString().toByteArray()
            }
        )
    }

}