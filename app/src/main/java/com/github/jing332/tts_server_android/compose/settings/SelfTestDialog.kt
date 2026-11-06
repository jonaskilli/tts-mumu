package com.github.jing332.tts_server_android.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jing332.database.dbm
import com.github.jing332.tts.selftest.SystemTtsSelfTest
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.conf.SysTtsConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** 一条待测配置：显示名 + 传给引擎的 voice 名。 */
private data class TestTarget(val label: String, val voiceName: String)

/**
 * 系统侧调用自检弹窗（2026-11-06 新增，当日两轮修正）。
 * 任务书：技术文档\任务书-系统TTS调用自检-20261106.md
 *
 * 流程：列出已启用配置 → 用户选一套 → 用该配置跑真实合成 → 显示完整指标。
 * 修正点（实机反馈驱动）：
 * 1. 原版盲测系统默认引擎，界面不显示测的谁 → 改为先选配置
 * 2. 原版硬编码 16000Hz，实机是 24000Hz，间隙时长算错 50% → 改为解析 WAV 头
 * 3. 用户自设的段间间隔会让「间隙」虚高 → 结果里标注当前设置
 * 4. 插件文本缓存让延迟虚低 → 强制新鲜合成并标注
 *
 * 纯测量，零行为变更：不改配置、不动合成与播放链。
 */
@Composable
internal fun SelfTestDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var targets by remember { mutableStateOf<List<TestTarget>>(emptyList()) }
    var selected by remember { mutableStateOf<TestTarget?>(null) }
    var running by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val tester = remember { SystemTtsSelfTest(context) }

    LaunchedEffect(Unit) {
        val list = withContext(Dispatchers.IO) {
            runCatching {
                dbm.systemTtsV2.getAllGroupWithTts()
                    .flatMap { group -> group.list }
                    .filter { it.isEnabled }
                    .map { cfg ->
                        TestTarget(
                            label = cfg.displayName.ifBlank { "#${cfg.id}" },
                            voiceName = "${cfg.displayName}_${cfg.id}",
                        )
                    }
            }.getOrDefault(emptyList())
        }
        targets = list
    }

    suspend fun runOnce(target: TestTarget) {
        running = true
        resultText = ""
        errorText = null
        val pauseMs = runCatching { SysTtsConfig.segmentPauseMs }.getOrDefault(0)
        val (text, err) = withContext(Dispatchers.IO) {
            buildResultText(tester, context, target, pauseMs)
        }
        running = false
        resultText = text
        errorText = err
    }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.selftest_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when {
                    targets.isEmpty() -> Text(stringResource(R.string.selftest_need_config))

                    running -> {
                        Text(selected?.let { "正在测：${it.label}" } ?: "")
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.selftest_running))
                    }

                    resultText.isNotBlank() || errorText != null -> {
                        errorText?.let {
                            Text(
                                "${stringResource(R.string.selftest_failed)}：$it",
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        if (resultText.isNotBlank()) Text(resultText)
                        Spacer(Modifier.height(10.dp))
                        TextButton(onClick = {
                            selected = null
                            resultText = ""
                            errorText = null
                        }) { Text(stringResource(R.string.selftest_pick_again)) }
                    }

                    else -> {
                        Text(stringResource(R.string.selftest_pick_config))
                        Spacer(Modifier.height(8.dp))
                        targets.forEach { t ->
                            Text(
                                "· ${t.label}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selected = t
                                        scope.launch { runOnce(t) }
                                    }
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(6.dp))
                if (resultText.isNotBlank()) {
                    Text(
                        stringResource(R.string.selftest_cold_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    stringResource(R.string.selftest_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.confirm)) }
        },
    )
}

/**
 * 跑自检并拼成可读文本。返回 (正文, 错误信息)。
 */
private suspend fun buildResultText(
    tester: SystemTtsSelfTest,
    context: android.content.Context,
    target: TestTarget,
    segmentPauseMs: Int,
): Pair<String, String?> {
    val sample = try {
        tester.measure(
            engineName = "",
            text = SELF_TEST_TEXT,
            voiceName = target.voiceName,
            segmentPauseMs = segmentPauseMs,
        )
    } catch (e: Exception) {
        return "" to "${e.javaClass.simpleName}: ${e.message}"
    }

    if (!sample.ok) return "" to sample.error

    val r = sample.report
    val sb = StringBuilder()

    sb.appendLine("— 调用链路 —")
    sb.appendLine("引擎绑定耗时      : ${sample.bindMs} ms")
    sb.appendLine(
        "系统回调 onStart  : " +
            if (sample.startCallbackMs >= 0) "${sample.startCallbackMs} ms" else "未上报"
    )
    sb.appendLine("合成总耗时        : ${sample.synthTotalMs} ms")
    sb.appendLine("产出音频时长      : ${sample.audioDurationMs} ms")
    sb.appendLine(
        "实时率 RTF        : " +
            if (sample.rtf >= 0) String.format(Locale.US, "%.2f", sample.rtf) + "  (<1 表示快于实时)"
            else "n/a"
    )

    sb.appendLine()
    sb.appendLine("— 音频格式 —")
    if (r != null) {
        sb.appendLine("采样率            : ${r.sampleRate} Hz / ${r.channels} ch")
    } else {
        sb.appendLine("采样率            : 解析失败（非 WAV 头）")
    }
    sb.appendLine("文件大小          : ${sample.wavFileBytes} B")

    if (r != null) {
        sb.appendLine()
        sb.appendLine("— 质量检测 —")
        sb.appendLine(
            "峰值电平          : " + String.format(Locale.US, "%.3f", r.peak) +
                "  " + if (r.silent) "静音！" else "正常"
        )
        sb.appendLine("底噪 RMS          : " + String.format(Locale.US, "%.5f", r.noiseFloor))
        sb.appendLine(
            "不连续跳变        : ${r.discontinuities} 次" +
                "（比值 " + String.format(Locale.US, "%.3f", r.discontinuityRatio) + "）"
        )
        sb.appendLine("削波比例          : " + String.format(Locale.US, "%.4f", r.clippedRatio))
        sb.appendLine("白噪音            : " + if (r.whiteNoiseSuspected) "疑似" else "未检出")

        sb.appendLine()
        sb.appendLine("— 静音间隙 —")
        sb.appendLine("前导静音          : ${r.leadingSilenceMs} ms")
        sb.appendLine("尾部静音          : ${r.trailingSilenceMs} ms")
        sb.appendLine("内部间隙          : 最长 ${r.longestInnerGapMs} ms / 合计 ${r.totalInnerSilenceMs} ms")
        sb.appendLine(
            "当前段间间隔设置  : ${segmentPauseMs} ms" +
                if (segmentPauseMs > 0) "（内部间隙的一部分可能来自此项，不是插件停顿）" else ""
        )
    }
    return sb.toString().trimEnd() to null
}

private const val SELF_TEST_TEXT = "这是一次系统侧调用自检，用来测量首包延迟、静音间隙和底噪。"
