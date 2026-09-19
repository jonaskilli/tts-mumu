package com.github.jing332.tts_server_android.compose.systts.role

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.jing332.compose.widgets.ShadowedDraggableItem
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.nav.NavTopAppBar
import com.github.jing332.tts_server_android.service.systts.help.KeyListFile
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

/**
 * 启用池页（密钥管理页的子页，页内全屏覆盖）：启用中的密钥按**轮换顺序**平铺。
 * 顶栏：「⚡测试全部」描边键（并发 4 路，回调在主页；用户 0919：加框 + ⚡，不带颜色）+ ☑ 多选（批量移出）。
 * 行 = 两行式（用户 0919：副标题独占第二行、整行宽，不再被图标挤到截断）：
 *   第一行 = 序号徽章（实心主题色反白）+ 模型名 + 闪电（颜色=测试结果，兼单测动作）+ ⧉复制 ✏编辑 ⊖移出；
 *   第二行 = 分组名 · *尾号（异组同名模型靠它分辨）。
 * ⊖ 移出 = 只移出池、密钥本体保留（可逆，主页多选可再加回）；真删除只在密钥管理页（🗑 + 二次确认）。
 * 排序 = 长按拖动（照主界面 reorderable 同款，放手落位、序号自动重排）；多选模式下禁拖。
 * 底层 = miyue.txt 启用池（KeyListFile.savePool 三写），朗读规则 DualKeyManager 按此顺序轮换，
 * 第一把同时供 app 心声 AI 兜底使用。
 */

/** 一行要展示的信息：显示名 + 副行素材 + 归一化值（测试结果/测试中标记的 key） */
private class PoolRowInfo(
    val display: String,
    val groupTitle: String?,
    val tail: String?,
    val stale: Boolean,
    val norm: String,
)

/** 池值 → 展示信息：优先按归一化值对回密钥条目；对不上 = 已删除条目的残留值 */
private fun resolvePoolRow(
    value: String,
    keys: List<KeyListFile.KeyEntry>,
    titleByEntry: Map<String, String>,
): PoolRowInfo {
    val norm = KeyListFile.normalizePoolValue(value)
    val entry = keys.firstOrNull { KeyListFile.normalizePoolValue(it.value) == norm }
    if (entry == null) {
        // 残留值（密钥被删但池里还挂着，或外部手改 miyue）：显示模型名/Key 尾
        val p = KeyListFile.parseKeyValue(value)
        val display = when {
            p == null -> value
            !p.isDirect && p.model.isNotBlank() -> p.model
            else -> "*" + p.key.takeLast(6)
        }
        return PoolRowInfo(display, null, null, true, norm)
    }
    // 归属分组照主页 buildKeyGroups 的顺序口径：第一个命中的接口组，否则 未分组/直连
    val groupTitle = titleByEntry[entry.name]
    val tail = KeyListFile.parseKeyValue(entry.value)?.key?.takeLast(4)
    return PoolRowInfo(KeyListFile.displayName(entry), groupTitle, tail, false, norm)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KeyPoolScreen(
    pool: List<String>,
    keys: List<KeyListFile.KeyEntry>,
    ifaces: List<KeyListFile.ApiInterface>,
    testByValue: Map<String, Boolean>,
    testingValue: String?,
    batchTesting: Boolean,
    selectionMode: Boolean,
    checked: Set<String>,
    onBack: () -> Unit,
    onToggleSelectionMode: () -> Unit,
    onToggleCheck: (norm: String) -> Unit,
    onToggleCheckAll: () -> Unit,
    onMove: (fromNorm: String, toNorm: String) -> Unit,
    onRemove: (Int) -> Unit,
    onCopy: (display: String) -> Unit,
    onEdit: (norm: String) -> Unit,
    onTest: (raw: String, display: String) -> Unit,
    onTestAll: () -> Unit,
    onRemoveBatch: () -> Unit,
) {
    // 返回键：多选模式先退多选，再退回密钥管理主页
    BackHandler {
        if (selectionMode) onToggleSelectionMode() else onBack()
    }
    // 归属分组映射（照主页 buildKeyGroups 的顺序口径：第一个命中的接口组，否则 未分组/直连）
    val titleByEntry = remember(keys, ifaces) {
        val m = mutableMapOf<String, String>()
        buildKeyGroups(keys, ifaces).forEach { g -> g.entries.forEach { m[it.name] = g.title } }
        m
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NavTopAppBar(
                title = { Text(stringResource(R.string.role_key_pool_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                },
                actions = {
                    // 「⚡测试全部」描边键（用户 0919 终稿：加框 + ⚡，不带颜色）；
                    // 测试中转小圈并禁点
                    Box(
                        Modifier
                            .padding(horizontal = 6.dp)
                            .heightIn(min = 32.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                            .clickable(
                                enabled = pool.isNotEmpty() && !batchTesting,
                                onClick = onTestAll
                            )
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (batchTesting) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    stringResource(R.string.role_key_pool_test_all),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                    // ☑ 多选：批量移出（与主页 ☑ 同款图标语言）
                    IconButton(onClick = onToggleSelectionMode) {
                        Icon(
                            Icons.Default.Checklist,
                            contentDescription = stringResource(R.string.desc_multi_select),
                            tint = if (selectionMode) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            // 多选底栏：全选 | 移出启用池(N)。放 bottomBar 让列表自动让位
            if (selectionMode) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FlatTextAction(
                            stringResource(R.string.select_all),
                            MaterialTheme.colorScheme.onSurfaceVariant
                        ) { onToggleCheckAll() }
                        Spacer(Modifier.weight(1f))
                        FlatTextAction(
                            stringResource(R.string.role_key_pool_remove_n, checked.size),
                            MaterialTheme.colorScheme.error
                        ) { onRemoveBatch() }
                    }
                }
            }
        }
    ) { paddingValues ->
        val listState = rememberLazyListState()
        val reorderState = rememberReorderableLazyListState(listState = listState, onMove = { from, to ->
            val fk = from.key as? String ?: return@rememberReorderableLazyListState
            val tk = to.key as? String ?: return@rememberReorderableLazyListState
            if (fk.startsWith("p:") && tk.startsWith("p:")) {
                onMove(fk.removePrefix("p:"), tk.removePrefix("p:"))
            }
        })
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .reorderable(reorderState),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
        ) {
            if (pool.isEmpty()) {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.role_key_pool_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                    )
                }
            } else {
                itemsIndexed(pool, key = { _, value -> "p:" + KeyListFile.normalizePoolValue(value) }) { idx, value ->
                    val row = resolvePoolRow(value, keys, titleByEntry)
                    val norm = row.norm
                    val testing = testingValue == norm
                    // 长按拖动排序（放手落位、序号自动重排）；多选/整批测试中禁拖
                    val dragModifier = if (selectionMode || batchTesting) Modifier
                    else Modifier.detectReorderAfterLongPress(reorderState)
                    ShadowedDraggableItem(reorderState, "p:" + norm) { _ ->
                        PoolRow(
                            orderNum = idx + 1,
                            info = row,
                            testOk = testByValue[norm],
                            testing = testing,
                            selectionMode = selectionMode,
                            checked = norm in checked,
                            batchTesting = batchTesting,
                            dragModifier = dragModifier,
                            onToggleCheck = { onToggleCheck(norm) },
                            onTest = { onTest(value, row.display) },
                            onCopy = { onCopy(row.display) },
                            onEdit = { onEdit(norm) },
                            onRemove = { onRemove(idx) },
                        )
                    }
                    if (idx < pool.lastIndex) {
                        HorizontalDivider(
                            thickness = 0.6.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 启用池一行 = 两行式（用户 0919：副标题独占第二行、整行宽，不再被图标挤到截断）：
 * 第一行 = 序号徽章（实心主题色反白）+ 模型名 + 动作区；
 * 第二行 = 分组名 · *尾号（异组同名模型靠它分辨；残留值此行说明来源）。
 * 动作区 = 闪电（颜色=测试结果，兼单测动作）+ ⊖ 圆圈减号（移出启用池，可逆）。
 * 多选模式：复选框顶替序号徽章，动作区隐藏。
 */
@Composable
private fun PoolRow(
    orderNum: Int,
    info: PoolRowInfo,
    testOk: Boolean?,
    testing: Boolean,
    selectionMode: Boolean,
    checked: Boolean,
    batchTesting: Boolean,
    dragModifier: Modifier,
    onToggleCheck: () -> Unit,
    onTest: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (selectionMode) {
                Checkbox(checked = checked, onCheckedChange = { onToggleCheck() })
                Spacer(Modifier.width(10.dp))
            } else {
                // 大序号徽章：本页的主角就是顺序（拖动放手后自动重排）。
                // 0919 实机二调：24dp 偏大，缩到 20dp；保持实心主题色（上一版 14% 透明底不显眼的教训不回退）
                Box(
                    Modifier.size(20.dp).background(
                        MaterialTheme.colorScheme.primary,
                        CircleShape
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        orderNum.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(10.dp))
            }
            Text(
                info.display,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (!selectionMode) {
                // 闪电 = 单测按钮（恢复纯灰，结果看主页行首灯——两页结果共享同一份）
                if (testing) {
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                } else {
                    FlatIconAction(
                        Icons.Default.Bolt,
                        stringResource(R.string.role_key_test),
                        enabled = !batchTesting
                    ) { onTest() }
                }
                // ⧉复制 ✏编辑 ⊖圆圈减号=移出（四键与主页条目卡一致，用户 0919；
                // 移出可逆：只出池不删钥，真删除在主页；残留值无条目可编辑，隐藏 ✏）
                FlatIconAction(
                    Icons.Default.ContentCopy,
                    stringResource(R.string.copy)
                ) { onCopy() }
                if (!info.stale) {
                    FlatIconAction(
                        Icons.Default.Edit,
                        stringResource(R.string.role_key_edit)
                    ) { onEdit() }
                }
                FlatIconAction(
                    Icons.Default.RemoveCircleOutline,
                    stringResource(R.string.desc_pool_remove)
                ) { onRemove() }
            }
        }
        if (!selectionMode) {
            // 副标题独占第二行（整行宽）：分组名 · 密钥尾号——异组同名模型靠它分辨
            val sub = when {
                info.stale -> stringResource(R.string.role_key_pool_stale)
                info.groupTitle != null && info.tail != null ->
                    stringResource(R.string.role_key_pool_group_tail, info.groupTitle, info.tail)
                else -> null
            }
            if (sub != null) {
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // 副标题与第一行名字同列：徽章 20dp + 间距 10dp = 30dp（0920 对齐反馈）
                    modifier = Modifier.padding(start = 30.dp, top = 2.dp)
                )
            }
        }
    }
}
