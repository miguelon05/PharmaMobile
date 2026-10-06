# S08_ActividadAutonoma_Apellidos

**Asignatura:** Desarrollo de Aplicaciones Móviles
**Ciclo:** VI · Semestre 2026-2
**Repositorio de GitHub:** [Enlace a tu rama feature/crud-productos-apellido]

---

## Producto 1. Matriz de operaciones

| N.º | Operación | Petición enviada | Código | Resultado observado en la app |
| --- | --- | --- | --- | --- |
| 1 | Listar | `GET /productos?pagina=0&tamanio=20` | 200 | Lista renderizada correctamente pasando a la fase `ConProductos`. |
| 2 | Obtener | `GET /productos/{id}` | 200 | Detalle extraído y pre-cargado en el formulario al pulsar el botón editar. |
| 3 | Crear | `POST /productos` con cuerpo válido | 201 | El producto aparece en la parte inferior de la lista (tras la recarga automática). Mensaje de éxito visible. |
| 4 | Actualizar | `PUT /productos/{id}` con cuerpo válido | 200 | Lista refrescada, los datos del Item ahora muestran los nuevos valores. |
| 5 | Eliminar | `DELETE /productos/{id}` | 204 | El indicador de carga desaparece y la fila del ítem eliminado se esfuma tras recargar el listado. Mensaje de éxito visible. |

> **NOTA AL ESTUDIANTE:** Debes comprobar la respuesta y añadir las capturas correspondientes dentro de tu PDF entregable.

---

## Producto 2. Bitácora de errores controlados

### Escenario 1: Validación del servidor
- **Fecha y hora:** `[Insertar Fecha]`
- **Plataforma:** Android
- **Pasos:** Clic en crear producto, ingreso del texto "Aa" en el campo Nombre. Pulso "Registrar".
- **Código HTTP:** `400 Bad Request`
- **ErrorApi producido:** `ErrorApi.Validacion`
- **Mensaje visto:** El mensaje "El tamaño tiene que estar entre 3 y 150" aparece justo debajo de la caja de texto `Nombre`.
- **Conclusión:** El ViewModel intercepta correctamente el `Map` del DTO de error del servidor y extrae la llave `"nombre"` inyectándolo en el estado del formulario en vez de quebrar la aplicación de forma descontrolada.
- **Captura:** `[Insertar Captura de Pantalla]`

### Escenario 2: Precio inválido
- **Fecha y hora:** `[Insertar Fecha]`
- **Plataforma:** iOS / Android
- **Pasos:** Crear un producto llenando el campo precio con el valor `0` o `-5`.
- **Código HTTP:** `400 Bad Request`
- **ErrorApi producido:** `ErrorApi.Validacion`
- **Mensaje visto:** "Debe ser mayor que o igual a 0.01" resaltado en rojo bajo el `TextField` de precio.
- **Conclusión:** La API delegó correctamente el rechazo de persistencia por violación del `@Min` en la base de datos de SpringBoot, mostrando la repuesta en forma humana en la capa de UI.
- **Captura:** `[Insertar Captura de Pantalla]`

### Escenario 3: Recurso inexistente
- **Fecha y hora:** `[Insertar Fecha]`
- **Plataforma:** Android
- **Pasos:** Simular `PUT` a través de herramientas o apuntando manualmente en el código a un ID que no existe (ejemplo: `9999`).
- **Código HTTP:** `404 Not Found`
- **ErrorApi producido:** `ErrorApi.NoEncontrado`
- **Mensaje visto:** Banner / Alerta indicando "El registro no existe".
- **Conclusión:** La operación fallida intercepta el 404 y lo propaga a través de la interfaz sellada `Operacion.Fallida` para generar una advertencia al usuario.
- **Captura:** `[Insertar Captura de Pantalla]`

### Escenario 4: Doble eliminación
- **Fecha y hora:** `[Insertar Fecha]`
- **Plataforma:** Android
- **Pasos:** Detener temporalmente el listado automático tras eliminar, pulsar en eliminar y pulsar nuevamente un segundo después.
- **Código HTTP:** `404 Not Found`
- **ErrorApi producido:** `ErrorApi.NoEncontrado`
- **Mensaje visto:** Alerta indicando "El registro no existe".
- **Conclusión:** Aunque la primera pasada devuelve `204`, la concurrencia de una segunda pasada delata que el objeto ya desapareció del backend.
- **Captura:** `[Insertar Captura de Pantalla]`

### Escenario 5: Regla de negocio
- **Fecha y hora:** `[Insertar Fecha]`
- **Plataforma:** Android
- **Pasos:** Eliminar un producto que actualmente forma parte de un pedido existente.
- **Código HTTP:** `409 Conflict`
- **ErrorApi producido:** `ErrorApi.Conflicto`
- **Mensaje visto:** "El producto está asociado a ventas existentes".
- **Conclusión:** Se respeta la integridad relacional de la BD y la excepción `ReglaNegocioException` del backend se convierte de forma exitosa a un error controlado.
- **Captura:** `[Insertar Captura de Pantalla]`

### Escenario 6: Servidor caído
- **Fecha y hora:** `[Insertar Fecha]`
- **Plataforma:** Android
- **Pasos:** Abortar el proceso (Ctrl+C en terminal de PowerShell de SpringBoot). Intentar dar de alta un producto.
- **Código HTTP:** N/A (Falla red)
- **ErrorApi producido:** `ErrorApi.SinConexion`
- **Mensaje visto:** "Revisa tu conexión a internet".
- **Conclusión:** Cuando la red arroja `IOException`, Ktor jamás recibe encabezados HTTP; el mapeador captura la base y responde adecuadamente.
- **Captura:** `[Insertar Captura de Pantalla]`

### Escenario 7: Tiempo agotado
- **Fecha y hora:** `[Insertar Fecha]`
- **Plataforma:** Android
- **Pasos:** Reducir a 1ms el `HttpTimeout` o simular un Thread.sleep en el backend.
- **Código HTTP:** N/A
- **ErrorApi producido:** `ErrorApi.TiempoAgotado`
- **Mensaje visto:** "El servidor tardó demasiado".
- **Conclusión:** Se comprobó que el Plugin de `HttpTimeout` en Ktor hace su trabajo y aborta conexiones lentas o muertas.
- **Captura:** `[Insertar Captura de Pantalla]`

### Escenario 8: Cancelación
- **Fecha y hora:** `[Insertar Fecha]`
- **Plataforma:** Android
- **Pasos:** Se envía una consulta y rápidamente se sale de la pantalla actual antes de recibir respuesta.
- **Código HTTP:** N/A
- **ErrorApi producido:** Ninguno.
- **Mensaje visto:** Ninguno.
- **Conclusión de Implementación:** El ViewModel captura explícitamente `catch (cancelacion: CancellationException) { throw cancelacion }` dentro de `ejecutarLlamada`. Esto propaga de inmediato la cancelación corrutinal, impidiendo que siga procesando o actualizando estados de UI "fantasmas" que ya no existen, garantizando así cero *leaks*.
- **Captura:** `[Insertar Captura de Pantalla del logcat de cancelación / Job cancelled]`

---

## Producto 4. Evidencias y repositorio
> **NOTA AL ESTUDIANTE:** Inserta debajo de esta línea todas las capturas visuales (emuladores iOS/Android) con las peticiones y sus comprobaciones, además del Log de la terminal donde ejecutaste las pruebas `commonTest` y la url de tu rama Git.
