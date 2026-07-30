package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.AbstractListGroup.Companion.DEFAULT_GROUP_ID
import com.github.jing332.database.entities.systts.SystemTtsGroup
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.list.GroupTreePickerDialog

@Composable
fun BasicInfoEditScreen(
    modifier: Modifier,
    systemTts: SystemTtsV2,
    onSystemTtsChange: (SystemTtsV2) -> Unit,

    group: SystemTtsGroup = rememberUpdatedState(
        newValue = dbm.systemTtsV2.getGroup(systemTts.groupId)
            ?: SystemTtsGroup(id = DEFAULT_GROUP_ID, name = "")
    ).value,
) {
    // 第6项: 统一分组树选择器入口
    var showGroupPicker by remember { mutableStateOf(false) }
    if (showGroupPicker) {
        GroupTreePickerDialog(
            currentGroupId = systemTts.groupId,
            currentCategoryPath = systemTts.categoryPath,
            onDismissRequest = { showGroupPicker = false },
            onConfirm = { gid, path ->
                onSystemTtsChange(systemTts.copy(groupId = gid, categoryPath = path))
                showGroupPicker = false
            }
        )
    }

    Column(modifier) {
        // 第6项: 替换原“大分组下拉 + 子分组文本框”为单一层级选择行
        // 点击打开树形选择器: 组内切子分组 / 组外切大分组 / 新建子分组, 并显示当前层级
        val locationText = remember(systemTts.groupId, systemTts.categoryPath, group.name) {
            buildString {
                append(group.name.ifBlank { "默认分组" })
                if (systemTts.categoryPath.isNotBlank()) {
                    append(" > ")
                    append(systemTts.categoryPath.replace("/", " > "))
                }
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showGroupPicker = true },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.group),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = locationText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 30.dp, top = 4.dp)
                )
            }
        }

        OutlinedTextField(
            label = { Text(stringResource(R.string.display_name)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            value = systemTts.displayName, onValueChange = {
                onSystemTtsChange(systemTts.copy(displayName = it))
            },
            trailingIcon = {
                if (systemTts.displayName.isNotEmpty())
                    IconButton(onClick = {
                        onSystemTtsChange(systemTts.copy(displayName = ""))
                    }) {
                        Icon(Icons.Default.Clear, stringResource(id = R.string.clear_text_content))
                    }
            }
        )
    }
}
