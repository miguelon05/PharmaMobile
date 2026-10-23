package pe.edu.upeu.pharmamobil.domain.model

@kotlinx.serialization.Serializable
data class Sesion(
    val token: String,
    val usuario: String,
    val nombreCompleto: String,
    val permisos: Set<String>,
    val soloLecturaOffline: Boolean = false
) {
    fun puede(permiso: String): Boolean =
        permiso in permisos
}
