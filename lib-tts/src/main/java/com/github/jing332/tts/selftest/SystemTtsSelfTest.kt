package com.github.jing332.tts.selftest

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.coroutines.resume

/**
 * 系统 TTS 调用自检：测量器（2026-11-06 新增，当日晚按参考版口径重写）。
 *
 * 走真实系统 TTS 接口（synthesizeToFile + UtteranceProgressListener.onStart），量：
 * 1. 引擎绑定耗时 / onStart 延迟 / 合成总耗时 / 产出音频时长 / RTF
 * 2. 音频格式——**采样率从 WAV 头解析**（原版硬编码 16000，实机实测 24000，
 *    导致静音间隙时长算错 50%——11-06 实机反馈后修正）
 * 3. 质量检测（峰值/底噪/削波/不连续点/白噪音）
 * 4. 内部静音间隙，附带用户当前段间间隔设置，便于区分「配置停顿」与「插件停顿」
 *
 * ⚠️ 诚实口径：进程内 bind，与 z 阅读跨进程调用差一层 Binder 税；
 * 可用于指导决策，但不得宣称为宿主实测。
 *
 * ⚠️ 副作用：SystemTtsService.onSynthesizeText 有「新请求冒头即强制注销旧任务」
 * 的破窗逻辑，朗读中调用自检会打断用户朗读。调用方须保证无朗读占用。
 */
class SystemTtsSelfTest(private val context: Context) {

    data class Sample(
        val engineName: String,
        val voiceName: String,
        val text: String,
        /** 引擎绑定耗时（ms）：new TextToSpeech → onInit 成功 */
        val bindMs: Long,
        /** onStart 回调延迟（ms）：提交 → onStart；-1 = 未上报 */
        val startCallbackMs: Long,
        /** 合成总耗时（ms）：提交 → onDone */
        val synthTotalMs: Long,
        /** 产出音频时长（ms，从 WAV 解析） */
        val audioDurationMs: Long,
        val wavFileBytes: Long,
        val report: AudioHealthAnalyzer.Report?,
        /** 用户当前设置的段间间隔（ms） */
        val segmentPauseMs: Int,
        val coldCache: Boolean,
        val error: String? = null,
    ) {
        val ok: Boolean get() = error == null

        /** 实时率 = 合成耗时 / 音频时长；<1 表示快于实时 */
        val rtf: Double
            get() = if (audioDurationMs > 0) synthTotalMs.toDouble() / audioDurationMs else -1.0
    }

    /**
     * @param engineName 引擎包名；空串 = 系统默认引擎
     * @param voiceName  UTTS 用 `displayName_id` 指定配置
     *                   （SystemTtsService.onIsValidVoiceName:375-378）
     * @param coldCache  追加随机数字强制新鲜合成，避免插件文本缓存让延迟虚高
     * @param segmentPauseMs 仅展示用，用于解释间隙来源
     */
    suspend fun measure(
        engineName: String,
        text: String,
        voiceName: String = "",
        timeoutMs: Long = 90_000L,
        coldCache: Boolean = true,
        segmentPauseMs: Int = 0,
    ): Sample = withContext(Dispatchers.IO) {
        val probeText = if (coldCache) "$text${(1000..9999).random()}" else text

        // 1) 构造并等初始化：TextToSpeech(context, OnInitListener, engineName)。
        // 用独立函数返回 Pair，避免 try/catch 里 return@withContext 导致整个表达式的
        // 类型被推断为 Nothing（11-06 CI 实锤：Boolean vs Nothing 冲突）。
        val bindStart = SystemClock.elapsedRealtime()
        val (tts, initOk) = bindEngine(engineName, timeoutMs)
        val bindMs = SystemClock.elapsedRealtime() - bindStart

        if (!initOk || tts == null) {
            runCatching { tts?.shutdown() }
            return@withContext Sample(
                engineName, voiceName, probeText, bindMs, -1, 0, 0, 0, null,
                segmentPauseMs, coldCache,
                error = "系统未能绑定到引擎 ${engineName.ifBlank { "(默认)" }}",
            )
        }
        // bindEngine 已保证 tts 非空（上面 !initOk || tts == null 的早退分支已 return）
        val ttsEngine: TextToSpeech = tts

        val cacheDir = File(context.externalCacheDir ?: context.cacheDir, "selftest")
        runCatching { cacheDir.mkdirs() }
        val wavFile = File(cacheDir, "selftest_${System.currentTimeMillis()}.wav")
        val utteranceId = "selftest_${System.currentTimeMillis()}"

        // UTTS 的配置通过 Voice 名选择（AndroidTtsEngine.setEnginePlayParams:99-102
        // 用 engine.voice = it，匹配 it.name == voice）。系统标准引擎忽略此项。
        if (voiceName.isNotBlank()) {
            runCatching {
                ttsEngine.voices?.firstOrNull { it.name == voiceName }?.let { v ->
                    ttsEngine.voice = v
                }
            }
        }

        val submitAt = SystemClock.elapsedRealtime()
        var startAt = -1L
        var errorMsg: String? = null

        val completed = try {
            withTimeout(timeoutMs) {
                suspendCancellableCoroutine<Boolean> { cont ->
                    ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(uId: String?) {
                            if (startAt < 0) startAt = SystemClock.elapsedRealtime()
                        }

                        override fun onDone(uId: String?) {
                            if (cont.isActive) cont.resume(true)
                        }

                        @Deprecated("required override")
                        override fun onError(uId: String?) {
                            errorMsg = "合成回调报错"
                            if (cont.isActive) cont.resume(false)
                        }

                        override fun onError(uId: String?, errorCode: Int) {
                            errorMsg = "合成回调报错，errorCode=$errorCode"
                            if (cont.isActive) cont.resume(false)
                        }
                    })

                    val bundle = Bundle().apply {
                        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f)
                    }
                    val ret = ttsEngine.synthesizeToFile(probeText, bundle, wavFile, utteranceId)
                    if (ret != TextToSpeech.SUCCESS) {
                        errorMsg = "synthesizeToFile 返回错误码 $ret"
                        if (cont.isActive) cont.resume(false)
                    }
                }
            }
        } catch (e: Exception) {
            errorMsg = "${e.javaClass.simpleName}: ${e.message}"
            false
        }

        val synthTotalMs = SystemClock.elapsedRealtime() - submitAt
        val startMs = if (startAt >= 0) startAt - submitAt else -1L
        runCatching { ttsEngine.shutdown() }

        if (!completed) {
            runCatching { wavFile.delete() }
            return@withContext Sample(
                engineName, voiceName, probeText, bindMs, startMs, synthTotalMs,
                0, 0, null, segmentPauseMs, coldCache,
                error = errorMsg ?: "合成未完成",
            )
        }

        val report = AudioHealthAnalyzer.analyzeWav(wavFile)
        val bytes = wavFile.length()
        val durationMs = report?.durationMs ?: 0L
        runCatching { wavFile.delete() }

        Sample(
            engineName = engineName,
            voiceName = voiceName,
            text = probeText,
            bindMs = bindMs.coerceAtLeast(0),
            startCallbackMs = startMs,
            synthTotalMs = synthTotalMs,
            audioDurationMs = durationMs,
            wavFileBytes = bytes,
            report = report,
            segmentPauseMs = segmentPauseMs,
            coldCache = coldCache,
        )
    }

    /**
     * 构造 TextToSpeech 并等待初始化回调。
     *
     * 独立函数且显式声明返回类型：调用处若把「try/catch + early return」写成表达式，
     * Kotlin 会把 catch 分支推断为 Nothing，导致整个表达式类型冲突
     * （11-06 CI 实锤：`Argument type mismatch: actual type is 'Boolean', but
     * 'Nothing' was expected`）。
     */
    private suspend fun bindEngine(
        engineName: String,
        timeoutMs: Long,
    ): Pair<TextToSpeech?, Boolean> {
        val holder = java.util.concurrent.atomic.AtomicReference<TextToSpeech?>(null)
        return try {
            val ok = withTimeout(timeoutMs) {
                suspendCancellableCoroutine<Boolean> { cont ->
                    val created = TextToSpeech(context, { status ->
                        if (cont.isActive) cont.resume(status == TextToSpeech.SUCCESS)
                    }, engineName)
                    holder.set(created)
                }
            }
            holder.get() to ok
        } catch (e: Exception) {
            runCatching { holder.get()?.shutdown() }
            null to false
        }
    }
}
