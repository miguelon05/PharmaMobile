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
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class ProductoRepositorioOffline(private val local: ProductoLocal, private val remoto: ProductoRepository) : ProductoRepository {
    private val mutex = Mutex()
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
            local.pendientes().forEach { fila ->
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
                }.getOrThrow()
                local.confirmar(fila.id, resultado)
            }
            local.reemplazarSincronizados(ejecutarLlamada { remoto.listar() }.getOrThrow())
        }
    }
}
