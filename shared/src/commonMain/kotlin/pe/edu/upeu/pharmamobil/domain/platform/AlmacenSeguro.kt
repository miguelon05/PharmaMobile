package pe.edu.upeu.pharmamobil.domain.platform

interface AlmacenSeguro {
    fun guardarSesion(sesion: String) {}
    fun leerSesion(): String? = null
    fun borrarSesion() {}
    fun guardarToken(token: String)
    fun leerToken(): String?
    fun borrarToken()
}
