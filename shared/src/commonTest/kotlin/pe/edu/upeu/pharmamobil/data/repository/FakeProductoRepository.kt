package pe.edu.upeu.pharmamobil.data.repository

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class FakeProductoRepository(
    private val productos: MutableList<Producto> = mutableListOf()
) : ProductoRepository {

    var fallaAlRegistrar: Throwable? = null
    var fallaAlListar: Throwable? = null

    private var siguienteId = 1L

    override suspend fun registrar(producto: Producto): Producto {
        fallaAlRegistrar?.let { throw it }
        val guardado = producto.copy(id = siguienteId++)
        productos.add(guardado)
        return guardado
    }

    override suspend fun listar(): List<Producto> {
        fallaAlListar?.let { throw it }
        return productos.toList()
    }

    override suspend fun obtener(id: Long): Producto {
        return productos.first { it.id == id }
    }

    override suspend fun actualizar(producto: Producto): Producto {
        val index = productos.indexOfFirst { it.id == producto.id }
        if (index >= 0) {
            productos[index] = producto
            return producto
        }
        throw IllegalArgumentException("Producto no encontrado")
    }

    override suspend fun eliminar(id: Long) {
        productos.removeAll { it.id == id }
    }
}
