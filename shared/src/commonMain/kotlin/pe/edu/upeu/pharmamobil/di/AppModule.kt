package pe.edu.upeu.pharmamobil.di

import io.ktor.client.HttpClient
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.dsl.onClose
import pe.edu.upeu.pharmamobil.data.remote.AuthApi
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.crearHttpClient
import pe.edu.upeu.pharmamobil.data.repository.AuthRepositorioRest
import pe.edu.upeu.pharmamobil.data.repository.ClienteRepositorioEnMemoria
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositorioRest
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.domain.repository.ClienteRepository
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.CerrarSesionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ListarClientesUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.LoginUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObtenerSesionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarClienteUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.presentation.auth.LoginViewModel
import pe.edu.upeu.pharmamobil.presentation.auth.SessionManager
import pe.edu.upeu.pharmamobil.presentation.cliente.ClienteViewModel
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoViewModel

const val CATEGORIA_POR_DEFECTO = 1L

val dataModule = module {
    single<HttpClient> { crearHttpClient(get(), get(), get()) } onClose { it?.close() }
    single { ProductoApi(get()) }
    single { AuthApi(get()) }
    single<ProductoRepository> { ProductoRepositorioRest(get(), CATEGORIA_POR_DEFECTO) }
    single<ClienteRepository> { ClienteRepositorioEnMemoria() }
    single<AuthRepository> { AuthRepositorioRest(get(), get(), get()) }
}

val domainModule = module {
    factory { RegistrarProductoUseCase(get()) }
    factory { ListarProductosUseCase(get()) }
    factory { ActualizarProductoUseCase(get()) }
    factory { EliminarProductoUseCase(get()) }
    factory { RegistrarClienteUseCase(get()) }
    factory { ListarClientesUseCase(get()) }
    factory { LoginUseCase(get()) }
    factory { ObtenerSesionUseCase(get()) }
    factory { CerrarSesionUseCase(get()) }
}

val presentationModule = module {
    viewModel { SessionManager(get(), get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { ProductoViewModel(get(), get(), get(), get(), get()) }
    viewModel { ClienteViewModel(get(), get()) }
}


expect val platformModule: Module

fun initKoin(configuracionAdicional: KoinApplication.() -> Unit = {}) {
    startKoin {
        configuracionAdicional()
        modules(
            dataModule,
            domainModule,
            presentationModule,
            platformModule,
        )
    }
}
