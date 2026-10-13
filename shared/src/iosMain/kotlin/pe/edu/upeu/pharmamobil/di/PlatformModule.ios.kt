package pe.edu.upeu.pharmamobil.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose
import pe.edu.upeu.pharmamobil.data.remote.ApiConfig
import pe.edu.upeu.pharmamobil.domain.platform.AlmacenSeguro
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.AlmacenSeguroIos
import pe.edu.upeu.pharmamobil.platform.CompartidorIos

actual val platformModule: Module = module {
    single<HttpClientEngine> { Darwin.create() } onClose { it?.close() }
    // Simulador: backend en el mismo Mac, con la CA local instalada en el simulador.
    single { ApiConfig("https://localhost:8443/api/v1/", loggingEnabled = true) }
    single<Compartidor> { CompartidorIos() }
    single<AlmacenSeguro> { AlmacenSeguroIos() }
}
