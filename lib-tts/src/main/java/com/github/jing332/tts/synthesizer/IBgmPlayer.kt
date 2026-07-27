package com.github.jing332.tts.synthesizer

interface IBgmPlayer {
    fun play()
    fun stop()
    fun init()
    fun destroy()
    fun setPlayList(
        list: List<BgmSource>
    )
}

data class BgmSource(val path: String, val volume: Float)