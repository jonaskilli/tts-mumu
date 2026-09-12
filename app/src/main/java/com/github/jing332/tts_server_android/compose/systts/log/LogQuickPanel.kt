package com.github.jing332.tts_server_android.compose.systts.log

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drake.net.utils.withIO
import com.drake.net.utils.withMain
import com.github.jing332.common.LogEntry
import com.github.jing332.common.utils.toParamText
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.AppSpinner
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts.PreviewState
import com.github.jing332.tts.TaggedTtsPreviewPlayer
import com.github.jing332.tts.CachedEngineManager
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.SharedViewModel
import com.github.jing332.tts_server_android.compose.SoftSegmentedTextToggle
import com.github.jing332.tts_server_android.compose.systts.list.ui.PluginDescriptor
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.AudioParamsDimensionSection
import com.github.jing332.tts_server_android.conf.SysTtsConfig
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import com.github.jing332.tts_server_android.service.systts.help.CharacterRecordsFile
import com.github.jing332.tts_server_android.service.systts.help.VoiceMarksFile
import kotlinx.coroutines.launch

/**
 * 日志快捷面板「发音人调整」：点带 configId 的"请求音频"主行弹出。
 * 结构（用户 09-09 定稿）：
 * - 顶部（两区共用）：当前发音人 + ▶试听 + 终值行（播放链同源三层乘积，值为 1.0 的维度不显示）；
 * - SegmentedButton 两区（同编辑页「朗读全部/标签」样式）：
 *   [更换发音人] 对话绑定模式=分类下拉(含全部，带N项)+搜索+候选列表；
 *   旁白模式=直接列旁白分类候选（标签是「旁白」的配置项，无下拉/搜索）；
 *   行内试听 ▶/…/■ 状态机参照角色管理v10；换声两段式：点行=暂存(●)，底部「确认」落库，
 *   旁白落库后主列表自动定位高亮被改项（sharedVM.pendingLocateConfigId）。
 *   [音频参数]（09-10 按维度改版）：语速/音量/音高第二级分段，每维三层滑杆同屏，
 *   重置/应用按维度一组（应用=该维三层一起落库，不关面板）；
 *   参数跟随所选发音人（用户 09-10）：绑定模式=绑定(含暂存)标签的启用配置项，
 *   旁白=暂存候选的配置项，无暂存=本配置项，目标切换时三层草稿整体重载。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LogQuickPanel(
    onDismissRequest: () -> Unit,
    entry: LogEntry,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 主界面共享状态：换旁白发音人后通知主列表定位高亮（Activity 级单例，与 MainPager 同实例）
    val sharedVM: SharedViewModel = viewModel()

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
    // 绑定键：多角色日志=角色名；「本地音效N」槽位=槽位名（目目 09-12：本地音效 tagName 带序号，
    // 按角色那种处理——改绑同样走 characterRecords.json 同一机制），两者共用绑定分支
    val bindingKey = entry.roleName.ifBlank {
        if (config.speechRule.tagName.matches(Regex("本地音效\\d*"))) config.speechRule.tagName else ""
    }
    val isBindingMode = bindingKey.isNotBlank()

    // ===== 行内试听状态（参照角色管理v9/v10试听状态机：▶ →(点击)… →(真正出声)■ →(播完复位)▶）=====
    // 播放器全局单实例（同一时刻只有一个试听），previewingKey 记当前行（顶部=current，绑定=tag，旁白=voice）；
    // 状态由 TaggedTtsPreviewPlayer.state 驱动，播完/失败/被顶替回到 IDLE 时复位行标记
    val previewState by TaggedTtsPreviewPlayer.state.collectAsState()
    var previewingKey by remember(entity.id) { mutableStateOf<Any?>(null) }
    androidx.compose.runtime.LaunchedEffect(previewState) {
        if (previewState == PreviewState.IDLE) previewingKey = null
    }
    fun previewLabel(key: Any?): String = when {
        previewingKey == key && previewState == PreviewState.PLAYING -> "■"
        previewingKey == key -> "…"
        else -> "▶"
    }

    @Composable
    fun previewLabelColor(key: Any?): Color =
        if (previewingKey == key) MaterialTheme.colorScheme.tertiary else Color.Unspecified

    // 面板分段：0=更换发音人 1=音频参数。声明在 AppDialog 外——buttons 槽也要读它
    //（09-09 CI教训：content 槽内声明的局部状态对 buttons 槽不可见）
    var panelTab by remember(entity.id) { mutableStateOf(0) }

    // ===== 本地编辑草稿：各维度「应用」才落库 =====
    var displayName by remember(entity.id) { mutableStateOf(entity.displayName) }
    var voice by remember(entity.id) { mutableStateOf(source?.voice ?: "") }
    var speed by remember(entity.id) { mutableStateOf(config.audioParams.speed) }
    var volume by remember(entity.id) { mutableStateOf(config.audioParams.volume) }
    var pitch by remember(entity.id) { mutableStateOf(config.audioParams.pitch) }

    // 换声两段式（用户 09-08）：点候选行=暂存选中（不落库），底部「确认」键才生效——
    // 即点即改的 Toast 反馈太弱且易误触；未确认选择在关闭面板时自然丢弃
    var pendingVoice by remember(entity.id) { mutableStateOf<String?>(null) }

    // 绑定模式当前绑定（提升到分支外：底部确认行生效后要更新它）
    // 绑定值=标签 id（fayinren.json/characterRecords 里存的都是 tag id，JS 侧 tags[voiceTag]=显示名仅用于回显）；
    // 无绑定记录时回落本配置项自己的 tag
    var boundVoice by remember(entity.id) {
        mutableStateOf(
            CharacterRecordsFile.readCharacterVoice(
                config.speechRule.tagRuleId, bindingKey
            ) ?: config.speechRule.tag
        )
    }

    // ===== 角色管理互通（用户 09-12）：候选行 ⋮ 菜单=发音人标记 + 删除配置项 =====
    // 标记写 voice_marks.json（与角色管理 v10 同文件同字段，按标签 id 多选 toggle，❤️🚶😈）；
    // marksVersion 自增触发候选行标记重读（文件通道无观察者，靠版本号刷新）
    var marksVersion by remember(entity.id) { mutableStateOf(0) }
    // 待删除确认的标签（非空时弹确认弹窗）
    var deleteConfirmTag by remember(entity.id) { mutableStateOf<String?>(null) }

    // 全部配置项（换声候选 / 参数跟随目标查找共用）
    val allConfigs = remember(entity.id) {
        dbm.systemTtsV2.getAllGroupWithTts().flatMap { it.list }
    }

    /** 按标签（tag id）查启用配置项（试听/当前发音人名/候选行displayName/参数跟随共用）——
     *  用户 09-12 定稿：匹配键=**tag（id）**，fayinren.json/characterRecords 存的都是 tag id，
     *  同 tag 下多配置（多 voice）运行时随机（一个标签绑定多个发音人、可随时切换）；
     *  tagName 是显示名不参与匹配（09-12 晚：tagName 兜底也移除，全链只认 tag） */
    fun enabledConfigEntityByTag(tag: String): SystemTtsV2? {
        if (tag.isEmpty()) return null
        return allConfigs.firstOrNull { item ->
            if (!item.isEnabled) return@firstOrNull false
            (item.config as? TtsConfigurationDTO)?.speechRule?.tag == tag
        }
    }

    /** 按发音人ID查配置项（旁白暂存候选的参数跟随目标） */
    fun narrationEntityByVoice(v: String): SystemTtsV2? =
        allConfigs.firstOrNull {
            ((it.config as? TtsConfigurationDTO)?.source as? PluginTtsSource)?.voice == v
        }

    // ===== 参数跟随目标（用户 09-10）：选择发音人后，音频参数区/终值/顶部试听全部
    // 跟随所选发音人对应的配置项——绑定模式=绑定(含暂存)标签的启用配置项；
    // 旁白=暂存候选的配置项；无暂存=本配置项。目标切换时三层草稿整体重载。=====
    var paramsTarget by remember(entity.id) { mutableStateOf(entity) }

    // 旁白/非多角色换声（对话绑定模式走 CharacterRecordsFile.rebind，两分支各自处理）：
    // 09-10 起连同当前参数草稿（=跟随所选发音人得来的值）一并写入本配置项，
    // 保证「确认」前后看到/听到的参数与实际生效一致
    fun applyVoice(selected: String) {
        voice = selected
        val sourceNow = (entity.config as? TtsConfigurationDTO)?.source as? PluginTtsSource
        val newConfig = sourceNow?.let { sn ->
            config.copy(
                source = sn.copy(voice = selected),
                audioParams = config.audioParams.copy(
                    speed = snapParam(speed), volume = snapParam(volume), pitch = snapParam(pitch),
                ),
            )
        }
        scope.launch {
            if (newConfig != null) withIO {
                dbm.systemTtsV2.update(entity.copy(config = newConfig))
                SystemTtsService.notifyUpdateConfig()
            }
            // 参数跟随目标回到本配置项（已带新参数），后续调整继续作用于本条
            if (newConfig != null) paramsTarget = entity.copy(config = newConfig)
            // 主界面定位（用户 09-09）：换完旁白发音人，主列表滚动到被改的配置项并短暂高亮
            sharedVM.pendingLocateConfigId.value = entity.id
            Toast.makeText(
                context,
                context.getString(R.string.log_panel_voice_applied),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    // 删除刚试听的那一条配置项（用户 09-12 晚纠正：只删听过的这条，同标签其他配置不动）：
    // ① 删该配置项（清理失效引擎缓存）② 仅当该标签已无其他启用配置时才从 fayinren.json 移除标签
    // （发音人还在就必须保留池子条目）；角色绑定不清，规则下次朗读自动为受影响角色重分配
    fun deletePreviewedConfig(target: SystemTtsV2) {
        val targetDto = target.config as? TtsConfigurationDTO ?: return
        scope.launch {
            var tagNowEmpty = false
            val deletedSelf = withIO {
                dbm.systemTtsV2.delete(target)
                targetDto.source.let { runCatching { CachedEngineManager.removeEngine(it) } }
                val stillEnabled = dbm.systemTtsV2.allEnabled.any { item ->
                    val d = item.config as? TtsConfigurationDTO ?: return@any false
                    d.speechRule.tagRuleId == targetDto.speechRule.tagRuleId &&
                        d.speechRule.tag == targetDto.speechRule.tag
                }
                if (!stillEnabled) {
                    tagNowEmpty = true
                    CharacterRecordsFile.removeFromPool(
                        targetDto.speechRule.tagRuleId, targetDto.speechRule.tag,
                    )
                }
                target.id == entity.id
            }
            SystemTtsService.notifyUpdateConfig()
            if (deletedSelf) {
                // 本配置项自身被删：外层 LogEntry 的 configId 已失效，提示后关闭面板
                Toast.makeText(context, "已删除配置项「${target.displayName}」", Toast.LENGTH_SHORT).show()
                onDismissRequest()
            } else {
                // 当前绑定恰是被删空标签：回落到本配置项自己的 tag（同初始化兜底）
                if (tagNowEmpty && boundVoice == targetDto.speechRule.tag) {
                    boundVoice = config.speechRule.tag
                }
                if (tagNowEmpty) {
                    Toast.makeText(
                        context,
                        "已删除「${target.displayName}」，该标签已无配置项，一并从标签池移除",
                        Toast.LENGTH_LONG,
                    ).show()
                } else {
                    Toast.makeText(context, "已删除配置项「${target.displayName}」", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 插件元数据（轻量）：插件层草稿初值 + 终值路由判断用（随参数跟随目标走）
    var plugin by remember(entity.id) {
        mutableStateOf(source?.let { dbm.pluginDao.getMetaByPluginId(it.pluginId) })
    }
    var pluginSpeed by remember(entity.id) { mutableStateOf(1f) }
    var pluginVolume by remember(entity.id) { mutableStateOf(1f) }
    var pluginPitch by remember(entity.id) { mutableStateOf(1f) }
    LaunchedEffectOnce(entity.id) {
        source?.let { s ->
            runCatching {
                val loaded = dbm.pluginDao.getByPluginId(s.pluginId) ?: return@runCatching
                withMain {
                    plugin = loaded
                    pluginSpeed = snapParam(loaded.audioParams.speed)
                    pluginVolume = snapParam(loaded.audioParams.volume)
                    pluginPitch = snapParam(loaded.audioParams.pitch)
                }
            }
        }
    }

    var globalSpeed by remember { mutableStateOf(SysTtsConfig.audioParamsSpeed) }
    var globalVolume by remember { mutableStateOf(SysTtsConfig.audioParamsVolume) }
    var globalPitch by remember { mutableStateOf(SysTtsConfig.audioParamsPitch) }

    // 按维度脏标记（09-10 ②A）：该维任一层滑杆改动置 true，应用成功清除（按钮 ● 提示）
    var speedDirty by remember(entity.id) { mutableStateOf(false) }
    var volumeDirty by remember(entity.id) { mutableStateOf(false) }
    var pitchDirty by remember(entity.id) { mutableStateOf(false) }

    // 暂存选择 → 参数跟随目标切换（含确认后/取消暂存的回退；初始运行顺带校正绑定模式目标）
    androidx.compose.runtime.LaunchedEffect(pendingVoice) {
        val pv = pendingVoice
        val target = when {
            isBindingMode -> enabledConfigEntityByTag(pv ?: boundVoice)
            pv != null -> narrationEntityByVoice(pv)
            else -> null
        } ?: return@LaunchedEffect
        if (target.id != paramsTarget.id) paramsTarget = target
    }

    // 目标切换：整体重载三层草稿（插件层随目标的插件走），脏标记复位
    androidx.compose.runtime.LaunchedEffect(paramsTarget.id) {
        if (paramsTarget.id == entity.id) return@LaunchedEffect
        val dto = paramsTarget.config as? TtsConfigurationDTO ?: return@LaunchedEffect
        val src = dto.source as? PluginTtsSource
        val loaded = src?.let { runCatching { dbm.pluginDao.getByPluginId(it.pluginId) }.getOrNull() }
        withMain {
            speed = snapParam(dto.audioParams.speed)
            volume = snapParam(dto.audioParams.volume)
            pitch = snapParam(dto.audioParams.pitch)
            plugin = loaded
            pluginSpeed = snapParam(loaded?.audioParams?.speed ?: 1f)
            pluginVolume = snapParam(loaded?.audioParams?.volume ?: 1f)
            pluginPitch = snapParam(loaded?.audioParams?.pitch ?: 1f)
            speedDirty = false
            volumeDirty = false
            pitchDirty = false
        }
    }

    /** 用当前草稿构造临时实体试听：未保存候选也先听，走统一试听链 */
    fun draftEntity(candidateVoice: String? = null): SystemTtsV2 {
        val v = candidateVoice ?: voice
        val renamed = if (v.isNotBlank() && v != (source?.voice ?: "")) v else displayName
        return entity.copy(
            displayName = renamed,
            config = config.copy(
                audioParams = config.audioParams.copy(speed = speed, volume = volume, pitch = pitch),
                source = source?.copy(voice = v) ?: config.source,
            ),
        )
    }

    /** 参数跟随目标+当前草稿构造试听实体（绑定模式顶部试听用） */
    fun draftParamsTarget(): SystemTtsV2 {
        val dto = paramsTarget.config as? TtsConfigurationDTO ?: return paramsTarget
        return paramsTarget.copy(
            config = dto.copy(
                audioParams = dto.audioParams.copy(
                    speed = speed, volume = volume, pitch = pitch,
                )
            )
        )
    }

    /** 维度应用（09-10 ②A）：该维三层一起落库——配置层写参数跟随目标，
     *  插件层写其插件，全局层写系统配置；接管判定已废除，所有维度恒可调恒落库 */
    fun applyDim(dim: Int) {
        val targetDto = paramsTarget.config as? TtsConfigurationDTO ?: return
        val targetSource = targetDto.source as? PluginTtsSource
        scope.launch {
            withIO {
                val newConfig = targetDto.copy(
                    audioParams = targetDto.audioParams.copy(
                        speed = if (dim == 0) snapParam(speed) else targetDto.audioParams.speed,
                        volume = if (dim == 1) snapParam(volume) else targetDto.audioParams.volume,
                        pitch = if (dim == 2) snapParam(pitch) else targetDto.audioParams.pitch,
                    )
                )
                dbm.systemTtsV2.update(paramsTarget.copy(config = newConfig))
                if (targetSource != null) {
                    dbm.pluginDao.getByPluginId(targetSource.pluginId)?.let { p ->
                        dbm.pluginDao.update(
                            p.copy(
                                audioParams = p.audioParams.copy(
                                    speed = if (dim == 0) snapParam(pluginSpeed) else p.audioParams.speed,
                                    volume = if (dim == 1) snapParam(pluginVolume) else p.audioParams.volume,
                                    pitch = if (dim == 2) snapParam(pluginPitch) else p.audioParams.pitch,
                                )
                            )
                        )
                        // 卡片"插件语速/音量"显示缓存失效
                        PluginDescriptor.invalidatePluginParamsCache(p.pluginId)
                    }
                }
                when (dim) {
                    0 -> SysTtsConfig.audioParamsSpeed = snapParam(globalSpeed)
                    1 -> SysTtsConfig.audioParamsVolume = snapParam(globalVolume)
                    else -> SysTtsConfig.audioParamsPitch = snapParam(globalPitch)
                }
                SystemTtsService.notifyUpdateConfig()
            }
            when (dim) {
                0 -> speedDirty = false
                1 -> volumeDirty = false
                else -> pitchDirty = false
            }
            Toast.makeText(
                context,
                context.getString(R.string.audio_params_apply_dim_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    // 居中弹窗（用户 09-09：底部弹窗全面撤回，恢复 AppDialog 中弹窗形态；标题即面板名）
    AppDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.log_panel_title)) },
        content = {
            // 候选列表高度上限统一按屏高 40% 自适应（用户 09-11 晚拍板）：原先绑定模式 220dp、
            // 旁白模式 300dp 两个手调固定值，矮屏撑不出、大屏又浪费；40% 随屏缩放，配合外层
            // 85% 屏高上限天然不越界（800dp 屏约 320dp ≈ 13 行）。
            val maxListHeight =
                (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.4f).dp
            Column(
                Modifier
                    .fillMaxWidth()
                    // 左右统一 16dp（用户 09-11）：此前只有音频参数区让 4dp，顶部/换声区是 12dp，
                    // 同一弹窗两种左边线；整体加 4dp（叠加 AppDialog 自带 12dp）后全面板一条边
                    .padding(horizontal = 4.dp)
                    // 上限跟随屏幕（85%）：固定 600dp 在矮屏上会把弹窗顶出屏幕外（用户 09-10 截图），
                    // 内容超出上限时由 verticalScroll 接管
                    .heightIn(max = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.85f).dp)
                    // 内容整体可滚：候选列表自带内滚，内层优先消费手势，到边缘后外层接管，不冲突
                    .verticalScroll(rememberScrollState())
            ) {
            // ===== 顶部块（用户 09-09 重排）：当前发音人 + 试听 + 终值 =====
            // 发音人名跟随暂存选择（09-10 参数跟随）：暂存了候选就先显示候选对应的配置项名
            val boundConfigName = remember(entity.id, boundVoice, pendingVoice) {
                if (isBindingMode) {
                    val tag = pendingVoice ?: boundVoice
                    enabledConfigEntityByTag(tag)?.displayName ?: tag
                } else ""
            }
            val pendingName = pendingVoice
                ?.takeIf { !isBindingMode }
                ?.let { narrationEntityByVoice(it)?.displayName }
            val currentVoiceName = if (isBindingMode) boundConfigName
            else pendingName ?: entity.displayName
            // 09-11 重排（用户拍板）：「当前发音人」小标签独占一行，▶ 键与发音人名同一行
            //（此前 ▶ 垂直居中在两行文字块上，与名字行错位）；名字加省略号防长名硬裁
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "当前发音人",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        currentVoiceName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        // 试听当前声音（09-10 参数跟随）：绑定模式=跟随目标+草稿；
                        // 旁白=本配置项+暂存voice+草稿；播放中/合成中再点=停止复位（角色管理同款交互）
                        if (previewingKey == PREVIEW_KEY_CURRENT && previewState != PreviewState.IDLE) {
                            TaggedTtsPreviewPlayer.stop()
                            previewingKey = null
                            return@TextButton
                        }
                        previewingKey = PREVIEW_KEY_CURRENT
                        scope.launch {
                            val target = if (isBindingMode) draftParamsTarget() else null
                            TaggedTtsPreviewPlayer.play(context, target ?: draftEntity(pendingVoice), "你好，这是试听语音。")
                        }
                    }) {
                        Text(
                            previewLabel(PREVIEW_KEY_CURRENT),
                            color = previewLabelColor(PREVIEW_KEY_CURRENT),
                        )
                    }
                }
            }

            // ===== 终值（播放链同源三层乘积；三维恒显，用户 09-10）=====
            // 最终值恒为 配置×插件×全局（三层草稿实时跟随；音高 09-10 起同规格走草稿；
            // 接管判定已废除，恒乘积）
            val finalSpeed = speed * pluginSpeed * globalSpeed
            val finalVolume = volume * pluginVolume * globalVolume
            val finalPitch = pitch * pluginPitch * globalPitch
            Text(
                // 与卡片参数行口径不同（用户 09-10 二稿）：
                // 卡片=管道+1 位+加粗，弹窗/面板=逗号+2 位+无后缀（删除 x 乘号，与日志/试听保持一致）
                text = stringResource(
                    R.string.audio_params_final,
                    finalSpeed.toParamText(),
                    finalVolume.toParamText(),
                    finalPitch.toParamText(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                // 上下各 2dp（用户 09-11：顶部三行是紧密信息，行距收小，与音频参数弹窗同步）
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
            )

            // 分段两区（用户 09-11 下午「完全 MD3 版」终裁：官方 SegmentedButton 全 app 统一）：
            // 0=更换发音人 1=音频参数；当前发音人+终值两区共用，固定在分段之上。
            // 09-11 晚：官方 SegmentedButton 不支持"宽度随文字收缩"（文字被压成省略号），
            // 撤 equalWidth=false，随组件默认均分撑满
            SoftSegmentedTextToggle(
                options = listOf("更换发音人", "音频参数"),
                selectedIndex = panelTab,
                onSelect = { panelTab = it },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally),
            )

            if (panelTab == 0 && source != null) {
                // 分类定稿（用户 09-09 下拉版顺序）：全部 + 女/男系列（旁白不下拉，见下）；
                // 提取方式参照标签分类（剥尾部数字取汉字前缀），前缀在白名单内才可被分类筛选命中；
                // 对话/括号/本地音效/角色名等不入分类；「全部」=不筛选
                val voiceCategories = listOf(
                    "女青年", "男青年", "女中年", "男中年", "女老年", "男老年",
                    "少女", "少年", "女童", "男童", "女主", "男主", "特殊女", "特殊男",
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

                // 候选键：""=全部（不筛选）；选项与展示名在绑定模式内按当前范围动态生成
                //（09-11 晚起 0 项分类被隐藏，见下），此处不再静态定义。

                if (isBindingMode) {
                    // ===== 绑定模式：下拉定范围 + 常驻搜索（范围内）+ 列表（行内试听）=====
                    var selectedCategory by remember(entity.id) {
                        // 默认选中配置项当前标签所属分类（「女青年25」→「女青年」；九类外如「括号1」→全部）
                        mutableStateOf(voiceCategoryOf(config.speechRule.tagName))
                    }
                    var tagSearch by remember(entity.id) { mutableStateOf("") }
                    // 改绑到无启用配置的标签会掉进随机兜底，读声不可控，必须排除
                    // （用户 09-12 定稿：池子/记录存的都是 tag id，启用标签集合只收 tag，
                    //   tagName 兜底已移除——全链只认 tag）
                    val enabledTags = remember(entity.id) {
                        dbm.systemTtsV2.getAllGroupWithTts().flatMap { it.list }
                            .filter { it.isEnabled }
                            .mapNotNull {
                                (it.config as? TtsConfigurationDTO)?.speechRule?.tag
                                    ?.takeIf { t -> t.isNotEmpty() }
                            }.toMutableSet()
                    }
                    val poolEnabled = CharacterRecordsFile.readVoicePool(config.speechRule.tagRuleId)
                        .filter { it in enabledTags }
                    // 下拉项带括号项数（不含搜索过滤，选分类前就知道各范围有多少可选）；
                    // 0 项分类直接隐藏（用户 09-11 晚改，替代 09-09「不标数量」——不标会被误读成
                    // 信息缺失，没货的分类干脆不列）；「全部」恒在首位。
                    // 当前选中分类若恰好 0 项，AppSpinner 会自动回落到「全部」（values 不含当前值时
                    // 回调第一项），与既有「九类外标签→全部」语义一致，不会留下幽灵值。
                    val categoryCounts = poolEnabled.groupingBy { voiceCategoryOf(it) }.eachCount()
                    val categoryOptions: List<Pair<String, String>> =
                        listOf("" to "全部") +
                            voiceCategories.filter { (categoryCounts[it] ?: 0) > 0 }.map { it to it }
                    val categoryEntries = categoryOptions.map { (key, label) ->
                        val n = if (key.isEmpty()) poolEnabled.size else categoryCounts[key] ?: 0
                        "$label（${n}项）"
                    }
                    AppSpinner(
                        modifier = Modifier.fillMaxWidth(),
                        labelText = "分类",
                        value = selectedCategory ?: "",
                        values = categoryOptions.map { it.first },
                        entries = categoryEntries,
                        onSelectedChange = { key, _ ->
                            selectedCategory = (key as? String)?.takeIf { it.isNotEmpty() }
                        },
                    )
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        // 提示用 placeholder 不用 label（用户 09-11）：AlertDialog 正文槽把 LocalTextStyle
                        // 设为 bodyMedium 14sp，字段值/分类框/候选行全是 14sp，而 label 空置中态官方写死
                        // bodyLarge 16sp，观感比旁边内容大一圈；placeholder 吃正文槽 14sp 恰好对齐，
                        // 代价是输入后提示消失（搜索框可接受）
                        placeholder = { Text("搜索标签名/名字（当前范围内）") },
                        value = tagSearch,
                        onValueChange = { tagSearch = it },
                        singleLine = true,
                    )
                    // 搜索词同时匹配「标签名」与「配置项名」（用户 09-11 晚：记忆里是"女青年01晓晓"，
                    // 原先只匹配标签名，搜"晓晓"搜不到）。候选行展示的正是这两段，搜索范围须与展示一致。
                    val filtered = poolEnabled.filter { tag ->
                        (selectedCategory == null || voiceCategoryOf(tag) == selectedCategory) &&
                            (tagSearch.isBlank() ||
                                tag.contains(tagSearch) ||
                                enabledConfigEntityByTag(tag)?.displayName?.contains(tagSearch) == true)
                    }
                    // 当前绑定不在候选时补在顶部，防丢值；搜索态不补（否则顶部挂着不匹配项，破坏搜索语义）
                    val displayTags =
                        if (tagSearch.isBlank() && boundVoice.isNotEmpty() &&
                            filtered.none { it == boundVoice }
                        ) {
                            listOf(boundVoice) + filtered
                        } else filtered

                    // 候选列表：点行=暂存改绑；▶=只试听该标签对应的启用配置（不应用）
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .heightIn(max = maxListHeight)
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
                            // 候选行显示「标签名+配置项名」（用户 09-09：原 displayName·tag 反过来去点）
                            // 候选池已筛 fayinren.json∩启用配置（tag id 口径），用 enabledConfigEntityByTag 即可取到 displayName
                            val cfgName = enabledConfigEntityByTag(tag)?.displayName.orEmpty()
                            // 行尾平铺标记按钮（❤️🚶😈），点亮态由按钮自身 alpha 表达，文案不再追加
                            val rowMarks = remember(tag, marksVersion) {
                                VoiceMarksFile.get(config.speechRule.tagRuleId, tag)
                            }
                            val displayText = tag + cfgName
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
                                        (if (isPending && !isCurrent) "● " else "") + displayText,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    // 行尾平铺 4 个操作键后文字空间变小，长名硬裁改省略号（顶部发音人行同款）
                                    overflow = TextOverflow.Ellipsis,
                                    color = when {
                                        isPending -> MaterialTheme.colorScheme.primary
                                        isCurrent -> MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                )
                                TextButton(onClick = {
                                    // 行内试听：播放该标签对应启用配置的声音，不应用
                                    if (previewingKey == tag && previewState != PreviewState.IDLE) {
                                        TaggedTtsPreviewPlayer.stop()
                                        previewingKey = null
                                        return@TextButton
                                    }
                                    previewingKey = tag
                                    scope.launch {
                                        val target = withIO { enabledConfigEntityByTag(tag) }
                                        if (target != null) {
                                            TaggedTtsPreviewPlayer.play(context, target, "你好，这是试听语音。")
                                        } else {
                                            previewingKey = null
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.log_panel_rebind_no_config),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                    }
                                }) {
                                    Text(
                                        previewLabel(tag),
                                        color = previewLabelColor(tag),
                                    )
                                }
                                // 标记+删除直接平铺在行内（用户 09-12 晚：面板空间大，不用菜单收起）；
                                // 功能与角色管理 v10「管理发音人」一致：三标记多选 toggle（已点亮→取消，
                                // 未点亮→添加）+ 删除配置项；换声入口就是行本身，不重复加。
                                // emoji 是彩色字形染不上色，点亮/熄灭用 alpha 表达（IconButton 官方默认形态，
                                // alpha 为表达点亮态的必要手段）
                                listOf(
                                    "like" to "❤️",
                                    "neutral" to "🚶",
                                    "bad" to "😈",
                                ).forEach { (mark, emoji) ->
                                    IconButton(
                                        onClick = {
                                            if (VoiceMarksFile.toggle(config.speechRule.tagRuleId, tag, mark)) {
                                                marksVersion++
                                            }
                                        },
                                        modifier = Modifier.alpha(if (mark in rowMarks) 1f else 0.35f),
                                    ) {
                                        Text(emoji)
                                    }
                                }
                                // 删除（红色=破坏性操作，有确认弹窗兜底防误触）：
                                // 只删试听指向的那条启用配置；该标签无启用配置时无物可删，置灰
                                IconButton(
                                    onClick = { deleteConfirmTag = tag },
                                    enabled = cfgName.isNotEmpty(),
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "删除配置项",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // ===== 非绑定换声（用户 09-12 定稿）：按本配置项的 **tag（id）** 列同标签候选 =====
                    // 一个 tag 挂多个配置（多个 voice）、运行时同 tag 随机（"一个标签绑定多个发音人，
                    // 可随时切换"）——旁白/对话(duihua)/括号/本地音效全部天然按此归组，无需特殊分支；
                    // 最早那版报"旁白分类没有可用的配置项"，根因是拿显示名"旁白"去比 id（narration）。
                    // 点行=暂存选中，底部「确认」写本配置项 voice，
                    // 落库后主列表自动定位高亮被改项（sharedVM.pendingLocateConfigId）
                    val currentTagId = config.speechRule.tag
                    val currentTagName = config.speechRule.tagName
                    val narrationCandidates = remember(entity.id, currentTagId) {
                        allConfigs.mapNotNull { c ->
                            val dto = c.config as? TtsConfigurationDTO ?: return@mapNotNull null
                            if (dto.speechRule.tag != currentTagId) return@mapNotNull null
                            val v = (dto.source as? PluginTtsSource)?.voice
                                ?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                            Pair(v, c.displayName)
                        }.distinctBy { it.first }
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .heightIn(max = maxListHeight)
                            .verticalScroll(rememberScrollState())
                            .border(
                                0.5.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(8.dp),
                            ),
                    ) {
                        if (narrationCandidates.isEmpty()) {
                            Text(
                                if (currentTagName.isBlank()) "该标签下没有可用的配置项"
                                else "「$currentTagName」标签下没有可用的配置项",
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        narrationCandidates.forEach { (v, name) ->
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
                                    (if (isCurrent) "✓ " else "") +
                                        (if (isPending && !isCurrent) "● " else "") + name,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    color = if (isPending && !isCurrent) MaterialTheme.colorScheme.primary
                                    else if (isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                                TextButton(onClick = {
                                    if (previewingKey == v && previewState != PreviewState.IDLE) {
                                        TaggedTtsPreviewPlayer.stop()
                                        previewingKey = null
                                        return@TextButton
                                    }
                                    val target = allConfigs.firstOrNull {
                                        (it.config as? TtsConfigurationDTO)?.source
                                            ?.let { s -> (s as? PluginTtsSource)?.voice } == v
                                    }
                                    if (target != null) {
                                        previewingKey = v
                                        TaggedTtsPreviewPlayer.play(context, target, "你好，这是试听语音。")
                                    }
                                }) {
                                    Text(
                                        previewLabel(v),
                                        color = previewLabelColor(v),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ===== 音频参数大区（分段第二区；用户 09-10 改按维度：一次调一个维度的三层）=====
            if (panelTab == 1) {
                // 左右 4dp 已上移到整个内容 Column（用户 09-11：全面板统一 16dp）；
                // 底部无按钮行，补 4dp 底边距与左右一致收尾
                Column(
                    Modifier
                        .padding(bottom = 4.dp)
                ) {
                    // 顶部那条 HorizontalDivider 已撤（用户 09-10 晚）：第二级换成软槽分段后，
                    // 分割线与"两区切换 + 软槽"的层次重复，撤掉更干净
                    // 三层现值总览行已撤（用户 09-10 ④）：顶部终值行足够，每维三层滑杆同屏可见；
                    // 按维度分段（语速/音量/音高，共用组件），滑杆层标签=配置/插件/全局，
                    // 接管判定已废除（09-10）：三层恒显示可调
                    val targetDto = paramsTarget.config as? TtsConfigurationDTO
                    val hasPluginLayer = (targetDto?.source as? PluginTtsSource) != null
                    AudioParamsDimensionSection(
                        hasPluginLayer = hasPluginLayer,
                        cfgSpeed = speed, onCfgSpeed = { speed = it; speedDirty = true },
                        cfgVolume = volume, onCfgVolume = { volume = it; volumeDirty = true },
                        cfgPitch = pitch, onCfgPitch = { pitch = it; pitchDirty = true },
                        pluginSpeed = pluginSpeed, onPluginSpeed = { pluginSpeed = it; speedDirty = true },
                        pluginVolume = pluginVolume, onPluginVolume = { pluginVolume = it; volumeDirty = true },
                        pluginPitch = pluginPitch, onPluginPitch = { pluginPitch = it; pitchDirty = true },
                        globalSpeed = globalSpeed, onGlobalSpeed = { globalSpeed = it; speedDirty = true },
                        globalVolume = globalVolume, onGlobalVolume = { globalVolume = it; volumeDirty = true },
                        globalPitch = globalPitch, onGlobalPitch = { globalPitch = it; pitchDirty = true },
                        isDirty = { when (it) { 0 -> speedDirty; 1 -> volumeDirty; else -> pitchDirty } },
                        onResetDim = { dim ->
                            when (dim) {
                                0 -> { speed = 1f; pluginSpeed = 1f; globalSpeed = 1f }
                                1 -> { volume = 1f; pluginVolume = 1f; globalVolume = 1f }
                                else -> { pitch = 1f; pluginPitch = 1f; globalPitch = 1f }
                            }
                        },
                        onApplyDim = { applyDim(it) },
                    )
                }
            }
            } // 外层内容 Column 收尾
        },
        buttons = {
            // 底部操作行仅更换发音人区显示（用户 09-09：音频参数区各块自带重置/应用，
            // 取消/确定多余；删除后由内容底部边距收尾，弹窗点外部/返回键即关）
            if (panelTab == 0) {
                // 换声两段式确认（用户 09-08，即点即改反馈弱且易误触）+ 取消
                Row(
                    Modifier.fillMaxWidth(),
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
                                            bindingKey,
                                            selected,
                                        )
                                    }
                                    if (ok) boundVoice = selected
                                    pendingVoice = null
                                    Toast.makeText(
                                        context,
                                        if (ok) "已将「$bindingKey」的发音人换为 $selected"
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
        },
    )

    // 删除确认弹窗（行内 🗑 入口）：只删试听指向的那一条配置项（标签下第一条启用配置），
    // 同标签其他配置不受影响；删空时该标签一并从标签池移除，受影响角色由规则自动重分配
    deleteConfirmTag?.let { delTag ->
        val delTarget = enabledConfigEntityByTag(delTag)
        AlertDialog(
            onDismissRequest = { deleteConfirmTag = null },
            title = { Text("删除确认") },
            text = {
                Text(
                    "确认删除配置项【" + (delTarget?.displayName ?: delTag) + "】？\n\n" +
                        "只删这一条，同标签其他配置不受影响；删空时该标签会一并从标签池移除。",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteConfirmTag = null
                        if (delTarget != null) deletePreviewedConfig(delTarget)
                    },
                    enabled = delTarget != null,
                ) { Text("确认删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmTag = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/** 顶部「当前发音人」试听键的 previewingKey 哨兵（行键为 tag/voice 字符串，避撞） */
private const val PREVIEW_KEY_CURRENT = "‹current›"

/** 单次 LaunchedEffect 简写：key 变化时只执行一次 */
@Composable
private fun LaunchedEffectOnce(key: Any?, block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {
    androidx.compose.runtime.LaunchedEffect(key) { block() }
}

private fun snapParam(v: Float): Float = (kotlin.math.round(v * 100f) / 100f)
