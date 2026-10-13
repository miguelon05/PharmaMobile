package pe.edu.upeu.pharmamobil.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.error.CredencialesInvalidasException
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.domain.usecase.LoginUseCase

data class LoginUiState(
    val usuario: String = "",
    val clave: String = "",
    val usuarioError: String? = null,
    val claveError: String? = null,
    val cargando: Boolean = false,
    val errorGeneral: String? = null
)

class LoginViewModel(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsuarioChange(usuario: String) {
        _uiState.update {
            it.copy(usuario = usuario, usuarioError = null, errorGeneral = null)
        }
    }

    fun onClaveChange(clave: String) {
        _uiState.update {
            it.copy(clave = clave, claveError = null, errorGeneral = null)
        }
    }

    fun iniciarSesion(onExito: (Sesion) -> Unit) {
        val u = _uiState.value.usuario.trim()
        val c = _uiState.value.clave.trim()

        var uError: String? = null
        var cError: String? = null

        if (u.isBlank()) uError = "El usuario es obligatorio"
        if (c.isBlank()) cError = "La contraseña es obligatoria"

        if (uError != null || cError != null) {
            _uiState.update {
                it.copy(usuarioError = uError, claveError = cError)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, errorGeneral = null) }
            println("LoginViewModel: Enviando credenciales para $u...")

            loginUseCase(Credenciales(usuario = u, clave = c)).fold(
                onSuccess = { sesion ->
                    println("LoginViewModel: Login exitoso para $u")
                    _uiState.update { it.copy(cargando = false) }
                    onExito(sesion)
                },
                onFailure = { fallo ->
                    println("LoginViewModel: Error en login: ${fallo.message}")
                    val msg = when (fallo) {
                        is CredencialesInvalidasException -> "Usuario o contraseña incorrectos"
                        is ErrorApiException -> "Error de servidor (${fallo.message})"
                        else -> fallo.message ?: "Error al conectar con el servidor"
                    }
                    _uiState.update { it.copy(cargando = false, errorGeneral = msg) }
                }
            )
        }
    }
}
