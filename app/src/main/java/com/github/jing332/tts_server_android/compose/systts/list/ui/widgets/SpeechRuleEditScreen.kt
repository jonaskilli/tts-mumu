package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Checkbox
import com.github.jing332.compose.widgets.AppDropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.github.jing332.common.utils.ClipboardUtils
import com.github.jing332.common.utils.StringUtils
import com.github.jing332.common.utils.longToast
import com.github.jing332.common.utils.toast
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.AppSpinner
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.SpeechRuleInfo
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.list.BasicAudioParamsDialog
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

        android.util.Log.i(
            "PluginTtsUI",
            "[参数链] tagName回调写入 speed=${config.audioParams.speed} systts@${System.identityHashCode(systts)}"
        )
        onSysttsChange(
            systts.copy(
                config = config.copy(config.speechRule.copy(tagName = tagName))
            )
        )
        true
    }

    var showStandbyHelpDialog by remember { mutableStateOf(false) }
    if (showStandbyHelpDialog)
        AppDialog(
            title = { Text(stringResource(id = R.string.systts_as_standby_help)) },
            content = {
                Text(
                    stringResource(id = R.string.systts_standby_help_msg)
                )
            },
            buttons = {
                TextButton(onClick = { showStandbyHelpDialog = false }) {
                    Text(stringResource(id = R.string.confirm))
                }
            },
            onDismissRequest = { showStandbyHelpDialog = false }
        )

    // 「音频参数」弹窗与下方滑块共用同一份 audioParams，两入口天然同步；
    // 提供大号滑块的快捷调整入口，重置语义与滑块一致（=1.0）
    var showParamsDialog by remember { mutableStateOf(false) }
    if (showParamsDialog) {
        val params = config.audioParams
        fun changeParams(speed: Float = params.speed, volume: Float = params.volume, pitch: Float = params.pitch) {
            onSysttsChange(
                systts.copy(
                    config = config.copy(audioParams = AudioParams(speed, volume, pitch))
                )
            )
        }

        BasicAudioParamsDialog(
            title = { Text(stringResource(id = R.string.audio_params)) },
            onDismissRequest = { showParamsDialog = false },
            resetValue = 1f,
            speed = params.speed,
            onSpeedChange = { changeParams(speed = it) },
            volume = params.volume,
            onVolumeChange = { changeParams(volume = it) },
            pitch = params.pitch,
            onPitchChange = { changeParams(pitch = it) },

            onReset = { changeParams(1f, 1f, 1f) }
        )
    }

    if (showSpeechTarget)
        Column(modifier.fillMaxWidth()) {
            // 第6项: 子分组(categoryPath)编辑已统一由 BasicInfoEditScreen 的分组树选择器负责,
            // 此处不再重复提供子分组输入,避免同一编辑流程出现两个 categoryPath 编辑入口。

            Row(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .horizontalScroll(rememberScrollState())
            ) {
                TextButton(onClick = { showParamsDialog = true }) {
                    Row {
                        Icon(Icons.Default.Speed, stringResource(R.string.audio_params))
                        Text(stringResource(id = R.string.audio_params))
                    }
                }

                Row(
                    Modifier
                        .minimumInteractiveComponentSize()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(role = Role.Checkbox) {
                            onSysttsChange(
                                systts.copy(
                                    config = config.copy(
                                        speechRule = config.speechRule.copy(isStandby = !config.speechRule.isStandby)
                                    )
                                )
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = config.speechRule.isStandby, onCheckedChange = null)
                    Text(stringResource(id = R.string.as_standby))
                    IconButton(onClick = { showStandbyHelpDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            stringResource(id = R.string.systts_as_standby_help)
                        )
                    }
                }
            }

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
                    Text(stringResource(id = R.string.ra_all), maxLines = 1)
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = isInnerThought,
                        onClick = {
                            if (config.speechRule.target != SpeechTarget.TAG) return@FilterChip
                            val newTag =
                                if (isInnerThought) "" else InnerThoughtClassifier.INNER_THOUGHT_TAG
                            onSysttsChange(
                                systts.copy(
                                    config = config.copy(
                                        speechRule = config.speechRule.copy(tag = newTag)
                                    )
                                )
                            )
                        },
                        label = { Text("心声(内心独白)") }
                    )
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