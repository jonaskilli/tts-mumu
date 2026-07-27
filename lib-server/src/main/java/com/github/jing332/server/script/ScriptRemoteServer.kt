package com.github.jing332.server.script

import com.github.jing332.common.LogEntry
import com.github.jing332.server.BaseCallback
import com.github.jing332.server.Server
import com.github.jing332.server.installPlugins
import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.util.getOrFail
import java.net.BindException

class ScriptRemoteServer(
    val port: Int,
    val callback: Callback,
) : Server {
    companion object {
        private val jsContentType = ContentType.parse("text/javascript")
        private const val BASE_PATH = "api/sync"
    }

    private val ktor by lazy {
        embeddedServer(Netty, port = port) {
            installPlugins()
            routing {
                get("$BASE_PATH/pull") {
                    call.respondText(contentType = jsContentType, text = callback.pull())
                }

                post("$BASE_PATH/push") {
                    val code = call.receiveText()
                    callback.push(code)
                    call.respondText("")
                }

                suspend fun RoutingContext.action() {
                    val name = call.parameters["name"] ?: call.parameters.getOrFail("action")
                    callback.action(name)
                    call.respondText("")
                }

                get("$BASE_PATH/action") { action() }
                post("$BASE_PATH/action") { action() }

                get("$BASE_PATH/log") {
                    call.respond(callback.log())
                }
            }
        }
    }


    @Throws(BindException::class)
    override fun start(wait: Boolean, onStarted: () -> Unit, onStopped: () -> Unit) {
        ktor.environment.log.info("Start script sync server on port $port")
        ktor.application.monitor.subscribe(ApplicationStarted) { application ->
            onStarted()
        }
        ktor.application.monitor.subscribe(ApplicationStopped) { application ->
            onStopped()
        }
        ktor.start(wait)

    }

    override fun stop() {
        ktor.stop(100, 500)
    }

    interface Callback : BaseCallback {
        fun pull(): String
        fun push(code: String)
        fun action(name: String)
        fun log(): List<LogEntry>
    }

}