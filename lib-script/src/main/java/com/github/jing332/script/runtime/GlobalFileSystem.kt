package com.github.jing332.script.runtime

import com.github.jing332.script.ensureArgumentsLength
import com.github.jing332.script.exception.runScriptCatching
import com.github.jing332.script.runtime.Environment.Companion.environment
import com.github.jing332.script.toNativeArrayBuffer
import io.github.oshai.kotlinlogging.KotlinLogging
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined
import org.mozilla.javascript.typedarrays.NativeArrayBuffer
import org.mozilla.javascript.typedarrays.NativeUint8Array
import java.io.File
import java.nio.charset.Charset

class GlobalFileSystem : Global() {
    companion object {
        private val logger = KotlinLogging.logger("NativeFileSystem")
        const val NAME = "fs"

        @JvmStatic
        fun init(cx: Context, scope: Scriptable, sealed: Boolean) {
            val obj = GlobalFileSystem()
            obj.parentScope = scope
            obj.prototype = getObjectPrototype(scope)

            obj.defineProperty(
                scope, "readText", 1, ::readText, DONTENUM,
                DONTENUM or READONLY
            )

            obj.defineProperty(
                scope, "readFile", 1, ::readFile, DONTENUM,
                DONTENUM or READONLY
            )

            obj.defineProperty(
                scope, "writeFile", 3, ::writeFile, DONTENUM,
                DONTENUM or READONLY
            )

            obj.defineProperty(
                scope, "rm", 1, ::rm, DONTENUM,
                DONTENUM or READONLY
            )

            obj.defineProperty(
                scope, "rename", 2, ::rename, DONTENUM,
                DONTENUM or READONLY
            )

            obj.defineProperty(
                scope, "mkdir", 2, ::mkdir, DONTENUM,
                DONTENUM or READONLY
            )

            obj.defineProperty(
                scope, "copy", 3, ::copy, DONTENUM,
                DONTENUM or READONLY
            )

            obj.defineProperty(
                scope, "exists", 1, ::exists, DONTENUM,
                DONTENUM or READONLY
            )

            obj.defineProperty(
                scope, "isFile", 3, ::isFile, DONTENUM,
                DONTENUM or READONLY
            )


            if (sealed) obj.sealObject()

            ScriptableObject.defineProperty(scope, NAME, obj, ScriptableObject.DONTENUM)
        }


        private fun Scriptable.file(path: String): File {
            val env = environment()
            var nicePath = path
            if (path.startsWith("./"))
                nicePath = nicePath.replaceFirst("./", env.cacheDir + "/${env.id}")
            else if (!path.startsWith("/"))
                nicePath = env.cacheDir + "/${env.id}/$path"

            logger.debug { "getFile(${nicePath})" }
            return File(nicePath)
        }

        private fun readText(
            cx: Context,
            scope: Scriptable,
            thisObj: Scriptable,
            args: Array<Any>,
        ): Any = ensureArgumentsLength(args, 1..2) {
            runScriptCatching {
                val path = (it[0] as CharSequence).toString()
                val charset = it.getOrNull(1) as? String ?: "UTF-8"
                val f = scope.file(path)
                f.readText(Charset.forName(charset))
            }
        }

        private fun readFile(
            cx: Context,
            scope: Scriptable,
            thisObj: Scriptable,
            args: Array<Any>,
        ): Any = ensureArgumentsLength(args, 1) {
            runScriptCatching {
                val path = (it[0] as CharSequence).toString()
                val file = scope.file(path)
                val buffer = file.readBytes().toNativeArrayBuffer()
                cx.newObject(scope, "Uint8Array", arrayOf(buffer, 0, buffer.length))
            }
        }

        private fun writeFile(
            cx: Context,
            scope: Scriptable,
            thisObj: Scriptable,
            args: Array<Any>,
        ): Any = ensureArgumentsLength(args, 2..3) {
            runScriptCatching {
                val path = (it[0] as CharSequence).toString()
                val body = it[1]
                val charset = args.getOrNull(2) as? CharSequence
                val f = scope.file(path)
                f.parentFile?.mkdirs()
                if (!(f.exists())) f.createNewFile()

                when (body) {
                    is CharSequence -> f.writeText(
                        body.toString(),
                        Charset.forName(charset?.toString() ?: "UTF-8")
                    )

                    is NativeArrayBuffer -> f.writeBytes(body.buffer)
                    is NativeUint8Array -> f.writeBytes(body.buffer.buffer)

                    is ByteArray -> f.writeBytes(body)
                }


                Undefined.instance
            }
        }


        private fun rm(
            cx: Context,
            scope: Scriptable,
            thisObj: Scriptable,
            args: Array<Any>,
        ): Any = ensureArgumentsLength(args, 1..2) {
            runScriptCatching {
                val path = (it[0] as CharSequence).toString()
                val recursive = it.getOrNull(1) == true
                val f = scope.file(path)
                if (f.exists())
                    if (recursive) f.deleteRecursively()
                    else f.delete()
                else false
            }
        }

        private fun rename(
            cx: Context,
            scope: Scriptable,
            thisObj: Scriptable,
            args: Array<Any>,
        ): Any = ensureArgumentsLength(args, 1..2) {
            runScriptCatching {
                val path = (it[0] as CharSequence).toString()
                val newPath = (it[1] as CharSequence).toString()
                val f = scope.file(path)
                if (f.exists())
                    f.renameTo(scope.file(newPath))
                else false
            }
        }

        private fun mkdir(
            cx: Context,
            scope: Scriptable,
            thisObj: Scriptable,
            args: Array<Any>,
        ): Any = ensureArgumentsLength(args, 1..2) {
            runScriptCatching {
                val path = (it[0] as CharSequence).toString()
                val recursive = it.getOrNull(1) == true
                val f = scope.file(path)

                if (recursive) f.mkdirs()
                else f.mkdir()
            }
        }

        private fun copy(
            cx: Context,
            scope: Scriptable,
            thisObj: Scriptable,
            args: Array<Any>,
        ): Any = ensureArgumentsLength(args, 2..3) {
            runScriptCatching {
                val path = (it[0] as CharSequence).toString()
                val newPath = (it[1] as CharSequence).toString()
                val overwrite = it.getOrNull(2) == true
                val f = scope.file(path)

                if (f.exists())
                    f.copyTo(scope.file(newPath), overwrite)
                else false
            }
        }

        private fun exists(
            cx: Context,
            scope: Scriptable,
            thisObj: Scriptable,
            args: Array<Any>,
        ): Any = ensureArgumentsLength(args, 1) {
            runScriptCatching {
                val path = (it[0] as CharSequence).toString()
                val f = scope.file(path)
                f.exists()
            }
        }

        private fun isFile(
            cx: Context,
            scope: Scriptable,
            thisObj: Scriptable,
            args: Array<Any>,
        ): Any = ensureArgumentsLength(args, 1) {
            runScriptCatching {
                val path = (it[0] as CharSequence).toString()
                val f = scope.file(path)
                f.isFile
            }
        }

    }
}