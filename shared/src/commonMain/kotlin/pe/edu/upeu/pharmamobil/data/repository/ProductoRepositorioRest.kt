package pe.edu.upeu.pharmamobil.data.repository

import pe.edu.upeu.pharmamobil.data.mapper.toDomain
import pe.edu.upeu.pharmamobil.data.mapper.toRequest
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.CategoriaApi
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class ProductoRepositorioRest(
    private val api: ProductoApi,
    private val categoriaPorDefecto: Long,
    private val categoriaApi: CategoriaApi? = null
) : ProductoRepository {

    private val cambios = kotlinx.coroutines.flow.MutableStateFlow<List<Producto>>(emptyList())
    override fun observar() = cambios
    override suspend fun sincronizar() { cambios.value = listar() }

    override suspend fun listar(): List<Producto> {
        val productos = mutableListOf<Producto>()
        var pagina = 0
        while (true) {
            val respuesta = api.listar(pagina = pagina, tamanio = 20)
            productos += respuesta.contenido.filter { it.estado }.map { it.toDomain() }
            if (respuesta.ultima || respuesta.contenido.isEmpty()) break
            pagina++
        }
        return productos
    }

    override suspend fun obtener(id: Long): Producto =
        api.obtener(id).toDomain()

    override suspend fun registrar(producto: Producto): Producto =
        api.crear(producto.toRequest(categoriaActiva())).toDomain()

    override suspend fun actualizar(producto: Producto): Producto =
        api.actualizar(producto.id, producto.toRequest(producto.categoriaId ?: categoriaActiva())).toDomain()

    override suspend fun eliminar(id: Long) {
        api.eliminar(id)
    }

    private suspend fun categoriaActiva(): Long {
        val categorias = categoriaApi?.listar()
            ?: return categoriaPorDefecto
        val activas = categorias.filter { it.estado != false }
        return activas.firstOrNull { it.id == categoriaPorDefecto }?.id
            ?: activas.firstOrNull()?.id
            ?: throw IllegalStateException("No hay categorías activas. Registra una categoría antes de guardar productos.")
    }
}
