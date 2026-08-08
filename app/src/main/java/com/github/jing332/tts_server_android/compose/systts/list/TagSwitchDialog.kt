package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.LoadingContent
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

private fun extractSuffixNum(name: String): Int? {
    val m = Regex("(\\d+)$").find(name)
    return m?.value?.toIntOrNull()
}

@Composable
fun TagSwitchDialog(
    item: SystemTtsV2,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val config = item.config as? TtsConfigurationDTO
    val currentTag = config?.speechRule?.tag ?: ""
    val tagRuleId = config?.speechRule?.tagRuleId ?: ""

    var speechRule by remember { mutableStateOf<SpeechRule?>(null) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(tagRuleId) {
        loaded = false
        if (tagRuleId.isNotBlank()) {
            speechRule = withContext(Dispatchers.IO) { dbm.speechRuleDao.getByRuleId(tagRuleId) }
        }
        loaded = true
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
                    items = items.sortedBy { extractSuffixNum(it.tagName) ?: 0 }
                )
            }
            .sortedBy { it.prefix }
    }

    val currentPrefix = remember(allTags, currentTag) {
        allTags.find { it.tag == currentTag }?.let { extractPrefix(it.tagName) }
    }

    var selectedGroup by remember { mutableStateOf<TagGroup?>(null) }

    // 第一层：自动滚动到当前所在的大分类
    val groupListState = rememberLazyListState()
    LaunchedEffect(groups, currentPrefix, selectedGroup) {
        if (selectedGroup == null && groups.isNotEmpty()) {
            val idx = groups.indexOfFirst { it.prefix == currentPrefix }
            groupListState.scrollToItem(if (idx >= 0) idx else 0)
        }
    }

    // 第二层：自动滚动到当前所在的具体标签
    val itemListState = rememberLazyListState()
    LaunchedEffect(selectedGroup, currentTag) {
        val g = selectedGroup
        if (g != null && g.items.isNotEmpty()) {
            val idx = g.items.indexOfFirst { it.tag == currentTag }
            itemListState.scrollToItem(if (idx >= 0) idx else 0)
        }
    }

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
                if (item.isEnabled) SystemTtsService.notifyUpdateConfig()
                onDismissRequest()
            }
        } else {
            onDismissRequest()
        }
    }

    AppDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = if (selectedGroup == null) "选择标签分类" else "选择标签序号",
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
            ) {
                when {
                    // 加载中：显示 Loading，避免空列表时闪现"无可用标签"红字
                    !loaded -> {
                        LoadingContent(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            isLoading = true
                        ) {}
                    }
                    tagRuleId.isBlank() -> {
                        Text(
                            "该配置项未绑定朗读规则，无法切换标签",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(16.dp)
                        )
                    }
                    allTags.isEmpty() -> {
                        Text(
                            "朗读规则中没有可用标签",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(16.dp)
                        )
                    }
                    else -> {
                        val targetGroup = selectedGroup
                        if (targetGroup == null) {
                            // 第一层：大分类列表，自动定位到当前分类
                            LazyColumn(
                                state = groupListState,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(groups, key = { it.prefix }) { group ->
                                    val isCurrent = group.prefix == currentPrefix
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (group.items.size == 1) {
                                                    handleSelect(group.items.first())
                                                } else {
                                                    selectedGroup = group
                                                }
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (group.items.size > 1)
                                                "${group.prefix}（${group.items.size}项）"
                                            else group.prefix,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrent)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isCurrent) {
                                            Text(
                                                "当前",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // 第二层：具体标签列表，自动定位到当前标签
                            LazyColumn(
                                state = itemListState,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(targetGroup.items, key = { it.tag }) { tagItem ->
                                    val isCurrent = tagItem.tag == currentTag
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { handleSelect(tagItem) }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tagItem.tagName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrent)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isCurrent) {
                                            Text(
                                                "当前",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        buttons = {
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
