package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.R
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickEditBottomSheet(
    onDismissRequest: () -> Unit,
    systts: SystemTtsV2,
    onSysttsChange: (SystemTtsV2) -> Unit,
) {
    val ui = remember {
        com.github.jing332.tts_server_android.compose.systts.list.ui.ConfigUiFactory.from(
            systts.config
        ) ?: throw IllegalArgumentException("Not supported config type: ${systts.config}")
    }
    val callbacks = rememberSaveCallBacks()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = {
        val ret = runBlocking {
            for (callback in callbacks) {
                if (!callback.onSave()) return@runBlocking false
            }
            true
        }

        if (ret) onDismissRequest()
    }) {
        Column(
            Modifier
                .padding(top = 12.dp)
                .padding(horizontal = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (ui.showSpeechEdit)
                CompositionLocalProvider(LocalSaveCallBack provides callbacks) {
                    SpeechRuleEditScreen(
                        modifier = Modifier.fillMaxWidth(),
                        systts = systts,
                        onSysttsChange = onSysttsChange,
                        showSpeechTarget = true
                    )
                }

            BasicInfoEditScreen(
                modifier = Modifier,
                systemTts = systts,
                onSystemTtsChange = onSysttsChange
            )
            // 插件TTS：滑杆区已删（用户 09-07 定稿：三层调节统一走「音频参数」弹窗，
            // 与编辑页卡片/列表⋮菜单同一弹窗），此处换成弹窗入口卡；
            // 本地TTS/BGM 保留原 ParamsEditScreen（含 PCM 采样率/直接播放等专属设置）
            val isPluginTts = (systts.config as? TtsConfigurationDTO)?.source is PluginTtsSource
            if (isPluginTts) {
                AudioParamsQuickEntry(
                    systemTts = systts,
                    onSysttsChange = onSysttsChange,
                )
            } else {
                ui.ParamsEditScreen(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    systemTts = systts,
                    onSystemTtsChange = onSysttsChange
                )
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

/**
 * 快速编辑弹层里的「音频参数」入口卡：点击弹三层音频参数弹窗（与编辑页 AudioParamsCard 同款交互）。
 */
@Composable
private fun AudioParamsQuickEntry(
    systemTts: SystemTtsV2,
    onSysttsChange: (SystemTtsV2) -> Unit,
) {
    var showParamsDialog by remember { mutableStateOf(false) }
    if (showParamsDialog) {
        AudioParamsDialog(
            onDismissRequest = { showParamsDialog = false },
            systemTts = systemTts,
            onSysttsChange = onSysttsChange,
        )
    }
    SectionCard(
        title = stringResource(id = R.string.audio_params),
        icon = Icons.Default.Speed,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clickable { showParamsDialog = true },
    ) {
        Text(
            text = stringResource(R.string.audio_params_card_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        )
    }
}
