package com.github.jing332.tts.speech.plugin

import android.content.Context
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts.speech.EngineState
import com.github.jing332.tts.speech.TextToSpeechProvider
import com.github.jing332.tts.speech.plugin.engine.TtsPluginEngineV2
import com.github.jing332.tts.synthesizer.SystemParams
import kotlinx.coroutines.CancellationException
import java.io.InputStream

open class PluginTtsProvider(
    val context: Context,
    val plugin: Plugin,
) : TextToSpeechProvider<PluginTtsSource>() {

    private var mEngine: TtsPluginEngineV2? = null

    var engine: TtsPluginEngineV2?
        get() = mEngine
        set(value) {
            mEngine = value
        }

    override var state: EngineState = EngineState.Uninitialized()

    override suspend fun getStream(params: SystemParams, source: PluginTtsSource): InputStream {
        val route = parameterRoute(
            pluginId = plugin.pluginId,
            legacySpeed = plugin.pluginHandlesSpeed,
            legacyVolume = plugin.pluginHandlesVolume,
            legacyPitch = plugin.pluginHandlesPitch,
        )
        // audioParams 是唯一用户调节层；仅把插件声明负责的维度发给插件，
        // 本机负责的维度发送中性值，避免插件与本机重复处理。
        val speed = if (route.pluginSpeed) params.speed else 1f
        val volume = if (route.pluginVolume) params.volume else 1f
        val pitch = if (route.pluginPitch) params.pitch else 1f

        // source.data mapping to ttsrv.tts.data for javascript
        mEngine?.source = source

        return try {
            mEngine?.getAudio(
                text = params.text,
                locale = source.locale,
                voice = source.voice,
                rate = speed,
                volume = volume,
                pitch = pitch,
                timeoutMs = params.requestTimeout
            ) ?: throw IllegalStateException("Engine not initialized: ${plugin.pluginId}")
        } catch (e: CancellationException) {
            // 超时或协程取消：仅重置状态，不执行可能阻塞的 onDestroy
            // onDestroy 内部的 onStop 会同步调用脚本（Rhino invokeMethod），可能永久阻塞，
            // 导致外层 withTimeout 的取消信号无法生效、retry() 永远无法触发。
            // 引擎清理由上层 retry() 中的 removeEngine 异步处理。
            state = EngineState.Uninitialized()
            throw e
        } catch (e: Exception) {
            // 发生网络或其他异常时，重置引擎状态并强制销毁
            // 确保下次请求重新执行 onInit()，实现网络恢复后的自愈
            state = EngineState.Uninitialized()
            onDestroy()
            throw e
        }
    }

    override suspend fun onInit() {
        state = EngineState.Initializing
        if (mEngine == null)
            mEngine = TtsPluginEngineManager.get(context, plugin)

        state = EngineState.Initialized
    }

    override fun onStop() {
        super.onStop()
        mEngine?.onStop()
    }

    override fun onDestroy() {
        state = EngineState.Uninitialized()
        mEngine?.onStop()
        TtsPluginEngineManager.remove(plugin.pluginId)
        mEngine = null
    }
}
