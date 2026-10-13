package pe.edu.upeu.pharmamobil.presentation.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.domain.usecase.LoginUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FakeAuthRepository : AuthRepository {
    var debeFallar = false

    override suspend fun login(credenciales: Credenciales): Result<Sesion> {
        if (debeFallar || credenciales.clave == "error") {
            return Result.failure(IllegalArgumentException("Credenciales incorrectas"))
        }
        return Result.success(
            Sesion(
                token = "fake-jwt-token",
                usuario = credenciales.usuario,
                nombreCompleto = "Usuario de Prueba",
                permisos = setOf("PRODUCTOS_CREAR", "CLIENTES_LEER")
            )
        )
    }

    override suspend fun obtenerSesionGuardada(): Sesion? = null

    override suspend fun cerrarSesion() {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @BeforeTest
    fun instalarMain() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restaurarMain() {
        Dispatchers.resetMain()
    }

    @Test
    fun rechazaCamposVaciosEnFormulario() {
        val repo = FakeAuthRepository()
        val vm = LoginViewModel(LoginUseCase(repo))

        vm.iniciarSesion {}

        val estado = vm.uiState.value
        assertEquals("El usuario es obligatorio", estado.usuarioError)
        assertEquals("La contraseña es obligatoria", estado.claveError)
        assertFalse(estado.cargando)
    }

    @Test
    fun loginExitosoInformaASesion() = runTest {
        val repo = FakeAuthRepository()
        val vm = LoginViewModel(LoginUseCase(repo))

        vm.onUsuarioChange("admin")
        vm.onClaveChange("Admin123")

        var sesionObtenida: Sesion? = null
        vm.iniciarSesion { sesion ->
            sesionObtenida = sesion
        }

        assertNotNull(sesionObtenida)
        assertEquals("admin", sesionObtenida.usuario)
        assertNull(vm.uiState.value.errorGeneral)
    }
}
