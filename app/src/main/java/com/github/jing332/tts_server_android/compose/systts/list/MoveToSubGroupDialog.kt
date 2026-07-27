package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MoveToSubGroupDialog(
    existingPaths: List<String>,
    onDismissRequest: () -> Unit,
    onConfirm: (selectedPath: String) -> Unit,
) {
    var selectedPath by remember { mutableStateOf("") }
    var newPath by remember { mutableStateOf("") }
    var isCreatingNew by remember { mutableStateOf(false) }
    val isNewPath = newPath.isNotBlank() || isCreatingNew
    val finalPath = if (isNewPath) newPath.trim() else selectedPath

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("移动到子分组") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "选择已有子分组或输入新名称",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 已有子分组列表
                if (existingPaths.isNotEmpty()) {
                    Text(
                        text = "已有子分组",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    existingPaths.forEach { path ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedPath == path && !isNewPath,
                                    onClick = {
                                        selectedPath = path
                                        newPath = ""
                                        isCreatingNew = false
                                    }
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPath == path && !isNewPath,
                                onClick = {
                                    selectedPath = path
                                    newPath = ""
                                    isCreatingNew = false
                                }
                            )
                            Text(
                                text = path,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // 新建子分组
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isNewPath,
                            onClick = {
                                selectedPath = ""
                                isCreatingNew = true
                            }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isNewPath,
                        onClick = {
                            selectedPath = ""
                            isCreatingNew = true
                        }
                    )
                    Text(
                        text = "新建子分组",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                if (isNewPath || existingPaths.isEmpty()) {
                    TextField(
                        value = newPath,
                        onValueChange = { newPath = it },
                        label = { Text("子分组名称 (支持 中文/男声 多级)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 48.dp, top = 4.dp),
                        singleLine = true
                    )
                }

                // 根分组选项
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = finalPath.isBlank() && !isNewPath,
                            onClick = {
                                selectedPath = ""
                                newPath = ""
                                isCreatingNew = false
                            }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = finalPath.isBlank() && !isNewPath,
                        onClick = {
                            selectedPath = ""
                            newPath = ""
                            isCreatingNew = false
                        }
                    )
                    Text(
                        text = "移出子分组（放回根目录）",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(finalPath) },
                enabled = finalPath.isNotBlank() || (selectedPath.isBlank() && newPath.isBlank() && !isCreatingNew)
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}
