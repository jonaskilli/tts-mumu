package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import com.github.jing332.common.utils.toScale
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.AppSpinner
import com.github.jing332.compose.widgets.LabelSlider
import com.github.jing332.tts_server_android.R

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
 * ## 四页与顺序（用户 09-13 定：删除垫底）
 * 1. **音频参数**：语速/音量/音高 —— 页脚 取消 / 重置 / 确定
 * 2. **采样率**：采样率 —— 页脚 取消 / 确定
 * 3. **更换插件**：目标插件 —— 页脚 取消 / 确定（未选目标插件时禁用）
 * 4. **删除配置项**：清单（可整组删）—— 页脚 取消 / **删除全部 N 项**（红色）
 *
 * 删除排最后：打开弹窗默认落在第一页（音频参数），破坏性操作垫底是通行惯例
 * （用户对菜单入口即持「低频的放最后」偏好）。
 *
 * 每页**只干一件事**，页脚主操作自然各归其位——上一版把删除与换插件挤在同一页时，
 * 用户看到页脚的「确定」与「删除全部」并排，问「这几个按钮是什么」（09-12 深夜），
 * 故拆页；页脚不再出现"一个确定管哪个操作"的歧义。
 *
 * ## 插件筛选：全页通用（用户 09-13 定）
 * 四页共用一个插件筛选（filterKey），排在胶囊组下方，形如"本弹窗当前作用的插件范围"。
 * 早期曾让各页各持一份以防串联，但拆成四页后那样会变成"每页都要重选一次插件"，
 * 用户定：全页通用，一处选定四页可见（同一批对象，范围本就该一致）。
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
 * [entries] 「删除配置项」页清单数据源，按所选插件过滤后折叠展示**项名**（不显示音色id，用户 09-12 晚定）。
 * [onApplyParams] 音频参数页应用：speed/volume/pitch = null 表示该项保持原值（滑条未拖动），
 *   非 null 为设定值。
 * [onApplySampleRate] 采样率页应用：null = 不修改 / -1 = 自动识别 / 其余为具体 Hz。
 * [onApplySource] 「更换插件」提交（targetPluginId 必非空——未选目标插件时按钮不提交）。
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
    ) -> Unit,
    onApplySampleRate: (pluginId: String?, sampleRate: Int?) -> Unit,
    onApplySource: (pluginId: String?, targetPluginId: String) -> Unit,
    onDelete: (pluginId: String, groupLabel: String?) -> Unit,
) {
    // 0=音频参数 1=采样率 2=更换插件 3=删除配置项（用户 09-13 定：删除垫底）
    var tab by remember { mutableStateOf(0) }
    // 插件筛选：全页通用（用户 09-13 定），一处选定四页可见
    var filterKey by remember { mutableStateOf<Any>("") }
    // AppSpinner 的 value 需非空 Any：用 "none"/"auto"/Int/"具体pluginId" 作为哨兵
    var rateSelKey by remember { mutableStateOf<Any>("none") }
    var targetPluginKey by remember { mutableStateOf<Any>("none") }
    // 音频参数页草稿值：null = 本次不修改该项。
    // 若滑条按界面显示的 1.00 无条件提交，只想改某一维的人会连带把其余维度刷成 1.00。
    // 拖动过（或点了「重置」）才变成实值，未动过则提交 null，由调用方保持原值。
    var speed by remember { mutableStateOf<Float?>(null) }
    var volume by remember { mutableStateOf<Float?>(null) }
    var pitch by remember { mutableStateOf<Float?>(null) }
    // 分组展开状态：默认全收起，点分组行才展开
    var expandedGroups by remember { mutableStateOf<Set<String>>(emptySet()) }

    // 全页通用的插件筛选键（空串="全部"）。删除页要求具体插件，理由见 deletableCount
    val pluginId = filterKey as? String ?: ""
    val itemBuckets = entries.filter { it.pluginId == pluginId }
        .groupBy { it.groupLabel }
        .toList()
    // 删除只对**具体插件**开放：选中「全部」时为 0，按钮禁用，
    // 避免一手滑把整个池子删空（沿用用户 09-12 拍板口径）
    val deletableCount = if (pluginId.isEmpty()) 0 else itemBuckets.sumOf { it.second.size }
    val targetPluginId = (targetPluginKey as? String)?.takeIf { it != "none" }
    // 分区标签走资源字符串（中文在 values-zh、英文在 values-en），不再硬编码中文
    val tabTitles = listOf(
        stringResource(R.string.batch_cfg_tab_audio_params),
        stringResource(R.string.batch_cfg_sample_rate),
        stringResource(R.string.batch_cfg_tab_change_plugin),
        stringResource(R.string.batch_cfg_tab_delete_items),
    )
    // 删除页清单高度上限（用户 09-13：尽量显示完整）。写死 150dp 在实机只露 3 行；
    // 改为按屏幕高度推算剩余空间——弹窗固定部分（标题 + chip 两行 + 分隔 +
    // 插件筛选 + 匹配行 + 按钮行）约 330dp（09-13 撤副标题后由 360 收紧，
    // 腾出的空间回给清单），剩给清单的即为可滚区，下限 180dp 保住小屏，
    // 上限 420dp 防大屏上弹窗过分拉长
    val listMaxHeight = (LocalConfiguration.current.screenHeightDp - 330).coerceIn(180, 420).dp

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

                // 全页通用：本弹窗当前作用的插件范围（四页共用一份选中值）
                // 更换插件/删除两页 restricted：下拉无「全部」、未选显示提示（目目 09-13）
                ScopePluginPicker(
                    selectedKey = filterKey,
                    onSelect = { key ->
                        filterKey = key
                        // 换插件后清单整批变样，展开状态一并重置
                        expandedGroups = emptySet()
                    },
                    pluginOptions = pluginOptions,
                    pluginItemCounts = pluginItemCounts,
                    restricted = tab == 2 || tab == 3,
                )

                when (tab) {
                    // ── 1. 音频参数：语速/音量/音高 ──
                    0 -> {
                        LabelSlider(
                            value = speed ?: 1f,
                            onValueChange = { speed = it.toScale(2) },
                            valueRange = 0.1f..3f,
                            step = 0.05f,
                            buttonLongSteps = 0.05f,
                            text = stringResource(id = R.string.label_speech_rate, "%.2f".format(speed ?: 1f))
                        )
                        LabelSlider(
                            value = volume ?: 1f,
                            onValueChange = { volume = it.toScale(2) },
                            valueRange = 0.1f..3f,
                            step = 0.05f,
                            buttonLongSteps = 0.05f,
                            text = stringResource(id = R.string.label_speech_volume, "%.2f".format(volume ?: 1f))
                        )
                        LabelSlider(
                            value = pitch ?: 1f,
                            onValueChange = { pitch = it.toScale(2) },
                            valueRange = 0.1f..3f,
                            step = 0.05f,
                            buttonLongSteps = 0.05f,
                            text = stringResource(id = R.string.label_speech_pitch, "%.2f".format(pitch ?: 1f))
                        )
                        // 「未拖动的参数保持原值」提示行已删（用户 09-13，独占一行）：
                        // 未拖动即不提交、保持原值，由下方 Float? 草稿状态保证，不再单独出提示
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

                    // ── 4. 删除配置项：清单（可整组删）──
                    else -> {
                        if (itemBuckets.isEmpty()) {
                            // 空态只可能是"没选具体插件"：筛选候选只含实际出现的插件，
                            // 选中任一插件就必然有项。删除必须指定插件，故提示先选。
                            // 空态提示换主题色（用户 09-13：原次要灰太不起眼，这条是"现在还不能删"的
                            // 行动指引，需要一眼看到）
                            Text(
                                stringResource(R.string.batch_cfg_delete_pick_plugin_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = listMaxHeight)
                        ) {
                            items(itemBuckets, key = { it.first }) { (groupLabel, groupItems) ->
                                val expanded = groupLabel in expandedGroups
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                expandedGroups = if (expanded)
                                                    expandedGroups - groupLabel
                                                else
                                                    expandedGroups + groupLabel
                                            }
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
                                        // 整组删除（用户 09-12 晚要求）：删该分组下全部项，确认后才落库
                                        TextButton(onClick = { onDelete(pluginId, groupLabel) }) {
                                            Text(
                                                stringResource(R.string.delete),
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
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
                    // 音频参数：重置（显式把三维设回 1.00）+ 确定
                    0 -> {
                        TextButton(onClick = {
                            // 与"拖动过才算改动"互补：没拖过是保持原值，点重置才是"整批恢复默认"；
                            // 仍需点「确定」才落库
                            speed = 1f
                            volume = 1f
                            pitch = 1f
                        }) {
                            Text(stringResource(R.string.reset))
                        }
                        TextButton(onClick = {
                            onApplyParams(pluginId.takeIf { it.isNotEmpty() }, speed, volume, pitch)
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
                    // 范围空=「全部」全量改写 1344 项的风险，目目 09-13 定：必须先选具体插件）
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
 * 「插件筛选 + 影响范围」块：插件选择器 + 匹配项数，全页通用（一处选定四页可见）。
 */
@Composable
private fun ScopePluginPicker(
    selectedKey: Any,
    onSelect: (Any) -> Unit,
    pluginOptions: List<Pair<String, String>>,
    pluginItemCounts: Map<String, Int>,
    // 更换插件/删除页要求具体插件（目目 09-13）：这两页下拉不再提供「全部」，
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
