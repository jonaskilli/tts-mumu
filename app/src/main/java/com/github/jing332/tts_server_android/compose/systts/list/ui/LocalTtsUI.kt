package com.github.jing332.tts_server_android.compose.systts.list.ui


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
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
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.AuditionTextField
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.BasicInfoEditScreen
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.SaveActionHandler
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

        Column(modifier) {
            val rateStr = stringResource(
                id = R.string.label_speech_rate,
                if (params.speed == 0f) stringResource(id = R.string.follow) else params.speed.toString()
            )
            LabelSlider(text = rateStr, value = params.speed, onValueChange = {
                onSystemTtsChange(
                    systemTts.copy(
                        config = config.copy(audioParams = params.copy(speed = it.toScale(2)))
                    )
                )
            }, valueRange = 0f..3f)

            val pitchStr = stringResource(
                id = R.string.label_speech_pitch,
                if (params.pitch == 0f) stringResource(id = R.string.follow) else params.pitch.toString()
            )
            LabelSlider(value = params.pitch, onValueChange = {
                onSystemTtsChange(
                    systemTts.copy(
                        config = config.copy(audioParams = params.copy(pitch = it.toScale(2)))
                    )
                )
            }, valueRange = 0f..3f, text = pitchStr)

            val volumeStr = stringResource(
                id = R.string.label_speech_volume,
                if (params.volume == 0f) stringResource(id = R.string.follow) else params.volume.toString()
            )
            LabelSlider(value = params.volume, onValueChange = {
                onSystemTtsChange(
                    systemTts.copy(
                        config = config.copy(audioParams = params.copy(volume = it.toScale(2)))
                    )
                )
            }, valueRange = 0f..3f, text = volumeStr)

            Row {
                var sampleRateStr by remember { mutableStateOf(config.audioFormat.sampleRate.toString()) }
                DenseOutlinedField(
                    label = { Text(stringResource(R.string.systts_sample_rate)) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp),
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

                Row(
                    Modifier
                        .minimumInteractiveComponentSize()
                        .padding(8.dp)
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
            content()
            Content(systts = systemTts, onSysttsChange = onSystemTtsChange)
        }
    }

    @Composable
    private fun Content(
        modifier: Modifier = Modifier,
        systts: SystemTtsV2,
        onSysttsChange: (SystemTtsV2) -> Unit,
        vm: LocalTtsViewModel = viewModel(),
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
            Column(Modifier.padding(horizontal = 8.dp)) {
                BasicInfoEditScreen(
                    modifier = Modifier.fillMaxWidth(),
                    systemTts = systts,
                    onSystemTtsChange = onSysttsChange,
                )
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
                            labelText = stringResource(id = R.string.label_tts_engine),
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
                            labelText = stringResource(id = R.string.label_language),
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
                            labelText = stringResource(id = R.string.label_voice),
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
            ParamsEditScreen(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                systemTts = systts,
                onSystemTtsChange = onSysttsChange
            )
        }
    }
}