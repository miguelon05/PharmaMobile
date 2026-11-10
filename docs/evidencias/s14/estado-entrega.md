# Sesión 14: estado de la entrega

Fecha de verificación: 10/11/2026
Rama: `feature/builds-14`
Aplicación: `pe.edu.upeu.pharmamobil`
Versión: `1.0.1` (`versionCode` 2)

## Artefactos Android

- APK firmado para revisión: `output/android-s14/PharmaMobil_Acevedo_1.0.1.apk`.
- AAB firmado: `output/android-s14/PharmaMobil_Acevedo_1.0.1.aab`.
- Certificado del APK: titular `CN=PharmaMobil Acevedo, O=PharmaMobil, C=PE`.
- Huella SHA-256: `515122fa8d8db57bdced119beb0b0c6584fb9d07518e518b1db9ca70c379709e`.
- La clave está en `keystore.properties`, excluido de Git. No se incluyen claves ni contraseñas en esta evidencia.

## Verificación realizada

- `:shared:testAndroidHostTest`: 82 pruebas, 0 fallos, 0 errores y 1 omitida (integración real HTTPS sin backend activo).
- `:androidApp:assembleRelease` y `:androidApp:bundleRelease`: `BUILD SUCCESSFUL`.
- `apksigner verify --print-certs`: firma del APK verificada.
- Instalación de la versión 1.0.1 y apertura confirmadas en el teléfono Xiaomi 24115RA8EG.
- El release se compiló con una URL LAN para la red usada durante la revisión. PharmaSoft no estaba escuchando en el puerto 8080; la sincronización real queda pendiente de iniciar el backend y confirmar el acceso desde el teléfono.
- R8 emitió avisos de lectura de metadata Kotlin más nueva que su versión; el build terminó, pero el recorrido completo de la versión minificada queda pendiente.

## Pendientes de entrega

- Completar en `docs/s13-integrador.md` la clasificación final de cada hueco como cerrado, declarado o descartado, respaldada por evidencia.
- Guardar una captura de la verificación de firma; el titular y la huella están registrados arriba.
- iOS: no se generó IPA/archivo de Xcode en esta computadora Windows. Declarar la limitación y adjuntar evidencia desde un Mac con Xcode, si está disponible.
- Ejecutar la matriz completa en dos dispositivos Android distintos y registrar cada defecto. En este teléfono solo se confirmó instalación y apertura; no se declara completado el recorrido funcional.
- La guía usa el nombre `Equipo<N>_ProyectoFinal_Android.aab`; reemplazar `<N>` por el número real del equipo para la entrega final.
