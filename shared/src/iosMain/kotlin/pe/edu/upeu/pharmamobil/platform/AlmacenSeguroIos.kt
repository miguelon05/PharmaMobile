package pe.edu.upeu.pharmamobil.platform

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import pe.edu.upeu.pharmamobil.domain.platform.AlmacenSeguro

@OptIn(ExperimentalSettingsImplementation::class)
class AlmacenSeguroIos : AlmacenSeguro {

    private val keychain = KeychainSettings("pe.edu.upeu.pharmamobil")

    override fun guardarSesion(sesion: String) { keychain.putString("sesion", sesion) }
    override fun leerSesion(): String? = keychain.getStringOrNull("sesion")
    override fun borrarSesion() { keychain.remove("sesion") }

    override fun guardarToken(token: String) {
        keychain.putString(KEY_TOKEN, token)
    }

    override fun leerToken(): String? = keychain.getStringOrNull(KEY_TOKEN)

    override fun borrarToken() {
        keychain.remove(KEY_TOKEN)
    }

    private companion object {
        const val KEY_TOKEN = "jwt_token"
    }
}
