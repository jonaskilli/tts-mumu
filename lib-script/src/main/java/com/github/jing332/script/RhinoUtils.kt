package com.github.jing332.script

import com.github.jing332.script.engine.RhinoContextFactory
import org.mozilla.javascript.Callable
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.LambdaConstructor
import org.mozilla.javascript.NativeMap
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.ScriptableObject.DONTENUM
import org.mozilla.javascript.ScriptableObject.READONLY
import org.mozilla.javascript.Undefined
import org.mozilla.javascript.typedarrays.NativeArrayBuffer
import org.mozilla.javascript.typedarrays.NativeArrayBufferView
import java.util.function.BiConsumer
import kotlin.jvm.Throws

fun <R> withRhinoContext(block: (Context) -> R): R {
    val cx = RhinoContextFactory.enterContext()
    try {
        return block(cx)
    } finally {
        Context.exit()
    }
}

@Throws(IllegalArgumentException::class)
fun <R> ensureArgumentsLength(
    args: Array<out Any?>?,
    count: Int,
    block: (args: Array<out Any?>) -> R,
): R = ensureArgumentsLength(args, count..count, block)

@Throws(IllegalArgumentException::class)
fun <R> ensureArgumentsLength(
    args: Array<out Any?>?,
    range: IntRange,
    block: (args: Array<out Any?>) -> R,
): R {
    checkNotNull(args)

    if (range.contains(args.size))
        return block(args)

    throw IllegalArgumentException("Method argument count error, need $range arguments, actual $args")
}


fun Scriptable.invokeMethod(scope: Scriptable, name: String, args: Array<Any?>?): Any? {
    val method = get(name, this) as? Function ?: throw NoSuchMethodException(name)
    return withRhinoContext { cx ->
        method.call(cx, scope, this, args)
    }.run { if (this is Undefined) null else this }
}

fun jsToString(any: Any): String {
    return when (any) {
        is NativeArrayBufferView -> any.buffer.buffer.contentToString()
        is Array<*> -> any.joinToString(",") { jsToString(it ?: "") }
        else -> any.toString()

    }
}

fun ByteArray.toNativeArrayBuffer(cx: Context, scope: Scriptable): NativeArrayBuffer {
    val obj = cx.newObject(
        scope,
        NativeArrayBuffer.CLASS_NAME,
        arrayOf(Context.toNumber(size))
    ) as NativeArrayBuffer
    this.copyInto(destination = obj.buffer)
    return obj
}

fun ByteArray.toNativeArrayBuffer(): NativeArrayBuffer {
    val buffer = NativeArrayBuffer(size.toDouble())
    this.copyInto(destination = buffer.buffer)
    return buffer
}


fun interface PropertyGetter<T> {
    fun accept(thisObj: T): Any
}

fun interface PropertySetter<T> {
    fun accept(thisObj: T, value: Any)
}

inline fun <reified T> LambdaConstructor.definePrototypeProperty(
    cx: Context,
    scope: Scriptable,
    name: String,
    getter: PropertyGetter<T>,
    setter: PropertySetter<T>? = null,
    attributes: Int = DONTENUM or READONLY,
) {
    val _setter = if (setter == null) null else object : BiConsumer<Scriptable, Any> {
        override fun accept(t: Scriptable, u: Any) {
            val rawObj = LambdaConstructor.convertThisObject<T>(t, T::class.java)
            setter.accept(rawObj, u)
        }
    }
    definePrototypeProperty(
        cx, name,
        object : java.util.function.Function<Scriptable, Any> {
            override fun apply(t: Scriptable): Any {
                val rawObj = LambdaConstructor.convertThisObject<T>(t, T::class.java)
                return getter.accept(rawObj)
            }
        },
        _setter,
        attributes
    )
}


inline fun <reified T> LambdaConstructor.definePrototypePropertys(
    cx: Context,
    scope: Scriptable,
    list: List<Pair<String, PropertyGetter<T>>>,
    attributes: Int = DONTENUM or READONLY,
) {

    for (v in list) {
        val (name, getter) = v
        definePrototypeProperty<T>(
            cx, scope, name,
            { getter.accept(it) },
            null,
            attributes
        )
    }
//    } else if (list is Triple<*, *, *>) {
//        val (name, getter, setter) = list as Triple<String, java.util.function.Function<T, Any>, java.util.function.BiConsumer<T, Any>>
//        definePrototypeProperty<T>(
//            cx, scope, name,
//            getter, setter, attributes
//        )
//    }
}


fun interface JSMethod<T> {
    fun accept(cx: Context, scope: Scriptable, thisObj: T, args: Array<out Any?>): Any
}

inline fun <reified T> LambdaConstructor.definePrototypeMethod(
    scope: Scriptable,
    name: String,
    length: Int,
    method: JSMethod<T>,
    attributes: Int = DONTENUM or READONLY,
    propertyAttributes: Int = DONTENUM or READONLY,
) {
    definePrototypeMethod(
        scope, name, length,
        object : Callable {
            override fun call(
                cx: Context,
                scope: Scriptable,
                thisObj: Scriptable,
                args: Array<out Any?>,
            ): Any? {
                val rawObj = LambdaConstructor.convertThisObject<T>(thisObj, T::class.java)
                return method.accept(cx, scope, rawObj, args)
            }
        },
        attributes,
        propertyAttributes,
    )
}

fun ScriptableObject.defineFunction(
    name: String,
    callable: Callable,
    length: Int = 0,
    attributes: Int = ScriptableObject.DONTENUM,
    propertyAttributes: Int = ScriptableObject.DONTENUM or ScriptableObject.READONLY,
) {
    defineProperty(
        this,
        name,
        length,
        callable,
        attributes,
        propertyAttributes
    )
}

@Suppress("UNCHECKED_CAST")
fun <K, V> ScriptableObject.toMap(): Map<K, V> {
    val map = mutableMapOf<K, V>()
    for (k in ids) {
        val v = get(k)
        map[k as K] = v as V
    }

    return map
}

object RhinoUtils {
}