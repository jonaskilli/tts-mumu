package com.github.jing332.tts.selftest

import android.content.Context
import android.os.SystemClock
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.tts.speech.local.AndroidTtsEngine
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.InputStream

/**
 * 系统 TTS 调用自检：测量器。
 *
 * 2026-11-06 新增。任务书：技术文档\任务书-系统TTS调用自检-20261106.md
 *
 * 走真实的系统 TTS 接口（[AndroidTtsEngine.getStream]：synthesizeToFile +
 * UtteranceProgressListener.onStart 首块即返回），量三项指标：
 * 1. 首音延迟（提交 → 首块 PCM 到达）
 * 2. 静音间隙（段内静音片段）
 * 3. 白噪音/底噪（静音段平均能量）
 *
 * ⚠️ 诚实口径：本自检是 **UTTS 进程内 bind 已绑的系统 TTS 引擎**（若引擎即 UTTS 自身，
 * 就是「UTTS 客户端 → UTTS 服务端」的同进程调用），与 z 阅读跨进程调用的差别是
 * 「不含 Binder 跨进程税」。合成耗时是主要项，故结果仍可用于指导决策，
 * 但**不得**宣称为宿主实测。
 *
 * ⚠️ 副作用：SystemTtsService.onSynthesizeText 有「新请求冒头即强制注销旧任务」的破窗
 * 逻辑，因此**朗读进行中调用自检会打断用户正在朗读**。调用方必须在进入自检前确认
 * 无朗读占用（UI 侧已做；本类不做全局占用检测，因为它无法可靠知道宿主状态）。
 */
class SystemTtsSelfTest(private val context: Context) {

    data class Sample(
        /** 用的哪个引擎包名。 */
        val engineName: String,
        val text: String,
        /** 首音延迟：提交到首块音频到达（ms）。-1 = 未测到。 */
        val firstAudioMs: Long,
        /** 从提交到整段读完（ms）。 */
        val totalMs: Long,
        /** 拿到的 PCM 字节数与格式。 */
        val pcmBytes: Int,
        val sampleRate: Int,
        val channels: Int,
        /** 音频指标分析。 */
        val report: AudioHealthAnalyzer.Report?,
        /** 失败原因（null = 成功）。 */
        val error: String? = null,
    ) {
        val ok: Boolean get() = error == null
    }

    /**
     * 跑一次真实调用并测量。
     *
     * @param engineName 系统 TTS 引擎包名；空串 = 系统默认引擎
     * @param voiceName  系统 TTS 的 voice 名。UTTS 用它指定「测哪套配置」，
     *                   格式 `displayName_id`（见 SystemTtsService.onIsValidVoiceName）；
     *                   空串 = 该引擎的默认配置
     * @param timeoutMs  整段超时（含合成与读流），默认 60s（与合成链 requestTimeout 同量级）
     * @param coldCache  true 时在文本末尾追加随机标记，强制插件走新鲜合成
     *                   （绕开插件对相同文本的缓存，否则延迟指标会被缓存污染——11-06 实锤：
     *                   首次自检出 21ms/295ms，明显是命中缓存的数字）
     */
    suspend fun measure(
        engineName: String,
        text: String,
        voiceName: String = "",
        timeoutMs: Long = 60_000L,
        coldCache: Boolean = true,
    ): Sample = withContext(Dispatchers.IO) {
        val started = SystemClock.elapsedRealtime()
        val engine = AndroidTtsEngine(context)

        // 防缓存污染：追加随机数字，使文本对插件而言唯一。
        // 用数字而非字母，避免插件把标记念成奇怪音节；标记会被念出来，故只在测速时用。
        val probeText = if (coldCache) {
            val n = (1000..9999).random()
            "$text$n"
        } else text

        try {
            val initOk = withTimeout(timeoutMs) { engine.init(engineName) }
            if (!initOk) {
                return@withContext Sample(
                    engineName = engineName, text = probeText,
                    firstAudioMs = -1L, totalMs = 0L,
                    pcmBytes = 0, sampleRate = 0, channels = 0, report = null,
                    error = "引擎初始化失败（engineName=$engineName）",
                )
            }

            val submitAt = SystemClock.elapsedRealtime()
            val streamResult = withTimeout(timeoutMs) {
                engine.getStream(text = probeText, voice = voiceName, params = AudioParams())
            }

            // michaelbull-result：沿用本仓统一写法（onSuccess/onFailure），
            // 不依赖 getOrNull/value 等扩展（全仓无先例，导入面不确定）
            var stream: InputStream? = null
            var errMsg: String? = null
            streamResult.onSuccess { stream = it }
                .onFailure { errMsg = it.toString() }
            val ins = stream
            if (ins == null) {
                return@withContext Sample(
                    engineName = engineName, text = probeText,
                    firstAudioMs = -1L,
                    totalMs = SystemClock.elapsedRealtime() - started,
                    pcmBytes = 0, sampleRate = 0, channels = 0, report = null,
                    error = "合成失败：${errMsg ?: "未知错误"}",
                )
            }

            // 首块到达时间：getStream 已在 onStart 时返回，此刻即首块可用
            val firstAudioMs = SystemClock.elapsedRealtime() - submitAt

            var bytes = 0
            val chunks = ArrayList<ByteArray>()
            val buffer = ByteArray(4096)
            val readDeadline = submitAt + timeoutMs
            while (isActive && SystemClock.elapsedRealtime() < readDeadline) {
                val n = try {
                    ins.read(buffer)
                } catch (e: Exception) {
                    return@withContext Sample(
                        engineName = engineName, text = probeText,
                        firstAudioMs = firstAudioMs,
                        totalMs = SystemClock.elapsedRealtime() - submitAt,
                        pcmBytes = bytes, sampleRate = 0, channels = 0, report = null,
                        error = "读流出错：${e.message ?: e.javaClass.simpleName}",
                    )
                }
                if (n <= 0) break
                chunks.add(buffer.copyOf(n))
                bytes += n
            }

            val totalMs = SystemClock.elapsedRealtime() - submitAt
            val pcm = ByteArray(bytes).also { out ->
                var off = 0
                for (c in chunks) {
                    System.arraycopy(c, 0, out, off, c.size)
                    off += c.size
                }
            }

            // 系统 TTS 固定 PCM16。采样率未知时按 16k 估算（SystemTtsService 空文本默认 16000），
            // 单声道为 TTS 回调常态；分析器对这两项不敏感，间隙判定按帧长换算即可。
            val sampleRate = 16000
            val channels = 1
            val report = AudioHealthAnalyzer.analyze(pcm, sampleRate, channels)

            engine.release()
            Sample(
                engineName = engineName,
                text = probeText,
                firstAudioMs = firstAudioMs,
                totalMs = totalMs,
                pcmBytes = bytes,
                sampleRate = sampleRate,
                channels = channels,
                report = report,
            )
        } catch (e: Exception) {
            Sample(
                engineName = engineName, text = probeText,
                firstAudioMs = -1L,
                totalMs = SystemClock.elapsedRealtime() - started,
                pcmBytes = 0, sampleRate = 0, channels = 0, report = null,
                error = "${e.javaClass.simpleName}: ${e.message}",
            )
        } finally {
            runCatching { engine.release() }
        }
    }
}
