package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.local.aDominio
import pe.edu.upeu.pharmamobil.data.remote.ejecutarLlamada
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.ResultadoEnvio
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class ProductoRepositorioOffline(
    private val local: ProductoLocal,
    private val remoto: ProductoRepository,
    private val ahora: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() }
) : ProductoRepository {
    private val mutex = Mutex()
    override fun observarEstadoSincronizacion() = local.observarEstado()
    override suspend fun reintentar(id: Long) = withContext(Dispatchers.Default) { mutex.withLock { local.reintentar(id); Unit } }
    override fun observarUltimaSincronizacion() = local.observarUltimaSincronizacion()
    override fun observar() = local.observar()
    override suspend fun listar() = withContext(Dispatchers.Default) { local.listar() }
    override suspend fun obtener(id: Long) = withContext(Dispatchers.Default) { local.obtener(id) }
    override suspend fun registrar(producto: Producto): Producto = withContext(Dispatchers.Default) {
        mutex.withLock { local.guardar(producto, nuevo = true) }
    }
    override suspend fun actualizar(producto: Producto): Producto = withContext(Dispatchers.Default) {
        mutex.withLock { local.guardar(producto) }
    }
    override suspend fun eliminar(id: Long) = withContext(Dispatchers.Default) {
        mutex.withLock { local.eliminar(id) }
    }
    override suspend fun sincronizar() = withContext(Dispatchers.Default) {
        mutex.withLock {
            local.pendientesPorEnviar(ahora()).forEach { fila ->
                val resultado = ejecutarLlamada {
                    when {
                        fila.eliminado == 1L -> {
                            try { ejecutarLlamada { remoto.eliminar(fila.id) }.getOrThrow() } catch (e: ErrorApiException) {
                                if (e.error != ErrorApi.NoEncontrado) throw e
                            }
                            null
                        }
                        fila.id < 0 -> remoto.registrar(fila.aDominio())
                        else -> remoto.actualizar(fila.aDominio())
                    }
                }
                when (val envio = resultado.fold({ ResultadoEnvio.Exito(it) }, { it.aResultadoEnvio() })) {
                    is ResultadoEnvio.Exito -> local.confirmar(fila.id, envio.producto)
                    is ResultadoEnvio.Permanente -> local.marcarFallida(fila.id, envio.motivo)
                    is ResultadoEnvio.Transitorio -> {
                        if (fila.id < 0) local.marcarFallida(fila.id,
                            "Creación sin confirmar. Revisa el servidor antes de reintentar para evitar duplicados.")
                        else local.sumarIntento(fila.id, envio.motivo,
                            ahora() + (1_000L shl fila.intentos.toInt().coerceAtMost(4)))
                    }
                }
            }
            local.reemplazarSincronizados(ejecutarLlamada { remoto.listar() }.getOrThrow())
            local.registrarSincronizacion(ahora())
        }
    }
}

private fun Throwable.aResultadoEnvio(): ResultadoEnvio {
    val error = (this as? ErrorApiException)?.error
    return when (error) {
        ErrorApi.SinConexion -> ResultadoEnvio.Transitorio("Sin conexión")
        ErrorApi.TiempoAgotado -> ResultadoEnvio.Transitorio("Tiempo de espera agotado")
        ErrorApi.Servidor -> ResultadoEnvio.Transitorio("Servicio no disponible")
        is ErrorApi.Validacion -> ResultadoEnvio.Permanente(error.porCampo.values.joinToString("; ").ifBlank { "Validación rechazada" })
        is ErrorApi.Conflicto -> ResultadoEnvio.Permanente(error.mensaje)
        ErrorApi.NoEncontrado -> ResultadoEnvio.Permanente("El registro ya no existe")
        null -> ResultadoEnvio.Permanente(when (this) {
            is pe.edu.upeu.pharmamobil.domain.error.SesionExpiradaException -> "Sesión vencida: inicia sesión"
            is pe.edu.upeu.pharmamobil.domain.error.SinPermisoException -> "Sin privilegios para esta operación"
            else -> "Respuesta inesperada: revisa la operación"
        })
    }
}
