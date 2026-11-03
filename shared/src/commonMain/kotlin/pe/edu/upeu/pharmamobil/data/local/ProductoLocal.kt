package pe.edu.upeu.pharmamobil.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.model.Producto

class ProductoLocal(base: PharmaDb, private val dispatcher: CoroutineDispatcher = Dispatchers.Default) {
    private val q = base.productoQueries
    private val sincronizacion = base.sincronizacionQueries
    fun observarUltimaSincronizacion() = sincronizacion.ultima().asFlow().mapToList(dispatcher).map { it.firstOrNull() }
    fun ultimaSincronizacion(): Long? = sincronizacion.ultima().executeAsOneOrNull()
    fun registrarSincronizacion(instante: Long) { sincronizacion.guardar(instante) }
    fun observar() = q.listarTodos().asFlow().mapToList(dispatcher).map { filas -> filas.map { it.aDominio() } }
    fun listar() = q.listarTodos().executeAsList().map { it.aDominio() }
    fun pendientesPorEnviar(ahora: Long) = q.pendientesPorEnviar(5, ahora).executeAsList()
    fun sumarIntento(id: Long, error: String, proximo: Long) = q.sumarIntento(error, proximo, id)
    fun marcarFallida(id: Long, error: String) = q.marcarFallida(5, error, id)
    fun reintentar(id: Long) = q.reintentar(id)
    fun observarEstado() = kotlinx.coroutines.flow.combine(
        q.contarPendientes(5).asFlow().mapToList(dispatcher),
        q.listarFallidas(5).asFlow().mapToList(dispatcher)
    ) { pendientes, fallidas ->
        pe.edu.upeu.pharmamobil.domain.model.EstadoSincronizacion(
            pendientes.first().toInt(), fallidas.map {
                pe.edu.upeu.pharmamobil.domain.model.FilaFallida(it.id, it.nombre, it.ultimoError.orEmpty())
            }
        )
    }
    fun pendientes() = q.listarPendientes().executeAsList()
    fun obtener(id: Long) = q.obtener(id).executeAsOne().aDominio()
    fun guardar(producto: Producto, nuevo: Boolean = false): Producto = q.transactionWithResult {
        val id = if (nuevo) minOf(q.siguienteIdLocal().executeAsOne(), 0L) - 1 else producto.id
        producto.copy(id = id, pendiente = true).also { insertar(it, 1, 0) }
    }
    fun eliminar(id: Long) = q.transaction {
        if (id < 0) q.borrar(id) else insertar(obtener(id), 1, 1)
    }
    fun confirmar(idLocal: Long, remoto: Producto?) = q.transaction {
        q.borrar(idLocal)
        remoto?.let { insertar(it, 0, 0) }
    }
    fun reemplazarSincronizados(productos: List<Producto>) = q.transaction {
        q.borrarSincronizados()
        productos.forEach { p ->
            if (q.obtener(p.id).executeAsOneOrNull()?.pendiente != 1L) insertar(p, 0, 0)
        }
    }
    private fun insertar(p: Producto, pendiente: Long, eliminado: Long) =
        q.insertar(p.id, p.nombre, p.precio, p.stock.toLong(), p.categoriaId, pendiente, eliminado)
}

internal fun pe.edu.upeu.pharmamobil.db.Producto.aDominio() =
    Producto(id, nombre, precio, stock.toInt(), categoriaId, pendiente == 1L)
