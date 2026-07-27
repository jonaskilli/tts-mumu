package com.github.jing332.tts

import android.content.Context
import com.github.jing332.tts.synthesizer.event.IEventDispatcher
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging

data class SynthesizerContext(
    var androidContext: Context,
    var logger: KLogger = KotlinLogging.logger { "tts-default" },
    var cfg: SynthesizerConfig = SynthesizerConfig(),
    var event: IEventDispatcher? = null
) {
}