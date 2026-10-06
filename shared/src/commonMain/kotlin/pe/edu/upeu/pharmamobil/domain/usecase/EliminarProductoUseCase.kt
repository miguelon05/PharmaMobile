package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.data.remote.ejecutarLlamada
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class EliminarProductoUseCase(
    private val productoRepository: ProductoRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        return ejecutarLlamada {
            productoRepository.eliminar(id)
        }
    }
}
