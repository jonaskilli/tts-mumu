package com.github.jing332.tts.loudness

import android.content.Context
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
 * 数据存储：单一文件 Download/chajian/loudness_stats.json（用户可见、可直接查看），
 * 不再使用 SharedPreferences。文件内字段为中文易读格式。
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

    /**
     * 学习数据文件：/storage/emulated/0/Download/chajian/loudness_stats.json
     * 路径与项目其它模块一致（朗读规则 JS 的 getFile()、插件缓存 PluginManager.CACHE_BASE_DIR
     * 均写到 /storage/emulated/0/Download/chajian）。这是唯一存储，用户可直接查看。
     */
    private const val FILE_BASE_DIR = "/storage/emulated/0/Download"
    private const val FILE_DIR_NAME = "chajian"
    private const val FILE_NAME = "loudness_stats.json"

    private val lock = Any()
    private var cachedStats: MutableMap<String, LoudnessStat> = linkedMapOf()
    private var fileLoaded = false
    private val analyzingKeys = mutableSetOf<String>()

    private var enabledProvider: () -> Boolean = { false }
    private var maxGainProvider: () -> Float = { 1.35f }

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    data class LoudnessInfo(
        val speakerKey: String,
        val gain: Float,
        val learned: Boolean
    )

    /**
     * 内部统计：保留 dB 等数值用于增益计算。
     * [displayName] 为用户可见发音人名，写文件时作为顶层 key 展示。
     */
    private data class LoudnessStat(
        val count: Int,
        val activeRmsDb: Float,
        val fullRmsDb: Float,
        val peak: Float,
        val voicedRatio: Float,
        val updatedAt: Long,
        val displayName: String = "",
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
     * @param context Android Context（保留以备将来按 context 解析存储路径）
     * @param enabledProvider 响度均衡是否启用的 provider
     * @param maxGainProvider 最大增益 provider（如 1.35f 表示 135%）
     */
    @Suppress("UNUSED_PARAMETER")
    fun init(
        context: Context,
        enabledProvider: () -> Boolean,
        maxGainProvider: () -> Float
    ) {
        this.enabledProvider = enabledProvider
        this.maxGainProvider = maxGainProvider
        loadFromFileLocked()
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
            val count = cachedStats.size
            cachedStats = linkedMapOf()
            fileLoaded = true
            deleteFileLocked()
            logger.info { "响度学习：已重置（清空 $count 个发音人数据）" }
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
                val newDisplayName = config?.speechInfo?.displayName?.takeIf { it.isNotBlank() }
                    ?: old?.displayName ?: ""
                map[key] = LoudnessStat(
                    count = newCount,
                    activeRmsDb = activeRmsDb,
                    fullRmsDb = fullRmsDb,
                    peak = peak.coerceIn(0f, 1f),
                    voicedRatio = voicedRatio.coerceIn(0f, 1f),
                    updatedAt = System.currentTimeMillis(),
                    displayName = newDisplayName,
                )
                cachedStats = map.entries
                    .sortedByDescending { it.value.updatedAt }
                    .take(MAX_STATS)
                    .associate { it.key to it.value }
                    .toMutableMap()
                persistLocked()
                logLearned(key, map[key]!!)
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
                val newDisplayName = config?.speechInfo?.displayName?.takeIf { it.isNotBlank() }
                    ?: old?.displayName ?: ""
                map[key] = LoudnessStat(
                    count = newCount,
                    activeRmsDb = activeRmsDb,
                    fullRmsDb = fullRmsDb,
                    peak = peak.coerceIn(0f, 1f),
                    voicedRatio = voicedRatio.coerceIn(0f, 1f),
                    updatedAt = System.currentTimeMillis(),
                    displayName = newDisplayName,
                )
                cachedStats = map.entries
                    .sortedByDescending { it.value.updatedAt }
                    .take(MAX_STATS)
                    .associate { it.key to it.value }
                    .toMutableMap()
                persistLocked()
                logLearned(key, map[key]!!)
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
        synchronized(lock) {
            if (!fileLoaded) loadFromFileLocked()
            return cachedStats
        }
    }

    /**
     * 从 Download/chajian/loudness_stats.json 读取数据到内存缓存。
     * 文件不存在或解析失败时回退为空数据，不影响播放。
     */
    private fun loadFromFileLocked() {
        fileLoaded = true
        runCatching {
            val file = File(File(FILE_BASE_DIR, FILE_DIR_NAME), FILE_NAME)
            if (!file.exists()) {
                cachedStats = linkedMapOf()
                return@runCatching
            }
            val raw = file.readText()
            cachedStats = parse(raw)
        }.onFailure {
            logger.warn(it) { "loudness load from file failed" }
            cachedStats = linkedMapOf()
        }
    }

    /**
     * 解析文件 JSON。同时兼容中文易读格式与旧版英文 key 格式。
     */
    private fun parse(raw: String): MutableMap<String, LoudnessStat> {
        if (raw.isBlank()) return linkedMapOf()
        return runCatching {
            val root = JSONObject(raw)
            val result = linkedMapOf<String, LoudnessStat>()
            root.keys().forEach { key ->
                val obj = root.optJSONObject(key) ?: return@forEach

                // 中文易读格式（新）
                val displayNameCn = obj.optString("发音人", "")
                if (displayNameCn.isNotEmpty()) {
                    val count = obj.optInt("学习次数", 0)
                    val loudnessStr = obj.optString("平均响度", "")
                    val activeRmsDb = parseDb(loudnessStr)
                    val peakStr = obj.optString("峰值音量", "")
                    val peak = parsePercent(peakStr)
                    val ratioStr = obj.optString("语音占比", "")
                    val voicedRatio = parsePercent(ratioStr)
                    val timeStr = obj.optString("最后更新", "")
                    val updatedAt = parseTime(timeStr)
                    val fullRmsDb = activeRmsDb
                    // 内存 map 必须用 speakerKey 作 key（与 infoFor/needsAnalysis 查找一致），
                    // 文件顶层是展示用的 displayKey，真正的 speakerKey 存在 _内部key 字段
                    val internalKey = obj.optString("_内部key", "").ifBlank { key }
                    if (count > 0 && activeRmsDb.isFinite()) {
                        result[internalKey] = LoudnessStat(
                            count, activeRmsDb, fullRmsDb, peak, voicedRatio, updatedAt, displayNameCn
                        )
                    }
                    return@forEach
                }

                // 旧版英文格式（兼容升级）
                val count = obj.optInt("count", 0)
                val legacyRms = obj.optDouble("rms", 0.0).toFloat()
                val activeRms = obj.optDouble("activeRms", legacyRms.toDouble()).toFloat()
                val fullRms = obj.optDouble("fullRms", legacyRms.toDouble()).toFloat()
                val activeRmsDb = obj.optDouble("activeRmsDb", rmsToDb(activeRms).toDouble()).toFloat()
                val fullRmsDb = obj.optDouble("fullRmsDb", rmsToDb(fullRms).toDouble()).toFloat()
                val peak = obj.optDouble("peak", 0.0).toFloat()
                val voicedRatio = obj.optDouble("voicedRatio", 1.0).toFloat()
                val updatedAt = obj.optLong("updatedAt", 0L)
                val displayName = obj.optString("displayName", "")
                if (count > 0 && activeRmsDb.isFinite() && fullRmsDb.isFinite()) {
                    result[key] = LoudnessStat(count, activeRmsDb, fullRmsDb, peak, voicedRatio, updatedAt, displayName)
                }
            }
            result
        }.getOrDefault(linkedMapOf())
    }

    /**
     * 把内存数据以中文易读格式写入 Download/chajian/loudness_stats.json。
     * 这是唯一存储，整体覆盖写入；失败仅记录日志，不影响播放（内存缓存仍有效）。
     */
    private fun persistLocked() {
        runCatching {
            val dir = File(FILE_BASE_DIR, FILE_DIR_NAME)
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, FILE_NAME)
            if (cachedStats.isEmpty()) {
                if (file.exists()) file.delete()
                return@runCatching
            }
            val root = JSONObject()
            cachedStats.forEach { (key, stat) ->
                val displayKey = buildDisplayKey(stat.displayName, key)
                root.put(displayKey, JSONObject().apply {
                    put("发音人", stat.displayName.ifBlank { extractVoice(key) })
                    put("学习次数", stat.count)
                    put("平均响度", "%.1f dB".format(stat.activeRmsDb))
                    put("峰值音量", "%.0f%%".format(stat.peak * 100))
                    put("语音占比", "%.0f%%".format(stat.voicedRatio * 100))
                    put("最后更新", timeFormat.format(Date(stat.updatedAt)))
                    // 内部 key，用于重启后重建 speakerKey 与 LoudnessStat 的映射
                    put("_内部key", key)
                })
            }
            file.writeText(root.toString(2))
        }.onFailure { logger.warn(it) { "loudness persist to file failed" } }
    }

    /**
     * 学习一段后打日志，便于在日志栏查看。
     * 含：发音人名、voice、本次响度、学习次数、是否学满。
     */
    private fun logLearned(key: String, stat: LoudnessStat) {
        val name = stat.displayName.ifBlank { extractVoice(key) }
        val full = if (stat.count >= MAX_LEARNED_SAMPLES_PER_SPEAKER) "（已学满）" else ""
        logger.info {
            "响度学习：$name | 响度 ${"%.1f".format(stat.activeRmsDb)} dB | " +
                "第 ${stat.count}/$MAX_LEARNED_SAMPLES_PER_SPEAKER 段$full"
        }
    }

    private fun deleteFileLocked() {
        runCatching {
            val file = File(File(FILE_BASE_DIR, FILE_DIR_NAME), FILE_NAME)
            if (file.exists()) file.delete()
        }.onFailure { logger.warn(it) { "loudness delete file failed" } }
    }

    /**
     * 顶层展示 key：优先用 displayName，为避免重名带上 voice 后缀。
     * 例："晓晓 (zh-CN-XiaoxiaoNeural)"
     */
    private fun buildDisplayKey(displayName: String, speakerKey: String): String {
        val voice = extractVoice(speakerKey)
        return if (displayName.isBlank()) {
            voice.ifBlank { speakerKey }
        } else if (voice.isBlank()) {
            displayName
        } else {
            "$displayName ($voice)"
        }
    }

    /**
     * 从 speakerKey 中提取 voice 部分（最后一段 | 之后）。
     * 例："plugin|com.xxx|zh-CN-XiaoxiaoNeural" → "zh-CN-XiaoxiaoNeural"
     */
    private fun extractVoice(speakerKey: String): String {
        val idx = speakerKey.lastIndexOf('|')
        return if (idx >= 0 && idx < speakerKey.length - 1) speakerKey.substring(idx + 1) else ""
    }

    private fun parseDb(s: String): Float {
        if (s.isBlank()) return Float.NaN
        val num = s.replace("dB", "", ignoreCase = true).trim()
        return num.toFloatOrNull() ?: Float.NaN
    }

    private fun parsePercent(s: String): Float {
        if (s.isBlank()) return 0f
        val num = s.replace("%", "", ignoreCase = true).trim()
        return (num.toFloatOrNull() ?: 0f) / 100f
    }

    private fun parseTime(s: String): Long {
        if (s.isBlank()) return 0L
        return runCatching { timeFormat.parse(s)?.time ?: 0L }.getOrDefault(0L)
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
    private fun dbToGain(db: Float): Float = 10f.pow(db / 20f)
    private fun dbForGain(gain: Float): Float = 20f * log10(gain.coerceAtLeast(0.01f))
}
