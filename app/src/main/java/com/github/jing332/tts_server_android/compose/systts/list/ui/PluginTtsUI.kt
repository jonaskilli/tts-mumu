package com.github.jing332.tts_server_android.compose.systts.list.ui

import android.content.Intent
import android.util.Log
import android.widget.LinearLayout
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
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
import com.github.jing332.tts_server_android.compose.systts.plugin.PluginManagerActivity
import com.github.jing332.tts_server_android.compose.systts.plugin.VoiceCatalogHandoff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.github.jing332.common.utils.startActivity
import com.github.jing332.common.utils.toast
import com.github.jing332.compose.widgets.AppSpinner
import com.github.jing332.compose.widgets.LocalSelectionRowHorizontalPadding
import com.github.jing332.compose.widgets.LoadingContent
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts.speech.plugin.engine.VoicePickerBus
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.AuditionDialog
import com.github.jing332.tts_server_android.compose.systts.common.VoicePickerDialog
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.AuditionTextField
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.isLocalSoundTagName
import com.github.jing332.tts_server_android.conf.AppConfig
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.AudioParamsDimRows
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.AudioParamsDraft
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.BasicInfoEditScreen
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.SaveActionHandler
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.SectionCard
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.withAudioParams
import com.github.jing332.tts_server_android.constant.SpeechTarget
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PluginTtsUI : IConfigUI() {
    companion object {
        const val TAG = "PluginTtsUI"
    }

    // ParamsEditScreen 已删（09-10）：插件 TTS 从不渲染该区（QuickEditBottomSheet 仅 !isPluginTts 时调用），
    // 配置层调参唯一入口=编辑页顶部「音频参数」按钮弹窗（本地 TTS/BGM 仍用各自 override 渲染专属设置）

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
            // 音频参数入口仅保留顶部「音频参数」按钮（SpeechRuleEditScreen，弹三层弹窗）；
            // 底部不再放任何音频参数区域/入口卡（用户 09-07 定稿：不要底部加弹窗入口）
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
    ) {
        var displayName by remember { mutableStateOf("") }

        @Suppress("NAME_SHADOWING")
        val systts by rememberUpdatedState(newValue = systts)
        val tts by rememberUpdatedState(newValue = (systts.config as TtsConfigurationDTO).source as PluginTtsSource)
        val isUiOnly = tts.isUiOnly
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        // ===== 通用换声弹窗桥宿主（用户 09-13「完全同源」定稿）=====
        // 角色管理插件经 ttsrv.showVoicePickerDialog 提交请求（VoicePickerBus），
        // 在此渲染与日志快捷面板**同款**的 VoicePickerDialog（更换发音人+音频参数两分段）。
        // 弹窗内换声落库/删除配置项/标记变化经 VoicePickerBus.notifyMutated 回喊插件 JS
        // （PluginJS 回调，主线程）；弹窗关闭清槽。挂在 EditContentScreen——角色管理栏与
        // 插件编辑页都经过这里，任一宿主在场都能响应请求。
        val bridgeRequest by VoicePickerBus.request.collectAsState()
        bridgeRequest?.let { req ->
            VoicePickerDialog(
                anchorConfigId = null,
                anchorTag = req.anchorTag,
                bindingKey = req.bindingKey,
                titleBadge = req.bindingKey,
                onChanged = { event, tag -> VoicePickerBus.notifyMutated(event, tag) },
                onDismissRequest = { VoicePickerBus.clear() },
            )
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

        // 音色广场弹窗（opt-in 协议 searchVoiceCatalog，用户 09-17 拍板做全套）
        var showVoiceCatalog by remember { mutableStateOf(false) }

        var auditionSystts by remember { mutableStateOf<SystemTtsV2?>(null) }
        // 音频参数三层草稿快照（AudioParamsDimRows 上报）：🎧 试听带未应用草稿（用户 09-17）
        var audioDraft by remember { mutableStateOf<AudioParamsDraft?>(null) }
        // 本次试听是否带草稿：只有「🎧 试听文本行」入口带（用户 09-17 定：切走即剥离）——
        // 行内 🎧、长按试听都是"试别的音色"，参数应走库值
        var auditionWithDraft by remember { mutableStateOf(false) }
        // 当前试听对应的发音人ID
        var auditionVoiceId by remember { mutableStateOf<Any?>(null) }
        // 批量试听分类入库整链已迁出（10-05 用户令：搬到插件管理页「按插件音色分类入库」，
        // 本弹窗只负责「给当前配置选声音 + 试听」）

        @Suppress("UNCHECKED_CAST")
        // 本地音效配置（tagName=本地音效N）用专用试听文本，与全局文本互不影响（用户 09-13）
        val isLocalSound = isLocalSoundTagName((systts.config as TtsConfigurationDTO).speechRule.tagName)
        if (auditionSystts != null) {
            val d = if (auditionWithDraft) audioDraft else null
            AuditionDialog(
                // 配置层草稿拼进实体；插件/全局两层走 override（null=读库值）。
                // 草稿只在「🎧 试听文本行」入口生效：上一个/下一个/行内试听切到别的音色即剥离
                systts = if (d != null) auditionSystts!!.withAudioParams(d.config) else auditionSystts!!,
                text = if (isLocalSound) AppConfig.localSoundSampleText.value else AppConfig.testSampleText.value,
                pluginParamsOverride = d?.plugin,
                globalParamsOverride = d?.global,
                engine = if (plugin == null) null else vm.service(),
                voiceId = auditionVoiceId,
            ) {
                auditionSystts = null
                auditionVoiceId = null
                auditionWithDraft = false
            }
        } // if (auditionSystts != null)

        // 音色广场（opt-in 协议 searchVoiceCatalog）：勾选后把音色**补进声音列表**，
        // 供用户在下拉里选中试听——广场只负责"找得到音色"。
        if (showVoiceCatalog) {
            PluginVoiceMarketplaceDialog(
                vm = vm,
                locale = tts.locale,
                onDismissRequest = { showVoiceCatalog = false },
                onAudition = { item ->
                    // 与行内 🎧 同源：试该音色、不带草稿。广场音色不在 vm.voices 里
                    auditionWithDraft = false
                    auditionVoiceId = item.id
                    auditionSystts = systts.copy(
                        displayName = item.name,
                        config = (systts.config as TtsConfigurationDTO).copy(
                            source = tts.copy(voice = item.id)
                        )
                    )
                },
                onPick = { items ->
                    // 广场音色不在 vm.voices 里（插件 getVoices 只回它自己的本地缓存，而缓存
                    // 只有广场查询会写）——不补进去的话下拉框里选不到这些音色
                    val known = vm.voices.map { it.id }.toHashSet()
                    items.forEach { item ->
                        if (item.id !in known) vm.voices.add(
                            com.github.jing332.tts.speech.plugin.engine.TtsPluginUiEngineV2.Voice(
                                item.id,
                                item.name,
                                item.icon,
                            )
                        )
                    }
                    showVoiceCatalog = false
                    context.toast(context.getString(R.string.voice_catalog_picked, items.size))
                },
                onImport = { items ->
                    // 广场 → 分类入库衔接（10-10 用户：广场只进下拉不进库，链路断着）：
                    // 勾选音色塞进交接单例，跳插件管理页，该插件卡片自动弹入库弹窗、
                    // 直接进声音列表阶段（分类=广场标签字面映射，试听可改）。广场音色
                    // 仍补进下拉（同 onPick——本次不选它，下次进来还能在列表里看到）。
                    val known = vm.voices.map { it.id }.toHashSet()
                    items.forEach { item ->
                        if (item.id !in known) vm.voices.add(
                            com.github.jing332.tts.speech.plugin.engine.TtsPluginUiEngineV2.Voice(
                                item.id,
                                item.name,
                                item.icon,
                            )
                        )
                    }
                    showVoiceCatalog = false
                    plugin?.let { p ->
                        VoiceCatalogHandoff.put(
                            p.pluginId,
                            items.map { VoiceCatalogHandoff.Item(it.id, it.name, it.tags.firstOrNull()) }
                        )
                        context.toast(context.getString(R.string.voice_catalog_imported, items.size))
                        context.startActivity(Intent(context, PluginManagerActivity::class.java))
                    }
                },
            )
        }

        // 编辑页音频参数入口（09-10 晚改版）：试听文本下方改为「值行 + 就地展开」（AudioParamsDimRows），
        // 不再从这里开 AudioParamsDialog——本弹窗只服务卡片⋮入口与日志快捷面板

        Column(modifier) {
            // 仅界面模式开关仅对角色管理类插件显示：兼容插件换 pluginId 后按名称回退识别
            // 用 getMetaByPluginId（code=''）而非 getByPluginId（SELECT *）：这里只要 name，
            // 原全量查会把 MB 级插件 JS 拉上主线程、每次重组都卡（10-05 加载慢定位）
            val isRoleManagementPlugin = remember(tts.pluginId) {
                tts.pluginId == "mingwuyan" ||
                    dbm.pluginDao.getMetaByPluginId(tts.pluginId)?.name?.contains("角色管理") == true
            }
            // 分区卡片化：基本信息 / 音色来源 /（朗读与标签由 FullEditScreen 渲染）/ 音频参数
            // 基本信息标题恢复显示（用户 09-10：与标签态正文卡的「ℹ️基本信息」标题对称）
            if (showBasicInfo)
                SectionCard(
                    title = "基本信息",
                    icon = Icons.Default.Info,
                    showHeader = true,
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

                    // 试听文本 + 🎧；下方三键直出音频参数（用户 09-10 定稿：放基本信息卡末尾，调完属性即可试听）
                    if (!isUiOnly) {
                        AuditionTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                // 横向 12dp 与卡片内其它字段（分组/显示名）对齐——此前缺这 12dp，
                                // 试听文本行比其它行宽出约 24dp（用户 09-10 晚指认）
                                .padding(horizontal = 12.dp)
                                .padding(top = 8.dp),
                            // 🎧 试听文本行=唯一带草稿的入口（用户 09-17：调完滑杆点这里听草稿效果）
                            onAudition = {
                                auditionWithDraft = true
                                auditionSystts = systts
                            },
                            isLocalSound = isLocalSound,
                        )
                        AudioParamsDimRows(
                            modifier = Modifier
                                .fillMaxWidth()
                                // 同步让 12dp，与试听文本行/其它字段一条边（用户 09-10 晚）
                                .padding(horizontal = 12.dp)
                                .padding(top = 4.dp),
                            systemTts = systts,
                            onSysttsChange = onSysttsChange,
                            onDraftChange = { audioDraft = it },
                        )
                    }
                }

            // 音色来源区：ui-only（角色管理栏）时不用卡片壳，直接渲染插件自定义UI躺在 surface 上
            // （用户反馈：去掉分区底色壳≠连插件UI一起消失）；完整编辑模式才包 SectionCard
            // （插件选择/语言/声音都在卡内；试听文本已移至基本信息卡末尾）
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
                if (showPluginSelector && !isUiOnly) {
                    AppSpinner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        // 与其他栏标题同款样式（用户 09-17：不要主题色/加粗，只保留 🧩 前缀）
                        labelText = "🧩 " + stringResource(R.string.plugin),
                        value = tts.pluginId,
                        values = vm.pluginList.map { it.pluginId },
                        entries = vm.pluginList.map { it.name },
                        // 点开的弹窗列表带各插件自己的图标（与插件管理页同款 PluginImage：
                        // 加载失败/无图标自动显示插件名首字——仅插件栏补首字,其他栏不补）；
                        // 收起栏只显示名称不带头像(用户定稿)
                        icons = vm.pluginList.map { it.iconUrl },
                        // 插件名长(如“小米 MiMo V2.5 TTS 三模型·…·情绪导演版”),收起栏完整显示
                        valueMaxLines = 3,
                        itemContent = { isSelected, entry, icon, _ ->
                            PluginImage(model = icon, name = entry)
                            Text(
                                entry,
                                // bodyMedium 14sp 与默认条目渲染同款（原 bodyLarge 16sp 比其他栏大一号）
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .weight(1f)
                                    // 横向内边距由外壳按形态提供（底部面板 0dp / 居中卡片 16dp），写死会叠出第二套左缘线
                                    .padding(
                                        horizontal = LocalSelectionRowHorizontalPadding.current,
                                        vertical = 12.dp
                                    ),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        onSelectedChange = { id, name ->
                            if (id == tts.pluginId) return@AppSpinner
                            // 切换插件：清空跨插件残留状态（试听）
                            auditionSystts = null
                            auditionVoiceId = null
                            auditionWithDraft = false
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
                                    // 插件 JS 的异常绝不能逃出协程：原写法把 runCatching 包在 launch **外面**
                                    // 等于没包——launch 自己不抛，插件在协程体里 throw 时无人接，异常直接冒到
                                    // 全局未捕获处理器把 app 打崩（09-17 呱呱官方付费插件：未填 Token 时
                                    // getVoices 主动 throw，在编辑页切换语言即闪退）。
                                    // 收敛在协程体内，失败弹错误弹窗——与打开页面时 load() 失败的处理一致
                                    // （displayErrorDialog 内部自带 runOnUI，IO 线程可直接调）。
                                    scope.launch(Dispatchers.IO) {
                                        runCatching { vm.updateVoices(locale.toString()) }
                                            .onFailure {
                                                it.printStackTrace()
                                                context.displayErrorDialog(it)
                                            }
                                    }
                                },
                            )

                            AppSpinner(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                // 发音人栏：试听入口=行内 🎧 与长按（批量分类入库已迁往插件管理页）
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
                                // 长按试听=试该音色，不带草稿（用户 09-17）
                                auditionWithDraft = false
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
                                    // 音色行内 🎧=试该音色，不带草稿（用户 09-17）
                                    auditionWithDraft = false
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
                        )

                        // 音色广场入口（opt-in 协议 searchVoiceCatalog）：这类插件（Fish Audio 官网
                        // 音色广场等）的 getVoices() 只回它自己写的本地缓存，而**只有
                        // searchVoiceCatalog() 会写这个缓存** ⇒ 不给入口就一个音色都选不到
                        // （声音下拉恒空）。插件没声明该接口时按钮不出现。
                        if (vm.supportsVoiceCatalog) {
                            OutlinedButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                onClick = { showVoiceCatalog = true },
                            ) {
                                Text("🎼 " + stringResource(R.string.voice_catalog))
                            }
                        }
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

            // 音频参数卡已删（用户 09-07 定稿：入口收敛到顶部「音频参数」按钮），
            // 预览路径同样不再渲染任何音频参数区域
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
