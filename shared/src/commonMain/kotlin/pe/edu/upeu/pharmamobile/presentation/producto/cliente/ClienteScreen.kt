package pe.edu.upeu.pharmamobile.presentation.producto.cliente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.edu.upeu.pharmamobile.presentation.producto.components.ValidatedTextField

@Composable
fun ClienteScreen() {

    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var intentoRegistrar by remember { mutableStateOf(false) }

    var nombreTocado by remember { mutableStateOf(false) }
    var correoTocado by remember { mutableStateOf(false) }
    var telefonoTocado by remember { mutableStateOf(false) }

    var mensajeExito by remember { mutableStateOf<String?>(null) }

    val nombreError = ClienteValidator.validarNombre(nombre)
    val correoError = ClienteValidator.validarCorreo(correo)
    val telefonoError = ClienteValidator.validarTelefono(telefono)

    val mostrarNombreError = (nombreTocado || intentoRegistrar) && nombreError != null
    val mostrarCorreoError = (correoTocado || intentoRegistrar) && correoError != null
    val mostrarTelefonoError = (telefonoTocado || intentoRegistrar) && telefonoError != null

    val formularioValido = nombreError == null && correoError == null && telefonoError == null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "PharmaMobil",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Registro de Cliente",
            style = MaterialTheme.typography.titleMedium
        )

        ValidatedTextField(
            value = nombre,
            onValueChange = {
                nombre = it
                nombreTocado = true
                mensajeExito = null
            },
            label = "Nombre",
            error = if (mostrarNombreError) nombreError else null
        )

        ValidatedTextField(
            value = correo,
            onValueChange = {
                correo = it
                correoTocado = true
                mensajeExito = null
            },
            label = "Correo",
            error = if (mostrarCorreoError) correoError else null
        )

        ValidatedTextField(
            value = telefono,
            onValueChange = {
                telefono = it
                telefonoTocado = true
                mensajeExito = null
            },
            label = "Teléfono (opcional)",
            error = if (mostrarTelefonoError) telefonoError else null
        )

        Button(
            onClick = {
                mensajeExito = null
                intentoRegistrar = true
                if (formularioValido) {
                    mensajeExito = "Cliente \"$nombre\" registrado correctamente"
                    nombre = ""
                    correo = ""
                    telefono = ""
                    intentoRegistrar = false
                    nombreTocado = false
                    correoTocado = false
                    telefonoTocado = false
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrar")
        }

        mensajeExito?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
