package com.github.jing332.tts_server_android.compose.systts.list.ui

import android.util.Log
import android.widget.LinearLayout
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Info
import com.github.jing332.tts_server_android.compose.systts.plugin.PluginImage
import androidx.compose.material.icons.filled.Speed
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
import com.github.jing332.common.utils.toScale
import com.github.jing332.common.utils.toast
import com.github.jing332.compose.widgets.AppSpinner
import com.github.jing332.compose.widgets.LabelSlider
import com.github.jing332.compose.widgets.LoadingContent
import com.github.jing332.compose.widgets.LoadingDialog
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.SystemTtsGroup
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.AuditionDialog
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.AuditionTextField
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.BasicInfoEditScreen
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.SaveActionHandler
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.SectionCard
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
        val config = systemTts.config as TtsConfigurationDTO
        val params = config.audioParams
        // 浮点去噪：滑块 `step=0.05f` 在某些步进处会产生 1.0999999 这种 JSON 反序列化噪声；
        // 显示用原始 params.speed，但 onValueChange 写回时统一 round 到 0.01，
        // 保证卡片/滑块/日志三处长期一致（1.10 显示 vs 1.0999999 噪声不会出现）。
        fun snap(v: Float): Float = (kotlin.math.round(v * 100f) / 100f)
        Column(modifier) {
            LabelSlider(
                text = stringResource(R.string.label_speech_rate, "%.2f".format(params.speed)),
                value = params.speed,
                onValueChange = {
                    val v = snap(it)
                    onSystemTtsChange(
                        systemTts.copy(
                            config = config.copy(
                                audioParams = params.copy(speed = v)
                            )
                        )
                    )
                },
                valueRange = 0.1f..3f,
                step = 0.05f
            )

            LabelSlider(
                text = stringResource(R.string.label_speech_volume, "%.2f".format(params.volume)),
                value = params.volume, onValueChange = {
                    val v = snap(it)
                    onSystemTtsChange(
                        systemTts.copy(
                            config = config.copy(
                                audioParams = params.copy(volume = v)
                            )
                        )
                    )
                }, valueRange = 0.1f..3f,
                step = 0.05f
            )

            LabelSlider(
                text = stringResource(R.string.label_speech_pitch, "%.2f".format(params.pitch)),
                value = params.pitch, onValueChange = {
                    val v = snap(it)
                    onSystemTtsChange(
                        systemTts.copy(
                            config = config.copy(
                                audioParams = params.copy(pitch = v)
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
                                audioParams = params.copy(speed = 1f, volume = 1f, pitch = 1f)
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
            // 标签态：正文+基本信息合卡由 SpeechRuleEditScreen(bodyInCard/cardTrailer) 内部处理，
            // 基本信息卡在此关闭；朗读全部态：平铺+基本信息卡原样
            val isTagTarget = (systemTts.config as? TtsConfigurationDTO)
                ?.speechRule?.target == SpeechTarget.TAG
            content()
            EditContentScreen(
                systts = systemTts,
                onSysttsChange = onSystemTtsChange,
                showBasicInfo = isTagTarget.not(),
                showParamsSection = false,
            )
            val isUiOnly = (systemTts.config as? TtsConfigurationDTO)
                ?.source?.let { it as? PluginTtsSource }?.isUiOnly == true
            if (!isUiOnly)
                SectionCard(
                    title = "音频参数",
                    icon = Icons.Default.Speed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    ParamsEditScreen(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        systemTts = systemTts,
                        onSystemTtsChange = onSystemTtsChange
                    )
                }
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
        // 是否在底部渲染「音频参数」卡片：完整编辑页由 FullEditScreen 统一放在朗读标签卡之后，
        // 预览/工具箱等调用方保持默认 true 维持原位
        showParamsSection: Boolean = true,
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
            val oldAudioFormat = (systts.config as TtsConfigurationDTO).audioFormat
            // 采样率/解码探测失败不阻断保存：保留当前格式值，播放时由「采样率自动识别」兜底。
            // 历史上这里 catch 后 return false，插件探测一旦抛异常（引擎初始化失败/桥接异常等）
            // 整条保存被中断，表现为"jread 转来的配置项保存不了"（本地条目不走此路径）。
            val sampleRate = try {
                withIO {
                    vm.engine.getSampleRate(tts.locale, tts.voice)?.takeIf { it > 0 }
                }
            } catch (e: Exception) {
                null
            } ?: oldAudioFormat.sampleRate
            val isNeedDecode = try {
                withIO { vm.engine.isNeedDecode(tts.locale, tts.voice) }
            } catch (e: Exception) {
                null
            } ?: oldAudioFormat.isNeedDecode

            onSysttsChange(
                systts.copy(
                    displayName = if (systts.displayName.isNullOrBlank()) displayName else systts.displayName,
                    config = (systts.config as TtsConfigurationDTO).copy(
                        audioFormat = oldAudioFormat.copy(
                            sampleRate = sampleRate,
                            isNeedDecode = isNeedDecode
                        )
                    ),
                )
            )

            true
            }
        }

        // 批量保存中：显示进度，避免大量声音入库时被误认为没有响应
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
        // 开关1：试听弹窗等待分类（播放完毕不自动关闭）
        var waitCategorySwitch by remember { mutableStateOf(false) }
        // 开关2：点分类后自动试听下一个
        var autoNextSwitch by remember { mutableStateOf(false) }
        // 全部分类入库：忽略勾选，逐分类拉全量，按每条音色所属分类落子分组
        var allPoolsImport by remember { mutableStateOf(false) }

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
                    "${currentVoiceIndex + 1}/${vm.voices.size}" else null
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
            // 分区卡片化：基本信息 / 音色来源 /（朗读与标签由 FullEditScreen 渲染）/ 音频参数
            // 基本信息：保留分区壳但不出标题（用户定稿：分区保留、标题删除）
            if (showBasicInfo)
                SectionCard(
                    title = "基本信息",
                    icon = Icons.Default.Info,
                    showHeader = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    BasicInfoEditScreen(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        systemTts = systts,
                        onSystemTtsChange = onSysttsChange
                    )
                }

            // 音色来源区：ui-only（角色管理栏）时不用卡片壳，直接渲染插件自定义UI躺在 surface 上
            // （用户反馈：去掉分区底色壳≠连插件UI一起消失）；完整编辑模式才包 SectionCard
            // （试听文本/插件选择/语言/声音都在卡内）
            if (isUiOnly) {
                RoleManagementPluginContent(
                    tts = tts,
                    vm = vm,
                    plugin = plugin,
                    reloadKey = reloadKey,
                    onLoadingError = { context.displayErrorDialog(it) }
                )
            } else SectionCard(
                title = "音色来源",
                icon = Icons.Default.Headset,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
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
                        // 插件是切换音色来源的核心入口：主题色加粗+🧩，与普通字段一眼区分
                        label = {
                            Text(
                                "🧩 " + stringResource(R.string.plugin),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        value = tts.pluginId,
                        values = vm.pluginList.map { it.pluginId },
                        entries = vm.pluginList.map { it.name },
                        // 插件列表带各插件自己的图标（与插件管理页同款 PluginImage：
                        // 加载失败/无图标自动显示插件名首字），长列表一眼区分
                        icons = vm.pluginList.map { it.iconUrl },
                        // 插件名长(如“小米 MiMo V2.5 TTS 三模型·…·情绪导演版”),收起栏完整显示
                        valueMaxLines = 3,
                        itemContent = { isSelected, entry, icon, _ ->
                            PluginImage(model = icon, name = entry)
                            Text(
                                entry,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        onSelectedChange = { id, name ->
                            if (id == tts.pluginId) return@AppSpinner
                            // 切换插件：清空跨插件残留状态（多选/分类/试听）
                            selectedVoiceIds = emptySet()
                            voiceCategoryMap = emptyMap()
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
                                // 语言池选择器：普通样式与默认绑定；音色来源组(插件/声音)才用主题色
                                labelText = "🌐 " + stringResource(R.string.language),
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
                                labelText = "🔊 " + stringResource(R.string.label_voice),
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
                                        val systtsSnapshot = systts
                                        val ttsSnapshot = tts
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

                                            // 待保存清单：(音色, 语言池ID, "")，locale 即当前表单所选
                                            val importItems =
                                                selectedVoices.map { Triple(it, ttsSnapshot.locale, "") }

                                            importItems.forEachIndexed { voiceIdx, (voice, voiceLocale, poolName) ->
                                                withContext(Dispatchers.Main) {
                                                    savingProgressText =
                                                        "正在保存 ${voiceIdx + 1}/${importItems.size}：${voice.name}"
                                                }
                                                val category = categoryMapSnapshot[voice.id]
                                                val newRuleData = config.speechRule.copy()
                                                // 未分配分类时保留用户在分组树中已选的子分组路径，
                                                // 不再被强制置空导致保存位置丢失
                                                var categoryPath = systtsSnapshot.categoryPath

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

                                                // 插件声明的采样率是用户在插件界面选择的请求/PCM兜底值。
                                                // MP3/WAV/Opus 等有音频头的实际输入格式在播放时自动识别，
                                                // 不再把试听或额外合成测得的瞬时值写进每个配置项。
                                                val voiceSampleRate = runCatching {
                                                    vm.engine.getSampleRate(voiceLocale, voice.id)
                                                }.getOrNull()?.takeIf { it > 0 }
                                                    ?: config.audioFormat.sampleRate
                                                val voiceNeedDecode = runCatching {
                                                    vm.engine.isNeedDecode(voiceLocale, voice.id)
                                                }.getOrNull() ?: config.audioFormat.isNeedDecode

                                                val newConfig = config.copy(
                                                    source = ttsSnapshot.copy(
                                                        locale = voiceLocale,
                                                        voice = voice.id
                                                    ),
                                                    speechRule = newRuleData,
                                                    audioFormat = config.audioFormat.copy(
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
                                                        importItems.size
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
            }

            if (!isUiOnly && showParamsSection) {
                SectionCard(
                    title = "音频参数",
                    icon = Icons.Default.Speed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    ParamsEditScreen(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        systemTts = systts,
                        onSystemTtsChange = onSysttsChange
                    )
                }
            }
        }
    }

    /**
     * 角色管理栏（仅界面模式）的插件自定义 UI 直渲染块。
     * 与「音色来源」卡内共用同一套加载逻辑（onLoadUI 填充 LinearLayout → AndroidView 展示），
     * 但不包任何卡片/分区壳：仅界面模式下去壳是用户要求，去壳≠连插件 UI 一起不渲染
     * （09a7c30 曾误把整卡 if(!isUiOnly) 导致角色管理栏空白）。
     */
    @Composable
    private fun RoleManagementPluginContent(
        tts: PluginTtsSource,
        vm: PluginTtsViewModel,
        plugin: Plugin?,
        reloadKey: Any?,
        onLoadingError: (Throwable) -> Unit,
    ) {
        val context = LocalContext.current
        key(tts.pluginId, reloadKey) {
            val customViewLayout = remember { LinearLayout(context).apply { orientation = LinearLayout.VERTICAL } }

            LaunchedEffect(tts.pluginId, reloadKey) {
                runCatching {
                    vm.load(context, plugin, tts, customViewLayout)
                }.onFailure {
                    it.printStackTrace()
                    onLoadingError(it)
                }
            }

            LoadingContent(isLoading = vm.isLoading) {
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
