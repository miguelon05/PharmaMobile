package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class SincronizarUseCase(private val repositorio: ProductoRepository) {
    suspend operator fun invoke() = repositorio.sincronizar()
    companion object { const val MAX_INTENTOS = 5 }
}
