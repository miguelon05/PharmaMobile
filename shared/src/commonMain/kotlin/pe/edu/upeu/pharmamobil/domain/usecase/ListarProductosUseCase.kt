package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.data.remote.ejecutarLlamada
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class ListarProductosUseCase(
    private val productoRepository: ProductoRepository
) {

    fun observar() = productoRepository.observar()
    fun observarUltimaSincronizacion() = productoRepository.observarUltimaSincronizacion()
    suspend fun sincronizar() = ejecutarLlamada { productoRepository.sincronizar() }

    suspend operator fun invoke(): Result<List<Producto>> = ejecutarLlamada {
        productoRepository.listar()
    }
}
