package pe.edu.upeu.pharmamobil.domain.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Notifica a la sesión cuando cualquier solicitud autenticada recibe 401. */
object SessionExpirationEvents {
    private val mutableEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = mutableEvents.asSharedFlow()

    fun notifyExpired() {
        mutableEvents.tryEmit(Unit)
    }
}
