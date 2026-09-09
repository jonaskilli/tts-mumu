package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.drake.net.utils.withIO
import com.github.jing332.compose.widgets.AppDialog
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
 * 编辑页「三键直出」音频参数（用户 09-10 定稿，取代试听行⚡→总弹窗路径）：
 * - [AudioParamsDimChipsRow]：试听文本下方直接列 语速/音量/音高 三个键（FilterChip），
 *   键上带该维终值（配置×插件×全局，与卡片参数行同口径）；点键回调维度下标；
 * - [AudioParamsDimDialog]：单维弹窗——标题即维度名，内容=该维 配置→插件→全局 三层滑杆
 *   （无插件源自动只有 配置/全局 两层）+ 重置/应用；应用=该维三层一起落库
 *   （配置层双写页面内存防旧值覆盖），立即生效不关弹窗；
 *   左下角 ▶试听键（用户 09-10 补定）：念试听文本、用该维配置层草稿直接合成——
 *   调完滑杆不用先应用就能听（▶→…→■，再点停止，播完自动复位）；
 * - 卡片⋮菜单「音频参数」入口不受影响，仍打开 AudioParamsDialog 折叠手风琴总弹窗。
 *
 * 维度下标：0=语速 1=音量 2=音高。
 */

/** 该维终值 = 配置 × 插件 × 全局（无插件源插件层按 1.0；09-10 接管判定废除） */
private fun dimFinalValue(dim: Int, cfg: AudioParams, pluginParams: AudioParams?): Float {
    val p = when (dim) {
        0 -> pluginParams?.speed ?: 1f
        1 -> pluginParams?.volume ?: 1f
        else -> pluginParams?.pitch ?: 1f
    }
    val g = when (dim) {
        0 -> SysTtsConfig.audioParamsSpeed
        1 -> SysTtsConfig.audioParamsVolume
        else -> SysTtsConfig.audioParamsPitch
    }
    val c = when (dim) {
        0 -> cfg.speed
        1 -> cfg.volume
        else -> cfg.pitch
    }
    return c * p * g
}

/**
 * 三键行：语速/音量/音高（各带该维终值，实时跟随配置层草稿），点键弹出对应单维弹窗。
 * 放在试听文本正下方（基本信息卡末尾）。
 */
@Composable
fun AudioParamsDimChipsRow(
    modifier: Modifier = Modifier,
    systemTts: SystemTtsV2,
    onSelectDim: (Int) -> Unit,
) {
    val config = systemTts.config as? TtsConfigurationDTO ?: return
    val source = config.source as? PluginTtsSource
    val pluginParams = source?.let { dbm.pluginDao.getByPluginId(it.pluginId)?.audioParams }

    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        audioParamsDimNames.forEachIndexed { i, name ->
            FilterChip(
                selected = false,
                onClick = { onSelectDim(i) },
                label = { Text("$name %.2f".format(dimFinalValue(i, config.audioParams, pluginParams))) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 取 AudioParams 的维度值 */
private fun dimParam(p: AudioParams, dim: Int): Float = when (dim) {
    0 -> p.speed
    1 -> p.volume
    else -> p.pitch
}

/** 返回该维写为新值的 AudioParams 副本 */
private fun dimCopy(p: AudioParams, dim: Int, v: Float): AudioParams = when (dim) {
    0 -> p.copy(speed = v)
    1 -> p.copy(volume = v)
    else -> p.copy(pitch = v)
}

/**
 * 单维音频参数弹窗：该维 配置→插件→全局 三层滑杆 + 重置/应用（应用=该维三层一起落库）。
 * 落库/生效逻辑与 AudioParamsDialog.applyDim 完全同源。
 */
@Composable
fun AudioParamsDimDialog(
    dim: Int,
    onDismissRequest: () -> Unit,
    systemTts: SystemTtsV2,
    onSysttsChange: (SystemTtsV2) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val config = systemTts.config as? TtsConfigurationDTO ?: return
    val source = config.source as? PluginTtsSource
    val plugin = source?.let { dbm.pluginDao.getByPluginId(it.pluginId) }
    val hasPluginLayer = source != null

    // 该维三层草稿
    var cfgVal by remember(systemTts.id, dim) { mutableStateOf(dimParam(config.audioParams, dim)) }
    var pluginVal by remember(systemTts.id, dim) {
        mutableStateOf(plugin?.audioParams?.let { dimParam(it, dim) } ?: 1f)
    }
    var globalVal by remember(systemTts.id, dim) {
        mutableStateOf(
            when (dim) {
                0 -> SysTtsConfig.audioParamsSpeed
                1 -> SysTtsConfig.audioParamsVolume
                else -> SysTtsConfig.audioParamsPitch
            }
        )
    }
    // 脏标记：任一层滑杆改动置 true，应用成功清除
    var dirty by remember(systemTts.id, dim) { mutableStateOf(false) }

    // ▶试听状态机（同旧总弹窗：▶ →(点击)… →(出声)■ →(播完复位)▶）
    val previewState by TaggedTtsPreviewPlayer.state.collectAsState()
    var previewing by remember(systemTts.id, dim) { mutableStateOf(false) }
    LaunchedEffect(previewState) { if (previewState == PreviewState.IDLE) previewing = false }

    /** 试听实体=本配置项+该维配置层草稿（其他维/插件层/全局层取已存值）：不点应用也能先听效果 */
    fun draftEntity(): SystemTtsV2 = systemTts.copy(
        config = config.copy(audioParams = dimCopy(config.audioParams, dim, snap(cfgVal)))
    )

    /** 应用：该维三层一起落库——配置层双写（库+页面内存），插件/全局层照常写入；立即生效不关弹窗 */
    fun apply() {
        scope.launch {
            val newConfig = withIO {
                val nc = config.copy(audioParams = dimCopy(config.audioParams, dim, snap(cfgVal)))
                dbm.systemTtsV2.update(systemTts.copy(config = nc))
                if (plugin != null) {
                    dbm.pluginDao.update(
                        plugin.copy(audioParams = dimCopy(plugin.audioParams, dim, snap(pluginVal)))
                    )
                    // 卡片"插件语速/音量"显示缓存失效，应用后重查
                    com.github.jing332.tts_server_android.compose.systts.list.ui.PluginDescriptor
                        .invalidatePluginParamsCache(plugin.pluginId)
                }
                when (dim) {
                    0 -> SysTtsConfig.audioParamsSpeed = snap(globalVal)
                    1 -> SysTtsConfig.audioParamsVolume = snap(globalVal)
                    else -> SysTtsConfig.audioParamsPitch = snap(globalVal)
                }
                SystemTtsService.notifyUpdateConfig()
                nc
            }
            // 配置层双写：回写页面内存，防"应用后再保存"被旧内存覆盖
            onSysttsChange(systemTts.copy(config = newConfig))
            dirty = false
            Toast.makeText(
                context,
                context.getString(R.string.audio_params_apply_dim_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    AppDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(audioParamsDimNames[dim]) },
        content = {
            Column(Modifier.fillMaxWidth()) {
                LayerSlider(
                    stringResource(R.string.audio_params_tag_config), cfgVal,
                    { cfgVal = it; dirty = true },
                )
                if (hasPluginLayer) {
                    LayerSlider(
                        stringResource(R.string.audio_params_tag_plugin), pluginVal,
                        { pluginVal = it; dirty = true },
                    )
                }
                LayerSlider(
                    stringResource(R.string.audio_params_tag_global), globalVal,
                    { globalVal = it; dirty = true },
                )

                // 重置=该维三层草稿回 1.0（不落库）；应用=该维三层一起落库
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = {
                        cfgVal = 1f; pluginVal = 1f; globalVal = 1f
                    }) { Text(stringResource(R.string.reset)) }
                    TextButton(onClick = { apply() }) {
                        Text((if (dirty) "● " else "") + stringResource(R.string.audio_params_apply))
                    }
                }
            }
        },
        buttons = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // ▶试听：念试听文本、用草稿参数合成（不用先应用）；播放中/合成中再点=停止复位
                TextButton(onClick = {
                    if (previewing && previewState != PreviewState.IDLE) {
                        TaggedTtsPreviewPlayer.stop()
                        previewing = false
                        return@TextButton
                    }
                    previewing = true
                    scope.launch {
                        // 文本被清空时回落默认句，避免合成空串
                        val auditionText = AppConfig.testSampleText.value
                            .ifBlank { "你好，这是试听语音。" }
                        TaggedTtsPreviewPlayer.play(context, draftEntity(), auditionText)
                    }
                }) {
                    Text(
                        when {
                            previewing && previewState == PreviewState.PLAYING -> "■ 停止"
                            previewing -> "… 合成中"
                            else -> "▶ 试听"
                        },
                        color = if (previewing) MaterialTheme.colorScheme.tertiary else Color.Unspecified,
                    )
                }
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
    )
}
