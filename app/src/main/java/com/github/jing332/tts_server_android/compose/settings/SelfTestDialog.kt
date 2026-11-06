package com.github.jing332.tts_server_android.compose.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import com.github.jing332.tts.selftest.SystemTtsSelfTest
import com.github.jing332.tts_server_android.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * 系统侧调用自检弹窗（2026-11-06 新增）。
 * 任务书：技术文档\任务书-系统TTS调用自检-20261106.md
 *
 * 纯测量，零行为变更：不改配置、不动合成与播放链。
 */
@Composable
internal fun SelfTestDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(true) }
    var resultText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val tester = remember { SystemTtsSelfTest(context) }

    suspend fun runOnce() {
        running = true
        resultText = ""
        errorText = null
        val (text, err) = withContext(Dispatchers.IO) { buildResultText(tester, context) }
        running = false
        resultText = text
        errorText = err
    }

    LaunchedEffect(Unit) { runOnce() }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.selftest_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (running) {
                    Text(stringResource(R.string.selftest_running))
                } else {
                    errorText?.let {
                        Text(
                            "${stringResource(R.string.selftest_failed)}：$it",
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    if (resultText.isNotBlank()) Text(resultText)
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.selftest_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            if (running) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            } else {
                TextButton(onClick = { scope.launch { runOnce() } }) {
                    Text(stringResource(R.string.selftest_retry))
                }
            }
        },
        dismissButton = {
            if (!running) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.confirm)) }
            }
        },
    )
}

/**
 * 跑自检并拼成可读文本。返回 (正文, 错误信息)。
 * 在 IO 线程调用；只用 context.getString，不涉及 UI 操作。
 */
private suspend fun buildResultText(
    tester: SystemTtsSelfTest,
    context: android.content.Context,
): Pair<String, String?> {
    val sample = try {
        // 空引擎名 = 系统当前默认引擎（正是 z 阅读调用时用到的那一个）
        tester.measure(engineName = "", text = SELF_TEST_TEXT)
    } catch (e: Exception) {
        return "" to "${e.javaClass.simpleName}: ${e.message}"
    }

    if (!sample.ok) return "" to sample.error

    val r = sample.report
    val sb = StringBuilder()
    sb.appendLine("${context.getString(R.string.selftest_first_audio)}：${sample.firstAudioMs} ms")
    sb.appendLine("${context.getString(R.string.selftest_total)}：${sample.totalMs} ms")
    sb.appendLine(
        "${context.getString(R.string.selftest_audio_size)}：" +
            String.format(Locale.US, "%.1f KB", sample.pcmBytes / 1024f)
    )
    if (r != null) {
        val gap = if (r.gapCount == 0) {
            context.getString(R.string.selftest_gap_none)
        } else {
            val longest = r.longestGap?.let { "，最长 ${it.durationMs} ms" } ?: ""
            "${r.gapCount} 处，共 ${r.gapTotalMs} ms$longest"
        }
        sb.appendLine("${context.getString(R.string.selftest_gap)}：$gap")

        val noise = if (r.noiseFloorSuspicious) {
            context.getString(R.string.selftest_noise_suspicious)
        } else {
            context.getString(R.string.selftest_noise_normal)
        }
        sb.appendLine(
            "${context.getString(R.string.selftest_noise)}：$noise" +
                String.format(
                    Locale.US, "（阈值 %.0f dBFS，实测 %.1f dBFS）",
                    r.options.noiseFloorWarnDb, r.gapRmsDb
                )
        )
    }
    return sb.toString().trimEnd() to null
}

private const val SELF_TEST_TEXT = "系统自检，用于测量合成速度与音频质量。"
