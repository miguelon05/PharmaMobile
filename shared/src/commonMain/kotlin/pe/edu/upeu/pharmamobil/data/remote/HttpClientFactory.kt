package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun crearHttpClient(engine: HttpClientEngine, config: ApiConfig): HttpClient = HttpClient(engine) {
    expectSuccess = true
    followRedirects = false // No permitir que un servidor redirija este cliente a HTTP.
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
    }
    install(Logging) {
        level = if (config.loggingEnabled) LogLevel.HEADERS else LogLevel.NONE
        logger = object : Logger {
            override fun log(message: String) { println("PharmaKtor: $message") }
        }
        sanitizeHeader { it.equals(HttpHeaders.Authorization, true) || it.equals(HttpHeaders.Cookie, true) }
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 15_000
    }
    defaultRequest {
        url(config.baseUrl)
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.Json)
    }
}
