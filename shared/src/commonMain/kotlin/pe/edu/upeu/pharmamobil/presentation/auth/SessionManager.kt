package pe.edu.upeu.pharmamobil.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.domain.session.SessionExpirationEvents
import pe.edu.upeu.pharmamobil.domain.usecase.CerrarSesionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObtenerSesionUseCase

sealed interface EstadoSesion {
    data object Cargando : EstadoSesion
    data object NoAutenticado : EstadoSesion
    data class Autenticado(val sesion: Sesion) : EstadoSesion
}

class SessionManager(
    private val obtenerSesionUseCase: ObtenerSesionUseCase,
    private val cerrarSesionUseCase: CerrarSesionUseCase
) : ViewModel() {

    private val _estadoSesion = MutableStateFlow<EstadoSesion>(EstadoSesion.Cargando)
    val estadoSesion: StateFlow<EstadoSesion> = _estadoSesion.asStateFlow()

    private val _mensajeNotificacion = MutableStateFlow<String?>(null)
    val mensajeNotificacion: StateFlow<String?> = _mensajeNotificacion.asStateFlow()

    init {
        viewModelScope.launch {
            SessionExpirationEvents.events.collect {
                expirarSesion()
            }
        }
        verificarSesionInicial()
    }

    fun verificarSesionInicial() {
        viewModelScope.launch {
            val sesion = obtenerSesionUseCase()
            if (sesion != null) {
                _estadoSesion.value = EstadoSesion.Autenticado(sesion)
            } else {
                _estadoSesion.value = EstadoSesion.NoAutenticado
            }
        }
    }

    fun onSesionIniciada(sesion: Sesion) {
        _mensajeNotificacion.value = null
        _estadoSesion.value = EstadoSesion.Autenticado(sesion)
    }

    fun expirarSesion(mensaje: String = "Tu sesión ha expirado. Por favor, vuelve a iniciar sesión.") {
        viewModelScope.launch {
            cerrarSesionUseCase()
            _mensajeNotificacion.value = mensaje
            _estadoSesion.value = EstadoSesion.NoAutenticado
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            cerrarSesionUseCase()
            _mensajeNotificacion.value = null
            _estadoSesion.value = EstadoSesion.NoAutenticado
        }
    }

    fun consumirNotificacion() {
        _mensajeNotificacion.value = null
    }
}
