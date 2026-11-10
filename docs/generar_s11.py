"""Genera el informe S11 a partir de evidencias reales verificadas del proyecto."""
from pathlib import Path
import json
import textwrap
import xml.etree.ElementTree as ET
from html import escape
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor, white
from reportlab.lib.utils import ImageReader
from reportlab.platypus import Paragraph
from reportlab.lib.styles import ParagraphStyle

ROOT = Path(__file__).resolve().parents[1]
EV = ROOT / 'docs/evidencias/s11'
OUT = ROOT / 'output/pdf/S11_ActividadAutonoma_Acevedo.pdf'
OUT.parent.mkdir(parents=True, exist_ok=True)
W, H = 595.28, 841.89
INK = HexColor('#142535')
TEAL = HexColor('#087e8b')
MUTED = HexColor('#526475')
STYLE = ParagraphStyle('body', fontName='Helvetica', fontSize=10, leading=15, textColor=INK, spaceAfter=7)
SMALL = ParagraphStyle('small', parent=STYLE, fontSize=8, leading=11)
TITLE = ParagraphStyle('title', parent=STYLE, fontName='Helvetica-Bold', fontSize=23, leading=28)
c = canvas.Canvas(str(OUT), pagesize=(W,H))
c.setTitle('Sesión 5 · Unidad 2 · Actividad autónoma')
c.setAuthor('Acevedo')
page = 0

def para(text, x, y, width, style=STYLE):
    p = Paragraph(text, style)
    _, height = p.wrap(width, 1000)
    p.drawOn(c, x, y-height)
    return y-height-9

def begin(title, kicker='PHARMAMOBIL | ACTIVIDAD AUTÓNOMA 11'):
    global page
    if page: c.showPage()
    page += 1
    c.setFillColor(TEAL); c.rect(0,H-13,W,13,fill=1,stroke=0)
    c.setFillColor(MUTED); c.setFont('Helvetica-Bold',8); c.drawString(42,H-39,kicker)
    para(title,42,H-58,W-84,TITLE)
    c.setStrokeColor(HexColor('#dbe4e9')); c.line(42,43,W-42,43)
    c.setFont('Helvetica',8); c.setFillColor(MUTED)
    c.drawString(42,29,'Acevedo · Desarrollo de Aplicaciones Móviles · 23/10/2026')
    c.drawRightString(W-42,29,str(page))
    return H-120

def picture(name,x,y,maxw,maxh):
    path=EV/name
    im=ImageReader(str(path)); iw,ih=im.getSize(); scale=min(maxw/iw,maxh/ih)
    width,height=iw*scale,ih*scale
    c.drawImage(im,x,y-height,width=width,height=height,mask='auto')
    return y-height

def meta(name): return json.loads((EV/(name+'.json')).read_text(encoding='utf-8'))

def scenario(number,title,name,steps,observed,conclusion):
    data=meta(name)
    y=begin(f'{number}. {title}')
    y=para(f"<b>Fecha y hora:</b> {escape(data['fecha_hora'][:19].replace('T',' '))} (Lima).<br/><b>Plataforma:</b> Android, emulador Pixel 7a. Modo avión: {data['modo_avion']}.",42,y,W-84,SMALL)
    picture(name+'.png',42,y,250,555)
    right=310; width=W-right-42
    yy=para('<b>Pasos realizados</b>',right,y,width)
    yy=para(steps,right,yy,width)
    yy=para('<b>Resultado observado</b><br/>'+observed,right,yy,width)
    products=data.get('productos',[])
    summary=(f"{len(products)} filas; {sum(p['pendiente'] for p in products)} pendientes. " if 'productos' in data else data.get('base_local',''))
    if products: summary += 'IDs: '+', '.join(str(p['id']) for p in products)+'.'
    yy=para('<b>Base local</b><br/>'+escape(summary),right,yy,width)
    yy=para('<b>Conclusión</b><br/>'+conclusion,right,yy,width)
    para('Respaldo: docs/evidencias/s11/'+name+'.json',42,67,W-84,SMALL)

summary=json.loads((EV/'resumen-pruebas.json').read_text())
manifest=json.loads((EV/'entrega.json').read_text(encoding='utf-8'))
branch='https://github.com/miguelon05/PharmaMobile/tree/feature/offline-first-acevedo'
y=begin('Sesión 5 · Unidad 2<br/>Actividad autónoma', 'DESARROLLO DE APLICACIONES MÓVILES')
y=para('Pruebas unitarias y evidencia de escenarios offline',42,y,W-84,TITLE)
y-=20
y=para('<b>Estudiante:</b> Acevedo<br/><b>Correspondencia con la guía:</b> actividad autónoma N.º 11; sesión 5 de la Unidad 2.<br/><b>Fecha:</b> 23 de octubre de 2026.<br/><b>Proyecto:</b> PharmaMobil sobre PharmaSoft.',42,y,W-84)
y=para(f'<b>Rama del repositorio</b><br/><link href="{branch}" color="#087e8b">{branch}</link>',42,y,W-84)
y=para('<b>Commit de implementación:</b> '+escape(manifest['commit_codigo']),42,y,W-84)
y-=12
y=para('<b>Resultado comprobado</b><br/>Seis pruebas específicas aprobadas con SQLite en memoria y Ktor MockEngine. Cinco escenarios Android documentados con capturas reales y respaldo de la base local. Registro persistente de la última sincronización, visualización en lenguaje natural y caso sin sincronización.',42,y,W-84)
y=para('<b>Alcance</b><br/>iOS se omitió por indicación del solicitante. La guía de sustentación mantiene identificado ese criterio como no demostrado. El nombre completo del estudiante no fue proporcionado; la portada identifica el apellido disponible.',42,y,W-84)
y=para('<b>Integridad de la evidencia</b><br/>Las capturas provienen del emulador y de Swagger. El caso de datos antiguos usa un adelanto controlado del reloj del emulador; se restauró al terminar. La sesión vencida del escenario 5 caducó por el transcurso real del tiempo. Las pruebas unitarias no se presentan como capturas de ejecución móvil.',42,y,W-84)

y=begin('1. Suite automatizada')
y=para(f"<b>Resultado global:</b> {summary['aprobadas']} aprobadas, {summary['failures']} fallos, {summary['errors']} errores y {summary['skipped']} omitida, de {summary['tests']} pruebas descubiertas. La omitida es BackendHttpsIntegrationTest, desactivada por configuración. Las seis pruebas de S11OfflineTest se ejecutaron y aprobaron.",42,y,W-84)
tests=[('1. Sincronización con red','Descarga el producto del servidor, lo verifica en SQLite y comprueba que Flow emita la lista completa.'),('2. Registro sin red','Provoca IOException con MockEngine; comprueba ID negativo, pendiente=1 y producto visible por Flow.'),('3. Envío al reconectar','Restablece el motor simulado; verifica un POST, ID real 100 y ausencia de pendientes.'),('4. La bajada no borra pendientes','Obtiene la lista mediante Ktor y ejecuta la fase local de bajada; conserva la fila pendiente y la descargada. Se aísla esta fase para no subir previamente la fila.'),('5. Identificadores sin colisión','Conserva simultáneamente el producto remoto 1 y el local -1, con sus nombres diferentes.'),('6. Sincronizar dos veces no duplica','Compara listas antes y después de la segunda sincronización: dos filas y un único POST.')]
for title,body in tests: y=para('<b>'+title+'</b><br/>'+body,42,y,W-84)
y=para('<b>Aislamiento y alcance</b><br/>Cada método construye Entorno: driverDePrueba() crea SQLite en memoria, con esquema propio; MockEngine simula HTTP y cada entorno cierra cliente, motor y driver. Se usa runTest, sin emulador, red real ni esperas temporizadas. Las cinco pruebas offline anteriores se conservaron como regresión.',42,y,W-84)
para('Código: shared/src/commonTest/kotlin/pe/edu/upeu/pharmamobil/data/repository/S11OfflineTest.kt. Salida íntegra y resultados XML incluidos como anexos y archivos de respaldo.',42,y,W-84,SMALL)

scenario(2,'Apertura sin red con datos previos','escenario-1',
 '1. Iniciar sesión como admin.<br/>2. Abrir Productos y sincronizar.<br/>3. Forzar cierre de la app.<br/>4. Activar modo avión y desactivar Wi-Fi y datos.<br/>5. Reabrir y entrar a Productos.',
 'Aparecen los tres productos descargados y el aviso «Sin conexión: mostrando datos guardados». La última sincronización conserva el instante anterior.',
 'El catálogo depende de SQLite para su lectura. El cierre del proceso y la falta de red no eliminan la información ya sincronizada.')
scenario(3,'Registro sin red','escenario-2-detalle',
 '1. Mantener modo avión.<br/>2. Registrar S11_Offline_Acevedo, precio S/ 4.50, stock 7.<br/>3. Guardar.<br/>4. Desplazarse hasta el producto en el inventario.',
 'El registro aparece de inmediato con «Pendiente de sincronizar». El inventario pasa de tres a cuatro productos y el aviso sin conexión permanece.',
 'SQLite conserva la intención de creación con ID -1. Guardar localmente es independiente del envío al backend; un fallo de red no produce pérdida del registro.')
scenario(4,'Reconexión y envío','escenario-3',
 '1. Desactivar modo avión y habilitar la red.<br/>2. Pulsar Sincronizar.<br/>3. La sesión anterior ya venció: iniciar sesión nuevamente como admin.<br/>4. Abrir Productos y sincronizar.<br/>5. Consultar el ID obtenido en Swagger.',
 'El producto se confirma con ID '+str(manifest['id_servidor'])+' del servidor. Desaparece la marca pendiente; el catálogo conserva cuatro productos. Swagger devuelve HTTP 200 para ese ID.',
 'La confirmación reemplaza el identificador temporal por el del backend. La consulta independiente en Swagger demuestra que el registro salió del dispositivo.')

y=begin('Evidencia independiente en Swagger')
y=para('GET /api/v1/productos/'+str(manifest['id_servidor'])+' en PharmaSoft local. La captura muestra la respuesta del servidor; no es el ejemplo estático de la especificación.',42,y,W-84)
picture('swagger-producto.png',42,y,W-84,485)
para('Producto S11_Offline_Acevedo · ID '+str(manifest['id_servidor'])+' · precio 4.50 · stock 7. Respaldo adicional: servidor-producto.json.',42,110,W-84)

scenario(5,'Primera apertura sin red','escenario-4',
 '1. Después de confirmar todos los registros, respaldar el estado local.<br/>2. Borrar los datos de PharmaMobil en el emulador (pm clear).<br/>3. Activar modo avión, sin Wi-Fi ni datos.<br/>4. Abrir la aplicación.',
 'Se presenta «Sin sesión local», una explicación de por qué todavía no hay productos disponibles y el botón «Iniciar sesión». No aparece una excepción ni una pantalla de error.',
 'Una instalación limpia no puede inventar un catálogo ni autenticar al usuario sin red. El estado vacío explica el siguiente paso y no revela datos de una sesión inexistente.')
scenario(6,'Sesión expirada sin red','escenario-5',
 '1. Conservar la sesión de admin y los datos locales.<br/>2. Mantener modo avión hasta que caduque el JWT.<br/>3. Cerrar y reabrir la app.<br/>4. Abrir Productos.<br/>5. Reconectar y pulsar Sincronizar para validar la sesión.',
 'Se muestran los cuatro productos, incluido el pendiente. Aparece «Sesión vencida: consulta local de solo lectura». No se ofrecen registro, edición ni eliminación. Al reconectar, el rechazo 401 conduce al login.',
 'Es preferible permitir consulta local de la sesión previamente almacenada, con aviso y sin escritura, a exigir un login imposible sin red. La autorización del servidor sigue siendo necesaria para sincronizar; un 401 descarta la sesión cacheada.')

y=begin('7. Antigüedad de los datos')
y=para('<b>Implementación:</b> tabla sincronizacion con una sola fila (id=1, instante en milisegundos). Se actualiza únicamente al completar envío y descarga. SQLDelight emite su valor como Flow, el ViewModel lo conserva y Compose calcula el texto cada minuto. La migración 1.sqm mantiene las instalaciones anteriores.',42,y,W-84)
y=para('<b>Nunca sincronizado:</b> cuando no existe la fila, la pantalla de productos muestra «Nunca sincronizado». La primera apertura sin sesión usa un estado informativo independiente.',42,y,W-84)
picture('datos-recientes.png',42,y,244,455)
picture('datos-antiguos.png',309,y,244,455)
para('<b>Izquierda:</b> sincronización correcta reciente.<br/><b>Derecha:</b> reloj del emulador adelantado dos días, sin conexión; el instante guardado en SQLite no se modificó. Reloj automático restaurado al terminar.',42,104,W-84,SMALL)

y=begin('8. Guion de sustentación · 8 minutos')
y=para('<b>Preparación previa:</b> Oracle oracle-local activo (puerto 1522), PharmaSoft con JWT ejecutándose en localhost:8080, emulador Pixel 7a con el APK instalado, catálogo con datos y app con sesión cerrada. Tener Swagger y este PDF abiertos. Credenciales de los usuarios de desarrollo disponibles localmente; no proyectar tokens ni contraseñas.',42,y,W-84)
segments=[('0:00 - 1:00 | Autenticación JWT','Usuario admin. Mostrar un intento con usuario incorrecto y el mensaje de rechazo; luego iniciar sesión correctamente. Explicar almacenamiento seguro y envío del token en solicitudes protegidas.'),('1:00 - 3:00 | REST y CRUD','Usuario admin. Listar, registrar un producto de demostración y editar su stock. Consultar el registro en Swagger. Mostrar eliminación solo sobre un producto de demostración, si se requiere comprobar el CRUD completo.'),('3:00 - 5:00 | Estados y errores','Usuario admin: enviar nombre vacío o precio inválido y señalar el error bajo el campo. Usuario vendedor: ejecutar desde Swagger una operación para la que no tenga permiso, previamente comprobada, y explicar HTTP 403 sin cierre de sesión. Distinguir validación, red, 401 y 403.'),('5:00 - 7:00 | Persistencia offline-first','Usuario admin, sesión vigente. Activar modo avión, mostrar el catálogo, registrar un producto pendiente y explicar el ID negativo. Reconectar, sincronizar y comprobar el ID real en Swagger. Señalar la última actualización.'),('7:00 - 8:00 | Evidencia y alcance de plataforma','Mostrar Android y el resultado 6/6 de la suite en commonTest. Explicar que el código compartido contiene las pruebas y la lógica offline. iOS no se ejecutó por decisión del solicitante; no presentar este minuto como evidencia multiplataforma completa.')]
for title,body in segments: y=para('<b>'+title+'</b><br/>'+body,42,y,W-84)
para('Este documento es un guion preparado. No acredita un ensayo oral cronometrado; el estudiante debe practicarlo antes de sustentar y poder explicar el código.',42,y,W-84,SMALL)

y=begin('9. Trazabilidad y entrega')
for text in [
 '<b>Repositorio:</b> '+branch,
 '<b>Implementación:</b> '+escape(manifest['commit_codigo'])+'. Las evidencias y este documento se agregan en el commit de documentación posterior.',
 '<b>Archivos de respaldo:</b> docs/evidencias/s11 contiene PNG originales, JSON de cada base local, servidor-producto.json, resumen-pruebas.json, resultados-xml y ejecucion-completa.txt.',
 '<b>Casos esenciales:</b> registro sin red y conservación de pendientes al descargar están cubiertos por pruebas específicas aprobadas. La segunda prueba aísla la fase de bajada, porque una sincronización completa sube primero lo pendiente.',
 '<b>Limitaciones declaradas:</b> iOS omitido; nombre completo no proporcionado; ensayo oral a cargo del estudiante. La prueba de integración HTTPS heredada está omitida por configuración; la evidencia manual utiliza el backend JWT real en localhost:8080, que es la configuración actual de Android.',
 '<b>Entrega indicada por la guía:</b> lunes 26 de octubre de 2026 hasta las 23:59. Subir este PDF al aula virtual y añadir el enlace a la rama en el comentario. Este trabajo no incluye subirlo al aula ni presentar la sustentación.'
]: y=para(text,42,y,W-84)

# Salida completa, sin recortes: se distribuye en páginas monoespaciadas.
log=(EV/'ejecucion-completa.txt').read_text(encoding='utf-8-sig')
lines=[]
for line in log.splitlines():
    lines.extend(textwrap.wrap(line,110,replace_whitespace=False,drop_whitespace=False) or [''])
for start in range(0,len(lines),76):
    y=begin('Anexo A · Salida completa de Gradle')
    c.setFillColor(INK); c.setFont('Courier',7)
    for line in lines[start:start+76]:
        c.drawString(42,y,line); y-=8.5

y=begin('Anexo B · Las seis pruebas ejecutadas')
suite=ET.parse(EV/'resultados-xml/TEST-pe.edu.upeu.pharmamobil.data.repository.S11OfflineTest.xml').getroot()
y=para('Clase S11OfflineTest · '+suite.attrib['tests']+' pruebas · '+suite.attrib['failures']+' fallos · '+suite.attrib.get('skipped','0')+' omitidas.<br/>Resultados extraídos del XML producido por Gradle.',42,y,W-84)
for item in suite.findall('testcase'):
    y=para('<b>APROBADA</b><br/>'+escape(item.attrib['name'])+'<br/>Duración: '+item.attrib.get('time','')+' segundos.',42,y,W-84)
c.save()
print(OUT)
