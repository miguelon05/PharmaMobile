package pe.edu.upeu.pharmamobil.presentation.producto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobil.data.repository.FakeProductoRepository
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

import pe.edu.upeu.pharmamobil.domain.platform.Compartidor

@OptIn(ExperimentalCoroutinesApi::class)
class ProductoViewModelTest {

    private val dummyCompartidor = object : Compartidor { override fun compartir(texto: String) {} }

    @AfterTest
    fun restaurarMain() {
        Dispatchers.resetMain()
    }

    private fun nuevoViewModel(
        repositorio: FakeProductoRepository = FakeProductoRepository()
    ) = ProductoViewModel(
        registrarProducto = RegistrarProductoUseCase(repositorio),
        listarProductos = ListarProductosUseCase(repositorio),
        actualizarProductoUseCase = ActualizarProductoUseCase(repositorio),
        eliminarProductoUseCase = EliminarProductoUseCase(repositorio),
        compartidor = dummyCompartidor
    )


    @Test
    fun cargaExitosa() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)

        val repositorio = FakeProductoRepository(
            mutableListOf(Producto(id = 1L, nombre = "Ibuprofeno", precio = 15.0, stock = 20))
        )
        val viewModel = nuevoViewModel(repositorio)

        // Verificamos estado inicial antes de que acabe la corrutina
        assertEquals(ProductoUiState.Fase.Cargando, viewModel.uiState.value.fase)

        // Avanzamos el tiempo para que termine de cargar
        advanceUntilIdle()

        // Verificamos estado final
        val fase = assertIs<ProductoUiState.Fase.ConProductos>(viewModel.uiState.value.fase)
        assertEquals(1, fase.productos.size)
    }

    @Test
    fun listadoVacio() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)

        val repositorio = FakeProductoRepository(productos = mutableListOf())
        val viewModel = nuevoViewModel(repositorio)

        advanceUntilIdle()
        assertIs<ProductoUiState.Fase.SinProductos>(viewModel.uiState.value.fase)
    }

    @Test
    fun errorDeValidacionServidor() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)

        val repositorio = FakeProductoRepository()
        val viewModel = nuevoViewModel(repositorio)
        // Evitamos que intente iniciar una carga asincrona para que el fake devuelva los datos limpios sin error de inicializacion
        advanceUntilIdle()

        // Forzamos al Fake a devolver una excepcion simulando un HTTP 400 Bad Request
        repositorio.fallaAlRegistrar = ErrorApiException(
            ErrorApi.Validacion(mapOf("nombre" to "El nombre debe tener al menos 3 caracteres", "precio" to "Precio muy bajo"))
        )


        viewModel.onNombreChange("Aa")
        viewModel.onPrecioChange("0")
        viewModel.onStockChange("10")
        viewModel.registrar()
        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals("El nombre debe tener al menos 3 caracteres", estado.formulario.nombreError)
        assertEquals("Precio muy bajo", estado.formulario.precioError)
        assertIs<ProductoUiState.Fase.ConProductos>(estado.fase) // La fase principal no cambia a Error, se queda donde estaba
    }

    @Test
    fun eliminacion() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)

        val repositorio = FakeProductoRepository(
            mutableListOf(Producto(id = 1L, nombre = "Paracetamol", precio = 2.0, stock = 10))
        )
        val viewModel = nuevoViewModel(repositorio)
        advanceUntilIdle()

        // La lista inicial tiene 1 producto
        assertEquals(1, (viewModel.uiState.value.fase as ProductoUiState.Fase.ConProductos).productos.size)

        // Lanzar eliminación
        viewModel.eliminar(1L)
        
        // Mientras elimina, la operación pasa a EnCurso
        val estadoEnCurso = viewModel.uiState.value
        assertIs<ProductoUiState.Operacion.EnCurso>(estadoEnCurso.operacion)
        assertEquals(ProductoUiState.Operacion.Tipo.Eliminar, estadoEnCurso.operacion.tipo)

        // Dejar que termine de procesar (Eliminación + Recarga automática)
        advanceUntilIdle()

        val estadoFinal = viewModel.uiState.value
        assertEquals(ProductoUiState.Operacion.Inactiva, estadoFinal.operacion)
        assertIs<ProductoUiState.Fase.SinProductos>(estadoFinal.fase) // Ahora la lista está vacía
    }
}
