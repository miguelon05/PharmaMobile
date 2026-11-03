package pe.edu.upeu.pharmamobil.data.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics
import pe.edu.upeu.pharmamobil.domain.port.Telemetria

class TelemetriaFirebase : Telemetria {
    private fun seguros(datos: Map<String, String>) = datos.filterKeys { it in setOf("pantalla", "operacion", "estado") }
    override fun registrarError(e: Throwable, contexto: Map<String, String>) {
        runCatching {
            seguros(contexto).forEach { (clave, valor) -> Firebase.crashlytics.setCustomKey(clave, valor) }
            // Evita enviar mensajes o causas que puedan contener datos del servidor o del usuario.
            Firebase.crashlytics.recordException(IllegalStateException(e::class.simpleName ?: "Error"))
        }
    }
    override fun registrarEvento(nombre: String, datos: Map<String, String>) {
        runCatching { Firebase.crashlytics.log(nombre + " " + seguros(datos).toString()) }
    }
}
