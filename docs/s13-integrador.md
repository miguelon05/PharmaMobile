# Sesión 13: proyecto integrador

## Tabla de huecos y decisiones

La revisión se basa en el código y las pruebas automatizadas. El recorrido manual y las capturas del dispositivo deben completarse antes de sustentar la entrega.

| N.º | Hueco identificado | Gravedad | Decisión | Estado |
|---|---|---|---|---|
| 1 | La cola no conservaba intentos ni último error. | Alta | Agregar intentos, ultimoError y proximoIntento con migración. | Implementado; prueba de migración incluida. |
| 2 | Un fallo de fila detenía el envío de las siguientes. | Alta | Clasificar fallos transitorios y permanentes y guardar el resultado por fila. | Implementado; pruebas incluidas. |
| 3 | Faltaban límite y espera creciente entre envíos. | Alta | Cinco intentos como máximo y próxima fecha de envío persistida. | Implementado; pruebas incluidas. |
| 4 | Una creación sin respuesta confirmada podía duplicarse al reenviar. | Alta | Exigir revisión y reintento manual para las creaciones ambiguas. | Implementado; pruebas incluidas. |
| 5 | El contador dependía de productos visibles y no de todas las operaciones pendientes. | Media | Usar consultas observables locales para pendientes y fallidas. | Implementado; captura pendiente. |
| 6 | Faltaba mostrar el motivo del fallo y una acción para resolverlo. | Media | Mostrar filas fallidas, motivo, reintento y antigüedad de los datos. | Implementado; captura pendiente. |
| 7 | Faltaba un contrato común para permisos nativos. | Alta | Interfaz común y gestor Android con tres estados y acceso a Ajustes. | Implementado; prueba lógica incluida; validación en dispositivo pendiente. |
| 8 | Faltaba integrar Firebase mediante una interfaz del dominio. | Media | Telemetria y adaptador Crashlytics en datos, registrados con Koin. | Configuración integrada; recepción en consola pendiente de confirmar. |

## Evidencias que se deben adjuntar

1. Esquema: capturar Producto.sq con las columnas nuevas y consultas de pendientes y fallidas.
2. Migración: capturar shared/src/commonMain/sqldelight/pe/edu/upeu/pharmamobil/db/2.sqm.
3. Sincronización: capturar sincronizar() y aResultadoEnvio() en ProductoRepositorioOffline.kt, más SincronizarUseCase.kt.
4. Pendientes: sincronizar con red, desconectar, editar un producto existente y guardar. Capturar el contador y el producto pendiente.
5. Fallidas: sin red, sincronizar dejando al menos 20 segundos entre intentos hasta agotar el límite. Capturar motivo y botón de reintento.
6. Recuperación: reconectar, reintentar y capturar el listado actualizado y la antigüedad de los datos.
7. Permisos: capturar GestorPermisos.kt, GestorPermisosAndroid.kt y POST_NOTIFICATIONS en AndroidManifest.xml.
8. En Android 13 o superior, capturar el diálogo nativo y los estados: denegado con opción de solicitar, bloqueado con acceso a Ajustes y concedido después de habilitarlo.
9. Firebase: capturar Telemetria.kt, TelemetriaFirebase.kt y su registro en AppModule.kt. Capturar un reporte recibido en Crashlytics cuando esté disponible.
10. Pruebas: capturar el reporte HTML de shared/build/reports/tests/testAndroidHostTest/index.html y BUILD SUCCESSFUL.
11. Git: capturar git branch --show-current y git log --oneline --decorate -12.

No se adjuntan capturas ni reportes externos que todavía no se han obtenido. La validación realizada corresponde a Android; iOS requiere su propia ejecución en macOS/Xcode.

## Verificación

```powershell
.\gradlew.bat :shared:testAndroidHostTest :androidApp:assembleDebug
```

La rama de entrega se crea sobre el trabajo existente de la sesión 11. No había una rama develop local ni remota disponible al preparar esta entrega.

## Configuración local de Firebase

El archivo androidApp/google-services.json permanece en el equipo y está excluido de Git. Para compilar otra copia del repositorio, debe colocarse allí el archivo correspondiente a pe.edu.upeu.pharmamobil.
