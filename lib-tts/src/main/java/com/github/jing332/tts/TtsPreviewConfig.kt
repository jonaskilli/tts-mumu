package com.github.jing332.tts

import com.github.jing332.database.entities.systts.AudioParams

/**
 * App-owned global audio parameters exposed to lib-tts preview bridges.
 *
 * lib-tts cannot depend on the app configuration module, so the application installs this
 * provider during startup. The default keeps standalone library consumers deterministic.
 */
object TtsPreviewConfig {
    @Volatile
    var globalAudioParamsProvider: () -> AudioParams = { AudioParams() }
}
