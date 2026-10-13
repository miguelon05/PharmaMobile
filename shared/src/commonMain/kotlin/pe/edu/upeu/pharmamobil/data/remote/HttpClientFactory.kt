package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
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
import pe.edu.upeu.pharmamobil.domain.platform.AlmacenSeguro

fun crearHttpClient(
    engine: HttpClientEngine,
    config: ApiConfig,
    almacenSeguro: AlmacenSeguro
): HttpClient = HttpClient(engine) {
    expectSuccess = true
    followRedirects = false // No permitir que un servidor redirija este cliente a HTTP.

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
    }

    install(Auth) {
        bearer {
            loadTokens {
                val token = almacenSeguro.leerToken()
                if (!token.isNullOrBlank()) {
                    BearerTokens(accessToken = token, refreshToken = "")
                } else {
                    null
                }
            }
            sendWithoutRequest { request ->
                val token = almacenSeguro.leerToken()
                val isLogin = request.url.pathSegments.contains("login") ||
                        request.url.toString().contains("auth/login")
                !token.isNullOrBlank() && !isLogin
            }
        }
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
