package org.mozilla.javascript.typedarrays;


import cn.hutool.core.util.HexUtil
import com.github.jing332.script.exception.runScriptCatching
import com.github.jing332.script.toNativeArrayBuffer
import org.mozilla.javascript.Context
import org.mozilla.javascript.LambdaConstructor
import org.mozilla.javascript.ScriptRuntime
import org.mozilla.javascript.ScriptRuntimeES6
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.Undefined
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Locale

class NativeBufferLoader {
    companion object {
        @JvmStatic
        fun init(cx: Context, scope: Scriptable, sealed: Boolean) {
            NativeBuffer.init2(cx, scope, sealed)
        }
    }
}

class NativeBuffer @JvmOverloads constructor(
    ab: NativeArrayBuffer,
    offset: Int = 0,
    len: Int = ab.length,
) : NativeUint8Array(ab, offset, len) {
    override fun getClassName(): String = CLASS_NAME

    companion object {
        const val CLASS_NAME = "Buffer"

        fun of(
            cx: Context,
            scope: Scriptable,
            buffer: NativeArrayBuffer,
            offset: Int = 0,
            len: Int = buffer.length,
        ): NativeBuffer {
            return cx.newObject(scope, CLASS_NAME, arrayOf(buffer, offset, len)) as NativeBuffer
        }

        fun init2(cx: Context, scope: Scriptable, sealed: Boolean) {
            val constructor = LambdaConstructor(
                scope,
                CLASS_NAME,
                3,
                LambdaConstructor.CONSTRUCTOR_NEW,
                { lcx: Context, lscope: Scriptable, args: Array<out Any> ->
                    NativeTypedArrayView.js_constructor(lcx, lscope, args, ::NativeBuffer, 1)
                }
            )
            constructor.setPrototypePropertyAttributes(DONTENUM or READONLY or PERMANENT);
            NativeTypedArrayView.init(cx, scope, constructor, ::realThis);

            constructor.defineProperty("BYTES_PER_ELEMENT", 1, DONTENUM or READONLY or PERMANENT);
            constructor.definePrototypeProperty(
                "BYTES_PER_ELEMENT", 1, DONTENUM or READONLY or PERMANENT
            );

            ScriptRuntimeES6.addSymbolSpecies(cx, scope, constructor);
            constructor.defineConstructorMethod(
                scope, "from", 2,
                { cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<Any?> ->
                    runScriptCatching { jsFrom(cx, scope, thisObj, args) }
                },
                DONTENUM,
                DONTENUM or READONLY
            )

            constructor.definePrototypeMethod(
                scope, "toString", 2,
                { cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<Any?> ->
                    runScriptCatching { realThis(thisObj).jsToString(cx, args) }
                },
                DONTENUM,
                DONTENUM or READONLY
            )

            defineProperty(scope, CLASS_NAME, constructor, DONTENUM)
            if (sealed) constructor.sealObject()
        }

        private fun realThis(thisObj: Scriptable): NativeBuffer {
            return LambdaConstructor.convertThisObject(thisObj, NativeBuffer::class.java)
        }


        private fun jsFrom(
            cx: Context,
            scope: Scriptable,
            thisObj: Scriptable,
            args: Array<Any?>,
        ): Scriptable {
            val arg0 = args[0]
            if (args.isEmpty() || arg0 == Undefined.instance)
                throw Context.reportRuntimeError("First argument must be a string or buffer")

            if (arg0 is NativeArrayBuffer) {
                val offset = if (args.size > 2) ScriptRuntime.toInt32(args[2]) else 0
                val len = if (args.size > 3) ScriptRuntime.toInt32(args[3]) else arg0.length
                return NativeBuffer.of(cx, scope, arg0, offset, len)
            }

            val input = Context.toString(arg0)
            val encoding = if (args.size > 1) Context.toString(args[1])
                .lowercase(Locale.getDefault()) else "utf-8"

            val bytes = when (encoding) {
                "utf8", "utf-8" -> input.toByteArray(StandardCharsets.UTF_8)
                "base64" -> Base64.getDecoder().decode(input)
                "hex" -> HexUtil.decodeHex(input)
                else -> throw Context.reportRuntimeError("Unsupported encoding: $encoding")
            }

            return NativeBuffer.of(cx, scope, bytes.toNativeArrayBuffer())
        }

    }

    private val data: ByteArray
        get() = buffer.buffer

    @OptIn(ExperimentalStdlibApi::class)
    private fun jsToString(cx: Context, args: Array<Any?>): Any {
        val encoding = if (args.isNotEmpty()) Context.toString(args[0])
            .lowercase(Locale.getDefault()) else "utf-8"

        Charsets
        return when (encoding) {
            "base64" -> Base64.getEncoder().encodeToString(data)
            "hex" -> data.toHexString()
            "utf8", "utf-8" -> data.toString(StandardCharsets.UTF_8)
            "ascii" -> data.toString(StandardCharsets.US_ASCII)
            else -> {
                runCatching { return Charset.forName(encoding).run { data.toString(this) } }
                throw Context.reportRuntimeError("Unsupported encoding: $encoding")
            }
        }
    }
}
