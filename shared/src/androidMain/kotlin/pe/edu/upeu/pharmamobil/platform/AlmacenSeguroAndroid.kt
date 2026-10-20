package pe.edu.upeu.pharmamobil.platform

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import pe.edu.upeu.pharmamobil.domain.platform.AlmacenSeguro

class AlmacenSeguroAndroid(
    contexto: Context
) : AlmacenSeguro {

    private val masterKey = MasterKey.Builder(contexto)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        contexto,
        "pharmamobil_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun guardarSesion(sesion: String) { prefs.edit().putString("sesion", sesion).apply() }
    override fun leerSesion(): String? = prefs.getString("sesion", null)
    override fun borrarSesion() { prefs.edit().remove("sesion").apply() }

    override fun guardarToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    override fun leerToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    override fun borrarToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    private companion object {
        const val KEY_TOKEN = "jwt_token"
    }
}
