package pe.edu.upeu.pharmamobil.domain.permiso

import kotlin.test.*
import kotlinx.coroutines.test.runTest

class GestorPermisosTest {
    @Test fun distinguePrimeraSolicitudDenegacionYBloqueo() {
        assertEquals(EstadoPermiso.Denegado, resolverEstadoPermiso(false, false, false))
        assertEquals(EstadoPermiso.Denegado, resolverEstadoPermiso(false, true, true))
        assertEquals(EstadoPermiso.DenegadoParaSiempre, resolverEstadoPermiso(false, true, false))
        assertEquals(EstadoPermiso.Concedido, resolverEstadoPermiso(true, true, false))
    }
    @Test fun interfazPermiteProbarSinDispositivo() = runTest {
        val gestor = object : GestorPermisos {
            var solicitado = false
            override suspend fun estado(permiso: Permiso) = if (solicitado) EstadoPermiso.Concedido else EstadoPermiso.Denegado
            override suspend fun solicitar(permiso: Permiso): EstadoPermiso { solicitado = true; return estado(permiso) }
            override fun abrirAjustes() {}
        }
        assertEquals(EstadoPermiso.Denegado, gestor.estado(Permiso.NOTIFICACIONES))
        assertEquals(EstadoPermiso.Concedido, gestor.solicitar(Permiso.NOTIFICACIONES))
    }
}
