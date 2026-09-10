package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.drake.net.utils.withIO
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts.PreviewState
import com.github.jing332.tts.TaggedTtsPreviewPlayer
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.conf.AppConfig
import com.github.jing332.tts_server_android.conf.SysTtsConfig
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import kotlinx.coroutines.launch

/**
 * 编辑页「音频参数」值行区（用户 09-10 晚定稿，取代原三键 FilterChip → 开弹窗）：
 *
 * 三行值行：`语速    1.00 ▾` / `音量    1.30 ▴` / `音高    1.00 ▾`
 * - 右侧数值＝该维**终值**（配置×插件×全局，FOLLOW 视为 1），跟随草稿实时变；
 * - **点行就地展开**该维三层滑杆（发音人/插件/全局，无插件源自动只有两层）+ ▶试听 / 重置 / 应用，
 *   同一时刻只展开一维，再点收起；
 * - ▶试听＝草稿试听：念试听文本、该维三层草稿经 pluginParamsOverride/globalParamsOverride 全带入，
 *   不用先应用就能听完整终值效果（▶→…→■，再点停止，播完复位）；
 * - 应用＝该维三层一起落库（配置层双写页面内存防旧值覆盖、插件层失效卡片缓存、全局写 SysTtsConfig、
 *   notifyUpdateConfig 立即生效），不关展开、toast 反馈；重置＝该维三层草稿回 1.0（不落库）。
 *
 * 为什么这么改（用户定稿）：调参全程不开弹窗、三维终值一直看得见、点一次就进编辑，
 * 且与编辑页其它「行」的形态一致（不再与顶部模式切换胶囊撞形）。
 * 卡片⋮入口与日志快捷面板仍走共用的 [AudioParamsDialog]（那边需要换发音人上下文/面板排版）。
 *
 * 维度下标：0=语速 1=音量 2=音高。
 */

@Composable
fun AudioParamsDimRows(
    modifier: Modifier = Modifier,
    systemTts: SystemTtsV2,
    onSysttsChange: (SystemTtsV2) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val config = systemTts.config as? TtsConfigurationDTO ?: return
    val source = config.source as? PluginTtsSource
    val plugin = source?.let { dbm.pluginDao.getByPluginId(it.pluginId) }
    val hasPluginLayer = source != null

    // 展开的维度（-1=全收起）
    var expanded by remember(systemTts.id) { mutableStateOf(-1) }

    // 三层草稿（三维共用一份；展开哪维就调哪维）
    var speed by remember(systemTts.id) { mutableStateOf(config.audioParams.speed) }
    var volume by remember(systemTts.id) { mutableStateOf(config.audioParams.volume) }
    var pitch by remember(systemTts.id) { mutableStateOf(config.audioParams.pitch) }
    var pluginSpeed by remember { mutableStateOf(plugin?.audioParams?.speed ?: 1f) }
    var pluginVolume by remember { mutableStateOf(plugin?.audioParams?.volume ?: 1f) }
    var pluginPitch by remember { mutableStateOf(plugin?.audioParams?.pitch ?: 1f) }
    var globalSpeed by remember { mutableStateOf(SysTtsConfig.audioParamsSpeed) }
    var globalVolume by remember { mutableStateOf(SysTtsConfig.audioParamsVolume) }
    var globalPitch by remember { mutableStateOf(SysTtsConfig.audioParamsPitch) }

    // 按维度脏标记：该维任一层滑杆改动置 true，应用成功清除（应用键带 ●）
    var speedDirty by remember(systemTts.id) { mutableStateOf(false) }
    var volumeDirty by remember(systemTts.id) { mutableStateOf(false) }
    var pitchDirty by remember(systemTts.id) { mutableStateOf(false) }
    fun isDirty(dim: Int) = when (dim) { 0 -> speedDirty; 1 -> volumeDirty; else -> pitchDirty }
    fun clearDirty(dim: Int) {
        when (dim) { 0 -> speedDirty = false; 1 -> volumeDirty = false; else -> pitchDirty = false }
    }

    // ▶试听状态机（同弹窗/日志面板）
    val previewState by TaggedTtsPreviewPlayer.state.collectAsState()
    var previewing by remember(systemTts.id) { mutableStateOf(false) }
    LaunchedEffect(previewState) { if (previewState == PreviewState.IDLE) previewing = false }

    /** 该维终值（走草稿，跟随滑杆实时变） */
    fun finalOf(dim: Int): Float {
        val c = when (dim) { 0 -> speed; 1 -> volume; else -> pitch }
        val p = if (hasPluginLayer) when (dim) { 0 -> pluginSpeed; 1 -> pluginVolume; else -> pluginPitch } else 1f
        val g = when (dim) { 0 -> globalSpeed; 1 -> globalVolume; else -> globalPitch }
        return c * p * g
    }

    fun draftEntity(): SystemTtsV2 = systemTts.copy(
        config = config.copy(
            audioParams = config.audioParams.copy(
                speed = snap(speed), volume = snap(volume), pitch = snap(pitch)
            )
        )
    )

    /** 该维三层一起落库（与 AudioParamsDialog.applyDim 完全同源） */
    fun applyDim(dim: Int) {
        scope.launch {
            val newConfig = withIO {
                val nc = config.copy(
                    audioParams = config.audioParams.copy(
                        speed = if (dim == 0) snap(speed) else config.audioParams.speed,
                        volume = if (dim == 1) snap(volume) else config.audioParams.volume,
                        pitch = if (dim == 2) snap(pitch) else config.audioParams.pitch,
                    )
                )
                dbm.systemTtsV2.update(systemTts.copy(config = nc))
                if (plugin != null) {
                    dbm.pluginDao.update(
                        plugin.copy(
                            audioParams = plugin.audioParams.copy(
                                speed = if (dim == 0) snap(pluginSpeed) else plugin.audioParams.speed,
                                volume = if (dim == 1) snap(pluginVolume) else plugin.audioParams.volume,
                                pitch = if (dim == 2) snap(pluginPitch) else plugin.audioParams.pitch,
                            )
                        )
                    )
                    PluginDescriptor.invalidatePluginParamsCache(plugin.pluginId)
                }
                when (dim) {
                    0 -> SysTtsConfig.audioParamsSpeed = snap(globalSpeed)
                    1 -> SysTtsConfig.audioParamsVolume = snap(globalVolume)
                    else -> SysTtsConfig.audioParamsPitch = snap(globalPitch)
                }
                SystemTtsService.notifyUpdateConfig()
                nc
            }
            // 配置层双写：回写页面内存，防"应用后再保存"被旧内存覆盖
            onSysttsChange(systemTts.copy(config = newConfig))
            clearDirty(dim)
            Toast.makeText(
                context,
                context.getString(R.string.audio_params_apply_dim_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val tagCfg = stringResource(R.string.audio_params_tag_config)
    val tagPlugin = stringResource(R.string.audio_params_tag_plugin)
    val tagGlobal = stringResource(R.string.audio_params_tag_global)

    Column(modifier.fillMaxWidth()) {
        audioParamsDimNames.forEachIndexed { dim, name ->
            val open = expanded == dim
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = if (open) -1 else dim }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (open) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "%.2f".format(finalOf(dim)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (open) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(52.dp),
                )
                Text(
                    if (open) "▴" else "▾",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(16.dp),
                )
            }

            // 就地展开：该维三层滑杆 + ▶试听/重置/应用（浅底槽，与弹窗的软槽同族视觉）
            if (open) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // 与音频参数弹窗**同款排版**（用户 09-10 晚定稿）：上=终值行 + ▶试听，
                    // 中=该维三层滑杆，下=重置/应用；只是内嵌展开、不弹窗，也不出现维度选择器
                    // （维度已由点的那一行决定）
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "$name %.2f".format(finalOf(dim)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = {
                            // ▶试听=该维三层草稿直接合成，不用先应用（同弹窗）
                            if (previewing && previewState != PreviewState.IDLE) {
                                TaggedTtsPreviewPlayer.stop()
                                previewing = false
                                return@TextButton
                            }
                            previewing = true
                            scope.launch {
                                val auditionText = AppConfig.testSampleText.value
                                    .ifBlank { "你好，这是试听语音。" }
                                TaggedTtsPreviewPlayer.play(
                                    context, draftEntity(), auditionText,
                                    pluginParamsOverride = plugin?.audioParams?.copy(
                                        speed = snap(pluginSpeed),
                                        volume = snap(pluginVolume),
                                        pitch = snap(pluginPitch),
                                    ),
                                    globalParamsOverride = AudioParams(
                                        speed = snap(globalSpeed),
                                        volume = snap(globalVolume),
                                        pitch = snap(globalPitch),
                                    ),
                                )
                            }
                        }) {
                            Text(
                                when {
                                    previewing && previewState == PreviewState.PLAYING -> "■ 停止"
                                    previewing -> "… 合成中"
                                    else -> "▶ 试听"
                                },
                                color = if (previewing) MaterialTheme.colorScheme.tertiary
                                else Color.Unspecified,
                            )
                        }
                    }

                    when (dim) {
                        0 -> {
                            LayerSlider(tagCfg, speed) { speed = it; speedDirty = true }
                            if (hasPluginLayer) {
                                LayerSlider(tagPlugin, pluginSpeed) { pluginSpeed = it; speedDirty = true }
                            }
                            LayerSlider(tagGlobal, globalSpeed) { globalSpeed = it; speedDirty = true }
                        }
                        1 -> {
                            LayerSlider(tagCfg, volume) { volume = it; volumeDirty = true }
                            if (hasPluginLayer) {
                                LayerSlider(tagPlugin, pluginVolume) { pluginVolume = it; volumeDirty = true }
                            }
                            LayerSlider(tagGlobal, globalVolume) { globalVolume = it; volumeDirty = true }
                        }
                        else -> {
                            LayerSlider(tagCfg, pitch) { pitch = it; pitchDirty = true }
                            if (hasPluginLayer) {
                                LayerSlider(tagPlugin, pluginPitch) { pluginPitch = it; pitchDirty = true }
                            }
                            LayerSlider(tagGlobal, globalPitch) { globalPitch = it; pitchDirty = true }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            when (dim) {
                                0 -> { speed = 1f; pluginSpeed = 1f; globalSpeed = 1f }
                                1 -> { volume = 1f; pluginVolume = 1f; globalVolume = 1f }
                                else -> { pitch = 1f; pluginPitch = 1f; globalPitch = 1f }
                            }
                        }) { Text(stringResource(R.string.reset)) }
                        TextButton(onClick = { applyDim(dim) }) {
                            Text((if (isDirty(dim)) "● " else "") + stringResource(R.string.audio_params_apply))
                        }
                    }
                }
            }
        }
    }
}
