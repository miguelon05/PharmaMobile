package pe.edu.upeu.pharmamobil.presentation.producto

fun antiguedadDatos(instante: Long?, ahora: Long): String {
    if (instante == null) return "Nunca sincronizado"
    val minutos = ((ahora - instante).coerceAtLeast(0) / 60_000)
    return when {
        minutos < 1 -> "Actualizado hace un momento"
        minutos < 60 -> "Actualizado hace $minutos ${if (minutos == 1L) "minuto" else "minutos"}"
        minutos < 1440 -> {
            val horas = minutos / 60
            "Actualizado hace $horas ${if (horas == 1L) "hora" else "horas"}"
        }
        else -> {
            val dias = minutos / 1440
            "Actualizado hace $dias ${if (dias == 1L) "día" else "días"}"
        }
    }
}
