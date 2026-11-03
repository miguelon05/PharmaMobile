package pe.edu.upeu.pharmamobil.data.permiso

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.pharmamobil.domain.permiso.*

class PuentePermisos(val actividad: ComponentActivity) {
    private var pendiente: CompletableDeferred<Boolean>? = null
    private val mutex = Mutex()
    private val lanzador = actividad.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendiente?.complete(it)
    }
    suspend fun pedir(nombre: String): Boolean = withContext(Dispatchers.Main.immediate) {
        mutex.withLock {
            val espera = CompletableDeferred<Boolean>()
            pendiente = espera
            try { lanzador.launch(nombre); espera.await() } finally { pendiente = null }
        }
    }
    fun cerrar() { pendiente?.cancel(); pendiente = null }
}
class GestorPermisosAndroid(private val contexto: Context) : GestorPermisos {
    private val preferencias = contexto.getSharedPreferences("permisos", Context.MODE_PRIVATE)
    var puente: PuentePermisos? = null
    private fun nombre(permiso: Permiso): String? = when (permiso) {
        Permiso.CAMARA -> Manifest.permission.CAMERA
        Permiso.UBICACION -> Manifest.permission.ACCESS_COARSE_LOCATION
        Permiso.NOTIFICACIONES -> if (Build.VERSION.SDK_INT >= 33) Manifest.permission.POST_NOTIFICATIONS else null
    }
    override suspend fun estado(permiso: Permiso): EstadoPermiso = withContext(Dispatchers.Main.immediate) {
        val nombre = nombre(permiso) ?: return@withContext EstadoPermiso.Concedido
        if (ContextCompat.checkSelfPermission(contexto, nombre) == PackageManager.PERMISSION_GRANTED)
            return@withContext EstadoPermiso.Concedido
        val actividad = puente?.actividad ?: return@withContext EstadoPermiso.Denegado
        resolverEstadoPermiso(false, preferencias.getBoolean(nombre, false),
            ActivityCompat.shouldShowRequestPermissionRationale(actividad, nombre))
    }
    override suspend fun solicitar(permiso: Permiso): EstadoPermiso = withContext(Dispatchers.Main.immediate) {
        val nombre = nombre(permiso) ?: return@withContext EstadoPermiso.Concedido
        val puente = puente ?: return@withContext EstadoPermiso.Denegado
        if (estado(permiso) != EstadoPermiso.Denegado) return@withContext estado(permiso)
        preferencias.edit().putBoolean(nombre, true).apply()
        if (puente.pedir(nombre)) EstadoPermiso.Concedido else estado(permiso)
    }
    override fun abrirAjustes() {
        contexto.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:" + contexto.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
