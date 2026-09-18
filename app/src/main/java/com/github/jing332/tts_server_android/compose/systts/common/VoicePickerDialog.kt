package com.github.jing332.tts_server_android.compose.systts.common

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.drake.net.utils.withIO
import com.drake.net.utils.withMain
import com.github.jing332.common.utils.toParamText
import com.github.jing332.compose.widgets.AppSelectionDialog
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts.PreviewState
import com.github.jing332.tts.TaggedTtsPreviewPlayer
import com.github.jing332.tts.CachedEngineManager
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.SharedViewModel
import com.github.jing332.tts_server_android.compose.SoftSegmentedTextToggle
import com.github.jing332.tts_server_android.compose.systts.list.ui.PluginDescriptor
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.AudioParamsDimensionSection
import com.github.jing332.tts_server_android.conf.AppConfig
import com.github.jing332.tts_server_android.conf.SysTtsConfig
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import com.github.jing332.tts_server_android.service.systts.help.CharacterRecordsFile
import com.github.jing332.tts_server_android.service.systts.help.VoiceMarksFile
import kotlinx.coroutines.launch

/**
 * 通用换声弹窗（用户 09-13 定稿「完全同源」）：日志快捷面板与角色管理插件共用的
 * 「更换发音人 + 音频参数」两分段弹窗，从 LogQuickPanel 原样抽出，宿主只传锚点。
 *
 * 宿主：
 * - 日志快捷面板（LogQuickPanel）：传 entry 的 configId/roleName + sharedVM（主列表定位联动）；
 * - 角色管理插件（桥）：VoicePickerBus 请求 → PluginTtsUI.EditContentScreen 宿主渲染，
 *   传 anchorTag=角色当前 tag + bindingKey=角色名，变化经 [onChanged] 回喊插件 JS。
 *
 * 容器（定版，四易其稿：居中弹窗 → 全屏对话框 → 底部面板+自适应 → 底部面板+定高
 * → **底部面板+定高+定头单滚动**）：
 * 全屏方案的病根是高度写死成屏高（音频参数段只占半屏多，下方四成空着）；随后试「高度跟着内容
 * 走」，又暴露三个毛病——切 tab 面板长高/缩矮、搜索每敲一个字面板跟着缩、候选只剩一两条时面板
 * 塌成小条。故取固定档位：**面板恒为可用高的 92%**（09-18 上调，原 88%——底部「确认」
 * 动作行占 ~52dp 后按比例找平；仍恒定不变，两段、任意候选数都不变）。
 * ⚠️ 两条必须在的口径（都是 09-14 晚实测踩出来的）：
 * ① 高度基准取**弹窗窗口的真实可用高**（BoxWithConstraints 的 maxHeight），不用
 *    Configuration.screenHeightDp——后者来自设备显示配置，偏大时面板底缘被顶出屏幕；而底栏
 *    「取消/确认」正好在面板最下缘，表现就是「面板里看不到确认键」（实锤）；
 * ② **定头 + 单滚动**：标题行 / 分段 / 当前发音人+终值 / 分类+搜索 四层固定，只有候选列表
 *   （或音频参数段）用 weight(1f) 吃满剩余高度并自带内滚。原先"整块内容可滚 + 列表再滚"
 *    是双层嵌套，手势互抢——列表只分到约 5 行（「上滑空间太小」），列表滚到底后手势链到外层，
 *    又把头部整块卷出视野（「上方的很多都隐藏了」）。
 * 宽度不受影响，左右各让 16dp，360dp 屏上内容区仍是 328dp，比旧居中弹窗的 275dp 宽。
 * **仍是 Dialog 语义、不改成 Activity**——插件桥靠 VoicePickerBus 请求 + notifyMutated 回喊
 * JS，跨页面会让这条回喊链变脆。
 *
 * 结构（用户 09-09 定稿，抽组件时未动）：
 * - 标题行（09-14 改紧凑行，取代 TopAppBar）：左「角色卡（名字）」/「信息卡」+ 右 ✕，下缘
 *   0.6dp 浅分隔线——**本面板唯一一条**（动作栏上那条 09-14 撤，理由见下）。原 TopAppBar 是
 *   M3 一级页面语汇（64dp 通栏 + 22sp 大标题），弹窗借来用会读成「App 的一个页面」，
 *   且它与底栏相距一屏、把内容夹在中间；
 * - 顶部（两区共用）：当前发音人 + ▶试听 + 终值行（播放链同源三层乘积，值为 1.0 的维度不显示）；
 * - [更换发音人] 绑定模式=分类下拉(含全部，带N项)+搜索+候选列表；旁白模式=只读分类框+同标签全量候选；
 *   行内试听 ▶/…/■ 状态机参照角色管理v10；换声两段式：点行=暂存（选中行染主色，无圆点标记），
 *   底部「确认」落库；
 *   候选行 ⋮ 菜单=发音人标记(❤️🚶😈，voice_marks.json 与角色管理同源) + 删除配置项。
 * - [音频参数]（09-10 按维度改版）：语速/音量/音高第二级分段，每维三层滑杆同屏，
 *   重置/应用按维度一组（应用=该维三层一起落库，不关面板）；
 *   参数跟随所选发音人（用户 09-10），目标切换时三层草稿整体重载。
 *
 * @param anchorConfigId 锚点配置项 id（日志面板传 entry.configId）；null/0 时按 [anchorTag] 解析
 * @param anchorTag      无具体配置项时按 tag 解析锚点（插件桥传角色当前绑定 tag）；也为非绑定候选归组键
 * @param bindingKey     绑定键：多角色日志=角色名、「本地音效N」槽位=槽位名；空=非绑定模式
 * @param isLocalSoundSlot 本地音效槽位：候选枚举同族 localSoundN（不读池子）、隐藏分类下拉/搜索
 * @param createIfMissing 添加角色模式（终版）：不弹独立名字窗，直接开本弹窗——
 *                       顶部多一行角色名填写框（描边=可输入），无「当前发音人/终值」顶部块
 *                       （新角色无当前绑定，候选行自带试听），标题随输入实时显「角色卡（名字）」；
 *                       确认键=建记录并写入所选 tag id，成功后自动关弹窗。锚点仍需有效
 *                       （调用方保证），bindingKey/titleBadge 传空即可
 * @param releaseOwnerName 释放模式：非空=「释放并固定」链路——把 [releaseName]
 *                       从该角色名下解绑、另立一条记录并写入所选 tag id（落库走 releaseAndFix）。
 *                       不显示顶部名字输入框（名字已定，标题即「角色卡（名字）」），其余
 *                       候选链路 / 试听 / 确认键与添加角色完全同源
 * @param releaseName     释放模式预填名字（= 被释放的别名）
 * @param titleBadge     角色名（非空=标题显示「角色卡（名字）」）；空=标题显示「信息卡」
 *                       ——调用方自传标题已废除（曾出现「发音人调整/更换发音人」两套乱名）
 * @param sharedVM       主界面共享状态（日志面板专用：换声后主列表定位高亮、标记版本联动）；null=跳过
 * @param onChanged      变化回调 (event, tag)：applied=换声落库 / deleted=配置项删除 / marked=标记变化；
 *                       插件桥宿主接它回喊 JS；日志面板传 null
 * @param onDismissRequest 关闭（点外部/返回键/取消；锚点配置项被删时也会触发）
 */
@OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    // decorFitsSystemWindows（全屏弹窗铺满到系统栏后面）在部分 Compose 版本仍标着实验注解。
    // 本项目未开 warnings-as-errors，冗余 opt-in 至多是条警告，换来的是跨版本都能编过。
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)
@Composable
fun VoicePickerDialog(
    anchorConfigId: Long?,
    anchorTag: String,
    bindingKey: String,
    titleBadge: String = "",
    isLocalSoundSlot: Boolean = false,
    groupBindingKeys: List<String> = emptyList(),
    createIfMissing: Boolean = false,
    releaseOwnerName: String = "",
    releaseName: String = "",
    sharedVM: SharedViewModel? = null,
    onChanged: ((event: String, tag: String) -> Unit)? = null,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun resolveAnchor(): SystemTtsV2? =
        anchorConfigId?.takeIf { it != 0L }?.let { dbm.systemTtsV2.get(it) }
            ?: if (anchorTag.isNotBlank()) {
                // 插件桥路径：按 tag 解析锚点（优先启用配置，禁用配置兜底——绑定候选虽然只要启用，
                // 但锚点仅作参数/显示基准，禁用配置也能看参数）
                dbm.systemTtsV2.getAllGroupWithTts().flatMap { it.list }
                    .firstOrNull {
                        (it.config as? TtsConfigurationDTO)?.speechRule?.tag == anchorTag
                    }
            } else null

    val anchorFallback = remember(anchorConfigId, anchorTag) { resolveAnchor() }
    if (anchorFallback == null) {
        // 开面板时锚点就无效（配置项已删/无锚点）：提示后由外层关闭
        Toast.makeText(context, context.getString(R.string.log_panel_config_missing), Toast.LENGTH_SHORT).show()
        onDismissRequest()
        return
    }
    // 锚点可被删后重解析（照角色管理 v10：删掉某个发音人后面板不关、列表继续用最新数据）。
    // ——同标签还有别的配置项就接替（草稿/暂存随新配置项重来）；
    // ——一个都不剩时沿用旧快照，面板保持打开（写回该配置项的动作会被拦，见确认键守卫）。
    // 不用 LaunchedEffect 观察：DB 无观察者，删除点自增 anchorVersion 触发这里重解析。
    var anchorVersion by remember { mutableStateOf(0) }
    val resolvedAnchor = remember(anchorConfigId, anchorTag, anchorVersion) { resolveAnchor() }
    val anchorMissing = resolvedAnchor == null
    val entity = resolvedAnchor ?: anchorFallback
    val config = entity.config as? TtsConfigurationDTO ?: run {
        Toast.makeText(context, context.getString(R.string.log_panel_config_missing), Toast.LENGTH_SHORT).show()
        onDismissRequest()
        return
    }
    val source = config.source as? PluginTtsSource
    // 规则 tags 表（tag id→显示名）：大分类显示的权威来源（与编辑页标签两层弹窗同源）。
    // config.speechRule.tagName 只是绑定时的快照，规则改版后会过期——实测
    // 非绑定分类显示不对，改从 rule.tags 现查
    var ruleTags by remember(entity.id) { mutableStateOf<Map<String, String>?>(null) }
    LaunchedEffectOnce(entity.id) {
        // getByRuleIdAll 不带 isEnabled 过滤（getByRuleId 对禁用规则返回 null → 只能拿旧快照）
        ruleTags = withIO {
            dbm.speechRuleDao.getByRuleIdAll(config.speechRule.tagRuleId)?.tags
        }
    }
    // 大分类口径：分类以 tag id 查 rule.tags 得显示名——显示名带尾序号的
    // 合并成一类（女青年01→女青年、本地音效1→本地音效），不带序号的每种各自独立
    // （旁白、男、女、【】括号发音人、「」括号发音人、『』括号发音人、在线音效——括号系不合并）。
    // 规则未加载时回落 tagName 快照
    val displayCategory = extractTagCategory(
        ruleTags?.get(config.speechRule.tag) ?: config.speechRule.tagName
    )
    // 添加角色 / 释放并固定两种模式都走绑定候选链路（分类下拉+池子∩启用候选），只是落库分支不同
    val addMode = createIfMissing || releaseOwnerName.isNotBlank()
    val isBindingMode = bindingKey.isNotBlank() || addMode

    // ===== 行内试听状态（参照角色管理v9/v10试听状态机：▶ →(点击)… →(真正出声)■ →(播完复位)▶）=====
    // 播放器全局单实例（同一时刻只有一个试听），previewingKey 记当前行（顶部=current，绑定=tag，旁白=voice）
    val previewState by TaggedTtsPreviewPlayer.state.collectAsState()
    var previewingKey by remember(entity.id) { mutableStateOf<Any?>(null) }
    // 标签纯由 previewingKey+previewState 推导，**不用 LaunchedEffect 在 IDLE 时清 key**——
    // 那条复位路会把「上一条试听刚结束/失败的 IDLE 广播」落进新点击与 play() 置 SYNTHESIZING
    // 的窗口里（绑定分支要经 withIO 查库才有 play，窗口更宽），刚写入的新 key 被抹掉、
    // play 照常出声 → 该行全程 ▶ 无反馈（「第一行有反馈、后面的行点了不动」实锤）。
    // 改状态推导后 IDLE 恒显 ▶、key 残留无害（所有消费点都有 state!=IDLE 守卫），与角色管理
    // 插件的轮询复位语义完全一致
    fun previewLabel(key: Any?): String = when {
        previewingKey != key -> "▶"
        previewState == PreviewState.PLAYING -> "■"
        previewState == PreviewState.SYNTHESIZING -> "…"
        else -> "▶"
    }

    @Composable
    fun previewLabelColor(key: Any?): Color =
        if (previewingKey == key) MaterialTheme.colorScheme.tertiary else Color.Unspecified

    // 面板分段：0=更换发音人 1=音频参数。提升到容器之前声明——底部动作行也要读它
    //（09-09 CI 教训：content 槽内声明的局部状态对 buttons 槽不可见；09-14 改全屏后虽不再有
    //  buttons 槽，但顶栏/底栏/正文三处仍共用它，保持这个「先声明后用」的位置）
    var panelTab by remember(entity.id) { mutableStateOf(0) }

    // ===== 本地编辑草稿：各维度「应用」才落库 =====
    var displayName by remember(entity.id) { mutableStateOf(entity.displayName) }
    var voice by remember(entity.id) { mutableStateOf(source?.voice ?: "") }
    var speed by remember(entity.id) { mutableStateOf(config.audioParams.speed) }
    var volume by remember(entity.id) { mutableStateOf(config.audioParams.volume) }
    var pitch by remember(entity.id) { mutableStateOf(config.audioParams.pitch) }

    // 换声两段式（用户 09-08）：点候选行=暂存选中（不落库），底部「确认」键才生效——
    // 即点即改的 Toast 反馈太弱且易误触；未确认选择在关闭面板时自然丢弃
    var pendingVoice by remember(entity.id) { mutableStateOf<String?>(null) }
    // 添加角色模式：角色名在弹窗内填写（终版：不弹独立名字窗），
    // 标题实时跟随；确认时非空才可点。
    // 释放模式：名字已定（= 被释放的别名），这里只作预填、不显示输入框
    var inputName by remember(entity.id) { mutableStateOf(releaseName) }

    // 绑定模式当前绑定（提升到分支外：底部确认行生效后要更新它）
    // 绑定值=标签 id（fayinren.json/characterRecords 里存的都是 tag id，JS 侧 tags[voiceTag]=显示名仅用于回显）；
    // 无绑定记录时回落本配置项自己的 tag
    var boundVoice by remember(entity.id) {
        mutableStateOf(
            // createIfMissing（添加角色链路）：新角色无记录也无「当前绑定」，回落空串——
            // 这样任何候选都 ≠ boundVoice，确认键才能走到建记录分支（否则选锚点自身的
            // 标签会被「与当前绑定相同」守卫吞掉，新角色永远建不出来）
            CharacterRecordsFile.readCharacterVoice(
                config.speechRule.tagRuleId, bindingKey
            ) ?: if (addMode) "" else config.speechRule.tag
        )
    }

    // ===== 角色管理互通（用户 09-12）：候选行 ⋮ 菜单=发音人标记 + 删除配置项 =====
    // 标记写 voice_marks.json（与角色管理 v10 同文件同字段，按标签 id 多选 toggle，❤️🚶😈）；
    // ===== 刷新分档：三把版本号钥匙（照角色管理 v10 的刷新粒度，判据=行集合是否变）=====
    // v10 对照：refreshFayinrenList()=纯数据重读；refreshCharacterData()=重读数据+只更新行内标签、
    // 不重建行；refreshCharacterList()=重建行。这里用三个独立 remember key 表达同一分档：
    // Compose 只重算挂了该 key 的表达式，拆开就等于"精确到该刷的那几项"；
    // 合成一把会连累无关重读（勾个标记不该去重查一遍 DB）。
    // ① 标记：只动 voice_marks.json（文件通道无观察者，靠版本号刷新）→ 顶部/行内标记
    var marksVersion by remember(entity.id) { mutableStateOf(0) }
    // ② 行集合/池子：删除配置项 ⇒ 启用标签集合与候选池都变。DB 通道同样没有观察者，
    //    只能靠版本号驱动重组。参照 v10：filterAndShowVoiceList 每次调用前先 refreshFayinrenList()，
    //    所以那边删除后面板里的列表仍是实时的；这里原来只 remember(entity.id)，删完行还留着（假数据）。
    var dataVersion by remember(entity.id) { mutableStateOf(0) }
    // ③ 行内容/归属：行集合不变、行内文本变（换声落库改的就是某行的显示名/发音人 id）。
    //    挂它的派生值见下方 allConfigs / narrationCandidates / voiceOwners / boundConfigName；
    //    池子与启用标签集合只挂 ②，不为一行改名整份重算。
    var rowVersion by remember(entity.id) { mutableStateOf(0) }
    // 待删除确认的配置项（非空时弹确认弹窗）
    var deleteConfirmTarget by remember(entity.id) { mutableStateOf<SystemTtsV2?>(null) }

    // 全部配置项（换声候选 / 参数跟随目标查找共用）——数据重读层（v10 的 refreshFayinrenList 位）：
    // 任何一次刷新都重读，故两个版本号都挂
    val allConfigs = remember(entity.id, dataVersion, rowVersion) {
        dbm.systemTtsV2.getAllGroupWithTts().flatMap { it.list }
    }

    /** 按标签（tag id）查启用配置项（试听/当前发音人名/候选行displayName/参数跟随共用）——
     *  用户 09-12 定稿：匹配键=**tag（id）**，fayinren.json/characterRecords 存的都是 tag id。
     *  正常配置=**一个标签只启用一条**：换声即改写这条启用配置，该标签后续
     *  片段确定全换；「同 tag 多配置随机轮播」属误操作，引擎侧 random 只是兜底，勿当设计意图；
     *  tagName 是显示名不参与匹配（09-12 晚：tagName 兜底也移除，全链只认 tag） */
    fun enabledConfigEntityByTag(tag: String): SystemTtsV2? {
        if (tag.isEmpty()) return null
        return allConfigs.firstOrNull { item ->
            if (!item.isEnabled) return@firstOrNull false
            (item.config as? TtsConfigurationDTO)?.speechRule?.tag == tag
        }
    }

    /** 按发音人ID查配置项（旁白暂存候选的参数跟随目标） */
    fun narrationEntityByVoice(v: String): SystemTtsV2? =
        allConfigs.firstOrNull {
            ((it.config as? TtsConfigurationDTO)?.source as? PluginTtsSource)?.voice == v
        }

    // ===== 参数跟随目标（用户 09-10）：选择发音人后，音频参数区/终值/顶部试听全部
    // 跟随所选发音人对应的配置项——绑定模式=绑定(含暂存)标签的启用配置项；
    // 旁白=暂存候选的配置项；无暂存=本配置项。目标切换时三层草稿整体重载。=====
    var paramsTarget by remember(entity.id) { mutableStateOf(entity) }

    // 非绑定换声落库后的新显示名——entity 是 remember 的库内旧快照，头部「当前发音人」靠它
    // 立即跟上。09-17 起确认成功即关面板，正常路径上它已看不到效果（值随后随组合一起丢弃）；
    // 留着是因为口径改回「面板不关」时只需删掉那行 dismiss，头部取值链不用动。
    var appliedDisplayName by remember(entity.id) { mutableStateOf<String?>(null) }

    // 旁白/非多角色换声（对话绑定模式走 CharacterRecordsFile.rebind，两分支各自处理）：
    // 09-10 起连同当前参数草稿（=跟随所选发音人得来的值）一并写入本配置项，
    // 保证「确认」前后看到/听到的参数与实际生效一致
    fun applyVoice(selected: String) {
        voice = selected
        val sourceNow = (entity.config as? TtsConfigurationDTO)?.source as? PluginTtsSource
        // 09-12 修复（装机反馈"声音变了名字还是旧的"）：displayName 一并同步成目标发音人的
        // 配置项名——日志"显示名"段/主界面列表/弹窗头部全读 displayName，只写 voice 会出现
        // 「声音换了、日志和主界面还挂原发音人」。取名口径与弹窗头部一致（同 voice 的候选配置项名）
        val targetDisplayName = allConfigs.firstOrNull { c ->
            c.id != entity.id &&
                ((c.config as? TtsConfigurationDTO)?.source as? PluginTtsSource)?.voice == selected
        }?.displayName?.takeIf { it.isNotBlank() }
        val newConfig = sourceNow?.let { sn ->
            config.copy(
                source = sn.copy(voice = selected),
                audioParams = config.audioParams.copy(
                    speed = snapParam(speed), volume = snapParam(volume), pitch = snapParam(pitch),
                ),
            )
        }
        scope.launch {
            if (newConfig != null) withIO {
                dbm.systemTtsV2.update(
                    entity.copy(config = newConfig, displayName = targetDisplayName ?: entity.displayName)
                )
                SystemTtsService.notifyUpdateConfig()
            }
            // 弹窗头部即时跟上新名字（库已落，重开弹窗走库值）
            appliedDisplayName = targetDisplayName
            // 行内容变了、行集合没变：只让挂了 rowVersion 的派生值重读（v10 口径：换声后
            // 候选列表原地更新行文本，不重建列表）——旁白页那行的配置项名/发音人 id 立刻跟上
            rowVersion++
            // 参数跟随目标回到本配置项（已带新参数），后续调整继续作用于本条
            if (newConfig != null) paramsTarget =
                entity.copy(config = newConfig, displayName = targetDisplayName ?: entity.displayName)
            // 主界面定位（用户 09-09）：换完旁白发音人，主列表滚动到被改的配置项并短暂高亮
            if (sharedVM != null) sharedVM.pendingLocateConfigId.value = entity.id
            onChanged?.invoke("applied", selected)
            Toast.makeText(
                context,
                context.getString(R.string.log_panel_voice_applied),
                Toast.LENGTH_SHORT,
            ).show()
            // 用户 09-17：确认落库后关面板（同一处口径：确认=完成即退出）。旁白这条原来也继承
            // 日志快捷面板的「不关」，但面板盖着主列表时，上面那行 pendingLocateConfigId 的
            // 滚动/高亮全发生在面板背后——关掉才看得见「刚改的是哪条」。
            // 放在协程里、withIO 落库之后关：若先关再写，rememberCoroutineScope 会随面板
            // 一起取消，落库可能被腰斩。没落库（newConfig == null，如本地音效无插件源）则保持
            // 打开，别用「已关闭」谎报成功。
            if (newConfig != null) onDismissRequest()
        }
    }

    // 删除这条配置项（深夜再纠偏：粒度=「我点的这一条」——
    // 删「女青年01 - 晓晓」只删这条启用配置，同标签下没启用的残留一律不动）：
    // ① 删该配置项并清失效引擎缓存；
    // ② 仅当该标签已无其他**启用**配置时，才从 fayinren.json 移除该标签
    //    （标签还有启用配置就必须留在池子里）；
    // ③ 角色绑定不清：启用项被删后该标签即失效，角色列表显示「标签 + ⚠」，
    //    规则下次朗读自动为受影响角色重新分配（与角色管理 v10 同口径）。
    fun deletePreviewedConfig(target: SystemTtsV2) {
        val targetDto = target.config as? TtsConfigurationDTO ?: return
        val targetTag = targetDto.speechRule.tag
        val tagRuleId = targetDto.speechRule.tagRuleId
        scope.launch {
            var tagNowEmpty = false
            val deletedSelf = withIO {
                dbm.systemTtsV2.delete(target)
                runCatching { CachedEngineManager.removeEngine(targetDto.source) }
                val stillEnabled = dbm.systemTtsV2.allEnabled.any { item ->
                    val d = item.config as? TtsConfigurationDTO ?: return@any false
                    d.speechRule.tagRuleId == tagRuleId && d.speechRule.tag == targetTag
                }
                if (!stillEnabled) {
                    tagNowEmpty = true
                    CharacterRecordsFile.removeFromPool(tagRuleId, targetTag)
                }
                target.id == entity.id
            }
            SystemTtsService.notifyUpdateConfig()
            onChanged?.invoke("deleted", targetTag)
            // 列表按最新数据重建（v10 口径：删完列表还在，而且是实时的）
            dataVersion++
            Toast.makeText(
                context,
                context.getString(R.string.role_voice_del_toast, target.displayName),
                Toast.LENGTH_SHORT,
            ).show()
            // 被删的恰是暂存选中那条、且该标签再无启用配置：清掉暂存——
            // 否则「确认」会把这个已经查不到配置的标签写进角色记录（改绑到无启用配置的标签
            // 会掉进随机兜底，读声不可控，候选池本来就排除这种标签）
            if (tagNowEmpty && pendingVoice == targetTag) pendingVoice = null
            // 参数跟随目标落在这条上：改跟回本面板的锚点，避免「应用」写进一条已不存在的配置项
            if (paramsTarget.id == target.id) paramsTarget = entity
            // 本配置项自身被删：面板**不关**（照 v10：删除发音人后面板留着继续挑）——
            // 改为重解析锚点，同标签还有配置项就接替；一个都不剩则标记失效。
            // 写回该配置项的动作由确认键守卫拦下（非绑定分支才写它，绑定分支改写角色记录，不受影响）
            if (deletedSelf) {
                anchorVersion++
            } else if (tagNowEmpty && boundVoice == targetTag) {
                // 当前绑定恰是被删空标签：回落到本面板配置项自己的 tag（同初始化兜底；仅绑定分支有意义）
                boundVoice = config.speechRule.tag
            }
        }
    }

    // 插件元数据（轻量）：插件层草稿初值 + 终值路由判断用（随参数跟随目标走）
    var plugin by remember(entity.id) {
        mutableStateOf(source?.let { dbm.pluginDao.getMetaByPluginId(it.pluginId) })
    }
    var pluginSpeed by remember(entity.id) { mutableStateOf(1f) }
    var pluginVolume by remember(entity.id) { mutableStateOf(1f) }
    var pluginPitch by remember(entity.id) { mutableStateOf(1f) }
    LaunchedEffectOnce(entity.id) {
        source?.let { s ->
            runCatching {
                val loaded = dbm.pluginDao.getByPluginId(s.pluginId) ?: return@runCatching
                withMain {
                    plugin = loaded
                    pluginSpeed = snapParam(loaded.audioParams.speed)
                    pluginVolume = snapParam(loaded.audioParams.volume)
                    pluginPitch = snapParam(loaded.audioParams.pitch)
                }
            }
        }
    }

    var globalSpeed by remember { mutableStateOf(SysTtsConfig.audioParamsSpeed) }
    var globalVolume by remember { mutableStateOf(SysTtsConfig.audioParamsVolume) }
    var globalPitch by remember { mutableStateOf(SysTtsConfig.audioParamsPitch) }

    // 按维度脏标记（09-10 ②A）：该维任一层滑杆改动置 true，应用成功清除（按钮 ● 提示）
    var speedDirty by remember(entity.id) { mutableStateOf(false) }
    var volumeDirty by remember(entity.id) { mutableStateOf(false) }
    var pitchDirty by remember(entity.id) { mutableStateOf(false) }

    // 暂存选择 → 参数跟随目标切换（含确认后/取消暂存的回退；初始运行顺带校正绑定模式目标）
    androidx.compose.runtime.LaunchedEffect(pendingVoice) {
        val pv = pendingVoice
        val target = when {
            isBindingMode -> enabledConfigEntityByTag(pv ?: boundVoice)
            pv != null -> narrationEntityByVoice(pv)
            else -> null
        } ?: return@LaunchedEffect
        if (target.id != paramsTarget.id) paramsTarget = target
    }

    // 目标切换：整体重载三层草稿（插件层随目标的插件走），脏标记复位
    androidx.compose.runtime.LaunchedEffect(paramsTarget.id) {
        if (paramsTarget.id == entity.id) return@LaunchedEffect
        val dto = paramsTarget.config as? TtsConfigurationDTO ?: return@LaunchedEffect
        val src = dto.source as? PluginTtsSource
        val loaded = src?.let { runCatching { dbm.pluginDao.getByPluginId(it.pluginId) }.getOrNull() }
        withMain {
            speed = snapParam(dto.audioParams.speed)
            volume = snapParam(dto.audioParams.volume)
            pitch = snapParam(dto.audioParams.pitch)
            plugin = loaded
            pluginSpeed = snapParam(loaded?.audioParams?.speed ?: 1f)
            pluginVolume = snapParam(loaded?.audioParams?.volume ?: 1f)
            pluginPitch = snapParam(loaded?.audioParams?.pitch ?: 1f)
            speedDirty = false
            volumeDirty = false
            pitchDirty = false
        }
    }

    /** 用当前草稿构造临时实体试听：未保存候选也先听，走统一试听链 */
    fun draftEntity(candidateVoice: String? = null): SystemTtsV2 {
        val v = candidateVoice ?: voice
        val renamed = if (v.isNotBlank() && v != (source?.voice ?: "")) v else displayName
        return entity.copy(
            displayName = renamed,
            config = config.copy(
                audioParams = config.audioParams.copy(speed = speed, volume = volume, pitch = pitch),
                source = source?.copy(voice = v) ?: config.source,
            ),
        )
    }

    /** 参数跟随目标+当前草稿构造试听实体（绑定模式顶部试听用） */
    fun draftParamsTarget(): SystemTtsV2 {
        val dto = paramsTarget.config as? TtsConfigurationDTO ?: return paramsTarget
        return paramsTarget.copy(
            config = dto.copy(
                audioParams = dto.audioParams.copy(
                    speed = speed, volume = volume, pitch = pitch,
                )
            )
        )
    }

    /** 维度应用（09-10 ②A）：该维三层一起落库——配置层写参数跟随目标，
     *  插件层写其插件，全局层写系统配置；接管判定已废除，所有维度恒可调恒落库 */
    fun applyDim(dim: Int) {
        val targetDto = paramsTarget.config as? TtsConfigurationDTO ?: return
        val targetSource = targetDto.source as? PluginTtsSource
        scope.launch {
            withIO {
                val newConfig = targetDto.copy(
                    audioParams = targetDto.audioParams.copy(
                        speed = if (dim == 0) snapParam(speed) else targetDto.audioParams.speed,
                        volume = if (dim == 1) snapParam(volume) else targetDto.audioParams.volume,
                        pitch = if (dim == 2) snapParam(pitch) else targetDto.audioParams.pitch,
                    )
                )
                dbm.systemTtsV2.update(paramsTarget.copy(config = newConfig))
                if (targetSource != null) {
                    dbm.pluginDao.getByPluginId(targetSource.pluginId)?.let { p ->
                        dbm.pluginDao.update(
                            p.copy(
                                audioParams = p.audioParams.copy(
                                    speed = if (dim == 0) snapParam(pluginSpeed) else p.audioParams.speed,
                                    volume = if (dim == 1) snapParam(pluginVolume) else p.audioParams.volume,
                                    pitch = if (dim == 2) snapParam(pluginPitch) else p.audioParams.pitch,
                                )
                            )
                        )
                        // 卡片"插件语速/音量"显示缓存失效
                        PluginDescriptor.invalidatePluginParamsCache(p.pluginId)
                    }
                }
                when (dim) {
                    0 -> SysTtsConfig.audioParamsSpeed = snapParam(globalSpeed)
                    1 -> SysTtsConfig.audioParamsVolume = snapParam(globalVolume)
                    else -> SysTtsConfig.audioParamsPitch = snapParam(globalPitch)
                }
                SystemTtsService.notifyUpdateConfig()
            }
            when (dim) {
                0 -> speedDirty = false
                1 -> volumeDirty = false
                else -> pitchDirty = false
            }
            Toast.makeText(
                context,
                context.getString(R.string.audio_params_apply_dim_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    // 底部面板 + 统一定高（终版，三易其稿：居中弹窗 → 全屏对话框 → 高度自适应 → 定高）。
    // 高度自适应看着"不浪费"，实测有三个毛病：①两段内容量差一倍，切 tab 时面板长高/缩矮；
    // ②搜索框每敲一个字候选就少几条，面板跟着一缩一缩；③候选只剩一两条时面板塌成小条，
    // 像个 snackbar 不像面板。底部面板本该是个稳定的容器，故改成固定档位 72% 屏高。
    // 宽度不受影响：面板左右各让 16dp，360dp 屏上内容区仍是 328dp——与全屏方案相同，不会退回
    // 旧居中弹窗被 MD3 24dp 内边距挤成 275dp 的老毛病。
    // 仍是 Dialog 语义，**不改成 Activity**（密钥管理那条路）：角色管理插件靠 VoicePickerBus
    // 提交请求、弹窗内变化再经 notifyMutated 回喊 JS，跨页面会让这条回喊链变脆。
    // 关闭出口：✕ 或点遮罩（底部面板有 scrim，无需强制只在 ✕ 上）；确认动作仍留底部栏——
    // 换声段它是落库出口，面板定高后它就贴在面板底缘（不再"跟着内容浮上来"）。
    // 外壳换 M3 ModalBottomSheet（09-18 实验定论：音色广场/筛选已实机验证，MBS 弹层窗口
    // 能逃过 OriginOS 把 Compose Dialog 窗口排版下移出屏的毛病，贴底动作行完整可见）。
    // ⚠️ 本面板首次换内核，插件桥回喊链（VoicePickerBus）理论无影响（MBS 内部同样是
    // Dialog 窗口语义），实机仍按验点过一遍：换声确认 → JS 变量/朗读生效。
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        // 防误关（目目 09-18）：底部弹窗里划列表选发音人，手势稍带下就整面板被拖走关掉。
        // 禁掉 Hidden 终点 ⇒ 下滑只回弹不关；关闭仍走 ✕ / 点遮罩 / 返回键三条路
        confirmValueChange = { it != SheetValue.Hidden },
    )
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = null,
        // 系统栏间距自己管（内层 Column navigationBarsPadding），避免默认 insets 叠加双份
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                // 92%：09-18 上调（原 09-08 定稿 88%）——底部「确认」动作行占了 ~52dp，
                // 上调 4% 找平，列表实际面积只比旧无底栏版少 ~20dp（半行不到）。
                // 不按条数自适应：搜索过滤会让条数跨阈值、面板跟着跳档，正是恒定档位
                // 当初要杀的抖动（见类注释「固定档位」一段）
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
                // 键盘让位：换声区一弹键盘，候选列表下半截会被盖住
                .imePadding()
                // 面板本体先吃掉落在空白处的点击，防止误触 scrim 关窗
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {}
        ) {
                    // 拖拽把（「做成底部弹窗的样式」）：M3 底部弹窗的识别特征就是
                    // 顶部这条 4dp×32dp 抓手。09-18 起外壳已是真 ModalBottomSheet，但
                    // dragHandle=null 关掉官方拖拽、沿用这条自绘标记——面板下滑已被
                    // confirmValueChange 禁关（只回弹），这条纯装饰不再有误关风险；
                    // 插件桥回喊链靠 Dialog 窗口语义，勿改 Activity。
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(width = 32.dp, height = 4.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(2.dp),
                                )
                        )
                    }
                    // 紧凑标题行：一行「标题 + ✕」，宽度与内容对齐。
                    // 原 TopAppBar 是 M3 一级页面语汇（64dp 通栏 + 22sp 大标题 + 通栏分割线），
                    // 弹窗借来用会读成「App 的一个页面」，且它与底栏相距一屏、把内容夹在中间。
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // 角色卡形态（终版，三易其稿：标题右侧→小字行→标题本身）：
                            // 带角色名时标题显示「角色卡（角色名）」——「角色卡」明说身份，名字绿色加粗
                            // 与「最终」行呼应；无角色名（旁白/本地音效槽位等）统一叫「信息卡」，与角色卡成对。
                            // 调用方自传标题已废除；超长省略号截断，标题槽单行不被挤。
                            // 添加角色模式：名字来自弹窗内填写框实时跟随，未输入时显示「添加角色」
                            val titleBadgeLive = when {
                                titleBadge.isNotBlank() -> titleBadge
                                addMode && inputName.isNotBlank() -> inputName.trim()
                                else -> ""
                            }
                            if (titleBadgeLive.isNotBlank()) {
                                // 模板走 R.string（三处同写），名字段做主色加粗 span
                                val tpl = stringResource(R.string.voice_picker_role_card)
                                val open = tpl.substringBefore("%1\$s")
                                val close = tpl.substringAfter("%1\$s")
                                Text(
                                    buildAnnotatedString {
                                        append(open)
                                        withStyle(
                                            SpanStyle(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        ) {
                                            append(titleBadgeLive)
                                        }
                                        append(close)
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            } else if (addMode) {
                                Text(stringResource(R.string.role_add_char_title))
                            } else {
                                Text(stringResource(R.string.voice_picker_info_card))
                            }
                        }
                        // 确认键挪进标题行（拍板方案 A）：底栏贴面板底缘，Dialog 窗口
                        // 拿不到导航栏 insets，两轮修复（窗口真实可用高定高 / 20dp 保底间隙）都压不住
                        // 「确认键被裁」，顶部锚定结构性免疫。仅换声区显示（音频参数各块自带
                        // 重置/应用，不需要统一确认）；✕ / 点遮罩=取消，原底部「取消」不再重复出现
                        // 关闭键：全屏对话框规范里导航位只用 ✕（不用 ←，← 会暗示「保存后返回」）；
                        // 底部面板同理——关闭走 ✕ / 点遮罩，不设「返回」语义。
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.cancel)
                            )
                        }
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
            Column(
                Modifier
                    .fillMaxWidth()
                    // 面板定高后内容区吃掉整块剩余高度（weight）——短内容的那片留白落在"内容区
                    // 之内"，底栏仍贴面板底；若沿用 heightIn(max) + 内容自然高，底栏会跟着短内容
                    // 浮到面板中间、下面空一块，比留白更难看
                    .weight(1f)
                    // 左右统一 16dp：顶部 / 换声区 / 音频参数区共用一条左边线
                    .padding(horizontal = 16.dp)
                    // ⚠️ 本区**不再**整体 verticalScroll（重构）：原来它挂着一层
                    // verticalScroll、候选列表自己又挂一层 → 两层嵌套滚动，手势互相抢。表现是
                    // ①上滑先滚列表，列表只有外层分给它的那点高度（约 5 行）＝「上滑空间太小」；
                    // ②列表滚到底后手势链到外层，把标题以下的头部整块卷出视野＝「上方都隐藏了」。
                    // 现在改成"定头 + 单滚动"：本 Column 不滚，只有候选列表（或音频参数段）
                    // 用 weight(1f) 吃满剩余高度并自带内滚，头部四层永远在视野里。
            ) {
            // 添加角色模式：顶部第一行=角色名填写框（描边=可输入，与搜索框同语汇）
            // 释放模式不显示（名字已定，标题「角色卡（名字）」已表明释放对象）
            if (addMode && releaseOwnerName.isBlank()) {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text(stringResource(R.string.role_add_char_input)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ===== 顶部块（用户 09-09 重排）：当前发音人 + 试听 + 终值 =====
            // 发音人名跟随暂存选择（09-10 参数跟随）：暂存了候选就先显示候选对应的配置项名
            val boundConfigName = remember(entity.id, boundVoice, pendingVoice, dataVersion, rowVersion) {
                if (isBindingMode) {
                    val tag = pendingVoice ?: boundVoice
                    enabledConfigEntityByTag(tag)?.displayName ?: tag
                } else ""
            }
            val pendingName = pendingVoice
                ?.takeIf { !isBindingMode }
                ?.let { narrationEntityByVoice(it)?.displayName }
            val currentVoiceName = if (isBindingMode) boundConfigName
            else pendingName ?: appliedDisplayName ?: entity.displayName
            // 顶部当前发音人：点亮标记 emoji 跟在名字后（用户 09-12 晚拍板"放后面"）——
            // 键与候选行同口径：绑定=绑定/暂存的 tag，非绑定=当前 voice；未点亮不占位。
            // 09-17 加 ⋮ 后非绑定并入暂存：顶部显示的名字本就跟随暂存候选（currentVoiceName 的
            // pendingName 一支），标记若还盯着已落库的 voice，暂存着 B 却把标记打到 A 上——
            // ⋮ 一出现，这个不一致就从"看不见"变成"点得到"，故与显示名统一取暂存优先。
            val topMarkKey = if (isBindingMode) (pendingVoice ?: boundVoice)
            else pendingVoice ?: voice
            val topMarks = remember(topMarkKey, marksVersion) {
                if (topMarkKey.isBlank()) emptyList()
                else VoiceMarksFile.get(config.speechRule.tagRuleId, topMarkKey)
            }
            // 顶部 ⋮ 的删除目标（用户 09-17 方案A）：与上面「当前发音人」的显示名**同源**——
            // 显示谁就删谁。绑定=当前绑定/暂存标签的启用配置项（与候选行 ⋮、与显示名同一口径）；
            // 非绑定=暂存候选那条配置项，无暂存则本面板这条。
            // 锚点已被删且无接替时（anchorMissing）本配置项已不在库，不给删——菜单项 disabled。
            val topDeleteTarget = if (isBindingMode) enabledConfigEntityByTag(pendingVoice ?: boundVoice)
            else pendingVoice?.let { narrationEntityByVoice(it) } ?: entity.takeIf { !anchorMissing }
            // 09-11 重排（用户拍板）：「当前发音人」小标签独占一行，▶ 键与发音人名同一行
            //（此前 ▶ 垂直居中在两行文字块上，与名字行错位）；名字加省略号防长名硬裁
            if (!addMode) {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    // 标题已改叫「角色卡（角色名）」（终版），本行回归纯「当前发音人」
                    "当前发音人",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 名字+标记包一层 weight(1f)：名字左对齐可省略、▶ 仍钉在行尾（不因标记数量漂移）
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            currentVoiceName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        val topEmoji = VoiceMarksFile.emojiOf(topMarks)
                        if (topEmoji.isNotEmpty()) {
                            Text(
                                topEmoji,
                                modifier = Modifier.padding(start = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    // ▶ 同候选行方案A：裸字符可点替代 TextButton（单字符占 58dp 底座，顶栏紧巴巴），
                    // 16dp 字形+两侧 12dp ≈40dp，上下 12dp 凑满 48dp 触控高；09-18 end 4dp 与候选行同距
                    Text(
                        previewLabel(PREVIEW_KEY_CURRENT),
                        color = previewLabelColor(PREVIEW_KEY_CURRENT),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable {
                            // 试听当前声音（09-10 参数跟随）：绑定模式=跟随目标+草稿；
                            // 旁白=本配置项+暂存voice+草稿；播放中/合成中再点=停止复位（角色管理同款交互）
                            if (previewingKey == PREVIEW_KEY_CURRENT && previewState != PreviewState.IDLE) {
                                TaggedTtsPreviewPlayer.stop()
                                previewingKey = null
                                return@clickable
                            }
                            previewingKey = PREVIEW_KEY_CURRENT
                            scope.launch {
                                val target = if (isBindingMode) draftParamsTarget() else null
                                // 音效槽位用专用试听文本（与全局/编辑页音效文本同源，用户 09-13）；其余照旧固定句
                                val text = if (isLocalSoundSlot)
                                    AppConfig.localSoundSampleText.value.ifBlank { "你好，这是试听语音。" }
                                else "你好，这是试听语音。"
                                TaggedTtsPreviewPlayer.play(
                                    context, target ?: draftEntity(pendingVoice), text,
                                    // 插件/全局层草稿全覆盖（用户 09-17）：调滑杆即听，
                                    // 不必先点应用；插件无实体时传 null 保住库值不被 1.0 抹掉
                                    pluginParamsOverride = plugin?.audioParams?.copy(
                                        speed = snapParam(pluginSpeed),
                                        volume = snapParam(pluginVolume),
                                        pitch = snapParam(pluginPitch),
                                    ),
                                    globalParamsOverride = AudioParams(
                                        speed = snapParam(globalSpeed),
                                        volume = snapParam(globalVolume),
                                        pitch = snapParam(globalPitch),
                                    ),
                                )
                            }
                        },
                    )
                    // ⋮ 菜单（用户 09-17 方案A）：打开面板想删当前这条配置项，得关掉面板回列表翻半天，
                    // 就地给一个入口。菜单项与候选行同源（标记 + 删除配置项），零学习成本。
                    // 宽度账：360dp 屏 − 面板左右 32dp − ▶ 40dp − ⋮ 48dp ⇒ 名字区仍有约 240dp，单行省略
                    // 09-18 offset 12dp：⋮ 与候选行、标题行 ✕ 共用一条 16dp 右缘线（目目嫌 ⋮ 偏里）
                    VoiceOverflowMenu(
                        marks = topMarks,
                        onToggleMark = { mark ->
                            if (topMarkKey.isNotBlank() &&
                                VoiceMarksFile.toggle(config.speechRule.tagRuleId, topMarkKey, mark)
                            ) {
                                // 标记走文件通道、无观察者：版本号自增驱动本行 emoji 与候选行重读
                                marksVersion++
                                onChanged?.invoke("marked", topMarkKey)
                                if (sharedVM != null) sharedVM.voiceMarksVersion.value += 1
                            }
                        },
                        deleteEnabled = topDeleteTarget != null,
                        onDelete = { topDeleteTarget?.let { deleteConfirmTarget = it } },
                        modifier = Modifier.offset(x = 12.dp),
                    )
                }
            }

            // ===== 终值（播放链同源三层乘积；三维恒显，用户 09-10）=====
            // 最终值恒为 配置×插件×全局（三层草稿实时跟随；音高 09-10 起同规格走草稿；
            // 接管判定已废除，恒乘积）
            val finalSpeed = speed * pluginSpeed * globalSpeed
            val finalVolume = volume * pluginVolume * globalVolume
            val finalPitch = pitch * pluginPitch * globalPitch
            Text(
                // 与卡片参数行口径不同（用户 09-10 二稿）：
                // 卡片=管道+1 位+加粗，弹窗/面板=逗号+2 位+无后缀（删除 x 乘号，与日志/试听保持一致）
                text = stringResource(
                    R.string.audio_params_final,
                    finalSpeed.toParamText(),
                    finalVolume.toParamText(),
                    finalPitch.toParamText(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                // 上下各 2dp（用户 09-11：顶部三行是紧密信息，行距收小，与音频参数弹窗同步）
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
            )
            } // if (!addMode) 顶部块收尾

            // 分段两区（用户 09-11 下午「完全 MD3 版」终裁：官方 SegmentedButton 全 app 统一）：
            // 0=更换发音人 1=音频参数；当前发音人+终值两区共用，固定在分段之上。
            // 09-11 晚：官方 SegmentedButton 不支持"宽度随文字收缩"（文字被压成省略号），
            // 撤 equalWidth=false，随组件默认均分撑满
            SoftSegmentedTextToggle(
                options = listOf("更换发音人", "音频参数"),
                selectedIndex = panelTab,
                onSelect = { panelTab = it },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally),
            )

            if (panelTab == 0 && source != null) {
                // 分类定稿（用户 09-09 下拉版顺序）：全部 + 女/男系列（旁白不下拉，见下）；
                // 提取方式参照标签分类（剥尾部数字取汉字前缀），前缀在白名单内才可被分类筛选命中；
                // 对话/括号/本地音效/角色名等不入分类；「全部」=不筛选
                val voiceCategories = listOf(
                    "女青年", "男青年", "女中年", "男中年", "女老年", "男老年",
                    "少女", "少年", "女童", "男童", "女主", "男主", "特殊女", "特殊男",
                )

                fun voiceCategoryOf(tagName: String): String? {
                    val base = Regex("^(.*[\\u4e00-\\u9fa5])\\d{0,4}$").find(tagName)
                        ?.groupValues?.getOrNull(1) ?: tagName.takeIf { it.isNotBlank() } ?: return null
                    return when {
                        base == "旁白" -> "旁白"
                        base in voiceCategories -> base
                        else -> null
                    }
                }

                // 候选键：""=全部（不筛选）；选项与展示名在绑定模式内按当前范围动态生成
                //（09-11 晚起 0 项分类被隐藏，见下），此处不再静态定义。

                if (isBindingMode) {
                    // ===== 绑定模式：下拉定范围 + 常驻搜索（范围内）+ 列表（行内试听）=====
                    var selectedCategory by remember(entity.id) {
                        // 默认选中配置项当前标签所属分类（「女青年25」→「女青年」；九类外如「括号1」→全部）
                        mutableStateOf(voiceCategoryOf(config.speechRule.tagName))
                    }
                    var tagSearch by remember(entity.id) { mutableStateOf("") }
                    // 改绑到无启用配置的标签会掉进随机兜底，读声不可控，必须排除
                    // （用户 09-12 定稿：池子/记录存的都是 tag id，启用标签集合只收 tag，
                    //   tagName 兜底已移除——全链只认 tag）
                    val enabledTags = remember(entity.id, dataVersion) {
                        dbm.systemTtsV2.getAllGroupWithTts().flatMap { it.list }
                            .filter { it.isEnabled }
                            .mapNotNull {
                                (it.config as? TtsConfigurationDTO)?.speechRule?.tag
                                    ?.takeIf { t -> t.isNotEmpty() }
                            }.toMutableSet()
                    }
                    // 候选来源分两类：
                    // - 角色槽位：发音人池 fayinren.json ∩ 启用标签（池子只装 GENSHIN 音色标签）
                    // - 本地音效槽位：**不能**用池子——规则 detectAvailableVoices 只遍历 GENSHIN_CHARACTERS
                    //   （localSound 前缀不在其中），音效标签根本进不了池子，取出来全是 TTS 音色。
                    //   改为直接在启用配置里枚举同族槽位（tag=localSoundN），即"本槽位可借用哪些音效槽位的配置"
                    val poolEnabled = if (isLocalSoundSlot) {
                        remember(entity.id, dataVersion) { enabledTags.filter { LOCAL_SOUND_TAG.matches(it) } }
                    } else {
                        CharacterRecordsFile.readVoicePool(config.speechRule.tagRuleId)
                            .filter { it in enabledTags }
                    }
                    // 下拉项带括号项数（不含搜索过滤，选分类前就知道各范围有多少可选）；
                    // 0 项分类直接隐藏（用户 09-11 晚改，替代 09-09「不标数量」——不标会被误读成
                    // 信息缺失，没货的分类干脆不列）；「全部」恒在首位。
                    // 当前选中分类若恰好 0 项，AppSpinner 会自动回落到「全部」（values 不含当前值时
                    // 回调第一项），与既有「九类外标签→全部」语义一致，不会留下幽灵值。
                    val categoryCounts = poolEnabled.groupingBy { voiceCategoryOf(it) }.eachCount()
                    val categoryOptions: List<Pair<String, String>> =
                        listOf("" to "全部") +
                            voiceCategories.filter { (categoryCounts[it] ?: 0) > 0 }.map { it to it }
                    val categoryEntries = categoryOptions.map { (key, label) ->
                        val n = if (key.isEmpty()) poolEnabled.size else categoryCounts[key] ?: 0
                        "$label（${n}项）"
                    }
                    // 当前选中分类若恰好 0 项（该分类已被隐藏），视同「全部」——
                    // 原 AppSpinner 会自动回落到第一项（见下），换状态条后由这里兜底，语义不变
                    val effectiveCategory = selectedCategory?.takeIf { key ->
                        categoryOptions.any { it.first == key }
                    }
                    // 音效槽位：候选只有同族 localSound 槽位（通常 1~N 条），
                    // 音色分类与搜索都无意义（分类表里音效恒落 null → 只有「全部（N项）」一项）
                    // → 下拉与搜索框整块隐藏，列表直接铺满
                    if (!isLocalSoundSlot) {
                        // 分类字段 + 搜索框并作一行：原来分类条与搜索框纵向各占
                        // 一行（48 + 56dp），白吃一行高度；并排后一行 56dp 收住，分类名也仍在视野里。
                        // 两者**同高（56dp）、同语境（都是字段）**：左=筛选条件、右=输入；分类灰底无框、
                        // 搜索描边无底，靠"填色 / 描边"这一对区分"选"与"输"，不靠形状家族硬区分
                        var categoryPickerOpen by remember(entity.id) { mutableStateOf(false) }
                        if (categoryPickerOpen) {
                            AppSelectionDialog(
                                onDismissRequest = { categoryPickerOpen = false },
                                title = { Text(stringResource(R.string.voice_picker_category_current)) },
                                value = effectiveCategory ?: "",
                                values = categoryOptions.map { it.first },
                                entries = categoryEntries,
                                // 分类最多十几个，不需要搜索框（默认 >5 项就带）
                                searchEnabled = false,
                                onClick = { key, _ ->
                                    selectedCategory = (key as? String)?.takeIf { it.isNotEmpty() }
                                    categoryPickerOpen = false
                                },
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            // chip 48dp、搜索框 56dp，spacedBy 让两者间距恒 8dp 不随名长漂移
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CategoryChip(
                                value = effectiveCategory ?: "全部",
                                switchable = true,
                                onClick = { categoryPickerOpen = true },
                            )
                            OutlinedTextField(
                                modifier = Modifier.weight(1f),
                                // 提示用 placeholder 不用 label（用户 09-11）：label 中态官方写死
                                // bodyLarge 16sp，观感比旁边内容大一圈；代价是输入后提示消失（可接受）
                                // 字号显式钉 bodyMedium 14sp（用户 09-13）：依赖 LocalTextStyle 继承时
                                // 此框落到 16sp、比分类名(14sp)大一号（老问题复发：面板弹窗
                                // 正文槽并非恒 14sp），显式指定与 chip 对齐。
                                // 09-13 二次修：textStyle 只作用于输入文字，placeholder 的 Text 不吃它、
                                // 仍走 LocalTextStyle(16sp)，必须给 placeholder 单独钉 style
                                textStyle = MaterialTheme.typography.bodyMedium,
                                placeholder = {
                                    Text(
                                        stringResource(R.string.search_tag_or_display_name),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                value = tagSearch,
                                onValueChange = { tagSearch = it },
                                singleLine = true,
                                // 描边淡化：默认未聚焦描边是 outline（与主色同族、
                                // 比旁边那块分类灰底重一档，并排看着刺眼）。降一档到 outlineVariant，
                                // 只留「描边=可输入」的语义、不再抢旁边的分类字段；聚焦态仍回 outline，
                                // 保留"正在输入"的反馈（光标本身另有一层反馈）
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                ),
                            )
                        }
                    } else {
                        // 只读态：音效槽位大分类取 rule.tags 现查（剥尾号→「本地音效」），
                        // 不可切、无搜索 → 无底无框无箭头，纯文字信息，不会被当成可点项
                        CategoryChip(value = displayCategory)
                    }
                    // 搜索词同时匹配「标签名」与「配置项名」（用户 09-11 晚：记忆里是"女青年01晓晓"，
                    // 原先只匹配标签名，搜"晓晓"搜不到）。候选行现在只显示配置项名（09-13 去序号），
                    // 但按标签名搜仍应命中，故匹配范围保持两者并集。
                    // 音效槽位没有搜索控件（上方已隐藏），直接全量出列
                    val filtered = if (isLocalSoundSlot) {
                        poolEnabled
                    } else {
                        poolEnabled.filter { tag ->
                            (effectiveCategory == null || voiceCategoryOf(tag) == effectiveCategory) &&
                                (tagSearch.isBlank() ||
                                    tag.contains(tagSearch) ||
                                    enabledConfigEntityByTag(tag)?.displayName?.contains(tagSearch) == true)
                        }
                    }
                    // 候选列表 = 候选池本身（用户 09-17）：不再把「当前绑定」补到列表顶部。
                    // 顶栏已经展示当前发音人（含 ▶ 试听与 ⋮ 菜单），列表首位该留给候选项，
                    // 再用一条重复信息占头部只会让人一眼看到的还是"已经在用的那个"。
                    // 当前绑定若不在池内（标签被停用等），就只从顶栏看它、从顶栏的 ⋮ 处理

                    // 候选列表：点行=暂存改绑；▶=只试听该标签对应的启用配置（不应用）。
                    // weight(1f) 吃满头部以下的剩余高度并自带内滚（09-14 晚重构：原来上限写死
                    // 55% 屏高、且整个内容区还挂着外层 verticalScroll → 列表只分到约 5 行高度）
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 6.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        if (filtered.isEmpty()) {
                            Text(
                                "该范围内没有可用的标签",
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        // 重名兜底（去序号后）：显示名在本轮候选里撞车才括号补回标签，
                        // 正常情况一个字不多（一标签一启用，显示名基本不重）
                        val dupNames = filtered.groupingBy { t ->
                            enabledConfigEntityByTag(t)?.displayName?.ifEmpty { null }
                                ?: if (isLocalSoundSlot) localSoundSlotLabel(t) else t
                        }.eachCount()
                        // 占用表（方案A）：characterRecords.json 里 voice→角色名列表，
                        // 与角色管理插件「已分配」徽章同源同口径；排除自己（bindingKey）——
                        // 自己当前绑定的那行已有 ✓ 主色，不重复标
                        val voiceOwners = remember(entity.id, dataVersion, rowVersion) {
                            CharacterRecordsFile.readVoiceOwnerMap(config.speechRule.tagRuleId)
                        }
                        filtered.forEach { tag ->
                            val isCurrent = tag == boundVoice
                            val isPending = tag == pendingVoice
                            // 候选行=纯配置项显示名（定稿：序号/标签不进行内，
                            // 大类由上方分类框表达）；无显示名回落：音效槽位→「本地音效N」，其余→tag
                            // 候选池已筛 fayinren.json∩启用配置（tag id 口径），用 enabledConfigEntityByTag 即可取到 displayName
                            val cfgName = enabledConfigEntityByTag(tag)?.displayName.orEmpty()
                            val rowName = if (isLocalSoundSlot) localSoundSlotLabel(tag) else tag
                            val rowText = if (dupNames.getOrDefault(cfgName.ifEmpty { rowName }, 0) > 1)
                                "${cfgName.ifEmpty { rowName }}（$rowName）" else cfgName.ifEmpty { rowName }
                            // 标记/删除收进 ⋮ 菜单（用户 09-12 晚定稿紧凑化：行内塞 5 键把名字挤没）；
                            // 点亮标记由 CandidateRow 渲染在名字后
                            val rowMarks = remember(tag, marksVersion) {
                                VoiceMarksFile.get(config.speechRule.tagRuleId, tag)
                            }
                            // 已被其他角色占用 → 行内「已用」徽章（占用≠禁用，仍可点选改绑）
                            val usedByOthers = voiceOwners[tag].orEmpty().any { it != bindingKey }
                            CandidateRow(
                                text = rowText,
                                isCurrent = isCurrent,
                                usedBadge = usedByOthers,
                                // 当前项染主色与候选行同口径（用户 09-13：图二绑定行当前项是黑的、
                                // 图一非绑定行是绿的，两处不一致 → 统一 current/pending 都主色；
                                // 09-18 目目：行内 ● 圆点标记废除，选中态=主色字，当前绑定另有 ✓）
                                nameColor = if (isPending || isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                onClick = {
                                    // 两段式（用户 09-08）：点行=暂存选中，底部「确认」才落库。
                                    // 点到当前绑定的那一行时行内 ✓ 不会变、颜色也不变，
                                    // 看不出任何反应 → 补一句 Toast 说明，别让人以为点坏了
                                    // （「选了角色无法确认」的来源之一）
                                    if (tag == boundVoice) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.voice_picker_same_voice),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                    pendingVoice = tag
                                },
                                // 音效槽位也渲染试听键（加回：也有自定义配置项的本地音效，
                                // 试听文本走 localSoundSampleText 专用轨，与顶部 ▶ 同源）
                                previewText = previewLabel(tag),
                                previewColor = previewLabelColor(tag),
                                onPreview = {
                                    // 行内试听：播放该标签对应启用配置的声音，不应用
                                    if (previewingKey == tag && previewState != PreviewState.IDLE) {
                                        TaggedTtsPreviewPlayer.stop()
                                        previewingKey = null
                                    } else {
                                        previewingKey = tag
                                        scope.launch {
                                            val target = withIO { enabledConfigEntityByTag(tag) }
                                            if (target != null) {
                                                // 音效槽位用专用试听文本（双轨，用户 09-13）；其余固定句
                                                val text = if (isLocalSoundSlot)
                                                    AppConfig.localSoundSampleText.value.ifBlank { "你好，这是试听语音。" }
                                                else "你好，这是试听语音。"
                                                TaggedTtsPreviewPlayer.play(context, target, text)
                                            } else {
                                                previewingKey = null
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.log_panel_rebind_no_config),
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            }
                                        }
                                    }
                                },
                                marks = rowMarks,
                                onToggleMark = { mark ->
                                    if (VoiceMarksFile.toggle(config.speechRule.tagRuleId, tag, mark)) {
                                        marksVersion++
                                        onChanged?.invoke("marked", tag)
                                        // 主列表标记显示同步刷新（文件通道无观察者，靠版本号驱动重组）
                                        if (sharedVM != null) sharedVM.voiceMarksVersion.value += 1
                                    }
                                },
                                // 删除：只删这条**启用**配置项（同标签的禁用残留不动）；
                                // 该标签再无启用配置时才从标签池移除
                                deleteEnabled = cfgName.isNotEmpty(),
                                onDelete = {
                                    deleteConfirmTarget = enabledConfigEntityByTag(tag)
                                },
                            )
                        }
                    }
                } else {
                    // ===== 非绑定换声（用户 09-12 定稿）：按本配置项的 **tag（id）** 列同标签候选 =====
                    // 候选**故意列全量配置（含禁用）**——正常配置一个标签只启用一条，
                    // 禁用条只是"可借用的发音人来源"；点行=暂存选中，底部「确认」改写**那一条启用配置**
                    // 的 voice → 该标签后续片段确定全换（引擎侧同 tag 随机只是兜底，非设计意图）；
                    // 旁白/对话(duihua)/括号/本地音效全部天然按此归组，无需特殊分支；
                    // 最早那版报"旁白分类没有可用的配置项"，根因是拿显示名"旁白"去比 id（narration）。
                    // 落库后主列表自动定位高亮被改项（sharedVM.pendingLocateConfigId）
                    val currentTagId = config.speechRule.tag
                    val currentTagName = config.speechRule.tagName
                    // 常驻搜索（补）：非绑定类（旁白/对话/括号…）原先只有一枚只读分类
                    // chip、下面直接就是候选行——无搜索可筛，几十条候选只能靠手翻。同标签候选
                    // 往往比绑定类更多（一个 tag 下每条配置项都是一个发音人），搜索必须补齐。
                    // 语汇与绑定类完全一致：左=分类（只读，填色语义）、右=搜索（描边语义），同高 56dp
                    var narrationSearch by remember(entity.id) { mutableStateOf("") }
                    // 只读态：非绑定类（旁白/对话/括号…）**没有大类可切**，分类取 rule.tags
                    // 现查（面板顶部 displayCategory，剥尾号）——无底无框无箭头、纯文字信息；
                    // 候选行因此不再重复带标签前缀
                    if (!isLocalSoundSlot) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            // chip 48dp、搜索框 56dp，spacedBy 让两者间距恒 8dp（与绑定模式同行口径）
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CategoryChip(value = displayCategory)
                            OutlinedTextField(
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                placeholder = {
                                    // 这里用泛化「搜索」而非绑定模式那句「搜索标签/显示名」：
                                    // 非绑定类的标签是固定的（旁白/对话/括号…），可搜的只有**配置项名**
                                    // 与发音人 id，写「标签」会让人以为能跨标签搜
                                    Text(
                                        stringResource(R.string.search),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                value = narrationSearch,
                                onValueChange = { narrationSearch = it },
                                singleLine = true,
                                // 描边淡化（同绑定模式）：未聚焦 outlineVariant、聚焦回 outline
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                ),
                            )
                        }
                    } else {
                        // 音效槽位（同族 localSound，通常 1~N 条）：搜索无意义，与绑定模式同一口径
                        CategoryChip(value = displayCategory)
                    }
                    val narrationCandidates = remember(entity.id, currentTagId, dataVersion, rowVersion) {
                        allConfigs.mapNotNull { c ->
                            val dto = c.config as? TtsConfigurationDTO ?: return@mapNotNull null
                            if (dto.speechRule.tag != currentTagId) return@mapNotNull null
                            val v = (dto.source as? PluginTtsSource)?.voice
                                ?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                            // 带实体出列：候选行要用它的 displayName / 试听 / 删除（09-12 全分类补齐）
                            Pair(v, c)
                        }.distinctBy { it.first }
                    }
                    // 搜索过滤（补）：命中「配置项名」或「发音人 id」——同绑定模式
                    // 「标签名 / 配置项名并集」的思路，只是这里一行对应一条配置项，按发音人 id 搜也应命中
                    val narrationShown = if (narrationSearch.isBlank()) {
                        narrationCandidates
                    } else {
                        narrationCandidates.filter { (v, cfg) ->
                            v.contains(narrationSearch) ||
                                cfg.displayName.contains(narrationSearch)
                        }
                    }
                    // 候选列表（非绑定）：weight(1f) 吃满头部以下的剩余高度并自带内滚
                    //（09-14 晚重构：原来上限写死 55% 屏高、外层又挂着一层 scroll，列表只见约 5 行）
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 6.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        if (narrationCandidates.isEmpty()) {
                            Text(
                                if (currentTagName.isBlank()) "该标签下没有可用的配置项"
                                else "「$currentTagName」标签下没有可用的配置项",
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else if (narrationShown.isEmpty()) {
                            // 搜出来的空态与"本标签下没有候选"要分开说：否则清空搜索前会被读成
                            // "这个标签坏了"（本标签明明有 N 条，只是没匹配上）
                            Text(
                                "没有匹配「${narrationSearch.trim()}」的配置项",
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        narrationShown.forEach { (v, cfgEntity) ->
                            val isCurrent = v == voice && voice.isNotEmpty()
                            val isPending = v == pendingVoice
                            // 标记键=voice（同一 tag 下多条配置靠 voice 区分；与角色行 tag 键同口径）
                            val rowMarks = remember(v, marksVersion) {
                                VoiceMarksFile.get(config.speechRule.tagRuleId, v)
                            }
                            CandidateRow(
                                text = cfgEntity.displayName,
                                isCurrent = isCurrent,
                                nameColor = if (isPending && !isCurrent) MaterialTheme.colorScheme.primary
                                else if (isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                onClick = {
                                    // 两段式（用户 09-08）：点行=暂存选中，底部「确认」才写配置项 voice
                                    pendingVoice = v
                                },
                                previewText = previewLabel(v),
                                previewColor = previewLabelColor(v),
                                onPreview = {
                                    if (previewingKey == v && previewState != PreviewState.IDLE) {
                                        TaggedTtsPreviewPlayer.stop()
                                        previewingKey = null
                                    } else {
                                        previewingKey = v
                                        TaggedTtsPreviewPlayer.play(
                                            context, cfgEntity, "你好，这是试听语音。"
                                        )
                                    }
                                },
                                marks = rowMarks,
                                onToggleMark = { mark ->
                                    if (VoiceMarksFile.toggle(config.speechRule.tagRuleId, v, mark)) {
                                        marksVersion++
                                        onChanged?.invoke("marked", v)
                                        // 主列表标记显示同步刷新（文件通道无观察者，靠版本号驱动重组）
                                        if (sharedVM != null) sharedVM.voiceMarksVersion.value += 1
                                    }
                                },
                                // 删除：同上，只删你点的这一条配置项
                                deleteEnabled = true,
                                onDelete = { deleteConfirmTarget = cfgEntity },
                            )
                        }
                    }
                }
            }

            // ===== 音频参数大区（分段第二区；用户 09-10 改按维度：一次调一个维度的三层）=====
            if (panelTab == 1) {
                // 左右 4dp 已上移到整个内容 Column（用户 09-11：全面板统一 16dp）；
                // 底部无按钮行，补 4dp 底边距与左右一致收尾。
                // 09-14 晚重构：外层内容 Column 不再整体滚动，本段必须自己吃满剩余高度并自带内滚，
                // 否则内容一长就被裁掉（原来靠外层那层 scroll 兜着）
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 4.dp)
                ) {
                    // 顶部那条 HorizontalDivider 已撤（用户 09-10 晚）：第二级换成软槽分段后，
                    // 分割线与"两区切换 + 软槽"的层次重复，撤掉更干净
                    // 三层现值总览行已撤（用户 09-10 ④）：顶部终值行足够，每维三层滑杆同屏可见；
                    // 按维度分段（语速/音量/音高，共用组件），滑杆层标签=配置/插件/全局，
                    // 接管判定已废除（09-10）：三层恒显示可调
                    val targetDto = paramsTarget.config as? TtsConfigurationDTO
                    val hasPluginLayer = (targetDto?.source as? PluginTtsSource) != null
                    AudioParamsDimensionSection(
                        hasPluginLayer = hasPluginLayer,
                        cfgSpeed = speed, onCfgSpeed = { speed = it; speedDirty = true },
                        cfgVolume = volume, onCfgVolume = { volume = it; volumeDirty = true },
                        cfgPitch = pitch, onCfgPitch = { pitch = it; pitchDirty = true },
                        pluginSpeed = pluginSpeed, onPluginSpeed = { pluginSpeed = it; speedDirty = true },
                        pluginVolume = pluginVolume, onPluginVolume = { pluginVolume = it; volumeDirty = true },
                        pluginPitch = pluginPitch, onPluginPitch = { pluginPitch = it; pitchDirty = true },
                        globalSpeed = globalSpeed, onGlobalSpeed = { globalSpeed = it; speedDirty = true },
                        globalVolume = globalVolume, onGlobalVolume = { globalVolume = it; volumeDirty = true },
                        globalPitch = globalPitch, onGlobalPitch = { globalPitch = it; pitchDirty = true },
                        isDirty = { when (it) { 0 -> speedDirty; 1 -> volumeDirty; else -> pitchDirty } },
                        onResetDim = { dim ->
                            when (dim) {
                                0 -> { speed = 1f; pluginSpeed = 1f; globalSpeed = 1f }
                                1 -> { volume = 1f; pluginVolume = 1f; globalVolume = 1f }
                                else -> { pitch = 1f; pluginPitch = 1f; globalPitch = 1f }
                            }
                        },
                        onApplyDim = { applyDim(it) },
                    )
                }
            }
            } // 内容 Column 收尾（本区不滚：只有候选列表/音频参数段自带内滚）

            // ---- 底部动作行：「确认」（09-18 定论：MBS 内核贴底动作行完整可见，
            // 确认键从标题行挪回底部——顶部确认难用）。仅换声区显示（音频参数各块自带
            // 重置/应用）；✕ / 点遮罩=取消
            if (panelTab == 0) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    // 动作键一律纯文字 TextButton（目目 09-17：不要框和填充色）
                    TextButton(
                        onClick = {
                            val selected = pendingVoice
                            if (selected == null) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.voice_picker_need_pick),
                                    Toast.LENGTH_SHORT,
                                ).show()
                                return@TextButton
                            }
                            if (addMode && inputName.isBlank()) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.voice_picker_need_name),
                                    Toast.LENGTH_SHORT,
                                ).show()
                                return@TextButton
                            }
                            // 锚点配置项已被删且同标签再无配置项：非绑定分支要写回该配置项，
                            // 落库会打空（更新不存在的行=静默无操作，却弹「已应用」）——拦住并说清。
                            // 绑定/添加分支改写 characterRecords 里的角色记录，不依赖锚点，照常放行。
                            if (anchorMissing && !isBindingMode && !addMode) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.voice_picker_anchor_deleted),
                                    Toast.LENGTH_SHORT,
                                ).show()
                                return@TextButton
                            }
                            when {
                                // 添加角色 / 释放并固定：都要在 characterRecords.json 里落一条
                                // voice=所选 tag id 的记录，成功后自动关弹窗。
                                // - 添加：addCharacter 建新记录，同名已存在返回 false（确认键已拦空名）
                                // - 释放并固定：releaseAndFix 先把该名字从原角色解绑、
                                //   再另立门户写记录；名字已定（不显示输入框），用户只需选发音人
                                addMode -> {
                                    val n = inputName.trim()
                                    val isRelease = releaseOwnerName.isNotBlank()
                                    // 发音人显示名口径与弹窗头部/候选行一致（同 tag 的启用配置项名）：
                                    // 不在 Toast 里抛 tag id（文案一律走 R.string 三处同写，
                                    // 这里原来硬编码中文、且原样打印 tag id）
                                    val shown = enabledConfigEntityByTag(selected)?.displayName ?: selected
                                    scope.launch {
                                        val ok = withIO {
                                            if (isRelease) CharacterRecordsFile.releaseAndFix(
                                                config.speechRule.tagRuleId, releaseOwnerName, n, selected,
                                            ) else CharacterRecordsFile.addCharacter(
                                                config.speechRule.tagRuleId, n, selected,
                                            )
                                        }
                                        if (ok) {
                                            onChanged?.invoke("applied", selected)
                                            onDismissRequest()
                                        }
                                        pendingVoice = null
                                        Toast.makeText(
                                            context,
                                            when {
                                                // 释放成功=「已释放并固定：名，发音人：X」
                                                ok && isRelease -> context.getString(
                                                    R.string.role_release_fixed_toast, n, shown,
                                                )
                                                // 新增成功=「角色卡（名）已换为 X」
                                                ok -> context.getString(R.string.role_add_char_ok, n, shown)
                                                // 释放失败（原角色/名字对不上）=通用失败；新增失败=角色名已存在
                                                isRelease -> context.getString(R.string.role_list_failed)
                                                else -> context.getString(R.string.role_add_char_exists, n, shown)
                                            },
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                                isBindingMode -> {
                                // 绑定模式：改写 characterRecords.json（与角色管理同文件同字段）；
                                // groupBindingKeys 非空=整组换声（内置角色列表组头入口，逐个 rebind 组内角色）
                                // 选的还是当前绑定的那条：原来静默 return（看着像点了没反应），
                                // 改给一句 Toast 说明——角色卡第一行自带 ✓，很容易点到它
                                if (selected == boundVoice) {
                                    pendingVoice = null
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.voice_picker_same_voice),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    return@TextButton
                                }
                                val targets = if (groupBindingKeys.isNotEmpty()) groupBindingKeys else listOf(bindingKey)
                                scope.launch {
                                    var okCount = 0
                                    withIO {
                                        targets.forEach { name ->
                                            if (CharacterRecordsFile.rebind(
                                                    config.speechRule.tagRuleId, name, selected,
                                                )
                                            ) okCount++
                                        }
                                    }
                                    val ok = okCount > 0
                                    if (ok) {
                                        boundVoice = selected
                                        onChanged?.invoke("applied", selected)
                                        // 用户 09-17：确认成功即关面板。原来的「确认后不关」
                                        // 是日志快捷面板时代（ed5a2c2 抽类时行为零变化带来的）
                                        // ——那会儿是「边看日志边连着改声」，现在换声是主入口，
                                        // 改完该退出去看角色列表/主列表
                                        onDismissRequest()
                                    }
                                    pendingVoice = null
                                    Toast.makeText(
                                        context,
                                        when {
                                            !ok -> context.getString(R.string.log_panel_rebind_failed)
                                            targets.size > 1 -> "已将 ${okCount}/${targets.size} 个角色换为 " +
                                                (if (isLocalSoundSlot) localSoundSlotLabel(selected) else selected)
                                            else -> "已将「$bindingKey」的发音人换为 " +
                                                (if (isLocalSoundSlot) localSoundSlotLabel(selected) else selected)
                                        },
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                                }
                                else -> {
                                    // 旁白/其他：写配置项 voice
                                    applyVoice(selected)
                                    pendingVoice = null
                                }
                            }
                        },
                    ) {
                        // ⚠️ 不用 enabled 灰键（「选了角色无法确认」）：灰键说不出为
                        // 什么是灰的，键在又按不动更像坏了。恒可点 + 前置条件各给一句 Toast
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        } // ModalBottomSheet 内容 Column 收尾
    } // ModalBottomSheet 收尾

    // 删除确认弹窗（⋮ 菜单 🗑 入口；深夜定稿：删除的粒度就是「你点的那一条配置项」，
    // 文案照插件 doDeleteVoiceAndReassign 口径、把「发音人」统一成「配置项」）：标题行=【tag - 显示名】，
    // 正文按该标签**是否已被角色占用**二选一。
    // 「已被分配」判定与候选行「已用」徽章同源（characterRecords.json 的 voice→角色名表）。
    deleteConfirmTarget?.let { delTarget ->
        val delDto = delTarget.config as? TtsConfigurationDTO
        val delTag = delDto?.speechRule?.tag.orEmpty()
        val tagLabel = ruleTags?.get(delTag) ?: delDto?.speechRule?.tagName.orEmpty()
        val tagShown = tagLabel.ifBlank { delTag }
        val assigned = remember(delTarget.id) {
            CharacterRecordsFile.readVoiceOwnerMap(config.speechRule.tagRuleId)[delTag]
                .orEmpty().isNotEmpty()
        }
        AlertDialog(
            onDismissRequest = { deleteConfirmTarget = null },
            title = { Text(stringResource(R.string.role_voice_del_title)) },
            text = {
                Text(
                    stringResource(R.string.role_voice_del_msg_title, tagShown, delTarget.displayName) +
                        "\n\n" +
                        stringResource(
                            if (assigned) R.string.role_voice_del_msg_used
                            else R.string.role_voice_del_msg_unused
                        ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteConfirmTarget = null
                        deletePreviewedConfig(delTarget)
                    },
                ) { Text(stringResource(R.string.role_voice_del_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmTarget = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/** 顶部「当前发音人」试听键的 previewingKey 哨兵（行键为 tag/voice 字符串，避撞） */
private const val PREVIEW_KEY_CURRENT = "‹current›"

/** 单次 LaunchedEffect 简写：key 变化时只执行一次 */
@Composable
private fun LaunchedEffectOnce(key: Any?, block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {
    androidx.compose.runtime.LaunchedEffect(key) { block() }
}

private fun snapParam(v: Float): Float = (kotlin.math.round(v * 100f) / 100f)

/**
 * 行末 ⋮ 菜单（候选行与顶部「当前发音人」行共用，用户 09-17 方案A）。
 *
 * 菜单项与候选行**完全同源**：❤️喜欢 / 🚶路人 / 😈坏人（多选 toggle，点亮行尾打勾）
 * + 分隔线 + 🗑 删除配置项。当前发音人行照搬这套语汇是为了零学习成本——用户已在候选行见过它；
 * 两处差异只在调用方给的标记键（顶部=topMarkKey，候选行=tag/voice）与删除目标。
 *
 * 标记项**点一次切一次、菜单不关**（可连点几个），点亮态即时反映到行内 emoji；
 * emoji 是彩色字形染不上色（与角色管理 v10 同款），故点亮态交给行尾勾。
 */
@Composable
private fun VoiceOverflowMenu(
    marks: List<String>,
    onToggleMark: (String) -> Unit,
    deleteEnabled: Boolean,
    onDelete: () -> Unit,
    // 行尾对齐用：IconButton 自带 12dp 图标内缩，行内再无右缘 padding 时图标右缘
    // 落在面板 16dp 内边距线上偏里 12dp；调用方传 offset 右移到与标题行 ✕ 同一条右缘线
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "更多操作")
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            VoiceMarksFile.MARK_ITEMS.forEach { (mark, emojiText, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    leadingIcon = { Text(emojiText) },
                    trailingIcon = {
                        if (mark in marks) Icon(Icons.Filled.Check, contentDescription = "已点亮")
                    },
                    // 多选：点一次切一次，菜单不关（可连点几个）；点亮态即时反映到行内 emoji
                    onClick = { onToggleMark(mark) },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("删除配置项", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                enabled = deleteEnabled,
                onClick = {
                    menuOpen = false
                    onDelete()
                },
            )
        }
    }
}

/**
 * 候选行（绑定/非绑定共用；用户 09-12 晚定稿紧凑化——原行内塞 ▶+3 标记+🗑 五键，窄屏把名字挤没）：
 * - 行内常驻：名字 + 点亮标记 emoji（跟在名字后）+ ▶ 试听 + ⋮；
 * - ⋮ 菜单：❤️喜欢 / 🚶路人 / 😈坏人（多选 toggle，点亮行尾打勾）+ 分隔线 + 🗑 删除配置项；
 *   emoji 是彩色字形染不上色（角色管理 v10 也是靠描边/勾表达点亮，非染色），故点亮态交给行尾勾，
 *   行内则只显示已点亮的 emoji；
 * - 抽共用组件的原因：两侧只差"标记键/删除目标"，行形态必须一致，免得以后同步维护两处。
 *   [nameColor] 由调用方给：绑定行与旁白行的选中色语义略有差异。
 */
@Composable
private fun CandidateRow(
    text: String,
    isCurrent: Boolean,
    nameColor: Color,
    onClick: () -> Unit,
    previewText: String,
    previewColor: Color,
    onPreview: () -> Unit,
    marks: List<String>,
    onToggleMark: (String) -> Unit,
    deleteEnabled: Boolean,
    onDelete: () -> Unit,
    // 该标签已被其他角色占用（方案A）：名字后标「已用」小徽章；
    // 仅绑定分支传 true，非绑定分支走默认 false 不显示
    usedBadge: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            // 水平 10→6dp（用户 09-13 方案A：行宽紧，省 8dp 给名字）；09-18 去 end——
            // 右缘让 ⋮ 的 IconButton 自身内缩 + offset 直接对齐面板 16dp 右缘线
            .padding(start = 6.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 名字+标记包一层 weight(1f)：操作键钉在行尾，不随标记数量漂移
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                (if (isCurrent) "✓ " else "") + text,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = nameColor,
            )
            if (usedBadge) {
                // 「已用」徽章（方案A）：主色淡底圆角 chip，10sp，与角色管理
                // 插件「已分配」徽章同语义；占用≠禁用，行仍可点选改绑
                Text(
                    "已用",
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            RoundedCornerShape(9.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
            val emoji = VoiceMarksFile.emojiOf(marks)
            if (emoji.isNotEmpty()) {
                Text(
                    emoji,
                    modifier = Modifier.padding(start = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        // ▶ 用裸字符可点替代 TextButton（用户 09-13 方案A）：TextButton 单字符却占 58dp
        // 按钮底座，缩成「16dp 字形+两侧 12dp」≈40dp 宽、上下 12dp 凑满 48dp 触控高；
        // 颜色沿用调用方（播放中=tertiary，默认走 LocalContentColor）。
        // 09-18 end 12→4：与行尾 ⋮ 的图标内缩叠出来曾隔 30dp，目目嫌远，收一半
        Text(
            previewText,
            color = previewColor,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .clickable(onClick = onPreview)
                .padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        )
        VoiceOverflowMenu(
            marks = marks,
            onToggleMark = onToggleMark,
            deleteEnabled = deleteEnabled,
            onDelete = onDelete,
            // 12dp = 抵消 IconButton 图标内缩，⋮ 与标题行 ✕ 共用一条 16dp 右缘线
            modifier = Modifier.offset(x = 12.dp),
        )
    }
}

/**
 * 本地音效槽位的两种形态：
 * - tagName：`本地音效N`（规则 tags 表把 localSoundN 映射成它）
 * - tag(id) ：`localSound1`~`localSound100`（规则循环注册；JReadConfigMigration 的
 *   RULE_SOUND_TAG_REGEX 同款）
 *
 * 为什么音效槽位不能读发音人池：池子 fayinren.json 由规则 detectAvailableVoices 生成，
 * 它只遍历 GENSHIN_CHARACTERS（音色标签），localSound 前缀不在其中，音效标签永远进不去池子
 * ——按池子取候选只会得到一堆 TTS 音色（09-13 截图实锤）。故音效槽位改从配置表枚举同族槽位。
 */
internal val LOCAL_SOUND_TAG_NAME = Regex("本地音效\\d*")

private val LOCAL_SOUND_TAG = Regex("^localSound\\d+$")

/** localSound7 → 本地音效7（候选行显示用，tagName 口径与规则 tags 表一致） */
private fun localSoundSlotLabel(tag: String): String =
    "本地音效" + tag.removePrefix("localSound")

/**
 * 大分类=标签显示名剥尾部数字（女青年01→女青年、本地音效1→本地音效）；
 * 显示名不带尾序号的整名独立成类（旁白、男、女、【】括号发音人等，括号系不合并）。
 * 与列表页标签两层弹窗的大分类同口径同来源（rule.tags 查显示名后剥尾号）。
 */
/**
 * 分类字段（终版，形态四易：输入框轮廓 → 全宽状态条 → 填色胶囊 → **灰底字段**）。
 *
 * 分类是**当前状态**、不是待输入项，故不用 OutlinedTextField 轮廓（跟旁边搜索框同形，分不出哪块
 * 是分类）；也不能用 `secondaryContainer` 填充——那个色在 MD3 里是 **SegmentedButton 选中态**的
 * 官方指定色，上面的「更换发音人/音频参数」正在用，分类再填就变成"第三个已选中的分段项"，
 * 与分段平级、层级被压平（察觉"都是胶囊形状、分类还填色，会不会混"）。
 * 故取**填色以外**的一种：`surfaceContainerHighest` 灰底 + 8dp 方角 + **56dp 与同行的搜索框等高**
 * ——读成"一对字段"（左=筛选条件，右=输入），填色在面板里只保留"分段选中"一个含义。
 *
 * switchable=true：尾部带下拉箭头（可切，点击弹 AppSelectionDialog），灰底字段形态；
 * false：**无底、无框、48dp 行高**（只读），纯文字信息，彻底不像 chip、不会被当成可点项。
 * 三处调用同源：绑定模式角色槽位（可切，与搜索框同行）、绑定模式音效槽位（只读）、
 * 旁白/对话/括号等非绑定类（只读）。
 */
@Composable
private fun CategoryChip(
    value: String,
    modifier: Modifier = Modifier,
    switchable: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier
            // 最小 104dp（仅可切版）：容得下 3 字分类名 + 图标 + 箭头，比它长的名字自然撑开；
            // 只读版无底无框、就是个文字标签，不需要最小宽（否则内容会被居中在一片看不见的
            // 104dp 里、凭空多出一段缩进）
            .then(if (switchable) Modifier.widthIn(min = 104.dp) else Modifier)
            // 可切版 56dp：与同一行的搜索框（M3 OutlinedTextField 默认 56dp）齐平，两件读成
            // "一对字段"；只读版 48dp 行高（与其他信息行同节奏）
            .height(if (switchable) 56.dp else 48.dp)
            .then(
                if (switchable && onClick != null) {
                    Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { onClick() }
                } else Modifier
            ),
        // 8dp 方角 = 字段语汇，与上面的胶囊分段、下面的搜索框（4dp 描边）都不同族
        shape = RoundedCornerShape(8.dp),
        // 可切=灰底字段；只读=透明（Surface 画不出东西 → 视觉上就是一行纯文字）
        color = if (switchable) MaterialTheme.colorScheme.surfaceContainerHighest
        else Color.Transparent,
    ) {
        Row(
            // 可切版文字在灰底字段内左右各留 12dp；只读版是纯文字标签，与下方候选行同一条左缘
            modifier = Modifier.padding(horizontal = if (switchable) 12.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Default.Category,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                value.ifBlank { "—" },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (switchable) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun extractTagCategory(name: String): String {
    val m = Regex("^(.+?)(\\d+)$").find(name)
    return m?.groupValues?.get(1) ?: name
}
