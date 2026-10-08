package com.github.jing332.tts_server_android.compose.systts.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.jing332.tts_server_android.service.systts.help.AccountPool
import com.github.jing332.tts_server_android.service.systts.help.DeviceCodeLogin
import com.github.jing332.tts_server_android.service.systts.help.GeminiChannel
import com.github.jing332.tts_server_android.service.systts.help.LobsteraiChannel
import com.github.jing332.tts_server_android.service.systts.help.LocalCallbackServer
import com.github.jing332.tts_server_android.service.systts.help.TraeChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

/**
 * 本地回调型登录弹窗（10-09 回调批）：trae / gemini / lobsterai / codearts 四渠道共用。
 *
 * 流程：起 LocalCallbackServer（IPv4+IPv6 双栈）→ 拼登录 URL → 自动拉起浏览器 +
 * 「打开授权页」按钮兜底 → 等浏览器回调（总超时 360s——gemini 300s 会掐掉二次验证
 * 用户，统一放宽）→ 按渠道路由换 token → 落盘 AccountPool → onDone 刷新列表。
 *
 * 渠道差异（协议=临时文件/渠道协议规格书-20261009.md）：
 *  - trae（§5.1）：固定 18080/authorize；登录 URL 17 参数一个不能少（machine_id 32hex/
 *    device_id 16 数字生成后存 extra 持久化，login_trace_id=(machineId+deviceId) 尾 16 字符）；
 *    回调无 code，直接 ?refreshToken=...&userInfo=...，拿 refreshToken 调
 *    TraeChannel.refresh（签名是 Account）换 access；
 *  - gemini（§12.1）：GeminiChannel.authorizeUrl(port, state)（主机名必须 localhost）；回调
 *    code+state（state 校验）；GeminiChannel.exchangeCode(code, port) 换 token；
 *  - lobsterai（§3.1）：登录 URL hash 段不能动，redirect_uri 百分号编码；客户端生成 uuid
 *    （=登录 URL state）与 firstKeyfrom（登录时刻毫秒串），两者随凭据永久持久化进 extra；
 *    LobsteraiChannel.exchange(authCode, uuid, firstKeyfrom) 换 token；
 *  - codearts（§1.1）：PKCE（verifier 48 字节 base64url）+ DPoP（ES256 P-256，私钥 JWK 存
 *    extra dpop_private_key_jwk）；端口随机 [10000,65535]（低端口 portal 拒绝），路径
 *    /oauth/callback；POST sts /v1/oauth2/tokens 换取（code_challenge_method 是 SHA-256，
 *    不加 auth_callback_url 参数）。
 */
@Composable
fun CallbackLoginDialog(
    provider: String,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("准备登录…") }
    var authUrl by remember { mutableStateOf("") }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(provider) {
        val (nick, err) = withContext(Dispatchers.IO) {
            callbackLogin(provider) { url ->
                authUrl = url
                openBrowser(context, url)
            }
        }
        finished = true
        status = if (err.isNotEmpty()) err else if (nick.isNotEmpty()) "登录成功：$nick" else "登录未完成"
        if (err.isEmpty() && nick.isNotEmpty()) onDone()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("浏览器授权登录") },
        text = {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!finished) {
                    CircularProgressIndicator(Modifier.padding(bottom = 12.dp))
                    Text("正在等待浏览器授权…", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "登录完成后本页自动确认，最长等待 6 分钟；浏览器未自动打开时点下方按钮。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                } else {
                    Text(status, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = {
            if (finished) {
                TextButton(onClick = onDismiss) { Text("关闭") }
            } else {
                Button(
                    onClick = { if (authUrl.isNotEmpty()) openBrowser(context, authUrl) },
                    enabled = authUrl.isNotEmpty(),
                ) { Text("打开授权页") }
            }
        },
        dismissButton = {
            if (!finished) TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

// ==================== 登录主流程（渠道路由） ====================

/** 总超时：统一 360s（gemini 规格本说 6 分钟，300s 会掐掉二次验证用户） */
private const val CALLBACK_TIMEOUT_MS = 360_000L

/**
 * 起回调服务器 → 拼登录 URL → onReady 自动开浏览器 → 等回调（360s）→ 换 token → 落盘。
 * 返回 (昵称, 错误)；err 非空 = 失败。
 */
private suspend fun callbackLogin(provider: String, onReady: (String) -> Unit): Pair<String, String> {
    var server: LocalCallbackServer? = null
    // 回调参数投递口（CompletableDeferred 自身线程安全；闭包捕获由编译器装 Ref，
    // listener 跑在服务器线程、await 在协程线程，跨线程赋值仅在 start 前发生一次）
    var pending: CompletableDeferred<Map<String, String>>? = null
    fun makeServer(port: Int, path: String): LocalCallbackServer =
        LocalCallbackServer(port, path, 360) { params -> pending?.complete(params) }

    return try {
        val url: String = when (provider) {
            "trae" -> {
                // machine_id 32hex / device_id 16 数字：登录时生成，成功后落 extra 持久化
                val machineId = DeviceCodeLogin.randomHex(16)
                val deviceId = randomDigits(16)
                sessionTraeIds = machineId to deviceId
                server = makeServer(18080, "/authorize")
                if (!server.start()) return Pair("", "本机 18080 端口被占用（trae 回调端口固定），请重试")
                TraeChannel.buildLoginUrl(machineId, deviceId, "http://127.0.0.1:18080/authorize")
            }
            "gemini" -> {
                server = makeServer(LocalCallbackServer.randomPort(49152), "/oauth-callback")
                // RFC 8252：必须先监听再拼 URL（端口回退会 redirect_uri_mismatch）
                if (!server.start()) return Pair("", "本地端口绑定失败，请重试")
                sessionGeminiPort = server.boundPort
                val state = DeviceCodeLogin.randomUuid()
                sessionGeminiState = state
                GeminiChannel.authorizeUrl(server.boundPort, state)
            }
            "lobsterai" -> {
                server = makeServer(LocalCallbackServer.randomPort(49152), "/auth/callback")
                if (!server.start()) return Pair("", "本地端口绑定失败，请重试")
                val state = DeviceCodeLogin.randomUuid() // uuid 与登录 state 同值
                sessionLobsterState = state
                sessionLobsterFirstKey = System.currentTimeMillis().toString() // 登录时刻毫秒串
                LobsteraiChannel.buildLoginUrl(server.boundPort, state)
            }
            "codearts" -> {
                // portal 低端口拒绝：随机 [10000,65535]，绑定冲突换口重试（最多 5 次）
                var bound: LocalCallbackServer? = null
                for (attempt in 1..5) {
                    val s = makeServer(LocalCallbackServer.randomPort(10000), "/oauth/callback")
                    if (s.start()) { bound = s; break }
                    try { s.stop() } catch (_: Exception) {}
                    if (attempt == 5) return Pair("", "本地端口绑定失败（连续 5 次），请重试")
                    delay(100)
                }
                server = bound!!
                sessionCodeartsPort = server.boundPort
                codeartsSession = CodeartsLoginState()
                codeartsSession!!.authorizeUrl(server.boundPort)
            }
            else -> return Pair("", "该渠道不是回调登录型：$provider")
        }

        val deferred = CompletableDeferred<Map<String, String>>()
        pending = deferred
        onReady(url)

        val params = withTimeoutOrNull(CALLBACK_TIMEOUT_MS) { deferred.await() }
            ?: return Pair("", "等待超时（6 分钟），请重试")
        server!!.stop()

        when (provider) {
            "trae" -> traeFinish(params)
            "gemini" -> geminiFinish(params)
            "lobsterai" -> lobsteraiFinish(params)
            "codearts" -> codeartsFinish(params)
            else -> Pair("", "未知渠道")
        }
    } catch (e: Exception) {
        Pair("", e.message ?: "登录失败")
    } finally {
        server?.stop()
    }
}

// ==================== trae（token 直传） ====================

/** trae 登录期生成的设备身份（成功落 extra；失败丢弃，下次登录重新生成） */
private var sessionTraeIds: Pair<String, String> = "" to ""

private fun traeFinish(params: Map<String, String>): Pair<String, String> {
    val refresh = params["refreshToken"].orEmpty()
    if (refresh.isEmpty()) {
        // ⚠️ 回调并存 PKCE code/authCodeInfo 形态——明确报「暂不支持」，不含糊
        return if (params.containsKey("code") || params.containsKey("authCodeInfo"))
            Pair("", "回调为 PKCE 形态（code/authCodeInfo），暂不支持，请重试")
        else Pair("", "回调缺 refreshToken：$params")
    }
    val nick = try {
        val u = JSONObject(params["userInfo"].orEmpty().ifEmpty { "{}" })
        u.optString("ScreenName").ifEmpty { u.optString("nickname") }
            .ifEmpty { u.optString("NonPlainTextMobile") }
    } catch (_: Exception) { "" }
    // TraeChannel.refresh 签名是 Account——借临时账号走 ExchangeToken 换 access
    val tmp = AccountPool.Account(
        id = "trae-tmp", provider = "trae", nickname = "",
        accessToken = "", refreshToken = refresh,
    )
    val r = TraeChannel.refresh(tmp) ?: return Pair("", "refreshToken 换 token 失败（ExchangeToken）")
    val (machineId, deviceId) = sessionTraeIds
    val acc = AccountPool.Account(
        id = "trae-${System.currentTimeMillis().toString(16)}",
        provider = "trae",
        nickname = nick.ifEmpty { "TRAE" },
        accessToken = r.first,
        refreshToken = r.second, // refresh_token 每次轮换必须回写（refresh 内已处理）
        expiresAt = r.third,
    )
        .withExtra("machine_id", machineId) // 32hex 设备指纹：必须持久化（签到/对话身份用）
        .withExtra("device_id", deviceId)   // 16 位数字：各账号必须互不相同（签到互斥）
    AccountPool.save(AccountPool.load().filterNot { it.id == acc.id } + acc)
    return Pair(acc.nickname, "")
}

// ==================== gemini（code+state） ====================

/** gemini 会话：state（防 CSRF，回调比对）与端口（exchangeCode 需同值 redirect_uri） */
private var sessionGeminiState: String = ""
private var sessionGeminiPort: Int = 0

private fun geminiFinish(params: Map<String, String>): Pair<String, String> {
    val err = params["error"]
    if (!err.isNullOrEmpty()) return Pair("", "授权被拒绝或失败：$err")
    val code = params["code"].orEmpty()
    if (code.isEmpty()) return Pair("", "回调缺 code：$params")
    if (params["state"].orEmpty() != sessionGeminiState) return Pair("", "state 校验失败")
    val t = GeminiChannel.exchangeCode(code, sessionGeminiPort)
        ?: return Pair("", "换 token 失败（HTTP 错误或响应异常，详见日志）")
    // 昵称：userinfo 兜底（token 响应的 id_token 被 exchangeCode 丢弃；spec 允许 userinfo 静默失败，
    // 账号主键语义上用 sub，但凭据层只落 token——sub 提取留给对话链二期）
    var nick = "Gemini"
    try {
        val ui = DeviceCodeLogin.get(
            "https://www.googleapis.com/oauth2/v2/userinfo",
            mapOf("Authorization" to "Bearer ${t.first}"),
        )
        if (ui.ok) {
            val email = JSONObject(ui.body).optString("email")
            if (email.isNotEmpty()) nick = email.substringBefore('@')
        }
    } catch (_: Exception) { /* 静默失败照 spec */ }
    val acc = AccountPool.Account(
        id = "gemini-${System.currentTimeMillis().toString(16)}",
        provider = "gemini",
        nickname = nick,
        accessToken = t.first,
        refreshToken = t.second, // Google 偶尔轮换 refresh_token——parseTokenResponse 已回写
        expiresAt = t.third,
    )
    AccountPool.save(AccountPool.load().filterNot { it.id == acc.id } + acc)
    return Pair(acc.nickname, "")
}

// ==================== lobsterai（code+state，uuid/firstKeyfrom 永久持久化） ====================

private var sessionLobsterState: String = ""
private var sessionLobsterFirstKey: String = ""

private fun lobsteraiFinish(params: Map<String, String>): Pair<String, String> {
    val code = params["code"].orEmpty()
    if (code.isEmpty()) return Pair("", "回调缺 code：$params")
    if (params["state"].orEmpty() != sessionLobsterState) return Pair("", "state 校验失败")
    val t = LobsteraiChannel.exchange(code, sessionLobsterState, sessionLobsterFirstKey)
    if (t.err.isNotEmpty()) return Pair("", t.err)
    val acc = AccountPool.Account(
        id = "lobsterai-${System.currentTimeMillis().toString(16)}",
        provider = "lobsterai",
        nickname = t.nickname.ifEmpty { "LobsterAI" },
        accessToken = t.accessToken,
        refreshToken = t.refreshToken,
        expiresAt = t.expiresAt,
    )
        // ⚠️ uuid 与 firstKeyfrom 随凭据永久持久化，续期原样回传（从不更新，LobsteraiChannel.refresh 读 extra）
        .withExtra("uuid", sessionLobsterState)
        .withExtra("firstKeyfrom", sessionLobsterFirstKey)
        .withExtra("latestKeyfrom", sessionLobsterFirstKey)
    AccountPool.save(AccountPool.load().filterNot { it.id == acc.id } + acc)
    return Pair(acc.nickname, "")
}

// ==================== codearts（PKCE + DPoP） ====================

/** codearts 会话状态：PKCE + DPoP 密钥对在登录期生成，成功后私钥 JWK 落 extra */
private class CodeartsLoginState {
    val verifier: String
    val challenge: String
    val keyPair: java.security.KeyPair
    val ticketId: String = DeviceCodeLogin.randomUuid()

    init {
        // PKCE：verifier 48 字节 base64url（DeviceCodeLogin.pkce 即 48 字节实现），challenge=S256
        val (v, c) = DeviceCodeLogin.pkce()
        verifier = v
        challenge = c
        // DPoP：ES256 P-256 密钥对（java.security 原生，零依赖）
        val kpg = java.security.KeyPairGenerator.getInstance("EC")
        kpg.initialize(java.security.spec.ECGenParameterSpec("secp256r1"))
        keyPair = kpg.generateKeyPair()
    }

    /** 公钥 JWK（DPoP 头 jwk 字段 + 私钥 JWK 的底座） */
    fun publicJwk(): JSONObject {
        val pub = keyPair.public as java.security.interfaces.ECPublicKey
        return JSONObject()
            .put("kty", "EC")
            .put("crv", "P-256")
            .put("x", b64u(bigTo32(pub.w.affineX)))
            .put("y", b64u(bigTo32(pub.w.affineY)))
    }

    /** 私钥 JWK（多一个 d）——落 extra dpop_private_key_jwk，续期做 DPoP 用 */
    fun privateJwk(): JSONObject = JSONObject(publicJwk().toString())
        .put("d", b64u(bigTo32((keyPair.private as java.security.interfaces.ECPrivateKey).s)))

    /** Portal 授权 URL（规格书 1.1 模板；⚠️ method 是 SHA-256 不是 S256；**不加** auth_callback_url） */
    fun authorizeUrl(port: Int): String =
        "https://codearts.huaweicloud.com/portal/authorize" +
            "?theme=2&locale=zh-cn&uri_scheme=codearts-agent&client_id=codearts-agent" +
            "&port=$port&code_challenge=$challenge&code_challenge_method=SHA-256" +
            "&ticket_id=$ticketId&plugin-name=snap_AIIDE&plugin-version=5.2.0"

    /** DPoP 头：ES256 签名 JWS（payload {htm,htu,iat,jti}，header {alg:'ES256',typ:'dpop+jwt',jwk:公钥JWK}） */
    fun dpopHeader(htm: String, htu: String): String {
        val header = JSONObject().put("alg", "ES256").put("typ", "dpop+jwt").put("jwk", publicJwk())
        val payload = JSONObject()
            .put("htm", htm)
            .put("htu", htu)
            .put("iat", System.currentTimeMillis() / 1000)
            .put("jti", DeviceCodeLogin.randomUuid())
        val input = b64u(header.toString().toByteArray(Charsets.UTF_8)) + "." +
            b64u(payload.toString().toByteArray(Charsets.UTF_8))
        val sig = java.security.Signature.getInstance("SHA256withECDSA").run {
            initSign(keyPair.private)
            update(input.toByteArray(Charsets.US_ASCII))
            sign()
        }
        // ⚠️ 关键细节：SHA256withECDSA 出的是 DER 签名，须转 raw R||S（64 字节）再 base64url
        return input + "." + b64u(derSignatureToRaw(sig))
    }
}

private var codeartsSession: CodeartsLoginState? = null
private var sessionCodeartsPort: Int = 0

private fun codeartsFinish(params: Map<String, String>): Pair<String, String> {
    if (params.containsKey("secret")) return Pair("", "回调为旧 ticket 流程（?secret=），暂不支持，请重试")
    val code = params["code"].orEmpty()
    if (code.isEmpty()) return Pair("", "回调缺 code：$params")
    val st = codeartsSession ?: return Pair("", "会话状态丢失，请重试")
    val tokenUrl = "https://sts.cn-north-4.myhuaweicloud.com/v1/oauth2/tokens"
    val r = DeviceCodeLogin.postForm(
        tokenUrl,
        mapOf(
            "grant_type" to "authorization_code",
            "code" to code,
            "redirect_uri" to "http://127.0.0.1:$sessionCodeartsPort/oauth/callback",
            "client_id" to "codearts-agent",
        ),
        mapOf(
            "DPoP" to st.dpopHeader("POST", tokenUrl),
            "Accept" to "application/json",
        ),
    )
    if (!r.ok) return Pair("", "换取失败 HTTP ${r.code}：${r.body.take(160)}")
    val o = try { JSONObject(r.body) } catch (_: Exception) { return Pair("", "响应不是 JSON") }
    if (o.optString("error").isNotEmpty())
        return Pair("", "换取失败：${o.optString("error")} ${o.optString("error_description")}")
    val c = o.optJSONObject("credentials") ?: return Pair("", "响应缺 credentials")
    val ak = c.optString("access_key_id")
    if (ak.isEmpty()) return Pair("", "credentials 缺 access_key_id")
    // expires_at：服务端 ISO 串，解析失败回退 +24h（照插件 1.2）
    val expiresAt = runCatching {
        java.time.Instant.parse(c.optString("expiration")).toEpochMilli()
    }.getOrElse { System.currentTimeMillis() + 24 * 3600_000L }
    val acc = AccountPool.Account(
        id = "codearts-${System.currentTimeMillis().toString(16)}",
        provider = "codearts",
        nickname = c.optString("user_name").ifEmpty { "CodeArts" },
        accessToken = ak,          // ⚠️ accessToken=AK（SDK-HMAC-SHA256 签名链用），非 Bearer
        refreshToken = o.optString("refresh_token"),
        expiresAt = expiresAt,
    )
        .withExtra("access_key_id", ak)
        .withExtra("secret_access_key", c.optString("secret_access_key"))
        .withExtra("security_token", c.optString("security_token"))
        .withExtra("code_verifier", st.verifier)                       // 续期 form 必带
        .withExtra("dpop_private_key_jwk", st.privateJwk().toString()) // 续期 DPoP 用
    AccountPool.save(AccountPool.load().filterNot { it.id == acc.id } + acc)
    return Pair(acc.nickname, "")
}

// ==================== 小工具 ====================

/** 16 位纯数字设备号（首位 1-9，各账号互不相同） */
private fun randomDigits(len: Int): String {
    val rnd = java.security.SecureRandom()
    val sb = StringBuilder()
    for (i in 0 until len) sb.append(if (i == 0) (1 + rnd.nextInt(9)) else rnd.nextInt(10))
    return sb.toString()
}

/** base64url（无 padding） */
private fun b64u(b: ByteArray): String = android.util.Base64.encodeToString(
    b, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING,
)

/** BigInteger → 定长 32 字节（大端，前补零；JWK x/y/d 定长要求） */
private fun bigTo32(v: java.math.BigInteger): ByteArray {
    val raw = v.toByteArray()
    val out = ByteArray(32)
    val src = if (raw.size > 32) raw.copyOfRange(raw.size - 32, raw.size) else raw
    System.arraycopy(src, 0, out, 32 - src.size, src.size)
    return out
}

/** DER ECDSA 签名 → raw R||S（各 32 字节） */
private fun derSignatureToRaw(der: ByteArray): ByteArray {
    var i = 2 // der[0]=0x30（SEQUENCE），der[1]=总长
    i++      // 0x02（INTEGER 标记）
    val rLen = der[i].toInt() and 0xFF; i++
    val r = der.copyOfRange(i, i + rLen); i += rLen
    i++      // 0x02
    val sLen = der[i].toInt() and 0xFF; i++
    val s = der.copyOfRange(i, i + sLen)
    return bigTo32(java.math.BigInteger(1, r)) + bigTo32(java.math.BigInteger(1, s))
}

/** 拉起系统浏览器（主线程 post；外部 Context 需 NEW_TASK） */
private fun openBrowser(context: android.content.Context, url: String) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        android.os.Handler(android.os.Looper.getMainLooper()).post { context.startActivity(intent) }
    } catch (_: Exception) { /* 无浏览器时保留「打开授权页」按钮重试 */ }
}
