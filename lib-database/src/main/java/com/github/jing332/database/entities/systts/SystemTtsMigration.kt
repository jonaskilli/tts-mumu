package com.github.jing332.database.entities.systts

import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.database.entities.systts.v1.SystemTts
import com.github.jing332.database.entities.systts.v1.tts.BgmTTS
import com.github.jing332.database.entities.systts.v1.tts.LocalTTS
import com.github.jing332.database.entities.systts.v1.tts.PluginTTS

object SystemTtsMigration {
    fun v1Tov2(v1: SystemTts): SystemTtsV2? {
        val config = if (v1.tts is BgmTTS) BgmConfiguration(
            musicList = (v1.tts as BgmTTS).musicList.toList(),
            volume = v1.tts.volume / 1000f
        )
        else {
            val (source, audioParams) = when (v1.tts) {
                is LocalTTS -> {
                    val tts = v1.tts as LocalTTS
                    // LocalTtsSource 不存储 speed/pitch/volume，只存储在 audioParams 中
                    val src = LocalTtsSource(
                        engine = tts.engine ?: "",
                        locale = tts.locale,
                        voice = tts.voiceName ?: "",
                        extraParams = tts.extraParams,
                        isDirectPlayMode = tts.isDirectPlayMode
                    )
                    // 将 rate/pitch 转换到 audioParams
                    val params = AudioParams(
                        speed = (tts.rate + 50) / 100f,
                        volume = 0f, // LocalTTS 没有单独的 volume，使用跟随
                        pitch = tts.pitch / 100f
                    )
                    src to params
                }

                is PluginTTS -> {
                    val tts = v1.tts as PluginTTS
                    // PluginTtsSource 不存储 speed/volume/pitch，只存储在 audioParams 中
                    val src = PluginTtsSource(
                        pluginId = tts.pluginId,
                        locale = tts.locale,
                        voice = tts.voice,
                        data = tts.data
                    )
                    val params = AudioParams(
                        speed = (tts.rate + 50) / 100f,
                        volume = (tts.volume + 50) / 100f,
                        pitch = (tts.pitch + 50) / 100f
                    )
                    src to params
                }

                else -> return null
            }
            
            TtsConfigurationDTO(
                speechRule = v1.speechRule,
                audioParams = audioParams,
                audioFormat = v1.tts.audioFormat,
                source = source
            )
        }

        return SystemTtsV2(
            id = v1.id,
            displayName = v1.displayName ?: "",
            groupId = v1.groupId,
            isEnabled = v1.isEnabled,
            order = v1.order,
            config = config
        )
    }

    fun needMigrate(): Boolean {
        return dbm.systemTtsDao.allTts.isNotEmpty()
    }

    private fun clear() {
        dbm.systemTtsDao.allTts.forEach {
            dbm.systemTtsDao.deleteTts(it)
        }
    }

    fun migrate() {
        dbm.systemTtsDao.allTts.forEach {
            dbm.systemTtsV2.insert(v1Tov2(it) ?: return@forEach)
        }
        clear()
    }
}