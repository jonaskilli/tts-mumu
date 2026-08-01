package com.github.jing332.tts_server_android.compose.systts.list.ui

import android.util.Log
import android.widget.LinearLayout
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drake.net.utils.withIO
import com.github.jing332.common.utils.toScale
import com.github.jing332.common.utils.toast
import com.github.jing332.compose.widgets.AppSpinner
import com.github.jing332.compose.widgets.LabelSlider
import com.github.jing332.compose.widgets.LoadingContent
import com.github.jing332.compose.widgets.LoadingDialog
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.BasicAudioFormat
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.AuditionDialog
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.AuditionTextField
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.BasicInfoEditScreen
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.SaveActionHandler
import com.github.jing332.tts_server_android.constant.SpeechTarget
import com.github.jing332.tts_server_android.model.rhino.speech_rule.SpeechRuleEngine
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PluginTtsUI : IConfigUI() {
    companion object {
        const val TAG = "PluginTtsUI"
    }

    @Composable
    override fun ParamsEditScreen(
        modifier: Modifier,
        systemTts: SystemTtsV2,
        onSystemTtsChange: (SystemTtsV2) -> Unit,
    ) {
        val tts = (systemTts.config as TtsConfigurationDTO).source as PluginTtsSource
        val config = systemTts.config as TtsConfigurationDTO
        Column(modifier) {
            LabelSlider(
                text = stringResource(R.string.label_speech_rate, "%.2f".format(tts.speed)),
                value = tts.speed,
                onValueChange = {
                    onSystemTtsChange(
                        systemTts.copy(
                            config = config.copy(
                                source = tts.copy(speed = it.toScale(2))
                            )
                        )
                    )
                },
                valueRange = 0.1f..3f,
                step = 0.05f
            )

            LabelSlider(
                text = stringResource(R.string.label_speech_volume, "%.2f".format(tts.volume)),
                value = tts.volume,                 onValueChange = {
                    onSystemTtsChange(
                        systemTts.copy(
                            config = config.copy(
                                source = tts.copy(volume = it.toScale(2))
                            )
                        )
                    )
                }, valueRange = 0.1f..3f,
                step = 0.05f
            )

            LabelSlider(
                text = stringResource(R.string.label_speech_pitch, "%.2f".format(tts.pitch)),
                value = tts.pitch,                 onValueChange = {
                    onSystemTtsChange(
                        systemTts.copy(
                            config = config.copy(
                                source = tts.copy(pitch = it.toScale(2))
                            )
                        )
                    )
                }, valueRange = 0.1f..3f,
                step = 0.05f
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    onSystemTtsChange(
                        systemTts.copy(
                            config = config.copy(
                                source = tts.copy(speed = 1f, volume = 1f, pitch = 1f),
                                audioParams = config.audioParams.copy(speed = 1f, volume = 1f, pitch = 1f)
                            )
                        )
                    )
                }) {
                    Text(stringResource(id = R.string.reset))
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun FullEditScreen(
        modifier: Modifier,
        systemTts: SystemTtsV2,
        onSystemTtsChange: (SystemTtsV2) -> Unit,
        onSave: () -> Unit,
        onCancel: () -> Unit,
        content: @Composable () -> Unit,
    ) {
        DefaultFullEditScreen(
            modifier,
            title = stringResource(id = R.string.edit_plugin_tts),
            onCancel = onCancel,
            onSave = onSave,
        ) {
            content()
            EditContentScreen(systts = systemTts, onSysttsChange = onSystemTtsChange,)
        }
    }

    @Composable
    fun EditContentScreen(
        modifier: Modifier = Modifier,
        systts: SystemTtsV2,
        onSysttsChange: (SystemTtsV2) -> Unit,
        showBasicInfo: Boolean = true,
        plugin: Plugin? = null,
        vm: PluginTtsViewModel = viewModel(),
        // 是否显示切换插件的选择框（预览界面设为 false，只显示当前插件 UI，避免混乱）
        showPluginSelector: Boolean = true,
    ) {
        var displayName by remember { mutableStateOf("") }

        @Suppress("NAME_SHADOWING")
        val systts by rememberUpdatedState(newValue = systts)
        val tts by rememberUpdatedState(newValue = (systts.config as TtsConfigurationDTO).source as PluginTtsSource)
        val isUiOnly = tts.isUiOnly
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        // 角色管理插件判定：pluginId为mingwuyan且name含"角色管理"
        // 仅此插件可开启仅界面模式，且自动启用
        val isRoleManagementPlugin = remember(tts.pluginId, plugin?.name) {
            tts.pluginId == "mingwuyan" &&
                (plugin?.name?.contains("角色管理") ?: true)
        }

        // 角色管理插件自动启用仅界面模式；非角色管理插件强制关闭
        LaunchedEffect(isRoleManagementPlugin, tts.pluginId) {
            if (isRoleManagementPlugin && !tts.isUiOnly) {
                onSysttsChange(
                    systts.copy(
                        config = (systts.config as TtsConfigurationDTO).copy(
                            source = tts.copy(isUiOnly = true)
                        )
                    )
                )
            } else if (!isRoleManagementPlugin && tts.isUiOnly) {
                onSysttsChange(
                    systts.copy(
                        config = (systts.config as TtsConfigurationDTO).copy(
                            source = tts.copy(isUiOnly = false)
                        )
                    )
                )
            }
        }

        LaunchedEffect(Unit) {
            vm.loadPluginList()
        }

        SaveActionHandler {
            if (tts.isUiOnly) {
                // 仅界面模式：无需读取采样率/解码信息，直接保存
                onSysttsChange(systts)
                true
            } else {
            val sampleRate = try {
                withIO { vm.engine.getSampleRate(tts.locale, tts.voice) ?: 16000 }
            } catch (e: Exception) {
                context.displayErrorDialog(
                    e,
                    context.getString(R.string.plugin_tts_get_sample_rate_failed)
                )
                null
            }

            val isNeedDecode = try {
                withIO { vm.engine.isNeedDecode(tts.locale, tts.voice) }
            } catch (e: Exception) {
                context.displayErrorDialog(
                    e,
                    context.getString(R.string.plugin_tts_get_need_decode_failed)
                )
                null
            }

            if (sampleRate != null && isNeedDecode != null) {
                onSysttsChange(
                    systts.copy(
                        displayName = if (systts.displayName.isNullOrBlank()) displayName else systts.displayName,
                        config = (systts.config as TtsConfigurationDTO).copy(
                            audioFormat = BasicAudioFormat(
                                sampleRate = sampleRate,
                                isNeedDecode = isNeedDecode
                            )
                        ),
                    )
                )

                true
            } else
                false
            }
        }

        var showLoadingDialog by remember { mutableStateOf(false) }
        if (showLoadingDialog)
            LoadingDialog(onDismissRequest = { showLoadingDialog = false })

        // 声音选择框中多选的发音人ID集合（用于批量保存到配置列表）
        var selectedVoiceIds by remember { mutableStateOf<Set<Any>>(emptySet()) }

        var auditionSystts by remember { mutableStateOf<SystemTtsV2?>(null) }
        // 当前试听对应的发音人ID（用于分类分配回调）
        var auditionVoiceId by remember { mutableStateOf<Any?>(null) }
        // 发音人 → 分类名（分配了分类的发音人保存时走新逻辑）
        var voiceCategoryMap by remember { mutableStateOf<Map<Any, String>>(emptyMap()) }
        // 开关1：试听弹窗等待分类（播放完毕不自动关闭）
        var waitCategorySwitch by remember { mutableStateOf(false) }
        // 开关2：点分类后自动试听下一个
        var autoNextSwitch by remember { mutableStateOf(false) }

        // 切换到指定发音人试听
        fun startAuditionForVoice(voice: com.github.jing332.tts.speech.plugin.engine.TtsPluginUiEngineV2.Voice) {
            auditionVoiceId = voice.id
            auditionSystts = systts.copy(
                displayName = voice.name,
                config = (systts.config as TtsConfigurationDTO).copy(
                    source = tts.copy(voice = voice.id)
                )
            )
        }

        // 当前试听发音人在列表中的索引
        val currentVoiceIndex = remember(auditionVoiceId, vm.voices) {
            vm.voices.indexOfFirst { it.id == auditionVoiceId }
        }

        @Suppress("UNCHECKED_CAST")
        if (auditionSystts != null)
            AuditionDialog(
                systts = auditionSystts!!,
                engine = if (plugin == null) null else vm.service(),
                voiceId = auditionVoiceId,
                autoDismiss = !waitCategorySwitch,
                hasPrev = currentVoiceIndex > 0,
                hasNext = currentVoiceIndex >= 0 && currentVoiceIndex < vm.voices.size - 1,
                onCategoryAssigned = { voiceId, category ->
                    voiceCategoryMap = voiceCategoryMap + (voiceId to category)
                    if (voiceId !in selectedVoiceIds) {
                        selectedVoiceIds = selectedVoiceIds + voiceId
                    }
                    val nextIndex = currentVoiceIndex + 1
                    // 脱离弹窗协程：先关闭当前弹窗，延迟一帧后再打开下一个，确保旧 composition 完全 dispose
                    scope.launch(Dispatchers.Main) {
                        auditionSystts = null
                        auditionVoiceId = null
                        if (autoNextSwitch && nextIndex in vm.voices.indices) {
                            kotlinx.coroutines.delay(50)
                            startAuditionForVoice(vm.voices[nextIndex])
                        }
                    }
                },
                onPrev = {
                    val prevIndex = currentVoiceIndex - 1
                    if (prevIndex >= 0) {
                        scope.launch(Dispatchers.Main) {
                            auditionSystts = null
                            auditionVoiceId = null
                            kotlinx.coroutines.delay(50)
                            startAuditionForVoice(vm.voices[prevIndex])
                        }
                    }
                },
                onNext = {
                    val nextIndex = currentVoiceIndex + 1
                    if (nextIndex < vm.voices.size) {
                        scope.launch(Dispatchers.Main) {
                            auditionSystts = null
                            auditionVoiceId = null
                            kotlinx.coroutines.delay(50)
                            startAuditionForVoice(vm.voices[nextIndex])
                        }
                    }
                }
            ) {
                auditionSystts = null
                auditionVoiceId = null
            }

        Column(modifier) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                if (showBasicInfo)
                    BasicInfoEditScreen(
                        Modifier.fillMaxWidth(),
                        systemTts = systts,
                        onSystemTtsChange = onSysttsChange
                    )

                if (!isUiOnly) {
                    AuditionTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        onAudition = {
                            auditionSystts = systts
                        }
                    )
                }

                if (showPluginSelector && !isUiOnly) {
                    AppSpinner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        labelText = stringResource(R.string.plugin), 
                        value = tts.pluginId,
                        values = vm.pluginList.map { it.pluginId },
                        entries = vm.pluginList.map { it.name },
                        onSelectedChange = { id, _ ->
                            if (id == tts.pluginId) return@AppSpinner
                            onSysttsChange(
                                systts.copy(
                                    config = (systts.config as TtsConfigurationDTO).copy(
                                        source = tts.copy(
                                            pluginId = id as String,
                                            locale = "",
                                            voice = ""
                                        )
                                    )
                                )
                            )
                        }
                    )
                }

                // 仅界面模式：仅角色管理插件(mingwuyan)可开启，其他插件不显示此开关
                if (isRoleManagementPlugin) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.plugin_ui_only_mode))
                        Switch(
                            checked = isUiOnly,
                            onCheckedChange = { checked ->
                                onSysttsChange(
                                    systts.copy(
                                        config = (systts.config as TtsConfigurationDTO).copy(
                                            source = tts.copy(isUiOnly = checked)
                                        )
                                    )
                                )
                            }
                        )
                    }
                }

                key(tts.pluginId) {
                    val customViewLayout = remember { LinearLayout(context).apply { orientation = LinearLayout.VERTICAL } }
                    
                    LaunchedEffect(tts.pluginId) {
                        runCatching {
                            vm.load(context, plugin, tts, customViewLayout)
                        }.onFailure {
                            it.printStackTrace()
                            context.displayErrorDialog(it)
                        }
                    }

                    LoadingContent(isLoading = vm.isLoading) {
                        if (!isUiOnly) {
                        Column {
                            AppSpinner(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                labelText = stringResource(R.string.language),
                                value = tts.locale,
                                values = vm.locales.map { it.first },
                                entries = vm.locales.map { it.second },
                                onSelectedChange = { locale, _ ->
                                    Log.d("PluginTtsUI", "locale onSelectedChange: $locale")
                                    if (locale.toString().isBlank() || locale == tts.locale) return@AppSpinner
                                    onSysttsChange(systts.copySource(tts.copy(locale = locale.toString())))
                                    runCatching {
                                        scope.launch(Dispatchers.IO) {
                                            vm.updateVoices(locale.toString())
                                        }
                                    }
                                },
                            )

                            AppSpinner(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                labelText = stringResource(R.string.label_voice),
                                value = tts.voice,
                                values = vm.voices.map { it.id },
                                entries = vm.voices.map { it.name },
                                icons = vm.voices.map { it.icon },
                                onSelectedChange = { voice, name ->
                                    if (voice == tts.voice || vm.isLoading) return@AppSpinner

                                    val lastName = vm.voices.find { it.id == tts.voice }?.name ?: ""
                                    onSysttsChange(
                                        systts.copy(
                                            displayName =
                                            if (systts.displayName.isNullOrBlank() || lastName == systts.displayName) name
                                            else systts.displayName,
                                            config = (systts.config as TtsConfigurationDTO).copy(
                                                source = tts.copy(
                                                    voice = voice as String
                                                )
                                            )
                                        )
                                    )

                                    runCatching {
                                        vm.updateCustomUI(tts.locale, voice as String)
                                    }.onFailure {
                                        context.displayErrorDialog(it)
                                    }

                                    displayName = name
                                },
                            onEntryLongClick = { voice, name ->
                                auditionSystts = systts.copy(
                                    displayName = name,
                                    config = (systts.config as TtsConfigurationDTO).copy(
                                        source = tts.copy(voice = voice as String)
                                    )
                                )
                            },
                            trailingContent = { voice, name, onHighlight ->
                                IconButton(onClick = {
                                    onHighlight()
                                    auditionVoiceId = voice
                                    auditionSystts = systts.copy(
                                        displayName = name,
                                        config = (systts.config as TtsConfigurationDTO).copy(
                                            source = tts.copy(voice = voice as String)
                                        )
                                    )
                                }) {
                                    Icon(Icons.Default.Headset, stringResource(id = R.string.audition))
                                }
                            },
                            selectedMultiValues = selectedVoiceIds,
                            onMultiSelectedChange = { selectedVoiceIds = it },
                            categoryMap = voiceCategoryMap,
                            onCategoryChange = { voiceId, category ->
                                voiceCategoryMap = if (category == null) {
                                    voiceCategoryMap - voiceId
                                } else {
                                    voiceCategoryMap + (voiceId to category)
                                }
                            },
                            waitCategorySwitch = waitCategorySwitch,
                            onWaitCategorySwitchChange = {
                                waitCategorySwitch = it
                                context.toast(if (it) "已开启：试听后等待选择分类" else "已关闭：试听后自动关闭")
                            },
                            autoNextSwitch = autoNextSwitch,
                            onAutoNextSwitchChange = {
                                autoNextSwitch = it
                                context.toast(if (it) "已开启：选分类后自动试听下一个" else "已关闭：选分类后不自动切换")
                            },
                            extraButtons = {
                                TextButton(
                                    enabled = selectedVoiceIds.isNotEmpty(),
                                    onClick = {
                                        val selectedVoices = vm.voices.filter { it.id in selectedVoiceIds }
                                        if (selectedVoices.isEmpty()) return@TextButton
                                        scope.launch(Dispatchers.IO) {
                                            val config = systts.config as TtsConfigurationDTO
                                            val ruleData = config.speechRule.copy()
                                            // 获取当前标签规则及有序标签列表
                                            val speechRule: SpeechRule? =
                                                if (ruleData.tagRuleId.isNotBlank())
                                                    dbm.speechRuleDao.getByRuleId(ruleData.tagRuleId)
                                                else null
                                            val tagKeys = speechRule?.tags?.keys?.toList() ?: emptyList()
                                            // 第一个用当前选中的标签，后续按标签列表顺序向下延续（取模循环）
                                            val startIndex = if (tagKeys.isNotEmpty())
                                                tagKeys.indexOf(ruleData.tag).coerceAtLeast(0)
                                            else 0

                                            // 各分类下已有数量（用于序号起点，避免重号）
                                            val categoryCountMap = mutableMapOf<String, Int>()
                                            // 未分配分类的序号计数
                                            var untaggedIdx = 0

                                            selectedVoices.forEach { voice ->
                                                val category = voiceCategoryMap[voice.id]
                                                val newRuleData = config.speechRule.copy()
                                                var categoryPath = ""

                                                if (category != null) {
                                                    // —— 分配了分类：新逻辑 ——
                                                    val count = categoryCountMap.getOrDefault(category, 0)
                                                    // 查询该子分组下已有数量作为起点
                                                    val existing = if (count == 0) {
                                                        dbm.systemTtsV2.getByGroup(systts.groupId)
                                                            .count { it.categoryPath == category }
                                                    } else count
                                                    val seq = existing + 1
                                                    categoryCountMap[category] = seq
                                                    val tagLabel = category + String.format("%02d", seq)
                                                    newRuleData.target = SpeechTarget.TAG
                                                    newRuleData.tag = tagLabel
                                                    newRuleData.tagName = tagLabel
                                                    newRuleData.tagRuleId = ruleData.tagRuleId
                                                    categoryPath = category
                                                } else if (tagKeys.isNotEmpty()) {
                                                    // —— 未分配分类：原逻辑 ——
                                                    val tagKey = tagKeys.getOrNull((startIndex + untaggedIdx) % tagKeys.size)
                                                    untaggedIdx++
                                                    if (tagKey != null) {
                                                        newRuleData.target = SpeechTarget.TAG
                                                        newRuleData.tag = tagKey
                                                        newRuleData.tagRuleId = ruleData.tagRuleId
                                                        runCatching {
                                                            speechRule?.let { sr ->
                                                                newRuleData.tagName =
                                                                    SpeechRuleEngine.getTagName(context, sr, newRuleData)
                                                            }
                                                        }
                                                    }
                                                }

                                                val newConfig = config.copy(
                                                    source = tts.copy(voice = voice.id),
                                                    speechRule = newRuleData
                                                )
                                                dbm.systemTtsV2.insert(
                                                    systts.copy(
                                                        id = System.currentTimeMillis() + untaggedIdx + categoryCountMap.values.sum(),
                                                        displayName = voice.name,
                                                        categoryPath = categoryPath,
                                                        config = newConfig
                                                    )
                                                )
                                            }
                                            withContext(Dispatchers.Main) {
                                                if (systts.isEnabled) SystemTtsService.notifyUpdateConfig()
                                                context.toast(
                                                    context.getString(R.string.save_success) + " (${selectedVoices.size})"
                                                )
                                                selectedVoiceIds = emptySet()
                                                voiceCategoryMap = emptyMap()
                                            }
                                        }
                                    }
                                ) {
                                    Text(stringResource(id = R.string.save))
                                }
                            }
                        )
                        }
                    }

                    // 插件自定义 UI 始终展示，即使界面模式(isUiOnly)下也可见
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        factory = { customViewLayout }
                    )
                }
                }

            }

            if (!isUiOnly) {
                ParamsEditScreen(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    systemTts = systts,
                    onSystemTtsChange = onSysttsChange
                )
            }
        }
    }
}
