package com.github.jing332.tts_server_android.service.systts.help

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 账号池（10-06 立项，任务书：临时文件/任务书-账号池-20261006.md）。
 *
 * 管理.CodeBuddy 账号：登录（WebView 宿主走 /v2/plugin/auth/state → 浏览器 → 轮询
 * /v2/plugin/auth/token）、续期（/v2/plugin/auth/token/refresh + X-Refresh-Token）、
 * 每日签到领积分。凭据存 app 私有外部存储，不进备份白名单（凭据跟着账号走，恢复后重登）。
 *
 * 协议底数 = DSH 插件逆向实测（10-06）：
 *  - 对话上游 https://copilot.tencent.com/v2/chat/completions 裸 Bearer 即通，仅流式
 *    （非流式 code 11101 被拒）——SSE 聚合见 SseAggregator。
 *  - 登录：POST /v2/plugin/auth/state → JSON 含登录跳转地址；浏览器登录成功后
 *    POST /v2/plugin/auth/token 轮询取 {access_token, refresh_token, expires_in}。
 *  - 续期：POST /v2/plugin/auth/token/refresh，头 X-Refresh-Token。
 *
 * 存储惯例跟 KeyListFile：org.json + File 直读写。文件 = AppConst.externalFilesDir/
 * account_pool/state.json（敏感凭据不落 Download/chajian 公共目录）。
 */
object AccountPool {
    private const val TAG = "AccountPool"

    // CodeBuddy 上游根（对话/鉴权同源）
    const val UPSTREAM_BASE = "https://copilot.tencent.com"

    private val dir: File
        get() = File(AppConst.externalFilesDir, "account_pool")
    private val stateFile: File
        get() = File(dir, "state.json")

    // ==================== 数据 ====================

    data class Account(
        val id: String,
        val provider: String = "codebuddy",
        val nickname: String,
        val accessToken: String,
        val refreshToken: String,
        // access_token 过期时刻（epoch ms；0=未知）
        val expiresAt: Long = 0L,
        // 最近一次积分查询结果（0=未知，-1=查询失败）
        val credits: Long = 0L,
        // 最近一次成功签到时刻（epoch ms；0=从未）
        val lastCheckinAt: Long = 0L,
        val enabled: Boolean = true,
        val createdAt: Long = System.currentTimeMillis(),
    ) {
        /** access_token 是否已过期（留 10 分钟余量；未知不算过期） */
        fun isExpired(now: Long = System.currentTimeMillis()): Boolean =
            expiresAt in 1..(now + 10 * 60_000L)
    }

    fun load(): List<Account> = try {
        if (!stateFile.exists()) emptyList()
        else {
            val arr = JSONObject(stateFile.readText()).optJSONArray("accounts") ?: return emptyList()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                Account(
                    id = o.optString("id"),
                    provider = o.optString("provider", "codebuddy"),
                    nickname = o.optString("nickname"),
                    accessToken = o.optString("accessToken"),
                    refreshToken = o.optString("refreshToken"),
                    expiresAt = o.optLong("expiresAt"),
                    credits = o.optLong("credits"),
                    lastCheckinAt = o.optLong("lastCheckinAt"),
                    enabled = o.optBoolean("enabled", true),
                    createdAt = o.optLong("createdAt"),
                )
            }.filter { it.id.isNotEmpty() && it.accessToken.isNotEmpty() }
        }
    } catch (e: Exception) {
        Log.w(TAG, "load failed: ${e.message}")
        emptyList()
    }

    fun save(accounts: List<Account>): Boolean = try {
        if (!dir.exists()) dir.mkdirs()
        val root = JSONObject()
        val arr = JSONArray()
        accounts.forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("provider", a.provider)
                put("nickname", a.nickname)
                put("accessToken", a.accessToken)
                put("refreshToken", a.refreshToken)
                put("expiresAt", a.expiresAt)
                put("credits", a.credits)
                put("lastCheckinAt", a.lastCheckinAt)
                put("enabled", a.enabled)
                put("createdAt", a.createdAt)
            })
        }
        root.put("accounts", arr)
        stateFile.writeText(root.toString(2))
        true
    } catch (e: Exception) {
        Log.w(TAG, "save failed: ${e.message}")
        false
    }

    // ==================== 登录 ====================

    private data class HttpResp(val ok: Boolean, val code: Int, val body: String)

    private fun httpJson(url: String, method: String, headers: Map<String, String>, body: String?): HttpResp {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/json")
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
            }
            val code = conn.responseCode
            val stream = if (code >= 400) conn.errorStream else conn.inputStream
            val content = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            HttpResp(code in 200..299, code, content)
        } catch (e: Exception) {
            HttpResp(false, -1, e.message ?: e.toString())
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * 登录第一步：请求 auth/state 拿浏览器登录地址。
     * 返回 null = 请求失败（body 含错误信息）。
     */
    fun fetchLoginUrl(): Pair<String?, String> {
        val r = httpJson("$UPSTREAM_BASE/v2/plugin/auth/state", "POST", emptyMap(), "{}")
        if (!r.ok) return null to "HTTP ${r.code}：${briefBody(r.body)}"
        // DSH 实测：state 响应含登录跳转地址；字段名以实际响应为准，逐层找 url 键
        return try {
            val o = JSONObject(r.body)
            val url = o.optString("url").ifEmpty {
                o.optJSONObject("data")?.optString("url") ?: ""
            }
            if (url.isEmpty()) null to "响应无登录地址：${briefBody(r.body)}"
            else url to ""
        } catch (e: Exception) {
            null to "解析失败：${briefBody(r.body)}"
        }
    }

    /**
     * 登录第二步：浏览器登录完成后轮询 auth/token 换凭据。
     * 返回 (新账号 or null, 错误信息)。成功即落盘。
     */
    fun pollToken(existing: Account?): Pair<Account?, String> {
        val headers = buildMap {
            if (existing != null && existing.refreshToken.isNotEmpty())
                put("X-Refresh-Token", existing.refreshToken)
        }
        val r = httpJson("$UPSTREAM_BASE/v2/plugin/auth/token", "POST", headers, "{}")
        if (!r.ok) return null to "HTTP ${r.code}：${briefBody(r.body)}"
        return try {
            val o = JSONObject(r.body).let { it.optJSONObject("data") ?: it }
            val access = o.optString("access_token")
            val refresh = o.optString("refresh_token").ifEmpty { existing?.refreshToken ?: "" }
            if (access.isEmpty()) return null to "响应无 access_token：${briefBody(r.body)}"
            val expiresAt = System.currentTimeMillis() + o.optLong("expires_in", 0L) * 1000L
            val nick = o.optString("nickname").ifEmpty { o.optString("username") }
            val acc = Account(
                id = existing?.id ?: "codebuddy-${System.currentTimeMillis().toString(16)}",
                nickname = nick.ifEmpty { existing?.nickname ?: "CodeBuddy" },
                accessToken = access,
                refreshToken = refresh,
                expiresAt = expiresAt,
                credits = existing?.credits ?: 0L,
                lastCheckinAt = existing?.lastCheckinAt ?: 0L,
                enabled = existing?.enabled ?: true,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            )
            val list = load().filterNot { it.id == acc.id } + acc
            save(list)
            acc to ""
        } catch (e: Exception) {
            null to "解析失败：${briefBody(r.body)}"
        }
    }

    // ==================== 续期 / 签到 / 积分 ====================

    /** 续期：POST auth/token/refresh + X-Refresh-Token。成功返回更新后账号（已落盘）。 */
    fun refresh(acc: Account): Pair<Account?, String> {
        if (acc.refreshToken.isEmpty()) return null to "无 refresh_token，请重新登录"
        val r = httpJson(
            "$UPSTREAM_BASE/v2/plugin/auth/token/refresh", "POST",
            mapOf("X-Refresh-Token" to acc.refreshToken), "{}"
        )
        if (!r.ok) return null to "HTTP ${r.code}：${briefBody(r.body)}"
        return try {
            val o = JSONObject(r.body).let { it.optJSONObject("data") ?: it }
            val access = o.optString("access_token").ifEmpty { acc.accessToken }
            val refresh = o.optString("refresh_token").ifEmpty { acc.refreshToken }
            val expiresAt = System.currentTimeMillis() + o.optLong("expires_in", 0L) * 1000L
            val updated = acc.copy(accessToken = access, refreshToken = refresh, expiresAt = expiresAt)
            save(load().map { if (it.id == acc.id) updated else it })
            updated to ""
        } catch (e: Exception) {
            null to "解析失败：${briefBody(r.body)}"
        }
    }

    /** 每日签到（credits.claimAll 协议；仅 CodeBuddy 有签到接口）。返回 (成功?, 提示)。 */
    fun checkIn(acc: Account): Pair<Boolean, String> {
        val r = httpJson(
            "$UPSTREAM_BASE/v2/plugin/credits/claim", "POST",
            mapOf("Authorization" to "Bearer ${acc.accessToken}"), "{}"
        )
        if (!r.ok) {
            // 401 先试一次静默续期再重签
            if (r.code == 401) {
                val (ref, err) = refresh(acc)
                if (ref != null) return checkIn(ref)
                return false to "登录已过期且续期失败：$err"
            }
            return false to "HTTP ${r.code}：${briefBody(r.body)}"
        }
        val updated = acc.copy(lastCheckinAt = System.currentTimeMillis())
        save(load().map { if (it.id == acc.id) updated else it })
        return true to try {
            val o = JSONObject(r.body).let { it.optJSONObject("data") ?: it }
            o.optLong("credits", -1L).takeIf { it >= 0 }?.let { "签到成功，积分 $it" } ?: "签到成功"
        } catch (e: Exception) {
            "签到成功"
        }
    }

    /** 积分查询（余额接口；-1=失败）。成功即落盘。 */
    fun queryCredits(acc: Account): Pair<Long, String> {
        val r = httpJson(
            "$UPSTREAM_BASE/v2/plugin/credits", "GET",
            mapOf("Authorization" to "Bearer ${acc.accessToken}"), null
        )
        if (r.code == 401) {
            val (ref, err) = refresh(acc)
            if (ref != null) return queryCredits(ref)
            return -1L to "登录已过期且续期失败：$err"
        }
        if (!r.ok) return -1L to "HTTP ${r.code}：${briefBody(r.body)}"
        return try {
            val o = JSONObject(r.body).let { it.optJSONObject("data") ?: it }
            val c = o.optLong("credits", o.optLong("balance", -1L))
            if (c >= 0) save(load().map { if (it.id == acc.id) acc.copy(credits = c) else it })
            c to ""
        } catch (e: Exception) {
            -1L to "解析失败：${briefBody(r.body)}"
        }
    }

    private fun briefBody(body: String): String {
        val compact = body.replace(Regex("\\s+"), " ").trim()
        return if (compact.length > 180) compact.take(180) + "..." else compact.ifEmpty { "无响应内容" }
    }
}
