package pe.edu.upeu.pharmamobil.presentation.producto

import kotlin.test.*

class AntiguedadDatosTest {
    @Test fun nuncaRecienteHorasYDias() {
        val ahora = 1_700_000_000_000L
        assertEquals("Nunca sincronizado", antiguedadDatos(null, ahora))
        assertEquals("Actualizado hace un momento", antiguedadDatos(ahora, ahora))
        assertEquals("Actualizado hace 1 minuto", antiguedadDatos(ahora - 60_000, ahora))
        assertEquals("Actualizado hace 2 horas", antiguedadDatos(ahora - 7_200_000, ahora))
        assertEquals("Actualizado hace 2 días", antiguedadDatos(ahora - 172_800_000, ahora))
        assertEquals("Actualizado hace un momento", antiguedadDatos(ahora + 1000, ahora))
    }
}
