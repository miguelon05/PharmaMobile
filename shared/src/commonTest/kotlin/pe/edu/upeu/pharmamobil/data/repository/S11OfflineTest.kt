package pe.edu.upeu.pharmamobil.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.TextContent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.local.driverDePrueba
import pe.edu.upeu.pharmamobil.data.remote.*
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoRequestDto
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoResponseDto
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.platform.AlmacenSeguro
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import kotlin.test.*

/** Cada escenario tiene su propia SQLite en memoria y servidor HTTP simulado. */
class S11OfflineTest {
    private class Entorno {
        val driver = driverDePrueba()
        val local = ProductoLocal(PharmaDb(driver))
        var conectado = true
        var instante = 1_700_000_000_000L
        var envios = 0
        val servidor = mutableListOf(ProductoResponseDto(10, "Paracetamol", 3.0, 5))
        val engine = MockEngine { request ->
            if (!conectado) throw io.ktor.utils.io.errors.IOException("Sin red")
            assertEquals("/api/v1/productos", request.url.encodedPath)
            when (request.method) {
                HttpMethod.Get -> respond(
                    """{"contenido":${Json.encodeToString(servidor)},"pagina":0,"tamanio":20,"totalElementos":${servidor.size},"totalPaginas":1,"ultima":true}""",
                    headers = headersOf("Content-Type", "application/json")
                )
                HttpMethod.Post -> {
                    val body = Json.decodeFromString<ProductoRequestDto>((request.body as TextContent).text)
                    val nuevo = ProductoResponseDto(100L + envios++, body.nombre, body.precio, body.stock)
                    servidor.add(nuevo)
                    respond(Json.encodeToString(nuevo), HttpStatusCode.Created, headersOf("Content-Type", "application/json"))
                }
                else -> error("Solicitud inesperada: ${request.method}")
            }
        }
        val client = crearHttpClient(engine, ApiConfig("https://prueba.invalid/api/v1/"), object : AlmacenSeguro {
            override fun guardarToken(token: String) {}
            override fun leerToken(): String? = null
            override fun borrarToken() {}
        })
        val remoto = ProductoRepositorioRest(ProductoApi(client), 1L)
        val repo = ProductoRepositorioOffline(local, remoto) { instante }
        fun cerrar() { client.close(); engine.close(); driver.close() }
    }
    private val nuevo = Producto(0, "S11 Offline", 4.5, 20)

    @Test fun sincronizacionConRedGuardaServidorYEmiteFlow() = runTest {
        val e = Entorno()
        try {
            e.repo.sincronizar()
            assertEquals(listOf(10L), e.local.listar().map { it.id })
            assertEquals(e.local.listar(), e.repo.observar().first())
            assertEquals(e.instante, e.repo.observarUltimaSincronizacion().first())
        } finally { e.cerrar() }
    }

    @Test fun registroSinRedQuedaPendienteYVisible() = runTest {
        val e = Entorno()
        try {
            e.conectado = false
            val creado = e.repo.registrar(nuevo)
            assertFailsWith<ErrorApiException> { e.repo.sincronizar() }
            assertTrue(creado.id < 0)
            assertTrue(e.local.pendientes().single().pendiente == 1L)
            assertEquals(creado, e.repo.observar().first().single())
            assertNull(e.local.ultimaSincronizacion())
        } finally { e.cerrar() }
    }

    @Test fun reconexionEnviaPendienteYConfirmaIdReal() = runTest {
        val e = Entorno()
        try {
            e.conectado = false
            val creado = e.repo.registrar(nuevo)
            assertFailsWith<ErrorApiException> { e.repo.sincronizar() }
            e.conectado = true
            e.repo.reintentar(creado.id)
            e.repo.sincronizar()
            assertEquals(1, e.envios)
            assertTrue(e.local.pendientes().isEmpty())
            assertFalse(e.repo.listar().any { it.id == creado.id })
            assertEquals(100L, e.repo.listar().first { it.nombre == nuevo.nombre }.id)
        } finally { e.cerrar() }
    }

    @Test fun bajadaNoBorraLasFilasPendientes() = runTest {
        val e = Entorno()
        try {
            e.conectado = false
            val creado = e.repo.registrar(nuevo)
            assertFailsWith<ErrorApiException> { e.repo.sincronizar() }
            e.conectado = true
            // Aísla la fase de bajada usada por sincronizar, sin subir previamente la fila.
            // El listado se obtiene por Ktor/MockEngine y se aplica a la SQLite real.
            e.local.reemplazarSincronizados(e.remoto.listar())
            assertEquals(creado, e.local.obtener(creado.id))
            assertEquals(1, e.local.pendientes().size)
            assertEquals(setOf(creado.id, 10L), e.repo.observar().first().map { it.id }.toSet())
        } finally { e.cerrar() }
    }

    @Test fun identificadoresLocalesYRemotosNoColisionan() = runTest {
        val e = Entorno()
        try {
            e.servidor.clear()
            e.servidor.add(ProductoResponseDto(1, "Servidor", 9.0, 7))
            e.repo.sincronizar()
            e.conectado = false
            val creado = e.repo.registrar(nuevo)
            assertEquals(-1L, creado.id)
            assertEquals(setOf(-1L, 1L), e.repo.listar().map { it.id }.toSet())
            assertEquals("Servidor", e.local.obtener(1).nombre)
            assertEquals(nuevo.nombre, e.local.obtener(-1).nombre)
        } finally { e.cerrar() }
    }

    @Test fun sincronizarDosVecesNoDuplicaFilas() = runTest {
        val e = Entorno()
        try {
            e.repo.registrar(nuevo)
            e.repo.sincronizar()
            val primera = e.repo.listar()
            e.repo.sincronizar()
            assertEquals(primera, e.repo.listar())
            assertEquals(2, e.repo.listar().size)
            assertEquals(1, e.envios)
        } finally { e.cerrar() }
    }
}
