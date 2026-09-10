package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.conf.SysTtsConfig

/**
 * 编辑页「三键直出」音频参数行：语速/音量/音高（各带该维终值，实时跟随配置层草稿），
 * 放在试听文本正下方（基本信息卡末尾）。
 *
 * 点键 = 打开 [AudioParamsDialog] 并直接落在该维度（`initialDim`）。
 * 原「单维弹窗」已于 09-10 晚撤销（用户定稿）：编辑页三键与卡片⋮入口**共用同一个** AudioParamsDialog
 * ——顶部同样有 发音人 + ▶试听 + 终值行，主体是维度软槽 + 该维三层滑杆 + 重置/应用，
 * 不再是"只有该维滑杆、没有顶部"的第三种弹窗。
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
