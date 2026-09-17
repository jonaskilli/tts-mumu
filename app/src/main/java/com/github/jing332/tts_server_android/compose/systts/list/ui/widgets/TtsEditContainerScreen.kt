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
import com.github.jing332.tts_server_android.compose.systts.AuditionDialog
import com.github.jing332.tts_server_android.compose.systts.list.ui.ConfigUiFactory
import com.github.jing332.tts_server_android.conf.AppConfig
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

    // 标签态卡片末尾的「试听文本 + 音频参数值行」所需状态（用户 09-10 晚补）：
    // 标签态此前只有 正文（规则脚本/标签）+ 基本信息，比朗读全部态少了试听文本与音频参数区，现在两边一致。
    // 试听走 AuditionDialog；音频参数改为值行+就地展开（AudioParamsDimRows），编辑页不再需要弹窗入口。
    var auditionSystts by remember { mutableStateOf<SystemTtsV2?>(null) }
    // 音频参数三层草稿快照（AudioParamsDimRows 上报）：🎧 试听带未应用草稿（用户 09-17）
    var audioDraft by remember { mutableStateOf<AudioParamsDraft?>(null) }

    // 本地音效配置（tagName=本地音效N）用专用试听文本，与全局文本互不影响（用户 09-13）。
    // ⚠️ 必须 as? 安全转换：BGM 等非 TTS 配置没有 speechRule，无条件强转会在
    // 进入编辑页时直接 ClassCastException 闪退（目目 09-17：添加背景音乐即闪退）；
    // 非 TTS 配置本就不走下面的试听/音频参数区，isLocalSound 恒 false 即可
    val isLocalSound =
        (systts.config as? TtsConfigurationDTO)?.let { isLocalSoundTagName(it.speechRule.tagName) }
            ?: false

    auditionSystts?.let { target ->
        val d = audioDraft
        AuditionDialog(
            // 配置层草稿拼进实体；插件/全局两层走 override（null=读库值）
            systts = if (d != null) target.withAudioParams(d.config) else target,
            text = if (isLocalSound) AppConfig.localSoundSampleText.value else AppConfig.testSampleText.value,
            pluginParamsOverride = d?.plugin,
            globalParamsOverride = d?.global,
        ) { auditionSystts = null }
    }

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
                        // 试听文本 + 三键直出音频参数（用户 09-10 晚：标签态漏了这两块，与朗读全部态对齐；
                        // 横向 12dp 与卡片内其它字段同边距）
                        AuditionTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(top = 8.dp),
                            onAudition = { auditionSystts = systts },
                            isLocalSound = isLocalSound,
                        )
                        AudioParamsDimRows(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(top = 4.dp),
                            systemTts = systts,
                            onSysttsChange = onSysttsChange,
                            onDraftChange = { audioDraft = it },
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
