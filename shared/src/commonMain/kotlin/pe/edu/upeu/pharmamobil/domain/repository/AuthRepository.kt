package pe.edu.upeu.pharmamobil.domain.repository

import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.model.Sesion

interface AuthRepository {
    suspend fun login(credenciales: Credenciales): Result<Sesion>
    suspend fun obtenerSesionGuardada(): Sesion?
    suspend fun cerrarSesion()
}
