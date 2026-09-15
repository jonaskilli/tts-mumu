package com.github.jing332.tts_server_android.compose.systts.role

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drake.net.utils.withIO
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.common.VoicePickerDialog
import com.github.jing332.tts_server_android.service.systts.help.CharacterRecordsFile
import com.github.jing332.tts_server_android.service.systts.help.VoiceMarksFile
import kotlinx.coroutines.launch

/**
 * 角色管理·1:1 复刻（目目 09-13 拍板「照 v10 插件原样搬，之后他再改」）：
 * 对照 角色管理v10_主题密钥增强.js 逐函数复刻——
 * **平铺列表**（非分组）：每行=左名字列（主名+别名每行一个、性别色圆点、收藏【】、主角👑）
 * + 右侧（发音人标签框 / ⋮ 发音人管理 / ▶ 试听）。
 * 点击名字=勾选（选中背景高亮）；长按名字=操作菜单（合并+跟随/合并+选发音人（标记≥2）、
 * 释放删除已合并角色（有别名）、修改角色名（单选）、删除角色、设为主角）。
 * 发音人标签框点击=换声弹窗（与日志弹窗同款，桥接已通过）。
 * 顶部：书籍栏（书名/切换/修改书名）；密钥管理与备份恢复入口在宿主顶栏（目目 09-14：给角色区留空）。
 * 列表下方「+ 添加角色」。主题🎨按钮不搬（原生即 MD3 主题）。
 */

/**
 * 性别圆点色：男/少年 = 青蓝，女/少女 = 粉红，判不出 = 灰。
 *
 * **只看发音人分类标签**（目目 09-14：「分性别只看分类标签就够了」）——标签本身就是
 * 「中文分类词 + 数字序号」（如 `女青年01`、`少年01`、`少女01`），性别信息已经带在里头，
 * 不必再读记录的 `gender` 字段（那字段常为空，反而会把圆点判成灰）。
 *
 * ⚠️「少年 / 少女」必须点名处理，不能只匹配「男 / 女」：
 * `少年` 里**没有「男」字**（少年 = 少 + 年），只按 男/女 匹配会落进 else 变灰。
 * 故顺序固定：少女 → 少年 → 女 → 男（先点名这一对，再走通用词）。
 */
private fun genderDotColor(tag: String): Color = when {
    tag.contains("少女") -> Color(0xFFE91E63)
    tag.contains("少年") -> Color(0xFF1976D2)
    tag.contains("女") -> Color(0xFFE91E63)
    tag.contains("男") -> Color(0xFF1976D2)
    else -> Color(0xFF9E9E9E)
}

/** 释放并固定的彩色圆点阵（照插件 releaseDotColors 顺序） */
private val releaseDotColors = listOf(
    0xFF7E57C2, 0xFF5C6BC0, 0xFF26A69A, 0xFF8D6E63,
    0xFF66BB6A, 0xFFEC407A, 0xFFFF7043, 0xFF42A5F5,
)

/**
 * 书籍栏书名的颜色：照 v10 `bookNameEditor.setTextColor("#333333")` 取同一枚深灰。
 *
 * 为什么硬编码不走 colorScheme：v10 里这枚色是固定的（与主题无关），目目 09-16 明确
 * 「书名的字体和颜色参考 v10」；之前用过 primary（绿主题下书名染成绿色）已被否。
 * 集中成常量便于日后调，不要在调用点写散色值。
 */
private val BOOK_NAME_COLOR = Color(0xFF333333)

/**
 * 浅一档的容器色（目目 09-15 定案「方案一」）：secondaryContainer 向 background 插 40%。
 *
 * 为什么必须这么写：各主题的 secondaryContainer 深浅不一，绿主题 #D2E8D4 上整页铺满
 * 书栏卡+标签框显得太深；直接改 Color2 的 29 槽基准会动到全 App，按主题各自的
 * secondaryContainer→background 插值则十主题通用、只影响本页。书栏卡与角色行标签框
 * 必须共用同一个函数（两处各算各的将来改比例就会岔色）。
 * 文字仍用 onSecondaryContainer：背景变浅对比度只会更大，不用换。
 */
@Composable
private fun softContainerColor(): Color = lerp(
    MaterialTheme.colorScheme.secondaryContainer,
    MaterialTheme.colorScheme.background,
    0.4f,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RoleListScreen(
    tagRuleId: String,
    reloadKey: Int,
    bottomPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ===== 数据 =====
    var version by remember { mutableIntStateOf(0) }
    var records by remember { mutableStateOf<List<CharacterRecordsFile.RoleRecord>>(emptyList()) }
    var marks by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var voiceNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var currentBook by remember { mutableStateOf("") }
    LaunchedEffect(version, reloadKey) {
        val loaded = withIO {
            val recs = CharacterRecordsFile.readRecords(tagRuleId)
            val groups = dbm.systemTtsV2.getAllGroupWithTts()
            val nameMap = LinkedHashMap<String, String>()
            groups.forEach { g ->
                g.list.forEach { item ->
                    // 只认**启用**配置（与角色管理 v10 getVoiceByTag 查 allEnabled 同口径）：
                    // 某标签一条启用项都查不到 → 角色行显示「标签 + ⚠」失效态
                    // （目目 09-13：删掉启用项后标签就该变成失效态，这是以前的逻辑）
                    if (!item.isEnabled) return@forEach
                    val cfg = item.config as? TtsConfigurationDTO ?: return@forEach
                    val tag = cfg.speechRule?.tag?.trim().orEmpty()
                    if (tag.isEmpty()) return@forEach
                    if (tag !in nameMap) nameMap[tag] = item.displayName
                }
            }
            listOf(recs, nameMap)
        }
        @Suppress("UNCHECKED_CAST")
        records = loaded[0] as List<CharacterRecordsFile.RoleRecord>
        @Suppress("UNCHECKED_CAST")
        voiceNames = loaded[1] as Map<String, String>
        marks = VoiceMarksFile.readAll(tagRuleId)
        currentBook = CharacterRecordsFile.readCurrentBook(tagRuleId)
    }

    fun reload() { version++ }

    // 照插件 onLoadUI：①先跑 initializeFileSystem 自愈（cunfang 空→「默认」、角色数据同步进
    // shuming.<当前书>、当前书补进 liebiao.json 末尾、保证「默认」在列表、去重后落盘）
    // ②再按自动备份开关执行一次备份
    LaunchedEffect(reloadKey) {
        withIO {
            CharacterRecordsFile.initializeFileSystem(tagRuleId)
            if (CharacterRecordsFile.readAutoBackupEnabled(tagRuleId)) {
                CharacterRecordsFile.backupAllFiles(tagRuleId)
            }
        }
    }

    fun toast(resId: Int, vararg args: Any) {
        android.widget.Toast.makeText(context, context.getString(resId, *args), android.widget.Toast.LENGTH_SHORT).show()
    }

    // ===== 搜索 + 多选标记 =====
    var keyword by rememberSaveable { mutableStateOf("") }
    // 标记用**文件下标**（照插件 markedIndices）：同名两条记录能分别标记/删除。
    // 旧版标记的是名字，同名记录只能一起操作——「删一条结果两条都没了」就是这么来的。
    var markedIdx by remember { mutableStateOf<Set<Int>>(emptySet()) }
    fun toggleMark(idx: Int) {
        markedIdx = if (idx in markedIdx) markedIdx - idx else markedIdx + idx
    }
    // 带文件下标的筛选结果：列表行 key / 标记 / 删除 / 改名 / 设主角全部用下标做身份，名字不再承担身份。
    // 搜索字段照插件 filterCharacterList(8019-8031)：**name / aliases / gender / age / voice 五字段**，
    // 任一命中即显示（旧版只看 name + aliases —— 按「男主」「男青年01」这类性别/年龄/发音人查会以为没这个角色）。
    val filtered = records.withIndex().filter { (_, rec) ->
        keyword.isBlank() ||
            rec.name.contains(keyword, true) ||
            CharacterRecordsFile.splitAliases(rec.aliases).any { it.contains(keyword, true) } ||
            rec.gender.contains(keyword, true) ||
            rec.age.contains(keyword, true) ||
            rec.voice.contains(keyword, true)
    }
    val selectableIdx = filtered.map { it.index }.toSet()

    // ===== 弹窗状态 =====
    var menuFor by remember { mutableStateOf<Pair<Int, CharacterRecordsFile.RoleRecord>?>(null) }
    var deleteIdx by remember { mutableStateOf<Set<Int>?>(null) }
    var editFor by remember { mutableStateOf<Pair<Int, CharacterRecordsFile.RoleRecord>?>(null) }
    var releaseForIdx by remember { mutableStateOf<Int?>(null) }
    var releaseTargetFor by remember { mutableStateOf<Pair<String, String>?>(null) } // (ownerName, releaseName) 释放并固定 → 换声弹窗
    // 添加角色（目目 09-14 终版）：直接开绑定类换声弹窗，顶部内嵌角色名填写框，
    // 选好发音人确认即建记录（voice=tag id）——独立名字弹窗/选关键词旧流程均废
    var addingChar by remember { mutableStateOf(false) }
    var mergeFollowFor by remember { mutableStateOf<List<String>?>(null) } // 标记的角色名列表，选目标
    var mergeVoiceTarget by remember { mutableStateOf<String?>(null) } // 合并+选择发音人：目标角色
    var pickerFor by remember { mutableStateOf<CharacterRecordsFile.RoleRecord?>(null) } // 换声（标签框点击）
    var showBookDialog by remember { mutableStateOf(false) }
    // 密钥管理/备份恢复入口已上移到宿主顶栏（目目 09-14：给角色区留空），
    // 弹窗状态与渲染都在 RoleManagementScreen，本页不再持有

    Column(modifier) {
        // ===== 书籍栏（照插件：圆角卡片 = 📖 + 书名 + ✎ 行内改名 + ▾ 管理；
        //      整条卡片点击即展开书籍列表弹窗（插件 showBookSwitchDialog 同入口：书名框与箭头共用））=====
        var editingBook by remember { mutableStateOf(false) }
        var bookEditName by remember { mutableStateOf(TextFieldValue("")) }
        // 书名编辑收尾（照插件 v10 endBookEdit 口径，所有退出路径共用）：
        // 空名回滚原书名、与原书名相同直接退出、真改了才写文件
        fun endBookEdit(save: Boolean) {
            if (!editingBook) return
            val target = bookEditName.text.trim()
            editingBook = false
            bookEditName = TextFieldValue(currentBook, TextRange(currentBook.length))
            if (!save || target.isEmpty() || target == currentBook) return
            scope.launch {
                val ok = withIO { CharacterRecordsFile.renameCurrentBook(tagRuleId, target) }
                toast(if (ok) R.string.role_book_renamed else R.string.role_list_failed, target)
                if (ok) reload()
            }
        }
        // 返回键先退编辑态（目目 09-14：编辑态原本只有 ✓ 一个出口，✓ 一旦不可用就困在里面）
        BackHandler(enabled = editingBook) { endBookEdit(save = false) }
        // 进编辑态即聚焦（照插件 v10：requestFocus + 弹软键盘，省得再点一下输入框）
        val bookFocus = remember { FocusRequester() }
        LaunchedEffect(editingBook) {
            if (editingBook) runCatching { bookFocus.requestFocus() }
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            // 浅一档容器色（目目 09-15「方案一」）：secondaryContainer 原值整页铺满嫌深，
            // 向 background 插 40%，仍带主题色相；与角色行标签框共用 softContainerColor 保同色
            color = softContainerColor(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                // 编辑态不触发展开，避免打断改名输入
                .clickable(enabled = !editingBook) { showBookDialog = true },
        ) {
            Row(
                // start=8：外层 8 + 内层 8 = 16dp 文字左缘，与搜索框/列表行对齐（整体化）
                Modifier.fillMaxWidth().padding(start = 8.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 彩色 📖 emoji（目目 09-15 晚拍板：书籍图标要彩色；推翻 09-14「入口类 emoji
                // 清零」口径——那条是通用纪律，此处是用户对书籍图标的明确偏好，偏好优先）
                Text(
                    "📖",
                    // 14sp 照 v10（bookLabel.setTextSize(14)）：18sp 的 emoji 白吃约 4dp 宽，
                    // 书名一行放不下就折第二行
                    fontSize = 14.sp,
                )
                Spacer(Modifier.width(8.dp))
                if (editingBook) {
                    // 紧凑行内编辑（目目 09-14 二次修：去内层描边/填色——书栏卡本身已是
                    // 灰底块，里面再套一个描边输入框成「框套框」；改无框裸 BasicTextField
                    // 直躺卡上，光标即编辑态，✓✕ 收尾不变。
                    // 出口三个：✓ / ✕ / 键盘回车，空名回滚由 endBookEdit 兜底）
                    Box(Modifier.weight(1f).height(40.dp)) {
                        BasicTextField(
                            value = bookEditName,
                            onValueChange = { bookEditName = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSecondaryContainer),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { endBookEdit(save = true) }),
                            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp)
                                .focusRequester(bookFocus),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (bookEditName.text.isBlank()) Text(
                                        stringResource(R.string.role_book_name),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    inner()
                                }
                            }
                        )
                    }
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).clickable { endBookEdit(save = true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).clickable { endBookEdit(save = false) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                    }
                } else {
                    Text(
                        currentBook,
                        // 目目 09-16：字体与颜色照 v10 —— bookNameEditor.setTextSize(16) +
                        // Typeface.DEFAULT_BOLD + setTextColor("#333333") ⇒ 16sp 加粗深灰；
                        // 这是硬编码色（与 v10 一致），不走 colorScheme
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = BOOK_NAME_COLOR,
                        modifier = Modifier.weight(1f),
                    )
                    // ✎ / ▾管理 照 v10 用裸文本键（padding 12/4/12/4 与 8/4/8/4、14sp）：
                    // 原先的 TextButton 自带 58dp 最小宽 + 12dp 内边距，两个键多占约 23dp，
                    // 书名被挤到第二行（v10 同字号一行放得下）
                    Text(
                        "✎",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                bookEditName = TextFieldValue(currentBook, TextRange(currentBook.length))
                                editingBook = true
                            }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "▾ ${stringResource(R.string.role_book_manage)}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showBookDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
        // ===== 角色区头（目目 09-14 精简：删「👤 角色列表:」前缀——搜索框 hint 自说明，
        //      搜索框（12dp圆角、hint自带🔍、无放大镜图标）占满整行，
        //      全选=文字键嵌搜索框右端，选中态红色显「取消全选」）=====
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val allSelected = markedIdx.containsAll(selectableIdx) && selectableIdx.isNotEmpty()
            // 紧凑搜索框（目目 09-14：OutlinedTextField 最小高 56dp 偏高）——
            // Surface+BasicTextField 手搓 44dp，外观保持 12dp 圆角描边；hint 手绘、光标主色
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.weight(1f).padding(start = 8.dp).height(44.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                        decorationBox = { inner ->
                            Box {
                                if (keyword.isBlank()) Text(
                                    stringResource(R.string.role_search_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                inner()
                            }
                        }
                    )
                    // 方案A 文字键（用户 09-14 定）：无框无底、主色文字——原「填色+描边」
                    // 双重强调是全页唯一彩色填充控件，与「填色=区块/状态」全局语汇冲突；
                    // 选中态=红色「取消全选」，警示语义保留（与模型拉取弹窗 TextButton 同款）；
                    // 关涟漪防高亮叠在搜索框描边内显得脏（同名字列口径）
                    Text(
                        stringResource(
                            if (allSelected) R.string.role_select_all_cancel
                            else R.string.role_list_select_all
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (allSelected) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                markedIdx = if (allSelected) emptySet() else selectableIdx
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
        // 操作提示行（目目 09-14：两行压一行短句、去 emoji、12sp——正文口径精简）
        Text(
            stringResource(R.string.role_hint_line),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            // 目目 09-15 晚：跟下方列表行左对齐——行内容左缘 = LazyColumn 12 + RoleRow 行内 14 = 26dp，
            // 提示行原来 12dp，比列表凸出去一截
            modifier = Modifier.padding(start = 26.dp, end = 12.dp, top = 2.dp, bottom = 2.dp)
        )

        // ===== 平铺角色列表（完全展开）=====
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 12.dp, end = 12.dp, top = 2.dp, bottom = bottomPadding + 96.dp
            )
        ) {
            if (filtered.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.role_list_empty),
                        // 目目 09-14：空状态字号缩小一档（bodyLarge 16sp → bodyMedium 14sp），
                        // 文案已精简为「暂无角色，朗读后自动生成」
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            filtered.forEach { (idx, rec) ->
                // 行 key 用下标：同名两条记录（合并产物 / 框架回写）不会再撞 key（旧版 name+voice 撞了会崩）
                val key = "row_$idx"
                item(key = key) {
                    RoleRow(
                        rec = rec,
                        voiceName = voiceTagText(rec.voice, voiceNames),
                        marks = marks[rec.voice].orEmpty(),
                        marked = idx in markedIdx,
                        onNameClick = { toggleMark(idx) },
                        onNameLongClick = { menuFor = idx to rec },
                        onTagClick = { pickerFor = rec },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
            // 「+ 添加角色」行（照插件：列表下方）
            item(key = "add_row") {
                // 照插件 addCharacterRow：低调灰色小字（12sp #9E9E9E 居中），刻意不抢眼
                Text(
                    stringResource(R.string.role_add_character),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { addingChar = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }

    // ===== 换声（发音人标签框点击，与日志弹窗同款）=====
    pickerFor?.let { rec ->
        VoicePickerDialog(
            anchorConfigId = null,
            anchorTag = rec.voice,
            bindingKey = rec.name,
            titleBadge = rec.name,
            onChanged = { _, _ -> reload() },
            onDismissRequest = { pickerFor = null },
        )
    }
    // ===== 合并+选择发音人：目标=长按角色，选完发音人后把其余标记角色并入 =====
    mergeVoiceTarget?.let { target ->
        val others = markedIdx.mapNotNull { records.getOrNull(it)?.name }.filter { it != target }.toSet()
        VoicePickerDialog(
            anchorConfigId = null,
            anchorTag = records.firstOrNull { it.name == target }?.voice.orEmpty(),
            bindingKey = target,
            titleBadge = target,
            onChanged = { _, _ ->
                scope.launch {
                    val n = withIO { CharacterRecordsFile.mergeCharacters(tagRuleId, target, others) }
                    toast(if (n > 0) R.string.role_list_merge_toast else R.string.role_list_failed, n)
                    markedIdx = emptySet()
                    reload()
                }
            },
            onDismissRequest = { mergeVoiceTarget = null },
        )
    }

    // ===== 长按操作菜单（照插件 showFirstDialog 动态项）=====
    menuFor?.let { (idx, rec) ->
        val markCount = markedIdx.size + if (idx in markedIdx) 0 else 1
        val hasMerged = CharacterRecordsFile.splitAliases(rec.aliases)
            .any { it.trim() != rec.name.trim() }
        AlertDialog(
            onDismissRequest = { menuFor = null },
            title = { Text(stringResource(R.string.role_menu_title)) },
            text = {
                Column {
                    if (markCount >= 2) {
                        MenuActionRow(stringResource(R.string.role_menu_merge_follow), MaterialTheme.colorScheme.primary) {
                            menuFor = null
                            mergeFollowFor = (markedIdx + idx).mapNotNull { records.getOrNull(it)?.name }
                        }
                        MenuActionRow(stringResource(R.string.role_menu_merge_voice), Color(0xFF7E57C2)) {
                            menuFor = null
                            markedIdx = markedIdx + idx
                            mergeVoiceTarget = rec.name
                        }
                    }
                    if (hasMerged) {
                        MenuActionRow(stringResource(R.string.role_menu_release), Color(0xFFFB8C00)) {
                            menuFor = null
                            releaseForIdx = idx
                        }
                    }
                    if (markCount < 2) {
                        MenuActionRow(stringResource(R.string.role_list_menu_rename), Color(0xFF00838F)) {
                            menuFor = null
                            editFor = idx to rec
                        }
                    }
                    MenuActionRow(stringResource(R.string.role_list_menu_delete), MaterialTheme.colorScheme.error) {
                        menuFor = null
                        deleteIdx = markedIdx + idx
                    }
                    MenuActionRow(stringResource(R.string.role_list_menu_set_main), Color(0xFFF57F17)) {
                        menuFor = null
                        scope.launch {
                            // 按下标改（照插件 setAsMainCharacter：`characterRecords[longPressedIndex]`
                            // 只动长按的那一条；旧版按名字改，同名两条会一起变主角）
                            val ok = withIO { CharacterRecordsFile.setMainCharacterAt(tagRuleId, idx) }
                            toast(
                                if (ok) R.string.role_list_set_main_toast else R.string.role_list_failed,
                                rec.name
                            )
                            if (ok) reload()
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { menuFor = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    // ===== 合并+跟随角色：在标记里选目标 =====
    mergeFollowFor?.let { candidates ->
        AlertDialog(
            onDismissRequest = { mergeFollowFor = null },
            title = { Text(stringResource(R.string.role_merge_follow_title)) },
            text = {
                Column {
                    candidates.forEach { name ->
                        MenuActionRow(name, MaterialTheme.colorScheme.primary) {
                            mergeFollowFor = null
                            val others = candidates.filter { it != name }.toSet()
                            scope.launch {
                                val n = withIO { CharacterRecordsFile.mergeCharacters(tagRuleId, name, others) }
                                toast(if (n > 0) R.string.role_list_merge_toast else R.string.role_list_failed, n)
                                markedIdx = emptySet()
                                reload()
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { mergeFollowFor = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // ===== 释放/删除已合并角色（照插件：名字列表逐行 释放并固定/删除）=====
    // 存**下标**而不是记录快照：一个别名被释放之后（尤其释放的正是主名——aliases 首个顶上），
    // 在同一个弹窗里接着释放第二个时，若拿旧快照的 ownerName 去定位会匹配不上原角色
    // （releaseAndFix 按名字找 owner）。按下标每帧重取最新记录，连续释放才对得上。
    releaseForIdx?.let { ridx ->
        records.getOrNull(ridx)?.let { rec ->
            ReleaseDialog(
                rec = rec,
                onDismiss = { releaseForIdx = null },
                onFix = { owner, name ->
                    releaseTargetFor = owner to name
                },
                onDelete = { owner, name ->
                    scope.launch {
                        val ok = withIO { CharacterRecordsFile.removeNameFromRecord(tagRuleId, owner, name) }
                        toast(if (ok) R.string.role_list_delete_toast else R.string.role_list_failed, 1)
                        if (ok) reload()
                    }
                },
            )
        }
    }
    // 释放并固定 → 直接开换声弹窗（目目 09-14：这一步本质＝「解绑别名 + 另立门户换发音人」，
    // 先选一个关键词、再拿关键词当发音人是多余一跳）。落库仍走 releaseAndFix，其余链路
    // （候选 / 试听 / 确认键 / Toast）与「+ 添加角色」完全同源——只有一处实现。
    // 顺带修正口径：原先写进 voice 的是裸关键词（「女青年」），现在是 tag id（「女青年01」），
    // 与本 App「voice=tag id」的数据链一致——旧写法朗读时根本匹配不上发音人。
    releaseTargetFor?.let { (owner, name) ->
        val anchorTag = records.firstOrNull { it.voice.isNotBlank() }?.voice
            ?: CharacterRecordsFile.readVoicePool(tagRuleId).firstOrNull().orEmpty()
        VoicePickerDialog(
            anchorConfigId = null,
            anchorTag = anchorTag,
            bindingKey = "",
            titleBadge = "",
            createIfMissing = true,
            releaseOwnerName = owner,
            releaseName = name,
            onChanged = { _, _ -> reload() },
            onDismissRequest = { releaseTargetFor = null },
        )
    }

    // ===== 修改角色名（多行名称编辑器：主名+别名）=====
    editFor?.let { (idx, rec) ->
        EditNamesDialog(
            initialNames = listOf(rec.name) + CharacterRecordsFile.splitAliases(rec.aliases).filter { it != rec.name },
            onDismiss = { editFor = null },
            onConfirm = { names ->
                scope.launch {
                    // 按下标改（照插件 showEditCharacterDialog 用 position 定位、names 去重）：
                    // 旧版按名字改，同名两条会一起被改，且重复名会被写进 aliases 两条
                    val ok = withIO { CharacterRecordsFile.editCharacterNamesAt(tagRuleId, idx, names) }
                    toast(if (ok) R.string.role_list_rename_toast else R.string.role_list_failed)
                    editFor = null
                    if (ok) reload()
                }
            }
        )
    }

    // ===== 删除角色（单/多共用，对全部标记生效）=====
    deleteIdx?.let { targets ->
        val names = targets.mapNotNull { records.getOrNull(it)?.name }
        AlertDialog(
            onDismissRequest = { deleteIdx = null },
            title = { Text(stringResource(R.string.role_list_delete_title)) },
            text = { Text(stringResource(R.string.role_list_delete_text, names.size, names.take(5).joinToString("、"))) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        // 按下标删（照插件 doDeleteCharacterOperation 只保留未标记 index）：
                        // 旧版按名字删，文件里同名两条会一起没
                        val n = withIO { CharacterRecordsFile.deleteRecordsAt(tagRuleId, targets) }
                        toast(if (n > 0) R.string.role_list_delete_toast else R.string.role_list_failed, n)
                        deleteIdx = null
                        markedIdx = emptySet()
                        if (n > 0) reload()
                    }
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteIdx = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // ===== 添加角色（目目 09-14 终版）：直接开绑定类换声弹窗，角色名在弹窗顶部填写，
    // 选好发音人确认即建记录（voice=tag id）——锚点只需有效（参数/显示基准）：
    // 优先取现有角色的绑定标签，无记录回落池子首个；候选列表=池子∩启用标签，与锚点无关 =====
    if (addingChar) {
        val anchorTag = records.firstOrNull { it.voice.isNotBlank() }?.voice
            ?: CharacterRecordsFile.readVoicePool(tagRuleId).firstOrNull().orEmpty()
        VoicePickerDialog(
            anchorConfigId = null,
            anchorTag = anchorTag,
            bindingKey = "",
            titleBadge = "",
            createIfMissing = true,
            onChanged = { _, _ -> reload() },
            onDismissRequest = { addingChar = false },
        )
    }

    // ===== 书籍管理弹窗（切换/✕删除/新增/多选删除）=====
    if (showBookDialog) {
        BookManagerDialog(
            tagRuleId = tagRuleId,
            onDismiss = { showBookDialog = false },
            onSwitched = { reload() },
        )
    }
    // 修改书名：已改为书籍栏 ✎ 行内编辑（照插件），弹窗通道退役
}

/**
 * 菜单动作行（彩色圆点 + 文字，照插件 showFirstDialog 行样式）。
 * 字号口径（目目 09-14 定：弹窗标题与字号全部对齐主界面实际弹窗 —— 即 M3 AlertDialog 默认档）：
 * 行文字用 bodyMedium 14sp（原 bodyLarge 16sp 比主界面弹窗正文大一号，如「转为子分组」的选项行）。
 */
@Composable
private fun MenuActionRow(text: String, dotColor: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            Modifier.size(7.dp).background(dotColor, CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * 发音人标签文本（照插件 generateVoiceTag 口径）：tag 前缀 + 显示名连写
 * （目目定稿「男主1晓伊」式）；显示名以 tag 开头时不重复拼（防"男主1男主1"）；
 * 查不到配置返回 null（RoleRow 回落 tag + ⚠）。
 *
 * 两级收口（目目 09-14 晚）：
 * ① **显示名本体限 8 个字**（目目 09-14 晚：「显示名不能超过 8 个字」）——超了直接切到
 *    8 字，末尾不补符号。常量就在本文件（TAG_DISPLAY_NAME_MAX_CHARS），**与日志行的
 *    12 字故意不同值**（目目同晚「这俩不要一样，那边也可以加省略号」：标签框是
 *    220dp 窄框、硬切不留符号；日志行宽度富余、放 12 字且带「…」）；
 *    标签框上限 220dp、字号 13sp，装得下"tag 前缀 + 8 个全角字"
 *    （最坏 5 字前缀 + 8 字 = 13 个全角字 ≈ 169dp 文本 + 20dp 内边距 ≈ 189dp）；
 * ② 再按框宽做一次权重字数截断兜底（全角 1.0 / 半角 0.55，预算 14.5 字 ≈ 208dp）
 *    ——tag 前缀特别长时才轮到它，保证框里既不出现「…」也不切到半个字。
 * 旧的「显示名限 20 字 + …」规则已撤（既有 8 字硬上限，20 字也装不进 220dp）。
 */
private fun voiceTagText(tag: String, nameMap: Map<String, String>): String? {
    val disp = nameMap[tag] ?: return null
    val prefix = if (disp.startsWith(tag)) "" else tag
    return cutToTagBoxWidth(prefix + disp.take(TAG_DISPLAY_NAME_MAX_CHARS))
}

/** 角色行标签框专用的显示名上限：8 字，超出直接切、**不补符号**（他不要省略号）。
 *  与日志行的 12 字（可带「…」）**故意不同值**，勿合并。 */
private const val TAG_DISPLAY_NAME_MAX_CHARS = 8

/**
 * 标签框权重字数预算：全角字 1.0 / 半角 0.55（数字、字母、半角符号）。
 * 14.5 字 ≈ 13sp × 14.5 ≈ 188.5dp 文本宽 + 左右各 10dp 内边距 ≈ 208.5dp，
 * 落在标签框 220dp 上限之内，并留约 1 个全角字的安全余量
 * （不同字体下数字/字母的实际字宽有出入，留余量保证 Clip 永不切到半个字）。
 * 显示名已先按 8 字收口（TAG_DISPLAY_NAME_MAX_CHARS），这里只在 tag 前缀偏长时才轮到。
 */
private const val TAG_BOX_CHAR_BUDGET = 14.5f

/** 按标签框可用宽度截断文本：只截不补符号（目目 09-14：不要省略号） */
private fun cutToTagBoxWidth(text: String, budget: Float = TAG_BOX_CHAR_BUDGET): String {
    var used = 0f
    val out = StringBuilder()
    for (ch in text) {
        val w = if (ch.code < 0x2E80) 0.55f else 1.0f
        if (used + w > budget) break
        used += w
        out.append(ch)
    }
    return out.toString()
}

/**
 * 角色行（照插件 createListRow / v9 排布）：左名字列竖排（主名第一行，别名从第二行起各占一行，
 * 每行 = 性别圆点 + 名称 + 收藏【】 + 主角👑），整列垂直居中 → 右侧标签框对这一列上下居中；
 * 右动作列只留发音人标签框（目目 09-13 定：原行内 ⋮ / ▶ 退役，一切操作点标签进换声弹窗），
 * 标签后接该发音人已点亮的标记 emoji（❤️🚶😈，与换声弹窗同源 voice_marks.json）。
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RoleRow(
    rec: CharacterRecordsFile.RoleRecord,
    voiceName: String?,
    marks: List<String>,
    marked: Boolean,
    onNameClick: () -> Unit,
    onNameLongClick: () -> Unit,
    onTagClick: () -> Unit,
) {
    val isFav = rec.obj.optInt("usageCount", 0) == 50
    val isProtagonist = rec.isMain
    // 名字列竖排的全部名称（照 v9：第一行主名，别名从第二行起各占一行）
    val nameList = (listOf(rec.name) + CharacterRecordsFile.splitAliases(rec.aliases))
        .distinctBy { it.trim() }
    Surface(
        // 选中态（用户 09-14 二次修）：整行染色保留，但加 12dp 圆角、
        // 色阶从 surfaceContainerHighest 降到 surfaceContainerHigh 浅一档——
        // 目目指认原先是「一整块直角淡紫」很丑；行内容 padding 不变，选中不跳位
        shape = RoundedCornerShape(12.dp),
        color = if (marked) MaterialTheme.colorScheme.surfaceContainerHigh
        else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            // 照插件 createListRow：内边距 14dp 横 / 10dp 纵（行高 ≥44dp 保证点击区域）
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 名字列
            // 选中态只由整行 Surface 一处表达（目目 09-13 方案 B「整行一块」）：
            // 这里必须关掉按压反馈波纹，否则名字列自己的涟漪会叠成第二块灰。
            Column(
                Modifier
                    .weight(1f)
                    .combinedClickable(
                        onClick = onNameClick,
                        onLongClick = onNameLongClick,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    )
                    .heightIn(min = 44.dp),
                // 名字列整体垂直居中（目目 09-14 修：Column 默认 Top 排列会把 16sp 单行文字顶在
                // min 44dp 的上沿，而右侧标签框是居中于整行的 → 标签框比名字低约 10dp 显歪。
                // 名字列多行（主名 + 别名竖排）时，右侧标签框同样对这整列上下居中）
                verticalArrangement = Arrangement.Center,
            ) {
                // 照 v9 排布：主名第一行，别名从第二行起逐个竖排，每行都带性别色圆点
                nameList.forEachIndexed { idx, name ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 性别色圆点：只看发音人分类标签（tag 本身即「分类词+序号」，见 genderDotColor 注释）
                        // 目目 09-15 晚：4dp 压不住场，加大到 8dp（与密钥页状态点/书籍列表圆点同档）
                        Spacer(Modifier.size(8.dp).background(genderDotColor(rec.voice), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = buildString {
                                if (isFav) append("【$name】") else append(name)
                                if (idx == 0 && isProtagonist) append(" 👑")
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (idx < nameList.lastIndex) Spacer(Modifier.height(2.dp))
                }
            }
            // 右侧动作列：只剩发音人标签框（voice 空不渲染，照插件）；
            // 标签后接已点亮的标记 emoji（顺序同管理弹窗 getVoiceMarkLabel，未点亮不渲染）
            if (rec.voice.isNotBlank()) {
                Spacer(Modifier.width(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 标签文本：正常态由 voiceTagText 按框宽截好；失效态（查不到配置）
                    // 回落 tag + ⚠——⚠ 本身也占宽，故先让出 2 字预算再拼，
                    // 否则尾巴又会被框挤掉（目目 09-14：框里不要出现「…」）
                    val tagLabel = voiceName
                        ?: (cutToTagBoxWidth(rec.voice, TAG_BOX_CHAR_BUDGET - 2f) + " ⚠")
                    // 发音人标签框（失效标签加 ⚠）；点它=换声弹窗（标记 / 删除配置项都在里面）
                    Surface(
                        onClick = onTagClick,
                        shape = RoundedCornerShape(8.dp),
                        // 浅一档容器色（目目 09-15「方案一」）：与书栏卡共用 softContainerColor 同色
                        color = softContainerColor(),
                    ) {
                        Text(
                            tagLabel,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            // 文本已在 voiceTagText 里按框宽收口，正常不会溢出；这里用
                            // Clip 而不是 Ellipsis——万一字体比我估的宽，也绝不吐「…」
                            // （目目 09-14：不要省略号；预算留了约 1 个全角字的余量兜底）
                            overflow = TextOverflow.Clip,
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .widthIn(max = 220.dp)
                        )
                    }
                    val litEmoji = VoiceMarksFile.emojiOf(marks)
                    if (litEmoji.isNotEmpty()) {
                        Text(
                            litEmoji,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 3.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 释放/删除已合并角色弹窗（照插件 doReleaseOperation：逐名字行 释放并固定/删除） */
@Composable
private fun ReleaseDialog(
    rec: CharacterRecordsFile.RoleRecord,
    onDismiss: () -> Unit,
    onFix: (ownerName: String, name: String) -> Unit,
    onDelete: (ownerName: String, name: String) -> Unit,
) {
    val processed = remember { mutableStateOf<Set<String>>(emptySet()) }
    val allNames = (listOf(rec.name) + CharacterRecordsFile.splitAliases(rec.aliases))
        .distinctBy { it.trim() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.role_release_title)) },
        text = {
            Column {
                allNames.forEachIndexed { i, name ->
                    val done = name in processed.value
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.size(8.dp).background(
                            Color(releaseDotColors[i % releaseDotColors.size]), CircleShape
                        ))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (done) "$name ✓" else name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (done) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (!done) {
                            TextButton(onClick = {
                                // 点了就标 ✓（与删除按钮同款）：换声弹窗关掉后回到本弹窗，
                                // 该名字不再显示按钮，避免同一个名字在本次弹窗里被释放两遍
                                processed.value = processed.value + name
                                onFix(rec.name, name)
                            }) {
                                Text(stringResource(R.string.role_release_fix), color = Color(0xFF2E7D32))
                            }
                            TextButton(onClick = {
                                processed.value = processed.value + name
                                onDelete(rec.name, name)
                            }) {
                                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.confirm)) }
        },
    )
}

/** 修改角色名称：多行名称编辑器（第1个=主名，其余=别名） */
@Composable
private fun EditNamesDialog(
    initialNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    // 光标默认落在末尾（照插件 v10 showEditCharacterDialog：setText 后 setSelection(len) 8249）
    val names = remember {
        mutableStateOf(
            initialNames.map { TextFieldValue(it, TextRange(it.length)) }.toMutableList()
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.role_list_menu_rename)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.role_edit_names_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                names.value.forEachIndexed { idx, value ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = value,
                            onValueChange = { run ->
                                names.value = names.value.toMutableList().also { it[idx] = run }
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (names.value.size > 1) {
                            TextButton(onClick = {
                                names.value = names.value.toMutableList().also { it.removeAt(idx) }
                            }) {
                                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                TextButton(onClick = {
                    names.value = (names.value + TextFieldValue("")).toMutableList()
                }) { Text(stringResource(R.string.role_edit_names_add)) }
            }
        },
        confirmButton = {
            val cleaned = names.value.map { it.text.trim() }.filter { it.isNotEmpty() }
            TextButton(
                enabled = cleaned.isNotEmpty(),
                onClick = { onConfirm(cleaned) }
            ) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
