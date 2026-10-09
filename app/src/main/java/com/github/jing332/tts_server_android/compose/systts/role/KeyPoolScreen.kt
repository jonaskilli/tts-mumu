package com.github.jing332.tts_server_android.compose.systts.role

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.nav.NavTopAppBar
import com.github.jing332.tts_server_android.compose.systts.OrderBadge
import com.github.jing332.tts_server_android.service.systts.help.KeyListFile
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

/**
 * 启用池页（密钥管理页的子页，页内全屏覆盖）：启用中的密钥按**轮换顺序**平铺。
 * 顶栏：「⚡测试全部」描边键（并发 4 路，回调在主页；用户 0919：加框 + ⚡，不带颜色）+ ☑ 多选（批量移出）。
 * 行 = 两行式（用户 0919：副标题独占第二行、整行宽，不再被图标挤到截断）：
 *   第一行 = 序号徽章（中性灰 OrderBadge，10-07 配色口径）+ 模型名 + 闪电（颜色=测试结果，兼单测动作）+ ⧉复制 ✏编辑 ⊖移出；
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
    // 计费倍率展示串（10-10）：站点|模型 查 modelRates；null=该站无数据不显示
    val rate: String? = null,
)

/** 池值 → 展示信息：优先按归一化值对回密钥条目；对不上 = 已删除条目的残留值 */
private fun resolvePoolRow(
    value: String,
    keys: List<KeyListFile.KeyEntry>,
    titleByEntry: Map<String, String>,
    ifcByEntry: Map<String, KeyListFile.ApiInterface> = emptyMap(),
    modelRates: Map<String, String> = emptyMap(),
): PoolRowInfo {
    val norm = KeyListFile.normalizePoolValue(value)
    val entry = keys.firstOrNull { KeyListFile.normalizePoolValue(it.value) == norm }
    if (entry == null) {
        // 残留值（密钥被删但池里还挂着，或外部手改 miyue）：显示模型名/Key 尾
        val p = KeyListFile.parseKeyValue(value)
        val display = when {
            p == null -> value
            !p.isDirect && p.model.isNotBlank() -> p.model
            // 10-08 用户定稿：短 key 尾段走 KeyListFile.keyTail（与主页同口径）
            else -> "*" + KeyListFile.keyTail(p.key)
        }
        return PoolRowInfo(display, null, null, true, norm)
    }
    // 归属分组照主页 buildKeyGroups 的顺序口径：第一个命中的接口组，否则 未分组
    val groupTitle = titleByEntry[entry.name]
    // 10-08 用户定稿：尾段走 KeyListFile.keyTail（>4 显尾4 / 3~4 显尾2 / ≤2 全显，与主页同口径）
    val tail = KeyListFile.parseKeyValue(entry.value)?.key?.let { KeyListFile.keyTail(it) }
    // 倍率（10-10）：按条目所属组站点+模型查（与主页同键口径）
    val rate = ifcByEntry[entry.name]?.let { ifc ->
        KeyListFile.parseKeyValue(entry.value)?.model?.takeIf { it.isNotBlank() }
            ?.let { m -> modelRates[KeyListFile.rateKey(ifc.baseUrl, m)] }
    }
    return PoolRowInfo(KeyListFile.displayName(entry), groupTitle, tail, false, norm, rate)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun KeyPoolScreen(
    pool: List<String>,
    keys: List<KeyListFile.KeyEntry>,
    ifaces: List<KeyListFile.ApiInterface>,
    // 10-08 卡片化：完整 TestOutcome（含用时/结论/锁定）——池卡渲染与主页同构的结果条
    testByValue: Map<String, KeyListFile.TestOutcome>,
    // 模型倍率表（10-10）：键=站点|模型 → 展示串；池页模型行倍率胶囊同主页
    modelRates: Map<String, String> = emptyMap(),
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
    // 归属分组映射（照主页 buildKeyGroups 的顺序口径：第一个命中的接口组，否则 未分组）
    // + 条目→所属分组本体（10-10 倍率用：拿 baseUrl 查 modelRates）——一次遍历建两表
    val (titleByEntry, ifcByEntry) = remember(keys, ifaces) {
        val titles = mutableMapOf<String, String>()
        val ifcMap = mutableMapOf<String, KeyListFile.ApiInterface>()
        buildKeyGroups(keys, ifaces).forEach { g ->
            g.entries.forEach { titles[it.name] = g.title }
            g.ifc?.let { ifc -> g.entries.forEach { ifcMap[it.name] = ifc } }
        }
        titles to ifcMap
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
                    // ⚡测试（10-08 用户令）：测试全部胶囊撤，改纯图标——与模型行灰闪电同款
                    // （FlatIconAction onSurfaceVariant 18dp、36dp 热区），文案进 contentDescription。
                    // 整批测试中原位转小圈，测完回灰闪电
                    // 10-10 M3 Expressive 改造：测活等待换 LoadingIndicator
                    val testAllEnabled = pool.isNotEmpty() && !batchTesting
                    if (batchTesting) {
                        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                            LoadingIndicator(Modifier.size(22.dp))
                        }
                    } else {
                        FlatIconAction(
                            Icons.Default.Bolt,
                            stringResource(R.string.role_key_pool_test_all),
                            enabled = testAllEnabled
                        ) { onTestAll() }
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
            // 10-07 用户令：左线 16 收 12（与密钥页同批；「8 太窄、16 太宽」）。
            // 池页行无自身水平内缩，容器值即内容线：16→12。右=左镜像同 12（本页图标右端
            // 靠行右缘，左右对称才不偏）
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp)
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
                    val row = resolvePoolRow(value, keys, titleByEntry, ifcByEntry, modelRates)
                    val norm = row.norm
                    val testing = testingValue == norm
                    // 长按拖动排序（放手落位、序号自动重排）；多选/整批测试中禁拖
                    val dragModifier = if (selectionMode || batchTesting) Modifier
                    else Modifier.detectReorderAfterLongPress(reorderState)
                    // 10-10 用户令（照主页模型卡）：独立 ElevatedCard 撤——改白底 Column
                    // 平铺（主页条目=连体卡内分区，无卡中卡）；多选勾中=12% 浅红（同主页
                    // cardColor 两态，compositeOver 防透页面底）；卡间 1dp 分隔线（连体卡
                    // 分区观感），垂直 4dp 节奏保留
                    val cardColor = if (selectionMode && norm in checked)
                        MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                            .compositeOver(MaterialTheme.colorScheme.surface)
                    else MaterialTheme.colorScheme.surface
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(cardColor)
                            .then(dragModifier)
                    ) {
                        if (idx > 0) HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                        PoolRow(
                            orderNum = idx + 1,
                            info = row,
                            testOutcome = testByValue[norm],
                            testing = testing,
                            selectionMode = selectionMode,
                            checked = norm in checked,
                            batchTesting = batchTesting,
                            onToggleCheck = { onToggleCheck(norm) },
                            onTest = { onTest(value, row.display) },
                            onCopy = { onCopy(row.display) },
                            onEdit = { onEdit(norm) },
                            onRemove = { onRemove(idx) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 启用池卡内容 = 两行式（用户 0919：副标题独占第二行、整行宽，不再被图标挤到截断）：
 * 第一行 = 序号徽章（进 32×36 盒=主页对勾盒规格，10-08 卡片化）+ 模型名 + 动作区；
 * 第二行 = 分组名 · *尾号（异组同名模型靠它分辨；残留值此行说明来源）；
 * 第三段 = 测试结果条（10-08 卡片化补：与主页同构单行——用时前置+结论+已锁定，
 * 黄/红尾挂「详情」可展开全文；此前只有一颗圆点，结论文字无处看）。
 * 动作区 = ⚡单测 + ⧉复制 ✏编辑 ⊖移出（与主页条目一致；移出可逆，真删除在主页）。
 * 多选模式：复选框顶替序号徽章，动作区隐藏。
 * 长按拖动排序挂整行内容（dragModifier），卡容器不参与拖拽手势。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class) // LoadingIndicator（10-10 Expressive 改造，CI 实锤函数级须标）
@Composable
private fun PoolRow(
    orderNum: Int,
    info: PoolRowInfo,
    testOutcome: KeyListFile.TestOutcome?,
    testing: Boolean,
    selectionMode: Boolean,
    checked: Boolean,
    batchTesting: Boolean,
    onToggleCheck: () -> Unit,
    onTest: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        // 拖拽手势已上移挂卡容器（10-10：连体卡化后手势随整卡，原行内 dragModifier 参数撤）
        Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (selectionMode) {
                Checkbox(checked = checked, onCheckedChange = { onToggleCheck() })
                Spacer(Modifier.width(10.dp))
            } else {
                // 序号徽章：统一走 OrderBadge（10-05 用户拍板形状「丙」胶囊）。
                // 10-08 卡片化：徽章包进 32×36 盒（=主页对勾盒规格）——盒右缘=文字总线 32，
                // 模型名/副标题/结果条三条线与主页同值；徽章本体形制不变（胶囊，宽度随内容）。
                // 历史注：0919 那条「14% 透明底不显眼」的教训针对的是 **14% 的 primary**；
                // 现用的是 primaryContainer@50%（色阶本身更实），不属该回退范围。
                Box(
                    Modifier.size(width = 32.dp, height = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OrderBadge(number = orderNum)
                }
            }
            // 名字区 weight(1f)：独占剩余宽度（0920 教训——名字格与弹性空格不许双 weight，
            // 各抢一半会把名字挤成半宽提前换行）
            // 10-10 倍率甲案同构：名字后跟灰底倍率胶囊——Row 包住名字+胶囊，名字
            // weight(1f,fill=false) 拿剩余宽度（短名不留空、长名两行内换行不挤坏胶囊）
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    info.display,
                    // 10-09 五令（字号 A 案同构）：bodyMedium(16)→14sp，与主页模型名同档
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!selectionMode && info.rate != null) {
                    Spacer(Modifier.width(5.dp))
                    Text(
                        info.rate,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            // 标签类胶囊淡绿：softContainerColor 单源（10-10 统一）
                            .background(softContainerColor())
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
            if (!selectionMode) {
                // 测试结果圆点：名字后、紧挨闪电前（0920 定稿，与主页同位置）——
                // 与闪电因果相邻、不被序号徽章抢视线、垂直成一列好扫。
                // 没测=空槽不显但保列对齐；三色（10-03）：绿=通且思考关/黄=通但思考开/红=不通
                // 槽 14→36dp（10-06 图4 对齐口径）：与主页灯槽同构——36dp 一整格、灯在格心，
                // 灯心距闪电热区中心恒 36dp，两页灯列/图标列视觉同律（原 14dp 槽灯贴闪电太近，
                // 且与主页 36dp 槽错位 11dp，用户实机指「快捷图标没竖向对齐」）
                Box(
                    Modifier.width(36.dp).height(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    testDotColor(testOutcome?.verdict, MaterialTheme.colorScheme.error)?.let {
                        Box(Modifier.size(8.dp).background(it, CircleShape))
                    }
                }
                // 闪电 = 单测按钮：常态灰（与其他图标同色），测试中原位转小圈，
                // 转完回灰；测完不变色（结果看名字后圆点）
                // 10-10 M3 Expressive 改造：测活等待换 LoadingIndicator
                if (testing) {
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        LoadingIndicator(Modifier.size(22.dp))
                    }
                } else {
                    // 批量测试中禁点但不降透明度（0920 反馈：禁用置灰让闪电看着发灰、
                    // 与主页不一致；拦截点击即可，颜色永远与其他图标一致）
                    FlatIconAction(
                        Icons.Default.Bolt,
                        stringResource(R.string.role_key_test)
                    ) {
                        if (!batchTesting) onTest()
                    }
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
                    // 副标题与第一行名字同列：序号盒 32×36（=主页对勾盒规格，10-08 卡片化）
                    // 盒右缘即文字总线 32（原徽章 20+间距 10=30 与主页差 2，随之校齐）
                    modifier = Modifier.padding(start = 32.dp, top = 2.dp)
                )
            }
        }
        // 测试结果条（10-08 卡片化补：与主页同构单行口径——用时前置+结论+已锁定，
        // 黄/红尾挂「详情」展开全文；三色文字即状态色、无圆点，左缘同文字线 32）
        if (!selectionMode && testOutcome != null) {
            PoolTestResultBar(testOutcome, onEditThinking = onEdit)
        }
    }
}

/**
 * 池卡测试结果条（10-08）：照主页 KeyEntryRow 收起行单行口径精简——
 * 用时前置（独立 Text，maxLines 截断吃不到它）+ 结论一行 + 黄/红「详情」展开全文。
 * 展开态 = 用时+全文同 Row（折行从用时右缘起，主页 10-08 三令同构）+ 底部「复制结果｜收起」。
 * 池页没有思考设置入口（编辑弹窗在主页），不挂「自定义思考 ›」。
 */
@Composable
private fun PoolTestResultBar(
    testOutcome: KeyListFile.TestOutcome,
    onEditThinking: () -> Unit,
) {
    val isWarn = testOutcome.verdict == KeyListFile.TestVerdict.PASS_THINKING
    val isPass = testOutcome.verdict == KeyListFile.TestVerdict.PASS
    val barColor = when {
        isPass -> TEST_PASS_COLOR
        isWarn -> TEST_WARN_COLOR
        else -> MaterialTheme.colorScheme.error
    }
    // 单行 / 展开共用同一段文字（10-10 用户令，与主页 KeyManagerScreen 同口径）：
    // 原收起用 pass_only/warn_short 摘要是「另一句话」、展开用 message，两态换句；
    // 改一源=message（本身即「用时 · 结论 · 已锁定 x」），收起=截断、展开=同段显示完整。
    val fullText = testOutcome.message
    var expanded by rememberSaveable(testOutcome.message) { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Column(
        Modifier.fillMaxWidth()
            // 左缘=文字总线 32（序号盒右缘）；右缘 0 与图标盒同线（主页同口径）
            .padding(start = 32.dp, end = 0.dp, top = 2.dp, bottom = 2.dp)
    ) {
        if (expanded) {
            // 展开＝同一段完整显示（10-10 用户令：不再拼前缀、不换另一句）
            Text(
                fullText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = barColor
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.role_key_test_result_copy),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            clipboard.setText(AnnotatedString(testOutcome.message))
                            android.widget.Toast.makeText(
                                context, context.getString(R.string.copied),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
                Spacer(Modifier.width(24.dp))
                // 10-10 用户令：展开态也给「自定义思考」（与主页同构；池页此前只在收起态挂）
                Text(
                    stringResource(R.string.role_key_thinking_entry),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onEditThinking() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
                Spacer(Modifier.width(24.dp))
                Text(
                    stringResource(R.string.role_key_pool_collapse),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { expanded = false }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 同段截断（10-10）：一源=fullText，收起只是 maxLines=1
                var truncated by remember(testOutcome.message) { mutableStateOf(false) }
                Text(
                    fullText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = barColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { truncated = it.hasVisualOverflow },
                    modifier = Modifier.weight(1f)
                )
                // 动作行（10-10 用户令，与主页同序）：自定义思考（黄态才出，在左）+ 详情（截断才出，在右）
                if (isWarn) {
                    Text(
                        stringResource(R.string.role_key_thinking_entry),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onEditThinking() }
                            .padding(start = 6.dp, end = 8.dp)
                    )
                }
                if (truncated) {
                    // 截断时挂「详情」（与主页同位同词）；右距 2→8 与动作键右线齐
                    Text(
                        stringResource(R.string.role_key_pool_detail),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { expanded = true }
                            .padding(start = 6.dp, end = 8.dp)
                    )
                }
            }
        }
    }
}
