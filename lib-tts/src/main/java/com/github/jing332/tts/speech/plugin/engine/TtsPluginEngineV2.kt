package com.github.jing332.tts.speech.plugin.engine

import android.content.Context
import android.util.Log
import com.drake.net.Net
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.script.engine.RhinoScriptEngine
import com.github.jing332.script.runtime.NativeResponse
import com.github.jing332.script.runtime.console.Console
import com.github.jing332.script.simple.CompatScriptRuntime
import com.github.jing332.script.source.toScriptSource
import com.github.jing332.tts.speech.EmptyInputStream
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined
import org.mozilla.javascript.typedarrays.NativeArrayBuffer
import org.mozilla.javascript.typedarrays.NativeTypedArrayView
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

open class TtsPluginEngineV2(val context: Context, var plugin: Plugin) {
    companion object {
        const val OBJ_PLUGIN_JS = "PluginJS"
        const val FUNC_GET_AUDIO = "getAudio"
        const val FUNC_GET_AUDIO_V2 = "getAudioV2"
        const val FUNC_ON_LOAD = "onLoad"
        const val FUNC_ON_STOP = "onStop"

        private val executor = Executors.newCachedThreadPool()

        /**
         * 在独立线程中执行 block，并通过 CompletableDeferred + withTimeout 确保：
         * 1. 无论底层是否响应中断，最多等待 timeoutMs 后一定返回。
         * 2. 外层协程取消时，future.cancel(true) 会被立即调用，尝试中断后台线程。
         */
        private suspend fun <T> runWithTimeout(timeoutMs: Long, block: () -> T): T {
            val deferred = CompletableDeferred<T>()
            val future = executor.submit {
                try {
                    deferred.complete(block())
                } catch (e: Throwable) {
                    deferred.completeExceptionally(e)
                }
            }

            val handle = currentCoroutineContext()[Job]?.invokeOnCompletion {
                future.cancel(true)
            }

            return try {
                withTimeout(timeoutMs) { deferred.await() }
            } finally {
                handle?.dispose()
                future.cancel(true) // 确保超时或协程取消时中断后台线程
            }
        }
    }

    var console: Console
        get() = engine.runtime.console
        set(value) { engine.runtime.console = value }

    protected val ttsrv = TtsEngineContext(PluginTtsSource(), plugin.userVars, context, plugin.pluginId)
    val runtime = CompatScriptRuntime(ttsrv)
    var source: PluginTtsSource
        get() = ttsrv.tts
        set(value) { ttsrv.tts = value }

    protected val pluginJsObj: ScriptableObject
        get() = (engine.get(OBJ_PLUGIN_JS) as? ScriptableObject) ?: throw IllegalStateException("Object `$OBJ_PLUGIN_JS` not found")

    protected var engine: RhinoScriptEngine = RhinoScriptEngine(runtime)

    open protected fun execute(script: String): Any? = engine.execute(script.toScriptSource(sourceName = plugin.pluginId))

    fun eval() {
        execute(plugin.code)
        pluginJsObj.apply {
            plugin.name = get("name").toString()
            plugin.pluginId = get("id").toString()
            plugin.author = get("author").toString()
            plugin.iconUrl = get("iconUrl")?.toString() ?: ""
            plugin.defVars = try { get("vars") as Map<String, Map<String, String>> } catch (_: Exception) { emptyMap() }
            plugin.version = try { org.mozilla.javascript.Context.toNumber(get("version")).toInt() } catch (e: Exception) { -1 }
        }
    }

    fun onLoad(): Any? = runCatching { engine.invokeMethod(pluginJsObj, FUNC_ON_LOAD) }.getOrNull()
    fun onStop(): Any? = runCatching { engine.invokeMethod(pluginJsObj, FUNC_ON_STOP) }.getOrNull()

    open fun destroy() {
        onStop()
        engine.destroy()
    }

    private fun handleAudioResult(result: Any?, timeoutMs: Long): InputStream? {
        if (result == null || result is Undefined) return null
        return when (result) {
            is NativeArrayBuffer -> ByteArrayInputStream(result.buffer)
            is NativeTypedArrayView<*> -> ByteArrayInputStream(result.buffer.buffer)
            is InputStream -> result
            is ByteArray -> result.inputStream()
            is NativeResponse -> {
                if (result.rawResponse?.isSuccessful == false) throw RuntimeException("HTTP Error: ${result.rawResponse?.code}")
                result.rawResponse?.body?.byteStream()
            }
            is CharSequence -> {
                val str = result.toString()
                if (str.startsWith("http")) {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                        .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                        .build()
                    val resp = client.newCall(Request.Builder().url(str).build()).execute()
                    if (!resp.isSuccessful) throw RuntimeException("URL Fetch Error: ${resp.code}")
                    resp.body?.byteStream()
                } else throw IllegalStateException(str)
            }
            else -> throw IllegalArgumentException("Unsupported return type: ${result.javaClass.name}")
        }
    }

    private val mMutex by lazy { Mutex() }

    private suspend fun getAudioV2Internal(request: Map<String, Any>, timeoutMs: Long): InputStream {
        val ins = JsBridgeInputStream()
        val callback = ins.getCallback()
        val result = runWithTimeout(timeoutMs) {
            engine.invokeMethod(pluginJsObj, FUNC_GET_AUDIO_V2, request, callback)
                ?: throw NoSuchMethodException("getAudioV2() not found")
        }
        // handleAudioResult 中的 URL 下载是同步阻塞，也需要在超时保护内
        return runWithTimeout(timeoutMs) {
            handleAudioResult(result, timeoutMs)
        } ?: ins
    }

    suspend fun getAudio(
        text: String, locale: String, voice: String,
        rate: Float = 1f, volume: Float = 1f, pitch: Float = 1f,
        timeoutMs: Long = 30000L
    ): InputStream {
        val r = (rate * 50f).toInt(); val v = (volume * 50f).toInt(); val p = (pitch * 50f).toInt()
        Log.d("TtsPluginEngineV2", "getAudio: rate=$rate->$r, volume=$volume->$v, pitch=$pitch->$p, timeout=$timeoutMs")

        return mMutex.withLock {
            val result = try {
                runWithTimeout(timeoutMs) {
                    engine.invokeMethod(pluginJsObj, FUNC_GET_AUDIO, text, locale, voice, r, v, p)
                }
            } catch (_: NoSuchMethodException) {
                return@withLock getAudioV2Internal(
                    mapOf("text" to text, "locale" to locale, "voice" to voice, "rate" to r, "volume" to v, "pitch" to p),
                    timeoutMs
                )
            }
            // handleAudioResult 中的 URL 下载（OkHttpClient.execute）是同步阻塞，
            // 原来在 runWithTimeout 之外，超时无法中断；移入超时范围后，
            // future.cancel(true) 可中断 OkHttp 的阻塞 IO
            val audioStream = runWithTimeout(timeoutMs) {
                handleAudioResult(result, timeoutMs)
            }
            return@withLock audioStream ?: throw RuntimeException("Synthesis Result is Empty")
        }
    }
}
