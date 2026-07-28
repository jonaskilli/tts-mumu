package com.github.jing332.server

import io.ktor.events.Events
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.netty.NettyApplicationEngine


/**
 * An [ApplicationEngineFactory] providing a Netty-based [ApplicationEngine]
 */
internal object CustomNetty : ApplicationEngineFactory<NettyApplicationEngine, NettyApplicationEngine.Configuration> {

    override fun configuration(
        configure: NettyApplicationEngine.Configuration.() -> Unit,
    ): NettyApplicationEngine.Configuration {

        return NettyApplicationEngine.Configuration().apply {
             maxInitialLineLength = 8192
        }.apply(configure)
    }

    override fun create(
        environment: ApplicationEnvironment,
        monitor: Events,
        developmentMode: Boolean,
        configuration: NettyApplicationEngine.Configuration,
        applicationProvider: () -> Application,
    ): NettyApplicationEngine {
        return NettyApplicationEngine(environment, monitor, developmentMode, configuration, applicationProvider)
    }
}
