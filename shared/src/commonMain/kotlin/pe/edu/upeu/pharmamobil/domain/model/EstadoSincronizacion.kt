package pe.edu.upeu.pharmamobil.domain.model

data class FilaFallida(val id: Long, val nombre: String, val motivo: String)
data class EstadoSincronizacion(val pendientes: Int = 0, val fallidas: List<FilaFallida> = emptyList())
sealed interface ResultadoEnvio {
    data class Exito(val producto: Producto?) : ResultadoEnvio
    data class Transitorio(val motivo: String) : ResultadoEnvio
    data class Permanente(val motivo: String) : ResultadoEnvio
}
