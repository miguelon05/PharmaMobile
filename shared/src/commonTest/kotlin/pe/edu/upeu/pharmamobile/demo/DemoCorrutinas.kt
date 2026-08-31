package pe.edu.upeu.pharmamobile.demo

import kotlinx.coroutines.runBlocking
import pe.edu.upeu.pharmamobile.domain.result.ResultadoProductos
import pe.edu.upeu.pharmamobile.domain.service.ProductoService
import kotlin.test.Test

class DemoCorrutinasTest {

    private val service = ProductoService()

    @Test
    fun probarFlujosYCorrutinas() = runBlocking {
        println("=== 1. PRUEBA DE FUNCIÓN SUSPEND ===")
        val listaPuntual = service.obtenerProductos()
        println("Productos obtenidos de forma puntual: ${listaPuntual.size} items\n")

        println("=== 2. PRUEBA DE FLOW DE ESTADOS (STRING) ===")
        service.observarEstados().collect { estado ->
            println("Estado emitido: $estado")
        }

        println("\n=== 3. PRUEBA DE FLOW CON SEALED CLASS ===")
        service.cargarProductos().collect { resultado ->
            when (resultado) {
                is ResultadoProductos.Cargando -> println("[STATE]: Cargando información...")
                is ResultadoProductos.Exito -> {
                    println("[STATE]: Datos recibidos con éxito:")
                    resultado.lista.forEach { println(" - ${it.nombre}: S/ ${it.precio} (Stock: ${it.stock})") }
                }
                is ResultadoProductos.Error -> println("[STATE]: Error: ${resultado.msg}")
            }
        }

        println("\n=== 4. PRUEBA DE FLOW REACTIVO DE INVENTARIO CON COPY() ===")
        service.observarProductos().collect { productos ->
            println("Cambio en inventario detectado (${productos.size} productos):")
            productos.forEach { println("   * ${it.nombre} -> Stock actual: ${it.stock}") }
        }
    }
}