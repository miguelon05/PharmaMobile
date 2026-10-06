package pe.edu.upeu.pharmamobil.platform

import platform.Foundation.*

actual fun formatearSoles(valor: Double): String {
    val formateador = NSNumberFormatter()
    formateador.numberStyle = NSNumberFormatterCurrencyStyle
    formateador.locale = NSLocale("es_PE")
    return formateador.stringFromNumber(NSNumber(valor)) ?: "S/ $valor"
}
