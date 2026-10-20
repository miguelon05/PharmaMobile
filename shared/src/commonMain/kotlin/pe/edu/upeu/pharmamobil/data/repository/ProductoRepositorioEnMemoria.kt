package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class ProductoRepositorioEnMemoria : ProductoRepository {

    private val candado = Mutex()
    private val productos = mutableListOf<Producto>()
    private val cambios = kotlinx.coroutines.flow.MutableStateFlow(productos.toList())
    override fun observar() = cambios
    override suspend fun sincronizar() { cambios.value = listar() }
    private var siguienteId = 1L

    override suspend fun registrar(producto: Producto): Producto {
        delay(RETARDO_REGISTRO_MS)
        return candado.withLock {
            val guardado = producto.copy(id = siguienteId++)
            productos.add(guardado)
        cambios.value = productos.toList()
            guardado
        }
    }

    override suspend fun listar(): List<Producto> {
        delay(RETARDO_LISTADO_MS)
        return candado.withLock {
            productos.toList()
        }
    }

    override suspend fun obtener(id: Long): Producto {
        return candado.withLock {
            productos.first { it.id == id }
        }
    }

    override suspend fun actualizar(producto: Producto): Producto {
        return candado.withLock {
            val index = productos.indexOfFirst { it.id == producto.id }
            if (index >= 0) {
                productos[index] = producto
            cambios.value = productos.toList()
                producto
            } else {
                throw IllegalArgumentException("No encontrado")
            }
        }
    }

    override suspend fun eliminar(id: Long) {
        candado.withLock {
            productos.removeAll { it.id == id }
        cambios.value = productos.toList()
        }
    }

    private companion object {
        const val RETARDO_REGISTRO_MS = 400L
        const val RETARDO_LISTADO_MS = 600L
    }
}
