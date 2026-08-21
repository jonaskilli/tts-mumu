package com.github.jing332.tts_server_android.compose.systts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drake.net.utils.withMain
import com.github.jing332.common.audio.AudioPlayer
import com.github.jing332.common.utils.messageChain
import com.github.jing332.common.utils.sizeToReadable
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.LoadingContent
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.database.entities.systts.source.TextToSpeechSource
import com.github.jing332.tts.CachedEngineManager
import com.github.jing332.tts.loudness.SpeakerLoudnessManager
import com.github.jing332.tts.stackedAudioParamsFor
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

    // 与实际朗读同源：五层叠加(插件×配置×子分组×分组×全局)后的参数，
    // 试听听到的即为真实播放效果，全局/分组/插件级修改即时体现
    config: TtsConfiguration = (systts.config as TtsConfigurationDTO).toVO().copy(
        audioParams = stackedAudioParamsFor(
            systts,
            AudioParams(
                speed = SysTtsConfig.audioParamsSpeed,
                volume = SysTtsConfig.audioParamsVolume,
                pitch = SysTtsConfig.audioParamsPitch
            )
        )
    ),
    engine: TextToSpeechProvider<TextToSpeechSource>? = null,
    voiceId: Any? = null,
    autoDismiss: Boolean = true,
    hasPrev: Boolean = false,
    hasNext: Boolean = false,
    onCategoryAssigned: ((voiceId: Any, categoryName: String?) -> Unit)? = null,
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
    val audioPlayer = remember { AudioPlayer(context) }

    DisposableEffect(systts) {
        onDispose {
            audioPlayer.stop()
        }
    }

    // 弹窗出现即后台预热引擎，缩短首次播放出声延迟（静默，不输出日志）
    LaunchedEffect(systts) {
        launch(Dispatchers.IO) {
            runCatching {
                val e = engine ?: CachedEngineManager.getEngine(appCtx, config.source) ?: return@runCatching
                if (e.state is EngineState.Uninitialized) e.onInit()
            }
        }
    }

    LaunchedEffect(systts) {
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
                        // 与日志一致的最终倍率展示(仅≠1的项)：试听时明确知道当前生效的叠加参数
                        val p = config.audioParams
                        val paramsInfo = buildList {
                            if (kotlin.math.abs(p.speed - 1f) > 0.005f) add("语速%.2fx".format(p.speed))
                            if (kotlin.math.abs(p.volume - 1f) > 0.005f) add("音量%.2fx".format(p.volume))
                            if (kotlin.math.abs(p.pitch - 1f) > 0.005f) add("音调%.2fx".format(p.pitch))
                        }.joinToString(" ")
                        info = context.getString(
                            R.string.systts_test_success_info, audio.size.toLong().sizeToReadable(),
                            rateAndMime.first, rateAndMime.second
                        ) + if (paramsInfo.isNotEmpty()) "\n$paramsInfo" else ""
                    }

                    // 与朗读路径一致的本地参数应用：插件表标记「插件自行处理」的项
                    // 服务端已生效、本地不再叠加，其余项在播放器本地应用；
                    // 音量并入响度均衡增益(朗读路径恒开响度均衡)
                    val pluginRecord = (config.source as? PluginTtsSource)?.let {
                        dbm.pluginDao.getByPluginId(it.pluginId)
                    }
                    val ap = config.audioParams
                    val effSpeed = if (pluginRecord?.pluginHandlesSpeed == true || ap.speed <= 0f) 1f else ap.speed
                    val effVolume = if (pluginRecord?.pluginHandlesVolume == true || ap.volume <= 0f) 1f else ap.volume
                    val effPitch = if (pluginRecord?.pluginHandlesPitch == true || ap.pitch <= 0f) 1f else ap.pitch
                    val loudnessGain = SpeakerLoudnessManager.infoFor(config).gain
                    val localVolume = (effVolume * loudnessGain).coerceIn(0f, 1f)

                    if (config.shouldDecode())
                        audioPlayer.play(audio, effSpeed, localVolume, effPitch)
                    else
                        audioPlayer.play(audio, config.audioFormat.sampleRate, effSpeed, localVolume, effPitch)
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

                // —— 分配分类：女性列 / 男性列 / 主角特殊旁白列 三列竖向排开，点击已选中的标签即取消（传 null） ——
                if (onCategoryAssigned != null && voiceId != null && error.isEmpty()) {
                    Text(
                        stringResource(id = R.string.assign_category_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    // 三列布局：女性组、男性组、主角特殊旁白组，每列内部竖向堆叠标签，互不混排
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        com.github.jing332.compose.widgets.VoiceCategories.COLUMNS.forEach { column ->
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                column.forEach { category ->
                                    FilterChip(
                                        selected = category == assignedCategory,
                                        // 再次点击已选中的分类 = 取消分配（传 null）
                                        onClick = {
                                            onCategoryAssigned.invoke(
                                                voiceId,
                                                if (category == assignedCategory) null else category
                                            )
                                        },
                                        label = { Text(category, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

            }
        },
        buttons = {
            if (onPrev != null || onNext != null) {
                // 仅剩切换按钮：居中排布，删除重播后不再挤在右下角
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                }
            }
        }
    )

}