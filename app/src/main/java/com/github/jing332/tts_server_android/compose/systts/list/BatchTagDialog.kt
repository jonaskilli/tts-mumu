package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchTagDialog(
    groupItems: List<SystemTtsV2>,
    onDismissRequest: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedItems by remember { mutableStateOf<Set<SystemTtsV2>>(emptySet()) }
    var selectedTagKey by remember { mutableStateOf<String>("") }

    // 批量分配/清空标签时显示加载遮罩，避免弹窗在处理期间看起来卡住
    var loading by remember { mutableStateOf(false) }
    if (loading) {
        com.github.jing332.compose.widgets.LoadingDialog(onDismissRequest = { loading = false })
    }

    // 按当前顺序排列，并只保留有 TtsConfigurationDTO 的项
    val sortedItems = remember(groupItems) {
        groupItems.sortedBy { it.order }.filter { it.config is TtsConfigurationDTO }
    }

    // 检测选中项是否使用同一个 tagRuleId
    val commonTagRuleId by remember(selectedItems) {
        derivedStateOf {
            val ruleIds = selectedItems.map {
                (it.config as TtsConfigurationDTO).speechRule.tagRuleId
            }.distinct()
            if (ruleIds.size == 1) ruleIds.first() else null
        }
    }

    // 获取可用标签列表
    var speechRule by remember { mutableStateOf<SpeechRule?>(null) }
    LaunchedEffect(commonTagRuleId) {
        speechRule = commonTagRuleId?.let { dbm.speechRuleDao.getByRuleId(it) }
        val keys = speechRule?.tags?.keys?.toList()
        if (!keys.isNullOrEmpty() && (selectedTagKey.isBlank() || !keys.contains(selectedTagKey))) {
            selectedTagKey = keys.first()
        }
    }

    val tagKeys = remember(speechRule) { speechRule?.tags?.keys?.toList() ?: emptyList() }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.92f),
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = stringResource(R.string.batch_assign_tags),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
            ) {
                // 全选 / 取消全选
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val allSelected = sortedItems.isNotEmpty() && sortedItems.all { it in selectedItems }
                    IconButton(
                        onClick = {
                            selectedItems = if (allSelected) emptySet() else sortedItems.toSet()
                        },
                        enabled = sortedItems.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = if (allSelected) Icons.Default.Clear else Icons.Default.DoneAll,
                            contentDescription = if (allSelected) stringResource(R.string.clear) else stringResource(R.string.select_all)
                        )
                    }
                    Text(
                        text = stringResource(R.string.selected_count, selectedItems.size),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 音色列表
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(sortedItems, key = { _, it -> it.id }) { index, item ->
                        val isSelected = item in selectedItems
                        val config = item.config as TtsConfigurationDTO
                        val currentTagName = config.speechRule.tagName.ifBlank {
                            if (config.speechRule.target == com.github.jing332.database.constants.SpeechTarget.ALL)
                                stringResource(R.string.no_tag) else ""
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedItems = if (isSelected) selectedItems - item else selectedItems + item
                                }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${index + 1}. ${item.displayName}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (currentTagName.isNotEmpty()) {
                                    Text(
                                        text = "${stringResource(R.string.tag)}: $currentTagName",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(8.dp))

                // 标签选择区
                if (selectedItems.isEmpty()) {
                    Text(
                        text = stringResource(R.string.please_select_voice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else if (commonTagRuleId == null) {
                    Text(
                        text = stringResource(R.string.tag_rule_inconsistent),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else if (tagKeys.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_tags_in_rule),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.start_tag),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    com.github.jing332.compose.widgets.AppSpinner(
                        modifier = Modifier.fillMaxWidth(),
                        labelText = stringResource(R.string.tag),
                        value = selectedTagKey,
                        values = tagKeys,
                        entries = tagKeys.map { speechRule?.tags?.get(it) ?: it },
                        onSelectedChange = { key, _ ->
                            selectedTagKey = key as String
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    loading = true
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            val keys = tagKeys
                            val startIndex = keys.indexOf(selectedTagKey).coerceAtLeast(0)
                            val sortedSelected = sortedItems.filter { it in selectedItems }
                            sortedSelected.forEachIndexed { idx, item ->
                                val config = item.config as TtsConfigurationDTO
                                val ruleData = config.speechRule.copy()
                                val tagKey = keys.getOrNull((startIndex + idx) % keys.size) ?: return@forEachIndexed
                                ruleData.target = com.github.jing332.tts_server_android.constant.SpeechTarget.TAG
                                ruleData.tag = tagKey
                                ruleData.tagRuleId = commonTagRuleId ?: ""
                                // 用与「一键分配/重排」一致的 computeTagName（含性格追加与 ⚠ 回退），
                                // 保证批量分配标签的显示名与朗读规则一致。
                                ruleData.tagName = computeTagName(context, speechRule, ruleData, tagKey)
                                dbm.systemTtsV2.update(
                                    item.copy(config = config.copy(speechRule = ruleData))
                                )
                            }
                        }
                        if (selectedItems.any { it.isEnabled }) {
                            SystemTtsService.notifyUpdateConfig()
                        }
                        loading = false
                        onDismissRequest()
                    }
                },
                enabled = selectedItems.isNotEmpty() && commonTagRuleId != null && tagKeys.isNotEmpty()
            ) {
                Text(stringResource(R.string.assign_in_order))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        loading = true
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                selectedItems.forEach { item ->
                                    val config = item.config as TtsConfigurationDTO
                                    val ruleData = config.speechRule.copy()
                                    ruleData.target = com.github.jing332.tts_server_android.constant.SpeechTarget.ALL
                                    ruleData.resetTag()
                                    dbm.systemTtsV2.update(
                                        item.copy(config = config.copy(speechRule = ruleData))
                                    )
                                }
                            }
                            if (selectedItems.any { it.isEnabled }) {
                                SystemTtsService.notifyUpdateConfig()
                            }
                            loading = false
                            onDismissRequest()
                        }
                    },
                    enabled = selectedItems.isNotEmpty()
                ) {
                    Text(stringResource(R.string.clear_tag))
                }
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}
