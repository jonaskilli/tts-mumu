package com.github.jing332.tts.selftest

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * 系统 TTS 调用自检：PCM/WAV 音频指标分析 + WAV 头解析。
 *
 * 2026-11-06 新增；11-06 晚按参考版《PcmReport》口径重写（原版硬编码 16000Hz 导致
 * 间隙时长算错 50%——实机实测本机是 24000Hz）。
 *
 * 口径：只做「测量 + 显式判据」，不给黑盒结论；判据随结果输出，用户可自行复核。
 *
 * 算法对齐参考版（`临时文件\参考版TTS-抽读源码-服务与试听UI\compose\systts\selftest\PcmReport.java`）：
 * - 窗口 RMS：每秒 50 个窗口（sampleRate/50 样本/窗），而非逐样本判定
 * - 静音阈值 SILENCE_RMS = 1e-4
 * - 间隙：窗口 RMS ≤ 阈值且持续 ≥120ms；区分前导/尾部/内部间隙
 * - 白噪音判据：窗口 RMS 最小值（>5e-5 才算有效）过低 = 底噪异常；
 *   高频占比 + 样本跳变（>16384）密集 = 不连续点（断音/拼接痕迹）
 */
object AudioHealthAnalyzer {

    /** 静音判定：窗口 RMS 低于此值视为静音（对齐参考版 SILENCE_RMS）。 */
    private const val SILENCE_RMS = 1e-4f

    /** 短于此时长的静音不计为「间隙」（对齐参考版 120ms）。 */
    private const val MIN_GAP_MS = 120L

    /** 样本跳变超过此绝对值记为一次不连续（对齐参考版 16384）。 */
    private const val DISCONTINUITY_DELTA = 16384

    /** 削波判定：样本绝对值 ≥ 此值（对齐参考版 32700）。 */
    private const val CLIP_THRESHOLD = 32700

    data class Report(
        val sampleRate: Int,
        val channels: Int,
        /** 音频总长（ms） */
        val durationMs: Long,
        /** 最长内部静音间隙（ms） */
        val longestInnerGapMs: Long,
        /** 内部静音合计（ms） */
        val totalInnerSilenceMs: Long,
        /** 前导静音（ms，音频开头到第一段有效声音） */
        val leadingSilenceMs: Long,
        /** 尾部静音（ms） */
        val trailingSilenceMs: Long,
        /** 底噪（窗口 RMS 最小值，0~1 线性） */
        val noiseFloor: Float,
        /** 峰值电平（0~1 线性） */
        val peak: Float,
        /** 削波样本占比 */
        val clippedRatio: Float,
        /** 平均跳变 / 峰值 的比值（越大越可能有断音/拼接） */
        val discontinuityRatio: Float,
        /** 不连续跳变次数 */
        val discontinuities: Int,
        /** 白噪音疑似（底噪过低或跳变异常） */
        val whiteNoiseSuspected: Boolean,
    ) {
        val silent: Boolean get() = peak <= 0f || durationMs <= 0
        /** 实时率：合成耗时 / 音频时长，<1 表示快于实时 */
        fun rtf(synthCostMs: Long): Double =
            if (durationMs > 0) synthCostMs.toDouble() / durationMs.toDouble() else -1.0
    }

    /**
     * 从 WAV 文件解析并分析。解析失败返回 null（例如系统写出的是裸 PCM 无头）。
     */
    fun analyzeWav(file: File): Report? {
        if (!file.exists() || file.length() <= 44) return null
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
        return analyzeWavBytes(bytes)
    }

    fun analyzeWavBytes(bytes: ByteArray): Report? {
        if (bytes.size < 44) return null
        // RIFF/WAVE 校验
        if (!(bytes.copyOfRange(0, 4).decodeToString() == "RIFF" &&
                    bytes.copyOfRange(8, 12).decodeToString() == "WAVE")
        ) return null

        var offset = 12
        var sampleRate = 0
        var channels = 0
        var bitsPerSample = 16
        var dataOffset = -1
        var dataLen = -1

        while (offset + 8 <= bytes.size) {
            val chunkId = bytes.copyOfRange(offset, offset + 4).decodeToString()
            val chunkSize = le32(bytes, offset + 4)
            if (chunkSize < 0) break
            val next = offset + 8 + chunkSize
            if (next > bytes.size) break
            when (chunkId) {
                "fmt " -> {
                    channels = le16(bytes, offset + 10)
                    sampleRate = le32(bytes, offset + 12)
                    bitsPerSample = le16(bytes, offset + 22)
                }
                "data" -> {
                    dataOffset = offset + 8
                    dataLen = chunkSize
                }
            }
            offset = next + (chunkSize and 1)
        }

        if (dataOffset < 0 || sampleRate <= 0 || channels <= 0 || dataLen <= 0) return null
        // 只支持 16bit PCM（系统 TTS 固定输出此格式）
        if (bitsPerSample != 16) return null

        val frameCount = dataLen / (channels * 2)
        if (frameCount <= 0) return null

        // 按声道求平均（对齐参考版：多声道取算术平均再分析）
        val samples = ShortArray(frameCount)
        for (f in 0 until frameCount) {
            var acc = 0
            for (c in 0 until channels) {
                val i = dataOffset + ((f * channels) + c) * 2
                if (i + 1 >= bytes.size) break
                acc += ((bytes[i + 1].toInt() shl 8) or (bytes[i].toInt() and 0xFF))
            }
            samples[f] = (acc / channels).toShort()
        }

        return analyzeSamples(samples, sampleRate, channels)
    }

    /** 直接分析 PCM16 样本（无 WAV 头时由调用方给出采样率/声道）。 */
    fun analyzeSamples(samples: ShortArray, sampleRate: Int, channels: Int): Report {
        val n = samples.size
        if (n <= 0 || sampleRate <= 0) {
            return Report(sampleRate, channels, 0, 0, 0, 0, 0, 0f, 0f, 0f, 0f, 0, false)
        }

        // 峰值、削波、跳变统计
        var peakAbs = 0
        var clipped = 0
        var jumpSum = 0.0
        var discontinuities = 0
        for (i in 0 until n) {
            val v = samples[i].toInt()
            val a = kotlin.math.abs(v)
            if (a > peakAbs) peakAbs = a
            if (a >= CLIP_THRESHOLD) clipped++
            if (i > 0) {
                val d = kotlin.math.abs(v - samples[i - 1].toInt())
                jumpSum += d
                if (d > DISCONTINUITY_DELTA) discontinuities++
            }
        }
        val peak = peakAbs / 32768f

        // 窗口 RMS：每秒 50 窗
        val winSize = (sampleRate / 50).coerceAtLeast(1)
        val winCount = n / winSize
        if (winCount <= 0) {
            return Report(sampleRate, channels, (n * 1000L) / sampleRate,
                0, 0, 0, 0, 0f, peak, 0f, 0f, discontinuities, false)
        }
        val winRms = FloatArray(winCount)
        for (w in 0 until winCount) {
            val base = w * winSize
            var sum = 0.0
            for (k in 0 until winSize) {
                val v = samples[base + k].toDouble()
                sum += v * v
            }
            winRms[w] = (Math.sqrt(sum / winSize) / 32768.0).toFloat()
        }

        val msPerWin = (winSize * 1000L) / sampleRate

        // 有声音的窗口范围
        var firstVoiced = -1
        var lastVoiced = -1
        for (w in 0 until winCount) {
            if (winRms[w] > SILENCE_RMS) {
                if (firstVoiced < 0) firstVoiced = w
                lastVoiced = w
            }
        }

        var totalInnerSilence = 0L
        var longestInnerGap = 0L
        var leadingSilence = 0L
        var trailingSilence = 0L

        if (firstVoiced >= 0) {
            leadingSilence = firstVoiced * msPerWin
            if (lastVoiced < winCount - 1) {
                trailingSilence = ((winCount - 1) - lastVoiced) * msPerWin
            }
            // 内部间隙
            var run = 0
            for (w in firstVoiced..lastVoiced) {
                if (winRms[w] <= SILENCE_RMS) {
                    run++
                } else {
                    val gapMs = run * msPerWin
                    if (gapMs >= MIN_GAP_MS) {
                        totalInnerSilence += gapMs
                        if (gapMs > longestInnerGap) longestInnerGap = gapMs
                    }
                    run = 0
                }
            }
        }

        // 底噪 = 有效窗口里的最小 RMS（须 >5e-5 才采信，对齐参考版）
        var noiseFloor = Float.MAX_VALUE
        if (firstVoiced >= 0) {
            for (w in firstVoiced..lastVoiced) {
                val r = winRms[w]
                if (r > 5.0e-5f && r < noiseFloor) noiseFloor = r
            }
        }
        if (noiseFloor == Float.MAX_VALUE) noiseFloor = 0f

        val jumpAvg = if (n > 1) jumpSum / (n - 1) else 0.0
        val discontinuityRatio = if (peakAbs > 0) (jumpAvg / peakAbs).toFloat() else 0f

        // 白噪音疑似：底噪异常低（<1e-4 且非全静）或跳变过于密集
        val whiteNoise = (noiseFloor in 1.0e-9f..1.0e-4f) || (discontinuities > n / 200)

        val durationMs = (n * 1000L) / sampleRate

        return Report(
            sampleRate = sampleRate,
            channels = channels,
            durationMs = durationMs,
            longestInnerGapMs = longestInnerGap,
            totalInnerSilenceMs = totalInnerSilence,
            leadingSilenceMs = leadingSilence,
            trailingSilenceMs = trailingSilence,
            noiseFloor = noiseFloor,
            peak = peak,
            clippedRatio = if (n > 0) clipped.toFloat() / n else 0f,
            discontinuityRatio = discontinuityRatio,
            discontinuities = discontinuities,
            whiteNoiseSuspected = whiteNoise,
        )
    }

    private fun le16(b: ByteArray, off: Int): Int =
        ((b[off + 1].toInt() and 0xFF) shl 8) or (b[off].toInt() and 0xFF)

    private fun le32(b: ByteArray, off: Int): Int =
        ((b[off + 3].toInt() and 0xFF) shl 24) or
                ((b[off + 2].toInt() and 0xFF) shl 16) or
                ((b[off + 1].toInt() and 0xFF) shl 8) or
                (b[off].toInt() and 0xFF)
}
