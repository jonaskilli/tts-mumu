package com.github.jing332.tts.speech.plugin

import android.content.Context
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.tts.speech.plugin.engine.TtsPluginUiEngineV2
import com.github.jing332.tts.util.AbstractCachedManager

object TtsPluginEngineManager : AbstractCachedManager<String, TtsPluginUiEngineV2>(
    timeout = 1000L * 60L * 10L, // 10 min
    delay = 1000L * 60L * 1L, // 1 min
) {
    fun get(context: Context, plugin: Plugin): TtsPluginUiEngineV2 {
        // 检查缓存的引擎是否使用了相同的代码
        val cachedEngine = cache.get(plugin.pluginId)
        if (cachedEngine != null && cachedEngine.plugin.code == plugin.code) {
            return cachedEngine
        }

        // 代码已更改，销毁旧引擎并创建新引擎
        cachedEngine?.destroy()

        val engine = TtsPluginUiEngineV2(context, plugin)
        engine.eval()
        cache.put(plugin.pluginId, engine)
        return engine
    }

    fun remove(pluginId: String) {
        cache.remove(pluginId)
    }
}