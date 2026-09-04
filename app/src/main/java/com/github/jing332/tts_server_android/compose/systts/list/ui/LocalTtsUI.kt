package com.github.jing332.tts_server_android.compose.systts.list.ui


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drake.net.utils.withIO
import com.github.jing332.common.utils.toCountryFlagEmoji
import com.github.jing332.common.utils.toScale
import com.github.jing332.compose.widgets.AppSpinner
import com.github.jing332.compose.widgets.DenseOutlinedField
import com.github.jing332.compose.widgets.LabelSlider
import com.github.jing332.compose.widgets.LoadingContent
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.tts_server_android.PackageDrawable
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.AuditionDialog
import com.github.jing332.tts_server_android.constant.SpeechTarget
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.AuditionTextField
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.BasicInfoEditScreen
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.SaveActionHandler
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.SectionCard
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog

class LocalTtsUI() : IConfigUI() {

    @Composable
    override fun ParamsEditScreen(
        modifier: Modifier,
        systemTts: SystemTtsV2,
        onSystemTtsChange: (SystemTtsV2) -> Unit,
    ) {
        val config = systemTts.config as TtsConfigurationDTO
        val source = config.source as LocalTtsSource
        val params = config.audioParams

        var showDirectPlayHelpDialog by remember { mutableStateOf(false) }
        var showPcmSampleRateHelpDialog by remember { mutableStateOf(false) }
        if (showDirectPlayHelpDialog)
            AlertDialog(
                onDismissRequest = { showDirectPlayHelpDialog = false },
                title = { Text(stringResource(id = R.string.systts_direct_play_help)) },
                text = { Text(stringResource(id = R.string.systts_direct_play_help_msg)) },
                confirmButton = {
                    TextButton(onClick = { showDirectPlayHelpDialog = false }) {
                        Text(text = stringResource(id = android.R.string.ok))
                    }
                }
            )
        if (showPcmSampleRateHelpDialog)
            AlertDialog(
                onDismissRequest = { showPcmSampleRateHelpDialog = false },
                title = { Text(stringResource(id = R.string.systts_pcm_sample_rate_help)) },
                text = { Text(stringResource(id = R.string.systts_pcm_sample_rate_help_msg)) },
                confirmButton = {
                    TextButton(onClick = { showPcmSampleRateHelpDialog = false }) {
                        Text(text = stringResource(id = android.R.string.ok))
                    }
                }
            )

        Column(modifier) {
            LabelSlider(
                text = stringResource(R.string.label_speech_rate, "%.2f".format(params.speed)),
                value = params.speed, onValueChange = {
                onSystemTtsChange(
                    systemTts.copy(
                        config = config.copy(audioParams = params.copy(speed = it.toScale(2)))
                    )
                )
            }, valueRange = 0.1f..3f, step = 0.05f)

            LabelSlider(
                text = stringResource(R.string.label_speech_pitch, "%.2f".format(params.pitch)),
                value = params.pitch, onValueChange = {
                onSystemTtsChange(
                    systemTts.copy(
                        config = config.copy(audioParams = params.copy(pitch = it.toScale(2)))
                    )
                )
            }, valueRange = 0.1f..3f, step = 0.05f)

            LabelSlider(
                text = stringResource(R.string.label_speech_volume, "%.2f".format(params.volume)),
                value = params.volume, onValueChange = {
                onSystemTtsChange(
                    systemTts.copy(
                        config = config.copy(audioParams = params.copy(volume = it.toScale(2)))
                    )
                )
            }, valueRange = 0.1f..3f, step = 0.05f)

            // PCM兜底采样率：独立一行（旧版与直接播放/重置挤一行，weight拉宽+label换行
            // 疑似把整行撑出大段不可见高度，也是"参数卡尾部大片空白"的头号嫌疑）
            var sampleRateStr by remember { mutableStateOf(config.audioFormat.sampleRate.toString()) }
            DenseOutlinedField(
                label = { Text(stringResource(R.string.systts_pcm_sample_rate)) },
                trailingIcon = {
                    IconButton(onClick = { showPcmSampleRateHelpDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            stringResource(id = R.string.systts_pcm_sample_rate_help)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                value = sampleRateStr,
                onValueChange = {
                    if (it.isEmpty()) {
                        sampleRateStr = it
                    } else {
                        sampleRateStr = it.toInt().toString()
                        onSystemTtsChange(systemTts.copy(config = config.copy(audioFormat = config.audioFormat.apply {
                            this.sampleRate = it.toInt()
                        })))
                    }
                }
            )

            // 直接播放 + 重置：一行收尾
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(role = Role.Checkbox) {
                            onSystemTtsChange(
                                systemTts.copy(
                                    config = config.copy(
                                        source = source.copy(
                                            isDirectPlayMode = !source.isDirectPlayMode
                                        ),
                                    )
                                )
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = source.isDirectPlayMode, onCheckedChange = null)
                    Text(text = stringResource(id = R.string.direct_play))
                    IconButton(onClick = { showDirectPlayHelpDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            stringResource(id = R.string.systts_direct_play_help)
                        )
                    }
                }

                TextButton(onClick = {
                    onSystemTtsChange(
                        systemTts.copy(
                            config = config.copy(
                                audioParams = params.copy(speed = 1f, volume = 1f, pitch = 1f)
                            )
                        )
                    )
                }) {
                    Text(stringResource(id = R.string.reset))
                }
            }
        }
    }

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
            title = stringResource(id = R.string.edit_local_tts),
            onCancel = onCancel,
            onSave = onSave,
        ) {
            // 标签态：正文+基本信息合卡由 SpeechRuleEditScreen(bodyInCard/cardTrailer) 内部处理，
            // 基本信息卡在此关闭；朗读全部态：平铺+基本信息卡原样（同插件页）
            val isTagTarget = (systemTts.config as? TtsConfigurationDTO)
                ?.speechRule?.target == SpeechTarget.TAG
            content()
            Content(systts = systemTts, onSysttsChange = onSystemTtsChange, showBasicInfo = isTagTarget.not(), showParams = false)
            SectionCard(
                title = "音频参数",
                icon = Icons.Default.Speed,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                ParamsEditScreen(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    systemTts = systemTts,
                    onSystemTtsChange = onSystemTtsChange
                )
            }
        }
    }

    @Composable
    private fun Content(
        modifier: Modifier = Modifier,
        systts: SystemTtsV2,
        onSysttsChange: (SystemTtsV2) -> Unit,
        vm: LocalTtsViewModel = viewModel(),
        showBasicInfo: Boolean = true,
        showParams: Boolean = true,
    ) {
        val systts by rememberUpdatedState(newValue = systts)

        val config = systts.config as TtsConfigurationDTO
        val source = config.source as LocalTtsSource

        SaveActionHandler {

            true
        }

        var showAuditionDialog by remember { mutableStateOf(false) }
        var auditionSystts by remember { mutableStateOf<SystemTtsV2?>(null) }
        if (showAuditionDialog && auditionSystts != null)
            AuditionDialog(systts = auditionSystts!!) {
                showAuditionDialog = false
            }

        Column(modifier) {
            // 基本信息：保留分区壳但不出标题（同 PluginTtsUI 用户定稿）
            if (showBasicInfo)
                SectionCard(
                    title = "基本信息",
                    icon = Icons.Default.Info,
                    showHeader = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    BasicInfoEditScreen(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        systemTts = systts,
                        onSystemTtsChange = onSysttsChange,
                    )
                }

            SectionCard(
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
                    // 使用 rememberUpdatedState 确保获取最新的 systts
                    val currentSystts by rememberUpdatedState(systts)
                    AuditionTextField(modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp), onAudition = {
                        // 强制创建新的对象副本，确保 Compose 检测到变化并重新触发试听
                        auditionSystts = currentSystts.copy()
                        showAuditionDialog = true
                    })

                    val context = LocalContext.current
                    var isLoading by remember { mutableStateOf(false) }
                LoadingContent(isLoading = isLoading) {
                    Column {
                        LaunchedEffect(source.engine) {
                            isLoading = true

                            runCatching {
                                withIO { vm.setEngine(source.engine) }
                                vm.updateLocales()
                                vm.updateVoices(source.locale)
                            }.onFailure {
                                context.displayErrorDialog(it, source.engine)
                            }

                            isLoading = false
                        }

                        AppSpinner(
                            modifier = Modifier.padding(vertical = 2.dp),
                            labelText = "🔌 " + stringResource(id = R.string.label_tts_engine),
                            value = source.engine,
                            values = vm.engines.map { it.name },
                            entries = vm.engines.map { it.label },
                            icons = vm.engines.map { PackageDrawable(it.name, it.icon) },
                            onSelectedChange = { k, name ->
                                val lastName = vm.engines.find { it.name == source.engine }?.label ?: ""
                                onSysttsChange(
                                    systts.copySource(source.copy(engine = k as String)).run {
                                        if (systts.displayName.isBlank() || lastName == systts.displayName)
                                            copy(displayName = name)
                                        else this
                                    }
                                )
                            }
                        )

                        AppSpinner(
                            modifier = Modifier.padding(vertical = 2.dp),
                            labelText = "🌐 " + stringResource(id = R.string.label_language),
                            value = source.locale,
                            values = vm.locales.map { it.toLanguageTag() },
                            entries = vm.locales.map { it.country.toCountryFlagEmoji() + " " + it.displayName },
                            onSelectedChange = { loc, _ ->
                                onSysttsChange(systts.copySource(source.copy(locale = loc as String)))

                                vm.updateVoices(loc)
                            }
                        )

                        AppSpinner(
                            modifier = Modifier.padding(vertical = 2.dp),
                            labelText = "🔊 " + stringResource(id = R.string.label_voice),
                            value = source.voice,
                            values = vm.voices.map { it.name },
                            entries = vm.voices.map {
                                val featureStr =
                                    if (it.features == null || it.features.isEmpty()) "" else it.features.toString()
                                "${it.name} $featureStr"
                            },

                            onSelectedChange = { k, _ ->
                                onSysttsChange(systts.copySource(source.copy(voice = k as String)))
                            }
                        )
                    }
                }
                }
            }

            if (showParams)
                SectionCard(
                    title = "音频参数",
                    icon = Icons.Default.Speed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    ParamsEditScreen(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        systemTts = systts,
                        onSystemTtsChange = onSysttsChange
                    )
                }
        }
    }
}