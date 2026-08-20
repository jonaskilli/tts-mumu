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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
    // 当前声音已分配的分类（回看时显示，重选分类即覆盖改派）
    assignedCategory: String? = null,
    // 进度序号，如 "12/50"
    progressText: String? = null,
    // 试听时从真实音频解析出采样率后回调，供批量保存缓存复用（避免保存时重复合成）
    onSampleRateResolved: ((voiceId: Any?, sampleRate: Int) -> Unit)? = null,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    var error by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
    // 重播计数：点「重播」时自增触发 LaunchedEffect 重启（失败时兼作重试）
    var retryKey by remember { mutableIntStateOf(0) }
    val audioPlayer = remember { AudioPlayer(context) }

    DisposableEffect(systts) {
        onDispose {
            audioPlayer.stop()
        }
    }

    LaunchedEffect(systts, retryKey) {
        error = ""
        info = ""
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
                    // 真实采样率回传：批量保存时缓存复用，无需再合成一次
                    if (rateAndMime.first > 0) {
                        onSampleRateResolved?.invoke(voiceId, rateAndMime.first)
                    }
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (assignedCategory.isNullOrBlank()) 6.dp else 2.dp)
                    ) {
                        Text(
                            systts.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        // 进度序号：批量试听分类时知道进行到第几个
                        if (!progressText.isNullOrBlank()) {
                            Text(
                                progressText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // 已分配的分类回显：回看上一个声音时知道是否已设置过
                    if (!assignedCategory.isNullOrBlank()) {
                        Text(
                            buildString {
                                append("分类：$assignedCategory")
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                // 仅失败时显示错误；正常试听不显示测试文本（文本本身仍用于合成）
                if (error.isNotEmpty()) {
                    SelectionContainer {
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
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

                // —— 分配分类：按性别年龄归类，保存时依据朗读规则生成标签 ——
                if (onCategoryAssigned != null && voiceId != null && error.isEmpty()) {
                    Text(
                        stringResource(id = R.string.assign_category_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        com.github.jing332.compose.widgets.VoiceCategories.ALL.forEach { category ->
                            FilterChip(
                                selected = category == assignedCategory,
                                onClick = { onCategoryAssigned.invoke(voiceId, category) },
                                label = { Text(category) }
                            )
                        }
                    }
                }

            }
        },
        buttons = {
            // 重播：没听清可再听一遍；失败时兼作重试（error 清空后分类按钮恢复显示）
            TextButton(onClick = { error = ""; info = ""; retryKey++ }) {
                Text("重播")
            }
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