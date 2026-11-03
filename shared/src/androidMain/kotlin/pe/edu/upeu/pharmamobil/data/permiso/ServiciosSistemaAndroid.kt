package pe.edu.upeu.pharmamobil.data.permiso

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import pe.edu.upeu.pharmamobil.domain.platform.*

class ConectividadAndroid(private val contexto: Context) : Conectividad {
    override fun observar() = callbackFlow {
        val gestor = contexto.getSystemService(ConnectivityManager::class.java)
        fun emitir() { trySend(gestor.getNetworkCapabilities(gestor.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = emitir()
            override fun onLost(network: Network) = emitir()
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = emitir()
        }
        gestor.registerDefaultNetworkCallback(callback)
        emitir()
        awaitClose { gestor.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
class AvisosSincronizacionAndroid(private val contexto: Context) : AvisosSincronizacion {
    override fun completada() {
        if (Build.VERSION.SDK_INT >= 33 && contexto.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val gestor = contexto.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) gestor.createNotificationChannel(NotificationChannel("sincronizacion", "Sincronización", NotificationManager.IMPORTANCE_LOW))
        val builder = if (Build.VERSION.SDK_INT >= 26) android.app.Notification.Builder(contexto, "sincronizacion") else android.app.Notification.Builder(contexto)
        gestor.notify(13, builder.setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle("PharmaMobil").setContentText("Cambios enviados y catálogo actualizado").setAutoCancel(true).build())
    }
}
