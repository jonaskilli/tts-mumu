package com.github.jing332.tts.speech

import java.io.InputStream

object EmptyInputStream : InputStream() {
    override fun read(): Int = -1
}