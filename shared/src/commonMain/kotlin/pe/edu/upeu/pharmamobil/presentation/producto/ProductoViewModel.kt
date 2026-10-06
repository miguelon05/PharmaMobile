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

    init {
        cargarProductos()
    }

    fun cargarProductos() {
        carga?.cancel()
        carga = viewModelScope.launch {
            _uiState.update {
                it.copy(fase = ProductoUiState.Fase.Cargando)
            }

            val fase = listarProductos().fold(
                onSuccess = { productos ->
                    if (productos.isEmpty()) {
                        ProductoUiState.Fase.SinProductos
                    } else {
                        ProductoUiState.Fase.ConProductos(productos.map { it.aUi() })
                    }
                },
                onFailure = { fallo ->
                    ProductoUiState.Fase.Error(
                        fallo.message ?: "No se pudo cargar el inventario"
                    )
                }
            )

            _uiState.update {
                it.copy(fase = fase)
            }
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
                formulario = it.formulario.copy(precio = precio, precioError = null),
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
                    precio = producto.precio.toString(),
                    stock = producto.stock.toString()
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
        val prodDomain = Producto(
            id = producto.id,
            nombre = producto.nombre,
            precio = producto.precio.replace("S/ ", "").toDoubleOrNull() ?: 0.0,
            stock = producto.stock.replace(" u.", "").toIntOrNull() ?: 0
        )
        compartidor.compartir(prodDomain.comoTextoParaCompartir())
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
                    stock = formulario.stock
                )
            } else {
                registrarProducto(
                    nombre = formulario.nombre,
                    precio = formulario.precio,
                    stock = formulario.stock
                )
            }

            resultado.onSuccess { producto ->
                cargarProductos()
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
                    cargarProductos()
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
            is ErrorApi.Validacion -> _uiState.update {
                it.copy(
                    operacion = ProductoUiState.Operacion.Inactiva,
                    formulario = it.formulario.copy(
                        nombreError = error.porCampo["nombre"],
                        precioError = error.porCampo["precio"],
                        stockError = error.porCampo["stock"]
                    )
                )
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
