package pe.edu.upeu.pharmamobil.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import pe.edu.upeu.pharmamobil.db.PharmaDb
import kotlin.test.*

class S13MigracionTest {
    @Test fun migracionConservaPendienteExistente() {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            driver.execute(null, "CREATE TABLE producto (id INTEGER NOT NULL PRIMARY KEY, nombre TEXT NOT NULL, precio REAL NOT NULL, stock INTEGER NOT NULL, categoriaId INTEGER, pendiente INTEGER NOT NULL DEFAULT 0, eliminado INTEGER NOT NULL DEFAULT 0)", 0)
            driver.execute(null, "INSERT INTO producto VALUES (-1, 'Pendiente', 4.5, 10, NULL, 1, 0)", 0)
            driver.execute(null, "CREATE TABLE sincronizacion (id INTEGER NOT NULL PRIMARY KEY CHECK (id = 1), instante INTEGER NOT NULL)", 0)
            PharmaDb.Schema.migrate(driver, 2, 3)
            val fila = PharmaDb(driver).productoQueries.obtener(-1).executeAsOne()
            assertEquals("Pendiente", fila.nombre)
            assertEquals(1L, fila.pendiente)
            assertEquals(0L, fila.intentos)
            assertNull(fila.ultimoError)
            assertEquals(0L, fila.proximoIntento)
        }
    }
}
