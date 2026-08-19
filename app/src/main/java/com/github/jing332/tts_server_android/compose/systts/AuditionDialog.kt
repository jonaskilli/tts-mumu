package com.github.jing332.tts_server_android.compose.systts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.drake.net.utils.withMain
import com.github.jing332.common.audio.AudioPlayer
import com.github.jing332.common.utils.messageChain
import com.github.jing332.common.utils.sizeToReadable
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.LoadingContent
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.TextToSpeechSource
import com.github.jing332.tts.CachedEngineManager
import com.github.jing332.tts.speech.EngineState
import com.github.jing332.tts.speech.TextToSpeechProvider
import com.github.jing332.tts.synthesizer.SystemParams
import com.github.jing332.tts.synthesizer.TtsConfiguration
import com.github.jing332.tts.synthesizer.TtsConfiguration.Companion.toVO
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.conf.AppConfig
import com.github.jing332.tts_server_android.conf.SysTtsConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.IOException
import splitties.init.appCtx


private val logger = KotlinLogging.logger("AuditionDialog")

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AuditionDialog(
    systts: SystemTtsV2,
    text: String = AppConfig.testSampleText.value,

    config: TtsConfiguration = (systts.config as TtsConfigurationDTO).toVO(),
    engine: TextToSpeechProvider<TextToSpeechSource>? = null,
    voiceId: Any? = null,
    autoDismiss: Boolean = true,
    hasPrev: Boolean = false,
    hasNext: Boolean = false,
    onCategoryAssigned: ((voiceId: Any, categoryName: String) -> Unit)? = null,
    onPrev: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    var error by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
    val audioPlayer = remember { AudioPlayer(context) }

    DisposableEffect(systts) {
        onDispose {
            audioPlayer.stop()
        }
    }

    LaunchedEffect(systts) {
        launch(Dispatchers.IO) {
            try {
                val e = engine ?: CachedEngineManager.getEngine(appCtx, config.source)
                ?: throw IllegalStateException("engine is null")

                if (e.state is EngineState.Uninitialized) e.onInit()
                if (e.isSyncPlay(config.source)) {
                    e.syncPlay(
                        SystemParams(
                            text = text,
                            speed = config.audioParams.speed,
                            volume = config.audioParams.volume,
                            pitch = config.audioParams.pitch,
                            requestTimeout = SysTtsConfig.requestTimeout.toLong()
                        ),
                        config.source
                    )
                } else {
                    val stream = e.getStream(
                        SystemParams(
                            text = text,
                            speed = config.audioParams.speed,
                            volume = config.audioParams.volume,
                            pitch = config.audioParams.pitch,
                            requestTimeout = SysTtsConfig.requestTimeout.toLong()
                        ),
                        config.source
                    )
                    val audio = stream.readBytes()
                    val rateAndMime =
                        com.github.jing332.common.audio.AudioDecoder.getSampleRateAndMime(audio)
                    withMain {
                        info = context.getString(
                            R.string.systts_test_success_info, audio.size.toLong().sizeToReadable(),
                            rateAndMime.first, rateAndMime.second
                        )
                    }

                    if (config.shouldDecode())
                        audioPlayer.play(audio)
                    else
                        audioPlayer.play(audio, config.audioFormat.sampleRate)
                }
                withContext(Dispatchers.Main) {
                    if (autoDismiss) onDismissRequest()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // 弹窗关闭/切换时协程被取消，属正常行为，忽略
            } catch (e: IOException) {
                error = e.cause.toString()
            } catch (e: Exception) {
                error = e.messageChain
                logger.warn { e.stackTraceToString() }
            }
        }
    }

    AppDialog(onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.audition)) },
        content = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                // 当前试听的声音名：分类时明确知道在给哪个发音人分配
                if (systts.displayName.isNotBlank()) {
                    Text(
                        systts.displayName,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                SelectionContainer {
                    Text(
                        error.ifEmpty { text },
                        color = if (error.isEmpty()) Color.Unspecified else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (error.isEmpty())
                    LoadingContent(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth(),
                        isLoading = info.isEmpty()
                    ) {
                        SelectionContainer {
                            Text(info, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                if (onCategoryAssigned != null && voiceId != null && error.isEmpty()) {
                    val categoryRows = remember {
                        listOf(
                            listOf("女童", "少女"),
                            listOf("女青年", "女中年", "女老年"),
                            listOf("男童", "少年"),
                            listOf("男青年", "男中年", "男老年")
                        )
                    }
                    Text(
                        stringResource(id = R.string.assign_category_hint),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categoryRows.forEach { rowCategories ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                rowCategories.forEach { category ->
                                    AssistChip(
                                        onClick = { onCategoryAssigned.invoke(voiceId, category) },
                                        label = { Text(category) }
                                    )
                                }
                            }
                        }
                    }
                }

            }
        },
        buttons = {
            if (onPrev != null) {
                TextButton(onClick = onPrev, enabled = hasPrev) {
                    Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "上一个")
                    Text("上一个")
                }
            }
            if (onNext != null) {
                TextButton(onClick = onNext, enabled = hasNext) {
                    Text("下一个")
                    Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "下一个")
                }
            }
            TextButton(onClick = onDismissRequest) { Text(stringResource(id = R.string.cancel)) }
        }
    )

}