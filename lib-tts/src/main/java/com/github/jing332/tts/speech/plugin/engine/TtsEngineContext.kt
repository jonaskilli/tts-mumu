package com.github.jing332.tts.speech.plugin.engine

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.script.annotation.ScriptInterface
import com.github.jing332.script.simple.ext.JsExtensions
import com.github.jing332.tts.CachedEngineManager
import com.github.jing332.tts.speech.EngineState
import com.github.jing332.tts.synthesizer.SystemParams
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * @param tts 在JS中用 `ttsrv.tts` 访问
 */
@Keep
data class TtsEngineContext(
    var tts: PluginTtsSource,
    val userVars: Map<String, String> = mutableMapOf(),
    override val context: Context,
    override val engineId: String
) : JsExtensions(context, engineId) {

    companion object {
        private const val TAG = "TtsEngineContext"
        private const val AUDITION_TIMEOUT = 30_000L
    }

    /**
     * 通过标签(tag)或角色名(personality)查找绑定的TTS配置项，合成试听音频并写入临时文件。
     *
     * 匹配顺序：speechRule.tag → tagData["personality"] → source.voice → displayName → tagName
     * 仅查找 tagRuleId == 当前插件ID 的配置项，跳过当前插件自身的配置项（避免死锁）。
     *
     * @param tag 标签名(如"男主1")或角色名(如"冷酷霸总")或发音人名
     * @param text 试听文本
     * @return 临时音频文件路径，失败返回null
     */
    @ScriptInterface
    fun getAudioByTag(tag: String, text: String): String? {
        return try {
            val audioBytes = synthesizeByTag(tag, text) ?: return null
            val tempFile = File(context.cacheDir, "audition_${System.currentTimeMillis()}.tmp")
            tempFile.writeBytes(audioBytes)
            tempFile.deleteOnExit()
            tempFile.absolutePath
        } catch (e: Exception) {
            Log.w(TAG, "getAudioByTag failed: ${e.message}")
            null
        }
    }

    private fun synthesizeByTag(tag: String, text: String): ByteArray? {
        val trimmedTag = tag.trim()
        if (trimmedTag.isEmpty()) return null

        // 1. 查找匹配的配置项（仅限当前插件管理的）
        val allEnabled = dbm.systemTtsV2.allEnabled
        val match = findConfigByTag(allEnabled, trimmedTag) ?: return null

        val ttsConfig = match.config as TtsConfigurationDTO
        val source = ttsConfig.source

        // 2. 避免死锁：跳过当前插件自身的配置项
        if (source is PluginTtsSource && source.pluginId == engineId) return null

        // 3. 获取或创建引擎
        val engine = CachedEngineManager.getEngine(context, source) ?: return null

        // 4. 初始化引擎（如需要）
        runBlocking {
            if (engine.state is EngineState.Uninitialized) engine.onInit()
        }

        // 5. 合成音频
        val params = SystemParams(
            text = text,
            speed = ttsConfig.audioParams.speed,
            volume = ttsConfig.audioParams.volume,
            pitch = ttsConfig.audioParams.pitch,
        )

        val stream = runBlocking {
            withTimeout(AUDITION_TIMEOUT) { engine.getStream(params, source) }
        }
        val audioBytes = stream.readBytes()
        if (audioBytes.isEmpty()) return null

        // 6. 如果是原始PCM，包装WAV头以便MediaPlayer播放
        return if (ttsConfig.isNeedDecode()) {
            audioBytes
        } else {
            wrapPcmInWav(audioBytes, ttsConfig.audioFormat.sampleRate)
        }
    }

    /**
     * 通过标签(tag)或角色名(personality)查找当前已启用的TTS配置项的发音人显示名。
     *
     * 用于切换分组后实时获取实际生效的发音人名称，替代静态存储的 record.voice。
     * 匹配逻辑与 getAudioByTag 一致，仅查找 tagRuleId == engineId 且已启用的配置项。
     *
     * @param tag 标签名(如"男主1")或角色名(如"冷酷霸总")或发音人名
     * @return 配置项的 displayName，未匹配返回 null
     */
    @ScriptInterface
    fun getVoiceByTag(tag: String): String? {
        return try {
            val trimmedTag = tag.trim()
            if (trimmedTag.isEmpty()) return null

            val allEnabled = dbm.systemTtsV2.allEnabled
            val match = findConfigByTag(allEnabled, trimmedTag) ?: return null

            val config = match.config as TtsConfigurationDTO
            val source = config.source
            if (source is PluginTtsSource && source.pluginId == engineId) return null

            match.displayName
        } catch (e: Exception) {
            Log.w(TAG, "getVoiceByTag failed: ${e.message}")
            null
        }
    }

    /**
     * 查找匹配 tag 的已启用配置项（五级匹配，与 getAudioByTag 共用）
     */
    private fun findConfigByTag(
        allEnabled: List<com.github.jing332.database.entities.systts.SystemTtsV2>,
        trimmedTag: String
    ): com.github.jing332.database.entities.systts.SystemTtsV2? {
        return allEnabled.firstOrNull {
            val config = it.config as? TtsConfigurationDTO ?: return@firstOrNull false
            config.speechRule.tagRuleId == engineId && config.speechRule.tag == trimmedTag
        } ?: allEnabled.firstOrNull {
            val config = it.config as? TtsConfigurationDTO ?: return@firstOrNull false
            config.speechRule.tagRuleId == engineId &&
                    config.speechRule.tagData["personality"]?.trim() == trimmedTag
        } ?: allEnabled.firstOrNull {
            val config = it.config as? TtsConfigurationDTO ?: return@firstOrNull false
            config.speechRule.tagRuleId == engineId && config.source.voice == trimmedTag
        } ?: allEnabled.firstOrNull {
            val config = it.config as? TtsConfigurationDTO ?: return@firstOrNull false
            config.speechRule.tagRuleId == engineId && it.displayName == trimmedTag
        } ?: allEnabled.firstOrNull {
            val config = it.config as? TtsConfigurationDTO ?: return@firstOrNull false
            config.speechRule.tagRuleId == engineId && config.speechRule.tagName.contains(trimmedTag)
        }
    }

    /**
     * 将原始PCM数据包装为WAV格式（16bit单声道）
     */
    private fun wrapPcmInWav(pcmData: ByteArray, sampleRate: Int): ByteArray {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcmData.size

        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        // RIFF header
        buffer.put("RIFF".toByteArray())
        buffer.putInt(36 + dataSize)
        buffer.put("WAVE".toByteArray())
        // fmt chunk
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)          // Subchunk1Size for PCM
        buffer.putShort(1)         // AudioFormat = PCM
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign.toShort())
        buffer.putShort(bitsPerSample.toShort())
        // data chunk
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)
        buffer.put(pcmData)

        return buffer.array()
    }
}