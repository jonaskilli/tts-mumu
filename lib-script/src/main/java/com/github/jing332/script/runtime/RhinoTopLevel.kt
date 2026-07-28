package com.github.jing332.script.runtime

import com.github.jing332.script.engine.ModuleSourceProvider
import org.mozilla.javascript.Context
import org.mozilla.javascript.ImporterTopLevel
import org.mozilla.javascript.LazilyLoadedCtor
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.commonjs.module.RequireBuilder
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider
import org.mozilla.javascript.typedarrays.NativeBuffer
import org.mozilla.javascript.typedarrays.NativeBufferLoader
import splitties.init.appCtx
import kotlin.reflect.KClass

/**
 * Custom global scope for Rhino JavaScript engine.
 */
class RhinoTopLevel(cx: Context) : ImporterTopLevel(cx) {
    private fun lazyLoaded(
        name: String,
        clazz: KClass<*>,
        sealed: Boolean = false,
        privileged: Boolean = true,
    ) {
        LazilyLoadedCtor(this, name, clazz.qualifiedName, sealed, privileged)
    }

    init {
        initRequireBuilder(cx, this)

        // Property
        lazyLoaded(GlobalUUID.NAME, GlobalUUID::class)
        lazyLoaded(GlobalFileSystem.NAME, GlobalFileSystem::class)
        lazyLoaded(GlobalHttp.NAME, GlobalHttp::class)
        lazyLoaded(GlobalWebview.NAME, GlobalWebview::class)

        // Class
        lazyLoaded(NativeBuffer.CLASS_NAME, NativeBufferLoader::class)
        lazyLoaded(NativeResponse.CLASS_NAME, NativeResponse::class)
        lazyLoaded(NativeWebSocket.CLASS_NAME, NativeWebSocket::class)
    }

    private fun initRequireBuilder(context: Context, scope: Scriptable) {
        val provider = ModuleSourceProvider(appCtx, "js")
        RequireBuilder()
            .setModuleScriptProvider(SoftCachingModuleScriptProvider(provider))
            .setSandboxed(false)
            .createRequire(context, scope)
            .install(scope)
    }
}