package com.github.jing332.tts.synthesizer

import com.github.jing332.database.entities.systts.BgmConfiguration

interface ITtsRepository {
    fun init()
    fun destroy()

    fun getTts(id: Long): TtsConfiguration?
    fun getAllTts(): Map<Long, TtsConfiguration>
    fun getAllBgm(): List<BgmConfiguration>
}