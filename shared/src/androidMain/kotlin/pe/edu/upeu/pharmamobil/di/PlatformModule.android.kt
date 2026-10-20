package pe.edu.upeu.pharmamobil.di

import android.content.pm.ApplicationInfo
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriver
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriverAndroid
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose
import pe.edu.upeu.pharmamobil.data.remote.ApiConfig
import pe.edu.upeu.pharmamobil.domain.platform.AlmacenSeguro
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.AlmacenSeguroAndroid
import pe.edu.upeu.pharmamobil.platform.CompartidorAndroid

actual val platformModule: Module = module {
    single<CreadorDeDriver> { CreadorDeDriverAndroid(androidContext()) }
    single<HttpClientEngine> { OkHttp.create() } onClose { it?.close() }
    single {
        ApiConfig(
            // Host del equipo anfitrión desde el emulador Android.
            baseUrl = "http://10.0.2.2:8080/api/v1/",
            loggingEnabled = androidContext().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        )
    }
    single<Compartidor> { CompartidorAndroid(androidContext()) }
    single<AlmacenSeguro> { AlmacenSeguroAndroid(androidContext()) }
}
