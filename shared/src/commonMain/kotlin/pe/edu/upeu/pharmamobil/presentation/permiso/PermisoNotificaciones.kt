package pe.edu.upeu.pharmamobil.presentation.permiso

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import pe.edu.upeu.pharmamobil.domain.permiso.*

@Composable
fun PermisoNotificaciones() {
    val gestor = org.koin.compose.getKoin().getOrNull<GestorPermisos>() ?: return
    var estado by remember { mutableStateOf<EstadoPermiso>(EstadoPermiso.Denegado) }
    var ocupado by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { scope.launch { estado = gestor.estado(Permiso.NOTIFICACIONES) } }
    Text(when (estado) {
        EstadoPermiso.Concedido -> "Notificaciones permitidas"
        EstadoPermiso.Denegado -> "Permite notificaciones para recibir avisos de sincronización."
        EstadoPermiso.DenegadoParaSiempre -> "Notificaciones bloqueadas. Puedes habilitarlas en Ajustes."
    })
    if (estado != EstadoPermiso.Concedido) OutlinedButton(enabled = !ocupado, onClick = {
        if (estado == EstadoPermiso.DenegadoParaSiempre) gestor.abrirAjustes()
        else scope.launch {
            ocupado = true
            try { estado = gestor.solicitar(Permiso.NOTIFICACIONES) } finally { ocupado = false }
        }
    }) { Text(if (estado == EstadoPermiso.DenegadoParaSiempre) "Abrir Ajustes" else "Permitir notificaciones") }
}
