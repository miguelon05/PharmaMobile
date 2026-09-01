package pe.edu.upeu.pharmamobile.presentation.producto.producto

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
import pe.edu.upeu.pharmamobile.domain.model.Producto

@Composable
fun ProductoScreen() {
    var nombre by remember {
        mutableStateOf("")
    }

    var precio by remember {
        mutableStateOf("")
    }

    var stock by remember {
        mutableStateOf("")
    }

    var mensaje by remember {
        mutableStateOf("")
    }

    var intentoRegistrar by remember {
        mutableStateOf(false)
    }

    var nombreTocado by remember {
        mutableStateOf(false)
    }

    var precioTocado by remember {
        mutableStateOf(false)
    }

    var stockTocado by remember {
        mutableStateOf(false)
    }

    val precioValor = precio.toDoubleOrNull()
    val stockValor = stock.toIntOrNull()
    val nombreError = if (nombre.isBlank()) "El nombre es obligatorio." else null
    val precioError = when {
        precioValor == null -> "Ingrese un precio numérico."
        precioValor <= 0.0 -> "El precio debe ser mayor que cero."
        else -> null
    }
    val stockError = when {
        stockValor == null -> "Ingrese un stock entero."
        stockValor < 0 -> "El stock no puede ser negativo."
        else -> null
    }
    val mostrarNombreError = (nombreTocado || intentoRegistrar) && nombreError != null
    val mostrarPrecioError = (precioTocado || intentoRegistrar) && precioError != null
    val mostrarStockError = (stockTocado || intentoRegistrar) && stockError != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text("PharmaMobil")
        Text("Registro de Producto")

        OutlinedTextField(
            value = nombre,
            onValueChange = {
                nombre = it
                nombreTocado = true
                mensaje = ""
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
            value = precio,
            onValueChange = {
                precio = it
                precioTocado = true
                mensaje = ""
            },
            label = {
                Text("Precio")
            },
            isError = mostrarPrecioError,
            supportingText = {
                if (mostrarPrecioError) {
                    Text(precioError)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = stock,
            onValueChange = {
                stock = it
                stockTocado = true
                mensaje = ""
            },
            label = {
                Text("Stock")
            },
            isError = mostrarStockError,
            supportingText = {
                if (mostrarStockError) {
                    Text(stockError)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                intentoRegistrar = true
                when {
                    nombreError != null -> mensaje = "Error: $nombreError"
                    precioError != null -> mensaje = "Error: $precioError"
                    stockError != null -> mensaje = "Error: $stockError"
                    else -> {
                        val producto = Producto(
                            id = 0L,
                            nombre = nombre.trim(),
                            precio = precioValor!!,
                            stock = stockValor!!
                        )
                        mensaje = "Producto ${producto.nombre} registrado correctamente."
                        intentoRegistrar = false
                    }
                }

                if (!intentoRegistrar) {
                    nombre = ""
                    precio = ""
                    stock = ""
                    nombreTocado = false
                    precioTocado = false
                    stockTocado = false
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrar")
        }

        if (mensaje.isNotBlank()) {
            Text(mensaje)
        }
    }
}
