package com.github.jing332.server.forwarder

import android.util.Log
import com.github.jing332.server.BaseCallback
import com.github.jing332.server.CustomNetty
import com.github.jing332.server.Server
import com.github.jing332.server.installPlugins
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.decodeURLQueryComponent
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.origin
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receive
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.util.getOrFail
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File

class SystemTtsForwardServer(val port: Int, val callback: Callback) : Server {
    private val ktor by lazy {
        embeddedServer(CustomNetty, port = port) {
            installPlugins()
            intercept(ApplicationCallPipeline.Call) {
                // 仅调试用
                // val method = call.request.httpMethod.value
                // val uri = call.request.uri
                // Log.d("ForwardServer", "Request: $method $uri")
            }

            routing {
                staticResources("/", "forwarder")

                suspend fun RoutingContext.handleTts(params: TtsParams, contentType: ContentType = ContentType.parse("audio/x-wav")) {
                    try {
                        Log.i("ForwardServer", "接收请求: ${params.text.take(10)}...")
                        callback.log(com.github.jing332.common.LogLevel.INFO, "接收请求: ${params.text.take(10)}...")

                        // 【核心修改】使用 NonCancellable 保护任务
                        // 即使阅读APP在15秒后断开连接，这里也会继续运行直到下载完成
                        // 这样音频会被存入系统缓存。下次重试时就能直接命中缓存。
                        val file = withContext(NonCancellable) {
                            callback.tts(params)
                        }

                        if (file == null) {
                            Log.e("ForwardServer", "TTS失败: 文件为null")
                            callback.log(com.github.jing332.common.LogLevel.ERROR, "TTS失败: 文件为null")
                            // 如果连接还活着，返回错误；如果已断开，这里会抛异常但无所谓了
                            runCatching {
                                call.respond(HttpStatusCode.InternalServerError, "TTS Generation Failed")
                            }
                        } else {
                            Log.i("ForwardServer", "TTS成功, 准备发送: ${file.length()} bytes")
                            callback.log(com.github.jing332.common.LogLevel.INFO, "TTS成功, 准备发送: ${file.length()} bytes")
                            // 尝试发送音频。如果客户端已经断开，这里会抛出异常，
                            // 但没关系，因为音频已经生成并可能被底层 SystemTtsService 缓存了。
                            call.respondOutputStream(
                                contentType,
                                HttpStatusCode.OK,
                                contentLength = file.length()
                            ) {
                                file.inputStream().use { input ->
                                    input.copyTo(this)
                                }
                                // 注意：AndroidTtsEngine 生成的临时文件通常用完即删
                                // 但 SystemTtsService 内部有自己的缓存机制 (TtsManager)
                                // 所以这里的删除只删除了转发器的临时中转文件
                                file.delete()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ForwardServer", "请求处理异常 (可能是客户端断开): ${e.message}")
                        callback.log(com.github.jing332.common.LogLevel.ERROR, "请求处理异常 (可能是客户端断开): ${e.message}")
                        // 尝试返回错误，如果客户端已断开则忽略
                        runCatching {
                            call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
                        }
                    }
                }

                get("api/tts") {
                    try {
                        val text = call.parameters.getOrFail("text")
                        val engine = call.parameters.getOrFail("engine")
                        val locale = call.parameters["locale"] ?: ""
                        val voice = call.parameters["voice"] ?: ""
                        val speed = (call.parameters["rate"] ?: call.parameters["speed"])?.toIntOrNull() ?: 50
                        val pitch = call.parameters["pitch"]?.toIntOrNull() ?: 100
                        handleTts(TtsParams(text, engine, locale, voice, speed, pitch))
                    } catch (e: Exception) {
                        runCatching { call.respond(HttpStatusCode.BadRequest, "Params Error") }
                    }
                }

                post("api/tts") {
                    try {
                        val params = call.receive<TtsParams>()
                        handleTts(params)
                    } catch (e: Exception) {
                        runCatching { call.respond(HttpStatusCode.BadRequest, "Body Error") }
                    }
                }

                post("v1/audio/speech") {
                    try {
                        val req = call.receive<OpenAiSpeechRequest>()
                        val text = req.input

                        // OpenAI 默认模型名 tts-1 / tts-1-hd 映射为空字符串（系统默认引擎）
                        val engine = when (req.model) {
                            "", "tts-1", "tts-1-hd" -> ""
                            else -> req.model
                        }

                        // 兼容客户端错误地对 voice 进行 URL 编码的情况
                        var voice = req.voice
                        if (voice.contains("%")) {
                            try {
                                voice = java.net.URLDecoder.decode(voice, "UTF-8")
                            } catch (_: Exception) { }
                        }

                        val speed = ((req.speed - 1.0) * 100).toInt()

                        Log.i("ForwardServer", "OpenAI请求: model=${req.model}, engine=$engine, voice=$voice, speed=$speed, input=${text.take(30)}...")
                        callback.log(com.github.jing332.common.LogLevel.INFO, "OpenAI请求: model=${req.model}, engine=$engine, voice=$voice, speed=$speed, input=${text.take(30)}...")

                        val responseFormat = req.response_format.lowercase()
                        val contentType = when (responseFormat) {
                            "mp3" -> ContentType.parse("audio/mpeg")
                            "opus" -> ContentType.parse("audio/opus")
                            "aac" -> ContentType.parse("audio/aac")
                            "flac" -> ContentType.parse("audio/flac")
                            "wav" -> ContentType.parse("audio/wav")
                            "pcm" -> ContentType.parse("audio/pcm")
                            else -> ContentType.parse("audio/mpeg")
                        }

                        if (text.isBlank()) {
                            call.respond(HttpStatusCode.BadRequest, "{\"error\":{\"message\":\"input is required\",\"type\":\"invalid_request_error\"}}")
                            return@post
                        }

                        handleTts(
                            TtsParams(text, engine, "", voice, speed, 100),
                            contentType = contentType
                        )
                    } catch (e: Exception) {
                        Log.e("ForwardServer", "OpenAI请求处理异常: ${e.message}")
                        callback.log(com.github.jing332.common.LogLevel.ERROR, "OpenAI请求处理异常: ${e.message}")
                        runCatching {
                            call.respond(HttpStatusCode.BadRequest, "{\"error\":{\"message\":\"${e.message}\",\"type\":\"invalid_request_error\"}}")
                        }
                    }
                }

                get("api/engines") { 
                     runCatching { call.respond(callback.engines()) }
                }
                get("api/voices") {
                    runCatching {
                        val engine = call.parameters.getOrFail("engine")
                        call.respond(callback.voices(engine))
                    }
                }
                get("api/legado") {
                    runCatching {
                        val api = call.parameters.getOrFail("api")
                        val name = call.parameters.getOrFail("name")
                        val engine = call.parameters.getOrFail("engine")
                        val voice = call.parameters["voice"] ?: ""
                        val pitch = call.parameters["pitch"] ?: "50"
                        call.respond(LegadoUtils.getLegadoJson(api, name, engine, voice, pitch))
                    }
                }
            }
        }
    }

    override fun start(wait: Boolean, onStarted: () -> Unit, onStopped: () -> Unit) {
        ktor.application.monitor.subscribe(ApplicationStarted) { onStarted() }
        ktor.application.monitor.subscribe(ApplicationStopped) { onStopped() }
        ktor.start(wait)
    }

    override fun stop() {
        ktor.stop(100, 500)
    }

    interface Callback : BaseCallback {
        suspend fun tts(params: TtsParams): File?
        suspend fun voices(engine: String): List<Voice>
        suspend fun engines(): List<Engine>
    }
}
