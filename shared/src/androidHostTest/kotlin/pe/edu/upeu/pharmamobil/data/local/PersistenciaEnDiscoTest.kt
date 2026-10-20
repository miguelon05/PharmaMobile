package pe.edu.upeu.pharmamobil.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.model.Producto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersistenciaEnDiscoTest {
    @Test fun cerrarYReabrirConservaProductosYPendientes() = runTest {
        val archivo = Files.createTempFile("pharma-offline-", ".db")
        val url = "jdbc:sqlite:$archivo"
        try {
            JdbcSqliteDriver(url).use { driver ->
                PharmaDb.Schema.create(driver)
                val local = ProductoLocal(PharmaDb(driver))
                local.reemplazarSincronizados(listOf(Producto(10, "Paracetamol", 3.0, 5)))
                local.guardar(Producto(0, "Ibuprofeno", 4.5, 20), nuevo = true)
            }
            JdbcSqliteDriver(url).use { driver ->
                val local = ProductoLocal(PharmaDb(driver))
                assertEquals(2, local.listar().size)
                assertEquals(1, local.pendientes().size)
                assertTrue(local.pendientes().single().id < 0)
            }
        } finally { Files.deleteIfExists(archivo) }
    }
}
