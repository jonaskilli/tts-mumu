package com.github.jing332.tts.selftest

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * 系统 TTS 调用自检：PCM 音频指标分析（纯 Kotlin，无 native 依赖）。
 *
 * 2026-11-06 新增。任务书：技术文档\任务书-系统TTS调用自检-20261106.md
 *
 * 口径：只做「测量 + 显式判据」，不给黑盒结论。判据全部随结果输出，
 * 用户可按自己的耳朵复核。阈值默认值取自常见语音合成音频经验区间，
 * 可通过 [Options] 调整（自检 UI 暴露为「灵敏度」）。
 */
object AudioHealthAnalyzer {
    data class Options(
        /** 静音判定阈值：低于此 dBFS 视为静音。默认 -50dB（语音 TTS 段间常见底噪低于此）。 */
        val silenceThresholdDb: Float = -50f,
        /** 判定为「间隙」的最短时长（ms）。太短的气口不算间隙。默认 120ms。 */
        val minGapMs: Long = 120L,
        /** 白噪音/底噪可疑阈值：语音间隙区 RMS 高于此值视为可疑底噪。默认 -45dBFS。 */
        val noiseFloorWarnDb: Float = -45f,
    )

    /** 单个静音片段。 */
    data class Gap(val startMs: Long, val durationMs: Long)

    /**
     * 分析结果。所有字段都是原始测量值，判据（threshold 等）一并带上，
     * 便于 UI 展示「这就是判据」而不是只给结论。
     */
    data class Report(
        /** 音频总时长（ms），按字节数/采样率/声道推算。 */
        val totalMs: Long,
        val sampleRate: Int,
        val channels: Int,
        /** 语音（非静音）总时长。 */
        val voicedMs: Long,
        /** 间隙总时长与个数。 */
        val gapTotalMs: Long,
        val gapCount: Int,
        /** 最长的一段间隙。null = 没有达到阈值长度的间隙。 */
        val longestGap: Gap?,
        /** 全程 RMS（dBFS），-inf 表示静到零。 */
        val overallRmsDb: Float,
        /** 间隙区（静音段）的平均 RMS（dBFS）：异常底噪的直接证据。 */
        val gapRmsDb: Float,
        /** 是否判定为可疑底噪。 */
        val noiseFloorSuspicious: Boolean,
        val options: Options,
    ) {
        val hasGaps: Boolean get() = gapCount > 0
        val gapRatio: Float
            get() = if (totalMs > 0) gapTotalMs.toFloat() / totalMs.toFloat() else 0f
    }

    /**
     * 分析 PCM16 单/多声道数据。
     *
     * @param pcm 原始 PCM 字节（16bit little-endian）
     * @param frameBytes 每帧字节数 = 2 * channels；用于按声道合并能量
     */
    fun analyze(
        pcm: ByteArray,
        sampleRate: Int,
        channels: Int,
        options: Options = Options(),
    ): Report {
        val ch = channels.coerceAtLeast(1)
        val frameBytes = 2 * ch
        val totalFrames = pcm.size / frameBytes
        val totalMs = if (sampleRate > 0) totalFrames.toLong() * 1000L / sampleRate.toLong() else 0L

        if (totalFrames <= 0) {
            return Report(
                totalMs = 0L, sampleRate = sampleRate, channels = ch,
                voicedMs = 0L, gapTotalMs = 0L, gapCount = 0, longestGap = null,
                overallRmsDb = Float.NEGATIVE_INFINITY,
                gapRmsDb = Float.NEGATIVE_INFINITY,
                noiseFloorSuspicious = false,
                options = options,
            )
        }

        // 逐帧取各声道最大绝对幅值（避免多声道相位抵消导致误判静音）
        val frameAmps = FloatArray(totalFrames)
        var peakOverall = 0.0
        for (f in 0 until totalFrames) {
            var maxAbs = 0
            val base = f * frameBytes
            for (c in 0 until ch) {
                val i = base + c * 2
                if (i + 1 >= pcm.size) break
                val v = ((pcm[i].toInt() and 0xFF) or (pcm[i + 1].toInt() shl 8))
                    .let { if (it > 32767) it - 65536 else it }
                val a = abs(v)
                if (a > maxAbs) maxAbs = a
            }
            frameAmps[f] = maxAbs.toFloat() / 32768f
            val d = frameAmps[f].toDouble()
            if (d > peakOverall) peakOverall = d
        }

        val silenceLinear = dbToLinear(options.silenceThresholdDb.toDouble())
        val minGapFrames = (options.minGapMs * sampleRate / 1000L).coerceAtLeast(1L)

        var voicedFrames = 0L
        var gapFramesTotal = 0L
        var gapCount = 0
        var longestGap: Gap? = null
        var currentGapStart = -1L
        var currentGapFrames = 0L

        // 能量累计（供 RMS）
        var sumSqAll = 0.0
        var sumSqGap = 0.0
        var gapSampleCount = 0L
        var allSampleCount = 0L

        for (f in 0 until totalFrames) {
            val a = frameAmps[f]
            sumSqAll += a.toDouble() * a
            allSampleCount++

            val isSilent = a <= silenceLinear
            if (isSilent) {
                if (currentGapStart < 0) currentGapStart = f.toLong()
                currentGapFrames++
                sumSqGap += a.toDouble() * a
                gapSampleCount++
            } else {
                voicedFrames++
                // 间隙收尾
                if (currentGapStart >= 0) {
                    if (currentGapFrames >= minGapFrames) {
                        val startMs = currentGapStart * 1000L / sampleRate.toLong()
                        val durMs = currentGapFrames * 1000L / sampleRate.toLong()
                        gapCount++
                        gapFramesTotal += currentGapFrames
                        val g = Gap(startMs, durMs)
                        if (longestGap == null || durMs > longestGap!!.durationMs) longestGap = g
                    }
                    currentGapStart = -1L
                    currentGapFrames = 0L
                }
            }
        }
        // 末尾未收尾的静音段也算
        if (currentGapStart >= 0) {
            if (currentGapFrames >= minGapFrames) {
                val startMs = currentGapStart * 1000L / sampleRate.toLong()
                val durMs = currentGapFrames * 1000L / sampleRate.toLong()
                gapCount++
                gapFramesTotal += currentGapFrames
                val g = Gap(startMs, durMs)
                if (longestGap == null || durMs > longestGap!!.durationMs) longestGap = g
            }
        }

        val overallRmsDb = rmsDb(sumSqAll, allSampleCount)
        val gapRmsDb = rmsDb(sumSqGap, gapSampleCount)
        // 底噪可疑 = 存在静音段，且静音段平均能量高于警戒线（说明「静音」里其实有持续噪音）
        val suspicious = gapSampleCount > 0 && gapRmsDb > options.noiseFloorWarnDb

        return Report(
            totalMs = totalMs,
            sampleRate = sampleRate,
            channels = ch,
            voicedMs = voicedFrames * 1000L / sampleRate.toLong(),
            gapTotalMs = gapFramesTotal * 1000L / sampleRate.toLong(),
            gapCount = gapCount,
            longestGap = longestGap,
            overallRmsDb = overallRmsDb,
            gapRmsDb = gapRmsDb,
            noiseFloorSuspicious = suspicious,
            options = options,
        )
    }

    private fun dbToLinear(db: Double): Double = Math.pow(10.0, db / 20.0)

    private fun rmsDb(sumSq: Double, count: Long): Float {
        if (count <= 0 || sumSq <= 0.0) return Float.NEGATIVE_INFINITY
        val rms = sqrt(sumSq / count.toDouble())
        if (rms <= 0.0) return Float.NEGATIVE_INFINITY
        return (20.0 * log10(rms)).toFloat()
    }
}
