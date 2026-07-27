package com.github.jing332.script.runtime

import com.github.jing332.script.PropertyGetter
import com.github.jing332.script.definePrototypeMethod
import com.github.jing332.script.definePrototypePropertys
import com.github.jing332.script.exception.runScriptCatching
import com.github.jing332.script.toNativeArrayBuffer
import okhttp3.Response
import org.mozilla.javascript.Context
import org.mozilla.javascript.LambdaConstructor
import org.mozilla.javascript.ScriptRuntime
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined
import org.mozilla.javascript.json.JsonParser
import org.mozilla.javascript.typedarrays.NativeBuffer

class NativeResponse private constructor(val rawResponse: Response? = null) :
    ScriptableObject() {
    override fun getClassName(): String = CLASS_NAME

    companion object {
        const val CLASS_NAME = "Response"

        @JvmStatic
        val serialVersionUID: Long = 3110411773054879588L

        fun of(cx: Context, scope: Scriptable, resp: Response?): Scriptable {
            return cx.newObject(scope, CLASS_NAME, arrayOf(resp))
        }

        @JvmStatic
        fun init(cx: Context, scope: Scriptable, sealed: Boolean) {
            val constructor =
                LambdaConstructor(
                    scope,
                    CLASS_NAME,
                    1,
                    LambdaConstructor.CONSTRUCTOR_NEW
                ) { cx: Context, scope: Scriptable, args: Array<Any?> ->
                    NativeResponse(args.getOrNull(0) as? Response)
                }
            constructor.setPrototypePropertyAttributes(DONTENUM or READONLY or PERMANENT)

            constructor.definePrototypePropertys<NativeResponse>(
                cx, scope,
                listOf(
                    "status" to PropertyGetter { it.rawResponse?.code ?: 0 },
                    "statusText" to PropertyGetter { it.rawResponse?.message ?: "" },
                    "headers" to PropertyGetter {
                        it.rawResponse?.headers?.toMap() ?: Undefined.instance
                    },
                    "url" to PropertyGetter { it.rawResponse?.request?.url?.toString() ?: "" },
                    "redirected" to PropertyGetter { it.rawResponse?.isRedirect == true },
                    "ok" to PropertyGetter { it.rawResponse?.isSuccessful == true },
                )
            )

            constructor.definePrototypeMethod<NativeResponse>(
                scope, "json", 0, { cx, scope, thisObj, args ->
                    thisObj.js_json(ScriptRuntime.toBoolean(args.getOrNull(0)))
                }
            )
            constructor.definePrototypeMethod<NativeResponse>(
                scope,
                "text",
                0,
                { cx, scope, thisObj, args ->
                    thisObj.js_text(
                        ScriptRuntime.toBoolean(
                            args.getOrNull(0)
                        )
                    )
                }
            )
            constructor.definePrototypeMethod<NativeResponse>(
                scope,
                "bytes",
                0,
                { cx, scope, thisObj, args ->
                    thisObj.js_bytes(
                        cx, scope,
                        ScriptRuntime.toBoolean(args.getOrNull(0))
                    )
                }
            )

            defineProperty(scope, CLASS_NAME, constructor, DONTENUM)
            if (sealed) constructor.sealObject()
        }


        private fun js_constructor(
            cx: Context,
            scope: Scriptable,
            args: Array<out Any?>,
        ): NativeResponse {
            val resp = args.getOrNull(0) as? Response
            val obj = NativeResponse(resp)
            return obj
        }

        // 【修改：不再抛出异常】
        // 为了防止子线程崩溃导致 APP 闪退，这里我们不再 throw Exception。
        // 而是直接返回 response，即使它是 503。
        // 我们会在 SystemTtsService 中拦截这个错误的响应内容。
        private fun NativeResponse.checkResponse(force: Boolean): Response {
            val resp = rawResponse ?: throw IllegalStateException("rawResponse is null")
            return resp
        }

        private fun NativeResponse.js_json(force: Boolean): Any = runScriptCatching {
            val resp = checkResponse(force)
            val str = resp.body?.string() ?: return@runScriptCatching ""
            // 如果是 503 错误，这里可能会解析失败，但 runScriptCatching 会接住它，不会闪退
            JsonParser(Context.getCurrentContext(), this).parseValue(str)
        }

        private fun NativeResponse.js_text(force: Boolean): Any = runScriptCatching {
            val resp = checkResponse(force)
            resp.body?.string() ?: ""
        }

        private fun NativeResponse.js_bytes(cx: Context, scope: Scriptable, force: Boolean): Any =
            runScriptCatching {
                val bytes = checkResponse(force).body?.bytes() ?: ByteArray(0)
                val buffer = bytes.toNativeArrayBuffer()

                NativeBuffer.of(cx, scope, buffer)
            }
    }
}
