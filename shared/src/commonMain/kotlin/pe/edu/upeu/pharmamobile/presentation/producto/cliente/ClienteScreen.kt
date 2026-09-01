package pe.edu.upeu.pharmamobile.presentation.producto.cliente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ClienteScreen() {

    var nombre by remember {
        mutableStateOf("")
    }

    var correo by remember {
        mutableStateOf("")
    }

    var telefono by remember {
        mutableStateOf("")
    }

    var intentoRegistrar by remember {
        mutableStateOf(false)
    }

    var nombreTocado by remember {
        mutableStateOf(false)
    }

    var correoTocado by remember {
        mutableStateOf(false)
    }

    var telefonoTocado by remember {
        mutableStateOf(false)
    }

    var mensajeExito by remember {
        mutableStateOf<String?>(null)
    }

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

        Text("PharmaMobil")
        Text("Registro de Cliente")

        OutlinedTextField(
            value = nombre,
            onValueChange = {
                nombre = it
                nombreTocado = true
                mensajeExito = null
            },
            label = {
                Text("Nombre")
            },
            isError = mostrarNombreError,
            supportingText = {
                if (mostrarNombreError) {
                    Text(nombreError)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = correo,
            onValueChange = {
                correo = it
                correoTocado = true
                mensajeExito = null
            },
            label = {
                Text("Correo")
            },
            isError = mostrarCorreoError,
            supportingText = {
                if (mostrarCorreoError) {
                    Text(correoError)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = telefono,
            onValueChange = {
                telefono = it
                telefonoTocado = true
                mensajeExito = null
            },
            label = {
                Text("Teléfono (opcional)")
            },
            isError = mostrarTelefonoError,
            supportingText = {
                if (mostrarTelefonoError) {
                    Text(telefonoError)
                }
            },
            modifier = Modifier.fillMaxWidth()
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
            Text(it)
        }
    }
}
