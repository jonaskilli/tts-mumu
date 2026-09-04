package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.jing332.common.utils.toast
import com.github.jing332.database.entities.systts.EmptyConfiguration
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.list.ui.ConfigUiFactory
import kotlinx.coroutines.launch

@Composable
fun TtsEditContainerScreen(
    modifier: Modifier,
    systts: SystemTtsV2,
    onSysttsChange: (SystemTtsV2) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val ui = remember(systts.config) { ConfigUiFactory.from(systts.config) }
    val context = LocalContext.current

    if (ui == null || systts.config == EmptyConfiguration) {
        LaunchedEffect(ui) {
            context.toast(R.string.cannot_empty)
            onCancel()
        }
        return
    }

    val callbacks = rememberSaveCallBacks()
    val scope = rememberCoroutineScope()
    CompositionLocalProvider(LocalSaveCallBack provides callbacks) {
        ui.FullEditScreen(
            modifier = modifier,
            systemTts = systts,
            content = {
                SpeechRuleEditScreen(
                    Modifier.padding(8.dp),
                    systts,
                    onSysttsChange = onSysttsChange,
                    // 完整编辑页：正文（规则脚本/标签字段）+基本信息 同一张淡底卡；
                    // 音频参数行/朗读切换行由组件留在卡外平铺
                    bodyInCard = true,
                    cardTrailer = {
                        BasicInfoEditScreen(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            systemTts = systts,
                            onSystemTtsChange = onSysttsChange,
                        )
                    }
                )
            },
            onSystemTtsChange = onSysttsChange,
            onSave = {
                scope.launch {
                    for (callBack in callbacks) {
                        if (!callBack.onSave()) return@launch
                    }

                    onSave()
                }
            },
            onCancel = onCancel
        )
    }
}

@Preview
@Composable
private fun PreviewContainer() {
    var systts by remember {
        mutableStateOf(
            SystemTtsV2(
                config = TtsConfigurationDTO(
                    source = LocalTtsSource(engine = "")
                )
            )
        )
    }
    TtsEditContainerScreen(
        modifier = Modifier.fillMaxSize(),
        systts = systts,
        onSysttsChange = { systts = it },
        onSave = {

        },
        onCancel = {

        }
    )
}