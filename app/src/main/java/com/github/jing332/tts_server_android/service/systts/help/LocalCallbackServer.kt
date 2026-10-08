package com.github.jing332.tts_server_android.service.systts.help

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder

/**
 * 极简本地 HTTP 回调服务器（10-09 回调登录批，四个本地回调型渠道共用）。
 *
 * 手写 java.net.ServerSocket，零依赖。只干一件事：起在 {port} 上，等浏览器回调
 * GET {pathPrefix}?k=v&...，解析出 query 参数回调 listener，给浏览器回一句
 * 「授权完成，可关闭此页」。
 *
 * ⚠️ 双栈绑定（gemini 硬要求）：浏览器常把 localhost 解析成 ::1，只绑 0.0.0.0 会收不到。
 * 先试 `::`（IPv6 通配，内核双栈时 IPv4-mapped 一并接住），失败退 0.0.0.0。
 *
 * 端口选取（规格书 §0.1/§1.1/§5.1/§12.1）：
 *  - codearts：portal 要求回调端口 ≥10000（低端口拒绝），随机 [10000,65535]；
 *  - gemini：RFC 8252 loopback 任意端口合法，但 redirect_uri 逐字一致 + 主机名必须 localhost；
 *  - lobsterai：任意端口，redirect_uri 百分号编码后逐字一致；
 *  - trae：固定 18080。
 */
class LocalCallbackServer(
    private val port: Int,
    private val pathPrefix: String,
    private val timeoutSeconds: Int,
    private val listener: (Map<String, String>) -> Unit,
) {
    @Volatile private var server: ServerSocket? = null
    @Volatile private var thread: Thread? = null

    /** 实际绑定成功的端口（port=0 随机时用它回读；未 start 前 = 传入值） */
    var boundPort: Int = port
        private set

    /** 起服务：成功返回 true（重复 start 幂等返回当前状态）；绑定失败返回 false */
    fun start(): Boolean {
        if (server != null && server!!.isBound && !server!!.isClosed) return true
        val ss = try { bindDualStack() } catch (e: Exception) { return false }
        boundPort = ss.localPort
        server = ss
        val t = Thread({
            while (!Thread.currentThread().isInterrupted) {
                val sock = try { ss.accept() } catch (e: Exception) { break } // stop() 关闭即退出
                handle(sock)
            }
        }, "LocalCallbackServer-$port")
        t.isDaemon = true
        t.start()
        thread = t
        return true
    }

    /** 停服：关 socket + 中断线程，幂等 */
    fun stop() {
        try { server?.close() } catch (_: Exception) {}
        server = null
        thread?.interrupt()
        thread = null
    }

    /** 双栈绑定：先 `::`（通配含 IPv4-mapped），内核不支持再退 0.0.0.0 */
    private fun bindDualStack(): ServerSocket =
        try {
            ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress("::", port)) }
        } catch (_: Exception) {
            ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress("0.0.0.0", port)) }
        }

    /** 单连接处理：只读请求行，路径匹配即回 HTML + 回调（浏览器连接短平快，逐连接关） */
    private fun handle(sock: Socket) {
        Thread({
            try {
                sock.soTimeout = timeoutSeconds * 1000
                val reader = BufferedReader(InputStreamReader(sock.getInputStream(), Charsets.ISO_8859_1))
                val requestLine = reader.readLine() ?: return@Thread
                val parts = requestLine.split(" ")
                if (parts.size < 2 || parts[0] != "GET") { respond(sock, 405, "Method Not Allowed"); return@Thread }
                val target = parts[1]
                val qIdx = target.indexOf('?')
                val path = if (qIdx >= 0) target.substring(0, qIdx) else target
                val query = if (qIdx >= 0) target.substring(qIdx + 1) else ""
                if (path != pathPrefix && !path.startsWith(pathPrefix)) {
                    respond(sock, 404, "Not Found")
                    return@Thread
                }
                val params = LinkedHashMap<String, String>()
                for (pair in query.split('&')) {
                    if (pair.isEmpty()) continue
                    val eq = pair.indexOf('=')
                    val k = URLDecoder.decode(if (eq >= 0) pair.substring(0, eq) else pair, "UTF-8")
                    val v = if (eq >= 0) URLDecoder.decode(pair.substring(eq + 1), "UTF-8") else ""
                    params[k] = v
                }
                respond(sock, 200, SUCCESS_HTML)
                listener(params)
            } catch (_: Exception) {
                // 浏览器提前断开/读超时等：单连接失败不影响整体等待
            } finally {
                try { sock.close() } catch (_: Exception) {}
            }
        }, "LocalCallback-conn").start()
    }

    /** 最小 HTTP 响应（Connection: close，Content-Length 必带，浏览器行为才干净） */
    private fun respond(sock: Socket, code: Int, text: String) {
        try {
            val body = "<html><head><meta charset=\"utf-8\"></head><body>$text</body></html>"
            val bytes = body.toByteArray(Charsets.UTF_8)
            val head = "HTTP/1.1 $code OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
            sock.getOutputStream().apply {
                write(head.toByteArray(Charsets.ISO_8859_1))
                write(bytes)
                flush()
            }
        } catch (_: Exception) {}
    }

    companion object {
        private const val SUCCESS_HTML = "<h2>授权完成</h2><p>凭据已回传到应用，可关闭此页返回。</p>"

        /**
         * 回调端口选取：
         *  - minPort=10000（codearts，规格书 §1.1 portal 低端口拒绝）→ 随机 [10000,65535]；
         *  - 其它渠道传任意下限（如 49152 起）也走随机，redirect_uri 拼进 URL 前先确定端口。
         */
        fun randomPort(minPort: Int = 10000): Int =
            java.security.SecureRandom().nextInt(65536 - minPort) + minPort
    }
}
