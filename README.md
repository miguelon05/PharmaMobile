This is a Kotlin Multiplatform project targeting Android, iOS.

## Autenticación JWT y Almacenamiento Seguro

Sincronización completa del sistema de autenticación contra el backend PharmaSoft (`POST /api/v1/auth/login` y `GET /api/v1/auth/me`):

1. **Almacenamiento Seguro:**
   - **Android:** Implementado con `EncryptedSharedPreferences` y `MasterKey` (`AES256_GCM`).
   - **iOS:** Implementado utilizando el llavero `NSUserDefaults` / Keychain nativo.
2. **Ktor Client Auth Plugin:**
   - Se instala el plugin `Auth` con proveedor `bearer`.
   - `loadTokens` lee dinámicamente el token desde `AlmacenSeguro`.
   - `sendWithoutRequest` omite el token en solicitudes hacia `auth/login`.
   - Tras el login exitoso o cierre de sesión, se invoca `clearToken()` sobre el proveedor Bearer para refrescar la caché del cliente Ktor.
3. **Manejo de Errores (401 y 403):**
   - **401 Unauthorized:** Si ocurre durante el login, muestra error de credenciales. Si ocurre durante una petición del CRUD (token expirado), limpia el almacenamiento seguro, resetea la sesión y redirige al usuario a `LoginScreen` con una notificación explícita.
   - **403 Forbidden:** No cierra la sesión; conserva el token y notifica que el usuario no cuenta con los permisos requeridos.
4. **Navegación y Permisos UI:**
   - Si existe sesión válida al abrir la app, navega directamente a la pantalla principal.
   - La pantalla `ProductoScreen` habilita u oculta los botones de creación, edición y eliminación según los permisos del usuario (`sesion.puede("PRODUCTOS_CREAR")`, etc.).

## Sesión 7: GET de productos con Ktor y HTTPS

Flujo implementado:

```text
Compose → ProductoViewModel (StateFlow/UiState)
        → ListarProductosUseCase → ProductoRepositoryImpl
        → ProductoApi → Ktor (HTTPS) → Spring Boot → Oracle Docker
```

El inventario consume datos reales del backend. El módulo Clientes conserva su
repositorio en memoria. La pantalla Productos es de consulta en esta sesión:
el antiguo formulario de registro no se muestra porque el POST del backend
requiere un contrato diferente. No se simula un registro exitoso ni se escribe
en Oracle durante las pruebas GET.

### Iniciar el backend local HTTPS

Backend local: carpeta hermana `../pharmaSoft`. Se agregó
`src/main/resources/application-https.yaml`, que se activa junto con `dev`.
Los certificados privados están en `../pharmaSoft/.local/https/`, ignorados por Git.
El perfil HTTPS valida el esquema existente; `application-dev.yaml` conserva su
configuración original de Oracle y `ddl-auto: update`.

Desde la raíz de este proyecto en PowerShell:

```powershell
.\tools\iniciar_backend_https.ps1
```

Requiere JDK 21 en `C:\Program Files\Java\jdk-21`. Si está en otro lugar, pasar
`-JavaHome 'ruta-del-jdk'`. El script mantiene el servidor en la terminal;
Ctrl+C lo detiene. No iniciarlo si ya hay un servidor en 8443.

Para ejecutarlo desde IntelliJ, abrir el backend, usar su carpeta raíz como
directorio de trabajo y agregar a los argumentos del programa:

```text
--spring.profiles.active=dev,https
```

En esta sesión se inició además una instancia HTTPS en segundo plano, sin detener
la instancia HTTP abierta en IntelliJ. Sus logs y el PID del lanzador están en
`.local/backend-https*` dentro del proyecto Android. Antes de iniciarlo desde
IntelliJ, detener esa instancia de 8443. Reiniciar Windows la detiene también.

Para detener exclusivamente el lanzador de esta sesión y sus procesos hijos:

```powershell
.\tools\detener_backend_https.ps1
```

### Direcciones y certificado

| Uso | URL |
|---|---|
| GET desde el emulador Android | `https://10.0.2.2:8443/api/v1/productos` |
| GET desde esta computadora | `https://localhost:8443/api/v1/productos` |
| Salud del servidor | `https://localhost:8443/api/health` |

La URL base incluye `/api/v1/` y termina en `/`. El servicio agrega `productos`
sin una barra inicial para conservar ese prefijo. `10.0.2.2` es el alias de la
computadora anfitriona dentro del emulador Android; el `localhost` del emulador
representa al propio emulador.

La CA pública se incluye solo en `androidApp/src/debug/res/raw/pharma_dev_ca.pem`.
La configuración de red de `debug` confía en ella únicamente para `10.0.2.2`.
Se verifican el certificado y el nombre del servidor. No hay un TrustManager
que acepte cualquier certificado ni se deshabilitó la verificación del hostname.
HTTP sin cifrar está bloqueado. No se instaló esta CA globalmente en Windows.
Por eso un navegador sin esa CA puede mostrar una advertencia; la app debug
tiene su confianza configurada de forma independiente.

El servidor escucha en `127.0.0.1:8443`: este montaje es para el emulador en la
misma computadora, no para teléfonos por Wi-Fi. Release necesita una URL de
producción HTTPS con un certificado válido y no incluye la CA de desarrollo.

Para otro equipo o renovar el certificado (servidor: 180 días; CA: 365 días):

```powershell
python tools/preparar_https.py ../pharmaSoft
```

Requiere el paquete Python `cryptography`. El script reutiliza los certificados
existentes. Para renovar, detener HTTPS, respaldar y retirar su carpeta
`.local/https`, ejecutar el script, reiniciar HTTPS y recompilar/reinstalar la
app. No subir claves privadas a Git. El certificado público no es una contraseña.

### Contrato real de Spring Boot

`GET /api/v1/productos?pagina=0&tamanio=20&ordenarPor=id&direccion=asc`
devuelve un objeto paginado, no un arreglo directo. Respuesta real observada:

```json
{"contenido":[],"pagina":0,"tamanio":20,"totalElementos":0,"totalPaginas":0,"ultima":true}
```

`ProductoDto` declara `id: Long`, `nombre: String`, `precio: Double`, `stock: Int`.
Son campos obligatorios del contrato; no se inventan valores por defecto para
ocultar un JSON inválido. `ignoreUnknownKeys` permite ignorar los otros campos
del backend: estado, categoría y fechas. `PaginaProductosDto` declara contenido,
pagina, tamanio, totalElementos, totalPaginas y ultima. El repositorio recorre
las páginas hasta `ultima`, conservando el orden por id.

El precio se convierte al modelo `Double` que ya utilizaba la app. Para cálculos
monetarios exactos futuros, adaptar el dominio a unidades mínimas o decimal.

### Dónde está cada parte

Rutas Kotlin relativas a `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/`:

| Archivo | Responsabilidad |
|---|---|
| `data/remote/ApiConfig.kt` | URL HTTPS y logging |
| `data/remote/HttpClientFactory.kt` | ContentNegotiation, Logging, HttpTimeout y DefaultRequest |
| `data/remote/dto/ProductoDto.kt` | DTO serializables de producto y página |
| `data/mapper/ProductoMapper.kt` | Conversión al dominio, sin red en domain/model |
| `data/remote/ProductoApi.kt` | GET y comprobación de HTTP 200 |
| `data/repository/ProductoRepositoryImpl.kt` | Paginación y mensajes de error |
| `di/AppModule.kt` | Cliente, API y repositorio como singletons de Koin |
| `presentation/producto/ProductoViewModel.kt` | Carga con viewModelScope y StateFlow |
| `presentation/producto/ProductoUiState.kt` | Cargando, SinProductos, ConProductos y Error |
| `presentation/producto/ProductoScreen.kt` | Lista Compose, Actualizar y Reintentar |

Los motores están en `androidMain/.../di/PlatformModule.android.kt` (OkHttp)
y `iosMain/.../di/PlatformModule.ios.kt` (Darwin). Ktor es 3.6.0; OkHttp está
alineado a 5.3.0 mediante su BOM para mantener compatibilidad con compileSdk 36.
El HttpClient se reutiliza y se cierra al cerrar su definición de Koin.
Las cancelaciones de corrutinas se propagan sin convertirse en errores de red.

### Ejecutar y comprobar

1. Mantener el backend HTTPS activo en 8443 y Oracle en Docker.
2. Sincronizar Gradle y ejecutar `androidApp` en el emulador.
3. Abrir el menú lateral y seleccionar **Productos**.
4. En Logcat filtrar `PharmaKtor`: deben verse GET, URL HTTPS y `200`.
5. La tabla real está vacía actualmente: se muestra el estado de consulta
   exitosa sin productos. No se agregaron registros ficticios a Oracle.
6. Para verificar una lista con filas, cargar productos válidos en el backend
   y pulsar **Actualizar**. El GET no crea productos.
7. Desconectar Wi-Fi y datos del emulador (modo avión puede conservar Wi-Fi),
   pulsar **Actualizar** y comprobar el mensaje de error. Reconectar y pulsar
   **Reintentar** para recuperar la consulta.

Pruebas de contrato, paginación, DTO/Mapper, Koin, ViewModel, errores y reintento:

```powershell
.\gradlew.bat :androidApp:assembleDebug :shared:testAndroidHostTest --max-workers=2
```

La prueba de integración real está desactivada por defecto para que la suite
no dependa de tener Oracle encendido. Para ejecutarla con el backend activo:

```powershell
$env:PHARMA_HTTPS_CA = (Resolve-Path androidApp/src/debug/res/raw/pharma_dev_ca.pem).Path
.\gradlew.bat :shared:testAndroidHostTest --tests '*BackendHttpsIntegrationTest' --rerun-tasks --max-workers=2
Remove-Item Env:PHARMA_HTTPS_CA
```

Esta prueba usa Ktor/OkHttp, confía exclusivamente en la CA local y conserva la
verificación del hostname. Exige HTTP 200 y deserializa el JSON real. Los datos
de ejemplo de MockEngine se usan solo en tests y nunca se envían a Oracle.

### iOS y evidencias de la guía

Darwin está configurado, pero iOS requiere macOS/Xcode para compilar y ejecutar.
La URL iOS actual supone un backend en el mismo Mac (`https://localhost:8443`).
Para reproducir este montaje allí, iniciar Oracle/backend en ese Mac, generar
los certificados e instalar la CA pública en el simulador:

```sh
xcrun simctl keychain booted add-root-cert androidApp/src/debug/res/raw/pharma_dev_ca.pem
```

No se añadieron excepciones ATS. Para conectarse al backend de Windows desde
otro equipo harían falta una dirección accesible y un certificado que incluya
esa dirección; cambiar `localhost` solo no basta.

La guía pide capturas del GET 200, lista Android, lista iOS y error sin conexión,
y un commit descriptivo en `feature/ktor-client`. Las capturas Android quedaron
pendientes: el emulador Pixel_7 no inició por falta de espacio en disco.
Las capturas iOS también están pendientes, porque esta computadora usa Windows.
No presentar los tests simulados como evidencia de ejecución en iOS.

Verificación realizada el 29/09/2026: APK debug generado, 58 pruebas aprobadas
(0 fallos, 0 omitidas), incluido el GET real con Ktor/OkHttp y certificado validado.
Registro guardado en `docs/evidencias/get-https-200.txt`. No se creó un commit
ni se publicaron cambios en GitHub en esta sesión.

## Manejo de Errores

En este proyecto, la capa de Presentación de Compose Multiplatform no trata directamente con las excepciones crudas de red (Ktor, OkHttp, etc.). En su lugar, el sistema de errores se gestiona a través de una arquitectura limpia:

1. **Traducción en Datos:** Cualquier llamada de red está envuelta en la función `ejecutarLlamada()`. Esta intercepta excepciones como `ServerResponseException`, `HttpRequestTimeoutException` o el clásico `IOException` y las convierte a una clase sellada del dominio llamada `ErrorApi`. Además, parsea los cuerpos de error `400 Bad Request` leyendo el `validationErrors`.
2. **Dominio Restrictivo:** Los casos de uso envían sus respuestas usando la envoltura `Result<T>`, asegurando que un fallo viaja de manera segura y verificable a las capas superiores como una `ErrorApiException(ErrorApi)`.
3. **Consumo en Presentación (MVVM):** `ProductoViewModel` reacciona a dichos errores de dos maneras distintas a través del método `manejarFallo()`:
   * **Errores de validación (400):** Extrae el diccionario de campos fallidos y los asigna dinámicamente al estado local (`FormularioProducto`), pintándose justo debajo del `ValidatedTextField` afectado, sin sacar al usuario de la pantalla.
   * **Errores de Red / Servidor (500, Timeout):** Cambia el estado transitorio `Operacion.Fallida(mensaje)` que despliega un banner de error explícito para informarle al usuario qué sucedió y por qué falló la carga.

## Capacidades nativas

1. **Formateo de moneda mediante expect/actual**: En Kotlin Multiplatform, hemos utilizado el patrón `expect`/`actual` para transformar los números a String representando soles en el área de Presentación de manera nativa sin recargar al Common Main con lógicas engorrosas.
2. **Implementación Android**: Android utiliza `java.text.NumberFormat` y la Localidad ("es", "PE") para dar formato. 
3. **Implementación iOS**: Al igual, en el `iosMain` se delega en el motor base usando `platform.Foundation.NSNumberFormatter`.
4. **Compartir mediante interfaz Compartidor**: La posibilidad de enviar a otras Apps los datos en bruto de un ítem se abstrae con la interfaz `Compartidor`, que es independiente de implementaciones.
5. **Implementación Android con Intent**: Usando `Intent.ACTION_SEND` e inyectando un ApplicationContext desde Koin se levanta un `Intent.createChooser` hacia las demás apps del OS.
6. **Implementación iOS con UIActivityViewController**: A través del framework `UIKit` (código de Kotlin-Native), se localiza el RootViewController nativo desde `UIApplication.sharedApplication` para desplegar la ventana ShareSheet inferior de iOS.
7. **Registro mediante Koin**: En cada módulo nativo (`PlatformModule.android.kt` y `.ios.kt`) se inyecta su implementación específica como un `single`.
8. **Archivos principales**: 
    - Android: `Formato.android.kt`, `CompartidorAndroid.kt`, `PlatformModule.android.kt`. 
    - iOS: `Formato.ios.kt`, `CompartidorIos.kt`, `PlatformModule.ios.kt`. 
    - Common: `Formato.kt`, `Compartidor.kt`, `AppModule.kt`.
9. **Diferencias Android/iOS**: Mientras Android depende de `Intent` y del `Context` (que Koin tiene que proveer inyectando el `androidContext()`), iOS delega directamente en llamadas estáticas al singleton `UIApplication` a través de los envoltorios nativos de Compose (`UIKit`).

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
