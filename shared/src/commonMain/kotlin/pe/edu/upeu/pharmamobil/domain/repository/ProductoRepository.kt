package pe.edu.upeu.pharmamobil.domain.repository

import pe.edu.upeu.pharmamobil.domain.model.Producto

interface ProductoRepository {
    fun observarUltimaSincronizacion(): kotlinx.coroutines.flow.Flow<Long?> = kotlinx.coroutines.flow.flowOf(null)
    fun observar(): kotlinx.coroutines.flow.Flow<List<Producto>>
    fun observarEstadoSincronizacion(): kotlinx.coroutines.flow.Flow<pe.edu.upeu.pharmamobil.domain.model.EstadoSincronizacion> = kotlinx.coroutines.flow.flowOf(pe.edu.upeu.pharmamobil.domain.model.EstadoSincronizacion())
    suspend fun reintentar(id: Long) {}
    suspend fun sincronizar()

    suspend fun listar(): List<Producto>
    suspend fun obtener(id: Long): Producto
    suspend fun registrar(producto: Producto): Producto
    suspend fun actualizar(producto: Producto): Producto
    suspend fun eliminar(id: Long)
}
