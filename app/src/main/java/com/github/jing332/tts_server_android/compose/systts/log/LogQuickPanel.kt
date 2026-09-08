package com.github.jing332.tts_server_android.compose.systts.log

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.drake.net.utils.withIO
import com.github.jing332.common.LogEntry
import com.github.jing332.compose.widgets.AppSpinner
import com.github.jing332.compose.widgets.LabelSlider
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts.TaggedTtsPreviewPlayer
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.app
import com.github.jing332.tts_server_android.compose.systts.list.ui.PluginDescriptor
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import com.github.jing332.tts_server_android.service.systts.help.CharacterRecordsFile
import kotlinx.coroutines.launch

/**
 * 日志快捷面板「发音人调整」：点带 configId 的"请求音频"主行弹出。
 * 结构对齐配置项音频参数弹窗（用户 09-07：面板 = 弹窗 + 顶部换发音人）：
 * - 发音人下拉（引擎 getVoices 缓存）+ ▶ 试听，未保存候选也先听（统一试听链）；
 * - 终值行：播放链同源三层乘积，值为 1.0 的维度不显示；
 * - 三块调节与弹窗同款且均为**无音高版**（用户定稿）：
 *   配置项音频参数（仅本条，应用含发音人）/ 插件音频参数 / 全局音频参数，
 *   各自带重置/应用，应用即落库生效不关面板。
 */
@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LogQuickPanel(
    onDismissRequest: () -> Unit,
    entry: LogEntry,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val entity = remember(entry.configId) { dbm.systemTtsV2.get(entry.configId) }
    if (entity == null) {
        // 配置项已被删除：提示后由外层关闭
        Toast.makeText(context, context.getString(R.string.log_panel_config_missing), Toast.LENGTH_SHORT).show()
        onDismissRequest()
        return
    }
    val config = entity.config as? TtsConfigurationDTO ?: run {
        Toast.makeText(context, context.getString(R.string.log_panel_config_missing), Toast.LENGTH_SHORT).show()
        onDismissRequest()
        return
    }
    val source = config.source as? PluginTtsSource

    // ===== 本地编辑草稿：各块应用才落库 =====
    var displayName by remember(entity.id) { mutableStateOf(entity.displayName) }
    var voice by remember(entity.id) { mutableStateOf(source?.voice ?: "") }
    var speed by remember(entity.id) { mutableStateOf(config.audioParams.speed) }
    var volume by remember(entity.id) { mutableStateOf(config.audioParams.volume) }

    // 换声两段式（用户 09-08）：点候选行=暂存选中（不落库），底部「确认」键才生效——
    // 即点即改的 Toast 反馈太弱且易误触；未确认选择在关闭面板时自然丢弃
    var pendingVoice by remember(entity.id) { mutableStateOf<String?>(null) }

    // 旁白/非多角色换声：写配置项 voice（对话绑定模式走 CharacterRecordsFile.rebind，两分支各自处理）
    fun applyVoice(selected: String) {
        voice = selected
        scope.launch {
            withIO {
                val sourceNow =
                    (entity.config as? TtsConfigurationDTO)?.source as? PluginTtsSource
                if (sourceNow != null) {
                    val newConfig = config.copy(source = sourceNow.copy(voice = selected))
                    dbm.systemTtsV2.update(entity.copy(config = newConfig))
                    SystemTtsService.notifyUpdateConfig()
                }
            }
            Toast.makeText(
                context,
                context.getString(R.string.log_panel_voice_applied),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    // 绑定模式当前绑定（提升到分支外：底部确认行生效后要更新它）
    var boundVoice by remember(entity.id) {
        mutableStateOf(
            CharacterRecordsFile.readCharacterVoice(
                config.speechRule.tagRuleId, entry.roleName
            ) ?: config.speechRule.tag
        )
    }

    // 发音人候选：插件配置走引擎 getVoices（引擎缓存，秒回）；本地引擎不提供下拉
    var voices by remember(entity.id) { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    val vm = remember {
        com.github.jing332.tts_server_android.compose.systts.list.ui.PluginTtsViewModel(app)
    }

    // 插件元数据（轻量）：插件层草稿初值 + 终值路由判断用
    var plugin by remember(entity.id) {
        mutableStateOf(source?.let { dbm.pluginDao.getMetaByPluginId(it.pluginId) })
    }
    var pluginSpeed by remember(entity.id) { mutableStateOf(1f) }
    var pluginVolume by remember(entity.id) { mutableStateOf(1f) }
    LaunchedEffectOnce(entity.id) {
        source?.let { s ->
            runCatching {
                val loaded = dbm.pluginDao.getByPluginId(s.pluginId) ?: return@runCatching
                plugin = loaded
                withIO {
                    vm.load(context, loaded, s, android.widget.LinearLayout(context).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                    })
                }
                com.drake.net.utils.withMain {
                    voices = vm.voices.map { it.id.toString() to it.name }
                    pluginSpeed = snapParam(loaded.audioParams.speed)
                    pluginVolume = snapParam(loaded.audioParams.volume)
                }
            }
        }
    }

    var globalSpeed by remember { mutableStateOf(com.github.jing332.tts_server_android.conf.SysTtsConfig.audioParamsSpeed) }
    var globalVolume by remember { mutableStateOf(com.github.jing332.tts_server_android.conf.SysTtsConfig.audioParamsVolume) }

    // 未保存标记：滑杆被改动后置 true，该块「应用」成功后清除（按钮高亮提示哪块有待保存）
    var configDirty by remember(entity.id) { mutableStateOf(false) }
    var pluginDirty by remember(entity.id) { mutableStateOf(false) }
    var globalDirty by remember { mutableStateOf(false) }

    /** 用当前草稿构造临时实体试听：未保存候选也先听，走统一试听链 */
    fun draftEntity(candidateVoice: String? = null): SystemTtsV2 {
        val v = candidateVoice ?: voice
        val renamed = if (v.isNotBlank() && v != (source?.voice ?: "")) v else displayName
        return entity.copy(
            displayName = renamed,
            config = config.copy(
                audioParams = config.audioParams.copy(speed = speed, volume = volume),
                source = source?.copy(voice = v) ?: config.source,
            ),
        )
    }

    /** 配置层应用：只保存本块语速/音量（发音人独立即选即存，不在此处捎带），即时生效不关面板 */
    fun applyConfigLayer() {
        scope.launch {
            withIO {
                val newConfig = config.copy(
                    audioParams = config.audioParams.copy(
                        speed = snapParam(speed),
                        volume = snapParam(volume),
                    ),
                )
                dbm.systemTtsV2.update(entity.copy(config = newConfig))
                SystemTtsService.notifyUpdateConfig()
            }
            Toast.makeText(
                context,
                context.getString(R.string.audio_params_apply_config_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    // 底部弹窗（用户 09-08 定稿）：内容多，sheet 比 AlertDialog 合适——占满宽、可拉高、下滑关闭；
    // 内容与快速编辑弹窗同惯例水平 8dp
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                // 弹窗内容整体可滚（用户 09-08：屏幕装不下时下方音频参数区域看不到）；
                // 候选列表自带 220dp 内滚，内层优先消费手势，到边缘后外层接管，不冲突
                .verticalScroll(rememberScrollState())
        ) {
            // 总标题（用户 09-07 定稿）：发音人调整
            Text(
                stringResource(R.string.log_panel_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            // ===== 换发音人（最上方，无标题字）=====
                // 分类定稿（用户 09-08 方案A）：14 个全称 chips（不合并男女），顺序用户拍板；
                // 提取方式参照标签分类（剥尾部数字取汉字前缀），前缀在白名单内才可被分类筛选命中，
                // 对话/括号/本地音效/角色名等不入分类；「全部」chip 移除（不选=不筛选）；
                // chips 用 FlowRow 流式换行不横滑（与日志栏筛选同款）
                val voiceCategories = listOf(
                    "女主", "男主", "少女", "少年", "女青年", "男青年", "女中年", "男中年",
                    "女老年", "男老年", "女童", "男童", "特殊男", "特殊女",
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
                // 终版模式分流（用户 09-08 互通定稿）：
                // - 对话请求（entry.roleName 非空）：候选=fayinren.json 标签池∩启用配置（角色管理同源），
                //   选中即改写 characterRecords.json 里该角色的绑定——与角色管理换发音人完全互通；
                // - 旁白/非多角色（无角色名）：两分类——[旁白]=主界面预置旁白配置声音直选；
                //   [其他]=按配置项名搜索（数量庞大无法分类，用户 09-08 定稿）
                val isBindingMode = entry.roleName.isNotBlank()
                if (source != null) {
                    if (isBindingMode) {
                        // ===== 绑定模式：chips 定范围 + 常驻搜索（范围内）+ 列表（行内试听）=====
                        var selectedCategory by remember(entity.id) {
                            // 默认选中配置项当前标签所属分类（「女青年25」→「青」；九类外如「括号1」→不选=不筛选）
                            mutableStateOf(voiceCategoryOf(config.speechRule.tagName))
                        }
                        var tagSearch by remember(entity.id) { mutableStateOf("") }
                        // FlowRow 流式换行：14 个全称 chips 一行放不下自动折行，不左右滑（与日志栏筛选同款）
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            voiceCategories.forEach { c ->
                                androidx.compose.material3.FilterChip(
                                    selected = selectedCategory == c,
                                    onClick = { selectedCategory = if (selectedCategory == c) null else c },
                                    label = { Text(c) },
                                )
                            }
                        }
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            label = { Text("搜索标签名（当前范围内）") },
                            value = tagSearch,
                            onValueChange = { tagSearch = it },
                            singleLine = true,
                        )

                        // 改绑到无启用配置的标签会掉进随机兜底，读声不可控，必须排除
                        val enabledTags = remember(entity.id) {
                            dbm.systemTtsV2.getAllGroupWithTts().flatMap { it.list }
                                .filter { it.isEnabled }
                                .mapNotNullTo(mutableSetOf()) {
                                    (it.config as? TtsConfigurationDTO)?.speechRule?.tag
                                }
                        }
                        val pool = CharacterRecordsFile.readVoicePool(config.speechRule.tagRuleId)
                        val filtered = pool.filter {
                            it in enabledTags &&
                                (selectedCategory == null || voiceCategoryOf(it) == selectedCategory) &&
                                (tagSearch.isBlank() || it.contains(tagSearch))
                        }
                        // 当前绑定不在候选时补在顶部，防丢值
                        val displayTags =
                            if (boundVoice.isNotEmpty() && filtered.none { it == boundVoice }) {
                                listOf(boundVoice) + filtered
                            } else filtered

                        // 候选列表：点行=改绑应用；▶=只试听该标签对应的启用配置（不应用）
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState())
                                .border(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(8.dp),
                                ),
                        ) {
                            if (displayTags.isEmpty()) {
                                Text(
                                    "该范围内没有可用的标签",
                                    modifier = Modifier.padding(10.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            displayTags.forEach { tag ->
                                val isCurrent = tag == boundVoice
                                val isPending = tag == pendingVoice
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // 两段式（用户 09-08）：点行=暂存选中，底部「确认」才落库
                                            pendingVoice = tag
                                        }
                                        .padding(horizontal = 10.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        (if (isCurrent) "✓ " else "") +
                                            (if (isPending && !isCurrent) "● " else "") + tag,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        color = when {
                                            isPending -> MaterialTheme.colorScheme.primary
                                            isCurrent -> MaterialTheme.colorScheme.onSurface
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                    TextButton(onClick = {
                                        // 行内试听：播放该标签对应启用配置的声音，不应用
                                        scope.launch {
                                            val target = withIO {
                                                dbm.systemTtsV2.getAllGroupWithTts().flatMap { it.list }
                                                    .firstOrNull {
                                                        it.isEnabled &&
                                                            (it.config as? TtsConfigurationDTO)?.speechRule?.tag == tag
                                                    }
                                            }
                                            if (target != null) {
                                                TaggedTtsPreviewPlayer.play(context, target, "你好，这是试听语音。")
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.log_panel_rebind_no_config),
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            }
                                        }
                                    }) {
                                        Text("▶")
                                    }
                                }
                            }
                        }
                    } else {
                        // ===== 旁白/非多角色：chips 定范围 + 常驻搜索 + 列表（行内试听）=====
                        // 分类=九桶+「旁白」（用户 09-08：旁白多加一个分类）；默认选中当前配置项标签所属桶
                        // （「旁白」配置→旁白，「男青年25」→青）；「全部」chip 移除（不选=不筛选）
                        val narrationCategories = listOf("旁白") + voiceCategories
                        val ownCategory = voiceCategoryOf(config.speechRule.tagName)
                        var narrationScope by remember(entity.id) {
                            mutableStateOf(ownCategory)
                        }
                        var searchQuery by remember(entity.id) { mutableStateOf("") }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            narrationCategories.forEach { c ->
                                androidx.compose.material3.FilterChip(
                                    selected = narrationScope == c,
                                    onClick = { narrationScope = if (narrationScope == c) null else c },
                                    label = { Text(c) },
                                )
                            }
                        }
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            label = { Text("搜索配置项名（当前范围内）") },
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                        )

                        val allConfigs = remember(entity.id) {
                            dbm.systemTtsV2.getAllGroupWithTts().flatMap { it.list }
                        }

                        // 候选配置项（去重：同一声音只留一个代表，供试听/应用）
                        val candidateConfigs = allConfigs
                            .mapNotNull { c ->
                                val dto = c.config as? TtsConfigurationDTO ?: return@mapNotNull null
                                val v = (dto.source as? PluginTtsSource)?.voice
                                    ?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                                val cat = voiceCategoryOf(dto.speechRule.tagName)
                                // 未选分类=不筛选；选中=候选标签归桶后与所选一致
                                val inScope = narrationScope == null || cat == narrationScope
                                if (!inScope) return@mapNotNull null
                                if (searchQuery.isNotBlank() &&
                                    !c.displayName.contains(searchQuery, ignoreCase = true)
                                ) return@mapNotNull null
                                Triple(v, c.displayName, dto.speechRule.tagName)
                            }
                            .distinctBy { it.first }

                        // 候选列表：点行=应用该声音到启用的旁白配置；▶=试听该配置的声音
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState())
                                .border(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(8.dp),
                                ),
                        ) {
                            if (candidateConfigs.isEmpty()) {
                                Text(
                                    "该范围内没有匹配的配置项",
                                    modifier = Modifier.padding(10.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            candidateConfigs.forEach { (v, name, tagName) ->
                                val isCurrent = v == voice && voice.isNotEmpty()
                                val isPending = v == pendingVoice
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // 两段式（用户 09-08）：点行=暂存选中，底部「确认」才写配置项 voice
                                            pendingVoice = v
                                        }
                                        .padding(horizontal = 10.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        // 分类(标签)在前、配置项名在后（用户 09-08 调换，原为 配置项名 · 标签）
                                        (if (isCurrent) "✓ " else "") +
                                            (if (isPending && !isCurrent) "● " else "") +
                                            (if (tagName.isNotBlank()) tagName + " · " else "") + name,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        color = if (isPending && !isCurrent) MaterialTheme.colorScheme.primary
                                        else if (isCurrent) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                    )
                                    TextButton(onClick = {
                                        val target = allConfigs.firstOrNull {
                                            (it.config as? TtsConfigurationDTO)?.source
                                                ?.let { s -> (s as? PluginTtsSource)?.voice } == v
                                        }
                                        if (target != null) {
                                            TaggedTtsPreviewPlayer.play(context, target, "你好，这是试听语音。")
                                        }
                                    }) {
                                        Text("▶")
                                    }
                                }
                            }
                        }
                    }
                }

                // ===== 终值（播放链同源三层乘积；值为 1.0 的维度不显示）=====
                // 最终值恒为 配置×插件×全局（弹窗有无音高滑杆不影响计算）
                val handlesSpeed = plugin?.pluginHandlesSpeed == true
                val handlesVolume = plugin?.pluginHandlesVolume == true
                val handlesPitch = plugin?.pluginHandlesPitch == true
                val finalSpeed = if (handlesSpeed) speed else speed * pluginSpeed * globalSpeed
                val finalVolume = if (handlesVolume) volume else volume * pluginVolume * globalVolume
                val finalPitch = if (handlesPitch) config.audioParams.pitch
                else config.audioParams.pitch * (plugin?.audioParams?.pitch ?: 1f) *
                    com.github.jing332.tts_server_android.conf.SysTtsConfig.audioParamsPitch
                val finalDims = buildList {
                    if (kotlin.math.abs(finalSpeed - 1f) > 0.005f) add("语速%.2fx".format(finalSpeed))
                    if (kotlin.math.abs(finalVolume - 1f) > 0.005f) add("音量%.2fx".format(finalVolume))
                    if (kotlin.math.abs(finalPitch - 1f) > 0.005f) add("音高%.2fx".format(finalPitch))
                }
                Text(
                    text = if (finalDims.isEmpty()) stringResource(R.string.audio_params_none)
                    else "最终：" + finalDims.joinToString("，"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
                )

                // ===== 音频参数大区（内部三块以短分隔线区分）=====
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                // ===== 配置项音频参数（仅本条）=====
                Text(
                    stringResource(R.string.audio_params_config_layer),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                LabelSlider(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.label_speech_rate, "%.2f".format(speed)),
                    value = speed,
                    onValueChange = { speed = it; configDirty = true },
                    valueRange = 0.1f..3f,
                    step = 0.05f,
                )
                LabelSlider(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.label_speech_volume, "%.2f".format(volume)),
                    value = volume,
                    onValueChange = { volume = it; configDirty = true },
                    valueRange = 0.1f..3f,
                    step = 0.05f,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { speed = 1f; volume = 1f }) {
                        Text(stringResource(R.string.reset))
                    }
                    TextButton(onClick = { applyConfigLayer(); configDirty = false }) {
                        Text((if (configDirty) "● " else "") + stringResource(R.string.audio_params_apply))
                    }
                }

                // ===== 插件音频参数（影响该插件全部配置项）=====
                HorizontalDivider(
                    Modifier
                        .fillMaxWidth(0.66f)
                        .padding(vertical = 6.dp),
                )
                if (source != null) {
                    Text(
                        stringResource(R.string.audio_params_plugin_layer),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    LabelSlider(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.label_speech_rate, "%.2f".format(pluginSpeed)),
                        value = pluginSpeed,
                        onValueChange = { pluginSpeed = it; pluginDirty = true },
                        valueRange = 0.1f..3f,
                        step = 0.05f,
                    )
                    LabelSlider(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.label_speech_volume, "%.2f".format(pluginVolume)),
                        value = pluginVolume,
                        onValueChange = { pluginVolume = it; pluginDirty = true },
                        valueRange = 0.1f..3f,
                        step = 0.05f,
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { pluginSpeed = 1f; pluginVolume = 1f }) {
                            Text(stringResource(R.string.reset))
                        }
                        TextButton(onClick = {
                            val p = plugin ?: return@TextButton
                            scope.launch {
                                withIO {
                                    dbm.pluginDao.update(
                                        p.copy(
                                            audioParams = p.audioParams.copy(
                                                speed = snapParam(pluginSpeed),
                                                volume = snapParam(pluginVolume),
                                            )
                                        )
                                    )
                                    // 卡片"插件语速/音量"显示缓存失效
                                    PluginDescriptor.invalidatePluginParamsCache(p.pluginId)
                                    SystemTtsService.notifyUpdateConfig()
                                }
                                pluginDirty = false
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.audio_params_apply_plugin_toast),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }) {
                            Text((if (pluginDirty) "● " else "") + stringResource(R.string.audio_params_apply))
                        }
                    }
                }

                // ===== 全局音频参数（影响全部配置项·谨慎）=====
                HorizontalDivider(
                    Modifier
                        .fillMaxWidth(0.66f)
                        .padding(vertical = 6.dp),
                )
                Text(
                    stringResource(R.string.audio_params_global_layer),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                LabelSlider(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.label_speech_rate, "%.2f".format(globalSpeed)),
                    value = globalSpeed,
                    onValueChange = { globalSpeed = it; globalDirty = true },
                    valueRange = 0.1f..3f,
                    step = 0.05f,
                )
                LabelSlider(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.label_speech_volume, "%.2f".format(globalVolume)),
                    value = globalVolume,
                    onValueChange = { globalVolume = it; globalDirty = true },
                    valueRange = 0.1f..3f,
                    step = 0.05f,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { globalSpeed = 1f; globalVolume = 1f }) {
                        Text(stringResource(R.string.reset))
                    }
                    TextButton(onClick = {
                        scope.launch {
                            com.github.jing332.tts_server_android.conf.SysTtsConfig.audioParamsSpeed =
                                snapParam(globalSpeed)
                            com.github.jing332.tts_server_android.conf.SysTtsConfig.audioParamsVolume =
                                snapParam(globalVolume)
                            SystemTtsService.notifyUpdateConfig()
                            globalDirty = false
                            Toast.makeText(
                                context,
                                context.getString(R.string.audio_params_apply_global_toast),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }) {
                        Text((if (globalDirty) "● " else "") + stringResource(R.string.audio_params_apply))
                    }
                }

            // ===== 底部操作行：换声两段式确认（用户 09-08，即点即改反馈弱且易误触）+ 取消 =====
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
                TextButton(
                    enabled = pendingVoice != null,
                    onClick = {
                        val selected = pendingVoice ?: return@TextButton
                        if (isBindingMode) {
                            // 绑定模式：改写 characterRecords.json（与角色管理同文件同字段）
                            if (selected == boundVoice) {
                                pendingVoice = null
                                return@TextButton
                            }
                            scope.launch {
                                val ok = withIO {
                                    CharacterRecordsFile.rebind(
                                        config.speechRule.tagRuleId,
                                        entry.roleName,
                                        selected,
                                    )
                                }
                                if (ok) boundVoice = selected
                                pendingVoice = null
                                Toast.makeText(
                                    context,
                                    if (ok) "已将「${entry.roleName}」的发音人换为 $selected"
                                    else context.getString(R.string.log_panel_rebind_failed),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        } else {
                            // 旁白/其他：写配置项 voice
                            applyVoice(selected)
                            pendingVoice = null
                        }
                    },
                ) {
                    Text((if (pendingVoice != null) "● " else "") + stringResource(R.string.confirm))
                }
            }
        }
    }
}

/** 单次 LaunchedEffect 简写：key 变化时只执行一次 */
@Composable
private fun LaunchedEffectOnce(key: Any?, block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {
    androidx.compose.runtime.LaunchedEffect(key) { block() }
}

private fun snapParam(v: Float): Float = (kotlin.math.round(v * 100f) / 100f)
