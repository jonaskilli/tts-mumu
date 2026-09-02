package com.github.jing332.tts

import android.content.Context
import com.github.jing332.database.entities.systts.source.TextToSpeechSource
import com.github.jing332.tts.speech.TextToSpeechProvider
import com.github.jing332.tts.speech.plugin.PluginTtsProvider
import com.github.jing332.tts.util.AbstractCachedManager
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.Executors

object CachedEngineManager :
    AbstractCachedManager<String, TextToSpeechProvider<TextToSpeechSource>>(
        timeout = 1000L * 60L * 10L, // 10 min
        delay = 1000L * 60 // 1 min
    ) {
    private val logger = KotlinLogging.logger("CachedEngineManager")

    // 专用线程池用于异步销毁引擎，防止 onDestroy/onStop 中的同步脚本调用
    // （如 Rhino invokeMethod）阻塞重试流程
    private val destroyExecutor = Executors.newCachedThreadPool { r ->
        Thread(r, "EngineDestroy").apply { isDaemon = true }
    }

    override fun onCacheRemove(key: String, value: TextToSpeechProvider<TextToSpeechSource>): Boolean {
        logger.atDebug { message = "Engine timeout destroy: $key" }
        value.onDestroy()

        return super.onCacheRemove(key, value)
    }

    fun getEngine(context: Context, source: TextToSpeechSource): TextToSpeechProvider<TextToSpeechSource>? {
        val key = source.getKey() + ";" + source.javaClass.simpleName

        val cachedEngine = cache[key]
        return if (cachedEngine == null) {
            val engine = SpeechServiceFactory.createEngine(context, source) ?: return null
            cache.put(key, engine)
            engine
        } else {
            cachedEngine
        }

    }

    fun removeEngine(source: TextToSpeechSource) {
        val key = source.getKey() + ";" + source.javaClass.simpleName
        val engine = cache[key]
        if (engine != null) {
            // 先从缓存移除，确保下次 getEngine 创建新实例
            cache.remove(key)
            // 异步销毁：onDestroy 内部的 onStop 会同步调用脚本（Rhino invokeMethod），
            // 可能因脚本卡死或后台线程占用而永久阻塞，同步等待会导致重试无法触发
            destroyExecutor.submit {
                runCatching { engine.onDestroy() }
                    .onFailure { logger.warn(it) { "async engine destroy failed: $key" } }
            }
        }
    }

    /**
     * 按插件失效全部合成引擎缓存：插件变量(userVars)/代码更新后调用。
     * 缓存是「访问即续期」的 TimedCache，频繁试听的旧引擎永不过期，
     * 不主动清掉的话，改完变量主界面试听仍会提示“请先填写变量”。
     */
    fun removeByPluginId(pluginId: String) {
        cache.removeAll { value ->
            if (value is PluginTtsProvider && value.plugin.pluginId == pluginId) {
                destroyExecutor.submit {
                    runCatching { value.onDestroy() }
                        .onFailure { logger.warn(it) { "async engine destroy failed: $pluginId" } }
                }
                true
            } else false
        }
    }

    fun expireAll() {
        logger.atDebug { message = "Expire all cached engine" }
        cache.removeAll {
            it.onDestroy()
            true
        }
    }
}