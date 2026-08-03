package com.github.jing332.tts.loudness

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts.synthesizer.TtsConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 发言人响度均衡管理器
 *
 * 负责分析 TTS 音频的响度特征（RMS、峰值），按发音人 key 记录统计，
 * 根据所有发音人的中位数响度计算每个发音人应有的增益值。
 *
 * 设计参考 legado 的 ReadAloudSpeakerLoudnessManager，适配 mumu 架构：
 * - 发言人 key 基于发音人特征（插件ID+voice / 引擎+voice），不随配置 id 变
 * - 分析的是最终输出 PCM（Sonic 手动调节 + LoudnessProcessor 之后），
 *   这样手动调节变化时，新数据会自然覆盖旧数据
 * - 置信度机制：学习次数少时补偿幅度打折扣，避免过度补偿
 *
 * 使用前需在 Application 中调用 [init] 传入 SharedPreferences。
 */
object SpeakerLoudnessManager {
    private val logger = KotlinLogging.logger("SpeakerLoudnessManager")

    private const val PEAK_LIMIT = 0.96f
    private const val MIN_RMS = 0.001f
    private const val VOICE_GATE = 0.012f
    private const val MIN_ANALYSIS_SAMPLES = 4_000
    private const val MAX_ANALYSIS_SAMPLES = 960_000
    private const val MAX_LEARNED_SAMPLES_PER_SPEAKER = 32
    private const val MAX_STATS = 240
    private const val MIN_MEDIAN_SPEAKERS = 2
    private const val MAX_ATTENUATION_DB = 9f

    private const val PREF_KEY = "speakerLoudnessStats"

    private val lock = Any()
    private var cachedRaw = ""
    private var cachedStats: MutableMap<String, LoudnessStat> = linkedMapOf()
    private val analyzingKeys = mutableSetOf<String>()
    private var sp: SharedPreferences? = null

    private var enabledProvider: () -> Boolean = { false }
    private var maxGainProvider: () -> Float = { 1.35f }

    data class LoudnessInfo(
        val speakerKey: String,
        val gain: Float,
        val learned: Boolean
    )

    private data class LoudnessStat(
        val count: Int,
        val activeRmsDb: Float,
        val fullRmsDb: Float,
        val peak: Float,
        val voicedRatio: Float,
        val updatedAt: Long,
    )

    private data class AudioAnalysis(
        val activeRms: Float,
        val fullRms: Float,
        val peak: Float,
        val voicedRatio: Float,
        val samples: Int
    )

    private data class PcmStats(
        val fullSumSquares: Double,
        val activeSumSquares: Double,
        val peak: Float,
        val samples: Int,
        val activeSamples: Int
    )

    /**
     * 初始化，需在 Application 中调用。
     * @param context Android Context
     * @param enabledProvider 响度均衡是否启用的 provider
     * @param maxGainProvider 最大增益 provider（如 1.35f 表示 135%）
     */
    fun init(
        context: Context,
        enabledProvider: () -> Boolean,
        maxGainProvider: () -> Float
    ) {
        this.sp = context.getSharedPreferences("tts_loudness", Context.MODE_PRIVATE)
        this.enabledProvider = enabledProvider
        this.maxGainProvider = maxGainProvider
    }

    /**
     * 根据发音人配置计算增益
     * @param config 当前 TTS 配置
     * @return 增益信息
     */
    fun infoFor(config: TtsConfiguration?): LoudnessInfo {
        if (config == null || !enabledProvider()) {
            return LoudnessInfo(speakerKey(config), 1f, false)
        }
        val key = speakerKey(config)
        val snapshot = stats()
        val stat = snapshot[key]
        val maxGain = maxGainProvider().coerceIn(1.05f, 2.4f)
        val minGain = (1f / maxGain).coerceIn(0.35f, 1f)
        val targetDb = medianDb(snapshot.values)
        val learnedGain = if (stat != null && targetDb != null) {
            val speakerDb = stat.dbForGain()
            val sampleConfidence = stat.count.toFloat() / (stat.count + 4f)
            val populationConfidence = ((snapshot.size - 1).toFloat() / 5f).coerceIn(0.35f, 1f)
            val correctionDb = ((targetDb - speakerDb) * sampleConfidence * populationConfidence)
                .coerceIn(-MAX_ATTENUATION_DB, dbForGain(maxGain))
            val peakGuard = if (stat.peak > MIN_RMS) PEAK_LIMIT / stat.peak else maxGain
            dbToGain(correctionDb).coerceAtMost(peakGuard)
        } else {
            1f
        }
        val gain = learnedGain.coerceIn(minGain, maxGain)
        logger.debug { "loudness infoFor: key=$key, gain=$gain, learned=${stat != null}" }
        return LoudnessInfo(key, gain, stat != null)
    }

    fun learnedSpeakerCount(): Int = stats().size

    fun needsAnalysis(config: TtsConfiguration?): Boolean {
        if (config == null || !enabledProvider()) return false
        val key = speakerKey(config)
        return synchronized(lock) {
            val stat = stats()[key]
            stat == null || stat.count < MAX_LEARNED_SAMPLES_PER_SPEAKER
        }
    }

    fun reset() {
        synchronized(lock) {
            cachedRaw = ""
            cachedStats = linkedMapOf()
            sp?.edit()?.putString(PREF_KEY, "")?.apply()
        }
    }

    /**
     * 分析音频文件并记录响度统计
     * @param file 音频文件
     * @param config TTS 配置（用于生成发言人 key）
     */
    fun analyzeAndRecord(file: File, config: TtsConfiguration?) {
        if (!enabledProvider() || !file.exists() || file.length() <= 0L) return
        val key = speakerKey(config)
        if (!beginAnalysis(key)) return
        try {
            val analysis = runCatching { analyze(file) }
                .onFailure { logger.warn(it) { "loudness analyze failed: $key" } }
                .getOrNull() ?: return
            if (analysis.samples < MIN_ANALYSIS_SAMPLES ||
                (analysis.activeRms <= MIN_RMS && analysis.fullRms <= MIN_RMS)
            ) return
            synchronized(lock) {
                val map = stats().toMutableMap()
                val old = map[key]
                val oldWeight = old?.count?.coerceAtMost(48) ?: 0
                val newCount = ((old?.count ?: 0) + 1).coerceAtMost(999)
                val activeRmsDb = if (old == null) {
                    rmsToDb(analysis.activeRms)
                } else {
                    ((old.activeRmsDb * oldWeight) + rmsToDb(analysis.activeRms)) / (oldWeight + 1)
                }
                val fullRmsDb = if (old == null) {
                    rmsToDb(analysis.fullRms)
                } else {
                    ((old.fullRmsDb * oldWeight) + rmsToDb(analysis.fullRms)) / (oldWeight + 1)
                }
                val peak = if (old == null) {
                    analysis.peak
                } else {
                    ((old.peak * oldWeight) + analysis.peak) / (oldWeight + 1)
                }
                val voicedRatio = if (old == null) {
                    analysis.voicedRatio
                } else {
                    ((old.voicedRatio * oldWeight) + analysis.voicedRatio) / (oldWeight + 1)
                }
                map[key] = LoudnessStat(
                    count = newCount,
                    activeRmsDb = activeRmsDb,
                    fullRmsDb = fullRmsDb,
                    peak = peak.coerceIn(0f, 1f),
                    voicedRatio = voicedRatio.coerceIn(0f, 1f),
                    updatedAt = System.currentTimeMillis(),
                )
                cachedStats = map.entries
                    .sortedByDescending { it.value.updatedAt }
                    .take(MAX_STATS)
                    .associate { it.key to it.value }
                    .toMutableMap()
                persistLocked()
            }
        } finally {
            synchronized(lock) { analyzingKeys.remove(key) }
        }
    }

    /**
     * 直接分析 PCM byte 数组并记录（用于流式模式下已缓存的 PCM）
     */
    fun analyzePcmAndRecord(pcm: ByteArray, config: TtsConfiguration?) {
        if (!enabledProvider() || pcm.size < MIN_ANALYSIS_SAMPLES * 2) return
        val key = speakerKey(config)
        if (!beginAnalysis(key)) return
        try {
            val buffer = ByteBuffer.wrap(pcm)
            val result = readPcm16(buffer, MAX_ANALYSIS_SAMPLES)
            if (result.samples < MIN_ANALYSIS_SAMPLES) return
            val fullRms = sqrt(result.fullSumSquares / result.samples).toFloat()
            val usableActive = result.activeSamples.takeIf { it >= MIN_ANALYSIS_SAMPLES / 6 } ?: 0
            val activeRms = if (usableActive > 0) {
                sqrt(result.activeSumSquares / usableActive).toFloat()
            } else fullRms
            if (activeRms <= MIN_RMS && fullRms <= MIN_RMS) return
            val voicedRatio = (result.activeSamples.toFloat() / result.samples.toFloat()).coerceIn(0f, 1f)
            synchronized(lock) {
                val map = stats().toMutableMap()
                val old = map[key]
                val oldWeight = old?.count?.coerceAtMost(48) ?: 0
                val newCount = ((old?.count ?: 0) + 1).coerceAtMost(999)
                val activeRmsDb = if (old == null) rmsToDb(activeRms)
                else ((old.activeRmsDb * oldWeight) + rmsToDb(activeRms)) / (oldWeight + 1)
                val fullRmsDb = if (old == null) rmsToDb(fullRms)
                else ((old.fullRmsDb * oldWeight) + rmsToDb(fullRms)) / (oldWeight + 1)
                val peak = if (old == null) result.peak
                else ((old.peak * oldWeight) + result.peak) / (oldWeight + 1)
                map[key] = LoudnessStat(
                    count = newCount,
                    activeRmsDb = activeRmsDb,
                    fullRmsDb = fullRmsDb,
                    peak = peak.coerceIn(0f, 1f),
                    voicedRatio = voicedRatio.coerceIn(0f, 1f),
                    updatedAt = System.currentTimeMillis(),
                )
                cachedStats = map.entries
                    .sortedByDescending { it.value.updatedAt }
                    .take(MAX_STATS)
                    .associate { it.key to it.value }
                    .toMutableMap()
                persistLocked()
            }
        } finally {
            synchronized(lock) { analyzingKeys.remove(key) }
        }
    }

    private fun beginAnalysis(key: String): Boolean {
        return synchronized(lock) {
            val old = stats()[key]
            if (old != null && old.count >= MAX_LEARNED_SAMPLES_PER_SPEAKER) {
                false
            } else {
                analyzingKeys.add(key)
            }
        }
    }

    /**
     * 基于发音人特征生成 key，不随配置 id 变化
     */
    fun speakerKey(config: TtsConfiguration?): String {
        if (config == null) return "unknown"
        val source = config.source
        return when (source) {
            is PluginTtsSource -> "plugin|${source.pluginId}|${source.voice}"
            is LocalTtsSource -> "local|${source.engine}|${source.voice}"
            else -> "tts|${source.javaClass.simpleName}|${source.voice}"
        }
    }

    private fun stats(): MutableMap<String, LoudnessStat> {
        val raw = sp?.getString(PREF_KEY, "") ?: ""
        synchronized(lock) {
            if (raw == cachedRaw) return cachedStats
            cachedRaw = raw
            cachedStats = parse(raw)
            return cachedStats
        }
    }

    private fun parse(raw: String): MutableMap<String, LoudnessStat> {
        if (raw.isBlank()) return linkedMapOf()
        return runCatching {
            val root = JSONObject(raw)
            val result = linkedMapOf<String, LoudnessStat>()
            root.keys().forEach { key ->
                val obj = root.optJSONObject(key) ?: return@forEach
                val count = obj.optInt("count", 0)
                val legacyRms = obj.optDouble("rms", 0.0).toFloat()
                val activeRms = obj.optDouble("activeRms", legacyRms.toDouble()).toFloat()
                val fullRms = obj.optDouble("fullRms", legacyRms.toDouble()).toFloat()
                val activeRmsDb = obj.optDouble("activeRmsDb", rmsToDb(activeRms).toDouble()).toFloat()
                val fullRmsDb = obj.optDouble("fullRmsDb", rmsToDb(fullRms).toDouble()).toFloat()
                val peak = obj.optDouble("peak", 0.0).toFloat()
                val voicedRatio = obj.optDouble("voicedRatio", 1.0).toFloat()
                val updatedAt = obj.optLong("updatedAt", 0L)
                if (count > 0 && activeRmsDb.isFinite() && fullRmsDb.isFinite()) {
                    result[key] = LoudnessStat(count, activeRmsDb, fullRmsDb, peak, voicedRatio, updatedAt)
                }
            }
            result
        }.getOrDefault(linkedMapOf())
    }

    private fun persistLocked() {
        val root = JSONObject()
        cachedStats.forEach { (key, stat) ->
            root.put(key, JSONObject().apply {
                put("count", stat.count)
                put("activeRms", dbToRms(stat.activeRmsDb))
                put("fullRms", dbToRms(stat.fullRmsDb))
                put("activeRmsDb", stat.activeRmsDb)
                put("fullRmsDb", stat.fullRmsDb)
                put("peak", stat.peak)
                put("voicedRatio", stat.voicedRatio)
                put("updatedAt", stat.updatedAt)
            })
        }
        cachedRaw = root.toString()
        sp?.edit()?.putString(PREF_KEY, cachedRaw)?.apply()
    }

    private fun analyze(file: File): AudioAnalysis? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        return try {
            extractor.setDataSource(file.absolutePath)
            var trackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val candidate = extractor.getTrackFormat(i)
                val mime = candidate.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    trackIndex = i
                    format = candidate
                    break
                }
            }
            if (trackIndex < 0 || format == null) return null
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            extractor.selectTrack(trackIndex)
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()
            decode(codec, extractor)
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }

    private fun decode(codec: MediaCodec, extractor: MediaExtractor): AudioAnalysis? {
        val info = MediaCodec.BufferInfo()
        var inputEnded = false
        var outputEnded = false
        var fullSumSquares = 0.0
        var activeSumSquares = 0.0
        var peak = 0f
        var samples = 0
        var activeSamples = 0
        while (!outputEnded && samples < MAX_ANALYSIS_SAMPLES) {
            if (!inputEnded) {
                val inputIndex = codec.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val input = codec.getInputBuffer(inputIndex)
                    input?.clear()
                    val size = input?.let { extractor.readSampleData(it, 0) } ?: -1
                    if (size < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputEnded = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            when (val outputIndex = codec.dequeueOutputBuffer(info, 10_000)) {
                MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                else -> if (outputIndex >= 0) {
                    val buffer = codec.getOutputBuffer(outputIndex)
                    if (buffer != null && info.size > 0) {
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        val result = readPcm16(buffer, MAX_ANALYSIS_SAMPLES - samples)
                        fullSumSquares += result.fullSumSquares
                        activeSumSquares += result.activeSumSquares
                        peak = max(peak, result.peak)
                        samples += result.samples
                        activeSamples += result.activeSamples
                    }
                    outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    codec.releaseOutputBuffer(outputIndex, false)
                }
            }
        }
        if (samples <= 0) return null
        val usableActiveSamples = activeSamples.takeIf { it >= MIN_ANALYSIS_SAMPLES / 6 } ?: 0
        val fullRms = sqrt(fullSumSquares / samples).toFloat()
        val activeRms = if (usableActiveSamples > 0) {
            sqrt(activeSumSquares / usableActiveSamples).toFloat()
        } else fullRms
        return AudioAnalysis(
            activeRms = activeRms,
            fullRms = fullRms,
            peak = peak,
            voicedRatio = (activeSamples.toFloat() / samples.toFloat()).coerceIn(0f, 1f),
            samples = samples
        )
    }

    private fun readPcm16(buffer: ByteBuffer, maxSamples: Int): PcmStats {
        var fullSumSquares = 0.0
        var activeSumSquares = 0.0
        var peak = 0f
        var samples = 0
        var activeSamples = 0
        while (buffer.remaining() >= 2 && samples < maxSamples) {
            val lo = buffer.get().toInt() and 0xFF
            val hi = buffer.get().toInt()
            val value = ((hi shl 8) or lo).toShort().toInt() / 32768f
            val abs = kotlin.math.abs(value)
            peak = max(peak, abs)
            fullSumSquares += (value * value).toDouble()
            if (abs >= VOICE_GATE) {
                activeSumSquares += (value * value).toDouble()
                activeSamples += 1
            }
            samples += 1
        }
        return PcmStats(fullSumSquares, activeSumSquares, peak, samples, activeSamples)
    }

    private fun LoudnessStat.dbForGain(): Float {
        return activeRmsDb.takeIf { it.isFinite() } ?: fullRmsDb
    }

    private fun medianDb(values: Collection<LoudnessStat>): Float? {
        val learned = values
            .mapNotNull { it.dbForGain().takeIf(Float::isFinite) }
            .sorted()
        if (learned.size < MIN_MEDIAN_SPEAKERS) return null
        val mid = learned.size / 2
        return if (learned.size % 2 == 0) {
            (learned[mid - 1] + learned[mid]) / 2f
        } else {
            learned[mid]
        }
    }

    private fun rmsToDb(rms: Float): Float = 20f * log10(rms.coerceAtLeast(MIN_RMS))
    private fun dbToRms(db: Float): Float = 10f.pow(db / 20f).coerceIn(0f, 1f)
    private fun dbToGain(db: Float): Float = 10f.pow(db / 20f)
    private fun dbForGain(gain: Float): Float = 20f * log10(gain.coerceAtLeast(0.01f))
}
