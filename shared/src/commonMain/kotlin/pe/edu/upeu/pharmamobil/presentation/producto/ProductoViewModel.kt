package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ProductoInvalidoException
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.domain.usecase.comoTextoParaCompartir
import pe.edu.upeu.pharmamobil.domain.model.Producto

class ProductoViewModel(
    private val registrarProducto: RegistrarProductoUseCase,
    private val listarProductos: ListarProductosUseCase,
    private val actualizarProductoUseCase: ActualizarProductoUseCase,
    private val eliminarProductoUseCase: EliminarProductoUseCase,
    private val compartidor: Compartidor
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoUiState())
    val uiState: StateFlow<ProductoUiState> = _uiState.asStateFlow()
    private var carga: Job? = null
    private var productosOriginales: Map<Long, Producto> = emptyMap()

    init {
        viewModelScope.launch {
            listarProductos.observar().collect { productos ->
                productosOriginales = productos.associateBy(Producto::id)
                _uiState.update { it.copy(
                    fase = if (productos.isEmpty()) ProductoUiState.Fase.SinProductos
                           else ProductoUiState.Fase.ConProductos(productos.map { p -> p.aUi() }),
                    pendientes = productos.count { p -> p.pendiente }
                ) }
            }
        }
        sincronizar()
    }

    fun cargarProductos() = sincronizar()

    fun sincronizar() {
        if (carga?.isActive == true) return
        carga = viewModelScope.launch {
            _uiState.update { it.copy(sincronizando = true) }
            val resultado = listarProductos.sincronizar()
            _uiState.update { it.copy(
                sincronizando = false,
                avisoRed = resultado.exceptionOrNull()?.let { fallo ->
                    when ((fallo as? ErrorApiException)?.error) {
                        ErrorApi.SinConexion, ErrorApi.TiempoAgotado -> "Sin conexión: mostrando datos guardados"
                        else -> "No se pudo sincronizar. Los cambios locales siguen guardados."
                    }
                }
            ) }
        }
    }

    fun onNombreChange(nombre: String) {
        _uiState.update {
            it.copy(
                formulario = it.formulario.copy(nombre = nombre, nombreError = null),
                mensajeExito = null
            )
        }
    }

    fun onPrecioChange(precio: String) {
        _uiState.update {
            it.copy(
                formulario = it.formulario.copy(precio = precio.replace(',', '.'), precioError = null),
                mensajeExito = null
            )
        }
    }

    fun onStockChange(stock: String) {
        _uiState.update {
            it.copy(
                formulario = it.formulario.copy(stock = stock, stockError = null),
                mensajeExito = null
            )
        }
    }

    fun seleccionarProductoParaEditar(producto: ProductoUi) {
        _uiState.update {
            it.copy(
                formulario = FormularioProducto(
                    id = producto.id,
                    nombre = producto.nombre,
                    precio = productosOriginales[producto.id]?.precio?.toString().orEmpty(),
                    stock = productosOriginales[producto.id]?.stock?.toString().orEmpty()
                ),
                mensajeExito = null
            )
        }
    }

    fun cancelarEdicion() {
        _uiState.update {
            it.copy(formulario = FormularioProducto(), mensajeExito = null)
        }
    }

    fun compartir(producto: ProductoUi) {
        productosOriginales[producto.id]?.let { productoOriginal ->
            compartidor.compartir(productoOriginal.comoTextoParaCompartir())
        }
    }

    fun registrar() {
        if (_uiState.value.operacion !is ProductoUiState.Operacion.Inactiva) return

        val formulario = _uiState.value.formulario
        val esEdicion = formulario.id != null

        viewModelScope.launch {
            val tipoOperacion = if (esEdicion) ProductoUiState.Operacion.Tipo.Actualizar else ProductoUiState.Operacion.Tipo.Crear
            _uiState.update {
                it.copy(operacion = ProductoUiState.Operacion.EnCurso(tipoOperacion), mensajeExito = null)
            }

            val resultado = if (esEdicion) {
                actualizarProductoUseCase(
                    id = formulario.id!!,
                    nombre = formulario.nombre,
                    precio = formulario.precio,
                    stock = formulario.stock,
                    categoriaId = productosOriginales[formulario.id!!]?.categoriaId
                )
            } else {
                registrarProducto(
                    nombre = formulario.nombre,
                    precio = formulario.precio,
                    stock = formulario.stock
                )
            }

            resultado.onSuccess { producto ->
                sincronizar()
                _uiState.update {
                    it.copy(
                        operacion = ProductoUiState.Operacion.Inactiva,
                        formulario = FormularioProducto(),
                        mensajeExito = "Producto \"${producto.nombre}\" guardado correctamente"
                    )
                }
            }.onFailure { fallo -> manejarFallo(fallo) }
        }
    }

    fun eliminar(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(operacion = ProductoUiState.Operacion.EnCurso(ProductoUiState.Operacion.Tipo.Eliminar)) }
            eliminarProductoUseCase(id)
                .onSuccess {
                    sincronizar()
                    _uiState.update {
                        it.copy(operacion = ProductoUiState.Operacion.Inactiva, mensajeExito = "Producto eliminado")
                    }
                }
                .onFailure { fallo -> manejarFallo(fallo) }
        }
    }

    private fun manejarFallo(fallo: Throwable) {
        val error = (fallo as? ErrorApiException)?.error

        if (fallo is ProductoInvalidoException) {
            _uiState.update {
                it.copy(
                    operacion = ProductoUiState.Operacion.Inactiva,
                    formulario = it.formulario.copy(
                        nombreError = fallo.errores.nombre,
                        precioError = fallo.errores.precio,
                        stockError = fallo.errores.stock
                    )
                )
            }
            return
        }

        when (error) {
            is ErrorApi.Validacion -> {
                val nombreError = error.porCampo["nombre"]
                val precioError = error.porCampo["precio"]
                val stockError = error.porCampo["stock"]
                val otrosErrores = error.porCampo
                    .filterKeys { it !in setOf("nombre", "precio", "stock") }
                    .values
                    .joinToString("\n")
                _uiState.update {
                    it.copy(
                        operacion = if (otrosErrores.isBlank()) ProductoUiState.Operacion.Inactiva
                            else ProductoUiState.Operacion.Fallida(otrosErrores),
                        formulario = it.formulario.copy(
                            nombreError = nombreError,
                            precioError = precioError,
                            stockError = stockError
                        )
                    )
                }
            }
            else -> _uiState.update {
                it.copy(operacion = ProductoUiState.Operacion.Fallida(mensajeDe(fallo, error)))
            }
        }
    }

    private fun mensajeDe(fallo: Throwable, error: ErrorApi?): String {
        return when (error) {
            is ErrorApi.Conflicto -> error.mensaje
            is ErrorApi.NoEncontrado -> "El registro no existe"
            ErrorApi.Servidor -> "Error interno del servidor"
            ErrorApi.SinConexion -> "Revisa tu conexión a internet"
            ErrorApi.TiempoAgotado -> "El servidor tardó demasiado"
            else -> fallo.message ?: "Ocurrió un error desconocido"
        }
    }
}
