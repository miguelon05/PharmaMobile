package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import pe.edu.upeu.pharmamobil.data.local.*
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.error.*
import kotlin.test.*

class S13SincronizacionTest {
    @Test fun transitorioRespetaEsperaLimiteYPersistencia() = runTest {
        val driver = driverDePrueba()
        try {
            val local = ProductoLocal(PharmaDb(driver))
            val remotoBase = FakeProductoRepository()
            var envios = 0
            val remoto = object : pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository by remotoBase {
                override suspend fun actualizar(producto: Producto): Producto {
                    envios++
                    throw ErrorApiException(ErrorApi.Servidor)
                }
            }
            var ahora = 0L
            val repo = ProductoRepositorioOffline(local, remoto) { ahora }
            local.guardar(Producto(8, "Prueba", 2.0, 4))
            repo.sincronizar()
            assertEquals(1L, local.pendientes().single().intentos)
            assertEquals(1000L, local.pendientes().single().proximoIntento)
            assertEquals(1, repo.observarEstadoSincronizacion().first().pendientes)
            repo.sincronizar()
            assertEquals(1, envios)
            repeat(4) { ahora = local.pendientes().single().proximoIntento; repo.sincronizar() }
            assertEquals(5, envios)
            assertEquals(1, repo.observarEstadoSincronizacion().first().fallidas.size)
            repo.sincronizar()
            assertEquals(5, envios)
        } finally { driver.close() }
    }
    @Test fun permanenteNoReintentaYLaBajadaConservaLaFila() = runTest {
        val driver = driverDePrueba()
        try {
            val local = ProductoLocal(PharmaDb(driver))
            val remoto = FakeProductoRepository().apply { fallaAlRegistrar = ErrorApiException(ErrorApi.Conflicto("Revisar nombre")) }
            val repo = ProductoRepositorioOffline(local, remoto)
            val creado = repo.registrar(Producto(0, "Prueba", 2.0, 4))
            repo.sincronizar()
            assertEquals("Revisar nombre", repo.observarEstadoSincronizacion().first().fallidas.single().motivo)
            remoto.fallaAlRegistrar = null
            repo.sincronizar()
            assertTrue(remoto.listar().isEmpty())
            assertEquals(creado, local.obtener(creado.id))
            repo.reintentar(creado.id)
            repo.sincronizar()
            assertEquals(1, remoto.listar().size)
            assertTrue(local.pendientes().isEmpty())
        } finally { driver.close() }
    }
    @Test fun creacionAmbiguaExigeDecisionManual() = runTest {
        val driver = driverDePrueba()
        try {
            val local = ProductoLocal(PharmaDb(driver))
            val remoto = FakeProductoRepository().apply { fallaAlRegistrar = ErrorApiException(ErrorApi.TiempoAgotado) }
            val repo = ProductoRepositorioOffline(local, remoto)
            repo.registrar(Producto(0, "Prueba", 2.0, 4))
            repo.sincronizar()
            assertTrue(repo.observarEstadoSincronizacion().first().fallidas.single().motivo.contains("duplicados"))
            remoto.fallaAlRegistrar = null
            repo.sincronizar()
            assertTrue(remoto.listar().isEmpty())
        } finally { driver.close() }
    }
}
