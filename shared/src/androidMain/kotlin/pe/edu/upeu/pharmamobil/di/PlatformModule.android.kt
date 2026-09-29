package pe.edu.upeu.pharmamobil.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose
import org.koin.android.ext.koin.androidContext
import android.content.pm.ApplicationInfo
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import pe.edu.upeu.pharmamobil.data.remote.ApiConfig

actual val platformModule: Module = module {
    single<HttpClientEngine> { OkHttp.create() } onClose { it?.close() }
    single {
        ApiConfig(
            baseUrl = "https://10.0.2.2:8443/api/v1/",
            loggingEnabled = androidContext().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        )
    }
}
