package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.local.driverDePrueba
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import kotlin.test.*

class ProductoRepositorioOfflineTest {
    private val producto = Producto(0, "Ibuprofeno", 4.5, 20)
    @Test fun crearSinRedEsVisibleYPendiente() = runTest {
        val driver = driverDePrueba()
        try {
            val local = ProductoLocal(PharmaDb(driver))
            val remoto = FakeProductoRepository().apply { fallaAlRegistrar = ErrorApiException(ErrorApi.SinConexion) }
            val repo = ProductoRepositorioOffline(local, remoto)
            val nuevo = repo.registrar(producto)
            assertTrue(nuevo.id < 0)
            assertFailsWith<ErrorApiException> { repo.sincronizar() }
            assertEquals(nuevo, repo.observar().first().single())
            assertEquals(1, local.pendientes().size)
        } finally { driver.close() }
    }
    @Test fun reconectarSubeAntesDeBajarYSinDuplicar() = runTest {
        val driver = driverDePrueba()
        try {
            val local = ProductoLocal(PharmaDb(driver))
            val remoto = FakeProductoRepository()
            val repo = ProductoRepositorioOffline(local, remoto)
            repo.registrar(producto)
            repo.sincronizar()
            repo.sincronizar()
            assertEquals(1, remoto.listar().size)
            assertTrue(repo.listar().single().id > 0)
            assertFalse(repo.listar().single().pendiente)
            assertTrue(local.pendientes().isEmpty())
        } finally { driver.close() }
    }
    @Test fun falloDeDescargaNoReenviaCreacionConfirmada() = runTest {
        val driver = driverDePrueba()
        try {
            val local = ProductoLocal(PharmaDb(driver))
            val remoto = FakeProductoRepository().apply { fallaAlListar = ErrorApiException(ErrorApi.SinConexion) }
            val repo = ProductoRepositorioOffline(local, remoto)
            repo.registrar(producto)
            assertFailsWith<ErrorApiException> { repo.sincronizar() }
            assertTrue(local.pendientes().isEmpty())
            assertEquals(1, repo.listar().size)
            remoto.fallaAlListar = null
            repo.sincronizar()
            assertEquals(1, remoto.listar().size)
        } finally { driver.close() }
    }
    @Test fun descargaConservaPendientesYEdicionEliminacionSeSincronizan() = runTest {
        val driver = driverDePrueba()
        try {
            val local = ProductoLocal(PharmaDb(driver))
            val remoto = FakeProductoRepository()
            val repo = ProductoRepositorioOffline(local, remoto)
            repo.registrar(producto)
            local.reemplazarSincronizados(emptyList())
            assertEquals(1, repo.listar().size)
            repo.sincronizar()
            val guardado = repo.listar().single()
            repo.actualizar(guardado.copy(stock = 8))
            repo.sincronizar()
            assertEquals(8, remoto.listar().single().stock)
            repo.eliminar(guardado.id)
            assertTrue(repo.listar().isEmpty())
            repo.sincronizar()
            assertTrue(remoto.listar().isEmpty())
        } finally { driver.close() }
    }
    @Test fun eliminarCreacionPendienteNoLaEnvia() = runTest {
        val driver = driverDePrueba()
        try {
            val local = ProductoLocal(PharmaDb(driver))
            val remoto = FakeProductoRepository()
            val repo = ProductoRepositorioOffline(local, remoto)
            repo.eliminar(repo.registrar(producto).id)
            repo.sincronizar()
            assertTrue(remoto.listar().isEmpty())
        } finally { driver.close() }
    }
}
