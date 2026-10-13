package pe.edu.upeu.pharmamobil.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProviders
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import pe.edu.upeu.pharmamobil.data.remote.AuthApi
import pe.edu.upeu.pharmamobil.data.remote.dto.LoginRequestDto
import pe.edu.upeu.pharmamobil.data.remote.dto.LoginResponseDto
import pe.edu.upeu.pharmamobil.data.remote.ejecutarLlamada
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
                    password = credenciales.clave.trim()
                )
            )
            almacenSeguro.guardarToken(response.token)
            limpiarCacheBearer()
            response.toDomain()
        }
    }

    override suspend fun obtenerSesionGuardada(): Sesion? {
        val token = almacenSeguro.leerToken()
        if (token.isNullOrBlank()) return null

        return ejecutarLlamada {
            api.me().toDomain()
        }.getOrElse {
            almacenSeguro.borrarToken()
            limpiarCacheBearer()
            null
        }
    }

    override suspend fun cerrarSesion() {
        almacenSeguro.borrarToken()
        limpiarCacheBearer()
    }

    private fun limpiarCacheBearer() {
        try {
            httpClient.authProviders.filterIsInstance<BearerAuthProvider>().forEach {
                it.clearToken()
            }
        } catch (_: Exception) {}
    }

    private fun LoginResponseDto.toDomain(): Sesion {
        val permisosSet = mutableSetOf<String>()
        permisosSet.addAll(permisos)

        roles.forEach { role ->
            when (role.uppercase()) {
                "ROLE_ADMIN", "ADMIN" -> {
                    permisosSet.add("PRODUCTOS_CREAR")
                    permisosSet.add("PRODUCTOS_EDITAR")
                    permisosSet.add("PRODUCTOS_ELIMINAR")
                    permisosSet.add("CLIENTES_LEER")
                    permisosSet.add("CLIENTES_CREAR")
                }
                "ROLE_ALMACENERO", "ALMACENERO", "ALMACEN" -> {
                    permisosSet.add("PRODUCTOS_CREAR")
                    permisosSet.add("PRODUCTOS_EDITAR")
                    permisosSet.add("PRODUCTOS_ELIMINAR")
                }
                "ROLE_VENDEDOR", "VENDEDOR" -> {
                    permisosSet.add("CLIENTES_LEER")
                    permisosSet.add("CLIENTES_CREAR")
                }
            }
        }

        return Sesion(
            token = token,
            usuario = username ?: "",
            nombreCompleto = nombreCompleto ?: username ?: "Usuario",
            permisos = permisosSet
        )
    }
}
