package com.github.jing332.compose.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.jing332.compose.ComposeExtensions.clickableRipple
import com.github.jing332.compose.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun AppSelectionDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    value: Any,
    values: List<Any>,
    entries: List<String>,
    icons: List<Any?> = emptyList(),
    isLoading: Boolean = false,
    searchEnabled: Boolean = values.size > 5,

    itemContent: @Composable RowScope.(Boolean, String, Any?, Any) -> Unit = { isSelected, entry, icon, _ ->
        if (icon != null)
            AsyncCircleImage(
                modifier = Modifier.size(32.dp),
                model = icon,
                contentDescription = entry
            )
        Text(
            entry,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
    },

    extraButtons: @Composable BoxScope.() -> Unit = {},
    buttons: @Composable BoxScope.() -> Unit = {
        extraButtons()
        TextButton(onClick = onDismissRequest) { Text(stringResource(id = R.string.close)) }
    },

    onValueSame: (Any, Any) -> Boolean = { a, b -> a == b },
    onClick: (Any, String) -> Unit,
    onLongClick: ((Any, String) -> Unit)? = null,
    trailingContent: (@Composable RowScope.(itemValue: Any, entry: String, onHighlight: () -> Unit) -> Unit)? = null,
    selectedMultiValues: Set<Any> = emptySet(),
    onMultiSelectedChange: ((Set<Any>) -> Unit)? = null,
    categoryMap: Map<Any, String> = emptyMap(),
    onCategoryChange: ((itemValue: Any, category: String?) -> Unit)? = null,
    waitCategorySwitch: Boolean = false,
    onWaitCategorySwitchChange: ((Boolean) -> Unit)? = null,
    autoNextSwitch: Boolean = false,
    onAutoNextSwitchChange: ((Boolean) -> Unit)? = null,
) {

    // 搜索框固定在顶部常显，无需点击图标再展开
    // 当前高亮的条目值（点击试听或多选按钮时设置，单选式：点另一个即转移）
    var highlightedValue by remember { mutableStateOf<Any?>(null) }

    val focusRequester = remember { FocusRequester() }

    AppDialog(
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(Modifier.weight(1f, fill = false)) { title() }
            }
        },
        content = {
            val state = rememberLazyListState()
            LaunchedEffect(values) {
                val index = values.indexOfFirst { onValueSame(it, value) }
                if (index >= 0 && index < entries.size)
                    state.scrollToItem(index)
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                // 开关行：等待分类 / 自动下一个，带文字标签，置于列表上方避免与标题挤在一起
                if (onWaitCategorySwitchChange != null || onAutoNextSwitchChange != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        onWaitCategorySwitchChange?.let { onChange ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = waitCategorySwitch, onCheckedChange = onChange)
                                Text(
                                    stringResource(R.string.wait_for_category),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                        if (onWaitCategorySwitchChange != null && onAutoNextSwitchChange != null) {
                            Spacer(Modifier.width(16.dp))
                        }
                        onAutoNextSwitchChange?.let { onChange ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = autoNextSwitch, onCheckedChange = onChange)
                                Text(
                                    stringResource(R.string.auto_next),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }

                var searchText by rememberSaveable { mutableStateOf("") }

                if (searchEnabled) {
                    val keyboardController = LocalSoftwareKeyboardController.current

                    var text by rememberSaveable { mutableStateOf("") }

                    // 搜索框固定在顶部常显，无需点击搜索图标再展开
                    DenseOutlinedField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .focusRequester(focusRequester),
                        value = text, onValueChange = { text = it },
                        label = { Text(stringResource(id = R.string.search) + " ${values.size}") },
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { keyboardController?.hide() }
                        )
                    )

                    LaunchedEffect(Unit) {
                        while (coroutineContext.isActive) {
                            delay(500)
                            searchText = text
                        }
                    }
                }

                // 用过滤后的条目数判断空，避免依赖 viewport 布局时机导致"空列表"红字闪现
                val visibleCount by remember {
                    derivedStateOf {
                        if (!searchEnabled || searchText.isBlank()) entries.size
                        else entries.count { it.contains(searchText, ignoreCase = true) }
                    }
                }

                if (searchText.isNotBlank() && visibleCount == 0)
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .minimumInteractiveComponentSize()
                            .align(Alignment.CenterHorizontally),
                        text = stringResource(id = R.string.empty_list),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )


                LoadingContent(
                    modifier = Modifier.padding(vertical = 16.dp),
                    isLoading = isLoading
                ) {

                    LazyColumn(
                        state = state,
                        modifier = Modifier
                    ) {
                        itemsIndexed(entries) { i, entry ->
                            if (searchEnabled && searchText.isNotBlank() &&
                                !entry.contains(searchText, ignoreCase = true)
                            ) return@itemsIndexed

                            val icon = icons.getOrNull(i)
                            val current = values[i]
                            val isSelected = onValueSame(value, current)
                            val isHighlighted = highlightedValue != null && onValueSame(highlightedValue!!, current)
                            // 不再用绿色(高亮)/蓝色(选中)背景着色，颜色保持默认
                            val rowBg = Color.Unspecified
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(rowBg)
                                    .clickableRipple(
                                        onClick = { onClick(current, entry) },
                                        onLongClick = onLongClick?.let { { it(current, entry) } }
                                    )
                                    .minimumInteractiveComponentSize()
                                    .focusable()
                                    .semantics(mergeDescendants = true) {
                                        selected = isSelected

                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                itemContent(isSelected, entry, icon, value)
                                if (trailingContent != null) trailingContent(current, entry) {
                                    highlightedValue = current
                                }
                                if (onMultiSelectedChange != null) {
                                    val isMultiSelected = current in selectedMultiValues
                                    val category = categoryMap[current]
                                    if (category != null) {
                                        // 已分配分类：显示分类名标签，点击可重新选择
                                        var showCategoryMenu by remember { mutableStateOf(false) }
                                        val allCategories = remember {
                                            listOf("默认") + VoiceCategories.ALL
                                        }
                                        Box {
                                            Surface(
                                                modifier = Modifier
                                                    .padding(start = 4.dp)
                                                    .clickableRipple(onClick = { showCategoryMenu = true }),
                                                shape = MaterialTheme.shapes.small,
                                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                                tonalElevation = 2.dp
                                            ) {
                                                Text(
                                                    category,
                                                    modifier = Modifier.padding(
                                                        horizontal = 8.dp,
                                                        vertical = 4.dp
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = showCategoryMenu,
                                                onDismissRequest = { showCategoryMenu = false }
                                            ) {
                                                allCategories.forEach { cat ->
                                                    DropdownMenuItem(
                                                        text = { Text(cat) },
                                                        onClick = {
                                                            showCategoryMenu = false
                                                            if (onCategoryChange != null) {
                                                                onCategoryChange.invoke(
                                                                    current,
                                                                    if (cat == "默认") null else cat
                                                                )
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        // 未分配分类：显示多选圆点，长按可弹出分类选择
                                        var showCategoryMenu by remember { mutableStateOf(false) }
                                        val allCategories = remember {
                                            listOf("默认") + VoiceCategories.ALL
                                        }
                                        Box {
                                            Box(
                                                Modifier
                                                    .padding(start = 4.dp)
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isMultiSelected) MaterialTheme.colorScheme.primary
                                                        else Color.Transparent
                                                    )
                                                    .border(
                                                        2.dp,
                                                        if (isMultiSelected) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.outline,
                                                        CircleShape
                                                    )
                                                    .clickableRipple(
                                                        onClick = {
                                                            highlightedValue = current
                                                            onMultiSelectedChange.invoke(
                                                                if (isMultiSelected) selectedMultiValues - current
                                                                else selectedMultiValues + current
                                                            )
                                                        },
                                                        onLongClick = {
                                                            if (onCategoryChange != null) showCategoryMenu = true
                                                        }
                                                    )
                                            )
                                            DropdownMenu(
                                                expanded = showCategoryMenu,
                                                onDismissRequest = { showCategoryMenu = false }
                                            ) {
                                                allCategories.forEach { cat ->
                                                    DropdownMenuItem(
                                                        text = { Text(cat) },
                                                        onClick = {
                                                            showCategoryMenu = false
                                                            if (onCategoryChange != null) {
                                                                onCategoryChange.invoke(
                                                                    current,
                                                                    if (cat == "默认") null else cat
                                                                )
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        buttons = buttons, onDismissRequest = onDismissRequest,
    )
}

/**
 * 发音人分类的公共常量：试听分类弹窗（AuditionDialog）与
 * 声音列表长按分类菜单（AppSelectionDialog）共用，避免两处硬编码不同步。
 */
object VoiceCategories {
    /** 全部分类（按性别年龄排列，后四项为朗读规则 2.87 中新增的主角/特殊分类） */
    val ALL: List<String> = listOf(
        "女童", "少女", "女青年", "女中年", "女老年",
        "男童", "少年", "男青年", "男中年", "男老年",
        "男主", "女主", "特殊男", "特殊女", "旁白"
    )

    /** 分类弹窗的三列布局：女性列 / 男性列 / 主角特殊旁白列，每列内部竖向堆叠 */
    val COLUMNS: List<List<String>> = listOf(
        // 女性列
        listOf("女童", "少女", "女青年", "女中年", "女老年"),
        // 男性列
        listOf("男童", "少年", "男青年", "男中年", "男老年"),
        // 主角特殊旁白列：旁白在最前
        listOf("旁白", "男主", "女主", "特殊男", "特殊女")
    )
}