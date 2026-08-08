package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

private data class TagItem(val tag: String, val tagName: String)

private data class TagGroup(
    val prefix: String,
    val items: List<TagItem>,
)

private fun extractPrefix(name: String): String {
    val m = Regex("^(.+?)(\\d+)$").find(name)
    return m?.groupValues?.get(1) ?: name
}

private fun extractSuffixNum(name: String): String? {
    val m = Regex("(\\d+)$").find(name)
    return m?.value
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagSwitchDialog(
    item: SystemTtsV2,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dbm = dbm

    val config = item.config as? TtsConfigurationDTO
    val currentTag = config?.speechRule?.tag ?: ""
    val tagRuleId = config?.speechRule?.tagRuleId ?: ""

    var speechRule by remember { mutableStateOf<SpeechRule?>(null) }
    LaunchedEffect(tagRuleId) {
        if (tagRuleId.isNotBlank()) {
            speechRule = dbm.speechRuleDao.getByRuleId(tagRuleId)
        }
    }

    val allTags = remember(speechRule) {
        val tags = speechRule?.tags ?: emptyMap()
        tags.entries.map { (key, value) -> TagItem(key, value) }
    }

    val groups = remember(allTags) {
        allTags.groupBy { extractPrefix(it.tagName) }
            .map { (prefix, items) ->
                TagGroup(
                    prefix = prefix,
                    items = items.sortedWith(compareBy({ extractSuffixNum(it.tagName)?.toIntOrNull() ?: 0 }, { it.tagName }))
                )
            }
            .sortedBy { it.prefix }
    }

    val currentPrefix = remember(allTags, currentTag) {
        allTags.find { it.tag == currentTag }?.let { extractPrefix(it.tagName) }
    }

    var selectedGroup by remember { mutableStateOf<TagGroup?>(null) }

    val handleSelect: (TagItem) -> Unit = { tagItem ->
        if (tagItem.tag != currentTag) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    val ruleData = config!!.speechRule.copy()
                    ruleData.tag = tagItem.tag
                    ruleData.tagName = computeTagName(context, speechRule, ruleData, tagItem.tag)
                    dbm.systemTtsV2.update(
                        item.copy(config = config.copy(speechRule = ruleData))
                    )
                }
                if (item.isEnabled) {
                    SystemTtsService.notifyUpdateConfig()
                }
                onDismissRequest()
            }
        } else {
            onDismissRequest()
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.8f),
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = if (selectedGroup == null) "切换标签" else selectedGroup!!.prefix,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                if (tagRuleId.isBlank()) {
                    Text(
                        text = "该配置项未绑定朗读规则，无法切换标签",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else if (allTags.isEmpty()) {
                    Text(
                        text = "朗读规则中没有可用标签",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    val targetGroup = selectedGroup
                    if (targetGroup == null) {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(groups, key = { it.prefix }) { group ->
                                val isCurrent = group.prefix == currentPrefix
                                TagRow(
                                    text = group.prefix,
                                    subtext = if (group.items.size > 1) "${group.items.size}项" else group.items.firstOrNull()?.tagName,
                                    isCurrent = isCurrent,
                                    onClick = {
                                        if (group.items.size == 1) {
                                            handleSelect(group.items.first())
                                        } else {
                                            selectedGroup = group
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        if (targetGroup.items.size == 1) {
                            handleSelect(targetGroup.items.first())
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(targetGroup.items, key = { it.tag }) { tagItem ->
                                    val isCurrent = tagItem.tag == currentTag
                                    TagRow(
                                        text = tagItem.tagName,
                                        subtext = if (tagItem.tagName != tagItem.tag) tagItem.tag else null,
                                        isCurrent = isCurrent,
                                        onClick = { handleSelect(tagItem) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedGroup != null) {
                TextButton(onClick = { selectedGroup = null }) {
                    Text("返回")
                }
            } else {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}

@Composable
private fun TagRow(
    text: String,
    subtext: String?,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (isCurrent)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surface,
        tonalElevation = if (isCurrent) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrent)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface,
                )
                if (subtext != null) {
                    Text(
                        text = subtext,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (isCurrent) {
                Text(
                    text = "当前",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
