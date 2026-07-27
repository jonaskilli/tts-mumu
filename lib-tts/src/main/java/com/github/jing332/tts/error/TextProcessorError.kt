package com.github.jing332.tts.error

import com.github.jing332.tts.ConfigType


sealed interface TextProcessorError {
    data class MissingConfig(val type: ConfigType, val details: String = "") :
        TextProcessorError

    data class HandleText(val error: Throwable) : TextProcessorError
    data class MissingRule(val id: String) : TextProcessorError
    object Initialization : TextProcessorError
}