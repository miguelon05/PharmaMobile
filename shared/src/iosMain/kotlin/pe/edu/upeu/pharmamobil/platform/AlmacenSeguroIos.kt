package pe.edu.upeu.pharmamobil.platform

import platform.Foundation.NSUserDefaults
import pe.edu.upeu.pharmamobil.domain.platform.AlmacenSeguro

class AlmacenSeguroIos : AlmacenSeguro {

    private val userDefaults = NSUserDefaults.standardUserDefaults

    override fun guardarToken(token: String) {
        userDefaults.setObject(token, forKey = KEY_TOKEN)
        userDefaults.synchronize()
    }

    override fun leerToken(): String? {
        return userDefaults.stringForKey(KEY_TOKEN)
    }

    override fun borrarToken() {
        userDefaults.removeObjectForKey(KEY_TOKEN)
        userDefaults.synchronize()
    }

    private companion object {
        const val KEY_TOKEN = "pe.edu.upeu.pharmamobil.jwt_token"
    }
}
