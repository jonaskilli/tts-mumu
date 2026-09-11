package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.AbstractListGroup.Companion.DEFAULT_GROUP_ID
import com.github.jing332.database.entities.systts.SystemTtsGroup
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.AudioParams
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
        // 分组选择: 用 OutlinedTextField 风格, 与上下显示名/试听文本框一致
        // enabled=false 防止 BasicTextField 拦截点击, clickable 打开树形选择器
        // colors 覆盖 disabled 颜色使其看起来与正常输入框一致
        val locationText = remember(systemTts.groupId, systemTts.categoryPath, group.name) {
            buildString {
                append(group.name.ifBlank { "默认分组" })
                if (systemTts.categoryPath.isNotBlank()) {
                    append(" > ")
                    append(systemTts.categoryPath.replace("/", " > "))
                }
            }
        }
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showGroupPicker = true },
            enabled = false,
            readOnly = true,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            label = { Text("📁 " + stringResource(R.string.group)) },
            value = locationText,
            onValueChange = {},
            trailingIcon = {
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        )

        OutlinedTextField(
            label = { Text("✏️ " + stringResource(R.string.display_name)) },
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

        val dto = systemTts.config as? TtsConfigurationDTO
        if (dto != null) {
            // 作为备用引擎（用户 09-10 定稿：从朗读标签卡顶部行挪入基本信息卡，与分组/显示名同区）
            var showStandbyHelp by remember { mutableStateOf(false) }
            if (showStandbyHelp)
                AppDialog(
                    title = { Text(stringResource(id = R.string.systts_as_standby_help)) },
                    content = { Text(stringResource(id = R.string.systts_standby_help_msg)) },
                    buttons = {
                        TextButton(onClick = { showStandbyHelp = false }) {
                            Text(stringResource(id = R.string.confirm))
                        }
                    },
                    onDismissRequest = { showStandbyHelp = false }
                )

            // 心声混响问号（用户 09-11：两项开关都补上问号解说，与「作为备用」同款形态）
            var showReverbHelp by remember { mutableStateOf(false) }
            if (showReverbHelp)
                AppDialog(
                    title = { Text(stringResource(id = R.string.systts_reverb_help)) },
                    content = { Text(stringResource(id = R.string.systts_reverb_help_msg)) },
                    buttons = {
                        TextButton(onClick = { showReverbHelp = false }) {
                            Text(stringResource(id = R.string.confirm))
                        }
                    },
                    onDismissRequest = { showReverbHelp = false }
                )

            // 心声混响 + 备用引擎合并一行（用户 09-10 定稿）：都是本条配置的播放行为布尔开关，并排省一行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val p = dto.audioParams
                            updateConfig(systemTts, onSystemTtsChange, p.copy(reverbEnabled = !p.reverbEnabled))
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 复选框 32dp 触摸区（用户 09-11 终裁根治）：M3 默认 48dp 里画 18dp 方块、
                    // 两侧各空 15dp，靠文字负偏移补偿属于魔法数字互相打架（文字被压到方块上）。
                    // 改为收触摸区这一处旋钮：方块两侧自然只剩 7dp，文字放自然位置即紧凑，
                    // 无任何 offset，结构上不可能再重叠。三行复选框行（心声标签/心声混响/作为备用）同款
                    Checkbox(
                        checked = dto.audioParams.reverbEnabled,
                        onCheckedChange = {
                            updateConfig(systemTts, onSystemTtsChange, dto.audioParams.copy(reverbEnabled = it))
                        },
                        modifier = Modifier.size(32.dp),
                    )
                    Text("心声混响")
                    // 问号 32dp 触摸区：紧跟文字不留空（与心声标签行同款）
                    IconButton(
                        onClick = { showReverbHelp = true },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            stringResource(id = R.string.systts_reverb_help)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clickable {
                            onSystemTtsChange(
                                systemTts.copy(
                                    config = dto.copy(
                                        speechRule = dto.speechRule.copy(isStandby = !dto.speechRule.isStandby)
                                    )
                                )
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = dto.speechRule.isStandby,
                        onCheckedChange = null,
                        modifier = Modifier.size(32.dp),
                    )
                    Text(stringResource(id = R.string.as_standby))
                    // 问号 32dp 触摸区：紧跟文字不留空（与心声标签行同款）
                    IconButton(
                        onClick = { showStandbyHelp = true },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            stringResource(id = R.string.systts_as_standby_help)
                        )
                    }
                }
            }
        }
    }
}

private fun updateConfig(
    systemTts: SystemTtsV2,
    onChange: (SystemTtsV2) -> Unit,
    audioParams: AudioParams,
) {
    val config = systemTts.config
    if (config is TtsConfigurationDTO)
        onChange(systemTts.copy(config = config.copy(audioParams = audioParams)))
}
