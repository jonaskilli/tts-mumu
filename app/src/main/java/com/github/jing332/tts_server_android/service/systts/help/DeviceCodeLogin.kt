package com.github.jing332.tts_server_android.service.systts.help

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 设备码登录通用引擎（10-09 全渠道批）：cline(WorkOS)/minimax/zcode/qoder 都是
 * 「申请设备码 → 用户浏览器授权 → 轮询换 token」形态，差异只在端点/参数/判定。
 * 本引擎承接共性：申请 → 返回 (verificationUri, 状态)；轮询 → 完成回调。
 * pending 判定由各渠道闭包给（cline=400+error 字段、minimax=200+status=pending）。
 */
object DeviceCodeLogin {

    /** form 表单 POST（设备码族全是 form body），返回 HttpResp 同款 */
    fun postForm(url: String, form: Map<String, String>, headers: Map<String, String> = emptyMap()): ChannelHttpResp {
        val body = form.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 30_000
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                doOutput = true
                outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code >= 400) conn.errorStream else conn.inputStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            ChannelHttpResp(code in 200..299, code, text)
        } catch (e: Exception) {
            ChannelHttpResp(false, -1, e.message ?: e.toString())
        } finally {
            conn?.disconnect()
        }
    }

    /** JSON POST（Content-Type: application/json） */
    fun postJson(url: String, body: String, headers: Map<String, String> = emptyMap()): ChannelHttpResp {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 30_000
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                doOutput = true
                outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code >= 400) conn.errorStream else conn.inputStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            ChannelHttpResp(code in 200..299, code, text)
        } catch (e: Exception) {
            ChannelHttpResp(false, -1, e.message ?: e.toString())
        } finally {
            conn?.disconnect()
        }
    }

    fun get(url: String, headers: Map<String, String> = emptyMap()): ChannelHttpResp {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 30_000
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }
            val code = conn.responseCode
            val stream = if (code >= 400) conn.errorStream else conn.inputStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            ChannelHttpResp(code in 200..299, code, text)
        } catch (e: Exception) {
            ChannelHttpResp(false, -1, e.message ?: e.toString())
        } finally {
            conn?.disconnect()
        }
    }

    /** PKCE：verifier（43~128 字符，RFC3986 字符集）+ S256 challenge（base64url 去 padding） */
    fun pkce(): Pair<String, String> {
        val bytes = ByteArray(48).also { java.security.SecureRandom().nextBytes(it) }
        val verifier = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING or android.util.Base64.URL_SAFE)
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        val challenge = android.util.Base64.encodeToString(digest, android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING or android.util.Base64.URL_SAFE)
        return verifier to challenge
    }

    fun randomHex(nBytes: Int): String =
        ByteArray(nBytes).also { java.security.SecureRandom().nextBytes(it) }
            .joinToString("") { "%02x".format(it) }

    fun randomUuid(): String = java.util.UUID.randomUUID().toString()
}

/** 各渠道 JSON 小工具（复用 org.json，与 AccountPool.parseJson 同源） */
internal fun JSONObject.optNonNullString(key: String): String = optString(key).takeIf { it != "null" } ?: ""

/** 通用 HTTP 响应（AccountPool/KeyListFile 各有 private 版，渠道层用这份公开的） */
data class ChannelHttpResp(val ok: Boolean, val code: Int, val body: String)
