package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.data.remote.ejecutarLlamada
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class ListarProductosUseCase(
    private val productoRepository: ProductoRepository
) {

    fun observarEstadoSincronizacion() = productoRepository.observarEstadoSincronizacion()
    suspend fun reintentar(id: Long) = productoRepository.reintentar(id)
    fun observar() = productoRepository.observar()
    fun observarUltimaSincronizacion() = productoRepository.observarUltimaSincronizacion()
    suspend fun sincronizar() = ejecutarLlamada { SincronizarUseCase(productoRepository)() }

    suspend operator fun invoke(): Result<List<Producto>> = ejecutarLlamada {
        productoRepository.listar()
    }
}
