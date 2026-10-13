package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.*
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositorioRest
import pe.edu.upeu.pharmamobil.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoViewModel
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoUiState

import pe.edu.upeu.pharmamobil.domain.platform.Compartidor

@OptIn(ExperimentalCoroutinesApi::class)
class ProductoApiTest {
    private val config = ApiConfig("https://prueba.invalid/api/v1/")
    private val headers = headersOf("Content-Type", "application/json")
    private val dummyCompartidor = object : Compartidor { override fun compartir(texto: String) {} }
    private val dummyAlmacen = object : pe.edu.upeu.pharmamobil.domain.platform.AlmacenSeguro {
        override fun guardarToken(token: String) {}
        override fun leerToken(): String? = null
        override fun borrarToken() {}
    }

    @Test
    fun get200RespetaRutaYPaginacionYEntregaDatosAlViewModel() = runTest {
        var llamadas = 0
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("https", request.url.protocol.name)
            assertEquals("/api/v1/productos", request.url.encodedPath)
            assertEquals("20", request.url.parameters["tamanio"])
            val page = request.url.parameters["pagina"]!!.toInt()
            assertEquals(llamadas++, page)
            respond("""{"contenido":[{"id":${page + 1},"nombre":"Producto de prueba","precio":12.50,"stock":5,"estado":true,"fechaModificacion":null}],"pagina":$page,"tamanio":20,"totalElementos":2,"totalPaginas":2,"ultima":${page == 1}}""", headers = headers)
        }
        val client = crearHttpClient(engine, config, dummyAlmacen)
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = ProductoRepositorioRest(ProductoApi(client), 1L)
            val vm = ProductoViewModel(
                RegistrarProductoUseCase(repo), 
                ListarProductosUseCase(repo),
                ActualizarProductoUseCase(repo),
                EliminarProductoUseCase(repo),
                dummyCompartidor
            )
            assertIs<ProductoUiState.Fase.Cargando>(vm.uiState.value.fase)
            advanceUntilIdle()
            vm.uiState.first { it.fase !is ProductoUiState.Fase.Cargando }
            val success = assertIs<ProductoUiState.Fase.ConProductos>(vm.uiState.value.fase)
            assertEquals(listOf(1L), success.productos.map { it.id })
            assertEquals(2, llamadas)
        } finally { client.close(); engine.close(); Dispatchers.resetMain() }
    }


    @Test
    fun errorHttpYJsonInvalidoProducenMensajesLegibles() = runTest {
        val cases = listOf(
            Triple(HttpStatusCode.InternalServerError, "{}", "Error interno del servidor"),
            Triple(HttpStatusCode.OK, """{"contenido":null}""", "formato esperado")
        )
        for ((status, body, expected) in cases) {
            val engine = MockEngine { respond(body, status, headers) }
            val client = crearHttpClient(engine, config, dummyAlmacen)
            try {
                val result = ListarProductosUseCase(ProductoRepositorioRest(ProductoApi(client), 1L))()
                assertTrue(result.isFailure)
                val ex = result.exceptionOrNull()
                val msg = if (ex is ErrorApiException) {
                    when (ex.error) {
                        is ErrorApi.Servidor -> "Error interno del servidor"
                        else -> ex.error.toString()
                    }
                } else ex?.message ?: ex.toString()
                assertTrue(msg.contains(expected), msg)
            } finally { client.close(); engine.close() }
        }

    }

    @Test
    fun perdidaDeRedProduceErrorYReintentoRecuperaListaVacia() = runTest {
        var online = false
        val engine = MockEngine {
            if (!online) throw io.ktor.utils.io.errors.IOException("Conexion cortada en la prueba")
            respond("""{"contenido":[],"pagina":0,"tamanio":20,"totalElementos":0,"totalPaginas":0,"ultima":true}""", headers = headers)
        }

        val client = crearHttpClient(engine, config, dummyAlmacen)
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = ProductoRepositorioRest(ProductoApi(client), 1L)
            val vm = ProductoViewModel(
                RegistrarProductoUseCase(repo), 
                ListarProductosUseCase(repo),
                ActualizarProductoUseCase(repo),
                EliminarProductoUseCase(repo),
                dummyCompartidor
            )
            advanceUntilIdle()
            vm.uiState.first { it.fase !is ProductoUiState.Fase.Cargando }
            assertIs<ProductoUiState.Fase.Error>(vm.uiState.value.fase)
            online = true
            vm.cargarProductos()
            advanceUntilIdle()
            vm.uiState.first { it.fase !is ProductoUiState.Fase.Cargando }
            assertIs<ProductoUiState.Fase.SinProductos>(vm.uiState.value.fase)
        } finally { client.close(); engine.close(); Dispatchers.resetMain() }
    }

    @Test
    fun cancelacionNoSeConvierteEnErrorDeRed() = runTest {
        val engine = MockEngine { throw CancellationException("Cancelado") }
        val client = crearHttpClient(engine, config, dummyAlmacen)
        try {
            assertFailsWith<CancellationException> {
                ListarProductosUseCase(ProductoRepositorioRest(ProductoApi(client), 1L))()
            }
        } finally { client.close(); engine.close() }
    }
}
