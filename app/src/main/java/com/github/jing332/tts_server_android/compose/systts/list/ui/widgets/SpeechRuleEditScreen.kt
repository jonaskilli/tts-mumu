package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Tag
import com.github.jing332.compose.widgets.AppDropdownMenu
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.jing332.common.utils.ClipboardUtils
import com.github.jing332.common.utils.StringUtils
import com.github.jing332.common.utils.longToast
import com.github.jing332.common.utils.toast
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.AppSpinner
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.database.entities.systts.SpeechRuleInfo
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.list.TagPickerDialog
import com.github.jing332.tts_server_android.compose.systts.list.expandSpeechRuleTagsIfNeeded
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.tts_server_android.constant.SpeechTarget
import com.github.jing332.tts_server_android.model.rhino.speech_rule.SpeechRuleEngine
import com.github.jing332.tts_server_android.service.systts.help.InnerThoughtClassifier
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import kotlinx.serialization.encodeToString

@Composable
fun SpeechRuleEditScreen(
    modifier: Modifier,
    systts: SystemTtsV2,
    onSysttsChange: (SystemTtsV2) -> Unit,

    showSpeechTarget: Boolean = true,
    speechRules: List<SpeechRule> = remember { dbm.speechRuleDao.allEnabled },
    // true 时正文（规则脚本/标签字段+心声+自定义字段+cardTrailer）包进无标题淡底卡（完整编辑页用），
    // 顶部的音频参数行/朗读切换行仍在卡外平铺；false 全部平铺（快捷编辑面板等）
    bodyInCard: Boolean = false,
    // bodyInCard 且标签态时渲染在正文卡末尾的内容（完整编辑页把分组/显示名称等基本信息放进同一张卡）
    cardTrailer: @Composable () -> Unit = {},
) {
    val context = LocalContext.current

    @Suppress("NAME_SHADOWING")
    val systts by rememberUpdatedState(newValue = systts)
    val config by rememberUpdatedState(newValue = systts.config as TtsConfigurationDTO)
    val speechRule by rememberUpdatedState(newValue = speechRules.find { it.ruleId == config.speechRule.tagRuleId })

    // 第4项: 标签实时刷新 - 修改tag或tagData后立即重算tagName
    LaunchedEffect(config.speechRule.tag, config.speechRule.tagData, config.speechRule.tagRuleId) {
        if (speechRule != null) {
            var tagName = ""
            runCatching {
                tagName = SpeechRuleEngine.getTagName(context, speechRule!!, info = config.speechRule)
            }.onFailure {
                // 静默失败，不弹框打扰用户编辑
            }
            tagName = tagName.ifBlank {
                StringUtils.WARNING_EMOJI + speechRule?.tags[config.speechRule.tag]
            }
            if (tagName != config.speechRule.tagName) {
                onSysttsChange(
                    systts.copy(
                        config = config.copy(config.speechRule.copy(tagName = tagName))
                    )
                )
            }
        }
    }

    SaveActionHandler {
        var tagName = ""
        if (speechRule != null) {
            runCatching {
                tagName =
                    SpeechRuleEngine.getTagName(context, speechRule!!, info = config.speechRule)
            }.onFailure {
                context.displayErrorDialog(it, context.getString(R.string.get_tag_name_failed))
            }

            tagName = tagName.ifBlank {
                StringUtils.WARNING_EMOJI + speechRule?.tags[config.speechRule.tag]
            }
        }

        onSysttsChange(
            systts.copy(
                config = config.copy(config.speechRule.copy(tagName = tagName))
            )
        )
        true
    }

    // 「音频参数」入口已于 09-10 从此处摘除（用户定稿）：改挂到试听文本行右侧⚡
    // （AuditionTextField），与🎧试听同框形成"改文本→调参数→立刻听"闭环。
    // 摘除后本组件的另两个使用方同步变化：
    //   完整编辑页(TtsEditContainerScreen) → 由 PluginTtsUI/LocalTtsUI 的试听文本行补上；
    //   快捷编辑面板(QuickEditBottomSheet) → 不再提供音频参数入口（用户确认：直接去掉）。
    // 其余入口不受影响：卡片⋮菜单「音频参数」保留。
    // 「作为备用引擎(isStandby)」也于同日挪入基本信息卡（BasicInfoEditScreen），
    // 与分组/显示名等属性同区——原顶部行随音频参数按钮摘除后只剩备用键，不再独占一行。

    if (showSpeechTarget)
        Column(modifier.fillMaxWidth()) {
            // 第6项: 子分组(categoryPath)编辑已统一由 BasicInfoEditScreen 的分组树选择器负责,
            // 此处不再重复提供子分组输入,避免同一编辑流程出现两个 categoryPath 编辑入口。

            var showTagClearDialog by remember { mutableStateOf(false) }
            if (showTagClearDialog) {
                TagDataClearConfirmDialog(
                    config.speechRule.tagData.toString(),
                    onDismissRequest = { showTagClearDialog = false },
                    onConfirm = {
                        onSysttsChange(
                            systts.copy(
                                config = config.copy(
                                    speechRule = config.speechRule.copy(
                                        tagName = "",
                                        target = SpeechTarget.ALL
                                    ).apply { resetTag() }
                                )
                            )
                        )
                        showTagClearDialog = false
                    })
            }

            var showTagOptions by remember { mutableStateOf(false) }
            SingleChoiceSegmentedButtonRow(
                Modifier
                    .padding(4.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                SegmentedButton(
                    config.speechRule.target != SpeechTarget.TAG,
                    onClick = {
                        if (config.speechRule.isTagDataEmpty())
                            onSysttsChange(
                                systts.copy(
                                    config = config.copy(
                                        speechRule = config.speechRule.copy(
                                            tagName = "",
                                            target = SpeechTarget.ALL
                                        ).apply { resetTag() }
                                    )
                                )
                            )
                        else
                            showTagClearDialog = true
                    },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    icon = { Icon(Icons.Default.SelectAll, stringResource(R.string.ra_all)) },
                ) {
                    // 字号 14sp（用户 09-11：16sp 太大；此前升 16sp 是为对齐日志面板父级，
                    // 但日志面板已降回 14sp，此处降回后全 app 分段控件字号统一 14sp）
                    Text(
                        stringResource(id = R.string.ra_all),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                        maxLines = 1,
                    )
                }

                SegmentedButton(
                    selected = config.speechRule.target == SpeechTarget.TAG,
                    onClick = {
                        if (config.speechRule.target == SpeechTarget.TAG)
                            showTagOptions = true
                        else
                            onSysttsChange(
                                systts.copy(
                                    config = config.copy(
                                        speechRule = config.speechRule.copy(target = SpeechTarget.TAG)
                                    )
                                )
                            )
                    },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    icon = {
                        Icon(
                            Icons.Default.Tag,
                            stringResource(R.string.tag),
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    },
                ) {
                    Text(
                        stringResource(id = R.string.tag),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                        maxLines = 1,
                        modifier = Modifier.padding(start = 4.dp, end = 10.dp)
                    )

                    AppDropdownMenu(
                        expanded = showTagOptions,
                        onDismissRequest = { showTagOptions = false }) {
                        Text(
                            text = stringResource(R.string.tag_data),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.copy)) },
                            onClick = {
                                showTagOptions = false
                                val jStr =
                                    AppConst.jsonBuilder.encodeToString(config.speechRule)
                                ClipboardUtils.copyText(jStr)
                                context.toast(R.string.copied)
                            })
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.paste)) },
                            onClick = {
                                showTagOptions = false
                                val jStr = ClipboardUtils.text.toString()
                                if (jStr.isBlank()) {
                                    context.toast(R.string.format_error)
                                    return@DropdownMenuItem
                                }

                                runCatching {
                                    val info =
                                        AppConst.jsonBuilder.decodeFromString<SpeechRuleInfo>(
                                            jStr
                                        )
                                    onSysttsChange(systts.copy(config = config.copy(speechRule = info)))
                                }.onSuccess {
                                    context.longToast(R.string.save_success)
                                }.onFailure {
                                    context.displayErrorDialog(
                                        it,
                                        context.getString(R.string.format_error)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // 正文：规则脚本/标签字段+心声芯片，卡片模式与平铺模式共用同一份内容。
            // 芯片、选择器、说明文字必须包进 Column 才竖排（AnimatedVisibility 内容是堆叠布局，
            // 此前「心声(内心独白)」芯片就压在规则脚本选择器上）
            val ruleTagBody: @Composable () -> Unit = {
                // 心声保留标签是否生效：标签下拉候选与芯片、说明文案共用
                val isInnerThought = config.speechRule.tag == InnerThoughtClassifier.INNER_THOUGHT_TAG

                // 勾选内心独白前的原标签（用户 09-11：取消勾选时还原为勾选前的标签，不再清空）。
                // 会话内 remember——若勾选→保存→退出→重进再取消，原标签已无从得知，回落为清空（旧行为）
                var tagBeforeMonologue by remember(systts.id) { mutableStateOf<String?>(null) }
                var showInnerThoughtHelp by remember { mutableStateOf(false) }
                if (showInnerThoughtHelp)
                    AppDialog(
                        title = { Text(stringResource(id = R.string.systts_inner_thought_help)) },
                        content = { Text(stringResource(id = R.string.systts_inner_thought_help_msg)) },
                        buttons = {
                            TextButton(onClick = { showInnerThoughtHelp = false }) {
                                Text(stringResource(id = R.string.confirm))
                            }
                        },
                        onDismissRequest = { showInnerThoughtHelp = false }
                    )

                fun toggleInnerThought() {
                    if (config.speechRule.target != SpeechTarget.TAG) return
                    val newTag: String
                    if (config.speechRule.tag == InnerThoughtClassifier.INNER_THOUGHT_TAG) {
                        // 取消勾选：还原为勾选前的原标签
                        newTag = tagBeforeMonologue ?: ""
                        tagBeforeMonologue = null
                    } else {
                        // 勾选：先记住当前标签
                        tagBeforeMonologue = config.speechRule.tag
                        newTag = InnerThoughtClassifier.INNER_THOUGHT_TAG
                    }
                    onSysttsChange(
                        systts.copy(
                            config = config.copy(config.speechRule.copy(tag = newTag))
                        )
                    )
                }
                Row(Modifier) {
                    AppSpinner(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp),
                        labelText = "📜 " + stringResource(R.string.speech_rule_script),
                        value = config.speechRule.tagRuleId,
                        values = speechRules.map { it.ruleId },
                        entries = speechRules.map { it.name },
                        onSelectedChange = { k, v ->
                            if (config.speechRule.target != SpeechTarget.TAG) return@AppSpinner
                            onSysttsChange(
                                systts.copy(
                                    config = config.copy(
                                        speechRule = config.speechRule.copy(
                                            tagRuleId = k as String
                                        )
                                    )
                                )
                            )
                        }
                    )

                    speechRule?.let { rule ->
                        var showTagPicker by remember { mutableStateOf(false) }
                        var pickerRule by remember { mutableStateOf<SpeechRule?>(null) }
                        // 打开选择弹窗前刷新规则并做标签扩容，覆盖超出初始序号范围的标签
                        LaunchedEffect(showTagPicker, rule.ruleId) {
                            if (!showTagPicker) return@LaunchedEffect
                            pickerRule = null
                            pickerRule = withContext(Dispatchers.IO) {
                                runCatching {
                                    val fresh = dbm.speechRuleDao.getByRuleId(rule.ruleId)
                                        ?: return@runCatching null
                                    runCatching { expandSpeechRuleTagsIfNeeded(fresh, dbm.systemTtsV2.all) }
                                    dbm.speechRuleDao.getByRuleId(rule.ruleId)
                                }.getOrNull()
                            }
                        }
                        // 点击标签字段 → 两层「分类→序号」选择弹窗，与列表页标签切换同一交互
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 4.dp)
                                .clickable { showTagPicker = true }
                        ) {
                            OutlinedTextField(
                                value = if (isInnerThought) "心声(内心独白)"
                                else config.speechRule.tagName.ifBlank { config.speechRule.tag },
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                singleLine = true,
                                label = { Text("🏷️ " + stringResource(R.string.tag)) },
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, null)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (showTagPicker) {
                            val pr = pickerRule
                            if (pr != null) {
                                TagPickerDialog(
                                    rule = pr,
                                    currentTag = config.speechRule.tag,
                                    onSelect = { tag, _ ->
                                        // tag 变化后编辑页既有的 LaunchedEffect 会经规则 JS 重算 tagName
                                        onSysttsChange(
                                            systts.copy(
                                                config = config.copy(
                                                    speechRule = config.speechRule.copy(tag = tag)
                                                )
                                            )
                                        )
                                        showTagPicker = false
                                    },
                                    onDismissRequest = { showTagPicker = false }
                                )
                            }
                        }
                    }
                }

                // 内心独白（用户 09-10 晚定稿）：与基本信息卡「心声混响 / 作为备用」同款复选框行，
                // 位置紧贴归属字段「标签」。勾选＝标签设为内置「内心独白」标签；
                // 取消＝还原为勾选前的原标签（用户 09-11，不再清空）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { toggleInnerThought() }
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 复选框 32dp 触摸区（用户 09-11 终裁根治）：撤所有 offset 魔法数字，
                    // 方块两侧自然剩 7dp，文字自然位置即紧凑；与基本信息卡两行同款（详见彼处注释）
                    Checkbox(
                        checked = isInnerThought,
                        onCheckedChange = { toggleInnerThought() },
                        modifier = Modifier.size(32.dp),
                    )
                    Text("心声标签")
                    // 问号 32dp 触摸区（24dp 图标只留 4dp 内缩）：紧跟文字不留空（用户 09-11 三行统一；
                    // 默认 48dp 会有 12dp 空隙）
                    IconButton(
                        onClick = { showInnerThoughtHelp = true },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            stringResource(id = R.string.systts_inner_thought_help)
                        )
                    }
                }
                if (isInnerThought) {
                    Text(
                        "朗读中判定为内心独白的句子会改用本配置的音色；可搭配设置页「启用心声 AI 判定」提升识别",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }

            // 卡片模式（完整编辑页）：正文+自定义字段+基本信息(cardTrailer)同一张淡底卡，
            // 卡标题「基本信息」；显隐仍由 AnimatedVisibility 承担；平铺模式（快捷编辑面板）：维持原结构
            if (bodyInCard) {
                AnimatedVisibility(visible = config.speechRule.target == SpeechTarget.TAG) {
                    SectionCard(
                        title = "基本信息",
                        icon = Icons.Default.Info,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                            ruleTagBody()
                            val currentRule = speechRule
                            if (currentRule != null) {
                                CustomTagScreen(
                                    info = config.speechRule,
                                    onInfoChange = {
                                        if (config.speechRule.target == SpeechTarget.TAG)
                                            onSysttsChange(systts.copy(config = config.copy(speechRule = it)))
                                    },
                                    speechRule = currentRule
                                )
                            }
                        }
                        cardTrailer()
                    }
                }
            } else {
                AnimatedVisibility(visible = config.speechRule.target == SpeechTarget.TAG) {
                    Column { ruleTagBody() }
                }

                speechRule?.let {
                    CustomTagScreen(
                        info = config.speechRule,
                        onInfoChange = {
                            if (config.speechRule.target == SpeechTarget.TAG)
                                onSysttsChange(systts.copy(config = config.copy(speechRule = it)))
                        },
                        speechRule = it
                    )
                }
            }
        }
}

@Composable
private fun CustomTagScreen(
    info: SpeechRuleInfo,
    onInfoChange: (SpeechRuleInfo) -> Unit,
    speechRule: SpeechRule,
) {
    var showHelpDialog by remember { mutableStateOf("" to "") }
    if (showHelpDialog.first.isNotEmpty()) {
        AppDialog(title = { Text(showHelpDialog.first) }, content = {
            Text(showHelpDialog.second)
        }, buttons = {
            TextButton(onClick = { showHelpDialog = "" to "" }) {
                Text(stringResource(id = R.string.confirm))
            }
        }, onDismissRequest = { showHelpDialog = "" to "" })
    }

    Column(Modifier.padding(vertical = 4.dp)) {
        speechRule.tagsData[info.tag]?.forEach { defTag ->
            val key = defTag.key
            val label = defTag.value["label"] ?: ""
            val hint = defTag.value["hint"] ?: ""

            val items = defTag.value["items"]
            val value by rememberUpdatedState(newValue = info.tagData[key] ?: "")
            if (items.isNullOrEmpty()) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    leadingIcon = {
                        if (hint.isNotEmpty())
                            IconButton(onClick = { showHelpDialog = label to hint }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.HelpOutline,
                                    stringResource(id = R.string.help)
                                )
                            }
                    },
                    label = { Text(label) },
                    value = value,
                    onValueChange = {
                        onInfoChange(
                            info.copy(
                                tagData = info.tagData.toMutableMap().apply {
                                    this[key] = it
                                }
                            )
                        )
                    }
                )
            } else {
                val itemsMap by rememberUpdatedState(
                    newValue = AppConst.jsonBuilder.decodeFromString<Map<String, String>>(items)
                )

                val defaultValue = remember { defTag.value["default"] ?: "" }
                AppSpinner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    labelText = label,
                    value = value.ifEmpty { defaultValue },
                    values = itemsMap.keys.toList(),
                    entries = itemsMap.values.toList(),
                    leadingIcon = {
                        if (hint.isNotEmpty())
                            IconButton(onClick = { showHelpDialog = label to hint }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.HelpOutline,
                                    stringResource(id = R.string.help)
                                )
                            }
                    },
                    onSelectedChange = { k, _ ->
                        onInfoChange(
                            info.copy(
                                tagData = info.tagData.toMutableMap().apply {
                                    this[key] = k as String
                                }
                            )
                        )
                    }
                )

            }

        }
    }
}