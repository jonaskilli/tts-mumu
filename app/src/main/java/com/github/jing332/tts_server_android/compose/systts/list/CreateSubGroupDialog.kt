package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.github.jing332.database.entities.systts.SystemTtsV2

@Composable
fun CreateSubGroupDialog(
    groupName: String,
    ungroupedItems: List<SystemTtsV2>,
    onDismissRequest: () -> Unit,
    onConfirm: (subGroupName: String, selectedItems: List<SystemTtsV2>) -> Unit,
) {
    var subGroupName by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = remember(searchQuery, ungroupedItems) {
        if (searchQuery.isBlank()) ungroupedItems
        else ungroupedItems.filter {
            it.displayName.contains(searchQuery, ignoreCase = true)
        }
    }

    val allFilteredSelected = filteredItems.isNotEmpty() && filteredItems.all { selectedIds.contains(it.id) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("新建子分组") },
        text = {
            Column {
                Text(
                    text = "在「$groupName」下创建子分组",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                TextField(
                    value = subGroupName,
                    onValueChange = { subGroupName = it },
                    label = { Text("子分组名称 (支持 中文/男声 多级)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (ungroupedItems.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "选择要移入的音色 (${selectedIds.size}/${ungroupedItems.size})",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        IconButton(
                            onClick = {
                                selectedIds = if (allFilteredSelected) {
                                    selectedIds - filteredItems.map { it.id }.toSet()
                                } else {
                                    selectedIds + filteredItems.map { it.id }
                                }
                            },
                            enabled = filteredItems.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = if (allFilteredSelected) Icons.Default.Clear else Icons.Default.DoneAll,
                                contentDescription = if (allFilteredSelected) "取消全选" else "全选"
                            )
                        }
                    }

                    // 搜索框
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("搜索音色名称") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, "清除")
                                }
                            }
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.padding(top = 4.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedIds = if (selectedIds.contains(item.id)) {
                                            selectedIds - item.id
                                        } else {
                                            selectedIds + item.id
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedIds.contains(item.id),
                                    onCheckedChange = { checked ->
                                        selectedIds = if (checked) {
                                            selectedIds + item.id
                                        } else {
                                            selectedIds - item.id
                                        }
                                    }
                                )
                                Text(
                                    text = item.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "当前分组下没有未归类的音色",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (subGroupName.isNotBlank()) {
                        val selected = ungroupedItems.filter { selectedIds.contains(it.id) }
                        onConfirm(subGroupName.trim(), selected)
                    }
                },
                enabled = subGroupName.isNotBlank()
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
