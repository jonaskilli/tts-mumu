package com.github.jing332.script.engine

import com.github.jing332.script.runtime.NativeResponse
import okhttp3.Response
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.WrapFactory

object RhinoWrapFactory : WrapFactory() {
    override fun wrap(cx: Context?, scope: Scriptable?, obj: Any?, staticType: Class<*>?): Any? {
        // 墨听/JRead 写法兼容：ttsrv.httpPost/httpGet 等宿主方法返回裸 okhttp3.Response，
        // 传统上 JS 拿到的是无 text()/json() 的 Java 对象，按墨听习惯写 resp.text() 的
        // 第三方插件（如腾讯混元 TTS 插件）直接报「找不到函数 text」。
        // 在 wrap 收口处统一包成 NativeResponse（含 text/json/bytes/code/body 等
        // 方法与 status/ok 等属性），全局 http.get/post 返回的对象与之一致；
        // body() 转发回原生 Response，resp.body().string() 等 okhttp 原生写法不受影响。
        if (obj is Response && cx != null && scope != null) {
            return NativeResponse.of(cx, scope, obj)
        }
        return super.wrap(cx, scope, obj, staticType)
    }

    override fun wrapNewObject(
        cx: Context?,
        scope: Scriptable?,
        obj: Any?,
    ): Scriptable? {
        return super.wrapNewObject(cx, scope, obj)
    }

    override fun wrapAsJavaObject(
        cx: Context?,
        scope: Scriptable?,
        javaObject: Any?,
        staticType: Class<*>?
    ): Scriptable {
        return super.wrapAsJavaObject(cx, scope, javaObject, staticType)
    }

    override fun wrapJavaClass(
        cx: Context?,
        scope: Scriptable?,
        javaClass: Class<*>?,
    ): Scriptable? {
        return super.wrapJavaClass(cx, scope, javaClass)
    }
}