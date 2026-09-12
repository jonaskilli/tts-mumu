package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.jing332.common.utils.toScale
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.AppSpinner
import com.github.jing332.compose.widgets.LabelSlider
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.SoftSegmentedTextToggle

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
 * 结构 = **胶囊分区**（官方分段按钮，与音频参数弹窗同款）：一次只显示一页，
 * 弹窗高度取最长的一页，而不是把三块内容叠在一起。
 *
 * 三段及顺序（用户 09-12 深夜定稿）：
 * 1. **音频参数**：语速/音量/音高
 * 2. **采样率**：单独成页（不再与音频三维同页）
 * 3. **配置项**：清单（按实际分组折叠，可整组删）+ 更换来源插件 + 按插件全删
 *    ——用户原话「配置项含有更换插件和批量删除的功能」，两者作用对象同为配置项，故合在一页
 *
 * 为什么删除不排第一：打开弹窗默认落在第一段，破坏性操作放最后是通行惯例
 * （用户对菜单入口即持「低频的放最后」偏好）。现在默认页是音频参数，天然安全。
 *
 * 为什么是三段不是四段：[SoftSegmentedTextToggle] 恒为均分撑满 + 文字单行省略，
 * 四段在窄屏上会被截成「音频…」「更换…」（组件注释已记有"曾试收缩、文字被压成省略号"的结论）。
 *
 * 每页**各自带插件筛选器、状态互不影响**（用户原话：「这不是针对某一个插件的三项操作，
 * 每项都有选择插件的自由」）。分页后一次只渲染一页，所以各带一份并不比共用多占高度，
 * 却避免了「在 A 页选插件时顺手把 B 页（尤其删除）的范围也改掉」的串联风险。
 * 「配置项」页的插件筛选为**换插件与删除共用**——同页两个操作本就该作用在同一范围上。
 *
 * [scopeDesc] 作用域描述（「当前池全部配置项」），逐页随该页插件筛选一并显示。
 * [pluginOptions] 插件筛选候选：pluginId（""=全部，不按插件筛选）→ 显示名，仅含作用域内实际出现的插件。
 * [pluginItemCounts] pluginId → 作用域内配置项数（""=总数），供选择后实时显示影响范围。
 * [sampleRateOptions] 「采样率自动识别」=-1 语义由调用方解释。
 * [targetPluginOptions] 「配置项 → 更换插件」的目标插件候选：全部已安装插件 pluginId → 显示名。
 * [entries] 「配置项」页清单数据源，按该页所选插件过滤后折叠展示**项名**（不显示音色id，用户 09-12 晚定）。
 * [onApplyParams] 音频参数页应用：speed/volume/pitch = null 表示该项保持原值（滑条未拖动），
 *   非 null 为设定值。
 * [onApplySampleRate] 采样率页应用：null = 不修改 / -1 = 自动识别 / 其余为具体 Hz。
 * [onApplySource] 「配置项」页更换插件（[targetPluginId] 必非空——未选目标插件时按钮不提交）。
 * [onDelete] 删除请求：groupLabel=null 表示删所选插件的**全部**匹配项，非空表示只删该分组。
 *   弹窗内不落库——调用方弹二次确认后才删（破坏性操作必须有确认，用户 09-12 晚定）。
 */
@Composable
fun BatchConfigDialog(
    scopeDesc: String,
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
    // 0=音频参数 1=采样率 2=配置项（换插件 + 批量删除）
    var tab by remember { mutableStateOf(0) }
    // 三页各自的插件筛选（互不影响）
    var paramsFilterKey by remember { mutableStateOf<Any>("") }
    var rateFilterKey by remember { mutableStateOf<Any>("") }
    // 「配置项」页的插件筛选：换插件与删除共用同一范围（同页两个操作作用于同一批配置项）
    var itemFilterKey by remember { mutableStateOf<Any>("") }
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

    // 「配置项」页的筛选键（空串="全部"，此时删除键禁用）
    val itemPluginId = itemFilterKey as? String ?: ""
    val itemBuckets = entries.filter { it.pluginId == itemPluginId }
        .groupBy { it.groupLabel }
        .toList()
    // 删除只对**具体插件**开放：选中「全部（不按插件筛选）」时为 0，按钮禁用，
    // 避免一手滑把整个池子删空（沿用用户 09-12 拍板口径）
    val deletableCount = if (itemPluginId.isEmpty()) 0 else itemBuckets.sumOf { it.second.size }
    val targetPluginId = (targetPluginKey as? String)?.takeIf { it != "none" }

    AppDialog(
        title = { Text("批量配置操作") },
        content = {
            Column {
                SoftSegmentedTextToggle(
                    options = listOf("音频参数", "采样率", "配置项"),
                    selectedIndex = tab,
                    onSelect = { tab = it },
                )

                when (tab) {
                    // ── 音频参数：语速/音量/音高 ──
                    0 -> Column(Modifier.padding(horizontal = 4.dp)) {
                        ScopePluginPicker(
                            selectedKey = paramsFilterKey,
                            onSelect = { paramsFilterKey = it },
                            pluginOptions = pluginOptions,
                            pluginItemCounts = pluginItemCounts,
                            scopeDesc = scopeDesc,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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
                        // 说明「未拖动=不改」这条规则，否则显示 1.00 会被理解成"会把所有项设成 1.00"
                        Text(
                            "未拖动的参数保持原值",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // ── 采样率：单独一页 ──
                    1 -> Column(Modifier.padding(horizontal = 4.dp)) {
                        ScopePluginPicker(
                            selectedKey = rateFilterKey,
                            onSelect = { rateFilterKey = it },
                            pluginOptions = pluginOptions,
                            pluginItemCounts = pluginItemCounts,
                            scopeDesc = scopeDesc,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val rateValues: List<Any> = listOf("none", "auto") + sampleRateOptions
                        AppSpinner(
                            modifier = Modifier.fillMaxWidth(),
                            labelText = "采样率",
                            value = rateSelKey,
                            values = rateValues,
                            entries = listOf("不修改", "采样率自动识别") + sampleRateOptions.map { "$it Hz" },
                            onSelectedChange = { key, _ -> rateSelKey = key }
                        )
                    }

                    // ── 配置项：清单（可整组删）+ 更换来源插件（合并在同一页）──
                    else -> Column(Modifier.padding(horizontal = 4.dp)) {
                        ScopePluginPicker(
                            selectedKey = itemFilterKey,
                            onSelect = { key ->
                                itemFilterKey = key
                                // 换插件后清单整批变样，展开状态一并重置
                                expandedGroups = emptySet()
                            },
                            pluginOptions = pluginOptions,
                            pluginItemCounts = pluginItemCounts,
                            scopeDesc = scopeDesc,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        // 清单区：本页的删除对象预览；越高越挤，把上限压到 150dp 给下方换插件留位
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 150.dp)
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
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ExpandMore,
                                            contentDescription = if (expanded) "收起" else "展开",
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
                                        TextButton(onClick = { onDelete(itemPluginId, groupLabel) }) {
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
                                                    .padding(start = 28.dp, top = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // 上「删」下「改」：清单属删除，换插件另起一区，用分隔线断开避免误读成同一件事
                        HorizontalDivider(modifier = Modifier.padding(top = 6.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        AppSpinner(
                            modifier = Modifier.fillMaxWidth(),
                            labelText = "更换插件为",
                            value = targetPluginKey,
                            values = listOf<Any>("none") + targetPluginOptions.map { it.first },
                            entries = listOf("不修改") + targetPluginOptions.map { it.second },
                            onSelectedChange = { key, _ -> targetPluginKey = key }
                        )
                    }
                }
            }
        },
        buttons = {
            Row {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.cancel))
                }
                // 页脚主操作随当前页变化：音频参数=重置/应用，采样率=应用，配置项=删除全部 N 项/应用（换插件）
                when (tab) {
                    0 -> {
                        TextButton(onClick = {
                            // 重置 = 显式把音频三维设为 1.00（与"拖动过才算改动"互补：没拖过是保持原值，
                            // 点重置才是"整批恢复默认"）；仍需点「应用」才落库
                            speed = 1f
                            volume = 1f
                            pitch = 1f
                        }) {
                            Text("重置")
                        }
                        TextButton(onClick = {
                            onApplyParams(
                                (paramsFilterKey as? String)?.takeIf { it.isNotEmpty() },
                                speed, volume, pitch
                            )
                        }) {
                            Text("应用")
                        }
                    }

                    1 -> TextButton(onClick = {
                        onApplySampleRate(
                            (rateFilterKey as? String)?.takeIf { it.isNotEmpty() },
                            when (val k = rateSelKey) {
                                "none" -> null
                                "auto" -> -1
                                else -> k as? Int
                            }
                        )
                    }) {
                        Text("应用")
                    }

                    else -> {
                        TextButton(
                            onClick = { onDelete(itemPluginId, null) },
                            enabled = deletableCount > 0
                        ) {
                            Text(
                                "删除全部 $deletableCount 项",
                                color = if (deletableCount > 0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = {
                                targetPluginId?.let {
                                    onApplySource(
                                        (itemFilterKey as? String)?.takeIf { it.isNotEmpty() },
                                        it
                                    )
                                }
                            },
                            enabled = targetPluginId != null
                        ) {
                            Text("应用")
                        }
                    }
                }
            }
        },
        onDismissRequest = onDismissRequest
    )
}

/**
 * 逐页共用的「插件筛选 + 影响范围」块：插件选择器 + 匹配项数 + 作用域。
 * 三页各持一份选中值（由调用方即弹窗本体保管），互不影响。
 */
@Composable
private fun ScopePluginPicker(
    selectedKey: Any,
    onSelect: (Any) -> Unit,
    pluginOptions: List<Pair<String, String>>,
    pluginItemCounts: Map<String, Int>,
    scopeDesc: String,
) {
    Column(Modifier.fillMaxWidth()) {
        AppSpinner(
            modifier = Modifier.fillMaxWidth(),
            labelText = "插件",
            value = selectedKey,
            values = pluginOptions.map { it.first },
            entries = pluginOptions.map { it.second },
            onSelectedChange = { key, _ -> onSelect(key) }
        )
        // 数字在插件框正下方（它说的是"当前选中的插件有多少项"），作用域另起一句。
        // 键转 String：AppSpinner 的 key 是 Any（哨兵值），而计数表的键是 pluginId
        Text(
            "匹配 ${pluginItemCounts[(selectedKey as? String).orEmpty()] ?: 0} 项",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "作用域：$scopeDesc",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 批量删除二次确认（用户 09-12 晚定：破坏性操作必须有确认，原删除弹窗点一下就直接删）。
 * [label] 删除对象：如「插件「剪映最新官方中文774_免登」」或「分组「旁白 › 通用旁白」」。
 * [count] 待删配置项数。
 */
@Composable
fun BatchDeleteConfirmDialog(
    label: String,
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AppDialog(
        title = { Text("删除确认") },
        content = {
            Text("将删除$label 下的 $count 项配置。\n\n此操作不可恢复。")
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
