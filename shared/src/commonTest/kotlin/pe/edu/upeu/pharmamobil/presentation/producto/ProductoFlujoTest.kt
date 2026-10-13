package pe.edu.upeu.pharmamobil.presentation.producto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ListarProductosUseCase

import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

/** Reto 02: transiciones observables y ausencia de efectos ante datos invalidos. */
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor

@OptIn(ExperimentalCoroutinesApi::class)
class ProductoFlujoTest {

    private val dummyCompartidor = object : Compartidor { override fun compartir(texto: String) {} }

    private class RepositorioControlado(
        var productos: List<Producto> = emptyList(),

        val fallo: Throwable? = null
    ) : ProductoRepository {
        var registros = 0
        var listados = 0

        override suspend fun listar(): List<Producto> {
            listados++
            fallo?.let { throw it }
            return productos
        }

        override suspend fun registrar(producto: Producto): Producto {
            registros++
            error("Este flujo no debe registrar productos")
        }
        
        override suspend fun obtener(id: Long): Producto = productos.first { it.id == id }
        override suspend fun actualizar(producto: Producto): Producto = producto
        override suspend fun eliminar(id: Long) {
             productos = productos.filterNot { it.id == id }
        }
    }

    private fun crear(repo: ProductoRepository) = ProductoViewModel(
        RegistrarProductoUseCase(repo), 
        ListarProductosUseCase(repo),
        ActualizarProductoUseCase(repo),
        EliminarProductoUseCase(repo),
        dummyCompartidor
    )

    @Test
    fun repositorioVacioProduceSinProductos() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = RepositorioControlado()
            val vm = crear(repo)
            assertEquals(ProductoUiState.Fase.Cargando, vm.uiState.value.fase)
            advanceUntilIdle()
            assertEquals(ProductoUiState.Fase.SinProductos, vm.uiState.value.fase)
            assertEquals(1, repo.listados)
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun tresProductosProducenConProductos() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = RepositorioControlado(listOf(
                Producto(1L, "Paracetamol", 12.5, 5),
                Producto(2L, "Ibuprofeno", 8.0, 15),
                Producto(3L, "Loratadina", 6.0, 20)
            ))
            val vm = crear(repo)
            assertEquals(ProductoUiState.Fase.Cargando, vm.uiState.value.fase)
            advanceUntilIdle()
            val fase = assertIs<ProductoUiState.Fase.ConProductos>(vm.uiState.value.fase)
            assertEquals(repo.productos.map { it.aUi() }, fase.productos)
            assertEquals(3, fase.productos.size)
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun excepcionProduceError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val vm = crear(RepositorioControlado(fallo = IllegalStateException("Sin conexión")))
            assertEquals(ProductoUiState.Fase.Cargando, vm.uiState.value.fase)
            advanceUntilIdle()
            val fase = assertIs<ProductoUiState.Fase.Error>(vm.uiState.value.fase)
            assertEquals("Sin conexión", fase.mensaje)
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun precioCeroDejaErrorSinLlamarAlRepositorio() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = RepositorioControlado()
            val vm = crear(repo)
            advanceUntilIdle()
            val listadosAntes = repo.listados
            vm.onNombreChange("Paracetamol")
            vm.onPrecioChange("0")
            vm.onStockChange("5")
            vm.registrar()
            advanceUntilIdle()
            val estado = vm.uiState.value
            assertEquals("El precio debe ser mayor a 0", estado.formulario.precioError)
            assertEquals("0", estado.formulario.precio)
            assertNull(estado.formulario.nombreError)
            assertNull(estado.formulario.stockError)
            assertNull(estado.mensajeExito)
            assertFalse(estado.operacion is ProductoUiState.Operacion.EnCurso)
            assertEquals(ProductoUiState.Fase.SinProductos, estado.fase)
            assertEquals(0, repo.registros)
            assertEquals(listadosAntes, repo.listados)
        } finally { Dispatchers.resetMain() }
    }
}
