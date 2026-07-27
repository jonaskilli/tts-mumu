package com.github.jing332.script.runtime

import com.github.jing332.script.toNativeArrayBuffer
import com.github.jing332.script.withRhinoContext
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.mozilla.javascript.Context
import org.mozilla.javascript.IdFunctionObject
import org.mozilla.javascript.IdScriptableObject
import org.mozilla.javascript.ScriptRuntime
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined
import org.mozilla.javascript.annotations.JSConstructor
import org.mozilla.javascript.typedarrays.NativeArrayBuffer
import org.mozilla.javascript.typedarrays.NativeBuffer
import org.mozilla.javascript.typedarrays.NativeUint8Array
import java.util.concurrent.TimeUnit

class NativeWebSocket constructor(
    val url: String = "",
    val headers: Map<CharSequence, CharSequence> = emptyMap(),
) : IdScriptableObject() {
    constructor() : this("")

    companion object {
        val logger = KotlinLogging.logger("NativeWebSocket")
        const val CLASS_NAME = "Websocket"

        const val Id_constructor = 1
        const val Id_send = 2
        const val Id_close = 3
        const val Id_cancel = 4
        const val MAX_PROTOTYPE_ID = Id_cancel

        const val WEBSOCKET_TAG = "Websocket"

        const val WS_CONNECTING = 0
        const val WS_OPEN = 1
        const val WS_CLOSING = 2
        const val WS_CLOSED = 3

        @JvmStatic
        fun init(cx: Context, scope: Scriptable?, sealed: Boolean) {
            val obj = NativeWebSocket()
            obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed)
        }

        private val client by lazy {
            OkHttpClient.Builder()
                .writeTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .connectTimeout(5, TimeUnit.SECONDS)
                .callTimeout(5, TimeUnit.SECONDS)
                .build()
        }
    }


    private var readyState: Int = WS_CLOSED
    private var ws: WebSocket? = null
    private lateinit var event: NativeEventTarget

    override fun getClassName(): String = CLASS_NAME

    override fun fillConstructorProperties(ctor: IdFunctionObject?) {
        ctor?.apply {
            defineProperty("CONNECTING", WS_CONNECTING, ScriptableObject.READONLY)
            defineProperty("OPEN", WS_OPEN, ScriptableObject.READONLY)
            defineProperty("CLOSING", WS_CLOSING, ScriptableObject.READONLY)
            defineProperty("CLOSED", WS_CLOSED, ScriptableObject.READONLY)
        }

        super.fillConstructorProperties(ctor)
    }

    override fun initPrototypeId(id: Int) {

        var s = ""
        var arity = 0
        when (id) {
            Id_constructor -> {
                arity = 1
                s = "constructor"
            }

            Id_send -> {
                arity = 1
                s = "send"
            }

            Id_close -> {
                arity = 2
                s = "close"
            }

            Id_cancel -> {
                arity = 0
                s = "cancel"
            }
        }
        initPrototypeMethod(WEBSOCKET_TAG, id, s, arity)
    }


    override fun findPrototypeId(name: String?): Int {
        return when (name) {
            "constructor" -> Id_constructor
            "send" -> Id_send
            "close" -> Id_close
            "cancel" -> Id_cancel
            else -> 0
        }
    }

    private fun sendBytes(bytes: ByteArray): Boolean {
        logger.trace { "send binary message: ${bytes.size} bytes" }
        return ws?.send(bytes.toByteString()) == true
    }

    override fun execIdCall(
        f: IdFunctionObject,
        cx: Context,
        scope: Scriptable,
        thisObj: Scriptable?,
        args: Array<out Any>?,
    ): Any {
        if (!f.hasTag(WEBSOCKET_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args)
        }
        when (f.methodId()) {
            Id_constructor -> {
                return js_constructor(cx, scope, args)
            }

            Id_send -> {
                when (val arg0 = args!![0]) {
                    is CharSequence -> {
                        val text = arg0.toString()
                        logger.trace { "send text message: $text" }
                        return ws?.send(text) == true
                    }

                    is ByteArray -> {
                        return sendBytes(arg0)
                    }

                    is NativeUint8Array -> {
                        return sendBytes(arg0.buffer.buffer)
                    }

                    is NativeArrayBuffer -> {
                        return sendBytes(arg0.buffer)
                    }
                }
            }

            Id_close -> {
                val code = ScriptRuntime.toInt32(args!![0])
                val reason = ScriptRuntime.toString(args[1])
                logger.trace { "closing: $code, $reason" }
                return ws?.close(code, reason) ?: false
            }

            Id_cancel -> {
                ws?.cancel()
                return Undefined.instance
            }
        }

        return super.execIdCall(f, cx, scope, thisObj, args)
    }

    @Suppress("UNCHECKED_CAST")
    @JSConstructor
    private fun js_constructor(
        cx: Context,
        scope: Scriptable,
        args: Array<out Any>?,
    ): Any {
        val url = args!![0].toString()
        val headers = args[1] as? Map<CharSequence, CharSequence> ?: emptyMap()
        val obj = NativeWebSocket(url)
        obj.defineProperty(
            "readyState",
            ::readyState,
            null,
            ScriptableObject.READONLY
        )
        event = NativeEventTarget(obj)

        val req = Request.Builder().url(url).apply {
            for (header in headers) {
                addHeader(header.key.toString(), header.value.toString())
            }
        }.build()

        logger.trace { "connecting to $url" }
        ws = client.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                readyState = WS_OPEN
                val res = withRhinoContext { NativeResponse.of(it, scope, response) }
                event.emit("open", res)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                event.emit("message", text)
                event.emit("text", text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val buffer =
                    withRhinoContext {
                        NativeBuffer.of(
                            it,
                            scope,
                            bytes.toByteArray().toNativeArrayBuffer(it, scope)
                        )
                    }

                event.emit("message", buffer)
                event.emit("binary", buffer)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                readyState = WS_CLOSING
                ws?.close(code, reason) // call onClosed
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                readyState = WS_CLOSED
                event.emit("close", code, reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                readyState = WS_CLOSED
                val res = withRhinoContext { NativeResponse.of(it, scope, response) }
                event.emit("error", t.message ?: "", res)
            }
        })

        return obj
    }
}