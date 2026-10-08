package com.github.jing332.tts_server_android.compose.systts.role

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.drake.net.utils.withIO
import org.json.JSONArray
import org.json.JSONObject
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.nav.NavTopAppBar
import com.github.jing332.tts_server_android.service.systts.help.AccountPool
import com.github.jing332.tts_server_android.service.systts.help.CharacterRecordsFile
import com.github.jing332.tts_server_android.service.systts.help.KeyListFile
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * 密钥管理页（KeyManagerActivity）：接口分组密钥列表 + 备份恢复 + 书籍管理。
 * 分组 = 一个接口一张卡；条目两行 = 显示名/当前徽章 + 动作图标居右。
 * 当前密钥用状态点 + scheme.secondary 强调（primary 各主题太淡，染了看不出）。
 */

/** 分组后的密钥组（照插件 buildKeyGroups：接口组 + 未分组）；密钥池页解析归属也用它 */
internal class KeyGroup(
    val title: String,
    val entries: List<KeyListFile.KeyEntry>,
    val ifc: KeyListFile.ApiInterface? = null,
    /** 未分组的身份说明（接口组此位置显示网址） */
    val hintRes: Int? = null,
)

internal fun buildKeyGroups(keys: List<KeyListFile.KeyEntry>, ifaces: List<KeyListFile.ApiInterface>): List<KeyGroup> {
    val groups = mutableListOf<KeyGroup>()
    val assigned = mutableSetOf<String>()
    ifaces.forEach { ifc ->
        val entries = keys.filter { KeyListFile.keyBelongsTo(it, ifc) }
        // 建组即渲染：空分组也显示，否则建完组页面不出现、点不到「拉取模型」
        groups.add(KeyGroup(ifc.name, entries, ifc))
        entries.forEach { assigned.add(it.name) }
    }
    // 「直连密钥」桶已退役（1002）：裸 Key 与匹配不上接口的条目统一落「未分组」。
    // 裸 Key 禁止启用（togglePool 拦截）；补全完整格式后由 heal 按一 key 一组自愈归组。
    // 智谱内置种子已退役（10-04 用户令：只认算法补全，不内置 Key）——分组判定零特例，无自动重建
    val ungrouped = keys.filter { it.name !in assigned }
    if (ungrouped.isNotEmpty()) groups.add(
        KeyGroup("未分组", ungrouped, hintRes = R.string.role_key_complete_hint)
    )
    return groups
}

/**
 * 裸文本动作键（全选 / 取消 / 删除不要外框）。
 *
 * 原先照插件 createSmallButton 用「透明底 + 彩色描边」小 chip，三个并排像三枚胶囊，
 * 与本页其余无框动作（组头图标区、条目行名字）语言不统一；去描边后只留文字 + 36dp 热区，
 * 红色删除键靠颜色本身表意（不再靠框）。左右内边距由本函数给，调用侧只用 Spacer 控间距。
 */
@Composable
internal fun FlatTextAction(text: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

/**
 * 测试通过的绿点：M3 没有 success 槽，也不能跟随主题走（红主题下「通过」会变红），
 * 只能用固定语义绿——这里的固定是有意的，别顺手换成 colorScheme。
 * （密钥池页的测试灯同用此色，故开放给同包的 KeyPoolScreen）
 */
internal val TEST_PASS_COLOR = Color(0xFF2E7D32)

/** 测试通过的黄点（10-03）：可达但思考仍开启（分配可能失败/慢）——固定琥珀，理由同绿点 */
internal val TEST_WARN_COLOR = Color(0xFFF9A825)

/**
 * 卡片内「测试结果条 / 探测进度条」的左缘缩进（10-08 定稿=文字总线 32）：
 * 32 = 卡内对勾盒右缘（盒 32×36）= 模型名文字线 = 组名/网址线（四线归一）。
 * 原 40 是把容器边距重复计入的错账（比模型名偏右 8）；行首圆点已撤，本值即文字起线。
 */
private val KEY_RESULT_BAR_START = 32.dp

/** 测试三态 → 圆点颜色（两页共用；null=没测过不显灯）。红=不通、黄=通但思考开启、绿=通且思考已关 */
internal fun testDotColor(verdict: KeyListFile.TestVerdict?, errorColor: Color): Color? = when (verdict) {
    null -> null
    KeyListFile.TestVerdict.FAIL -> errorColor
    KeyListFile.TestVerdict.PASS_THINKING -> TEST_WARN_COLOR
    KeyListFile.TestVerdict.PASS -> TEST_PASS_COLOR
}

/** 从测试 message 里提取用时（「…，123ms」尾段）；提不出返回空串（收起行省略用时） */
internal fun timingOf(message: String): String {
    val m = Regex("(\\d+)\\s*ms").find(message) ?: return ""
    return m.groupValues[1] + "ms"
}

/** 扁平图标动作：无描边无底色，18dp onSurfaceVariant 灰、36dp 热区；删除模式随组头转红。
 *  enabled=false 置灰不可点（调序箭头在列表两端用） */
@Composable
internal fun FlatIconAction(
    icon: ImageVector,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val effectiveTint = if (enabled) tint else tint.copy(alpha = 0.3f)
    Box(
        Modifier.size(36.dp).clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = effectiveTint, modifier = Modifier.size(18.dp))
    }
}

/**
 * 密钥条目 = 一张卡片（**排布一行不动**，只把每个模型包成卡片）：
 * 行首对勾 + 显示名 | 动作图标 ⚡✏🗑 同行居右（启用方式 10-03 二次改版）：
 *  - 行首 = **对勾即启用开关**（照主界面 Item.kt 同款方框，role Switch）——
 *    启用视觉三易其稿：⊕⊖ → 底色承担（0919）→ 描边承担（0920）→ 对勾承担（10-03），
 *    描边随对勾方案一并退役。
 *  - **点名字 = 复制模型名**（10-03 拍板）；多选/组内删除模式下点名字仍是勾选，
 *    行首同一颗对勾也切换为勾选语义。
 *  - ⚡✏🗑 三键图标区 108dp，右对齐后仍与组头图标列垂直成列（📋 复制键退役——与点名字复制合并）。
 *  - 测试圆点保持名字后、闪电前（0920 定稿不动）。
 *
 * 10-07 连体卡重构：条目不再各自成卡（原 ElevatedCard 撤），改为组卡内的一个区块，
 * 行间分隔线由组卡统一画；底色口径保留——多选/组内删除勾中=12% 浅红，否则透明随组卡。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeyEntryRow(
    entry: KeyListFile.KeyEntry,
    enabled: Boolean,
    testOutcome: KeyListFile.TestOutcome?,
    testing: Boolean,
    // 探测进度（10-03 九改）：测试中显示的「探测中：第 N/M 种写法「xxx」」；null=不显示
    probeProgress: String? = null,
    // 模型计费倍率（10-10 甲案）：展示串（"x0.8"/"免费"/"x12→x6"）；null=该站无数据不显示
    modelRate: String? = null,
    selectionMode: Boolean,
    checked: Boolean,
    // 来源标签（10-06 方案B）：密钥 key 段命中账号池 access_token → 名字后绿标「账号池」。
    // ⚠️ 10-09 用户令：账号池绿标撤除（腾空间给分配专用 🚀）——参数保留但不再渲染，
    // 调用侧照常传值，恢复时把下方 fromPool 渲染块的 if 注释放开即可
    fromPool: Boolean = false,
    // 分配专用（10-09）：该键（归一化值）当前持有 assign_marker.json 的唯一标记
    hasAssign: Boolean = false,
    onToggleCheck: () -> Unit,
    onTogglePool: () -> Unit,
    onCopy: () -> Unit,
    onTest: () -> Unit,
    onEdit: () -> Unit,
    // 提示条「思考设置 ›」入口（10-07）：单独回调，进去时直达思考区（展开+预选自定义 JSON），
    // 与上面 onEdit（✏/编辑键，打开完整编辑弹窗的原形态）区分开
    onEditThinking: () -> Unit = onEdit,
    // 长按模型名菜单（10-09）：设为/取消分配专用（权威开关在编辑弹窗 Switch，此处是快捷通道）
    onToggleAssign: () -> Unit = {},
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    // 长按模型名菜单（10-09）：只在普通模式生效——多选/组内删除模式点名字仍是勾选，
    // 菜单入口随 combinedClickable 条件挂载，勾选语义零影响
    var nameMenu by remember { mutableStateOf(false) }
    // 卡片底色两态：多选/组内删除勾中=12% 浅红 > 默认卡面白。
    // 启用态不再染底/描边（10-03 对勾方案：启用视觉全归行首对勾，0920 描边口径一并退役）
    // compositeOver：近似半透明色叠在卡面上，避免半透明直接给 ElevatedCard 透出页面底色
    // 10-08 五令（装机反馈：组卡/模型区分不开）：surfaceContainerLow → surface——
    // 豆绿主题的中性槽由 themedNeutral 掺 4% 主色派生（Theme.kt NEUTRAL_TINT），本就带绿，
    // 与组头 primary 8% 绿带色相撞、明度只差 2~3 灰阶（截图实测 #E6F1E9 vs #E9F0E9）；
    // 回 surface（纯卡面白 #FAFAF3）后绿带↔模型区是色相差，恒定成立
    val cardColor = when {
        selectionMode && checked ->
            MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                .compositeOver(MaterialTheme.colorScheme.surface)
        else -> MaterialTheme.colorScheme.surface
    }

    // 10-07 连体卡：条目行并入组 ElevatedCard。勾中浅红底改由本区块 background 表达
    // （原 ElevatedCard containerColor 口径不变）；卡内校准全是行内相对值，原样保留
    Column(
        modifier = Modifier.fillMaxWidth().background(cardColor)
    ) {
        Row(
            // start 10（10-05 用户拍板：对勾方框原偏左突出——字形左缘 28.6 不在文字线 34 上；
            // 改 10 后方框字形 ≈33.6 与组名/URL 文字同一条竖线，模型名随之后移到 ≈66）
            // end 必须为 0：条目动作图标右缘才能落在卡右缘（= 组头图标区右缘）同列
            Modifier.fillMaxWidth()
                // 卡片本体不可点（10-03：启用走行首对勾、复制走点名字——旧「点卡片启用」退役）
                // start 4→0（10-06 用户：模型名大幅左移——行首改 36dp 自绘对勾盒，名字起线
                // = 对勾字形右缘，比原 48dp M3 触控盒收紧 12dp；触控由盒内 clickable 全覆盖）
                .padding(start = 0.dp, end = 0.dp, top = 8.dp,
                    // 乙案压扁（10-08 用户拍板）：模型行底距 8→3——有结果条时两者间距
                    // 由结果条自身 padding 承担，绿/黄/红态结果行整体上提 ~5dp
                    bottom = if (testOutcome != null && probeProgress == null && !selectionMode) 3.dp else 8.dp),
            // 名字换行成两行时图标垂直居中，不再用 Top 咬行
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 行首对勾：常规=启用开关（role Switch 语义保留）；多选/组内删除模式=勾选。
            // 10-07 四改（装机反馈）：盒 24→32 —— 两个诉求一并解：①用户嫌对勾太小难点，
            // 触摸盒 24→32dp（高仍 36）；②模型名文字线随盒右缘移动后与组名线的差值
            // 收敛（实测截图：盒 24 时代组名 44.9 / 模型名 44.6 本已同线——差的是箭头盒
            // 与对勾盒的字形留白，属字形内空；盒加宽 8 后两条线的**盒缘**同为页 40）。
            // 三改收 24 那版注释里的「名字=页 32」算式是按组头行内距算的，与本行（无
            // start 内距、盒即行首）不可比，以实测截图为准。
            Box(
                modifier = Modifier
                    .size(width = 32.dp, height = 36.dp)
                    // 热区外扩（10-08 用户令：视觉不动只加热区）——负 padding 运行时直接抛
                    // IllegalArgumentException「Padding must be non-negative」（编译/CI/静守卫
                    // 全查不出，真机点卡即崩，02:22 崩溃实锤）。改 requiredSize：size 仍锁
                    // 32×36 视觉占位，点击节点强行撑成 48×48 并居中（四周各溢出 8/6dp），
                    // 等效外扩且不占布局空间
                    .requiredSize(width = 48.dp, height = 48.dp)
                    .then(
                        if (selectionMode) Modifier
                            .clip(CircleShape)
                            .clickable { onToggleCheck() }
                        else Modifier
                            .clip(CircleShape)
                            .clickable { onTogglePool() }
                            .semantics {
                                role = Role.Switch
                                context.getString(
                                    if (enabled) R.string.config_enabled_desc else R.string.config_disabled_desc,
                                    KeyListFile.displayName(entry)
                                ).let {
                                    contentDescription = it
                                    stateDescription = it
                                }
                            }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    modifier = Modifier.scale(0.85f),
                    checked = if (selectionMode) checked else enabled,
                    onCheckedChange = null   // 点击由外层盒接管，避免双触发
                )
            }
            // 名字区 weight(1f)。多选模式下点名字 = 勾选（整行即复选框的延伸）。
            // clickable 只在多选时挂载：非多选挂着 enabled=false 也拦掉整卡的启用切换
            // 点击（0920 实机反馈：只有名字前小空隙能点），条件挂载才干净
            // 10-09：普通模式点名字=复制（原样）+ **长按=分配专用菜单**（combinedClickable，
            // ExperimentalFoundationApi）——多选/组内删除模式没有长按，勾选语义不动。
            // 菜单锚点 = 包住名字 Text 的 Box（weight 上移到 Box）
            Box(modifier = Modifier.weight(1f)) {
                // 名字+🚀 同 Row（10-10 用户令：🚀 紧跟模型名，不再被 weight(1f)
                // 挤到右侧图标区旁）；去绿留粗（10-10 用户令：模型名不染主色，
                // 加粗保留——仍是「这把被分配占用」的扫视锚）
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        KeyListFile.displayName(entry),
                        // 10-09 五令（字号 A 案）：15→14sp——与插件/替换两卡模型名同档，
                        // 长模型名（aion-labs/aion-2.0 类）截断概率降；组名 15 半粗扛层级
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        // 10-10 用户令：去绿（原 hasAssign 染 primary），加粗保留
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (hasAssign) FontWeight.Bold else null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            // weight(fill=false)：胶囊先量、名字拿剩余宽度——短名不留空
                            //（胶囊紧跟名字），长名在两行内换行后省略（10-10 倍率甲案；
                            // 不加 weight 的话长名会吃满整行把胶囊挤成 0 宽）
                            .weight(1f, fill = false)
                            // 多选/组内删除模式点名字=勾选；常规模式点名字=复制模型名
                            //（10-03 拍板，📋 键退役）+ 长按弹分配菜单（10-09）
                            .then(
                                if (selectionMode) Modifier.clickable { onToggleCheck() }
                                else Modifier.combinedClickable(
                                    onClick = { onCopy() },
                                    onLongClick = { nameMenu = true }
                                )
                            )
                    )
                    if (hasAssign && !selectionMode) {
                        Spacer(Modifier.width(4.dp))
                        // 分配专用标记（10-09）：名字后一枚 🚀——只读小标，权威开关在
                        // 编辑弹窗 Switch / 长按菜单（10-10 改：进名字区紧跟模型名）
                        Text("🚀", fontSize = 14.sp)
                    }
                    // 计费倍率（10-10 甲案）：名字后灰底小胶囊，跟 🚀 之后。
                    // 模型名 maxLines=2 可换行，倍率跟在文字流后不挤坏长名；无数据显示时整体缺席
                    if (!selectionMode && modelRate != null) {
                        Spacer(Modifier.width(5.dp))
                        Text(
                            modelRate,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
                if (!selectionMode) {
                    DropdownMenu(expanded = nameMenu, onDismissRequest = { nameMenu = false }) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (hasAssign) "🚀 取消分配专用（当前已设）"
                                    else "🚀 设为分配专用（当前未设）",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (hasAssign) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.primary,
                                    fontWeight = if (hasAssign) null else FontWeight.SemiBold,
                                )
                            },
                            onClick = {
                                nameMenu = false
                                onToggleAssign()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "📋 复制模型名",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            onClick = {
                                nameMenu = false
                                onCopy()
                            }
                        )
                    }
                }
            }
            // 分配专用标记（10-09）已并入名字区 Row（10-10：🚀 紧跟模型名）——
            // 此处不再渲染；来源标签（10-06 方案B）：密钥取自账号池（key 段=某账号 access_token）→ 绿底胶囊。
            // ⚠️ 10-09 用户令：绿标撤除腾空间（分配专用 🚀 顶位）——渲染块注释保留，
            // fromPool 参数与调用侧取值原样留着，用户要恢复时解开即可
            // if (fromPool && !selectionMode) {
            //     Text(
            //         stringResource(R.string.account_pool_source_tag),
            //         style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            //         color = MaterialTheme.colorScheme.primary,
            //         modifier = Modifier
            //             .clip(RoundedCornerShape(6.dp))
            //             .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            //             .padding(horizontal = 5.dp, vertical = 1.dp)
            //     )
            //     Spacer(Modifier.width(4.dp))
            // }
            if (!selectionMode) {
                // 固定宽图标区：测试灯槽 36 + 三键 108 = 144dp（10-07 二改：恢复测试结果点——
                // 与启用池 PoolRow 同构：名字后、闪电前 36dp 槽内 8dp 圆点，三色与提示条同源；
                // 撤掉那版让模型名吃掉整段宽的方案作废）
                Row(
                    Modifier.width(144.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 测试结果圆点（恢复 10-05 定稿形态）：36dp 槽居中 8dp 点，没测=空槽保列对齐
                    Box(
                        Modifier.width(36.dp).height(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        testDotColor(testOutcome?.verdict, MaterialTheme.colorScheme.error)?.let {
                            Box(Modifier.size(8.dp).background(it, CircleShape))
                        }
                    }
                    if (testing) {
                        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                    } else {
                        FlatIconAction(
                            Icons.Default.Bolt,
                            stringResource(R.string.role_key_test),
                            enabled = !testing
                        ) { onTest() }
                    }
                    FlatIconAction(Icons.Default.Edit, stringResource(R.string.role_key_edit)) { onEdit() }
                    FlatIconAction(Icons.Default.DeleteOutline, stringResource(R.string.delete)) { onDelete() }
                }
            }
        }
        // 探测进度行（10-03 九改/十改修）：有进度就显示——组测不设 testingValue，条件不能依赖 testing；
        // 进度存在期间优先于结果条（此时旧结果已过时）；测完进度清掉，回落到下面的结果条
        if (!selectionMode && probeProgress != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    // 与结果条同口径（10-04 用户拍板）：start 对齐模型名、end 对齐动作图标盒
                    .padding(start = KEY_RESULT_BAR_START, end = 0.dp, top = 0.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🔍 " + stringResource(R.string.role_key_probe_progress, probeProgress),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        // 结果提示条（10-03 三~六改）：黄/红/绿三态都在卡内行下方常驻（六改：绿也显示——「测试通过·用时」有普适性）。
        // 用户口径——都放卡片底部：收起=单行省略；点击展开=全文多行 + 动作键；再点收起。
        // 10-08 用户令：①结果条与模型名框对齐（原 end=12 白白少一截文字面积，右缘回到图标盒线）
        // ②红态恒给「详情/复制」（原 message==reason 不给详情——但显示不全正需要点开看）
        // ③用时前置：黄态/绿态把用时放行首圆点后（原放行尾，maxLines=1 时最先被省略号吃掉——
        //   「我都不知道满不满」）
        // 10-08 二令（装机反馈）：黄/红收起态一律**单行**（原 2 行：换行总从「可用」下方起
        // 很怪——用时是独立 Text、正文在自己框里折；且两行+动作行三段太高），
        // 黄态尾挂「展开 ›」、红态尾挂「详情」，动作键全部收进展开态
        if (!selectionMode && testOutcome != null && probeProgress == null) {
            val isWarn = testOutcome.verdict == KeyListFile.TestVerdict.PASS_THINKING
            val isPass = testOutcome.verdict == KeyListFile.TestVerdict.PASS
            val barColor = when {
                isPass -> TEST_PASS_COLOR
                isWarn -> TEST_WARN_COLOR
                else -> MaterialTheme.colorScheme.error
            }
            // 用时（「123ms」）从 message 提取；提不出=空串不显示
            val passTiming = timingOf(testOutcome.message)
            // 用时前缀徽标：统一放行首「● 123ms ·」位置——三态都能看到测试花的时间
            val timingPrefix = if (passTiming.isEmpty()) "" else "$passTiming · "
            // 锁定提示（10-08 用户令：测试完自动设了思考模式要显示出来）——
            // green/warn 且 locked 非空 → 收起行尾部「已锁定 xxx」；点「详情」可见全文
            val lockedSuffix = testOutcome.locked?.takeIf { it.isNotEmpty() }
                ?.let { " · 已锁定 ${it}" } ?: ""
            // 收起行文字（用时已独立前置；红=原始错误全文，靠 maxLines 截断）
            val collapsedText = when {
                isPass -> stringResource(R.string.role_key_test_pass_only) + lockedSuffix
                isWarn -> stringResource(R.string.role_key_test_warn_short) + lockedSuffix
                else -> testOutcome.reason.ifEmpty { testOutcome.message }
            }
            // 红态恒可展开；绿/黄原不可展开——10-09 六令细化：详情键改**超一行才出**，
            // 截断时三态都可展开（展开=TestOutcome 全文，含写法名/状态码等底层信息，
            // 必比摘要多东西），未截断则无详情键（一行了结就不给第二态）
            // 10-10 实锤修复：门槛原为 !isPass && !isWarn——黄态被排除但详情键照出，
            // 点击置 expanded=true 而 549 行渲染条件不成立 → 「详情点不动」；
            // 与池页 KeyPoolScreen 同构条目对齐改 !isPass
            val hasExpandable = !isPass
            var expanded by rememberSaveable(entry.name) { mutableStateOf(false) }
            val clipboard = LocalClipboardManager.current
            Column(
                Modifier
                    .fillMaxWidth()
                    // 10-08：start 对齐模型名文字线；end=0 与动作图标盒右缘同线——
                    // 原先 end=12 让文字白少一截（用户：跟方框对齐能有更多文字面积）
                    // 乙案压扁：top 0（模型行底距已让 3dp）、bottom 8→5，行高再省 3dp
                    .padding(start = KEY_RESULT_BAR_START, end = 0.dp, top = 0.dp, bottom = 5.dp)
            ) {
                // 10-07 用户令：展开态首行让位——原实现首行（截断 reason）+ 下方全文并列，
                // 同一段话读两遍（401 详情尤其明显）；展开后只渲染全文一次。
                // 10-08 用户令「结果条前方灯去掉」：展开态首行圆点一并撤（同理由——整行
                // 文字已是红色，圆点重复编码），全文左缘与收起行/模型名同一条线
                // 10-08 四令（装机反馈）：黄态展开的分支原先套在收起行的 else 里——展开后
                // 收起行+「详情」不肯退场，同一段话两遍。重构为三分支互斥：红展开/黄展开/收起。
                if (expanded && hasExpandable) {
                    // 10-08 用户令（推翻「同 Row 固定用时列」提案）：展开全文**从行首起、
                    // 折行也回行首**——第二行挂用时右缘=每行白一段，没必要。用时只是
                    // 行首普通前缀，连成一段自然折行；收起→展开文字左缘始终不动
                    // 10-09 六令：绿/黄截断也能进来（详情键动态化）——绿/黄展开渲染
                    // TestOutcome 全文（必比摘要多），红照旧 reason 全文；展开分支不再
                    // 限红态，hasExpandable 语义改为「详情点开过」
                    Text(
                        timingPrefix + testOutcome.message,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = barColor
                    )
                    // 底部动作行（右对齐）：复制结果 + 收起（红态展开后「收起」在底部，与复制同排）
                    // 间距 12→24（10-07 用户：两键挨太近像连成一个词）——两键性质不同
                    // （复制 vs 折叠），且都靠右排，加大间距不占额外行宽。
                    // 垂直热区 2→6：原热区仅 11sp 文字+4dp 太薄，点着飘；提高后键高 ≈23dp
                    Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
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
                                    clipboard.setText(
                                        AnnotatedString(
                                            KeyListFile.displayName(entry) + "：" + testOutcome.message
                                        )
                                    )
                                    android.widget.Toast.makeText(
                                        context, context.getString(R.string.copied),
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                        Spacer(Modifier.width(24.dp))
                        Text(
                            "收起",
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
                // 收起行（10-10 用户令：详情/自定义思考两键下移第二行——原先与摘要挤
                // 同一行，长摘要+两键互相挤压）
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 行首圆点已撤（10-08 用户令）：整行文字本身就是状态色（绿/黄/红），
                    // 圆点是重复编码；撤后文字左缘齐模型名文字线。
                    // 扫视锚点不丢——模型行灯槽（乙案保留）仍是行内状态锚。
                    // 10-08 二令：黄/红改单行（原 2 行+黄态独立动作行=三段太高；
                    // 换行还总从「可用」下方起——用时前置是独立 Text，正文在自己框里折）。
                    // 10-08 六令：黄态删「详情」键与展开分支——收起行摘要比全文长、
                    // 展开反而变短（反了）；结论+锁名一行已够。黄态收起行尾挂
                    // 「自定义思考 ›」直键（修思考入口常驻，无需先展开）
                    // 10-09 六令细化（用户拍板）：详情键=**超一行才出**——onTextLayout 的
                    // hasVisualOverflow 动态判（截断即出、放得下即无），三态统一；
                    // 点开=完整 TestOutcome 全文（含写法名/状态码等底层信息），必比摘要多
                    var truncated by remember(entry.name, collapsedText) { mutableStateOf(false) }
                    if (timingPrefix.isNotEmpty()) {
                        Text(
                            timingPrefix,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = barColor,
                            maxLines = 1
                        )
                    }
                    Text(
                        collapsedText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = barColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { truncated = it.hasVisualOverflow },
                        modifier = Modifier.weight(1f)
                    )
                    // 第二行动作行（10-10 用户令）：「详情」（截断才出）+「自定义思考›」
                    // （黄态才出）——原与摘要同排挤一行，摘要截断时两键还互相顶
                    if (truncated || isWarn) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (truncated) {
                                // 截断时三态统一挂「详情」（绿/黄展开=TestOutcome 全文，红=reason 全文）
                                Text(
                                    "详情",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { expanded = true }
                                        .padding(start = 6.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)
                                )
                            }
                            if (isWarn) {
                                // 黄态「自定义思考 ›」直键（原独立动作行撤，省一行高）
                                Text(
                                    stringResource(R.string.role_key_thinking_entry),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { onEditThinking() }
                                        .padding(start = 6.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)
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

/**
 * 组启用状态胶囊（10-07 用户拍板，替掉「(N) 计数 + 三态方框」）：
 * 纯数字「已启用数/总数」，三态用色与形态表达——
 *  - 全启：实心绿底 + 白字；部分：浅绿底(primary 12%) + 主色字；全停：透明底 + 灰描边 + 灰字。
 * 单击=批量启停（全停点=全启；其余点=全停，与旧方框口径一致），语义描述沿用 group_* 三串。
 * 宽：内容自适应（4/4≈34dp、10/11≈44dp），恒窄于旧占位 72~80dp。
 */
@Composable
private fun GroupEnablePill(
    enabled: Int,
    total: Int,
    groupTitle: String,
    // 10-07 用户令：改纯显示——点它切换整组启停太容易误触；批量启停挪组头 🗑 菜单
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val allOn = enabled == total && total > 0
    val noneOn = enabled == 0
    val bg = when {
        allOn -> MaterialTheme.colorScheme.primary
        noneOn -> Color.Transparent
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    }
    val fg = when {
        allOn -> MaterialTheme.colorScheme.onPrimary
        noneOn -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.primary
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .then(
                if (noneOn) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
                else Modifier
            )
            .background(bg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics {
                stateDescription = context.getString(
                    when {
                        allOn -> R.string.group_all_enabled
                        noneOn -> R.string.group_all_disabled
                        else -> R.string.group_part_enabled
                    }, groupTitle
                )
            }
            .padding(horizontal = 6.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$enabled/$total",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = fg,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * 组头 + 元信息行（接口组=网址+尾号小块；未分组=身份说明）。
 * 折叠/展开只认行首展开键（10-07 用户终稿：行内内容密，点组名/空白误触折叠比漏点更烦）；
 * 组名后胶囊 = 整组启停开关（状态+控制一体，10sp）；
 * 动作图标区仅正常模式渲染：+拉取 ⚡测组 ✏编辑接口 🗑两项菜单
 *（删除整组=红🗑 带二次确认；多选删除子项=灰🧹 进组内删除模式，组保留）。
 * 组内删除模式下整块组头替换为「删除密钥 + 全选」标题行（0916 定稿形态恢复）。
 * 10-07：组尾「(N) + 三态方框」合并为纯数字胶囊 GroupEnablePill（省宽 + 不撞行首启用对勾）。
 */
@Composable
private fun GroupHeaderBlock(
    grp: KeyGroup,
    isCollapsed: Boolean,
    enabledCount: Int,
    selectionMode: Boolean,
    deleteMode: Boolean,
    onSetGroupEnabled: (Boolean) -> Unit,
    onFold: () -> Unit,
    onPull: () -> Unit,
    onTestGroup: () -> Unit,
    onEditIfc: () -> Unit,
    menuExpanded: Boolean,
    onMenuOpen: () -> Unit,
    onMenuDeleteAll: () -> Unit,
    onMenuDeleteMulti: () -> Unit,
    onMenuDismiss: () -> Unit,
    testingThisGroup: Boolean,
) {
    val context = LocalContext.current
    // 10-07 连体卡：组头并入组 ElevatedCard（不再裸排）——组=一张卡，折叠时只剩组头行。
    // 卡片间距/边距由调用处的组卡层负责，此处不再吃 top 16
    // 10-08 配色 C 案（用户拍板）：组头区包 primary 绿带当「卡名条」，与模型区拉开层次。
    // 10-09 四令（装机反馈：折叠卡整卡绿连排糊成一团）绿带减淡 8%→5%——
    // 绿的面积太大是「不好看」的本质，减淡后折叠卡退成似有若无的一层底；
    // 绿胶囊/账号池标/状态字相对更跳。primary 跟主题，深浅色模式各自成立
    Column(
        Modifier.fillMaxWidth().background(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        )
    ) {
        Column(Modifier.padding(vertical = 4.dp)) {
            if (deleteMode) {
                // 组内删除模式标题行（10-05 改：全选挪到底部动作行与 取消/删除(N) 同排——
                // 「选谁+执行」都在底部一排，视线不用上下跑；标题恢复 titleMedium SemiBold
                // 与组名同档（原 bodyLarge Medium 太弱）。左右缩进对齐条目名文字列（28dp）；
                // end=8（右线=卡右缘 8）
                Row(
                    Modifier.fillMaxWidth()
                        .padding(start = 28.dp, end = 8.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.role_key_delete_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
            // 10-08 定稿（尺寸四线归一）：折叠键盒 32×36 固定（与模型行对勾盒同规格）——
            // 原裸图标无固定盒，组名线随箭头字形飘；盒右缘=文字总线 32，组名/网址/
            // 模型名/提示条四条线全部钉在这条线上。箭头只对组头行垂直居中（不再跨
            // 组头+网址两行居中——发飘、不属于任何一行）；折叠点击区仍整块组头两行可点。
            // 10-08 修「折叠键没功能」：折叠点击不分散挂多处（两处兄弟 clickable 曾互吞手势），
            // 整块一个 clickable——外层 Row 直接挂 onFold（canFold=false 时不挂），内部
            // 胶囊/图标键各自消费自己的点击，不冒泡上来（M3 组件自带消费，无需额外拦截）
                val canFold = grp.entries.isNotEmpty()
                val arrowAngle by animateFloatAsState(
                    targetValue = if (isCollapsed) -90f else 0f, label = ""
                )
                Row(
                    Modifier.fillMaxWidth().then(
                        if (canFold && !selectionMode) Modifier.clickable(onClick = onFold)
                        else Modifier
                    )
                ) {
                    // 折叠键盒：32宽（=对勾盒宽，盒右缘即文字线）。
                    // 10-08 三修（装机反馈：箭头比组名偏高）：盒高改 40 = 组头行实际高——
                    // 行内 padding 2×2 + 动作图标热区 36（图标热区才是行高主导者；旧算式按
                    // 组名字形 24+4=28 计漏了它，36 盒加内里 2/2 padding 中心落 18 ≠ 行中心
                    // 20 → 箭头恒偏高 2dp）。今盒顶与组头行顶同线（外层 Row 顶对齐）、
                    // 无附加 padding，盒中心 = 行中心。空组（无模型可折）同盒占位（盒宽一致，文字线不偏）
                    Box(
                        Modifier
                            .size(width = 32.dp, height = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (canFold) {
                            Icon(
                                Icons.Default.ExpandMore,
                                contentDescription = context.getString(
                                    if (isCollapsed) R.string.desc_expand_group
                                    else R.string.desc_collapse_group, grp.title
                                ),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp).rotate(arrowAngle)
                            )
                        }
                    }
                    // 右侧两行：组头行 / 网址行（共用 Column）
                    Column(Modifier.weight(1f)) {
                    // ———— 组头行 ————
                    // 组名线 = 折叠键盒右缘 = 卡内 32（10-08 四线归一：组名/网址/模型名/提示条同值）。
                    // end=0：条目卡内 end=0 → 🗑 字形贴卡缘；组头行同口径，
                    // 两组图标列逐像素同列
                    Row(
                        Modifier.fillMaxWidth()
                            .padding(start = 0.dp, end = 0.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左段（名字+胶囊）weight(1f)：折叠点击已上收到整卡 Row（见外层），
                        // 这里不再单独挂 clickable；右段图标区固定 144dp 贴右线
                        Row(
                            Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                grp.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                // 10-08 用户令：组名不染绿——原「有启用染 TEST_PASS_COLOR+Bold、
                                // 全停黑字 SemiBold」的双态撤掉，统一黑字；启用态已有 1/1 胶囊承担，
                                // 组名不再重复编码
                                // 10-09 五令：16→15sp（字号 A 案）——模型名降 14 后组名只靠
                                // 半粗字重扛层级；原 titleMedium(16) 与卡内模型行落差过大
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (!selectionMode) {
                                Spacer(Modifier.width(6.dp))
                                // 组启用胶囊（10-07 用户定稿）：组名后方原位、字号收小（10sp）；
                                // 点击=整组启停（全停→全部启用，其余→全部停用）
                                GroupEnablePill(
                                    enabled = enabledCount,
                                    total = grp.entries.size,
                                    groupTitle = grp.title,
                                    onClick = { onSetGroupEnabled(enabledCount == 0) }
                                )
                            }
                        }
                        if (!selectionMode) {
                        // 固定宽图标区（方案 A）：144dp=4×36dp 热区，组头与模型行图标垂直成列；
                        // 不足 4 键（未分组）右对齐留空。顺序按使用频次：+拉取 ⚡测组 ✏编辑 🗑菜单
                        Row(
                            Modifier.width(144.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            grp.ifc?.let { ifc ->
                                FlatIconAction(
                                    // 拉取模型 = 往组里加模型（+「添加」语义；放大镜易与页内搜索混淆，弃）
                                    Icons.Default.Add,
                                    stringResource(R.string.role_key_fetch)
                                ) { onPull() }
                                if (testingThisGroup) {
                                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(
                                            Modifier.size(18.dp), strokeWidth = 2.dp
                                        )
                                    }
                                } else {
                                    FlatIconAction(
                                        Icons.Default.Bolt,
                                        stringResource(R.string.role_key_test)
                                    ) { onTestGroup() }
                                }
                                FlatIconAction(
                                    Icons.Default.Edit,
                                    stringResource(R.string.role_key_interface_edit)
                                ) { onEditIfc() }
                            }
                            // 组头 🗑 两项菜单（0916 双路径恢复）：删除整组 / 多选删除子项
                            Box {
                                FlatIconAction(
                                    Icons.Default.DeleteOutline,
                                    stringResource(R.string.delete)
                                ) { onMenuOpen() }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = onMenuDismiss
                                ) {
                                    DropdownMenuItem(
                                        // 警示交给红色图标承载，标题不再整行红字（原样太扎眼）
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        text = {
                                            Column {
                                                Text(
                                                    stringResource(R.string.role_key_group_delete_all),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    stringResource(
                                                        R.string.role_key_group_delete_all_sub,
                                                        grp.entries.size
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = onMenuDeleteAll
                                    )
                                    DropdownMenuItem(
                                        // 两项各带一枚 18dp 前置图标，文字左缘才对得齐：
                                        // 删除整组=红🗑（警示），多选删除=灰🧹（组保留、非毁灭）
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.DeleteSweep,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        text = {
                                            Column {
                                                Text(
                                                    stringResource(R.string.role_key_group_delete_multi),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    stringResource(R.string.role_key_group_delete_multi_sub),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = onMenuDeleteMulti
                                    )
                                }
                            }
                        }
                    }
                }
                // 元信息行（10-07 连体卡：折叠时藏进卡内不渲染，展开才出现——组头行即折叠态）。
                // 接口组 = 网址 + 尾号小块；未分组 = 一句身份说明。
                // 左缘 = 组名文字左缘（行首箭头 22 + spacer 3 + 行 start 3 = 28）；右缘 end=8（尾号小块盒缘与卡右缘同列）
                // 10-07 二改（用户令）：网址/尾号行**不随折叠**——折叠收的是下方模型列表，
                // 组卡折叠态 = 组头行 + 网址行（接口身份信息常驻，「折叠后不认识这是哪组」的问题不再有）。
                // 未分组的身份说明行同口径常驻。
                val ifc = grp.ifc
                if (ifc != null) {
                    Row(
                        Modifier.fillMaxWidth()
                            // 10-08 四线归一：本行在「折叠键盒右侧的 Column」里，盒右缘=文字线 32
                            // 已由外层结构给足——旧 start 28 是挪进右列前遗留的独立缩进，
                            // 两道相加网址缩到 52，重构漏改（bug）；归零后网址与组名同一条线。
                            // end=8（尾号小块盒缘与卡右缘同列，不动）
                            .padding(start = 0.dp, end = 8.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            ifc.baseUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            // 网址放不下换行（原单行省略号会吃掉长网址）
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        // 间隙 8→12（10-05 用户：长网址换行时首行尾部离尾号徽章太近，
                        // 观感挤；徽章保持垂直居中于两行 URL）
                        Spacer(Modifier.width(12.dp))
                        // 尾号独立小块（原先挤在网址尾巴上，网址一长就被省略号吃掉）
                        // 10-08 用户定稿：尾段走 KeyListFile.keyTail（>4 显尾4 / 3~4 显尾2 /
                        // ≤2 全显——takeLast(4) 对短 key 会把整把全裸）
                        if (ifc.apiKey.length > 2) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Text(
                                    stringResource(
                                        R.string.role_key_tail, KeyListFile.keyTail(ifc.apiKey)
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                } else {
                    grp.hintRes?.let { hint ->
                        Text(
                            stringResource(hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            // 与网址分支同列（同在折叠键盒右列，文字线 32）；右端同落右线 8
                            modifier = Modifier.padding(start = 0.dp, end = 8.dp, bottom = 6.dp)
                        )
                    }
                }
            } // 右侧两行 Column（组头行/网址行）
        } // 新外层可点 Row（10-08 折叠键修复）
            } // else：组内删除模式只渲染标题行，组头与元信息行都不渲染
        } // Column(padding vertical=4)
    } // GroupHeaderBlock 顶层 Column
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyManagerScreen(tagRuleId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 列表行 📋 复制模型名用
    val clipboard = LocalClipboardManager.current
    var version by remember { mutableIntStateOf(0) }
    var keys by remember { mutableStateOf<List<KeyListFile.KeyEntry>>(emptyList()) }
    var ifaces by remember { mutableStateOf<List<KeyListFile.ApiInterface>>(emptyList()) }
    // 模型倍率缓存（10-10）：键=站点|模型 → 展示串（"x0.8"/"免费"/"x12→x6"）；空=不显示
    var modelRates by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    // 启用池（miyue.txt 启用集，按序 = 规则轮换顺序）；元素为归一化值（normalizePoolValue）
    var pool by remember { mutableStateOf<List<String>>(emptyList()) }
    // 测试结果记忆（归一化密钥值 → 完整结果）：持久化到 key_test_results.json，
    // 「保存到下次测试」语义（10-05 用户拍板）——退出页面再进不丢、重测即覆盖、可手动清。
    // 10-03 三改：从只存三态升级为存 TestOutcome——模型行下方的常驻提示条要显示原因文字
    var testResults by remember { mutableStateOf<Map<String, KeyListFile.TestOutcome>>(emptyMap()) }
    // 分配专用键标记（10-09）：assign_marker.json 的内存镜像——
    // 当前持有标记的归一化密钥值（null=全站无分配键）
    var assignMarker by remember { mutableStateOf<Pair<String, KeyListFile.AssignMarker>?>(null) }
    // 是否已从盘上读过结果表（防 version++ 重载/ON_RESUME 刷新时用旧盘值覆盖当场新测的结果）
    var testResultsLoaded by remember { mutableStateOf(false) }
    // 组折叠状态：持久化到 key_ui_state.json（null=尚未从盘上读，防重载覆盖用户当场切换）
    var collapsed by remember { mutableStateOf<Set<String>?>(null) }
    // 测试中（单条按归一化值记 / 启用池整批）
    var testingValue by remember { mutableStateOf<String?>(null) }
    // 探测进度（10-03 九改/十改）：归一化值 → 「第 N/M 种写法「xxx」」；测试完清空。
    // mutableStateMapOf：组测并发 4 路各在 IO 线程写——普通 Map+ 是读改写有丢更新竞态，状态地图逐键操作安全
    val probeProgress = remember { mutableStateMapOf<String, String>() }
    var testingGroup by remember { mutableStateOf<String?>(null) }
    var testingPoolAll by remember { mutableStateOf(false) }
    // 启用池子页（页内全屏覆盖，返回键退回）
    var showPool by remember { mutableStateOf(false) }
    // 账号池子页（10-06 方案B：页内全屏覆盖，返回键退回；登录/签到/积分/续期）
    var showAccountPool by remember { mutableStateOf(false) }
    // 账号池令牌集（10-06 来源标签）：密钥 value 里的 key 段命中任一账号 access_token → 行内显示「账号池」绿标
    val poolTokens by remember(version) {
        mutableStateOf(AccountPool.load().map { it.accessToken }.toSet())
    }
    // 页面级 ☑ 多选已整体退役（10-08 用户令「多选没用」）：入口/底栏/跨组删除确认全删。
    // 组内多选删除仍在（组头 🗑 菜单），走独立的 deleteModeGroup/deleteChecked。
    // 启用池页的多选移出（状态同样 hoist 在主页）
    var poolSelection by remember { mutableStateOf(false) }
    var poolChecked by remember { mutableStateOf<Set<String>>(emptySet()) }
    var menuGroup by remember { mutableStateOf<String?>(null) }          // 组头 🗑 菜单（两项）
    var deleteGroupConfirm by remember { mutableStateOf<String?>(null) } // 「删除整组」二次确认
    // 组内多选删除（菜单第二项「多选删除子项」）：组保留、只删勾选条目，与页面级 ☑ 多选互斥
    var deleteModeGroup by remember { mutableStateOf<String?>(null) }
    var deleteChecked by remember { mutableStateOf<Set<String>>(emptySet()) }
    var groupDeleteConfirm by remember { mutableStateOf<String?>(null) } // 组内批量删除二次确认

    // 朗读规则会在后台改启用池（miyue.txt：密钥试满重试次数自动停用）；
    // 回到本页（ON_RESUME）时 version++ 重读 keys/ifaces/pool，与盘上状态联动（照 RoleManagementScreen 角色列表同款）。
    // 跳过首次 ON_RESUME（首次进入由 LaunchedEffect(version) 初始加载，避免重复 load）
    var firstResume by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (firstResume) {
                    firstResume = false
                } else {
                    version++
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(version) {
        // 旧数据迁移（幂等）：裸 Key 条目补全智谱全串、池内空地址段补真端点
        // → 分组自愈：匹配不上分组的 @@ 条目按（网址 + 密钥）自动建组
        //（智谱内置种子已退役 10-04：不再内置 Key/自动重建任何组；存量失效 key 由用户手动删）
        withIO {
            KeyListFile.migrateLegacy(tagRuleId)
            KeyListFile.heal(tagRuleId)
            KeyListFile.writeFolderGuide(tagRuleId) // 文件说明.txt（幂等，内容没变不重写）
        }
        val loaded = withIO {
            Triple(
                KeyListFile.readKeys(tagRuleId),
                KeyListFile.readInterfaces(tagRuleId),
                KeyListFile.readCurrentRaw(tagRuleId).trim()
            )
        }
        keys = loaded.first
        ifaces = loaded.second
        // 启用池按实际来（1002 拍板）：没有启用就没有密钥，不再有「池空自动启用第一条」兜底
        //（规则侧池空回落自己的 defaultConfig）；pool_cleared.flag 机制随兜底一并退役
        pool = KeyListFile.parsePoolValues(loaded.third)
        // 测试结果持久化（10-05 用户拍板「保存到下次测试」）：进页面读回上次结论；重测覆盖写。
        // 只在结果表尚未加载时读（与折叠记忆同款防覆盖：version++ 重载不推翻当场测试结果）
        if (testResultsLoaded.not()) {
            testResults = withIO { KeyListFile.readTestResults(tagRuleId) }
            testResultsLoaded = true
        }
        // 分配专用标记（10-09）：进页读一次；之后随 toggleAssign/测试自动佩戴当场更新
        //（读文件很轻，但与折叠记忆同款防覆盖——version++ 重载不推翻用户当场切的状态）
        if (assignMarker == null) {
            assignMarker = withIO { KeyListFile.readAssignMarker(tagRuleId) }
        }
        // 模型倍率缓存（10-10）：每次 version++ 重读（拉取完落盘即刷新显示；
        // 与测试结果不同——倍率只增不因会话变，重读无覆盖风险）
        modelRates = withIO { KeyListFile.readModelRates(tagRuleId) }
        // 折叠记忆：只在首次进页面时从盘上读（后续 version++ 重载不覆盖用户当场切换的折叠）
        if (collapsed == null) {
            collapsed = withIO { KeyListFile.readCollapsedGroups(tagRuleId) }
        }
    }

    fun toast(resId: Int, vararg args: Any) {
        // 无参不过 format（见 lib-common ToastUtils.getStringSafe）：否则串里留 %1$s 就崩
        val msg = if (args.isEmpty()) context.getString(resId) else context.getString(resId, *args)
        android.widget.Toast.makeText(
            context, msg, android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    fun save(list: List<KeyListFile.KeyEntry>) {
        scope.launch {
            val ok = withIO { KeyListFile.saveKeys(tagRuleId, list) }
            toast(if (ok) R.string.role_key_saved else R.string.role_list_failed)
            if (ok) version++
        }
    }
    /** 启用池落盘并刷新（主页与密钥池子页共用的唯一写入口）。空池就是空池，不自动顶回 */
    fun savePoolList(list: List<String>) {
        scope.launch {
            withIO { KeyListFile.savePool(tagRuleId, list) }
            pool = list
            version++
        }
    }
    // 偏重启用确认：待确认启用的条目（togglePool 判偏重时置值，弹窗确认后走 doTogglePool）。
    // 声明必须在 togglePool 之前——Kotlin 局部函数只引用先声明的变量（37736194147 实锤）
    var enableConfirmFor by remember { mutableStateOf<KeyListFile.KeyEntry?>(null) }
    /** togglePool 的执行段（偏重确认弹窗「仍要启用」也走这里）——声明在 togglePool 之前（同 37736194147 作用域铁律） */
    fun doTogglePool(entry: KeyListFile.KeyEntry, norm: String) {
        if (norm in pool) {
            savePoolList(pool - norm)
            toast(R.string.role_key_disabled, KeyListFile.displayName(entry))
        } else {
            savePoolList(pool + norm)
            toast(R.string.role_key_enabled, KeyListFile.displayName(entry), pool.size + 1)
        }
    }
    /** 启用/停用一把密钥（点卡片）：在池里则摘除，不在则追加到队尾（= 轮换顺序最后）。
     *  裸 Key 禁止启用（池是扁平 @@ 串，裸段会错位）：先在编辑框补全为完整格式再启用。
     *  10-09 偏重启用确认：有测试结果且判定「偏重」（思考未关 或 用时 ≥ HEAVY_MODEL_MS 3s，
     *  用户实测校准：真机分布 1.3~5.4s，3s 线筛出 glm-5.3-flash/hy4-preview/kimi-k3-1
     *  与黄态思考未关键）时，启用前弹一次确认——确认才真正启用；停用不拦、无测试结果不拦 */
    fun togglePool(entry: KeyListFile.KeyEntry) {
        val p = KeyListFile.parseKeyValue(entry.value)
        if (p != null && p.isDirect) {
            toast(R.string.role_key_complete_first)
            return
        }
        val norm = KeyListFile.normalizePoolValue(entry.value)
        if (norm.isEmpty()) {
            toast(R.string.role_key_value_empty)
            return
        }
        if (norm !in pool) {
            val r = testResults[norm]
            if (r != null) {
                val ms = Regex("(\\d+)\\s*ms").find(r.message)?.groupValues?.get(1)?.toLongOrNull()
                val heavy = r.thinkingOff == false || (ms ?: 0L) >= KeyListFile.HEAVY_MODEL_MS
                if (heavy) {
                    enableConfirmFor = entry
                    return
                }
            }
        }
        doTogglePool(entry, norm)
    }
    /** 设为/取消分配专用（10-09 长按菜单快捷通道）：写盘（manual=true，全站唯一）后重读镜像 */
    fun toggleAssign(entry: KeyListFile.KeyEntry) {
        val norm = KeyListFile.normalizePoolValue(entry.value)
        if (norm.isEmpty()) {
            toast(R.string.role_key_value_empty)
            return
        }
        scope.launch {
            val nowHas = withIO { KeyListFile.hasAssignMarker(tagRuleId, norm) }
            val ok = withIO {
                if (nowHas) KeyListFile.clearAssignMarker(tagRuleId, norm)
                else KeyListFile.setAssignMarker(tagRuleId, norm, manual = true)
            }
            if (ok) {
                assignMarker = withIO { KeyListFile.readAssignMarker(tagRuleId) }
                android.widget.Toast.makeText(
                    context,
                    if (nowHas) "已取消分配专用：" + KeyListFile.displayName(entry)
                    else "🚀 已设为分配专用：" + KeyListFile.displayName(entry),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else toast(R.string.role_list_failed)
        }
    }
    /** 调轮换顺序：把 fromNorm 那把挪到 toNorm 的位置（启用池页拖动回调） */
    fun movePool(fromNorm: String, toNorm: String) {
        val from = pool.indexOf(fromNorm)
        val to = pool.indexOf(toNorm)
        if (from < 0 || to < 0 || from == to) return
        val m = pool.toMutableList()
        m.add(to, m.removeAt(from))
        savePoolList(m)
    }
    /** 启用池行的显示名：优先按归一化值对回条目；对不上（残留值）显示模型名/Key 尾 */
    fun poolDisplayName(value: String): String {
        val norm = KeyListFile.normalizePoolValue(value)
        keys.firstOrNull { KeyListFile.normalizePoolValue(it.value) == norm }
            ?.let { return KeyListFile.displayName(it) }
        val p = KeyListFile.parseKeyValue(value)
        return when {
            p == null -> value
            !p.isDirect && p.model.isNotBlank() -> p.model
            // 10-08 用户定稿：短 key 尾段走 KeyListFile.keyTail（3~4 显尾2 / ≤2 全显）
            else -> "*" + KeyListFile.keyTail(p.key)
        }
    }
    fun removeFromPool(index: Int) {
        if (index !in pool.indices) return
        val removed = pool[index]
        savePoolList(pool.filterIndexed { i, _ -> i != index })
        toast(R.string.role_key_pool_removed_one, poolDisplayName(removed))
    }
    // ———— 页面级多选三个函数（exitSelection/toggleCheckAll/addSelectedToPool）随 ☑ 入口退役删除 ————

    /** 组头三态对勾的批量启停（10-03 照主界面组头口径：半选/全选单击=全停，全停单击=全启）：
     *  裸 Key 跳过（与卡片单点同一口径）；启用追加到队尾保持组内轮换顺序；全停只摘除本组归一化值 */
    fun setGroupPool(grp: KeyGroup, allEnabled: Boolean) {
        val (ok, bare) = grp.entries.partition {
            val p = KeyListFile.parseKeyValue(it.value)
            p != null && !p.isDirect
        }
        val norms = ok.map { KeyListFile.normalizePoolValue(it.value) }.filter { it.isNotEmpty() }
        if (allEnabled) {
            val fresh = norms.filter { it !in pool }
            if (fresh.isNotEmpty()) savePoolList(pool + fresh)
        } else {
            savePoolList(pool.filter { it !in norms.toSet() })
        }
        if (bare.isNotEmpty()) toast(R.string.role_key_pool_skip_bare, bare.size)
    }
    // ———— 启用池页：多选移出（状态 hoist 在主页，写入口同 savePoolList）————
    fun exitPoolSelection() {
        poolSelection = false
        poolChecked = emptySet()
    }
    fun togglePoolCheckAll() {
        poolChecked = if (poolChecked.containsAll(pool)) emptySet() else pool.toSet()
    }
    fun removePoolBatch() {
        if (poolChecked.isEmpty()) return
        val n = poolChecked.size
        savePoolList(pool.filter { it !in poolChecked })
        toast(R.string.role_key_pool_removed_batch, n)
        exitPoolSelection()
    }
    /** 折叠/展开分组并持久化：下次进页面保持上次状态 */
    fun toggleFold(title: String) {
        val cur = collapsed ?: emptySet()
        val next = if (title in cur) cur - title else cur + title
        collapsed = next
        scope.launch { withIO { KeyListFile.saveCollapsedGroups(tagRuleId, next) } }
    }
    /** 强制展开某组（10-08 装机反馈：新增密钥/拉取模型/组测试完成后自动展开——
     *  操作的结果落在组里，折叠着就什么都看不见；不动持久化文件只改内存态，
     *  用户再手动折叠时 toggleFold 照常落盘） */
    fun expandGroup(title: String) {
        if (collapsed?.contains(title) == true) {
            collapsed = (collapsed ?: emptySet()) - title
        }
    }
    /** 展开并把该组滚进视野（10-08 装机反馈：组在屏幕底部时点展开不知道展开了——
     *  LazyColumn 不自动滚，组头顶在屏底、内容全在屏外）。scrollToItem 定位组卡 item，
     *  稍等重组完成再滚（折叠状态切换是异步重组）。
     *  ⚠️ listState 声明在 Scaffold 内容区，本函数用 lateinit 挂接（见下方赋值）。 */
    var listStateRef: androidx.compose.foundation.lazy.LazyListState? = null
    suspend fun expandAndReveal(title: String) {
        expandGroup(title)
        val idx = buildKeyGroups(keys, ifaces).indexOfFirst { it.title == title }
        // 列表 item 序 = ops(1) + 组序（空态时无组卡；有组才谈得上 reveal）
        listStateRef?.let { ls -> runCatching { ls.scrollToItem(index = 1 + idx) } }
    }
    /** 主页拖动排序已整段删除（用户 0919 终稿：点卡片启用的交互下没有拖动场景；
     * 调轮换顺序在启用池页做，那边拖动保留）。组内显示顺序 = key_list.json 的存储顺序 */
    /** 记一条测试结果并落盘（10-05 持久化：内存表更新+覆盖写 key_test_results.json） */
    fun recordTestResult(norm: String, r: KeyListFile.TestOutcome) {
        testResults = testResults + (norm to r)
        val snapshot = testResults
        scope.launch { withIO { KeyListFile.saveTestResults(tagRuleId, snapshot) } }
    }
    /** 按原始值测试一把密钥（主页条目与启用池行共用；结果按归一化值共享）。
     *  10-05：display 参数退役（toast 不再报名字——卡片就在眼前、模型名还长） */
    fun testRawValue(raw: String) {
        if (raw.isBlank()) {
            toast(R.string.role_key_value_empty)
            return
        }
        scope.launch {
            val norm = KeyListFile.normalizePoolValue(raw)
            testingValue = norm
            // 传原始值：@@串走对话端点（带接口思考模式适配）、纯 Key 走智谱 /models。
            // 进度回调（10-03 九改）在 IO 线程被调——Compose snapshot state 支持后台线程写，直接更新
            val r = withIO {
                KeyListFile.testWithThinking(tagRuleId, raw) { p ->
                    probeProgress[norm] = p
                }
            }
            testingValue = null
            probeProgress.remove(norm)
            recordTestResult(norm, r)
            // 10-10 用户令「全撤」：测试结果 Toast 一并撤——结论已落各模型卡下方结果条
            //（用时/结论/详情常驻可展开），Toast 是同一段话在屏底再闪一遍，纯重复
        }
    }
    fun testKey(entry: KeyListFile.KeyEntry) {
        testRawValue(entry.value)
    }
    // 整组测试并发 4 路（原为串行）。限 4：同站点共用额度，全发会撞 429、被记成「测试失败」；
    // withIO 是真并行、testKey 纯阻塞且每次新建连接 ⇒ 并发安全，灯谁先回谁先亮
    fun testGroup(grp: KeyGroup) {
        val targets = grp.entries.filter { it.value.isNotBlank() }
        if (targets.isEmpty()) {
            toast(R.string.role_key_test_none)
            return
        }
        scope.launch {
            testingGroup = grp.title
            // 10-10 用户令「全撤」：批次开始/完成 Toast 一并撤（组头转圈 + 各行各自转圈是
            // 进行中反馈；结果落各卡结果条，汇总 Toast 也是重复）
            val gate = Semaphore(4)
            targets.map { e ->
                async {
                    gate.withPermit {
                        val norm = KeyListFile.normalizePoolValue(e.value)
                        // 组测同一时刻可能多条在测（并发 4）——进度按各自归一化值分开记
                        val r = withIO {
                            KeyListFile.testWithThinking(tagRuleId, e.value) { p ->
                                probeProgress[norm] = p
                            }
                        }
                        probeProgress.remove(norm)
                        recordTestResult(norm, r)
                    }
                }
            }.awaitAll()
            testingGroup = null
            // 10-10 用户令「全撤」：完成汇总 Toast 撤（10-03 五改已是「组测不弹框」，
            // 现连同这句汇总一并去）——结果看各卡结果条；组头转圈停下即「跑完了」
            expandAndReveal(grp.title)
        }
    }
    // 密钥池整批测试：同 testGroup 的并发 4 路口径
    fun testAllPool() {
        if (pool.isEmpty()) {
            toast(R.string.role_key_test_none)
            return
        }
        scope.launch {
            testingPoolAll = true
            // 10-10 用户令「全撤」：池测开始/完成 Toast 同撤（池页 ⚡ + 各行转圈为进行中反馈）
            val gate = Semaphore(4)
            pool.map { v ->
                async {
                    gate.withPermit {
                        val norm = KeyListFile.normalizePoolValue(v)
                        val r = withIO {
                            KeyListFile.testWithThinking(tagRuleId, v) { p ->
                                probeProgress[norm] = p
                            }
                        }
                        probeProgress.remove(norm)
                        recordTestResult(norm, r)
                    }
                }
            }.awaitAll()
            testingPoolAll = false
            // 10-10 用户令「全撤」：池测完成汇总 Toast 撤（结果看各卡结果条）
        }
    }
    // 批量删除（照插件 deleteMultipleBooks）。被删条目若在启用池里，从池里一并摘除
    //（池值已无对应条目但仍是可用密钥串，规则会照常轮换 ⇒ 必须显式清掉才符合「删除=连启用一起撤」直觉）
    fun deleteNames(names: List<String>) {
        if (names.isEmpty()) return
        val nameSet = names.toSet()
        val remaining = keys.filter { it.name !in nameSet }
        val removedNorms = keys.filter { it.name in nameSet }
            .map { KeyListFile.normalizePoolValue(it.value) }
            .filter { it.isNotEmpty() }
            .toSet()
        val nextPool = pool.filter { it !in removedNorms }
        scope.launch {
            withIO { KeyListFile.saveKeys(tagRuleId, remaining) }
            if (nextPool.size != pool.size) {
                withIO { KeyListFile.savePool(tagRuleId, nextPool) }
                pool = nextPool
            }
            toast(R.string.role_key_deleted_toast, names.size)
            version++
        }
    }

    // 删除整组：接口组连 模型接口中心.json 的接口一起删；未分组只删条目
    // ⚠️ 空组（条目已删光、组还在）走 deleteNames 会提前返回 ⇒ 手动 version++ 触发重载，
    //    否则接口已从盘上删掉、界面还挂着空组直到下次重进（「删了没反应」的观感）
    fun deleteGroupAll(grp: KeyGroup) {
        val names = grp.entries.map { it.name }
        val ifc = grp.ifc
        scope.launch {
            if (ifc != null) {
                withIO {
                    KeyListFile.saveInterfaces(
                        tagRuleId,
                        KeyListFile.readInterfaces(tagRuleId).filter { it.name != ifc.name }
                    )
                }
            }
            if (names.isEmpty()) version++ else deleteNames(names)
        }
    }

    // 弹窗状态
    var showAdd by remember { mutableStateOf(false) }
    var renameFor by remember { mutableStateOf<KeyListFile.KeyEntry?>(null) }
    // 提示条「思考设置 ›」进来的编辑弹窗（10-07）：与 renameFor 同一个弹窗，但直达思考自定义区。
    // 两条路分开存，避免「✏ 编辑」也被强制跳思考区
    var thinkingFor by remember { mutableStateOf<KeyListFile.KeyEntry?>(null) }
    var overwriteFor by remember { mutableStateOf<Triple<String, String, Pair<String, String>>?>(null) } // (新名, 值, 思考模式 to 自定义JSON) 覆盖确认
    var deleteFor by remember { mutableStateOf<KeyListFile.KeyEntry?>(null) }
    var ifcFormFor by remember { mutableStateOf<KeyListFile.ApiInterface?>(null) } // 组头 ✏️ 编辑该接口
    var showPullModels by remember { mutableStateOf(false) }
    var pullForIfc by remember { mutableStateOf<String?>(null) } // 组头 🔍 预选接口

    // 返回键：组内删除模式先退组内删除；页面级多选先退多选（两种多选互斥，同一时刻至多一种在）
    BackHandler(enabled = deleteModeGroup != null) {
        deleteModeGroup = null
        deleteChecked = emptySet()
    }
    // 页面级 ☑ 多选已删：原「BackHandler(selectionMode)→exitSelection」随状态链一并退役

    // 启用池子页：页内全屏覆盖（照 KeyManagerActivity 的独立全屏页模式，返回键退回主页）。
    // 状态全部 hoist 在主页（池、测试结果、测试中标记、多选），子页是纯展示 + 回调
    // 账号池子页（10-06 方案B）：同级全屏覆盖，返回键退回密钥主页。
    // 10-07 修「点不开」：本块漏了 return——启用池同款结构（覆盖后 return 终止主页渲染），
    // 这里没 return 时 Compose 会把账号池 Scaffold 和主页 Scaffold 叠着组进同一布局，
    // 实机表现即「点了账号池没反应」（后组内容的布局被前者吞/测距异常）。
    if (showAccountPool) {
        com.github.jing332.tts_server_android.compose.systts.account.AccountPoolScreen(
            onBack = { showAccountPool = false }
        )
        return
    }

    if (showPool) {
        KeyPoolScreen(
            pool = pool,
            keys = keys,
            ifaces = ifaces,
            // 10-08 卡片化：传完整 TestOutcome（含用时/结论/锁定）——池卡要渲染与主页
            // 同构的单行结果条，只有 verdict 拼不出文字
            testByValue = testResults,
            // 模型倍率表（10-10）：池页模型行同主页显示倍率胶囊（键=站点|模型）
            modelRates = modelRates,
            testingValue = testingValue,
            batchTesting = testingPoolAll,
            selectionMode = poolSelection,
            checked = poolChecked,
            onBack = { showPool = false },
            onToggleSelectionMode = {
                if (poolSelection) exitPoolSelection() else poolSelection = true
            },
            onToggleCheck = { norm ->
                poolChecked = if (norm in poolChecked) poolChecked - norm else poolChecked + norm
            },
            onToggleCheckAll = { togglePoolCheckAll() },
            onMove = { fromNorm, toNorm -> movePool(fromNorm, toNorm) },
            onRemove = { index -> removeFromPool(index) },
            onCopy = { display ->
                clipboard.setText(AnnotatedString(display))
                toast(R.string.role_key_copied_model)
            },
            onEdit = { norm ->
                // 从启用池直接进编辑：关池子页 → 打开对应条目的编辑弹窗
                keys.firstOrNull { KeyListFile.normalizePoolValue(it.value) == norm }?.let { e ->
                    showPool = false
                    renameFor = e
                }
            },
            onTest = { raw, _ -> testRawValue(raw) },
            onTestAll = { testAllPool() },
            onRemoveBatch = { removePoolBatch() },
        )
        return
    }

    // 独立全屏页（照替换管理 / 插件管理模式）：返回键退页，导入/导出在顶栏
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NavTopAppBar(
                title = { Text(stringResource(R.string.role_key_title)) },
                // 10-06 用户：标题离返回键太远，收紧（12→4；仅本页，其他页默认不变）
                titleStartPadding = 4.dp,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                },
                // 顶栏（10-08 装机反馈终稿）：「密」标题竖排的根因是动作区太宽——
                // ①导入/导出撤文字只留图标（用户令：文字删除；含义由 contentDescription 承载）
                // ②页面级多选 ☑ 整个删除（用户：感觉没用——单条启停/删除在卡内都有，
                // 整组启停在组头胶囊；随删 page 级 selectionMode 底栏与其状态链）
                // 10-09 二令（装机反馈）：账号池入口回顶栏——操作行四胶囊放不下（「账号池」
                // 胶囊挤压其余三键、视觉挤成一团）；顶栏动作区图标化后足够容纳，纯图标
                // 误触风险已随「含义进 contentDescription+点开是独立子页」可接受
                // 10-09 三令：账号池入口要重点突出——纯图标退场，改 TextButton（图标+文字）。
                // 顶栏动作区已图标化（导入/导出本就无文字），一个文字键放得下且视觉重心立住；
                // 「密」标题竖排风险不再（当时根因=动作区总宽过大，现只多一个短文字键）
                actions = {
                    // 账号池（10-09 二令回顶栏 / 三令改图标+文字 / 四令改名 Jet 与 DSH
                    // Jet Hub 呼应）：FlightTakeoff 图标 + 「Jet」文字
                    TextButton(
                        onClick = { showAccountPool = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        // 高度对齐 IconButton 48 热区；高度In设 min 防文字换行顶高顶栏
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(
                            Icons.Default.FlightTakeoff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.account_pool_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    // 导入（纯图标；一步导入逻辑不变）
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (!withIO { KeyListFile.exportFileExists(tagRuleId) }) {
                                    toast(R.string.role_key_no_export)
                                } else {
                                    val data = withIO {
                                        KeyListFile.readExportFile(tagRuleId, KeyListFile.EXPORT_FILE_NAME)
                                    }
                                    if (data == null) toast(R.string.role_list_failed)
                                    else {
                                        val (added, skipped, addedIfc) =
                                            withIO { KeyListFile.importAll(tagRuleId, data) }
                                        toast(R.string.role_key_import_done, added, skipped, addedIfc)
                                        version++
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = stringResource(R.string.role_key_action_import),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // 导出（纯图标）
                    IconButton(
                        onClick = {
                            scope.launch {
                                val name = withIO { KeyListFile.exportKeys(tagRuleId, keys) }
                                if (name != null) toast(R.string.role_key_exported, keys.size, name)
                                else toast(R.string.role_list_failed)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.FileUpload,
                            contentDescription = stringResource(R.string.role_key_action_export),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            // 10-08：页面级 ☑ 多选已删（入口+底栏整体退役）——单条启停/删除在卡内、
            // 整组启停在组头胶囊、组内多选删除走组头 🗑 菜单（isDeleting 底部动作行在组卡内）
        }
    ) { paddingValues ->
        val listState = rememberLazyListState()
        listStateRef = listState
        val groups = buildKeyGroups(keys, ifaces)
        // 甲方案（10-09 用户拍板）：上浮只在进页时算一次——以 keys 实例为快照键记「哪些组
        // 当时启用中」，页内启停只改对勾不重排（原实时排序导致编辑中分组跳走、用户追着找）。
        // keys 引用变化（进页/导入/拉模型/删条目后重载）即重算快照；同一实例内 pool 变化不算。
        val floatSnapshot = remember(keys) {
            groups.associate { g ->
                g.title to g.entries.any { KeyListFile.normalizePoolValue(it.value) in pool }
            }
        }
        // 主页拖动排序已整段删除（用户 0919 终稿）：无 reorderable modifier、无落位回调；
        // LazyColumn 仅为长列表性能保留
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            // 10-07 二改（用户令：边距回 16）——容器 start 4→12，容器 12 + 组卡 4 = 卡缘 16，
            // 对齐全站 16 内容线（行首箭头取消后组名文字线 = 卡缘）。
            // end 保持 8（右线不动：条目图标右缘对卡右缘、组头图标区右缘同列的校准全在行内相对值里）
            contentPadding = PaddingValues(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 12.dp)
        ) {
            item(key = "ops") {
                    // 操作行三键（10-08 用户拍板乙案）：键宽内容自适应（0919 weight 均分折行坑
                    // 与它无关——根本不均分宽），SpaceBetween 首键贴左/尾键贴右、两缝自动均分，
                    // 与下方组卡同宽对齐；键左右内距 12→18 兜住文字呼吸。
                    // 高度 44dp 不动（10-07 定的，加厚显挤）
                    // 10-09 二令（装机反馈）：四胶囊放不下——账号池回顶栏，操作行回三键
                    // （新增密钥/拉取模型/池(N)），contentPadding 回 18、字号回 14sp（A1 的
                    // 收缩兜底一并退役）
                    Row(
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showAdd = true },
                            modifier = Modifier.heightIn(min = 44.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp)
                        ) {
                            Text(stringResource(R.string.role_key_add), maxLines = 1, style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = { showPullModels = true },
                            modifier = Modifier.heightIn(min = 44.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp)
                        ) {
                            Text(stringResource(R.string.role_key_fetch), maxLines = 1, style = MaterialTheme.typography.labelLarge)
                        }
                        // 启用池子页入口：调轮换顺序 / 移出 / 整批测试在那边做
                        FilledTonalButton(
                            onClick = { showPool = true },
                            modifier = Modifier.heightIn(min = 44.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp)
                        ) {
                            // 10-10 用户令：补全回「启用池(N)」（role_key_pool_open 现成串；
                            // 当年 fe86073 缩短是 A1 四胶囊时代口径，1fb4ea0 回三键后没改回）
                            Text(stringResource(R.string.role_key_pool_open, pool.size), maxLines = 1, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            // 有分组（哪怕全是空组）就渲染：先建组、再拉模型这条路必须走得通
            if (groups.isEmpty()) {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.role_key_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                    )
                }
            } else {
                // 10-07 用户令：启用中的组自动上浮——甲方案（10-09 用户拍板）改为按
                // floatSnapshot（进页快照）排序：两档内部保持原相对顺序（稳定排序），
                // 页内启停不再触发重排（消除编辑中分组跳走）
                groups.sortedByDescending { g -> floatSnapshot[g.title] == true }.forEach { grp ->
                    val isCollapsed = collapsed?.contains(grp.title) == true
                    // 组内删除模式（菜单第二项「多选删除子项」）：只对该组生效，组保留
                    val isDeleting = deleteModeGroup == grp.title
                    val selCount = grp.entries.count { it.name in deleteChecked }
                    // 组头三态对勾判据（10-03 照主界面）：0=全停 / 全数=全启 / 中间=部分
                    val enabledCount = grp.entries.count {
                        KeyListFile.normalizePoolValue(it.value) in pool
                    }
                    // 组头不可拖动（用户 0919 实机：展开态拖组头与子项交错、必须收起分组才顺，
                    // 收益配不上体验——组序不常调，取消；子项组内拖动保留）
                    // 10-07 连体卡：一组一张卡——组头/元信息/条目/提示条全在卡内，条目间画分隔线
                    // 10-08 配色 C 案（用户拍板）：卡底转 surface（模型区白）——红/黄结果字
                    // 在白底对比度回来；组头绿带由 GroupHeaderBlock 自包（视觉=卡名条）
                    item(key = "g:" + grp.title) {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, end = 0.dp, top = 10.dp, bottom = 4.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                        GroupHeaderBlock(
                            grp = grp,
                            isCollapsed = isCollapsed,
                            enabledCount = enabledCount,
                            onSetGroupEnabled = { allEnabled -> setGroupPool(grp, allEnabled) },
                            // 页面级 ☑ 多选已删：组头只在组内删除模式渲染删除标题行
                            selectionMode = isDeleting,
                            deleteMode = isDeleting,
                            onFold = { toggleFold(grp.title) },
                            onPull = {
                                grp.ifc?.let { ifc -> pullForIfc = ifc.name }
                                showPullModels = true
                            },
                            onTestGroup = { testGroup(grp) },
                            onEditIfc = { grp.ifc?.let { ifc -> ifcFormFor = ifc } },
                            menuExpanded = menuGroup == grp.title,
                            onMenuOpen = { menuGroup = grp.title },
                            onMenuDeleteAll = {
                                menuGroup = null
                                deleteGroupConfirm = grp.title
                            },
                            onMenuDeleteMulti = {
                                menuGroup = null
                                deleteModeGroup = grp.title
                                deleteChecked = emptySet()
                                if (isCollapsed) toggleFold(grp.title) // 组内删除要见子项，折叠组自动展开
                            },
                            onMenuDismiss = { menuGroup = null },
                            testingThisGroup = testingGroup == grp.title,
                        )
                        // 组内删除模式进组时自动展开（折叠组没法多选子项），退出恢复原折叠
                        if (!isCollapsed || isDeleting) {
                            // 组头区与模型区分隔线（10-07 装机反馈）：比条目之间的线重一档——
                            // 它是「组信息 / 模型列表」的分界，模型行间那条只做行切分
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            grp.entries.forEachIndexed { i, entry ->
                                val norm = KeyListFile.normalizePoolValue(entry.value)
                                // 条目分隔线（连体卡内，10-07）：淡描边半透明，只做视觉切分不抢内容
                                if (i > 0) HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                KeyEntryRow(
                                    entry = entry,
                                    enabled = norm in pool,
                                    testOutcome = testResults[norm],
                                    testing = testingValue == norm,
                                    probeProgress[norm],
                                    // 模型倍率（10-10 甲案）：组站点|模型 查缓存；无数据传 null 不显示
                                    modelRate = grp.ifc?.let { ifc ->
                                        KeyListFile.parseKeyValue(entry.value)?.model
                                            ?.takeIf { it.isNotBlank() }
                                            ?.let { m -> modelRates[KeyListFile.rateKey(ifc.baseUrl, m)] }
                                    },
                                    // 组内删除模式：复选框顶替行首对勾、动作区隐藏、勾中染浅红。
                                    // 10-08：页面级 ☑ 多选已删，selectionMode 只剩 isDeleting 一个来源
                                    selectionMode = isDeleting,
                                    checked = entry.name in deleteChecked,
                                    // 来源标签（10-06 方案B）：key 段命中账号池 access_token → 绿标「账号池」
                                    // ⚠️ 10-09 用户令：绿标不再渲染（参数保留，KeyEntryRow 内已注释渲染块）
                                    fromPool = run {
                                        val k = KeyListFile.parseKeyValue(entry.value)
                                        k != null && k.key.isNotEmpty() && k.key in poolTokens
                                    },
                                    // 分配专用（10-09）：该键持有 assign_marker.json 唯一标记 → 🚀 + 主色名
                                    hasAssign = assignMarker?.first == norm,
                                    onToggleCheck = {
                                        deleteChecked = if (entry.name in deleteChecked)
                                            deleteChecked - entry.name
                                        else deleteChecked + entry.name
                                    },
                                    onTogglePool = { togglePool(entry) },
                                    // 📋 列表行复制模型名（编辑弹窗里复制的才是完整密钥串）
                                    onCopy = {
                                        clipboard.setText(AnnotatedString(KeyListFile.displayName(entry)))
                                        toast(R.string.role_key_copied_model)
                                    },
                                    onTest = { testKey(entry) },
                                    onEdit = { renameFor = entry },
                                    // 提示条「思考设置 ›」→ 直达自定义 JSON 区（10-07 用户令）
                                    onEditThinking = { thinkingFor = entry },
                                    // 长按模型名菜单（10-09）：设为/取消分配专用快捷通道
                                    onToggleAssign = { toggleAssign(entry) },
                                    onDelete = { deleteFor = entry }
                                )
                            } // forEachIndexed（条目区块已并入组卡，无独立 item）
                        // 组内删除模式动作行（条目区块之后，10-05 改）：全选挪下来与 取消/删除(N)
                        // 同排——选谁+执行一条线（原全选在顶部标题行，视线跑两趟）；
                        // 全选靠左、执行键靠右，三键都是无框文字键
                        if (isDeleting) {
                                Row(
                                    Modifier.fillMaxWidth()
                                        .padding(start = 28.dp, end = 8.dp, top = 2.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FlatTextAction(
                                        stringResource(R.string.select_all),
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    ) {
                                        // 全组全选/反选（原 GroupHeaderBlock 顶部全选的内联逻辑随键搬迁）
                                        val allSel = grp.entries.all { it.name in deleteChecked }
                                        val names = grp.entries.map { it.name }.toSet()
                                        deleteChecked =
                                            if (allSel) deleteChecked - names else deleteChecked + names
                                    }
                                    Spacer(Modifier.weight(1f))
                                    FlatTextAction(
                                        stringResource(R.string.cancel),
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    ) {
                                        deleteModeGroup = null
                                        deleteChecked = emptySet()
                                    }
                                    FlatTextAction(
                                        stringResource(R.string.role_key_delete_n, selCount),
                                        MaterialTheme.colorScheme.error
                                    ) {
                                        if (selCount == 0) toast(R.string.role_key_delete_none)
                                        else groupDeleteConfirm = grp.title
                                    }
                                }
                        } // if (isDeleting)
                        } // if (!isCollapsed || isDeleting)——展开内容收尾
                        } // ElevatedCard：连体卡内容收尾
                    } // item(key = "g:")——一组一张卡
                }
            }
        }
    }
    // 页面级多选删除确认弹窗已随 ☑ 多选入口退役（10-08）

    // 删除整组确认（显示将删条数）
    deleteGroupConfirm?.let { gTitle ->
        val grp = buildKeyGroups(keys, ifaces).firstOrNull { it.title == gTitle }
        val n = grp?.entries?.size ?: 0
        AlertDialog(
            onDismissRequest = { deleteGroupConfirm = null },
            title = { Text(stringResource(R.string.role_key_delete_group_title)) },
            text = { Text(stringResource(R.string.role_key_delete_group_text, gTitle, n)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteGroupConfirm = null
                    if (grp != null) deleteGroupAll(grp)
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteGroupConfirm = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 偏重启用确认（10-09）：该键最新测试「偏重」（思考未关 或 用时 ≥HEAVY_MODEL_MS 3s，
    // 用户实测校准线）→ 启用前问一次。只在有测试结果时弹（无结果不拦）；停用永不拦。
    // 判定与拦截在 togglePool 内
    enableConfirmFor?.let { entry ->
        val norm = KeyListFile.normalizePoolValue(entry.value)
        val r = testResults[norm]
        val ms = r?.message?.let { m -> Regex("(\\d+)\\s*ms").find(m)?.groupValues?.get(1)?.toLongOrNull() }
        val why = when {
            r == null -> ""
            r.thinkingOff == false -> "思考未关闭，会拖慢角色分配，仍要启用？"
            ms != null -> "该模型测试耗时 ${"%.1f".format(ms / 1000.0)}s，会拖慢角色分配，仍要启用？"
            else -> ""
        }
        AlertDialog(
            onDismissRequest = { enableConfirmFor = null },
            title = { Text("🚀 仍要启用？") },
            text = { Text(why) },
            confirmButton = {
                TextButton(onClick = {
                    enableConfirmFor = null
                    doTogglePool(entry, norm)
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { enableConfirmFor = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 组内批量删除确认（「多选删除子项」勾中 N 条 → 二次确认，组保留）
    groupDeleteConfirm?.let { gTitle ->
        val grp = buildKeyGroups(keys, ifaces).firstOrNull { it.title == gTitle }
        val targets = grp?.entries?.filter { it.name in deleteChecked }?.map { it.name }.orEmpty()
        AlertDialog(
            onDismissRequest = { groupDeleteConfirm = null },
            title = { Text(stringResource(R.string.role_key_delete_title)) },
            text = { Text(stringResource(R.string.role_key_delete_batch_text, gTitle, targets.size)) },
            confirmButton = {
                TextButton(onClick = {
                    groupDeleteConfirm = null
                    deleteModeGroup = null
                    deleteChecked = emptySet()
                    deleteNames(targets)
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { groupDeleteConfirm = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 新增（名称留空自动生成；同组重名 → 覆盖确认，跨组撞名 → 加序号另存）
    if (showAdd) {
        KeyEditDialog(
            tagRuleId = tagRuleId,
            initial = null,
            existing = keys,
            onDismiss = { showAdd = false },
            onConfirm = { name, value, overwrite, thinkMode, thinkCustom ->
                if (overwrite) {
                    showAdd = false
                    overwriteFor = Triple(name, value, thinkMode to thinkCustom)
                } else {
                    showAdd = false
                    val newEntry = KeyListFile.KeyEntry(
                        name = name, keyCode = KeyListFile.nextKeyCode(keys), value = value,
                        thinkingMode = thinkMode, thinkingCustom = thinkCustom,
                    )
                    save(keys + newEntry)
                    // 新增后自动展开其所属组（10-08：结果落在组里，折叠着看不见）；
                    // 未分组条目没有组卡可展开，跳过
                    val p0 = KeyListFile.parseKeyValue(value)
                    if (p0 != null && !p0.isDirect) {
                        buildKeyGroups(keys + newEntry, ifaces)
                            .firstOrNull { g -> g.entries.any { it.name == newEntry.name } && g.ifc != null }
                            ?.let { expandGroup(it.title) }
                    }
                    // 手动模式随条目带锁：写进锁定表（规则读这份）；auto 不写（等 ⚡ 探测）
                    val p = KeyListFile.parseKeyValue(value)
                    if (p != null && !p.isDirect && thinkMode != KeyListFile.THINKING_AUTO) {
                        scope.launch {
                            withIO { KeyListFile.saveThinkingParam(tagRuleId, p.url, p.model, thinkMode, thinkCustom) }
                        }
                    }
                    // 第一把密钥：池空才自动入池接管（不打扰已有启用集）
                    // ⚠️ 旧版无条件三写 miyue ⇒ 新增 B 会把正在朗读用的密钥静默切走
                    if (pool.isEmpty()) {
                        val norm = KeyListFile.normalizePoolValue(value)
                        if (norm.isNotEmpty()) savePoolList(listOf(norm))
                        toast(R.string.role_key_add_first, name)
                    } else {
                        toast(R.string.role_key_saved)
                    }
                }
            }
        )
    }
    // 改名（重名 → 拒绝并提示，照插件 showKeyNameDialog）
    // 编辑弹窗：✏ 编辑与「思考设置 ›」共用（后者多传 jumpToThinkingCustom=true 直达 JSON 区）。
    // 取 renameFor 或 thinkingFor 命中的那个（两者互斥：打开一个时另一个必为 null）
    (renameFor ?: thinkingFor)?.let { entry ->
        KeyEditDialog(
            tagRuleId = tagRuleId,
            initial = entry,
            existing = keys,
            jumpToThinkingCustom = thinkingFor != null,
            onAssignChanged = {
                scope.launch { assignMarker = withIO { KeyListFile.readAssignMarker(tagRuleId) } }
            },
            onDismiss = { renameFor = null; thinkingFor = null },
            onDelete = { renameFor = null; thinkingFor = null; deleteFor = entry },
            onConfirm = { name, value, overwrite, thinkMode, thinkCustom ->
                renameFor = null
                thinkingFor = null
                if (overwrite && name != entry.name) {
                    // 改成已存在的名字 → 拒绝（照插件密钥详情页「保存」）
                    // ⚠️ 旧版走覆盖分支：只覆盖同名条目、旧名条目没删 ⇒ 列表里两条并存
                    toast(R.string.role_key_name_dup, name)
                } else {
                    save(keys.map {
                        if (it.name == entry.name) it.copy(
                            name = name, value = value,
                            thinkingMode = thinkMode, thinkingCustom = thinkCustom,
                        ) else it
                    })
                    // 请求失败锁定表同步（10-03 二改，模型级）：
                    // ① 旧锁定键（旧网址+旧模型）清掉；② 手动模式写新键；auto 清掉新键（等下轮 ⚡ 重探）
                    val oldP = KeyListFile.parseKeyValue(entry.value)
                    val newP = KeyListFile.parseKeyValue(value)
                    scope.launch {
                        withIO {
                            if (oldP != null && !oldP.isDirect) {
                                KeyListFile.saveThinkingParam(tagRuleId, oldP.url, oldP.model, "", "")
                            }
                            if (newP != null && !newP.isDirect) {
                                if (thinkMode != KeyListFile.THINKING_AUTO) {
                                    KeyListFile.saveThinkingParam(tagRuleId, newP.url, newP.model, thinkMode, thinkCustom)
                                } else {
                                    KeyListFile.saveThinkingParam(tagRuleId, newP.url, newP.model, "", "")
                                }
                            }
                        }
                    }
                    // 改的是启用中的密钥 → 同步池里的值
                    // ⚠️ 只写 key_list.json 不改池 ⇒ miyue 留旧值，规则仍轮换旧密钥
                    val oldNorm = KeyListFile.normalizePoolValue(entry.value)
                    val newNorm = KeyListFile.normalizePoolValue(value)
                    if (oldNorm in pool && newNorm.isNotEmpty()) {
                        savePoolList(pool.map { if (it == oldNorm) newNorm else it })
                    }
                }
            }
        )
    }
    // 覆盖确认（照插件「覆盖确认」对话框：保留原 keyCode，换 value）
    overwriteFor?.let { (name, value, thinkPair) ->
        AlertDialog(
            onDismissRequest = { overwriteFor = null },
            title = { Text(stringResource(R.string.role_key_overwrite_title)) },
            text = { Text(stringResource(R.string.role_key_overwrite, name)) },
            confirmButton = {
                TextButton(onClick = {
                    overwriteFor = null
                    val old = keys.firstOrNull { it.name == name }
                    val (thinkMode, thinkCustom) = thinkPair
                    save(keys.map {
                        if (it.name == name) it.copy(
                            value = value,
                            thinkingMode = thinkMode, thinkingCustom = thinkCustom,
                        ) else it
                    })
                    // 锁定表同步（模型级）：旧键清理 + 按新设置写入（IO 走 withIO）
                    val oldP = old?.let { KeyListFile.parseKeyValue(it.value) }
                    val newP = KeyListFile.parseKeyValue(value)
                    scope.launch {
                        withIO {
                            if (oldP != null && !oldP.isDirect) {
                                KeyListFile.saveThinkingParam(tagRuleId, oldP.url, oldP.model, "", "")
                            }
                            if (newP != null && !newP.isDirect && thinkMode != KeyListFile.THINKING_AUTO) {
                                KeyListFile.saveThinkingParam(tagRuleId, newP.url, newP.model, thinkMode, thinkCustom)
                            }
                        }
                    }
                    // 覆盖的是启用中的密钥 → 同步池值；池原本为空 → 这条顶上当第一把
                    val oldNorm = old?.let { KeyListFile.normalizePoolValue(it.value) }.orEmpty()
                    val newNorm = KeyListFile.normalizePoolValue(value)
                    when {
                        oldNorm.isNotEmpty() && oldNorm in pool && newNorm.isNotEmpty() ->
                            savePoolList(pool.map { if (it == oldNorm) newNorm else it })
                        pool.isEmpty() && newNorm.isNotEmpty() -> savePoolList(listOf(newNorm))
                    }
                    if (old != null) toast(R.string.role_key_saved)
                }) { Text(stringResource(R.string.role_key_overwrite_btn)) }
            },
            dismissButton = {
                TextButton(onClick = { overwriteFor = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    // 删除密钥
    deleteFor?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteFor = null },
            title = { Text(stringResource(R.string.role_key_delete_title)) },
            text = { Text(stringResource(R.string.role_key_delete_text, KeyListFile.displayName(entry))) },
            confirmButton = {
                TextButton(onClick = {
                    deleteFor = null
                    // 与批量删除同一条路（照插件 deleteMultipleBooks：删当前密钥后切到剩余第一条）
                    // ⚠️ 旧版只 save(filter)，miyue 仍指向已删的 key，重进页面被兜底切走
                    deleteNames(listOf(entry.name))
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteFor = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    // 接口表单（照插件 showInterfaceFormDialog）：名称 / 地址 / Key + 校验 + 🗑级联删除；
    // 「新建接口」入口已并进拉取弹窗（ensureGroup 自动建档），这里只剩编辑
    ifcFormFor?.let { ifc ->
        InterfaceFormDialog(
            tagRuleId = tagRuleId,
            initial = ifc,
            onDismiss = { ifcFormFor = null },
            onSaved = { ifcFormFor = null; version++ },
            onRefresh = { version++ },
        )
    }
    // 拉取模型：顶部 = 填网址+Key 建组/并入；分组卡 🔍 = 给本组拉。两入口共用，落库走 ensureGroup
    if (showPullModels) {
        ModelPullDialog(
            keys = keys,
            ifaces = ifaces,
            initialIfcName = pullForIfc,
            onDismiss = { showPullModels = false; pullForIfc = null },
            onConfirm = { url, apiKey, groupName, pickedModels, pulledRates ->
                showPullModels = false
                pullForIfc = null
                scope.launch {
                    // groupName 非空 = 用户手填（撞名已被弹窗拦下）；空 = 按网址短名自动提取
                    val ifc = withIO { KeyListFile.ensureGroup(tagRuleId, url, apiKey, groupName) }
                    if (ifc == null) {
                        toast(R.string.role_list_failed)
                    } else {
                        // 名字逐个占位去重（跨组重名只加序号）
                        val used = keys.map { it.name }.toMutableSet()
                        val entries = pickedModels.map { m ->
                            val n = KeyListFile.dedupName(m, used)
                            used.add(n)
                            KeyListFile.KeyEntry(
                                name = n,
                                keyCode = "",
                                value = "${ifc.baseUrl}@@$m@@${ifc.apiKey}",
                            )
                        }
                        // 组内去重：同（站点 + 密钥 + 模型）已在组里 → 不再多出一条
                        val toAdd = entries.filterNot { e ->
                            val p = KeyListFile.parseKeyValue(e.value)
                            p != null && !p.isDirect && KeyListFile.hasModel(keys, ifc, p.model)
                        }
                        val merged = toAdd.fold(keys) { acc, e ->
                            acc + e.copy(keyCode = KeyListFile.nextKeyCode(acc))
                        }
                        withIO {
                            if (toAdd.isNotEmpty()) KeyListFile.saveKeys(tagRuleId, merged)
                            // 拉到的模型登记进分组 models
                            KeyListFile.addModelsToInterface(tagRuleId, ifc.name, pickedModels)
                            // 倍率落盘（10-10）：键=站点|模型，只写本次抓到的（合并写，旧键保留）
                            if (pulledRates.isNotEmpty()) {
                                KeyListFile.saveModelRates(
                                    tagRuleId,
                                    pulledRates.mapKeys { (m, _) -> KeyListFile.rateKey(ifc.baseUrl, m) }
                                )
                            }
                        }
                        toast(R.string.role_key_pull_done, toAdd.size, entries.size - toAdd.size)
                        // 拉取完成后自动展开并滚到该组（10-08：结果落在组里，折叠/屏外都看不见）
                        if (toAdd.isNotEmpty()) expandAndReveal(ifc.name)
                    }
                    version++
                }
            }
        )
    }
}

/**
 * 密钥编辑（条目 ✏️ / 新增共用）：只留「密钥」一栏（整串 网址@@模型@@Key）；
 * 底部 取消 / 确定（10-07：复制/删除上移到密钥标签行右端——作用于密钥本身，思考区展开
 * 时不再被推远）。两处复制不同：列表行 📋 = 模型名，密钥标签行「复制」= 完整密钥串。
 * 名称不再手填（0919 拍板）：新增时自动取密钥串里的模型名（取不到用 keyN 顺延）；
 * 编辑时名称保持不变——列表里显示的名字本来就取自密钥串里的模型名。
 */
@Composable
private fun KeyEditDialog(
    tagRuleId: String,
    initial: KeyListFile.KeyEntry?,
    existing: List<KeyListFile.KeyEntry>,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    // 从提示条「思考设置 ›」进入=true：直达思考区——展开 + 预选「自定义」，
    // 用户落到就是 JSON 输入框（用户 10-07：黄色那个应该直接跳转到自定义 JSON 界面）。
    // 常规 ✏ 编辑进=false，保持老形态（自动/手动 二选一未展开）
    jumpToThinkingCustom: Boolean = false,
    // 分配专用 Switch 写盘成功后回调（10-09）：主页重读 assign_marker 镜像，列表 🚀 即时刷新
    onAssignChanged: () -> Unit = {},
    onConfirm: (String, String, Boolean, String, String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val existingNames = existing.map { it.name }.toSet()
    // 预填光标落末尾（照插件 setSelection）
    val initValue = initial?.value.orEmpty()
    var value by remember { mutableStateOf(TextFieldValue(initValue, TextRange(initValue.length))) }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    // 思考模式（10-03 二改：模型级）——默认自动（⚡ 测试时试探并锁定）；手动展开可选写法
    var thinkingMode by remember {
        // 10-07 编译修复：原 remember { 表达式 } 少了 mutableStateOf —— remember 直接返回
        // String，by 委托要 MutableState（编译器报「Property delegate must have getValue」）。
        // 补 mutableStateOf 并与下面 thinkingExpanded 同写法
        mutableStateOf(
            // jumpToThinkingCustom：从提示条「思考设置 ›」进 → 直接落到「自定义」（JSON 输入框）
            if (jumpToThinkingCustom) KeyListFile.THINKING_CUSTOM
            else initial?.thinkingMode ?: KeyListFile.THINKING_AUTO
        )
    }
    // 三类结构（10-08 用户定稿）：自动 / 关闭思考（现成写法）/ 自定义 JSON，三者同级。
    // thinkingExpanded 语义收窄为「第二类选中」；第三类由 thinkingMode==CUSTOM 独立表达
    // （不再藏在手动抽屉底）。存储值不变：AUTO / 7 种现成写法 / CUSTOM。
    var thinkingExpanded by remember {
        mutableStateOf(
            initial?.thinkingMode?.let { it != KeyListFile.THINKING_AUTO && it != KeyListFile.THINKING_CUSTOM } ?: false
        )
    }
    var customText by remember { mutableStateOf(initial?.thinkingCustom.orEmpty()) }
    fun toast(resId: Int) {
        android.widget.Toast.makeText(
            context, context.getString(resId), android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (initial == null) R.string.role_key_add else R.string.role_key_edit))
        },
        text = {
            // 内容整体可滚动（10-05 修溢出：标题+密钥框+思考模式 8 选项+按钮总高超屏幕，
            // M3 AlertDialog 把内容槽按剩余高度压缩 → 列表末尾几行被挤扁，只剩孤立 RadioButton
            // 圆环顶在上一个选项下面（用户实机截图「最后几个挤一块」）。与下方 InterfaceFormDialog
            // 同款：heightIn 限高 + verticalScroll，超出部分改滚动，不再压缩子项）
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                // 「密钥」一栏（整串 网址@@模型名@@API key；裸 Key 可存但禁用启用，见 togglePool 拦截）。
                // 10-07 用户令：复制/删除挪到密钥标签行——它们作用于密钥本身，原先在底部按钮区，
                // 思考区一展开就被推得老远（新用的人也不知道底部按钮是给谁的）
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        // 12→14（10-07 装机反馈甲案）：原 bodySmall 比同行右侧的动作键（复制/删除
                        // labelLarge 14sp）小两档，夹在中间显小；升到 14 后本行「标签/动作」同档，
                        // 靠颜色（灰字 vs 绿/红）分语义，也与「思考模式」那一行的 14sp 对齐。
                        stringResource(R.string.role_key_value),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        enabled = value.text.isNotBlank(),
                        // 紧凑文字键：贴标签行，高度对齐 bodySmall 行
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        onClick = {
                            clipboard.setText(AnnotatedString(value.text.trim()))
                            toast(R.string.copied)
                        }
                    ) { Text(stringResource(R.string.copy)) }
                    if (onDelete != null) {
                        TextButton(
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            onClick = onDelete
                        ) {
                            Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                // 密钥串很长，允许多行（原 singleLine 会把中段吞掉）
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    // 占位提示必须自己钉字号（10-07 装机反馈根治）：OutlinedTextField 的
                    // textStyle 只管输入文字，placeholder 里的 Text 不吃它、回落弹窗默认
                    // 16sp（换声面板搜索框 09-13 已记录过同坑）——本框实测占位比输入字大两档。
                    placeholder = {
                        Text(
                            stringResource(R.string.role_key_value_hint),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    singleLine = false,
                    minLines = 2,
                    // maxLines 4→10（10-05 用户：按内容适配高度，长密钥串要显示全不内滚）
                    maxLines = 10,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
                // 裸 Key 补全引导：值是裸 Key 时显示（与未分组组头同一句提示），补全即消失——
                // 占位提示只在空框时可见，编辑已有裸 Key 时这里是唯一能引导补全的地方
                val parsed = KeyListFile.parseKeyValue(value.text.trim())
                if (parsed != null && parsed.isDirect) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.role_key_complete_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(10.dp))

                // ===== 分配专用 Switch（10-09）：权威开关（长按菜单是快捷通道，两处状态同步）=====
                // 编辑已有条目才显示（新增时密钥还没落盘，无 normalizedValue 可挂标记）
                if (initial != null) {
                    // 只在进入弹窗时读一次盘（开/关 Switch 即时写盘 + 更新本地态）
                    var assignOn by remember(initial.name) {
                        mutableStateOf(
                            KeyListFile.hasAssignMarker(
                                tagRuleId,
                                KeyListFile.normalizePoolValue(initial.value)
                            )
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "🚀 分配专用",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "姓名/别名分析优先用这把（建议选快的模型）",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = assignOn,
                            onCheckedChange = { on ->
                                val norm = KeyListFile.normalizePoolValue(initial.value)
                                scope.launch {
                                    val ok = withIO {
                                        if (on) KeyListFile.setAssignMarker(tagRuleId, norm, manual = true)
                                        else KeyListFile.clearAssignMarker(tagRuleId, norm)
                                    }
                                    if (ok) {
                                        assignOn = on
                                        onAssignChanged()
                                    } else {
                                        android.widget.Toast.makeText(
                                            context, context.getString(R.string.role_list_failed),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )
                    }
                }

                // ===== 思考模式（10-05 方案一：折叠一行，点击展开——原整段平铺把按钮行
                // 推得离密钥栏太远，用户不知道按钮是给谁的；功能零搬家）=====
                // 10-07：从提示条「自定义思考 ›」进来时直接展开——「直达」之前只预选了写法，
                // 折叠区还关着，用户得多点一下才见 JSON 框
                var thinkingOpen by remember { mutableStateOf(jumpToThinkingCustom) }
                // 10-08 用户定稿：直达落折叠态（进来是「自定义 JSON 填写界面」）。
                // 关闭思考六种写法平铺（原「换一种写法 ▾」折叠键已删——折叠后只显一条，
                // 用户不知道还有别的写法；实测六行并不算长，换写法不再要多点一下）
                // 锁定状态（读取该模型在 thinking_params.json 的锁定值；键 = 网址+模型）
                val lockedMode = remember(initial) {
                    val p = initial?.let { KeyListFile.parseKeyValue(it.value) }
                    if (p == null || p.isDirect) null
                    else KeyListFile.readThinkingParams(tagRuleId)[
                        KeyListFile.thinkingLockKey(p.url, p.model)
                    ]?.first
                }
                val multiLabel = stringResource(R.string.role_key_thinking_opt_multi)
                val typeLabel = stringResource(R.string.role_key_thinking_opt_type)
                val tmodeLabel = stringResource(R.string.role_key_thinking_opt_tmode)
                val dthinkLabel = stringResource(R.string.role_key_thinking_opt_dthink)
                val ncotLabel = stringResource(R.string.role_key_thinking_opt_ncot)
                val lowLabel = stringResource(R.string.role_key_thinking_opt_low)
                val noneLabel = stringResource(R.string.role_key_thinking_opt_none)
                val customOptionLabel = stringResource(R.string.role_key_thinking_opt_custom)
                fun modeLabel(m: String): String = when (m) {
                    KeyListFile.THINKING_MULTI -> multiLabel
                    KeyListFile.THINKING_TYPE -> typeLabel
                    KeyListFile.THINKING_TMODE -> tmodeLabel
                    KeyListFile.THINKING_DTHINK -> dthinkLabel
                    KeyListFile.THINKING_NCOT -> ncotLabel
                    KeyListFile.THINKING_LOW -> lowLabel
                    KeyListFile.THINKING_NONE -> noneLabel
                    KeyListFile.THINKING_CUSTOM -> customOptionLabel
                    else -> m
                }
                // 折叠摘要行：模式 + 锁定写法名（与测试结果的「multi」同词）+ 展开箭头。
                // 三类摘要文案：自动（锁定 x）/关闭思考（写法名）/自定义 JSON
                val isCustomSelected = thinkingMode == KeyListFile.THINKING_CUSTOM
                val isLowSelected = thinkingExpanded && thinkingMode == KeyListFile.THINKING_LOW
                // 摘要行取短名（截掉「（…）」括注）：完整文案如「全部关闭字段一起发（老口径，
                // 宽容平台可用）」进摘要会变双括号嵌套还折行（10-08 实机截图）；括注留给平铺列表
                fun shortLabel(m: String): String = modeLabel(m).substringBefore("（")
                val summaryText = when {
                    !thinkingExpanded && !isCustomSelected ->
                        lockedMode?.let { "自动（锁定 $it）" } ?: "自动（未测试）"
                    isCustomSelected -> "自定义 JSON"
                    isLowSelected -> shortLabel(KeyListFile.THINKING_LOW)
                    else -> "关闭思考（${shortLabel(thinkingMode)}）"
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { thinkingOpen = !thinkingOpen }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "思考模式",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        summaryText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (lockedMode != null || thinkingExpanded || isCustomSelected) TEST_PASS_COLOR
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (thinkingOpen) "▾" else "▸",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                if (thinkingOpen) {
                Text(
                    stringResource(R.string.role_key_thinking_desc),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                // ===== 三类同级（10-08 用户定稿）：自动 / 关闭思考（现成写法）/ 自定义 JSON =====
                // 原「自动 vs 手动抽屉（8 项塞抽屉里）」两层结构层级乱——自定义跟现成写法
                // 不是一类东西；摊平成三个 RadioButton，第二类点开才铺 7 项现成写法。
                // 第一类：自动
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !thinkingExpanded && thinkingMode != KeyListFile.THINKING_CUSTOM,
                        onClick = { thinkingExpanded = false; thinkingMode = KeyListFile.THINKING_AUTO }
                    )
                    Text(
                        stringResource(R.string.role_key_thinking_auto),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (lockedMode != null && !thinkingExpanded && thinkingMode != KeyListFile.THINKING_CUSTOM) {
                    Text(
                        // 写法名+中文释义一起给（10-05 用户：测试结果说锁定「multi」，
                        // 进设置只看到「全部关闭字段一起发」对不上号——名字和释义必须同现）
                        stringResource(R.string.role_key_thinking_locked, lockedMode) + "，" + modeLabel(lockedMode),
                        style = MaterialTheme.typography.labelSmall,
                        color = TEST_PASS_COLOR,
                        modifier = Modifier.padding(start = 48.dp, top = 2.dp)
                    )
                } else if (initial != null && !thinkingExpanded && thinkingMode != KeyListFile.THINKING_CUSTOM) {
                    Text(
                        stringResource(R.string.role_key_thinking_not_tested),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 48.dp, top = 2.dp)
                    )
                }
                // 第二类：关闭思考（现成写法）——收 7 项，点开才铺；折叠态显当前写法名
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        // LOW 提为独立选项后 thinkingExpanded 不再专属第二类（LOW 也置真）——
                        // 选中须排除 LOW，否则 LOW 选中时这里跟着亮、双选
                        selected = thinkingExpanded && thinkingMode != KeyListFile.THINKING_LOW,
                        onClick = {
                            thinkingExpanded = true
                            if (thinkingMode == KeyListFile.THINKING_AUTO ||
                                thinkingMode == KeyListFile.THINKING_CUSTOM ||
                                thinkingMode == KeyListFile.THINKING_LOW
                            ) thinkingMode = KeyListFile.THINKING_MULTI
                        }
                    )
                    Text(
                        stringResource(R.string.role_key_thinking_off_option),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                // 六种关闭写法平铺（10-08 用户令：删「换一种写法」折叠键——
                // 折叠后只显一条，用户不知道还有别的写法；六行 RadioButton 是最短路径）。
                // LOW 不在列：语义不同（只降档不关闭），提为下方独立一级选项
                if (thinkingExpanded) {
                    val options = listOf(
                        KeyListFile.THINKING_MULTI, KeyListFile.THINKING_TYPE,
                        KeyListFile.THINKING_TMODE, KeyListFile.THINKING_DTHINK,
                        KeyListFile.THINKING_NCOT, KeyListFile.THINKING_NONE,
                    )
                    options.forEach { opt ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { thinkingMode = opt }
                                // 上下内距 2→0（10-07 装机反馈）：撤掉后剩 M3 最小触摸高 48dp
                                .padding(start = 48.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = thinkingMode == opt, onClick = { thinkingMode = opt })
                            Text(modeLabel(opt), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                // 第四个一级选项：reasoning_effort = low——只降档不关闭，不属「关闭思考」，
                // 三类定稿时先收进第二类靠文案区分；平铺六项后混在里头更看不出来，独立成行。
                // 与其它三个一级行同字号 bodyMedium（原沿用二级 bodySmall 是不一致）、上距 4dp 归组
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = thinkingExpanded && thinkingMode == KeyListFile.THINKING_LOW,
                        onClick = {
                            thinkingExpanded = true
                            thinkingMode = KeyListFile.THINKING_LOW
                        }
                    )
                    Text(modeLabel(KeyListFile.THINKING_LOW), style = MaterialTheme.typography.bodyMedium)
                }
                // 第三类：自定义 JSON——选中即出框（直达「自定义思考 ›」落这里）
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = thinkingMode == KeyListFile.THINKING_CUSTOM,
                        onClick = {
                            thinkingMode = KeyListFile.THINKING_CUSTOM
                            thinkingExpanded = false
                        }
                    )
                    Text(
                        stringResource(R.string.role_key_thinking_custom_option),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (thinkingMode == KeyListFile.THINKING_CUSTOM) {
                    OutlinedTextField(
                        value = customText, onValueChange = { customText = it },
                        singleLine = false, minLines = 2, maxLines = 4,
                        textStyle = MaterialTheme.typography.bodySmall,
                        placeholder = {
                            Text("{\"key\": \"value\"}", style = MaterialTheme.typography.bodySmall)
                        },
                        modifier = Modifier.fillMaxWidth().padding(start = 48.dp)
                    )
                }
                } // thinkingOpen 展开区收尾
            }
        },
        confirmButton = {
            // 10-07：复制/删除已上移到密钥标签行（作用于密钥本身），底部只留 取消/确定——
            // 思考区展开再长，底部按钮语义也不混
            TextButton(
                enabled = value.text.isNotBlank(),
                onClick = {
                    val raw = value.text.trim()
                    // 三类语义（10-08）：第一类自动=collapsed 且非 custom；第二类现成写法=
                    // expanded；第三类自定义=custom（不再依赖 expanded）
                    val finalThink = when {
                        thinkingMode == KeyListFile.THINKING_CUSTOM -> KeyListFile.THINKING_CUSTOM
                        thinkingExpanded -> thinkingMode
                        else -> KeyListFile.THINKING_AUTO
                    }
                    // custom 校验：不可解析直接拦（toast 在 Dialog 内）
                    if (finalThink == KeyListFile.THINKING_CUSTOM &&
                        KeyListFile.thinkingBodyFields(KeyListFile.THINKING_CUSTOM, customText) == null
                    ) {
                        toast(R.string.role_key_thinking_custom_bad); return@TextButton
                    }
                    val finalCustom = if (finalThink == KeyListFile.THINKING_CUSTOM) customText.trim() else ""
                    // 编辑：名称保持不变（没有名称输入框了）
                    if (initial != null) {
                        onConfirm(initial.name, raw, false, finalThink, finalCustom)
                        return@TextButton
                    }
                    // 新增：名称自动生成（照插件 defaultName）——从密钥串里抽模型名，抽不出用 keyN
                    val wanted = KeyListFile.parseKeyValue(raw)?.let { p ->
                        if (!p.isDirect && p.model.isNotEmpty()) p.model else null
                    } ?: "key" + (existing.size + 1)
                    // 撞名判定：只有同一分组（同网址 + 同密钥）才算真重复 → 覆盖确认；
                    // 跨组撞名加序号另存
                    val clash = existing.firstOrNull { it.name == wanted }
                    val overwrite = clash != null && KeyListFile.sameGroupValue(clash.value, raw)
                    val finalName =
                        if (clash != null && !overwrite) KeyListFile.dedupName(wanted, existingNames)
                        else wanted
                    onConfirm(finalName, raw, overwrite, finalThink, finalCustom)
                }
            ) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/** 接口表单（照插件 showInterfaceFormDialog：名称/地址/Key + 校验 + 🗑级联删除） */
@Composable
private fun InterfaceFormDialog(
    tagRuleId: String,
    initial: KeyListFile.ApiInterface?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    // 只刷新外面列表、不关本弹窗（手动加模型用）
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 新建时名称框随网址自动填短名；人改过的不覆盖；预填值光标落末尾
    var name by remember {
        val init = initial?.name.orEmpty()
        mutableStateOf(TextFieldValue(init, TextRange(init.length)))
    }
    // 名称被手改过就不再被网址覆盖；编辑已有分组时视为已改
    var nameTouched by remember { mutableStateOf(initial != null) }
    // 地址框用 TextFieldValue：失焦补协议头、预填都要把光标挪到末尾
    var url by remember {
        val init = initial?.baseUrl.orEmpty()
        mutableStateOf(TextFieldValue(init, TextRange(init.length)))
    }
    var key by remember { mutableStateOf(initial?.apiKey.orEmpty()) }
    // 思考模式（10-03 二改：模型级为主，本弹窗只做「统一设为下方全部模型」的批量入口）：
    // 默认折叠不参与（thinkingTouched=false 时保存不动任何模型的思考设置）
    var thinkingExpanded by remember { mutableStateOf(false) }
    var thinkingMode by remember { mutableStateOf(KeyListFile.THINKING_MULTI) }
    var thinkingTouched by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("") }
    // 手动加模型：点确定直接落库（拉取弹窗里那个只进候选列表）
    var addModelVisible by remember { mutableStateOf(false) }
    var addModelText by remember { mutableStateOf("") }
    fun toast(resId: Int, vararg args: Any) {
        // 无参不过 format（见 lib-common ToastUtils.getStringSafe）：否则串里留 %1$s 就崩
        val msg = if (args.isEmpty()) context.getString(resId) else context.getString(resId, *args)
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    // 保存：名称留空按网址短名兜底；改网址/密钥时同步改写组内条目的 value
    // ⚠️ 只改 模型接口中心.json 不改条目 ⇒ 旧条目仍指旧地址，整组掉进「未分组」
    fun doSave() {
        val u = KeyListFile.normalizeBaseUrl(url.text.trim())
        val k = key.trim()
        val n = name.text.trim().ifEmpty {
            runCatching { KeyListFile.shortName(u) }.getOrDefault("")
        }
        if (n.isEmpty()) { toast(R.string.role_key_ifc_name_empty); return }
        if (!u.startsWith("http")) { toast(R.string.role_key_ifc_url_bad); return }
        if (k.isEmpty()) { toast(R.string.role_key_ifc_key_empty); return }
        scope.launch {
            val ifaces = withIO { KeyListFile.readInterfaces(tagRuleId) }
            // 不许改成别的接口已有的名字
            val dup = ifaces.any { it.name == n && (initial == null || it.name != initial.name) }
            if (dup) { toast(R.string.role_key_ifc_name_dup); return@launch }
            // 改成与另一个接口完全同组（同网址 + 同密钥）⇒ 拦住报错，不静默合并
            val collide = ifaces.firstOrNull {
                it.name != (initial?.name ?: "") &&
                    KeyListFile.sameApiSite(it.baseUrl, u) && it.apiKey.trim() == k
            }
            if (collide != null) { toast(R.string.role_key_ifc_group_dup, collide.name); return@launch }
            // 思考模式（10-03 二改，模型级为主 + 分组批量）：本弹窗的「统一设为」把所选写法
            // 批量写入下方全部模型（KeyEntry + 各自锁定表）；仅当用户动过此项才应用（thinkingTouched）
            val thinkMode = if (thinkingExpanded) thinkingMode else KeyListFile.THINKING_AUTO
            if (thinkingTouched && thinkMode == KeyListFile.THINKING_CUSTOM &&
                KeyListFile.thinkingBodyFields(KeyListFile.THINKING_CUSTOM, customText) == null
            ) {
                toast(R.string.role_key_thinking_custom_bad); return@launch
            }
            val updated = if (initial == null) {
                ifaces + KeyListFile.ApiInterface(n, u, k, emptyList())
            } else {
                ifaces.map { if (it.name == initial.name) KeyListFile.ApiInterface(n, u, k, it.models) else it }
            }
            withIO { KeyListFile.saveInterfaces(tagRuleId, updated) }
            if (initial != null) {
                val oldUrl = initial.baseUrl
                val oldKey = initial.apiKey
                val entryList = withIO { KeyListFile.readKeys(tagRuleId) }
                // 批量应用思考模式（只改本组条目；用户没动这一项则原样保留）
                var rewritten = entryList
                if (thinkingTouched) {
                    val targets = entryList.filter { e ->
                        val p = KeyListFile.parseKeyValue(e.value)
                        p != null && !p.isDirect && p.key == oldKey && KeyListFile.sameApiSite(p.url, oldUrl)
                    }
                    val updatedTargets = withIO {
                        KeyListFile.applyThinkingToGroup(tagRuleId, targets, thinkMode, customText.trim())
                    }
                    val byName = updatedTargets.associateBy { it.name }
                    rewritten = entryList.map { e -> byName[e.name] ?: e }
                }
                var changed = thinkingTouched
                val finalList = rewritten.map { e ->
                    val p = KeyListFile.parseKeyValue(e.value)
                    if (p != null && !p.isDirect && p.key == oldKey &&
                        KeyListFile.sameApiSite(p.url, oldUrl)
                    ) {
                        changed = true
                        e.copy(value = "$u@@${p.model}@@$k")
                    } else e
                }
                if (changed) withIO { KeyListFile.saveKeys(tagRuleId, finalList) }
                // 启用池同步（10-04）：本组条目在池里用旧（网址+密钥）串 ⇒ 换成新串。
                // ⚠️ 旧版只改接口与条目、漏同步池 ⇒ 界面显示新 Key，朗读仍按池里的旧 Key 轮换
                //（换 Key/换地址等于没换；旧 Key 失效时朗读直接失败）
                val normMap = mutableMapOf<String, String>()
                rewritten.forEach { e ->
                    val p = KeyListFile.parseKeyValue(e.value) ?: return@forEach
                    if (!p.isDirect && p.key == oldKey && KeyListFile.sameApiSite(p.url, oldUrl)) {
                        val oldNorm = KeyListFile.normalizePoolValue(e.value)
                        val newNorm = KeyListFile.normalizePoolValue("$u@@${p.model}@@$k")
                        if (oldNorm.isNotEmpty() && newNorm.isNotEmpty() && oldNorm != newNorm) {
                            normMap[oldNorm] = newNorm
                        }
                    }
                }
                if (normMap.isNotEmpty()) {
                    val curPool = withIO { KeyListFile.readPool(tagRuleId) }
                    val nextPool = curPool.map { normMap[it] ?: it }
                    if (nextPool != curPool) withIO { KeyListFile.savePool(tagRuleId, nextPool) }
                }
            }
            toast(R.string.role_key_saved)
            onSaved()
        }
    }

    // 删除该接口和它下面的密钥条目
    fun doDelete() {
        val cur = initial ?: return
        scope.launch {
            val (kept, removed) = withIO {
                KeyListFile.deleteInterfaceCascade(tagRuleId, cur, KeyListFile.readKeys(tagRuleId))
            }
            withIO { KeyListFile.saveKeys(tagRuleId, kept) }
            toast(R.string.role_key_ifc_deleted, removed)
            onSaved()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            // 标题行右上角 = 手动加模型；只在编辑已有分组时给（新组还没落盘，条目无处归属）
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(
                        if (initial == null) R.string.role_key_interface_new else R.string.role_key_interface_edit
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (initial != null) {
                    TextButton(onClick = { addModelText = ""; addModelVisible = true }) {
                        Text(
                            "＋ " + stringResource(R.string.role_key_manual_model),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        },
        text = {
            // 加了思考模式区后内容变长（键盘弹出时易溢出）——整块可滚动，照备份恢复弹窗口径
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.role_key_ifc_name),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameTouched = true },
                    singleLine = true, textStyle = MaterialTheme.typography.bodyMedium,
                    // 清空后灰字摆出将用的短名（替代原来标签里那句括号说明）
                    // 占位钉字号：placeholder 不吃 textStyle，不钉会回落 16sp（比输入字大）
                    placeholder = {
                        Text(
                            runCatching { KeyListFile.shortName(url.text) }.getOrDefault(""),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.role_key_ifc_url),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 地址可能很长，允许多行
                OutlinedTextField(
                    value = url,
                    onValueChange = { v ->
                        url = v
                        // 改地址时同步刷新名称（取短名）；人改过的不覆盖
                        if (!nameTouched || name.text.isBlank()) {
                            val s = KeyListFile.shortName(v.text)
                            name = TextFieldValue(s, TextRange(s.length))
                        }
                    },
                    singleLine = false, minLines = 1, maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                        // 失焦补协议头并回写（与拉取弹窗同口径）
                        .onFocusChanged { st ->
                            if (!st.isFocused) {
                                val fixed = KeyListFile.withScheme(url.text)
                                if (fixed != url.text) {
                                    url = TextFieldValue(fixed, TextRange(fixed.length))
                                }
                            }
                        },
                )
                // 未填显示填写提示、填了显示实际请求地址（两行互补）
                val previewBase = if (url.text.isBlank()) "" else
                    runCatching { KeyListFile.openAiBaseUrl(url.text.trim()) }.getOrDefault("")
                if (previewBase.isEmpty()) {
                    Text(
                        stringResource(R.string.role_key_ifc_url_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                } else {
                    Text(
                        stringResource(R.string.role_key_will_request, "$previewBase/models"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.role_key_ifc_key),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Key 可能很长，允许多行
                OutlinedTextField(
                    value = key, onValueChange = { key = it },
                    singleLine = false, minLines = 1, maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))

                // ===== 思考模式（10-03 二改）：模型级为主，这里只做下方全部模型的批量「统一设为」=====
                Text(
                    stringResource(R.string.role_key_thinking_group_title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.role_key_thinking_group_desc),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                // ===== 思考模式（10-03 二改：模型级为主，本处只做批量「统一设为」）=====
                // 默认不参与：保存不动任何模型的思考设置；点「统一设为」才落到下方全部模型
                val multiLabel = stringResource(R.string.role_key_thinking_opt_multi)
                val typeLabel = stringResource(R.string.role_key_thinking_opt_type)
                val tmodeLabel = stringResource(R.string.role_key_thinking_opt_tmode)
                val dthinkLabel = stringResource(R.string.role_key_thinking_opt_dthink)
                val ncotLabel = stringResource(R.string.role_key_thinking_opt_ncot)
                val lowLabel = stringResource(R.string.role_key_thinking_opt_low)
                val noneLabel = stringResource(R.string.role_key_thinking_opt_none)
                val customOptionLabel = stringResource(R.string.role_key_thinking_opt_custom)
                fun modeLabel(m: String): String = when (m) {
                    KeyListFile.THINKING_MULTI -> multiLabel
                    KeyListFile.THINKING_TYPE -> typeLabel
                    KeyListFile.THINKING_TMODE -> tmodeLabel
                    KeyListFile.THINKING_DTHINK -> dthinkLabel
                    KeyListFile.THINKING_NCOT -> ncotLabel
                    KeyListFile.THINKING_LOW -> lowLabel
                    KeyListFile.THINKING_NONE -> noneLabel
                    KeyListFile.THINKING_CUSTOM -> customOptionLabel
                    else -> m
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !thinkingExpanded,
                        onClick = { thinkingExpanded = false; thinkingTouched = false }
                    )
                    Text(
                        stringResource(R.string.role_key_thinking_group_keep),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = thinkingExpanded,
                        onClick = { thinkingExpanded = true; thinkingTouched = true }
                    )
                    Text(
                        stringResource(R.string.role_key_thinking_group_set),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (thinkingExpanded) {
                    Text(
                        stringResource(R.string.role_key_thinking_group_warn),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 48.dp, top = 2.dp, bottom = 2.dp)
                    )
                    // 10-07 用户令：同编辑弹窗——按用途归组。auto 单独一行（它自己就是
                    // 「自动试探并锁定」），其余写法收在「关掉思考」小节下；8 项不删
                    // （auto 试探序列的弹药 + 黄态手动出路）
                    // 局部函数体里调 Row/stringResource（都是 @Composable）→ 必须标 @Composable
                    // （10-07 编译修复：原缺注解，报「Functions which invoke @Composable
                    // functions must be marked with the @Composable annotation」）
                    @Composable
                    fun pickRow(opt: String) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { thinkingMode = opt; thinkingTouched = true }
                                // 上下内距 2→0（10-07 装机反馈）：与编辑弹窗同步收敛到
                                // M3 最小触摸高 48dp——两处是同一份 8 项写法列表，行距须一致
                                .padding(start = 48.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = thinkingMode == opt,
                                onClick = { thinkingMode = opt; thinkingTouched = true }
                            )
                            Text(
                                if (opt == KeyListFile.THINKING_AUTO) stringResource(R.string.role_key_thinking_auto)
                                else modeLabel(opt),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    pickRow(KeyListFile.THINKING_AUTO)
                    Text(
                        // 11→12 + 中粗（10-07 装机反馈）：原 labelSmall 11sp 比它自己的选项
                        // （bodySmall 12sp）还小，小标题不该小于条目；labelMedium 正好 12sp、
                        // M3 默认 Medium 字重，一档同时补上尺寸与层级感。
                        stringResource(R.string.role_key_thinking_goal_off),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 48.dp, top = 6.dp, bottom = 2.dp)
                    )
                    // 10-08 与编辑弹窗三类结构对齐：CUSTOM 从现成写法列表拆出、独立成类
                    // （7 项现成写法 + 自定义不是一类东西）
                    listOf(
                        KeyListFile.THINKING_MULTI, KeyListFile.THINKING_TYPE,
                        KeyListFile.THINKING_TMODE, KeyListFile.THINKING_DTHINK,
                        KeyListFile.THINKING_NCOT, KeyListFile.THINKING_NONE,
                        KeyListFile.THINKING_LOW,
                    ).forEach { pickRow(it) }
                    pickRow(KeyListFile.THINKING_CUSTOM)
                    if (thinkingMode == KeyListFile.THINKING_CUSTOM) {
                        OutlinedTextField(
                            value = customText, onValueChange = { customText = it },
                            singleLine = false, minLines = 2, maxLines = 4,
                            textStyle = MaterialTheme.typography.bodySmall,
                            placeholder = {
                                Text("{\"key\": \"value\"}", style = MaterialTheme.typography.bodySmall)
                            },
                            modifier = Modifier.fillMaxWidth().padding(start = 48.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            // 删除靠左、取消/保存靠右（同一行）；删除字数多，窄屏靠收缩+省略号保证不换行
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (initial != null) {
                    TextButton(
                        onClick = { doDelete() },
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            stringResource(R.string.role_key_ifc_delete),
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Spacer(Modifier)
                }
                Row {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = { doSave() }) { Text(stringResource(R.string.confirm)) }
                }
            }
        }
    )
    // 手动加模型：填名字点确定即落库（建条目 + 登记 models）；加完不关编辑弹窗，只刷新外层列表
    if (addModelVisible) {
        val target = initial
        AlertDialog(
            onDismissRequest = { addModelVisible = false },
            title = { Text(stringResource(R.string.role_key_manual_model)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = addModelText, onValueChange = { addModelText = it },
                        label = { Text(stringResource(R.string.role_key_model_input_hint)) },
                        singleLine = true, textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (target != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.role_key_add_model_to, target.name),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = addModelText.trim().isNotEmpty() && target != null,
                    onClick = {
                        // 用局部非空变量接住再进协程（嵌套 lambda 里智能转换不稳）
                        val t = target ?: return@TextButton
                        val m = addModelText.trim()
                        scope.launch {
                            val r = withIO { KeyListFile.addManualModel(tagRuleId, t, m) }
                            addModelVisible = false
                            if (r.first) {
                                toast(R.string.role_key_saved)
                                onRefresh()
                            } else {
                                toast(
                                    if (r.second == "exist") R.string.role_key_model_exist
                                    else R.string.role_list_failed
                                )
                            }
                        }
                    }
                ) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { addModelVisible = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

/**
 * 拉取模型：两个入口共用——顶部 = 新建（填网址 + Key，确认时 ensureGroup 找组 / 建组）；
 * 分组卡 🔍 = 分组模式（进来即按该组网址+密钥自动拉）。手动添加模型在标题行右上角。
 * 「已在组内」只按目标分组算（同站点 + 同密钥 + 同模型），别的接口拉过同一模型照样能存。
 */
@Composable
private fun ModelPullDialog(
    keys: List<KeyListFile.KeyEntry>,
    ifaces: List<KeyListFile.ApiInterface>,
    initialIfcName: String? = null,
    onDismiss: () -> Unit,
    // onConfirm(网址, 密钥, 分组名, 选中的模型, 倍率表)；分组名留空 = 按网址短名自动提取
    // rates：本次拉取抓回的 模型→倍率展示串（可能为空=该站无倍率数据）
    onConfirm: (String, String, String, List<String>, Map<String, String>) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 分组模式取回接口本体；组已被删则退回新建模式
    val initialIfc = remember(ifaces, initialIfcName) {
        initialIfcName?.let { n -> ifaces.firstOrNull { it.name == n } }
    }
    val forGroup = initialIfc != null
    // 分组名框只有新建模式用（分组模式整框隐藏）
    var nameText by remember { mutableStateOf(TextFieldValue("")) }
    var urlText by remember { mutableStateOf(TextFieldValue(initialIfc?.baseUrl.orEmpty())) }
    var keyText by remember { mutableStateOf(TextFieldValue(initialIfc?.apiKey.orEmpty())) }
    var models by remember { mutableStateOf<List<String>>(emptyList()) }
    // 拉取时顺手抓回的倍率（10-10）：模型→展示串；空=该站无倍率数据（不显示）
    var rates by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("") }
    var manualVisible by remember { mutableStateOf(false) }
    // 拉取成功后把表单折成一行摘要，把高度让给模型列表；点 ✏ 重新展开可改
    var formCollapsed by remember { mutableStateOf(false) }

    val url = urlText.text.trim()
    val key = keyText.text.trim()
    // 目标分组：分组模式=点进来的那个；新建模式=网址+密钥命中已有分组就并入
    val targetIfc = initialIfc ?: ifaces.firstOrNull {
        KeyListFile.sameApiSite(it.baseUrl, url) && it.apiKey.trim() == key
    }
    val ready = forGroup || (url.isNotEmpty() && key.isNotEmpty())
    // 分组名留空 ⇒ 网址短名；手填撞名不当场改字，预览行变红 + 确认时 Toast 拦下
    val autoName = remember(url) { runCatching { KeyListFile.shortName(url) }.getOrDefault("") }
    val typedName = nameText.text.trim()
    val finalName = typedName.ifEmpty { autoName }
    // ⚠️ 只在要新建组时判重名（命中已有分组时这个框用不上，别误拦）
    val nameTaken = targetIfc == null && typedName.isNotEmpty() && ifaces.any { it.name == typedName }

    fun fetch() {
        if (!ready) return
        val u = targetIfc?.baseUrl ?: url
        val k = targetIfc?.apiKey ?: key
        scope.launch {
            loading = true; error = ""
            val r = withIO { KeyListFile.fetchModelsWithRates(u, k) }
            loading = false
            if (r.first == null) error = r.third
            else {
                models = r.first ?: emptyList()
                rates = r.second
                selected = emptySet() // 默认不勾选
                formCollapsed = true
            }
        }
    }
    // 分组模式进来就拉
    LaunchedEffect(Unit) { if (forGroup) fetch() }
    val visibleModels = models.filter { filter.isBlank() || it.contains(filter, true) }
    // 「已在组内」只按目标分组算（同站点 + 同密钥 + 同模型），别的分组拉过不算
    fun inGroup(m: String) = targetIfc != null && KeyListFile.hasModel(keys, targetIfc, m)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                // 标题行右上角 = 手动添加模型；操作行只留「拉取」。
                // 标题只留字段名（填写提示挪到各自字段标题后，标题后不带括号注记）
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.role_key_fetch),
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { manualVisible = true }, enabled = ready) {
                        Text(
                            "＋ " + stringResource(R.string.role_key_manual_model),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                // 表单只在「没拉到模型」时显示（拉取成功后表单直接收起，
                // 不折摘要行——分组名/地址/尾号那行删掉，要改就取消重开；分组模式无表单）
                if (!forGroup && !formCollapsed) {
                    // 新建模式：字段顺序 分组名 → 接口地址 → API Key
                    // 提示跟在字段标题后（不放标题下、不放弹窗底部）
                    Text(
                        stringResource(R.string.role_key_group_name_label) +
                            stringResource(R.string.role_key_group_name_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = nameText, onValueChange = { nameText = it },
                        singleLine = true,
                        // 空框时把将用的短名当占位显示；网址没填就空着，不写解释
                        // （占位钉字号：placeholder 不吃 textStyle，不钉就回落 16sp）
                        placeholder = {
                            Text(autoName, style = MaterialTheme.typography.bodyMedium)
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.role_key_ifc_url) +
                            stringResource(R.string.role_key_url_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // 地址可能很长，允许多行；框内不放示例（使用提示在标题下那行）
                    OutlinedTextField(
                        value = urlText, onValueChange = { urlText = it },
                        singleLine = false, minLines = 1, maxLines = 3,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                            // 失焦补协议头并回写（与编辑弹窗同口径）
                            .onFocusChanged { st ->
                                if (!st.isFocused) {
                                    val fixed = KeyListFile.withScheme(urlText.text)
                                    if (fixed != urlText.text) {
                                        urlText = TextFieldValue(fixed, TextRange(fixed.length))
                                    }
                                }
                            },
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.role_key_ifc_key),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = keyText, onValueChange = { keyText = it },
                        singleLine = false, minLines = 1, maxLines = 3,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // 底部「请求 /models / 并入分组」预览行已删（没事儿别占地方）；
                    // 手填分组名撞车仍由确认键 Toast 拦下
                }
                Spacer(Modifier.height(4.dp))
                // 「拉取」按钮只在没拉到模型时显示（标题已有「拉取模型」四字，
                // 列表出来了按钮就多余）；loading 转圈也在这一行——分组模式首次拉取 / 失败重试都覆盖
                if (models.isEmpty()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { fetch() }, enabled = !loading && ready) {
                            Text(stringResource(if (loading) R.string.role_key_fetching else R.string.role_key_fetch))
                        }
                    }
                }
                if (error.isNotEmpty()) {
                    Text(
                        stringResource(R.string.role_key_fetch_fail, error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                // 搜索过滤（全选改到各分类标题行，见下方 LazyColumn）
                if (models.isNotEmpty()) {
                    OutlinedTextField(
                        value = filter,
                        onValueChange = { filter = it },
                        placeholder = {
                            Text(
                                stringResource(R.string.role_key_model_filter),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // 五类分组列表
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    val byCat = linkedMapOf<String, MutableList<String>>()
                    visibleModels.sorted().forEach { m ->
                        byCat.getOrPut(KeyListFile.classifyModel(m)) { mutableListOf() }.add(m)
                    }
                    byCat.forEach { (cat, list) ->
                        item(key = "cat_$cat") {
                            // 全选按分类（全局全选「不能把所有模型都选」不对头，
                            // 位置也浮在搜索框和列表之间没有归属）——挪进分类标题行，只作用本分类可加项
                            Row(
                                // 间距放整行（原来 top=8 只压在标题上，
                                // 标题被顶下去而全选按钮垂直居中，两截错位不像同一行）
                                Modifier.fillMaxWidth().padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "$cat (${list.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                val catSelectable = list.filter { !inGroup(it) }
                                TextButton(onClick = {
                                    selected = if (catSelectable.any { it in selected }) {
                                        selected - catSelectable.toSet()
                                    } else {
                                        selected + catSelectable.toSet()
                                    }
                                }) {
                                    Text(
                                        if (catSelectable.any { it in selected }) stringResource(R.string.role_select_all_cancel)
                                        else stringResource(R.string.role_list_select_all)
                                    )
                                }
                            }
                        }
                        list.forEach { m ->
                            item(key = "m_$m") {
                                val already = inGroup(m)
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clickable(enabled = !already) {
                                            selected = if (m in selected) selected - m else selected + m
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = m in selected,
                                        enabled = !already,
                                        onCheckedChange = { selected = if (it) selected + m else selected - m }
                                    )
                                    Text(
                                        m,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (already) MaterialTheme.colorScheme.onSurfaceVariant
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (already) {
                                        Spacer(Modifier.width(8.dp))
                                        // 浅 primary 底徽章（与「当前」徽章同款式）：灰字扫一眼注意不到
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                        ) {
                                            Text(
                                                stringResource(R.string.role_key_model_in_group),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // 确认时不在这里写条目：只把（网址 + 密钥 + 选中模型 + 分组名）交回上层。
                // 取消/添加是「拉取之后」的事，没拉到模型前不占这行高度
                if (models.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                        TextButton(
                            enabled = selected.isNotEmpty() && ready,
                            onClick = {
                                // 手填分组名撞已有组 ⇒ 拦住确认（不当场改字，Toast 提示）
                                if (nameTaken) {
                                    android.widget.Toast.makeText(
                                        context,
                                        context.getString(R.string.role_key_group_name_exists, typedName),
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                    return@TextButton
                                }
                                val u = targetIfc?.baseUrl ?: url
                                val k = targetIfc?.apiKey ?: key
                                // 并入已有组 ⇒ 名字交空
                                onConfirm(u, k, if (targetIfc != null) "" else finalName, selected.sorted(), rates)
                            }
                        ) {
                            Text(stringResource(R.string.role_key_add_selected, selected.size))
                        }
                    }
                }
            }
        }
    }
    // 手动添加模型：输入模型名进候选列表（不直接入库）；归属由（网址 + 密钥）决定
    if (manualVisible) {
        var manual by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { manualVisible = false },
            title = { Text(stringResource(R.string.role_key_manual_model)) },
            text = {
                OutlinedTextField(
                    value = manual, onValueChange = { manual = it },
                    label = { Text(stringResource(R.string.role_key_model_input_hint)) },
                    singleLine = true, textStyle = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = manual.trim().isNotEmpty() && ready,
                    onClick = {
                        val m = manual.trim()
                        manualVisible = false
                        models = models + m
                        selected = selected + m
                    }
                ) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { manualVisible = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

/**
 * 备份恢复中心（照插件 showBackupRestoreDialog 五项 + 自动备份设置）：
 * 导出当前书籍到剪贴板 / 从剪贴板导入书籍 / 备份全部文件(fullBackup.json) /
 * 从备份完整还原 / 自动备份开关（开启后进角色管理页自动备份）。
 */
@Composable
fun BackupCenterDialog(
    tagRuleId: String,
    version: Int,
    onDismiss: () -> Unit,
    onRestored: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var autoOn by remember { mutableStateOf(false) }
    var inputVisible by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var autoSettingVisible by remember { mutableStateOf(false) }
    // 备份概况与「还原前现场」都摆出来：否则你看不出这份备份是刚才的还是上个月的、有没有后悔药
    var backupInfo by remember { mutableStateOf<CharacterRecordsFile.BackupInfo?>(null) }
    var beforeExists by remember { mutableStateOf(false) }
    // 待确认动作："restore"（完整还原）/ "undo"（撤销上次还原）——两者都覆盖整目录，先问一句
    var confirmAction by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(version) {
        autoOn = withIO { CharacterRecordsFile.readAutoBackupEnabled(tagRuleId) }
        backupInfo = withIO { CharacterRecordsFile.readBackupInfo(tagRuleId) }
        beforeExists = withIO { CharacterRecordsFile.hasBeforeRestore(tagRuleId) }
    }
    fun toast(resId: Int, vararg args: Any) {
        // 无参不过 format（见 lib-common ToastUtils.getStringSafe）：否则串里留 %1$s 就崩
        val msg = if (args.isEmpty()) context.getString(resId) else context.getString(resId, *args)
        android.widget.Toast.makeText(
            context, msg, android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    // 完整还原 / 撤销还原共用：只是数据源不同（fullBackup.json vs fullBackup.before.json）
    fun runRestore(fromBefore: Boolean) {
        scope.launch {
            val n = withIO {
                if (fromBefore) CharacterRecordsFile.restoreFromBefore(tagRuleId)
                else CharacterRecordsFile.restoreAllFiles(tagRuleId)
            }
            toast(
                when {
                    n < 0 -> R.string.backup_none
                    n > 0 -> if (fromBefore) R.string.backup_undo_done else R.string.backup_restore_done
                    else -> R.string.role_list_failed
                }, n
            )
            beforeExists = withIO { CharacterRecordsFile.hasBeforeRestore(tagRuleId) }
            if (n > 0) onRestored()
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)) {
                Text(
                    stringResource(R.string.backup_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(12.dp))
                BackupOptionRow("📋", stringResource(R.string.backup_export_book)) {
                    scope.launch {
                        val recs = withIO { CharacterRecordsFile.readRecords(tagRuleId) }
                        val book = withIO { CharacterRecordsFile.readCurrentBook(tagRuleId) }
                        val arr = JSONArray()
                        recs.forEach { arr.put(it.obj) }
                        val payload = JSONObject()
                            .put("bookName", book)
                            .put("characterData", arr)
                        clipboard.setText(AnnotatedString(payload.toString(2)))
                        toast(R.string.backup_clip_ok)
                    }
                }
                BackupOptionRow("📥", stringResource(R.string.backup_import_book)) {
                    inputText = ""
                    inputVisible = true
                }
                BackupOptionRow(
                    "💾",
                    stringResource(R.string.backup_export_all),
                    // 摆出这份备份是什么时候的、装了几个文件——只有一份、看不见时间就等于闭眼点
                    subtitle = backupInfo?.let { info ->
                        stringResource(
                            R.string.backup_last_time,
                            info.time.ifBlank { stringResource(R.string.backup_time_unknown) },
                            info.fileCount
                        )
                    } ?: stringResource(R.string.backup_none_yet)
                ) {
                    scope.launch {
                        val n = withIO { CharacterRecordsFile.backupAllFiles(tagRuleId) }
                        backupInfo = withIO { CharacterRecordsFile.readBackupInfo(tagRuleId) }
                        toast(if (n > 0) R.string.backup_done else R.string.role_list_failed, n)
                    }
                }
                BackupOptionRow("♻️", stringResource(R.string.backup_restore_all)) {
                    confirmAction = "restore"
                }
                // 撤销入口只在真有现场可退时才摆出来（还原过一次之后才有）
                if (beforeExists) {
                    BackupOptionRow(
                        "↩️",
                        stringResource(R.string.backup_undo_restore)
                    ) { confirmAction = "undo" }
                }
                BackupOptionRow(
                    "🕐", stringResource(R.string.backup_auto_enable)
                ) { autoSettingVisible = true }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                }
            }
        }
    }
    // 覆盖整目录前的确认：把「这份备份是什么时候的」「有后悔药」一并说清
    confirmAction?.let { action ->
        val undo = action == "undo"
        val unknownTime = stringResource(R.string.backup_time_unknown)
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = {
                Text(
                    stringResource(
                        if (undo) R.string.backup_undo_restore else R.string.backup_restore_all
                    )
                )
            },
            text = {
                Text(
                    if (undo) stringResource(R.string.backup_undo_confirm)
                    else stringResource(
                        R.string.backup_restore_confirm,
                        backupInfo?.time?.ifBlank { unknownTime } ?: unknownTime,
                        backupInfo?.fileCount ?: 0
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmAction = null
                    runRestore(fromBefore = undo)
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    // 自动备份设置（标题显当前状态 → 开启/关闭）
    if (autoSettingVisible) {
        AlertDialog(
            onDismissRequest = { autoSettingVisible = false },
            title = {
                Text(
                    stringResource(
                        R.string.backup_auto_state_title,
                        stringResource(if (autoOn) R.string.backup_auto_on else R.string.backup_auto_off)
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    autoSettingVisible = false
                    scope.launch {
                        withIO { CharacterRecordsFile.writeAutoBackupEnabled(tagRuleId, true) }
                        autoOn = true
                        toast(R.string.backup_auto_enabled_toast)
                    }
                }) { Text(stringResource(R.string.backup_auto_turn_on)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        autoSettingVisible = false
                        scope.launch {
                            withIO { CharacterRecordsFile.writeAutoBackupEnabled(tagRuleId, false) }
                            autoOn = false
                            toast(R.string.backup_auto_disabled_toast)
                        }
                    }) { Text(stringResource(R.string.backup_auto_turn_off)) }
                    TextButton(onClick = { autoSettingVisible = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        )
    }

    // 从剪贴板 / 文本导入书籍（照插件 restoreFromText）
    if (inputVisible) {
        AlertDialog(
            onDismissRequest = { inputVisible = false },
            title = { Text(stringResource(R.string.backup_import_book)) },
            text = {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.backup_import_hint),
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    textStyle = MaterialTheme.typography.bodySmall,
                    minLines = 3,
                    maxLines = 8,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = inputText.isNotBlank(),
                    onClick = {
                        scope.launch {
                            val ok = withIO {
                                runCatching {
                                    val obj = JSONObject(inputText)
                                    val bookName = obj.optString("bookName").trim()
                                    val data = obj.optJSONArray("characterData")
                                        ?: throw IllegalArgumentException("bad format")
                                    require(bookName.isNotEmpty()) { "empty book" }
                                    // 统一入口（照插件 restoreFromText）：liebiao / cunfang / characterRecords / shuming.<书> / gengxin 五写
                                    // ⚠️ 旧版只写 3 个 ⇒ 书切走再回来会从书架消失；不写 gengxin 则朗读规则内存是旧角色表
                                    CharacterRecordsFile.importBook(tagRuleId, bookName, data.toString())
                                }.getOrDefault(false)
                            }
                            if (ok) {
                                inputVisible = false
                                onRestored()
                                toast(R.string.backup_restore_book_ok)
                            } else {
                                toast(R.string.backup_clip_bad)
                            }
                        }
                    }
                ) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { inputVisible = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

/** 选项行：无框行 + 语义图标（原「白卡片 + 彩色圆点」压在弹窗底上 = 框中框） */
/** 备份中心选项行；subtitle 用来摆「上次备份于 X · N 个文件」这类事实，不解释了就换行显示 */
@Composable
private fun BackupOptionRow(
    emoji: String,
    text: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            emoji,
            fontSize = 18.sp,
        )
        Spacer(Modifier.width(14.dp))
        if (subtitle == null) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
        } else {
            Column(Modifier.weight(1f)) {
                Text(text, style = MaterialTheme.typography.bodyMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 书籍行左侧圆点配色（照插件 switchColors：8 色循环） */
private val BOOK_DOT_COLORS = listOf(
    Color(0xFF7E57C2), Color(0xFF5C6BC0), Color(0xFF26A69A), Color(0xFF8D6E63),
    Color(0xFF66BB6A), Color(0xFFEC407A), Color(0xFFFF7043), Color(0xFF42A5F5),
)

/** 默认书籍名：不可删除（与 CharacterRecordsFile 同源） */
private const val DEFAULT_BOOK_NAME = "默认"

/** 书籍列表弹窗（照插件 showBookSwitchDialog）：当前书主题浅底 + 描边 + ✓，仅非「默认」书给 ✕（二次确认） */
@Composable
fun BookManagerDialog(
    tagRuleId: String,
    onDismiss: () -> Unit,
    onSwitched: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var version by remember { mutableIntStateOf(0) }
    var books by remember { mutableStateOf<List<String>>(emptyList()) }
    var current by remember { mutableStateOf("") }
    var addBookVisible by remember { mutableStateOf(false) }
    var multiVisible by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(version) {
        val loaded = withIO {
            Pair(CharacterRecordsFile.readBookList(tagRuleId), CharacterRecordsFile.readCurrentBook(tagRuleId))
        }
        books = loaded.first
        current = loaded.second
    }
    fun toast(resId: Int, vararg args: Any) {
        // 无参不过 format（见 lib-common ToastUtils.getStringSafe）：否则串里留 %1$s 就崩
        val msg = if (args.isEmpty()) context.getString(resId) else context.getString(resId, *args)
        android.widget.Toast.makeText(
            context, msg, android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    fun switchTo(book: String) {
        scope.launch {
            val ok = withIO { CharacterRecordsFile.switchBook(tagRuleId, book) }
            toast(if (ok) R.string.role_key_saved else R.string.role_list_failed)
            if (ok) {
                version++
                onSwitched()
                // 切换动作已完成即关窗（10-06 用户令）：原来切完留在弹窗里，
                // 还得手动点外部关掉，像"没生效"；关窗后角色列表立刻展示新书
                onDismiss()
            }
        }
    }
    fun deleteBooks(target: Set<String>) {
        scope.launch {
            val (n, currentDeleted) = withIO { CharacterRecordsFile.deleteBooks(tagRuleId, target) }
            toast(
                if (n > 0) R.string.role_book_deleted_toast else R.string.role_list_failed, n
            )
            if (n > 0) {
                version++
                if (currentDeleted) onSwitched()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Column(
                Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
                    .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.85f).dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    stringResource(R.string.role_book_list_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(12.dp))
                // 清单上限按屏高推算（不写死 dp）；行间靠 0.6dp 浅分隔线分区（首行不加，同密钥弹窗）
                Column(Modifier.fillMaxWidth()) {
                    books.forEachIndexed { idx, book ->
                        if (idx > 0) {
                            HorizontalDivider(
                                thickness = 0.6.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                        }
                        val isCurrent = book == current
                        BookRow(
                            name = book,
                            isCurrent = isCurrent,
                            dotColor = BOOK_DOT_COLORS[idx % BOOK_DOT_COLORS.size],
                            deletable = book != DEFAULT_BOOK_NAME,
                            onClick = { if (!isCurrent) switchTo(book) },
                            onDelete = { pendingDelete = book },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                // 底部操作行：+ 新增书籍 / 多选删除（照插件居中双按钮）
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { addBookVisible = true }) {
                        Text(
                            "+  " + stringResource(R.string.role_book_add),
                            // 不要粗体——TextButton 默认 labelLarge 自带 w500，
                            // 换 bodyMedium（同为 14sp，常规字重）与弹窗正文统一
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = { multiVisible = true }) {
                        Text(
                            stringResource(R.string.role_book_multi_delete_mode),
                            style = MaterialTheme.typography.bodyMedium,
                            // 删除类入口统一 error 红（照插件 #EF6C00 橙已废——
                            // 它不随主题走，且与全 app 「删除=红」口径冲突）
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }

    // 单本删除二次确认（照插件：提示删当前书会切默认）
    pendingDelete?.let { name ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.role_book_delete_title)) },
            text = {
                Text(
                    stringResource(R.string.role_book_delete_text, name) + "\n" +
                            stringResource(
                                if (name == current) R.string.role_book_del_confirm_current
                                else R.string.role_book_del_confirm_other
                            )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    deleteBooks(setOf(name))
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    // 批量删除（照插件 showMultiSelectBookDialog：卡片行 + 复选框 + 当前徽章）
    if (multiVisible) {
        var checked by remember { mutableStateOf<Set<String>>(emptySet()) }
        val allChecked = books.isNotEmpty() && checked.size == books.size
        Dialog(
            onDismissRequest = { multiVisible = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
            ) {
                Column(
                    Modifier
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
                        .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.85f).dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        stringResource(R.string.role_book_multi_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.fillMaxWidth()) {
                        books.forEachIndexed { idx, book ->
                            if (idx > 0) {
                                HorizontalDivider(
                                    thickness = 0.6.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )
                            }
                            val isCurrent = book == current
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        checked = if (book in checked) checked - book else checked + book
                                    }
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = book in checked,
                                    onCheckedChange = {
                                        checked = if (it) checked + book else checked - book
                                    },
                                )
                                Text(
                                    book,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (isCurrent) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                    ) {
                                        Text(
                                            stringResource(R.string.role_key_current),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = {
                            checked = if (allChecked) emptySet() else books.toSet()
                        }) {
                            Text(
                                stringResource(
                                    if (allChecked) R.string.deselect_all else R.string.select_all
                                )
                            )
                        }
                        TextButton(onClick = { multiVisible = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                        TextButton(
                            enabled = checked.isNotEmpty(),
                            onClick = {
                                val target = checked
                                multiVisible = false
                                deleteBooks(target)
                            },
                        ) {
                            Text(
                                stringResource(R.string.role_book_multi_delete, checked.size),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }

    // 新增书籍（照插件：输入书名 → 建档并直接切换）
    if (addBookVisible) {
        var bookName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addBookVisible = false },
            title = { Text(stringResource(R.string.role_book_add)) },
            text = {
                OutlinedTextField(
                    value = bookName,
                    onValueChange = { bookName = it },
                    label = { Text(stringResource(R.string.role_book_name)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = bookName.trim().isNotEmpty() && bookName.trim() !in books,
                    onClick = {
                        val n = bookName.trim()
                        addBookVisible = false
                        scope.launch {
                            val ok = withIO {
                                CharacterRecordsFile.addBook(tagRuleId, n) &&
                                    CharacterRecordsFile.switchBook(tagRuleId, n)
                            }
                            toast(if (ok) R.string.role_book_added_switch else R.string.role_list_failed, n)
                            if (ok) { version++; onSwitched() }
                        }
                    }
                ) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { addBookVisible = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

/** 书籍列表行（无框行，与密钥条目行同一口径） */
@Composable
private fun BookRow(
    name: String,
    isCurrent: Boolean,
    dotColor: Color,
    deletable: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 行首标记位固定 16dp：当前书=主色竖条 / 其余=彩色圆点 → 书名左缘始终对齐
        Box(Modifier.width(16.dp), contentAlignment = Alignment.CenterStart) {
            if (isCurrent) {
                Box(
                    Modifier
                        .width(3.dp)
                        .height(18.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                )
            } else {
                // 5dp（原 7dp→6dp 仍嫌大；取 5 不取 4——4 与「当前书」竖条 3dp 宽度太近，
                // 两种标记的体量差会被抹平）
                Box(Modifier.size(5.dp).background(dotColor, CircleShape))
            }
        }
        Text(
            name,
            // 15sp（10-06 用户：16sp 偏大，降一档）。原沿革：14sp(bodyMedium 次要信息档)
            // → 16sp(与角色名同款) → 15sp（本次回调，仍高于次要档、小于角色名，弹窗内不喧宾）
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            // 当前书不再加粗（不喜欢粗体）——行首主色竖条 + 主色书名已够表达
            color = if (isCurrent) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (deletable) {
            // 删除叉染红（10-06 用户令）：删书不可逆，红色表意更明确；
            // 原「扁平灰叉，进批量删除弹窗才红」口径作废
            FlatIconAction(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.error,
            ) {
                onDelete()
            }
        }
    }
}
