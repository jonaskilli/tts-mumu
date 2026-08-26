package com.github.jing332.tts_server_android.compose.systts.list.ui

import android.util.Log
import android.widget.LinearLayout
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
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
import com.github.jing332.common.audio.AudioDecoder
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
import com.github.jing332.database.entities.systts.SystemTtsGroup
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.database.entities.systts.source.TextToSpeechSource
import com.github.jing332.tts.speech.TextToSpeechProvider
import com.github.jing332.tts.synthesizer.SystemParams
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.conf.AppConfig
import com.github.jing332.tts_server_android.conf.SysTtsConfig
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
        // 是否显示「仅界面模式」开关：角色管理栏顶部已有独立开关，传 false 隐藏内容区开关节省空间
        showUiOnlySwitch: Boolean = true,
        // 插件 UI 重建触发器：变化时强制重新 onLoadUI（用于运行规则后刷新角色列表）
        reloadKey: Any? = null,
        // 批量保存成功后的回调：用于关闭当前界面（如预览 Activity finish），
        // 不传则停留原地（如工具箱页，保存后列表就在本页）
        onSaved: (() -> Unit)? = null,
    ) {
        var displayName by remember { mutableStateOf("") }

        @Suppress("NAME_SHADOWING")
        val systts by rememberUpdatedState(newValue = systts)
        val tts by rememberUpdatedState(newValue = (systts.config as TtsConfigurationDTO).source as PluginTtsSource)
        val isUiOnly = tts.isUiOnly
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

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

        // 批量保存中：显示带进度的加载弹窗。保存会对每个声音合成音频解析采样率，
        // 耗时可达数十秒，无反馈时容易被误认为"点了保存没反应/没保存成功"
        var showLoadingDialog by remember { mutableStateOf(false) }
        var savingProgressText by remember { mutableStateOf("") }
        if (showLoadingDialog)
            LoadingDialog(
                onDismissRequest = { },
                text = savingProgressText.takeIf { it.isNotBlank() }
            )

        // 声音选择框中多选的发音人ID集合（用于批量保存到配置列表）
        var selectedVoiceIds by remember { mutableStateOf<Set<Any>>(emptySet()) }

        var auditionSystts by remember { mutableStateOf<SystemTtsV2?>(null) }
        // 当前试听对应的发音人ID（用于分类分配回调）
        var auditionVoiceId by remember { mutableStateOf<Any?>(null) }
        // 发音人 → 分类名（分配了分类的发音人保存时走新逻辑）
        var voiceCategoryMap by remember { mutableStateOf<Map<Any, String>>(emptyMap()) }
        // 发音人 → 真实采样率（试听时从音频解析缓存，批量保存直接复用，无需再合成）
        var voiceSampleRateCache by remember { mutableStateOf<Map<Any, Int>>(emptyMap()) }
        // 开关1：试听弹窗等待分类（播放完毕不自动关闭）
        var waitCategorySwitch by remember { mutableStateOf(false) }
        // 开关2：点分类后自动试听下一个
        var autoNextSwitch by remember { mutableStateOf(false) }
        // 开关3：按插件分区（语言）自动分组，未手动分配分类时以其显示名作 categoryPath
        var autoGroupByLocale by remember { mutableStateOf(false) }
        val currentLocaleName =
            vm.locales.firstOrNull { it.first == tts.locale }?.second ?: tts.locale

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
                // 带分类回调（批量试听分类场景）时，播放完成不自动关闭弹窗，
                // 否则用户来不及选分类、且当前声音高亮/分类选择被打断
                autoDismiss = !waitCategorySwitch,
                hasPrev = currentVoiceIndex > 0,
                hasNext = currentVoiceIndex >= 0 && currentVoiceIndex < vm.voices.size - 1,
                onCategoryAssigned = { voiceId, category ->
                    // category == null 表示取消分配（归为默认），与列表长按分类「取消」语义一致
                    voiceCategoryMap = if (category == null) {
                        voiceCategoryMap - voiceId
                    } else {
                        voiceCategoryMap + (voiceId to category)
                    }
                    if (category != null && voiceId !in selectedVoiceIds) {
                        selectedVoiceIds = selectedVoiceIds + voiceId
                    }
                    // 原地切换到下一个：仅真正分配分类时才自动跳下一个，
                    // 取消分配（category == null）不触发跳转，避免误触后直接跳过
                    val nextIndex = currentVoiceIndex + 1
                    if (category != null && autoNextSwitch && nextIndex in vm.voices.indices) {
                        startAuditionForVoice(vm.voices[nextIndex])
                    }
                },
                onPrev = {
                    val prevIndex = currentVoiceIndex - 1
                    if (prevIndex >= 0) {
                        startAuditionForVoice(vm.voices[prevIndex])
                    }
                },
                onNext = {
                    val nextIndex = currentVoiceIndex + 1
                    if (nextIndex < vm.voices.size) {
                        startAuditionForVoice(vm.voices[nextIndex])
                    }
                },
                assignedCategory = voiceCategoryMap[auditionVoiceId],
                progressText = if (currentVoiceIndex >= 0 && vm.voices.size > 1)
                    "${currentVoiceIndex + 1}/${vm.voices.size}" else null,
                onSampleRateResolved = { vid, rate ->
                    if (vid != null) voiceSampleRateCache = voiceSampleRateCache + (vid to rate)
                }
            ) {
                auditionSystts = null
                auditionVoiceId = null
            }

        Column(modifier) {
            // 仅界面模式开关仅对角色管理类插件显示：兼容插件换 pluginId 后按名称回退识别
            val isRoleManagementPlugin = remember(tts.pluginId) {
                tts.pluginId == "mingwuyan" ||
                    dbm.pluginDao.getByPluginId(tts.pluginId)?.name?.contains("角色管理") == true
            }
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
                        onSelectedChange = { id, name ->
                            if (id == tts.pluginId) return@AppSpinner
                            // 切换插件：清空跨插件残留状态（多选/分类/缓存/试听）
                            selectedVoiceIds = emptySet()
                            voiceCategoryMap = emptyMap()
                            voiceSampleRateCache = emptyMap()
                            auditionSystts = null
                            auditionVoiceId = null
                            vm.voices.clear()
                            vm.locales.clear()
                            // 显示名跟随新插件名；非角色管理类插件需退出仅界面模式，否则编辑区被隐藏且无法恢复
                            val newPlugin = vm.pluginList.find { it.pluginId == id }
                            onSysttsChange(
                                systts.copy(
                                    displayName = newPlugin?.name ?: "",
                                    config = (systts.config as TtsConfigurationDTO).copy(
                                        source = tts.copy(
                                            pluginId = id as String,
                                            locale = "",
                                            voice = "",
                                            isUiOnly = false,
                                        )
                                    )
                                )
                            )
                        }
                    )
                }

                key(tts.pluginId, reloadKey) {
                    val customViewLayout = remember { LinearLayout(context).apply { orientation = LinearLayout.VERTICAL } }

                    LaunchedEffect(tts.pluginId, reloadKey) {
                        runCatching {
                            vm.load(context, plugin, tts, customViewLayout)
                        }.onFailure {
                            it.printStackTrace()
                            context.displayErrorDialog(it)
                        }
                    }

                    LoadingContent(isLoading = vm.isLoading) {
                        Column {
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
                                            // 切换发音人时显示名无条件跟随（用户要求）
                                            displayName = name,
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
                                    // 与试听弹窗分配分类行为一致：分配了分类即视为待保存项，
                                    // 否则长按分好类后保存按钮仍禁用、或保存时漏掉该项
                                    if (voiceId !in selectedVoiceIds)
                                        selectedVoiceIds = selectedVoiceIds + voiceId
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
                            autoGroupByLocale = autoGroupByLocale,
                            onAutoGroupByLocaleChange = { autoGroupByLocale = it },
                            extraButtons = {
                                TextButton(
                                    enabled = selectedVoiceIds.isNotEmpty() && !showLoadingDialog,
                                    onClick = {
                                        val selectedVoices = vm.voices.filter { it.id in selectedVoiceIds }
                                        if (selectedVoices.isEmpty()) {
                                            // 勾选项不在当前声音列表（切换语言/插件后列表已刷新）：
                                            // 显式提示而非静默返回，避免"点了保存没反应"
                                            context.toast("所选声音不在当前列表中，可能已切换语言或插件，请重新选择")
                                            return@TextButton
                                        }
                                        // 主线程先捕获状态快照，IO 协程内不再读取 Compose 状态
                                        val categoryMapSnapshot = voiceCategoryMap
                                        val sampleRateCacheSnapshot = voiceSampleRateCache
                                        val systtsSnapshot = systts
                                        val ttsSnapshot = tts
                                        val autoGroupSnapshot = autoGroupByLocale
                                        val localeNameSnapshot = currentLocaleName
                                        showLoadingDialog = true
                                        savingProgressText = ""
                                        scope.launch(Dispatchers.IO) {
                                            runCatching {
                                            val config = systtsSnapshot.config as TtsConfigurationDTO
                                            val ruleData = config.speechRule.copy()

                                            // 目标分组兜底：从插件预览等入口进入时配置项未落库（groupId=0），
                                            // 直接插入会进入不存在的分组导致主界面不可见。
                                            // 此时按插件名新建（或复用同名）分组承载，并在结果提示中告知分组名。
                                            val existingGroup =
                                                if (systtsSnapshot.groupId != 0L) dbm.systemTtsV2.getGroup(systtsSnapshot.groupId) else null
                                            val targetGroupId: Long
                                            val targetGroupName: String
                                            if (existingGroup != null) {
                                                targetGroupId = existingGroup.id
                                                targetGroupName = existingGroup.name
                                            } else {
                                                val pluginName = plugin?.name
                                                    ?: dbm.pluginDao.getByPluginId(ttsSnapshot.pluginId)?.name
                                                    ?: "插件分组"
                                                val sameName = dbm.systemTtsV2.allGroup()
                                                    .firstOrNull { it.group.name == pluginName }
                                                if (sameName != null) {
                                                    targetGroupId = sameName.group.id
                                                    targetGroupName = sameName.group.name
                                                } else {
                                                    val group = SystemTtsGroup(
                                                        name = pluginName,
                                                        order = dbm.systemTtsV2.groupCount,
                                                        // 新建分组默认展开：保存完成后立即可见，避免找不到保存项
                                                        isExpanded = true
                                                    )
                                                    dbm.systemTtsV2.insertGroup(group)
                                                    targetGroupId = group.id
                                                    targetGroupName = group.name
                                                }
                                            }

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

                                            // 循环外仅 eval 一次规则引擎并复用：
                                            // 每个声音重复 eval 整个 JS（数千行）开销过大
                                            val ruleEngine = speechRule?.let { sr ->
                                                runCatching {
                                                    SpeechRuleEngine(context, sr).apply { eval() }
                                                }.getOrNull()
                                            }

                                            // 各分类下已有数量（用于序号起点，避免重号）
                                            val categoryCountMap = mutableMapOf<String, Int>()
                                            // 未分配分类的序号计数
                                            var untaggedIdx = 0
                                            // 批次内自增序号：与时间戳基值组合保证 ID 唯一，
                                            // 避免原「untaggedIdx+分类数之和」组合可能撞车（REPLACE 会静默覆盖）
                                            val baseId = System.currentTimeMillis()
                                            var idSeq = 0
                                            // 排序追加起点：目标分组内现有最大 order + 1。
                                            // 新项若全部继承模板项的 order，与已有项互相冲突导致列表顺序混乱
                                            val baseOrder = (dbm.systemTtsV2.getByGroup(targetGroupId)
                                                .maxOfOrNull { it.order } ?: -1) + 1
                                            var orderSeq = 0

                                            selectedVoices.forEachIndexed { voiceIdx, voice ->
                                                // 进度反馈：每个声音可能触发一次合成来解析采样率，
                                                // N 个声音耗时可达数十秒，必须让用户看到正在处理
                                                withContext(Dispatchers.Main) {
                                                    savingProgressText =
                                                        "正在保存 ${voiceIdx + 1}/${selectedVoices.size}：${voice.name}"
                                                }
                                                val category = categoryMapSnapshot[voice.id]
                                                val newRuleData = config.speechRule.copy()
                                                // 未分配分类时保留用户在分组树中已选的子分组路径，
                                                // 不再被强制置空导致保存位置丢失
                                                var categoryPath = systtsSnapshot.categoryPath

                                                // 按语言自动分组：未手动分配分类时以当前语言显示名作 categoryPath
                                                if (category == null && autoGroupSnapshot && localeNameSnapshot.isNotBlank()) {
                                                    categoryPath = localeNameSnapshot
                                                }

                                                if (category != null) {
                                                    // —— 分配了分类：标签依据朗读规则生成 ——
                                                    val count = categoryCountMap.getOrDefault(category, 0)
                                                    // 查询该子分组下已有数量作为起点
                                                    val existing = if (count == 0) {
                                                        dbm.systemTtsV2.getByGroup(targetGroupId)
                                                            .count { it.categoryPath == category }
                                                    } else count
                                                    val seq = existing + 1
                                                    categoryCountMap[category] = seq
                                                    // 优先由朗读规则自定义生成（每套规则可有不同逻辑），
                                                    // 未实现 getCategoryTag 或返回空时回退「分类名+两位序号」
                                                    // 「旁白」为单一角色分类，不带序号
                                                    val tagLabel = if (category == "旁白") {
                                                        category
                                                    } else {
                                                        ruleEngine?.getCategoryTag(category, seq)
                                                            ?: (category + String.format(
                                                                java.util.Locale.US, "%02d", seq
                                                            ))
                                                    }
                                                    newRuleData.target = SpeechTarget.TAG
                                                    newRuleData.tag = tagLabel
                                                    // 标签名同样优先走规则的 getTagName（各规则自己的取名逻辑），
                                                    // 取不到再用 tag 本身
                                                    newRuleData.tagName = runCatching {
                                                        ruleEngine?.getTagName(tagLabel, newRuleData.tagData)
                                                    }.getOrNull()?.takeIf { it.isNotBlank() } ?: tagLabel
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
                                                            ruleEngine?.let { re ->
                                                                newRuleData.tagName =
                                                                    re.getTagName(tagKey, newRuleData.tagData)
                                                            }
                                                        }.onFailure {
                                                            // 与 SpeechRuleEngine.getTagName 伴生方法行为一致：
                                                            // JS 未实现 getTagName 时回退 tags 内的显示名
                                                            newRuleData.tagName = speechRule?.tags?.get(tagKey) ?: ""
                                                        }
                                                    }
                                                }

                                                // 解析该声音的真实采样率与是否需要解码：
                                                // 1. 试听时从真实音频解析并缓存的采样率（零额外开销）；
                                                // 2. 插件 JS 实现的 getAudioSampleRate（快且准）；
                                                // 3. 实际合成一次并从音频字节解出采样率（避免落入 16000 默认值）。
                                                val voiceSampleRate = sampleRateCacheSnapshot[voice.id]
                                                    ?: runCatching {
                                                        vm.engine.getSampleRate(ttsSnapshot.locale, voice.id)
                                                    }.getOrNull()?.takeIf { it > 0 } ?: resolveSampleRateBySynth(
                                                    provider = vm.service(),
                                                    config = config,
                                                    voiceId = voice.id,
                                                    tts = ttsSnapshot
                                                )
                                                val voiceNeedDecode = runCatching {
                                                    vm.engine.isNeedDecode(ttsSnapshot.locale, voice.id)
                                                }.getOrNull() ?: config.audioFormat.isNeedDecode

                                                val newConfig = config.copy(
                                                    source = ttsSnapshot.copy(voice = voice.id),
                                                    speechRule = newRuleData,
                                                    audioFormat = BasicAudioFormat(
                                                        sampleRate = voiceSampleRate,
                                                        isNeedDecode = voiceNeedDecode
                                                    )
                                                )
                                                dbm.systemTtsV2.insert(
                                                    systtsSnapshot.copy(
                                                        id = baseId + idSeq++,
                                                        groupId = targetGroupId,
                                                        order = baseOrder + orderSeq++,
                                                        displayName = voice.name,
                                                        categoryPath = categoryPath,
                                                        config = newConfig
                                                    )
                                                )
                                            }
                                            withContext(Dispatchers.Main) {
                                                if (systtsSnapshot.isEnabled) SystemTtsService.notifyUpdateConfig()
                                                // 明确提示保存数量与位置（语音列表/分组），
                                                // 保存成功后直接关闭当前界面，避免停留在编辑页还要手动关闭
                                                context.toast(
                                                    context.getString(
                                                        R.string.save_to_list,
                                                        selectedVoices.size
                                                    ) + " → $targetGroupName"
                                                )
                                                selectedVoiceIds = emptySet()
                                                voiceCategoryMap = emptyMap()
                                                onSaved?.invoke()
                                            }
                                            }.onFailure { e ->
                                                // 保存过程任何异常（DB 写入、分组创建等）都显式提示，
                                                // 不再静默失败让用户误以为已保存
                                                withContext(Dispatchers.Main) {
                                                    context.toast("保存失败：${e.message}")
                                                }
                                            }
                                            // 无论成败都关闭进度弹窗：置于 runCatching 之外，异常路径也能关闭
                                            withContext(Dispatchers.Main) {
                                                showLoadingDialog = false
                                                savingProgressText = ""
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

                        // 仅界面模式开关：放在语音参数之后、插件自定义UI之前
                        if (showUiOnlySwitch && isRoleManagementPlugin) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.plugin_ui_only_mode),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Switch(
                                    checked = isUiOnly,
                                    onCheckedChange = { enabled ->
                                        onSysttsChange(
                                            systts.copy(
                                                config = (systts.config as TtsConfigurationDTO).copy(
                                                    source = tts.copy(isUiOnly = enabled)
                                                )
                                            )
                                        )
                                    }
                                )
                            }
                        }

                        // 插件自定义 UI 始终展示，即使界面模式(isUiOnly)下也可见
                        // 加载期间不做高度动画：插件JS逐个addView会让animateContentSize
                        // 一直表演"从上往下撑开"(观感像黑影掉下来)；加载完成后的零星
                        // 尺寸变化(增删角色行)才保留平滑动画
                        AndroidView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (vm.isLoading) Modifier else Modifier.animateContentSize()),
                            factory = { customViewLayout }
                        )
                        }
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

/**
 * 批量分组保存时，若插件 JS 未实现 getAudioSampleRate，则实际合成一次，
 * 从返回音频字节解析真实采样率，避免落入 16000 默认值。
 */
private suspend fun resolveSampleRateBySynth(
    provider: TextToSpeechProvider<TextToSpeechSource>,
    config: TtsConfigurationDTO,
    tts: PluginTtsSource,
    voiceId: String,
): Int {
    return try {
        val stream = provider.getStream(
            SystemParams(
                text = AppConfig.testSampleText.value,
                speed = config.audioParams.speed,
                volume = config.audioParams.volume,
                pitch = config.audioParams.pitch,
                requestTimeout = SysTtsConfig.requestTimeout.toLong()
            ),
            tts.copy(voice = voiceId)
        )
        val audio = stream.readBytes()
        val rate = AudioDecoder.getSampleRateAndMime(audio).first
        if (rate <= 0) config.audioFormat.sampleRate else rate
    } catch (e: Exception) {
        // 合成失败时退回原配置值，保留用户已有采样率
        config.audioFormat.sampleRate
    }
}
