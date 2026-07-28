package com.github.jing332.tts.synthesizer

data class RequestPayload(val params: SystemParams, val config: TtsConfiguration) {
    val text: String
        get() = params.text
}