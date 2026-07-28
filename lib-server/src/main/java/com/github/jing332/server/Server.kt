package com.github.jing332.server

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import kotlinx.serialization.json.Json

interface Server {
    fun start(wait: Boolean, onStarted: () -> Unit = {}, onShutdown: () -> Unit = {})
    fun stop()
}

internal val json by lazy {
    Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }
}

fun Application.installPlugins() {
    install(ContentNegotiation) {
        json(json)
    }

    install(CORS) {
        anyHost() // allow all domains
        allowNonSimpleContentTypes = true
        allowHeaders { true }
        allowMethod(io.ktor.http.HttpMethod.Options)
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Delete)
        allowMethod(io.ktor.http.HttpMethod.Patch)
    }

    install(StatusPages) {
        exception<Throwable> { call: ApplicationCall, cause: Throwable ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
}