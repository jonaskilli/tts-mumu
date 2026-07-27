package com.github.jing332.tts.synthesizer

import java.nio.ByteBuffer

fun interface PcmAudioDataListener {
    fun receive(data: ByteBuffer)
}