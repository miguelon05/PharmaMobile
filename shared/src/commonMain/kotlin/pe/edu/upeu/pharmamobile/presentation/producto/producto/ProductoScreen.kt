package pe.edu.upeu.pharmamobile.presentation.producto.producto

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
import pe.edu.upeu.pharmamobile.domain.model.Producto
import pe.edu.upeu.pharmamobile.presentation.producto.components.ValidatedTextField

@Composable
fun ProductoScreen() {
    var nombre by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var intentoRegistrar by remember { mutableStateOf(false) }

    var nombreTocado by remember { mutableStateOf(false) }
    var precioTocado by remember { mutableStateOf(false) }
    var stockTocado by remember { mutableStateOf(false) }

    val nombreError = ProductoValidator.validarNombre(nombre)
    val precioError = ProductoValidator.validarPrecio(precio)
    val stockError = ProductoValidator.validarStock(stock)

    val mostrarNombreError = (nombreTocado || intentoRegistrar) && nombreError != null
    val mostrarPrecioError = (precioTocado || intentoRegistrar) && precioError != null
    val mostrarStockError = (stockTocado || intentoRegistrar) && stockError != null

    val formularioValido = nombreError == null && precioError == null && stockError == null

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
            text = "Registro de Producto",
            style = MaterialTheme.typography.titleMedium
        )

        ValidatedTextField(
            value = nombre,
            onValueChange = {
                nombre = it
                nombreTocado = true
                mensaje = ""
            },
            label = "Nombre",
            error = if (mostrarNombreError) nombreError else null
        )

        ValidatedTextField(
            value = precio,
            onValueChange = {
                precio = it
                precioTocado = true
                mensaje = ""
            },
            label = "Precio",
            error = if (mostrarPrecioError) precioError else null
        )

        ValidatedTextField(
            value = stock,
            onValueChange = {
                stock = it
                stockTocado = true
                mensaje = ""
            },
            label = "Stock",
            error = if (mostrarStockError) stockError else null
        )

        Button(
            onClick = {
                intentoRegistrar = true
                if (formularioValido) {
                    val producto = Producto(
                        id = 0L,
                        nombre = nombre.trim(),
                        precio = precio.toDouble(),
                        stock = stock.toInt()
                    )
                    mensaje = "Producto ${producto.nombre} registrado correctamente."
                    
                    // Reset
                    nombre = ""
                    precio = ""
                    stock = ""
                    intentoRegistrar = false
                    nombreTocado = false
                    precioTocado = false
                    stockTocado = false
                } else {
                    mensaje = "Por favor, corrija los errores en el formulario."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrar")
        }

        if (mensaje.isNotBlank()) {
            Text(
                text = mensaje,
                color = if (formularioValido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}
