from pathlib import Path
from xml.sax.saxutils import escape
import re, shutil, xml.etree.ElementTree as ET
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Table, TableStyle, KeepTogether, Flowable
from reportlab.platypus.tableofcontents import TableOfContents
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
ROOT=Path.cwd()
OUT=ROOT/'output/pdf/Manual-Arquitectura-PharmaMobil_Acevedo.pdf'
for n,f in [('Arial','arial.ttf'),('ArialBold','arialbd.ttf')]: pdfmetrics.registerFont(TTFont(n,'C:/Windows/Fonts/'+f))
styles=getSampleStyleSheet()
styles.add(ParagraphStyle(name='Body',fontName='Arial',fontSize=10,leading=14,spaceAfter=9,textColor=colors.black))
styles.add(ParagraphStyle(name='TitleBlack',fontName='ArialBold',fontSize=24,leading=30,spaceAfter=18))
styles.add(ParagraphStyle(name='SectionBlack',fontName='ArialBold',fontSize=17,leading=22,spaceAfter=15))
styles.add(ParagraphStyle(name='SubBlack',fontName='ArialBold',fontSize=11,leading=15,spaceBefore=10,spaceAfter=8))
styles.add(ParagraphStyle(name='SmallBlack',fontName='Arial',fontSize=8.4,leading=11,spaceAfter=6))
styles.add(ParagraphStyle(name='CellBlack',fontName='Arial',fontSize=7.9,leading=10.6))
styles.add(ParagraphStyle(name='CodeBlack',fontName='Courier',fontSize=8,leading=11,spaceAfter=8,wordWrap='CJK'))
story=[]; md=[]
def p(t,style='Body'):
 story.append(Paragraph(t,styles[style])); md.append(re.sub('<[^>]+>','',t))
def title(t):
 story.append(PageBreak()); p(t,'SectionBlack')
def sub(t): p(t,'SubBlack')
def table(rows,widths):
 cells=[[Paragraph(escape(str(c)),styles['CellBlack']) for c in r] for r in rows]
 t=Table(cells,colWidths=widths,repeatRows=1,hAlign='LEFT')
 t.setStyle(TableStyle([('GRID',(0,0),(-1,-1),.5,colors.black),('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),6),('RIGHTPADDING',(0,0),(-1,-1),6),('TOPPADDING',(0,0),(-1,-1),7),('BOTTOMPADDING',(0,0),(-1,-1),7),('LINEBELOW',(0,0),(-1,0),1.2,colors.black)]))
 story.append(t); story.append(Spacer(1,10)); md.extend([' | '.join(r) for r in rows])
def box(t,height=95):
 content=Paragraph('<b>SE NECESITA CAPTURA REAL</b><br/>'+t,styles['SmallBlack'])
 tb=Table([[content]],colWidths=[499],rowHeights=[height]); tb.setStyle(TableStyle([('BOX',(0,0),(-1,-1),.8,colors.black),('VALIGN',(0,0),(-1,-1),'TOP'),('TOPPADDING',(0,0),(-1,-1),10),('LEFTPADDING',(0,0),(-1,-1),10),('RIGHTPADDING',(0,0),(-1,-1),10)])); story.append(tb); story.append(Spacer(1,12)); md.append('SE NECESITA CAPTURA REAL: '+t)
class Diagram(Flowable):
 def __init__(self): Flowable.__init__(self); self.width=499; self.height=295
 def draw(self):
  c=self.canv; c.setStrokeColor(colors.black); c.setFillColor(colors.black)
  def rect(x,y,w,h,lines):
   c.rect(x,y,w,h); c.setFont('ArialBold',10)
   for i,l in enumerate(lines):
    c.drawCentredString(x+w/2,y+h-18-i*16,l); c.setFont('Arial',9)
  def arrow(x1,y1,x2,y2):
   import math
   c.line(x1,y1,x2,y2); a=math.atan2(y2-y1,x2-x1)
   for delta in [-.45,.45]: c.line(x2,y2,x2-8*math.cos(a+delta),y2-8*math.sin(a+delta))
  rect(0,170,236,92,['PRESENTACIÓN / MVVM','ProductoScreen → ProductoViewModel','ProductoUiState · FormularioProducto','ProductoUi · componentes Compose'])
  rect(265,170,234,92,['DATOS','ProductoRepositorioEnMemoria','implementa ProductoRepository','lista + ids + Mutex'])
  rect(38,28,422,92,['DOMINIO','RegistrarProductoUseCase · ListarProductosUseCase','ProductoRepository · Producto','Reglas, modelos y contratos independientes'])
  arrow(115,170,115,120); arrow(382,170,382,120)
  c.setFont('Arial',8); c.drawString(121,141,'usa casos de uso'); c.drawString(320,141,'implementa contrato')
  c.setFont('ArialBold',9); c.drawString(4,282,'AppModule (composición): ensambla presentación, dominio y datos.')
  c.setFont('Arial',8); c.drawString(38,8,'Las flechas señalan dependencias de código; el dominio no depende del exterior.')
class Doc(SimpleDocTemplate):
 def afterFlowable(self,f):
  if isinstance(f,Paragraph) and f.style.name=='SectionBlack' and re.match(r'^[1-5]\.',f.getPlainText()): self.notify('TOCEntry',(0,f.getPlainText(),self.page))
def footer(c,d):
 c.setStrokeColor(colors.black); c.setFillColor(colors.black); c.setFont('Arial',8)
 if d.page>1:
  c.line(48,44,547,44); c.drawString(48,31,'PharmaMobil | Manual de arquitectura | Sesión 5'); c.drawRightString(547,31,str(d.page))
# Portada
story.append(Spacer(1,70)); p('DESARROLLO DE APLICACIONES MÓVILES','SubBlack'); p('ACTIVIDAD AUTÓNOMA<br/>SESIÓN 5 · UNIDAD 1','SubBlack'); story.append(Spacer(1,26))
p('Manual de arquitectura<br/>de PharmaMobil','TitleBlack'); p('Responsabilidades por capa y pruebas de flujo','SectionBlack')
p('Reto 02 · Clean + MVVM en Kotlin Multiplatform')
story.append(Spacer(1,28))
p('<b>Apellidos y nombres:</b> Acevedo, [completar nombres]<br/><b>Código:</b> [completar]<br/><b>Fecha de entrega:</b> [completar]')
p('<b>Curso:</b> Desarrollo de Aplicaciones Móviles<br/><b>Unidad 1:</b> Fundamentos de Kotlin Multiplatform y UI con Compose Multiplatform<br/><b>Modalidad:</b> trabajo independiente')
p('<b>Repositorio:</b><br/><link href="https://github.com/miguelon05/PharmaMobile" color="black">https://github.com/miguelon05/PharmaMobile</link>')
p('<b>Rama local de la actividad:</b> codex/actividad-autonoma-s5<br/><b>Base revisada:</b> 1565816 · GUIA PRACTICA 08/09','SmallBlack')
p('Documento en negro sobre fondo blanco. Las cajas de evidencia indican las capturas reales que deben insertarse; no representan capturas de una ejecución.','SmallBlack')
# Indice
story.append(PageBreak()); p('Índice','TitleBlack'); toc=TableOfContents(); toc.levelStyles=[ParagraphStyle(name='TOCBlack',fontName='Arial',fontSize=11,leading=20,spaceBefore=10)]; story.append(toc)
story.append(Spacer(1,35)); sub('Objetivo y alcance')
p('Documentar la arquitectura Clean + MVVM del proyecto KMP, relacionando cada componente con su capa y responsabilidad, y verificar mediante commonTest el recorrido de los estados de la interfaz.')
p('El manual describe el código presente en shared, incluidos los componentes heredados, las funciones Compose y los puntos de arranque por plataforma. Las clases de pruebas se distinguen de las clases de producción. Pedidos conserva modelos de dominio y un destino de navegación, pero su pantalla operativa todavía no está implementada.')
p('La guía se usa como especificación del trabajo. Las afirmaciones sobre implementación y resultados se contrastaron con el repositorio local y los registros generados durante esta actividad. El tutorial anterior contiene referencias históricas que ya no describen toda la versión actual.')
sub('Estado de la entrega')
p('Pruebas Android: 53 ejecutadas, 0 fallos, 0 errores; cuatro corresponden al nuevo ProductoFlujoTest. iOS: compilación de las pruebas completada, ejecución omitida por el host Windows. Capturas reales, datos personales, publicación de la rama y envío al aula: pendientes de completar.')
#1
title('1. Mapa de capas'); story.append(Diagram()); p('<b>Figura 1.</b> Dependencias de Producto en PharmaMobil. <i>Elaboración propia</i>, a partir del código del repositorio.','SmallBlack')
p('La presentación solicita operaciones a los casos de uso; estos dependen del contrato ProductoRepository y del modelo Producto. La implementación en memoria depende del contrato que implementa. AppModule es el punto externo de composición: importa las clases y registra sus dependencias en Koin; ninguna clase del dominio necesita conocerlo.')
sub('Recorrido de una carga')
p('ProductoScreen observa uiState con collectAsStateWithLifecycle. ProductoViewModel inicia cargarProductos(), emite Cargando y llama a ListarProductosUseCase. El contrato entrega una lista o un fallo; el ViewModel convierte los modelos con aUi() y publica SinProductos, ConProductos o Error. El resultado vuelve hacia la pantalla como datos, sin crear una dependencia del dominio hacia la UI.')
sub('Recorrido de un registro')
p('Los cambios de los campos actualizan FormularioProducto. Al registrar, RegistrarProductoUseCase valida nombre, precio y stock antes de invocar el repositorio. Si el precio es cero, devuelve ProductoInvalidoException con ErroresDeProducto; el ViewModel asigna precioError y conserva la fase del inventario. Si el registro es válido, limpia el formulario, anuncia el éxito y recarga el inventario.')
sub('Extensión a clientes y plataformas')
p('Clientes sigue el mismo patrón mediante ClienteViewModel, RegistrarClienteUseCase, ListarClientesUseCase y ClienteRepository. Android e iOS aportan platformModule; initKoin incorpora los módulos comunes y el específico. MainViewController integra Compose en iOS. Los modelos Pedido y DetallePedido permanecen dentro del dominio.')
#2
title('2. Tabla de responsabilidades')
p('Rutas relativas a shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil, salvo indicación. Se incluyen las clases, interfaces, tipos anidados y componentes funcionales relevantes: ProductoScreen, AppModule y PharmaMobilTheme son funciones o archivos de composición, no clases Kotlin.')
rows=[]
def r(n,l,a,b,c): rows.append([n,l,a,b,c])
r('Producto','Dominio','Representa producto; exige datos válidos y calcula reposición.','Acceder a UI o persistencia.','Faltan entidad y regla de stock mínimo.')
r('Cliente','Dominio','Representa identidad y contacto con invariantes básicas.','Renderizar o enviar datos a red.','Pedidos y registro pierden su cliente tipado.')
r('Pedido','Dominio','Agrupa cliente, detalles y estado del pedido.','Orquestar navegación o almacenamiento.','Desaparece el agregado del pedido.')
r('DetallePedido','Dominio','Exige cantidad positiva y calcula subtotal.','Consultar repositorios o dibujar precios.','Se pierde la línea y su cálculo válido.')
r('EstadoPedido','Dominio','Limita los estados posibles del pedido.','Cambiar pantallas o guardar pedidos.','El estado deja de estar tipado y acotado.')
for n,a in [('Pendiente','pedido por atender'),('Procesando','pedido en preparación'),('Entregado','pedido entregado'),('Rechazado','rechazo con motivo')]: r('EstadoPedido.'+n,'Dominio','Representa '+a+'.','Ejecutar por sí mismo la transición.','Falta esta variante en el ciclo del pedido.')
r('ProductoRepository','Dominio','Define registrar y listar productos como suspend.','Conocer HTTP, Compose o Koin.','Casos de uso pierden el contrato sustituible.')
r('ClienteRepository','Dominio','Define registrar y listar clientes como suspend.','Depender del repositorio concreto.','Se acoplarían clientes y almacenamiento.')
for obj in ['Producto','Cliente']:
 r('Registrar'+obj+'UseCase','Dominio','Valida campos y registra un '+obj.lower()+' válido.','Manipular widgets o resolver Koin.','No hay entrada central de registro validado.')
 r('Listar'+('Productos' if obj=='Producto' else 'Clientes')+'UseCase','Dominio','Obtiene la lista mediante el contrato y devuelve Result.','Formatear tarjetas o instanciar datos.','ViewModel pierde la operación de consulta.')
 r('ErroresDe'+obj,'Dominio','Agrupa errores de validación por campo.','Dibujar o almacenar entidades.','No llegan errores independientes al formulario.')
 r(obj+'InvalidoException','Dominio','Transporta errores de campos en Result.failure.','Actualizar uiState.','Se pierde la distinción de validación y fallo externo.')
r('resultadoDe (función)','Dominio','Convierte fallos a Result y propaga cancelación.','Tratar cancelación como error de negocio.','Casos de uso pierden su manejo común de fallos.')
for obj in ['Producto','Cliente']: r(obj+'RepositorioEnMemoria','Datos','Mantiene lista e ids en memoria con exclusión mutua.','Ser dueño del formulario o navegar.','El contrato queda sin almacenamiento de ejecución.')
for obj,plural,sin in [('Producto','Productos','SinProductos'),('Cliente','Clientes','SinClientes')]:
 r(obj+'ViewModel','Presentación','Orquesta casos de uso y expone StateFlow de UI.','Acceder a la implementación de datos.','La pantalla pierde acciones y estado persistente en memoria.')
 r(obj+'UiState','Presentación','Reúne fase, formulario, registro y confirmación.','Ejecutar consultas o validación de negocio.','No hay estado único coherente para renderizar.')
 r('Formulario'+obj,'Presentación','Conserva entradas y mensajes de cada campo.','Registrar o definir reglas de negocio.','Se pierde el estado del formulario.')
 r(obj+'Ui','Presentación','Entrega datos preparados para mostrar.','Modificar entidades o almacenar datos.','El formato volvería a mezclarse con la pantalla.')
 r(obj+'UiState.Fase','Presentación','Define fases excluyentes mediante sealed interface.','Consultar repositorios.','Se pierde exhaustividad del when de pantalla.')
 for n,a in [('Cargando','consulta en curso'),(sin,'lista vacía'),('Con'+plural,'lista preparada para UI'),('Error','fallo con mensaje')]:
  r(obj+'UiState.Fase.'+n,'Presentación','Representa '+a+'.','Ejecutar la carga o el reintento.','No puede representarse esta situación de UI.')
 r(obj+'Screen (función)','Presentación','Observa el estado y delega eventos al ViewModel.','Validar negocio o crear repositorios.','No se puede operar visualmente este módulo.')
r('Screen','Presentación','Define los destinos de navegación.','Aplicar reglas o persistir datos.','El enrutado deja de ser exhaustivo.')
for n in ['Inicio','Productos','Clientes','Pedidos']: r('Screen.'+n,'Presentación','Identifica el destino '+n+'.','Construir repositorios.','Falta el destino en la navegación.')
r('Destino (privada, App.kt)','Presentación','Relaciona Screen, título e icono del menú.','Ejecutar casos de uso.','Se pierde la estructura común de destinos.')
r('App (función)','Presentación','Compone tema, menú y pantalla seleccionada.','Guardar entidades o validar negocio.','No hay raíz compartida de interfaz.')
r('InicioScreen (función)','Presentación','Presenta portada y accesos de la aplicación.','Consultar datos directamente.','Falta la pantalla de bienvenida.')
r('PharmaMobilTheme (función)','Presentación','Aplica MaterialTheme según modo claro u oscuro.','Contener reglas de negocio.','Se pierde el tema central de la app.')
r('ValidatedTextField (función)','Presentación','Muestra entrada, ayuda y error proporcionados.','Decidir si un producto es válido.','Se duplica la presentación de campos y errores.')
r('MensajeExito (función)','Presentación','Renderiza una confirmación de operación.','Registrar productos o decidir éxito.','Falta la confirmación visual reutilizable.')
r('EstadoVacio (función)','Presentación','Muestra icono, texto y acción opcional.','Cargar inventario por cuenta propia.','Falta la vista común de vacío, error y pendientes.')
r('AppModule.kt (archivo)','Composición','Declara módulos y arranca Koin con initKoin.','Ser dependencia de los modelos.','No se ensambla el grafo de dependencias.')
r('dataModule (valor)','Composición','Vincula contratos a repositorios singleton.','Ejecutar reglas del formulario.','Koin no resuelve repositorios por interfaz.')
r('domainModule (valor)','Composición','Registra los cuatro casos de uso como factory.','Contener reglas de validación.','Koin no construye los casos de uso.')
r('presentationModule (valor)','Composición','Registra los dos ViewModels con sus casos de uso.','Renderizar pantallas.','La UI no obtiene ViewModels por Koin.')
r('platformModule (expect/actual)','Composición','Reserva dependencias de Android e iOS en sus source sets.','Introducir Android o iOS en dominio.','Se pierde el punto específico de plataforma.')
r('initKoinIos (función, iosMain)','Composición','Expone el arranque de Koin al código Swift.','Duplicar el grafo común.','El arranque Swift pierde su función de inicialización.')
r('MainViewController (función, iosMain)','Presentación','Aloja App en un ComposeUIViewController.','Aplicar validación o acceder a datos.','SwiftUI pierde la entrada a la UI compartida.')
table([['Clase / componente','Capa','Responsabilidad en una frase','Lo que tiene prohibido hacer','Qué se rompería si desapareciera']]+rows,[114,66,106,98,115])
sub('Clases de pruebas del módulo shared')
tr=[]
for n,resp,loss in [
('ProductoFlujoTest','Comprueba los cuatro escenarios exactos del Reto 02.','Falta evidencia de flujo exigida por la guía.'),
('ProductoFlujoTest.RepositorioControlado','Fuerza listas o excepción y cuenta invocaciones.','No se demuestra ausencia de efectos con precio cero.'),
('FakeProductoRepository','Ofrece inventario y fallos controlables para pruebas previas.','Pruebas de producto pierden su doble.'),
('FakeClienteRepository','Ofrece clientes y fallos controlables.','Pruebas de cliente pierden su doble.'),
('ProductoViewModelTest','Comprueba formato, validación, carga y registro de productos.','Se pierde cobertura previa de presentación.'),
('ClienteViewModelTest','Comprueba estados y acciones de clientes.','Se pierde cobertura de la presentación de clientes.'),
('RegistrarProductoUseCaseTest','Verifica validación y registro de productos.','Se debilita la detección de reglas incorrectas.'),
('RegistrarClienteUseCaseTest','Verifica validación y registro de clientes.','Se debilita la detección de datos de contacto inválidos.'),
('ListarProductosUseCaseTest','Verifica listado correcto y fallo encapsulado.','No se detectan regresiones de consulta.'),
('ProductoTest','Verifica invariantes y reposición del producto.','Se pierde cobertura del modelo Producto.'),
('ClienteTest','Verifica invariantes del cliente.','Se pierde cobertura del modelo Cliente.'),
('ProductoRepositorioEnMemoriaTest','Verifica almacenamiento e ids de productos.','No se detectan regresiones del repositorio.'),
('ClienteRepositorioEnMemoriaTest','Verifica almacenamiento e ids de clientes.','No se detectan regresiones de persistencia temporal.'),
('AppModuleTest','Comprueba resolución del grafo Koin.','Errores de ensamblaje llegarían a ejecución de la app.'),
('SharedCommonTest','Comprueba una aserción de ejemplo en commonTest.','Se pierde solo la prueba inicial, no cobertura de negocio.'),
('SharedLogicAndroidHostTest','Comprueba una aserción básica en androidHostTest.','Falta el ejemplo específico del host Android.'),
('SharedLogicIOSTest','Comprueba una aserción básica en iosTest.','Falta el ejemplo específico del target iOS.')]: tr.append([n,'Pruebas',resp,'Participar en el código de producción.',loss])
table([['Clase','Capa','Responsabilidad en una frase','Lo que tiene prohibido hacer','Qué se rompería si desapareciera']]+tr,[114,66,106,98,115])
p('Los companion object guardan constantes: Producto.STOCK_MINIMO y retardos de los repositorios. Los helpers privados de pantallas y los mapeadores aUi() pertenecen a presentación; no son nuevas capas ni clases. Las pruebas de ejemplo no acreditan flujos funcionales.','SmallBlack')
#3
title('3. Decisiones justificadas')
just=[
'Modelé ProductoUiState como una data class que conserva formulario, indicador de registro y mensaje de éxito, junto con una Fase sellada para el inventario. Así puedo representar Cargando, SinProductos, ConProductos o Error de forma excluyente sin borrar lo que la persona escribió. Frente a varios booleanos independientes, evito combinaciones contradictorias, como cargar y mostrar un error terminal simultáneamente. Frente a sellar todo el estado, evito repetir el formulario en cada variante o perderlo al cambiar de fase. El when exhaustivo obliga a atender nuevas fases; StateFlow expone una instantánea de solo lectura. El indicador registrando describe una operación distinta del listado y por eso permanece separado.',
'Trasladé las reglas de ProductoValidator a RegistrarProductoUseCase porque un registro debe ser válido aunque llegue desde otra pantalla. El caso de uso valida antes de llamar a ProductoRepository y devuelve errores por campo dentro de ProductoInvalidoException. Producto también protege invariantes cuando se construye directamente: esa defensa no sustituye los mensajes útiles del formulario. Mantener reglas únicamente en Compose habría permitido saltarlas desde otro consumidor. Respecto de ClienteValidator, la guía describe una versión anterior: en el código revisado ya no existe y RegistrarClienteUseCase concentra nombre, correo y teléfono. Mantendría esa migración y reservaría a la presentación el formato, los mensajes y los eventos; no reintroduciría el validador de negocio en la pantalla.',
'Cuando exista la implementación remota, cambiaría el enlace de la línea 21 de shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/di/AppModule.kt: single&lt;ProductoRepository&gt; { ProductoRepositorioEnMemoria() }. Lo sustituiría por una construcción de ProductoRepositorioRemoto con sus dependencias reales. La línea 9, que importa la implementación en memoria, también se actualizaría. La sustitución del enlace es puntual, pero desarrollar el cliente HTTP, los mapeos y su configuración no equivale a cambiar una sola línea. Los casos de uso y ProductoViewModel seguirían usando el mismo contrato suspend; las dependencias específicas del motor se registrarían en platformModule. Esta separación permite conservar las pruebas con repositorios falsos mientras se incorpora el backend.'
]
for t in just:p(t)
count=len(re.sub('<[^>]+>','', ' '.join(just)).split()); p(f'Extensión de la justificación: {count} palabras. Líneas verificadas sobre la versión local documentada.','SmallBlack')
#4
title('4. Pruebas de flujo')
p('Se agregó ProductoFlujoTest.kt en shared/src/commonTest/kotlin/pe/edu/upeu/pharmamobil/presentation/producto. Sus cuatro pruebas usan RepositorioControlado, una implementación falsa de ProductoRepository con las dos funciones suspend del contrato. No requieren emulador, red ni el repositorio de producción.')
p('Cada prueba ejecuta runTest, instala StandardTestDispatcher(testScheduler) como Dispatchers.Main y avanza con advanceUntilIdle() antes de leer el resultado. Los tres casos de carga también comprueban Cargando antes de avanzar. finally restaura Dispatchers.Main para no contaminar otras pruebas.')
sub('Matriz de pruebas de flujo')
matrix=[['N.º / condición inicial forzada','Acción ejecutada','Estado esperado','Estado observado','Resultado'],
['1. Repositorio falso vacío','Crear ViewModel y avanzar dispatcher.','Fase.SinProductos','Android: Cargando → SinProductos. iOS: no ejecutado.','Android: PASS. iOS: pendiente.'],
['2. Repositorio falso con tres productos','Crear ViewModel y avanzar dispatcher.','Fase.ConProductos con 3 elementos','Android: Cargando → ConProductos; lista mapeada completa. iOS: no ejecutado.','Android: PASS. iOS: pendiente.'],
['3. Repositorio falso lanza excepción','Crear ViewModel y avanzar dispatcher.','Fase.Error','Android: Error con “Sin conexión”. iOS: no ejecutado.','Android: PASS. iOS: pendiente.'],
['4. Nombre Paracetamol, precio “0”, stock “5”','Completar carga inicial; llenar formulario; registrar y avanzar.','Error de precio sin llamar al repositorio durante el registro','Android: “El precio debe ser mayor a 0”; 0 registros; sin listados adicionales. iOS: no ejecutado.','Android: PASS. iOS: pendiente.']]
table(matrix,[101,93,103,122,80])
p('En el caso 4 ya ocurrió un listado al construir el ViewModel. “Sin llamar al repositorio” se verifica sobre la acción de registrar: cero llamadas a registrar y ninguna llamada adicional a listar. También se comprueba que registrando vuelve a false, el precio “0” permanece, no hay éxito y la fase sigue en SinProductos.','SmallBlack')
sub('Resultados reproducibles')
p('<b>Android:</b> gradlew.bat :shared:testAndroidHostTest --rerun-tasks --console=plain. BUILD SUCCESSFUL; 53 pruebas, 0 fallos, 0 errores y 0 omitidas. ProductoFlujoTest: 4 pruebas satisfactorias. Log: docs/evidencias/android-host-test.txt. Resultados originales: shared/build/test-results/testAndroidHostTest. Informe: shared/build/reports/tests/testAndroidHostTest/index.html.')
p('<b>iOS:</b> gradlew.bat :shared:iosSimulatorArm64Test --console=plain. La compilación de pruebas concluyó, pero linkDebugTestIosSimulatorArm64 e iosSimulatorArm64Test aparecen como SKIPPED. BUILD SUCCESSFUL no significa que las pruebas iOS hayan pasado. Log: docs/evidencias/ios-simulator-test.txt.')
story.append(PageBreak()); sub('Evidencias por prueba: Android')
p('Abrir en Android Studio el archivo ProductoFlujoTest.kt. Ejecutar la clase y expandir sus cuatro resultados. Alternativamente, abrir el informe HTML indicado y seleccionar la clase. Cada captura debe mostrar nombre del caso, resultado y contexto de ejecución; insertar imágenes reales en estas cajas.')
for n,name,desc in [(1,'repositorioVacioProduceSinProductos','Mostrar la prueba aprobada y las aserciones de Cargando y SinProductos.'),(2,'tresProductosProducenConProductos','Mostrar la prueba aprobada y la comprobación de los tres productos.'),(3,'excepcionProduceError','Mostrar la prueba aprobada y el mensaje “Sin conexión”.'),(4,'precioCeroDejaErrorSinLlamarAlRepositorio','Mostrar la prueba aprobada, el error de precio, registros = 0 y listados sin incremento.')]:
 sub(f'Captura {n} · caso {n}'); box(f'En Android Studio o en el reporte HTML, localizar {name}. {desc}',90)
story.append(PageBreak()); sub('Evidencias de ejecución por plataforma')
box('Android: desde la raíz del proyecto, ejecutar .\\gradlew.bat :shared:testAndroidHostTest --rerun-tasks --console=plain. Capturar la terminal con el comando y BUILD SUCCESSFUL; acompañar con el resumen del informe HTML que muestre 53 pruebas y cero fallos.',125)
box('iOS: en una Mac con Xcode y simulador iOS ARM64 disponible, abrir el mismo proyecto y ejecutar ./gradlew :shared:iosSimulatorArm64Test --rerun-tasks --console=plain. Capturar el comando y las pruebas ejecutadas (no SKIPPED). Mostrar los cuatro casos de ProductoFlujoTest y completar la columna iOS de la matriz con el resultado real.',145)
sub('Captura opcional de la aplicación')
box('Abrir PharmaMobil en Android Studio, ejecutar androidApp en un emulador o teléfono y entrar a Productos. Escribir Paracetamol, precio 0 y stock 5; pulsar Registrar. Capturar el formulario con “El precio debe ser mayor a 0”. Esta imagen complementa las pruebas; por sí sola no demuestra que el repositorio no recibió llamadas.',125)
p('Para completar la evidencia iOS se necesita ejecución en macOS. No se adjuntan imágenes simuladas ni se marca como aprobada una tarea omitida. Los registros de texto y XML incluidos son evidencia real de la ejecución Android realizada durante esta actividad.','SmallBlack')
#5
title('5. Bitácora de dificultades')
table([['Problema encontrado','Cómo se abordó','Resultado / verificación'],
['La cobertura existente no coincidía con la matriz: un producto y precio “abc”.','Se añadió ProductoFlujoTest con tres productos y precio “0”, más un falso con contadores.','Cuatro pruebas nuevas pasan; se verifica que validar precio no produce efectos en el repositorio.'],
['El ViewModel inicia corutinas; leer inmediatamente podía observar la fase inicial.','Se compartió testScheduler con StandardTestDispatcher y se usó advanceUntilIdle.','Los casos de carga verifican fase inicial y final de forma determinista.'],
['La guía y el tutorial histórico mencionan ClienteValidator, ausente en el código actual.','Se inspeccionó RegistrarClienteUseCase y el estado de clientes. Se documentó la migración existente.','La tabla y la justificación describen las clases reales, sin inventar un validador vigente.'],
['La primera ejecución de Gradle no pudo descargar la distribución por restricciones de red.','Se repitió la ejecución con acceso autorizado y el JDK incluido en Android Studio.','BUILD SUCCESSFUL; 53 pruebas Android sin fallos.'],
['La tarea de iOS termina como SKIPPED en Windows.','Se registró la salida y se dejó el procedimiento de ejecución en Mac con Xcode.','Compilación completada; ejecución iOS pendiente, sin atribuirle un PASS.']],[136,186,177])
sub('Comprobación final frente a la guía')
p('Se incluyen portada, índice, repositorio, cinco secciones, diagrama de elaboración propia, tabla con las cinco columnas, justificación de 250 a 400 palabras, punto de cambio con archivo y línea, matriz y bitácora. Las cuatro pruebas nuevas están en commonTest y pasan en Android. Faltan las capturas reales, la ejecución iOS, completar los datos del estudiante y publicar la rama antes de entregar en el aula virtual.')
sub('Preparación del envío')
p('Completar nombres, código y fecha de entrega; insertar las capturas indicadas y actualizar iOS con resultados comprobados. Revisar y publicar la rama codex/actividad-autonoma-s5 en el repositorio. Finalmente, subir Manual-Arquitectura-PharmaMobil_Acevedo.pdf al aula virtual. La creación de este documento no realiza una entrega académica ni confirma la publicación remota.')
sub('Fuentes y referencias (APA 7)')
p('Actividad autónoma: Sesión 5, Unidad 1. Manual de arquitectura de PharmaMobil: responsabilidades por capa y pruebas de flujo. (s. f.). [Guía de actividad no publicada, proporcionada por el estudiante].','SmallBlack')
p('miguelon05. (s. f.). <i>PharmaMobile</i> [Código fuente]. GitHub. https://github.com/miguelon05/PharmaMobile. Código local base 1565816 y pruebas añadidas en esta actividad.','SmallBlack')
p('Las figuras son de elaboración propia. El código y los registros locales constituyen la fuente primaria de las responsabilidades y los resultados; no se han usado capturas ajenas como evidencia.','SmallBlack')
doc=Doc(str(OUT),pagesize=A4,rightMargin=48,leftMargin=48,topMargin=48,bottomMargin=59,title='Manual de arquitectura de PharmaMobil: responsabilidades por capa y pruebas de flujo',author='Acevedo')
doc.multiBuild(story,onFirstPage=footer,onLaterPages=footer)
(ROOT/'docs/Manual-Arquitectura-PharmaMobil_Acevedo.md').write_text('\n\n'.join(md),encoding='utf-8')
for src in (ROOT/'shared/build/test-results/testAndroidHostTest').glob('*.xml'):
 dst=ROOT/'docs/evidencias/android-xml'/src.name; dst.parent.mkdir(exist_ok=True); shutil.copy2(src,dst)
print('PDF:',OUT,'Justificacion:',count,'palabras','Filas produccion:',len(rows),'Filas pruebas:',len(tr))
