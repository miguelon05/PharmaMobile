package pe.edu.upeu.pharmamobil.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.test.*
import pe.edu.upeu.pharmamobil.data.remote.*
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.domain.platform.AlmacenSeguro

class AuthOfflineTest {
    private class Almacen : AlmacenSeguro {
        var token: String? = null
        var sesion: String? = null
        override fun guardarToken(token: String) { this.token = token }
        override fun leerToken() = token
        override fun borrarToken() { token = null }
        override fun guardarSesion(sesion: String) { this.sesion = sesion }
        override fun leerSesion() = sesion
        override fun borrarSesion() { sesion = null }
    }
    private fun almacen(exp: Long): Almacen {
        val payload = Base64.UrlSafe.encode("{\"exp\":$exp}".encodeToByteArray())
        return Almacen().apply {
            token = "header.$payload.firma"
            sesion = Json.encodeToString(Sesion(token!!, "admin", "Administrador", setOf("CATALOGO_ESCRIBIR")))
        }
    }
    @Test fun sesionPreviaVigenteSeRecuperaSinRed() = runTest {
        val almacen = almacen(kotlin.time.Clock.System.now().epochSeconds + 3600)
        val engine = MockEngine { throw io.ktor.utils.io.errors.IOException("Sin red") }
        val client = crearHttpClient(engine, ApiConfig("https://prueba.invalid/api/v1/"), almacen)
        try {
            val repo = AuthRepositorioRest(AuthApi(client), almacen, client)
            assertEquals("admin", repo.obtenerSesionGuardada()?.usuario)
            assertTrue(repo.obtenerSesionGuardada()!!.puede("CATALOGO_ESCRIBIR"))
            repo.cerrarSesion()
            assertNull(almacen.sesion)
            assertNull(repo.obtenerSesionGuardada())
        } finally { client.close() }
    }
    @Test fun sesionExpiradaNoPermiteEntrarSinRed() = runTest {
        val almacen = almacen(1)
        val client = crearHttpClient(MockEngine { throw io.ktor.utils.io.errors.IOException("Sin red") }, ApiConfig("https://prueba.invalid/api/v1/"), almacen)
        try { assertNull(AuthRepositorioRest(AuthApi(client), almacen, client).obtenerSesionGuardada()) }
        finally { client.close() }
    }
    @Test fun rechazoDelServidorNoUsaSesionCacheada() = runTest {
        val almacen = almacen(kotlin.time.Clock.System.now().epochSeconds + 3600)
        val client = crearHttpClient(MockEngine { respond("{}", HttpStatusCode.Unauthorized) }, ApiConfig("https://prueba.invalid/api/v1/"), almacen)
        try {
            assertNull(AuthRepositorioRest(AuthApi(client), almacen, client).obtenerSesionGuardada())
            assertNull(almacen.token)
            assertNull(almacen.sesion)
        } finally { client.close() }
    }
}
