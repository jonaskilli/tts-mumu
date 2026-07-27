package com.github.jing332.script.engine

import android.content.Context
import org.mozilla.javascript.commonjs.module.provider.ModuleSource
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider
import java.io.File
import java.io.IOException
import java.net.URI

class ModuleSourceProvider(
    private val androidContext: Context,
    val assetDirectory: String,
) :
    UrlModuleSourceProvider(File.listRoots().map { it.toURI() }, null) {

    private val baseAssetUri = URI.create("file:///android_asset/$assetDirectory")
    private fun loadAsset(id: String, validator: Any?): ModuleSource? {
        val filename = if (id.endsWith(".js", ignoreCase = true)) id else "$id.js"

        val reader = try {
            androidContext.assets.open("$assetDirectory/$filename").reader()
        } catch (e: IOException) {
            return null
        }

        val uri = URI("$baseAssetUri/$filename")
        return ModuleSource(reader, null, uri, baseAssetUri, validator)
    }

    override fun loadFromPrivilegedLocations(moduleId: String, validator: Any?): ModuleSource? {
        val networkSource = super.loadFromPrivilegedLocations(moduleId, validator)
        return networkSource ?: loadAsset(moduleId, validator)
    }

    /*    override fun loadSource(moduleId: String, paths: Scriptable?, validator: Any?): ModuleSource? {
            val source = super.loadSource(moduleId, paths, validator)
            return if (source == null) {
                val jsName =
                    if (moduleId.endsWith(".js", ignoreCase = true)) moduleId else "$moduleId.js"
                val filename = "js/$jsName"
                val reader = try {
                    am.open(filename).reader()
                } catch (e: IOException) {
                    return null
                }

                val uri = URI.create("asset:///$filename")
                val baseUri = URI.create("asset:///js/")
                ModuleSource(reader, null, uri, baseUri, validator)
            } else
                source
        }*/
}