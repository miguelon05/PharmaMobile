package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository

class LoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(credenciales: Credenciales): Result<Sesion> {
        if (credenciales.usuario.isBlank() || credenciales.clave.isBlank()) {
            return Result.failure(IllegalArgumentException("El usuario y la contraseña son obligatorios"))
        }
        return authRepository.login(credenciales)
    }
}
