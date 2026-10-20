package pe.edu.upeu.pharmamobil.data.repository

import kotlin.io.encoding.Base64
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProviders
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import pe.edu.upeu.pharmamobil.data.remote.AuthApi
import pe.edu.upeu.pharmamobil.data.remote.dto.LoginRequestDto
import pe.edu.upeu.pharmamobil.data.remote.dto.LoginResponseDto
import pe.edu.upeu.pharmamobil.data.remote.dto.UsuarioActualDto
import pe.edu.upeu.pharmamobil.data.remote.ejecutarLlamada
import pe.edu.upeu.pharmamobil.domain.error.SesionExpiradaException
import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.domain.platform.AlmacenSeguro
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository

class AuthRepositorioRest(
    private val api: AuthApi,
    private val almacenSeguro: AlmacenSeguro,
    private val httpClient: HttpClient
) : AuthRepository {

    override suspend fun login(credenciales: Credenciales): Result<Sesion> {
        return ejecutarLlamada {
            val response = api.login(
                LoginRequestDto(
                    username = credenciales.usuario.trim(),
                    password = credenciales.clave
                )
            )
            almacenSeguro.guardarToken(response.token)
            limpiarCacheBearer()
            response.toDomain().also { almacenSeguro.guardarSesion(Json.encodeToString(it)) }
        }
    }

    override suspend fun obtenerSesionGuardada(): Sesion? {
        val token = almacenSeguro.leerToken()
        if (token.isNullOrBlank()) return null

        return ejecutarLlamada {
            api.me().toDomain(token).also { almacenSeguro.guardarSesion(Json.encodeToString(it)) }
        }.getOrElse { error ->
            if (error is SesionExpiradaException) {
                almacenSeguro.borrarToken()
                almacenSeguro.borrarSesion()
                limpiarCacheBearer()
            }
            val tipo = (error as? ErrorApiException)?.error
            if (tipo == ErrorApi.SinConexion || tipo == ErrorApi.TiempoAgotado || tipo == ErrorApi.Servidor) {
                almacenSeguro.leerSesion()?.let { guardada ->
                    runCatching { Json.decodeFromString<Sesion>(guardada) }.getOrNull()
                }?.takeIf { it.token == token && tokenVigente(token) }
            } else null
        }
    }

    override suspend fun cerrarSesion() {
        almacenSeguro.borrarToken()
        almacenSeguro.borrarSesion()
        limpiarCacheBearer()
    }

    private fun tokenVigente(token: String): Boolean = runCatching {
        val payload = token.split(".")[1]
        val texto = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).decode(payload).decodeToString()
        val expiracion = Json.parseToJsonElement(texto).jsonObject["exp"]?.jsonPrimitive?.longOrNull
        expiracion != null && expiracion > kotlin.time.Clock.System.now().epochSeconds
    }.getOrDefault(false)

    private fun limpiarCacheBearer() {
        try {
            httpClient.authProviders.filterIsInstance<BearerAuthProvider>().forEach {
                it.clearToken()
            }
        } catch (_: Exception) {}
    }

    private fun LoginResponseDto.toDomain(): Sesion = toDomain(token)

    private fun UsuarioActualDto.toDomain(token: String): Sesion = Sesion(
        token = token,
        usuario = username ?: usuario ?: "",
        nombreCompleto = nombreCompleto ?: username ?: usuario ?: "Usuario",
        permisos = permisos.toSet()
    )

    private fun LoginResponseDto.toDomain(token: String): Sesion = Sesion(
        token = token,
        usuario = username ?: "",
        nombreCompleto = nombreCompleto ?: username ?: "Usuario",
        permisos = permisos.toSet()
    )
}
