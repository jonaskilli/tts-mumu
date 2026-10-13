package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import kotlinx.coroutines.runBlocking

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

    // 居中弹窗（10-13 用户令「适合居中弹窗的都要用居中弹窗…底部弹窗才是少见的特殊形式」）
    AppDialog(
        onDismissRequest = {
        val ret = runBlocking {
            for (callback in callbacks) {
                if (!callback.onSave()) return@runBlocking false
            }
            true
        }

        if (ret) onDismissRequest()
    },
    title = {},
    content = {
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
            // 插件TTS：音频参数区域整体不渲染（用户 09-07 定稿：删除底部滑块区，
            // 也不要在底部加入口卡；调参统一走编辑页顶部「音频参数」按钮与列表⋮菜单，
            // 两处均弹三层音频参数弹窗）。
            // 本地TTS/BGM 保留原 ParamsEditScreen（含 PCM 采样率/直接播放等专属设置）
            val isPluginTts = (systts.config as? TtsConfigurationDTO)?.source is PluginTtsSource
            if (!isPluginTts) {
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
        },
        buttons = {},
    )
}
