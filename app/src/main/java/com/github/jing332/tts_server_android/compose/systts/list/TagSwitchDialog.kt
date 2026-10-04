package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.AppSelectionDialog
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

internal data class TagItem(val tag: String, val tagName: String)

internal data class TagGroup(
    val prefix: String,
    val items: List<TagItem>,
)

private fun extractPrefix(name: String): String {
    val m = Regex("^(.+?)(\\d+)$").find(name)
    return m?.groupValues?.get(1) ?: name
}

/**
 * 标签选择弹窗（可复用纯 UI 构件）：顶部「分类筛选框」+ 下方该分类的序号列表
 * （10-04 用户定稿形态——旧两层「分类→序号」的层级差用筛选框吸收，一步可达）。
 * 点筛选框弹出原分类列表选择（带项数、当前分类高亮）；单项分类（旁白/括号/音效等）点一下直接选中。
 * 序号列表保留原样式：高亮当前标签并自动滚动定位。
 * 不写库不通知，选中经 [onSelect]（tag 与 tags 表显示名）交调用方处理。
 */
@Composable
fun TagPickerDialog(
    rule: SpeechRule,
    currentTag: String,
    onSelect: (tag: String, tagName: String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val allTags = remember(rule) {
        rule.tags.entries.map { (key, value) -> TagItem(key, value) }
    }

    val groups = remember(allTags) {
        // 直接用 tags 的原始迭代顺序（JS 定义顺序），groupBy 返回 LinkedHashMap 保留首次出现顺序。
        // 同 prefix 的项在 JS 中按 1..100 连续生成，天然有序，无需额外排序。
        allTags.groupBy { extractPrefix(it.tagName) }
            .map { (prefix, items) ->
                TagGroup(prefix = prefix, items = items)
            }
    }

    val currentPrefix = remember(allTags, currentTag) {
        allTags.find { it.tag == currentTag }?.let { extractPrefix(it.tagName) }
    }

    // 筛选框选中分类（默认停在当前标签所属分类）；null = 当前标签找不到分类 → 回落第一组
    var selectedPrefix by remember(currentPrefix, groups) {
        mutableStateOf(currentPrefix ?: groups.firstOrNull()?.prefix)
    }
    val selectedGroup = remember(groups, selectedPrefix) {
        groups.firstOrNull { it.prefix == selectedPrefix }
    }

    // 序号列表自动滚动到当前标签（照原第二层行为）
    val itemListState = rememberLazyListState()
    LaunchedEffect(selectedGroup, currentTag) {
        val g = selectedGroup
        if (g != null && g.items.isNotEmpty()) {
            val idx = g.items.indexOfFirst { it.tag == currentTag }
            itemListState.scrollToItem(if (idx >= 0) idx else 0)
        }
    }

    // 分类选择弹窗（复用 AppSelectionDialog；点开那一刻的组列表快照，够用——只有「当前分类」跟着变）
    var categoryPickerOpen by remember { mutableStateOf(false) }
    if (categoryPickerOpen) {
        AppSelectionDialog(
            onDismissRequest = { categoryPickerOpen = false },
            title = { Text("选择标签分类") },
            value = selectedPrefix ?: "",
            values = groups.map { it.prefix },
            entries = groups.map {
                if (it.items.size > 1) "${it.prefix}（${it.items.size}项）" else it.prefix
            },
            // 分类最多二十来个，不需要搜索框
            searchEnabled = false,
            onClick = { key, _ ->
                val g = groups.find { it.prefix == key }
                if (g != null) {
                    if (g.items.size == 1) {
                        // 单项分类：点一下直接选中（照原行为；父层 onSelect 会关闭整个弹窗）
                        val only = g.items.first()
                        onSelect(only.tag, only.tagName)
                    } else {
                        selectedPrefix = g.prefix
                        categoryPickerOpen = false
                    }
                }
            },
        )
    }

    AppDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("选择标签") },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
            ) {
                if (groups.isEmpty()) {
                    Text(
                        "朗读规则中没有可用标签",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(16.dp)
                    )
                } else {
                    // 分类筛选框：照换声弹窗 CategoryChip 样式（灰底 8dp 圆角 + 分类图标 + 箭头）
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) { categoryPickerOpen = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Category,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                selectedPrefix ?: "—",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // 序号列表（原第二层样式原样）：高亮当前 + 自动定位
                    val targetGroup = selectedGroup
                    if (targetGroup != null) {
                        LazyColumn(
                            state = itemListState,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            items(targetGroup.items, key = { it.tag }) { tagItem ->
                                val isCurrent = tagItem.tag == currentTag
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelect(tagItem.tag, tagItem.tagName) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tagItem.tagName,
                                        // bodyMedium 14sp（用户 09-11 静态扫查定案）：AlertDialog 正文槽
                                        // LocalTextStyle=bodyMedium，弹窗内选择列表与字段值同档对齐，
                                        // 与 AppSelectionDialog（61dab68）同处方；原 bodyLarge 16sp 与
                                        // 环境 14sp 错位一圈
                                        style = MaterialTheme.typography.bodyMedium,
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
        },
        buttons = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
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
            val rule = withContext(Dispatchers.IO) { dbm.speechRuleDao.getByRuleId(tagRuleId) }
            if (rule != null) {
                // 标签扩容：按配置列表里实际用到的最大序号补齐 tags，确保点标签时列表覆盖全部序号
                withContext(Dispatchers.IO) {
                    runCatching {
                        expandSpeechRuleTagsIfNeeded(rule, dbm.systemTtsV2.all)
                    }
                }
            }
            speechRule = rule
        }
        loaded = true
    }

    val handleSelect: (tag: String, tagName: String) -> Unit = { tag, _ ->
        if (tag != currentTag) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    val ruleData = config!!.speechRule.copy()
                    ruleData.tag = tag
                    ruleData.tagName = computeTagName(context, speechRule, ruleData, tag)
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

    // 未加载/未绑定态用独立外壳；正常态直接由 TagPickerDialog 自带外壳，避免双弹窗嵌套
    if (!loaded) {
        AppDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(stringResource(R.string.tag)) },
            content = {
                LoadingContent(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    isLoading = true
                ) {}
            },
        )
    } else {
        val rule = speechRule
        if (rule == null) {
            AppDialog(
                onDismissRequest = onDismissRequest,
                title = { Text(stringResource(R.string.tag)) },
                content = {
                    Text(
                        "该配置项未绑定朗读规则，无法切换标签",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        // AppDialog content 是 BoxScope：align 需要 Alignment 而非 Horizontal，
                        // 用占满宽度 + 文字居中实现同样的视觉效果
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                },
            )
        } else {
            TagPickerDialog(
                rule = rule,
                currentTag = currentTag,
                onSelect = handleSelect,
                onDismissRequest = onDismissRequest,
            )
        }
    }
}
