package com.github.jing332.script.simple.ext

import com.drake.net.Net
import com.drake.net.exception.ConvertException
import com.github.jing332.script.annotation.ScriptInterface
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File

open class JsNet(private val engineId: String) {
    private val groupId by lazy { engineId + hashCode() }

    fun cancelNetwork() {
        Net.cancelGroup(groupId)
    }

    // timeoutMs 参数为兼容 JRead 插件生态的调用习惯（如 httpGet(url, headers, timeout)）。
    // 本框架不支持单请求超时，实际超时由引擎层 runWithTimeout 统一强制执行。
    @JvmOverloads
    @ScriptInterface
    fun httpGet(
        url: CharSequence,
        headers: Map<CharSequence, CharSequence>? = null,
        timeoutMs: Long = 0L
    ): Response {
        return Net.get(url.toString()) {
            setGroup(groupId)
            headers?.let {
                it.forEach {
                    setHeader(it.key.toString(), it.value.toString())
                }
            }
        }.execute()
    }

    /**
     * HTTP GET
     */
    @JvmOverloads
    @ScriptInterface
    fun httpGetString(
        url: CharSequence, headers: Map<CharSequence, CharSequence>? = null
    ): String? {
        return try {
            Net.get(url.toString()) {
                setGroup(groupId)
                headers?.let {
                    it.forEach {
                        setHeader(it.key.toString(), it.value.toString())
                    }
                }
            }.execute<String>()
        } catch (e: ConvertException) {
            throw Exception("Body is not a String, HTTP-${e.response.code}=${e.response.message}")
        }
    }

    @JvmOverloads
    @ScriptInterface
    fun httpGetBytes(
        url: CharSequence, headers: Map<CharSequence, CharSequence>? = null
    ): ByteArray? {
        return try {
            httpGet(url, headers).body?.bytes()
        } catch (e: ConvertException) {
            throw Exception("Body is not a Bytes, HTTP-${e.response.code}=${e.response.message}")
        }
    }

    /**
     * HTTP POST
     */
    // timeoutMs 参数为兼容 JRead 插件生态的调用习惯；实际超时由引擎层统一强制执行
    @JvmOverloads
    @ScriptInterface
    fun httpPost(
        url: CharSequence,
        body: CharSequence? = null,
        headers: Map<CharSequence, CharSequence>? = null,
        timeoutMs: Long = 0L
    ): Response {
        return Net.post(url.toString()) {
            setGroup(groupId)
            body?.let { this.body = it.toString().toRequestBody() }
            headers?.let {
                it.forEach {
                    setHeader(it.key.toString(), it.value.toString())
                }
            }
        }.execute()
    }

    @Suppress("UNCHECKED_CAST")
    @ScriptInterface
    private fun postMultipart(type: String, form: Map<String, Any>): MultipartBody.Builder {
        val multipartBody = MultipartBody.Builder()
        multipartBody.setType(type.toMediaType())

        form.forEach { entry ->
            when (entry.value) {
                // 文件表单
                is Map<*, *> -> {
                    val filePartMap = entry.value as Map<String, Any>
                    val fileName = filePartMap["fileName"] as? String
                    val body = filePartMap["body"]
                    val contentType = filePartMap["contentType"] as? String

                    val mediaType = contentType?.toMediaType()
                    val requestBody = when (body) {
                        is File -> body.asRequestBody(mediaType)
                        is ByteArray -> body.toRequestBody(mediaType)
                        is String -> body.toRequestBody(mediaType)
                        else -> body.toString().toRequestBody()
                    }

                    multipartBody.addFormDataPart(entry.key, fileName, requestBody)
                }

                // 常规表单
                else -> multipartBody.addFormDataPart(entry.key, entry.value as String)
            }
        }

        return multipartBody
    }

    @JvmOverloads
    @ScriptInterface
    fun httpPostMultipart(
        url: CharSequence,
        form: Map<String, Any>,
        type: String = "multipart/form-data",
        headers: Map<CharSequence, CharSequence>? = null
    ): Response {
        return Net.post(url.toString()) {
            setGroup(groupId)
            headers?.let {
                it.forEach {
                    setHeader(it.key.toString(), it.value.toString())
                }
            }
            body = postMultipart(type, form).build()
        }.execute()
    }
}