package com.github.jing332.tts.speech.plugin.engine.type.ws.internal

import okhttp3.Response

data class WebSocketException(val response: Response? = null) : Exception()