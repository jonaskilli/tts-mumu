package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.jing332.common.utils.toScale
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.AppSpinner
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.SoftSegmentedTextToggle
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.LayerSlider
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.audioParamsDimNames

/**
 * 清单条目：来源插件 + 所属**实际分组**（大分组，或「大分组 › 子分组」）+ 项名 + 配置项 id。
 * 本地TTS项不参与（无来源插件），由调用方过滤掉。
 */
data class BatchConfigEntry(
    val pluginId: String,
    val groupLabel: String,
    val name: String,
    val configId: Long,
)

/**
 * 批量配置操作（用户 09-12 晚拍板并定稿：「批量修改配置」「批量删除插件配置项」
 * 「批量调整音频参数」三个弹窗合并为一个）。
 *
 * ## 分区：胶囊组（用户 09-13 定：分段按钮换成可换行的 FilterChip 组）
 * 原用官方分段按钮（SoftSegmentedTextToggle），但它恒为**均分撑满 + 文字单行省略**，
 * 四段在窄屏会被压成「删除…」——段数被顶死在 3 段以内。
 * 用户 09-13 提议换成日志筛选弹窗（LogFilterDialog）里那套 **FilterChip + FlowRow**：
 * 宽度随文字、一行放不下自动折行，分区数不再受限。
 * 注意这与 09-11 撤销的「分段按钮的宽度收缩变体」不是一回事——那是改同一个连体控件，
 * 这里是换组件（同一套 chip 日志筛选弹窗已在用，风格一致）。
 *
 * **chip 不带 leadingIcon 勾号**：FilterChip 在 MD3 里本是"可多选的开关"语义（日志页那组就是），
 * 这里当单选页签用，靠"点了立刻切页、无需确认"让用户自行感知单选，去掉勾号避免误读成开关。
 *
 * ## 五页与顺序（用户 09-13 定：删除垫底；09-17 插入启用/停用，排在删除之前）
 * 1. **音频参数**：层分段（配置项 / 插件）＋该层 语速/音量/音高 三个滑杆
 *    —— 页脚 取消 / 重置 / 确定。形态照配置项音频参数弹窗（AudioParamsDialog）的
 *    软槽分段胶囊 + 滑杆组，只去掉「全局」层：批量作用域是 N 个配置项，
 *    全局参数由 ⋮ 菜单的「音频参数设置」单独管。
 *    两层的目标（用户 09-17 明确）：**配置项层** = 范围（上方所选插件，或「全部」）内那些
 *    配置项各自的 audioParams；**插件层** = 这些项**来源插件**自己的 audioParams，去重后
 *    逐只写 —— 范围=「全部」时即把范围内涉及的插件全调一遍（插件级实体，改它等于改该
 *    插件下全部配置项，与配置项弹窗的插件层同语义）。
 * 2. **采样率**：采样率 —— 页脚 取消 / 确定
 * 3. **更换插件**：目标插件 —— 页脚 取消 / 确定（未选目标插件时禁用）
 * 4. **启用/停用**：分组折叠清单（与删除页同版式 —— 用户 09-17 晚定，早先做成平铺被纠）——
 *    组头右侧 **启用** · **停用**（整组）；页脚 取消 / **启用 N 项** · **停用 N 项**（整批）。
 *    范围允许「全部」（启停可逆，与删除页必须锁定具体插件的口径不同）；
 *    作用对象=范围内全部配置项（含无来源插件的本地TTS项，故清单数据源也带上了它们）
 * 5. **删除配置项**：清单（可整组删）—— 页脚 取消 / **删除全部 N 项**（红色）
 *
 * 删除排最后：打开弹窗默认落在第一页（音频参数），破坏性操作垫底是通行惯例
 * （用户对菜单入口即持「低频的放最后」偏好）。
 *
 * 每页**只干一件事**，页脚主操作自然各归其位——上一版把删除与换插件挤在同一页时，
 * 用户看到页脚的「确定」与「删除全部」并排，问「这几个按钮是什么」（09-12 深夜），
 * 故拆页；页脚不再出现"一个确定管哪个操作"的歧义。
 *
 * ## 插件筛选：全页通用（用户 09-13 定）
 * 五页共用一个插件筛选（filterKey），排在胶囊组下方，形如"本弹窗当前作用的插件范围"。
 * 早期曾让各页各持一份以防串联，但拆页后那样会变成"每页都要重选一次插件"，
 * 用户定：全页通用，一处选定全页可见（同一批对象，范围本就该一致）。
 *
 * 「作用域：当前池全部配置项」那行已删除：调用方写死传入、永不变化，零信息量；
 * 真正有用的是「匹配 N 项」，保留。
 *
 * 09-13 装机反馈四项调整：
 * 1. 删除页清单上限由写死 150dp 改为按屏幕高度推算（150dp 在实机只露 3 行，用户要求尽量显示完整）；
 * 2. 「匹配 N 项」补与插件框的间距（原先零间距，视觉上"夹"在字段与清单之间显得挤）；
 * 3. 删「未拖动的参数保持原值」提示行（用户要求，该提示独占一行）；
 * 4. 每页新增副标题说明该操作——09-13 01:5x 用户看过后要求去掉，已回退，此项不做。
 *
 * [pluginOptions] 插件筛选候选：pluginId（""=全部，不按插件筛选）→ 显示名，仅含作用域内实际出现的插件。
 * [pluginItemCounts] pluginId → 作用域内配置项数（""=总数），供选择后实时显示影响范围。
 * [sampleRateOptions] 「采样率自动识别」=-1 语义由调用方解释。
 * [targetPluginOptions] 「更换插件」的目标插件候选：全部已安装插件 pluginId → 显示名。
 * [entries] 「启用/停用」与「删除配置项」两页共用的清单数据源。按范围过滤后，
 *   两页都折叠成「分组 → 项名」（共用 GroupedEntryList 一套版式），仅组头按钮不同。
 *   不显示音色id（用户 09-12 晚定）。**须含无来源插件的本地TTS项（pluginId="")**：
 *   启停是全量操作，本地项也要能启停；删除页按具体 pluginId 过滤，本地项永不入内。
 * [onApplyParams] 音频参数页应用：六个值中 null = 该层该维保持原值（滑条未拖动），非 null 为设定值；
 *   前三个 = 配置项层（写各选中项），后三个 = 插件层（写来源插件）。
 * [onApplySampleRate] 采样率页应用：null = 不修改 / -1 = 自动识别 / 其余为具体 Hz。
 *   作用对象=范围内全部配置项，**含本地TTS项**（用户 09-17 定：本地项也写它的
 *   audioFormat.sampleRate，即编辑页那个「PCM 兜底采样率」，不再跳过）。
 * [onApplySource] 「更换插件」提交（targetPluginId 必非空——未选目标插件时按钮不提交）。
 * [onToggleEnabled] 启用/停用提交：pluginId=null 表示范围=「全部」（含本地TTS项）；
 *   groupLabel=null 表示整批（页脚），非空表示只启停该分组（组头按钮）；
 *   enabled=true 启用、false 停用。作用对象=范围内全部配置项。
 * [onDelete] 删除请求：groupLabel=null 表示删所选插件的**全部**匹配项，非空表示只删该分组。
 *   弹窗内不落库——调用方弹二次确认后才删（破坏性操作必须有确认，用户 09-12 晚定）。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BatchConfigDialog(
    pluginOptions: List<Pair<String, String>>,
    pluginItemCounts: Map<String, Int>,
    sampleRateOptions: List<Int>,
    targetPluginOptions: List<Pair<String, String>>,
    entries: List<BatchConfigEntry>,
    onDismissRequest: () -> Unit,
    onApplyParams: (
        pluginId: String?,
        speed: Float?,
        volume: Float?,
        pitch: Float?,
        pluginSpeed: Float?,
        pluginVolume: Float?,
        pluginPitch: Float?,
    ) -> Unit,
    onApplySampleRate: (pluginId: String?, sampleRate: Int?) -> Unit,
    onApplySource: (pluginId: String?, targetPluginId: String) -> Unit,
    onToggleEnabled: (pluginId: String?, groupLabel: String?, enabled: Boolean) -> Unit,
    onDelete: (pluginId: String, groupLabel: String?) -> Unit,
) {
    // 0=音频参数 1=采样率 2=更换插件 3=启用/停用 4=删除配置项
    //（用户 09-13 定：删除垫底；09-17 插入启用/停用，排在其前）
    var tab by remember { mutableStateOf(0) }
    // 插件筛选：全页通用（用户 09-13 定），一处选定全页可见
    var filterKey by remember { mutableStateOf<Any>("") }
    // AppSpinner 的 value 需非空 Any：用 "none"/"auto"/Int/"具体pluginId" 作为哨兵
    var rateSelKey by remember { mutableStateOf<Any>("none") }
    var targetPluginKey by remember { mutableStateOf<Any>("none") }
    // 音频参数页当前层（0=配置项 1=插件）：上方胶囊分段切换，下方恒为语速/音量/音高 三个滑杆
    var layer by remember { mutableStateOf(0) }
    // 音频参数页配置项层草稿值：null = 本次不修改该维。
    // 若滑条按界面显示的 1.00 无条件提交，只想改某一维的人会连带把其余维度刷成 1.00。
    // 拖动过（或点了「重置」）才变成实值，未动过则提交 null，由调用方保持原值。
    var speed by remember { mutableStateOf<Float?>(null) }
    var volume by remember { mutableStateOf<Float?>(null) }
    var pitch by remember { mutableStateOf<Float?>(null) }
    // 插件层草稿（配置项层的上一层；值语义同上）
    var pluginSpeed by remember { mutableStateOf<Float?>(null) }
    var pluginVolume by remember { mutableStateOf<Float?>(null) }
    var pluginPitch by remember { mutableStateOf<Float?>(null) }
    // 分组展开状态：默认全收起，点分组行才展开
    var expandedGroups by remember { mutableStateOf<Set<String>>(emptySet()) }

    // 全页通用的插件筛选键（空串="全部"）。删除页要求具体插件，理由见 deletableCount
    val pluginId = filterKey as? String ?: ""
    // 范围内清单：空串="全部"⇒不过滤。不能写成 filter { it.pluginId == pluginId }：
    // 无来源插件的本地TTS项 pluginId 也是空串，「全部」会被筛成"只剩本地项"
    val scopeEntries = if (pluginId.isEmpty()) entries else entries.filter { it.pluginId == pluginId }
    // 删除页只认具体插件：未选（=「全部」）时清单为空、按钮禁用，避免一手滑把整个池子删空
    //（沿用用户 09-12 拍板口径）
    val itemBuckets =
        if (pluginId.isEmpty()) emptyList() else scopeEntries.groupBy { it.groupLabel }.toList()
    val deletableCount = if (pluginId.isEmpty()) 0 else itemBuckets.sumOf { it.second.size }
    // 启用/停用页按同一套分组展示（用户 09-17 晚：跟删除页一样分组列出，组头也能启停），
    // 但没有删除页那条「必须先选具体插件」的限制——启停可逆，范围=「全部」时
    // 就是"当前池全部配置项按分组列出"，组头即该组全部项（含无来源插件的本地TTS项）
    val enableBuckets = scopeEntries.groupBy { it.groupLabel }.toList()
    val targetPluginId = (targetPluginKey as? String)?.takeIf { it != "none" }
    // 分区标签走资源字符串（中文在 values-zh、英文在 values-en），不再硬编码中文。
    // 顺序（用户 09-17 定）：启用/停用排在「删除配置项」之前，破坏性的删除仍垫底
    val tabTitles = listOf(
        stringResource(R.string.batch_cfg_tab_audio_params),
        stringResource(R.string.batch_cfg_sample_rate),
        stringResource(R.string.batch_cfg_tab_change_plugin),
        stringResource(R.string.batch_cfg_tab_enable_items),
        stringResource(R.string.batch_cfg_tab_delete_items),
    )
    // 清单高度上限（用户 09-13：尽量显示完整）。写死 150dp 在实机只露 3 行；
    // 改为按屏幕高度推算剩余空间——弹窗固定部分（标题 + chip 两行 + 分隔 +
    // 插件筛选 + 匹配行 + 按钮行）约 330dp；09-17 加第 5 个 chip（窄屏可能再占一行）
    // 上浮到 350dp（09-13 撤副标题后曾由 360 收紧到 330），腾出的空间回给清单。
    // 剩给清单的即为可滚区，下限 180dp 保住小屏，上限 420dp 防大屏上弹窗过分拉长
    val listMaxHeight = (LocalConfiguration.current.screenHeightDp - 350).coerceIn(180, 420).dp

    AppDialog(
        title = { Text(stringResource(R.string.batch_cfg_title)) },
        content = {
            Column {
                // 分区选择：可换行 chip 组（宽度随文字，能放四段以上）
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        FilterChip(
                            selected = tab == index,
                            onClick = { tab = index },
                            label = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // 全页通用：本弹窗当前作用的插件范围（全页共用一份选中值）
                // 更换插件/删除两页 restricted：下拉无「全部」、未选显示提示
                ScopePluginPicker(
                    selectedKey = filterKey,
                    onSelect = { key ->
                        filterKey = key
                        // 换插件后清单整批变样，展开状态一并重置
                        expandedGroups = emptySet()
                        // 插件层草稿跟着失效：它记的是"当前范围涉及的那批插件"的参数，
                        // 范围一换目标就换了一批，旧值不该落到新目标上
                        pluginSpeed = null
                        pluginVolume = null
                        pluginPitch = null
                    },
                    pluginOptions = pluginOptions,
                    pluginItemCounts = pluginItemCounts,
                    restricted = tab == 2 || tab == 4,
                )

                when (tab) {
                    // ── 1. 音频参数：层分段（配置项 / 插件）+ 该层 语速/音量/音高 三滑杆 ──
                    0 -> {
                        // 层分段（配置项 / 插件）：照配置项音频参数弹窗的软槽分段胶囊，只去掉「全局」层
                        SoftSegmentedTextToggle(
                            options = listOf(
                                stringResource(R.string.batch_cfg_layer_config),
                                stringResource(R.string.audio_params_tag_plugin),
                            ),
                            selectedIndex = layer,
                            onSelect = { layer = it },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        val dims = audioParamsDimNames
                        // 左缩进 8dp、行距 4dp：与配置项弹窗的维度内容同规格。
                        // 层已由上方分段表达，滑杆标签只标维度与当前值
                        Column(
                            Modifier.padding(top = 8.dp, start = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (layer == 0) {
                                LayerSlider(dims[0], speed ?: 1f) { speed = it.toScale(2) }
                                LayerSlider(dims[1], volume ?: 1f) { volume = it.toScale(2) }
                                LayerSlider(dims[2], pitch ?: 1f) { pitch = it.toScale(2) }
                            } else {
                                LayerSlider(dims[0], pluginSpeed ?: 1f) { pluginSpeed = it.toScale(2) }
                                LayerSlider(dims[1], pluginVolume ?: 1f) { pluginVolume = it.toScale(2) }
                                LayerSlider(dims[2], pluginPitch ?: 1f) { pluginPitch = it.toScale(2) }
                            }
                        }
                        // 「未拖动的参数保持原值」提示行已删（用户 09-13，独占一行）：
                        // 未拖动即不提交、保持原值，由上面 Float? 草稿状态保证，不再单独出提示
                    }

                    // ── 2. 采样率 ──
                    1 -> {
                        val rateValues: List<Any> = listOf("none", "auto") + sampleRateOptions
                        AppSpinner(
                            modifier = Modifier.fillMaxWidth(),
                            labelText = stringResource(R.string.batch_cfg_sample_rate),
                            value = rateSelKey,
                            values = rateValues,
                            entries = listOf(
                                stringResource(R.string.batch_cfg_keep_unchanged),
                                stringResource(R.string.systts_auto_detect_audio_format),
                            ) + sampleRateOptions.map { "$it Hz" },
                            onSelectedChange = { key, _ -> rateSelKey = key }
                        )
                    }

                    // ── 3. 更换插件 ──
                    2 -> AppSpinner(
                        modifier = Modifier.fillMaxWidth(),
                        labelText = stringResource(R.string.batch_cfg_change_plugin_to),
                        value = targetPluginKey,
                        values = listOf<Any>("none") + targetPluginOptions.map { it.first },
                        entries = listOf(stringResource(R.string.batch_cfg_keep_unchanged)) +
                            targetPluginOptions.map { it.second },
                        onSelectedChange = { key, _ -> targetPluginKey = key }
                    )

                    // ── 4. 启用/停用：分组折叠清单，组头带「启用 / 停用」──
                    // 用户 09-17 晚：与删除页同一套版式（分组 → 项名），组头也能整组启停；
                    // 页脚的 启用N项/停用N项 作用范围为整批（含本地TTS项，范围=「全部」即整池）
                    3 -> GroupedEntryList(
                        buckets = enableBuckets,
                        expandedGroups = expandedGroups,
                        onToggleExpand = { groupLabel ->
                            expandedGroups = if (groupLabel in expandedGroups)
                                expandedGroups - groupLabel
                            else
                                expandedGroups + groupLabel
                        },
                        maxHeight = listMaxHeight,
                    ) { groupLabel, _ ->
                        TextButton(onClick = {
                            onToggleEnabled(
                                pluginId.takeIf { it.isNotEmpty() }, groupLabel, true
                            )
                        }) {
                            Text(stringResource(R.string.batch_cfg_enable))
                        }
                        TextButton(onClick = {
                            onToggleEnabled(
                                pluginId.takeIf { it.isNotEmpty() }, groupLabel, false
                            )
                        }) {
                            Text(stringResource(R.string.batch_cfg_disable))
                        }
                    }

                    // ── 5. 删除配置项：分组清单（可整组删）──
                    // 空态提示由 ScopePluginPicker(restricted=true) 负责（未选插件时
                    // 它在下拉框下方出「请先在上方选择一个插件」），此处不再重复出提示
                    // （用户 09-13：两条一模一样的提示叠着显示）
                    else -> GroupedEntryList(
                        buckets = itemBuckets,
                        expandedGroups = expandedGroups,
                        onToggleExpand = { groupLabel ->
                            expandedGroups = if (groupLabel in expandedGroups)
                                expandedGroups - groupLabel
                            else
                                expandedGroups + groupLabel
                        },
                        maxHeight = listMaxHeight,
                    ) { groupLabel, _ ->
                        // 整组删除（用户 09-12 晚要求）：删该分组下全部项，确认后才落库
                        TextButton(onClick = { onDelete(pluginId, groupLabel) }) {
                            Text(
                                stringResource(R.string.delete),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        buttons = {
            Row {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.cancel))
                }
                // 每页只干一件事，页脚主操作各归其位（用户 09-13 拆页的初衷）
                when (tab) {
                    // 音频参数：重置（当前层 语速/音量/音高 三个草稿设回 1.00）+ 确定
                    0 -> {
                        TextButton(onClick = {
                            // 与"拖动过才算改动"互补：没拖过是保持原值，点重置才是恢复默认；
                            // 只重置当前层（与配置项弹窗按维度重置同口径），仍需点「确定」才落库
                            if (layer == 0) {
                                speed = 1f; volume = 1f; pitch = 1f
                            } else {
                                pluginSpeed = 1f; pluginVolume = 1f; pluginPitch = 1f
                            }
                        }) {
                            Text(stringResource(R.string.reset))
                        }
                        TextButton(onClick = {
                            // 插件三参写作用域内涉及的全部插件（去重后逐只写；范围=「全部」时
                            // 即范围内全部插件），见 ListManagerViewModel.updateAudioParamsBatch
                            onApplyParams(
                                pluginId.takeIf { it.isNotEmpty() },
                                speed, volume, pitch,
                                pluginSpeed, pluginVolume, pluginPitch,
                            )
                        }) {
                            Text(stringResource(R.string.confirm))
                        }
                    }

                    // 采样率：确定
                    1 -> TextButton(
                        onClick = {
                            onApplySampleRate(
                                pluginId.takeIf { it.isNotEmpty() },
                                when (val k = rateSelKey) {
                                    "none" -> null
                                    "auto" -> -1
                                    else -> k as? Int
                                }
                            )
                        }
                    ) {
                        Text(stringResource(R.string.confirm))
                    }

                    // 更换插件：确定（未选目标插件、或插件范围还是空时禁用——
                    // 范围空=「全部」全量改写 1344 项的风险：必须先选具体插件）
                    2 -> TextButton(
                        onClick = {
                            targetPluginId?.let {
                                onApplySource(pluginId.takeIf { it.isNotEmpty() }, it)
                            }
                        },
                        enabled = targetPluginId != null && pluginId.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.confirm))
                    }

                    // 启用/停用：启用 N 项 / 停用 N 项（整批=groupLabel null；N=范围内匹配项数，
                    // 与「删除全部 N 项」同口径）。组头另有按分组的启停，两处都走同一回调。
                    // 启停可逆，范围允许「全部」，不像删除页必须锁定具体插件
                    3 -> {
                        val n = scopeEntries.size
                        TextButton(
                            onClick = {
                                onToggleEnabled(pluginId.takeIf { it.isNotEmpty() }, null, true)
                            },
                            enabled = n > 0
                        ) {
                            Text(stringResource(R.string.batch_cfg_enable_n, n))
                        }
                        TextButton(
                            onClick = {
                                onToggleEnabled(pluginId.takeIf { it.isNotEmpty() }, null, false)
                            },
                            enabled = n > 0
                        ) {
                            Text(stringResource(R.string.batch_cfg_disable_n, n))
                        }
                    }

                    // 删除配置项：删除全部 N 项（红色；未选具体插件时为 0、禁用）
                    else -> TextButton(
                        onClick = { onDelete(pluginId, null) },
                        enabled = deletableCount > 0
                    ) {
                        Text(
                            stringResource(R.string.batch_cfg_delete_all, deletableCount),
                            color = if (deletableCount > 0) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        onDismissRequest = onDismissRequest
    )
}

/**
 * 分组折叠清单：组头（展开箭头 + 组名 + 项数 + 右侧操作按钮）＋ 展开后的项名。
 *
 * 「启用/停用」与「删除配置项」两页共用同一套版式（用户 09-17 晚：启停也要跟删除页一样
 * 按分组列出，且组头能整组启停），唯一差别是组头右侧那排按钮——故由调用方经 [groupActions]
 * 提供（接收者是 RowScope，便于按需用 weight 撑开）。点组头整行=展开/收起。
 *
 * [maxHeight] 上限外仍可滚动（沿用 09-13 定下的按屏幕高度推算值）。
 */
@Composable
private fun GroupedEntryList(
    buckets: List<Pair<String, List<BatchConfigEntry>>>,
    expandedGroups: Set<String>,
    onToggleExpand: (String) -> Unit,
    maxHeight: Dp,
    groupActions: @Composable RowScope.(groupLabel: String, items: List<BatchConfigEntry>) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
    ) {
        items(buckets, key = { it.first }) { (groupLabel, groupItems) ->
            val expanded = groupLabel in expandedGroups
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleExpand(groupLabel) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = stringResource(
                            if (expanded) R.string.batch_cfg_collapse
                            else R.string.batch_cfg_expand
                        ),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .rotate(if (expanded) 0f else -90f),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = groupLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "(${groupItems.size})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    groupActions(groupLabel, groupItems)
                }
                if (expanded) {
                    groupItems.forEach { entry ->
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 28.dp, top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 「插件筛选 + 影响范围」块：插件选择器 + 匹配项数，全页通用（一处选定全页可见）。
 */
@Composable
private fun ScopePluginPicker(
    selectedKey: Any,
    onSelect: (Any) -> Unit,
    pluginOptions: List<Pair<String, String>>,
    pluginItemCounts: Map<String, Int>,
    // 更换插件/删除页要求具体插件：这两页下拉不再提供「全部」，
    // 未选时显示「选择插件」提示（哨兵 "none"，不算已选、不可提交）
    restricted: Boolean = false,
) {
    Column(Modifier.fillMaxWidth()) {
        val concreteOptions = pluginOptions.filter { it.first.isNotEmpty() }
        val unselected = restricted && (selectedKey as? String).isNullOrEmpty()
        val scopeValues: List<Any> = when {
            !restricted -> pluginOptions.map { it.first }
            unselected -> listOf<Any>("none") + concreteOptions.map { it.first }
            else -> concreteOptions.map { it.first }
        }
        val scopeEntries = when {
            !restricted -> pluginOptions.map { it.second }
            unselected -> listOf(stringResource(R.string.select_plugin)) + concreteOptions.map { it.second }
            else -> concreteOptions.map { it.second }
        }
        AppSpinner(
            modifier = Modifier.fillMaxWidth(),
            labelText = stringResource(R.string.plugin),
            value = if (unselected) "none" else selectedKey,
            values = scopeValues,
            entries = scopeEntries,
            onSelectedChange = { key, _ -> if (key != "none") onSelect(key) }
        )
        // 未选具体插件时给行动指引（替代无意义的「匹配 0 项」）；选定后回到计数行
        if (unselected) {
            Text(
                stringResource(R.string.batch_cfg_delete_pick_plugin_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
            )
        } else {
            // 数字在插件框正下方（它说的是"当前选中的插件有多少项"）。
            // 键转 String：AppSpinner 的 key 是 Any（哨兵值），而计数表的键是 pluginId
            Text(
                stringResource(
                    R.string.batch_cfg_matched,
                    pluginItemCounts[(selectedKey as? String).orEmpty()] ?: 0
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                // 与插件框留出间距：原先紧贴字段下沿，视觉上"夹"在字段与清单之间（用户 09-13）
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
            )
        }
    }
}

/**
 * 删除二次确认（用户 09-12 晚定：破坏性操作必须有确认，原删除弹窗点一下就直接删）。
 * [label] 删除对象：如「插件「剪映最新官方中文774_免登」」或「分组「旁白 › 通用旁白」」。
 * [count] 待删配置项数。
 * [messageOverride] 覆盖默认正文：默认模板「将删除%1$s 下的 %2$d 项配置」句式固定，
 *   不适用时（顶栏失效配置项清理）直接传整句，见 invalid_delete_confirm_msg_all / _source。
 */
@Composable
fun BatchDeleteConfirmDialog(
    label: String = "",
    count: Int = 0,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    messageOverride: String? = null,
) {
    AppDialog(
        title = { Text(stringResource(R.string.batch_cfg_delete_confirm_title)) },
        content = {
            Text(
                messageOverride
                    ?: stringResource(R.string.batch_cfg_delete_confirm_msg, label, count)
            )
        },
        buttons = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(onClick = onConfirm) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        onDismissRequest = onDismiss
    )
}
