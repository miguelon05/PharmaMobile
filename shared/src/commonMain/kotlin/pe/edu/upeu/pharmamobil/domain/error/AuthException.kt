package pe.edu.upeu.pharmamobil.domain.error

class CredencialesInvalidasException(
    mensaje: String = "Usuario o contraseña incorrectos"
) : Exception(mensaje)

class SesionExpiradaException(
    mensaje: String = "La sesión ha expirado. Por favor, vuelva a iniciar sesión."
) : Exception(mensaje)

class SinPermisoException(
    mensaje: String = "No dispone de permisos para realizar esta acción."
) : Exception(mensaje)
