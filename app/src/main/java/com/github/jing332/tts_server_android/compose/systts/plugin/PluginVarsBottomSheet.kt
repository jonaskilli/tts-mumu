package com.github.jing332.tts_server_android.compose.systts.plugin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.plugin.login.LoginData
import com.github.jing332.tts_server_android.compose.systts.plugin.login.PluginLoginActivityResult

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun PluginVarsBottomSheet(
    onDismissRequest: () -> Unit,
    plugin: Plugin,
    onPluginChange: (Plugin) -> Unit,
) {
    var currentLoginKey by rememberSaveable { mutableStateOf("") }
    val loginLauncher = rememberLauncherForActivityResult(PluginLoginActivityResult) {
        if (currentLoginKey.isNotEmpty() && it.isNotEmpty())
            onPluginChange(
                plugin.copy(
                    userVars = plugin.userVars.toMutableMap().apply {
                        this[currentLoginKey] = it
                    }
                )
            )
    }

    val state = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        sheetState = state,
        onDismissRequest = onDismissRequest,
    ) {
        Text(
            text = plugin.name,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Column(
            Modifier
                .fillMaxHeight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            plugin.defVars.forEach {
                val key = it.key
                val hint = it.value["hint"] ?: ""
                // jread 系插件 defVars 用 "name" 字段承载显示名,仅取 "label" 会显示为空
                val label = it.value["label"] ?: it.value["name"] ?: ""

                val binding = it.value["binding"] ?: ""
                val loginUrl = it.value["loginUrl"] ?: ""
                val loginDesc = it.value["loginDesc"] ?: ""
                val ua = it.value["ua"] ?: ""
                val value = plugin.userVars.getOrDefault(key, "")

                @Composable
                fun loginIcon(key: String): (@Composable () -> Unit)? {
                    return if (loginUrl.isBlank()) null
                    else {
                        {
                            IconButton(onClick = {
                                currentLoginKey = key
                                loginLauncher.launch(
                                    LoginData(
                                        url = loginUrl,
                                        binding = binding,
                                        description = loginDesc,
                                        ua = ua
                                    )
                                )
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Default.Login,
                                    stringResource(R.string.login)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp, start = 8.dp, end = 8.dp),
                    maxLines = 10,
                    value = value,
                    onValueChange = {
                        onPluginChange(
                            plugin.copy(
                                userVars = plugin.userVars.toMutableMap().apply {
                                    this[key] = it
                                    if (it.isBlank()) this.remove(key)
                                }
                            )
                        )
                    },
                    label = { Text(label) },
                    trailingIcon = loginIcon(key),
                    placeholder = { Text(hint) },
                )
            }
            val scannedFormVars = remember(plugin.code, plugin.defVars) {
                scanSelfDrawnFormVars(plugin.code, plugin.defVars.keys)
            }
            scannedFormVars.forEach { (key, formLabel) ->
                val value = plugin.userVars.getOrDefault(key, "")
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp, start = 8.dp, end = 8.dp),
                    maxLines = 10,
                    value = value,
                    onValueChange = {
                        onPluginChange(
                            plugin.copy(
                                userVars = plugin.userVars.toMutableMap().apply {
                                    this[key] = it
                                    if (it.isBlank()) this.remove(key)
                                }
                            )
                        )
                    },
                    label = { Text(formLabel.ifBlank { key }) },
                    placeholder = { Text(key) },
                )
            }

        }
    }
}

/**
 * 扫描插件 onLoadUI 自绘表单的字面量字段(addInput/addSpinner 等)。
 * jread 系插件(火山豆包 v7 等)defVars 为空、全部变量定义只在自绘表单里,
 * 变量弹窗按 defVars 渲染会一条不剩;把扫出的字段补为变量条目,
 * 值存 userVars,插件 data() 的 userVars 兜底链可读到。与 defVars 条目去重。
 */
private fun scanSelfDrawnFormVars(code: String, existingKeys: Set<String>): Map<String, String> {
    if (code.isBlank()) return emptyMap()
    val result = linkedMapOf<String, String>()
    val re = Regex(
        """(?:addInput|addSpinner|addSwitch|addCheckBox|addEditText)\(\s*"([A-Za-z_][A-Za-z0-9_]*)"\s*,\s*"((?:[^"\\]|\\.)*)"""
    )
    re.findAll(code).forEach { m ->
        val key = m.groupValues[1]
        if (key in existingKeys || result.containsKey(key)) return@forEach
        result[key] = m.groupValues[2]
    }
    return result
}