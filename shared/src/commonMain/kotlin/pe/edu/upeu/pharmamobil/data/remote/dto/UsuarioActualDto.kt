package pe.edu.upeu.pharmamobil.data.remote.dto

import kotlinx.serialization.Serializable

/** Respuesta de GET /auth/me. El servidor no vuelve a enviar el token. */
@Serializable
data class UsuarioActualDto(
    val username: String? = null,
    val usuario: String? = null,
    val nombreCompleto: String? = null,
    val roles: List<String> = emptyList(),
    val permisos: List<String> = emptyList()
)
